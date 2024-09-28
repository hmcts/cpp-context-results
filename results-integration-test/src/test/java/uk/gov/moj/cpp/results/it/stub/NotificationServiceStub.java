package uk.gov.moj.cpp.results.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.awaitility.Awaitility.await;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;

import java.util.List;
import java.util.UUID;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

public class NotificationServiceStub {
    public static final String NOTIFICATION_NOTIFY_ENDPOINT = "/notificationnotify-service/command/api/rest/notificationnotify/notifications/.*";
    public static final String NOTIFICATIONNOTIFY_SEND_EMAIL_NOTIFICATION_JSON = "application/vnd.notificationnotify.email+json";


    public static void setUp() {
        stubPingFor("notificationnotify-service");
        stubFor(post(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT))
                .withHeader(CONTENT_TYPE, equalTo(NOTIFICATIONNOTIFY_SEND_EMAIL_NOTIFICATION_JSON))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString()))
        );
    }

    public static void verifyEmailNotificationIsRaised(final List<String> expectedValues) {
        await().atMost(30, SECONDS).pollInterval(5, SECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT));
            expectedValues.forEach(
                    expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
            );
            requestPatternBuilder.withRequestBody(notMatching("materialUrl"));
            verify(requestPatternBuilder);
            return true;
        });
    }

}
