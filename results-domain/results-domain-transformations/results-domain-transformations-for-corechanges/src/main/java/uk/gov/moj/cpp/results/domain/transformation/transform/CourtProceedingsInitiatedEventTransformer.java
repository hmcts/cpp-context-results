package uk.gov.moj.cpp.results.domain.transformation.transform;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.results.domain.transformation.util.SchemaVariableConstants;
import uk.gov.moj.cpp.results.domain.transformation.util.TransformUtil;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.results.domain.transformation.util.SchemaVariableConstants.EVENT_RESULTS_ADD_HEARING;

public class CourtProceedingsInitiatedEventTransformer implements ResultsEventTransformer {

    private static final Map<String, Pattern> eventAndJsonPaths = Collections.unmodifiableMap(
            Stream.of(new String[][]{
                    { EVENT_RESULTS_ADD_HEARING, "hearing\\.prosecutionCases\\.\\d\\.defendants\\.\\d" }
            }).collect(Collectors.toMap(data -> data[0], data -> Pattern.compile(data[1]))));


    public static Map<String, Pattern> getEventAndJsonPaths() {
        return eventAndJsonPaths;
    }

    @Override
    public JsonObject transform(final Metadata eventMetadata, final JsonObject payload) {
        Pattern jsonPath = eventAndJsonPaths.get(eventMetadata.name().toLowerCase());
        final Optional<ZonedDateTime> createdAt = eventMetadata.createdAt();
        final ZonedDateTime courtProceedingsInitiated = createdAt.orElseThrow(() -> new TransformationException("Created At Metadata is null"));

        final BiFunction<JsonValue, Deque<String>, Object> filter = (jsonValue, path) -> {
            if (!path.isEmpty() && match(jsonPath, path) && (jsonValue instanceof JsonObject)) {
                return courtProceedingsInitiatedTransform((JsonObject) jsonValue, courtProceedingsInitiated);
            } else {
                return jsonValue;
            }
        };

        final JsonObjectBuilder transformedPayloadObjectBuilder = TransformUtil.cloneObjectWithPathFilter(payload, filter);
        JsonObject transformedObject =  transformedPayloadObjectBuilder.build();

        return transformedObject;
    }

    public boolean match(final Pattern jsonPath, final Deque<String> path) {
        String pathMerged = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(path.descendingIterator(), 0),
                        false)
                .collect(Collectors.joining("."));
        return jsonPath.matcher(String.join(".", pathMerged)).matches();
    }

    private Object courtProceedingsInitiatedTransform(final JsonObject replace, final ZonedDateTime courtProceedingsInitiated) {

        if (replace.containsKey(SchemaVariableConstants.COURT_PROCEEDINGS_INITIATED)) {
            return replace;
        }
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        replace.forEach(jsonObjectBuilder::add);
        jsonObjectBuilder.add(SchemaVariableConstants.COURT_PROCEEDINGS_INITIATED, ZonedDateTimes.toString(courtProceedingsInitiated));
        return jsonObjectBuilder;
    }
}
