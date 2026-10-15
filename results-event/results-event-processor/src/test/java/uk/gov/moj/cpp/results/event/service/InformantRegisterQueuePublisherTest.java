package uk.gov.moj.cpp.results.event.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.domains.InformantRegisterHelper.informantRegisterRequestId;

import java.io.StringReader;
import java.time.Duration;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.azure.core.amqp.AmqpRetryOptions;
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
    private static final UUID USER = UUID.fromString(USER_ID);

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

    /**
     * An environment with no Service Bus grant must not dead-letter every resulted hearing, so an
     * unconfigured publisher is inert rather than throwing. Such an environment is expected to leave
     * the InformantRegisterService feature off, which stops the publish request being recorded at
     * all; this branch only backstops a misconfiguration.
     */
    @Test
    public void publishDistributionCommand_withNoNamespaceConfigured_should_stayInert() {
        final InformantRegisterQueuePublisher unconfigured = new InformantRegisterQueuePublisher();
        setField(unconfigured, "informantRegisterQueueNamespace", "");
        setField(unconfigured, "informantRegisterQueueName", "steccm42.informantregister.requests");

        unconfigured.setup();
        unconfigured.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER);

        verifyNoInteractions(senderClient);
    }

    @Test
    public void publishDistributionCommand_withNoQueueNameConfigured_should_stayInert() {
        final InformantRegisterQueuePublisher unconfigured = new InformantRegisterQueuePublisher();
        setField(unconfigured, "informantRegisterQueueNamespace", "sbsteccm01.servicebus.windows.net");
        setField(unconfigured, "informantRegisterQueueName", "");

        unconfigured.setup();
        unconfigured.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER);

        verifyNoInteractions(senderClient);
    }

    /**
     * The consuming service owns this contract (distribution-command.schema.json) and closes it with
     * additionalProperties false, so the field count is asserted as well as the fields: an extra
     * field would be rejected on arrival, and this repo holds no copy of that schema to catch it.
     */
    @Test
    public void publishDistributionCommand_should_sendAMessageMatchingTheContract() {
        publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER);

        verify(senderClient).sendMessage(messageCaptor.capture());

        final ServiceBusMessage message = messageCaptor.getValue();
        final UUID expectedRequestId = informantRegisterRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME);

        assertThat(message.getMessageId(), is("RESULTS:" + expectedRequestId));
        assertThat(message.getContentType(), is("application/json"));
        assertThat(message.getCorrelationId(), is(HEARING_ID));

        final JsonObject body = readBody(message);

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
    public void publishDistributionCommand_should_notDeriveTheRequestIdFromTheUserId() {
        final String otherUserId = "9d2b1e04-5f77-4c8a-8b31-0f5c7d6e2a94";

        publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER);
        publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, UUID.fromString(otherUserId));

        verify(senderClient, times(2)).sendMessage(messageCaptor.capture());

        final ServiceBusMessage first = messageCaptor.getAllValues().get(0);
        final ServiceBusMessage second = messageCaptor.getAllValues().get(1);

        final UUID expectedRequestId = informantRegisterRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME);

        assertThat(second.getMessageId(), is(first.getMessageId()));
        assertThat(second.getMessageId(), is("RESULTS:" + expectedRequestId));
        assertThat(readBody(first).getString("requestId"), is(expectedRequestId.toString()));
        assertThat(readBody(second).getString("requestId"), is(expectedRequestId.toString()));

        assertThat(readBody(first).getString("userId"), is(USER_ID));
        assertThat(readBody(second).getString("userId"), is(otherUserId));
    }

    @Test
    public void publishDistributionCommand_forARepublishAndAReshare_should_mintTheSameThenANewRequestId() {
        publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER);
        publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER);
        publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, "2026-08-21T19:30:00.000Z", USER);

        verify(senderClient, times(3)).sendMessage(messageCaptor.capture());

        final String firstMessageId = messageCaptor.getAllValues().get(0).getMessageId();
        final String secondMessageId = messageCaptor.getAllValues().get(1).getMessageId();
        final String reshareMessageId = messageCaptor.getAllValues().get(2).getMessageId();

        assertThat(secondMessageId, is(firstMessageId));
        assertThat(reshareMessageId, is(not(firstMessageId)));
    }

    /**
     * A failed publish must never be silently lost. Throwing rolls back the event delivery, so the
     * framework redelivers and ultimately parks the event on the DLQ where the owed publish is
     * recoverable. The cause is kept so the DLQ entry can be diagnosed.
     */
    @Test
    public void publishDistributionCommand_whenTheSendFails_should_throw() {
        final RuntimeException brokerFailure = new RuntimeException("broker unavailable");
        doThrow(brokerFailure).when(senderClient).sendMessage(any(ServiceBusMessage.class));

        final InformantRegisterPublishException thrown = assertThrows(InformantRegisterPublishException.class,
                () -> publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER));

        assertThat(thrown.getCause(), is(brokerFailure));
        assertThat(thrown.getMessage().contains(HEARING_ID), is(true));
        assertThat(thrown.getMessage().contains(
                informantRegisterRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME).toString()), is(true));
    }

    /**
     * The retry envelope runs inside the event delivery's JTA transaction and occupies one of the 15
     * MDB sessions shared by every results event processor, so its worst case has to stay well under
     * the transaction timeout and short enough not to starve the pool during a Service Bus outage.
     * The values are pinned because the unit tests inject the sender directly and never execute
     * setup(), so nothing else would notice them drifting back towards the SDK default of ~4 minutes.
     */
    @Test
    public void retryOptions_should_keepTheWorstCaseShortEnoughForTheTransactionAndThePool() {
        final AmqpRetryOptions retryOptions = InformantRegisterQueuePublisher.retryOptions();

        assertThat(retryOptions.getMaxRetries(), is(1));
        assertThat(retryOptions.getDelay(), is(Duration.ofSeconds(2)));
        assertThat(retryOptions.getTryTimeout(), is(Duration.ofSeconds(10)));

        // getDelay, not getMaxDelay: with a single retry the back-off never compounds, so the delay
        // between the two attempts is the whole schedule and no cap is set or reachable.
        final Duration worstCase = retryOptions.getTryTimeout()
                .multipliedBy(retryOptions.getMaxRetries() + 1L)
                .plus(retryOptions.getDelay().multipliedBy(retryOptions.getMaxRetries()));
        assertThat(worstCase, is(Duration.ofSeconds(22)));
        assertThat(worstCase.getSeconds() < 30, is(true));
    }

    private JsonObject readBody(final ServiceBusMessage message) {
        try (JsonReader jsonReader = createReader(new StringReader(message.getBody().toString()))) {
            return jsonReader.readObject();
        }
    }
}
