package uk.gov.moj.cpp.results.event.blobstore;

import static java.time.Duration.ofSeconds;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import org.junit.jupiter.api.Test;

class AzureFileStoreBlobConfigurationTest {

    @Test
    void shouldExposeConfiguredValues() {
        final AzureFileStoreBlobConfiguration configuration = new AzureFileStoreBlobConfiguration();
        setField(configuration, "azuriteEnabled", "true");
        setField(configuration, "connectionString", "UseDevelopmentStorage=true");
        setField(configuration, "endpoint", "https://account.blob.core.windows.net");
        setField(configuration, "containerName", "results-filestore");
        setField(configuration, "connectionTimeoutSeconds", "5");
        setField(configuration, "responseTimeoutSeconds", "15");
        setField(configuration, "transferTimeoutSeconds", "120");
        setField(configuration, "payloadBlobPrefix", "nces/requests/");
        setField(configuration, "documentBlobPrefix", "nces/documents/");

        assertThat(configuration.isAzuriteEnabled(), is(true));
        assertThat(configuration.getConnectionString(), is("UseDevelopmentStorage=true"));
        assertThat(configuration.getEndpoint(), is("https://account.blob.core.windows.net"));
        assertThat(configuration.getContainerName(), is("results-filestore"));
        assertThat(configuration.getConnectionTimeout(), is(ofSeconds(5)));
        assertThat(configuration.getResponseTimeout(), is(ofSeconds(15)));
        assertThat(configuration.getTransferTimeout(), is(ofSeconds(120)));
        assertThat(configuration.getPayloadBlobPrefix(), is("nces/requests/"));
        assertThat(configuration.getDocumentBlobPrefix(), is("nces/documents/"));
    }

    @Test
    void shouldTreatAnythingOtherThanTrueAsAzuriteDisabled() {
        final AzureFileStoreBlobConfiguration configuration = new AzureFileStoreBlobConfiguration();

        setField(configuration, "azuriteEnabled", "false");
        assertThat(configuration.isAzuriteEnabled(), is(false));

        setField(configuration, "azuriteEnabled", "");
        assertThat(configuration.isAzuriteEnabled(), is(false));
    }
}
