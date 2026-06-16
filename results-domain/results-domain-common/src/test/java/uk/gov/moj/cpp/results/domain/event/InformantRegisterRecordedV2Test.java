package uk.gov.moj.cpp.results.domain.event;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterDocumentRequest;
import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterHearingVenue;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class InformantRegisterRecordedV2Test {

    @Test
    public void name_constantEqualsEventName() {
        assertThat(InformantRegisterRecordedV2.NAME, is("results.event.informant-register-recorded-v2"));
    }

    @Test
    public void builder_setsProsecutionAuthorityId() {
        final UUID prosecutionAuthorityId = randomUUID();
        final InformantRegisterDocumentRequest request = InformantRegisterDocumentRequest.informantRegisterDocumentRequest()
                .withRegisterDate(ZonedDateTime.parse("2026-04-01T09:00:00Z"))
                .withHearingId(randomUUID())
                .withProsecutionAuthorityId(prosecutionAuthorityId)
                .withProsecutionAuthorityCode("TFL")
                .withHearingVenue(InformantRegisterHearingVenue.informantRegisterHearingVenue()
                        .withCourtHouse("Crown Court").build())
                .build();

        final InformantRegisterRecordedV2 event = InformantRegisterRecordedV2.informantRegisterRecordedV2()
                .withProsecutionAuthorityId(prosecutionAuthorityId)
                .withInformantRegister(request)
                .build();

        assertThat(event.getProsecutionAuthorityId(), is(prosecutionAuthorityId));
        assertThat(event.getInformantRegister(), is(notNullValue()));
    }
}
