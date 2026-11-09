package uk.gov.moj.cpp.results.command.handler;

import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.aggregate.DocumentDeliveryAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.DocumentDeliveryRequest;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records a change to the delivery tracking of a document results stores in its blob container;
 * the aggregate decides what the change amounts to and the view is updated from its event.
 */
@ServiceComponent(Component.COMMAND_HANDLER)
public class DocumentDeliveryCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDeliveryCommandHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("results.command.record-document-delivery")
    public void recordDocumentDelivery(final Envelope<DocumentDeliveryRequest> envelope) throws EventStreamException {
        final DocumentDeliveryRequest request = envelope.payload();
        LOGGER.info("Recording document delivery of material {}", request.materialId());

        final EventStream eventStream = eventSource.getStreamById(request.materialId());
        final DocumentDeliveryAggregate aggregate = aggregateService.get(eventStream, DocumentDeliveryAggregate.class);
        final Stream<Object> events = aggregate.recordDocumentDelivery(request);

        appendEventsToStream(envelope, eventStream, events);
    }

    private static void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
