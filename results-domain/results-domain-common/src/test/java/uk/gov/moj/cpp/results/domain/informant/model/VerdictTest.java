package uk.gov.moj.cpp.results.domain.informant.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

public class VerdictTest {

    @Test
    public void builder_setsAllThreeFields() {
        final Verdict verdict = Verdict.verdict()
                .withVerdictCode("G")
                .withVerdictDate("2026-04-13")
                .withVerdictType("FOUND_GUILTY")
                .build();

        assertThat(verdict.getVerdictCode(), is("G"));
        assertThat(verdict.getVerdictDate(), is("2026-04-13"));
        assertThat(verdict.getVerdictType(), is("FOUND_GUILTY"));
    }

    @Test
    public void builder_toleratesNullFields() {
        final Verdict verdict = Verdict.verdict().build();

        assertThat(verdict.getVerdictCode(), is(nullValue()));
        assertThat(verdict.getVerdictDate(), is(nullValue()));
        assertThat(verdict.getVerdictType(), is(nullValue()));
    }

    @Test
    public void jacksonDeserialization_roundtrip_preservesAllFields() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = "{\"verdictCode\":\"G\",\"verdictDate\":\"2026-04-13\",\"verdictType\":\"FOUND_GUILTY\"}";

        final Verdict verdict = mapper.readValue(json, Verdict.class);

        assertThat(verdict.getVerdictCode(), is("G"));
        assertThat(verdict.getVerdictDate(), is("2026-04-13"));
        assertThat(verdict.getVerdictType(), is("FOUND_GUILTY"));
    }

    @Test
    public void jacksonDeserialization_missingFields_producesNulls() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final Verdict verdict = mapper.readValue("{}", Verdict.class);

        assertThat(verdict.getVerdictCode(), is(nullValue()));
        assertThat(verdict.getVerdictDate(), is(nullValue()));
        assertThat(verdict.getVerdictType(), is(nullValue()));
    }
}
