package uk.gov.moj.cpp.results.event.processor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@ServiceComponent(EVENT_PROCESSOR)
public class NowsMaterialStatusUpdateProcessor {

    static final String PUBLIC_RESULTS_EVENT_NOWS_MATERIAL_STATUS_UPDATED = "public.results.event.nows-material-status-updated";

    private static final Logger LOGGER = LoggerFactory.getLogger(NowsMaterialStatusUpdateProcessor.class);

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("results.event.nows-material-status-updated")
    public void handleNowsMaterialStatusUpdated(final JsonEnvelope jsonEnvelope) {
        JsonObject privateEventPayload = jsonEnvelope.payloadAsJsonObject();
        final String materialId = privateEventPayload.getString("materialId");
        LOGGER.debug("Now material status updated '{}' ", materialId);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_RESULTS_EVENT_NOWS_MATERIAL_STATUS_UPDATED).apply(privateEventPayload));
    }


}
