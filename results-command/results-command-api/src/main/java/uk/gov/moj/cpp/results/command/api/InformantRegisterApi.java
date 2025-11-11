package uk.gov.moj.cpp.results.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class InformantRegisterApi {
    @Inject
    private Sender sender;

    @Handles("results.add-informant-register")
    public void handleAddInformantRegister(final JsonEnvelope command) {
        this.sender.send(envelopeFrom(metadataFrom(command.metadata()).withName("results.command.add-informant-register").build(),
                command.payloadAsJsonObject()));
    }

    @Handles("results.generate-informant-register")
    public void handleGenerateInformantRegister(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonObject wrappedPayload = createObjectBuilder(payload).add("registerDate", LocalDate.now().toString()).build();
        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("results.command.generate-informant-register").build(),
                wrappedPayload));
    }

    @Handles("results.generate-informant-register-by-date")
    public void handleGenerateInformantRegisterByDate(final JsonEnvelope envelope) {
        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("results.command.generate-informant-register-by-date").build(),
                envelope.payloadAsJsonObject()));
    }
}
