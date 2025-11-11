package uk.gov.moj.cpp.results.command.handler;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

abstract class AbstractCommandHandler {

    protected final EventSource eventSource;
    protected final Enveloper enveloper;
    protected final AggregateService aggregateService;

    public AbstractCommandHandler(final EventSource eventSource,
                                  final Enveloper enveloper,
                                  final AggregateService aggregateService) {
        this.eventSource = eventSource;
        this.enveloper = enveloper;
        this.aggregateService = aggregateService;
    }

    protected <A extends Aggregate> A aggregate(final Class<A> clazz,
                                                final UUID streamId,
                                                final JsonEnvelope envelope,
                                                final Function<A, Stream<Object>> function) throws EventStreamException {

        final EventStream eventStream = eventSource.getStreamById(streamId);
        final A aggregate = aggregateService.get(eventStream, clazz);
        eventStream.append(function.apply(aggregate).map(enveloper.withMetadataFrom(envelope)));
        return aggregate;
    }
}