package uk.gov.moj.cpp.results.event.service;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.time.ZonedDateTime;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentGeneratorServiceTest {

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

    /** 00:30 on 2 July in the UK, still 1 July in UTC. The JVM runs on UK time. */
    @Test
    public void chd10_generatedFileNameTimestamp_should_use_jvm_zone() throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(randomUUID()));
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(new byte[]{1});
        when(fileStorer.store(any(), any())).thenReturn(randomUUID());

        final String fileName = generateFileNameAtUkTime(ZonedDateTime.parse("2026-07-01T23:30:00Z"));

        assertThat(fileName, is("ENFDocumentOrder_20260702003000.pdf"));
    }

    private String generateFileNameAtUkTime(final ZonedDateTime now) {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());
        final TimeZone originalZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
        try (MockedConstruction<UtcClock> utcClock = mockConstruction(UtcClock.class, (clock, context) -> when(clock.now()).thenReturn(now))) {
            return documentGeneratorService.generateNcesDocument(sender, envelope, randomUUID(), randomUUID(), "originator").getFilename();
        } finally {
            TimeZone.setDefault(originalZone);
        }
    }
}
