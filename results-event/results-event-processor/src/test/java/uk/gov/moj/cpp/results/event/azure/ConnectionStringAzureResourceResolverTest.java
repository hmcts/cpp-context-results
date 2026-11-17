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

class ConnectionStringAzureResourceResolverTest {

    // placeholder emulator connection string (not a real credential) - same shape testcontainers-azure's
    // Service Bus emulator hands back at runtime
    private static final String SERVICE_BUS_EMULATOR_CONNECTION_STRING =
            "Endpoint=sb://localhost;SharedAccessKeyName=RootManageSharedAccessKey;"
                    + "SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";

    private final ConnectionStringAzureResourceResolver resolver = new ConnectionStringAzureResourceResolver();

    @Test
    void shouldBuildServiceBusSenderClientForTheConfiguredConnectionStringAndQueue() {
        final NotifyGatewayServiceBusConfiguration configuration = new NotifyGatewayServiceBusConfiguration();
        setField(configuration, "connectionString", SERVICE_BUS_EMULATOR_CONNECTION_STRING);
        setField(configuration, "sendQueueName", "steccm01.ng-send-email");

        final ServiceBusSenderClient senderClient = resolver.serviceBusSenderClient(configuration);

        assertThat(senderClient.getEntityPath(), is("steccm01.ng-send-email"));
    }

    @Test
    void shouldBuildServiceBusProcessorClientForTheConfiguredConnectionStringAndReplyQueue() {
        final NotifyGatewayServiceBusConfiguration configuration = new NotifyGatewayServiceBusConfiguration();
        setField(configuration, "connectionString", SERVICE_BUS_EMULATOR_CONNECTION_STRING);
        setField(configuration, "replyQueueName", "results-notification-result");

        final ServiceBusProcessorClient processorClient = resolver.serviceBusProcessorClient(
                configuration, (final ServiceBusReceivedMessageContext context) -> { }, (final ServiceBusErrorContext context) -> { });

        assertThat(processorClient.getQueueName(), is("results-notification-result"));
    }
}
