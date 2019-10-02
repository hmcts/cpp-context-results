package uk.gov.moj.cpp.data.anonymization.generator;

import static java.time.LocalDate.now;
import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings({"squid:S2119", "squid:S2245"})
public class FutureDateGenerator implements Generator<String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String convert(final String fieldValue) {
        return randomDateBetweenNowAndTenYears().format(FORMATTER);
    }

    private LocalDate randomDateBetweenNowAndTenYears() {

        final LocalDate now = now();
        final LocalDate maxDate = now.plusYears(10);
        final long daysBetween = DAYS.between(now, maxDate);

        return now.plusDays(ThreadLocalRandom.current().nextLong(daysBetween) + 1L);
    }
}