package uk.gov.moj.cpp.results.domain.transformation.transform;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.results.domain.transformation.util.SchemaVariableConstants.EVENT_RESULTS_ADD_HEARING;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.domain.transformation.util.SchemaVariableConstants;
import uk.gov.moj.cpp.results.domain.transformation.util.TransformUtil;

import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class MasterDefendantIdEventTransformer implements ResultsEventTransformer {

    private static final Map<String, Pattern> eventAndJsonPaths = Collections.unmodifiableMap(
            Stream.of(new String[][]{
                    { EVENT_RESULTS_ADD_HEARING, "hearing\\.prosecutionCases\\.\\d\\.defendants\\.\\d" }
            }).collect(Collectors.toMap(data -> data[0], data -> Pattern.compile(data[1]))));


    public static Map<String, Pattern> getEventAndJsonPaths() {
        return eventAndJsonPaths;
    }

    @Override
    public JsonObject transform(final Metadata eventMetadata, final JsonObject payload) {

        final Pattern jsonPath = eventAndJsonPaths.get(eventMetadata.name().toLowerCase());

        final BiFunction<JsonValue, Deque<String>, Object> filter = (jsonValue, path) -> {
            if (!path.isEmpty() && match(jsonPath, path) && (jsonValue instanceof JsonObject)) {
                return masterDefendantIdTransform((JsonObject) jsonValue);
            } else {
                return jsonValue;
            }
        };

        final JsonObjectBuilder transformedPayloadObjectBuilder = TransformUtil.cloneObjectWithPathFilter(payload, filter);
        return  transformedPayloadObjectBuilder.build();
    }

    private boolean match(final Pattern jsonPath, final Deque<String> path) {
        final String pathMerged = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(path.descendingIterator(), 0),
                        false)
                .collect(Collectors.joining("."));
        return jsonPath.matcher(String.join(".", pathMerged)).matches();
    }

    private Object masterDefendantIdTransform(final JsonObject replace) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        if (replace.containsKey(SchemaVariableConstants.MASTER_DEFENDANT_ID)) {
            return replace;
        }

        for (final Map.Entry<String, JsonValue> property : replace.entrySet()) {
            final String key = property.getKey();
            jsonObjectBuilder.add(key, property.getValue());
            if (key.equalsIgnoreCase(SchemaVariableConstants.ID)) {
                jsonObjectBuilder.add(SchemaVariableConstants.MASTER_DEFENDANT_ID, property.getValue());
            }
        }
        return jsonObjectBuilder;
    }
}
