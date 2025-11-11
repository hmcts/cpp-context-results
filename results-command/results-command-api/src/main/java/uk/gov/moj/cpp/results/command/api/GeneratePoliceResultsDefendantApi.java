package uk.gov.moj.cpp.results.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class GeneratePoliceResultsDefendantApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("results.command.generate-police-results-for-a-defendant")
    public void generatePoliceResultsForDefendant(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "results.command.generate-police-results-for-a-defendant").apply(envelope.payloadAsJsonObject()));
    }
}




