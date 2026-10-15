package uk.gov.moj.cpp.results.event.blobstore;

import static java.time.Duration.ofSeconds;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import com.azure.core.http.HttpResponse;
import com.azure.core.exception.HttpResponseException;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class AzureFileStoreBlobContainerClientProducerTest {

    private static final String CONTAINER_NAME = "results-filestore";

    @Mock
    private Logger logger;

    @Mock
    private AzureFileStoreBlobConfiguration configuration;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private HttpResponse httpResponse;

    private AzureFileStoreBlobContainerClientProducer producer;

    @BeforeEach
    void setUp() {
        producer = new AzureFileStoreBlobContainerClientProducer() {
            @Override
            protected BlobContainerClient buildBlobContainerClient(final AzureFileStoreBlobConfiguration ignored) {
                return blobContainerClient;
            }
        };
        setField(producer, "logger", logger);
        setField(producer, "azureBlobConfiguration", configuration);
    }

    @Test
    void shouldCreateContainerAndProduceTheSharedClient() {
        producer.initialise();

        verify(blobContainerClient).createIfNotExists();
        assertThat(producer.blobContainerClient(), is(sameInstance(blobContainerClient)));
        assertThat(producer.blobContainerClient(), is(sameInstance(producer.blobContainerClient())));
    }

    @Test
    void shouldTolerateConflictWhenContainerAlreadyExists() {
        when(configuration.getContainerName()).thenReturn(CONTAINER_NAME);
        when(httpResponse.getStatusCode()).thenReturn(409);
        when(blobContainerClient.createIfNotExists()).thenThrow(new HttpResponseException("container already exists", httpResponse));

        producer.initialise();

        assertThat(producer.blobContainerClient(), is(sameInstance(blobContainerClient)));
    }

    @Test
    void shouldFailWhenContainerCannotBeCreated() {
        when(configuration.getContainerName()).thenReturn(CONTAINER_NAME);
        when(httpResponse.getStatusCode()).thenReturn(403);
        final HttpResponseException forbidden = new HttpResponseException("authorization permission mismatch", httpResponse);
        when(blobContainerClient.createIfNotExists()).thenThrow(forbidden);

        final AzureBlobContainerClientCreationException exception =
                assertThrows(AzureBlobContainerClientCreationException.class, () -> producer.initialise());

        assertThat(exception.getCause(), is(sameInstance(forbidden)));
    }

    @Test
    void shouldFailWhenCreateIfNotExistsThrowsWithoutResponse() {
        when(configuration.getContainerName()).thenReturn(CONTAINER_NAME);
        when(blobContainerClient.createIfNotExists()).thenThrow(new HttpResponseException("connection reset", null));

        final AzureBlobContainerClientCreationException exception =
                assertThrows(AzureBlobContainerClientCreationException.class, () -> producer.initialise());

        assertThat(exception.getCause(), is(instanceOf(HttpResponseException.class)));
    }

    @Test
    void shouldBuildAzuriteClientFromConnectionString() {
        when(configuration.isAzuriteEnabled()).thenReturn(true);
        when(configuration.getConnectionTimeout()).thenReturn(ofSeconds(10));
        when(configuration.getResponseTimeout()).thenReturn(ofSeconds(30));
        when(configuration.getContainerName()).thenReturn(CONTAINER_NAME);
        when(configuration.getConnectionString()).thenReturn("DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
                + "AccountKey=a2V5;BlobEndpoint=http://localhost:10000/devstoreaccount1;");

        final BlobContainerClient client = new AzureFileStoreBlobContainerClientProducer().buildBlobContainerClient(configuration);

        assertThat(client.getBlobContainerName(), is(CONTAINER_NAME));
        assertThat(client.getBlobContainerUrl(), is("http://localhost:10000/devstoreaccount1/" + CONTAINER_NAME));
    }

    @Test
    void shouldBuildManagedIdentityClientFromEndpoint() {
        when(configuration.isAzuriteEnabled()).thenReturn(false);
        when(configuration.getConnectionTimeout()).thenReturn(ofSeconds(10));
        when(configuration.getResponseTimeout()).thenReturn(ofSeconds(30));
        when(configuration.getContainerName()).thenReturn(CONTAINER_NAME);
        when(configuration.getEndpoint()).thenReturn("https://account.blob.core.windows.net");

        final BlobContainerClient client = new AzureFileStoreBlobContainerClientProducer().buildBlobContainerClient(configuration);

        assertThat(client.getBlobContainerName(), is(CONTAINER_NAME));
        assertThat(client.getBlobContainerUrl(), is("https://account.blob.core.windows.net/" + CONTAINER_NAME));
    }
}
