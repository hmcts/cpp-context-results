package uk.gov.moj.cpp.results.domain.transformation.pilotdata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EventStreamTransformTest {

    public static Stream<Arguments> streamIdsAndActions() {
        return Stream.of(
                Arguments.of("41a15ddf-c913-45e4-86dc-40d3c659b36e", DEACTIVATE),
                Arguments.of("eb7231f3-9cb5-4baa-9e9c-5fad78ad3a92", DEACTIVATE),
                Arguments.of("36ac57aa-01e5-463f-ac6a-51276fcfa90b", DEACTIVATE),
                Arguments.of("be2e2c78-66e5-42c6-b641-1d8e0f13b48d", NO_ACTION),
                Arguments.of("1ec7adb4-cf0e-45c3-9654-d1ebbdfd43c4", DEACTIVATE),
                Arguments.of("39fe39ea-a823-41fd-bb0b-0c63d4069343", NO_ACTION)
        );
    }

    @ParameterizedTest
    @MethodSource("streamIdsAndActions")
    public void testActionFor(final String streamId, final Action expectedAction) {
        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class, RETURNS_DEEP_STUBS);
        when(jsonEnvelope.metadata().streamId()).thenReturn(Optional.of(UUID.fromString(streamId)));

        final Action action = new EventStreamTransform("test-data-1.csv", "test-data-2.csv").actionFor(jsonEnvelope);

        assertThat(action, is(expectedAction));
    }
}