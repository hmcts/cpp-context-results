package uk.gov.moj.cpp.results.event.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ProgressionService {

    private static final String PROGRESSION_QUERY_CASE_EXISTS_BY_CASEURN = "progression.query.case-exist-by-caseurn";
    private static final String PROGRESSION_CASE_DETAILS = "progression.query.prosecutioncase";
    private static final String PROGRESSION_QUERY_APPLICATION = "progression.query.application-only";
    public static final String CASE_ID = "caseId";

    @Inject
    private UtcClock utcClock;

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    public JsonEnvelope getDefendantsByCaseId(final UUID caseId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add(CASE_ID, caseId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, "progression.query.defendants").apply(payload);
        return requester.requestAsAdmin(request);
    }

    public JsonEnvelope getCase(final UUID caseId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add(CASE_ID, caseId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, "progression.query.caseprogressiondetail").apply(payload);
        return requester.requestAsAdmin(request);
    }

    public Optional<JsonObject> caseExistsByCaseUrn(final String caseUrn) {
        final JsonObject payload = createObjectBuilder().add("caseUrn", caseUrn).build();
        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(PROGRESSION_QUERY_CASE_EXISTS_BY_CASEURN)
                .build();
        final Envelope<JsonObject> envelope = envelopeFrom(metadata, payload);
        final Envelope<JsonObject> response = requester.requestAsAdmin(envelope, JsonObject.class);
        return Optional.ofNullable(response.payload());
    }

    public JsonObject getProsecutionCaseDetails(final UUID caseId) {
        final JsonObject query = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build();

        final Envelope<JsonObject> envelope = envelopeFrom(
                metadataBuilder()
                        .createdAt(utcClock.now())
                        .withName(PROGRESSION_CASE_DETAILS)
                        .withId(randomUUID())
                        .build(),
                query);

        return requester.requestAsAdmin(envelope, JsonObject.class).payload();
    }

    /**
     * This method is used to get the application details for a given application ID.
     *
     * @param applicationId The UUID of the application.
     * @return An Optional containing the JsonObject with application details, or empty if not found.
     */
    public Optional<JsonObject> getApplicationDetails(UUID applicationId) {
        final JsonObject payload = createObjectBuilder()
                .add("applicationId", applicationId.toString())
                .build();
        final Envelope<JsonObject> envelope = envelopeFrom(
                metadataBuilder()
                        .createdAt(utcClock.now())
                        .withName(PROGRESSION_QUERY_APPLICATION)
                        .withId(randomUUID())
                        .build(),
                payload);
        return Optional.ofNullable(requester.requestAsAdmin(envelope, JsonObject.class).payload());
    }
}
