package uk.gov.moj.cpp.results.event.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import java.io.StringReader;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InformantRegisterQueuePublisherTest {

    private static final String HEARING_ID = "0aa5bf35-1e51-45e5-9e42-64bd57e15c11";
    private static final String HEARING_DAY = "2026-08-21";
    private static final String SHARED_TIME = "2026-08-21T16:05:00.000Z";

    @Mock
    private ServiceBusSenderClient senderClient;

    @Captor
    private ArgumentCaptor<ServiceBusMessage> messageCaptor;

    private InformantRegisterQueuePublisher publisher;

    @BeforeEach
    public void setup() {
        publisher = new InformantRegisterQueuePublisher();
        setField(publisher, "informantRegisterQueuePublishEnabled", "true");
        setField(publisher, "informantRegisterQueueName", "informantregister.requests");
        setField(publisher, "senderClient", senderClient);
    }

    @Test
    public void shouldNotSendWhenDisabled() {
        setField(publisher, "informantRegisterQueuePublishEnabled", "false");

        final boolean result = publisher.sendDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME);

        assertThat(result, is(true));
        verifyNoInteractions(senderClient);
    }

    @Test
    public void shouldSendDistributionCommandMatchingTheContract() {
        final boolean result = publisher.sendDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME);

        assertThat(result, is(true));
        verify(senderClient).sendMessage(messageCaptor.capture());

        final ServiceBusMessage message = messageCaptor.getValue();
        final UUID expectedRequestId = UUID.nameUUIDFromBytes(
                (HEARING_ID + "|" + HEARING_DAY + "|" + SHARED_TIME).getBytes(UTF_8));

        assertThat(message.getMessageId(), is("RESULTS:" + expectedRequestId));
        assertThat(message.getContentType(), is("application/json"));

        final JsonReader jsonReader = createReader(new StringReader(message.getBody().toString()));
        final JsonObject body = jsonReader.readObject();
        jsonReader.close();

        assertThat(body.keySet().size(), is(6));
        assertThat(body.getString("source"), is("RESULTS"));
        assertThat(body.getString("requestId"), is(expectedRequestId.toString()));
        assertThat(body.getString("hearingId"), is(HEARING_ID));
        assertThat(body.getString("hearingDay"), is(HEARING_DAY));
        assertThat(body.getString("sharedTime"), is(SHARED_TIME));
        assertThat(body.getString("eventType"), is("Hearing_Resulted"));
    }

    @Test
    public void shouldMintTheSameRequestIdForARepublishAndANewOneForAReshare() {
        publisher.sendDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME);
        publisher.sendDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME);
        publisher.sendDistributionCommand(HEARING_ID, HEARING_DAY, "2026-08-21T19:30:00.000Z");

        verify(senderClient, org.mockito.Mockito.times(3)).sendMessage(messageCaptor.capture());

        final String firstMessageId = messageCaptor.getAllValues().get(0).getMessageId();
        final String secondMessageId = messageCaptor.getAllValues().get(1).getMessageId();
        final String reshareMessageId = messageCaptor.getAllValues().get(2).getMessageId();

        assertThat(secondMessageId, is(firstMessageId));
        assertThat(reshareMessageId.equals(firstMessageId), is(false));
    }

    @Test
    public void shouldReturnFalseAndNotThrowWhenTheSendFails() {
        doThrow(new RuntimeException("broker unavailable")).when(senderClient).sendMessage(any(ServiceBusMessage.class));

        final boolean result = publisher.sendDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME);

        assertThat(result, is(false));
    }
}
