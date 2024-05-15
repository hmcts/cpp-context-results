package uk.gov.moj.cpp.results.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static com.jayway.awaitility.Duration.TEN_SECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;

import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.util.List;

import javax.ws.rs.core.Response;

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;

public class DocumentGeneratorStub {

    public static final String PATH = "/systemdocgenerator-service/command/api/rest/systemdocgenerator";

    public static void stubDocumentCreate(String documentText) {
        stubFor(post(urlPathMatching(PATH))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.systemdocgenerator.render+json"))
                .willReturn(aResponse().withStatus(Response.Status.ACCEPTED.getStatusCode())
                        .withBody(documentText.getBytes())));
        waitForPostStubToBeReady(PATH, "application/vnd.systemdocgenerator.render+json", Response.Status.ACCEPTED);
    }

    public static void verifyCreate(List<String> expectedValues) {
        await().atMost(30, SECONDS).pollInterval(5, SECONDS).until(() -> {
        RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(PATH));
        expectedValues.forEach(
                expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
        );
        verify(requestPatternBuilder);
        });
    }

    public static void waitForPostStubToBeReady(final String resource, final String mediaType, final Response.Status expectedStatus) {
        final RestClient restClient = new RestClient();
        waitAtMost(TEN_SECONDS).until(() -> restClient.postCommand(getBaseUri() + resource, mediaType, "{}").getStatus() == expectedStatus.getStatusCode());
    }

}
