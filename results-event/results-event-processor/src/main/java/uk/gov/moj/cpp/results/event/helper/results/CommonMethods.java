package uk.gov.moj.cpp.results.event.helper.results;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;

public class CommonMethods {
    private static final String POLICE_URN_DEFAULT_VALUE = "00PP0000008";
    private static final String NON_POLICE_URN_DEFAULT_VALUE = "00NP0000008";
    private static final String N = "N";
    private static final String Y = "Y";
    private static final String PATTERN_URN = "\\d{2}[a-z |A-Z]{2}\\d{7}";

    private CommonMethods() {
    }

    public static String getPresentAtHearing(final List<uk.gov.justice.core.courts.AttendanceDay> attendanceDays, final Hearing hearing, final MasterDefendant defendant) {
        String result = N;
        final Optional<ZonedDateTime> sittingDayOptional = hearing.getHearingDays().stream().map(HearingDay::getSittingDay).findFirst();
        if (null != attendanceDays && sittingDayOptional.isPresent()) {
            final Optional<AttendanceDay> attendanceDay = attendanceDays.stream().filter(a -> a.getDay().equals(sittingDayOptional.get().toLocalDate()) && a.getAttendanceType() != AttendanceType.NOT_PRESENT).findFirst();
            if (attendanceDay.isPresent()) {
                result = Y;
            }
        }
        if (N.equals(result) && getDefenceCounsel(hearing, defendant, sittingDayOptional).isPresent()) {
            result = "A";
        }
        return result;
    }

    public static String getPresentAtHearing(final List<uk.gov.justice.core.courts.AttendanceDay> attendanceDays, final Hearing hearing, final Defendant defendant) {
        String result = N;
        final Optional<ZonedDateTime> sittingDayOptional = hearing.getHearingDays().stream().map(HearingDay::getSittingDay).findFirst();
        if (null != attendanceDays && sittingDayOptional.isPresent()) {
            final Optional<AttendanceDay> attendanceDay = attendanceDays.stream().filter(a -> a.getDay().equals(sittingDayOptional.get().toLocalDate()) && a.getAttendanceType() != AttendanceType.NOT_PRESENT).findFirst();
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

    private static Optional<UUID> getDefenceCounsel(final Hearing hearing, final MasterDefendant defendant, final Optional<ZonedDateTime> sittingDayOptional) {
        Optional<UUID> defendantDefenceCounsel = empty();
        if (null != hearing.getDefenceCounsels() && sittingDayOptional.isPresent()) {
            defendantDefenceCounsel = hearing.getDefenceCounsels().stream()
                    .filter(d -> d.getAttendanceDays().contains(sittingDayOptional.get().toLocalDate()))
                    .map(DefenceCounsel::getDefendants)
                    .flatMap(Collection::stream)
                    .filter(a -> a.equals(defendant.getMasterDefendantId())).findFirst();
        }
        return defendantDefenceCounsel;
    }

    public static String getUrn(final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final boolean isPoliceProsecutor, final boolean isURNValid) {
        if (isNotEmpty(prosecutionCaseIdentifier.getCaseURN()) && (isPoliceProsecutor || isURNValid)) {
            return prosecutionCaseIdentifier.getCaseURN();
        }

        if (isPoliceProsecutor) {
            return POLICE_URN_DEFAULT_VALUE;
        }

        return NON_POLICE_URN_DEFAULT_VALUE;
    }

    public static String getUrn(final CourtApplication courtApplication, final boolean isPoliceProsecutor) {

        final List<String> urnList = new ArrayList<>();

        final Stream<CourtApplicationCase> courtApplicationCasesStream = ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty);

        final Stream<CourtOrderOffence> courtOrderOffenceStream = ofNullable(courtApplication.getCourtOrder()).map(courtOrder -> courtOrder.getCourtOrderOffences().stream()).orElseGet(Stream::empty);

        final List<String> courtApplicationCasesUrn = courtApplicationCasesStream.map(c -> getUrn(c.getProsecutionCaseIdentifier(), isPoliceProsecutor, true)).collect(Collectors.toList());

        final List<String> courtOrderUrn = courtOrderOffenceStream.map(o -> getUrn(o.getProsecutionCaseIdentifier(), isPoliceProsecutor, true)).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(courtApplicationCasesUrn)) {
            urnList.addAll(courtApplicationCasesUrn);
        }

        if (CollectionUtils.isNotEmpty(courtOrderUrn)) {
            urnList.addAll(courtOrderUrn);
        }

        return String.join(",", urnList);
    }

    public static String getCode(final CourtApplication courtApplication) {

        final List<String> urnList = new ArrayList<>();

        final Stream<CourtApplicationCase> courtApplicationCasesStream = ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty);

        final Stream<CourtOrderOffence> courtOrderOffenceStream = ofNullable(courtApplication.getCourtOrder()).map(courtOrder -> courtOrder.getCourtOrderOffences().stream()).orElseGet(Stream::empty);

        final List<String> courtApplicationCasesCode = courtApplicationCasesStream.map(c -> c.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()).collect(Collectors.toList());

        final List<String> courtOrderCode = courtOrderOffenceStream.map(o -> o.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(courtApplicationCasesCode)) {
            urnList.addAll(courtApplicationCasesCode);
        }

        if (CollectionUtils.isNotEmpty(courtOrderCode)) {
            urnList.addAll(courtOrderCode);
        }

        return String.join(",", urnList);
    }

    public static boolean checkURNValidity(final String caseURN) {
        return nonNull(caseURN) && isUrnFormatValid(caseURN);
    }

    public static boolean isUrnFormatValid(final String urn) {
        final Pattern pattern = Pattern.compile(PATTERN_URN);
        final Matcher matcher = pattern.matcher(urn);
        return matcher.matches();
    }
}
