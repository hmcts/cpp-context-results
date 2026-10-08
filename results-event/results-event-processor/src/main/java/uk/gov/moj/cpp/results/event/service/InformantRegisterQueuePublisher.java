package uk.gov.moj.cpp.results.event.service;

import static java.lang.String.format;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.domains.InformantRegisterHelper.informantRegisterRequestId;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.identity.WorkloadIdentityCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.configuration.Value;

/**
 * Publishes the thin informant-register distribution command to the dedicated Azure Service Bus
 * queue for one resulted regular hearing. The message body is the closed contract owned by
 * service-cp-crime-informant-register (distribution-command.schema.json): seven fields,
 * additionalProperties false. requestId is minted deterministically from
 * hearingId|hearingDay|sharedTime so a republish of the same share carries the same id, while a
 * genuine re-share (new sharedTime) mints a new one. The broker messageId is
 * "RESULTS:{requestId}" and the correlationId is the hearingId.
 *
 * <p>The authoritative duplicate guard is the consuming service's (source, requestId) processed-log,
 * NOT the broker. Service Bus duplicate detection only applies if requiresDuplicateDetection is
 * enabled on the queue and only inside its window (7 days maximum), so it cannot cover a processor
 * CATCHUP or stream replay: a replay re-dispatches every publish-requested event ever recorded, and
 * each one mints a publish. The deterministic requestId is what makes that survivable, because every
 * replayed publish of a given share carries the id the consumer has already recorded.
 *
 * <p>userId is the CPP user who shared the results, taken from the hearing-resulted envelope's
 * metadata and parsed by the caller, so a metadata value that is not a canonical uuid never reaches
 * the queue: the consumer's schema types the field as one, so publishing anything else would buy a
 * guaranteed dead-letter. It is carried so the consumer can attribute every downstream call it makes
 * for this message to that user through CJSCPPUID, which is what the function app does today: the
 * envelope's userId becomes the orchestration's cjscppuid and is threaded unchanged into the
 * now-subscriptions read and the add-informant-register POST. It is optional in the schema - support
 * replay tooling has no user to name - but this publisher only runs when the envelope carried one.
 * It is deliberately NOT part of the requestId recipe, so the same share republished by a different
 * route still dedupes.
 *
 * <p>Failure is thrown, not swallowed. This publisher runs on an event processor handling
 * results.events.informant-register-publish-requested, so an exception rolls back the delivery and
 * the framework redelivers and then dead-letters the event: the owed publish is recoverable rather
 * than lost. That is the whole reason the publish moved off the inline hearing-resulted path.
 *
 * <p>Two layers of retry sit under that, deliberately. The Service Bus client retries in-process
 * with exponential back-off, because JMS redelivery back-off is broker configuration this service
 * does not own and may be zero - without an in-process delay a transient broker blip would burn
 * every redelivery attempt in milliseconds and dead-letter a message that would have gone through a
 * second later. Framework redelivery then covers everything longer-lived than that.
 *
 * <p>The {@code InformantRegisterService} feature flag is evaluated upstream, on
 * HearingResultedEventProcessor, before the command that leads here is sent - not here. One
 * decision point, taken before anything is committed: once the publish request is in the event
 * store it is owed, and a flag flipped off mid-flight must not strand it.
 *
 * <p>Authentication is workload identity only: when {@code informantRegisterQueueNamespace} and
 * {@code informantRegisterQueueName} are configured the sender authenticates as the pod's managed
 * identity, which needs an AzureServiceBusDataSender grant on the namespace (declared in the AKS
 * deploy config, ccm_workload_identities). With either value unconfigured the publisher is inert, so
 * environments without the grant are unaffected - they must also leave the feature flag off, or
 * every resulted hearing would record a publish that silently never happens.
 * {@code InformantRegisterQueueConfigurationHealthcheck} exists to make that pairing visible rather
 * than trusting it: it fails when the flag is on and no queue is configured.
 */
public class InformantRegisterQueuePublisher implements InformantRegisterQueueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InformantRegisterQueuePublisher.class);

    private static final String SOURCE = "RESULTS";
    private static final String EVENT_TYPE_HEARING_RESULTED = "Hearing_Resulted";
    private static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * Deliberately a short envelope: worst case one retry after 2s with a 10s try-timeout, so about
     * 22 seconds. This runs inside the event delivery's JTA transaction and occupies one of the 15
     * MDB sessions shared by every results event processor, so a long envelope would be doubly
     * harmful - it could outlive the transaction timeout (turning the clean
     * redeliver-then-dead-letter into an XA rollback race), and during a Service Bus outage it would
     * hold sessions that police results, NCES notifications and DCS publishing also need. The SDK
     * default (3 retries at a 60s try-timeout, ~4 minutes) is far too long for both reasons.
     *
     * <p>This layer only absorbs a blip. Anything longer-lived is the framework's job: the delivery
     * rolls back, Artemis redelivers, and the event dead-letters on exhaustion.
     *
     * <p>No maxDelay is set: with a single retry the back-off never compounds, so a cap would be an
     * unreachable knob inviting someone to read a ceiling into a schedule that has none.
     */
    private static final int MAX_RETRIES = 1;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    private static final Duration TRY_TIMEOUT = Duration.ofSeconds(10);

    @Inject
    @Value(key = "informantRegisterQueueNamespace", defaultValue = "")
    private String informantRegisterQueueNamespace;

    @Inject
    @Value(key = "informantRegisterQueueName", defaultValue = "")
    private String informantRegisterQueueName;

    @Inject
    @Value(key = "azure.local.mi.clientId", defaultValue = "")
    private String managedIdentityClientId;

    @Inject
    @Value(key = "azure.local.mi.tenantId", defaultValue = "")
    private String managedIdentityTenantId;

    private ServiceBusSenderClient senderClient;

    @PostConstruct
    public void setup() {
        if (!informantRegisterQueueNamespace.isBlank() && !informantRegisterQueueName.isBlank()) {
            final String tokenFile = System.getenv().getOrDefault(
                    "AZURE_FEDERATED_TOKEN_FILE", "/var/run/secrets/azure/tokens/azure-identity-token");
            LOGGER.info("Informant register publisher connecting to {} queue {} as client {} (tenant {}, token file {} exists {})",
                    informantRegisterQueueNamespace, informantRegisterQueueName, managedIdentityClientId, managedIdentityTenantId,
                    tokenFile, Files.exists(Paths.get(tokenFile)));
            senderClient = new ServiceBusClientBuilder()
                    .fullyQualifiedNamespace(informantRegisterQueueNamespace)
                    .credential(new WorkloadIdentityCredentialBuilder()
                            .clientId(managedIdentityClientId)
                            .tenantId(managedIdentityTenantId)
                            .tokenFilePath(tokenFile)
                            .build())
                    .retryOptions(retryOptions())
                    .sender()
                    .queueName(informantRegisterQueueName)
                    .buildClient();
        }
    }

    @PreDestroy
    public void teardown() {
        if (senderClient != null) {
            // The sender owns an AMQP connection and a workload-identity token-refresh timer. Without
            // this, a hot redeploy of the processor leaks both, and a half-dead sender makes every
            // subsequent publish throw and dead-letter rather than fail visibly at startup.
            LOGGER.info("Closing informant register queue sender for queue {}", informantRegisterQueueName);
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
            // otherwise dead-letter every resulted hearing. Such an environment must leave the
            // InformantRegisterService flag off, which stops the publish request being recorded at
            // all - this branch is the backstop for a misconfiguration, and says so loudly.
            // InformantRegisterQueueConfigurationHealthcheck fails on the same condition, so the
            // misconfiguration surfaces at the healthcheck rather than only in this per-hearing WARN.
            LOGGER.warn("Informant register queue is not configured - publish skipped for hearing {}, hearingDay {}. "
                            + "The InformantRegisterService feature is enabled for an environment with no queue configured.",
                    hearingId, hearingDay);
            return;
        }

        final UUID requestId = informantRegisterRequestId(hearingId, hearingDay, sharedTime);

        final String body = createObjectBuilder()
                .add("source", SOURCE)
                .add("requestId", requestId.toString())
                .add("hearingId", hearingId)
                .add("hearingDay", hearingDay)
                .add("sharedTime", sharedTime)
                .add("eventType", EVENT_TYPE_HEARING_RESULTED)
                .add("userId", userId.toString())
                .build()
                .toString();

        final ServiceBusMessage message = new ServiceBusMessage(body);
        message.setMessageId(SOURCE + ":" + requestId);
        message.setContentType(CONTENT_TYPE_JSON);
        message.setCorrelationId(hearingId);

        try {
            LOGGER.info("Publishing informant register distribution command for hearing {}, hearingDay {}, requestId {} to queue {}",
                    hearingId, hearingDay, requestId, informantRegisterQueueName);
            senderClient.sendMessage(message);
        } catch (final Exception e) {
            LOGGER.error("Failed to publish informant register distribution command for hearing {}, hearingDay {}, requestId {}",
                    hearingId, hearingDay, requestId, e);
            throw new InformantRegisterPublishException(
                    format("Failed to publish informant register distribution command for hearing %s, hearingDay %s, requestId %s",
                            hearingId, hearingDay, requestId), e);
        }
    }
}
