package uk.gov.moj.cpp.results.query.view.service;

import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

@SuppressWarnings("squid:S3655")
public class UserGroupsService {

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public List<String> findUserGroupsByUserId(JsonEnvelope query) {
        final JsonEnvelope requestEnvelope = enveloper.withMetadataFrom(query, "usersgroups.get-groups-by-user")
                .apply(JsonObjects.createObjectBuilder().add("userId", query.metadata().userId().get()).build());
        final JsonEnvelope responseEnvelope = requester.requestAsAdmin(requestEnvelope);
        final JsonObject responsePayload = responseEnvelope.payloadAsJsonObject();
        final JsonArray groups = responsePayload.getJsonArray("groups");
        return groups.getValuesAs(JsonObject.class)
                .stream()
                .map(group -> group.getString("groupName"))
                .collect(Collectors.toList());
    }
}
