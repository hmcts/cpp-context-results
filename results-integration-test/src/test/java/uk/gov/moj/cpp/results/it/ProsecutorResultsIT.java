package uk.gov.moj.cpp.results.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.results.it.helper.InformantRegisterDocumentRequestHelper.recordInformantRegister;
import static uk.gov.moj.cpp.results.it.utils.UriConstants.BASE_URI;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUsersGroupQueryStub;

import uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ProsecutorResultsIT {

    private static final String ERROR_CODE_SHARED_DATE_IN_FUTURE = "SHARED_DATE_IN_FUTURE";
    private static final String ERROR_CODE_SHARED_DATE_RANGE_INVALID = "SHARED_DATE_RANGE_INVALID";

    public static Stream<Arguments> errorScenarioSpecification() {
        return Stream.of(
                // start date, end date, error code

                // start date in future
                Arguments.of(LocalDate.now().plusDays(1).toString(), null, ERROR_CODE_SHARED_DATE_IN_FUTURE),

                // end date in future
                Arguments.of(LocalDate.now().toString(), LocalDate.now().plusDays(1).toString(), ERROR_CODE_SHARED_DATE_IN_FUTURE),

                // date range exceeding last 7 days
                Arguments.of(LocalDate.now().minusDays(8).toString(), LocalDate.now().minusDays(5).toString(), ERROR_CODE_SHARED_DATE_RANGE_INVALID),

                // end date before start date
                Arguments.of(LocalDate.now().minusDays(3).toString(), LocalDate.now().minusDays(5).toString(), ERROR_CODE_SHARED_DATE_RANGE_INVALID),

                // invalid start date
                Arguments.of("dummy", LocalDate.now().minusDays(5).toString(), ERROR_CODE_SHARED_DATE_RANGE_INVALID),

                // invalid end date
                Arguments.of(LocalDate.now().minusDays(5).toString(), "dummy", ERROR_CODE_SHARED_DATE_RANGE_INVALID)
        );
    }

    @BeforeAll
    public static void setupStubs() {
        setupUsersGroupQueryStub();
    }

    @Test
    public void shouldQueryProsecutorResults() throws IOException {
        final UUID prosecutionAuthorityId = randomUUID();
        final String prosecutionAuthorityOuCode = randomAlphanumeric(7);
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = now(UTC);
        final ZonedDateTime hearingDate = now(UTC).minusHours(1);
        final String prosecutionAuthorityCode = STRING.next();

        for (int i = 0; i < 5; i++) {
            final Response writeResponse = recordInformantRegister(prosecutionAuthorityId, prosecutionAuthorityCode, prosecutionAuthorityOuCode, registerDate.minusMinutes(i), hearingId, hearingDate.minusMinutes(i), "json/informant-register/results.add-informant-register-document-request.json");
            assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        }
        for (int i = 0; i < 5; i++) {
            final Response writeResponse = recordInformantRegister(prosecutionAuthorityId, prosecutionAuthorityCode, prosecutionAuthorityOuCode, registerDate.minusDays(1).minusMinutes(i), hearingId, hearingDate.minusDays(1).minusMinutes(i), "json/informant-register/results.add-informant-register-document-request.json");
            assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        }

        final ResponsePayloadMatcher responsePayloadMatcherForValidOuCode = payload().isJson(allOf(
                withJsonPath("$.prosecutionAuthorityId", is(prosecutionAuthorityId.toString())),
                withJsonPath("$.prosecutionAuthorityCode", is(prosecutionAuthorityCode)),
                withJsonPath("$.startDate", is(registerDate.toLocalDate().toString())),
                withoutJsonPath("$.endDate"),
                withJsonPath("$.hearingVenues", hasSize(5)))
        );

        validateProsecutorResults(prosecutionAuthorityOuCode, registerDate.toLocalDate().toString(), null, javax.ws.rs.core.Response.Status.OK, responsePayloadMatcherForValidOuCode);

        final ResponsePayloadMatcher responsePayloadMatcherForValidOuCodeWithDateRange = payload().isJson(allOf(
                withJsonPath("$.prosecutionAuthorityId", is(prosecutionAuthorityId.toString())),
                withJsonPath("$.prosecutionAuthorityCode", is(prosecutionAuthorityCode)),
                withJsonPath("$.startDate", is(registerDate.minusDays(1).toLocalDate().toString())),
                withJsonPath("$.endDate", is(registerDate.toLocalDate().toString())),
                withJsonPath("$.hearingVenues", hasSize(10)))
        );

        validateProsecutorResults(prosecutionAuthorityOuCode, registerDate.minusDays(1).toLocalDate().toString(), registerDate.toLocalDate().toString(), javax.ws.rs.core.Response.Status.OK, responsePayloadMatcherForValidOuCodeWithDateRange);


        final ResponsePayloadMatcher responsePayloadMatcherForUnknownOuCode = payload().isJson(allOf(
                withoutJsonPath("$.prosecutionAuthorityId"),
                withoutJsonPath("$.prosecutionAuthorityCode"),
                withJsonPath("$.startDate", is(registerDate.toLocalDate().toString())),
                withoutJsonPath("$.endDate"),
                withJsonPath("$.hearingVenues", hasSize(0)))
        );
        validateProsecutorResults(randomAlphanumeric(7), registerDate.toLocalDate().toString(), null, javax.ws.rs.core.Response.Status.OK, responsePayloadMatcherForUnknownOuCode);

    }

    @MethodSource("errorScenarioSpecification")
    @ParameterizedTest
    public void shouldQueryProsecutorResults_BadRequestCheck(final String startDate, final String endDate, final String errorCode) {
        final String prosecutionAuthorityOuCode = randomAlphanumeric(7);

        final ResponsePayloadMatcher responsePayloadMatcherStartDateInFuture = payload().isJson(allOf(
                withJsonPath("$.error", is(errorCode)))
        );

        validateProsecutorResults(prosecutionAuthorityOuCode, startDate, endDate, javax.ws.rs.core.Response.Status.BAD_REQUEST, responsePayloadMatcherStartDateInFuture);

    }

    private void validateProsecutorResults(final String ouCode, final String startDate, final String endDate, final javax.ws.rs.core.Response.Status status, final ResponsePayloadMatcher responsePayloadMatcher) {
        final String url = BASE_URI + format("/results-query-api/query/api/rest/results/prosecutor/%s?startDate=%s%s", ouCode, startDate, Objects.nonNull(endDate) ? "&endDate=" + endDate : "");

        poll(requestParams(url, "application/vnd.results.prosecutor-results+json")
                .withHeader(USER_ID, randomUUID()))
                .until(
                        status().is(status),
                        responsePayloadMatcher
                );
    }
}
