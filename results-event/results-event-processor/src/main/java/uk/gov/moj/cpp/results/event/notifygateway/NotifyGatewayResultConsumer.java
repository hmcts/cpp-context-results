package uk.gov.moj.cpp.results.event.notifygateway;

import uk.gov.moj.cpp.results.event.azure.AzureResourceResolver;
import uk.gov.moj.cpp.results.event.service.exception.MalformedResultMessageException;
import uk.gov.moj.cpp.results.event.service.NotifyGatewayResultService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the start/stop lifecycle of the {@link ServiceBusProcessorClient} that consumes results' own
 * result queue ({@code NotifyGatewayServiceBusConfiguration#getReplyQueueName()}), on which
 * {@code cp-gov-uk-notify-gateway} replies with the outcome of the emails results sent it.
 *
 * <p>The actual parsing/recording logic lives in {@link NotifyGatewayResultService}, kept
 * deliberately separate so it stays unit-testable without a container - this class is only the
 * transport lifecycle and the complete/dead-letter decision per message.
 */
@Singleton
@Startup
public class NotifyGatewayResultConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyGatewayResultConsumer.class);

    @Inject
    private AzureResourceResolver azureResourceResolver;

    @Inject
    private NotifyGatewayServiceBusConfiguration configuration;

    @Inject
    private NotifyGatewayResultService notifyGatewayResultService;

    private ServiceBusProcessorClient client;

    @PostConstruct
    void init() {
        client = azureResourceResolver.serviceBusProcessorClient(configuration, this::processMessage, this::processError);
        client.start();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("NotifyGatewayResultConsumer started on {}", configuration.getReplyQueueName());
        }
    }

    @PreDestroy
    void cleanup() {
        if (client != null) {
            client.close();
        }
    }

    void processMessage(final ServiceBusReceivedMessageContext context) {
        try {
            notifyGatewayResultService.handle(context.getMessage());
            context.complete();
        } catch (final MalformedResultMessageException e) {
            LOGGER.warn("dead-lettering unprocessable notify-gateway result message", e);
            context.deadLetter();
        }
        // any other exception (e.g. a transient failure recording the outcome) propagates and is
        // left uncompleted - Service Bus redelivers it once its lock expires, up to the queue's
        // maxDeliveryCount, then dead-letters it. Recording an outcome again is harmless, as the
        // aggregate ignores a status that changes nothing, so redelivery and duplicates are safe
    }

    void processError(final ServiceBusErrorContext context) {
        LOGGER.error("notify-gateway result consumer error on entity {}", context.getEntityPath(), context.getException());
    }

}
