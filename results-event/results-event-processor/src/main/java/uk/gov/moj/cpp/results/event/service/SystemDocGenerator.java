package uk.gov.moj.cpp.results.event.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

public class SystemDocGenerator {

    private static final String GENERATE_DOCUMENT_COMMAND = "systemdocgenerator.generate-document";

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;

    public void generateDocument(final DocumentGenerationRequest request, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder()
                .add("originatingSource", "nces")
                .add("templateIdentifier", request.getTemplateIdentifier().getValue())
                .add("conversionFormat", request.getConversionFormat().getValue())
                .add("sourceCorrelationId", request.getSourceCorrelationId())
                .add("payloadFileServiceId", request.getPayloadFileServiceId().toString())
                .build();

        sender.sendAsAdmin(Envelope.envelopeFrom(
                metadataFrom(envelope.metadata()).withName(GENERATE_DOCUMENT_COMMAND),
                payload
        ));
    }

}
