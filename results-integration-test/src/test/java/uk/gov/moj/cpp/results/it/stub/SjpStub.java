package uk.gov.moj.cpp.results.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;

import org.apache.http.HttpHeaders;


public class SjpStub {

    private static final String SJP_CASE_URN_MEDIA_TYPE = "application/vnd.sjp.query.case-by-urn+json";
    private static final String SJP_CASE_QUERY_URL = "/sjp-service/query/api/rest/sjp/cases";

    public static void setupSjpQueryStub(final String caseUrn, final UUID caseId) {

        final String payload = JsonObjects.createObjectBuilder()
                .add("id", caseId.toString())
                .build().toString();

        final String stringUrl = format(SJP_CASE_QUERY_URL);

        stubFor(get(urlPathEqualTo(stringUrl))
                .withQueryParam("urn", equalTo(caseUrn))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, SJP_CASE_URN_MEDIA_TYPE)
                        .withBody(payload)));
    }
}
