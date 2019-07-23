package uk.gov.moj.cpp.data.anonymization;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilderFrom;

import org.junit.Before;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventStoreDataAnonymizerTest {
    private static final String CASE_ID = "9b42e998-158a-4683-8073-8e9453fe6cc9";
    private static final String COURT_ROOM = "Lavender Hill Magistrates' Court";
    private EventStoreDataAnonymizer eventStoreDataAnonymizer;

    @Before
    public void initialize() throws IOException {
        eventStoreDataAnonymizer = new EventStoreDataAnonymizer();
    }

    @Test
    public void shouldCreateSendingSheetCompletedEventTransformation() throws IOException {
        eventStoreDataAnonymizer.setEnveloper(EnveloperFactory.createEnveloper());
        final JsonEnvelope event = buildSendingSheetCompletedEnvelope();
        final Stream<JsonEnvelope> jsonEnvelopeStream = eventStoreDataAnonymizer.apply(event);
        Optional<JsonEnvelope> optionalJsonEnvelope = jsonEnvelopeStream.findFirst();

        if (optionalJsonEnvelope.isPresent()) {
            final JsonEnvelope jsonEnvelope = optionalJsonEnvelope.get();
            final JsonObject hearingObject = jsonEnvelope.payloadAsJsonObject().getJsonObject("hearing");
            assertThat("name ", hearingObject.getJsonObject("courtCentre").getString("name"), is(COURT_ROOM));
            final JsonObject judiciaryObject = hearingObject.getJsonArray("judiciary").getJsonObject(0);
            assertThat(judiciaryObject.getString("firstName"), equalTo("XXXXX"));
            assertThat(judiciaryObject.getString("lastName"), equalTo("XXXXX"));
            final JsonObject prosecutionObject = hearingObject.getJsonArray("prosecutionCases").getJsonObject(0);
            final JsonObject defendantsObject = prosecutionObject.getJsonArray("defendants").getJsonObject(0);
            final JsonObject offencesObject = defendantsObject.getJsonArray("offences").getJsonObject(0);
            final JsonObject judicialResultsObject = offencesObject.getJsonArray("judicialResults").getJsonObject(0);
            assertThat(judicialResultsObject.getJsonObject("courtClerk").getString("firstName"), equalTo("XXXXX"));
//            assertThat(judicialResultsObject.getString("orderedDate"), equalTo("1996-02-21"));
        }
        else {
            fail("EventStoreDataAnonymizer failed to load.");
        }
    }

    private JsonEnvelope buildSendingSheetCompletedEnvelope() throws IOException {

        final StringWriter stringWriter = new StringWriter();
        final InputStream stream = EventStoreDataAnonymizerTest.class.getResourceAsStream("/test-data.json");
        IOUtils.copy(stream, stringWriter, UTF_8);
        final JsonReader jsonReader = Json.createReader(new StringReader(stringWriter.toString()));
        final JsonObject payload = jsonReader.readObject();
        jsonReader.close();

        MetadataBuilder metadataBuilder = metadataBuilderFrom(createObjectBuilder().add("id", randomUUID().toString()).add("name", "data.anon.test-event2").build());
        metadataBuilder.withStreamId(fromString(CASE_ID));

        return envelopeFrom(metadataBuilder.withStreamId(fromString(CASE_ID)).build(), payload);
    }


}