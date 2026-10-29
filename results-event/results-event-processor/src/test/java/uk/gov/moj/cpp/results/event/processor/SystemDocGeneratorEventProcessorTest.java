package uk.gov.moj.cpp.results.event.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonMetadata.USER_ID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.material.client.MaterialUrlGenerator;
import uk.gov.moj.cpp.results.event.service.NotificationNotifyService;
import uk.gov.moj.cpp.results.event.service.UploadMaterialContext;
import uk.gov.moj.cpp.results.event.service.UploadMaterialService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

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
    private static final String PROPERTY_NAME = "propertyName";
    private static final String PROPERTY_VALUE = "propertyValue";


    @Mock
    private Sender sender;

    @InjectMocks
    private SystemDocGeneratorEventProcessor systemDocGeneratorEventProcessor;

    @Mock
    private UploadMaterialService uploadMaterialService;

    @Mock
    private NotificationNotifyService notificationNotifyService;

    @Mock
    private MaterialUrlGenerator materialUrlGenerator;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Captor
    private ArgumentCaptor<UploadMaterialContext> uploadMaterialContextCaptor;

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

        final Metadata metadata = metadataFrom(createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add(NAME, "public.systemdocgenerator.events.document-available")
                .add("originator", "court")
                .add("originator-nces", "nces")
                .add("context", createObjectBuilder()
                        .add(USER_ID, UUID.randomUUID().toString()))
                        .add("CJSCPPUID", UUID.randomUUID().toString())
                .build()).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata,
                createObjectBuilder()
                        .add("originatingSource", "NCES_EMAIL_NOTIFICATION_REQUEST")
                        .add("templateIdentifier", "OEE_Layout5")
                        .add("conversionFormat", "pdf")
                        .add("sourceCorrelationId", materialId.toString())
                        .add("payloadFileServiceId", fileId.toString())
                        .add("documentFileServiceId", UUID.randomUUID().toString())
                        .build());

        doNothing().when(uploadMaterialService).uploadMaterial(any(), eq("nces"));

        systemDocGeneratorEventProcessor.handleDocumentAvailable(jsonEnvelope);

        verify(uploadMaterialService).uploadMaterial(any(), eq("nces"));
    }

    @Test
    public void shouldHandlePoliceNotificationHearingResult() {
        ArgumentCaptor<JsonObject> argumentCaptor = ArgumentCaptor.forClass(JsonObject.class);
        final UUID notificationId = UUID.randomUUID();
        final UUID emailTemplateId = UUID.randomUUID();

        final UUID fileId = UUID.randomUUID();
        final String email = "test@test.com";
        final String defendants = "Jake Brown";

        final Metadata metadata = metadataFrom(createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add(NAME, "public.systemdocgenerator.events.document-available")
                .add("originator", "court")
                .add("originator-nces", "nces")
                .add("context", createObjectBuilder()
                        .add(USER_ID, UUID.randomUUID().toString()))
                .add("CJSCPPUID", UUID.randomUUID().toString())
                .build()).build();
        JsonArrayBuilder infoArrayBuilder = createArrayBuilder();
        final Map<String, String> additionalInfo = new HashMap<>();
        additionalInfo.put("notificationId", notificationId.toString());
        additionalInfo.put("emailTemplateId", emailTemplateId.toString());
        additionalInfo.put("sendToAddress", email);
        additionalInfo.put("defendants", defendants);
        additionalInfo.forEach((k, v) ->
                infoArrayBuilder.add(createObjectBuilder()
                        .add(PROPERTY_NAME, k)
                        .add(PROPERTY_VALUE, v)
                ));
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata,
                createObjectBuilder()
                        .add("originatingSource", "POLICE_NOTIFICATION_HEARING_RESULTS")
                        .add("templateIdentifier", "OEE_Layout5")
                        .add("conversionFormat", "thymeleaf")
                        .add("sourceCorrelationId", notificationId.toString())
                        .add("payloadFileServiceId", fileId.toString())
                        .add("documentFileServiceId", UUID.randomUUID().toString())
                        .add("additionalInformation", infoArrayBuilder.build())
                        .build());

        doNothing().when(uploadMaterialService).uploadMaterial(any(), eq("nces"));
        final String url = "http://matarial.url";
        when(materialUrlGenerator.fileStreamUrlFor(eq(notificationId), eq(true))).thenReturn(url);

        systemDocGeneratorEventProcessor.handleDocumentAvailable(jsonEnvelope);

        verify(uploadMaterialService).uploadMaterial(any(), eq("nces"));


        verify(notificationNotifyService).sendEmailNotification(any(), argumentCaptor.capture());
        final JsonObject argumentCaptorValue = argumentCaptor.getValue();

        assertThat(argumentCaptorValue.getString("notificationId"), is(notificationId.toString()));
        assertThat(argumentCaptorValue.getString("templateId"), is(emailTemplateId.toString()));
        assertThat(argumentCaptorValue.getString("sendToAddress"), is(email));
        assertThat(argumentCaptorValue.getString("materialUrl"), is(url));
        final JsonObject personalisation = argumentCaptorValue.getJsonObject("personalisation");
        assertThat(personalisation.getString("defendants"), is(defendants));


    }

    @Test
    public void handleDocumentAvailable_whenNcesDocumentIsBlobAddressed_shouldUploadMaterialByUri() {
        final UUID materialId = UUID.randomUUID();
        final String destinationFileUri = "https://blob.example/sdg/documents/nces.pdf";

        final JsonEnvelope jsonEnvelope = envelopeFrom(documentAvailableMetadata(),
                createObjectBuilder()
                        .add("originatingSource", "NCES_EMAIL_NOTIFICATION_REQUEST")
                        .add("templateIdentifier", "OEE_Layout5")
                        .add("conversionFormat", "pdf")
                        .add("sourceCorrelationId", materialId.toString())
                        .add("payloadFileUri", "https://blob.example/sdg/requests/nces.json")
                        .add("destinationFileUri", destinationFileUri)
                        .build());

        systemDocGeneratorEventProcessor.handleDocumentAvailable(jsonEnvelope);

        verify(uploadMaterialService).uploadMaterialWithUri(uploadMaterialContextCaptor.capture(), eq("nces"));
        verify(uploadMaterialService, never()).uploadMaterial(any(), any());
        assertThat(uploadMaterialContextCaptor.getValue().getMaterialId(), is(materialId));
        assertThat(uploadMaterialContextCaptor.getValue().getFileUri(), is(destinationFileUri));
        assertThat(uploadMaterialContextCaptor.getValue().getFileId(), is(nullValue()));
    }

    @Test
    public void handleDocumentAvailable_whenPoliceNotificationIsBlobAddressed_shouldUploadMaterialByUriAndSendEmail() {
        final UUID notificationId = UUID.randomUUID();
        final UUID emailTemplateId = UUID.randomUUID();
        final String destinationFileUri = "https://blob.example/sdg/documents/police.html";
        final String url = "http://material.url";

        final JsonArrayBuilder infoArrayBuilder = createArrayBuilder();
        final Map<String, String> additionalInfo = new HashMap<>();
        additionalInfo.put("notificationId", notificationId.toString());
        additionalInfo.put("emailTemplateId", emailTemplateId.toString());
        additionalInfo.put("sendToAddress", "test@test.com");
        additionalInfo.forEach((k, v) -> infoArrayBuilder.add(createObjectBuilder()
                .add(PROPERTY_NAME, k)
                .add(PROPERTY_VALUE, v)));

        final JsonEnvelope jsonEnvelope = envelopeFrom(documentAvailableMetadata(),
                createObjectBuilder()
                        .add("originatingSource", "POLICE_NOTIFICATION_HEARING_RESULTS")
                        .add("templateIdentifier", "OEE_Layout5")
                        .add("conversionFormat", "thymeleaf")
                        .add("sourceCorrelationId", notificationId.toString())
                        .add("payloadFileUri", "https://blob.example/sdg/requests/police.html")
                        .add("destinationFileUri", destinationFileUri)
                        .add("additionalInformation", infoArrayBuilder.build())
                        .build());
        when(materialUrlGenerator.fileStreamUrlFor(eq(notificationId), eq(true))).thenReturn(url);

        systemDocGeneratorEventProcessor.handleDocumentAvailable(jsonEnvelope);

        verify(uploadMaterialService).uploadMaterialWithUri(uploadMaterialContextCaptor.capture(), eq("nces"));
        verify(uploadMaterialService, never()).uploadMaterial(any(), any());
        assertThat(uploadMaterialContextCaptor.getValue().getMaterialId(), is(notificationId));
        assertThat(uploadMaterialContextCaptor.getValue().getFileUri(), is(destinationFileUri));

        final ArgumentCaptor<JsonObject> notificationCaptor = ArgumentCaptor.forClass(JsonObject.class);
        verify(notificationNotifyService).sendEmailNotification(any(), notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getString("materialUrl"), is(url));
    }

    @Test
    public void handleDocumentGenerationFailedEvent_whenFileServiceAddressed_shouldNotThrow() {
        final JsonEnvelope jsonEnvelope = envelopeFrom(documentAvailableMetadata(),
                generationFailedPayload().add("payloadFileServiceId", UUID.randomUUID().toString()).build());

        assertDoesNotThrow(() -> systemDocGeneratorEventProcessor.handleDocumentGenerationFailedEvent(jsonEnvelope));
    }

    @Test
    public void handleDocumentGenerationFailedEvent_whenBlobAddressed_shouldNotThrow() {
        final JsonEnvelope jsonEnvelope = envelopeFrom(documentAvailableMetadata(),
                generationFailedPayload().add("payloadFileUri", "https://blob.example/sdg/requests/nces.json").build());

        assertDoesNotThrow(() -> systemDocGeneratorEventProcessor.handleDocumentGenerationFailedEvent(jsonEnvelope));
    }

    private static JsonObjectBuilder generationFailedPayload() {
        return createObjectBuilder()
                .add("originatingSource", "NCES_EMAIL_NOTIFICATION_REQUEST")
                .add("templateIdentifier", "OEE_Layout5")
                .add("conversionFormat", "pdf")
                .add("sourceCorrelationId", UUID.randomUUID().toString())
                .add("reason", "template not found");
    }

    private static Metadata documentAvailableMetadata() {
        return metadataFrom(createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add(NAME, "public.systemdocgenerator.events.document-available")
                .add("context", createObjectBuilder()
                        .add(USER_ID, UUID.randomUUID().toString()))
                .build()).build();
    }
}