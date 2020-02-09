package uk.gov.moj.cpp.results.test;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.ApplicationJurisdictionType;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtIndicatedSentence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.moj.cpp.domains.JudicialRoleTypeEnum;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

public class TestTemplates {

    private TestTemplates() {

    }

    public static Hearing basicHearingTemplate(final UUID hearingId) {
        return Hearing.hearing()
                .withId(hearingId)
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Trial")
                        .build())
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withLinkedCaseId(UUID.fromString("cccc1111-1e20-4c21-916a-81a6c90239e5"))
                        .withType(courtApplicationTypeTemplates())
                        .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                        .withApplicant(courtApplicationPartyTemplates())
                        .withApplicationStatus(ApplicationStatus.DRAFT)
                        .build()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(circuitJudge())
                        .build()))
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 6, 4), LocalTime.of(12, 0), ZoneId.of("UTC")))
                        .withListedDurationMinutes(100)
                        .build(), HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 4, 3), LocalTime.of(12, 0), ZoneId.of("UTC")))
                        .withListedDurationMinutes(100)
                        .build(), HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 2, 2), LocalTime.of(12, 0), ZoneId.of("UTC")))
                        .withListedDurationMinutes(100)
                        .build()))
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withName(STRING.next())
                        .withRoomId(randomUUID())
                        .withRoomName(STRING.next())
                        .withWelshName(STRING.next())
                        .withWelshRoomName(STRING.next())
                        .withAddress(address())
                        .build())
                .withProsecutionCases(Arrays.asList(createProsecutionCase1(), createProsecutionCase2()))
                .build();
    }

    public static Address address(){
        return Address.address().withAddress1(STRING.next())
                .withAddress2(STRING.next())
                .withAddress3(STRING.next())
                .withAddress4(STRING.next())
                .withAddress5(STRING.next())
                .withPostcode("AA1 1AA").build();
    }
    public static JudicialRoleType circuitJudge() {
        return JudicialRoleType.judicialRoleType()
                .withJudicialRoleTypeId(UUID.randomUUID())
                .withJudiciaryType(JudicialRoleTypeEnum.CIRCUIT_JUDGE.name()).build();
    }

    public static PublicHearingResulted basicShareResultsTemplate() {

        final UUID hearingId = randomUUID();

        return PublicHearingResulted.publicHearingResulted()
                .setHearing(basicHearingTemplate(hearingId))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    protected static ProsecutionCase createProsecutionCase1() {
        return ProsecutionCase.prosecutionCase()
                .withId(UUID.fromString("cccc1111-1e20-4c21-916a-81a6c90239e5"))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next())
                        .withProsecutionAuthorityReference(STRING.next())
                        .build())
                .withDefendants(
                        Arrays.asList(createDefendant("dddd1111-1e20-4c21-916a-81a6c90239e5"),
                                createDefendant("dddd2222-1e20-4c21-916a-81a6c90239e5")
                        ))
                .withCaseStatus("ACTIVE")
                .build();
    }

    protected static ProsecutionCase createProsecutionCase2() {
        return ProsecutionCase.prosecutionCase()
                .withId(UUID.fromString("cccc2222-1e20-4c21-916a-81a6c90239e5"))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next())
                        .withCaseURN(STRING.next())
                        .build())
                .withDefendants(
                        Arrays.asList(createDefendant("dddd3333-1e20-4c21-916a-81a6c90239e5"),
                                createDefendant("dddd4444-1e20-4c21-916a-81a6c90239e5")
                        ))
                .withCaseStatus("ACTIVE")
                .build();
    }

    private static Defendant createDefendant(String defendantId) {
        return Defendant.defendant()
                .withId(UUID.fromString(defendantId))
                .withProsecutionCaseId(randomUUID())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withFirstName(STRING.next())
                                .withLastName(STRING.next())
                                .withTitle("Baroness")
                                .withGender(Gender.NOT_KNOWN)
                                .build())
                        .build())
                .withOffences(Arrays.asList(Offence.offence()
                        .withId(randomUUID())
                        .withOffenceDefinitionId(randomUUID())
                        .withOffenceCode(STRING.next())
                        .withOffenceTitle(STRING.next())
                        .withWording(STRING.next())
                        .withStartDate(LocalDate.now())
                        .withAllocationDecision(createAllocationDecision())
                        .withOrderIndex(65)
                        .withCount(434)
                        .withProceedingsConcluded(true)
                        .withIsIntroduceAfterInitialProceedings(true)
                        .withIsDiscontinued(true)
                        .build()))
                .build();
    }

    private static AllocationDecision createAllocationDecision() {
        return AllocationDecision.allocationDecision()
                .withAllocationDecisionDate(PAST_LOCAL_DATE.next())
                .withCourtIndicatedSentence(createCourtIndicatedSentence())
                .withMotReasonId(randomUUID())
                .withMotReasonCode(STRING.next())
                .withMotReasonDescription(STRING.next())
                .withOffenceId(randomUUID())
                .withOriginatingHearingId(randomUUID())
                .withSequenceNumber(1)
                .build();
    }

    private static CourtIndicatedSentence createCourtIndicatedSentence() {
        return CourtIndicatedSentence.courtIndicatedSentence()
                .withCourtIndicatedSentenceDescription(STRING.next())
                .withCourtIndicatedSentenceTypeId(randomUUID())
                .build();
    }

    private static CourtApplicationType courtApplicationTypeTemplates() {
        return CourtApplicationType.courtApplicationType()
                .withId(randomUUID())
                .withApplicationCode(STRING.next())
                .withApplicationType(STRING.next())
                .withApplicationLegislation(STRING.next())
                .withApplicationCategory(STRING.next())
                .withLinkType(LinkType.LINKED)
                .withApplicationJurisdictionType(ApplicationJurisdictionType.CROWN)
                .build();
    }

    private static CourtApplicationParty courtApplicationPartyTemplates() {
        return CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .build();
    }
}
