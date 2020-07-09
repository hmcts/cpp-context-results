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
        public HearingResultedEventData(UUID userId, String hearingId) {
            this.hearingId = hearingId;
            this.userId = userId;
        }
    }

    public boolean sendHearingResultedEvent(UUID userId, String hearingId) {

        if ("localhost".equals(eventgridTopicHost)) {
            return true;
        }

        final UUID uuid = randomUUID();

        final List<EventGridEvent> eventsList = new ArrayList<>();
        eventsList.add(new EventGridEvent(
                uuid.toString(),
                String.format("HearingResulted%s", hearingId),
                new HearingResultedEventData(userId, hearingId),
                "Hearing_Resulted",
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
        } catch(URISyntaxException e) {
            LOGGER.error("Exception occured while sending hearing resulted event: {} ", e);
            return false;
        }

        return true;
    }

}
