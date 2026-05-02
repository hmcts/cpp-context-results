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
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.results.it.helper.InformantRegisterDocumentRequestHelper.recordInformantRegister;
import static uk.gov.moj.cpp.results.it.helper.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenSharedV2WithoutPoliceResultGenerated;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyAppealUpdateEmail;
import static uk.gov.moj.cpp.results.it.stub.NotificationNotifyServiceStub.NOTIFICATION_NOTIFY_ENDPOINT;
import static uk.gov.moj.cpp.results.it.stub.NotificationNotifyServiceStub.setupNotificationNotifyStubs;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrganisationUnit;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubPoliceFlag;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubSpiOutFlag;
import static uk.gov.moj.cpp.results.it.utils.UriConstants.BASE_URI;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsTemplateWithAppealFlag;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ProsecutorResultsIT {

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

    @Test
    public void testAppealApplicationEmailNotificationSuccess() {
        stubGetOrganisationUnit();
        createMessageConsumers();
        setupNotificationNotifyStubs();
        ReferenceDataServiceStub.stubCountryNationalities();

        final PublicHearingResulted resultsMessage = basicShareResultsTemplateWithAppealFlag(JurisdictionType.CROWN, true);
        resultsMessage.setIsReshare(Optional.of(Boolean.FALSE));
        resultsMessage.setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        resultsMessage.setHearingDay(Optional.of(LocalDate.now()));

        stubSpiOutFlag(true, true, "CCSU@derbyshire.pnn.police.uk");
        stubPoliceFlag("DERPF", "DERPF");
        hearingResultsHaveBeenSharedV2WithoutPoliceResultGenerated(resultsMessage);

        verifyAppealUpdateEmail(NOTIFICATION_NOTIFY_ENDPOINT);
    }

    private void validateProsecutorResults(final String ouCode, final String startDate, final String endDate, final javax.ws.rs.core.Response.Status status, final ResponsePayloadMatcher responsePayloadMatcher) {
        final String url = BASE_URI + format("/results-query-api/query/api/rest/results/prosecutor/%s?startDate=%s%s", ouCode, startDate, Objects.nonNull(endDate) ? "&endDate=" + endDate : "");

        pollWithDefaults(requestParams(url, "application/vnd.results.prosecutor-results+json")
                .withHeader(USER_ID, randomUUID()).build())
                .until(
                        status().is(status),
                        responsePayloadMatcher
                );
    }
}
