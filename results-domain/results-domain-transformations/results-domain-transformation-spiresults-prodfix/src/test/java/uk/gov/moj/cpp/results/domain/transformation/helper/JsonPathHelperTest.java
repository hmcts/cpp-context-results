package uk.gov.moj.cpp.results.domain.transformation.helper;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class JsonPathHelperTest {

    @Test
    public void testGetValueForJsonPathReturningSingleValue() throws Exception {

        final String payload = readFromClasspath("/sample-helper-payload.json");
        final Optional<String> firstValueForJsonPath = JsonPathHelper.getFirstValueForJsonPath(payload, "$.prompts[0].id");
        assertThat(firstValueForJsonPath.get(), is("d6caa3c4-ec9d-41ec-8f86-2c617ef0d5d9"));
    }

    @Test
    public void testGetFirstValueForJsonPathReturningList() throws Exception {
        final String payload = readFromClasspath("/sample-helper-payload.json");
        final Optional<String> firstValueForJsonPath = JsonPathHelper.getFirstValueForJsonPath(payload, "$..label");
        assertThat(firstValueForJsonPath.get(), is("value"));
    }

    @Test
    public void testGetLastValueForJsonPathReturningList() throws Exception {
        final String payload =  readFromClasspath("/sample-helper-payload.json");
        final Optional<String> firstValueForJsonPath = JsonPathHelper.getLastValueForJsonPath(payload, "$..label");
        assertThat(firstValueForJsonPath.get(), is("prompt label 2"));
    }

    @SuppressWarnings("SameParameterValue")
    private String readFromClasspath(final String pathOnClasspath) throws Exception {

        final URL resource = getClass().getResource(pathOnClasspath);
        assertThat(format("File' %s' not found on classpath", pathOnClasspath), resource, is(notNullValue()));

        final File jsonFile = new File(resource.toURI());
        return readFileToString(jsonFile, UTF_8);
    }
}