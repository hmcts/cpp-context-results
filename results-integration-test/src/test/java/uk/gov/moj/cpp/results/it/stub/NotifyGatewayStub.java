package uk.gov.moj.cpp.results.it.stub;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;

/**
 * Plays cp-gov-uk-notify-gateway, which is not deployed in the IT stack, on the Service Bus emulator
 * ({@code cpp-servicebus-emulator}, profile "servicebus", queues from cpp-developers-docker's
 * {@code servicebus-emulator/Config.json}): reads the {@code send-email-notification} command results
 * puts on {@code ng-send-email}, and replies on results' own result queue as the gateway does.
 */
public class NotifyGatewayStub {

    // the emulator's development connection string: SAS_KEY_VALUE is the fixed placeholder it accepts
    private static final String CONNECTION_STRING = "Endpoint="
            + Optional.ofNullable(System.getenv("SERVICEBUS_EMULATOR_ENDPOINT")).orElse("sb://localhost")
            + ";SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";
    private static final String SEND_QUEUE_NAME = "ng-send-email";
    public static final String RESULT_QUEUE_NAME = "results-notification-result";

    private static final String SEND_EMAIL_NOTIFICATION_SUBJECT = "send-email-notification";
    private static final String NOTIFICATION_SENT_SUBJECT = "notification-sent";
    private static final String CLIENT_CONTEXT = "clientContext";

    private static final ServiceBusReceiverClient RECEIVER_CLIENT = new ServiceBusClientBuilder()
            .connectionString(CONNECTION_STRING)
            .receiver()
            .receiveMode(ServiceBusReceiveMode.RECEIVE_AND_DELETE)
            .queueName(SEND_QUEUE_NAME)
            .buildClient();

    private static final ServiceBusSenderClient RESULT_SENDER_CLIENT = new ServiceBusClientBuilder()
            .connectionString(CONNECTION_STRING)
            .sender()
            .queueName(RESULT_QUEUE_NAME)
            .buildClient();

    // a receive deletes every message it takes off the queue, so the ones not asked for yet are kept
    // here for a later await: a test can send several emails before it looks for any
    private static final List<ServiceBusReceivedMessage> RECEIVED = new ArrayList<>();

    private NotifyGatewayStub() {
    }

    /**
     * Waits for the send-email command results sent to the gateway for the client context.
     */
    public static synchronized ServiceBusReceivedMessage awaitEmailSentToGateway(final String clientContext) {
        await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).until(() -> {
            RECEIVER_CLIENT.receiveMessages(10, ofSeconds(2)).forEach(RECEIVED::add);
            return findFor(clientContext).isPresent();
        });
        final ServiceBusReceivedMessage message = findFor(clientContext).orElseThrow();
        RECEIVED.remove(message);
        return message;
    }

    public static boolean isSendEmailNotification(final ServiceBusReceivedMessage message) {
        return SEND_EMAIL_NOTIFICATION_SUBJECT.equals(message.getSubject());
    }

    public static JsonObject bodyOf(final ServiceBusReceivedMessage message) {
        return createReader(new StringReader(message.getBody().toString())).readObject();
    }

    /**
     * Replies notification-sent for the email of the client context, as the gateway does once
     * GOV.UK Notify has sent it.
     */
    public static void publishNotificationSent(final String clientContext, final UUID notificationId) {
        final ServiceBusMessage message = new ServiceBusMessage(createObjectBuilder()
                .add("notificationId", notificationId.toString())
                .add("sentTime", Instant.now().toString())
                .add(CLIENT_CONTEXT, clientContext)
                .build()
                .toString());
        message.setSubject(NOTIFICATION_SENT_SUBJECT);
        RESULT_SENDER_CLIENT.sendMessage(message);
    }

    private static Optional<ServiceBusReceivedMessage> findFor(final String clientContext) {
        return RECEIVED.stream()
                .filter(message -> clientContext.equals(clientContextOf(message)))
                .findFirst();
    }

    private static String clientContextOf(final ServiceBusReceivedMessage message) {
        try {
            return bodyOf(message).getString(CLIENT_CONTEXT, null);
        } catch (final RuntimeException e) {
            // a body that is not json is not an email this test is looking for
            return null;
        }
    }
}
