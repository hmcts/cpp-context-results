package uk.gov.moj.cpp.results.event.notifygateway;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.results.event.azure.AzureResourceResolver;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotifyGatewaySenderClientProducerTest {

    @Mock
    private AzureResourceResolver azureResourceResolver;

    @Mock
    private NotifyGatewayServiceBusConfiguration configuration;

    @Mock
    private ServiceBusSenderClient senderClient;

    @InjectMocks
    private NotifyGatewaySenderClientProducer producer;

    @Test
    void shouldDelegateClientBuildingToTheSharedAzureResourceResolverAndCacheTheResult() {
        when(azureResourceResolver.serviceBusSenderClient(configuration)).thenReturn(senderClient);

        producer.initialise();

        assertThat(producer.notifyGatewaySenderClient(), sameInstance(senderClient));
    }

    @Test
    void shouldCloseTheCachedClientWhenDestroyed() {
        when(azureResourceResolver.serviceBusSenderClient(configuration)).thenReturn(senderClient);
        producer.initialise();

        producer.cleanup();

        verify(senderClient).close();
    }

    @Test
    void shouldNotFailOnDestroyWhenTheClientWasNeverBuilt() {
        // a fresh instance: @InjectMocks would inject the sender client mock into the client field
        final NotifyGatewaySenderClientProducer neverInitialised = new NotifyGatewaySenderClientProducer();

        assertDoesNotThrow(neverInitialised::cleanup);
    }
}
