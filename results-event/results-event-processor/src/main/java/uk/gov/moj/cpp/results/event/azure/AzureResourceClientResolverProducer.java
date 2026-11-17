package uk.gov.moj.cpp.results.event.azure;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Picks which {@link AzureResourceResolver} applies, based on
 * {@link AzureEmulatorConfiguration#isLocalEmulatorEnabled()} — scoped to the notify-gateway
 * Service Bus clients; the blob file-store path is out of scope and keeps its own
 * separate, untouched credential-selection logic. The two implementations are plain
 * constructor-wired objects, not CDI beans in their own right — if they were, CDI would see three
 * unqualified {@link AzureResourceResolver} beans (the two implementations plus this producer's
 * own output) and fail injection with an ambiguous-resolution error.
 */
@ApplicationScoped
public class AzureResourceClientResolverProducer {

    @Inject
    private AzureEmulatorConfiguration configuration;

    @Produces
    @ApplicationScoped
    public AzureResourceResolver azureResourceResolver() {
        return configuration.isLocalEmulatorEnabled()
                ? new ConnectionStringAzureResourceResolver()
                : new ManagedIdentityAzureResourceResolver();
    }
}
