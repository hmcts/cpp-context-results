package uk.gov.moj.cpp.results.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

import java.util.function.Supplier;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.results.persist.HearingResultedDocumentRepository;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;
import uk.gov.moj.cpp.results.query.view.response.DefendantView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummaryView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingService.class);

    @Inject
    private HearingResultedDocumentRepository hearingResultedDocumentRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;


    private HearingResultSummaryView createSummary(final Hearing hearing, final ProsecutionCase prosecutionCase, final Defendant defendant) {
        // assume that it is a person defendant otherwise query api requires change
        final PersonDefendant personDefendant = defendant.getPersonDefendant();
        String firstName = null;
        String lastName = null;
        if (personDefendant != null && personDefendant.getPersonDetails() != null) {
            firstName = personDefendant.getPersonDetails().getFirstName();
            lastName = personDefendant.getPersonDetails().getLastName();
        }
        final DefendantView defendantView = new DefendantView(defendant.getId(), firstName, lastName);

        final String urn = prosecutionCase.getProsecutionCaseIdentifier().getCaseURN();

        final String hearingTypeDescription = Objects.nonNull(hearing.getType()) ? hearing.getType().getDescription() : null;

        if (isNull(urn)) {
            final String prosecutionAuthorityReference = prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
            return new HearingResultSummaryView(hearing.getId(), hearingTypeDescription,
                    hearing.getHearingDays().get(0).getSittingDay().toLocalDate(), asList(prosecutionAuthorityReference), defendantView, hearing.getCourtCentre().getId());
        }

        return new HearingResultSummaryView(hearing.getId(), hearingTypeDescription,
                hearing.getHearingDays().get(0).getSittingDay().toLocalDate(), asList(urn), defendantView, hearing.getCourtCentre().getId());
    }

    public HearingResultSummariesView findHearingResultSummariesFromDate(final LocalDate fromDate) {
        final List<HearingResultedDocument> documents = hearingResultedDocumentRepository.findByFromDate(fromDate);
        final List<HearingResultSummaryView> hearingResultSummaryViews = new ArrayList<>();
        //find the case and defendant - defendants are contained by 1 case only
        documents.forEach(
                document -> {
                    final JsonObject jsonPayload = stringToJsonObjectConverter.convert(document.getPayload());
                    final HearingResultsAdded hearingResultsAdded = jsonObjectToObjectConverter.convert(jsonPayload, HearingResultsAdded.class);
                    final Hearing hearing = hearingResultsAdded.getHearing();
                    if (CollectionUtils.isNotEmpty(hearing.getProsecutionCases())) {
                        for (final ProsecutionCase prosecutionCase : hearing.getProsecutionCases()) {
                            for (final Defendant defendant : prosecutionCase.getDefendants()) {
                                final HearingResultSummaryView summary = createSummary(hearing, prosecutionCase, defendant);
                                hearingResultSummaryViews.add(summary);
                            }
                        }
                    }
                }
        );
        return new HearingResultSummariesView(hearingResultSummaryViews);
    }

    public HearingResultsAdded findHearingDetailsByHearingIdDefendantId(final UUID hearingId, final UUID defendantId) {

        final HearingResultsAdded hearingResultsAdded = findHearingForHearingId(hearingId);
        Hearing hearing = hearingResultsAdded.getHearing();
        ProsecutionCase foundProsecutionCase = null;
        Defendant foundDefendant = null;
        for (final ProsecutionCase prosecutionCase : hearing.getProsecutionCases()) {
            for (final Defendant defendant : prosecutionCase.getDefendants()) {
                if (defendant.getId().equals(defendantId)) {
                    foundProsecutionCase = prosecutionCase;
                    foundDefendant = defendant;
                    break;
                }
            }
            if (foundProsecutionCase != null) {
                break;
            }
        }
        if (foundProsecutionCase == null) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("findHearingDetailsByHearingIdDefendantId can't find defendant %s for hearing %s in payload", defendantId, hearingId));
            }
            return null;
        }
        foundProsecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(foundProsecutionCase).withDefendants(asList(foundDefendant)).build();
        hearing = Hearing.hearing().withValuesFrom(hearingResultsAdded.getHearing()).withProsecutionCases(asList(foundProsecutionCase)).build();
        return new HearingResultsAdded(hearing, hearingResultsAdded.getSharedTime());
    }

    public HearingResultsAdded findHearingForHearingId(final UUID hearingId ){
        return findHearing(() -> hearingResultedDocumentRepository.findByHearingIdAndLatestHearingDay(hearingId),
                () -> String.format("findHearingForHearingId cant find hearing %s ", hearingId));
    }

    public HearingResultsAdded findHearingForHearingIdAndHearingDate(final UUID hearingId, final LocalDate hearingDate ){
        return findHearing(() -> hearingResultedDocumentRepository.findByHearingIdAndHearingDay(hearingId, hearingDate),
                () -> String.format("findHearingForHearingIdAndHearingDate cant find hearing %s and date %s ", hearingId, hearingDate));
    }

    private HearingResultsAdded findHearing(Supplier<HearingResultedDocument> documentFinder, Supplier<String> getLogString){
        final HearingResultedDocument document = documentFinder.get();
        if (document == null) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(getLogString.get());
            }
            return null;
        }
        final JsonObject jsonPayload = stringToJsonObjectConverter.convert(document.getPayload());
        return jsonObjectToObjectConverter.convert(jsonPayload, HearingResultsAdded.class);
    }

}

