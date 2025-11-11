package uk.gov.moj.cpp.results.event.helper.results;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
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
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.getPresentAtHearing;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.getUrn;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.isUrnFormatValid;

import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommonMethodsTest {
    private static final UUID DEFAULT_DEFENDANT_ID1 = fromString("dddd1111-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID3 = fromString("dddd3333-1e20-4c21-916a-81a6c90239e5");
    private static final String DEFAULT_URN = "URN12345";
    private static final String PROSECUTION_AUTHORITY_REFERENCE = "reference";
    private static final String POLICE_URN_DEFAULT_VALUE = "00PP0000008";
    private static final String NON_POLICE_URN_DEFAULT_VALUE = "00NP0000008";

    @Test
    public void testGetPresentAtHearingAsYWhenDefendantIsPresentAtHearing() {
        final List<AttendanceDay> attendanceDays = new ArrayList<>();
        attendanceDays.add(AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.of(2018, 6, 4)).build());

        final String result = getPresentAtHearing(attendanceDays, buildHearing(), buildDefendant());
        assertThat(result, is("Y"));
    }

    @Test
    public void testGetPresentAtHearingAsAWhenDefenceCounselPresentAtHearing() {
        final List<AttendanceDay> attendanceDays = new ArrayList<>();
        attendanceDays.add(AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.NOT_PRESENT).withDay(LocalDate.of(2019, 02, 02)).build());

        final String result = getPresentAtHearing(attendanceDays, buildHearingForDefenceCounsel(), buildDefendant());
        assertThat(result, is("A"));
    }

    @Test
    public void testGetPresentAtHearingAsNWhenNoOneIsPresentAtHearing() {
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
    public void testGetUrn() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier().withCaseURN(DEFAULT_URN)
                .withProsecutionAuthorityCode("12345")
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .withProsecutionAuthorityReference("reference")
                .withCaseURN("URN-12345678")
                .build();
        String result = getUrn(prosecutionCaseIdentifier, true, false);
        assertThat(result, is("URN-12345678"));
    }

    @Test
    public void testGetUrnWhenCourtApplicationCasesUrnsSupplied() {
        final String URN1 = randomUUID().toString();
        final String URN2 = randomUUID().toString();
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withCourtApplicationCases(Arrays.asList(CourtApplicationCase.courtApplicationCase()
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN1).build()).build(),
                        CourtApplicationCase.courtApplicationCase()
                                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN2).build()).build())).build();
        final String extractedURNs = getUrn(courtApplication,true);

        assertThat(extractedURNs.split(",").length,is(2));
        assertTrue(Arrays.stream(extractedURNs.split(",")).allMatch(s -> s.equals(URN1) || s.equals(URN2)));
    }

    @Test
    public void testGetUrnWhencourtOrderUrnsSupplied() {
        final String URN1 = randomUUID().toString();
        final String URN2 = randomUUID().toString();
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(
                                Arrays.asList(CourtOrderOffence.courtOrderOffence()
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                                .withCaseURN(URN1).build()).build(),
                                        CourtOrderOffence.courtOrderOffence()
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                                .withCaseURN(URN2).build()).build()))
                        .build()).build();
        final String extractedURNs = getUrn(courtApplication,true);

        assertThat(extractedURNs.split(",").length,is(2));
        assertTrue(Arrays.stream(extractedURNs.split(",")).allMatch(s -> s.equals(URN1) || s.equals(URN2)));
    }

    @Test
    public void testGetUrnWhenURNIsEmptyForPoliceProsecutor() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .build();

        final String result = getUrn(prosecutionCaseIdentifier, true, false);
        assertThat(result, is(POLICE_URN_DEFAULT_VALUE));
    }

    @Test
    public void testGetUrnWhenURNIsEmptyForNonPoliceProsecutor() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .build();

        final String result = getUrn(prosecutionCaseIdentifier, false, false);
        assertThat(result, is(NON_POLICE_URN_DEFAULT_VALUE));
    }

    @Test
    public void testGetUrnWhenURNIsInValidForNonPoliceProsecutor() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .withCaseURN("20PP12345212")
                .build();

        final String result = getUrn(prosecutionCaseIdentifier, false, false);
        assertThat(result, is(NON_POLICE_URN_DEFAULT_VALUE));
    }

    @Test
    public void testGetUrnWhenURNIsValidForNonPoliceProsecutor() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier()
                .withProsecutionAuthorityId(DEFAULT_DEFENDANT_ID1)
                .withCaseURN("20PP1234521")
                .build();

        final String result = getUrn(prosecutionCaseIdentifier, false, true);
        assertThat(result, is("20PP1234521"));
    }

    @Test
    public void testValidUrn() {
        final String urn = "20PP1234521";
        assertTrue(isUrnFormatValid(urn));
    }

    @Test
    public void testInValidUrnWrongLength() {
        final String urn = "20PP12345212";
        assertFalse(isUrnFormatValid(urn));
        final String anotherURN = "sdfjshkfsdkfhksdhhsdkhfk";
        assertFalse(isUrnFormatValid(anotherURN));
    }

    @Test
    public void testInValidUrnWrongForceCode() {
        final String urn = "201PP123452";
        assertFalse(isUrnFormatValid(urn));
    }

    @Test
    public void testInValidUrnWrongSubDivisionCode() {
        final String urn = "20PPA123452";
        assertFalse(isUrnFormatValid(urn));
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
                .withDefenceCounsels(ImmutableList.of(defenceCounsel().withId(randomUUID()).withAttendanceDays(ImmutableList.of(now(), LocalDate.of(2018, 6, 4))).withDefendants(ImmutableList.of(randomUUID(), randomUUID(), DEFAULT_DEFENDANT_ID1)).build(),
                        defenceCounsel().withId(randomUUID()).withAttendanceDays(ImmutableList.of(now())).withDefendants(ImmutableList.of(randomUUID(), DEFAULT_DEFENDANT_ID3, randomUUID())).build()))

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
