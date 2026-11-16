package uk.gov.moj.cpp.results.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.FAILED;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.SUCCESS;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.EmailDeliveryStatus;
import uk.gov.moj.cpp.results.event.blobstore.ResultsBlobContainer;
import uk.gov.moj.cpp.results.event.service.DocumentDeliveryStatusService;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentDeliveryOutcomeEventProcessorTest {

    private static final String DOCUMENT_BLOB_URI = "http://localhost/results/sdg/documents/ENFDocumentOrder_1.pdf";

    @Mock
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    @Mock
    private ResultsBlobContainer resultsBlobContainer;

    @InjectMocks
    private DocumentDeliveryOutcomeEventProcessor processor;

    private final UUID materialId = randomUUID();
    private final UUID caseId = randomUUID();

    @Test
    void shouldHandleTheDocumentDeliveryOutcomeEvents() {
        assertThat(processor, isHandler(EVENT_PROCESSOR)
                .with(method("handleFailedToAddMaterial").thatHandles("public.events.material.failed-to-add-material"))
                .with(method("handleCaseDocumentAdded").thatHandles("public.sjp.case-document-added"))
                .with(method("handleCaseDocumentAdditionFailed").thatHandles("public.sjp.case-document-addition-failed"))
                .with(method("handleCaseDocumentUploadRejected").thatHandles("public.sjp.events.case-document-upload-rejected"))
                .with(method("handleNotificationSent").thatHandles("public.notificationnotify.events.notification-sent"))
                .with(method("handleNotificationFailed").thatHandles("public.notificationnotify.events.notification-failed")));
    }

    @Test
    void shouldRecordAMaterialResultsUploadedAsFailed() {
        final JsonEnvelope event = failedToAddMaterial("nces");

        processor.handleFailedToAddMaterial(event);

        verify(documentDeliveryStatusService).recordMaterialStatus(event.metadata(), materialId, FAILED);
    }

    @Test
    void shouldRecordAMigratedInactiveMaterialResultsUploadedAsFailed() {
        final JsonEnvelope event = failedToAddMaterial("nces:" + randomUUID() + ":" + randomUUID());

        processor.handleFailedToAddMaterial(event);

        verify(documentDeliveryStatusService).recordMaterialStatus(event.metadata(), materialId, FAILED);
    }

    @Test
    void shouldIgnoreAFailedMaterialResultsUploadedOnTheFileServicePath() {
        processor.handleFailedToAddMaterial(failedToAddMaterial("nces", false));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    void shouldIgnoreAFailedMaterialOfAnotherContext() {
        processor.handleFailedToAddMaterial(event("public.events.material.failed-to-add-material", createObjectBuilder()
                .add("materialId", materialId.toString()).add("failedTime", "2026-11-11T10:00:00Z").add("errorMessage", "error").build()));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    private JsonEnvelope failedToAddMaterial(final String ncesOriginator) {
        return failedToAddMaterial(ncesOriginator, true);
    }

    private JsonEnvelope failedToAddMaterial(final String ncesOriginator, final boolean fileUri) {
        return envelopeFrom(metadataFrom(createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("name", "public.events.material.failed-to-add-material")
                        .add("originator-nces", ncesOriginator)
                        .add("originator-file-uri", fileUri)
                        .build()),
                createObjectBuilder().add("materialId", materialId.toString()).add("failedTime", "2026-11-11T10:00:00Z")
                        .add("errorMessage", "error").build());
    }

    @Test
    void shouldRecordTheSjpCaseAsSucceededWhenTheDocumentIsAdded() {
        when(resultsBlobContainer.contains(DOCUMENT_BLOB_URI)).thenReturn(true);
        when(resultsBlobContainer.materialIdOf(DOCUMENT_BLOB_URI)).thenReturn(materialId);
        final JsonEnvelope event = event("public.sjp.case-document-added", createObjectBuilder()
                .add("caseId", caseId.toString()).add("id", randomUUID().toString()).add("materialId", randomUUID().toString())
                .add("documentUri", DOCUMENT_BLOB_URI).build());

        processor.handleCaseDocumentAdded(event);

        verify(documentDeliveryStatusService).recordSjpStatus(event.metadata(), materialId, caseId, SUCCESS);
    }

    @Test
    void shouldIgnoreAnSjpDocumentOfTheFileServicePath() {
        processor.handleCaseDocumentAdded(event("public.sjp.case-document-added", createObjectBuilder()
                .add("caseId", caseId.toString()).add("id", randomUUID().toString()).add("materialId", randomUUID().toString()).build()));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    void shouldRecordTheSjpCaseAsSucceededWhenTheDocumentIsAlreadyOnTheCase() {
        when(resultsBlobContainer.contains(DOCUMENT_BLOB_URI)).thenReturn(true);
        when(resultsBlobContainer.materialIdOf(DOCUMENT_BLOB_URI)).thenReturn(materialId);
        final JsonEnvelope event = event("public.sjp.case-document-addition-failed", createObjectBuilder()
                .add("caseId", caseId.toString()).add("documentId", randomUUID().toString())
                .add("documentUri", DOCUMENT_BLOB_URI).add("description", "already exists").build());

        processor.handleCaseDocumentAdditionFailed(event);

        verify(documentDeliveryStatusService).recordSjpStatus(event.metadata(), materialId, caseId, SUCCESS);
    }

    @Test
    void shouldRecordTheSjpCaseAsFailedWhenTheUploadIsRejected() {
        when(resultsBlobContainer.contains(DOCUMENT_BLOB_URI)).thenReturn(true);
        when(resultsBlobContainer.materialIdOf(DOCUMENT_BLOB_URI)).thenReturn(materialId);
        final JsonEnvelope event = event("public.sjp.events.case-document-upload-rejected", createObjectBuilder()
                .add("caseId", caseId.toString()).add("documentId", randomUUID().toString())
                .add("documentReferenceUri", DOCUMENT_BLOB_URI).add("description", "referred to court").build());

        processor.handleCaseDocumentUploadRejected(event);

        verify(documentDeliveryStatusService).recordSjpStatus(event.metadata(), materialId, caseId, FAILED);
    }

    @Test
    void shouldIgnoreAnSjpDocumentOfAnotherContext() {
        final String otherBlobUri = "http://localhost/other-context/documents/1.pdf";
        when(resultsBlobContainer.contains(otherBlobUri)).thenReturn(false);

        processor.handleCaseDocumentAdded(event("public.sjp.case-document-added", createObjectBuilder()
                .add("caseId", caseId.toString()).add("id", randomUUID().toString()).add("materialId", randomUUID().toString())
                .add("documentUri", otherBlobUri).build()));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    void shouldFailOnARejectionOfAResultsDocumentThatDoesNotNameTheCase() {
        when(resultsBlobContainer.contains(DOCUMENT_BLOB_URI)).thenReturn(true);
        final JsonEnvelope event = event("public.sjp.events.case-document-upload-rejected", createObjectBuilder()
                .add("documentReferenceUri", DOCUMENT_BLOB_URI).add("description", "referred to court").build());

        assertThrows(NullPointerException.class, () -> processor.handleCaseDocumentUploadRejected(event));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    void shouldRecordTheEmailAsSentForTheMaterialOfAResultsNotification() {
        final JsonEnvelope event = event("public.notificationnotify.events.notification-sent", createObjectBuilder()
                .add("notificationId", randomUUID().toString()).add("sentTime", "2026-11-11T10:00:00Z")
                .add("clientContext", "RESULTS_" + materialId).build());

        processor.handleNotificationSent(event);

        verify(documentDeliveryStatusService).recordEmailStatus(event.metadata(), materialId, EmailDeliveryStatus.SUCCESS);
    }

    @Test
    void shouldRecordTheEmailAsFailedForTheMaterialOfAResultsNotification() {
        final JsonEnvelope event = notificationFailed(createObjectBuilder()
                .add("notificationId", randomUUID().toString()).add("clientContext", "RESULTS_" + materialId));

        processor.handleNotificationFailed(event);

        verify(documentDeliveryStatusService).recordEmailStatus(event.metadata(), materialId, EmailDeliveryStatus.FAILED);
    }

    @Test
    void shouldIgnoreANotificationOfAnotherContext() {
        processor.handleNotificationFailed(notificationFailed(createObjectBuilder()
                .add("notificationId", randomUUID().toString()).add("clientContext", "STAGINGDVLA_" + materialId)));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    void shouldIgnoreANotificationWithoutAClientContext() {
        processor.handleNotificationFailed(notificationFailed(createObjectBuilder().add("notificationId", materialId.toString())));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    private static JsonEnvelope notificationFailed(final JsonObjectBuilder payload) {
        return event("public.notificationnotify.events.notification-failed",
                payload.add("failedTime", "2026-11-11T10:00:00Z").add("errorMessage", "error").build());
    }

    private static JsonEnvelope event(final String name, final JsonObject payload) {
        return envelopeFrom(metadataWithRandomUUID(name), payload);
    }
}
