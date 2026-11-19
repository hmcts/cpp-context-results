package uk.gov.moj.cpp.results.event.service;

import static java.lang.String.format;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.domains.DistributionRequestIdHelper.distributionRequestId;

import uk.gov.justice.services.common.configuration.Value;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.json.JsonObjectBuilder;

import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.identity.WorkloadIdentityCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes the thin probation results distribution command to the probation.requests Azure Service
 * Bus queue for one resulted regular hearing. A copy of {@code InformantRegisterQueuePublisher}
 * with the names changed; read that class for the full reasoning.
 *
 * <p>The message body is the closed contract owned by service-cp-crime-probation-results-distribution
 * (distribution-command.schema.json, additionalProperties false): source, requestId, hearingId,
 * hearingDay, sharedTime, eventType and, when known, userId. requestId is minted by the shared
 * {@link uk.gov.moj.cpp.domains.DistributionRequestIdHelper#distributionRequestId}, so a republish of
 * the same share carries the same id while a genuine re-share (new sharedTime) mints a new one. The
 * broker messageId is "RESULTS:{requestId}" and the correlationId is the hearingId.
 *
 * <p>Broker duplicate detection is off on probation.requests: every copy is delivered, and the
 * consuming service's (source, requestId) processed-log is the duplicate guard.
 *
 * <p>Failure is thrown, not swallowed. This publisher runs on an event processor handling
 * results.events.probation-distribution-publish-requested, so an exception rolls back the delivery
 * and the framework redelivers with back-off and then dead-letters the event.
 *
 * <p>The {@code ProbationResultsDistributionService} feature flag is evaluated upstream, on
 * HearingResultedEventProcessor, before the command that leads here is sent - not here. Once the
 * publish request is in the event store it is owed, and a flag flipped off mid-flight must not
 * strand it.
 *
 * <p>Authentication is workload identity only: when {@code probationDistributionQueueNamespace} and
 * {@code probationDistributionQueueName} are configured the sender authenticates as the pod's managed
 * identity, which needs a Service Bus Data Sender grant on the queue. With either value unconfigured
 * the publisher is inert; {@code ProbationDistributionQueueConfigurationHealthcheck} fails when the
 * flag is on and no queue is configured.
 */
public class ProbationDistributionQueuePublisher implements ProbationDistributionQueueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProbationDistributionQueuePublisher.class);

    private static final String SOURCE = "RESULTS";
    private static final String EVENT_TYPE_HEARING_RESULTED = "Hearing_Resulted";
    private static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * Deliberately a short envelope, for the same reasons as
     * {@code InformantRegisterQueuePublisher}: worst case one retry after 2s with a 10s try-timeout,
     * about 22 seconds, so it stays inside the delivery's JTA transaction and does not hold the
     * shared MDB sessions during a Service Bus outage.
     */
    private static final int MAX_RETRIES = 1;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    private static final Duration TRY_TIMEOUT = Duration.ofSeconds(10);

    @Inject
    @Value(key = "probationDistributionQueueNamespace", defaultValue = "")
    private String probationDistributionQueueNamespace;

    @Inject
    @Value(key = "probationDistributionQueueName", defaultValue = "")
    private String probationDistributionQueueName;

    @Inject
    @Value(key = "azure.local.mi.clientId", defaultValue = "")
    private String managedIdentityClientId;

    @Inject
    @Value(key = "azure.local.mi.tenantId", defaultValue = "")
    private String managedIdentityTenantId;

    private ServiceBusSenderClient senderClient;

    @PostConstruct
    public void setup() {
        if (!probationDistributionQueueNamespace.isBlank() && !probationDistributionQueueName.isBlank()) {
            final String tokenFile = System.getenv().getOrDefault(
                    "AZURE_FEDERATED_TOKEN_FILE", "/var/run/secrets/azure/tokens/azure-identity-token");
            LOGGER.info("Probation distribution publisher connecting to {} queue {} as client {} (tenant {}, token file {} exists {})",
                    probationDistributionQueueNamespace, probationDistributionQueueName, managedIdentityClientId, managedIdentityTenantId,
                    tokenFile, Files.exists(Paths.get(tokenFile)));
            senderClient = new ServiceBusClientBuilder()
                    .fullyQualifiedNamespace(probationDistributionQueueNamespace)
                    .credential(new WorkloadIdentityCredentialBuilder()
                            .clientId(managedIdentityClientId)
                            .tenantId(managedIdentityTenantId)
                            .tokenFilePath(tokenFile)
                            .build())
                    .retryOptions(retryOptions())
                    .sender()
                    .queueName(probationDistributionQueueName)
                    .buildClient();
        }
    }

    @PreDestroy
    public void teardown() {
        if (senderClient != null) {
            LOGGER.info("Closing probation distribution queue sender for queue {}", probationDistributionQueueName);
            senderClient.close();
        }
    }

    static AmqpRetryOptions retryOptions() {
        return new AmqpRetryOptions()
                .setMaxRetries(MAX_RETRIES)
                .setDelay(RETRY_DELAY)
                .setTryTimeout(TRY_TIMEOUT);
    }

    @Override
    public void publishDistributionCommand(final String hearingId, final String hearingDay, final String sharedTime, final UUID userId) {
        if (senderClient == null) {
            // Returning rather than throwing: an environment with no Service Bus grant would
            // otherwise dead-letter every resulted hearing. ProbationDistributionQueueConfigurationHealthcheck
            // fails on the same condition, so the misconfiguration surfaces there too.
            LOGGER.warn("Probation distribution queue is not configured - publish skipped for hearing {}, hearingDay {}. "
                            + "The ProbationResultsDistributionService feature is enabled for an environment with no queue configured.",
                    hearingId, hearingDay);
            return;
        }

        final UUID requestId = distributionRequestId(hearingId, hearingDay, sharedTime);

        final JsonObjectBuilder bodyBuilder = createObjectBuilder()
                .add("source", SOURCE)
                .add("requestId", requestId.toString())
                .add("hearingId", hearingId)
                .add("hearingDay", hearingDay)
                .add("sharedTime", sharedTime)
                .add("eventType", EVENT_TYPE_HEARING_RESULTED);
        // The contract allows userId to be absent but refuses an explicit null.
        if (userId != null) {
            bodyBuilder.add("userId", userId.toString());
        }

        final ServiceBusMessage message = new ServiceBusMessage(bodyBuilder.build().toString());
        message.setMessageId(SOURCE + ":" + requestId);
        message.setContentType(CONTENT_TYPE_JSON);
        message.setCorrelationId(hearingId);

        try {
            LOGGER.info("Publishing probation distribution command for hearing {}, hearingDay {}, requestId {} to queue {}",
                    hearingId, hearingDay, requestId, probationDistributionQueueName);
            senderClient.sendMessage(message);
        } catch (final Exception e) {
            LOGGER.error("Failed to publish probation distribution command for hearing {}, hearingDay {}, requestId {}",
                    hearingId, hearingDay, requestId, e);
            throw new ProbationDistributionPublishException(
                    format("Failed to publish probation distribution command for hearing %s, hearingDay %s, requestId %s",
                            hearingId, hearingDay, requestId), e);
        }
    }
}
