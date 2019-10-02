package uk.gov.moj.cpp.data.anonymization.generator;

import static java.time.LocalDate.now;
import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings({"squid:S2119", "squid:S2245"})
public class PastDateGenerator implements Generator<String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String convert(final String fieldValue) {
        return randomDateBetweenOneHundredYearsInThePastAndNow().format(FORMATTER);
    }

    private LocalDate randomDateBetweenOneHundredYearsInThePastAndNow() {

        final LocalDate now = now();
        final LocalDate minDate = now.minusYears(100);
        final long daysBetween = DAYS.between(minDate, now);

        return now.minusDays(ThreadLocalRandom.current().nextLong(daysBetween) + 1L);
    }
}
