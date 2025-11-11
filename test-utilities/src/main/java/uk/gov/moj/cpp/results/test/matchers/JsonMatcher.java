package uk.gov.moj.cpp.results.test.matchers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

/**
 * Custom JSON matcher that allows to compare JSON strings with the ability to exclude certain fields from comparison.
 * Excluded fields are only checked for presence in the actual JSON.
 */
public class JsonMatcher extends TypeSafeMatcher<String> {
    private final String expectedJson;
    private final JSONComparator comparator;

    public JsonMatcher(String expectedJson, JSONCompareMode compareMode, List<String> fieldsToCheckPresenceOnly) {
        this.expectedJson = expectedJson;
        this.comparator = JsonComparatorWithExclusions.withExclusionsAndPresenceCheck(compareMode, fieldsToCheckPresenceOnly);
    }

    @Override
    protected boolean matchesSafely(String actualJson) {
        final JSONCompareResult result = JSONCompare.compareJSON(expectedJson, actualJson, comparator);
        if (result.failed()) {
            throw new AssertionError(result.getMessage());
        }
        return result.passed();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("JSON to match: ").appendValue(expectedJson);
    }

    @Override
    protected void describeMismatchSafely(String item, Description mismatchDescription) {
        mismatchDescription.appendText("was ").appendValue(item);
    }

    /**
     * Creates a JsonMatcher instance for matching expected JSON with the provided jsonpath with or without wildcards
     * Ex:
     * "store.book[*].title" will ignore all title under book
     * "store.book[0].title" will only ignore first title under book.
     */
    public static JsonMatcher matchesJson(String expectedJson, List<String> fieldsToCheckPresenceOnly) {
        return new JsonMatcher(expectedJson, JSONCompareMode.STRICT, fieldsToCheckPresenceOnly);
    }

    /**
     * Custom JSON comparator that allows to exclude certain fields from comparison.
     */
    public static class JsonComparatorWithExclusions extends CustomComparator {
        private final List<Pattern> excludedPathPatterns = new ArrayList<>();

        public JsonComparatorWithExclusions(JSONCompareMode mode, List<String> fieldsToCheckPresenceOnly) {
            super(mode);
            for (String field : fieldsToCheckPresenceOnly) {
                if (!field.isBlank()) {
                    //Convert JSON path to regular expression to compare against json-path.
                    field = field.replace(".", "\\.")
                            .replace("[", "\\[")
                            .replace("]", "\\]")
                            .replace("*", ".*?");
                    excludedPathPatterns.add(Pattern.compile(field));
                }
            }
        }

        @Override
        public void compareValues(String jsonPath, Object expectedValue, Object actualValue, JSONCompareResult result) throws JSONException {
            if (jsonPathMatchesExcludedFields(jsonPath)) {
                if (actualValue == null) {
                    result.fail("field " + jsonPath + " is missing in actual");
                }
            } else {
                super.compareValues(jsonPath, expectedValue, actualValue, result);
            }
        }

        private boolean jsonPathMatchesExcludedFields(final String prefix) {
            for (Pattern pattern : excludedPathPatterns) {
                if (pattern.matcher(prefix).find()) {
                    return true;
                }
            }
            return false;
        }

        public static JsonComparatorWithExclusions withExclusionsAndPresenceCheck(JSONCompareMode mode, List<String> fieldsToCheckPresenceOnly) {
            return new JsonComparatorWithExclusions(mode, fieldsToCheckPresenceOnly);
        }
    }
}
