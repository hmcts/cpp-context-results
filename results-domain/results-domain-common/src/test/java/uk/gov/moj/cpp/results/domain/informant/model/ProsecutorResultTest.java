package uk.gov.moj.cpp.results.domain.informant.model;

import static java.time.LocalDate.parse;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

public class ProsecutorResultTest {

    @Test
    public void builder_setsRequiredFields() {
        final ProsecutorResult result = ProsecutorResult.prosecutorResult()
                .withStartDate(parse("2026-04-01"))
                .withProsecutionAuthorityId(randomUUID())
                .withProsecutionAuthorityCode("TFL")
                .build();

        assertThat(result.getStartDate(), is(parse("2026-04-01")));
        assertThat(result.getProsecutionAuthorityCode(), is("TFL"));
    }

    @Test
    public void builder_optionalFieldsAreNull() {
        final ProsecutorResult result = ProsecutorResult.prosecutorResult()
                .withStartDate(parse("2026-04-01"))
                .withProsecutionAuthorityId(randomUUID())
                .withProsecutionAuthorityCode("TFL")
                .build();

        assertThat(result.getEndDate(), is(nullValue()));
        assertThat(result.getProsecutionAuthorityName(), is(nullValue()));
        assertThat(result.getHearingVenues(), is(nullValue()));
    }
}
