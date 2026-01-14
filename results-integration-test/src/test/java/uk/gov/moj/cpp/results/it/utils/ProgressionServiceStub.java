package uk.gov.moj.cpp.results.it.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.getJsonResponse;

import java.util.UUID;

public class ProgressionServiceStub {

    private static final String PROGRESSION_QUERY_GROUP_MEMBER_CASES_URL = "/progression-service/query/api/rest/progression/prosecutioncases/group/{0}";
    private static final String PROGRESSION_QUERY_GROUP_MEMBER_CASES_MEDIA_TYPE = "application/vnd.progression.query.group-member-cases+json";
    private static final String PROGRESSION_QUERY_GROUP_MEMBER_CASES_RESPONSE = "stub-data/progression.query.group-member-cases.json";

    private static final String PROGRESSION_QUERY_INACTIVE_MIGRATED_CASES_URL = "/progression-service/query/api/rest/progression/search-inactive-migratedcases?caseIds={0}";
    private static final String PROGRESSION_QUERY_INACTIVE_MIGRATED_CASES_MEDIA_TYPE = "application/vnd.progression.query.search-inactive-migrated-cases+json";
    private static final String PROGRESSION_QUERY_INACTIVE_MIGRATED_CASES_RESPONSE = "stub-data/progression.query.inactive.migrated.cases.json";



    public static void stubQueryGroupMemberCases(final UUID groupId) {
        final String queryURI = format(PROGRESSION_QUERY_GROUP_MEMBER_CASES_URL, groupId);
        stubFor(get(urlPathEqualTo(queryURI))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, PROGRESSION_QUERY_GROUP_MEMBER_CASES_MEDIA_TYPE)
                        .withBody(getJsonResponse(PROGRESSION_QUERY_GROUP_MEMBER_CASES_RESPONSE))));
    }


    public static void stubQueryInactiveMigratedCases(final String caseIds) {
        final String queryURI = format(PROGRESSION_QUERY_INACTIVE_MIGRATED_CASES_URL, caseIds);

        stubFor(get(urlEqualTo(queryURI))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, PROGRESSION_QUERY_INACTIVE_MIGRATED_CASES_MEDIA_TYPE)
                        .withBody(getJsonResponse(PROGRESSION_QUERY_INACTIVE_MIGRATED_CASES_RESPONSE))));
    }
}