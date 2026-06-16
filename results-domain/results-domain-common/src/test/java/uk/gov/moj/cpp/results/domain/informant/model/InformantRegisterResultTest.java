package uk.gov.moj.cpp.results.domain.informant.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

public class InformantRegisterResultTest {

    @Test
    public void builder_setsAllFields() {
        final InformantRegisterResultData data = InformantRegisterResultData.informantRegisterResultData()
                .withAmount("100.00").build();

        final InformantRegisterResult result = InformantRegisterResult.informantRegisterResult()
                .withResultText("Fined")
                .withCjsResultCode("F")
                .withResultData(data)
                .build();

        assertThat(result.getResultText(), is("Fined"));
        assertThat(result.getCjsResultCode(), is("F"));
        assertThat(result.getResultData(), is(notNullValue()));
        assertThat(result.getResultData().getAmount(), is("100.00"));
    }

    @Test
    public void builder_optionalFieldsAreNull() {
        final InformantRegisterResult result = InformantRegisterResult.informantRegisterResult()
                .withResultText("Fined")
                .build();

        assertThat(result.getCjsResultCode(), is(nullValue()));
        assertThat(result.getResultData(), is(nullValue()));
    }

    @Test
    public void jacksonDeserialization_roundtrip() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = "{\"resultText\":\"Fined\",\"cjsResultCode\":\"F\"}";

        final InformantRegisterResult result = mapper.readValue(json, InformantRegisterResult.class);

        assertThat(result.getResultText(), is("Fined"));
        assertThat(result.getCjsResultCode(), is("F"));
    }
}
