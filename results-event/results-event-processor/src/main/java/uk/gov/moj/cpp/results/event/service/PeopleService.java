package uk.gov.moj.cpp.results.event.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class PeopleService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    public JsonEnvelope getPersonById(final UUID personId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("personId", personId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, "people.query.person").apply(payload);
        return requester.requestAsAdmin(request);
    }
}
