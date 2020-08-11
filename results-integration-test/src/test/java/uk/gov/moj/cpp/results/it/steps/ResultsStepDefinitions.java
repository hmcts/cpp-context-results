package uk.gov.moj.cpp.results.it.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.AllOf.allOf;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.results.it.utils.QueueUtilForPrivateEvents.RETRIEVE_TIMEOUT;
import static uk.gov.moj.cpp.results.it.utils.QueueUtilForPrivateEvents.privateEvents;

import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.sjp.results.PublicSjpResulted;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.it.utils.WireMockStubUtils;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.test.matchers.MapJsonObjectToTypeMatcher;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class ResultsStepDefinitions extends AbstractStepDefinitions {

    private static final String CONTENT_TYPE_HEARING_DETAILS = "application/vnd.results.hearing-details+json";
    private static final String CONTENT_TYPE_HEARING_INFORMATION_DETAILS = "application/vnd.results.hearing-information-details+json";
    private static final String CONTENT_TYPE_RESULTS_DETAILS = "application/vnd.results.results-details+json";
    private static final String CONTENT_TYPE_HEARING_DETAILS_INTERNAL = "application/vnd.results.hearing-details-internal+json";
    private static final String CONTENT_TYPE_RESULTS_SUMMARY = "application/vnd.results.results-summary+json";
    private static final String RESULTS_EVENT_SESSION_ADDED_EVENT = "results.event.session-added-event";
    private static final String RESULTS_EVENT_CASE_REJECTED_EVENT = "results.event.sjp-case-rejected-event";
    private static final String RESULTS_EVENT_CASE_ADDED_EVENT = "results.event.case-added-event";
    private static final String RESULTS_EVENT_DEFENDANT_ADDED_EVENT = "results.event.defendant-added-event";
    private static final String RESULTS_EVENT_DEFENDANT_UPDATED_EVENT = "results.event.defendant-updated-event";
    private static final String RESULTS_EVENT_DEFENDANT_REJECTED_EVENT = "results.event.defendant-rejected-event";
    private static final String RESULTS_EVENT_POLICE_RESULT_GENERATED = "results.event.police-result-generated";
    private static final String RESULTS_EVENT_POLICE_NOTIFICATION_REQUESTED = "results.event.police-notification-requested";
    private static final String RESULTS_EVENT_POLICE_NOTIFICATION_FAILED = "results.event.email-notification-failed";


    private static final String PUBLIC_EVENT_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_EVENT_SJP_RESULTED = "public.sjp.case-resulted";
    private static final String GET_RESULTS_SUMMARY = "results.get-results-summary";

    private static MessageConsumer privateSessionAddedEventConsumer;
    private static MessageConsumer privateCaseRejectedEventConsumer;
    private static MessageConsumer privateCaseAddedEventConsumer;
    private static MessageConsumer privateDefendantAddedEventConsumer;
    private static MessageConsumer privateDefendantUpdatedEventConsumer;
    private static MessageConsumer privateDefendantRejectedEventConsumer;
    private static MessageConsumer privatePoliceResultGeneratedConsumer;
    private static MessageConsumer privatePoliceNotificationRequestedConsumer;
    private static MessageConsumer privatePoliceNotificationFailedConsumer;
    private static MessageConsumerClient publicMessageConsumer;


    public static void createMessageConsumers() {
        privateSessionAddedEventConsumer = privateEvents.createConsumer(RESULTS_EVENT_SESSION_ADDED_EVENT);
        privateCaseRejectedEventConsumer = privateEvents.createConsumer(RESULTS_EVENT_CASE_REJECTED_EVENT);
        privateCaseAddedEventConsumer = privateEvents.createConsumer(RESULTS_EVENT_CASE_ADDED_EVENT);
        privateDefendantAddedEventConsumer = privateEvents.createConsumer(RESULTS_EVENT_DEFENDANT_ADDED_EVENT);
        privateDefendantUpdatedEventConsumer = privateEvents.createConsumer(RESULTS_EVENT_DEFENDANT_UPDATED_EVENT);
        privateDefendantRejectedEventConsumer = privateEvents.createConsumer(RESULTS_EVENT_DEFENDANT_REJECTED_EVENT);
        privatePoliceResultGeneratedConsumer = privateEvents.createConsumer(RESULTS_EVENT_POLICE_RESULT_GENERATED);
        privatePoliceNotificationRequestedConsumer = privateEvents.createConsumer(RESULTS_EVENT_POLICE_NOTIFICATION_REQUESTED);
        privatePoliceNotificationFailedConsumer = privateEvents.createConsumer(RESULTS_EVENT_POLICE_NOTIFICATION_FAILED);
        publicMessageConsumer = new MessageConsumerClient();
    }

    public static void whenPrisonAdminTriesToViewResultsForThePerson(final UUID userId) {
        WireMockStubUtils.setupUserAsPrisonAdminGroup(userId);
        setLoggedInUser(userId);
    }

    public static HearingResultSummariesView getSummariesByDate(final LocalDate fromDate) {
        final String resultSummaryUrl = format("%s%s?fromDate=%s", BASE_URI, getProperty(GET_RESULTS_SUMMARY), fromDate);
        final RequestParamsBuilder resultsSummaryRequest = requestParams(resultSummaryUrl,
                CONTENT_TYPE_RESULTS_SUMMARY)
                .withHeader(USER_ID, getLoggedInUser());

        final ResponseData resultsSummaryResponse = poll(resultsSummaryRequest)
                .until(
                        status().is(OK)
                );
        return MapJsonObjectToTypeMatcher.convert(HearingResultSummariesView.class, resultsSummaryResponse.getPayload());
    }

    private static <T> Matcher<ResponseData> jsonPayloadMatcher(final Class<T> theClass, final Matcher<T> matcher) {
        return new BaseMatcher<ResponseData>() {
            @Override
            public boolean matches(final Object o) {
                if (o instanceof ResponseData) {
                    final ResponseData responseData = (ResponseData) o;
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

        poll(requestParams(personHearingResultDetailsUrl, CONTENT_TYPE_HEARING_DETAILS).withHeader(USER_ID, getLoggedInUser())).until(
                print(),
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
                        payload().isJson(CoreMatchers.allOf(matchers)
                        ));

    }

    public static void thenReturnsBadRequestForResultsSummaryWithoutFromDate() {
        final String resultSummaryUrlWithoutFromDateParameter = format("%s%s", BASE_URI, getProperty(GET_RESULTS_SUMMARY));
        final Response resultsSummaryResponse = new RestClient()
                .query(resultSummaryUrlWithoutFromDateParameter,
                        CONTENT_TYPE_RESULTS_SUMMARY,
                        getUserHeader(getLoggedInUser()));

        assertThatResponseIndicatesBadRequest(resultsSummaryResponse);
    }

    public static void hearingResultsHaveBeenShared(final PublicHearingResulted shareResultsMessage) {
        publicMessageConsumer.startConsumer("public.results.police-result-generated", "public.event");
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);

            final JsonObject payload = convertToJsonObject(shareResultsMessage);

            messageProducer.sendMessage(PUBLIC_EVENT_HEARING_RESULTED, payload);
        }
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

    public static void hearingResultsHaveBeenShared(final JsonObject payload) {
        publicMessageConsumer.startConsumer("public.results.police-result-generated", "public.event");
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);

            messageProducer.sendMessage(PUBLIC_EVENT_HEARING_RESULTED, payload);
        }
    }

    public static void publicSjpResultedShared(final PublicSjpResulted sjpResultedMessage) {
        publicMessageConsumer.startConsumer("public.results.police-result-generated", "public.event");
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);

            final JsonObject payload = convertToJsonObject(sjpResultedMessage);

            messageProducer.sendMessage(PUBLIC_EVENT_SJP_RESULTED, payload);
        }
    }

    public static void verifyPrivateEventsForPoliceGenerateResultsForDefendant() throws JMSException {
        verifyPrivateEventsForPoliceGenerateResultsForDefendant(true);
    }

    public static void verifyPrivateEventsForPoliceGenerateResultsForDefendant(final boolean eventExpected) throws JMSException {
        final Message policeResultGenerated = privatePoliceResultGeneratedConsumer.receive(RETRIEVE_TIMEOUT);

        if (eventExpected) {
            assertThat(policeResultGenerated, notNullValue());
        } else {
            assertThat(policeResultGenerated, nullValue());
        }
    }

    public static void verifyPrivateEventsWithPoliceResultGenerated() throws JMSException {
        verifyPrivateEventsWithPoliceResultGenerated(true);
    }

    public static void verifyPrivateEventsWithPoliceResultGenerated(final boolean includePoliceResults) throws JMSException {
        final Message sessionAdded = privateSessionAddedEventConsumer.receive(RETRIEVE_TIMEOUT);
        assertThat(sessionAdded, notNullValue());

        final Message caseAdded = privateCaseAddedEventConsumer.receive(RETRIEVE_TIMEOUT);
        assertThat(caseAdded, notNullValue());

        final Message defendantAdded = privateDefendantAddedEventConsumer.receive(RETRIEVE_TIMEOUT);
        assertThat(defendantAdded, notNullValue());

        if (includePoliceResults) {
            final Message policeResultGenerated = privatePoliceResultGeneratedConsumer.receive(RETRIEVE_TIMEOUT);
            assertThat(policeResultGenerated, notNullValue());
        }
    }

    public static void verifyPrivateEventsWithPoliceNotificationRequested(final boolean isSuccessExpected) throws JMSException {
        final Message sessionAdded = privateSessionAddedEventConsumer.receive(RETRIEVE_TIMEOUT);
        assertThat(sessionAdded, notNullValue());

        final Message caseAdded = privateCaseAddedEventConsumer.receive(RETRIEVE_TIMEOUT);
        assertThat(caseAdded, notNullValue());

        final Message defendantAdded = privateDefendantAddedEventConsumer.receive(RETRIEVE_TIMEOUT);
        assertThat(defendantAdded, notNullValue());

        if(isSuccessExpected){
            final Message policeNotificationRequestedGenerated = privatePoliceNotificationRequestedConsumer.receive(RETRIEVE_TIMEOUT);
            assertThat(policeNotificationRequestedGenerated, notNullValue());
        }else{
            final Message policeNotificationFailedGenerated = privatePoliceNotificationFailedConsumer.receive(RETRIEVE_TIMEOUT);
            assertThat(policeNotificationFailedGenerated, notNullValue());
        }


    }

    public static void verifyPrivateEventsForAmendment() throws JMSException {
        final Message defendantAdded = privateDefendantUpdatedEventConsumer.receive(RETRIEVE_TIMEOUT);
        assertThat(defendantAdded, notNullValue());

        final Message policeResultGenerated = privatePoliceResultGeneratedConsumer.receive(RETRIEVE_TIMEOUT);
        assertThat(policeResultGenerated, notNullValue());
    }

    public static void verifyPrivateEventsForRejected() throws JMSException {
        final Message sessionAdded = privateSessionAddedEventConsumer.receive(RETRIEVE_TIMEOUT);
        assertThat(sessionAdded, notNullValue());

        final Message caseAdded = privateCaseAddedEventConsumer.receive(RETRIEVE_TIMEOUT);
        assertThat(caseAdded, notNullValue());

        final Message defendantRejected = privateDefendantRejectedEventConsumer.receive(RETRIEVE_TIMEOUT);
        assertThat(defendantRejected, notNullValue());
    }

    public static void verifyPrivateEventsForCaseRejectedSjp() throws JMSException {
        final Message caseRejected = privateCaseRejectedEventConsumer.receive(RETRIEVE_TIMEOUT);
        assertThat(caseRejected, notNullValue());
    }

    public static Optional<String> verifyInPublicTopic() {
        final Optional<String> response = publicMessageConsumer.retrieveMessage();
        assertThat(response, not(empty()));
        return response;
    }

    public static void verifyNotInPublicTopic() {
        final Optional<String> response = publicMessageConsumer.retrieveMessage(RETRIEVE_TIMEOUT);
        assertThat(response, is(empty()));
    }

    public static void closeMessageConsumers() throws JMSException {
        privateSessionAddedEventConsumer.close();
        privateCaseAddedEventConsumer.close();
        privateDefendantAddedEventConsumer.close();
        privatePoliceResultGeneratedConsumer.close();
        privateDefendantRejectedEventConsumer.close();
        privateDefendantUpdatedEventConsumer.close();
        privateCaseRejectedEventConsumer.close();
        privatePoliceNotificationRequestedConsumer.close();
        privatePoliceNotificationFailedConsumer.close();
        publicMessageConsumer.close();
    }

    public static void getInternalHearingDetailsForHearingId(final UUID hearingId, final Matcher... matchers) {

        final String url = format("%s%s", BASE_URI,
                getProperty("results.get-hearing-details-internal", hearingId));

        poll(requestParams(url, CONTENT_TYPE_HEARING_DETAILS_INTERNAL).withHeader(USER_ID, getLoggedInUser())).until(
                print(),
                status().is(OK),
                payload().isJson(CoreMatchers.allOf(matchers))
        );

    }

}
