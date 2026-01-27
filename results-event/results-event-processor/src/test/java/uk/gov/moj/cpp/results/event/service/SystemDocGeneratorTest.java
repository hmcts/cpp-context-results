package uk.gov.moj.cpp.results.event.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemDocGeneratorTest {

    private static final String NCES_EMAIL_NOTIFICATION_REQUEST = "NCES_EMAIL_NOTIFICATION_REQUEST";

    @Mock
    private Sender sender;
    @InjectMocks
    private SystemDocGenerator systemDocGenerator;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> argumentCaptor;

    @Test
    public void shouldGenerateDocument() {
        final String sourceCorrelationId = randomUUID().toString();
        final UUID payloadFileServiceId = randomUUID();
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                NCES_EMAIL_NOTIFICATION_REQUEST,
                TemplateIdentifier.NCES_EMAIL_NOTIFICATION_TEMPLATE_ID,
                ConversionFormat.PDF,
                sourceCorrelationId,
                payloadFileServiceId,
                null
        );

        systemDocGenerator.generateDocument(request, envelope());

        verify(sender).sendAsAdmin(argumentCaptor.capture());
        final Envelope<JsonObject> actual = argumentCaptor.getValue();
        assertThat(actual.metadata().name(), equalTo("systemdocgenerator.generate-document"));
        assertThat(actual.payload().getString("templateIdentifier"), equalTo("NCESNotification"));
        assertThat(actual.payload().getString("conversionFormat"), equalTo("pdf"));
        assertThat(actual.payload().getString("sourceCorrelationId"), equalTo(sourceCorrelationId));
        assertThat(actual.payload().getString("payloadFileServiceId"), equalTo(payloadFileServiceId.toString()));
    }

    @Test
    public void shouldGenerateDocumentWithAdditionalInformation() {
        final String sourceCorrelationId = randomUUID().toString();
        final UUID payloadFileServiceId = randomUUID();
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                NCES_EMAIL_NOTIFICATION_REQUEST,
                TemplateIdentifier.NCES_EMAIL_NOTIFICATION_TEMPLATE_ID,
                ConversionFormat.PDF,
                sourceCorrelationId,
                payloadFileServiceId,
                map
        );

        systemDocGenerator.generateDocument(request, envelope());

        verify(sender).sendAsAdmin(argumentCaptor.capture());
        final Envelope<JsonObject> actual = argumentCaptor.getValue();
        assertThat(actual.metadata().name(), equalTo("systemdocgenerator.generate-document"));
        final JsonObject payload = actual.payload();
        assertThat(payload.getString("templateIdentifier"), equalTo("NCESNotification"));
        assertThat(payload.getString("conversionFormat"), equalTo("pdf"));
        assertThat(payload.getString("sourceCorrelationId"), equalTo(sourceCorrelationId));
        assertThat(payload.getString("payloadFileServiceId"), equalTo(payloadFileServiceId.toString()));

        JsonArray additionalInformation = payload.getJsonArray("additionalInformation");
        assertThat(additionalInformation.size(), is(2));
        assertThat(additionalInformation.getJsonObject(0).getString("propertyName"), is("key1"));
        assertThat(additionalInformation.getJsonObject(0).getString("propertyValue"), is("value1"));
        assertThat(additionalInformation.getJsonObject(1).getString("propertyName"), is("key2"));
        assertThat(additionalInformation.getJsonObject(1).getString("propertyValue"), is("value2"));

    }

    private JsonEnvelope envelope() {
        return envelopeFrom(metadataWithRandomUUID("test envelope"), createObjectBuilder().build());
    }
}