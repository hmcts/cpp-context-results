package uk.gov.moj.cpp.results.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.event.service.InformantRegisterQueueService;

/**
 * Publishes the informant register distribution command for a resulted regular hearing, off the
 * durable publish-request event rather than inline on the hearing-resulted fan-out.
 *
 * <p>Nothing here is caught. That is the point of the class: an exception rolls back the delivery,
 * the framework redelivers, and on exhaustion the event parks on the Artemis DLQ, so an owed publish
 * is recoverable. The previous inline publish logged its failures and dropped them, which lost the
 * register for that share with no record that one was ever owed.
 *
 * <p>A redelivered event re-publishes the same message, which is harmless: the message id is derived
 * from the deterministic requestId so the broker dedupes it, and the consuming service's
 * (source, requestId) processed-log guards beyond that.
 *
 * <p>The payload is read as raw strings rather than converted to the event POJO because the
 * requestId minted downstream is a hash of hearingDay and sharedTime: they must reach the queue in
 * exactly the form the hearing-resulted event carried, and a date/time round-trip could reformat
 * them and mint a different id for the same share.
 */
@ServiceComponent(EVENT_PROCESSOR)
public class InformantRegisterPublishRequestedProcessor {

    private static final String HEARING_ID = "hearingId";
    private static final String HEARING_DAY = "hearingDay";
    private static final String SHARED_TIME = "sharedTime";
    private static final String USER_ID = "userId";

    @Inject
    private InformantRegisterQueueService informantRegisterQueueService;

    @Handles("results.events.informant-register-publish-requested")
    public void publishInformantRegisterRequest(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        informantRegisterQueueService.publishDistributionCommand(
                payload.getString(HEARING_ID),
                payload.getString(HEARING_DAY),
                payload.getString(SHARED_TIME),
                UUID.fromString(payload.getString(USER_ID)));
    }
}
