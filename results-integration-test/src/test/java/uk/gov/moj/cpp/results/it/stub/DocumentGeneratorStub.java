package uk.gov.moj.cpp.results.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static com.jayway.awaitility.Duration.ONE_MINUTE;
import static com.jayway.awaitility.Duration.TEN_SECONDS;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_GATEWAY_TIMEOUT;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.Response;

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentGeneratorStub {

    private final static Logger LOGGER = LoggerFactory.getLogger(DocumentGeneratorStub.class);

    public static final String PATH = "/systemdocgenerator-service/command/api/rest/systemdocgenerator";
    private static final String SYS_DOC_GENERATOR_URL = "/.*/rest/systemdocgenerator/generate-document";
    private static final String GENERATE_DOCUMENT_MEDIA_TYPE = "application/vnd.systemdocgenerator.generate-document+json";

    public static void stubDocumentCreate(String documentText) {
        stubFor(post(urlPathMatching(PATH))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.systemdocgenerator.render+json"))
                .willReturn(aResponse().withStatus(Response.Status.ACCEPTED.getStatusCode())
                        .withBody(documentText.getBytes())));
        waitForPostStubToBeReady(PATH, "application/vnd.systemdocgenerator.render+json", Response.Status.ACCEPTED);
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

    public static void stubDocGeneratorEndPointFailed() {
        stubPingFor("systemdocgenerator-service");

        stubFor(post(urlPathMatching(SYS_DOC_GENERATOR_URL))
                .withHeader(CONTENT_TYPE, equalTo(GENERATE_DOCUMENT_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_GATEWAY_TIMEOUT)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, GENERATE_DOCUMENT_MEDIA_TYPE)
                ));
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

    public static List<JSONObject> pollSysDocGenerationRequestsForOriginatingSource(final Matcher<Collection<?>> matcher, final String originatingSource) {
        try {
            final List<JSONObject> postRequests = await().pollInterval(Duration.ONE_SECOND).atMost(ONE_MINUTE).until(() -> findAll(
                    postRequestedFor(urlPathMatching(SYS_DOC_GENERATOR_URL)))
                    .stream()
                    .map(LoggedRequest::getBodyAsString)
                    .map(JSONObject::new)
                    .filter(j -> j.getString("originatingSource").equals(originatingSource))
                    .collect(toList()), matcher);
            return postRequests;
        } catch (final ConditionTimeoutException timeoutException) {
            LOGGER.info("Exception while finding the captured requests in wire mock:" + timeoutException);
            return emptyList();
        }
    }

}
