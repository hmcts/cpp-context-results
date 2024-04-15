package uk.gov.moj.cpp.results.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;

import javax.json.Json;

public class ProgressionStub {

    private static final String PROGRESSION_SERVICE_NAME = "progression-service";

    private static final String PROGRESSION_PROSECUTION_CASE_QUERY_URL = "/progression-service/query/api/rest/progression/prosecutioncases/{0}";
    private static final String PROGRESSION_PROSECUTION_CASE_MEDIA_TYPE = "application/vnd.progression.query.prosecutioncase+json";

    private static final String PROGRESSION_PROSECUTION_CASE_URN_URL = "/progression-service/query/api/rest/progression/search";
    private static final String PROGRESSION_PROSECUTION_CASE_URN_MEDIA_TYPE = "application/vnd.progression.query.case-exists-by-caseurn+json";

    public static void stubGetProgressionProsecutionCases(final UUID caseId) {
        InternalEndpointMockUtils.stubPingFor(PROGRESSION_SERVICE_NAME);

        final String stringUrl = format(PROGRESSION_PROSECUTION_CASE_QUERY_URL, caseId);
        final String payload = getPayload("stub-data/progression.query.prosecutioncase.json");
        stubFor(get(urlPathEqualTo(stringUrl))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", PROGRESSION_PROSECUTION_CASE_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(stringUrl, PROGRESSION_PROSECUTION_CASE_MEDIA_TYPE);
    }

    public static void stubGetProgressionCaseExistsByUrn(final String caseUrn, final UUID caseId) {
        InternalEndpointMockUtils.stubPingFor(PROGRESSION_SERVICE_NAME);

        final String stringUrl = format(PROGRESSION_PROSECUTION_CASE_URN_URL);
        final String payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .build().toString();
        stubFor(get(urlPathEqualTo(stringUrl))
                .withQueryParam("caseUrn", equalTo(caseUrn))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", PROGRESSION_PROSECUTION_CASE_URN_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(stringUrl+"?caseUrn="+caseUrn, PROGRESSION_PROSECUTION_CASE_URN_MEDIA_TYPE);
    }
}
