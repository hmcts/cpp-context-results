package uk.gov.moj.cpp.results.query.api;

import static org.mockito.Mockito.verify;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.results.query.view.InformantRegisterDocumentRequestQueryView;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class InformantRegisterRequestApiTest {
    @Mock
    private InformantRegisterDocumentRequestQueryView requester;

    @InjectMocks
    private InformantRegisterRequestApi informantRegisterRequestApi;

    @Test
    public void getInformantRegisterDocumentRequest() {
        final JsonObjectBuilder informantRegisterDocumentPayload = Json.createObjectBuilder();
        final JsonEnvelope response = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("results.query.informant-register-document-request"),
                informantRegisterDocumentPayload);
        informantRegisterRequestApi.getInformantRegisterDocumentRequest(response);
        verify(requester).getInformantRegisterRequests(response);
    }

    @Test
    public void getInformantRegisterDocumentRequestByMaterial() {
        final JsonObjectBuilder informantRegisterDocumentPayload = Json.createObjectBuilder();
        final JsonEnvelope response = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("results.query.informant-register-document-by-material"),
                informantRegisterDocumentPayload);
        informantRegisterRequestApi.getInformantRegisterDocumentRequestByMaterial(response);
        verify(requester).getInformantRegistersByMaterial(response);

    }

    @Test
    public void getInformantRegisterDocumentRequestByDate() {
        final JsonObjectBuilder informantRegisterDocumentPayload = Json.createObjectBuilder();
        final JsonEnvelope response = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("results.query.informant-register-document-by-request-date"),
                informantRegisterDocumentPayload);
        informantRegisterRequestApi.getInformantRegisterDocumentByRequestDate(response);
        verify(requester).getInformantRegistersByRequestDate(response);

    }
}
