package uk.gov.moj.cpp.results.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;

import uk.gov.justice.services.common.http.HeaderConstants;

import javax.ws.rs.core.Response;

public class DocumentGeneratorStub {

    private static final String SYS_DOC_GENERATOR_URL = "/.*/rest/systemdocgenerator/generate-document";
    private static final String GENERATE_DOCUMENT_MEDIA_TYPE = "application/vnd.systemdocgenerator.generate-document+json";

    public static void stubDocumentCreateWithStatusOk(String documentText) {
        final String PATH = "/systemdocgenerator-command-api/command/api/rest/systemdocgenerator/render";
        stubFor(post(urlPathMatching(PATH))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.systemdocgenerator.render+json"))
                .willReturn(aResponse().withStatus(Response.Status.OK.getStatusCode())
                        .withBody(documentText.getBytes())));
    }

    public static void stubDocGeneratorEndPoint() {
        stubPingFor("systemdocgenerator-service");

        stubFor(post(urlPathMatching(SYS_DOC_GENERATOR_URL))
                .withHeader(CONTENT_TYPE, equalTo(GENERATE_DOCUMENT_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, GENERATE_DOCUMENT_MEDIA_TYPE)
                ));
    }

}
