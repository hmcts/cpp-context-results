package uk.gov.moj.cpp.results.domain.transformation.util;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.slf4j.Logger;

public class ResultsAddHearingTransformationEventHelperTest {

    private static final Logger LOGGER = getLogger(ResultsAddHearingTransformationEventHelperTest.class);

    private final ResultsAddHearingTransformationEventHelper resultsAddHearingTransformationEventHelper = new ResultsAddHearingTransformationEventHelper();

    @Test
    public void buildTransformedPayloadForResults() {

        final String EVENT_RESULTS_ADD_HEARING = "results.hearing-results-added";

        final JsonEnvelope event = buildEnvelope(EVENT_RESULTS_ADD_HEARING, "results.hearing-results-added-2.json");

        final JsonEnvelope transformedEvent = resultsAddHearingTransformationEventHelper.buildTransformedPayloadForResults(event, EVENT_RESULTS_ADD_HEARING);

        final JsonObject jsonObject = transformedEvent.payloadAsJsonObject();

        final JsonObject courtClerk = jsonObject.getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("judicialResults").getJsonObject(0).getJsonObject("courtClerk");

        assertThat(courtClerk.getString("userId"), CoreMatchers.is("31ec3a16-8721-498c-8da5-f099390ee254"));
        assertThat(courtClerk.getString("firstName"), CoreMatchers.is("Erica"));
        assertThat(courtClerk.getString("lastName"), CoreMatchers.is("Wilson"));
    }

    private JsonEnvelope buildEnvelope(final String eventName, final String payloadFileName) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final InputStream stream = loader.getResourceAsStream(payloadFileName);
             final JsonReader jsonReader = Json.createReader(stream)) {
            final JsonObject payload = jsonReader.readObject();
            return envelopeFrom(metadataBuilder().withId(randomUUID()).withName(eventName), payload);
        } catch (final IOException e) {
            LOGGER.warn("Error in reading payload {}", payloadFileName, e);
        }
        return null;
    }
}