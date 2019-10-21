package uk.gov.moj.cpp.results.domain.transformation.ctl;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.moj.cpp.results.domain.transformation.ctl.ResultsEventStreamTransform.HEARING_RESULTS_ADDED;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.moj.cpp.coredomain.transform.transforms.BailStatusEnum2ObjectTransformer;

import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(MockitoJUnitRunner.class)
public class ResultsEventStreamTransformTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsEventStreamTransformTest.class);

    private ResultsEventStreamTransform target = new ResultsEventStreamTransform();

    private Enveloper enveloper = createEnveloper();

    @Before
    public void setup() {
        target.setEnveloper(enveloper);
    }

    @Test
    public void shouldCreateInstanceOfEventTransformation() {
        assertThat(target, is(instanceOf(EventTransformation.class)));
    }

    @Test
    public void shouldSetActionToNoActionForTheEventsThatDoesNotMatch() {
        final JsonEnvelope event = buildEnvelope("hearing.events.other");
        assertThat(target.actionFor(event), is(NO_ACTION));
    }

    @Test
    public void shouldTransformHearingResultsAddedEvent() {

        final JsonEnvelope inputEnvelope = loadTestFile(HEARING_RESULTS_ADDED, HEARING_RESULTS_ADDED);

        final JsonEnvelope result = target.apply(inputEnvelope).findFirst().get();

        JsonArray defendants =  result.payloadAsJsonObject()
                .getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants");

        JsonObject bailStatusOut0 = defendants.getJsonObject(0).getJsonObject("personDefendant").getJsonObject("bailStatus");
        System.out.println("bailStatusOut0: " + bailStatusOut0);
        JsonObject bailStatusOut1 = defendants.getJsonObject(1).getJsonObject("personDefendant").getJsonObject("bailStatus");
        System.out.println("bailStatusOut1: " + bailStatusOut1);

        assertEquals("U", bailStatusOut0.getString("code"));
        assertEquals("C", bailStatusOut1.getString("code"));

    }

    private JsonEnvelope buildEnvelope(final String eventName, final JsonObject jsonPayload) {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName(eventName), jsonPayload);
    }

    private JsonEnvelope buildEnvelope(final String eventName) {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(eventName),
                createObjectBuilder().add("field", "value").build());
    }

    private JsonEnvelope loadTestFile(String eventName, String resourceFileName) {
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceFileName + ".json");
            final JsonReader jsonReader = Json.createReader(is);
            final JsonObject payload = jsonReader.readObject();
            return envelopeFrom(metadataBuilder().withId(randomUUID()).withName(eventName), payload);

        } catch (Exception ex) {
            throw new RuntimeException("failed to load test file " + resourceFileName, ex);
        }
    }

}