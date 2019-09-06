package uk.gov.moj.cpp.results.domain.transformation.util;

import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.results.domain.transformation.util.HearingHelper.transformHearing;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;

public class ResultsAddHearingTransformationEventHelper {

    private static final Logger LOGGER = getLogger(ResultsAddHearingTransformationEventHelper.class);
    private static final String HEARING = "hearing";
    public static final String SHARED_TIME = "sharedTime";

    public JsonEnvelope buildTransformedPayloadForResults(JsonEnvelope event, String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug("Actual Payload as per master -> {} ", payload);

        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(HEARING, transformHearing(payload.getJsonObject(HEARING)))
                .add(SHARED_TIME, payload.getJsonString(SHARED_TIME));

        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug("Transformed Payload as per mot -> {} ", transformedObject);

        return envelopeFrom(metadataBuilder()
                .withName(newEvent)
                .withId(UUID.fromString(event.metadata().asJsonObject().getString("id"))),
                transformedObject);
    }
}
