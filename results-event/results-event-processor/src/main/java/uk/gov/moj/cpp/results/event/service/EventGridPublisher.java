package uk.gov.moj.cpp.results.event.service;


import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.configuration.Value;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.UUID;

public class EventGridPublisher implements EventGridService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventGridPublisher.class);

    @Inject
    @Value(key = "eventGridTopicEndpoint", defaultValue = "http://localhost:8080")
    private String eventgridTopicEndpoint;

    @Inject
    @Value(key = "eventGridTopicKey", defaultValue = "test_key")
    private String eventgridTopicKey;

    private EventGridPublisherClient<EventGridEvent> eventGridPublisherClient;

    @PostConstruct
    public void setup() {
        final AzureKeyCredential azureKeyCredential = new AzureKeyCredential(eventgridTopicKey);
        eventGridPublisherClient = new EventGridPublisherClientBuilder()
                .endpoint(eventgridTopicEndpoint)
                .credential(azureKeyCredential)
                .buildEventGridEventPublisherClient();
    }

    @SuppressWarnings({"squid:ClassVariableVisibilityCheck"})
    public static class HearingResultedEventData {
        public String hearingId;
        public UUID userId;

        public HearingResultedEventData(final UUID userId, final String hearingId) {
            this.userId = userId;
            this.hearingId = hearingId;
        }
    }

    @SuppressWarnings({"squid:ClassVariableVisibilityCheck"})
    public static class HearingResultedForDayEventData {
        public String hearingId;
        public UUID userId;
        public String hearingDay;

        public HearingResultedForDayEventData(final UUID userId, final String hearingId, final String hearingDay) {
            this.userId = userId;
            this.hearingId = hearingId;
            this.hearingDay = hearingDay;
        }
    }

    public boolean sendHearingResultedEvent(final UUID userId, final String hearingId, final String eventType) {
        return publishResults(hearingId, new EventGridPublisher.HearingResultedEventData(userId, hearingId), eventType);
    }

    public boolean sendHearingResultedForDayEvent(final UUID userId, final String hearingId, final String hearingDay, final String eventType) {
        return publishResults(hearingId, new EventGridPublisher.HearingResultedForDayEventData(userId, hearingId, hearingDay), eventType);
    }

    private boolean publishResults(final String hearingId, final Object object, final String eventType) {
        if ("localhost".equals(eventgridTopicEndpoint)) {
            return true;
        }
        try {
            LOGGER.info("Publishing events to {} via the Event Grid Publisher", eventgridTopicEndpoint);
            final BinaryData binaryData = BinaryData.fromObject(object);
            final EventGridEvent event = new EventGridEvent(String.format("HearingResulted%s", hearingId),
                    eventType,
                    binaryData,
                    "0.1");
            eventGridPublisherClient.sendEvent(event);
            LOGGER.debug("Published events to the event grid successfully via the Event Grid Publisher");
        } catch (final Exception e) {
            LOGGER.error("Failed to publish events to the event grid via the Event Grid Publisher", e);
            return false;
        }
        return true;

    }
}
