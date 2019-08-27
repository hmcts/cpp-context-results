package uk.gov.moj.cpp.results.domain.transformation;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class ResultsNewTransformerTest {

    private static final Logger LOGGER = getLogger(ResultsNewTransformerTest.class);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private ResultsNewTransformer resultsNewTransformer = new ResultsNewTransformer();

    private Enveloper enveloper = createEnveloper();

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        resultsNewTransformer.setEnveloper(enveloper);
    }

    @Test
    public void shouldSetActionFor() {
        JsonEnvelope event = mock(JsonEnvelope.class);
        Metadata metadata = mock(Metadata.class);

        when(event.metadata()).thenReturn(metadata);
        when(event.metadata().name()).thenReturn("results.hearing-results-added");

        assertThat(resultsNewTransformer.actionFor(event), CoreMatchers.is(TRANSFORM));
    }
    @Test
    public void shouldCreateInstanceOfEventTransformation() {
        assertThat(resultsNewTransformer, is(instanceOf(EventTransformation.class)));
    }

    @Test
    public void shouldSetNoActionForEventsThatDoNotMatch() {
        final UUID caseId = randomUUID();
        final JsonEnvelope event = buildEnvelope("wrong.results.hearing-results-added", "results.hearing-results-added.json");

        assertThat(resultsNewTransformer.actionFor(event), is(NO_ACTION));
    }

    @Test
    public void testTransformFrom24To25() {
        final JsonEnvelope event = buildEnvelope("results.hearing-results-added", "results.hearing-results-added.json");
        final JsonObject expectedResult  = event.payloadAsJsonObject().getJsonObject("hearing");

        final Stream<JsonEnvelope> actualResult = resultsNewTransformer.apply(event);
        final List<JsonEnvelope> transformedEvents = actualResult.collect(toList());

        assertThat(transformedEvents, hasSize(1));

        assertThat(transformedEvents.get(0).payloadAsJsonObject().getJsonObject("hearing").get("id"), is(expectedResult.get("id")));
        //SharedresultLine is now judicialResults
        assertThat(transformedEvents.get(0).payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("judicialResults"), hasSize(1));
        //PNC_ID moved to defendants from child person defendant
        assertThat(transformedEvents.get(0).payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonString("pncId"), is(expectedResult.getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonObject("personDefendant").getJsonString("pncId")));
        //Ethnicity Object
        assertThat(transformedEvents.get(0).payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonObject("personDefendant").getJsonObject("personDetails").getJsonObject("ethnicity").getJsonString("observedEthnicityId"), is(expectedResult.getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonObject("personDefendant").getJsonString("observedEthnicityId")));

        assertThat(transformedEvents.get(0).payloadAsJsonObject().getJsonObject("hearing").get("id"), is(expectedResult.get("id")));
        assertThat(transformedEvents.get(0).metadata().name(), is("results.hearing-results-added"));
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
