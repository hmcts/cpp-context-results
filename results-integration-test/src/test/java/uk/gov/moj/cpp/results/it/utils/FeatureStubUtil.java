package uk.gov.moj.cpp.results.it.utils;

import static uk.gov.moj.cpp.results.it.utils.ContextNameProvider.CONTEXT_NAME;

import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;

import com.google.common.collect.ImmutableMap;

public class FeatureStubUtil {

    public static void setFeatureToggle(final String featureName, final boolean isToggleOn) {
        final ImmutableMap<String, Boolean> features = ImmutableMap.of(featureName, isToggleOn);
        FeatureStubber.clearCache(CONTEXT_NAME);
        FeatureStubber.stubFeaturesFor(CONTEXT_NAME, features);
    }
}
