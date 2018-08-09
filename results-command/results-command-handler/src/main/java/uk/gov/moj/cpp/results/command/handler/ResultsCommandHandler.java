package uk.gov.moj.cpp.results.command.handler;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.aggregate.ResultsAggregate;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.UUID;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

@ServiceComponent(COMMAND_HANDLER)
public class ResultsCommandHandler extends AbstractCommandHandler {

    @Inject
    public ResultsCommandHandler(final EventSource eventSource, final Enveloper enveloper,
                                 final AggregateService aggregateService) {
        super(eventSource, enveloper, aggregateService);
    }

    @Handles("results.add-hearing-result")
    public void addHearingResult(final JsonEnvelope envelope) throws EventStreamException {

        final JsonObject payload = envelope.payloadAsJsonObject();

        aggregate(ResultsAggregate.class, UUID.fromString(payload.getJsonObject("hearing").getString("id")),
                envelope, a -> a.saveHearingResults(payload));
    }
}
