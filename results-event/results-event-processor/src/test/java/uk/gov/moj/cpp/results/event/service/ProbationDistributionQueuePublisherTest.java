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
import static uk.gov.moj.cpp.domains.DistributionRequestIdHelper.distributionRequestId;

import java.io.InputStream;
import java.io.StringReader;
import java.time.Duration;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProbationDistributionQueuePublisherTest {

    /**
     * A vendored copy of the consumer's closed inbound contract,
     * service-cp-crime-probation-results-distribution
     * src/main/resources/contracts/distribution-command.schema.json. The consumer owns it and its
     * vendored file is the authority; this copy exists so a drift on this side fails here rather
     * than as a dead-letter in another team's queue. Re-take it if the consumer changes it.
     */
    private static final String CONSUMER_CONTRACT =
            "contracts/probation-results-distribution/distribution-command.schema.json";

    private static final String HEARING_ID = "0aa5bf35-1e51-45e5-9e42-64bd57e15c11";
    private static final String HEARING_DAY = "2026-08-19";
    private static final String SHARED_TIME = "2026-08-19T16:42:07.512Z";
    private static final String USER_ID = "1a3f7c68-4c4b-4a1f-93cd-6e2ac2c4a1d0";
    private static final UUID USER = UUID.fromString(USER_ID);

    @Mock
    private ServiceBusSenderClient senderClient;

    @Captor
    private ArgumentCaptor<ServiceBusMessage> messageCaptor;

    private ProbationDistributionQueuePublisher publisher;

    @BeforeEach
    public void setup() {
        publisher = new ProbationDistributionQueuePublisher();
        setField(publisher, "senderClient", senderClient);
    }

    /**
     * AC6: an environment with no queue configured must not dead-letter every resulted hearing, so
     * an unconfigured publisher logs a WARN and returns. The healthcheck fails on the same condition.
     */
    @Test
    public void publishDistributionCommand_withNoNamespaceConfigured_should_stayInert() {
        final ProbationDistributionQueuePublisher unconfigured = new ProbationDistributionQueuePublisher();
        setField(unconfigured, "probationDistributionQueueNamespace", "");
        setField(unconfigured, "probationDistributionQueueName", "steccm42.probation.requests");

        unconfigured.setup();
        unconfigured.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER);

        verifyNoInteractions(senderClient);
    }

    @Test
    public void publishDistributionCommand_withNoQueueNameConfigured_should_stayInert() {
        final ProbationDistributionQueuePublisher unconfigured = new ProbationDistributionQueuePublisher();
        setField(unconfigured, "probationDistributionQueueNamespace", "sbsteccm01.servicebus.windows.net");
        setField(unconfigured, "probationDistributionQueueName", "");

        unconfigured.setup();
        unconfigured.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER);

        verifyNoInteractions(senderClient);
    }

    /**
     * AC1: the message validates against the consumer's closed schema and carries
     * messageId RESULTS:{requestId} and correlationId hearingId.
     */
    @Test
    public void publishDistributionCommand_should_sendAMessageMatchingTheContract() {
        publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER);

        verify(senderClient).sendMessage(messageCaptor.capture());

        final ServiceBusMessage message = messageCaptor.getValue();
        final UUID expectedRequestId = distributionRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME);

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

        consumerContract().validate(new JSONObject(message.getBody().toString()));
    }

    /**
     * The contract makes userId optional but refuses an explicit null, so a publish with no user
     * must leave the field out entirely. Today's caller only sends when the share named a user; this
     * keeps the publisher safe for support tooling that has none.
     */
    @Test
    public void publishDistributionCommand_withNoUserId_should_omitTheFieldRatherThanSendNull() {
        publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, null);

        verify(senderClient).sendMessage(messageCaptor.capture());

        final ServiceBusMessage message = messageCaptor.getValue();
        final JsonObject body = readBody(message);

        assertThat(body.keySet().size(), is(6));
        assertThat(body.containsKey("userId"), is(false));

        consumerContract().validate(new JSONObject(message.getBody().toString()));
    }

    /**
     * Guards the guard: the vendored contract really is closed and really refuses a null userId,
     * so the two contract assertions above are not passing against a schema that accepts anything.
     */
    @Test
    public void consumerContract_should_refuseANullUserIdAndAnUnknownField() {
        final Schema contract = consumerContract();
        final UUID requestId = distributionRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME);

        final JSONObject withNullUserId = validBody(requestId).put("userId", JSONObject.NULL);
        final JSONObject withUnknownField = validBody(requestId).put("prosecutionAuthorityId", "x");

        assertThrows(ValidationException.class, () -> contract.validate(withNullUserId));
        assertThrows(ValidationException.class, () -> contract.validate(withUnknownField));
    }

    /**
     * AC4: the user who shared the results is carried for attribution only, and is NOT part of the
     * requestId recipe - the same share republished by a different route must still dedupe.
     */
    @Test
    public void publishDistributionCommand_should_notDeriveTheRequestIdFromTheUserId() {
        final String otherUserId = "9d2b1e04-5f77-4c8a-8b31-0f5c7d6e2a94";

        publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER);
        publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, UUID.fromString(otherUserId));

        verify(senderClient, times(2)).sendMessage(messageCaptor.capture());

        final ServiceBusMessage first = messageCaptor.getAllValues().get(0);
        final ServiceBusMessage second = messageCaptor.getAllValues().get(1);

        assertThat(second.getMessageId(), is(first.getMessageId()));
        assertThat(readBody(second).getString("requestId"), is(readBody(first).getString("requestId")));
        assertThat(readBody(second).getString("userId"), is(otherUserId));
    }

    /**
     * AC4: the same share published twice carries the same requestId and messageId; a re-share
     * with a new sharedTime gets a new one.
     */
    @Test
    public void publishDistributionCommand_forARepublishAndAReshare_should_mintTheSameThenANewRequestId() {
        publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER);
        publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER);
        publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, "2026-08-19T19:30:00.000Z", USER);

        verify(senderClient, times(3)).sendMessage(messageCaptor.capture());

        final ServiceBusMessage first = messageCaptor.getAllValues().get(0);
        final ServiceBusMessage republish = messageCaptor.getAllValues().get(1);
        final ServiceBusMessage reshare = messageCaptor.getAllValues().get(2);

        assertThat(republish.getMessageId(), is(first.getMessageId()));
        assertThat(readBody(republish).getString("requestId"), is(readBody(first).getString("requestId")));
        assertThat(reshare.getMessageId(), is(not(first.getMessageId())));
        assertThat(readBody(reshare).getString("requestId"), is(not(readBody(first).getString("requestId"))));
    }

    /**
     * AC5: a failed publish must never be silently lost. Throwing rolls back the event delivery, so
     * the framework redelivers and ultimately parks the event on the DLQ. The cause is kept so the
     * DLQ entry can be diagnosed.
     */
    @Test
    public void publishDistributionCommand_whenTheSendFails_should_throw() {
        final RuntimeException brokerFailure = new RuntimeException("broker unavailable");
        doThrow(brokerFailure).when(senderClient).sendMessage(any(ServiceBusMessage.class));

        final ProbationDistributionPublishException thrown = assertThrows(ProbationDistributionPublishException.class,
                () -> publisher.publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER));

        assertThat(thrown.getCause(), is(brokerFailure));
        assertThat(thrown.getMessage().contains(HEARING_ID), is(true));
        assertThat(thrown.getMessage().contains(
                distributionRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME).toString()), is(true));
    }

    /**
     * Same envelope as the informant register publisher, for the same reasons: it runs inside the
     * event delivery's JTA transaction on the shared MDB session pool. Pinned because the unit tests
     * inject the sender directly and never execute setup().
     */
    @Test
    public void retryOptions_should_keepTheWorstCaseShortEnoughForTheTransactionAndThePool() {
        final AmqpRetryOptions retryOptions = ProbationDistributionQueuePublisher.retryOptions();

        assertThat(retryOptions.getMaxRetries(), is(1));
        assertThat(retryOptions.getDelay(), is(Duration.ofSeconds(2)));
        assertThat(retryOptions.getTryTimeout(), is(Duration.ofSeconds(10)));
    }

    private JSONObject validBody(final UUID requestId) {
        return new JSONObject()
                .put("source", "RESULTS")
                .put("requestId", requestId.toString())
                .put("hearingId", HEARING_ID)
                .put("hearingDay", HEARING_DAY)
                .put("sharedTime", SHARED_TIME)
                .put("eventType", "Hearing_Resulted");
    }

    private Schema consumerContract() {
        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONSUMER_CONTRACT);
        assertThat("contract " + CONSUMER_CONTRACT + " is on the classpath", inputStream != null, is(true));

        // The consumer declares draft-07, which the everit version on this classpath (1.6.0) does not
        // know. Every keyword the contract uses - type, required, enum, pattern,
        // additionalProperties - means the same in draft-04, so it is validated as draft-04; the
        // uuid and date patterns still enforce those formats.
        final JSONObject contract = new JSONObject(new JSONTokener(inputStream));
        contract.remove("$schema");
        return SchemaLoader.load(contract);
    }

    private JsonObject readBody(final ServiceBusMessage message) {
        try (JsonReader jsonReader = createReader(new StringReader(message.getBody().toString()))) {
            return jsonReader.readObject();
        }
    }
}
