package uk.gov.moj.cpp.results.test.matchers;

import uk.gov.justice.services.common.converter.exception.ConverterException;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.io.StringReader;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class MapJsonObjectToTypeMatcher<T> extends BaseMatcher<JsonObject> {
    private Class<T> clz;
    private final Matcher<T> matcher;

    public MapJsonObjectToTypeMatcher(Class<T> clz, Matcher<T> matcher) {
        this.clz = clz;
        this.matcher = matcher;
    }

    @Override
    public boolean matches(Object item) {
        final JsonObject jsonObject = (JsonObject) item;
        final T subject = convert(clz, jsonObject);
        return this.matcher.matches(subject);
    }

    @Override
    public void describeTo(Description description) {
        this.matcher.describeTo(description);
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        final JsonObject jsonObject = (JsonObject) item;
        final T subject = convert(clz, jsonObject);
        this.matcher.describeMismatch(subject, description);
    }

    public static <T> T convert(Class<T> clazz, String source) {
        try (JsonReader jr = JsonObjects.createReader(new StringReader(source))) {
            return convert(clazz, jr.readObject());
        }
     }

    public static <T> T  convert(Class<T> clazz, JsonObject source) {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        try {
            final T object = mapper.readValue(mapper.writeValueAsString(source), clazz);
            if (object == null) {
                throw new ConverterException(String.format("Failed to convert %s to Object", source));
            } else {
                return object;
            }
        } catch (IOException var4) {
            throw new IllegalArgumentException(String.format("Error while converting %s to JsonObject", source), var4);
        }
    }

}