package uk.gov.moj.cpp.results.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SharedHearing;
import uk.gov.justice.core.courts.SharedResultLine;
import uk.gov.justice.core.courts.SharedVariant;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;
import uk.gov.moj.cpp.results.persist.HearingResultedDocumentRepository;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;
import uk.gov.moj.cpp.results.query.view.response.DefendantView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummaryView;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private HearingResultSummaryView createSummary(final SharedHearing hearing, final ProsecutionCase prosecutionCase, final Defendant defendant) {
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

        if(isNull(urn)) {
            final String prosecutionAuthorityReference = prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
            return new HearingResultSummaryView(hearing.getId(), hearing.getType().getDescription(),
                    hearing.getHearingDays().get(0).getSittingDay().toLocalDate(), asList(prosecutionAuthorityReference), defendantView, hearing.getCourtCentre().getId());
        }
        
        return new HearingResultSummaryView(hearing.getId(), hearing.getType().getDescription(),
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
                    final SharedHearing hearing = hearingResultsAdded.getHearing();
                    for (final ProsecutionCase prosecutionCase : hearing.getProsecutionCases()) {
                        for (final Defendant defendant : prosecutionCase.getDefendants()) {
                            final HearingResultSummaryView summary = createSummary(hearing, prosecutionCase, defendant);
                            hearingResultSummaryViews.add(summary);
                        }
                    }
                }
        );
        return new HearingResultSummariesView(hearingResultSummaryViews);
    }

    public HearingResultsAdded findHearingDetailsByHearingIdDefendantId(final UUID hearingId, final UUID defendantId) {
        final HearingResultedDocument document = hearingResultedDocumentRepository.findBy(hearingId);
        if (document == null) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("findHearingDetailsByHearingIdDefendantId cant find hearing %s ", hearingId));
            }
            return null;
        }
        final JsonObject jsonPayload = stringToJsonObjectConverter.convert(document.getPayload());
        final HearingResultsAdded hearingResultsAdded = jsonObjectToObjectConverter.convert(jsonPayload, HearingResultsAdded.class);
        final SharedHearing hearing = hearingResultsAdded.getHearing();
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
                LOGGER.error(String.format("findHearingDetailsByHearingIdDefendantId cant find defendant %s for hearing %s in payload %s ", defendantId, hearingId, document.getPayload()));
            }
            return null;
        }
        foundProsecutionCase.setDefendants(asList(foundDefendant));
        hearingResultsAdded.getHearing().setProsecutionCases(asList(foundProsecutionCase));
        final List<SharedVariant> filteredVariants =
                hearingResultsAdded.getVariants() == null ? Collections.emptyList() :
                        hearingResultsAdded.getVariants().stream().filter(v -> v.getKey().getDefendantId().equals(defendantId)).collect(Collectors.toList());
        final List<SharedResultLine> filteredSharedResultLines = hearing.getSharedResultLines().stream().filter(rl -> rl.getDefendantId().equals(defendantId)).collect(Collectors.toList());
        hearing.setSharedResultLines(filteredSharedResultLines);
        return new HearingResultsAdded(hearing, hearingResultsAdded.getSharedTime(), filteredVariants);
    }


}

