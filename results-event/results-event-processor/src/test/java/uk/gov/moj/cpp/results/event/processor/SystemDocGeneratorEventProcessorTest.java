package uk.gov.moj.cpp.results.event.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.results.event.service.NotificationNotifyService;
import uk.gov.moj.cpp.results.event.service.UploadMaterialService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
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

        doNothing().when(uploadMaterialService).uploadMaterial(any());

        systemDocGeneratorEventProcessor.handleDocumentAvailable(jsonEnvelope);

        verify(uploadMaterialService).uploadMaterial(any());
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
        JsonArrayBuilder infoArrayBuilder = JsonObjects.createArrayBuilder();
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

        doNothing().when(uploadMaterialService).uploadMaterial(any());
        final String url = "http://matarial.url";
        when(materialUrlGenerator.fileStreamUrlFor(eq(notificationId), eq(true))).thenReturn(url);

        systemDocGeneratorEventProcessor.handleDocumentAvailable(jsonEnvelope);

        verify(uploadMaterialService).uploadMaterial(any());


        verify(notificationNotifyService).sendEmailNotification(any(), argumentCaptor.capture());
        final JsonObject argumentCaptorValue = argumentCaptor.getValue();

        assertThat(argumentCaptorValue.getString("notificationId"), is(notificationId.toString()));
        assertThat(argumentCaptorValue.getString("templateId"), is(emailTemplateId.toString()));
        assertThat(argumentCaptorValue.getString("sendToAddress"), is(email));
        assertThat(argumentCaptorValue.getString("materialUrl"), is(url));
        final JsonObject personalisation = argumentCaptorValue.getJsonObject("personalisation");
        assertThat(personalisation.getString("defendants"), is(defendants));


    }
}