package uk.gov.moj.cpp.results.event.service;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FileServiceTest {

    @Mock
    private Sender sender;
    @InjectMocks
    private FileService fileService;
    @Mock
    private FileStorer fileStorer;
    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> argumentCaptor;

    @Test
    public void shouldStorePayloadIntoFileService() throws FileServiceException {

        UUID fileId = UUID.randomUUID();

        String fileName = "NcesNotificationRequest_.pdf";

        String templateName = "NCESNotification_template";

        when(fileStorer.store(any(JsonObject.class), any(InputStream.class))).thenReturn(fileId);

        final UUID exceptedFileId = fileService.storePayload(createObjectBuilder().build(), fileName, templateName);

        assertThat(exceptedFileId, equalTo(fileId));
    }

    @Test
    public void shouldNotStorePayloadIntoFileService() throws FileServiceException {

        String fileName = "NcesNotificationRequest_.pdf";

        String templateName = "NCESNotification_template";

        when(fileStorer.store(any(JsonObject.class), any(InputStream.class))).thenThrow(FileServiceException.class);

        assertThrows(RuntimeException.class, () -> {
            fileService.storePayload(createObjectBuilder().build(), fileName, templateName);
        });
    }

}