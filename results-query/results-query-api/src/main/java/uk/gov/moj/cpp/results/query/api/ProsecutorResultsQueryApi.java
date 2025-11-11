package uk.gov.moj.cpp.results.query.api;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.query.api.validator.ProsecutorResultsQueryValidator;
import uk.gov.moj.cpp.results.query.view.ProsecutorResultsQueryView;

import javax.inject.Inject;

@ServiceComponent(QUERY_API)
public class ProsecutorResultsQueryApi {

    @Inject
    private ProsecutorResultsQueryValidator prosecutorResultsQueryValidator;

    @Inject
    private ProsecutorResultsQueryView prosecutorResultsQueryView;

    @Handles("results.prosecutor-results")
    public JsonEnvelope handleProsecutorResults(final JsonEnvelope jsonEnvelope) {

        prosecutorResultsQueryValidator.validatePayload(jsonEnvelope);

        return prosecutorResultsQueryView.getProsecutorResults(jsonEnvelope);
    }


}
