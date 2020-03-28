package uk.gov.moj.cpp.results.command.handler;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;

public class ReferenceDataService {

    private static final Logger LOGGER = getLogger(ReferenceDataService.class);

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    @SuppressWarnings("squid:S1696")
    boolean getSpiOutFlagForProsecutionAuthorityCode(final String prosecutingAuthority) {
        final JsonObject payload = createObjectBuilder().add("prosecutorCode", prosecutingAuthority).build();
        final JsonEnvelope request = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("referencedata.query.prosecutors").build(), payload);
        final JsonObject response = requester.requestAsAdmin(request).payloadAsJsonObject();

        final JsonArray prosecutors = response.getJsonArray("prosecutors");

        if (null == prosecutors || prosecutors.size() != 1) {
            LOGGER.error("prosecutingAuthority not found or not unique");
            return false;
        }

        try {
            return response.getJsonArray("prosecutors").getJsonObject(0).getBoolean("spiOutFlag");
        } catch (final ClassCastException | NullPointerException e) {
            LOGGER.error("spiOutFlag not found", e);
            return false;
        }
    }
}
