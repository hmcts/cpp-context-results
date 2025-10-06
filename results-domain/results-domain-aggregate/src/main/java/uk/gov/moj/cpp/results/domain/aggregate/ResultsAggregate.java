package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.builder;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.core.courts.CaseAddedEvent.caseAddedEvent;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.CourtCentreWithLJA.courtCentreWithLJA;
import static uk.gov.justice.core.courts.DefendantAddedEvent.defendantAddedEvent;
import static uk.gov.justice.core.courts.DefendantRejectedEvent.defendantRejectedEvent;
import static uk.gov.justice.core.courts.DefendantUpdatedEvent.defendantUpdatedEvent;
import static uk.gov.justice.core.courts.LjaDetails.ljaDetails;
import static uk.gov.justice.core.courts.PoliceResultGenerated.policeResultGenerated;
import static uk.gov.justice.core.courts.SessionAddedEvent.sessionAddedEvent;
import static uk.gov.justice.core.courts.SjpCaseRejectedEvent.sjpCaseRejectedEvent;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.domains.results.structure.CorporateDefendant.corporateDefendant;
import static uk.gov.moj.cpp.domains.results.structure.Offence.offence;
import static uk.gov.moj.cpp.domains.results.structure.Person.person;
import static uk.gov.moj.cpp.domains.results.structure.Result.result;
import static uk.gov.moj.cpp.results.domain.aggregate.ResultReshareHelper.hasResults;
import static uk.gov.moj.cpp.results.domain.event.AppealUpdateNotificationRequested.appealUpdateNotificationRequested;

import com.google.common.base.Functions;
import org.apache.commons.collections.map.HashedMap;
import uk.gov.justice.core.courts.AssociatedIndividual;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.CaseAddedEvent;
import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtCentreWithLJA;
import uk.gov.justice.core.courts.DefendantAddedEvent;
import uk.gov.justice.core.courts.DefendantUpdatedEvent;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationEjected;
import uk.gov.justice.core.courts.HearingCaseEjected;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.core.courts.HearingResultsAddedForDay;
import uk.gov.justice.core.courts.Individual;
import uk.gov.justice.core.courts.IndividualDefendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.OffenceDetails;
import uk.gov.justice.core.courts.OrganisationDetails;
import uk.gov.justice.core.courts.PoliceResultGenerated;
import uk.gov.justice.core.courts.SessionAddedEvent;
import uk.gov.justice.core.courts.SessionDay;
import uk.gov.justice.core.courts.YouthCourt;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.domains.ResultAmendmentDetailsHelper;
import uk.gov.moj.cpp.domains.resultdetails.CaseResultDetails;
import uk.gov.moj.cpp.domains.results.structure.AttendanceDay;
import uk.gov.moj.cpp.domains.results.structure.Case;
import uk.gov.moj.cpp.domains.results.structure.Defendant;
import uk.gov.moj.cpp.domains.results.structure.Offence;
import uk.gov.moj.cpp.domains.results.structure.CivilOffence;
import uk.gov.moj.cpp.domains.results.structure.Result;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.event.EmailNotificationFailed;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequestedV2;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class ResultsAggregate implements Aggregate {

    public static final String AMEND_RESHARE = "Amend & Reshare";
    private static final String CASE_ID = "caseId";
    private static final String APPLICATION_ID = "applicationId";
    private static final String INVALID_EMAIL_ID = "Police Email Address is not Available";
    private static final long serialVersionUID = 106L;
    private final Set<UUID> hearingIds = new HashSet<>();
    private final List<Case> cases = new ArrayList<>();
    private UUID id;
    private CourtCentreWithLJA courtCentreWithLJA;
    private List<SessionDay> sessionDays = new ArrayList<>();
    private List<CaseDefendant> defendants = new ArrayList<>();

    private YouthCourt youthCourt;
    private List<UUID> youthCourtDefendantIds;
    private Map<UUID, CaseResultDetails> caseResultAmendmentDetailsList = new HashedMap();
    private Hearing hearing;
    private ZonedDateTime sharedDate;


    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(SessionAddedEvent.class).apply(this::storeSessionAddedEvent),
                when(CaseAddedEvent.class).apply(this::storeCaseAddedEvent),
                when(DefendantAddedEvent.class).apply(this::storeDefendantAddedEvent),
                when(DefendantUpdatedEvent.class).apply(this::storeDefendantUpdatedEvent),
                when(HearingResultsAddedForDay.class).apply(this::handleHearingResultsAddedForDay),
                when(HearingResultsAdded.class).apply(this::handleHearingResultsAdded),
                otherwiseDoNothing());
    }

    private void handleHearingResultsAddedForDay(final HearingResultsAddedForDay hearingResultsAddedForDay) {
        this.hearing = hearingResultsAddedForDay.getHearing();
        this.sharedDate = hearingResultsAddedForDay.getSharedTime();
        this.hearingIds.add(hearing.getId());

        final List<CaseResultDetails> newCaseResultDetailsList = ResultAmendmentDetailsHelper.buildHearingResultDetails(hearing, caseResultAmendmentDetailsList);
        this.caseResultAmendmentDetailsList = newCaseResultDetailsList.stream()
                .collect(toMap(CaseResultDetails::getCaseId, Functions.identity()));
    }

    private void handleHearingResultsAdded(final HearingResultsAdded hearingResultsAdded) {
        final Hearing resultedHearing = hearingResultsAdded.getHearing();
        this.hearingIds.add(resultedHearing.getId());

        this.youthCourt = resultedHearing.getYouthCourt();
        this.youthCourtDefendantIds = new ArrayList<>(); // interested in the latest event
        ofNullable(resultedHearing.getYouthCourtDefendantIds())
                .ifPresent(e -> youthCourtDefendantIds.addAll(e));

        final List<CaseResultDetails> newCaseResultDetailsList = ResultAmendmentDetailsHelper.buildHearingResultDetails(resultedHearing, caseResultAmendmentDetailsList);
        this.caseResultAmendmentDetailsList = newCaseResultDetailsList.stream()
                .collect(toMap(CaseResultDetails::getCaseId, Functions.identity()));
    }

    private void storeDefendantUpdatedEvent(final DefendantUpdatedEvent defendantUpdatedEvent) {
        final CaseDefendant defendantFromEvent = defendantUpdatedEvent.getDefendant();
        final Optional<Defendant> defendantFromAggregateOptional = this.cases.stream().filter(c -> defendantUpdatedEvent.getCaseId().equals(c.getCaseId())).map(Case::getDefendants)
                .flatMap(List::stream).filter(d -> d.getId().equals(defendantFromEvent.getDefendantId())).findFirst();
        if (defendantFromAggregateOptional.isPresent()) {
            final Defendant defendantFromAggregate = defendantFromAggregateOptional.get();
            final List<OffenceDetails> offencesFromEvent = defendantFromEvent.getOffences();
            storeOffence(defendantFromAggregate, offencesFromEvent);
        }
    }

    private void storeOffence(final Defendant defendantFromAggregate, final List<OffenceDetails> offencesFromEvent) {
        for (final OffenceDetails offenceFromEvent : offencesFromEvent) {
            final Optional<Offence> offenceOptional = defendantFromAggregate.getOffences().stream().filter(o -> o.getId().equals(offenceFromEvent.getId())).findFirst();
            if (offenceOptional.isPresent()) {
                defendantFromAggregate.getOffences().remove(offenceOptional.get());
                defendantFromAggregate.getOffences().add(buildOffence(offenceFromEvent));
            } else {
                defendantFromAggregate.getOffences().add(buildOffence(offenceFromEvent));
            }
        }
    }

    public Stream<Object> saveHearingResults(final PublicHearingResulted payload) {
        return apply(Stream.of(new HearingResultsAdded(payload.getHearing(), payload.getSharedTime())));
    }

    public Stream<Object> saveHearingResultsForDay(final PublicHearingResulted payload, final LocalDate hearingDay) {
        Boolean isReshare = Boolean.FALSE;
        final Optional<Boolean> isReshareFromPayload = payload.getIsReshare();
        if (nonNull(isReshareFromPayload) && isReshareFromPayload.isPresent()) {
            isReshare = isReshareFromPayload.get();
        }
        return apply(Stream.of(new HearingResultsAddedForDay(payload.getDeletedJudicialResults(), payload.getHearing(), hearingDay,
                isReshare, payload.getSharedTime())));
    }

    public Stream<Object> ejectCaseOrApplication(final UUID hearingId, final JsonObject payload) {
        if (hearingIds.contains(hearingId)) {
            if (payload.containsKey(CASE_ID)) {
                return apply(Stream.of(new HearingCaseEjected(fromString(payload.getString(CASE_ID)), hearingId)));
            } else {
                return apply(Stream.of(new HearingApplicationEjected(fromString(payload.getString(APPLICATION_ID)), hearingId)));
            }
        }
        return empty();
    }

    private void storeDefendantAddedEvent(final DefendantAddedEvent defendantAddedEvent) {
        final Optional<Case> aCaseOption = this.cases.stream().filter(c -> defendantAddedEvent.getCaseId().equals(c.getCaseId())).findFirst();
        if (aCaseOption.isPresent()) {
            final CaseDefendant defendantFromEvent = defendantAddedEvent.getDefendant();
            final Defendant defendant = new Defendant(defendantFromEvent.getDefendantId());
            defendant.setProsecutorReference(defendantFromEvent.getProsecutorReference());
            defendant.setPncId(defendantFromEvent.getPncId());
            final IndividualDefendant individualDefendant = defendantFromEvent.getIndividualDefendant();
            addIndividualDefendant(defendant, individualDefendant);

            addCorporateDefendant(defendantFromEvent, defendant);

            addAssociatedPerson(defendantFromEvent, defendant);

            final List<uk.gov.justice.core.courts.JudicialResult> judicialResultsFromEvent = defendantFromEvent.getJudicialResults();

            addJudicialResult(defendant, judicialResultsFromEvent);
            final List<OffenceDetails> offencesFromEvent = defendantFromEvent.getOffences();
            addOffences(defendant, offencesFromEvent);
            addAttendanceDay(defendantFromEvent, defendant);
            aCaseOption.get().getDefendants().add(defendant);
        }
    }

    private void addAttendanceDay(final CaseDefendant defendantFromEvent, final Defendant defendant) {
        final List<uk.gov.justice.core.courts.AttendanceDay> attendanceDaysFromEvent = defendantFromEvent.getAttendanceDays();
        if (null != attendanceDaysFromEvent) {
            for (final uk.gov.justice.core.courts.AttendanceDay attendanceDay : attendanceDaysFromEvent) {
                defendant.getAttendanceDays().add(new AttendanceDay(attendanceDay.getDay(), (attendanceDay.getAttendanceType() != AttendanceType.NOT_PRESENT)));
            }
        }
    }

    private void addOffences(final Defendant defendant, final List<OffenceDetails> offencesFromEvent) {
        if (null != offencesFromEvent) {
            for (final OffenceDetails offenceFromEvent : offencesFromEvent) {
                defendant.getOffences().add(buildOffence(offenceFromEvent));
            }
        }
    }

    private void addJudicialResult(final Defendant defendant, final List<JudicialResult> judicialResultsFromEvent) {
        if (null != judicialResultsFromEvent) {
            for (final JudicialResult judicialResultFromEvent : judicialResultsFromEvent) {
                defendant.getResults().add(buildJudicialResult(judicialResultFromEvent));
            }
        }
    }

    private void addAssociatedPerson(final CaseDefendant defendantFromEvent, final Defendant defendant) {
        final List<AssociatedIndividual> associatedPerson = defendantFromEvent.getAssociatedPerson();
        if (null != associatedPerson) {
            defendant.setAssociatedIndividuals(associatedPerson);
        }
    }

    private void addCorporateDefendant(final CaseDefendant defendantFromEvent, final Defendant defendant) {
        final OrganisationDetails corporateDefendant = defendantFromEvent.getCorporateDefendant();
        if (null != corporateDefendant) {
            defendant.setCorporateDefendant(corporateDefendant()
                    .withIncorporationNumber(corporateDefendant.getIncorporationNumber())
                    .withAddress(corporateDefendant.getAddress())
                    .withContact(corporateDefendant.getContact())
                    .withName(corporateDefendant.getName())
                    .build());
            defendant.setPresentAtHearing(corporateDefendant.getPresentAtHearing());
        }
    }

    private void addIndividualDefendant(final Defendant defendant, final IndividualDefendant individualDefendant) {
        if (null != individualDefendant) {
            defendant.setBailStatus(individualDefendant.getBailStatus());
            defendant.setBailCondition(individualDefendant.getBailConditions());
            defendant.setReasonForBailConditionsOrCustody(individualDefendant.getReasonForBailConditionsOrCustody());

            final Individual individual = individualDefendant.getPerson();
            defendant.setPerson(person()
                    .withTitle(individual.getTitle())
                    .withFirstName(individual.getFirstName())
                    .withMiddleName(individual.getMiddleName())
                    .withLastName(individual.getLastName())
                    .withNationality(individual.getNationality())
                    .withGender(individual.getGender())
                    .withDateOfBirth(individual.getDateOfBirth())
                    .withContact(individual.getContact())
                    .withAddress(individual.getAddress())
                    .build());
            defendant.setPresentAtHearing(individualDefendant.getPresentAtHearing());
        }
    }

    private Offence buildOffence(final OffenceDetails offenceFromEvent) {

        return offence()
                .withId(offenceFromEvent.getId())
                .withOffenceCode(offenceFromEvent.getOffenceCode())
                .withOrderSequenceNumber(offenceFromEvent.getOffenceSequenceNumber())
                .withWording(offenceFromEvent.getWording())
                .withOffenceDateCode(offenceFromEvent.getOffenceDateCode())
                .withStartDate(offenceFromEvent.getStartDate())
                .withEndDate(offenceFromEvent.getEndDate())
                .withArrestDate(offenceFromEvent.getArrestDate())
                .withChargeDate(offenceFromEvent.getChargeDate())
                .withOffenceFacts(offenceFromEvent.getOffenceFacts())
                .withPlea(offenceFromEvent.getPlea())
                .withAllocationDecision(offenceFromEvent.getAllocationDecision())
                .withModeOfTrial(offenceFromEvent.getModeOfTrial())
                .withConvictingCourt(offenceFromEvent.getConvictingCourt())
                .withFinding(offenceFromEvent.getFinding())
                .withConvictionDate(offenceFromEvent.getConvictionDate())
                .withFinalDisposal(offenceFromEvent.getFinalDisposal())
                .withJudicialResults(buildJudicialResults(offenceFromEvent.getJudicialResults()))
                .withCivilOffence(buildCivilOffence(offenceFromEvent.getCivilOffence()))
                .build();
    }

    private List<Result> buildJudicialResults(final List<uk.gov.justice.core.courts.JudicialResult> judicialResults) {
        final List<Result> results = new ArrayList<>();
        if (isNotNullOrEmpty(judicialResults)) {
            for (final uk.gov.justice.core.courts.JudicialResult judicialResultFromEvent : judicialResults) {
                results.add(buildJudicialResult(judicialResultFromEvent));
            }
        }
        return results;
    }

    private CivilOffence buildCivilOffence(final uk.gov.justice.core.courts.CivilOffence civilOffence) {
        if (nonNull(civilOffence)) {
            return CivilOffence.civilOffence()
                    .withIsExParte(civilOffence.getIsExParte())
                    .withIsRespondent(civilOffence.getIsRespondent())
                    .build();
        }
        return null;
    }

    private Result buildJudicialResult(final uk.gov.justice.core.courts.JudicialResult judicialResultFromEvent) {
        return result()
                .withResultId(judicialResultFromEvent.getJudicialResultId())
                .withAmendmentDate(judicialResultFromEvent.getAmendmentDate())
                .withAmendmentReason(judicialResultFromEvent.getAmendmentReason())
                .withApprovedDate(judicialResultFromEvent.getApprovedDate())
                .withCjsCode(judicialResultFromEvent.getCjsCode())
                .withCategory(judicialResultFromEvent.getCategory())
                .withCourtClerk(judicialResultFromEvent.getCourtClerk())
                .withDelegatedPowers(judicialResultFromEvent.getDelegatedPowers())
                .withFourEyesApproval(judicialResultFromEvent.getFourEyesApproval())
                .withIsAdjournmentResult(judicialResultFromEvent.getIsAdjournmentResult())
                .withIsAvailableForCourtExtract(judicialResultFromEvent.getIsAvailableForCourtExtract())
                .withIsConvictedResult(judicialResultFromEvent.getIsConvictedResult())
                .withIsFinancialResult(judicialResultFromEvent.getIsFinancialResult())
                .withJudicialResultPrompts(judicialResultFromEvent.getJudicialResultPrompts())
                .withLabel(judicialResultFromEvent.getLabel())
                .withLastSharedDateTime(judicialResultFromEvent.getLastSharedDateTime())
                .withOrderedDate(judicialResultFromEvent.getOrderedDate())
                .withOrderedHearingId(judicialResultFromEvent.getOrderedHearingId())
                .withRank(judicialResultFromEvent.getRank())
                .withUsergroups(judicialResultFromEvent.getUsergroups())
                .withWelshLabel(judicialResultFromEvent.getWelshLabel())
                .withIsDeleted(judicialResultFromEvent.getIsDeleted())
                .withLifeDuration(judicialResultFromEvent.getLifeDuration())
                .withResultText(judicialResultFromEvent.getResultText())
                .withTerminatesOffenceProceedings(judicialResultFromEvent.getTerminatesOffenceProceedings())
                .withPublishedAsAPrompt(judicialResultFromEvent.getPublishedAsAPrompt())
                .withExcludedFromResults(judicialResultFromEvent.getExcludedFromResults())
                .withAlwaysPublished(judicialResultFromEvent.getAlwaysPublished())
                .withUrgent(judicialResultFromEvent.getUrgent())
                .withD20(judicialResultFromEvent.getD20())
                .withJudicialResultTypeId(judicialResultFromEvent.getJudicialResultTypeId())
                .withPublishedForNows(judicialResultFromEvent.getPublishedForNows())
                .withRollUpPrompts(judicialResultFromEvent.getRollUpPrompts())
                .build();
    }

    private void storeCaseAddedEvent(final CaseAddedEvent caseAddedEvent) {
        this.cases.add(new Case(caseAddedEvent.getCaseId(), caseAddedEvent.getUrn(), caseAddedEvent.getProsecutionAuthorityCode(),
                caseAddedEvent.getIsCivil(), caseAddedEvent.getGroupId(), caseAddedEvent.getIsGroupMember(), caseAddedEvent.getIsGroupMaster()));
    }

    private void storeSessionAddedEvent(final SessionAddedEvent sessionAddedEvent) {
        this.id = sessionAddedEvent.getId();
        this.courtCentreWithLJA = sessionAddedEvent.getCourtCentreWithLJA();
        this.sessionDays = sessionAddedEvent.getSessionDays();
    }

    public Stream<Object> handleSession(final UUID id, final CourtCentreWithLJA courtCentreWithLJA, final List<uk.gov.justice.core.courts.SessionDay> sessionDays) {
        if (id != null && this.sessionDays != null && !this.sessionDays.containsAll(sessionDays)) {
            return apply(of(sessionAddedEvent().withId(id).withCourtCentreWithLJA(courtCentreWithLJA).withSessionDays(sessionDays).build()));
        }
        return apply(builder().build());
    }

    public Stream<Object> handleCase(final CaseDetails caseFromRequest) {
        final Stream.Builder<Object> builder = builder();
        final Optional<Case> aCaseAggregateOption = this.cases.stream().filter(c -> caseFromRequest.getCaseId().equals(c.getCaseId())).findFirst();
        if (aCaseAggregateOption.isEmpty()) {
            builder.add(caseAddedEvent()
                    .withCaseId(caseFromRequest.getCaseId())
                    .withUrn(caseFromRequest.getUrn())
                    .withProsecutionAuthorityCode(caseFromRequest.getProsecutionAuthorityCode())
                    .withIsCivil(caseFromRequest.getIsCivil())
                    .withGroupId(caseFromRequest.getGroupId())
                    .withIsGroupMember(caseFromRequest.getIsGroupMember())
                    .withIsGroupMaster(caseFromRequest.getIsGroupMaster())
                    .build());
        }
        return apply(builder.build());
    }

    public Stream<Object> handleRejectedSjpCase(final UUID id) {
        return apply(of(sjpCaseRejectedEvent().withId(id).build()));
    }

    @SuppressWarnings("java:S107")
    public Stream<Object> handleDefendants(final CaseDetails caseDetailsFromRequest, final boolean sendSpiOut, final Optional<JurisdictionType> jurisdictionType, final String prosecutorEmailAddress, final boolean isPoliceProsecutor, final Optional<LocalDate> hearingDay, final String applicationTypeForCase, final String courtCentre, final Optional<Boolean> isReshare) {
        final Stream.Builder<Object> builder = builder();
        final Optional<Case> aCaseAggregateOptional = this.cases.stream().filter(c -> caseDetailsFromRequest.getCaseId().equals(c.getCaseId())).findFirst();
        aCaseAggregateOptional.ifPresent(aCase -> createOrUpdateDefendant(caseDetailsFromRequest, builder, aCase, sendSpiOut, jurisdictionType, prosecutorEmailAddress, isPoliceProsecutor, hearingDay, applicationTypeForCase, courtCentre,isReshare));
        return apply(builder.build());
    }

    public Stream<Object> handleApplicationUpdateNotification(final String emailAddress, final UUID applicationId, final String urn, final String defendant){
        return apply(of(appealUpdateNotificationRequested()
                .withEmailAddress(emailAddress)
                .withNotificationId(randomUUID())
                .withSubject("Appeal Update")
                .withApplicationId(applicationId.toString())
                .withUrn(urn)
                .withDefendant(defendant)
                .build()));
    }

    @SuppressWarnings("java:S3776")
    private void createOrUpdateDefendant(final CaseDetails caseDetailsFromRequest, final Stream.Builder<Object> builder, final Case aCaseAggregate,
                                         final boolean sendSpiOut, final Optional<JurisdictionType> jurisdictionType, final String prosecutorEmailAddress,
                                         final boolean isPoliceProsecutor, final Optional<LocalDate> hearingDay, final String applicationTypeForCase, final String courtCentre,
                                         final Optional<Boolean> isReshare) {
        final List<Defendant> defendantsFromAggregate = aCaseAggregate.getDefendants();
        final String hearingDate = hearingDay.map(LocalDate::toString).orElse(EMPTY);
        final String caseId = caseDetailsFromRequest.getCaseId().toString();
        final CaseResultDetails caseResultDetails = caseResultAmendmentDetailsList.get(caseDetailsFromRequest.getCaseId());

        defendants = new ArrayList<>();
        boolean isResultReshared = false;
        if (hearing != null && isNotEmpty(hearing.getHearingDays()) && hearing.getHearingDays().size() > 1) {
            isResultReshared = isReshare.orElse(false);
        } else {
            isResultReshared = nonNull(caseResultDetails) ? caseResultDetails.isThisCaseReshared() : isReshare.orElse(false);
        }
        for (final CaseDefendant defendantFromRequest : caseDetailsFromRequest.getDefendants()) {
            final Optional<Defendant> defendantOptional = defendantsFromAggregate.stream().filter(d -> d.getId().equals(defendantFromRequest.getDefendantId())).findFirst();
            if (defendantOptional.isEmpty()) {
                buildDefendantEvent(caseDetailsFromRequest, builder, defendantFromRequest, sendSpiOut, jurisdictionType, hearingDay, isResultReshared);
            } else {
                updateDefendant(hearing, hearingDay, caseDetailsFromRequest, builder, defendantFromRequest, isResultReshared, sendSpiOut, jurisdictionType);
            }
        }

        if (isEligibleForEmailNotification(jurisdictionType, isPoliceProsecutor, isResultReshared, sendSpiOut)) {
            if (nonNull(prosecutorEmailAddress) && !(EMPTY).equalsIgnoreCase(prosecutorEmailAddress)) {

                final uk.gov.moj.cpp.results.domain.event.CaseResultDetails caseResultDetailsForEmail = CaseResultDetailsConverter.convert(caseResultDetails);

                if (hasResults(caseResultDetailsForEmail)) {
                    builder.add(PoliceNotificationRequestedV2.policeNotificationRequestedV2()
                            .withNotificationId(randomUUID())
                            .withCaseDefendants(defendants)
                            .withAmendReshare(isResultReshared ? AMEND_RESHARE : EMPTY)
                            .withPoliceEmailAddress(prosecutorEmailAddress)
                            .withDateOfHearing(hearingDate)
                            .withApplicationTypeForCase(applicationTypeForCase)
                            .withUrn(caseDetailsFromRequest.getUrn())
                            .withCaseId(caseId)
                            .withCourtCentre(courtCentre)
                            .withCaseResultDetails(caseResultDetailsForEmail)
                            .build());
                }
            } else {
                builder.add(EmailNotificationFailed.emailNotificationFailed().withUrn(caseDetailsFromRequest.getUrn()).withErrorMessage(INVALID_EMAIL_ID).build());
            }
        }
    }



    private boolean isEligibleForEmailNotification(final Optional<JurisdictionType> jurisdictionType, final boolean isProsecutorPolice, final boolean isResultAmendedReshared, final boolean sendSpiOut) {
        if (!isProsecutorPolice || !sendSpiOut) {
            return false;
        }

        return isCrownCourt(jurisdictionType) || (isMagsCourt(jurisdictionType) && isResultAmendedReshared);
    }

    private boolean isCrownCourt(final Optional<JurisdictionType> jurisdictionType) {
        return jurisdictionType.map(type -> type.equals(JurisdictionType.CROWN)).orElse(false);
    }

    private boolean isMagsCourt(final Optional<JurisdictionType> jurisdictionType) {
        return jurisdictionType.map(type -> type.equals(JurisdictionType.MAGISTRATES)).orElse(false);
    }

    @SuppressWarnings({"squid:CommentedOutCodeLine"})
    private void buildDefendantEvent(final CaseDetails casesDetailsFromRequest, final Stream.Builder<Object> builder, final CaseDefendant defendantFromRequest,
                                     final boolean sendSpiOut, final Optional<JurisdictionType> jurisdictionType, final Optional<LocalDate> hearingDay, final boolean isResultReshared) {
        if (isResultPresent(defendantFromRequest)) {
            builder.add(defendantAddedEvent().withCaseId(casesDetailsFromRequest.getCaseId()).withDefendant(defendantFromRequest).build());
            final CourtCentreWithLJA enhancedCourtCenter = enhanceCourtCenter(defendantFromRequest.getDefendantId());
            if (sendSpiOut) {
                defendants.add(defendantFromRequest);
                if (!isCrownCourt(jurisdictionType) && !isResultReshared) {
                    builder.add(
                            buildPoliceResultGeneratedEvent(casesDetailsFromRequest.getCaseId(), casesDetailsFromRequest.getUrn(), defendantFromRequest, hearingDay, enhancedCourtCenter)
                    );
                }
            }
        } else {
            builder.add(defendantRejectedEvent().withCaseId(casesDetailsFromRequest.getCaseId()).withDefendantId(defendantFromRequest.getDefendantId()).build());
        }
    }

    private void updateDefendant(final Hearing hearing, final Optional<LocalDate> hearingDay, final CaseDetails casesDetailsFromRequest,
                                 final Stream.Builder<Object> builder,
                                 final CaseDefendant defendantFromRequest,
                                 final boolean isResultReshared, boolean sendSpiOut, Optional<JurisdictionType> jurisdictionType) {

        if (isResultReshared) {
            builder.add(defendantUpdatedEvent().withCaseId(casesDetailsFromRequest.getCaseId()).withDefendant(defendantFromRequest).build());
            defendants.add(defendantFromRequest);
        } else {
            if (isResultPresent(defendantFromRequest) && isNotEmpty(hearing.getHearingDays()) && hearing.getHearingDays().size() > 1) {
                final CourtCentreWithLJA enhancedCourtCenter = enhanceCourtCenter(defendantFromRequest.getDefendantId());
                if (sendSpiOut) {
                    defendants.add(defendantFromRequest);
                    if (!isCrownCourt(jurisdictionType) && !isResultReshared) {
                        builder.add(
                                buildPoliceResultGeneratedEvent(casesDetailsFromRequest.getCaseId(), casesDetailsFromRequest.getUrn(), defendantFromRequest, hearingDay, enhancedCourtCenter)
                        );
                    }
                }
            } else {
                builder.add(defendantUpdatedEvent().withCaseId(casesDetailsFromRequest.getCaseId()).withDefendant(defendantFromRequest).build());
                defendants.add(defendantFromRequest);
            }
        }
    }

    public Stream<Object> generatePoliceResults(final String caseIdFromApi, final String defendantIdFromApi, final Optional<LocalDate> hearingDay) {
        final Stream.Builder<Object> builder = builder();
        final Optional<Case> aCaseAggregateOptional = this.cases.stream().filter(c -> caseIdFromApi.equals(c.getCaseId().toString())).findFirst();

        if (aCaseAggregateOptional.isPresent()) {
            final Case aCaseAggregate = aCaseAggregateOptional.get();
            final Optional<Defendant> defendantOptional = aCaseAggregate.getDefendants().stream().filter(d -> d.getId().toString().equals(defendantIdFromApi)).findFirst();
            defendantOptional.ifPresent(defendant -> buildPoliceResultGeneratedEvent(aCaseAggregate, builder, defendant, hearingDay));
        }
        return apply(builder.build());
    }

    private void buildPoliceResultGeneratedEvent(final Case aCaseAggregate, final Stream.Builder<Object> builder, final Defendant defendant, final Optional<LocalDate> hearingDay) {

        final CaseDefendant caseDefendant = DefendantToCaseDefendantConverter.convert(defendant);
        final CourtCentreWithLJA enhancedCourtCenter = enhanceCourtCenter(defendant.getId());

        if (isResultPresent(caseDefendant)) {
            builder.add(
                    buildPoliceResultGeneratedEvent(aCaseAggregate.getCaseId(), aCaseAggregate.getUrn(), caseDefendant, hearingDay, enhancedCourtCenter)
            );
        }
    }

    /**
     * Builds an {@link PoliceResultGenerated} event. If the request is from a specific hearingDay
     * then that day will become the session day, to be send to SPI.
     *
     * @param caseId     - the caseId
     * @param caseUrn    - the caseUrn
     * @param defendant  - the defendant
     * @param hearingDay - Optional day of the hearing (should be mandatory after multi-day hearings
     *                   implemented)
     * @return Event with session day representing the hearingDay being processed if provided from
     * command.
     */
    private PoliceResultGenerated buildPoliceResultGeneratedEvent(final UUID caseId, final String caseUrn, final CaseDefendant defendant, final Optional<LocalDate> hearingDay, final CourtCentreWithLJA enhancedCourtCenter) {
        List<SessionDay> sessionDayList = this.sessionDays;
        if (hearingDay.isPresent()) {
            sessionDayList = sessionDayList.stream().filter(sd -> hearingDay.get().equals(sd.getSittingDay().toLocalDate())).collect(Collectors.toList());
        }
        if(hearing != null && Boolean.TRUE.equals(hearing.getIsBoxHearing())){
            sessionDayList = sessionDayList.stream().map(sd -> new SessionDay(sd.getListedDurationMinutes(), sd.getListingSequence(), this.sharedDate)).collect(Collectors.toList());
        }


        return policeResultGenerated()
                .withId(id)
                .withCourtCentreWithLJA(enhancedCourtCenter)
                .withSessionDays(sessionDayList)
                .withCaseId(caseId)
                .withUrn(caseUrn)
                .withDefendant(defendant)
                .build();
    }

    private boolean isResultPresent(final CaseDefendant defendantFromRequest) {
        return isNotNullOrEmpty(defendantFromRequest.getJudicialResults()) || (isNotNullOrEmpty(defendantFromRequest.getOffences()) && defendantFromRequest.getOffences().stream().anyMatch(o -> isNotNullOrEmpty(o.getJudicialResults())));
    }

    private boolean isNotNullOrEmpty(final List list) {
        return !isNullOrEmpty(list);
    }

    private boolean isNullOrEmpty(final List list) {
        return null == list || list.isEmpty();
    }

    private CourtCentreWithLJA enhanceCourtCenter(final UUID defendantId) {

        if (youthCourtDefendantIds != null &&
                youthCourtDefendantIds.stream().anyMatch(defendantId::equals)) {
            final CourtCentre.Builder courtCentreBuilder =
                    courtCentre()
                            .withValuesFrom(this.courtCentreWithLJA.getCourtCentre())
                            .withPsaCode(youthCourt.getCourtCode());

            populateLjaDetails().ifPresent(courtCentreBuilder::withLja);

            return courtCentreWithLJA()
                    .withValuesFrom(this.courtCentreWithLJA)
                    .withCourtCentre(courtCentreBuilder.build())
                    .withPsaCode(youthCourt.getCourtCode())
                    .build();
        }

        return this.courtCentreWithLJA;
    }

    private Optional<LjaDetails> populateLjaDetails() {
        if (courtCentreWithLJA.getCourtCentre() != null &&
                courtCentreWithLJA.getCourtCentre().getLja() != null) {
            return Optional.of(ljaDetails()
                    .withValuesFrom(courtCentreWithLJA
                            .getCourtCentre()
                            .getLja())
                    .withLjaCode(youthCourt.getCourtCode().toString())
                    .build());
        }
        return Optional.empty();
    }

    public List<UUID> getCaseIds() {
        return this.cases.stream().map(Case::getCaseId).collect(toList());
    }

    public Hearing getHearing() {
        return this.hearing;
    }

    public Optional<String> getProsecutionAuthorityCode(final String caseIdFromApi) {
        return this.cases.stream()
                .filter(c -> caseIdFromApi.equals(c.getCaseId().toString()))
                .findFirst()
                .map(Case::getProsecutionAuthorityCode);
    }


}
