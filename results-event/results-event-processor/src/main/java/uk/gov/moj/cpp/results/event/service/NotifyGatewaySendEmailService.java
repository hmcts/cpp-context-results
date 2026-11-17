package uk.gov.moj.cpp.results.event.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.event.notifygateway.NotifyGatewayServiceBusConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends the email of a document results stores in its blob container (the file uri path) - the
 * NCES email and the police notification email - onto {@code cp-gov-uk-notify-gateway}'s
 * {@code ng-send-email} queue. The email of the file service path is still sent by
 * {@link NotificationNotifyService}'s notificationnotify command.
 *
 * <p>The gateway's inbound contract is a raw ASB message, not the JEE framework's own envelope
 * shape: a flat JSON body matching {@code command-send-email-notification.schema.json}, the ASB
 * {@code subject} set to the literal command name, {@code correlationId} in the application
 * properties, and the native ASB {@code ReplyTo} property naming results' own result queue - not
 * a body field. The gateway dead-letters a command without a {@code ReplyTo}.
 */
public class NotifyGatewaySendEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyGatewaySendEmailService.class);

    private static final String SEND_EMAIL_NOTIFICATION_SUBJECT = "send-email-notification";
    private static final String CORRELATION_ID = "correlationId";

    /**
     * Echoed back with the outcome of an email, by notificationnotify on notification-sent /
     * notification-failed and by the notify gateway on its result events. Results sets it, with
     * this prefix, on the email of a document it tracks the delivery of, so those outcomes can be
     * told apart from other contexts' and matched back to the material: the rest of it is the
     * material id.
     */
    public static final String CLIENT_CONTEXT = "clientContext";
    public static final String CLIENT_CONTEXT_PREFIX = "RESULTS_";

    @Inject
    private ServiceBusSenderClient notifyGatewaySenderClient;

    @Inject
    private NotifyGatewayServiceBusConfiguration configuration;

    public void sendEmailNotification(final JsonEnvelope event, final JsonObject emailNotification) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("sending email notification to the notify gateway for {} ", event.toObfuscatedDebugString());
        }

        final ServiceBusMessage message = new ServiceBusMessage(emailNotification.toString().getBytes(StandardCharsets.UTF_8));
        message.setSubject(SEND_EMAIL_NOTIFICATION_SUBJECT);
        message.setReplyTo(configuration.getReplyQueueName());
        message.getApplicationProperties().put(CORRELATION_ID, correlationIdOf(event));

        notifyGatewaySenderClient.sendMessage(message);
    }

    public void sendNcesEmail(final EmailNotification emailNotification, final JsonEnvelope envelope) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("notificationId", emailNotification.getNotificationId().toString())
                .add("templateId", emailNotification.getTemplateId().toString())
                .add("sendToAddress", emailNotification.getSendToAddress())
                .add("materialUrl", emailNotification.getMaterialUrl());

        emailNotification.getSubject().ifPresent(subject ->
                payload.add("personalisation", createObjectBuilder().add("subject", subject)));
        emailNotification.getMaterialId().ifPresent(materialId -> payload.add(CLIENT_CONTEXT, clientContextFor(materialId)));

        sendEmailNotification (envelope, payload.build());
    }

    // the envelope's client correlation id, else its own id: a value the request can be traced by,
    // rather than a fresh random one
    private String correlationIdOf(final JsonEnvelope event) {
        return event.metadata().clientCorrelationId().orElseGet(() -> event.metadata().id().toString());
    }

    public static String clientContextFor(final UUID materialId) {
        return CLIENT_CONTEXT_PREFIX + materialId;
    }

    public static boolean isResultsNotification(final JsonObject payload) {
        return payload.getString(CLIENT_CONTEXT, "").startsWith(CLIENT_CONTEXT_PREFIX);
    }

    public static UUID materialIdFrom(final JsonObject payload) {
        return UUID.fromString(payload.getString(CLIENT_CONTEXT).substring(CLIENT_CONTEXT_PREFIX.length()));
    }
}
