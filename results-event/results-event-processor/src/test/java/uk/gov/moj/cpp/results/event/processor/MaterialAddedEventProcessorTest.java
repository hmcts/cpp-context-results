package uk.gov.moj.cpp.results.event.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.results.event.helper.Originator;

import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class MaterialAddedEventProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private Enveloper enveloper;

    @Mock
    private MaterialUrlGenerator materialUrlGenerator;

    @Mock
    private JsonEnvelope outEnvelope;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private MaterialAddedEventProcessor materialAddedEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> sentEnvelopes;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleTheMaterialAdded() {
        final String materialId = randomUUID().toString();
        final String materialUrl = "http://localhost:8080/";
        final JsonObject metaDataJson = Json.createObjectBuilder()
                .add(Originator.SOURCE_NCES, Originator.ORIGINATOR_VALUE_NCES)
                .add("id", UUID.randomUUID().toString())
                .add("userId", UUID.randomUUID().toString())
                .add("name", "dummy")
                .build();
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataFrom(metaDataJson))
                .withPayloadOf(materialId, "materialId")
                .build();
        when(materialUrlGenerator.pdfFileStreamUrlFor(fromString(materialId))).thenReturn(materialUrl);
        Function<Object, JsonEnvelope> factory = (o) -> outEnvelope;
        when(enveloper.withMetadataFrom(jsonEnvelope, "results.command.nces-document-notification"))
                .thenReturn(factory);
        final Envelope<JsonObject> envelope = mock(Envelope.class);
        when(envelope.payload()).thenReturn(Json.createObjectBuilder().build());

        materialAddedEventProcessor.processMaterialAdded(jsonEnvelope);

        verify(materialUrlGenerator, times(1)).pdfFileStreamUrlFor(fromString(materialId));
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        verify(sender, times(1)).send(sentEnvelopes.capture());
        assertThat(sentEnvelopes.getValue(), is(outEnvelope));
    }

}
