package uk.gov.moj.cpp.results.event.blobstore;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.slf4j.Logger;

/**
 * CDI producer for {@link BlobContainerClient}.
 *
 * <p>Credential scheme is picked by {@link AzureFileStoreBlobConfiguration#isAzuriteEnabled()}:
 * <ul>
 *   <li>{@code true} (local dev/IT only) - connects with {@code azure.filestore.connection-string}
 *       directly; Azurite has no Azure AD support, so it needs SharedKey auth.</li>
 *   <li>{@code false} (the default, every real environment) - uses {@code DefaultAzureCredential},
 *       which on AKS resolves to the pod's Workload Identity (FIC) automatically.  The endpoint
 *       is read from {@code azure.filestore.endpoint}.</li>
 * </ul>
 *
 * <p><strong>Scope note:</strong> {@code BlobContainerClient} is a {@code final} class.  Weld
 * cannot create a proxy subclass for it, so the {@link Produces} method must be {@link Dependent}
 * rather than {@code @ApplicationScoped}.  A single shared instance is constructed once in
 * {@link #initialise()} and returned on every injection point.
 *
 * <p><strong>One producer per WAR:</strong> Only one module in a given WAR deployment may
 * transitively include this class.  Duplicate producers cause WELD-001409 at deploy time.
 */
@SuppressWarnings("java:S6813")
@ApplicationScoped
public class AzureFileStoreBlobContainerClientProducer {

    private static final int HTTP_CONFLICT = 409;

    @Inject
    private Logger logger;

    @Inject
    private AzureFileStoreBlobConfiguration azureBlobConfiguration;

    private BlobContainerClient blobContainerClient;

    @PostConstruct
    public void initialise() {
        blobContainerClient = buildBlobContainerClient(azureBlobConfiguration);
        try {
            blobContainerClient.createIfNotExists();
        } catch (final HttpResponseException e) {
            if (e.getResponse() != null && e.getResponse().getStatusCode() == HTTP_CONFLICT) {
                logger.warn("BlobContainerClient.createIfNotExists returned 409 Conflict for container '{}' — container already exists",
                        azureBlobConfiguration.getContainerName());
            } else {
                throw new AzureBlobContainerClientCreationException(
                        "Failed to create BlobContainerClient for container '" + azureBlobConfiguration.getContainerName() + "'", e);
            }
        }
    }

    /**
     * Produces the shared {@link BlobContainerClient} instance.
     *
     * <p>Scope is {@link Dependent} rather than {@code @ApplicationScoped} because
     * {@code BlobContainerClient} is {@code final} and Weld cannot proxy it (WELD-001410).
     *
     * @return the container client initialised in {@link #initialise()}
     */
    @Produces
    @Dependent
    public BlobContainerClient blobContainerClient() {
        return blobContainerClient;
    }

    protected BlobContainerClient buildBlobContainerClient(final AzureFileStoreBlobConfiguration configuration) {
        final JdkHttpClientBuilder httpClientBuilder = new JdkHttpClientBuilder()
                .connectionTimeout(configuration.getConnectionTimeout())
                .responseTimeout(configuration.getResponseTimeout());

        if (configuration.isAzuriteEnabled()) {
            return new BlobServiceClientBuilder()
                    .httpClient(httpClientBuilder.build())
                    .connectionString(configuration.getConnectionString())
                    .buildClient()
                    .getBlobContainerClient(configuration.getContainerName());
        }

        return new BlobServiceClientBuilder()
                .httpClient(httpClientBuilder.build())
                .credential(new DefaultAzureCredentialBuilder().build())
                .endpoint(configuration.getEndpoint())
                .buildClient()
                .getBlobContainerClient(configuration.getContainerName());
    }
}
