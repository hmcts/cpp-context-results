package uk.gov.moj.cpp.results.event.service;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentGeneratorServiceTest {

    private static final ZoneId ZONE_FAR_FROM_UTC = ZoneId.of("Pacific/Kiritimati");

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
    private Sender sender;

    @InjectMocks
    private DocumentGeneratorService documentGeneratorService;

    @Test
    public void chd10_generatedFileNameTimestamp_should_use_jvm_zone() throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(randomUUID()));
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(new byte[]{1});
        when(fileStorer.store(any(), any())).thenReturn(randomUUID());
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());
        final TimeZone originalZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE_FAR_FROM_UTC));
        final String fileName;
        try {
            fileName = documentGeneratorService.generateNcesDocument(sender, envelope, randomUUID(), randomUUID(), "originator").getFilename();
        } finally {
            TimeZone.setDefault(originalZone);
        }

        assertThat(SECONDS.between(fileTimestampIn(fileName), LocalDateTime.now(ZONE_FAR_FROM_UTC)), lessThanOrEqualTo(60L));
    }

    private LocalDateTime fileTimestampIn(final String fileName) {
        final String timestamp = fileName.substring(fileName.lastIndexOf('_') + 1, fileName.length() - ".pdf".length());
        return LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
}
