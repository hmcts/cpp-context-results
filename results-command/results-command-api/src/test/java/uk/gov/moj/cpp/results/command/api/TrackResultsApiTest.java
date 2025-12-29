package uk.gov.moj.cpp.results.command.api;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TrackResultsApiTest {

    private static final String COMMAND_NAME = "results.api.track-results";
    private static final String DUMMY_FIELD = "dummyField";
    private static final String DUMMY_FIELD_VALUE = "dummyFieldValue";

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> senderArgumentCaptor;

    @InjectMocks
    private TrackResultsApi target;

    @Test
    public void shouldHandleCommand() {
        assertThat(TrackResultsApi.class, isHandlerClass(COMMAND_API)
                .with(method("trackResults").thatHandles(COMMAND_NAME)));
    }

    @Test
    public void shouldPassThroughTrackResultsRequestToCommandHandler() {
        final JsonEnvelope envelope = buildDummyJsonRequestEnvelopeWithName("results.api.track-results");

        target.trackResults(envelope);

        assertEnvelopeIsPassedThroughWithName(envelope.payloadAsJsonObject(), "results.command.track-results");
    }

    private JsonEnvelope buildDummyJsonRequestEnvelopeWithName(final String name) {
        return envelopeFrom(metadataWithRandomUUID(name).withCausation(randomUUID())
                        .build(),
                createObjectBuilder()
                        .add(DUMMY_FIELD, DUMMY_FIELD_VALUE)
                        .build());
    }

    private void assertEnvelopeIsPassedThroughWithName(final JsonObject originalPayload, final String expectedName) {
        verify(sender).send(senderArgumentCaptor.capture());

        final Envelope<JsonObject> actualSentEnvelope = senderArgumentCaptor.getValue();
        assertThat(actualSentEnvelope.metadata().name(), is(expectedName));
        assertThat(actualSentEnvelope.payload(), is(originalPayload));
    }

}