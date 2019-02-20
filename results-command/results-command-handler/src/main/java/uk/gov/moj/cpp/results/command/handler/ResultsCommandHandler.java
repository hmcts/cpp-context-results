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

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class ResultsCommandHandler extends AbstractCommandHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ResultsCommandHandler.class);

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

    @Handles("results.handler.update-nows-material-status")
    public void updateNowsMaterialStatus(final JsonEnvelope envelope) throws EventStreamException {
        LOGGER.debug("results.handler.update-nows-material-status event received {}", envelope.payloadAsJsonObject());
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID hearingId = fromString(payload.getString("hearingId"));
        final UUID materialId = fromString(payload.getString("materialId"));
        final String status = payload.getString("status");

        aggregate(ResultsAggregate.class, hearingId, envelope,
                aggregate -> aggregate.updateNowsMaterialStatus(hearingId, materialId, status));
    }

}
