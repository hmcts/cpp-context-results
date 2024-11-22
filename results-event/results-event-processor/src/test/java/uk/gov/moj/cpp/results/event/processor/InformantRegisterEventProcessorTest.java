package uk.gov.moj.cpp.results.event.processor;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.results.event.service.ApplicationParameters;
import uk.gov.moj.cpp.results.event.service.NotificationNotifyService;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
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
public class InformantRegisterEventProcessorTest {
    private static final String TEMPLATE_ID = randomUUID().toString();

    @Spy
    private final Enveloper enveloper = createEnveloper();
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    @Mock
    private FileStorer fileStorer;
    @Mock
    private NotificationNotifyService notificationNotifyService;
    @Mock
    private ApplicationParameters applicationParameters;
    @Captor
    private ArgumentCaptor<JsonObject> notificationJson;
    @InjectMocks
    private InformantRegisterEventProcessor informantRegisterEventProcessor;
    @Captor
    private ArgumentCaptor<JsonObject> filestorerMetadata;
    @Captor
    private ArgumentCaptor<ByteArrayInputStream> byteInputStreamArgumentCaptor;
    @Mock
    private Sender sender;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldGenerateInformantRegister() throws Exception {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("results.event.informant-register-generated")
                .withUserId(randomUUID().toString());
        final JsonArray informantRegisters = getJsonPayload("informant-register-document-requests.json").getJsonArray("informantRegisterDocumentRequests");

        final JsonObject payload = createObjectBuilder()
                .add("informantRegisterDocumentRequests", informantRegisters)
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataBuilder, payload);
        when(applicationParameters.getEmailTemplateId(anyString())).thenReturn(TEMPLATE_ID);

        informantRegisterEventProcessor.generateInformantRegister(event);
        verify(fileStorer).store(filestorerMetadata.capture(), byteInputStreamArgumentCaptor.capture());
        final String expectedCsvContent = getFileContent("informantRegister.csv");
        assertThat(IOUtils.toString(byteInputStreamArgumentCaptor.getValue(), defaultCharset()), is(expectedCsvContent));
        final JsonObject fileStoreMetadataValue = filestorerMetadata.getValue();
        final UUID fileId = randomUUID();
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().payload().toString(), is(containsString("31af405e-7b60-4dd8-a244-c24c2d3fa595")));
    }

    @Test
    public void shouldGenerateInformantRegisterForNoRecipients() throws Exception {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("results.event.informant-register-generated")
                .withUserId(randomUUID().toString());
        final JsonArray informantRegisters = getJsonPayload("informant-register-document-requests-without-recipients.json").getJsonArray("informantRegisterDocumentRequests");

        final JsonObject payload = createObjectBuilder()
                .add("informantRegisterDocumentRequests", informantRegisters)
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataBuilder, payload);

        informantRegisterEventProcessor.generateInformantRegister(event);
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().payload().toString(), is(containsString("31af405e-7b60-4dd8-a244-c24c2d3fa595")));
    }

    @Test
    public void notifyProsecutionAuthority() {
        final JsonArrayBuilder recipientJsonArray = createArrayBuilder();
        final String emailAddress1 = "abc@test.com";
        recipientJsonArray.add(createObjectBuilder()
                .add("recipientName", "Prosecutor_name")
                .add("emailTemplateName", "some template")
                .add("emailAddress1", emailAddress1).build());
        final UUID fileId = randomUUID();
        final JsonObject notificationObject = createObjectBuilder()
                .add("recipients", recipientJsonArray)
                .add("fileId", fileId.toString())
                .add("templateId", TEMPLATE_ID)
                .build();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("results.event.informant-register-notified").withUserId(randomUUID().toString()),
                notificationObject);
        informantRegisterEventProcessor.notifyProsecutionAuthority(requestEnvelope);
        verify(notificationNotifyService).sendEmailNotification(eq(requestEnvelope), notificationJson.capture());
        assertThat(notificationJson.getValue().getString("notificationId"), is(notNullValue()));
        assertThat(notificationJson.getValue().getString("templateId"), is(TEMPLATE_ID));
        assertThat(notificationJson.getValue().getString("sendToAddress"), is(emailAddress1));
        assertThat(notificationJson.getValue().getString("fileId"), is(fileId.toString()));
    }

    @Test
    public void notifyProsecutionAuthorityV2() {
        final JsonArrayBuilder recipientJsonArray = createArrayBuilder();
        final String emailAddress1 = "abc@test.com";
        recipientJsonArray.add(createObjectBuilder()
                .add("recipientName", "Prosecutor_name")
                .add("emailTemplateName", "some template")
                .add("emailAddress1", emailAddress1).build());
        final UUID fileId = randomUUID();
        final JsonObject notificationObject = createObjectBuilder()
                .add("recipients", recipientJsonArray)
                .add("fileId", fileId.toString())
                .add("templateId", TEMPLATE_ID)
                .build();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("results.event.informant-register-notified-v2").withUserId(randomUUID().toString()),
                notificationObject);
        informantRegisterEventProcessor.notifyProsecutionAuthorityV2(requestEnvelope);
        verify(notificationNotifyService).sendEmailNotification(eq(requestEnvelope), notificationJson.capture());
        assertThat(notificationJson.getValue().getString("notificationId"), is(notNullValue()));
        assertThat(notificationJson.getValue().getString("templateId"), is(TEMPLATE_ID));
        assertThat(notificationJson.getValue().getString("sendToAddress"), is(emailAddress1));
        assertThat(notificationJson.getValue().getString("fileId"), is(fileId.toString()));

    }

    private static JsonObject getJsonPayload(final String fileName) {
        return new StringToJsonObjectConverter().convert(getFileContent(fileName));
    }

    private static String getFileContent(final String fileName) {
        String response = null;
        try {
            response = Resources.toString(
                    getResource(fileName),
                    defaultCharset()
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return response;
    }

}
