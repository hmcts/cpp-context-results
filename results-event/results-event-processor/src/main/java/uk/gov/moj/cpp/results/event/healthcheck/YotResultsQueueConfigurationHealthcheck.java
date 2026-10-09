package uk.gov.moj.cpp.results.event.healthcheck;

import static java.lang.String.format;
import static uk.gov.justice.services.healthcheck.api.HealthcheckResult.failure;
import static uk.gov.justice.services.healthcheck.api.HealthcheckResult.success;
import static uk.gov.moj.cpp.results.event.processor.HearingResultedEventProcessor.YOT_RESULTS_DISTRIBUTION_SERVICE_FEATURE;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.healthcheck.api.Healthcheck;
import uk.gov.justice.services.healthcheck.api.HealthcheckResult;

import javax.inject.Inject;

/**
 * Fails when the YotResultsDistributionService feature is enabled for this environment but no
 * Service Bus queue is configured for it. Same reasoning as
 * {@code InformantRegisterQueueConfigurationHealthcheck}: in that combination every resulted regular
 * hearing records a durable {@code results.events.yot-results-publish-requested} event that
 * {@code YotResultsQueuePublisher} consumes and silently skips, so nothing is dead-lettered and the
 * only trace is a WARN per hearing.
 */
public class YotResultsQueueConfigurationHealthcheck implements Healthcheck {

    public static final String YOT_RESULTS_QUEUE_CONFIGURATION_HEALTHCHECK_NAME =
            "yot-results-queue-configuration-healthcheck";

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Inject
    @Value(key = "yotResultsDistributionQueueNamespace", defaultValue = "")
    private String yotResultsQueueNamespace;

    @Inject
    @Value(key = "yotResultsDistributionQueueName", defaultValue = "")
    private String yotResultsQueueName;

    @Override
    public String getHealthcheckName() {
        return YOT_RESULTS_QUEUE_CONFIGURATION_HEALTHCHECK_NAME;
    }

    @Override
    public String healthcheckDescription() {
        return "Checks that a YOT results Service Bus queue is configured whenever the "
                + YOT_RESULTS_DISTRIBUTION_SERVICE_FEATURE + " feature is enabled";
    }

    @Override
    public HealthcheckResult runHealthcheck() {
        if (!featureControlGuard.isFeatureEnabled(YOT_RESULTS_DISTRIBUTION_SERVICE_FEATURE)) {
            // The legacy function app owns YOT distribution in this environment, so an unconfigured
            // queue is the expected state rather than a fault.
            return success();
        }

        if (yotResultsQueueNamespace.isBlank() || yotResultsQueueName.isBlank()) {
            return failure(format(
                    "Feature %s is enabled but the YOT results queue is not configured "
                            + "(yotResultsDistributionQueueNamespace '%s', yotResultsDistributionQueueName '%s'). "
                            + "Every resulted regular hearing will record a publish request that is never published.",
                    YOT_RESULTS_DISTRIBUTION_SERVICE_FEATURE, yotResultsQueueNamespace, yotResultsQueueName));
        }

        return success();
    }
}
