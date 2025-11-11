package uk.gov.moj.cpp.results.it.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static javax.ws.rs.core.Response.Status.OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.CIVIL_OFFENCE;
import static uk.gov.moj.cpp.results.it.helper.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.utils.HttpClientUtil.trackResultsCommand;
import static uk.gov.moj.cpp.results.it.utils.QueueUtilForPrivateEvents.RETRIEVE_TIMEOUT;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupAsSystemUser;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsCourtAdminGroup;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;

import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.test.matchers.MapJsonObjectToTypeMatcher;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.json.JSONObject;

public class ResultsStepDefinitions extends AbstractStepDefinitions {

    private static final String CONTENT_TYPE_HEARING_DETAILS = "application/vnd.results.hearing-details+json";
    private static final String CONTENT_TYPE_HEARING_INFORMATION_DETAILS = "application/vnd.results.hearing-information-details+json";
    private static final String CONTENT_TYPE_HEARING_DETAILS_INTERNAL = "application/vnd.results.hearing-details-internal+json";
    private static final String CONTENT_TYPE_RESULTS_SUMMARY = "application/vnd.results.results-summary+json";
    private static final String CONTENT_TYPE_DEFENDANT_TRACKING_STATUS = "application/vnd.results.get-defendants-tracking-status+json";
    private static final String CONTENT_TYPE_NCES_EMAIL_NOTIFICATION_DETAILS = "application/vnd.results.query.nces-email-notification-details+json";
    private static final String PUBLIC_EVENT_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final String GET_RESULTS_SUMMARY = "results.get-results-summary";
    private static MessageConsumerClient publicMessageConsumerPoliceResultsGenerated;


    public static void createMessageConsumers() {
        publicMessageConsumerPoliceResultsGenerated = new MessageConsumerClient();
    }

    public static void whenPrisonAdminTriesToViewResultsForThePerson(final UUID userId) {
        setupUserAsPrisonAdminGroup(userId);
        setLoggedInUser(userId);
    }

    public static void setLoggedInUserAsCourtAdmin(final UUID userId) {
        setupUserAsCourtAdminGroup(userId);
        setLoggedInUser(userId);
    }

    public static HearingResultSummariesView getSummariesByDate(final LocalDate fromDate) {
        final String resultSummaryUrl = format("%s%s?fromDate=%s", BASE_URI, getProperty(GET_RESULTS_SUMMARY), fromDate);
        final RequestParamsBuilder resultsSummaryRequest = requestParams(resultSummaryUrl,
                CONTENT_TYPE_RESULTS_SUMMARY)
                .withHeader(USER_ID, getLoggedInUser());

        final ResponseData resultsSummaryResponse = pollWithDefaults(resultsSummaryRequest.build())
                .until(
                        status().is(OK)
                );
        return MapJsonObjectToTypeMatcher.convert(HearingResultSummariesView.class, resultsSummaryResponse.getPayload());
    }

    public static <T> Matcher<ResponseData> jsonPayloadMatcher(final Class<T> theClass, final Matcher<T> matcher) {
        return new BaseMatcher<>() {
            @Override
            public boolean matches(final Object o) {
                if (o instanceof final ResponseData responseData) {
                    if (responseData.getPayload() != null) {
                        final T object = MapJsonObjectToTypeMatcher.convert(theClass, responseData.getPayload());
                        return matcher.matches(object);
                    }
                }
                return false;
            }

            @Override
            public void describeMismatch(final Object item, final Description description) {
                final ResponseData responseData = (ResponseData) item;
                final JsonObject jsonObject = Json.createReader(new StringReader(responseData.getPayload())).readObject();
                matcher.describeMismatch(jsonObject, description);
            }

            @Override
            public void describeTo(final Description description) {
                matcher.describeTo(description);
            }
        };
    }

    public static void getHearingDetails(final UUID hearingId, final UUID defendantId, final Matcher<HearingResultsAdded> matcher) {
        //TODO change this name
        final String personHearingResultDetailsUrl = format("%s%s", BASE_URI,
                getProperty("results.get-person-hearing-result-details", defendantId,
                        hearingId));

        final Matcher<ResponseData> responseDataMatcher = jsonPayloadMatcher(HearingResultsAdded.class, matcher);

        pollWithDefaults(requestParams(personHearingResultDetailsUrl, CONTENT_TYPE_HEARING_DETAILS).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearing.id", equalTo(hearingId.toString()))
                        )),
                        responseDataMatcher
                );

    }

    public static void getHearingDetailsForHearingId(final UUID hearingId, final Matcher... matchers) {
        final String hearingResultDetailsUrl = format("%s%s", BASE_URI,
                getProperty("results.get-hearing-information-details-for-hearing",
                        hearingId));

        poll(requestParams(hearingResultDetailsUrl, CONTENT_TYPE_HEARING_INFORMATION_DETAILS).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(OK),
                        payload().isJson(allOf(matchers)
                        ));

    }

    public static void getHearingDetailsForHearingIdAndHearingDate(final UUID hearingId, final LocalDate hearingDate, final Matcher... matchers) {
        final String hearingResultDetailsUrl = format("%s%s", BASE_URI,
                getProperty("results.get-hearing-information-details-for-hearing-and-hearingdate",
                        hearingId, hearingDate.toString()));

        poll(requestParams(hearingResultDetailsUrl, CONTENT_TYPE_HEARING_INFORMATION_DETAILS).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(OK),
                        payload().isJson(allOf(matchers)
                        ));

    }

    public static void verifyQueryDefendantTrackingStatus(final String defendantIds, final Matcher... matchers) {
        final String trackingStatusResultUrl = format("%s%s", BASE_URI,
                getProperty("results.get-defendants-tracking-status", defendantIds));

        pollWithDefaults(requestParams(trackingStatusResultUrl, CONTENT_TYPE_DEFENDANT_TRACKING_STATUS).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(matchers))
                );

    }

    public static void thenReturnsBadRequestForResultsSummaryWithoutFromDate() {
        final String resultSummaryUrlWithoutFromDateParameter = format("%s%s", BASE_URI, getProperty(GET_RESULTS_SUMMARY));
        final Response resultsSummaryResponse = new RestClient()
                .query(resultSummaryUrlWithoutFromDateParameter,
                        CONTENT_TYPE_RESULTS_SUMMARY,
                        getUserHeader(getLoggedInUser()));

        assertThatResponseIndicatesBadRequest(resultsSummaryResponse);
    }

    private static void assertThatResponseIndicatesBadRequest(final Response response) {
        assertResponseStatusCode(SC_BAD_REQUEST, response);
    }

    private static void assertResponseStatusCode(final int statusCode, final Response response) {
        assertThat(response.getStatus(), CoreMatchers.equalTo(statusCode));
    }

    private static JsonObject convertToJsonObject(final Object input) {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        try {
            return mapper.readValue(mapper.writeValueAsString(input), JsonObject.class);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Error while trying to convert.");
        }
    }

    public static void hearingResultsHaveBeenSharedV2(final PublicHearingResulted shareResultsMessage) {
        hearingResultsHaveBeenSharedV2(shareResultsMessage, true);
    }

    public static void hearingResultsHaveBeenSharedV2WithoutPoliceResultGenerated(final PublicHearingResulted shareResultsMessage) {
        hearingResultsHaveBeenSharedV2(shareResultsMessage, false);
    }

    public static void hearingResultsHaveBeenSharedV2(final PublicHearingResulted shareResultsMessage, final boolean listenToPublicEventPoliceResultGenerated) {
        if (listenToPublicEventPoliceResultGenerated) {
            startListeningToPublicEventPoliceResultsGenerated();
        }
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);

            final JsonObject payload = convertToJsonObject(shareResultsMessage);

            messageProducer.sendMessage(PUBLIC_EVENT_HEARING_RESULTED_V2, payload);
        }
    }

    public static void hearingResultsHaveBeenSharedV2(final JsonObject payload) {
        startListeningToPublicEventPoliceResultsGenerated();
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);

            messageProducer.sendMessage(PUBLIC_EVENT_HEARING_RESULTED_V2, payload);
        }
    }

    public static void verifyPublicEventForPoliceResultsGenerated(final boolean eventExpected) throws JMSException {
        final Optional<String> policeResultGenerated = publicMessageConsumerPoliceResultsGenerated.retrieveMessage();

        if (eventExpected) {
            assertThat(policeResultGenerated, not(empty()));
        } else {
            assertThat(policeResultGenerated, is(empty()));
        }
    }

    public static void verifyPublicEventPoliceResultsGenerated() throws JMSException {
        verifyPublicEventPoliceResultsGenerated(Boolean.FALSE, null, null, null);
    }

    public static void verifyPublicEventPoliceResultsGenerated(final Boolean isGroupProceedings, final String groupId,
                                                               final List<Boolean> isGroupMember, final List<Boolean> isGroupMaster) throws JMSException {
        verifyPublicEventPoliceResultsGenerated(true, isGroupProceedings, groupId, isGroupMember, isGroupMaster);
    }

    public static void verifyPublicEventPoliceResultsGenerated(final boolean includePoliceResults, final Boolean isGroupProceedings, final String groupId,
                                                               final List<Boolean> isGroupMember, final List<Boolean> isGroupMaster) {
        if (includePoliceResults) {
            if (isGroupProceedings) {
                for (int i = 0; i < isGroupMember.size(); i++) {
                    final Optional<String> response = publicMessageConsumerPoliceResultsGenerated.retrieveMessage();
                    assertThat(response, not(empty()));
                    assertThat(response.get().contains(CIVIL_OFFENCE), is(true));
                }
            } else {
                final Optional<String> response = publicMessageConsumerPoliceResultsGenerated.retrieveMessage();
                assertThat(response, not(empty()));
            }
        }
    }

    public static Optional<String> verifyPublicEventPoliceResultGeneratedAndReturnPayload() {
        return publicMessageConsumerPoliceResultsGenerated.retrieveMessage();
    }

    public static void verifyPublicEventPoliceResultGeneratedMessage(final String expectedFindingValue) {

        final String eventStr = publicMessageConsumerPoliceResultsGenerated.retrieveMessage().orElse(null);
        assertThat(eventStr, notNullValue());

        final JSONObject event = new JSONObject(eventStr);
        assertThat(event.getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).getString("finding"), is(expectedFindingValue));

    }

    public static void verifyPublicEventPoliceResultGeneratedNotRaised() {
        final Optional<String> response = publicMessageConsumerPoliceResultsGenerated.retrieveMessage(RETRIEVE_TIMEOUT);
        assertThat(response, is(empty()));
    }

    public static void verifyAppealUpdateEmail(final String url){
        await().atMost(30, SECONDS).pollInterval(5, SECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(url));
            Arrays.asList("Appeal Update").forEach(
                    expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
            );
            verify(requestPatternBuilder);
            return true;
        });
    }

    public static void closeMessageConsumers() {
        publicMessageConsumerPoliceResultsGenerated.close();
    }

    public static void getInternalHearingDetailsForHearingId(final UUID hearingId, final Matcher... matchers) {

        final String url = format("%s%s", BASE_URI,
                getProperty("results.get-hearing-details-internal", hearingId));

        pollWithDefaults(requestParams(url, CONTENT_TYPE_HEARING_DETAILS_INTERNAL).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(matchers))
                );

    }

    public static void whenResultsAreTraced(final String payload) {
        setupAsSystemUser(getUserId());
        trackResultsCommand(payload);
    }

    public static void getEmailNotificationDetails(final UUID userId, final UUID materialId, final Matcher... matchers) {

        final String url = format("%s%s", BASE_URI,
                getProperty("results.get-email-notification-details", materialId));

        pollWithDefaults(requestParams(url, CONTENT_TYPE_NCES_EMAIL_NOTIFICATION_DETAILS).withHeader(USER_ID, userId).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(matchers))
                );
    }

    private static void startListeningToPublicEventPoliceResultsGenerated() {
        publicMessageConsumerPoliceResultsGenerated.startConsumer("public.results.police-result-generated", "jms.topic.public.event");
    }

}
