package uk.gov.moj.cpp.results.event.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import com.azure.identity.WorkloadIdentityCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.configuration.Value;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Publishes the thin informant-register distribution command to the dedicated Azure Service Bus
 * queue when a regular hearing is resulted. The message body is the closed contract owned by
 * service-cp-crime-informant-register (distribution-command.schema.json): seven fields,
 * additionalProperties false. requestId is minted deterministically from
 * hearingId|hearingDay|sharedTime so a republish of the same share carries the same id, while a
 * genuine re-share (new sharedTime) mints a new one. The broker messageId is
 * "RESULTS:{requestId}" for duplicate detection.
 *
 * <p>userId is the CPP user who shared the results, taken from the hearing-resulted envelope's
 * metadata and parsed by the caller, so a metadata value that is not a canonical uuid never reaches
 * the queue: the consumer's schema types the field as one, so publishing anything else would buy a
 * guaranteed dead-letter. It is carried so the consumer can attribute every downstream call it makes for this
 * message to that user through CJSCPPUID, which is what the function app does today: the envelope's
 * userId becomes the orchestration's cjscppuid and is threaded unchanged into the now-subscriptions
 * read and the add-informant-register POST. It is optional in the schema - support replay tooling
 * has no user to name - but this publisher only runs when the envelope carries one. It is
 * deliberately NOT part of the requestId recipe, so the same share republished by a different route
 * still dedupes.
 *
 * <p>Authentication is workload identity only: when {@code informantRegisterQueueNamespace} and
 * {@code informantRegisterQueueName} are configured the sender authenticates as the pod's managed
 * identity via DefaultAzureCredential, which needs an AzureServiceBusDataSender grant on the
 * namespace (declared in the AKS deploy config, ccm_workload_identities). With either value
 * unconfigured the publisher is inert, so environments without the grant are unaffected.
 */
public class InformantRegisterQueuePublisher implements InformantRegisterQueueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InformantRegisterQueuePublisher.class);

    private static final String SOURCE = "RESULTS";
    private static final String EVENT_TYPE_HEARING_RESULTED = "Hearing_Resulted";

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
                    .sender()
                    .queueName(informantRegisterQueueName)
                    .buildClient();
        }
    }

    @Override
    public boolean sendDistributionCommand(final String hearingId, final String hearingDay, final String sharedTime, final UUID userId) {
        if (senderClient == null) {
            return true;
        }
        try {
            final UUID requestId = UUID.nameUUIDFromBytes(
                    (hearingId + "|" + hearingDay + "|" + sharedTime).getBytes(UTF_8));

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
            message.setContentType("application/json");

            LOGGER.info("Publishing informant register distribution command for hearing {}, hearingDay {}, requestId {} to queue {}",
                    hearingId, hearingDay, requestId, informantRegisterQueueName);
            senderClient.sendMessage(message);
            return true;
        } catch (final Exception e) {
            LOGGER.error("Failed to publish informant register distribution command for hearing {}, hearingDay {}", hearingId, hearingDay, e);
            return false;
        }
    }
}
