package uk.gov.moj.cpp.results.event.healthcheck;

import static java.lang.String.format;
import static uk.gov.justice.services.healthcheck.api.HealthcheckResult.failure;
import static uk.gov.justice.services.healthcheck.api.HealthcheckResult.success;
import static uk.gov.moj.cpp.results.event.processor.HearingResultedEventProcessor.PROBATION_RESULTS_DISTRIBUTION_SERVICE_FEATURE;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.healthcheck.api.Healthcheck;
import uk.gov.justice.services.healthcheck.api.HealthcheckResult;

import javax.inject.Inject;

/**
 * Fails when the ProbationResultsDistributionService feature is enabled for this environment but no
 * Service Bus queue is configured for it. Same reasoning as
 * {@code InformantRegisterQueueConfigurationHealthcheck}: in that combination every resulted regular
 * hearing records a durable {@code results.events.probation-distribution-publish-requested} event
 * that {@code ProbationDistributionQueuePublisher} consumes and skips with a WARN, so nothing is
 * dead-lettered and nothing reaches probation.
 */
public class ProbationDistributionQueueConfigurationHealthcheck implements Healthcheck {

    public static final String PROBATION_DISTRIBUTION_QUEUE_CONFIGURATION_HEALTHCHECK_NAME =
            "probation-distribution-queue-configuration-healthcheck";

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Inject
    @Value(key = "probationDistributionQueueNamespace", defaultValue = "")
    private String probationDistributionQueueNamespace;

    @Inject
    @Value(key = "probationDistributionQueueName", defaultValue = "")
    private String probationDistributionQueueName;

    @Override
    public String getHealthcheckName() {
        return PROBATION_DISTRIBUTION_QUEUE_CONFIGURATION_HEALTHCHECK_NAME;
    }

    @Override
    public String healthcheckDescription() {
        return "Checks that a probation distribution Service Bus queue is configured whenever the "
                + PROBATION_RESULTS_DISTRIBUTION_SERVICE_FEATURE + " feature is enabled";
    }

    @Override
    public HealthcheckResult runHealthcheck() {
        if (!featureControlGuard.isFeatureEnabled(PROBATION_RESULTS_DISTRIBUTION_SERVICE_FEATURE)) {
            // The legacy function app owns probation distribution in this environment, so an
            // unconfigured queue is the expected state rather than a fault.
            return success();
        }

        if (probationDistributionQueueNamespace.isBlank() || probationDistributionQueueName.isBlank()) {
            return failure(format(
                    "Feature %s is enabled but the probation distribution queue is not configured "
                            + "(probationDistributionQueueNamespace '%s', probationDistributionQueueName '%s'). "
                            + "Every resulted regular hearing will record a publish request that is never published.",
                    PROBATION_RESULTS_DISTRIBUTION_SERVICE_FEATURE, probationDistributionQueueNamespace, probationDistributionQueueName));
        }

        return success();
    }
}
