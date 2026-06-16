package uk.gov.moj.cpp.results.domain.event;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterDocumentRequest;
import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterHearingVenue;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

public class InformantRegisterGeneratedV2Test {

    @Test
    public void name_constantEqualsEventName() {
        assertThat(InformantRegisterGeneratedV2.NAME, is("results.event.informant-register-generated-v2"));
    }

    @Test
    public void builder_setsDocumentRequestsList() {
        final InformantRegisterDocumentRequest request = InformantRegisterDocumentRequest.informantRegisterDocumentRequest()
                .withRegisterDate(ZonedDateTime.parse("2026-04-01T09:00:00Z"))
                .withHearingId(randomUUID())
                .withProsecutionAuthorityId(randomUUID())
                .withProsecutionAuthorityCode("TFL")
                .withHearingVenue(InformantRegisterHearingVenue.informantRegisterHearingVenue()
                        .withCourtHouse("Crown Court").build())
                .build();

        final InformantRegisterGeneratedV2 event = InformantRegisterGeneratedV2.informantRegisterGeneratedV2()
                .withInformantRegisterDocumentRequests(singletonList(request))
                .withSystemGenerated(true)
                .build();

        final List<InformantRegisterDocumentRequest> requests = event.getInformantRegisterDocumentRequests();
        assertThat(requests, hasSize(1));
        assertThat(event.getSystemGenerated(), is(true));
    }

    @Test
    public void builder_systemGeneratedOptional() {
        final InformantRegisterGeneratedV2 event = InformantRegisterGeneratedV2.informantRegisterGeneratedV2()
                .withInformantRegisterDocumentRequests(singletonList(
                        InformantRegisterDocumentRequest.informantRegisterDocumentRequest()
                                .withRegisterDate(ZonedDateTime.parse("2026-04-01T09:00:00Z"))
                                .withHearingId(randomUUID())
                                .withProsecutionAuthorityId(randomUUID())
                                .withProsecutionAuthorityCode("TFL")
                                .withHearingVenue(InformantRegisterHearingVenue.informantRegisterHearingVenue()
                                        .withCourtHouse("Crown Court").build())
                                .build()))
                .build();

        assertThat(event.getSystemGenerated(), is((Boolean) null));
    }
}
