package uk.gov.moj.cpp.results.it.utils;

import static uk.gov.moj.cpp.results.it.utils.ContextNameProvider.CONTEXT_NAME;

import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class FeatureStubUtil {

    private static final String RESULTS_FILE_STORE_FEATURE = "resultsFileStore";
    private static final String RESULTS_FILE_STORE_DELETE_FEATURE = "resultsFileStoreDelete";

    private FeatureStubUtil() {
    }

    public static void setFeatureToggle(final String featureName, final boolean isToggleOn) {
        setFeatureToggles(ImmutableMap.of(featureName, isToggleOn));
    }

    /**
     * Stubs all the given toggles at once: each stub replaces the previous one, so toggles that must
     * be on together are set in one call.
     */
    public static void setFeatureToggles(final Map<String, Boolean> features) {
        FeatureStubber.clearCache(CONTEXT_NAME);
        FeatureStubber.stubFeaturesFor(CONTEXT_NAME, ImmutableMap.copyOf(features));
    }

    /**
     * Turns the blob store on with the deletion of a document's blobs once it is delivered.
     */
    public static void enableResultsFileStoreWithDelete() {
        setFeatureToggles(ImmutableMap.of(RESULTS_FILE_STORE_FEATURE, true, RESULTS_FILE_STORE_DELETE_FEATURE, true));
    }
}
