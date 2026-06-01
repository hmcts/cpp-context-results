package uk.gov.moj.cpp.results.event.service;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.Optional;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;

public class SjpService {

    private static final String SJP_QUERY_CASE_EXISTS_BY_CASEURN = "sjp.query.case-by-urn";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;


    public Optional<JsonObject> caseExistsByCaseUrn(final String caseUrn) {
        final JsonObject payload = createObjectBuilder().add("urn", caseUrn).build();
        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(SJP_QUERY_CASE_EXISTS_BY_CASEURN)
                .build();
        final Envelope<JsonObject> envelope = envelopeFrom(metadata, payload);
        final Envelope<JsonObject> response = requester.requestAsAdmin(envelope,JsonObject.class);
        return Optional.of(response.payload());
    }
}
