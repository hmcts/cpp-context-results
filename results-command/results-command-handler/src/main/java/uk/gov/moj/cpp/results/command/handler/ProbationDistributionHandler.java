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
import uk.gov.moj.cpp.results.command.RequestProbationDistributionPublish;
import uk.gov.moj.cpp.results.domain.event.ProbationDistributionPublishRequested;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class ProbationDistributionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProbationDistributionHandler.class);

    @Inject
    private EventSource eventSource;

    /**
     * Records that a resulted regular hearing owes a probation results distribution publish, so the
     * publish survives a Service Bus failure. Same shape and reasoning as
     * {@code InformantRegisterHandler#handleRequestInformantRegisterPublish}.
     *
     * <p>The stream id is the hearingId, so every share and re-share of one hearing lands on one
     * stream and the framework dispatches them in order: a re-share cannot overtake the share it
     * supersedes on the way to the queue.
     *
     * <p>The stream is an audit log, not an idempotency key: a redelivered command appends a second
     * event, and the framework then publishes a second, byte-identical Service Bus message. That is
     * survivable because the requestId minted downstream is deterministic and the consuming service
     * keeps a (source, requestId) processed-log - the broker's duplicate detection is off on
     * probation.requests, so that log is the only duplicate guard.
     *
     * <p>No aggregate is loaded: nothing about this event mutates case or financial state. Note that
     * the hearingId stream is also the ResultsAggregate stream, so the aggregate replays this event
     * and ignores it - do not add an apply case for it there, and do not move the stream id without
     * accounting for that.
     */
    @Handles("results.command.request-probation-distribution-publish")
    public void handleRequestProbationDistributionPublish(final Envelope<RequestProbationDistributionPublish> envelope)
            throws EventStreamException {

        final RequestProbationDistributionPublish command = envelope.payload();

        // hearingDay and sharedTime are copied through as the raw strings from the hearing-resulted
        // event and are not re-rendered here: the requestId the publisher mints is a hash of them,
        // so reformatting either would mint a different id for the same share and defeat the dedupe.
        LOGGER.info("results.command.request-probation-distribution-publish for hearing {}, hearingDay {}, sharedTime {}",
                command.getHearingId(), command.getHearingDay(), command.getSharedTime());

        final EventStream eventStream = eventSource.getStreamById(command.getHearingId());
        final Stream<Object> events = Stream.of(ProbationDistributionPublishRequested.probationDistributionPublishRequested()
                .withHearingId(command.getHearingId())
                .withHearingDay(command.getHearingDay())
                .withSharedTime(command.getSharedTime())
                .withUserId(command.getUserId())
                .build());

        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
