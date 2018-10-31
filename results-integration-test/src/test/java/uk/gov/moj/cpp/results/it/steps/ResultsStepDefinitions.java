package uk.gov.moj.cpp.results.it.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.domains.results.shareresults.*;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;
import uk.gov.moj.cpp.results.it.utils.WireMockStubUtils;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.test.matchers.MapJsonObjectToTypeMatcher;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.lang.String.format;
import static java.time.LocalDate.parse;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.AllOf.allOf;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

public class ResultsStepDefinitions extends AbstractStepDefinitions {

    private static final String CONTENT_TYPE_PERSON_DETAILS = "application/vnd.results.person-details+json";
    private static final String CONTENT_TYPE_HEARING_DETAILS = "application/vnd.results.hearing-details+json";
    private static final String CONTENT_TYPE_RESULTS_DETAILS = "application/vnd.results.results-details+json";
    private static final String CONTENT_TYPE_RESULTS_SUMMARY = "application/vnd.results.results-summary+json";

    private static final String PUBLIC_EVENT_HEARING_RESULTED = "public.hearing.resulted";

    private static final String GET_RESULTS_SUMMARY = "results.get-results-summary";

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

    private static <T> Matcher<ResponseData> jsonPayloadMatcher(Class<T> theClass, Matcher<T> matcher) {
        return new BaseMatcher<ResponseData>() {
            @Override
            public boolean matches(final Object o) {
                if (o instanceof ResponseData) {
                    final ResponseData responseData = (ResponseData) o;
                    if (responseData.getPayload() != null) {
                        T object = MapJsonObjectToTypeMatcher.convert(theClass, responseData.getPayload());
                        return matcher.matches(object);
                    }
                }
                return false;
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                ResponseData responseData = (ResponseData) item;
                JsonObject jsonObject = Json.createReader(new StringReader(responseData.getPayload())).readObject();
                matcher.describeMismatch(jsonObject, description);
            }

            @Override
            public void describeTo(final Description description) {
                matcher.describeTo(description);
            }
        };
    }

    public static void  getHearingDetails(final UUID hearingId, final UUID defendantId, final Matcher<HearingResultsAdded> matcher) {
        //TODO change this name
        final String personHearingResultDetailsUrl = format("%s%s", BASE_URI,
                getProperty("results.get-person-hearing-result-details", defendantId,
                        hearingId));

        final Matcher<ResponseData>  responseDataMatcher = jsonPayloadMatcher(HearingResultsAdded.class, matcher);


        poll(requestParams(personHearingResultDetailsUrl, CONTENT_TYPE_HEARING_DETAILS).withHeader(USER_ID, getLoggedInUser())) .until(
                print(),
                status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.hearing.id", equalTo(hearingId.toString()))
                )),
                responseDataMatcher
        );

    }





    public static void thenResultsSummaryShowsHearingWithinDateRange(final LocalDate fromDate) {
        final String resultSummaryUrl = format("%s%s?fromDate=%s", BASE_URI, getProperty(GET_RESULTS_SUMMARY), fromDate);
        final RequestParamsBuilder resultsSummaryRequest = requestParams(resultSummaryUrl,
                CONTENT_TYPE_RESULTS_SUMMARY)
                .withHeader(USER_ID, getLoggedInUser());

        final ResponseData resultsSummaryResponse = poll(resultsSummaryRequest)
                .until(
                        status().is(OK)
                );

        final JsonPath jsonResponse = new JsonPath(resultsSummaryResponse.getPayload());
        final List<String> hearingDates = jsonResponse.getList("results.hearingDate");

        assertThatHearingDatesAreNotBeforeFromDate(hearingDates, fromDate);
    }


    private static JsonObject getResultSummaryJson(final LocalDate fromDate) {
        final String resultSummaryUrl = format("%s%s?fromDate=%s", BASE_URI, getProperty(GET_RESULTS_SUMMARY), fromDate);
        final RequestParamsBuilder resultsSummaryRequest = requestParams(resultSummaryUrl,
                CONTENT_TYPE_RESULTS_SUMMARY)
                .withHeader(USER_ID, getLoggedInUser());

        final ResponseData resultsSummaryResponse = poll(resultsSummaryRequest)
                .until(
                        status().is(OK)
                );
        return new StringToJsonObjectConverter().convert(resultsSummaryResponse.getPayload());
    }

    public static void thenReturnsBadRequestForResultsSummaryWithoutFromDate() {
        final String resultSummaryUrlWithoutFromDateParameter = format("%s%s", BASE_URI, getProperty(GET_RESULTS_SUMMARY));
        final Response resultsSummaryResponse = new RestClient()
                .query(resultSummaryUrlWithoutFromDateParameter,
                        CONTENT_TYPE_RESULTS_SUMMARY,
                        getUserHeader(getLoggedInUser()));

        assertThatResponseIndicatesBadRequest(resultsSummaryResponse);
    }


    public static void givenPersonDetailsAreAvailable(final uk.gov.moj.cpp.results.it.steps.data.people.Person person) {
        WireMockStubUtils.mockPersonDetails(person);
    }

    public static void hearingResultsHaveBeenShared(final PublicHearingResulted shareResultsMessage) {
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);

            final JsonObject payload = convertToJsonObject(shareResultsMessage);

            messageProducer.sendMessage(PUBLIC_EVENT_HEARING_RESULTED, payload);
        }
    }

    private static void assertThatHearingDatesAreNotBeforeFromDate(final List<String> hearingDates, final LocalDate fromDate) {
        assertThat(hearingDates.stream().filter(hearingDate -> parse(hearingDate).isBefore(fromDate)).collect(toList()), hasSize(0));
    }

    private static void assertThatResponseIndicatesBadRequest(final Response response) {
        assertResponseStatusCode(SC_BAD_REQUEST, response);
    }

    private static void assertResponseStatusCode(final int statusCode, final Response response) {
        assertThat(response.getStatus(), CoreMatchers.equalTo(statusCode));
    }

    private static <T> Predicate<T> distinct(final Function<? super T, ?> func) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(func.apply(t));
    }

    private static JsonObject convertToJsonObject(final Object input) {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        try {
            return mapper.readValue(mapper.writeValueAsString(input), JsonObject.class);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Error while trying to convert.");
        }
    }
}
