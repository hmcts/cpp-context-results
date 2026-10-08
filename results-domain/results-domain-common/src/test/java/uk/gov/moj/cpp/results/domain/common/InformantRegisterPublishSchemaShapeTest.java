package uk.gov.moj.cpp.results.domain.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.Test;

/**
 * Locks the two determinism-critical field types on the informant-register publish contracts.
 *
 * <p>The requestId the consuming service dedupes on is a hash of the raw {@code hearingDay} and
 * {@code sharedTime} strings. Giving either a {@code format: date-time} or a {@code datePattern}
 * $ref would make the POJO generator emit {@code ZonedDateTime}/{@code LocalDate}, and
 * {@code ZonedDateTime.toString()} is not guaranteed to reproduce the wire string it was parsed
 * from - so the command handler would start minting a different id for the same share than the
 * publisher does, and dedupe would fail silently in production with no test failing.
 *
 * <p>That is a plausible tidy-up for someone who notices the untyped strings and "fixes" them, so it
 * is asserted rather than left to a code comment.
 */
public class InformantRegisterPublishSchemaShapeTest {

    private static final String COMMAND_SCHEMA = "json/schema/results.command.request-informant-register-publish.json";
    private static final String EVENT_SCHEMA = "json/schema/results.events.informant-register-publish-requested.json";

    @Test
    public void commandSchema_should_keepTheShareTimestampsAsBareStrings() {
        assertBareString(COMMAND_SCHEMA, "hearingDay");
        assertBareString(COMMAND_SCHEMA, "sharedTime");
    }

    @Test
    public void eventSchema_should_keepTheShareTimestampsAsBareStrings() {
        assertBareString(EVENT_SCHEMA, "hearingDay");
        assertBareString(EVENT_SCHEMA, "sharedTime");
    }

    /**
     * The command and the event must agree field for field: the handler copies one straight to the
     * other, so a field added to only one side would be dropped without trace.
     */
    @Test
    public void commandAndEventSchemas_should_declareTheSameFields() {
        assertThat(properties(EVENT_SCHEMA).keySet(), is(properties(COMMAND_SCHEMA).keySet()));
        assertThat(readSchema(EVENT_SCHEMA).getJsonArray("required"),
                is(readSchema(COMMAND_SCHEMA).getJsonArray("required")));
    }

    private void assertBareString(final String resource, final String field) {
        final JsonObject property = properties(resource).getJsonObject(field);

        assertThat(resource + " " + field + " type", property.getString("type"), is("string"));
        assertThat(resource + " " + field + " must declare no format", property.containsKey("format"), is(false));
        assertThat(resource + " " + field + " must not be a $ref", property.containsKey("$ref"), is(false));
    }

    private JsonObject properties(final String resource) {
        return readSchema(resource).getJsonObject("properties");
    }

    private JsonObject readSchema(final String resource) {
        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource);
        assertThat("schema " + resource + " is on the classpath", inputStream != null, is(true));

        try (JsonReader jsonReader = createReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return jsonReader.readObject();
        }
    }
}
