package uk.gov.moj.cpp.results.test;

import static com.google.common.collect.ImmutableList.of;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.core.courts.AllocationDecision.allocationDecision;
import static uk.gov.justice.core.courts.AssociatedPerson.associatedPerson;
import static uk.gov.justice.core.courts.AttendanceDay.attendanceDay;
import static uk.gov.justice.core.courts.BailStatus.bailStatus;
import static uk.gov.justice.core.courts.CourtIndicatedSentence.courtIndicatedSentence;
import static uk.gov.justice.core.courts.DefenceCounsel.defenceCounsel;
import static uk.gov.justice.core.courts.DefendantAttendance.defendantAttendance;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.sjp.results.BaseOffense.baseOffense;
import static uk.gov.justice.sjp.results.BasePersonDetail.basePersonDetail;
import static uk.gov.justice.sjp.results.BaseResult.baseResult;
import static uk.gov.justice.sjp.results.BaseSessionStructure.baseSessionStructure;
import static uk.gov.justice.sjp.results.CaseDefendant.caseDefendant;
import static uk.gov.justice.sjp.results.CaseDetails.caseDetails;
import static uk.gov.justice.sjp.results.CaseOffence.caseOffence;
import static uk.gov.justice.sjp.results.IndividualDefendant.individualDefendant;
import static uk.gov.justice.sjp.results.Prompts.prompts;
import static uk.gov.justice.sjp.results.SessionLocation.sessionLocation;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.ApplicationJurisdictionType;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.Category;
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
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.sjp.results.BaseResult;
import uk.gov.justice.sjp.results.BaseSessionStructure;
import uk.gov.justice.sjp.results.CaseDefendant;
import uk.gov.justice.sjp.results.Plea;
import uk.gov.justice.sjp.results.PleaMethod;
import uk.gov.justice.sjp.results.PleaType;
import uk.gov.justice.sjp.results.PublicSjpResulted;
import uk.gov.moj.cpp.domains.JudicialRoleTypeEnum;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableList;

public class TestTemplates {

    private static final UUID DEFAULT_DEFENDANT_ID1 = fromString("dddd1111-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID2 = fromString("dddd2222-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID3 = fromString("dddd3333-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID4 = fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");
    private static final UUID ID = randomUUID();
    private static final UUID NATIONALITY_ID = fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");
    private static final UUID PROMPT_ID = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6741d");
    private static final UUID PROMPT_ID_1 = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6742d");
    private static final UUID RESULT_ID = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6743d");
    public static final String DEFAULT_VALUE = "DEFAULT_VALUE";
    public static final String REASON = "reason";
    public static final String FIELD_NAME_SMITH = "Smith";
    public static final String OFFENCE_WORDING = "offenceWording";
    public static final String LABEL = "label";


    private TestTemplates() {

    }

    public static Hearing basicShareHearingTemplate(final UUID hearingId, final List<ProsecutionCase> prosecutionCases) {
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
                .withDefendantAttendance(of(
                        defendantAttendance()
                                .withAttendanceDays(of(attendanceDay().withDay(LocalDate.of(2018, 5, 2)).withAttendanceType(AttendanceType.IN_PERSON).build()))
                                .withDefendantId(DEFAULT_DEFENDANT_ID1)
                                .build(),
                        defendantAttendance()
                                .withAttendanceDays(of(attendanceDay().withDay(LocalDate.of(2019, 1, 2)).withAttendanceType(AttendanceType.NOT_PRESENT).build()))
                                .withDefendantId(DEFAULT_DEFENDANT_ID2)
                                .build(),
                        defendantAttendance()
                                .withAttendanceDays(of(attendanceDay().withDay(LocalDate.of(2018, 5, 2)).withAttendanceType(AttendanceType.NOT_PRESENT).build()))
                                .withDefendantId(DEFAULT_DEFENDANT_ID3)
                                .build(),
                        defendantAttendance()
                                .withAttendanceDays(of(attendanceDay().withDay(LocalDate.of(2018, 5, 2)).withAttendanceType(AttendanceType.IN_PERSON).build()))
                                .withDefendantId(DEFAULT_DEFENDANT_ID4)
                                .build()
                ))
                .withDefenceCounsels(of(defenceCounsel().withId(randomUUID()).withAttendanceDays(of(LocalDate.now())).withDefendants(of(randomUUID(), randomUUID(), DEFAULT_DEFENDANT_ID1)).build(),
                        defenceCounsel().withId(randomUUID()).withAttendanceDays(of(LocalDate.of(2018, 5, 2))).withDefendants(of(randomUUID(), DEFAULT_DEFENDANT_ID3, randomUUID())).build()))
                .withCourtApplications(asList(CourtApplication.courtApplication()
                        .withId(fromString("f8254db1-1683-483e-afb3-b87fde5a0a26"))
                        .withType(courtApplicationTypeTemplates())
                        .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                        .withApplicant(courtApplicationPartyTemplates())
                        .withApplicationStatus(ApplicationStatus.DRAFT)
                        .build()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withJudiciary(asList(JudicialRole.judicialRole()
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(circuitJudge())
                        .build()))
                .withHearingDays(asList(HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 5, 2), LocalTime.of(12, 1, 1), ZoneId.systemDefault()))
                        .withListedDurationMinutes(100)
                        .withListingSequence(10)
                        .build(), HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 4, 3), LocalTime.of(12, 0), ZoneId.of("UTC")))
                        .withListedDurationMinutes(100)
                        .withListingSequence(10)
                        .build(), HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 2, 2), LocalTime.of(12, 0), ZoneId.of("UTC")))
                        .withListedDurationMinutes(100)
                        .withListingSequence(10)
                        .build()))
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withName("courtName")
                        .withRoomId(randomUUID())
                        .withRoomName(STRING.next())
                        .withWelshName(STRING.next())
                        .withWelshRoomName(STRING.next())
                        .withAddress(address())
                        .build())
                .withProsecutionCases(Arrays.asList(createProsecutionCase1(null), createProsecutionCase2(null)))
                .withProsecutionCases(prosecutionCases)
                .build();
    }

    public static PublicHearingResulted sharedResultTemplateWithTwoOffences() {

        final UUID hearingId = randomUUID();

        return PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplate(hearingId,
                        asList(createCaseWithDefendantAndOffenceLevelJudicialResults(buildJudicialResultList()))))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

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
                .setHearing(basicShareHearingTemplate(hearingId, asList(createProsecutionCase1(buildJudicialResultList()), createProsecutionCase2(buildJudicialResultList()))))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

    }

    public static PublicHearingResulted basicShareResultsTemplateWithoutResult() {

        final UUID hearingId = randomUUID();

        return PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplate(hearingId, asList(createProsecutionCase1(null), createProsecutionCase2(null))))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

    }

    private static ProsecutionCase createProsecutionCase1(final List<JudicialResult> judicialResults) {
        return ProsecutionCase.prosecutionCase()
                .withId(fromString("cccc1111-1e20-4c21-916a-81a6c90239e5"))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next())
                        .withProsecutionAuthorityReference("authorityReference")
                        .build())
                .withDefendants(
                        asList(createDefendant(DEFAULT_DEFENDANT_ID1.toString(), null, judicialResults),
                                createDefendant(DEFAULT_DEFENDANT_ID2.toString(), DEFAULT_VALUE, judicialResults)
                        ))
                .withCaseStatus("ACTIVE")
                .build();
    }

    protected static ProsecutionCase createProsecutionCase2(final List<JudicialResult> judicialResults) {
        return ProsecutionCase.prosecutionCase()
                .withId(fromString("cccc2222-1e20-4c21-916a-81a6c90239e5"))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next())
                        .withCaseURN(STRING.next())
                        .build())
                .withDefendants(
                        asList(createDefendant(DEFAULT_DEFENDANT_ID3.toString(), DEFAULT_VALUE, judicialResults),
                                createDefendant(DEFAULT_DEFENDANT_ID4.toString(), null, judicialResults)
                        ))
                .withCaseStatus("ACTIVE")
                .build();
    }

    private static Defendant createDefendant(final String defendantId, final String prosecutionAuthorityReference, final List<JudicialResult> judicialResults) {
        return Defendant.defendant()
                .withId(fromString(defendantId))
                .withProsecutionCaseId(randomUUID())
                .withProsecutionAuthorityReference(prosecutionAuthorityReference)
                .withPncId("pncId")
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withBailStatus(bailStatus().withCode("Bail status code").withDescription("Bail status description").withId(randomUUID()).build())
                        .withBailReasons(REASON)
                        .withPersonDetails(person()
                                .withFirstName("John")
                                .withLastName(FIELD_NAME_SMITH)
                                .withTitle("Baroness")
                                .withGender(Gender.NOT_KNOWN)
                                .withNationalityId(NATIONALITY_ID)
                                .build())
                        .build())
                .withAssociatedPersons(of(associatedPerson()
                        .withPerson(person()
                                .withFirstName("Tina")
                                .withLastName(FIELD_NAME_SMITH)
                                .withTitle("Baroness")
                                .withGender(Gender.MALE)
                                .withNationalityId(NATIONALITY_ID)
                                .build())
                        .withRole("parentGuardian")
                        .build()))
                .withOffences(asList(Offence.offence()
                        .withId(ID)
                        .withOffenceDefinitionId(randomUUID())
                        .withOffenceCode("offenceCode")
                        .withOffenceTitle(STRING.next())
                        .withWording(STRING.next())
                        .withStartDate(now())
                        .withEndDate(now())
                        .withArrestDate(now())
                        .withChargeDate(now())
                        .withConvictionDate(now())
                        .withEndDate(now())
                        .withModeOfTrial("1010")
                        .withAllocationDecision(createAllocationDecision())
                        .withJudicialResults(judicialResults)
                        .withOrderIndex(12)
                        .withOrderIndex(65)
                        .withIsDisposed(true)
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

    private static ImmutableList<JudicialResult> buildJudicialResultList() {
        return of(judicialResult()
                .withJudicialResultId(randomUUID())
                .withCategory(Category.FINAL)
                .withCjsCode("cjsCode")
                .withIsAdjournmentResult(false)
                .withIsAvailableForCourtExtract(false)
                .withIsConvictedResult(false)
                .withIsFinancialResult(false)
                .withLabel(LABEL)
                .withOrderedHearingId(ID)
                .withOrderedDate(now())
                .withRank(BigDecimal.ZERO)
                .withWelshLabel(LABEL)
                .withResultText("resultText")
                .withLifeDuration(false)
                .withTerminatesOffenceProceedings(Boolean.FALSE)
                .withLifeDuration(false)
                .withPublishedAsAPrompt(false)
                .withExcludedFromResults(false)
                .withAlwaysPublished(false)
                .withUrgent(false)
                .withD20(false)
                .withJudicialResultTypeId(randomUUID())
                .build());
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

    public static PublicSjpResulted basicSJPCaseResulted() {

        return PublicSjpResulted.publicSjpResulted()
                .withSession(createbaseSessionStructure())
                .withCases(createCaseDetails()).build();
    }

    private static List<uk.gov.justice.sjp.results.CaseDetails> createCaseDetails() {
        final List<uk.gov.justice.sjp.results.CaseDetails> caseDetails = new ArrayList<>();
        caseDetails.add(caseDetails().
                withCaseId(randomUUID())
                .withUrn("123456789")
                .withDefendants(asList(buildSjpDefendant(DEFAULT_DEFENDANT_ID1.toString()),
                        buildSjpDefendant(DEFAULT_DEFENDANT_ID2.toString())
                ))
                .withProsecutionAuthorityCode("someProsecutingAuthority")
                .build());
        return caseDetails;
    }

    private static CaseDefendant buildSjpDefendant(final String defendantId) {
        return caseDefendant().withDefendantId(fromString(defendantId))
                .withIndividualDefendant(individualDefendant()
                        .withBailConditions("22222222222")
                        .withBailStatus("33333333333")
                        .withBasePersonDetails(basePersonDetail()
                                .withAddress(buildAddress())
                                .withBirthDate(ZonedDateTime.of(LocalDate.of(2019, 2, 2), LocalTime.of(12, 3, 10), ZoneId.systemDefault()))
                                .withEmailAddress1("somerandomemail1@random.random")
                                .withEmailAddress2("somerandomemail2@random.random")
                                .withFirstName("ParentGuardianFirstName")
                                .withGender(uk.gov.justice.sjp.results.Gender.MALE)
                                .withLastName("ParentGuardianLastName")
                                .withTelephoneNumberBusiness("99999999999")
                                .withTelephoneNumberHome("88888888888")
                                .withTelephoneNumberMobile("77777777777")
                                .build())
                        .withPersonStatedNationality("UK")
                        .withPncIdentifier("")
                        .withPresentAtHearing(Boolean.valueOf("hearing"))
                        .withReasonForBailConditionsOrCustody(REASON)
                        .build())
                .withParentGuardianDetails(basePersonDetail()
                        .withAddress(buildAddress())
                        .withBirthDate(ZonedDateTime.of(LocalDate.of(2019, 2, 2), LocalTime.of(12, 3, 10), ZoneId.systemDefault()))
                        .withEmailAddress1("parentguardianmemail1@random.random")
                        .withEmailAddress2("parentguardianemail2@random.random")
                        .withFirstName("ParentGuardianFirstName")
                        .withGender(uk.gov.justice.sjp.results.Gender.MALE)
                        .withLastName("ParentGuardianLastName")
                        .withPersonTitle("Ms")
                        .withTelephoneNumberBusiness("6666666666")
                        .withTelephoneNumberHome("77777777777")
                        .withTelephoneNumberMobile("8888888888")
                        .build())
                .withProsecutorReference("")
                .withOffences(asList(caseOffence()
                        .withResults(Collections.emptyList())
                        .withPlea(Plea.plea()
                                .withPleaDate(ZonedDateTime.of(LocalDate.of(2019, 3, 2), LocalTime.of(12, 3, 10), ZoneId.systemDefault()))
                                .withPleaMethod(PleaMethod.POSTAL)
                                .withPleaType(PleaType.GUILTY)
                                .build())
                        .withModeOfTrial(1)
                        .withFinding("finding")
                        .withBaseOffenceDetails(baseOffense()
                                .withAlcoholLevelAmount(300)
                                .withAlcoholLevelMethod("Breath Test")
                                .withArrestDate(LocalDate.of(2019, 1, 2))
                                .withChargeDate(LocalDate.of(2019, 2, 2))
                                .withLocationOfOffence("Gypsy Corner")
                                .withOffenceCode("61131")
                                .withOffenceDateCode(2018 - 10 - 10)
                                .withOffenceEndDate(LocalDate.of(2019, 4, 2))
                                .withOffenceId(fromString("aa746921-d839-4867-bcf9-b41db8ebc852"))
                                .withOffenceSequenceNumber(1)
                                .withOffenceStartDate(LocalDate.of(2018, 1, 2))
                                .withOffenceWording("wording")
                                .withVehicleCode("12345")
                                .withVehicleRegistrationMark("OF01ENC")
                                .build()) //baseOffence
                        .withConvictingCourt(2)
                        .withConvictionDate(ZonedDateTime.of(LocalDate.of(2018, 5, 2), LocalTime.of(12, 3, 10), ZoneId.systemDefault()))
                        .withInitiatedDate(ZonedDateTime.of(LocalDate.of(2018, 2, 2), LocalTime.of(12, 3, 10), ZoneId.systemDefault()))
                        .withResults(buildResults())
                        .build()))  //offence
                .build();
    }

    private static List<BaseResult> buildResults() {
        final List<BaseResult> baseResults = new ArrayList<>();
        baseResults.add(buildBaseResult());
        baseResults.add(buildBaseResult());
        baseResults.add(buildBaseResult());
        return baseResults;
    }

    private static BaseResult buildBaseResult() {
        return baseResult()
                .withId(RESULT_ID)
                .withPrompts(of(prompts().withId(PROMPT_ID_1).withValue("10.00").build(), prompts().withId(PROMPT_ID).withValue("10.00").build()))
                .build();
    }

    private static BaseSessionStructure createbaseSessionStructure() {
        return baseSessionStructure()
                .withSessionId(randomUUID())
                .withDateAndTimeOfSession(ZonedDateTime.of(LocalDate.of(2019, 5, 2), LocalTime.of(12, 3, 10), ZoneId.systemDefault()))
                .withOuCode("B22HM00")
                .withSessionLocation(sessionLocation()
                        .withAddress(buildAddress())
                        .withCourtHouseCode("DC")
                        .withCourtId(randomUUID())
                        .withName("Cardiff Magistrates' Court")
                        .withRoomId(randomUUID().toString())
                        .withRoomName("HorseFerry")
                        .withLja("8505")
                        .build())
                .build();
    }

    private static Address buildAddress() {
        return uk.gov.justice.core.courts.Address.address()
                .withAddress1("Fitzalan Place")
                .withAddress2("Cardiff")
                .withAddress3("addressline3")
                .withAddress4("address4")
                .withAddress5("address5")
                .withPostcode("CF24 0RZ")
                .build();
    }

    private  static AllocationDecision buildAllocationDecision(final UUID offenceId) {
        return allocationDecision()
                .withOriginatingHearingId(randomUUID())
                .withAllocationDecisionDate(LocalDate.of(2018,3,14))
                .withAllocationDecisionDate(LocalDate.of(2018,12,12))
                .withMotReasonDescription("motDescription")
                .withMotReasonCode("01")
                .withMotReasonId(randomUUID())
                .withSequenceNumber(10)
                .withCourtIndicatedSentence(courtIndicatedSentence()
                        .withCourtIndicatedSentenceTypeId(randomUUID())
                        .withCourtIndicatedSentenceDescription("courtIndicatedSentenceDescription")
                        .build())
                .withOffenceId(offenceId)
                .build();
    }

    protected static ProsecutionCase createCaseWithDefendantAndOffenceLevelJudicialResults(final List<JudicialResult> judicialResults) {
        return ProsecutionCase.prosecutionCase()
                .withId(fromString("cccc2222-1e20-4c21-916a-81a6c90239e5"))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next())
                        .withCaseURN("urn123")
                        .build())
                .withDefendants(
                        // one Defendant [with 2 offences] ::  JudicialResult present at both Defendant  and Offence level
                        asList(createDefendantWithDefendantAndOffenceJudicialResults(DEFAULT_DEFENDANT_ID3.toString(), DEFAULT_VALUE, judicialResults)))
                .build();
    }

    private static Defendant createDefendantWithDefendantAndOffenceJudicialResults(final String defendantId, final String prosecutionAuthorityReference,
                                                                                   final List<JudicialResult> judicialResults) {

        final UUID firstOffenceId = UUID.randomUUID();
        final UUID secondOffenceId = UUID.randomUUID();

        return Defendant.defendant()
                .withId(fromString(defendantId))
                .withProsecutionCaseId(randomUUID())
                .withProsecutionAuthorityReference(prosecutionAuthorityReference)
                .withPncId("pncId")
                // Defendant Level JudicialResults
                .withJudicialResults(judicialResults)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withBailStatus(bailStatus().withCode("Bail status code").withDescription("Bail status description").withId(randomUUID()).build())
                        .withBailReasons(REASON)
                        .withPersonDetails(person()
                                .withFirstName("John")
                                .withLastName(FIELD_NAME_SMITH)
                                .withTitle("Baroness")
                                .withGender(Gender.NOT_KNOWN)
                                .withNationalityId(NATIONALITY_ID)
                                .build())
                        .build())
                .withAssociatedPersons(of(associatedPerson()
                        .withPerson(person()
                                .withFirstName("Tina")
                                .withLastName(FIELD_NAME_SMITH)
                                .withTitle("Baroness")
                                .withGender(Gender.MALE)
                                .withNationalityId(NATIONALITY_ID)
                                .build())
                        .withRole("parentGuardian")
                        .build()))
                .withOffences(asList(Offence.offence()
                                .withId(firstOffenceId)
                                .withOffenceDefinitionId(randomUUID())
                                .withOffenceCode("offenceCode_1")
                                .withOffenceTitle(STRING.next())
                                .withWording(OFFENCE_WORDING)
                                .withStartDate(now())
                                .withEndDate(now())
                                .withArrestDate(now())
                                .withChargeDate(now())
                                .withConvictionDate(now())
                                .withEndDate(now())
                                .withModeOfTrial("1010")
                                .withAllocationDecision(buildAllocationDecision(ID))
                                .withJudicialResults(offenceLevelJudicialResults())
                                .withOrderIndex(12)
                                .withOrderIndex(65)
                                .withIsDisposed(true)
                                .withCount(434)
                                .build(),
                        Offence.offence()       // 2nd Offence
                                .withId(secondOffenceId)
                                .withOffenceDefinitionId(randomUUID())
                                .withOffenceCode("offenceCode_2")
                                .withOffenceTitle(STRING.next())
                                .withWording(OFFENCE_WORDING)
                                .withStartDate(now())
                                .withEndDate(now())
                                .withArrestDate(now())
                                .withChargeDate(now())
                                .withConvictionDate(now())
                                .withEndDate(now())
                                .withModeOfTrial("1010")
                                .withAllocationDecision(buildAllocationDecision(ID))
                                .withJudicialResults(offenceLevelJudicialResults())
                                .withOrderIndex(12)
                                .withOrderIndex(65)
                                .withIsDisposed(true)
                                .withCount(434)
                                .build()))
                .build();
    }
    private static ImmutableList<JudicialResult> offenceLevelJudicialResults(){
        return of(judicialResult()
                .withJudicialResultId(randomUUID())
                .withCategory(Category.INTERMEDIARY)
                .withCjsCode("cjsCode")
                .withIsAdjournmentResult(false)
                .withIsAvailableForCourtExtract(false)
                .withIsConvictedResult(false)
                .withIsFinancialResult(false)
                .withLabel("OFFENCE")
                .withOrderedHearingId(ID)
                .withOrderedDate(now())
                .withRank(BigDecimal.ZERO)
                .withWelshLabel(LABEL)
                .withResultText("resultText")
                .withLifeDuration(false)
                .withTerminatesOffenceProceedings(Boolean.TRUE)
                .withLifeDuration(false)
                .withPublishedAsAPrompt(false)
                .withExcludedFromResults(false)
                .withAlwaysPublished(false)
                .withUrgent(false)
                .withD20(false)
                .withJudicialResultTypeId(randomUUID())
                .build());

    }


}
