package uk.gov.moj.cpp.results.it.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.getPayload;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import com.google.common.io.Resources;
import org.apache.http.HttpHeaders;

/**
 * Utility class for setting stubs.
 */
public class WireMockStubUtils {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String APPLICATION_JSON = "application/json";
    public static final String UPLOAD_MATERIAL_COMMAND = "/material-service/command/api/rest/material/material";

    static {
        configureFor(HOST, 8080);
        reset();
    }

    public static void setupUsersGroupQueryStub() {
        stubFor(get(urlMatching("/usersgroups-service/query/api/rest/usersgroups/users/.*"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/usersgroups.get-systemuser-groups-by-user.json"))));
    }

    public static void setupAsSystemUser(final UUID userId) {
        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FileUtil.getPayload("stub-data/usersgroups.get-systemuser-groups-by-user.json"))));
    }

    public static void setupUserAsPrisonAdminGroup(final UUID userId) {
        setupUserWithGroup(userId, "stub-data/get-groups-prison-admin.json");
    }

    public static void setupUserAsCourtAdminGroup(final UUID userId) {
        setupUserWithGroup(userId, "stub-data/get-groups-court-admin.json");
    }

    private static void setupUserWithGroup(final UUID userId, final String userGroupFile) {
        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse(userGroupFile))));
    }

    public static String getJsonResponse(final String filename) {
        try {
            return Resources.toString(getResource(filename), defaultCharset());
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static void stubMaterialUploadFile() {
        stubFor(post(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody("")
                ));

        stubFor(get(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));
    }

    public static void stubDocGeneratorEndPoint() {
        final String urlPath = "/systemdocgenerator-service/command/api/rest/systemdocgenerator/generate-document/.*";

        stubFor(post(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void stubNotificationNotifyEndPoint() {
        final String urlPath = "/notificationnotify-service/command/api/rest/notificationnotify/notifications/.*";

        stubFor(post(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void stubDocumentCreate(String documentText) {

        final String PATH = "/systemdocgenerator-service/command/api/rest/systemdocgenerator/render";

        stubFor(post(urlPathMatching(PATH))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.systemdocgenerator.render+json"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(documentText.getBytes())));
    }
}
