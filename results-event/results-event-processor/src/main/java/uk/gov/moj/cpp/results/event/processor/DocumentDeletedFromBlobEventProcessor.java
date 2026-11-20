package uk.gov.moj.cpp.results.event.processor;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.event.DocumentDeletedFromBlob;
import uk.gov.moj.cpp.results.event.blobstore.ResultsBlobContainer;

import javax.inject.Inject;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobUrlParts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes the blobs of a document whose delivery is complete (see DocumentDeliveryAggregate), behind
 * the resultsFileStoreDelete toggle - the same flow as staging-dvla's.
 */
@ServiceComponent(EVENT_PROCESSOR)
public class DocumentDeletedFromBlobEventProcessor {

    static final String RESULTS_FILE_STORE_DELETE_FEATURE = "resultsFileStoreDelete";

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDeletedFromBlobEventProcessor.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    // the bean DocumentGeneratorService writes the blobs through, reused to delete them
    @Inject
    private BlobContainerClient blobContainerClient;

    @Inject
    private ResultsBlobContainer resultsBlobContainer;

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Handles("results.event.document-deleted-from-blob")
    public void processDocumentDeletedFromBlob(final JsonEnvelope envelope) {
        if (!featureControlGuard.isFeatureEnabled(RESULTS_FILE_STORE_DELETE_FEATURE)) {
            LOGGER.info("{} is disabled, the blobs are not deleted", RESULTS_FILE_STORE_DELETE_FEATURE);
            return;
        }
        final DocumentDeletedFromBlob documentDeletedFromBlob = jsonObjectToObjectConverter
                .convert(envelope.payloadAsJsonObject(), DocumentDeletedFromBlob.class);

        deleteBlob(documentDeletedFromBlob.getPayloadBlobUri());
        deleteBlob(documentDeletedFromBlob.getDocumentBlobUri());
    }

    // deleteIfExists keeps a redelivered event harmless; a blob outside results' own container (or a
    // missing uri, e.g. NCES has no payload blob) is never resolved against this container and deleted
    private void deleteBlob(final String blobUri) {
        if (!resultsBlobContainer.contains(blobUri)) {
            if (nonNull(blobUri)) {
                LOGGER.warn("Blob {} is not in results' container, it is not deleted", blobUri);
            }
            return;
        }
        blobContainerClient.getBlobClient(BlobUrlParts.parse(blobUri).getBlobName()).deleteIfExists();
    }
}
