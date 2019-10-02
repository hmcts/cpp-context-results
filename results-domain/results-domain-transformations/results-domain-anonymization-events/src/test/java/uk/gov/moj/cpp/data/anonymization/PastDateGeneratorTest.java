package uk.gov.moj.cpp.data.anonymization;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.data.anonymization.generator.PastDateGenerator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

public class PastDateGeneratorTest {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Test
    public void shouldGenerateHostName() {

        for (int index = 0; index < 100; index++) {
            final LocalDate now = LocalDate.now();

            final PastDateGenerator pastDateGenerator = new PastDateGenerator();
            final String pastDate = pastDateGenerator.convert("2018-05-19");
            final LocalDate resultDate = LocalDate.parse(pastDate, FORMATTER);

            assertThat(resultDate.isBefore(now), is(true));
        }
    }
}