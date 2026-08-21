package uk.gov.moj.cpp.results.event.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.configuration.Value;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.UUID;

/**
 * Publishes the thin informant-register distribution command to the dedicated Azure Service Bus
 * queue when a regular hearing is resulted. The message body is the closed contract owned by
 * service-cp-crime-informant-register (distribution-command.schema.json): exactly six fields,
 * additionalProperties false. requestId is minted deterministically from
 * hearingId|hearingDay|sharedTime so a republish of the same share carries the same id, while a
 * genuine re-share (new sharedTime) mints a new one. The broker messageId is
 * "RESULTS:{requestId}" for duplicate detection.
 */
public class InformantRegisterQueuePublisher implements InformantRegisterQueueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InformantRegisterQueuePublisher.class);

    private static final String SOURCE = "RESULTS";
    private static final String EVENT_TYPE_HEARING_RESULTED = "Hearing_Resulted";

    // POC: fixed to the STE-42 queue. Per-environment configuration arrives with the production
    // command/event chain.
    private static final String QUEUE_NAME = "steccm42.informantregister.requests";

    @Inject
    @Value(key = "informantRegisterQueueConnectionString", defaultValue = "")
    private String informantRegisterQueueConnectionString;

    private ServiceBusSenderClient senderClient;

    @PostConstruct
    public void setup() {
        if (!informantRegisterQueueConnectionString.isBlank()) {
            senderClient = new ServiceBusClientBuilder()
                    .connectionString(informantRegisterQueueConnectionString)
                    .sender()
                    .queueName(QUEUE_NAME)
                    .buildClient();
        }
    }

    @Override
    public boolean sendDistributionCommand(final String hearingId, final String hearingDay, final String sharedTime) {
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
                    .build()
                    .toString();

            final ServiceBusMessage message = new ServiceBusMessage(body);
            message.setMessageId(SOURCE + ":" + requestId);
            message.setContentType("application/json");

            LOGGER.info("Publishing informant register distribution command for hearing {}, hearingDay {}, requestId {} to queue {}",
                    hearingId, hearingDay, requestId, QUEUE_NAME);
            senderClient.sendMessage(message);
            return true;
        } catch (final Exception e) {
            LOGGER.error("Failed to publish informant register distribution command for hearing {}, hearingDay {}", hearingId, hearingDay, e);
            return false;
        }
    }
}
