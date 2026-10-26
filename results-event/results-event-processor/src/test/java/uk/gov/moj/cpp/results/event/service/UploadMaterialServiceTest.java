package uk.gov.moj.cpp.results.event.service;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UploadMaterialServiceTest {

    private static final String NCES_ORIGINATOR = "nces";

    @Mock
    private MaterialService materialService;

    @InjectMocks
    private UploadMaterialService uploadMaterialService;

    private final UUID materialId = randomUUID();
    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.nces-email-notification-requested"),
            createObjectBuilder().build());

    @Test
    void shouldUploadMaterialByFileId() {
        final UUID fileId = randomUUID();

        uploadMaterialService.uploadMaterial(new UploadMaterialContextBuilder()
                .setOriginatingEnvelope(envelope)
                .setMaterialId(materialId)
                .setFileId(fileId)
                .build(), NCES_ORIGINATOR);

        verify(materialService).uploadMaterial(fileId, materialId, envelope, NCES_ORIGINATOR);
        verify(materialService, never()).uploadMaterialWithUri(any(), any(), any(), any());
    }

    @Test
    void shouldUploadMaterialByFileUri() {
        final String fileUri = "https://account.blob.core.windows.net/results/nces/documents/" + materialId + ".pdf";

        uploadMaterialService.uploadMaterialWithUri(new UploadMaterialContextBuilder()
                .setOriginatingEnvelope(envelope)
                .setMaterialId(materialId)
                .setFileUri(fileUri)
                .build(), NCES_ORIGINATOR);

        verify(materialService).uploadMaterialWithUri(fileUri, materialId, envelope, NCES_ORIGINATOR);
        verify(materialService, never()).uploadMaterial(any(), any(), any(), any());
    }
}
