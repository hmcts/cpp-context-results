package uk.gov.moj.cpp.results.event.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.results.event.service.DocumentGeneratorService.ENF_DOCUMENT_ORDER;
import static uk.gov.moj.cpp.results.event.service.DocumentGeneratorService.NCES_DLRM_DOC_TEMPLATE_NAME;
import static uk.gov.moj.cpp.results.event.service.DocumentGeneratorService.NCES_DOCUMENT_TEMPLATE_NAME;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.event.blobstore.AzureFileStoreBlobConfiguration;
import uk.gov.moj.cpp.results.event.service.exception.FileUploadException;
import uk.gov.moj.cpp.results.event.service.exception.TemplateNameNotFoundException;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentGeneratorServiceTest {

    private static final String NCES_ORIGINATOR = "nces";
    private static final String DOCUMENT_BLOB_PREFIX = "nces/documents/";
    private static final String PAYLOAD_BLOB_PREFIX = "sdg/requests/";
    private static final String CONTAINER_URL = "https://account.blob.core.windows.net/results/";
    private static final byte[] PDF_CONTENT = "%PDF-1.4 nces".getBytes();

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Mock
    private DocumentGeneratorClient documentGeneratorClient;

    @Mock
    private FileStorer fileStorer;

    @Mock
    private UploadMaterialService uploadMaterialService;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private AzureFileStoreBlobConfiguration azureBlobConfiguration;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<UploadMaterialContext> uploadMaterialContextCaptor;

    @Captor
    private ArgumentCaptor<JsonObject> fileMetadataCaptor;

    @Captor
    private ArgumentCaptor<BlobParallelUploadOptions> blobUploadOptionsCaptor;

    private DocumentGeneratorService documentGeneratorService;

    private final UUID systemUserId = randomUUID();
    private final UUID userId = randomUUID();
    private final UUID materialId = randomUUID();
    private final JsonEnvelope ncesEnvelope = envelopeFrom(
            metadataWithRandomUUID("results.event.nces-email-notification-requested"),
            createObjectBuilder().add("materialId", materialId.toString()).build());

    @BeforeEach
    void setUp() {
        documentGeneratorService = new DocumentGeneratorService(systemUserProvider, documentGeneratorClientProducer, fileStorer, uploadMaterialService);
        setField(documentGeneratorService, "blobContainerClient", blobContainerClient);
        setField(documentGeneratorService, "azureBlobConfiguration", azureBlobConfiguration);
    }

    @Test
    void shouldStoreNcesDocumentInFileServiceAndUploadMaterialByFileId() throws Exception {
        final UUID fileId = randomUUID();
        givenPdfIsGenerated(NCES_DOCUMENT_TEMPLATE_NAME);
        when(fileStorer.store(fileMetadataCaptor.capture(), any(InputStream.class))).thenReturn(fileId);

        final FileParams fileParams = documentGeneratorService.generateNcesDocument(sender, ncesEnvelope, userId, materialId, NCES_ORIGINATOR);

        assertThat(fileParams.getFileId(), is(fileId));
        assertThat(fileParams.getFileUri(), is(nullValue()));
        assertGeneratedFileName(fileParams.getFilename());
        assertThat(fileMetadataCaptor.getValue().getString("fileName"), is(fileParams.getFilename()));

        verify(uploadMaterialService).uploadMaterial(uploadMaterialContextCaptor.capture(), eq(NCES_ORIGINATOR));
        final UploadMaterialContext context = uploadMaterialContextCaptor.getValue();
        assertThat(context.getFileId(), is(fileId));
        assertThat(context.getFileUri(), is(nullValue()));
        assertThat(context.getMaterialId(), is(materialId));
        assertThat(context.getUserId(), is(userId));
        assertThat(context.getSender(), is(sender));
        assertThat(context.getOriginatingEnvelope(), is(ncesEnvelope));

        verify(uploadMaterialService, never()).uploadMaterialWithUri(any(), any());
        verifyNoInteractions(blobContainerClient);
    }

    @Test
    void shouldUseDlrmTemplateForMigratedInactiveNcesDocument() throws Exception {
        givenPdfIsGenerated(NCES_DLRM_DOC_TEMPLATE_NAME);
        when(fileStorer.store(any(JsonObject.class), any(InputStream.class))).thenReturn(randomUUID());

        documentGeneratorService.generateMigratedInactiveNcesDocument(sender, ncesEnvelope, userId, materialId, NCES_ORIGINATOR);

        verify(documentGeneratorClient).generatePdfDocument(ncesEnvelope.payloadAsJsonObject(), NCES_DLRM_DOC_TEMPLATE_NAME, systemUserId);
        verify(uploadMaterialService).uploadMaterial(any(), eq(NCES_ORIGINATOR));
        verifyNoInteractions(blobContainerClient);
    }

    @Test
    void shouldReturnEmptyFileParamsWhenFileServiceStoreFails() throws Exception {
        givenPdfIsGenerated(NCES_DOCUMENT_TEMPLATE_NAME);
        when(fileStorer.store(any(JsonObject.class), any(InputStream.class))).thenThrow(new FileServiceException("file service unavailable"));

        final FileParams fileParams = documentGeneratorService.generateNcesDocument(sender, ncesEnvelope, userId, materialId, NCES_ORIGINATOR);

        assertThat(fileParams.getFileId(), is(nullValue()));
        assertThat(fileParams.getFileUri(), is(nullValue()));
        verifyNoInteractions(uploadMaterialService);
    }

    @Test
    void shouldUploadNcesDocumentToBlobStoreAndUploadMaterialByFileUri() throws Exception {
        // <document name>_<material id>.<extension>, under the configured prefix
        final String blobName = DOCUMENT_BLOB_PREFIX + ENF_DOCUMENT_ORDER + "_" + materialId + ".pdf";
        final String blobUrl = "https://account.blob.core.windows.net/results/" + blobName;
        givenPdfIsGenerated(NCES_DOCUMENT_TEMPLATE_NAME);
        givenBlobStore(blobName, blobUrl);

        final FileParams fileParams = documentGeneratorService.generateNcesDocumentWithUri(sender, ncesEnvelope, userId, materialId, NCES_ORIGINATOR);

        assertThat(fileParams.getFileUri(), is(blobUrl));
        assertThat(fileParams.getFileId(), is(nullValue()));
        assertGeneratedFileName(fileParams.getFilename());

        verify(blobClient).uploadWithResponse(blobUploadOptionsCaptor.capture(), eq(ofSeconds(300)), eq(Context.NONE));
        assertThat(blobUploadOptionsCaptor.getValue().getMetadata().get("fileName"), is(fileParams.getFilename()));

        verify(uploadMaterialService).uploadMaterialWithUri(uploadMaterialContextCaptor.capture(), eq(NCES_ORIGINATOR));
        final UploadMaterialContext context = uploadMaterialContextCaptor.getValue();
        assertThat(context.getFileUri(), is(blobUrl));
        assertThat(context.getFileId(), is(nullValue()));
        assertThat(context.getMaterialId(), is(materialId));
        assertThat(context.getUserId(), is(userId));
        assertThat(context.getSender(), is(sender));
        assertThat(context.getOriginatingEnvelope(), is(ncesEnvelope));

        verify(uploadMaterialService, never()).uploadMaterial(any(), any());
        verifyNoInteractions(fileStorer);
    }

    @Test
    void shouldUseDlrmTemplateAndUploadMigratedInactiveNcesDocumentToBlobStoreByFileUri() throws Exception {
        final String blobName = DOCUMENT_BLOB_PREFIX + ENF_DOCUMENT_ORDER + "_" + materialId + ".pdf";
        final String blobUrl = "https://account.blob.core.windows.net/results/" + blobName;
        givenPdfIsGenerated(NCES_DLRM_DOC_TEMPLATE_NAME);
        givenBlobStore(blobName, blobUrl);

        final FileParams fileParams = documentGeneratorService.generateMigratedInactiveNcesDocumentWithUri(sender, ncesEnvelope, userId, materialId, NCES_ORIGINATOR);

        assertThat(fileParams.getFileUri(), is(blobUrl));
        assertThat(fileParams.getFileId(), is(nullValue()));
        assertGeneratedFileName(fileParams.getFilename());

        verify(documentGeneratorClient).generatePdfDocument(ncesEnvelope.payloadAsJsonObject(), NCES_DLRM_DOC_TEMPLATE_NAME, systemUserId);
        verify(blobClient).uploadWithResponse(blobUploadOptionsCaptor.capture(), eq(ofSeconds(300)), eq(Context.NONE));
        assertThat(blobUploadOptionsCaptor.getValue().getMetadata().get("fileName"), is(fileParams.getFilename()));

        verify(uploadMaterialService).uploadMaterialWithUri(uploadMaterialContextCaptor.capture(), eq(NCES_ORIGINATOR));
        final UploadMaterialContext context = uploadMaterialContextCaptor.getValue();
        assertThat(context.getFileUri(), is(blobUrl));
        assertThat(context.getFileId(), is(nullValue()));
        assertThat(context.getMaterialId(), is(materialId));
        assertThat(context.getUserId(), is(userId));

        verify(uploadMaterialService, never()).uploadMaterial(any(), any());
        verifyNoInteractions(fileStorer);
    }

    @Test
    void shouldThrowFileUploadExceptionForMigratedInactiveNcesDocumentWhenPdfGenerationFails() throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(systemUserId));
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), eq(NCES_DLRM_DOC_TEMPLATE_NAME), eq(systemUserId))).thenThrow(new IOException("document generator unavailable"));

        assertThrows(FileUploadException.class,
                () -> documentGeneratorService.generateMigratedInactiveNcesDocumentWithUri(sender, ncesEnvelope, userId, materialId, NCES_ORIGINATOR));

        verifyNoInteractions(blobContainerClient);
        verifyNoInteractions(uploadMaterialService);
        verifyNoInteractions(fileStorer);
    }

    @Test
    void shouldPropagateBlobUploadFailureAndNotUploadMaterial() throws Exception {
        givenPdfIsGenerated(NCES_DOCUMENT_TEMPLATE_NAME);
        when(azureBlobConfiguration.getDocumentBlobPrefix()).thenReturn(DOCUMENT_BLOB_PREFIX);
        when(azureBlobConfiguration.getTransferTimeout()).thenReturn(ofSeconds(300));
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any())).thenThrow(new IllegalStateException("blob store unavailable"));

        assertThrows(IllegalStateException.class,
                () -> documentGeneratorService.generateNcesDocumentWithUri(sender, ncesEnvelope, userId, materialId, NCES_ORIGINATOR));

        verifyNoInteractions(uploadMaterialService);
        verifyNoInteractions(fileStorer);
    }

    @Test
    void shouldThrowFileUploadExceptionAndNotTouchBlobStoreWhenPdfGenerationFails() throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(systemUserId));
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), eq(NCES_DOCUMENT_TEMPLATE_NAME), eq(systemUserId))).thenThrow(new IOException("document generator unavailable"));

        assertThrows(FileUploadException.class,
                () -> documentGeneratorService.generateNcesDocumentWithUri(sender, ncesEnvelope, userId, materialId, NCES_ORIGINATOR));

        verifyNoInteractions(blobContainerClient);
        verifyNoInteractions(uploadMaterialService);
    }

    @Test
    void shouldThrowWhenSystemUserIsMissingOnBlobPath() {
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(empty());

        assertThrows(TemplateNameNotFoundException.class,
                () -> documentGeneratorService.generateNcesDocumentWithUri(sender, ncesEnvelope, userId, materialId, NCES_ORIGINATOR));

        verifyNoInteractions(blobContainerClient);
        verifyNoInteractions(uploadMaterialService);
    }

    @Test
    void shouldStorePayloadInBlobStoreAndReturnPayloadAndDestinationUris() {
        final String fileName = "POLICE_NOTIFICATION_HEARING_RESULTS" + randomUUID() + ".html";
        final String payloadBlobName = PAYLOAD_BLOB_PREFIX + fileName;
        final JsonObject payload = createObjectBuilder().add("urn", "URN123").build();
        final byte[] payloadBytes = payload.toString().getBytes(UTF_8);
        when(azureBlobConfiguration.getPayloadBlobPrefix()).thenReturn(PAYLOAD_BLOB_PREFIX);
        when(azureBlobConfiguration.getDocumentBlobPrefix()).thenReturn(DOCUMENT_BLOB_PREFIX);
        when(azureBlobConfiguration.getTransferTimeout()).thenReturn(ofSeconds(300));
        final String documentBlobName = DOCUMENT_BLOB_PREFIX + fileName;
        final BlobClient documentBlobClient = mock(BlobClient.class);
        when(blobContainerClient.getBlobClient(payloadBlobName)).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn(CONTAINER_URL + payloadBlobName);
        when(blobContainerClient.getBlobClient(documentBlobName)).thenReturn(documentBlobClient);
        when(documentBlobClient.getBlobUrl()).thenReturn(CONTAINER_URL + documentBlobName);

        final DocumentGeneratorService.Result result = documentGeneratorService.storePayloadWithUri(payload, fileName, "PoliceNotificationHearingResults", ConversionFormat.THYMELEAF);

        assertThat(result.payloadFileUri(), is(CONTAINER_URL + payloadBlobName));
        // the generated document goes beside the payload, under the document prefix and with the same file name
        assertThat(result.destinationFileUri(), is(CONTAINER_URL + documentBlobName));
        // only the payload is uploaded; the destination is just addressed
        verify(documentBlobClient, never()).uploadWithResponse(any(), any(), any());

        verify(blobClient).uploadWithResponse(blobUploadOptionsCaptor.capture(), eq(ofSeconds(300)), eq(Context.NONE));
        final Map<String, String> metadata = blobUploadOptionsCaptor.getValue().getMetadata();
        assertThat(metadata.get("fileName"), is(fileName));
        assertThat(metadata.get("conversionFormat"), is(ConversionFormat.THYMELEAF.getValue()));
        assertThat(metadata.get("templateName"), is("PoliceNotificationHearingResults"));
        assertThat(metadata.get("numberOfPages"), is("1"));
        assertThat(metadata.get("fileSize"), is(String.valueOf(payloadBytes.length)));

        verifyNoInteractions(fileStorer);
        verifyNoInteractions(uploadMaterialService);
    }

    @Test
    void shouldPropagatePayloadBlobUploadFailure() {
        when(azureBlobConfiguration.getPayloadBlobPrefix()).thenReturn(PAYLOAD_BLOB_PREFIX);
        when(azureBlobConfiguration.getTransferTimeout()).thenReturn(ofSeconds(300));
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any())).thenThrow(new IllegalStateException("blob store unavailable"));

        assertThrows(IllegalStateException.class,
                () -> documentGeneratorService.storePayloadWithUri(createObjectBuilder().build(), "payload.html", "template", ConversionFormat.THYMELEAF));

        verifyNoInteractions(fileStorer);
    }

    private void givenPdfIsGenerated(final String templateName) throws IOException {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(systemUserId));
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(ncesEnvelope.payloadAsJsonObject(), templateName, systemUserId)).thenReturn(PDF_CONTENT);
    }

    private void givenBlobStore(final String blobName, final String blobUrl) {
        when(azureBlobConfiguration.getDocumentBlobPrefix()).thenReturn(DOCUMENT_BLOB_PREFIX);
        when(azureBlobConfiguration.getTransferTimeout()).thenReturn(ofSeconds(300));
        when(blobContainerClient.getBlobClient(blobName)).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn(blobUrl);
    }

    private static void assertGeneratedFileName(final String fileName) {
        assertThat(fileName, startsWith("ENFDocumentOrder_"));
        assertThat(fileName, endsWith(".pdf"));
    }
}
