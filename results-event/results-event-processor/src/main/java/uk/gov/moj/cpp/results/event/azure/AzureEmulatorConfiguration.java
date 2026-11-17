package uk.gov.moj.cpp.results.event.azure;

import uk.gov.justice.services.common.configuration.Value;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * The local/IT emulator-mode switch for the notify-gateway Service Bus clients, read
 * once by {@link AzureResourceClientResolverProducer}. Named after the mode, not any one emulator
 * product (Azurite is storage-only), so any future Azure resource needing the same switch can
 * reuse this flag rather than inventing its own.
 *
 * <p>Deliberately separate from the pre-existing {@code AzureFileStoreBlobConfiguration}'s own
 * {@code azure.filestore.azurite-enabled} key — the blob file-store path keeps its own,
 * independent credential-selection logic and is not wired to this class or
 * {@link AzureResourceResolver} at all.
 */
@ApplicationScoped
public class AzureEmulatorConfiguration {

    @Inject
    @Value(key = "azure.emulator-enabled", defaultValue = "false")
    private String localEmulatorEnabled;

    public boolean isLocalEmulatorEnabled() {
        return Boolean.parseBoolean(localEmulatorEnabled);
    }
}
