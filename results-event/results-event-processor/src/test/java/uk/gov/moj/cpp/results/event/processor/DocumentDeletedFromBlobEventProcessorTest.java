package uk.gov.moj.cpp.results.event.processor;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.results.event.processor.DocumentDeletedFromBlobEventProcessor.RESULTS_FILE_STORE_DELETE_FEATURE;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.event.blobstore.AzureFileStoreBlobConfiguration;
import uk.gov.moj.cpp.results.event.blobstore.ResultsBlobContainer;

import javax.json.JsonObjectBuilder;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentDeletedFromBlobEventProcessorTest {

    private static final String CONTAINER_NAME = "stack-results";
    private static final String CONTAINER_URI = "https://sadevfilestore.blob.core.windows.net/" + CONTAINER_NAME + "/";
    private static final String PAYLOAD_BLOB_NAME = "sdg/requests/POLICE_NOTIFICATION_HEARING_RESULTS1.html";
    private static final String DOCUMENT_BLOB_NAME = "sdg/documents/POLICE_NOTIFICATION_HEARING_RESULTS1.html";

    @InjectMocks
    private DocumentDeletedFromBlobEventProcessor documentDeletedFromBlobEventProcessor;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ResultsBlobContainer resultsBlobContainer;

    @Mock
    private AzureFileStoreBlobConfiguration azureFileStoreBlobConfiguration;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @Mock
    private BlobClient payloadBlobClient;

    @Mock
    private BlobClient documentBlobClient;

    @BeforeEach
    void setUp() {
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(resultsBlobContainer, "azureFileStoreBlobConfiguration", azureFileStoreBlobConfiguration);
    }

    @Test
    void shouldDeletePayloadAndDocumentBlobsWhenDeleteToggleIsEnabled() {
        when(featureControlGuard.isFeatureEnabled(RESULTS_FILE_STORE_DELETE_FEATURE)).thenReturn(true);
        when(azureFileStoreBlobConfiguration.getContainerName()).thenReturn(CONTAINER_NAME);
        when(blobContainerClient.getBlobClient(PAYLOAD_BLOB_NAME)).thenReturn(payloadBlobClient);
        when(blobContainerClient.getBlobClient(DOCUMENT_BLOB_NAME)).thenReturn(documentBlobClient);

        documentDeletedFromBlobEventProcessor.processDocumentDeletedFromBlob(
                event(CONTAINER_URI + PAYLOAD_BLOB_NAME, CONTAINER_URI + DOCUMENT_BLOB_NAME));

        verify(payloadBlobClient).deleteIfExists();
        verify(documentBlobClient).deleteIfExists();
    }

    @Test
    void shouldDeleteOnlyTheDocumentBlobOfADocumentWithNoPayloadBlob() {
        // NCES: results uploads the document itself, there is no payload blob
        when(featureControlGuard.isFeatureEnabled(RESULTS_FILE_STORE_DELETE_FEATURE)).thenReturn(true);
        when(azureFileStoreBlobConfiguration.getContainerName()).thenReturn(CONTAINER_NAME);
        when(blobContainerClient.getBlobClient(DOCUMENT_BLOB_NAME)).thenReturn(documentBlobClient);

        documentDeletedFromBlobEventProcessor.processDocumentDeletedFromBlob(event(null, CONTAINER_URI + DOCUMENT_BLOB_NAME));

        verify(documentBlobClient).deleteIfExists();
        verify(blobContainerClient).getBlobClient(DOCUMENT_BLOB_NAME);
        verifyNoMoreInteractions(blobContainerClient);
    }

    @Test
    void shouldNotDeleteABlobOutsideResultsContainer() {
        when(featureControlGuard.isFeatureEnabled(RESULTS_FILE_STORE_DELETE_FEATURE)).thenReturn(true);
        when(azureFileStoreBlobConfiguration.getContainerName()).thenReturn(CONTAINER_NAME);
        when(blobContainerClient.getBlobClient(DOCUMENT_BLOB_NAME)).thenReturn(documentBlobClient);

        documentDeletedFromBlobEventProcessor.processDocumentDeletedFromBlob(
                event("https://sadevfilestore.blob.core.windows.net/stack-other/" + PAYLOAD_BLOB_NAME, CONTAINER_URI + DOCUMENT_BLOB_NAME));

        verify(documentBlobClient).deleteIfExists();
        verify(blobContainerClient, never()).getBlobClient(PAYLOAD_BLOB_NAME);
    }

    @Test
    void shouldDeleteNothingWithoutBlobUris() {
        when(featureControlGuard.isFeatureEnabled(RESULTS_FILE_STORE_DELETE_FEATURE)).thenReturn(true);

        documentDeletedFromBlobEventProcessor.processDocumentDeletedFromBlob(event(null, null));

        verifyNoInteractions(blobContainerClient);
    }

    @Test
    void shouldFailSoTheEventIsRetriedWhenADeletionFails() {
        when(featureControlGuard.isFeatureEnabled(RESULTS_FILE_STORE_DELETE_FEATURE)).thenReturn(true);
        when(azureFileStoreBlobConfiguration.getContainerName()).thenReturn(CONTAINER_NAME);
        when(blobContainerClient.getBlobClient(PAYLOAD_BLOB_NAME)).thenReturn(payloadBlobClient);
        when(payloadBlobClient.deleteIfExists()).thenThrow(new IllegalStateException("blob store unavailable"));

        final JsonEnvelope event = event(CONTAINER_URI + PAYLOAD_BLOB_NAME, CONTAINER_URI + DOCUMENT_BLOB_NAME);

        assertThrows(IllegalStateException.class, () -> documentDeletedFromBlobEventProcessor.processDocumentDeletedFromBlob(event));

        verify(blobContainerClient, never()).getBlobClient(DOCUMENT_BLOB_NAME);
    }

    @Test
    void shouldFailOnABlobUriThatIsNotAUrl() {
        when(featureControlGuard.isFeatureEnabled(RESULTS_FILE_STORE_DELETE_FEATURE)).thenReturn(true);
        // stubbed so the uri itself is what fails, not a missing container name
        when(azureFileStoreBlobConfiguration.getContainerName()).thenReturn(CONTAINER_NAME);
        final JsonEnvelope event = event(null, "not a url");

        assertThrows(IllegalArgumentException.class, () -> documentDeletedFromBlobEventProcessor.processDocumentDeletedFromBlob(event));

        verifyNoInteractions(blobContainerClient);
    }

    @Test
    void shouldNotDeleteBlobsWhenDeleteToggleIsDisabled() {
        when(featureControlGuard.isFeatureEnabled(RESULTS_FILE_STORE_DELETE_FEATURE)).thenReturn(false);

        documentDeletedFromBlobEventProcessor.processDocumentDeletedFromBlob(
                event(CONTAINER_URI + PAYLOAD_BLOB_NAME, CONTAINER_URI + DOCUMENT_BLOB_NAME));

        verifyNoInteractions(blobContainerClient);
        verify(jsonObjectToObjectConverter, never()).convert(any(), any());
    }

    private static JsonEnvelope event(final String payloadBlobUri, final String documentBlobUri) {
        final JsonObjectBuilder payload = createObjectBuilder().add("materialId", randomUUID().toString());
        if (payloadBlobUri != null) {
            payload.add("payloadBlobUri", payloadBlobUri);
        }
        if (documentBlobUri != null) {
            payload.add("documentBlobUri", documentBlobUri);
        }
        return envelopeFrom(metadataWithRandomUUID("results.event.document-deleted-from-blob"), payload.build());
    }
}
