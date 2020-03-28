package uk.gov.moj.cpp.results.domain.transformation.helper;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Iterables;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonPathHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPathHelper.class);

    private JsonPathHelper() {
    }

    public static Optional<String> getFirstValueForJsonPath(final String eventPayload, final String path) {
        try {
            if (JsonPath.isPathDefinite(path)) {
                final String value = JsonPath.read(eventPayload, path);
                if (null != value) {
                    return Optional.of(value);
                }
            } else {
                final List<String> values = JsonPath.read(eventPayload, path);
                if (CollectionUtils.isNotEmpty(values)) {
                    return Optional.of(values.get(0));
                }
            }
        } catch (final PathNotFoundException e) {
            LOGGER.error("Path not found for '{}' in payload", path, e);
            return Optional.empty();
        }

        return Optional.empty();
    }

    public static Optional<String> getLastValueForJsonPath(final String eventPayload, final String path) {
        try {
            if (JsonPath.isPathDefinite(path)) {
                final String value = JsonPath.read(eventPayload, path);
                if (null != value) {
                    return Optional.of(value);
                }
            } else {
                final List<String> values = JsonPath.read(eventPayload, path);
                if (CollectionUtils.isNotEmpty(values)) {
                    return Optional.of(Iterables.getLast(values));
                }
            }
        } catch (final PathNotFoundException e) {
            LOGGER.error("Path not found for '{}' in payload", path, e);
            return Optional.empty();
        }

        return Optional.empty();
    }
}
