package uk.gov.moj.cpp.results.command.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.aggregate.HearingResultAggregate;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.UUID;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

@ServiceComponent(COMMAND_HANDLER)
public class UpdateNowsMaterialStatusHandler extends AbstractCommandHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UpdateNowsMaterialStatusHandler.class.getName());

    @Inject
    public UpdateNowsMaterialStatusHandler(final EventSource eventSource,
                                           final Enveloper enveloper,
                                           final AggregateService aggregateService) {
        super(eventSource, enveloper, aggregateService);
    }

    @Handles("results.handler.update-nows-material-status")
    public void updateNowsMaterialStatus(final JsonEnvelope envelope) throws EventStreamException {
        LOGGER.debug("results.handler.update-nows-material-status event received {}", envelope.payloadAsJsonObject());
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID hearingId = fromString(payload.getString("hearingId"));
        final UUID materialId = fromString(payload.getString("materialId"));
        final String status = payload.getString("status");

        aggregate(HearingResultAggregate.class, hearingId, envelope,
                (aggregate) -> aggregate.updateNowsMaterialStatus(hearingId, materialId, status));
    }

}