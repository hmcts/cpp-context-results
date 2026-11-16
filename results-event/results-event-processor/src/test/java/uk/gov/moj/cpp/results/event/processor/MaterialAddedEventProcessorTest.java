package uk.gov.moj.cpp.results.event.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.SUCCESS;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.material.client.MaterialUrlGenerator;
import uk.gov.moj.cpp.results.event.helper.Originator;
import uk.gov.moj.cpp.results.event.service.DocumentDeliveryStatusService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

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

    @Mock
    private DocumentDeliveryStatusService documentDeliveryStatusService;

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
        final JsonObject metaDataJson = createObjectBuilder()
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
        when(envelope.payload()).thenReturn(createObjectBuilder().build());

        materialAddedEventProcessor.processMaterialAdded(jsonEnvelope);

        verify(materialUrlGenerator, times(1)).pdfFileStreamUrlFor(fromString(materialId));
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        verify(sender, times(1)).send(sentEnvelopes.capture());
        assertThat(sentEnvelopes.getValue(), is(outEnvelope));
        // the file service path is not tracked
        verify(documentDeliveryStatusService, never()).recordMaterialStatus(any(), any(), any());
    }

    @Test
    void shouldRecordTheMaterialOfTheFileUriPathAsAddedAndCarryThePathToItsEmail() {
        final String materialId = randomUUID().toString();
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataFrom(createObjectBuilder()
                        .add(Originator.SOURCE_NCES, Originator.ORIGINATOR_VALUE_NCES)
                        .add(Originator.ORIGINATOR_FILE_URI, true)
                        .add("id", UUID.randomUUID().toString())
                        .add("name", "material.material-added")
                        .build()))
                .withPayloadOf(materialId, "materialId")
                .build();
        when(materialUrlGenerator.pdfFileStreamUrlFor(fromString(materialId))).thenReturn("http://localhost:8080/");
        final List<Object> commandPayloads = new ArrayList<>();
        when(enveloper.withMetadataFrom(jsonEnvelope, "results.command.nces-document-notification")).thenReturn(payload -> {
            commandPayloads.add(payload);
            return outEnvelope;
        });

        materialAddedEventProcessor.processMaterialAdded(jsonEnvelope);

        verify(documentDeliveryStatusService).recordMaterialStatus(jsonEnvelope.metadata(), fromString(materialId), SUCCESS);
        assertThat(((JsonObject) commandPayloads.get(0)).getBoolean("isFileUri"), is(true));
    }

    @Test
    void shouldCarryTheFileUriPathOfAMigratedInactiveMaterialToItsEmail() {
        final String materialId = randomUUID().toString();
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataFrom(createObjectBuilder()
                        .add(Originator.SOURCE_NCES, Originator.ORIGINATOR_VALUE_NCES_CASEID + randomUUID() + ":" + randomUUID())
                        .add(Originator.ORIGINATOR_FILE_URI, true)
                        .add("id", UUID.randomUUID().toString())
                        .add("name", "material.material-added")
                        .build()))
                .withPayloadOf(materialId, "materialId")
                .build();
        when(materialUrlGenerator.pdfFileStreamUrlFor(fromString(materialId))).thenReturn("http://localhost:8080/");
        final List<Object> commandPayloads = new ArrayList<>();
        when(enveloper.withMetadataFrom(jsonEnvelope, MaterialAddedEventProcessor.RESULTS_COMMAND_INACTIVE_MIGRATED_NCES_DOCUMENT_NOTIFICATION)).thenReturn(payload -> {
            commandPayloads.add(payload);
            return outEnvelope;
        });

        materialAddedEventProcessor.processMaterialAdded(jsonEnvelope);

        verify(documentDeliveryStatusService).recordMaterialStatus(jsonEnvelope.metadata(), fromString(materialId), SUCCESS);
        assertThat(((JsonObject) commandPayloads.get(0)).getBoolean("isFileUri"), is(true));
    }

    @Test
    void shouldNotRecordTheDeliveryOfAMaterialOfAnotherContext() {
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataFrom(createObjectBuilder()
                        .add("id", UUID.randomUUID().toString())
                        .add("name", "material.material-added")
                        .build()))
                .withPayloadOf(randomUUID().toString(), "materialId")
                .build();

        materialAddedEventProcessor.processMaterialAdded(jsonEnvelope);

        verifyNoInteractions(documentDeliveryStatusService, sender);
    }

    @Test
    public void shouldHandleMigratedInactiveNcesDocumentNotificationWhenOriginatorStartsWithNcesCaseId() {
        final String materialId = randomUUID().toString();
        final String materialUrl = "http://localhost:8080/material.pdf";
        final String masterDefendantId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String originatorValue = Originator.ORIGINATOR_VALUE_NCES_CASEID + masterDefendantId + ":" + caseId;
        final JsonObject metaDataJson = createObjectBuilder()
                .add(Originator.SOURCE_NCES, originatorValue)
                .add("id", UUID.randomUUID().toString())
                .add("userId", UUID.randomUUID().toString())
                .add("name", "dummy")
                .build();
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataFrom(metaDataJson))
                .withPayloadOf(materialId, "materialId")
                .build();
        when(materialUrlGenerator.pdfFileStreamUrlFor(fromString(materialId))).thenReturn(materialUrl);
        Function<Object, JsonEnvelope> factory = (payload) -> {
            final JsonObject jsonPayload = (JsonObject) payload;
            final String[] splitted = originatorValue.split(":");
            final String expectedMasterDefendantId = splitted[1];
            final String expectedCaseId = splitted[2];
            final JsonObject expectedPayload = createObjectBuilder()
                    .add("materialId", materialId)
                    .add("materialUrl", materialUrl)
                    .add("masterDefendantId", expectedMasterDefendantId)
                    .add("caseId", expectedCaseId)
                    .build();
            assertThat(jsonPayload, is(expectedPayload));
            return outEnvelope;
        };
        when(enveloper.withMetadataFrom(jsonEnvelope, MaterialAddedEventProcessor.RESULTS_COMMAND_INACTIVE_MIGRATED_NCES_DOCUMENT_NOTIFICATION))
                .thenReturn(factory);

        materialAddedEventProcessor.processMaterialAdded(jsonEnvelope);

        verify(materialUrlGenerator, times(1)).pdfFileStreamUrlFor(fromString(materialId));
        verify(enveloper, times(1)).withMetadataFrom(jsonEnvelope, MaterialAddedEventProcessor.RESULTS_COMMAND_INACTIVE_MIGRATED_NCES_DOCUMENT_NOTIFICATION);
        verify(sender, times(1)).send(outEnvelope);
        // the file service path is not tracked
        verify(documentDeliveryStatusService, never()).recordMaterialStatus(any(), any(), any());
    }

}
