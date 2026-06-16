package uk.gov.moj.cpp.results.domain.informant.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

public class InformantRegisterResultDataTest {

    @Test
    public void builder_setsFields() {
        final InformantRegisterResultData data = InformantRegisterResultData.informantRegisterResultData()
                .withAmount("50.00")
                .withDurationValue("6")
                .withDurationUnit("MONTHS")
                .withNextHearingDate("2026-09-01T10:00:00Z")
                .build();

        assertThat(data.getAmount(), is("50.00"));
        assertThat(data.getDurationValue(), is("6"));
        assertThat(data.getDurationUnit(), is("MONTHS"));
        assertThat(data.getNextHearingDate(), is("2026-09-01T10:00:00Z"));
    }

    @Test
    public void builder_allFieldsOptional() {
        final InformantRegisterResultData data = InformantRegisterResultData.informantRegisterResultData().build();

        assertThat(data.getAmount(), is(nullValue()));
        assertThat(data.getDurationValue(), is(nullValue()));
    }

    @Test
    public void jacksonDeserialization_roundtrip() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = "{\"amount\":\"50.00\",\"durationValue\":\"6\",\"durationUnit\":\"MONTHS\"}";

        final InformantRegisterResultData data = mapper.readValue(json, InformantRegisterResultData.class);

        assertThat(data.getAmount(), is("50.00"));
        assertThat(data.getDurationValue(), is("6"));
        assertThat(data.getDurationUnit(), is("MONTHS"));
    }
}
