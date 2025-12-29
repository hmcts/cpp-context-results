package uk.gov.moj.cpp.results.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.text.MessageFormat.format;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.getPayload;

import java.util.Map;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;

public class ProgressionStub {

    private static final String PROGRESSION_PROSECUTION_CASE_QUERY_URL = "/progression-service/query/api/rest/progression/prosecutioncases/{0}";
    private static final String PROGRESSION_PROSECUTION_CASE_MEDIA_TYPE = "application/vnd.progression.query.prosecutioncase+json";

    private static final String PROGRESSION_PROSECUTION_CASE_URN_URL = "/progression-service/query/api/rest/progression/search";
    private static final String PROGRESSION_PROSECUTION_CASE_URN_MEDIA_TYPE = "application/vnd.progression.query.case-exists-by-caseurn+json";

    public static void stubGetProgressionProsecutionCases(final UUID caseId) {
        final String stringUrl = format(PROGRESSION_PROSECUTION_CASE_QUERY_URL, caseId);
        final String payload = getPayload("stub-data/progression.query.prosecutioncase.json");
        stubFor(get(urlPathEqualTo(stringUrl))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", PROGRESSION_PROSECUTION_CASE_MEDIA_TYPE)
                        .withBody(payload)));
    }

    public static void stubGetProgressionProsecutionCasesFromPayload(final String filepath, final UUID caseId, final String urn, final UUID hearingId, final UUID applicationId) {
        final String stringUrl = format(PROGRESSION_PROSECUTION_CASE_QUERY_URL, caseId);
        final String payload = getPayload(filepath)
                .replaceAll("HEARING_ID", hearingId.toString())
                .replaceAll("CASE_ID", caseId.toString())
                .replaceAll("CASE_URN", urn);
        if (nonNull(applicationId)) {
            payload.replaceAll("APPLICATION_ID", applicationId.toString());
        }
        stubFor(get(urlPathEqualTo(stringUrl))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", PROGRESSION_PROSECUTION_CASE_MEDIA_TYPE)
                        .withBody(payload)));
    }

    public static void stubGetProgressionProsecutionCase_WhichHasLikedCases_AndHearings(final String filepath, final Map<String,String> caseIdMap, final Map<String,String> urnMap, final Map<String,String> hearingIdMap, final Map<String,String> applicationIdMap) {
        final String stringUrl = format(PROGRESSION_PROSECUTION_CASE_QUERY_URL, caseIdMap.get("MAIN_CASE_ID"));
        String payload = getPayload(filepath);
        payload = updateKeyValueInString(caseIdMap,payload);
        payload = updateKeyValueInString(urnMap,payload);
        payload = updateKeyValueInString(applicationIdMap,payload);
        payload = updateKeyValueInString(hearingIdMap,payload);
        stubFor(get(urlPathEqualTo(stringUrl))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", PROGRESSION_PROSECUTION_CASE_MEDIA_TYPE)
                        .withBody(payload)));
    }

    public static void stubGetProgressionCaseExistsByUrn(final String caseUrn, final UUID caseId) {
        final String stringUrl = format(PROGRESSION_PROSECUTION_CASE_URN_URL);
        final String payload = JsonObjects.createObjectBuilder()
                .add("caseId", caseId.toString())
                .build().toString();
        stubFor(get(urlPathEqualTo(stringUrl))
                .withQueryParam("caseUrn", equalTo(caseUrn))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", PROGRESSION_PROSECUTION_CASE_URN_MEDIA_TYPE)
                        .withBody(payload)));
    }

    public static String updateKeyValueInString(final Map<String, String> keyValueMap, String payload) {
        if (nonNull(keyValueMap)) {
            for (Map.Entry entry : keyValueMap.entrySet()) {
                payload = payload.replaceAll(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return payload;
    }
}
