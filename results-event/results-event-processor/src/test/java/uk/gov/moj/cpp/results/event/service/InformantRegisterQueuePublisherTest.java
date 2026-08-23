package uk.gov.moj.cpp.results.event.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
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
    private static final String USER_ID = "1a3f7c68-4c4b-4a1f-93cd-6e2ac2c4a1d0";

    @Mock
    private ServiceBusSenderClient senderClient;

    @Captor
    private ArgumentCaptor<ServiceBusMessage> messageCaptor;

    private InformantRegisterQueuePublisher publisher;

    @BeforeEach
    public void setup() {
        publisher = new InformantRegisterQueuePublisher();
        setField(publisher, "senderClient", senderClient);
    }

    @Test
    public void shouldStayInertWhenNoNamespaceIsConfigured() {
        final InformantRegisterQueuePublisher unconfigured = new InformantRegisterQueuePublisher();
        setField(unconfigured, "informantRegisterQueueNamespace", "");
        setField(unconfigured, "informantRegisterQueueName", "steccm42.informantregister.requests");

        unconfigured.setup();
        final boolean result = unconfigured.sendDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER_ID);

        assertThat(result, is(true));
        verifyNoInteractions(senderClient);
    }

    @Test
    public void shouldStayInertWhenNoQueueNameIsConfigured() {
        final InformantRegisterQueuePublisher unconfigured = new InformantRegisterQueuePublisher();
        setField(unconfigured, "informantRegisterQueueNamespace", "sbsteccm01.servicebus.windows.net");
        setField(unconfigured, "informantRegisterQueueName", "");

        unconfigured.setup();
        final boolean result = unconfigured.sendDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER_ID);

        assertThat(result, is(true));
        verifyNoInteractions(senderClient);
    }

    @Test
    public void shouldSendDistributionCommandMatchingTheContract() {
        final boolean result = publisher.sendDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER_ID);

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

        assertThat(body.keySet().size(), is(7));
        assertThat(body.getString("source"), is("RESULTS"));
        assertThat(body.getString("requestId"), is(expectedRequestId.toString()));
        assertThat(body.getString("hearingId"), is(HEARING_ID));
        assertThat(body.getString("hearingDay"), is(HEARING_DAY));
        assertThat(body.getString("sharedTime"), is(SHARED_TIME));
        assertThat(body.getString("eventType"), is("Hearing_Resulted"));
        assertThat(body.getString("userId"), is(USER_ID));
    }

    /**
     * The user who shared the results is carried for attribution only. It is deliberately NOT part
     * of the requestId recipe: the same share republished by a different route - a different user,
     * or support replay tooling - must still dedupe against the first publication.
     */
    @Test
    public void shouldNotDeriveTheRequestIdFromTheUserId() {
        final String otherUserId = "9d2b1e04-5f77-4c8a-8b31-0f5c7d6e2a94";

        publisher.sendDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER_ID);
        publisher.sendDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, otherUserId);

        verify(senderClient, times(2)).sendMessage(messageCaptor.capture());

        final ServiceBusMessage first = messageCaptor.getAllValues().get(0);
        final ServiceBusMessage second = messageCaptor.getAllValues().get(1);

        final UUID expectedRequestId = UUID.nameUUIDFromBytes(
                (HEARING_ID + "|" + HEARING_DAY + "|" + SHARED_TIME).getBytes(UTF_8));

        assertThat(second.getMessageId(), is(first.getMessageId()));
        assertThat(second.getMessageId(), is("RESULTS:" + expectedRequestId));
        assertThat(readBody(first).getString("requestId"), is(expectedRequestId.toString()));
        assertThat(readBody(second).getString("requestId"), is(expectedRequestId.toString()));

        assertThat(readBody(first).getString("userId"), is(USER_ID));
        assertThat(readBody(second).getString("userId"), is(otherUserId));
    }

    @Test
    public void shouldMintTheSameRequestIdForARepublishAndANewOneForAReshare() {
        publisher.sendDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER_ID);
        publisher.sendDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER_ID);
        publisher.sendDistributionCommand(HEARING_ID, HEARING_DAY, "2026-08-21T19:30:00.000Z", USER_ID);

        verify(senderClient, times(3)).sendMessage(messageCaptor.capture());

        final String firstMessageId = messageCaptor.getAllValues().get(0).getMessageId();
        final String secondMessageId = messageCaptor.getAllValues().get(1).getMessageId();
        final String reshareMessageId = messageCaptor.getAllValues().get(2).getMessageId();

        assertThat(secondMessageId, is(firstMessageId));
        assertThat(reshareMessageId.equals(firstMessageId), is(false));
    }

    @Test
    public void shouldReturnFalseAndNotThrowWhenTheSendFails() {
        doThrow(new RuntimeException("broker unavailable")).when(senderClient).sendMessage(any(ServiceBusMessage.class));

        final boolean result = publisher.sendDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER_ID);

        assertThat(result, is(false));
    }

    private JsonObject readBody(final ServiceBusMessage message) {
        try (JsonReader jsonReader = createReader(new StringReader(message.getBody().toString()))) {
            return jsonReader.readObject();
        }
    }
}
