package uk.gov.moj.cpp.results.command.handler;

import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.command.RequestYotResultsPublish;
import uk.gov.moj.cpp.results.domain.event.YotResultsPublishRequested;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class YotResultsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(YotResultsHandler.class);

    @Inject
    private EventSource eventSource;

    /**
     * Records that a resulted regular hearing owes a YOT results publish, so the publish survives a
     * Service Bus failure. Same shape and reasoning as
     * {@code InformantRegisterHandler#handleRequestInformantRegisterPublish}.
     *
     * <p>The stream id is the hearingId, so every share and re-share of one hearing lands on one
     * stream and the framework dispatches them in order: a re-share cannot overtake the share it
     * supersedes on the way to the queue.
     *
     * <p>The stream is an audit log, not an idempotency key: a redelivered command appends a second
     * event, and the framework then publishes a second, byte-identical Service Bus message. That is
     * survivable because the requestId minted downstream is deterministic, so the consuming service
     * can dedupe on it.
     *
     * <p>No aggregate is loaded: nothing about this event mutates case or financial state, and no
     * aggregate reads it back.
     */
    @Handles("results.command.request-yot-results-publish")
    public void handleRequestYotResultsPublish(final Envelope<RequestYotResultsPublish> envelope)
            throws EventStreamException {

        final RequestYotResultsPublish command = envelope.payload();

        // hearingDay and sharedTime are copied through as the raw strings from the hearing-resulted
        // event and are not re-rendered here: the requestId the publisher mints is a hash of them,
        // so reformatting either would mint a different id for the same share and defeat both dedupes.
        LOGGER.info("results.command.request-yot-results-publish for hearing {}, hearingDay {}, sharedTime {}",
                command.getHearingId(), command.getHearingDay(), command.getSharedTime());

        final EventStream eventStream = eventSource.getStreamById(command.getHearingId());
        final Stream<Object> events = Stream.of(YotResultsPublishRequested.yotResultsPublishRequested()
                .withHearingId(command.getHearingId())
                .withHearingDay(command.getHearingDay())
                .withSharedTime(command.getSharedTime())
                .withUserId(command.getUserId())
                .build());

        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
