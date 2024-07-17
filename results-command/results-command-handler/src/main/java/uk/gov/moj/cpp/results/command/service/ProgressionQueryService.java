package uk.gov.moj.cpp.results.command.service;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressionQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionQueryService.class);
    public static final String GROUP_ID = "groupId";

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    public Optional<JsonObject> getGroupMemberCases(final JsonEnvelope envelope, final String groupId) {
        final JsonObject requestParameter = createObjectBuilder()
                .add(GROUP_ID, groupId)
                .build();

        final Envelope<JsonObject> requestEnvelope = envelop(requestParameter)
                .withName("progression.query.group-member-cases")
                .withMetadataFrom(envelope);

        LOGGER.info("groupId {}, Get group member cases detail request {}", groupId, requestParameter);

        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("groupId {} group member case detail payload {}", groupId, getPayload(response));
        }

        return Optional.ofNullable(getPayload(response));
    }

    private JsonObject getPayload(final Envelope<JsonObject> envelope) {
        if (nonNull(envelope)) {
            return envelope.payload();
        } else {
            return null;
        }
    }
}