package uk.gov.moj.cpp.results.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.event.service.YotResultsQueueService;

/**
 * Publishes the YOT results distribution command for a resulted regular hearing, off the durable
 * publish-request event rather than inline on the hearing-resulted fan-out. Same shape and reasoning
 * as {@code InformantRegisterPublishRequestedProcessor}.
 *
 * <p>Nothing here is caught: an exception rolls back the delivery, the framework redelivers, and on
 * exhaustion the event parks on the Artemis DLQ, so an owed publish is recoverable.
 *
 * <p>The payload is read as raw strings rather than converted to the event POJO because the
 * requestId minted downstream is a hash of hearingDay and sharedTime: they must reach the queue in
 * exactly the form the hearing-resulted event carried.
 */
@ServiceComponent(EVENT_PROCESSOR)
public class YotResultsPublishRequestedProcessor {

    private static final String HEARING_ID = "hearingId";
    private static final String HEARING_DAY = "hearingDay";
    private static final String SHARED_TIME = "sharedTime";
    private static final String USER_ID = "userId";

    @Inject
    private YotResultsQueueService yotResultsQueueService;

    @Handles("results.events.yot-results-publish-requested")
    public void publishYotResultsRequest(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        yotResultsQueueService.publishDistributionCommand(
                payload.getString(HEARING_ID),
                payload.getString(HEARING_DAY),
                payload.getString(SHARED_TIME),
                UUID.fromString(payload.getString(USER_ID)));
    }
}
