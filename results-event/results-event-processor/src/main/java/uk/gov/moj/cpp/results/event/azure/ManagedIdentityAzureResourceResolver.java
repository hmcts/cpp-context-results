package uk.gov.moj.cpp.results.event.azure;

import uk.gov.moj.cpp.results.event.notifygateway.NotifyGatewayServiceBusConfiguration;

import java.util.function.Consumer;

import javax.enterprise.inject.Vetoed;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

/**
 * Workload Identity resolution for real Azure environments (AKS) — every real environment uses
 * this, never the emulator/local {@link ConnectionStringAzureResourceResolver} path.
 *
 * <p>{@code @Vetoed}: only {@link AzureResourceClientResolverProducer} ever constructs this.
 * Without it, its implicit public no-arg constructor makes Weld auto-discover it as a managed bean
 * anyway (despite no scope annotation), colliding with the producer's own
 * {@link AzureResourceResolver} output as an ambiguous dependency.
 */
@Vetoed
public final class ManagedIdentityAzureResourceResolver implements AzureResourceResolver {

    @Override
    public ServiceBusSenderClient serviceBusSenderClient(final NotifyGatewayServiceBusConfiguration configuration) {
        return new ServiceBusClientBuilder()
                .fullyQualifiedNamespace(configuration.getFullyQualifiedNamespace())
                .credential(new DefaultAzureCredentialBuilder().build())
                .sender()
                .queueName(configuration.getSendQueueName())
                .buildClient();
    }

    @Override
    public ServiceBusProcessorClient serviceBusProcessorClient(final NotifyGatewayServiceBusConfiguration configuration,
                                                                 final Consumer<ServiceBusReceivedMessageContext> processMessage,
                                                                 final Consumer<ServiceBusErrorContext> processError) {
        return new ServiceBusClientBuilder()
                .fullyQualifiedNamespace(configuration.getFullyQualifiedNamespace())
                .credential(new DefaultAzureCredentialBuilder().build())
                .processor()
                .queueName(configuration.getReplyQueueName())
                .disableAutoComplete()
                .processMessage(processMessage)
                .processError(processError)
                .buildProcessorClient();
    }
}
