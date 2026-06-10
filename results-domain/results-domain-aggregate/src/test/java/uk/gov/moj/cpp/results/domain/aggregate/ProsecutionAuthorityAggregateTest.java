package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequestV2.informantRegisterDocumentRequestV2;
import static uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterRecipient.informantRegisterRecipient;
import static uk.gov.justice.results.courts.InformantRegisterGenerated.informantRegisterGenerated;
import static uk.gov.justice.results.courts.NotifyInformantRegister.notifyInformantRegister;

import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequestV2;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterRecipient;
import uk.gov.justice.results.courts.InformantRegisterNotificationIgnored;
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

    @Test
    public void shouldPopulateRecipientsFromInformantRegisterGeneratedEvent() {
        final InformantRegisterRecipient recipient = informantRegisterRecipient().withRecipientName("John").build();
        final InformantRegisterDocumentRequestV2 requestWithRecipients = informantRegisterDocumentRequestV2()
                .withRecipients(singletonList(recipient))
                .build();

        aggregate.apply(informantRegisterGenerated()
                .withInformantRegisterDocumentRequests(singletonList(requestWithRecipients))
                .build());

        final List<Object> events = aggregate.notifyProsecutingAuthority(notifyInformantRegister()
                .withProsecutionAuthorityId(randomUUID())
                .withTemplateId("templateId")
                .withFileId(randomUUID())
                .build()).collect(toList());

        assertThat(events.size(), is(1));
        assertThat(events.get(0).getClass(), is(equalTo(InformantRegisterNotifiedV2.class)));
    }

    @Test
    public void shouldNotPopulateRecipientsWhenNoRequestHasRecipients() {
        final InformantRegisterDocumentRequestV2 requestWithoutRecipients = informantRegisterDocumentRequestV2()
                .withRecipients(emptyList())
                .build();

        aggregate.apply(informantRegisterGenerated()
                .withInformantRegisterDocumentRequests(singletonList(requestWithoutRecipients))
                .build());

        final List<Object> events = aggregate.notifyProsecutingAuthority(notifyInformantRegister()
                .withProsecutionAuthorityId(randomUUID())
                .withTemplateId("templateId")
                .withFileId(randomUUID())
                .build()).collect(toList());

        assertThat(events.size(), is(1));
        assertThat(events.get(0).getClass(), is(equalTo(InformantRegisterNotificationIgnored.class)));
    }
}
