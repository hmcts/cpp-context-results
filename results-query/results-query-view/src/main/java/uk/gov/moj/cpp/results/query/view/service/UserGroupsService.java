package uk.gov.moj.cpp.results.query.view.service;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.HearingRepository;
import uk.gov.moj.cpp.results.persist.HearingResultRepository;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;

@SuppressWarnings("squid:S3655")
public class UserGroupsService {

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private HearingResultRepository hearingResultRepository;

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public List<String> findUserGroupsByUserId(JsonEnvelope query) {
        final JsonEnvelope requestEnvelope = enveloper.withMetadataFrom(query, "usersgroups.get-groups-by-user")
                .apply(Json.createObjectBuilder().add("userId", query.metadata().userId().get()).build());
        final JsonEnvelope responseEnvelope = requester.requestAsAdmin(requestEnvelope);
        final JsonObject responsePayload = responseEnvelope.payloadAsJsonObject();
        final JsonArray groups = responsePayload.getJsonArray("groups");
        return groups.getValuesAs(JsonObject.class)
                .stream()
                .map(group -> group.getString("groupName"))
                .collect(Collectors.toList());
    }
}
