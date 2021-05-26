package uk.gov.moj.cpp.results.command.handler;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferenceDataService {

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    Optional<JsonObject> getSpiOutFlagForProsecutionAuthorityCode(final String prosecutingAuthority) {
        final JsonObject payload = createObjectBuilder().add("prosecutorCode", prosecutingAuthority).build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName("referencedata.query.prosecutors")
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);
        final JsonObject response  = requester.requestAsAdmin(jsonEnvelope, JsonObject.class).payload();
        if(response.getJsonArray("prosecutors").isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(response.getJsonArray("prosecutors").getJsonObject(0));
    }
}
