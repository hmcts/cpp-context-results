package uk.gov.moj.cpp.results.query.api;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(QUERY_API)
public class ResultsQueryApi {

    @Inject
    private Requester requester;

    @Handles("results.get-hearing-details")
    public JsonEnvelope handleGetHearingDetails(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("results.get-results-summary")
    public JsonEnvelope handleGetResultsSummary(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("results.get-hearing-information-details-for-hearing")
    public JsonEnvelope handleGetResultsDetails(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("results.get-hearing-details-internal")
    public JsonEnvelope handleGetResultsDetailsInternal(final JsonEnvelope query) {
        return requester.request(query);
    }
}
