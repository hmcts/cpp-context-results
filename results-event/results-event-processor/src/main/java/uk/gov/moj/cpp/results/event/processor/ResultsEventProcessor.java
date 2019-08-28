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
public class ResultsEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsEventProcessor.class);

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;


    @Handles("public.hearing.resulted")
    public void hearingResulted(final JsonEnvelope envelope) {

        final JsonObject hearingResultPayload = envelope.payloadAsJsonObject();

        LOGGER.debug("public.hearing.resulted event received {}", hearingResultPayload);

        sender.sendAsAdmin(enveloper.withMetadataFrom(envelope, "results.command.add-hearing-result").apply(hearingResultPayload));
    }
}
