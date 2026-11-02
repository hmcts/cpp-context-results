package uk.gov.moj.cpp.results.it.utils;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.ArrayList;
import java.util.List;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueUtil.class);

    private static final String EVENT_SELECTOR_TEMPLATE = "CPPNAME IN ('%s')";

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String QUEUE_URI = "tcp://" + HOST + ":61616";

    private static final long RETRIEVE_TIMEOUT = 20000;

    private static Session session;

    private Topic topic;

    private final String topicName;

    private static Connection connection;

    public static final QueueUtil publicEvents = new QueueUtil("jms.topic.public.event");

    public static final QueueUtil privateEvents = new QueueUtil("jms.topic.results.event");

    private QueueUtil(final String topicName) {
        this.topicName = topicName;
        initialize(topicName);
    }

    private void initialize(final String topicName) {
        if (connection == null || !isAlive(connection)) {
            connect();
        }
        topic = new ActiveMQTopic(topicName);
    }

    /**
     * (Re)creates the single shared connection + session used by all QueueUtil instances. Artemis 2.54 is more
     * aggressive about closing idle client connections, and a long-running test class (e.g. StagingEnforcementIT
     * ~16m) previously left the shared session closed for the following classes ("AMQ219019: Session is closed" /
     * a 30s "AMQ219014: Timed out" hang on the half-open connection). We disable the broker idle timeout
     * (connectionTTL -1), keep the client failure-check keepalive at its default, and shorten the call timeout so
     * a genuinely dead connection fails fast enough to reconnect-and-retry within a test's polling window.
     */
    private static synchronized void connect() {
        closeQuietly();
        try {
            LOGGER.info("Artemis URI: {}", QUEUE_URI);
            final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(QUEUE_URI);
            factory.setConnectionTTL(-1);
            factory.setCallTimeout(15000);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (final JMSException e) {
            LOGGER.error("Fatal error initialising Artemis", e);
            throw new RuntimeException(e);
        }
    }

    private static synchronized void closeQuietly() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (final Exception ignored) {
            // best-effort cleanup before reconnect
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (final Exception ignored) {
            // best-effort cleanup before reconnect
        }
        session = null;
        connection = null;
    }

    public MessageConsumer createConsumer(final String eventSelector) {
        try {
            return session.createConsumer(topic, String.format(EVENT_SELECTOR_TEMPLATE, eventSelector));
        } catch (final JMSException e) {
            connect();
            try {
                return session.createConsumer(topic, String.format(EVENT_SELECTOR_TEMPLATE, eventSelector));
            } catch (final JMSException retryEx) {
                throw new RuntimeException(retryEx);
            }
        }
    }

    public MessageProducer createProducer() {
        try {
            return session.createProducer(topic);
        } catch (final JMSException e) {
            connect();
            try {
                return session.createProducer(topic);
            } catch (final JMSException retryEx) {
                throw new RuntimeException(retryEx);
            }
        }
    }

    public static JsonPath retrieveMessage(final MessageConsumer consumer) {
        return retrieveMessage(consumer, RETRIEVE_TIMEOUT);
    }

    public static List<JsonPath> retrieveMessages(final MessageConsumer consumer, final int expectedNumberOfMessages) {
        final List<JsonPath> messages = new ArrayList<>(expectedNumberOfMessages);
        for (int i = 0; i < expectedNumberOfMessages; i++) {
            messages.add(retrieveMessage(consumer));
        }
        return messages;
    }

    public static void sendMessage(final MessageProducer messageProducer, final String commandName, final JsonObject payload, final Metadata metadata) {

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);
        final String json = jsonEnvelope.toDebugStringPrettyPrint();

        try {
            final TextMessage message = session.createTextMessage();

            message.setText(json);
            message.setStringProperty("CPPNAME", commandName);

            messageProducer.send(message);
        } catch (final JMSException e) {
            throw new RuntimeException("Failed to send message. commandName: '" + commandName + "', json: " + json, e);
        }
    }

    public static JsonPath retrieveMessage(final MessageConsumer consumer, final long customTimeOutInMillis) {
        try {
            final TextMessage message = (TextMessage) consumer.receive(customTimeOutInMillis);
            if (message == null) {
                LOGGER.error("No message retrieved using consumer with selector {}", consumer.getMessageSelector());
                return null;
            }
            return new JsonPath(message.getText());
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeMessagesFromQueue(final MessageConsumer consumer) {
        JsonPath message;
        do {
            message = retrieveMessage(consumer, 10);
        } while (message != null);

    }

    public MessageProducer createPublicProducer() {
        try {
            return session.createProducer(topic);
        } catch (final JMSException e) {
            // Connection/session dropped (isAlive() can false-positive on an Artemis-closed connection);
            // recreate the shared connection + session and retry once.
            connect();
            try {
                return session.createProducer(topic);
            } catch (final JMSException retryEx) {
                throw new RuntimeException(retryEx);
            }
        }
    }

    public static boolean isAlive(Connection connection) {
        try {
            return (connection != null && connection.getMetaData() != null);
        } catch (JMSException ex) {
            LOGGER.error("Failed on isAlive", ex);
            return false;
        }
    }
}
