package uk.gov.moj.cpp.results.event.helper;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.event.PublishToDcs;
import uk.gov.moj.cpp.results.event.service.DcsCreateCaseRequest;
import uk.gov.moj.cpp.results.event.service.DcsDefendant;
import uk.gov.moj.cpp.results.event.service.DcsHearing;
import uk.gov.moj.cpp.results.event.service.DcsOffence;
import uk.gov.moj.cpp.results.event.service.DcsService;
import uk.gov.moj.cpp.results.event.service.DefendantOrganisation;
import uk.gov.moj.cpp.results.event.service.DefendantPerson;
import uk.gov.moj.cpp.results.event.service.OffenceDetails;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DcsCaseHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DcsCaseHelper.class);
    public static final String DCS_UNKNOWN = "UNKNOWN";
    public static final String STAGING_DCS = "StagingDcs";
    public static final String ADDED_OFFENCES = "addedOffences";
    public static final String DELETED_OFFENCES = "deletedOffences";

    @Inject
    private DcsService dcsService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private FeatureControlGuard featureControlGuard;

    public void prepareAndSendToDCSIfEligible(final JsonEnvelope publishToDcsEvent) {

        if (!featureControlGuard.isFeatureEnabled(STAGING_DCS)) {
            LOGGER.info("Feature StagingDcs is not enabled hence DCS request will not be initiated");
            return;
        }

        final PublishToDcs publishToDcs = getPublishToDcs(publishToDcsEvent);
        final Hearing hearing = publishToDcs.getCurrentHearing();

        if(isNotEmpty(hearing.getProsecutionCases())) {
            hearing.getProsecutionCases()
                    .forEach(prosecutionCase -> sendToDcs(publishToDcsEvent, prosecutionCase));
        }
    }

    private void sendToDcs(final JsonEnvelope publishToDcsEvent, final ProsecutionCase prosecutionCase) {

        final PublishToDcs publishToDcs = getPublishToDcs(publishToDcsEvent);
        List<DcsDefendant> defendantList = prosecutionCase.getDefendants().stream()
                .map(defendant -> buildDcsDefendant(defendant, publishToDcs.getPreviousHearing()))
                .filter(this::isDefendantAddedOrDeletedOffencesNotEmpty)
                .toList();

        if (!defendantList.isEmpty()) {
            final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
            final DcsCreateCaseRequest request = DcsCreateCaseRequest.DcsCreateCaseRequestBuilder
                    .aDcsCreateCaseRequest()
                    .withCaseId(prosecutionCase.getId())
                    .withCaseUrn(prosecutionCaseIdentifier != null ? getCaseUrn(prosecutionCaseIdentifier) : null)
                    .withProsecutionAuthority(getProsecutorAuthorityCode(prosecutionCase))
                    .withDefendants(defendantList)
                    .build();
            dcsService.createCase(request, publishToDcsEvent);
        } else {
            LOGGER.info("Prosecution case with id {} is either not eligible or already has DCS entry", prosecutionCase.getId());
        }
    }

    private DcsDefendant buildDcsDefendant(Defendant defendant, final Hearing previousHearing) {

        DefendantPerson defendantPerson = null;
        String bailStatus = null;
        String interpreterLanguageNeeds = null;

        final PersonDefendant personDefendant = defendant.getPersonDefendant();
        if(personDefendant != null) {
            final Person personDetails = personDefendant.getPersonDetails();
            if(personDetails != null) {
                defendantPerson = getDefendantPerson(personDetails);
                interpreterLanguageNeeds = personDetails.getInterpreterLanguageNeeds();
            }
            bailStatus = (personDefendant.getBailStatus() != null) ? personDefendant.getBailStatus().getDescription() : null;
        }

        List<PreviousResult> previousResults = getPreviousResults(defendant, previousHearing);
        final Map<String, Set<DcsOffence>> offenceMap = processOffences(defendant, previousResults);

        return getDcsDefendant(defendant, bailStatus, interpreterLanguageNeeds, defendantPerson, getDefendantOrganisation(defendant), offenceMap);
    }

    private DefendantOrganisation getDefendantOrganisation(final Defendant defendant) {

        DefendantOrganisation defendantOrganisation = null;
        final LegalEntityDefendant legalEntityDefendant = defendant.getLegalEntityDefendant();
        if(legalEntityDefendant != null) {
            final Organisation organisation = legalEntityDefendant.getOrganisation();
            defendantOrganisation = DefendantOrganisation.DefendantOrganisationBuilder.aDefendantOrganisation()
                    .withName(nonNull(organisation) ? organisation.getName() : null)
                    .build();
        }
        return defendantOrganisation;
    }

    private DcsDefendant getDcsDefendant(final Defendant defendant,
                                                  final String bailStatus,
                                                  final String interpreterLanguageNeeds,
                                                  final DefendantPerson defendantPerson,
                                                  final DefendantOrganisation defendantOrganisation,
                                                  final Map<String, Set<DcsOffence>> offenceMap) {

        List<DcsHearing> hearings = buildHearingList(defendant);
        return DcsDefendant.DcsDefendantBuilder
                .aDcsDefendant()
                .withId(defendant.getId())
                .withBailStatus(bailStatus)
                .withInterpreterInformation(interpreterLanguageNeeds)
                .withDefendantPerson(defendantPerson)
                .withDefendantOrganisation(defendantOrganisation)
                .withOffenceDetails(OffenceDetails.Builder.newOffence()
                        .withAddedOffences(offenceMap.get(ADDED_OFFENCES))
                        .withRemovedOffences(offenceMap.get(DELETED_OFFENCES))
                        .build())
                .withHearings(hearings)
                .build();
    }

    private DefendantPerson getDefendantPerson(final Person personDetails) {
        DefendantPerson defendantPerson;
        defendantPerson = DefendantPerson.DefendantPersonBuilder.aDefendantPerson()
                .withSurname(personDetails.getLastName())
                .withForename(personDetails.getFirstName())
                .withMiddleName(personDetails.getMiddleName())
                .withDateOfBirth(personDetails.getDateOfBirth())
                .build();
        return defendantPerson;
    }

    private List<DcsHearing> buildHearingList(final Defendant defendant) {
        Set<Pair<String, LocalDate>> hearings = new HashSet<>();

        defendant.getOffences().stream()
                .flatMap(offence -> Optional.ofNullable(offence.getJudicialResults()).stream()
                        .flatMap(Collection::stream))
                .filter(this::isQualifyingResultForDCS)
                .map(JudicialResult::getNextHearing)
                .filter(Objects::nonNull)
                .forEach(nextHearing -> {
                    String courtCentreName = Optional.ofNullable(nextHearing.getCourtCentre())
                            .map(CourtCentre::getName)
                            .orElse(null);
                    ZonedDateTime listedStartDateTime = nextHearing.getListedStartDateTime();
                    LocalDate hearingDate = listedStartDateTime != null ? listedStartDateTime.withZoneSameInstant(ZoneId.of("Europe/London")).toLocalDate() : null;
                    hearings.add(Pair.of(courtCentreName, hearingDate));
                });

        return hearings.stream()
                .map(pair -> DcsHearing.Builder.newHearing()
                        .withCourtCentre(pair.getLeft())
                        .withHearingDate(pair.getRight())
                        .build())
                .toList();
    }

    private boolean isDefendantAddedOrDeletedOffencesNotEmpty(final DcsDefendant dcsDefendant) {
        final OffenceDetails offencesDetails = dcsDefendant.getOffencesDetails();
        return nonNull(offencesDetails) && (isNotEmpty(offencesDetails.getAddedOffences()) || isNotEmpty(offencesDetails.getRemovedOffences()));
    }

    private Map<String, Set<DcsOffence>> processOffences(final Defendant defendant, final List<PreviousResult> previousResults) {

        Set<DcsOffence> addedOffences = new HashSet<>();
        Set<DcsOffence> deletedOffences = new HashSet<>();

        for (Offence offence : defendant.getOffences()) {

            List<PreviousResult> previousResultsByOffenceId = getPreviousResultByOffenceId(previousResults, offence.getId());

            boolean isPreviousResultQualifying = previousResultsByOffenceId.stream().anyMatch(PreviousResult::isQualifying);
            List<JudicialResult> judicialResults = !isEmpty(offence.getJudicialResults()) ? getJudicialResults(offence) : Collections.emptyList();

            boolean isAnyCurrentJudicialResultQualified =  judicialResults.stream().anyMatch(this::isQualifyingResultForDCS);

            if (!isPreviousResultQualifying && isAnyCurrentJudicialResultQualified) {
                final DcsOffence dcsOffence = createDcsOffence(offence);
                addedOffences.add(dcsOffence);
            }

            if (isPreviousResultQualifying && !isAnyCurrentJudicialResultQualified) {
                final DcsOffence dcsOffence = createDcsOffence(offence);
                deletedOffences.add(dcsOffence);
            }
        }

        Map<String, Set<DcsOffence>> extractedOffences = new HashMap<>();
        extractedOffences.put(ADDED_OFFENCES, addedOffences);
        extractedOffences.put(DELETED_OFFENCES, deletedOffences);
        return extractedOffences;
    }

    private List<PreviousResult> getPreviousResults(final Defendant defendant, final Hearing previousHearing) {

        List<PreviousResult> previousResults = new ArrayList<>();
        if(previousHearing != null) {
            previousResults = previousHearing.getProsecutionCases().stream()
                    .map(ProsecutionCase::getDefendants)
                    .flatMap(Collection::stream)
                    .filter(defendantObject -> defendantObject.getId().equals(defendant.getId()))
                    .flatMap(defendantObject -> defendantObject.getOffences().stream())
                    .filter(offenceDetails -> defendant.getOffences().stream()
                            .anyMatch(offence -> offence.getId().equals(offenceDetails.getId())))
                    .filter(offence -> isNotEmpty(offence.getJudicialResults()))
                    .flatMap(offenceDetails -> offenceDetails.getJudicialResults().stream()
                            .map(judicialResultDetails -> getPreviousResult(offenceDetails, judicialResultDetails)))
                    .toList();
        }
        return previousResults;
    }

    private static List<JudicialResult> getJudicialResults(final Offence offence) {
        List<JudicialResult> list = new ArrayList<>();
        for (JudicialResult result : offence.getJudicialResults()) {
            final UUID rootJudicialResultTypeId = result.getRootJudicialResultTypeId();
            if (rootJudicialResultTypeId == null || result.getJudicialResultTypeId().equals(rootJudicialResultTypeId)) {
                list.add(result);
            }
        }
        return list;
    }

    public List<PreviousResult> getPreviousResultByOffenceId(List<PreviousResult> previousResults, UUID offenceId) {
        return previousResults.stream().filter(previousResult -> previousResult.getOffenceId().equals(offenceId)).toList();
    }

    private PreviousResult getPreviousResult(final Offence offenceDetails, final JudicialResult judicialResult) {
        return new PreviousResult(
                judicialResult.getJudicialResultTypeId(),
                isQualifyingResultForDCS(judicialResult),
                offenceDetails.getId(),
                offenceDetails.getOffenceCode());
    }

    private DcsOffence createDcsOffence(Offence offence) {
        return DcsOffence.Builder.newOffence()
                .withOffenceId(offence.getId())
                .withOffenceCode(offence.getOffenceCode())
                .build();
    }

    private String getCaseUrn(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return isNotBlank(prosecutionCaseIdentifier.getCaseURN()) ?
                prosecutionCaseIdentifier.getCaseURN() : prosecutionCaseIdentifier.getProsecutionAuthorityReference();
    }

    private String getProsecutorAuthorityCode(final ProsecutionCase prosecutionCase) {
        if (nonNull(prosecutionCase.getProsecutor())) {
            return prosecutionCase.getProsecutor().getProsecutorCode();
        }

        if (nonNull(prosecutionCase.getProsecutionCaseIdentifier())) {
            return prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode();
        }

        return DCS_UNKNOWN;
    }

    private PublishToDcs getPublishToDcs(final JsonEnvelope publishToDcsEvent) {
        return jsonObjectToObjectConverter.convert(publishToDcsEvent.payloadAsJsonObject(), PublishToDcs.class);
    }

    private boolean isQualifyingResultForDCS(final JudicialResult judicialResult) {
        return requireNonNullElse(judicialResult.getCommittedToCC(), false) || requireNonNullElse(judicialResult.getSentToCC(), false);
    }
}
