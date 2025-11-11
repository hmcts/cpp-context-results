package uk.gov.moj.cpp.results.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.NOTIFIED;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.RECORDED;
import static uk.gov.moj.cpp.results.it.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.results.it.helper.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.results.it.utils.UriConstants.BASE_URI;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.google.common.base.Joiner;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;

public class InformantRegisterDocumentRequestHelper {

    public static final String USER_ID = randomUUID().toString();

    private static final String WRITE_BASE_URL = "/results-service/command/api/rest/results";
    private static final String READ_BASE_URL = "/results-service/query/api/rest/results";


    public InformantRegisterDocumentRequestHelper() {
    }

    public static String getWriteUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, WRITE_BASE_URL, resource);
    }

    public static String getReadUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, READ_BASE_URL, resource);
    }

    public void verifyInformantRegisterRequestsExists(final UUID prosecutionAuthorityId) {
        verifyInformantRegisterRequestsExists(prosecutionAuthorityId, false, 0);
    }

    public void verifyInformantRegisterRequestsExists(final UUID prosecutionAuthorityId, final boolean isGroup, final int defCount) {
        String payload = getInformantRegisterDocumentRequests(RECORDED.name(), allOf(
                withJsonPath("$.informantRegisterDocumentRequests[*].prosecutionAuthorityId", hasItem(prosecutionAuthorityId.toString())),
                withJsonPath("$.informantRegisterDocumentRequests[*].status", hasItem(RECORDED.name()))
        ));

        if (isGroup) {
            final int multiple = countMatches(payload, "groupId");
            for (int i = 0; i < defCount; i++) {
                assertThat(countMatches(payload, ("person" + i + "Address1")), is(multiple));
                assertThat(countMatches(payload, ("person" + i + "FirstName")), is(2 * multiple));
                assertThat(countMatches(payload, ("person" + i + "LastName")), is(2 * multiple));
            }

            assertThat(countMatches(payload, "MASTER_CASE_ASN"), is(multiple));
            assertThat(countMatches(payload, "MASTER_CASE_OFFENCE_CODE"), is(defCount * multiple));
            assertThat(countMatches(payload, "MASTER_CASE_RESULT_TEXT"), is(defCount * multiple));
            assertThat(countMatches(payload, "MASTER_CASE_OFFENCE_RESULT_TEXT"), is(defCount * multiple));
        }
    }

    private String getInformantRegisterDocumentRequests(final String status, final Matcher... matchers) {
        return pollWithDefaults(requestParams(getReadUrl(StringUtils.join("/informant-register/request/", status)),
                "application/vnd.results.query.informant-register-document-request+json")
                .withHeader(HeaderConstants.USER_ID, USER_ID).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                matchers
                        )))
                .getPayload();
    }

    public void verifyInformantRegisterIsNotified(final UUID prosecutionAuthorityId) {
        getInformantRegisterDocumentRequests(NOTIFIED.name(), allOf(
                withJsonPath("$.informantRegisterDocumentRequests[*].status", hasItem(NOTIFIED.name())),
                withJsonPath("$.informantRegisterDocumentRequests[*].prosecutionAuthorityId", hasItem(prosecutionAuthorityId.toString()))
        ));
    }

    public static Response recordInformantRegister(final UUID prosecutionAuthorityId, final String prosecutionAuthorityCode, final ZonedDateTime registerDate, final UUID hearingId, final ZonedDateTime hearingDate, final String fileName) throws IOException {
        return recordInformantRegister(prosecutionAuthorityId, prosecutionAuthorityCode, randomAlphanumeric(7), registerDate, hearingId, hearingDate, fileName, null);
    }

    public static Response recordInformantRegister(final UUID prosecutionAuthorityId, final String prosecutionAuthorityCode, final String prosecutionAuthorityOuCode, final ZonedDateTime registerDate, final UUID hearingId, final ZonedDateTime hearingDate, final String fileName) throws IOException {
        return recordInformantRegister(prosecutionAuthorityId, prosecutionAuthorityCode, prosecutionAuthorityOuCode, registerDate, hearingId, hearingDate, fileName, null);
    }

    public static Response recordInformantRegister(final UUID prosecutionAuthorityId, final String prosecutionAuthorityCode, final String prosecutionAuthorityOuCode, final ZonedDateTime registerDate, final UUID hearingId, final ZonedDateTime hearingDate, final String fileName, final UUID groupId) {
        final String body = getPayload(fileName)
                .replaceAll("%PROSECUTION_AUTHORITY_ID%", prosecutionAuthorityId.toString())
                .replaceAll("%PROSECUTION_AUTHORITY_CODE%", prosecutionAuthorityCode)
                .replaceAll("%PROSECUTION_AUTHORITY_OU_CODE%", prosecutionAuthorityOuCode)
                .replaceAll("%REGISTER_DATE%", registerDate.toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%HEARING_DATE%", hearingDate.toString())
                .replaceAll("%GROUP_ID%", nonNull(groupId) ? groupId.toString() : EMPTY);

        return postCommand(getWriteUrl("/informant-register"),
                "application/vnd.results.add-informant-register+json",
                body);
    }
}
