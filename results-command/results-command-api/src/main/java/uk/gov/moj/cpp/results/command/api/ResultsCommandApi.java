package uk.gov.moj.cpp.results.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class ResultsCommandApi {
    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;


    @Handles("results.update-nows-material-status")
    public void updateNowsStatus(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope, "results.handler.update-nows-material-status");
        sender.send(commandEnvelope);
    }

    private JsonEnvelope envelopeWithUpdatedActionName(final JsonEnvelope existingEnvelope, final String name) {
        return enveloper.withMetadataFrom(existingEnvelope, name).apply(existingEnvelope.payloadAsJsonObject());
    }
}
