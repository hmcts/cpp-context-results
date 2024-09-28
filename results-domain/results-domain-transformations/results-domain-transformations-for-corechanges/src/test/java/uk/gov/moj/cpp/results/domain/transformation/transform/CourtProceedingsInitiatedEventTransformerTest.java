package uk.gov.moj.cpp.results.domain.transformation.transform;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;

import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.Test;

public class CourtProceedingsInitiatedEventTransformerTest {


    @Test
    public void transform() {
        final JsonObject oldJsonObject = loadTestFile("court-proceedings-initiated/old/results.hearing-results-added.json");
        final JsonObject expectedJsonObject = loadTestFile("court-proceedings-initiated/new/results.hearing-results-added.json");

        final JsonObject resultJsonObject = new CourtProceedingsInitiatedEventTransformer()
                .transform(
                        DefaultJsonMetadata.metadataBuilder()
                                .withName("results.hearing-results-added")
                                .createdAt(ZonedDateTimes.fromString("2020-03-07T14:22:00.000Z"))
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
