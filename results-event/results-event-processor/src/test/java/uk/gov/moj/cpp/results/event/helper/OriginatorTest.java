package uk.gov.moj.cpp.results.event.helper;

import static org.jgroups.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.results.event.helper.Originator.assembleEnvelopeWithPayloadAndMetaDetails;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class OriginatorTest {

    public static final String SOURCE = "originator";
    public static final String ORIGINATOR_VALUE = "court";
    public static final String SOURCE_NCES = "originator-nces";
    public static final String ORIGINATOR_VALUE_NCES = "nces";

    @Test
    public void shouldCreateMetadataWithProcessIdAndUserId() {
        final String userId = randomUUID().toString();
        final JsonObject payload = createObjectBuilder().add("key1", "value1").build();
        final JsonEnvelope envelope = assembleEnvelopeWithPayloadAndMetaDetails(payload, "application/json", userId, "nces");
        assertEquals(envelope.metadata().userId().get(), userId);
        assertEquals (ORIGINATOR_VALUE, envelope.metadata().asJsonObject().getString(SOURCE));
        assertEquals(ORIGINATOR_VALUE_NCES, envelope.metadata().asJsonObject().getString(SOURCE_NCES));
        assertEquals("application/json", envelope.metadata().asJsonObject().getString(NAME));

    }


    @Test
    void shouldTellAMaterialResultsUploadedApartFromOneOfAnotherContext() {
        final Metadata nces = metadataWithNcesOriginator("nces");
        final Metadata migratedInactiveNces = metadataWithNcesOriginator("nces:" + java.util.UUID.randomUUID() + ":" + java.util.UUID.randomUUID());
        final Metadata otherContext = JsonEnvelope.metadataFrom(createObjectBuilder()
                .add("id", java.util.UUID.randomUUID().toString()).add(NAME, "material.material-added").build()).build();

        assertTrue(Originator.isNcesMaterial(nces));
        assertTrue(Originator.isResultsMaterial(nces));
        assertTrue(Originator.isMigratedInactiveNcesMaterial(migratedInactiveNces));
        assertTrue(Originator.isResultsMaterial(migratedInactiveNces));
        assertFalse(Originator.isNcesMaterial(migratedInactiveNces));
        assertFalse(Originator.isResultsMaterial(otherContext));
        assertFalse(Originator.isResultsMaterial(metadataWithNcesOriginator("court")));
    }

    private static Metadata metadataWithNcesOriginator(final String ncesOriginator) {
        return JsonEnvelope.metadataFrom(createObjectBuilder()
                .add("id", java.util.UUID.randomUUID().toString())
                .add(NAME, "material.material-added")
                .add(Originator.SOURCE_NCES, ncesOriginator)
                .build()).build();
    }
}
