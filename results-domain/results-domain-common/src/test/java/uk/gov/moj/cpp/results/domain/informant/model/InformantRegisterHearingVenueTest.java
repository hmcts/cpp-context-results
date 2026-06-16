package uk.gov.moj.cpp.results.domain.informant.model;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

public class InformantRegisterHearingVenueTest {

    @Test
    public void builder_setsRequiredFields() {
        final InformantRegisterHearing hearing = InformantRegisterHearing.informantRegisterHearing()
                .withCourtRoom("Room 1").withHearingStartTime("09:00").build();

        final InformantRegisterHearingVenue venue = InformantRegisterHearingVenue.informantRegisterHearingVenue()
                .withCourtHouse("Crown Court")
                .withLjaName("LJA1")
                .withCourtSessions(singletonList(hearing))
                .build();

        assertThat(venue.getCourtHouse(), is("Crown Court"));
        assertThat(venue.getLjaName(), is("LJA1"));
        assertThat(venue.getCourtSessions(), hasSize(1));
    }

    @Test
    public void builder_ljaNameOptional() {
        final InformantRegisterHearingVenue venue = InformantRegisterHearingVenue.informantRegisterHearingVenue()
                .withCourtHouse("Crown Court").build();

        assertThat(venue.getLjaName(), is(nullValue()));
    }

    @Test
    public void jacksonDeserialization_roundtrip() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = "{\"courtHouse\":\"Crown Court\",\"courtSessions\":[" +
                "{\"courtRoom\":\"Room 1\",\"hearingStartTime\":\"09:00\",\"defendants\":[]}]}";

        final InformantRegisterHearingVenue venue = mapper.readValue(json, InformantRegisterHearingVenue.class);

        assertThat(venue.getCourtHouse(), is("Crown Court"));
        assertThat(venue.getCourtSessions(), hasSize(1));
    }
}
