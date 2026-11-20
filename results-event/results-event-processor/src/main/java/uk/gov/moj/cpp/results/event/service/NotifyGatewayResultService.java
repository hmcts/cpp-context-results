package uk.gov.moj.cpp.results.event.service;


import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.moj.cpp.results.event.service.NotifyGatewaySendEmailService.isResultsNotification;
import static uk.gov.moj.cpp.results.event.service.NotifyGatewaySendEmailService.materialIdFrom;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.domains.EmailDeliveryStatus;
import uk.gov.moj.cpp.results.event.service.exception.MalformedResultMessageException;

import java.io.StringReader;
import java.util.UUID;


import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonReader;


import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records the outcome {@code cp-gov-uk-notify-gateway} replies with on results' own result queue
 * (the transport half is {@code NotifyGatewayResultConsumer}): parses a {@code notification-sent},
 * {@code notification-failed} or {@code notification-validation-failed} message and records the
 * email status of its document via {@link DocumentDeliveryStatusService}, as
 * {@code DocumentDeliveryOutcomeEventProcessor} does for notificationnotify's public events - the
 * same client context match ({@link NotifyGatewaySendEmailService#materialIdFrom}), a different
 * transport. Deliberately not the {@code @Singleton @Startup} EJB itself, so this class stays a
 * plain, container-free unit under test.
 */
public class NotifyGatewayResultService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyGatewayResultService.class);

    private static final String NOTIFICATION_SENT_SUBJECT = "notification-sent";
    private static final String NOTIFICATION_FAILED_SUBJECT = "notification-failed";
    private static final String NOTIFICATION_VALIDATION_FAILED_SUBJECT = "notification-validation-failed";

    private static final String REASON = "reason";
    private static final String DETAILS = "details";
    private static final String ORIGINAL_EVENT_PAYLOAD = "originalEventPayload";

    @Inject
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    public void handle(final ServiceBusReceivedMessage message) {
        final String subject = message.getSubject();
        final JsonObject payload = parseJson(message.getBody().toString(), "notify-gateway result body");

        // an unrecognised subject is rejected as malformed
        final EmailDeliveryStatus status = statusFor(subject);

        // a rejected command was never accepted: its client context is in the command it echoes back
        final JsonObject correlationPayload = status == EmailDeliveryStatus.VALIDATION_FAILED
                ? parseJson(payload.getString(ORIGINAL_EVENT_PAYLOAD, ""), "notification-validation-failed originalEventPayload")
                : payload;

        if (!isResultsNotification(correlationPayload)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("ignoring {} on the notify-gateway result queue with no results clientContext", subject);
            }
            return;
        }
        // metadata requires a name; the result's subject names it, and the command sent renames it
        final Metadata originatingMetadata = metadataBuilder()
                .withId(randomUUID())
                .withName(subject)
                .build();
        final UUID materialId = materialIdFrom(correlationPayload);
        if (status == EmailDeliveryStatus.VALIDATION_FAILED && LOGGER.isWarnEnabled()) {
            // the gateway rejected the command itself: say why, but not what it carried (the recipient)
            LOGGER.warn("notify gateway rejected the email of material {}: {} - {}", materialId,
                    payload.getString(REASON, null), payload.getString(DETAILS, null));
        }
        documentDeliveryStatusService.recordEmailStatus(originatingMetadata, materialId, status);

    }


    private static EmailDeliveryStatus statusFor(final String subject) {
        if (NOTIFICATION_SENT_SUBJECT.equals(subject)) {
            return EmailDeliveryStatus.SUCCESS;
        }
        if (NOTIFICATION_FAILED_SUBJECT.equals(subject)) {
            return EmailDeliveryStatus.FAILED;
        }
        if (NOTIFICATION_VALIDATION_FAILED_SUBJECT.equals(subject)) {
            return EmailDeliveryStatus.VALIDATION_FAILED;
        }
        throw new MalformedResultMessageException("unrecognised notify-gateway result subject: " + subject);
    }

    private static JsonObject parseJson(final String json, final String errorContext) {
        try (JsonReader reader = createReader(new StringReader(json))) {
            return reader.readObject();
        } catch (final RuntimeException e) {
            throw new MalformedResultMessageException("unparseable " + errorContext + ": " + e.getMessage());
        }
    }

}
