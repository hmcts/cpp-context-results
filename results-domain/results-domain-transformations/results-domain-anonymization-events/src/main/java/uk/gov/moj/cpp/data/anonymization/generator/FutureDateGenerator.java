package uk.gov.moj.cpp.data.anonymization.generator;

import static java.time.LocalDate.now;
import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FutureDateGenerator implements Generator<String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SecureRandomLongGenerator secureRandomLongGenerator = new SecureRandomLongGenerator();

    public String convert() {
        return randomDateBetweenNowAndTenYears().format(FORMATTER);
    }

    private LocalDate randomDateBetweenNowAndTenYears() {

        final LocalDate now = now();
        final LocalDate maxDate = now.plusYears(10);
        final long daysBetween = DAYS.between(now, maxDate);

        return now.plusDays(secureRandomLongGenerator.nextLong(daysBetween) + 1);
    }
}
