package uk.gov.moj.cpp.results.event.healthcheck;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.results.event.processor.HearingResultedEventProcessor.INFORMANT_REGISTER_SERVICE_FEATURE;

import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.healthcheck.api.HealthcheckResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InformantRegisterQueueConfigurationHealthcheckTest {

    private static final String NAMESPACE = "sbsteccm01.servicebus.windows.net";
    private static final String QUEUE_NAME = "steccm42.informantregister.requests";

    @Mock
    private FeatureControlGuard featureControlGuard;

    private InformantRegisterQueueConfigurationHealthcheck healthcheck;

    @BeforeEach
    public void setup() {
        healthcheck = new InformantRegisterQueueConfigurationHealthcheck();
        setField(healthcheck, "featureControlGuard", featureControlGuard);
    }

    private void givenConfiguration(final String namespace, final String queueName) {
        setField(healthcheck, "informantRegisterQueueNamespace", namespace);
        setField(healthcheck, "informantRegisterQueueName", queueName);
    }

    private void givenTheFeatureIs(final boolean enabled) {
        when(featureControlGuard.isFeatureEnabled(INFORMANT_REGISTER_SERVICE_FEATURE)).thenReturn(enabled);
    }

    /**
     * The combination this healthcheck exists for: the feature is on, so every resulted regular
     * hearing records a publish request, but the publisher is inert and none of them is ever
     * published. Nothing is dead-lettered and nothing fails, so without this the only symptom is a
     * WARN per hearing.
     */
    @Test
    public void runHealthcheck_withTheFeatureOnAndNoNamespace_should_fail() {
        givenTheFeatureIs(true);
        givenConfiguration("", QUEUE_NAME);

        final HealthcheckResult result = healthcheck.runHealthcheck();

        assertThat(result.isPassed(), is(false));
        assertThat(result.getErrorMessage().orElseThrow(), containsString(INFORMANT_REGISTER_SERVICE_FEATURE));
    }

    @Test
    public void runHealthcheck_withTheFeatureOnAndNoQueueName_should_fail() {
        givenTheFeatureIs(true);
        givenConfiguration(NAMESPACE, "");

        assertThat(healthcheck.runHealthcheck().isPassed(), is(false));
    }

    @Test
    public void runHealthcheck_withTheFeatureOnAndTheQueueConfigured_should_pass() {
        givenTheFeatureIs(true);
        givenConfiguration(NAMESPACE, QUEUE_NAME);

        assertThat(healthcheck.runHealthcheck().isPassed(), is(true));
    }

    /**
     * An environment still served by the legacy function app has no queue and no grant, and must not
     * report itself unhealthy for it. This is the default state of every environment until the flag
     * is explicitly enabled for its label.
     */
    @Test
    public void runHealthcheck_withTheFeatureOff_should_passEvenWithNoQueueConfigured() {
        givenTheFeatureIs(false);
        givenConfiguration("", "");

        assertThat(healthcheck.runHealthcheck().isPassed(), is(true));
    }
}
