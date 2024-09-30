package uk.gov.moj.cpp.results.it;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.results.it.helper.InformantRegisterDocumentRequestHelper.getWriteUrl;
import static uk.gov.moj.cpp.results.it.helper.InformantRegisterDocumentRequestHelper.recordInformantRegister;
import static uk.gov.moj.cpp.results.it.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.results.it.utils.ProgressionServiceStub.stubQueryGroupMemberCases;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubNotificationNotifyEndPoint;

import uk.gov.moj.cpp.results.it.helper.InformantRegisterDocumentRequestHelper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InformantRegisterDocumentRequestIT {
    private MessageProducer producer;
    private InformantRegisterDocumentRequestHelper helper;
    private static final UUID GROUP_ID = randomUUID();

    @BeforeAll
    public static void setupStubs() {
        setupUsersGroupQueryStub();
        stubQueryGroupMemberCases(GROUP_ID);
        stubNotificationNotifyEndPoint();
    }

    @BeforeEach
    public void setup() {
        helper = new InformantRegisterDocumentRequestHelper();
        producer = publicEvents.createProducer();
    }

    @AfterEach
    public void tearDown() throws JMSException {
        producer.close();
        helper.closeMessageConsumers();
    }

    @Test
    public void shouldAddInformantRegisterRequest() throws IOException {
        final UUID prosecutionAuthorityId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = now(UTC);
        final ZonedDateTime hearingDate = now(UTC).minusHours(1);
        final String prosecutionAuthorityCode = STRING.next();

        final Response writeResponse = recordInformantRegister(prosecutionAuthorityId, prosecutionAuthorityCode, registerDate, hearingId, hearingDate, "json/informant-register/results.add-informant-register-document-request.json");
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        helper.verifyInformantRegisterDocumentRequestRecordedPrivateTopic(prosecutionAuthorityId.toString());
        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);

        generateInformantRegister();
        helper.verifyInformantRegisterDocumentRequestNotifiedPrivateTopic(prosecutionAuthorityId.toString());
        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId);
    }

    @Test
    public void shouldAddInformantRegisterRequestForGroupCases() throws IOException {
        final UUID prosecutionAuthorityId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = now(UTC);
        final ZonedDateTime hearingDate = now(UTC).minusHours(1);
        final String prosecutionAuthorityCode = STRING.next();

        final Response writeResponse = recordInformantRegister(prosecutionAuthorityId, prosecutionAuthorityCode, randomAlphanumeric(7), registerDate,
                hearingId, hearingDate, "json/informant-register/results.add-informant-register-document-request-with-groupId.json", GROUP_ID);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        helper.verifyInformantRegisterDocumentRequestRecordedPrivateTopic(prosecutionAuthorityId.toString());
        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId, true, 3);

        generateInformantRegister();
        helper.verifyInformantRegisterDocumentRequestNotifiedPrivateTopic(prosecutionAuthorityId.toString());
        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId);
    }

    @Test
    public void shouldGenerateInformantRegisterForLatestHearingSharedRequest() throws IOException {
        final UUID prosecutionAuthorityId = randomUUID();
        final String prosecutionAuthorityCode = STRING.next();

        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingDate = now(UTC).minusHours(1);

        final ZonedDateTime registerDate1 = now(UTC).minusMinutes(3);

        final Response writeResponse1 = recordInformantRegister(prosecutionAuthorityId,
                prosecutionAuthorityCode, registerDate1, hearingId, hearingDate,
                "json/informant-register/results.add-informant-register-document-request.json");

        assertThat(writeResponse1.getStatusCode(), equalTo(SC_ACCEPTED));

        final ZonedDateTime registerDate2 = now(UTC).minusMinutes(2);
        final Response writeResponse2 = recordInformantRegister(prosecutionAuthorityId,
                prosecutionAuthorityCode, registerDate2, hearingId, hearingDate,
                "json/informant-register/results.add-informant-register-document-request.json");

        assertThat(writeResponse2.getStatusCode(), equalTo(SC_ACCEPTED));

        final ZonedDateTime registerDate3 = now(UTC).minusMinutes(1);
        final Response writeResponse3 = recordInformantRegister(prosecutionAuthorityId,
                prosecutionAuthorityCode, registerDate3, hearingId, hearingDate,
                "json/informant-register/results.add-informant-register-document-request.json");

        assertThat(writeResponse3.getStatusCode(), equalTo(SC_ACCEPTED));

        final Response writeResponse4 = recordInformantRegister(prosecutionAuthorityId,
                prosecutionAuthorityCode, registerDate1, randomUUID(), hearingDate,
                "json/informant-register/results.add-informant-register-document-request.json");

        assertThat(writeResponse4.getStatusCode(), equalTo(SC_ACCEPTED));

        final InformantRegisterDocumentRequestHelper helper = new InformantRegisterDocumentRequestHelper();
        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);

        generateInformantRegister();

        helper.verifyInformantRegisterDocumentRequestNotifiedPrivateTopic(prosecutionAuthorityId.toString());
        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId);


        final ZonedDateTime registerDate5 = now(UTC).minusMinutes(3);

        final Response writeResponse5 = recordInformantRegister(prosecutionAuthorityId,
                prosecutionAuthorityCode, registerDate5, hearingId, hearingDate,
                "json/informant-register/results.add-informant-register-document-request.json");

        assertThat(writeResponse5.getStatusCode(), equalTo(SC_ACCEPTED));

        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);
        generateInformantRegister();

        helper.verifyInformantRegisterDocumentRequestNotifiedPrivateTopic(prosecutionAuthorityId.toString());
        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId);

    }

    @Test
    public void shouldGenerateInformantRegistersByDateAndProsecutionAuthorities() throws IOException {
        final UUID prosecutionAuthorityId_1 = randomUUID();
        final UUID prosecutionAuthorityId_2 = randomUUID();
        final UUID hearingId_1 = randomUUID();
        final UUID hearingId_2 = randomUUID();
        final ZonedDateTime registerDate = now(UTC);
        final ZonedDateTime hearingDate = now(UTC).minusHours(1);
        final String prosecutionAuthorityCode_1 = STRING.next();
        final String prosecutionAuthorityCode_2 = STRING.next();
        final String fileName = "json/informant-register/results.add-informant-register-document-request.json";

        final InformantRegisterDocumentRequestHelper helper = new InformantRegisterDocumentRequestHelper();

        recordInformantRegister(prosecutionAuthorityId_1, prosecutionAuthorityCode_1, registerDate, hearingId_1, hearingDate, fileName);
        recordInformantRegister(prosecutionAuthorityId_2, prosecutionAuthorityCode_2, registerDate, hearingId_2, hearingDate, fileName);

        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId_1);
        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId_2);

        generateInformantRegister();

        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId_1);
        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId_2);

        final String generateIRByDateCommandBody = getPayload("json/informant-register/results.generate-informant-register-by-date-and-prosecution.json")
                .replaceAll("%REGISTER_DATE%", registerDate.toLocalDate().toString())
                .replaceAll("%PROSECUTION_AUTHORITY_CODE%", prosecutionAuthorityCode_1 + "," + prosecutionAuthorityCode_2);

        final Response generateRegisterByDateAndProsecutionResponse = postCommand(
                getWriteUrl("/informant-register/generate"),
                "application/vnd.results.generate-informant-register-by-date+json",
                generateIRByDateCommandBody);

        assertThat(generateRegisterByDateAndProsecutionResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId_1);
    }

    @Test
    public void shouldGenerateInformantRegistersOnlyByRequestDate() throws IOException {
        final UUID prosecutionAuthorityId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = now(UTC);
        final ZonedDateTime hearingDate = now(UTC).minusHours(1);
        final String prosecutionAuthorityCode = STRING.next();

        final InformantRegisterDocumentRequestHelper helper = new InformantRegisterDocumentRequestHelper();

        recordInformantRegister(prosecutionAuthorityId, prosecutionAuthorityCode, registerDate, hearingId, hearingDate, "json/informant-register/results.add-informant-register-document-request.json");
        helper.verifyInformantRegisterDocumentRequestRecordedPrivateTopic(prosecutionAuthorityId.toString());

        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);

        generateInformantRegister();
        helper.verifyInformantRegisterDocumentRequestNotifiedPrivateTopic(prosecutionAuthorityId.toString());

        generateInformantRegisterByDate(registerDate.toLocalDate());
        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId);
    }

    @Test
    public void shouldNotSendInformantRegistersNotificationWithoutRecipients() throws IOException {
        final UUID prosecutionAuthorityId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = now(UTC);
        final ZonedDateTime hearingDate = now(UTC).minusHours(1);
        final String prosecutionAuthorityCode = STRING.next();

        final InformantRegisterDocumentRequestHelper helper = new InformantRegisterDocumentRequestHelper();

        recordInformantRegister(prosecutionAuthorityId, prosecutionAuthorityCode, registerDate, hearingId, hearingDate, "json/informant-register/results.add-informant-register-document-request-without-recipients.json");
        helper.verifyInformantRegisterDocumentRequestRecordedPrivateTopic(prosecutionAuthorityId.toString());

        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);

        generateInformantRegister();
        helper.verifyInformantRegisterNotificationIgnoredPrivateTopic(prosecutionAuthorityId.toString());
    }

    @Test
    public void shouldNotSendInformantRegistersNotificationWithoutMatchingTemplate() throws IOException {
        final UUID prosecutionAuthorityId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = now(UTC);
        final ZonedDateTime hearingDate = now(UTC).minusHours(1);
        final String prosecutionAuthorityCode = STRING.next();

        final InformantRegisterDocumentRequestHelper helper = new InformantRegisterDocumentRequestHelper();

        recordInformantRegister(prosecutionAuthorityId, prosecutionAuthorityCode, registerDate, hearingId, hearingDate, "json/informant-register/results.add-informant-register-document-request-without-matching-template.json");
        helper.verifyInformantRegisterDocumentRequestRecordedPrivateTopic(prosecutionAuthorityId.toString());

        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);

        generateInformantRegister();
        helper.verifyInformantRegisterNotificationIgnoredPrivateTopic(prosecutionAuthorityId.toString());
    }


    private void generateInformantRegister() throws IOException {
        final Response generateRegisterResponse = postCommand(
                getWriteUrl("/informant-register/generate"),
                "application/vnd.results.generate-informant-register+json",
                "");
        assertThat(generateRegisterResponse.getStatusCode(), equalTo(SC_ACCEPTED));
    }

    private void generateInformantRegisterByDate(final LocalDate registerDate) throws IOException {
        final String generateCommandBody = getPayload("json/informant-register/results.generate-informant-register-by-date.json")
                .replaceAll("%REGISTER_DATE%", registerDate.toString());

        final Response generateRegisterResponse = postCommand(
                getWriteUrl("/informant-register/generate"),
                "application/vnd.results.generate-informant-register-by-date+json",
                generateCommandBody);
        assertThat(generateRegisterResponse.getStatusCode(), equalTo(SC_ACCEPTED));
    }

}
