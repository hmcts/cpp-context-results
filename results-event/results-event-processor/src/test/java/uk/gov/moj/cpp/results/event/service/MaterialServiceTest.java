package uk.gov.moj.cpp.results.event.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.results.event.service.MaterialService.UPLOAD_MATERIAL;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialServiceTest {

    private static final String NCES_ORIGINATOR = "nces";
    private static final String FILE_URI = "https://account.blob.core.windows.net/results/nces/documents/material.pdf";

    @Mock
    private Sender sender;

    @InjectMocks
    private MaterialService materialService;

    @Captor
    private ArgumentCaptor<Envelope<?>> envelopeCaptor;

    private final UUID materialId = randomUUID();

    @Test
    public void shouldUploadMaterialByFileServiceIdAsUser() {
        final UUID fileServiceId = randomUUID();
        final String userId = randomUUID().toString();

        materialService.uploadMaterial(fileServiceId, materialId, envelopeWithUserId(userId), NCES_ORIGINATOR);

        verify(sender).send(envelopeCaptor.capture());
        verify(sender, never()).sendAsAdmin(any());
        final JsonObject payload = assertUploadFileCommand(envelopeCaptor.getValue(), userId);
        assertThat(payload.getString("fileServiceId"), is(fileServiceId.toString()));
        assertThat(payload.containsKey("fileUri"), is(false));
    }

    @Test
    public void shouldUploadMaterialByFileServiceIdAsAdminWhenNoUserId() {
        final UUID fileServiceId = randomUUID();

        materialService.uploadMaterial(fileServiceId, materialId, envelopeWithoutUserId(), NCES_ORIGINATOR);

        verify(sender).sendAsAdmin(envelopeCaptor.capture());
        verify(sender, never()).send(any());
        final JsonObject payload = assertUploadFileCommand(envelopeCaptor.getValue(), null);
        assertThat(payload.getString("fileServiceId"), is(fileServiceId.toString()));
    }

    @Test
    public void shouldUploadMaterialByFileUriAsUser() {
        final String userId = randomUUID().toString();

        materialService.uploadMaterialWithUri(FILE_URI, materialId, envelopeWithUserId(userId), NCES_ORIGINATOR);

        verify(sender).send(envelopeCaptor.capture());
        verify(sender, never()).sendAsAdmin(any());
        final JsonObject payload = assertUploadFileCommand(envelopeCaptor.getValue(), userId);
        assertThat(payload.getString("fileUri"), is(FILE_URI));
        // material.command.upload-file is an exclusive oneOf: a uri command must not also carry a file service id
        assertThat(payload.containsKey("fileServiceId"), is(false));
    }

    @Test
    public void shouldUploadMaterialByFileUriAsAdminWhenNoUserId() {
        materialService.uploadMaterialWithUri(FILE_URI, materialId, envelopeWithoutUserId(), NCES_ORIGINATOR);

        verify(sender).sendAsAdmin(envelopeCaptor.capture());
        verify(sender, never()).send(any());
        final JsonObject payload = assertUploadFileCommand(envelopeCaptor.getValue(), null);
        assertThat(payload.getString("fileUri"), is(FILE_URI));
        assertThat(payload.containsKey("fileServiceId"), is(false));
    }

    private JsonObject assertUploadFileCommand(final Envelope<?> command, final String expectedUserId) {
        assertThat(command.metadata().name(), is(UPLOAD_MATERIAL));
        assertThat(command.metadata().asJsonObject().getString("originator-nces"), is(NCES_ORIGINATOR));
        assertThat(command.metadata().userId().orElse(null), is(expectedUserId));
        final JsonObject payload = (JsonObject) command.payload();
        assertThat(payload.getString("materialId"), is(materialId.toString()));
        return payload;
    }

    private static JsonEnvelope envelopeWithUserId(final String userId) {
        return envelopeFrom(metadataWithRandomUUID("results.event.nces-email-notification-requested").withUserId(userId),
                createObjectBuilder().build());
    }

    private static JsonEnvelope envelopeWithoutUserId() {
        return envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                createObjectBuilder().build());
    }
}
