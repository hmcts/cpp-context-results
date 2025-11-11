package uk.gov.moj.cpp.results.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getSingleStubMapping;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.removeStub;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.results.it.utils.FeatureStubUtil.setFeatureToggle;

import java.util.List;
import java.util.UUID;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DcsStub {

    private  static final Logger LOGGER = LoggerFactory.getLogger(DcsStub.class);

    final static UUID stubId = randomUUID();

    public static final String DCS_ENDPOINT = "/stagingdcs-service/command/api/rest/stagingdcs/create-case";


    public static void setupDCSStub() {
        setFeatureToggle("StagingDcs", true);
        stubFor(post(urlPathMatching(DCS_ENDPOINT))
                .withId(stubId)
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.stagingdcs.submit-dcs-case-record+json"))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, randomUUID().toString()))
        );
    }
    public static void verifyDCSRequestIsRaised(final List<String> expectedValues, int numberOfTimes) {
        try {
            final RequestPatternBuilder urlPatternBuilder = postRequestedFor(urlPathMatching(DCS_ENDPOINT));
            expectedValues.forEach(value -> urlPatternBuilder.withRequestBody(new ContainsPattern(value)));
            verify(numberOfTimes, urlPatternBuilder);
        } catch (VerificationException e) {
            fail("A POST request to the DCS endpoint was not made as expected: " + e.getMessage());
        }
    }

    public static void clearDcsStub() {
        try {
            removeStub(getSingleStubMapping(stubId));
            setFeatureToggle("StagingDcs", false);
        } catch (Exception e) {
            LOGGER.error("unable to remove dcs stub", e);
        }
    }
}
