package uk.gov.moj.cpp.results.domain.transformation.transform;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Spy;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(Parameterized.class)
public class MasterDefendantIdEventTransformerTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"results.hearing-results-added.json", "results.hearing-results-added"}
        });
    }

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private String file;
    private String eventName;

    public MasterDefendantIdEventTransformerTest(final String file, final String eventName) {
        this.file = file;
        this.eventName = eventName;
    }

    @Test
    public void transform() {
        JsonObject oldJsonObject = loadTestFile("master-defendant-id/old/" + file);
        JsonObject expectedJsonObject = loadTestFile("master-defendant-id/new/" + file);

        JsonObject resultJsonObject = new MasterDefendantIdEventTransformer()
                .transform(
                        DefaultJsonMetadata.metadataBuilder()
                                .withName(eventName)
                                .withId(randomUUID())
                                .build(),
                        oldJsonObject);

        assertThat(resultJsonObject.toString(), equalTo(expectedJsonObject.toString()));
    }

    private JsonObject loadTestFile(String resourceFileName) {
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceFileName);
            final JsonReader jsonReader = Json.createReader(is);
            return jsonReader.readObject();

        } catch (Exception ex) {
            throw new RuntimeException("failed to load test file " + resourceFileName, ex);
        }
    }
}