package uk.gov.moj.cpp.results.event.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.results.event.helper.Originator.assembleEnvelopeWithPayloadAndMetaDetails;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.event.service.UploadMaterialContextBuilder;
import uk.gov.moj.cpp.results.event.service.UploadMaterialService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class SystemDocGeneratorEventProcessor {

    private static final String PUBLIC_DOCUMENT_AVAILABLE_EVENT_NAME = "public.systemdocgenerator.events.document-available";
    private static final String PUBLIC_DOCUMENT_FAILED_EVENT_NAME = "public.systemdocgenerator.events.generation-failed";
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemDocGeneratorEventProcessor.class);
    private static final String DOCUMENT_FILE_SERVICE_ID = "documentFileServiceId";
    private static final String SOURCE_CORRELATION_ID = "sourceCorrelationId";
    private static final String PAYLOAD_FILE_SERVICE_ID = "payloadFileServiceId";
    private static final String NCES_EMAIL_NOTIFICATION_REQUEST = "NCES_EMAIL_NOTIFICATION_REQUEST";

    @Inject
    private UploadMaterialService uploadMaterialService;

    @Inject
    private Sender sender;

    @Handles(PUBLIC_DOCUMENT_AVAILABLE_EVENT_NAME)
    public void handleDocumentAvailable(final JsonEnvelope documentAvailableEvent) {
        LOGGER.info(PUBLIC_DOCUMENT_AVAILABLE_EVENT_NAME + " {}", documentAvailableEvent.payload());

        final JsonObject documentAvailablePayload = documentAvailableEvent.payloadAsJsonObject();

        final String originatingSource = documentAvailablePayload.getString("originatingSource", "");

        if (NCES_EMAIL_NOTIFICATION_REQUEST.equalsIgnoreCase(originatingSource)) {
            processNcesNotificationDocumentAvailable(documentAvailableEvent);
        }
    }

    @Handles(PUBLIC_DOCUMENT_FAILED_EVENT_NAME)
    public void handleDocumentGenerationFailedEvent(final JsonEnvelope envelope) {
        LOGGER.info(PUBLIC_DOCUMENT_AVAILABLE_EVENT_NAME + " {}", envelope.payload());

        final JsonObject documentAvailablePayload = envelope.payloadAsJsonObject();

        final String originatingSource = documentAvailablePayload.getString("originatingSource", "");

        final String materialId = documentAvailablePayload.getString(SOURCE_CORRELATION_ID);

        final UUID payloadFileId = fromString(documentAvailablePayload.getString(PAYLOAD_FILE_SERVICE_ID));

        final String reason = documentAvailablePayload.getString("reason");

        if (NCES_EMAIL_NOTIFICATION_REQUEST.equalsIgnoreCase(originatingSource)) {
            LOGGER.error("Asynchronous document generation failed. Failed to generate nces notification for material id: '{}', payloadFileId: '{}', reason: '{}'",
                    materialId, payloadFileId, reason);
        }
    }

    private void processNcesNotificationDocumentAvailable(final JsonEnvelope envelope) {

        final JsonObject documentAvailablePayload = envelope.payloadAsJsonObject();

        final UUID materialId = UUID.fromString(documentAvailablePayload.getString(SOURCE_CORRELATION_ID));

        final UUID documentFileServiceId = UUID.fromString(documentAvailablePayload.getString(DOCUMENT_FILE_SERVICE_ID));

        LOGGER.info("Retrieved Nces Notification Document Available Public event");

        final JsonEnvelope jsonEnvelope = assembleEnvelopeWithPayloadAndMetaDetails(
                documentAvailablePayload, envelope.metadata().name(), envelope.metadata().userId().orElse(null));

        //Uploading the file
        LOGGER.info("Stored material {} in file store {}", materialId, documentFileServiceId);
        final UploadMaterialContextBuilder uploadMaterialContextBuilder = new UploadMaterialContextBuilder();
        uploadMaterialService.uploadMaterial(uploadMaterialContextBuilder
                .setSender(sender)
                .setOriginatingEnvelope(jsonEnvelope)
                .setMaterialId(materialId)
                .setFileId(documentFileServiceId)
                .build());
    }
}