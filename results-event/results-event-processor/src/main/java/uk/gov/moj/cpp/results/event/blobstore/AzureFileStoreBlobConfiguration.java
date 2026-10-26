package uk.gov.moj.cpp.results.event.blobstore;

import static java.lang.Long.parseLong;
import static java.time.Duration.ofSeconds;

import uk.gov.justice.services.common.configuration.Value;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * JNDI-backed configuration for Azure Blob Storage access. {@code azure.filestore.
 * azurite-enabled} is the explicit switch between the two credential schemes: {@code "true"}
 * (local dev/IT only) makes {@link AzureFileStoreBlobContainerClientProducer} use {@code azure.
 * filestore.connection-string} directly (Azurite has no Azure AD support, so it needs SharedKey
 * auth); {@code "false"} (the default, every real environment) makes it authenticate via
 * {@code DefaultAzureCredential} (Workload Identity on AKS) against {@code azure.filestore.
 * endpoint}.
 *
 * <p>Unlike system-doc-generator - a participant that addresses a different producer's container
 * on every call, taken from the request URI - results is a producer that owns exactly one
 * container, configured by {@code azure.filestore.container-name}.
 */
@SuppressWarnings("java:S6813")
@ApplicationScoped
public class AzureFileStoreBlobConfiguration {

    @Inject
    @Value(key = "azure.filestore.azurite-enabled", defaultValue = "false")
    private String azuriteEnabled;

    @Inject
    @Value(key = "azure.filestore.connection-string", defaultValue = "")
    private String connectionString;

    @Inject
    @Value(key = "azure.filestore.endpoint", defaultValue = "")
    private String endpoint;

    @Inject
    @Value(key = "azure.filestore.container-name")
    private String containerName;

    @Inject
    @Value(key = "azure.filestore.connection-timeout-seconds", defaultValue = "10")
    private String connectionTimeoutSeconds;

    @Inject
    @Value(key = "azure.filestore.response-timeout-seconds", defaultValue = "30")
    private String responseTimeoutSeconds;

    @Inject
    @Value(key = "azure.filestore.transfer-timeout-seconds", defaultValue = "300")
    private String transferTimeoutSeconds;

    @Inject
    @Value(key = "azure.filestore.payload-blob-prefix", defaultValue = "sdg/requests/")
    private String payloadBlobPrefix;

    @Inject
    @Value(key = "azure.filestore.document-blob-prefix", defaultValue = "sdg/documents/")
    private String documentBlobPrefix;

    /**
     * Returns the raw {@code azure.filestore.connection-string} JNDI value. Only meaningful when
     * {@link #isAzuriteEnabled()} is {@code true}.
     */
    public String getConnectionString() {
        return connectionString;
    }

    /**
     * {@code true} when {@code azure.filestore.azurite-enabled} is explicitly set to
     * {@code "true"} (local dev/IT), in which case {@link AzureFileStoreBlobContainerClientProducer}
     * uses {@link #getConnectionString()} directly; {@code false} (the default, every real
     * environment) means it authenticates via {@code DefaultAzureCredential}.
     */
    public boolean isAzuriteEnabled() {
        return Boolean.parseBoolean(azuriteEnabled);
    }

    /**
     * Azure Blob Storage service endpoint, e.g. {@code https://mystorage.blob.core.windows.net}.
     * Only used when {@link #isAzuriteEnabled()} is {@code false}.
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * The one blob container owned by this service; created at startup via
     * {@code createIfNotExists()} if it does not already exist.
     */
    public String getContainerName() {
        return containerName;
    }

    /** TCP connection timeout. JNDI key {@code azure.filestore.connection-timeout-seconds}, default 10s. */
    public Duration getConnectionTimeout() {
        return ofSeconds(parseLong(connectionTimeoutSeconds));
    }

    /** Response-header timeout. JNDI key {@code azure.filestore.response-timeout-seconds}, default 30s. */
    public Duration getResponseTimeout() {
        return ofSeconds(parseLong(responseTimeoutSeconds));
    }

    /** Data-transfer deadline (upload/download body). JNDI key {@code azure.filestore.transfer-timeout-seconds}, default 300s. */
    public Duration getTransferTimeout() {
        return ofSeconds(parseLong(transferTimeoutSeconds));
    }

    /**
     * Virtual folder the payload sent to system-doc-generator is uploaded under. JNDI key
     * {@code azure.filestore.payload-blob-prefix}, default {@code sdg/requests/}.
     */
    public String getPayloadBlobPrefix() {
        return payloadBlobPrefix;
    }

    /**
     * Virtual folder system-doc-generator writes the generated document to. JNDI key
     * {@code azure.filestore.document-blob-prefix}, default {@code sdg/documents/}.
     */
    public String getDocumentBlobPrefix() {
        return documentBlobPrefix;
    }
}
