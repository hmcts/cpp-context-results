package uk.gov.moj.cpp.results.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.FAILED;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.SUCCESS;
import static uk.gov.moj.cpp.results.event.helper.Originator.isFileUriMaterial;
import static uk.gov.moj.cpp.results.event.helper.Originator.isResultsMaterial;
import static uk.gov.moj.cpp.results.event.service.NotificationNotifyService.isResultsNotification;
import static uk.gov.moj.cpp.results.event.service.NotificationNotifyService.materialIdFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.DocumentDeliveryStatus;
import uk.gov.moj.cpp.domains.EmailDeliveryStatus;
import uk.gov.moj.cpp.results.event.blobstore.ResultsBlobContainer;
import uk.gov.moj.cpp.results.event.service.DocumentDeliveryStatusService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records the outcome other contexts report for a document results sent them - material, SJP and
 * notification notify - on its delivery tracking. An outcome for a document that is not tracked (a
 * document of another context, or of the file service path) is ignored by the aggregate.
 */
@ServiceComponent(EVENT_PROCESSOR)
public class DocumentDeliveryOutcomeEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDeliveryOutcomeEventProcessor.class);

    private static final String MATERIAL_ID = "materialId";
    private static final String CASE_ID = "caseId";

    @Inject
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    @Inject
    private ResultsBlobContainer resultsBlobContainer;

    /**
     * Only a material results uploaded on the file uri path is recorded - known by the originator
     * material echoes back from the upload, as material.material-added is in
     * {@link MaterialAddedEventProcessor}.
     */
    @Handles("public.events.material.failed-to-add-material")
    public void handleFailedToAddMaterial(final JsonEnvelope event) {
        if (isResultsMaterial(event.metadata()) && isFileUriMaterial(event.metadata())) {
            final UUID materialId = UUID.fromString(event.payloadAsJsonObject().getString(MATERIAL_ID));
            LOGGER.info("Material {} failed to be added", materialId);
            documentDeliveryStatusService.recordMaterialStatus(event.metadata(), materialId, FAILED);
        }
    }

    @Handles("public.sjp.case-document-added")
    public void handleCaseDocumentAdded(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        recordSjpStatus(event, payload.getString(CASE_ID), payload.getString("documentUri", null), SUCCESS);
    }

    /**
     * Raised when the document is already on the case, so it has been delivered.
     */
    @Handles("public.sjp.case-document-addition-failed")
    public void handleCaseDocumentAdditionFailed(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        recordSjpStatus(event, payload.getString(CASE_ID, null), payload.getString("documentUri", null), SUCCESS);
    }

    @Handles("public.sjp.events.case-document-upload-rejected")
    public void handleCaseDocumentUploadRejected(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        recordSjpStatus(event, payload.getString(CASE_ID, null), payload.getString("documentReferenceUri", null), FAILED);
    }

    @Handles("public.notificationnotify.events.notification-sent")
    public void handleNotificationSent(final JsonEnvelope event) {
        recordEmailStatus(event, EmailDeliveryStatus.SUCCESS);
    }

    @Handles("public.notificationnotify.events.notification-failed")
    public void handleNotificationFailed(final JsonEnvelope event) {
        recordEmailStatus(event, EmailDeliveryStatus.FAILED);
    }

    /**
     * Notificationnotify publishes these events for every context's emails; only one whose client
     * context carries the results prefix is the email of a document results tracks, and the rest of
     * the client context is its material id.
     */
    private void recordEmailStatus(final JsonEnvelope event, final EmailDeliveryStatus emailStatus) {
        final JsonObject payload = event.payloadAsJsonObject();
        if (isResultsNotification(payload)) {
            final UUID materialId = materialIdFrom(payload);
            LOGGER.info("Email of material {} is {}", materialId, emailStatus);
            documentDeliveryStatusService.recordEmailStatus(event.metadata(), materialId, emailStatus);
        }
    }

    /**
     * SJP names the document by the blob uri it was filed with, which names the material results
     * stored it for. Only a document results filed - its uri in results' blob container - is
     * recorded; one of another context, or one without the uri (the file service path), is ignored.
     *
     * @throws NullPointerException for a document results filed whose event does not name the case,
     * so the event fails rather than being ignored
     */
    private void recordSjpStatus(final JsonEnvelope event, final String caseId, final String documentUri, final DocumentDeliveryStatus sjpStatus) {
        if (!resultsBlobContainer.contains(documentUri)) {
            return;
        }
        documentDeliveryStatusService.recordSjpStatus(event.metadata(), resultsBlobContainer.materialIdOf(documentUri),
                UUID.fromString(caseId), sjpStatus);
    }
}
