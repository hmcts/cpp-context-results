package uk.gov.moj.cpp.data.anonymization.generator;

import static java.time.LocalDate.now;
import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PastDateGenerator implements Generator<String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SecureRandomLongGenerator secureRandomLongGenerator = new SecureRandomLongGenerator();

    @Override
    public String convert() {
        return randomDateBetweenTwentyYearsInThePastAndNow().format(FORMATTER);
    }

    private LocalDate randomDateBetweenTwentyYearsInThePastAndNow() {

        final LocalDate now = now();
        final LocalDate minDate = now.minusYears(20);
        final long daysBetween = DAYS.between(minDate, now);

        return now.minusDays(secureRandomLongGenerator.nextLong(daysBetween) + 1L);
    }
}
