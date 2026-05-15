package uk.gov.moj.cpp.results.event.helper.results;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.List.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.core.courts.DefenceCounsel.defenceCounsel;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.core.courts.HearingDay.hearingDay;
import static uk.gov.justice.core.courts.MasterDefendant.masterDefendant;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.checkBichardPtiURNValidity;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.checkURNValidity;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.getPresentAtHearing;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.getUrn;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.isPtiUrnFormatValid;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.isUrnFormatValid;

import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommonMethodsTest {
    private static final UUID DEFAULT_DEFENDANT_ID1 = fromString("dddd1111-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID3 = fromString("dddd3333-1e20-4c21-916a-81a6c90239e5");
    private static final String DEFAULT_URN = "URN12345";
    private static final String POLICE_URN_DEFAULT_VALUE = "00PP0000008";
    private static final String NON_POLICE_URN_DEFAULT_VALUE = "00NP0000008";

    @Test
    void testGetPresentAtHearingAsYWhenDefendantIsPresentAtHearing() {
        final List<AttendanceDay> attendanceDays = new ArrayList<>();
        attendanceDays.add(AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.of(2018, 6, 4)).build());

        final String result = getPresentAtHearing(attendanceDays, buildHearing(), buildDefendant());
        assertThat(result, is("Y"));
    }

    @Test
    void testGetPresentAtHearingAsAWhenDefenceCounselPresentAtHearing() {
        final List<AttendanceDay> attendanceDays = new ArrayList<>();
        attendanceDays.add(AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.NOT_PRESENT).withDay(LocalDate.of(2019, 02, 02)).build());

        final String result = getPresentAtHearing(attendanceDays, buildHearingForDefenceCounsel(), buildDefendant());
        assertThat(result, is("A"));
    }

    @Test
    void testGetPresentAtHearingAsNWhenNoOneIsPresentAtHearing() {
        final List<AttendanceDay> attendanceDays = new ArrayList<>();
        attendanceDays.add(AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.NOT_PRESENT).withDay(LocalDate.of(2019, 02, 02)).build());

        final Hearing hearing = hearing().withHearingDays(Collections.singletonList(hearingDay()
                .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 6, 1), LocalTime.of(12, 3, 10), ZoneId.systemDefault()))
                .withListedDurationMinutes(100)
                .withListingSequence(10)
                .build())).build();
        final String result = getPresentAtHearing(attendanceDays, hearing, buildDefendant());
        assertThat(result, is("N"));
    }

    @Test
    void testGetUrn() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier().withCaseURN(DEFAULT_URN)
                .withProsecutionAuthorityCode("12345")
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .withProsecutionAuthorityReference("reference")
                .withCaseURN("URN-12345678")
                .build();
        String result = getUrn(prosecutionCaseIdentifier, true, false, false);
        assertThat(result, is("URN-12345678"));
    }

    @Test
    void testGetUrnWhenURNIsEmptyForPoliceProsecutor() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .build();

        final String result = getUrn(prosecutionCaseIdentifier, true, false, false);
        assertThat(result, is(POLICE_URN_DEFAULT_VALUE));
    }

    @Test
    void testGetUrnWhenURNIsEmptyForNonPoliceProsecutor() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .build();

        final String result = getUrn(prosecutionCaseIdentifier, false, false, false);
        assertThat(result, is(NON_POLICE_URN_DEFAULT_VALUE));
    }

    @Test
    void testGetUrnWhenURNIsInValidForNonPoliceProsecutor() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .withCaseURN("20PP12345212")
                .build();

        final String result = getUrn(prosecutionCaseIdentifier, false, false, false);
        assertThat(result, is(NON_POLICE_URN_DEFAULT_VALUE));
    }

    @Test
    void testGetUrnWhenURNIsValidForNonPoliceProsecutor() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .withCaseURN("20PP1234521")
                .build();

        final String result = getUrn(prosecutionCaseIdentifier, false, true, false);
        assertThat(result, is("20PP1234521"));
    }

    @Test
    void testGetUrnWhenCivilWithValidBichardPtiUrn() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .withCaseURN("ABCD2242123")
                .build();

        final String result = getUrn(prosecutionCaseIdentifier, false, false, true);
        assertThat(result, is("ABCD2242123"));
    }

    @Test
    void testGetUrnWhenCivilWithInvalidPtiUrnForPoliceProsecutor() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .withCaseURN("invalid-pti-urn")
                .build();

        final String result = getUrn(prosecutionCaseIdentifier, true, false, true);
        assertThat(result, is(POLICE_URN_DEFAULT_VALUE));
    }

    @Test
    void testGetUrnWhenCivilWithInvalidPtiUrnForNonPoliceProsecutor() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .withCaseURN("invalid-pti-urn")
                .build();

        final String result = getUrn(prosecutionCaseIdentifier, false, false, true);
        assertThat(result, is(NON_POLICE_URN_DEFAULT_VALUE));
    }

    @Test
    void testGetUrnWhenCivilWithNullUrnForPoliceProsecutor() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .build();

        final String result = getUrn(prosecutionCaseIdentifier, true, false, true);
        assertThat(result, is(POLICE_URN_DEFAULT_VALUE));
    }

    @Test
    void testGetUrnWhenCivilWithNullUrnForNonPoliceProsecutor() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .build();

        final String result = getUrn(prosecutionCaseIdentifier, false, false, true);
        assertThat(result, is(NON_POLICE_URN_DEFAULT_VALUE));
    }

    @Test
    void testCheckURNValidityForNullUrn() {
        assertFalse(checkURNValidity(null));
    }

    @Test
    void testCheckURNValidityForValidUrn() {
        assertTrue(checkURNValidity("20PP1234521"));
    }

    @Test
    void testCheckURNValidityForInvalidUrn() {
        assertFalse(checkURNValidity("20PP12345212"));
    }

    @Test
    void testCheckBichardPtiURNValidityForNullUrn() {
        assertFalse(checkBichardPtiURNValidity(null));
    }

    @Test
    void testCheckBichardPtiURNValidityForValidUrn() {
        assertTrue(checkBichardPtiURNValidity("ABCD2242123"));
    }

    @Test
    void testCheckBichardPtiURNValidityForInvalidUrn() {
        assertFalse(checkBichardPtiURNValidity("invalid-pti-urn"));
    }

    @Test
    void testValidUrn() {
        final String urn = "20PP1234521";
        assertTrue(isUrnFormatValid(urn));
    }

    @Test
    void testPtiValidUrn() {
        final String urn1 = "12342242123";
        assertTrue(isPtiUrnFormatValid(urn1));
        final String urn2 = "GD137523211";
        assertTrue(isPtiUrnFormatValid(urn2));
        final String urn3 = "12GD2242123";
        assertTrue(isPtiUrnFormatValid(urn3));
        final String urn4 = "ABCD2242123";
        assertTrue(isPtiUrnFormatValid(urn4));
        final String urn5 = "ABCD22421";
        assertTrue(isPtiUrnFormatValid(urn5));
    }

    @Test
    void testInValidUrnWrongLength() {
        final String urn = "20PP12345212";
        assertFalse(isUrnFormatValid(urn));
        final String anotherURN = "sdfjshkfsdkfhksdhhsdkhfk";
        assertFalse(isUrnFormatValid(anotherURN));
    }

    @Test
    void testInValidUrnWrongForceCode() {
        final String urn = "201PP123452";
        assertFalse(isUrnFormatValid(urn));
    }

    @Test
    void testInValidUrnWrongSubDivisionCode() {
        final String urn = "20PPA123452";
        assertFalse(isUrnFormatValid(urn));
    }

    @Test
    void testInValidPtiUrnTooShort() {
        final String urn = "ABCD22";
        assertFalse(isPtiUrnFormatValid(urn));
    }

    @Test
    void testInValidPtiUrnTooLong() {
        final String urn = "ABCD22421234";
        assertFalse(isPtiUrnFormatValid(urn));
    }

    @Test
    void testInValidPtiUrnLowerCase() {
        final String urn = "abcd2242123";
        assertFalse(isPtiUrnFormatValid(urn));
    }

    @Test
    void testInValidPtiUrnWithSpecialCharacters() {
        final String urn = "AB-D2242123";
        assertFalse(isPtiUrnFormatValid(urn));
    }

    @Test
    void testInValidPtiUrnWithNonDigitsAfterPrefix() {
        final String urn = "ABCDABCD123";
        assertFalse(isPtiUrnFormatValid(urn));
    }

    @Test
    void testGetPresentAtHearingAsYWhenMasterDefendantIsPresentAtHearing() {
        final List<AttendanceDay> attendanceDays = new ArrayList<>();
        attendanceDays.add(AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.of(2018, 6, 4)).build());

        final String result = getPresentAtHearing(attendanceDays, buildHearing(), buildMasterDefendant());
        assertThat(result, is("Y"));
    }

    @Test
    void testGetPresentAtHearingAsAWhenDefenceCounselPresentAtHearingForMasterDefendant() {
        final List<AttendanceDay> attendanceDays = new ArrayList<>();
        attendanceDays.add(AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.NOT_PRESENT).withDay(LocalDate.of(2019, 02, 02)).build());

        final String result = getPresentAtHearing(attendanceDays, buildHearingForDefenceCounsel(), buildMasterDefendant());
        assertThat(result, is("A"));
    }

    @Test
    void testGetPresentAtHearingAsNWhenNoOneIsPresentAtHearingForMasterDefendant() {
        final List<AttendanceDay> attendanceDays = new ArrayList<>();
        attendanceDays.add(AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.NOT_PRESENT).withDay(LocalDate.of(2019, 02, 02)).build());

        final Hearing hearing = hearing().withHearingDays(Collections.singletonList(hearingDay()
                .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 6, 1), LocalTime.of(12, 3, 10), ZoneId.systemDefault()))
                .withListedDurationMinutes(100)
                .withListingSequence(10)
                .build())).build();
        final String result = getPresentAtHearing(attendanceDays, hearing, buildMasterDefendant());
        assertThat(result, is("N"));
    }

    @Test
    void testGetPresentAtHearingAsAWhenAttendanceDaysIsNullAndDefenceCounselPresent() {
        final String result = getPresentAtHearing(null, buildHearingForDefenceCounsel(), buildDefendant());
        assertThat(result, is("A"));
    }

    @Test
    void testGetPresentAtHearingAsAWhenAttendanceDaysIsNullAndDefenceCounselPresentForMasterDefendant() {
        final String result = getPresentAtHearing(null, buildHearingForDefenceCounsel(), buildMasterDefendant());
        assertThat(result, is("A"));
    }

    @Test
    void testGetPresentAtHearingAsNWhenAttendanceDaysIsNullAndNoDefenceCounsels() {
        final String result = getPresentAtHearing(null, buildHearing(), buildDefendant());
        assertThat(result, is("N"));
    }

    @Test
    void testGetPresentAtHearingAsNWhenAttendanceDaysIsNullAndNoDefenceCounselsForMasterDefendant() {
        final String result = getPresentAtHearing(null, buildHearing(), buildMasterDefendant());
        assertThat(result, is("N"));
    }

    private MasterDefendant buildMasterDefendant() {
        return masterDefendant().withMasterDefendantId(DEFAULT_DEFENDANT_ID1).build();
    }


    private Defendant buildDefendant() {
        return defendant().withId(DEFAULT_DEFENDANT_ID1).build();
    }

    private Hearing buildHearing() {
        return hearing().withHearingDays(asList(
                hearingDay()
                        .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 6, 4), LocalTime.of(12, 1, 1), ZoneId.systemDefault()))
                        .withListedDurationMinutes(100)
                        .withListingSequence(10)
                        .build(),
                hearingDay()
                        .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 5, 2), LocalTime.of(12, 1, 1), ZoneId.systemDefault()))
                        .withListedDurationMinutes(100)
                        .withListingSequence(10)
                        .build(),
                hearingDay()
                        .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 6, 1), LocalTime.of(12, 3, 10), ZoneId.systemDefault()))
                        .withListedDurationMinutes(100)
                        .withListingSequence(10)
                        .build())).build();
    }

    private Hearing buildHearingForDefenceCounsel() {
        return hearing()
                .withDefenceCounsels(of(defenceCounsel().withId(randomUUID()).withAttendanceDays(of(now(), LocalDate.of(2018, 6, 4))).withDefendants(of(randomUUID(), randomUUID(), DEFAULT_DEFENDANT_ID1)).build(),
                        defenceCounsel().withId(randomUUID()).withAttendanceDays(of(now())).withDefendants(of(randomUUID(), DEFAULT_DEFENDANT_ID3, randomUUID())).build()))

                .withHearingDays(asList(
                        hearingDay()
                                .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 6, 4), LocalTime.of(12, 1, 1), ZoneId.systemDefault()))
                                .withListedDurationMinutes(100)
                                .withListingSequence(10)
                                .build(),
                        hearingDay()
                                .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 5, 2), LocalTime.of(12, 1, 1), ZoneId.systemDefault()))
                                .withListedDurationMinutes(100)
                                .withListingSequence(10)
                                .build(),
                        hearingDay()
                                .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 6, 1), LocalTime.of(12, 3, 10), ZoneId.systemDefault()))
                                .withListedDurationMinutes(100)
                                .withListingSequence(10)
                                .build())).build();

    }
}
