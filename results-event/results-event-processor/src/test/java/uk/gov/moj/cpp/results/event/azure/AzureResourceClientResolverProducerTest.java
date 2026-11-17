package uk.gov.moj.cpp.results.event.azure;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AzureResourceClientResolverProducerTest {

    @Mock
    private AzureEmulatorConfiguration configuration;

    @InjectMocks
    private AzureResourceClientResolverProducer producer;

    @Test
    void shouldProduceConnectionStringResolverWhenLocalEmulatorEnabled() {
        when(configuration.isLocalEmulatorEnabled()).thenReturn(true);

        assertThat(producer.azureResourceResolver(), instanceOf(ConnectionStringAzureResourceResolver.class));
    }

    @Test
    void shouldProduceManagedIdentityResolverWhenLocalEmulatorNotEnabled() {
        when(configuration.isLocalEmulatorEnabled()).thenReturn(false);

        assertThat(producer.azureResourceResolver(), instanceOf(ManagedIdentityAzureResourceResolver.class));
    }
}
