package uk.gov.moj.cpp.results.event.helper.results;

import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CommonMethods {

    private static final String POLICE_URN_DEFAULT_VALUE = "00PP0000008";
    private static final String N = "N";
    private static final String Y = "Y";

    private CommonMethods() {
    }

    public static String getPresentAtHearing(final List<uk.gov.justice.core.courts.AttendanceDay> attendanceDays, final Hearing hearing, final Defendant defendant) {
        String result = N;
        final Optional<ZonedDateTime> sittingDayOptional = hearing.getHearingDays().stream().map(HearingDay::getSittingDay).findFirst();
        if (null != attendanceDays && sittingDayOptional.isPresent()) {
            final Optional<AttendanceDay> attendanceDay = attendanceDays.stream().filter(a -> a.getDay().equals(sittingDayOptional.get().toLocalDate()) && a.getAttendanceType()!= AttendanceType.NOT_PRESENT).findFirst();
            if (attendanceDay.isPresent()) {
                result = Y;
            }
        }
        if (N.equals(result) && getDefenceCounsel(hearing, defendant, sittingDayOptional).isPresent()) {
            result = "A";
        }
        return result;
    }

    private static Optional<UUID> getDefenceCounsel(final Hearing hearing, final Defendant defendant, final Optional<ZonedDateTime> sittingDayOptional) {
        Optional<UUID> defendantDefenceCounsel = empty();
        if (null != hearing.getDefenceCounsels() && sittingDayOptional.isPresent()) {
            defendantDefenceCounsel = hearing.getDefenceCounsels().stream()
                    .filter(d -> d.getAttendanceDays().contains(sittingDayOptional.get().toLocalDate()))
                    .map(DefenceCounsel::getDefendants)
                    .flatMap(Collection::stream)
                    .filter(a -> a.equals(defendant.getId())).findFirst();
        }
        return defendantDefenceCounsel;
    }

    public static String getUrn(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        if (isNotEmpty(prosecutionCaseIdentifier.getCaseURN())) {
            return prosecutionCaseIdentifier.getCaseURN();
        } else if (isNotEmpty(prosecutionCaseIdentifier.getProsecutionAuthorityReference())) {
            return prosecutionCaseIdentifier.getProsecutionAuthorityReference();
        }
        return POLICE_URN_DEFAULT_VALUE;
    }
}
