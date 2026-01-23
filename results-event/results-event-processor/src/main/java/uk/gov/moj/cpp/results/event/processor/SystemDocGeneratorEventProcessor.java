package uk.gov.moj.cpp.results.event.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.results.event.helper.Originator.ORIGINATOR_VALUE_NCES;
import static uk.gov.moj.cpp.results.event.helper.Originator.assembleEnvelopeWithPayloadAndMetaDetails;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.notification.Notification;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.results.event.service.NotificationNotifyService;
import uk.gov.moj.cpp.results.event.service.UploadMaterialContextBuilder;
import uk.gov.moj.cpp.results.event.service.UploadMaterialService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
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
    private static final String POLICE_NOTIFICATION_HEARING_RESULTS = "POLICE_NOTIFICATION_HEARING_RESULTS";
    private static final String ADDITIONAL_INFORMATION = "additionalInformation";
    private static final String PROPERTY_NAME = "propertyName";
    private static final String PROPERTY_VALUE = "propertyValue";
    private static final String NOTIFICATION_ID = "notificationId";
    private static final String EMAIL_TEMPLATE_ID = "emailTemplateId";
    private static final String SEND_TO_ADDRESS = "sendToAddress";

    @Inject
    private UploadMaterialService uploadMaterialService;

    @Inject
    private NotificationNotifyService notificationNotifyService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Sender sender;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private MaterialUrlGenerator materialUrlGenerator;

    @Handles(PUBLIC_DOCUMENT_AVAILABLE_EVENT_NAME)
    public void handleDocumentAvailable(final JsonEnvelope documentAvailableEvent) {
        LOGGER.info(PUBLIC_DOCUMENT_AVAILABLE_EVENT_NAME + " {}", documentAvailableEvent.payload());

        final JsonObject documentAvailablePayload = documentAvailableEvent.payloadAsJsonObject();

        final String originatingSource = documentAvailablePayload.getString("originatingSource", "");

        if (NCES_EMAIL_NOTIFICATION_REQUEST.equalsIgnoreCase(originatingSource)) {
            processNcesNotificationDocumentAvailable(documentAvailableEvent);
        } else if (POLICE_NOTIFICATION_HEARING_RESULTS.equals(originatingSource)) {
            processPoliceNotificationHearingResult(documentAvailableEvent);
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

        if (NCES_EMAIL_NOTIFICATION_REQUEST.equalsIgnoreCase(originatingSource)
                || POLICE_NOTIFICATION_HEARING_RESULTS.equals(originatingSource)) {
            LOGGER.error("Asynchronous document generation failed. Failed to generate email notification, originatingSource: '{}', material id: '{}', payloadFileId: '{}', reason: '{}'",
                    originatingSource, materialId, payloadFileId, reason);
        }
    }

    private void processNcesNotificationDocumentAvailable(final JsonEnvelope envelope) {

        final JsonObject documentAvailablePayload = envelope.payloadAsJsonObject();

        final UUID materialId = UUID.fromString(documentAvailablePayload.getString(SOURCE_CORRELATION_ID));

        final UUID documentFileServiceId = UUID.fromString(documentAvailablePayload.getString(DOCUMENT_FILE_SERVICE_ID));

        LOGGER.info("Retrieved Nces Notification Document Available Public event");

        final JsonEnvelope jsonEnvelope = assembleEnvelopeWithPayloadAndMetaDetails(
                documentAvailablePayload, envelope.metadata().name(), envelope.metadata().userId().orElse(null), ORIGINATOR_VALUE_NCES);

        //Uploading the file
        LOGGER.info("Stored material {} in file store {}", materialId, documentFileServiceId);
        final UploadMaterialContextBuilder uploadMaterialContextBuilder = new UploadMaterialContextBuilder();
        uploadMaterialService.uploadMaterial(uploadMaterialContextBuilder
                .setSender(sender)
                .setOriginatingEnvelope(jsonEnvelope)
                .setMaterialId(materialId)
                .setFileId(documentFileServiceId)
                .build(), ORIGINATOR_VALUE_NCES);
    }

    private void processPoliceNotificationHearingResult(final JsonEnvelope envelope) {
        final JsonObject documentAvailablePayload = envelope.payloadAsJsonObject();
        final UUID materialId = UUID.fromString(documentAvailablePayload.getString(SOURCE_CORRELATION_ID));

        final UUID documentFileServiceId = UUID.fromString(documentAvailablePayload.getString(DOCUMENT_FILE_SERVICE_ID));
        final JsonArray additionalInformation = documentAvailablePayload.getJsonArray(ADDITIONAL_INFORMATION);

        final JsonEnvelope jsonEnvelope = assembleEnvelopeWithPayloadAndMetaDetails(
                documentAvailablePayload, envelope.metadata().name(), envelope.metadata().userId().orElse(null), ORIGINATOR_VALUE_NCES);

        LOGGER.info("Material with ID {} has been stored in the file store with ID {}", materialId, documentFileServiceId);
        final UploadMaterialContextBuilder uploadMaterialContextBuilder = new UploadMaterialContextBuilder();
        uploadMaterialService.uploadMaterial(uploadMaterialContextBuilder
                .setSender(sender)
                .setOriginatingEnvelope(jsonEnvelope)
                .setMaterialId(materialId)
                .setFileId(documentFileServiceId)
                .build(), ORIGINATOR_VALUE_NCES);

        final String url = materialUrlGenerator.fileStreamUrlFor(materialId, true);
        final Map<String, String> personalisationMap = new HashMap<>();
        additionalInformation.forEach(info -> personalisationMap.put(((JsonObject) info).getString(PROPERTY_NAME), ((JsonObject) info).getString(PROPERTY_VALUE)));
        final Notification notification = new Notification(UUID.fromString(personalisationMap.get(NOTIFICATION_ID)),
                UUID.fromString(personalisationMap.get(EMAIL_TEMPLATE_ID)),
                personalisationMap.get(SEND_TO_ADDRESS),
                personalisationMap, url);

        notificationNotifyService.sendEmailNotification(envelope, objectToJsonObjectConverter.convert(notification));
    }
}