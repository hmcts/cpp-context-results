package uk.gov.moj.cpp.results.event.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferenceDataService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    // TODO: cjsOffenceCode is a queryParameter not a uriParameter so check this actually works..
    public JsonEnvelope getOffenceByCjsCode(final String cjsOffenceCode, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("cjsoffencecode", cjsOffenceCode).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, "referencedata.query.offences").apply(payload);
        return requester.requestAsAdmin(request);
    }

}
