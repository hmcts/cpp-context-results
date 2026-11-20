package uk.gov.moj.cpp.results.event.azure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.moj.cpp.results.event.notifygateway.NotifyGatewayServiceBusConfiguration;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.Test;

class ManagedIdentityAzureResourceResolverTest {

    private final ManagedIdentityAzureResourceResolver resolver = new ManagedIdentityAzureResourceResolver();

    @Test
    void shouldBuildServiceBusSenderClientForTheConfiguredNamespaceAndQueue() {
        final NotifyGatewayServiceBusConfiguration configuration = new NotifyGatewayServiceBusConfiguration();
        setField(configuration, "fullyQualifiedNamespace", "sbsteccm01.servicebus.windows.net");
        setField(configuration, "sendQueueName", "steccm01.ng-send-email");

        final ServiceBusSenderClient senderClient = resolver.serviceBusSenderClient(configuration);

        assertThat(senderClient.getFullyQualifiedNamespace(), is("sbsteccm01.servicebus.windows.net"));
        assertThat(senderClient.getEntityPath(), is("steccm01.ng-send-email"));
    }

    @Test
    void shouldBuildServiceBusProcessorClientForTheConfiguredNamespaceAndReplyQueue() {
        final NotifyGatewayServiceBusConfiguration configuration = new NotifyGatewayServiceBusConfiguration();
        setField(configuration, "fullyQualifiedNamespace", "sbsteccm01.servicebus.windows.net");
        setField(configuration, "replyQueueName", "steccm01.results-notification-result");

        final ServiceBusProcessorClient processorClient = resolver.serviceBusProcessorClient(
                configuration, (final ServiceBusReceivedMessageContext context) -> { }, (final ServiceBusErrorContext context) -> { });

        // ServiceBusProcessorClient exposes no getFullyQualifiedNamespace() (unlike ServiceBusSenderClient
        // above) - getQueueName() is the only identifying accessor available to assert against
        assertThat(processorClient.getQueueName(), is("steccm01.results-notification-result"));
    }
}
