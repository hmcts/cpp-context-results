package uk.gov.moj.cpp.results.event.healthcheck;

import static java.lang.String.format;
import static uk.gov.justice.services.healthcheck.api.HealthcheckResult.failure;
import static uk.gov.justice.services.healthcheck.api.HealthcheckResult.success;
import static uk.gov.moj.cpp.results.event.processor.HearingResultedEventProcessor.INFORMANT_REGISTER_SERVICE_FEATURE;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.healthcheck.api.Healthcheck;
import uk.gov.justice.services.healthcheck.api.HealthcheckResult;

import javax.inject.Inject;

/**
 * Fails when the InformantRegisterService feature is enabled for this environment but no Service Bus
 * queue is configured for it.
 *
 * <p>That combination is the one silent failure the publish-request chain cannot absorb. Every
 * resulted regular hearing records a durable
 * {@code results.events.informant-register-publish-requested} event, the processor picks it up, and
 * {@code InformantRegisterQueuePublisher} finds no sender and returns - so the event is consumed
 * successfully, nothing is dead-lettered, no register is ever distributed, and the only trace is a
 * WARN per hearing. Losing the register silently is precisely what this design set out to stop, so
 * the misconfiguration is surfaced here rather than left to be noticed downstream.
 *
 * <p>The publisher's inert branch stays as the runtime backstop: an unconfigured environment must
 * not dead-letter every resulted hearing. This healthcheck is what makes that branch loud instead of
 * quiet.
 *
 * <p>Deliberately not asserting reachability of the namespace or the AzureServiceBusDataSender
 * grant. Those fail visibly - the publish throws, the event is redelivered and dead-lettered, and
 * the DLQ is the record. Only the inert case needs a healthcheck, because only the inert case
 * leaves no record at all.
 */
public class InformantRegisterQueueConfigurationHealthcheck implements Healthcheck {

    public static final String INFORMANT_REGISTER_QUEUE_CONFIGURATION_HEALTHCHECK_NAME =
            "informant-register-queue-configuration-healthcheck";

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Inject
    @Value(key = "informantRegisterQueueNamespace", defaultValue = "")
    private String informantRegisterQueueNamespace;

    @Inject
    @Value(key = "informantRegisterQueueName", defaultValue = "")
    private String informantRegisterQueueName;

    @Override
    public String getHealthcheckName() {
        return INFORMANT_REGISTER_QUEUE_CONFIGURATION_HEALTHCHECK_NAME;
    }

    @Override
    public String healthcheckDescription() {
        return "Checks that an informant register Service Bus queue is configured whenever the "
                + INFORMANT_REGISTER_SERVICE_FEATURE + " feature is enabled";
    }

    @Override
    public HealthcheckResult runHealthcheck() {
        if (!featureControlGuard.isFeatureEnabled(INFORMANT_REGISTER_SERVICE_FEATURE)) {
            // The legacy function app owns distribution in this environment, so an unconfigured
            // queue is the expected state rather than a fault.
            return success();
        }

        if (informantRegisterQueueNamespace.isBlank() || informantRegisterQueueName.isBlank()) {
            return failure(format(
                    "Feature %s is enabled but the informant register queue is not configured "
                            + "(informantRegisterQueueNamespace '%s', informantRegisterQueueName '%s'). "
                            + "Every resulted regular hearing will record a publish request that is never published.",
                    INFORMANT_REGISTER_SERVICE_FEATURE, informantRegisterQueueNamespace, informantRegisterQueueName));
        }

        return success();
    }
}
