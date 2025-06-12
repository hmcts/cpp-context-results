package uk.gov.moj.cpp.results.test;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.List.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.core.courts.AllocationDecision.allocationDecision;
import static uk.gov.justice.core.courts.AssociatedPerson.associatedPerson;
import static uk.gov.justice.core.courts.AttendanceDay.attendanceDay;
import static uk.gov.justice.core.courts.BailStatus.bailStatus;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtIndicatedSentence.courtIndicatedSentence;
import static uk.gov.justice.core.courts.DefenceCounsel.defenceCounsel;
import static uk.gov.justice.core.courts.DefendantAttendance.defendantAttendance;
import static uk.gov.justice.core.courts.Gender.MALE;
import static uk.gov.justice.core.courts.Gender.NOT_KNOWN;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.BreachType;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtIndicatedSentence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.Jurisdiction;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceActiveOrder;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.Source;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.domains.JudicialRoleTypeEnum;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"java:S4738"})
public class TestTemplates {

    private static final UUID DEFAULT_DEFENDANT_ID1 = fromString("dddd1111-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID2 = fromString("dddd2222-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID3 = fromString("dddd3333-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID4 = fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");
    private static final UUID ID = randomUUID();
    private static final UUID NATIONALITY_ID = fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");
    public static final String DEFAULT_VALUE = "DEFAULT_VALUE";
    public static final String REASON = "reason";
    public static final String FIELD_NAME_SMITH = "Smith";
    public static final String OFFENCE_WORDING = "offenceWording";
    public static final String LABEL = "label";
    public static final String HEARING_LABEL = "hearingLabel";
    public static final String WELSH_LABEL = "welshLabel";
    private static final String TITLE = "Baroness";
    public static final String RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_ACTIVATE = "some result definition,ELMON ,some other result definition";
    public static final String RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_DEACTIVATE = "some result definition,ELmonEND,some other result definition";
    public static final String RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_ON = "some result definition,Warrants of arrest ,some other result definition";
    public static final String RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_OFF = "some result definition,WOAEXTEND,some other result definition";
    private static final String RESULT_TEXT = "resultText";
    private static final String CJS_CODE = "cjsCode";
    private static final String LINKED_CASE_ID = "cccc1111-1e20-4c21-916a-81a6c90239e5";
    private static final String ACTIVE = "ACTIVE";
    private static final String AUTHORITY_REFERENCE = "authorityReference";
    private static final String TRIAL = "Trial";
    private static final String COURT_NAME = "courtName";
    private static final ZonedDateTime FIXED_UTC_TIME = ZonedDateTime.of(2021, 6, 15, 10, 35, 10, 0, ZoneId.of("UTC"));


    private TestTemplates() {

    }

    public static Hearing basicShareHearingTemplateWithApplication(final UUID hearingId, final JurisdictionType jurisdictionType) {
        return basicShareHearingTemplateWithCustomApplication(hearingId, jurisdictionType, singletonList(CourtApplication.courtApplication()
                .withId(fromString("f8254db1-1683-483e-afb3-b87fde5a0a26"))
                .withType(courtApplicationTypeTemplates())
                .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                .withApplicant(courtApplicationPartyTemplates())
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withSubject(courtApplicationPartyTemplates())
                .withCourtApplicationCases(asList(createCourtApplicationCaseWithOffences()))
                .withApplicationParticulars("bail application")
                .withAllegationOrComplaintStartDate(now())
                .build()));
    }

    public static Hearing basicShareHearingTemplateWithCustomApplication(final UUID hearingId, final JurisdictionType jurisdictionType, final List<CourtApplication> courtApplications) {
        final List<HearingDay> hearingDays = buildHearingDays();

        return Hearing.hearing()
                .withId(hearingId)
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription(TRIAL)
                        .build())
                .withCourtApplications(courtApplications)
                .withProsecutionCases(asList(createProsecutionCase1(null, false)))
                .withJurisdictionType(jurisdictionType)
                .withHearingDays(hearingDays)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withName(COURT_NAME)
                        .withRoomId(randomUUID())
                        .withRoomName(STRING.next())
                        .withWelshName(STRING.next())
                        .withWelshRoomName(STRING.next())
                        .withAddress(address())
                        .build())
                .withDefendantAttendance(of(
                        defendantAttendance()
                                .withAttendanceDays(of(attendanceDay().withDay(LocalDate.of(2018, 5, 2)).withAttendanceType(AttendanceType.IN_PERSON).build()))
                                .withDefendantId(DEFAULT_DEFENDANT_ID1)
                                .build(),
                        defendantAttendance()
                                .withAttendanceDays(of(attendanceDay().withDay(LocalDate.of(2018, 5, 2)).withAttendanceType(AttendanceType.NOT_PRESENT).build()))
                                .withDefendantId(DEFAULT_DEFENDANT_ID2)
                                .build()))
                .build();
    }

    public static Hearing basicShareHearingTemplate(final UUID hearingId, final List<ProsecutionCase> prosecutionCases, final JurisdictionType jurisdictionType, final boolean isSJPHearing) {
        final List<HearingDay> hearingDays = buildHearingDays();

        return Hearing.hearing()
                .withId(hearingId)
                .withIsSJPHearing(isSJPHearing)
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription(TRIAL)
                        .build())
                .withCourtApplications(asList(CourtApplication.courtApplication()
                        .withId(randomUUID())
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
                .withDefenceCounsels(of(defenceCounsel().withId(randomUUID()).withAttendanceDays(of(now())).withDefendants(of(randomUUID(), randomUUID(), DEFAULT_DEFENDANT_ID1)).build(),
                        defenceCounsel().withId(randomUUID()).withAttendanceDays(of(LocalDate.of(2018, 5, 2))).withDefendants(of(randomUUID(), DEFAULT_DEFENDANT_ID3, randomUUID())).build()))
                .withJurisdictionType(jurisdictionType)
                .withJudiciary(singletonList(JudicialRole.judicialRole()
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(circuitJudge())
                        .build()))
                .withCourtApplications(asList(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withType(courtApplicationTypeTemplates())
                        .withSubject(courtApplicationPartyTemplates())
                        .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                        .withApplicant(courtApplicationPartyTemplates())
                        .withApplicationStatus(ApplicationStatus.DRAFT)
                        .build()))
                .withHearingDays(hearingDays)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withName(COURT_NAME)
                        .withRoomId(randomUUID())
                        .withRoomName(STRING.next())
                        .withWelshName(STRING.next())
                        .withWelshRoomName(STRING.next())
                        .withAddress(address())
                        .build())
                .withProsecutionCases(asList(createProsecutionCase1(null, false), createProsecutionCase2(null, false)))
                .withProsecutionCases(prosecutionCases)
                .withDefendantJudicialResults(asList(DefendantJudicialResult.defendantJudicialResult()
                        .withMasterDefendantId(randomUUID())
                        .withJudicialResult(judicialResult()
                                .withJudicialResultId(randomUUID())
                                .withCategory(JudicialResultCategory.FINAL)
                                .withCjsCode(CJS_CODE)
                                .withIsAdjournmentResult(false)
                                .withIsAvailableForCourtExtract(false)
                                .withIsConvictedResult(false)
                                .withIsFinancialResult(false)
                                .withLabel(HEARING_LABEL)
                                .withOrderedHearingId(ID)
                                .withOrderedDate(now())
                                .withRank(BigDecimal.ZERO)
                                .withWelshLabel(WELSH_LABEL)
                                .withResultText(RESULT_TEXT)
                                .withLifeDuration(false)
                                .withTerminatesOffenceProceedings(Boolean.FALSE)
                                .withLifeDuration(false)
                                .withPublishedAsAPrompt(false)
                                .withExcludedFromResults(false)
                                .withAlwaysPublished(false)
                                .withUrgent(false)
                                .withD20(false)
                                .withPublishedForNows(false)
                                .withRollUpPrompts(false)
                                .withJudicialResultTypeId(randomUUID())
                                .withJudicialResultPrompts(singletonList(JudicialResultPrompt.judicialResultPrompt()
                                        .withIsFinancialImposition(true)
                                        .withJudicialResultPromptTypeId(randomUUID())
                                        .withCourtExtract("Y")
                                        .withLabel(LABEL)
                                        .withValue("value")
                                        .withTotalPenaltyPoints(BigDecimal.TEN)
                                        .build()))
                                .build())
                        .build()))
                .build();
    }

    public static Hearing basicShareHearingTemplateWithAppealType(final UUID hearingId, final List<ProsecutionCase> prosecutionCases, final JurisdictionType jurisdictionType, final boolean isSJPHearing,
                                                    final boolean appealType) {
        final List<HearingDay> hearingDays = buildHearingDays();

        return Hearing.hearing()
                .withId(hearingId)
                .withIsSJPHearing(isSJPHearing)
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription(TRIAL)
                        .build())
                .withCourtApplications(asList(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withApplicationReference(STRING.next())
                        .withType(courtApplicationTypeTemplatesWithAppealType(appealType))
                        .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                        .withApplicant(courtApplicationPartyTemplates())
                        .withApplicationStatus(ApplicationStatus.DRAFT)
                        .withCourtApplicationCases(asList(TestTemplates.createCourtApplicationCaseWithOffencesWithProsecutionAuthorityCode("DERPF")))
                        .withJudicialResults(buildJudicialResultList())
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
                .withDefenceCounsels(of(defenceCounsel().withId(randomUUID()).withAttendanceDays(of(now())).withDefendants(of(randomUUID(), randomUUID(), DEFAULT_DEFENDANT_ID1)).build(),
                        defenceCounsel().withId(randomUUID()).withAttendanceDays(of(LocalDate.of(2018, 5, 2))).withDefendants(of(randomUUID(), DEFAULT_DEFENDANT_ID3, randomUUID())).build()))
                .withJurisdictionType(jurisdictionType)
                .withJudiciary(singletonList(JudicialRole.judicialRole()
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(circuitJudge())
                        .build()))
                .withCourtApplications(asList(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withApplicationReference(STRING.next())
                        .withType(courtApplicationTypeTemplatesWithAppealType(appealType))
                        .withSubject(courtApplicationPartyTemplates())
                        .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                        .withApplicant(courtApplicationPartyTemplates())
                        .withApplicationStatus(ApplicationStatus.DRAFT)
                        .withCourtApplicationCases(asList(TestTemplates.createCourtApplicationCaseWithOffencesWithProsecutionAuthorityCode("DERPF")))
                        .withJudicialResults(buildJudicialResultList())
                        .build()))
                .withHearingDays(hearingDays)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withName(COURT_NAME)
                        .withRoomId(randomUUID())
                        .withRoomName(STRING.next())
                        .withWelshName(STRING.next())
                        .withWelshRoomName(STRING.next())
                        .withAddress(address())
                        .build())
                .withProsecutionCases(asList(createProsecutionCase1(null, false), createProsecutionCase2(null, false)))
                .withProsecutionCases(prosecutionCases)
                .withDefendantJudicialResults(asList(DefendantJudicialResult.defendantJudicialResult()
                        .withMasterDefendantId(randomUUID())
                        .withJudicialResult(judicialResult()
                                .withJudicialResultId(randomUUID())
                                .withCategory(JudicialResultCategory.FINAL)
                                .withCjsCode(CJS_CODE)
                                .withIsAdjournmentResult(false)
                                .withIsAvailableForCourtExtract(false)
                                .withIsConvictedResult(false)
                                .withIsFinancialResult(false)
                                .withLabel(HEARING_LABEL)
                                .withOrderedHearingId(ID)
                                .withOrderedDate(now())
                                .withRank(BigDecimal.ZERO)
                                .withWelshLabel(WELSH_LABEL)
                                .withResultText(RESULT_TEXT)
                                .withLifeDuration(false)
                                .withTerminatesOffenceProceedings(Boolean.FALSE)
                                .withLifeDuration(false)
                                .withPublishedAsAPrompt(false)
                                .withExcludedFromResults(false)
                                .withAlwaysPublished(false)
                                .withUrgent(false)
                                .withD20(false)
                                .withPublishedForNows(false)
                                .withRollUpPrompts(false)
                                .withJudicialResultTypeId(randomUUID())
                                .withJudicialResultPrompts(singletonList(JudicialResultPrompt.judicialResultPrompt()
                                        .withIsFinancialImposition(true)
                                        .withJudicialResultPromptTypeId(randomUUID())
                                        .withCourtExtract("Y")
                                        .withLabel(LABEL)
                                        .withValue("value")
                                        .withTotalPenaltyPoints(BigDecimal.TEN)
                                        .build()))
                                .build())
                        .build()))
                .build();
    }

    public static PublicHearingResulted sharedResultTemplateWithTwoOffences(final JurisdictionType jurisdictionType) {

        final UUID hearingId = randomUUID();

        return PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplate(hearingId,
                        singletonList(createCaseWithDefendantAndOffenceLevelJudicialResults(buildJudicialResultList())), jurisdictionType, false))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

    }

    public static Address address() {
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

    public static PublicHearingResulted basicShareResultsWithMagistratesAlongWithOffenceDateCodeTemplate(final Integer offenceDateCode) {
        return basicShareResultsTemplate(JurisdictionType.MAGISTRATES, false, offenceDateCode);
    }

    public static PublicHearingResulted basicShareResultsV2WithMagistratesAlongWithOffenceDateCodeTemplate(final Integer offenceDateCode) {
        final PublicHearingResulted publicHearingResulted = basicShareResultsTemplate(JurisdictionType.MAGISTRATES, false, offenceDateCode);
        publicHearingResulted.setIsReshare(Optional.of(false));
        publicHearingResulted.setHearingDay(Optional.of(LocalDate.of(2018, 5, 2)));
        return publicHearingResulted;
    }

    public static PublicHearingResulted basicShareResultsV2Template(final JurisdictionType jurisdictionType) {
        final PublicHearingResulted publicHearingResulted = basicShareResultsTemplate(jurisdictionType, false);
        publicHearingResulted.setIsReshare(Optional.of(false));
        publicHearingResulted.setHearingDay(Optional.of(LocalDate.of(2018, 5, 2)));
        return publicHearingResulted;
    }

    public static PublicHearingResulted basicShareResultsV2TemplateWithTwoOffences(final JurisdictionType jurisdictionType) {

        final UUID hearingId = randomUUID();

        return PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplate(hearingId,
                        singletonList(createCaseWithDefendantAndOffenceLevelJudicialResults(buildJudicialResultList())), jurisdictionType, false))
                .setIsReshare(Optional.of(false))
                .setHearingDay(Optional.of(LocalDate.of(2018, 5, 2)))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

    }

    public static PublicHearingResulted basicShareResultsV2TemplateForIndicatedPlea(final JurisdictionType jurisdictionType) {
        final PublicHearingResulted publicHearingResulted = basicShareResultsTemplateForIndicatedPlea(jurisdictionType, false, false);
        publicHearingResulted.setIsReshare(Optional.of(false));
        publicHearingResulted.setHearingDay(Optional.of(LocalDate.of(2018, 5, 2)));
        return publicHearingResulted;
    }

    public static PublicHearingResulted basicShareResultsV2TemplateWithHearingDay(final JurisdictionType jurisdictionType, final LocalDate hearingDay) {
        final PublicHearingResulted publicHearingResulted = basicShareResultsTemplate(jurisdictionType, false);
        publicHearingResulted.getHearing().getHearingDays().add(HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.of(hearingDay, LocalTime.MIDNIGHT, ZoneId.of("UTC")))
                .withListedDurationMinutes(100)
                .withListingSequence(10)
                .build());
        publicHearingResulted.setIsReshare(Optional.of(false));
        publicHearingResulted.setHearingDay(Optional.of(hearingDay));
        return publicHearingResulted;
    }

    public static PublicHearingResulted basicShareResultsV2WithVerdictTemplate(final JurisdictionType jurisdictionType, final boolean isWithVerdict, final boolean isSJPHearing) {
        final PublicHearingResulted publicHearingResulted = basicShareResultsTemplate(jurisdictionType, isWithVerdict, isSJPHearing);
        publicHearingResulted.setIsReshare(Optional.of(false));
        publicHearingResulted.setHearingDay(Optional.of(LocalDate.of(2018, 5, 2)));
        return publicHearingResulted;
    }

    public static PublicHearingResulted basicShareResultsV2WithVerdictTemplate(final JurisdictionType jurisdictionType, final boolean isWithVerdict) {
        final PublicHearingResulted publicHearingResulted = basicShareResultsTemplate(jurisdictionType, isWithVerdict);
        publicHearingResulted.setIsReshare(Optional.of(false));
        publicHearingResulted.setHearingDay(Optional.of(LocalDate.of(2018, 5, 2)));
        return publicHearingResulted;
    }

    public static PublicHearingResulted basicShareResultsTemplate(final JurisdictionType jurisdictionType) {
        return basicShareResultsTemplate(jurisdictionType, false, false);
    }

    public static PublicHearingResulted basicShareResultsTemplateWithAppealFlag(final JurisdictionType jurisdictionType, final boolean appealFlag) {
        return basicShareResultsTemplateWithAppealType(jurisdictionType, false, false, appealFlag);
    }

    public static PublicHearingResulted basicShareResultsTemplate(final JurisdictionType jurisdictionType, final boolean isSJPHearing) {
        return basicShareResultsTemplate(jurisdictionType, false, isSJPHearing);
    }

    public static PublicHearingResulted basicShareResultsTemplate(final JurisdictionType jurisdictionType, final boolean isWithVerdict, final boolean isSJPHearing) {

        final UUID hearingId = randomUUID();

        return PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplate(hearingId, asList(createProsecutionCase1(buildJudicialResultList(), isWithVerdict), createProsecutionCase2(buildJudicialResultList(), isWithVerdict)), jurisdictionType, isSJPHearing))
                .setSharedTime(FIXED_UTC_TIME);
    }

    public static PublicHearingResulted basicShareResultsTemplateWithOneCaseOneDefendant(final UUID hearingId, final JurisdictionType jurisdictionType, final boolean isReshare, final LocalDate hearingDay) {

        return PublicHearingResulted.publicHearingResulted()
                .setIsReshare(Optional.of(isReshare))
                .setHearingDay(Optional.of(hearingDay))
                .setHearing(basicShareHearingTemplate(hearingId, asList(createProsecutionCaseForIndicatesPlea(buildJudicialResultList(), false)), jurisdictionType, false))
                .setSharedTime(FIXED_UTC_TIME);
    }

    public static PublicHearingResulted basicShareResultsTemplateWithAppealType(final JurisdictionType jurisdictionType, final boolean isWithVerdict, final boolean isSJPHearing,
                                                                                final boolean appealType) {

        final UUID hearingId = randomUUID();

        return PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithAppealType(hearingId, asList(createProsecutionCase1(buildJudicialResultList(), isWithVerdict),
                        createProsecutionCase2(buildJudicialResultList(), isWithVerdict)), jurisdictionType, isSJPHearing, appealType))
                .setSharedTime(FIXED_UTC_TIME);
    }

    public static PublicHearingResulted basicShareResultsTemplateForIndicatedPlea(final JurisdictionType jurisdictionType, final boolean isWithVerdict, final boolean isSJPHearing) {

        final UUID hearingId = randomUUID();

        return PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplate(hearingId, asList(createProsecutionCaseForIndicatesPlea(buildJudicialResultList(), isWithVerdict)), jurisdictionType, isSJPHearing))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    public static PublicHearingResulted basicShareResultsTemplate(final JurisdictionType jurisdictionType, final boolean isWithVerdict, final Integer offenceDateCode) {

        final UUID hearingId = randomUUID();

        return PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplate(hearingId, asList(createProsecutionCaseWithOffenceDateCode(buildJudicialResultList(), isWithVerdict, offenceDateCode), createProsecutionCase2(buildJudicialResultList(), isWithVerdict)), jurisdictionType, false))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    public static PublicHearingResulted basicShareResultsWithShadowListedOffencesTemplate() {

        final UUID hearingId = randomUUID();

        return PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplate(hearingId, asList(createProsecutionCase1(buildJudicialResultList(), false), createProsecutionCase2(buildJudicialResultList(), false)), JurisdictionType.MAGISTRATES, false))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")))
                .setShadowListedOffences(singletonList(UUID.randomUUID()));
    }

    private static ProsecutionCase createProsecutionCase1(final List<JudicialResult> judicialResults, final boolean isWithVerdict) {
        return ProsecutionCase.prosecutionCase()
                .withId(fromString(LINKED_CASE_ID))
                .withInitiationCode(InitiationCode.C)
                .withOriginatingOrganisation(STRING.next())
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next())
                        .withProsecutionAuthorityReference(AUTHORITY_REFERENCE)
                        .build())
                .withDefendants(
                        asList(createDefendant(DEFAULT_DEFENDANT_ID1.toString(), null, judicialResults, isWithVerdict, null, true),
                                createDefendant(DEFAULT_DEFENDANT_ID2.toString(), DEFAULT_VALUE, judicialResults, isWithVerdict, null, false)
                        ))
                .withCaseStatus(ACTIVE)
                .build();
    }

    private static ProsecutionCase createProsecutionCaseForIndicatesPlea(final List<JudicialResult> judicialResults, final boolean isWithVerdict) {
        return ProsecutionCase.prosecutionCase()
                .withId(fromString(LINKED_CASE_ID))
                .withInitiationCode(InitiationCode.C)
                .withOriginatingOrganisation(STRING.next())
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next())
                        .withProsecutionAuthorityReference(AUTHORITY_REFERENCE)
                        .build())
                .withDefendants(
                        asList(createDefendant(DEFAULT_DEFENDANT_ID1.toString(), null, judicialResults, isWithVerdict, null, true)
                        ))
                .withCaseStatus(ACTIVE)
                .build();
    }

    public static CourtApplicationCase createCourtApplicationCaseWithOffences() {
        return courtApplicationCase()
                .withCaseStatus(ACTIVE)
                .withIsSJP(false)
                .withProsecutionCaseId(fromString(LINKED_CASE_ID))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next())
                        .withProsecutionAuthorityReference(AUTHORITY_REFERENCE)
                        .build())
                .withOffences(getOffenceList(buildJudicialResultList(), true, null, false))
                .build();
    }

    public static CourtApplicationCase createCourtApplicationCaseWithOffencesWithProsecutionAuthorityCode(final String prosecutionAuthorityCode) {
        return courtApplicationCase()
                .withCaseStatus(ACTIVE)
                .withIsSJP(false)
                .withProsecutionCaseId(fromString(LINKED_CASE_ID))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(prosecutionAuthorityCode)
                        .withCaseURN(STRING.next())
                        .withContact(ContactNumber.contactNumber().withPrimaryEmail("criminaldataderbyshire@derbyshire.police.uk").build())
                        .withAddress(Address.address()
                                .withAddress1("Criminal Justice Department")
                                .withAddress2("Derbyshire Constabulary")
                                .withAddress3("Butterley Hall")
                                .withAddress4("Ripley")
                                .withAddress5("Derby")
                                .withPostcode("DE5 3RS")
                                .build())
                        .withProsecutionAuthorityOUCode("0300000")
                        .withMajorCreditorCode("PO30")
                        .withProsecutionAuthorityName("Derbyshire Police")
                        .build())
                .withOffences(getOffenceList(buildJudicialResultList(), true, null, false))
                .build();
    }

    public static CourtApplicationCase createCourtApplicationCaseWithoutOffences() {
        return courtApplicationCase()
                .withCaseStatus(ACTIVE)
                .withIsSJP(false)
                .withProsecutionCaseId(fromString(LINKED_CASE_ID))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next())
                        .withProsecutionAuthorityReference(AUTHORITY_REFERENCE)
                        .build())
                .build();
    }

    protected static ProsecutionCase createProsecutionCase2(final List<JudicialResult> judicialResults, final boolean isWithVerdict) {
        return ProsecutionCase.prosecutionCase()
                .withId(fromString("cccc2222-1e20-4c21-916a-81a6c90239e5"))
                .withInitiationCode(InitiationCode.C)
                .withOriginatingOrganisation(STRING.next())
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next())
                        .withCaseURN(STRING.next())
                        .build())
                .withDefendants(
                        asList(createDefendant(DEFAULT_DEFENDANT_ID3.toString(), DEFAULT_VALUE, judicialResults, isWithVerdict, null, false),
                                createDefendant(DEFAULT_DEFENDANT_ID4.toString(), null, judicialResults, isWithVerdict, null, false)
                        ))
                .withCaseStatus(ACTIVE)
                .build();
    }

    private static ProsecutionCase createProsecutionCaseWithOffenceDateCode(final List<JudicialResult> judicialResults, final boolean isWithVerdict, final Integer offenceDateCode) {
        return ProsecutionCase.prosecutionCase()
                .withId(fromString(LINKED_CASE_ID))
                .withInitiationCode(InitiationCode.C)
                .withOriginatingOrganisation(STRING.next())
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next())
                        .withProsecutionAuthorityReference(AUTHORITY_REFERENCE)
                        .build())
                .withDefendants(
                        asList(createDefendant(DEFAULT_DEFENDANT_ID1.toString(), null, judicialResults, isWithVerdict, offenceDateCode, false),
                                createDefendant(DEFAULT_DEFENDANT_ID2.toString(), DEFAULT_VALUE, judicialResults, isWithVerdict, offenceDateCode, false)
                        ))
                .withCaseStatus(ACTIVE)
                .build();
    }

    private static Defendant createDefendant(final String defendantId, final String prosecutionAuthorityReference, final List<JudicialResult> judicialResults, final boolean isWithVerdict, final Integer offenceDateCode, final Boolean isIndicatedPlea) {
        return Defendant.defendant()
                .withCourtProceedingsInitiated(new UtcClock().now())
                .withMasterDefendantId(UUID.fromString(defendantId))
                .withId(fromString(defendantId))
                .withProsecutionCaseId(fromString(LINKED_CASE_ID))
                .withProsecutionAuthorityReference(prosecutionAuthorityReference)
                .withPncId("pncId")
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withBailStatus(bailStatus().withCode("Bail status code").withDescription("Bail status description").withId(randomUUID()).build())
                        .withBailReasons(REASON)
                        .withPersonDetails(person()
                                .withFirstName("John")
                                .withLastName(FIELD_NAME_SMITH)
                                .withTitle(TITLE)
                                .withGender(NOT_KNOWN)
                                .withNationalityId(NATIONALITY_ID)
                                .build())
                        .build())
                .withAssociatedPersons(of(associatedPerson()
                        .withPerson(person()
                                .withFirstName("Tina")
                                .withLastName(FIELD_NAME_SMITH)
                                .withTitle(TITLE)
                                .withGender(MALE)
                                .withNationalityId(NATIONALITY_ID)
                                .build())
                        .withRole("parentGuardian")
                        .build()))
                .withOffences(
                        getOffenceList(judicialResults, isWithVerdict, offenceDateCode, isIndicatedPlea)
                )
                .build();
    }

    private static List<HearingDay> buildHearingDays() {
        final List<HearingDay> hearingDays = new ArrayList<>();
        hearingDays.add(HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 5, 2), LocalTime.of(12, 1, 1), ZoneId.of("UTC")))
                .withListedDurationMinutes(100)
                .withListingSequence(10)
                .build());
        hearingDays.add(HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 4, 3), LocalTime.of(12, 0), ZoneId.of("UTC")))
                .withListedDurationMinutes(100)
                .withListingSequence(10)
                .build());
        hearingDays.add(HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 2, 2), LocalTime.of(12, 0), ZoneId.of("UTC")))
                .withListedDurationMinutes(100)
                .withListingSequence(10)
                .build());
        return hearingDays;
    }

    private static List<Offence> getOffenceList(final List<JudicialResult> judicialResults, final boolean isWithVerdict, final Integer offenceDateCode, final boolean isIndicatedPlea) {

        final Offence.Builder builder = Offence.offence();
        builder.withId(randomUUID())
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
                .withOrderIndex(65)
                .withIsDisposed(true)
                .withCount(434)
                .withPlea(uk.gov.justice.core.courts.Plea.plea().withOffenceId(ID).withPleaDate(LocalDate.now()).withPleaValue("NOT_GUILTY").build())
                .withProceedingsConcluded(true)
                .withIntroducedAfterInitialProceedings(true)
                .withOffenceDateCode(offenceDateCode)
                .withIsDiscontinued(true);
        if (isWithVerdict) {
            builder.withVerdict(Verdict.verdict()
                    .withVerdictType(VerdictType.verdictType()
                            .withId(fromString("3f0d69d0-2fda-3472-8d4c-a6248f661825"))
                            .withCategory(STRING.next())
                            .withCategoryType(STRING.next())
                            .withCjsVerdictCode("N")
                            .build())
                    .withOriginatingHearingId(randomUUID())
                    .withOffenceId(ID)
                    .withVerdictDate(now())
                    .build());
        }

        if (isIndicatedPlea) {
            builder.withIndicatedPlea(IndicatedPlea.indicatedPlea()
                    .withOffenceId(ID)
                    .withIndicatedPleaDate(LocalDate.now())
                    .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY)
                    .withSource(Source.IN_COURT)
                    .build());
        }

        return asList(
                builder.build()
        );
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

    public static List<JudicialResult> buildJudicialResultList() {
        return asList(judicialResult()
                .withJudicialResultId(randomUUID())
                .withCategory(JudicialResultCategory.FINAL)
                .withCjsCode(CJS_CODE)
                .withIsAdjournmentResult(false)
                .withIsAvailableForCourtExtract(false)
                .withIsConvictedResult(false)
                .withIsFinancialResult(false)
                .withLabel(LABEL)
                .withOrderedHearingId(ID)
                .withOrderedDate(now())
                .withRank(BigDecimal.ZERO)
                .withWelshLabel(LABEL)
                .withResultText(RESULT_TEXT)
                .withLifeDuration(false)
                .withTerminatesOffenceProceedings(Boolean.FALSE)
                .withLifeDuration(false)
                .withPublishedAsAPrompt(false)
                .withExcludedFromResults(false)
                .withAlwaysPublished(false)
                .withUrgent(false)
                .withD20(false)
                .withPublishedForNows(false)
                .withRollUpPrompts(false)
                .withJudicialResultTypeId(randomUUID())
                .build());
    }

    public static CourtApplicationType courtApplicationTypeTemplates() {
        return CourtApplicationType.courtApplicationType()
                .withId(randomUUID())
                .withCode(STRING.next())
                .withType(STRING.next())
                .withLegislation(STRING.next())
                .withCategoryCode(STRING.next())
                .withLinkType(LinkType.LINKED)
                .withJurisdiction(Jurisdiction.CROWN)
                .withSummonsTemplateType(SummonsTemplateType.BREACH)
                .withBreachType(BreachType.GENERIC_BREACH)
                .withAppealFlag(false)
                .withApplicantAppellantFlag(false)
                .withPleaApplicableFlag(false)
                .withCommrOfOathFlag(false)
                .withCourtOfAppealFlag(false)
                .withCourtExtractAvlFlag(false)
                .withProsecutorThirdPartyFlag(false)
                .withSpiOutApplicableFlag(true)
                .withOffenceActiveOrder(OffenceActiveOrder.OFFENCE)
                .build();
    }

    public static CourtApplicationType courtApplicationTypeTemplatesWithAppealType(final boolean appealType) {
        return CourtApplicationType.courtApplicationType()
                .withId(randomUUID())
                .withCode(STRING.next())
                .withType(STRING.next())
                .withLegislation(STRING.next())
                .withCategoryCode(STRING.next())
                .withLinkType(LinkType.LINKED)
                .withJurisdiction(Jurisdiction.CROWN)
                .withSummonsTemplateType(SummonsTemplateType.BREACH)
                .withBreachType(BreachType.GENERIC_BREACH)
                .withAppealFlag(appealType)
                .withApplicantAppellantFlag(false)
                .withPleaApplicableFlag(false)
                .withCommrOfOathFlag(false)
                .withCourtOfAppealFlag(false)
                .withCourtExtractAvlFlag(false)
                .withProsecutorThirdPartyFlag(false)
                .withSpiOutApplicableFlag(true)
                .withOffenceActiveOrder(OffenceActiveOrder.OFFENCE)
                .build();
    }

    public static CourtApplicationParty courtApplicationPartyTemplates() {
        return CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(DEFAULT_DEFENDANT_ID1)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(person()
                                        .withFirstName("John")
                                        .withLastName(FIELD_NAME_SMITH)
                                        .withTitle(TITLE)
                                        .withGender(NOT_KNOWN)
                                        .withNationalityId(NATIONALITY_ID)
                                        .build())
                                .withArrestSummonsNumber("ARREST_1234")
                                .build())
                        .build())
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();
    }


    private static AllocationDecision buildAllocationDecision(final UUID offenceId) {
        return allocationDecision()
                .withOriginatingHearingId(randomUUID())
                .withAllocationDecisionDate(LocalDate.of(2018, 3, 14))
                .withAllocationDecisionDate(LocalDate.of(2018, 12, 12))
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
                .withOriginatingOrganisation(STRING.next())
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next())
                        .withCaseURN("urn123")
                        .build())
                .withDefendants(
                        // one Defendant [with 2 offences] ::  JudicialResult present at both Defendant  and Offence level
                        singletonList(createDefendantWithDefendantAndOffenceJudicialResults(DEFAULT_DEFENDANT_ID3.toString(), DEFAULT_VALUE, judicialResults)))
                .build();
    }

    private static Defendant createDefendantWithDefendantAndOffenceJudicialResults(final String defendantId, final String prosecutionAuthorityReference,
                                                                                   final List<JudicialResult> judicialResults) {

        final UUID firstOffenceId = UUID.randomUUID();
        final UUID secondOffenceId = UUID.randomUUID();

        return Defendant.defendant()
                .withId(fromString(defendantId))
                .withMasterDefendantId(fromString(defendantId))
                .withCourtProceedingsInitiated(ZonedDateTime.now())
                .withProsecutionCaseId(randomUUID())
                .withProsecutionAuthorityReference(prosecutionAuthorityReference)
                .withPncId("pncId")
                // Defendant Level JudicialResults
                .withDefendantCaseJudicialResults(judicialResults)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withBailStatus(bailStatus().withCode("Bail status code").withDescription("Bail status description").withId(randomUUID()).build())
                        .withBailReasons(REASON)
                        .withPersonDetails(person()
                                .withFirstName("John")
                                .withLastName(FIELD_NAME_SMITH)
                                .withTitle(TITLE)
                                .withGender(NOT_KNOWN)
                                .withNationalityId(NATIONALITY_ID)
                                .build())
                        .build())
                .withAssociatedPersons(of(associatedPerson()
                        .withPerson(person()
                                .withFirstName("Tina")
                                .withLastName(FIELD_NAME_SMITH)
                                .withTitle(TITLE)
                                .withGender(MALE)
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

    private static List<JudicialResult> offenceLevelJudicialResults() {
        return of(judicialResult()
                .withJudicialResultId(randomUUID())
                .withCategory(JudicialResultCategory.INTERMEDIARY)
                .withCjsCode(CJS_CODE)
                .withIsAdjournmentResult(false)
                .withIsAvailableForCourtExtract(false)
                .withIsConvictedResult(false)
                .withIsFinancialResult(false)
                .withLabel("OFFENCE")
                .withOrderedHearingId(ID)
                .withOrderedDate(now())
                .withRank(BigDecimal.ZERO)
                .withWelshLabel(LABEL)
                .withResultText(RESULT_TEXT)
                .withLifeDuration(false)
                .withTerminatesOffenceProceedings(Boolean.TRUE)
                .withLifeDuration(false)
                .withPublishedAsAPrompt(false)
                .withExcludedFromResults(false)
                .withAlwaysPublished(false)
                .withRollUpPrompts(false)
                .withPublishedForNows(false)
                .withUrgent(false)
                .withD20(false)
                .withJudicialResultTypeId(randomUUID())
                .build());

    }
}
