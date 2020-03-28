package uk.gov.moj.cpp.domains.resultStructure;

import java.io.Serializable;
import java.time.LocalDate;

public class AttendanceDay implements Serializable {
    private LocalDate day;
    private Boolean isInAttendance;
    private long serialVersionUID = -9176890205806560222L;

    public AttendanceDay(LocalDate day, Boolean isInAttendance) {
        this.day = day;
        this.isInAttendance = isInAttendance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AttendanceDay that = (AttendanceDay) o;

        if (day != null ? !day.equals(that.day) : that.day != null) {
            return false;
        }
        return isInAttendance != null ? isInAttendance.equals(that.isInAttendance) : that.isInAttendance == null;
    }

    @Override
    public int hashCode() {
        int result = day != null ? day.hashCode() : 0;
        result = 31 * result + (isInAttendance != null ? isInAttendance.hashCode() : 0);
        return result;
    }
}
