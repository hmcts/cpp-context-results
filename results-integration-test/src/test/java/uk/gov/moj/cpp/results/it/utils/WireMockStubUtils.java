package uk.gov.moj.cpp.results.it.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
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
import static org.awaitility.Awaitility.waitAtMost;
import static org.awaitility.Durations.TEN_SECONDS;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.getPayload;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.io.Resources;
import org.apache.http.HttpHeaders;

/**
 * Utility class for setting stubs.
 */
public class WireMockStubUtils {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE_QUERY_GROUPS = "application/vnd.usersgroups.groups+json";
    public static final String MATERIAL_UPLOAD_COMMAND_TYPE = "material.command.upload-file";
    public static final String UPLOAD_MATERIAL_COMMAND = "/material-service/command/api/rest/material/material";
    static {
        configureFor(HOST, 8080);
        reset();
    }

    public static void setupUsersGroupQueryStub() {
        InternalEndpointMockUtils.stubPingFor("usersgroups-service");
        stubFor(get(urlMatching("/usersgroups-service/query/api/rest/usersgroups/users/.*"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/usersgroups.get-systemuser-groups-by-user.json"))));
    }

    public static void setupAsSystemUser(final UUID userId) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FileUtil.getPayload("stub-data/usersgroups.get-systemuser-groups-by-user.json"))));

        waitForStubToBeReady(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId), CONTENT_TYPE_QUERY_GROUPS);
    }

    public static void setupUserAsPrisonAdminGroup(final UUID userId) {
        setupUserWithGroup(userId, "stub-data/get-groups-prison-admin.json");
    }

    public static void setupUserAsCourtAdminGroup(final UUID userId) {
        setupUserWithGroup(userId, "stub-data/get-groups-court-admin.json");
    }

    private static void setupUserWithGroup(final UUID userId, final String userGroupFile) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse(userGroupFile))));

        waitForStubToBeReady(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId), CONTENT_TYPE_QUERY_GROUPS);
    }

    public static String getJsonResponse(final String filename) {
        try {
            return Resources.toString(getResource(filename), defaultCharset());
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static void stubMaterialUploadFile() {
        InternalEndpointMockUtils.stubPingFor("material-service");

        stubFor(post(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody("")
                ));

        stubFor(get(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(UPLOAD_MATERIAL_COMMAND, MATERIAL_UPLOAD_COMMAND_TYPE);
    }


    public static void waitForStubToBeReady(final String resource, final String mediaType) {
        waitForStubToBeReady(resource, mediaType, Status.OK);
    }

    public static void waitForStubToBeReady(final String resource, final String mediaType, final Status expectedStatus) {
        var urlFormatter = resource.startsWith("/") ? "{0}{1}" : "{0}/{1}";
        poll(requestParams(format(urlFormatter, getBaseUri(), resource), mediaType)).until(status().is(expectedStatus));
    }

    public static void stubDocGeneratorEndPoint() {
        stubPingFor("systemdocgenerator-service");

        final String urlPath = "/systemdocgenerator-service/command/api/rest/systemdocgenerator/generate-document/.*";

        stubFor(post(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
        waitForPostStubToBeReady(urlPath, "application/vnd.systemdocgenerator.generate-document+json", Response.Status.ACCEPTED);
    }

    public static void waitForPostStubToBeReady(final String resource, final String mediaType, final Response.Status expectedStatus) {
        final RestClient restClient = new RestClient();
        waitAtMost(TEN_SECONDS).until(() -> restClient.postCommand(getBaseUri() + resource, mediaType, "").getStatus() == expectedStatus.getStatusCode());
    }

    public static void stubNotificationNotifyEndPoint() {
        stubPingFor("notificationnotify-service");

        final String urlPath = "/notificationnotify-service/command/api/rest/notificationnotify/notifications/.*";

        stubFor(post(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
        waitForPostStubToBeReady(urlPath, "application/vnd.notificationnotify.send-email-notification+json", Response.Status.ACCEPTED);
    }
}
