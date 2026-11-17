package uk.gov.moj.cpp.results.event.azure;

import uk.gov.moj.cpp.results.event.notifygateway.NotifyGatewayServiceBusConfiguration;

import java.util.function.Consumer;

import javax.enterprise.inject.Vetoed;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

/**
 * Connection-string resolution for local dev/IT Azure emulators (the Service Bus emulator) —
 * these have no Azure AD support, so they need this instead of the
 * {@link ManagedIdentityAzureResourceResolver} path every real environment uses.
 *
 * <p>{@code @Vetoed}: only {@link AzureResourceClientResolverProducer} ever constructs this — see
 * {@link ManagedIdentityAzureResourceResolver} for why this matters even with an implicit no-arg
 * constructor.
 */
@Vetoed
public final class ConnectionStringAzureResourceResolver implements AzureResourceResolver {

    @Override
    public ServiceBusSenderClient serviceBusSenderClient(final NotifyGatewayServiceBusConfiguration configuration) {
        return new ServiceBusClientBuilder()
                .connectionString(configuration.getConnectionString())
                .sender()
                .queueName(configuration.getSendQueueName())
                .buildClient();
    }

    @Override
    public ServiceBusProcessorClient serviceBusProcessorClient(final NotifyGatewayServiceBusConfiguration configuration,
                                                                 final Consumer<ServiceBusReceivedMessageContext> processMessage,
                                                                 final Consumer<ServiceBusErrorContext> processError) {
        return new ServiceBusClientBuilder()
                .connectionString(configuration.getConnectionString())
                .processor()
                .queueName(configuration.getReplyQueueName())
                .disableAutoComplete()
                .processMessage(processMessage)
                .processError(processError)
                .buildProcessorClient();
    }
}
