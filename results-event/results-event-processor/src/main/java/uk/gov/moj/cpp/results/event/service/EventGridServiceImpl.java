package uk.gov.moj.cpp.results.event.service;

import static java.util.UUID.randomUUID;

import uk.gov.justice.services.common.configuration.Value;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.microsoft.azure.eventgrid.EventGridClient;
import com.microsoft.azure.eventgrid.TopicCredentials;
import com.microsoft.azure.eventgrid.implementation.EventGridClientImpl;
import com.microsoft.azure.eventgrid.models.EventGridEvent;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventGridServiceImpl implements EventGridService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventGridServiceImpl.class);

    @Inject
    @Value(key = "eventGridTopicHost", defaultValue = "localhost")
    private String eventgridTopicHost;

    @Inject
    @Value(key = "eventGridTopicKey", defaultValue = "test_key")
    private String eventgridTopicKey;

    @Inject
    @Value(key = "eventGridTopicProtocol", defaultValue = "http")
    private String eventgridTopicProtocol;

    @Inject
    @Value(key = "eventGridTopicPort", defaultValue = "8080")
    private String eventgridTopicPort;

    private EventGridClient eventGridClient;

    @PostConstruct
    public void setup() {
        LOGGER.info("LAA Event Grid Topic Host is {}", eventgridTopicHost);
        final TopicCredentials topicCredentials = new TopicCredentials(eventgridTopicKey);
        LOGGER.debug("Creating an Azure EventGrid Client");
        this.eventGridClient = new EventGridClientImpl(topicCredentials);
        LOGGER.debug("Done creating an Azure EventGrid Client...");
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
        return sendEventToEventGrid(hearingId, new HearingResultedEventData(userId, hearingId), eventType);
    }

    public boolean sendHearingResultedForDayEvent(final UUID userId, final String hearingId, final String hearingDay, final String eventType) {
        return sendEventToEventGrid(hearingId, new HearingResultedForDayEventData(userId, hearingId, hearingDay), eventType);
    }

    private boolean sendEventToEventGrid(final String hearingId, final Object payload, final String eventType) {
        if ("localhost".equals(eventgridTopicHost)) {
            return true;
        }

        final UUID uuid = randomUUID();

        final List<EventGridEvent> eventsList = new ArrayList<>();
        eventsList.add(new EventGridEvent(
                uuid.toString(),
                String.format("HearingResulted%s", hearingId),
                payload,
                eventType,
                DateTime.now(),
                "2.0"
        ));

        try {
            final String eventGridEndpoint = String.format(
                    "%s://%s:%s/",
                    eventgridTopicProtocol,
                    new URI(eventgridTopicHost),
                    eventgridTopicPort
            );
            eventGridClient.publishEvents(eventGridEndpoint, eventsList);
            LOGGER.debug("Done publishing hearing resulted event to the EventGrid");
        } catch (URISyntaxException e) {
            LOGGER.error("Exception occurred while sending hearing resulted event: {} ", e);
            return false;
        }

        return true;
    }

}
