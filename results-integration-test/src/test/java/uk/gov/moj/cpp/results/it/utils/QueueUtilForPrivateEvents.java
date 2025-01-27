package uk.gov.moj.cpp.results.it.utils;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueUtilForPrivateEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueUtilForPrivateEvents.class);

    private static final String EVENT_SELECTOR_TEMPLATE = "CPPNAME IN ('%s')";

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String QUEUE_URI = "tcp://" + HOST + ":61616";

    public static final long RETRIEVE_TIMEOUT = 10000;

    private static Session session;

    private final Topic topic;

    public static final QueueUtilForPrivateEvents privateEvents = new QueueUtilForPrivateEvents("jms.topic.results.event");

    private QueueUtilForPrivateEvents(final String topicName) {
        try {
            LOGGER.info("Artemis URI: {}", QUEUE_URI);
            final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(QUEUE_URI);
            final Connection connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            topic = new ActiveMQTopic(topicName);
        } catch (final JMSException e) {
            LOGGER.error("Fatal error initialising Artemis", e);
            throw new RuntimeException(e);
        }
    }

    public MessageConsumer createConsumer(final String eventSelector) {
        try {
            return session.createConsumer(topic, String.format(EVENT_SELECTOR_TEMPLATE, eventSelector));
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
