package uk.gov.moj.cpp.results.event.processor;

import static java.lang.Boolean.TRUE;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.SUCCESS;
import static uk.gov.moj.cpp.results.event.helper.Originator.isFileUriMaterial;
import static uk.gov.moj.cpp.results.event.helper.Originator.isMigratedInactiveNcesMaterial;
import static uk.gov.moj.cpp.results.event.helper.Originator.isNcesMaterial;
import static uk.gov.moj.cpp.results.event.helper.Originator.isResultsMaterial;

import uk.gov.justice.results.courts.NcesDocumentNotification;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.material.client.MaterialUrlGenerator;
import uk.gov.moj.cpp.results.event.helper.Originator;
import uk.gov.moj.cpp.results.event.service.DocumentDeliveryStatusService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class MaterialAddedEventProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MaterialAddedEventProcessor.class.getName());
    public static final String ORIGINATOR = Originator.SOURCE_NCES;
    public static final String RESULTS_COMMAND_INACTIVE_MIGRATED_NCES_DOCUMENT_NOTIFICATION = "result.command.migrated-inactive-nces-document-notification";
    public static final String MATERIAL_ID = "materialId";
    public static final String MATERIAL_URL = "materialUrl";
    public static final String MASTER_DEFENDANT_ID = "masterDefendantId";
    public static final String CASE_ID = "caseId";
    public static final String SEPARATOR = ":";
    // the material was uploaded on the file uri path, carried to the email so its delivery is tracked
    public static final String IS_FILE_URI = "isFileUri";

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    @Handles("material.material-added")
    public void processMaterialAdded(final JsonEnvelope envelope) {
        LOGGER.info("Received MaterialAddedEvent {}", envelope.toObfuscatedDebugString());
        if (isResultsMaterial(envelope.metadata()) && isFileUriMaterial(envelope.metadata())) {
            documentDeliveryStatusService.recordMaterialStatus(envelope.metadata(),
                    UUID.fromString(envelope.payloadAsJsonObject().getString(MATERIAL_ID)), SUCCESS);
        }

        if (isNcesMaterial(envelope.metadata())) {
            processNcesDocumentNotification(envelope);
        } else if (isMigratedInactiveNcesMaterial(envelope.metadata())) {
            processMigratedInactiveNcesDocumentNotification(envelope);
        }
    }
    @SuppressWarnings("squid:CallToDeprecatedMethod")
    private void processNcesDocumentNotification(JsonEnvelope envelope) {

        final UUID materialId = UUID.fromString(envelope.payloadAsJsonObject().getString(MATERIAL_ID));
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);

        final NcesDocumentNotification ncesDocumentNotificationCommand = NcesDocumentNotification.ncesDocumentNotification()
                .withMaterialId(materialId)
                .withMaterialUrl(materialUrl)
                .withIsFileUri(isFileUriMaterial(envelope.metadata()) ? TRUE : null)
                .build();

        this.sender.send(this.enveloper.withMetadataFrom(envelope, "results.command.nces-document-notification")
                .apply(this.objectToJsonObjectConverter.convert(ncesDocumentNotificationCommand)));
    }

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    private void processMigratedInactiveNcesDocumentNotification(JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID materialId = UUID.fromString(payload.getString(MATERIAL_ID));
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);

        final String originator = envelope.metadata().asJsonObject().getString(ORIGINATOR);
        final String[] splitted = originator.split(SEPARATOR);

        final JsonObjectBuilder enrichedPayload = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(MATERIAL_URL, materialUrl)
                .add(MASTER_DEFENDANT_ID, splitted[1])
                .add(CASE_ID, splitted[2]);
        if (isFileUriMaterial(envelope.metadata())) {
            enrichedPayload.add(IS_FILE_URI, true);
        }

        this.sender.send(this.enveloper
                .withMetadataFrom(envelope, RESULTS_COMMAND_INACTIVE_MIGRATED_NCES_DOCUMENT_NOTIFICATION)
                .apply(enrichedPayload.build()));
    }
}
