package uk.gov.moj.cpp.results.event.blobstore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResultsBlobContainerTest {

    @Mock
    private AzureFileStoreBlobConfiguration azureFileStoreBlobConfiguration;

    private static final UUID MATERIAL_ID = UUID.randomUUID();

    @InjectMocks
    private ResultsBlobContainer resultsBlobContainer;

    @BeforeEach
    void setUp() {
        when(azureFileStoreBlobConfiguration.getContainerName()).thenReturn("results");
    }

    @Test
    void shouldContainABlobOfResultsContainerInAzurite() {
        assertThat(resultsBlobContainer.contains("http://localhost:10000/devstoreaccount1/results/sdg%2Fdocuments%2FENFDocumentOrder_1.pdf"), is(true));
    }

    @Test
    void shouldContainABlobOfResultsContainerInAzure() {
        assertThat(resultsBlobContainer.contains("https://account.blob.core.windows.net/results/sdg%2Fdocuments%2FENFDocumentOrder_1.pdf"), is(true));
    }

    @Test
    void shouldNotContainABlobOfAnotherContainer() {
        assertThat(resultsBlobContainer.contains("https://account.blob.core.windows.net/stagingdvla/documents%2F1.pdf"), is(false));
    }

    @Test
    void shouldNotContainABlobOfAContainerWhoseNameStartsWithResults() {
        assertThat(resultsBlobContainer.contains("http://localhost:10000/devstoreaccount1/results-archive/1.pdf"), is(false));
    }

    @Test
    void shouldFailOnAUriThatIsNotAUrl() {
        assertThrows(IllegalArgumentException.class, () -> resultsBlobContainer.contains("not a url"));
    }

    @Test
    void shouldNotContainAMissingUri() {
        assertThat(resultsBlobContainer.contains(null), is(false));
    }

    @Test
    void shouldReadTheMaterialOfADocumentInAzurite() {
        assertThat(resultsBlobContainer.materialIdOf("http://localhost:10000/devstoreaccount1/results/sdg%2Fdocuments%2FENFDocumentOrder_" + MATERIAL_ID + ".pdf"),
                is(MATERIAL_ID));
    }

    @Test
    void shouldReadTheMaterialOfADocumentInAzure() {
        assertThat(resultsBlobContainer.materialIdOf("https://account.blob.core.windows.net/results/sdg/documents/ENFDocumentOrder_" + MATERIAL_ID + ".pdf"),
                is(MATERIAL_ID));
    }

    @Test
    void shouldFailOnADocumentOfResultsContainerThatIsNotNamedByAMaterial() {
        assertThrows(IllegalArgumentException.class, () -> resultsBlobContainer.materialIdOf(
                "https://account.blob.core.windows.net/results/sdg/documents/POLICE_NOTIFICATION_HEARING_RESULTS1.pdf"));
        assertThrows(IllegalArgumentException.class, () -> resultsBlobContainer.materialIdOf(
                "https://account.blob.core.windows.net/results/sdg/documents/ENFDocumentOrder_20261111.pdf"));
        assertThrows(StringIndexOutOfBoundsException.class, () -> resultsBlobContainer.materialIdOf(
                "https://account.blob.core.windows.net/results/sdg/documents/noextension"));
    }
}
