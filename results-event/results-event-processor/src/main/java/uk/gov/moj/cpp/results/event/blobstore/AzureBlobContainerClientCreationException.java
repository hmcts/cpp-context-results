package uk.gov.moj.cpp.results.event.blobstore;

public class AzureBlobContainerClientCreationException extends RuntimeException {

    public AzureBlobContainerClientCreationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
