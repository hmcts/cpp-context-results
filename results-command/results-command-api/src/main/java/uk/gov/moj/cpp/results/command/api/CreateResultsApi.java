package uk.gov.moj.cpp.results.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class CreateResultsApi {

    @Inject
    private Sender sender;

    @Handles("results.api.create-results")
    public void createResults(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("results.create-results"),
                envelope.payloadAsJsonObject()));
    }

}
