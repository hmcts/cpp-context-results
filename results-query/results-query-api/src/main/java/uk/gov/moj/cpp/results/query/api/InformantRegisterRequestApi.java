package uk.gov.moj.cpp.results.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.query.view.InformantRegisterDocumentRequestQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class InformantRegisterRequestApi {
    @Inject
    private InformantRegisterDocumentRequestQueryView requester;

    @Handles("results.query.informant-register-document-request")
    public JsonEnvelope getInformantRegisterDocumentRequest(final JsonEnvelope query) {
        return requester.getInformantRegisterRequests(query);
    }

    @Handles("results.query.informant-register-document-by-material")
    public JsonEnvelope getInformantRegisterDocumentRequestByMaterial(final JsonEnvelope query) {
        return requester.getInformantRegistersByMaterial(query);
    }

    @Handles("results.query.informant-register-document-by-request-date")
    public JsonEnvelope getInformantRegisterDocumentByRequestDate(final JsonEnvelope query) {
        return requester.getInformantRegistersByRequestDate(query);
    }
}
