package uk.gov.moj.cpp.results.event.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.results.event.service.EmailNotification.emailNotification;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.event.notifygateway.NotifyGatewayServiceBusConfiguration;

import java.io.StringReader;
import java.util.UUID;

import javax.json.JsonObject;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotifyGatewaySendEmailServiceTest {

    private static final String REPLY_QUEUE = "results-notification-result";
    private static final String SEND_EMAIL_NOTIFICATION = "send-email-notification";

    @Mock
    private ServiceBusSenderClient notifyGatewaySenderClient;

    @Mock
    private NotifyGatewayServiceBusConfiguration configuration;

    @InjectMocks
    private NotifyGatewaySendEmailService notifyGatewaySendEmailService;

    @Captor
    private ArgumentCaptor<ServiceBusMessage> messageCaptor;

    @BeforeEach
    void setUp() {
        // lenient: the client context helpers' test sends nothing
        lenient().when(configuration.getReplyQueueName()).thenReturn(REPLY_QUEUE);
    }

    @Test
    void shouldSendTheNcesEmailAsAJsonBodyWithItsClientContext() {
        final UUID notificationId = randomUUID();
        final UUID templateId = randomUUID();
        final UUID materialId = randomUUID();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.nces-email-notification")
                .withClientCorrelationId("client-correlation-id"), createObjectBuilder().build());

        notifyGatewaySendEmailService.sendNcesEmail(emailNotification()
                .withNotificationId(notificationId)
                .withTemplateId(templateId)
                .withSendToAddress("nces@example.com")
                .withSubject("NCES subject")
                .withMaterialUrl("http://localhost:8080/material-query-api/query/api/rest/material/material/" + materialId)
                .withMaterialId(materialId)
                .build(), envelope);

        verify(notifyGatewaySenderClient).sendMessage(messageCaptor.capture());
        final ServiceBusMessage message = messageCaptor.getValue();
        assertTransport(message, "client-correlation-id");

        final JsonObject body = bodyOf(message);
        assertThat(body.getString("notificationId"), is(notificationId.toString()));
        assertThat(body.getString("templateId"), is(templateId.toString()));
        assertThat(body.getString("sendToAddress"), is("nces@example.com"));
        assertThat(body.getString("materialUrl"), is("http://localhost:8080/material-query-api/query/api/rest/material/material/" + materialId));
        assertThat(body.getJsonObject("personalisation").getString("subject"), is("NCES subject"));
        assertThat(body.getString("clientContext"), is("RESULTS_" + materialId));
    }

    @Test
    void shouldSendTheNcesEmailWithoutAClientContextOrPersonalisationWhenItHasNeither() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.nces-email-notification"), createObjectBuilder().build());

        notifyGatewaySendEmailService.sendNcesEmail(emailNotification()
                .withNotificationId(randomUUID())
                .withTemplateId(randomUUID())
                .withSendToAddress("nces@example.com")
                .withMaterialUrl("http://localhost:8080/material")
                .build(), envelope);

        verify(notifyGatewaySenderClient).sendMessage(messageCaptor.capture());
        final ServiceBusMessage message = messageCaptor.getValue();
        // no client correlation id: the envelope's own id correlates the request
        assertTransport(message, envelope.metadata().id().toString());

        final JsonObject body = bodyOf(message);
        assertThat(body.containsKey("clientContext"), is(false));
        assertThat(body.containsKey("personalisation"), is(false));
    }

    @Test
    void shouldSendTheEmailNotificationBodyAsGiven() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.document-available")
                .withClientCorrelationId("client-correlation-id"), createObjectBuilder().build());
        final JsonObject emailNotification = createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("templateId", randomUUID().toString())
                .add("sendToAddress", "police@example.com")
                .add("materialUrl", "http://localhost:8080/material")
                .add("personalisation", createObjectBuilder().add("URN", "URN123"))
                .add("clientContext", "RESULTS_" + randomUUID())
                .build();

        notifyGatewaySendEmailService.sendEmailNotification(envelope, emailNotification);

        verify(notifyGatewaySenderClient).sendMessage(messageCaptor.capture());
        final ServiceBusMessage message = messageCaptor.getValue();
        assertTransport(message, "client-correlation-id");
        assertThat(bodyOf(message), is(emailNotification));
    }

    @Test
    void shouldTellAResultsNotificationApartAndReadItsMaterialId() {
        final UUID materialId = randomUUID();
        final JsonObject resultsNotification = createObjectBuilder().add("clientContext", NotifyGatewaySendEmailService.clientContextFor(materialId)).build();

        assertThat(NotifyGatewaySendEmailService.isResultsNotification(resultsNotification), is(true));
        assertThat(NotifyGatewaySendEmailService.materialIdFrom(resultsNotification), is(materialId));
        assertThat(NotifyGatewaySendEmailService.isResultsNotification(createObjectBuilder().add("clientContext", "STAGINGDVLA_" + materialId).build()), is(false));
        assertThat(NotifyGatewaySendEmailService.isResultsNotification(createObjectBuilder().build()), is(false));
    }

    private static void assertTransport(final ServiceBusMessage message, final String correlationId) {
        assertThat(message.getSubject(), is(SEND_EMAIL_NOTIFICATION));
        assertThat(message.getReplyTo(), is(REPLY_QUEUE));
        assertThat(message.getApplicationProperties().get("correlationId"), is(correlationId));
    }

    private static JsonObject bodyOf(final ServiceBusMessage message) {
        return createReader(new StringReader(message.getBody().toString())).readObject();
    }
}
