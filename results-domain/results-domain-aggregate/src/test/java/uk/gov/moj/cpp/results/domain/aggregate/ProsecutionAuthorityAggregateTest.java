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
import uk.gov.justice.results.courts.NotifyInformantRegister;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProsecutionAuthorityAggregateTest {
    @InjectMocks
    private ProsecutionAuthorityAggregate aggregate;

    @Before
    public void setUp() {
        aggregate = new ProsecutionAuthorityAggregate();
    }

    @Test
    public void shouldReturnInformantRegisterAdded() {
        final UUID prosecutionAuthId = randomUUID();

        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = informantRegisterDocumentRequest()
                .withProsecutionAuthorityId(prosecutionAuthId)
                .withHearingVenue(informantRegisterHearingVenue().build())
                .build();

        final List<Object> eventStream = aggregate.createInformantRegister(prosecutionAuthId, informantRegisterDocumentRequest).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(InformantRegisterRecorded.class)));
    }

    @Test
    public void shouldReturnInformantRegisterGenerated() {
        final UUID prosecutionAuthId = randomUUID();

        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = informantRegisterDocumentRequest()
                .withProsecutionAuthorityId(prosecutionAuthId)
                .withHearingVenue(informantRegisterHearingVenue().build())
                .build();

        final List<Object> eventStream = aggregate.generateInformantRegister(singletonList(informantRegisterDocumentRequest), false).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(InformantRegisterGenerated.class)));
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
        assertThat(object.getClass(), is(equalTo(InformantRegisterNotified.class)));
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
