package uk.gov.moj.cpp.results.event.helper.results;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AttendanceDayTest {

    private static final UUID DEFAULT_DEFENDANT_ID = fromString("dddd1111-1e20-4c21-916a-81a6c90239e5");

    @InjectMocks
    uk.gov.moj.cpp.results.event.helper.results.AttendanceDay attendanceDay;


    @Test
    public void testBuildAttendance() {

        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES);
        final Hearing hearing = shareResultsMessage.getHearing();

        final List<AttendanceDay> attendanceDays = attendanceDay.buildAttendance(hearing.getDefendantAttendance(), DEFAULT_DEFENDANT_ID);
        final uk.gov.justice.core.courts.AttendanceDay attendanceDay = attendanceDays.get(0);
        assertThat(attendanceDay.getAttendanceType(), is(AttendanceType.IN_PERSON));
        assertThat(attendanceDay.getDay(), is(LocalDate.of(2018, 05, 02)));

    }

}
