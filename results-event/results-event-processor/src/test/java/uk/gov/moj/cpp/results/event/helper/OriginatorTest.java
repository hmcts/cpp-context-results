package uk.gov.moj.cpp.results.event.helper;

import static org.jgroups.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.moj.cpp.results.event.helper.Originator.assembleEnvelopeWithPayloadAndMetaDetails;

import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;

public class OriginatorTest {

    public static final String SOURCE = "originator";
    public static final String ORIGINATOR_VALUE = "court";
    public static final String SOURCE_NCES = "originator-nces";
    public static final String ORIGINATOR_VALUE_NCES = "nces";

    @Test
    public void shouldCreateMetadataWithProcessIdAndUserId() {
        final String userId = randomUUID().toString();
        final JsonObject payload = Json.createObjectBuilder().add("key1", "value1").build();
        final JsonEnvelope envelope = assembleEnvelopeWithPayloadAndMetaDetails(payload, "application/json", userId);
        assertEquals(envelope.metadata().userId().get(), userId);
        assertEquals (ORIGINATOR_VALUE,envelope.metadata().asJsonObject().getString(SOURCE) );
        assertEquals(ORIGINATOR_VALUE_NCES,envelope.metadata().asJsonObject().getString(SOURCE_NCES));
        assertEquals("application/json",envelope.metadata().asJsonObject().getString(NAME));

    }

}