package uk.gov.moj.cpp.results.test;

import static java.nio.charset.Charset.defaultCharset;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;
import java.util.function.Consumer;

import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsNull;

public class TestUtilities {

    private TestUtilities() {
    }

    public static <T> T with(T object, Consumer<T> consumer) {
        consumer.accept(object);
        return object;
    }

    public static JsonObject stringToJsonObject(final String request) {
        try (JsonReader reader = createReader(new StringReader(request))) {
            return reader.readObject();
        }
    }

    public static String payloadAsString(final String path) {
        try {
            final InputStream inputStream = TestUtilities.class.getClassLoader().getResourceAsStream(path);
            assertThat(path, inputStream, IsNull.notNullValue());
            return IOUtils.toString(inputStream, defaultCharset());
        } catch (final IOException e) {
            throw new AssertionError("Failed to read payload from file:" + path, e);
        }
    }

    public static String payloadAsString(final String path, final Map<String, String> parameters) {
        try {
            final InputStream inputStream = TestUtilities.class.getClassLoader().getResourceAsStream(path);
            assertThat(path, inputStream, IsNull.notNullValue());
            String payload = IOUtils.toString(inputStream, defaultCharset());
            for (final Map.Entry<String, String> entry : parameters.entrySet()) {
                payload = payload.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
            return payload;
        } catch (final IOException e) {
            throw new AssertionError("Failed to read payload from file" +path,  e);
        }
    }
}