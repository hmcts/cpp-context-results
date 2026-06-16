package uk.gov.moj.cpp.results.domain.informant.model;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

public class InformantRegisterHearingTest {

    @Test
    public void builder_setsAllFields() {
        final InformantRegisterDefendant defendant = InformantRegisterDefendant.informantRegisterDefendant()
                .withName("John").withAddress1("1 Street").build();

        final InformantRegisterHearing hearing = InformantRegisterHearing.informantRegisterHearing()
                .withCourtRoom("Room 1")
                .withHearingStartTime("09:00")
                .withDefendants(singletonList(defendant))
                .build();

        assertThat(hearing.getCourtRoom(), is("Room 1"));
        assertThat(hearing.getHearingStartTime(), is("09:00"));
        assertThat(hearing.getDefendants(), hasSize(1));
    }

    @Test
    public void jacksonDeserialization_roundtrip() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = "{\"courtRoom\":\"Room 1\",\"hearingStartTime\":\"09:00\",\"defendants\":[" +
                "{\"name\":\"John\",\"address1\":\"1 Street\"}]}";

        final InformantRegisterHearing hearing = mapper.readValue(json, InformantRegisterHearing.class);

        assertThat(hearing.getCourtRoom(), is("Room 1"));
        assertThat(hearing.getDefendants(), hasSize(1));
    }
}
