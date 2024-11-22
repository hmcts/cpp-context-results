package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest.informantRegisterDocumentRequest;
import static uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterHearingVenue.informantRegisterHearingVenue;
import static uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterRecipient.informantRegisterRecipient;
import static uk.gov.justice.results.courts.NotifyInformantRegister.notifyInformantRegister;

import uk.gov.justice.core.courts.InformantRegisterRecorded;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterRecipient;
import uk.gov.justice.results.courts.InformantRegisterGenerated;
import uk.gov.justice.results.courts.InformantRegisterNotificationIgnored;
import uk.gov.justice.results.courts.InformantRegisterNotified;
import uk.gov.justice.results.courts.InformantRegisterNotifiedV2;
import uk.gov.justice.results.courts.NotifyInformantRegister;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionAuthorityAggregateTest {
    @InjectMocks
    private ProsecutionAuthorityAggregate aggregate;

    @BeforeEach
    public void setUp() {
        aggregate = new ProsecutionAuthorityAggregate();
    }

    @Test
    public void shouldReturnInformantRegisterNotified() {
        final UUID fileId = randomUUID();
        final InformantRegisterRecipient recipient = informantRegisterRecipient().withRecipientName("John").build();
        final NotifyInformantRegister notifyInformantRegister = notifyInformantRegister()
                .withProsecutionAuthorityId(randomUUID())
                .withTemplateId("template Id")
                .withFileId(fileId)
                .build();

        aggregate.setInformantRegisterRecipients(singletonList(recipient));
        final List<Object> eventStream = aggregate.notifyProsecutingAuthority(notifyInformantRegister).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(InformantRegisterNotifiedV2.class)));
    }

    @Test
    public void shouldReturnInformantRegisterIgnored() {
        final NotifyInformantRegister notifyInformantRegister = notifyInformantRegister()
                .withProsecutionAuthorityId(randomUUID())
                .withTemplateId("template Id")
                .withFileId(UUID.randomUUID())
                .build();
        final List<Object> eventStream = aggregate.notifyProsecutingAuthority(notifyInformantRegister).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(InformantRegisterNotificationIgnored.class)));
    }
}
