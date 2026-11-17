package uk.gov.moj.cpp.results.event.notifygateway;

import uk.gov.justice.services.common.configuration.Value;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * JNDI-backed configuration of results' {@code cp-gov-uk-notify-gateway} Azure Service Bus
 * integration: the email of a document results stores in its blob container (the file uri path)
 * is sent to the gateway, and its outcome comes back on results' own result queue. The
 * local-emulator-vs-real-Azure switch is {@link uk.gov.moj.cpp.results.event.azure.AzureEmulatorConfiguration}
 * ({@code azure.emulator-enabled}); this class carries the values of this integration only: the
 * emulator's connection string (a different value from Azurite's, hence its own key), the real
 * namespace, and the queue names.
 *
 * <p>The queue names are supplied fully resolved: any environment prefix is expanded at deploy
 * time, this class does no templating of its own.
 */
@SuppressWarnings("java:S6813")
@ApplicationScoped
public class NotifyGatewayServiceBusConfiguration {

    @Inject
    @Value(key = "notifygateway.servicebus.connection-string", defaultValue = "")
    private String connectionString;

    @Inject
    @Value(key = "notifygateway.servicebus.fully-qualified-namespace", defaultValue = "")
    private String fullyQualifiedNamespace;

    @Inject
    @Value(key = "notifygateway.servicebus.send-queue-name", defaultValue = "")
    private String sendQueueName;

    @Inject
    @Value(key = "notifygateway.servicebus.reply-queue-name", defaultValue = "")
    private String replyQueueName;

    /** Only meaningful when {@code AzureEmulatorConfiguration.isLocalEmulatorEnabled()} is {@code true}. */
    public String getConnectionString() {
        return connectionString;
    }

    /** The Service Bus namespace host. Only used when {@code AzureEmulatorConfiguration.isLocalEmulatorEnabled()} is {@code false}. */
    public String getFullyQualifiedNamespace() {
        return fullyQualifiedNamespace;
    }

    /** The gateway's shared command queue, {@code ng-send-email}, resolved. */
    public String getSendQueueName() {
        return sendQueueName;
    }

    /** Results' own result queue, resolved (locally {@code results-notification-result}). Set as the outbound message's {@code ReplyTo}, so the gateway replies on it. */
    public String getReplyQueueName() {
        return replyQueueName;
    }
}
