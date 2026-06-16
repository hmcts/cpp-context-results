package uk.gov.moj.cpp.results.domain.informant.model;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;

public class InformantRegisterDocumentRequestTest {

    @Test
    public void builder_setsAllRequiredFields() {
        final UUID hearingId = randomUUID();
        final UUID prosecutionAuthorityId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.parse("2026-04-13T09:00:00Z");

        final InformantRegisterHearingVenue venue = InformantRegisterHearingVenue.informantRegisterHearingVenue()
                .withCourtHouse("Crown Court").build();

        final InformantRegisterDocumentRequest request = InformantRegisterDocumentRequest.informantRegisterDocumentRequest()
                .withRegisterDate(registerDate)
                .withHearingDate(registerDate)
                .withHearingId(hearingId)
                .withProsecutionAuthorityId(prosecutionAuthorityId)
                .withProsecutionAuthorityCode("TFL")
                .withFileName("file.csv")
                .withHearingVenue(venue)
                .build();

        assertThat(request.getRegisterDate(), is(registerDate));
        assertThat(request.getHearingId(), is(hearingId));
        assertThat(request.getProsecutionAuthorityId(), is(prosecutionAuthorityId));
        assertThat(request.getProsecutionAuthorityCode(), is("TFL"));
        assertThat(request.getFileName(), is("file.csv"));
        assertThat(request.getHearingVenue(), is(notNullValue()));
    }

    @Test
    public void jacksonDeserialization_nestedVerdictGraph_preservesVerdictCode() throws Exception {
        final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        final String json = "{" +
                "\"registerDate\":\"2026-04-13T09:00:00Z\"," +
                "\"hearingDate\":\"2026-04-13T09:00:00Z\"," +
                "\"hearingId\":\"" + randomUUID() + "\"," +
                "\"prosecutionAuthorityId\":\"" + randomUUID() + "\"," +
                "\"prosecutionAuthorityCode\":\"TFL\"," +
                "\"fileName\":\"file.csv\"," +
                "\"hearingVenue\":{\"courtHouse\":\"Crown Court\",\"courtSessions\":[{" +
                "\"courtRoom\":\"Room 1\",\"hearingStartTime\":\"09:00\"," +
                "\"defendants\":[{\"name\":\"John\",\"address1\":\"1 Street\"," +
                "\"prosecutionCasesOrApplications\":[{" +
                "\"caseOrApplicationReference\":\"URN-001\"," +
                "\"offences\":[{\"offenceCode\":\"AB001\",\"offenceTitle\":\"Theft\",\"orderIndex\":1," +
                "\"verdict\":{\"verdictCode\":\"G\",\"verdictDate\":\"2026-04-13\"}}]}]}]}]}}";

        final InformantRegisterDocumentRequest request = mapper.readValue(json, InformantRegisterDocumentRequest.class);

        assertThat(request.getHearingVenue(), is(notNullValue()));
        final String verdictCode = request.getHearingVenue()
                .getCourtSessions().get(0)
                .getDefendants().get(0)
                .getProsecutionCasesOrApplications().get(0)
                .getOffences().get(0)
                .getVerdict().getVerdictCode();
        assertThat(verdictCode, is("G"));
    }
}
