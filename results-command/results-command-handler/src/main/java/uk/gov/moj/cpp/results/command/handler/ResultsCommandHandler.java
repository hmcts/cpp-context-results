package uk.gov.moj.cpp.results.command.handler;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.aggregate.ResultsAggregate;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.UUID;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

@ServiceComponent(COMMAND_HANDLER)
public class ResultsCommandHandler extends AbstractCommandHandler {

    final JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    public ResultsCommandHandler(final EventSource eventSource, final Enveloper enveloper,
                                 final AggregateService aggregateService, final JsonObjectToObjectConverter jsonObjectToObjectConverter) {
        super(eventSource, enveloper, aggregateService);
        this.jsonObjectToObjectConverter=jsonObjectToObjectConverter;
    }

    @Handles("results.add-hearing-result")
    public void addHearingResult(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final PublicHearingResulted publicHearingResulted = jsonObjectToObjectConverter.convert(payload, PublicHearingResulted.class);

        aggregate(ResultsAggregate.class, UUID.fromString(payload.getJsonObject("hearing").getString("id")),
                envelope, a -> a.saveHearingResults(publicHearingResulted));
    }
}
