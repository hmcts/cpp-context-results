package uk.gov.moj.cpp.data.anonymization;

import static java.time.LocalDate.parse;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.data.anonymization.generator.FutureDateGenerator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

public class FutureDateGeneratorTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Test
    public void shouldGenerateHostName() {

        for (int index = 0; index < 100; index++) {
            final LocalDate now = LocalDate.now();

            final FutureDateGenerator futureDateGenerator = new FutureDateGenerator();
            final String futureDate = futureDateGenerator.convert("2018-05-19");
            final LocalDate resultDate = parse(futureDate, FORMATTER);

            assertThat(resultDate.isAfter(now), is(true));
        }
    }
}