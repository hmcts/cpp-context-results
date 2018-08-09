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

public class HearingService {

    private static final String FIELD_HEARING_ID = "hearingId";
    private static final String FIELD_CASE_ID = "caseId";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    public JsonEnvelope getHearingById(final UUID hearingId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add(FIELD_HEARING_ID, hearingId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, "hearing.get.hearing").apply(payload);
        return requester.requestAsAdmin(request);
    }

    public JsonEnvelope getDefenceCounselsByHearingId(final UUID hearingId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add(FIELD_HEARING_ID, hearingId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, "hearing.get.defence-counsels").apply(payload);
        return requester.requestAsAdmin(request);
    }

    public JsonEnvelope getProsecutionCounselsByHearingId(final UUID hearingId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add(FIELD_HEARING_ID, hearingId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, "hearing.get.prosecution-counsels").apply(payload);
        return requester.requestAsAdmin(request);
    }

    public JsonEnvelope getPleasByCaseId(final UUID caseId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add(FIELD_CASE_ID, caseId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, "hearing.get.case.pleas").apply(payload);
        return requester.requestAsAdmin(request);
    }
}
