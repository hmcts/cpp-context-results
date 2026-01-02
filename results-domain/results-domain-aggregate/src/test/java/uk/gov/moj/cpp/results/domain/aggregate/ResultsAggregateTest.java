package uk.gov.moj.cpp.results.domain.aggregate;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.Integer.valueOf;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.AllocationDecision.allocationDecision;
import static uk.gov.justice.core.courts.AssociatedIndividual.associatedIndividual;
import static uk.gov.justice.core.courts.BailStatus.bailStatus;
import static uk.gov.justice.core.courts.CaseDefendant.caseDefendant;
import static uk.gov.justice.core.courts.CaseDetails.caseDetails;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.CourtCentreWithLJA.courtCentreWithLJA;
import static uk.gov.justice.core.courts.CourtIndicatedSentence.courtIndicatedSentence;
import static uk.gov.justice.core.courts.Gender.FEMALE;
import static uk.gov.justice.core.courts.Gender.MALE;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.core.courts.Individual.individual;
import static uk.gov.justice.core.courts.IndividualDefendant.individualDefendant;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.OffenceDetails.offenceDetails;
import static uk.gov.justice.core.courts.SessionDay.sessionDay;
import static uk.gov.justice.core.courts.YouthCourt.youthCourt;
import static uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted.publicHearingResulted;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.CaseAddedEvent;
import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentreWithLJA;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAddedEvent;
import uk.gov.justice.core.courts.DefendantRejectedEvent;
import uk.gov.justice.core.courts.DefendantUpdatedEvent;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationEjected;
import uk.gov.justice.core.courts.HearingCaseEjected;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.core.courts.HearingResultsAddedForDay;
import uk.gov.justice.core.courts.Individual;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceDetails;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.PoliceResultGenerated;
import uk.gov.justice.core.courts.PoliceResultGeneratedForStandaloneApplication;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SessionAddedEvent;
import uk.gov.justice.core.courts.SessionDay;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.event.AmendmentType;
import uk.gov.moj.cpp.results.domain.event.AppealUpdateNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.DefendantResultDetails;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequestedV2;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResultsAggregateTest {

    public static final String PLEA_VALUE_DENIES = "DENIES";
    private static final UUID OFFENCE_ID = randomUUID();
    private static final String FIRST_NAME_1 = "Jane";
    private static final String LAST_NAME_1 = "Johnson";
    private static final Gender GENDER_1 = FEMALE;
    private static final LocalDate DATE_OF_BIRTH_1 = LocalDate.of(1980, 11, 15);
    private static final String TITLE_1 = "MR";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Smith";
    private static final Gender GENDER = MALE;
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1970, 1, 10);
    private static final String TITLE = "MR";
    private static final Address ADDRESS = address().withAddress1("101 green house").withPostcode("XE10 FR").build();
    private static final Address ADDRESS_1 = address().withAddress1("101 blue house").withPostcode("AE10 FR").build();
    private static final ContactNumber CONTACT_NUMBER = contactNumber().withFax("454645").withHome("56567").withMobile("123232").withWork("5465767").withPrimaryEmail("abc@com.uk").withSecondaryEmail("xyx@com.uk").build();
    private static final ContactNumber CONTACT_NUMBER_1 = contactNumber().withFax("12345").withHome("5678").withMobile("91010").withWork("1233").withPrimaryEmail("efg@com.uk").withSecondaryEmail("ljk@com.uk").build();
    private static final UUID CASE_ID = randomUUID();
    private static final String URN = "123445";
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final String EMAIL_ADDRESS = "test@hmcts.net";
    private final PublicHearingResulted input = PublicHearingResulted.publicHearingResulted()
            .setHearing(hearing()
                    .withId(UUID.randomUUID())
                    .build())
            .setSharedTime(ZonedDateTime.now());

    private final UUID hearingId = UUID.randomUUID();
    private final String courtCode = "cc123";
    private final PublicHearingResulted hearingResultedWithYouthCourt = publicHearingResulted()
            .setHearing(hearing()
                    .withId(hearingId)
                    .withYouthCourt(youthCourt()
                            .withCourtCode(1234)
                            .build())
                    .withYouthCourtDefendantIds(singletonList(DEFENDANT_ID))
                    .build())
            .setSharedTime(ZonedDateTime.now());

    @InjectMocks
    private ResultsAggregate resultsAggregate;

    @Test
    public void testSaveShareResults_shouldRaiseHearingResultsAddedEvent() {
        final HearingResultsAdded hearingResultsAdded = resultsAggregate.saveHearingResults(input)
                .map(o -> (HearingResultsAdded) o)
                .findFirst()
                .orElse(null);

        assertNotNull(hearingResultsAdded);
        assertEquals(input.getHearing(), hearingResultsAdded.getHearing());
        assertEquals(input.getSharedTime(), hearingResultsAdded.getSharedTime());
    }

    @Test
    public void testHandleStandaloneApplication() {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withJudicialResults(singletonList(judicialResult().build()))
                .withType(CourtApplicationType.courtApplicationType()
                        .withLinkType(LinkType.STANDALONE)
                        .build())
                .build();
        final boolean sendSpiOut = true;
        final Optional<LocalDate> hearingDay = Optional.of(LocalDate.now());
        final Optional<Boolean> isReshare = Optional.of(false);
        final PoliceResultGeneratedForStandaloneApplication policeResultGeneratedForStandaloneApplication = resultsAggregate.handleStandaloneApplication(courtApplication, sendSpiOut, hearingDay, isReshare)
                .map(o -> (PoliceResultGeneratedForStandaloneApplication) o)
                .findFirst()
                .orElse(null);

        assertNotNull(policeResultGeneratedForStandaloneApplication);
    }



    @Test
    public void testEjectCaseOrApplication_whenPayloadContainsCaseId_expectHearingCaseEjectedEvent() {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final String CASE_ID = "caseId";
        final String HEARING_ID = "hearingId";
        final PublicHearingResulted input = PublicHearingResulted.publicHearingResulted()
                .setHearing(hearing()
                        .withId(hearingId)
                        .build())
                .setSharedTime(ZonedDateTime.now());
        resultsAggregate.saveHearingResults(input);
        final JsonObject payload = createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(CASE_ID, caseId.toString())
                .build();
        final HearingCaseEjected hearingCaseEjected = resultsAggregate.ejectCaseOrApplication(hearingId, payload)
                .map(o -> (HearingCaseEjected) o)
                .findFirst().orElse(null);
        assertNotNull(hearingCaseEjected);
        assertEquals(hearingId, hearingCaseEjected.getHearingId());
        assertEquals(caseId, hearingCaseEjected.getCaseId());

    }

    @Test
    public void testEjectCaseOrApplication_whenPayloadContainsCaseId_expectHearingApplicationEjectedEvent() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final String APPLICATION_ID = "applicationId";
        final String HEARING_ID = "hearingId";
        final PublicHearingResulted input = PublicHearingResulted.publicHearingResulted()
                .setHearing(hearing()
                        .withId(hearingId)
                        .build())
                .setSharedTime(ZonedDateTime.now());
        resultsAggregate.saveHearingResults(input);
        final JsonObject payload = createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(APPLICATION_ID, applicationId.toString())
                .build();
        final HearingApplicationEjected hearingApplicationEjected = resultsAggregate.ejectCaseOrApplication(hearingId, payload)
                .map(o -> (HearingApplicationEjected) o)
                .findFirst().orElse(null);
        assertNotNull(hearingApplicationEjected);
        assertEquals(hearingId, hearingApplicationEjected.getHearingId());
        assertEquals(applicationId, hearingApplicationEjected.getApplicationId());

    }

    @Test
    public void testEjectCaseOrApplication_whenHearingNotResultedForHearingInPayload_expecNull() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final String APPLICATION_ID = "applicationId";
        final String HEARING_ID = "hearingId";

        final JsonObject payload = createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(APPLICATION_ID, applicationId.toString())
                .build();
        final List<Object> eventStream = resultsAggregate.ejectCaseOrApplication(hearingId, payload).collect(toList());
        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void testHandleSession() {
        final UUID id = randomUUID();
        final CourtCentreWithLJA courtCentre = courtCentreWithLJA().build();
        final ZonedDateTime sittingDay = now();
        final SessionDay sessionDay = sessionDay().withListedDurationMinutes(10).withListingSequence(15).withSittingDay(sittingDay).build();
        final List<SessionDay> sessionDays = of(sessionDay);

        final SessionAddedEvent update = resultsAggregate.handleSession(id, courtCentre, sessionDays)
                .map(o -> (SessionAddedEvent) o)
                .findFirst()
                .orElse(null);

        assertNotNull(update);
        assertEquals(id, update.getId());
        assertEquals(courtCentre, update.getCourtCentreWithLJA());
        final List<SessionDay> sessionDaysResult = update.getSessionDays();
        final SessionDay sessionDayResult = sessionDaysResult.get(0);
        assertEquals(sessionDays, sessionDaysResult);
        assertEquals(sessionDay.getListedDurationMinutes(), sessionDayResult.getListedDurationMinutes());
        assertEquals(sessionDay.getListingSequence(), sessionDayResult.getListingSequence());
        assertEquals(sessionDay.getSittingDay(), sessionDayResult.getSittingDay());
    }

    @Test
    public void shouldNotRaiseSessionAddedEventWhenSessionDaysUnchanged() {
        final UUID id = randomUUID();
        final CourtCentreWithLJA courtCentre = courtCentreWithLJA().build();
        final ZonedDateTime sittingDay = now();
        final SessionDay sessionDay = sessionDay().withListedDurationMinutes(10).withListingSequence(15).withSittingDay(sittingDay).build();
        final List<SessionDay> sessionDays = of(sessionDay);
        final SessionAddedEvent update = resultsAggregate.handleSession(id, courtCentre, sessionDays)
                .map(o -> (SessionAddedEvent) o)
                .findFirst()
                .orElse(null);
        assertNotNull(update);
        assertEquals(id, update.getId());
        assertEquals(courtCentre, update.getCourtCentreWithLJA());
        final List<SessionDay> sessionDaysResult = update.getSessionDays();
        final SessionDay sessionDayResult = sessionDaysResult.get(0);
        assertEquals(sessionDays, sessionDaysResult);
        assertEquals(sessionDay.getListedDurationMinutes(), sessionDayResult.getListedDurationMinutes());
        assertEquals(sessionDay.getListingSequence(), sessionDayResult.getListingSequence());
        assertEquals(sessionDay.getSittingDay(), sessionDayResult.getSittingDay());

        assertThat(resultsAggregate.handleSession(id, courtCentre, sessionDays).count(), is(0L));
    }

    @Test
    public void shouldRaiseSessionAddedEventWhenSessionDaysUpdatedForHearing() {
        final UUID id = randomUUID();
        final CourtCentreWithLJA courtCentre = courtCentreWithLJA().build();
        final ZonedDateTime sittingDay = now();
        final SessionDay sessionDay = sessionDay().withListedDurationMinutes(10).withListingSequence(15).withSittingDay(sittingDay).build();
        final List<SessionDay> sessionDays = of(sessionDay);
        final SessionAddedEvent update = resultsAggregate.handleSession(id, courtCentre, sessionDays)
                .map(o -> (SessionAddedEvent) o)
                .findFirst()
                .orElse(null);
        assertNotNull(update);
        assertEquals(id, update.getId());
        assertEquals(courtCentre, update.getCourtCentreWithLJA());
        final List<SessionDay> sessionDaysResult = update.getSessionDays();
        final SessionDay sessionDayResult = sessionDaysResult.get(0);
        assertEquals(sessionDays, sessionDaysResult);
        assertEquals(sessionDay.getListedDurationMinutes(), sessionDayResult.getListedDurationMinutes());
        assertEquals(sessionDay.getListingSequence(), sessionDayResult.getListingSequence());
        assertEquals(sessionDay.getSittingDay(), sessionDayResult.getSittingDay());

        final SessionDay updatedSessionDay = sessionDay().withListedDurationMinutes(10).withListingSequence(15).withSittingDay(sittingDay.plusDays(1l)).build();
        final List<SessionDay> updatedSessionDays = of(updatedSessionDay);

        assertThat(resultsAggregate.handleSession(id, courtCentre, updatedSessionDays).count(), is(1L));
    }

    @Test
    public void testHandleCases() {
        final UUID id = randomUUID();
        final String urn = "123445";
        final String prosecutionAuthorityCode = randomAlphanumeric(5);
        final CaseDetails caseDetails = caseDetails().withCaseId(id).withUrn(urn).withProsecutionAuthorityCode(prosecutionAuthorityCode).build();
        final CaseAddedEvent update = resultsAggregate.handleCase(caseDetails)
                .map(o -> (CaseAddedEvent) o)
                .findFirst()
                .orElse(null);
        assertNotNull(update);
        assertEquals(id, update.getCaseId());
        assertEquals(urn, update.getUrn());
        assertEquals(prosecutionAuthorityCode, update.getProsecutionAuthorityCode());
    }

    @Test
    public void testHandleCasesForGroupCases() {
        final UUID id = randomUUID();
        final UUID groupId = randomUUID();
        final String urn = "123445";
        final String prosecutionAuthorityCode = randomAlphanumeric(5);
        final CaseDetails caseDetails = caseDetails()
                .withCaseId(id)
                .withUrn(urn)
                .withProsecutionAuthorityCode(prosecutionAuthorityCode)
                .withIsCivil(Boolean.TRUE)
                .withGroupId(groupId)
                .withIsGroupMember(Boolean.TRUE)
                .withIsGroupMaster(Boolean.FALSE)
                .build();
        final CaseAddedEvent update = resultsAggregate.handleCase(caseDetails)
                .map(o -> (CaseAddedEvent) o)
                .findFirst()
                .orElse(null);
        assertNotNull(update);
        assertEquals(id, update.getCaseId());
        assertEquals(urn, update.getUrn());
        assertEquals(prosecutionAuthorityCode, update.getProsecutionAuthorityCode());
        assertEquals(true, update.getIsCivil());
        assertEquals(groupId, update.getGroupId());
        assertEquals(true, update.getIsGroupMember());
        assertEquals(false, update.getIsGroupMaster());
    }

    @Test
    public void testHandleDefendantsWhenOffenceAndTheirResultIsPresent() {
        final CaseDetails caseDetails = createCaseDetails(null, of(offenceDetails()
                .withId(OFFENCE_ID).withAllocationDecision(buildAllocationDecision())
                .withJudicialResults(of(judicialResult().build()))
                .withPlea(Plea.plea()
                        .withOffenceId(OFFENCE_ID)
                        .withDelegatedPowers(DelegatedPowers.delegatedPowers().withUserId(randomUUID()).build())
                        .withPleaValue(PLEA_VALUE_DENIES)
                        .build())
                .build()));
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        resultsAggregate.handleCase(caseDetails);
        final List<Object> objectList = resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.FALSE)).collect(toList());
        assertDefendantAddedEvent(caseDetails.getDefendants().get(0), objectList);
        assertPoliceResultGeneratedEvent(caseDetails.getDefendants().get(0), objectList);
        assertNoPoliceNotificationRequestedV2Event(caseDetails.getDefendants().get(0), objectList);
    }


    @Test
    public void testHandleDefendantsWhenTheirResultIsPresentButNoOffenceExist() {
        final CaseDetails caseDetails = createCaseDetails(of(judicialResult().withJudicialResultId(randomUUID()).build()), of());
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        resultsAggregate.handleCase(caseDetails);
        final List<Object> objectList = resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.FALSE)).collect(toList());
        assertDefendantAddedEvent(caseDetails.getDefendants().get(0), objectList);
        assertPoliceResultGeneratedEvent(caseDetails.getDefendants().get(0), objectList);
    }


    @Test
    public void testHandleDefendantsRejectEvent() {
        final CaseDetails caseDetails = createCaseDetails(of(), of());
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        resultsAggregate.handleCase(caseDetails);
        final List<Object> objectList = resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE)).collect(toList());
        final DefendantRejectedEvent defendantRejectedEvent = objectList.stream().filter(e -> e instanceof DefendantRejectedEvent)
                .map(o -> (DefendantRejectedEvent) o)
                .findFirst()
                .orElse(null);
        assertNotNull(defendantRejectedEvent);
        assertEquals(CASE_ID, defendantRejectedEvent.getCaseId());
        assertEquals(DEFENDANT_ID, defendantRejectedEvent.getDefendantId());

    }

    @Test
    public void shouldNotRaisePoliceResultGeneratedEventWhenSpiOutFlagFalse() {
        final List<OffenceDetails> offenceDetailsList = new ArrayList<>();
        offenceDetailsList.add(offenceDetails().withId(OFFENCE_ID).withJudicialResults(of(judicialResult().build())).build());
        final CaseDetails caseDetails = createCaseDetails(null, offenceDetailsList);
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        resultsAggregate.handleCase(caseDetails);
        final List<Object> objectList = resultsAggregate.handleDefendants(caseDetails, false, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE)).collect(toList());
        assertDefendantAddedEvent(caseDetails.getDefendants().get(0), objectList);
        assertThat(objectList.size(), is(1));
        assertNoPoliceNotificationRequestedV2Event(caseDetails.getDefendants().get(0), objectList);
    }

    @Test
    public void testHandleDefendantsWhenMagsCourtJudicialResultHasBeenAmended_NoPoliceGeneratedEventShouldBeRaised() {

        final List<OffenceDetails> offences = new ArrayList<>();
        final JudicialResult judicialResult = judicialResult().withJudicialResultId(randomUUID()).withIsNewAmendment(true).build();
        offences.add(offenceDetails().withId(randomUUID()).withJudicialResults(Arrays.asList(judicialResult)).build());
        final CaseDetails caseDetails = createCaseDetails(null, offences);
        final CaseDefendant caseDefendant = caseDetails.getDefendants().get(0);

        resultsAggregate.saveHearingResultsForDay(PublicHearingResulted.publicHearingResulted()
                .setHearing(createHearing(caseDetails.getCaseId(), caseDefendant.getDefendantId(), offences.get(0).getId(), judicialResult)), LocalDate.now());

        resultsAggregate.handleCase(caseDetails);

        resultsAggregate.saveHearingResultsForDay(PublicHearingResulted.publicHearingResulted()
                .setHearing(createHearing(caseDetails.getCaseId(), caseDefendant.getDefendantId(), offences.get(0).getId(), judicialResult)), LocalDate.now());

        resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE));

        offences.get(0).getJudicialResults().set(0, JudicialResult.judicialResult().withValuesFrom(judicialResult).withAmendmentDate(LocalDate.of(2019, 2, 3)).build());
        final CaseDetails finalCaseDetails = CaseDetails.caseDetails().withValuesFrom(caseDetails).withDefendants(of(CaseDefendant.caseDefendant().withValuesFrom(caseDetails.getDefendants().get(0)).withOffences(offences).build()))
                .build();

        final List<Object> objectList = resultsAggregate.handleDefendants(finalCaseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE)).collect(toList());
        assertDefendantUpdatedEvent(caseDefendant, objectList);
        assertNoPoliceResultGeneratedEvent(finalCaseDetails.getDefendants().get(0), objectList);
        assertPoliceNotificationRequestedV2Event(finalCaseDetails.getDefendants().get(0), objectList);
        assertResultIsAmended(objectList);
    }

    @Test
    public void testHandleDefendantsWhenAJudicialResultHasBeenDeleted() {
        final List<OffenceDetails> offences = new ArrayList<>();
        final JudicialResult judicialResultOne = judicialResult().withJudicialResultId(randomUUID()).build();
        final JudicialResult judicialResultTwo = judicialResult().withJudicialResultId(randomUUID()).build();
        List<JudicialResult> resultList = new ArrayList<>();
        resultList.add(judicialResultOne);
        resultList.add(judicialResultTwo);
        offences.add(offenceDetails().withId(randomUUID()).withJudicialResults(resultList).build());
        final CaseDetails caseDetails = createCaseDetails(null, offences);
        final CaseDefendant caseDefendant = caseDetails.getDefendants().get(0);
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        resultsAggregate.handleCase(caseDetails);
        resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE));
        resultList.remove(judicialResultTwo);

        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        final List<Object> objectList = resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE)).collect(toList());
        assertDefendantUpdatedEvent(caseDefendant, objectList);
        assertNoPoliceResultGeneratedEvent(caseDetails.getDefendants().get(0), objectList);
        assertPoliceNotificationRequestedV2Event(caseDetails.getDefendants().get(0), objectList);
    }

    @Test
    public void testHandleDefendantsWhenMagsCourtJudicialResultHasBeenAmendedForTextOrDeleted() {
        final List<OffenceDetails> offences = new ArrayList<>();
        final JudicialResult judicialResultOne = judicialResult().withJudicialResultId(randomUUID()).build();
        final JudicialResult judicialResultTwo = judicialResult().withJudicialResultId(randomUUID()).build();
        List<JudicialResult> resultList = new ArrayList<>();
        resultList.add(judicialResultOne);
        resultList.add(judicialResultTwo);
        offences.add(offenceDetails().withId(randomUUID()).withJudicialResults(resultList).build());
        final CaseDetails caseDetails = createCaseDetails(null, offences);
        final CaseDefendant caseDefendant = caseDetails.getDefendants().get(0);
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        resultsAggregate.handleCase(caseDetails);
        resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE));
        offences.get(0).getJudicialResults().set(1, JudicialResult.judicialResult().withValuesFrom(judicialResultTwo).withIsNewAmendment(true).build());
        final CaseDetails finalCaseDetails = CaseDetails.caseDetails().withValuesFrom(caseDetails).withDefendants(of(CaseDefendant.caseDefendant().withValuesFrom(caseDetails.getDefendants().get(0)).withOffences(offences).build()))
                .build();
        resultsAggregate.apply(hearingResultsAddedForDay(finalCaseDetails));
        final List<Object> objectList = resultsAggregate.handleDefendants(finalCaseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE)).collect(toList());
        assertDefendantUpdatedEvent(caseDefendant, objectList);
        assertNoPoliceResultGeneratedEvent(finalCaseDetails.getDefendants().get(0), objectList);
        assertPoliceNotificationRequestedV2Event(finalCaseDetails.getDefendants().get(0), objectList);
    }

    @Test
    public void testHandleDefendantsWhenAJudicialResultHasBeenAddedDuringSubsequentRequest() {
        final List<OffenceDetails> offences = new ArrayList<>();
        final JudicialResult judicialResult = judicialResult().withJudicialResultId(randomUUID()).build();
        offences.add(offenceDetails().withId(OFFENCE_ID).build());
        offences.add(offenceDetails().withId(randomUUID()).withJudicialResults(of(judicialResult().withJudicialResultId(randomUUID()).build())).build());
        CaseDetails caseDetails = createCaseDetails(null, offences);
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        resultsAggregate.handleCase(caseDetails);
        resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE));

        caseDetails = createCaseDetails(null, of(offenceDetails().withId(OFFENCE_ID).withJudicialResults(of(judicialResult)).build()));
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        final List<Object> objectList = resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE)).collect(toList());
        assertDefendantUpdatedEvent(caseDetails.getDefendants().get(0), objectList);
        assertNoPoliceResultGeneratedEvent(caseDetails.getDefendants().get(0), objectList);
        assertPoliceNotificationRequestedV2Event(caseDetails.getDefendants().get(0), objectList);
    }

    @Test
    public void testHandleDefendantsWhenNoJudicialResultHasBeenAdded() {
        final List<OffenceDetails> offences = new ArrayList<>();
        offences.add(offenceDetails().withId(OFFENCE_ID).build());
        offences.add(offenceDetails().withId(randomUUID()).withJudicialResults(of(judicialResult().withJudicialResultId(randomUUID()).build())).build());
        CaseDetails caseDetails = createCaseDetails(null, offences);
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        resultsAggregate.handleCase(caseDetails);
        resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE));

        caseDetails = createCaseDetails(null, of(offenceDetails().withId(OFFENCE_ID).build()));

        final List<Object> objectList = resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.FALSE)).collect(toList());
        final DefendantUpdatedEvent defendantUpdatedEvent = objectList.stream().filter(e -> e instanceof DefendantUpdatedEvent)
                .map(o -> (DefendantUpdatedEvent) o)
                .findFirst()
                .orElse(null);
        assertNotNull(defendantUpdatedEvent);

        final PoliceResultGenerated policeResultGenerated = objectList.stream().filter(e -> e instanceof PoliceResultGenerated)
                .map(o -> (PoliceResultGenerated) o)
                .findFirst()
                .orElse(null);
        assertNull(policeResultGenerated);
    }

    @Test
    public void testHandleDefendantsWhenExistingOffenceJudicialResultsRemoved() {
        final List<OffenceDetails> offences = new ArrayList<>();
        offences.add(offenceDetails().withId(OFFENCE_ID).build());
        offences.add(offenceDetails().withId(randomUUID()).withJudicialResults(of(judicialResult().withJudicialResultId(randomUUID()).build())).build());
        CaseDetails caseDetails = createCaseDetails(null, offences);
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        resultsAggregate.handleCase(caseDetails);
        resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE));

        final JudicialResult judicialResult = judicialResult().withJudicialResultId(randomUUID()).build();
        caseDetails = createCaseDetails(null, of(offenceDetails().withId(OFFENCE_ID).withJudicialResults(of(judicialResult)).build()));
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        final List<Object> objectListAfterAdd = resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE)).collect(toList());

        assertDefendantUpdatedEvent(caseDetails.getDefendants().get(0), objectListAfterAdd);
        assertNoPoliceResultGeneratedEvent(caseDetails.getDefendants().get(0), objectListAfterAdd);

        caseDetails = createCaseDetails(null, of(offenceDetails().withId(OFFENCE_ID).build()));
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        final List<Object> objectListAfterRemove = resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE)).collect(toList());

        assertDefendantUpdatedEvent(caseDetails.getDefendants().get(0), objectListAfterRemove);
        final PoliceResultGenerated policeResultGenerated = objectListAfterRemove.stream().filter(e -> e instanceof PoliceResultGenerated)
                .map(o -> (PoliceResultGenerated) o)
                .findFirst()
                .orElse(null);
        assertThat(policeResultGenerated, nullValue());
        assertPoliceNotificationRequestedV2Event(caseDetails.getDefendants().get(0), objectListAfterRemove);

    }

    @Test
    public void testHandleDefendantWhenAnotheCourtOrderOffenceIsResultedInAmendAndReshare() {

        final List<OffenceDetails> offences = new ArrayList<>();

        offences.add(offenceDetails().withId(UUID.fromString("f0b1f9b1-c182-4401-8c64-c69027e84e92")).build());

        final JudicialResult judicialResult1 = judicialResult().withJudicialResultId(UUID.fromString("e0a49380-71ce-4426-85b6-9bf0e3f9ce1a"))
                .withLevel("FINAL")
                .withIsUnscheduled(false)
                .withIsNewAmendment(true)
                .withLabel("Conditional discharge")
                .build();

        offences.add(offenceDetails().withId(UUID.fromString("11896a41-e3d4-49ed-8775-753b46ce237d")).withJudicialResults(of(judicialResult1)).build());

        final JudicialResult judicialResult2 = judicialResult().withJudicialResultId(UUID.fromString("52da305d-7a14-4307-91fa-7b20086eb9c8"))
                .withLevel("FINAL")
                .withIsUnscheduled(false)
                .withIsNewAmendment(true)
                .withLabel("Absolute discharge")
                .build();

        offences.add(offenceDetails().withId(UUID.fromString("17d13499-e166-4d65-851f-6eb8eadcbb82")).withJudicialResults(of(judicialResult2)).build());

        CaseDetails caseDetails = createCaseDetails(null, offences);
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        resultsAggregate.handleCase(caseDetails);
        resultsAggregate.saveHearingResultsForDay(PublicHearingResulted.publicHearingResulted()
                .setHearing(createHearing(caseDetails.getCaseId(), caseDetails.getDefendants().get(0).getDefendantId(), offences.get(0).getId(), judicialResult1)), LocalDate.now());
        resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE));

        final List<OffenceDetails> offences2 = new ArrayList<>();

        offences2.add(offenceDetails().withId(UUID.fromString("f0b1f9b1-c182-4401-8c64-c69027e84e92")).build());


        final JudicialResult judicialResult3 = judicialResult().withJudicialResultId(UUID.fromString("e0a49380-71ce-4426-85b6-9bf0e3f9ce1a"))
                .withLevel("FINAL")
                .withIsUnscheduled(false)
                .withIsNewAmendment(false)
                .withLabel("Conditional discharge")
                .build();
        offences2.add(offenceDetails().withId(UUID.fromString("11896a41-e3d4-49ed-8775-753b46ce237d")).withJudicialResults(of(judicialResult3)).build());

        final JudicialResult judicialResult4 = judicialResult().withJudicialResultId(UUID.fromString("90e43644-4953-47c5-93f7-9b611f4c9146"))
                .withLevel("FINAL")
                .withIsUnscheduled(false)
                .withIsNewAmendment(true)
                .withLabel("Conditional discharge")
                .build();

        offences2.add(offenceDetails().withId(UUID.fromString("591a13d1-500b-415d-bd84-2447499d318b")).withJudicialResults(of(judicialResult4)).build());

        CaseDetails caseDetails2 = createCaseDetails(null, offences2);
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails2));
        final List<Object> objectList = resultsAggregate.handleDefendants(caseDetails2, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE)).collect(toList());

        assertDefendantUpdatedEvent(caseDetails2.getDefendants().get(0), objectList);

    }


    @Test
    public void testHandleDefendantWhenCourtOrderOffenceIsResultedWithAmendAndReshareMultipleTimes() {

        final List<OffenceDetails> offences = new ArrayList<>();

        offences.add(offenceDetails().withId(UUID.fromString("f0b1f9b1-c182-4401-8c64-c69027e84e92")).build());

        final JudicialResult judicialResult1 = judicialResult().withJudicialResultId(UUID.fromString("e0a49380-71ce-4426-85b6-9bf0e3f9ce1a"))
                .withLevel("FINAL")
                .withIsUnscheduled(false)
                .withIsNewAmendment(true)
                .withLabel("Conditional discharge")
                .build();

        offences.add(offenceDetails().withId(UUID.fromString("11896a41-e3d4-49ed-8775-753b46ce237d")).withJudicialResults(of(judicialResult1)).build());

        final JudicialResult judicialResult2 = judicialResult().withJudicialResultId(UUID.fromString("52da305d-7a14-4307-91fa-7b20086eb9c8"))
                .withLevel("FINAL")
                .withIsUnscheduled(false)
                .withIsNewAmendment(true)
                .withLabel("Absolute discharge")
                .build();

        offences.add(offenceDetails().withId(UUID.fromString("17d13499-e166-4d65-851f-6eb8eadcbb82")).withJudicialResults(of(judicialResult2)).build());

        CaseDetails caseDetails = createCaseDetails(null, offences);

        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        resultsAggregate.handleCase(caseDetails);

        resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE));

        final List<OffenceDetails> offences2 = new ArrayList<>();


        offences2.add(offenceDetails().withId(UUID.fromString("f0b1f9b1-c182-4401-8c64-c69027e84e92")).build());

        final JudicialResult judicialResult4 = judicialResult().withJudicialResultId(UUID.fromString("e0a49380-71ce-4426-85b6-9bf0e3f9ce1a"))
                .withLevel("FINAL")
                .withIsUnscheduled(false)
                .withIsNewAmendment(false)
                .withLabel("Conditional discharge")
                .build();

        offences2.add(offenceDetails().withId(UUID.fromString("11896a41-e3d4-49ed-8775-753b46ce237d")).withJudicialResults(of(judicialResult4)).build());


        final JudicialResult judicialResult5 = judicialResult().withJudicialResultId(UUID.fromString("c386b775-774c-45da-b61d-4387beea2a8b"))
                .withLevel("FINAL")
                .withIsUnscheduled(false)
                .withIsNewAmendment(false)
                .withLabel("DISM - Dismissed")
                .build();


        offences2.add(offenceDetails().withId(UUID.fromString("591a13d1-500b-415d-bd84-2447499d318b")).withJudicialResults(of(judicialResult5)).build());

        final JudicialResult judicialResult6 = judicialResult().withJudicialResultId(UUID.fromString("4bb41c3d-15ca-45c5-8f3a-93b16f05b37d"))
                .withLevel("FINAL")
                .withIsUnscheduled(false)
                .withIsNewAmendment(false)
                .withLabel("Absolute discharge")
                .build();

        offences2.add(offenceDetails().withId(UUID.fromString("17d13499-e166-4d65-851f-6eb8eadcbb82")).withJudicialResults(of(judicialResult6)).build());

        CaseDetails caseDetails2 = createCaseDetails(null, offences2);

        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails2));
        resultsAggregate.handleDefendants(caseDetails2, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE));

        final List<OffenceDetails> offences3 = new ArrayList<>();

        offences3.add(offenceDetails().withId(UUID.fromString("f0b1f9b1-c182-4401-8c64-c69027e84e92")).build());


        final JudicialResult judicialResult7 = judicialResult().withJudicialResultId(UUID.fromString("e0a49380-71ce-4426-85b6-9bf0e3f9ce1a"))
                .withLevel("FINAL")
                .withIsUnscheduled(false)
                .withIsNewAmendment(true)
                .withLabel("Conditional discharge")
                .build();

        offences3.add(offenceDetails().withId(UUID.fromString("11896a41-e3d4-49ed-8775-753b46ce237d")).withJudicialResults(of(judicialResult7)).build());


        final JudicialResult judicialResult8 = judicialResult().withJudicialResultId(UUID.fromString("52da305d-7a14-4307-91fa-7b20086eb9c8"))
                .withLevel("FINAL")
                .withIsUnscheduled(false)
                .withIsNewAmendment(true)
                .withLabel("Absolute discharge")
                .build();

        offences3.add(offenceDetails().withId(UUID.fromString("17d13499-e166-4d65-851f-6eb8eadcbb82")).withJudicialResults(of(judicialResult8)).build());


        CaseDetails caseDetails3 = createCaseDetails(null, offences3);

        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails3));
        final List<Object> objectList = resultsAggregate.handleDefendants(caseDetails3, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE)).collect(toList());

        assertDefendantUpdatedEvent(caseDetails3.getDefendants().get(0), objectList);
        assertPoliceNotificationRequestedV2Event(caseDetails3.getDefendants().get(0), objectList);


    }

    @Test
    public void testHandleDefendantsWhenCrownCourtJudicialResultReplaced_NoPoliceGeneratedEvent() {
        final List<OffenceDetails> offences = new ArrayList<>();
        final JudicialResult judicialResult = judicialResult().withAmendmentDate(LocalDate.of(2019, 2, 2)).withJudicialResultId(randomUUID()).build();
        offences.add(offenceDetails().withId(randomUUID()).withJudicialResults(Arrays.asList(judicialResult)).build());
        final CaseDetails caseDetails = createCaseDetails(null, offences);
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        resultsAggregate.handleCase(caseDetails);
        resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE));

        offences.get(0).getJudicialResults().set(0, JudicialResult.judicialResult().withValuesFrom(judicialResult).withJudicialResultId(randomUUID()).build());
        final CaseDetails finalCaseDetails = CaseDetails.caseDetails().withValuesFrom(caseDetails).withDefendants(of(CaseDefendant.caseDefendant().withValuesFrom(caseDetails.getDefendants().get(0)).withOffences(offences).build()))
                .build();
        resultsAggregate.apply(hearingResultsAddedForDay(finalCaseDetails));
        final List<Object> objectList = resultsAggregate.handleDefendants(finalCaseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE)).collect(toList());
        assertDefendantUpdatedEvent(finalCaseDetails.getDefendants().get(0), objectList);
        assertNoPoliceResultGeneratedEvent(finalCaseDetails.getDefendants().get(0), objectList);
        assertPoliceNotificationRequestedV2Event(finalCaseDetails.getDefendants().get(0), objectList);
    }

    @Test
    public void shouldRaisePoliceResultGeneratedEvent_WhenMagsCourtResultFirstTime() {
        final CourtCentreWithLJA courtCentre = courtCentreWithLJA()
                .withCourtCentre(courtCentre()
                        .withCode(courtCode)
                        .withLja(LjaDetails.ljaDetails().withLjaCode("123").build())
                        .withPsaCode(987)
                        .build())
                .build();
        final ZonedDateTime sittingDay = now();
        final SessionDay sessionDay = sessionDay().withListedDurationMinutes(10).withListingSequence(15).withSittingDay(sittingDay).build();
        final List<SessionDay> sessionDays = of(sessionDay);

        resultsAggregate.saveHearingResults(hearingResultedWithYouthCourt);
        resultsAggregate.handleSession(hearingId, courtCentre, sessionDays);

        final CaseDetails caseDetails = createCaseDetails(null, of(offenceDetails().withId(OFFENCE_ID).withAllocationDecision(buildAllocationDecision()).withJudicialResults(of(judicialResult().build())).build()));
        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        resultsAggregate.handleCase(caseDetails);
        resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.TRUE));

        final UUID defendantId = caseDetails.getDefendants().get(0).getDefendantId();
        final List<Object> objectList = resultsAggregate.generatePoliceResults(caseDetails.getCaseId().toString(), defendantId.toString(), Optional.empty()).collect(toList());
        final PoliceResultGenerated policeResultGenerated = objectList.stream().filter(e -> e instanceof PoliceResultGenerated)
                .map(o -> (PoliceResultGenerated) o)
                .findFirst()
                .orElse(null);
        assertThat(policeResultGenerated, notNullValue());
        assertThat(policeResultGenerated.getCaseId(), is(CASE_ID));
        assertThat(policeResultGenerated.getUrn(), is(URN));
        assertThat(policeResultGenerated.getDefendant().getDefendantId(), is(defendantId));
        assertPoliceResultGeneratedEvent(caseDetails.getDefendants().get(0), objectList);

        assertEquals(policeResultGenerated.getCourtCentreWithLJA().getPsaCode(), valueOf(1234));
        assertEquals(policeResultGenerated.getCourtCentreWithLJA().getCourtCentre().getPsaCode(), valueOf(1234));
        assertEquals(policeResultGenerated.getCourtCentreWithLJA().getCourtCentre().getCode(), courtCode);
        assertThat(policeResultGenerated.getCourtCentreWithLJA().getCourtCentre().getLja().getLjaCode(), is("1234"));

    }

    @Test
    public void shouldReturnProsecutionAuthorityCode() {
        final UUID id = randomUUID();
        final String urn = randomAlphanumeric(5);
        final String prosecutionAuthorityCode = randomAlphanumeric(5);
        final CaseDetails caseDetails = caseDetails().withCaseId(id).withUrn(urn).withProsecutionAuthorityCode(prosecutionAuthorityCode).build();
        resultsAggregate.handleCase(caseDetails);
        final Optional<String> result = resultsAggregate.getProsecutionAuthorityCode(id.toString());
        assertThat(result, is(Optional.of(prosecutionAuthorityCode)));
    }

    @Test
    public void shouldNotReturnProsecutionAuthorityCode() {
        final UUID id = randomUUID();
        final Optional<String> result = resultsAggregate.getProsecutionAuthorityCode(id.toString());
        assertThat(result, is(empty()));
    }

    @Test
    public void shouldSendYouthCourtCodeWhenTheHearingIsDoneAsAYouthCourtSession() {
        final CourtCentreWithLJA courtCentre = courtCentreWithLJA()
                .withCourtCentre(courtCentre()
                        .withCode(courtCode)
                        .withLja(LjaDetails.ljaDetails().withLjaCode("123").build())
                        .withPsaCode(987)
                        .build())
                .build();
        final ZonedDateTime sittingDay = now();
        final SessionDay sessionDay = sessionDay().withListedDurationMinutes(10).withListingSequence(15).withSittingDay(sittingDay).build();
        final List<SessionDay> sessionDays = of(sessionDay);

        final List<OffenceDetails> offenceDetailsList = new ArrayList<>();
        offenceDetailsList.add(offenceDetails().withId(OFFENCE_ID).withJudicialResults(of(judicialResult().build())).build());
        final CaseDetails caseDetails = createCaseDetails(null, offenceDetailsList);

        resultsAggregate.apply(hearingResultedForYouthCourt(caseDetails));
        resultsAggregate.handleSession(hearingId, courtCentre, sessionDays);
        resultsAggregate.handleCase(caseDetails);
        final List<Object> objectList = resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.FALSE)).collect(toList());

        final PoliceResultGenerated policeResultGenerated = (PoliceResultGenerated) objectList.get(1);
        assertPoliceResultGeneratedEvent(caseDetails.getDefendants().get(0), objectList);
        assertEquals(policeResultGenerated.getCourtCentreWithLJA().getPsaCode(), valueOf(1234));
        assertEquals(policeResultGenerated.getCourtCentreWithLJA().getCourtCentre().getPsaCode(), valueOf(1234));
        assertEquals(policeResultGenerated.getCourtCentreWithLJA().getCourtCentre().getCode(), courtCode);
        assertThat(policeResultGenerated.getCourtCentreWithLJA().getCourtCentre().getLja().getLjaCode(), is("1234"));
    }

    @Test
    public void shouldSetSharedDateAsSessionDayForBoxWorkHearingInPoliceResultGenerated() {
        final CourtCentreWithLJA courtCentre = courtCentreWithLJA()
                .withCourtCentre(courtCentre()
                        .withCode(courtCode)
                        .withLja(LjaDetails.ljaDetails().withLjaCode("123").build())
                        .withPsaCode(987)
                        .build())
                .build();
        final ZonedDateTime sittingDay = now();
        final SessionDay sessionDay = sessionDay().withListedDurationMinutes(10).withListingSequence(15).withSittingDay(sittingDay).build();
        final List<SessionDay> sessionDays = of(sessionDay);

        final List<OffenceDetails> offenceDetailsList = new ArrayList<>();
        offenceDetailsList.add(offenceDetails().withId(OFFENCE_ID).withJudicialResults(of(judicialResult().build())).build());
        final JudicialResult judicialResult1 = judicialResult().withJudicialResultId(UUID.fromString("e0a49380-71ce-4426-85b6-9bf0e3f9ce1a"))
                .withLevel("FINAL")
                .withIsUnscheduled(false)
                .withIsNewAmendment(true)
                .withLabel("Conditional discharge")
                .build();
        final CaseDetails caseDetails = createCaseDetails(null, offenceDetailsList);

        resultsAggregate.saveHearingResultsForDay(PublicHearingResulted.publicHearingResulted()
                .setSharedTime(ZonedDateTime.now())
                .setHearing(createHearing(caseDetails.getCaseId(), caseDetails.getDefendants().get(0).getDefendantId(), offenceDetailsList.get(0).getId(), judicialResult1)), LocalDate.now());
        resultsAggregate.handleSession(hearingId, courtCentre, sessionDays);
        resultsAggregate.handleCase(caseDetails);
        final List<Object> objectList = resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.FALSE)).collect(toList());

        final PoliceResultGenerated policeResultGenerated = (PoliceResultGenerated) objectList.get(1);
        assertPoliceResultGeneratedEvent(caseDetails.getDefendants().get(0), objectList);
        assertEquals(policeResultGenerated.getCourtCentreWithLJA().getCourtCentre().getPsaCode(), valueOf(987));
        assertEquals(policeResultGenerated.getCourtCentreWithLJA().getCourtCentre().getCode(), courtCode);
        assertThat(policeResultGenerated.getCourtCentreWithLJA().getCourtCentre().getLja().getLjaCode(), is("123"));
        assertEquals(LocalDate.now(), policeResultGenerated.getSessionDays().get(0).getSittingDay().toLocalDate());
    }

    @Test
    public void shouldGenerateHandleApplicationUpdateNotificationEvent() {
        final Stream<Object> objectList = resultsAggregate.handleApplicationUpdateNotification("emailAddress", UUID.fromString("5a783b97-0203-4bd7-9f57-90008364eb35"),
                "urn", "defendant");
        final AppealUpdateNotificationRequested appealUpdateNotificationRequested = (AppealUpdateNotificationRequested) objectList.findFirst().get();
        assertNotNull(appealUpdateNotificationRequested.getNotificationId());
        assertEquals(appealUpdateNotificationRequested.getSubject(), "Appeal Update");
        assertEquals(appealUpdateNotificationRequested.getApplicationId(), "5a783b97-0203-4bd7-9f57-90008364eb35");
        assertEquals(appealUpdateNotificationRequested.getEmailAddress(), "emailAddress");
        assertEquals(appealUpdateNotificationRequested.getUrn(), "urn");
        assertEquals(appealUpdateNotificationRequested.getDefendant(), "defendant");
    }


    @Test
    public void shouldRaisePoliceResultGeneratedEvent_WhenMagsCourtResultFirstTimeForMultiDayHearing() {
        final CourtCentreWithLJA courtCentre = courtCentreWithLJA()
                .withCourtCentre(courtCentre()
                        .withCode(courtCode)
                        .withLja(LjaDetails.ljaDetails().withLjaCode("123").build())
                        .withPsaCode(987)
                        .build())
                .build();
        final ZonedDateTime sittingDay = now();
        final SessionDay sessionDay = sessionDay().withListedDurationMinutes(10).withListingSequence(15).withSittingDay(sittingDay).build();
        final List<SessionDay> sessionDays = of(sessionDay);

        final List<OffenceDetails> offenceDetailsList = new ArrayList<>();
        offenceDetailsList.add(offenceDetails().withId(OFFENCE_ID).withJudicialResults(of(judicialResult().build())).build());
        final JudicialResult judicialResult1 = judicialResult().withJudicialResultId(UUID.fromString("e0a49380-71ce-4426-85b6-9bf0e3f9ce1a"))
                .withLevel("FINAL")
                .withIsUnscheduled(false)
                .withIsNewAmendment(true)
                .withLabel("Conditional discharge")
                .build();
        final CaseDetails caseDetails = createCaseDetails(null, offenceDetailsList);

        resultsAggregate.saveHearingResultsForDay(PublicHearingResulted.publicHearingResulted()
                .setSharedTime(ZonedDateTime.now())
                .setIsReshare(Optional.of(Boolean.FALSE))
                .setHearing(createMultiDayHearing(caseDetails.getCaseId(), caseDetails.getDefendants().get(0).getDefendantId(), offenceDetailsList.get(0).getId(), judicialResult1)), LocalDate.of(2018, 2, 2));
        resultsAggregate.handleSession(hearingId, courtCentre, sessionDays);
        resultsAggregate.handleCase(caseDetails);
        List<Object> objectList = resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.FALSE)).collect(toList());
        final PoliceResultGenerated policeResultGenerated = (PoliceResultGenerated) objectList.get(1);
        assertPoliceResultGeneratedEvent(caseDetails.getDefendants().get(0), objectList);
        assertEquals(policeResultGenerated.getCourtCentreWithLJA().getCourtCentre().getPsaCode(), valueOf(987));
        assertEquals(policeResultGenerated.getCourtCentreWithLJA().getCourtCentre().getCode(), courtCode);
        assertThat(policeResultGenerated.getCourtCentreWithLJA().getCourtCentre().getLja().getLjaCode(), is("123"));
        assertEquals(LocalDate.now(), policeResultGenerated.getSessionDays().get(0).getSittingDay().toLocalDate());

        resultsAggregate.saveHearingResultsForDay(PublicHearingResulted.publicHearingResulted()
                .setSharedTime(ZonedDateTime.now())
                .setIsReshare(Optional.of(Boolean.FALSE))
                .setHearing(createMultiDayHearing(caseDetails.getCaseId(), caseDetails.getDefendants().get(0).getDefendantId(), offenceDetailsList.get(0).getId(), judicialResult1)), LocalDate.of(2018, 4, 3));
        resultsAggregate.handleSession(hearingId, courtCentre, sessionDays);
        resultsAggregate.handleCase(caseDetails);
        objectList = resultsAggregate.handleDefendants(caseDetails, true, Optional.of(JurisdictionType.MAGISTRATES), EMAIL_ADDRESS, true, Optional.empty(), "", "", Optional.of(Boolean.FALSE)).collect(toList());
        final PoliceResultGenerated policeResultGenerated1 = (PoliceResultGenerated) objectList.get(0);
        assertPoliceResultGeneratedEvent(caseDetails.getDefendants().get(0), objectList);
        assertEquals(policeResultGenerated1.getCourtCentreWithLJA().getCourtCentre().getPsaCode(), valueOf(987));
        assertEquals(policeResultGenerated1.getCourtCentreWithLJA().getCourtCentre().getCode(), courtCode);
        assertThat(policeResultGenerated1.getCourtCentreWithLJA().getCourtCentre().getLja().getLjaCode(), is("123"));
        assertEquals(LocalDate.now(), policeResultGenerated1.getSessionDays().get(0).getSittingDay().toLocalDate());
    }

    private Hearing createHearing(UUID caseId, UUID defendantId, UUID offenceId, JudicialResult judicialResult) {
        return hearing().withIsBoxHearing(true).withProsecutionCases(Arrays.asList(
                ProsecutionCase.prosecutionCase()
                        .withId(caseId)
                        .withDefendants(singletonList(Defendant.defendant()
                                .withId(defendantId)
                                .withPersonDefendant(PersonDefendant.personDefendant()
                                        .withPersonDetails(Person.person().withFirstName(FIRST_NAME).withLastName(LAST_NAME).build())
                                        .build())
                                .withOffences(singletonList(Offence.offence()
                                        .withId(offenceId)
                                        .withJudicialResults(singletonList(judicialResult))
                                        .build()))
                                .build()))
                        .build()
        )).build();
    }

    private Hearing createMultiDayHearing(UUID caseId, UUID defendantId, UUID offenceId, JudicialResult judicialResult) {
        final List<HearingDay> hearingDays = new ArrayList<>();
        hearingDays.add(HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 2, 2), LocalTime.of(12, 1, 1), ZoneId.of("UTC")))
                .withListedDurationMinutes(100)
                .withListingSequence(10)
                .build());
        hearingDays.add(HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 4, 3), LocalTime.of(12, 0), ZoneId.of("UTC")))
                .withListedDurationMinutes(100)
                .withListingSequence(10)
                .build());
        hearingDays.add(HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 5, 2), LocalTime.of(12, 0), ZoneId.of("UTC")))
                .withListedDurationMinutes(100)
                .withListingSequence(10)
                .build());
        return hearing().withHearingDays(hearingDays).withProsecutionCases(Arrays.asList(
                ProsecutionCase.prosecutionCase()
                        .withId(caseId)
                        .withDefendants(singletonList(Defendant.defendant()
                                .withId(defendantId)
                                .withPersonDefendant(PersonDefendant.personDefendant()
                                        .withPersonDetails(Person.person().withFirstName(FIRST_NAME).withLastName(LAST_NAME).build())
                                        .build())
                                .withOffences(singletonList(Offence.offence()
                                        .withId(offenceId)
                                        .withJudicialResults(singletonList(judicialResult))
                                        .build()))
                                .build()))
                        .build()
        )).build();
    }

    private CaseDetails createCaseDetails(final List<JudicialResult> defendantResults, final List<OffenceDetails> offenceDetailsList) {
        final CaseDefendant caseDefendant = caseDefendant()
                .withDefendantId(DEFENDANT_ID)
                .withProsecutorReference("prosecutorReference")
                .withJudicialResults(defendantResults)
                .withIndividualDefendant(individualDefendant()
                        .withBailConditions("bailCondition")
                        .withBailStatus(bailStatus().withCode("Bail status code").withDescription("Bail status description").withId(randomUUID()).build())
                        .withPerson(createPerson(FIRST_NAME, LAST_NAME, GENDER, DATE_OF_BIRTH, TITLE, ADDRESS, CONTACT_NUMBER)).build())
                .withAssociatedPerson(of(associatedIndividual().withPerson(createPerson(FIRST_NAME_1, LAST_NAME_1, GENDER_1, DATE_OF_BIRTH_1, TITLE_1, ADDRESS_1, CONTACT_NUMBER_1)).withRole("role").build()))
                .withOffences(offenceDetailsList)
                .build();
        final List<CaseDefendant> caseDefendants = of(caseDefendant);
        return caseDetails().withCaseId(CASE_ID).withUrn(URN).withDefendants(caseDefendants).withProsecutionAuthorityCode("").build();
    }

    private void assertDefendantAddedEvent(final CaseDefendant caseDefendant, final List<Object> objectList) {
        final DefendantAddedEvent defendantAddedEvent = objectList.stream().filter(e -> e instanceof DefendantAddedEvent)
                .map(o -> (DefendantAddedEvent) o)
                .findFirst()
                .orElse(null);
        assertNotNull(defendantAddedEvent);
        assertEquals(CASE_ID, defendantAddedEvent.getCaseId());
        assertEquals(caseDefendant, defendantAddedEvent.getDefendant());
    }

    private void assertDefendantUpdatedEvent(final CaseDefendant caseDefendant, final List<Object> objectList) {
        final DefendantUpdatedEvent defendantUpdatedEvent = objectList.stream().filter(e -> e instanceof DefendantUpdatedEvent)
                .map(o -> (DefendantUpdatedEvent) o)
                .findFirst()
                .orElse(null);
        assertNotNull(defendantUpdatedEvent);
        assertEquals(CASE_ID, defendantUpdatedEvent.getCaseId());
        assertEquals(caseDefendant, defendantUpdatedEvent.getDefendant());
    }

    private void assertPoliceResultGeneratedEvent(final CaseDefendant caseDefendant, final List<Object> objectList) {
        final PoliceResultGenerated policeResultGenerated = objectList.stream().filter(e -> e instanceof PoliceResultGenerated)
                .map(o -> (PoliceResultGenerated) o)
                .findFirst()
                .orElse(null);
        assertNotNull(policeResultGenerated);
        assertEquals(CASE_ID, policeResultGenerated.getCaseId());
        assertEquals(URN, policeResultGenerated.getUrn());
        assertEquals(caseDefendant, policeResultGenerated.getDefendant());
        assertAllocationDecision(caseDefendant, policeResultGenerated);
    }

    private void assertResultIsAmended(final List<Object> objectList) {
        final PoliceNotificationRequestedV2 policeNotificationRequestedV2 = objectList.stream().filter(e -> e instanceof PoliceNotificationRequestedV2)
                .map(o -> (PoliceNotificationRequestedV2) o)
                .findFirst()
                .orElse(null);
        assertNotNull(policeNotificationRequestedV2.getCaseResultDetails());

        assertThat(policeNotificationRequestedV2.getCaseResultDetails().getDefendantResultDetails().size(), is(1));
        final DefendantResultDetails defendantResultDetails = policeNotificationRequestedV2.getCaseResultDetails().getDefendantResultDetails().get(0);
        assertThat(defendantResultDetails.getOffenceResultDetails().size(), is(1));
        assertThat(defendantResultDetails.getOffenceResultDetails().get(0).getJudicialResultDetails().size(), is(1));
        assertThat(defendantResultDetails.getOffenceResultDetails().get(0).getJudicialResultDetails().get(0).getAmendmentType(), is(AmendmentType.UPDATED));
    }

    private void assertPoliceNotificationRequestedV2Event(final CaseDefendant caseDefendant, final List<Object> objectList) {
        final PoliceNotificationRequestedV2 policeNotificationRequestedV2 = objectList.stream().filter(e -> e instanceof PoliceNotificationRequestedV2)
                .map(o -> (PoliceNotificationRequestedV2) o)
                .findFirst()
                .orElse(null);
        assertNotNull(policeNotificationRequestedV2);
        assertEquals(CASE_ID.toString(), policeNotificationRequestedV2.getCaseId());
        assertEquals(URN, policeNotificationRequestedV2.getUrn());
        assertTrue(policeNotificationRequestedV2.getCaseDefendants().contains(caseDefendant));
    }

    private void assertNoPoliceNotificationRequestedV2Event(final CaseDefendant caseDefendant, final List<Object> objectList) {
        final PoliceNotificationRequestedV2 policeNotificationRequestedV2 = objectList.stream().filter(e -> e instanceof PoliceNotificationRequestedV2)
                .map(o -> (PoliceNotificationRequestedV2) o)
                .findFirst()
                .orElse(null);
        assertNull(policeNotificationRequestedV2);
    }

    private void assertNoPoliceResultGeneratedEvent(final CaseDefendant caseDefendant, final List<Object> objectList) {
        final PoliceResultGenerated policeResultGenerated = objectList.stream().filter(e -> e instanceof PoliceResultGenerated)
                .map(o -> (PoliceResultGenerated) o)
                .findFirst()
                .orElse(null);
        assertNull(policeResultGenerated);
    }

    private void assertAllocationDecision(final CaseDefendant caseDefendant, final PoliceResultGenerated policeResultGenerated) {
        final OffenceDetails offenceDetails = caseDefendant.getOffences().stream().findFirst().isPresent() ? caseDefendant.getOffences().stream().findFirst().get() : null;
        if (nonNull(offenceDetails) && nonNull(offenceDetails.getAllocationDecision())) {
            assertThat(offenceDetails.getAllocationDecision().getMotReasonDescription(), is(policeResultGenerated.getDefendant().getOffences().get(0).getAllocationDecision().getMotReasonDescription()));
            assertThat(offenceDetails.getAllocationDecision().getAllocationDecisionDate(), is(policeResultGenerated.getDefendant().getOffences().get(0).getAllocationDecision().getAllocationDecisionDate()));
            assertThat(offenceDetails.getAllocationDecision().getMotReasonCode(), is(policeResultGenerated.getDefendant().getOffences().get(0).getAllocationDecision().getMotReasonCode()));
            assertThat(offenceDetails.getAllocationDecision().getMotReasonId(), is(policeResultGenerated.getDefendant().getOffences().get(0).getAllocationDecision().getMotReasonId()));
            assertThat(offenceDetails.getAllocationDecision().getOffenceId(), is(policeResultGenerated.getDefendant().getOffences().get(0).getAllocationDecision().getOffenceId()));
            assertThat(offenceDetails.getAllocationDecision().getOriginatingHearingId(), is(policeResultGenerated.getDefendant().getOffences().get(0).getAllocationDecision().getOriginatingHearingId()));
            if (nonNull(offenceDetails.getAllocationDecision().getCourtIndicatedSentence())) {
                assertThat(offenceDetails.getAllocationDecision().getCourtIndicatedSentence().getCourtIndicatedSentenceTypeId(),
                        is(policeResultGenerated.getDefendant().getOffences().get(0).getAllocationDecision().getCourtIndicatedSentence().getCourtIndicatedSentenceTypeId()));
                assertThat(offenceDetails.getAllocationDecision().getCourtIndicatedSentence().getCourtIndicatedSentenceDescription(),
                        is(policeResultGenerated.getDefendant().getOffences().get(0).getAllocationDecision().getCourtIndicatedSentence().getCourtIndicatedSentenceDescription()));
            }
        }
    }

    private Individual createPerson(final String firstName, final String lastName, final Gender gender, final LocalDate dateOfBirth, final String title, final Address address, final ContactNumber contactNumber) {
        return individual()
                .withFirstName(firstName)
                .withLastName(lastName)
                .withGender(gender)
                .withDateOfBirth(dateOfBirth)
                .withTitle(title)
                .withAddress(address)
                .withContact(contactNumber)
                .build();
    }

    private AllocationDecision buildAllocationDecision() {
        return allocationDecision()
                .withAllocationDecisionDate(LocalDate.of(2019, 10, 1))
                .withMotReasonCode("motReasonCode")
                .withSequenceNumber(1)
                .withOriginatingHearingId(randomUUID())
                .withMotReasonId(randomUUID())
                .withMotReasonDescription("motReasonDescription")
                .withOffenceId(OFFENCE_ID)
                .withCourtIndicatedSentence(courtIndicatedSentence()
                        .withCourtIndicatedSentenceTypeId(randomUUID())
                        .withCourtIndicatedSentenceDescription("description")
                        .build())
                .build();
    }

    private HearingResultsAddedForDay hearingResultsAddedForDay(CaseDetails caseDetails) {
        return HearingResultsAddedForDay.hearingResultsAddedForDay()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withProsecutionCases(Arrays.asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(caseDetails.getCaseId())
                                        .withDefendants(
                                                caseDetails.getDefendants().stream()
                                                        .map(d -> Defendant.defendant()
                                                                .withId(d.getDefendantId())
                                                                .withPersonDefendant(PersonDefendant.personDefendant()
                                                                        .withPersonDetails(Person.person()
                                                                                .withFirstName(d.getIndividualDefendant().getPerson().getFirstName())
                                                                                .withLastName(d.getIndividualDefendant().getPerson().getLastName())
                                                                                .build())
                                                                        .build())
                                                                .withOffences(
                                                                        d.getOffences().stream()
                                                                                .map(o -> Offence.offence()
                                                                                        .withId(o.getId())
                                                                                        .withJudicialResults(o.getJudicialResults())
                                                                                        .build())
                                                                                .collect(toList())
                                                                )
                                                                .build())
                                                        .collect(toList())
                                        )
                                        .build()
                        ))
                        .build())
                .build();
    }

    private HearingResultsAdded hearingResultedForYouthCourt(CaseDetails caseDetails) {
        return HearingResultsAdded.hearingResultsAdded()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withYouthCourt(youthCourt()
                                .withCourtCode(1234)
                                .build())
                        .withYouthCourtDefendantIds(singletonList(DEFENDANT_ID))
                        .withProsecutionCases(Arrays.asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(caseDetails.getCaseId())
                                        .withDefendants(
                                                caseDetails.getDefendants().stream()
                                                        .map(d -> Defendant.defendant()
                                                                .withId(d.getDefendantId())
                                                                .withPersonDefendant(PersonDefendant.personDefendant()
                                                                        .withPersonDetails(Person.person()
                                                                                .withFirstName(d.getIndividualDefendant().getPerson().getFirstName())
                                                                                .withLastName(d.getIndividualDefendant().getPerson().getLastName())
                                                                                .build())
                                                                        .build())
                                                                .withOffences(
                                                                        d.getOffences().stream()
                                                                                .map(o -> Offence.offence()
                                                                                        .withId(o.getId())
                                                                                        .withJudicialResults(o.getJudicialResults())
                                                                                        .build())
                                                                                .collect(toList())
                                                                )
                                                                .build())
                                                        .collect(toList())
                                        )
                                        .build()
                        ))
                        .build())
                .build();
    }

}
