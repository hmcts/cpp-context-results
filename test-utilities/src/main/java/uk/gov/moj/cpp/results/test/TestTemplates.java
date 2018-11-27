package uk.gov.moj.cpp.results.test;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.json.schemas.core.CourtCentre;
import uk.gov.justice.json.schemas.core.Defendant;
import uk.gov.justice.json.schemas.core.Gender;
import uk.gov.justice.json.schemas.core.HearingDay;
import uk.gov.justice.json.schemas.core.HearingType;
import uk.gov.justice.json.schemas.core.InitiationCode;
import uk.gov.justice.json.schemas.core.JudicialRole;
import uk.gov.justice.json.schemas.core.JudicialRoleType;
import uk.gov.justice.json.schemas.core.Offence;
import uk.gov.justice.json.schemas.core.Person;
import uk.gov.justice.json.schemas.core.PersonDefendant;
import uk.gov.justice.json.schemas.core.ProsecutionCase;
import uk.gov.justice.json.schemas.core.ProsecutionCaseIdentifier;
import uk.gov.justice.json.schemas.core.ResultPrompt;
import uk.gov.justice.json.schemas.core.Title;
import uk.gov.justice.json.schemas.core.JurisdictionType;
import uk.gov.justice.json.schemas.core.Key;
import uk.gov.justice.json.schemas.core.SharedHearing;
import uk.gov.justice.json.schemas.core.SharedResultLine;
import uk.gov.justice.json.schemas.core.SharedVariant;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TestTemplates {

    public static final UUID DEFAULT_DEFENDANT_ID1 = UUID.fromString("dddd1111-1e20-4c21-916a-81a6c90239e5");
    public static final UUID DEFAULT_DEFENDANT_ID2 = UUID.fromString("dddd2222-1e20-4c21-916a-81a6c90239e5");
    public static final UUID DEFAULT_DEFENDANT_ID3 = UUID.fromString("dddd3333-1e20-4c21-916a-81a6c90239e5");
    public static final UUID DEFAULT_DEFENDANT_ID4 = UUID.fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");

    private TestTemplates() {

    }



        public static SharedHearing basicShareHearingTemplate(final UUID hearingId) {
            return SharedHearing.sharedHearing()
                    .withId(hearingId)
                    .withType(HearingType.hearingType()
                            .withId(randomUUID())
                            .withDescription("Trial")
                            .build())
                    .withJurisdictionType(JurisdictionType.CROWN)
                    .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                            .withJudicialId(randomUUID())
                            .withJudicialRoleType(JudicialRoleType.CIRCUIT_JUDGE)
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
                            .build())
                    .withProsecutionCases(Arrays.asList(createProsecutionCase1(), createProsecutionCase2()))
                    .withSharedResultLines(Arrays.asList(
                            createSharedResultLine(DEFAULT_DEFENDANT_ID1),
                            createSharedResultLine(DEFAULT_DEFENDANT_ID1),
                            createSharedResultLine(DEFAULT_DEFENDANT_ID1),
                            createSharedResultLine(DEFAULT_DEFENDANT_ID1),
                            createSharedResultLine(DEFAULT_DEFENDANT_ID2),
                            createSharedResultLine(DEFAULT_DEFENDANT_ID2),
                            createSharedResultLine(DEFAULT_DEFENDANT_ID2),
                            createSharedResultLine(DEFAULT_DEFENDANT_ID3),
                            createSharedResultLine(DEFAULT_DEFENDANT_ID3),
                            createSharedResultLine(DEFAULT_DEFENDANT_ID4)
                    ))
                    .build();
        }


        public static PublicHearingResulted basicShareResultsTemplate() {

        final UUID hearingId = randomUUID();

        return PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplate(hearingId))
                .setVariants(createVariants(hearingId))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

    }


    private static SharedResultLine createSharedResultLine(UUID defendantId) {
        return SharedResultLine.sharedResultLine()
                .withId(randomUUID())
                .withLabel(STRING.next())
                .withLevel("CASE")
                .withDefendantId(defendantId)
                .withIsAvailableForCourtExtract(true)
                .withWelshLabel(STRING.next())
                .withRank(BigDecimal.ONE)
                .withProsecutionCaseId(UUID.randomUUID())
//                .withLastSharedDateTime()
                .withPrompts(Arrays.asList(
                        ResultPrompt.resultPrompt()
                                .withValue(STRING.next())
                                .withLabel(STRING.next())
                                .withId(UUID.randomUUID())
                                .withIsAvailableForCourtExtract(true)
                                .build()
                ))
                .build();
    }

    private static List<SharedVariant> createVariants(UUID hearingId) {
        return Arrays.asList(
                createVariant(hearingId, DEFAULT_DEFENDANT_ID1, UUID.fromString("aaaa1111-1e20-4c21-916a-81a6c90239e5")),
                createVariant(hearingId, DEFAULT_DEFENDANT_ID1, UUID.fromString("aaaa2222-1e20-4c21-916a-81a6c90239e5")),
                createVariant(hearingId, DEFAULT_DEFENDANT_ID1, UUID.fromString("aaaa3333-1e20-4c21-916a-81a6c90239e5")),
                createVariant(hearingId, DEFAULT_DEFENDANT_ID1, UUID.fromString("aaaa4444-1e20-4c21-916a-81a6c90239e5")),
                createVariant(hearingId, DEFAULT_DEFENDANT_ID2, UUID.fromString("aaaa5555-1e20-4c21-916a-81a6c90239e5")),
                createVariant(hearingId, DEFAULT_DEFENDANT_ID2, UUID.fromString("aaaa6666-1e20-4c21-916a-81a6c90239e5")),
                createVariant(hearingId, DEFAULT_DEFENDANT_ID2, UUID.fromString("aaaa7777-1e20-4c21-916a-81a6c90239e5")),
                createVariant(hearingId, DEFAULT_DEFENDANT_ID3, UUID.fromString("aaaa8888-1e20-4c21-916a-81a6c90239e5")),
                createVariant(hearingId, DEFAULT_DEFENDANT_ID3, UUID.fromString("aaaa9999-1e20-4c21-916a-81a6c90239e5")),
                createVariant(hearingId, DEFAULT_DEFENDANT_ID4, UUID.fromString("aaaa0000-1e20-4c21-916a-81a6c90239e5"))
        );
    }

    private static SharedVariant createVariant(UUID hearingId, UUID defendantId, UUID variantId) {
        return SharedVariant.sharedVariant()
                .withKey(Key.key()
                        .withHearingId(hearingId)
                        .withDefendantId(defendantId)
                        .withNowsTypeId(randomUUID())
                        .withUsergroups(Arrays.asList(STRING.next()))
                        .build())
                .withMaterialId(variantId)
                .withDescription(STRING.next())
                .withTemplateName(STRING.next())
                .withStatus("failed")
                .build();
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
                .build();
    }

    protected static ProsecutionCase createProsecutionCase2() {
        return ProsecutionCase.prosecutionCase()
                .withId(UUID.fromString("cccc2222-1e20-4c21-916a-81a6c90239e5"))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next())
                        //.withProsecutionAuthorityReference(STRING.next())
                        .withCaseURN(STRING.next())
                        .build())
                .withDefendants(
                        Arrays.asList(createDefendant("dddd3333-1e20-4c21-916a-81a6c90239e5"),
                                createDefendant("dddd4444-1e20-4c21-916a-81a6c90239e5")
                        ))
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
                                .withTitle(Title.MS)
                                .withGender(Gender.NOT_KNOWN)
                                .build())
                        .build())
                .withOffences(Arrays.asList(Offence.offence()
                        .withId(randomUUID())
                        .withOffenceDefinitionId(randomUUID())
                        .withOffenceCode(STRING.next())
                        .withWording(STRING.next())
                        .withStartDate(LocalDate.now())
                        .withOrderIndex(65)
                        .withCount(434)
                        .build()))
                .build();
    }
}
