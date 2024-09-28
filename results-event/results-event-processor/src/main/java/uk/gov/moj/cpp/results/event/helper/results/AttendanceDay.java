package uk.gov.moj.cpp.results.event.helper.results;

import uk.gov.justice.core.courts.DefendantAttendance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AttendanceDay {

    public List<uk.gov.justice.core.courts.AttendanceDay> buildAttendance(final List<DefendantAttendance> defendantAttendance, final UUID defendantId) {
        final Optional<List<uk.gov.justice.core.courts.AttendanceDay>> attendanceDays = defendantAttendance.stream().filter(a -> a.getDefendantId().equals(defendantId)).findFirst().map(a -> a.getAttendanceDays());
        return attendanceDays.orElse(null);
    }
}
