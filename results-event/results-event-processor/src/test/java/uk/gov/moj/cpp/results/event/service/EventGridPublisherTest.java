package uk.gov.moj.cpp.results.event.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import java.util.UUID;

import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventGridPublisherTest {

    @Mock
    private EventGridPublisherClient<EventGridEvent> client;

    private EventGridPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new EventGridPublisher();
        setField(publisher, "eventGridPublisherClient", client);
    }

    @Test
    void disabledPublishingDoesNotRequireAnEndpointOrKeyAndSkipsBothEventTypes() {
        setField(publisher, "eventGridPublishingEnabled", false);

        publisher.setup();

        assertTrue(publisher.sendHearingResultedEvent(UUID.randomUUID(), "hearing", "resulted"));
        assertTrue(publisher.sendHearingResultedForDayEvent(UUID.randomUUID(), "hearing", "2026-11-04", "resulted-for-day"));
        verifyNoInteractions(client);
    }

    @Test
    void legacyLocalhostSettingSkipsSdkInitializationAsWellAsPublishing() {
        setField(publisher, "eventgridTopicEndpoint", "localhost");

        publisher.setup();

        assertTrue(publisher.sendHearingResultedEvent(UUID.randomUUID(), "hearing", "resulted"));
        assertTrue(publisher.sendHearingResultedForDayEvent(UUID.randomUUID(), "hearing", "2026-11-04", "resulted-for-day"));
        verifyNoInteractions(client);
    }

    @Test
    void enabledPublishingStillRejectsAnInvalidEndpoint() {
        setField(publisher, "eventgridTopicEndpoint", "not-a-valid-url");
        setField(publisher, "eventgridTopicKey", "test-key");

        assertThrows(IllegalArgumentException.class, publisher::setup);
    }

    @Test
    void publishingRemainsEnabledByDefaultForBothEventTypes() {
        setField(publisher, "eventgridTopicEndpoint", "https://example.test/api/events");

        assertTrue(publisher.sendHearingResultedEvent(UUID.randomUUID(), "hearing", "resulted"));
        assertTrue(publisher.sendHearingResultedForDayEvent(UUID.randomUUID(), "hearing", "2026-11-04", "resulted-for-day"));
        verify(client, times(2)).sendEvent(any(EventGridEvent.class));
    }

    @Test
    void enabledPublishingStillReportsDeliveryFailure() {
        setField(publisher, "eventgridTopicEndpoint", "https://example.test/api/events");
        doThrow(new IllegalStateException("send failed")).when(client).sendEvent(any(EventGridEvent.class));

        assertFalse(publisher.sendHearingResultedEvent(UUID.randomUUID(), "hearing", "resulted"));
    }
}
