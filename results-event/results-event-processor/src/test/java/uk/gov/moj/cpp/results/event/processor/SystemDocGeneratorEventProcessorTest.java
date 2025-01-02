package uk.gov.moj.cpp.results.event.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonMetadata.USER_ID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.event.service.UploadMaterialService;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemDocGeneratorEventProcessorTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private SystemDocGeneratorEventProcessor systemDocGeneratorEventProcessor;

    @Mock
    private UploadMaterialService uploadMaterialService;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void testHandleDocumentAvailable() {
        final UUID materialId = UUID.randomUUID();

        final UUID fileId = UUID.randomUUID();

        final Metadata metadata = metadataFrom(Json.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add(NAME, "public.systemdocgenerator.events.document-available")
                .add("originator", "court")
                .add("originator-nces", "nces")
                .add("context", Json.createObjectBuilder()
                        .add(USER_ID, UUID.randomUUID().toString()))
                        .add("CJSCPPUID", UUID.randomUUID().toString())
                .build()).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata,
                Json.createObjectBuilder()
                        .add("originatingSource", "NCES_EMAIL_NOTIFICATION_REQUEST")
                        .add("templateIdentifier", "OEE_Layout5")
                        .add("conversionFormat", "pdf")
                        .add("sourceCorrelationId", materialId.toString())
                        .add("payloadFileServiceId", fileId.toString())
                        .add("documentFileServiceId", UUID.randomUUID().toString())
                        .build());

        doNothing().when(uploadMaterialService).uploadMaterial(any());

        systemDocGeneratorEventProcessor.handleDocumentAvailable(jsonEnvelope);

        verify(uploadMaterialService).uploadMaterial(any());
    }
}