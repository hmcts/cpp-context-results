package uk.gov.moj.cpp.results.event.blobstore;

import static java.util.Objects.nonNull;

import java.util.UUID;

import javax.inject.Inject;

import com.azure.storage.blob.BlobUrlParts;

/**
 * The blob container results owns: tells a blob uri results stored apart from one of another
 * context by its container name, which {@link BlobUrlParts} reads from both the path style url
 * (Azurite) and the host style one (Azure).
 */
public class ResultsBlobContainer {

    @Inject
    private AzureFileStoreBlobConfiguration azureFileStoreBlobConfiguration;

    /**
     * @return whether the blob uri is in results' container; false for a uri in any other container,
     * or for a missing one
     * @throws IllegalArgumentException for a uri that is not a url
     */
    public boolean contains(final String blobUri) {
        return nonNull(blobUri)
                && azureFileStoreBlobConfiguration.getContainerName().equals(BlobUrlParts.parse(blobUri).getBlobContainerName());
    }

    /**
     * A document results stores is named {@code <documentName>_<materialId>.<extension>}, so its blob
     * uri names its material. To be called for a blob uri results' container {@link #contains}.
     *
     * @return the material of the document blob uri
     * @throws RuntimeException for a blob that is not named by a material id, so the event that
     * names it fails rather than being ignored
     */
    public UUID materialIdOf(final String documentBlobUri) {
        final String blobName = BlobUrlParts.parse(documentBlobUri).getBlobName();
        final int materialIdStart = blobName.lastIndexOf('_') + 1;
        final int extensionStart = blobName.lastIndexOf('.');
        return UUID.fromString(blobName.substring(materialIdStart, extensionStart));
    }
}
