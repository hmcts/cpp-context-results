package uk.gov.moj.cpp.results.event.notifygateway;

import uk.gov.moj.cpp.results.event.azure.AzureResourceResolver;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import com.azure.messaging.servicebus.ServiceBusSenderClient;

/**
 * CDI producer for the {@link ServiceBusSenderClient} that sends onto
 * {@code cp-gov-uk-notify-gateway}'s {@code ng-send-email} queue. Credential-scheme
 * selection lives entirely in the shared {@link AzureResourceResolver} (picked by
 * {@code AzureResourceClientResolverProducer}) — this class only builds the client once and caches
 * it, mirroring {@code AzureFileStoreBlobContainerClientProducer}.
 *
 * <p><strong>Scope note:</strong> as with {@code BlobContainerClient}, the Azure SDK's client
 * classes are not proxyable by Weld ({@code ServiceBusSenderClient} is {@code final}), so the
 * {@link Produces} method is {@link Dependent} rather than {@code @ApplicationScoped}; one shared
 * instance is built once in {@link #initialise()}.
 *
 * <p><strong>Lifecycle:</strong> the client holds a live AMQP connection, so it is closed in
 * {@link #cleanup()} when this bean is destroyed (undeploy/redeploy, shutdown). It is deliberately
 * not closed through a {@code @Disposes} method: the produced bean is {@code @Dependent}, so a
 * disposer would run as each injection point's owner is destroyed and close the shared client
 * under every other user.
 */
@SuppressWarnings("java:S6813")
@ApplicationScoped
public class NotifyGatewaySenderClientProducer {

    @Inject
    private AzureResourceResolver azureResourceResolver;

    @Inject
    private NotifyGatewayServiceBusConfiguration configuration;

    private ServiceBusSenderClient senderClient;

    @PostConstruct
    public void initialise() {
        senderClient = azureResourceResolver.serviceBusSenderClient(configuration);
    }

    @PreDestroy
    void cleanup() {
        if (senderClient != null) {
            senderClient.close();
        }
    }

    @Produces
    @Dependent
    public ServiceBusSenderClient notifyGatewaySenderClient() {
        return senderClient;
    }
}
