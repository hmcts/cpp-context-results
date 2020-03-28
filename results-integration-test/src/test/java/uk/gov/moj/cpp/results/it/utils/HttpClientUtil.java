package uk.gov.moj.cpp.results.it.utils;

import static java.lang.String.format;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.base.Joiner;

public class HttpClientUtil {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String PORT = "8080";

    private static final String WRITE_BASE_URL = "/results-service/command/api/rest/results";
    private static final String BASE_URI = format("http://%s:%s", HOST, PORT);
    private static final String WRITE_MEDIA_TYPE = "application/vnd.results.command.generate-police-results-for-a-defendant+json";
    private static final RestClient restClient = new RestClient();

    public static void sendGeneratePoliceResultsForADefendantCommand(final JsonObject jsonObject) {
        final String writeUrl = format("/results/%s/", jsonObject.getString("sessionId"));
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(HeaderConstants.USER_ID, getUserId());
        restClient.postCommand(getWriteUrl(writeUrl), WRITE_MEDIA_TYPE, jsonObject.toString(), headers);
    }

    private static String getWriteUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, WRITE_BASE_URL, resource);
    }

}