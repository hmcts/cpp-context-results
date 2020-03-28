package uk.gov.moj.cpp.results.event.helper.sjp;

import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.justice.core.courts.JudicialResultPromptDurationElement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

public class DurationDateHelper {

    private static final Logger LOGGER = getLogger(DurationDateHelper.class);

    private static final String YEAR = "Y";
    private static final String MONTH = "M";
    private static final String DAY = "D";
    private static final String WEEK = "W";
    private static final String HOUR = "H";

    private DurationDateHelper() {

    }

    public static void populateStartAndEndDates(final JudicialResultPromptDurationElement.Builder builder, final ZonedDateTime dateAndTimeOfSession , final Pair<String, Integer> primaryValue) {
        final LocalDate  startDate = calculateStartDate(dateAndTimeOfSession);
        final LocalDateTime endDate = calculateEndDate(dateAndTimeOfSession, primaryValue);
        builder.withDurationStartDate(startDate.toString()).withDurationEndDate(endDate.toLocalDate().toString());
    }

    private static LocalDate calculateStartDate(final ZonedDateTime dateAndTimeOfSession ) {
        final LocalDate sjpSessionDate = dateAndTimeOfSession.toLocalDate();
        return nonNull(sjpSessionDate) ? sjpSessionDate : LocalDate.now();
    }


    private static LocalDateTime calculateEndDate(final ZonedDateTime zonedDateTime, final Pair<String, Integer> primaryValue) {

        final String  unit = primaryValue.getKey();
        final Integer value = primaryValue.getValue();

        final LocalDateTime hearingDate = zonedDateTime.toLocalDateTime();

        switch (unit) {
             case YEAR:
                return hearingDate.plusYears(value).minusDays(1);
            case MONTH:
                return hearingDate.plusMonths(value).minusDays(1);
            case DAY:
                return hearingDate.plusDays(value).minusDays(1);
            case WEEK:
                return hearingDate.plusWeeks(value).minusDays(1);
            case HOUR:
                return hearingDate.plusHours(value).minusDays(1);
            default:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Invalid or Unknown primary unit %s.Can not calculate end Date.End date will be same as start date.", unit));
                }
                return hearingDate;
        }
    }
}