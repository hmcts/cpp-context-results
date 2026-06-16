package uk.gov.moj.cpp.results.domain.informant.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

public class InformantRegisterOffenceTest {

    @Test
    public void builder_setsVerdictField() {
        final Verdict verdict = Verdict.verdict().withVerdictCode("G").withVerdictDate("2026-04-13").build();

        final InformantRegisterOffence offence = InformantRegisterOffence.informantRegisterOffence()
                .withOffenceCode("AB001")
                .withOffenceTitle("Theft")
                .withOrderIndex(1)
                .withVerdict(verdict)
                .build();

        assertThat(offence.getVerdict(), is(notNullValue()));
        assertThat(offence.getVerdict().getVerdictCode(), is("G"));
        assertThat(offence.getVerdict().getVerdictDate(), is("2026-04-13"));
        assertThat(offence.getOffenceCode(), is("AB001"));
        assertThat(offence.getOffenceTitle(), is("Theft"));
        assertThat(offence.getOrderIndex(), is(1));
    }

    @Test
    public void builder_withoutVerdict_verdictIsNull() {
        final InformantRegisterOffence offence = InformantRegisterOffence.informantRegisterOffence()
                .withOffenceCode("AB001")
                .withOffenceTitle("Theft")
                .withOrderIndex(1)
                .build();

        assertThat(offence.getVerdict(), is(nullValue()));
    }

    @Test
    public void jacksonDeserialization_withVerdictObject_deserializesCorrectly() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = "{\"offenceCode\":\"AB001\",\"offenceTitle\":\"Theft\",\"orderIndex\":1," +
                "\"verdict\":{\"verdictCode\":\"G\",\"verdictDate\":\"2026-04-13\",\"verdictType\":\"FOUND_GUILTY\"}}";

        final InformantRegisterOffence offence = mapper.readValue(json, InformantRegisterOffence.class);

        assertThat(offence.getOffenceCode(), is("AB001"));
        assertThat(offence.getVerdict(), is(notNullValue()));
        assertThat(offence.getVerdict().getVerdictCode(), is("G"));
    }

    @Test
    public void jacksonDeserialization_withNoVerdict_verdictIsNull() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = "{\"offenceCode\":\"AB001\",\"offenceTitle\":\"Theft\",\"orderIndex\":1}";

        final InformantRegisterOffence offence = mapper.readValue(json, InformantRegisterOffence.class);

        assertThat(offence.getVerdict(), is(nullValue()));
    }
}
