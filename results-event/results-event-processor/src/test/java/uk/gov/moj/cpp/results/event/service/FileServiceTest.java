package uk.gov.moj.cpp.results.event.service;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;

import java.io.InputStream;
import java.util.UUID;

import javax.json.JsonObject;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
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