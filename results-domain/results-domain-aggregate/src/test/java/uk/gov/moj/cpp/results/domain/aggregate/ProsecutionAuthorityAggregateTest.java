package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import static uk.gov.justice.results.courts.NotifyInformantRegister.notifyInformantRegister;
import static uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterDocumentRequest.informantRegisterDocumentRequest;
import static uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterRecipient.informantRegisterRecipient;

import uk.gov.justice.results.courts.InformantRegisterNotificationIgnored;
import uk.gov.justice.results.courts.InformantRegisterNotifiedV2;
import uk.gov.justice.results.courts.NotifyInformantRegister;
import uk.gov.moj.cpp.results.domain.event.InformantRegisterGeneratedV2;
import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterDocumentRequest;
import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterRecipient;

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
    public void apply_informantRegisterGeneratedV2_shouldSetRecipientsFromLocalTypes() {
        final InformantRegisterRecipient recipient = informantRegisterRecipient()
                .withRecipientName("Jane")
                .withEmailAddress1("jane@hmcts.net")
                .withEmailTemplateName("template")
                .build();
        final InformantRegisterDocumentRequest documentRequest = informantRegisterDocumentRequest()
                .withRecipients(singletonList(recipient))
                .build();
        final InformantRegisterGeneratedV2 event = InformantRegisterGeneratedV2.informantRegisterGeneratedV2()
                .withInformantRegisterDocumentRequests(singletonList(documentRequest))
                .withSystemGenerated(false)
                .build();

        aggregate.apply(event);

        final NotifyInformantRegister notifyInformantRegister = notifyInformantRegister()
                .withProsecutionAuthorityId(randomUUID())
                .withTemplateId("template Id")
                .withFileId(randomUUID())
                .build();

        final List<Object> eventStream = aggregate.notifyProsecutingAuthority(notifyInformantRegister).collect(toList());
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(equalTo(InformantRegisterNotifiedV2.class)));
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
