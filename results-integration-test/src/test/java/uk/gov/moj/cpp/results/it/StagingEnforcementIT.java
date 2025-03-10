package uk.gov.moj.cpp.results.it;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenSharedV2;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenResultsAreTraced;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.stub.DocumentGeneratorStub.stubDocumentCreateWithStatusOk;
import static uk.gov.moj.cpp.results.it.stub.ProgressionStub.stubGetProgressionCaseExistsByUrn;
import static uk.gov.moj.cpp.results.it.stub.SjpStub.setupSjpQueryStub;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.convertStringToJson;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.removeMessagesFromQueue;
import static uk.gov.moj.cpp.results.it.utils.QueueUtilForPrivateEvents.privateEvents;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubDocGeneratorEndPoint;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubDocumentCreate;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubMaterialUploadFile;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubNotificationNotifyEndPoint;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.results.it.utils.QueueUtil;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StagingEnforcementIT {

    private static final String TEMPLATE_PAYLOAD_FOR_STAGING = "json/public.events.hearing.hearing-resulted_for_staging_enforcement.json";
    private static final String TEMPLATE_PAYLOAD_FOR_STAGING_FOR_SJP = "json/public.events.hearing.hearing-resulted_for_staging_enforcement_for_sjp.json";

    public static final String CORRELATION_ID_AND_MASTERDEFENDANT_ADDED = "results.event.correlation-id-and-masterdefendant-added";
    public static final String CORRELATION_ID = "correlationId";
    public static final String MASTER_DEFENDANT_ID = "masterDefendantId";
    public static final String REQUEST_ID = "requestId";
    public static final String EXPORT_STATUS = "exportStatus";
    public static final String UPDATED = "updated";
    public static final String ACKNOWLEDGEMENT = "acknowledgement";
    public static final String ACCOUNT_NUMBER = "accountNumber";
    public static final String SUBJECT = "subject";
    public static final String DEFENDANT_NAME = "defendantName";
    public static final String APPLICATION_RESULT = "applicationResult";
    public static final String SEND_TO = "sendTo";
    public static final String GOB_ACCOUNT_NUMBER = "gobAccountNumber";
    public static final String OLD_GOB_ACCOUNT_NUMBER = "oldGobAccountNumber";
    public static final String CASE_REFERENCE = "caseReferences";
    public static final String DIVISION_CODE = "divisionCode";
    public static final String OLD_DIVISION_CODE = "oldDivisionCode";
    public static final String LISTED_DATE = "listedDate";
    public static final String APPEAL_APPLICATION_RECEIVED = "APPEAL APPLICATION RECEIVED";
    public static final String APPEAL_APPLICATION_GRANTED = "APPEAL GRANTED";
    public static final String APPLICATION_TO_REOPEN_GRANTED = "APPLICATION TO REOPEN GRANTED";

    public static final String APPEAL_APPLICATION_UPDATED = "APPEAL APPLICATION UPDATED";
    public static final String IMPOSITION_OFFENCE_DETAILS = "impositionOffenceDetails";
    private static final String MATERIAL_ID = "materialId";

    private final String HEARING_SITTING_DAY = "hearingSittingDay";
    private final String HEARING_COURT_CENTRE_NAME = "hearingCourtCentreName";
    private final String DEFENDANT_DATE_OF_BIRTH = "defendantDateOfBirth";
    private final String DEFENDANT_ADDRESS = "defendantAddress";
    private final String DEFENDANT_EMAIL = "defendantEmail";
    private final String DEFENDANT_CONTACT_NUMBER = "defendantContactNumber";
    private static final String HEARING_SITTING_DAY_PATTERN = "yyyy-MM-dd";

    private static final String PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT = "public.stagingenforcement.enforce-financial-imposition-acknowledgement";
    private static final String PUBLIC_EVENT_SEND_NCES_EMAIL_FOR_NEW_APPLICATION = "public.hearing.nces-email-notification-for-application";
    private static final String HEARING_FINANCIAL_RESULT_UPDATED = "results.event.hearing-financial-results-updated";
    private static final String SJP_UPLOAD_CASE_DOCUMENT = "sjp.upload-case-document";
    private static final String PRIVATE_EMAIL_EVENT = "results.event.nces-email-notification-requested";
    public static final String WRITE_OFF_ONE_DAY_DEEMED_SERVED = "WRITE OFF ONE DAY DEEMED SERVED";
    public static final String AMEND_AND_RESHARE = "AMEND AND RESHARE- DUPLICATE ACCOUNT: WRITE OFF REQUIRED";
    private static final String PUBLIC_EVENT_TOPIC = "public.event";
    private static final String ACON_SUBJECT = "ACCOUNTS TO BE CONSOLIDATED";

    private static final String TRACE_RESULT = "json/results.api.trace-results.json";
    private static final String TRACE_RESULT_SJP = "json/results.api.trace-results-sjp.json";
    private static final String TRACE_RESULT_GRANTED = "json/results.api.granted-application.json";
    private static final String TRACE_RESULT_GRANTED_SJP = "json/results.api.granted-application-sjp.json";
    private static final String TRACE_RESULT_APPLICATION_TO_REOPEN_GRANTED = "json/results.api.application-reopen-granted.json";
    private static final String TRACE_RESULT_UPDATED = "json/results.api.updated-application.json";
    private static final String TRACE_RESULT_AMENDMENT = "json/results.api.trace-results-amendment.json";
    private static final String REJECTED_APPLICATION = "json/results.api.rejected-application.json";
    public static final String DATE_DECISION_MADE = "dateDecisionMade";

    private static final String TRACE_AMENDED_RESULT = "json/results.api.trace-amended-results.json";

    private final String HEARING_SITTING_DAY_VALUE = "2018-09-30T09:30:00.000Z";
    private final String HEARING_COURT_CENTRE_NAME_VALUE = "South West London Magistrates Court";
    private final String DEFENDANT_DATE_OF_BIRTH_VALUE = "01-01-1988";
    private final String DEFENDANT_ADDRESS_VALUE = "176A Lavender Hill";
    private final String DEFENDANT_EMAIL_VALUE = "defendantEmail@gmail.com";
    private final String DEFENDANT_CONTACT_NUMBER_VALUE = "0206989859";

    static MessageConsumer correlationIdAndMasterDefendantIdAddedConsumer;
    static MessageConsumer hearingFinancialResultsUpdatedConsumer;
    static MessageConsumer sjpUploadCaseDocumentConsumer;
    static MessageConsumer ncesEmailEventConsumer;

    @BeforeAll
    public static void beforeClass() {
        stubDocGeneratorEndPoint();
        stubNotificationNotifyEndPoint();
        stubDocumentCreate(STRING.next());
        stubDocumentCreateWithStatusOk(STRING.next());
        stubMaterialUploadFile();
        setupSjpQueryStub("caseUrn1", randomUUID());
        stubGetProgressionCaseExistsByUrn("32DN1212262", randomUUID());
        correlationIdAndMasterDefendantIdAddedConsumer = privateEvents.createConsumer(CORRELATION_ID_AND_MASTERDEFENDANT_ADDED);
        hearingFinancialResultsUpdatedConsumer = privateEvents.createConsumer(HEARING_FINANCIAL_RESULT_UPDATED);
        sjpUploadCaseDocumentConsumer = privateEvents.createConsumer(SJP_UPLOAD_CASE_DOCUMENT);
        ncesEmailEventConsumer = privateEvents.createConsumer(PRIVATE_EMAIL_EVENT);

    }

    @BeforeEach
    public void setUp() {
        removeMessagesFromQueue(correlationIdAndMasterDefendantIdAddedConsumer);
        removeMessagesFromQueue(hearingFinancialResultsUpdatedConsumer);
        removeMessagesFromQueue(sjpUploadCaseDocumentConsumer);
        removeMessagesFromQueue(ncesEmailEventConsumer);
        createMessageConsumers();
    }

    @AfterAll
    public static void tearDown() throws JMSException {
        correlationIdAndMasterDefendantIdAddedConsumer.close();
        hearingFinancialResultsUpdatedConsumer.close();
        sjpUploadCaseDocumentConsumer.close();
        ncesEmailEventConsumer.close();
    }

    @SuppressWarnings("java:S5961")
    @Test
    public void shouldSendNcesEmailForNewApplicationThatWasRejectedOrGranted() {
        final String masterDefendantId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();
        final String accountNumber = "AER123451";

        final String payload = getPayload(TRACE_RESULT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", accountCorrelationId)
                .replaceAll("DEEMED_SERVED", "false")
                .replaceAll("FINANCIAL", "true")
                .replaceAll("HEARING_SITTING_DAY", HEARING_SITTING_DAY_VALUE)
                .replaceAll("HEARING_COURT_CENTRE_NAME", HEARING_COURT_CENTRE_NAME_VALUE)
                .replaceAll("DEFENDANT_DATE_OF_BIRTH", DEFENDANT_DATE_OF_BIRTH_VALUE)
                .replaceAll("DEFENDANT_ADDRESS", DEFENDANT_ADDRESS_VALUE)
                .replaceAll("DEFENDANT_EMAIL", DEFENDANT_EMAIL_VALUE)
                .replaceAll("DEFENDANT_CONTACT_NUMBER", DEFENDANT_CONTACT_NUMBER_VALUE)
                .replaceAll("HEARING_ID", hearingId);

        whenResultsAreTraced(payload);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(correlationIdAndMasterDefendantIdAddedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));


        final JsonObject stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add(REQUEST_ID, accountCorrelationId)
                .add(EXPORT_STATUS, "ENFORCEMENT_ACKNOWLEDGED")
                .add(UPDATED, "2019-12-01T10:00:00Z")
                .add(ACKNOWLEDGEMENT, createObjectBuilder().add(ACCOUNT_NUMBER, accountNumber).build())
                .build();

        raisePublicEventForAcknowledgement(stagingEnforcementAckPayload, PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT);

        jsonResponse = QueueUtil.retrieveMessage(hearingFinancialResultsUpdatedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(ACCOUNT_NUMBER), is(accountNumber));


        final JsonObject ncesEmailPayload = createObjectBuilder()
                .add("applicationType", "APPEAL")
                .add(MASTER_DEFENDANT_ID, masterDefendantId)
                .add("listingDate", "01/12/2019")
                .add("caseUrns", createArrayBuilder().add("caseUrn1").build())
                .add(HEARING_COURT_CENTRE_NAME, HEARING_COURT_CENTRE_NAME_VALUE)
                .build();
        raisePublicEventForAcknowledgement(ncesEmailPayload, PUBLIC_EVENT_SEND_NCES_EMAIL_FOR_NEW_APPLICATION);

        final List<JsonPath> messages = QueueUtil.retrieveMessages(ncesEmailEventConsumer, 2);
        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase(APPEAL_APPLICATION_RECEIVED)).findFirst().orElseGet(() -> JsonPath.from("{}"));
        assertThat(jsonResponse.getString(SUBJECT), is(APPEAL_APPLICATION_RECEIVED));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("caseUrn1"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString(LISTED_DATE), is("2019-12-01"));
        assertThat(jsonResponse.getString(MATERIAL_ID), is(notNullValue()));
        assertThat(jsonResponse.getString("originalDateOfSentence"), is("30/09/2018"));

        assertThat(jsonResponse.getString(HEARING_SITTING_DAY), is(ZonedDateTime.parse(HEARING_SITTING_DAY_VALUE).format(DateTimeFormatter.ofPattern(HEARING_SITTING_DAY_PATTERN))));
        assertThat(jsonResponse.getString(HEARING_COURT_CENTRE_NAME), is(HEARING_COURT_CENTRE_NAME_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_EMAIL), is(DEFENDANT_EMAIL_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_ADDRESS), is(DEFENDANT_ADDRESS_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_CONTACT_NUMBER), is(DEFENDANT_CONTACT_NUMBER_VALUE));

        final String rejectPayload = getPayload(REJECTED_APPLICATION).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId);
        whenResultsAreTraced(rejectPayload);

        jsonResponse = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        assertThat(jsonResponse.getString(SUBJECT), is("APPEAL WITHDRAWN"));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("caseUrn1"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString(DATE_DECISION_MADE), is("01/01/2021"));
        assertThat(jsonResponse.getString(MATERIAL_ID), is(notNullValue()));
        Map impositionOffenceDetails = (Map) jsonResponse.getList(IMPOSITION_OFFENCE_DETAILS).get(0);
        assertThat(impositionOffenceDetails.get("details"), is(nullValue()));
        assertThat(impositionOffenceDetails.get("title"), is("Title 1"));
    }

    @Test
    public void shouldSendNcesEmailForNewApplicationThatWasGranted() {
        final String masterDefendantId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();
        final String accountNumber = "AER123451";

        final JsonObject payloadJson = convertStringToJson(getPayload(TEMPLATE_PAYLOAD_FOR_STAGING)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("MASTER_DEFENDANT_ID", masterDefendantId));

        hearingResultsHaveBeenSharedV2(payloadJson);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        initiateResultBeforeAmendment(TRACE_RESULT, masterDefendantId, accountCorrelationId, false, true, hearingId);

        initiateResultForGranted(TRACE_RESULT_GRANTED, masterDefendantId, hearingId, accountCorrelationId);

        final JsonObject stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add(REQUEST_ID, accountCorrelationId)
                .add(EXPORT_STATUS, "ENFORCEMENT_ACKNOWLEDGED")
                .add(UPDATED, "2019-12-01T10:00:00Z")
                .add(ACKNOWLEDGEMENT, createObjectBuilder().add(ACCOUNT_NUMBER, accountNumber).build())
                .build();

        raisePublicEventForAcknowledgement(stagingEnforcementAckPayload, PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(hearingFinancialResultsUpdatedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(ACCOUNT_NUMBER), is(accountNumber));


        final List<JsonPath> messages = QueueUtil.retrieveMessages(ncesEmailEventConsumer, 2);
        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase(APPEAL_APPLICATION_GRANTED)).findFirst().orElseGet(() -> JsonPath.from("{}"));
        assertThat(jsonResponse.getString(SUBJECT), is(APPEAL_APPLICATION_GRANTED));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("caseUrn1"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(jsonResponse.getString("originalDateOfSentence"), is(nullValue()));

        assertThat(jsonResponse.getString("newOffenceByResult"), containsString("FO - Fine\nFined £50.00\nPDATE - Pay by date\nPay by date. Date to pay in full by: 22/12/2023."));
    }


    @Test
    public void shouldSendNcesEmailForNewApplicationThatWasGrantedWhenSjpCase() {
        final String masterDefendantId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();
        final String accountNumber = "AER123451";

        final JsonObject payloadJson = convertStringToJson(getPayload(TEMPLATE_PAYLOAD_FOR_STAGING)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("MASTER_DEFENDANT_ID", masterDefendantId));

        hearingResultsHaveBeenSharedV2(payloadJson);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        initiateResultBeforeAmendment(TRACE_RESULT_SJP, masterDefendantId, accountCorrelationId, false, true, hearingId);

        initiateResultForGranted(TRACE_RESULT_GRANTED_SJP, masterDefendantId, hearingId, accountCorrelationId);

        final JsonObject stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add(REQUEST_ID, accountCorrelationId)
                .add(EXPORT_STATUS, "ENFORCEMENT_ACKNOWLEDGED")
                .add(UPDATED, "2019-12-01T10:00:00Z")
                .add(ACKNOWLEDGEMENT, createObjectBuilder().add(ACCOUNT_NUMBER, accountNumber).build())
                .build();

        raisePublicEventForAcknowledgement(stagingEnforcementAckPayload, PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(hearingFinancialResultsUpdatedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(ACCOUNT_NUMBER), is(accountNumber));


        final List<JsonPath> messages = QueueUtil.retrieveMessages(ncesEmailEventConsumer, 2);
        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase(APPEAL_APPLICATION_GRANTED)).findFirst().orElseGet(() -> JsonPath.from("{}"));
        assertThat(jsonResponse.getString(SUBJECT), is(APPEAL_APPLICATION_GRANTED));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("caseUrn1"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(jsonResponse.getString("originalDateOfSentence"), is(nullValue()));
        assertThat(jsonResponse.getString("newOffenceByResult"), allOf(
                containsString("FO - Fine\nFined £50.00\nPDATE - Pay by date\nPay by date. Date to pay in full by: 22/12/2023."),
                containsString("offenceDate:2023-11-22")
        ));
    }

    @Test
    public void shouldSendNcesEmailForNewApplicationThatWasGrantedForSJP() {
        final String masterDefendantId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();
        final String accountNumber = "AER123451";

        final JsonObject payloadJson = convertStringToJson(getPayload(TEMPLATE_PAYLOAD_FOR_STAGING_FOR_SJP)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("MASTER_DEFENDANT_ID", masterDefendantId));

        hearingResultsHaveBeenSharedV2(payloadJson);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        initiateResultBeforeAmendment(TRACE_RESULT, masterDefendantId, accountCorrelationId, false, true, hearingId);

        initiateResultForGranted(TRACE_RESULT_APPLICATION_TO_REOPEN_GRANTED, masterDefendantId, hearingId, accountCorrelationId);

        final JsonObject stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add(REQUEST_ID, accountCorrelationId)
                .add(EXPORT_STATUS, "ENFORCEMENT_ACKNOWLEDGED")
                .add(UPDATED, "2019-12-01T10:00:00Z")
                .add(ACKNOWLEDGEMENT, createObjectBuilder().add(ACCOUNT_NUMBER, accountNumber).build())
                .build();

        raisePublicEventForAcknowledgement(stagingEnforcementAckPayload, PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(hearingFinancialResultsUpdatedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(ACCOUNT_NUMBER), is(accountNumber));


        final List<JsonPath> messages = QueueUtil.retrieveMessages(ncesEmailEventConsumer, 2);
        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase(APPLICATION_TO_REOPEN_GRANTED)).findFirst().orElseGet(() -> JsonPath.from("{}"));
        assertThat(jsonResponse.getString(SUBJECT), is(APPLICATION_TO_REOPEN_GRANTED));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("caseUrn1"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(jsonResponse.getString("originalDateOfSentence"), is("03/05/2024"));
        assertThat(jsonResponse.getString("originalDateOfOffence"), is(nullValue()));
    }

    @Test
    public void shouldSendNcesEmailForNewApplicationThatWasUpdated() {
        final String masterDefendantId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();
        final String accountNumber = "AER123451";

        final JsonObject payloadJson = convertStringToJson(getPayload(TEMPLATE_PAYLOAD_FOR_STAGING)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("MASTER_DEFENDANT_ID", masterDefendantId));

        hearingResultsHaveBeenSharedV2(payloadJson);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        initiateResultBeforeAmendment(TRACE_RESULT, masterDefendantId, accountCorrelationId, false, true, hearingId);

        initiateResultForGranted(TRACE_RESULT_UPDATED, masterDefendantId, hearingId, accountCorrelationId);

        final JsonObject stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add(REQUEST_ID, accountCorrelationId)
                .add(EXPORT_STATUS, "ENFORCEMENT_ACKNOWLEDGED")
                .add(UPDATED, "2019-12-01T10:00:00Z")
                .add(ACKNOWLEDGEMENT, createObjectBuilder().add(ACCOUNT_NUMBER, accountNumber).build())
                .build();

        raisePublicEventForAcknowledgement(stagingEnforcementAckPayload, PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(hearingFinancialResultsUpdatedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(ACCOUNT_NUMBER), is(accountNumber));


        final List<JsonPath> messages = QueueUtil.retrieveMessages(ncesEmailEventConsumer, 2);
        messages.forEach(jsonPath -> System.out.println("subject = " + jsonPath.getString(SUBJECT)));
        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT)
                        .equalsIgnoreCase(APPEAL_APPLICATION_UPDATED))
                .findFirst()
                .orElseGet(() -> JsonPath.from("{}"));
        assertThat(jsonResponse.getString(SUBJECT), is(APPEAL_APPLICATION_UPDATED));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("caseUrn1"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(jsonResponse.getString(APPLICATION_RESULT), is("STDEC - Statutory Declaration Statutory declaration made under Section 14 of the Magistrates' Courts Act 1980. "));
        System.out.println(jsonResponse.getString(APPLICATION_RESULT));
    }

    private void initiateResultForGranted(final String traceResultType, final String masterDefendantId, final String hearingId, final String accountCorrelationId) {
        final String payload = getPayload(traceResultType)
                .replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", accountCorrelationId)
                .replaceAll("DEEMED_SERVED", "false")
                .replaceAll("FINANCIAL", "true")
                .replaceAll("HEARING_SITTING_DAY", HEARING_SITTING_DAY_VALUE)
                .replaceAll("HEARING_COURT_CENTRE_NAME", HEARING_COURT_CENTRE_NAME_VALUE)
                .replaceAll("DEFENDANT_DATE_OF_BIRTH", DEFENDANT_DATE_OF_BIRTH_VALUE)
                .replaceAll("DEFENDANT_ADDRESS", DEFENDANT_ADDRESS_VALUE)
                .replaceAll("DEFENDANT_EMAIL", DEFENDANT_EMAIL_VALUE)
                .replaceAll("DEFENDANT_CONTACT_NUMBER", DEFENDANT_CONTACT_NUMBER_VALUE)
                .replaceAll("HEARING_ID", hearingId);


        whenResultsAreTraced(payload);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(correlationIdAndMasterDefendantIdAddedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
    }

    @SuppressWarnings("java:S5961")
    @Test
    public void shouldSendNcesEmailForNewApplicationThenRejectAfterReceivedAccountNumber() {
        final String masterDefendantId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();
        final String accountNumber = "AER123451";

        final String payload = getPayload(TRACE_RESULT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", accountCorrelationId)
                .replaceAll("DEEMED_SERVED", "false")
                .replaceAll("FINANCIAL", "true")
                .replaceAll("HEARING_SITTING_DAY", HEARING_SITTING_DAY_VALUE)
                .replaceAll("HEARING_COURT_CENTRE_NAME", HEARING_COURT_CENTRE_NAME_VALUE)
                .replaceAll("DEFENDANT_DATE_OF_BIRTH", DEFENDANT_DATE_OF_BIRTH_VALUE)
                .replaceAll("DEFENDANT_ADDRESS", DEFENDANT_ADDRESS_VALUE)
                .replaceAll("DEFENDANT_EMAIL", DEFENDANT_EMAIL_VALUE)
                .replaceAll("DEFENDANT_CONTACT_NUMBER", DEFENDANT_CONTACT_NUMBER_VALUE)
                .replaceAll("HEARING_ID", hearingId);
        whenResultsAreTraced(payload);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(correlationIdAndMasterDefendantIdAddedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));


        final JsonObject ncesEmailPayload = createObjectBuilder()
                .add("applicationType", "APPEAL")
                .add(MASTER_DEFENDANT_ID, masterDefendantId)
                .add("listingDate", "01/12/2019")
                .add("caseUrns", createArrayBuilder().add("caseUrn1").build())
                .add(HEARING_COURT_CENTRE_NAME, HEARING_COURT_CENTRE_NAME_VALUE)
                .build();
        raisePublicEventForAcknowledgement(ncesEmailPayload, PUBLIC_EVENT_SEND_NCES_EMAIL_FOR_NEW_APPLICATION);

        final String rejectPayload = getPayload(REJECTED_APPLICATION).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId);
        whenResultsAreTraced(rejectPayload);

        final JsonObject stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add(REQUEST_ID, accountCorrelationId)
                .add(EXPORT_STATUS, "ENFORCEMENT_ACKNOWLEDGED")
                .add(UPDATED, "2019-12-01T10:00:00Z")
                .add(ACKNOWLEDGEMENT, createObjectBuilder().add(ACCOUNT_NUMBER, accountNumber).build())
                .build();

        raisePublicEventForAcknowledgement(stagingEnforcementAckPayload, PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT);

        jsonResponse = QueueUtil.retrieveMessage(hearingFinancialResultsUpdatedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(ACCOUNT_NUMBER), is(accountNumber));

        final List<JsonPath> messages = QueueUtil.retrieveMessages(ncesEmailEventConsumer, 3);
        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase(APPEAL_APPLICATION_RECEIVED)).findFirst().orElseGet(() -> JsonPath.from("{}"));

        assertThat(jsonResponse.getString(SUBJECT), is(APPEAL_APPLICATION_RECEIVED));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("caseUrn1"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString(LISTED_DATE), is("2019-12-01"));
        assertThat(jsonResponse.getString(MATERIAL_ID), is(notNullValue()));

        assertThat(jsonResponse.getString(HEARING_SITTING_DAY), is(ZonedDateTime.parse(HEARING_SITTING_DAY_VALUE).format(DateTimeFormatter.ofPattern(HEARING_SITTING_DAY_PATTERN))));
        assertThat(jsonResponse.getString(HEARING_COURT_CENTRE_NAME), is(HEARING_COURT_CENTRE_NAME_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_EMAIL), is(DEFENDANT_EMAIL_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_ADDRESS), is(DEFENDANT_ADDRESS_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_CONTACT_NUMBER), is(DEFENDANT_CONTACT_NUMBER_VALUE));

        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase("APPEAL WITHDRAWN")).findFirst().orElseGet(() -> JsonPath.from("{}"));
        assertThat(jsonResponse.getString(SUBJECT), is("APPEAL WITHDRAWN"));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("caseUrn1"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString(DATE_DECISION_MADE), is("01/01/2021"));
        assertThat(jsonResponse.getString(MATERIAL_ID), is(notNullValue()));
        Map impositionOffenceDetails = (Map) jsonResponse.getList(IMPOSITION_OFFENCE_DETAILS).get(0);
        assertThat(impositionOffenceDetails.get("details"), is(nullValue()));
        assertThat(impositionOffenceDetails.get("title"), is("Title 1"));
    }

    @Test
    public void shouldSendNcesEmailForAfterUpdatingGobAccountNumber() {
        final String masterDefendantId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();
        final String accountNumber = "AER123451";

        final String payload = getPayload(TRACE_RESULT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", accountCorrelationId)
                .replaceAll("DEEMED_SERVED", "false")
                .replaceAll("FINANCIAL", "true")
                .replaceAll("HEARING_SITTING_DAY", HEARING_SITTING_DAY_VALUE)
                .replaceAll("HEARING_COURT_CENTRE_NAME", HEARING_COURT_CENTRE_NAME_VALUE)
                .replaceAll("DEFENDANT_DATE_OF_BIRTH", DEFENDANT_DATE_OF_BIRTH_VALUE)
                .replaceAll("DEFENDANT_ADDRESS", DEFENDANT_ADDRESS_VALUE)
                .replaceAll("DEFENDANT_EMAIL", DEFENDANT_EMAIL_VALUE)
                .replaceAll("DEFENDANT_CONTACT_NUMBER", DEFENDANT_CONTACT_NUMBER_VALUE)
                .replaceAll("HEARING_ID", hearingId);

        whenResultsAreTraced(payload);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(correlationIdAndMasterDefendantIdAddedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));

        final JsonObject stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add(REQUEST_ID, accountCorrelationId)
                .add(EXPORT_STATUS, "ENFORCEMENT_ACKNOWLEDGED")
                .add(UPDATED, "2019-12-01T10:00:00Z")
                .add(ACKNOWLEDGEMENT, createObjectBuilder().add(ACCOUNT_NUMBER, accountNumber).build())
                .build();

        raisePublicEventForAcknowledgement(stagingEnforcementAckPayload, PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT);

        jsonResponse = QueueUtil.retrieveMessage(ncesEmailEventConsumer);

        assertThat(jsonResponse.getString(SUBJECT), is(ACON_SUBJECT));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString("impositionOffenceDetails[0].title"), is("Title 2"));
        assertThat(jsonResponse.getString("impositionOffenceDetails[0].details"), is("Amount of surcharge: £115"));
        assertThat(jsonResponse.getString("impositionOffenceDetails[1].title"), is("Title 3"));
        assertThat(jsonResponse.getString("impositionOffenceDetails[1].details"), is("Amount of surcharge: £200"));


    }

    @Test
    public void shouldSendNcesEmailWhenResultAmendedWithDeemedServed() {
        final String masterDefendantId = randomUUID().toString();
        final String accountCorrelationId1 = randomUUID().toString();
        final String accountCorrelationId2 = randomUUID().toString();
        final String accountCorrelationId3 = randomUUID().toString();
        final String accountNumber1 = "AER123451";
        final String accountNumber2 = "AER123452";
        final String accountNumber3 = "AER123453";
        final String divisionCode1 = "DIV01";
        final String divisionCode2 = "DIV02";
        final String divisionCode3 = "DIV03";
        final String hearingId = randomUUID().toString();

        initiateResultBeforeAmendment(TRACE_RESULT, masterDefendantId, accountCorrelationId1, true, true, hearingId);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId1, accountNumber1);
        JsonPath response1 = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        String subject1 = response1.getString(SUBJECT);
        assertThat(subject1, is(WRITE_OFF_ONE_DAY_DEEMED_SERVED));

        //Financial + Deemed served result amended to Financial + Deemed served -> Two Nces Email
        whenResultAmended(masterDefendantId, accountCorrelationId2, divisionCode2, true, true, hearingId);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId2, accountNumber2);

        final List<JsonPath> responses = validateTwoEmailNotificationRequested();
        JsonPath writeOffOneDayDeemedServed;
        JsonPath amendResultInputError;

        final String subject = responses.get(0).getString(SUBJECT);
        if (subject.equals(WRITE_OFF_ONE_DAY_DEEMED_SERVED)) {
            writeOffOneDayDeemedServed = responses.get(0);
            amendResultInputError = responses.get(1);
        } else {
            amendResultInputError = responses.get(0);
            writeOffOneDayDeemedServed = responses.get(1);
        }

        assertThat(amendResultInputError.getString(SUBJECT), is(AMEND_AND_RESHARE));
        assertThat(amendResultInputError.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(amendResultInputError.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(amendResultInputError.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(amendResultInputError.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(amendResultInputError.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(amendResultInputError.getString(DIVISION_CODE), is(divisionCode2));
        assertThat(amendResultInputError.getString(OLD_DIVISION_CODE), is(divisionCode1));
        assertThat(amendResultInputError.getString(OLD_GOB_ACCOUNT_NUMBER), is(accountNumber1));

        assertThat(writeOffOneDayDeemedServed.getString(SUBJECT), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED));
        assertThat(writeOffOneDayDeemedServed.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(writeOffOneDayDeemedServed.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(writeOffOneDayDeemedServed.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(writeOffOneDayDeemedServed.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(writeOffOneDayDeemedServed.getString(DIVISION_CODE), is(divisionCode2));

        //Financial + Deemed served result amended to Financial result -> One Nces Email
        whenResultAmended(masterDefendantId, accountCorrelationId3, divisionCode3, false, true, hearingId);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId3, accountNumber3);

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        assertThat(jsonResponse.getString(SUBJECT), is(AMEND_AND_RESHARE));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(amendResultInputError.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is(divisionCode3));
        assertThat(jsonResponse.getString(OLD_DIVISION_CODE), is(divisionCode2));
        assertThat(jsonResponse.getString(OLD_GOB_ACCOUNT_NUMBER), is(accountNumber2));
    }


    @Test
    public void shouldSendNcesEmailWhenFinancialResultAmendedWithFinancial() {
        final String masterDefendantId = randomUUID().toString();
        final String accountCorrelationId1 = randomUUID().toString();
        final String accountCorrelationId2 = randomUUID().toString();
        final String accountCorrelationId3 = randomUUID().toString();
        final String accountNumber1 = "AER123451";
        final String accountNumber2 = "AER123452";
        final String accountNumber3 = "AER123453";
        final String divisionCode1 = "DIV01";
        final String divisionCode2 = "DIV02";
        final String divisionCode3 = "DIV03";

        final String hearingId = randomUUID().toString();

        //Initial Financial Result
        initiateResultBeforeAmendment(TRACE_RESULT, masterDefendantId, accountCorrelationId1, false, true, hearingId);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId1, accountNumber1);

        //Financial Result amended to Financial -> One Nces Email
        whenResultAmended(masterDefendantId, accountCorrelationId2, divisionCode2, false, true, hearingId);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId2, accountNumber2);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        assertThat(jsonResponse.getString(SUBJECT), is(AMEND_AND_RESHARE));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is(divisionCode2));
        assertThat(jsonResponse.getString(OLD_DIVISION_CODE), is(divisionCode1));
        assertThat(jsonResponse.getString(OLD_GOB_ACCOUNT_NUMBER), is(accountNumber1));
        assertThat(jsonResponse.getString(MATERIAL_ID), is(notNullValue()));

        //Financial Result amended to Financial + Deemed Served -> Two Nces Email
        whenResultAmended(masterDefendantId, accountCorrelationId3, divisionCode3, true, true, hearingId);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId3, accountNumber3);

        final List<JsonPath> responses = validateTwoEmailNotificationRequested();
        JsonPath writeOffOneDayDeemedServed;
        JsonPath amendResultInputError;

        final String subject = responses.get(0).getString(SUBJECT);
        if (subject.equals(WRITE_OFF_ONE_DAY_DEEMED_SERVED)) {
            writeOffOneDayDeemedServed = responses.get(0);
            amendResultInputError = responses.get(1);
        } else {
            amendResultInputError = responses.get(0);
            writeOffOneDayDeemedServed = responses.get(1);
        }

        assertThat(amendResultInputError.getString(SUBJECT), is(AMEND_AND_RESHARE));
        assertThat(amendResultInputError.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(amendResultInputError.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(amendResultInputError.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(amendResultInputError.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(amendResultInputError.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(amendResultInputError.getString(DIVISION_CODE), is(divisionCode3));
        assertThat(amendResultInputError.getString(OLD_DIVISION_CODE), is(divisionCode2));
        assertThat(amendResultInputError.getString(OLD_GOB_ACCOUNT_NUMBER), is(accountNumber2));
        assertThat(writeOffOneDayDeemedServed.getString(SUBJECT), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED));
        assertThat(writeOffOneDayDeemedServed.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(writeOffOneDayDeemedServed.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(writeOffOneDayDeemedServed.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(writeOffOneDayDeemedServed.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(writeOffOneDayDeemedServed.getString(DIVISION_CODE), is(divisionCode3));
    }

    @SuppressWarnings("java:S5961")
    @Test
    public void shouldSendNcesEmailWhenAccountNumberReceivedUnordered() {
        final String masterDefendantId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String accountCorrelationId1 = randomUUID().toString();
        final String accountCorrelationId2 = randomUUID().toString();
        final String accountCorrelationId3 = randomUUID().toString();
        final String accountNumber1 = "AER123451";
        final String accountNumber2 = "AER123452";
        final String accountNumber3 = "AER123453";
        final String divisionCode2 = "DIV02";
        final String divisionCode3 = "DIV03";

        //Initial Financial Result
        initiateResultBeforeAmendment(TRACE_RESULT, masterDefendantId, accountCorrelationId1, false, true, hearingId);

        //Financial Result amended to Financial -> One Nces Email
        whenResultAmended(masterDefendantId, accountCorrelationId2, divisionCode2, false, true, hearingId);

        //Financial Result amended to Financial + Deemed Served -> Two Nces Email
        whenResultAmended(masterDefendantId, accountCorrelationId3, divisionCode3, true, true, hearingId);


        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId3, accountNumber3);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId1, accountNumber1);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId2, accountNumber2);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        assertThat(jsonResponse.getString(SUBJECT), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is(divisionCode3));
        assertThat(jsonResponse.getString(MATERIAL_ID), is(notNullValue()));

        jsonResponse = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        assertThat(jsonResponse.getString(SUBJECT), is(AMEND_AND_RESHARE));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(MATERIAL_ID), is(notNullValue()));

        jsonResponse = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        assertThat(jsonResponse.getString(SUBJECT), is(AMEND_AND_RESHARE));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(MATERIAL_ID), is(notNullValue()));

    }

    private List<JsonPath> validateTwoEmailNotificationRequested() {
        JsonPath response1 = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        String subject1 = response1.getString(SUBJECT);
        assertThat(subject1, anyOf(is(AMEND_AND_RESHARE), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED)));

        JsonPath response2 = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        String subject2 = response2.getString(SUBJECT);
        assertThat(subject2, anyOf(is(AMEND_AND_RESHARE), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED)));

        assertThat(subject1, is(not(subject2)));
        return asList(response1, response2);
    }


    private void initiateResultBeforeAmendment(String payloadFilname, final String masterDefendantId, final String correlationId, final Boolean deemedServed, final Boolean financial, final String hearingId) {
        final String tracePayload = getPayload(payloadFilname).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", correlationId)
                .replaceAll("ACON", "CODE01")
                .replaceAll("DEEMED_SERVED", deemedServed.toString())
                .replaceAll("FINANCIAL", financial.toString())
                .replaceAll("HEARING_SITTING_DAY", HEARING_SITTING_DAY_VALUE)
                .replaceAll("HEARING_COURT_CENTRE_NAME", HEARING_COURT_CENTRE_NAME_VALUE)
                .replaceAll("DEFENDANT_DATE_OF_BIRTH", DEFENDANT_DATE_OF_BIRTH_VALUE)
                .replaceAll("DEFENDANT_ADDRESS", DEFENDANT_ADDRESS_VALUE)
                .replaceAll("DEFENDANT_EMAIL", DEFENDANT_EMAIL_VALUE)
                .replaceAll("DEFENDANT_CONTACT_NUMBER", DEFENDANT_CONTACT_NUMBER_VALUE)
                .replaceAll("HEARING_ID", hearingId);

        whenResultsAreTraced(tracePayload);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(correlationIdAndMasterDefendantIdAddedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(correlationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
    }

    private void whenAccountNumberRetrieved(final String masterDefendantId, final String correlationId, final String accountNumber) {
        JsonObject stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add(REQUEST_ID, correlationId)
                .add(EXPORT_STATUS, "ENFORCEMENT_ACKNOWLEDGED")
                .add(UPDATED, "2019-12-01T10:00:00Z")
                .add(ACKNOWLEDGEMENT, createObjectBuilder().add(ACCOUNT_NUMBER, accountNumber).build())
                .build();

        raisePublicEventForAcknowledgement(stagingEnforcementAckPayload, PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT);

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(hearingFinancialResultsUpdatedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(correlationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(ACCOUNT_NUMBER), is(accountNumber));
    }

    private void whenResultAmended(final String masterDefendantId, final String correlationId, final String divisionCode, final Boolean deemedServed, final Boolean financial, final String hearingId) {

        final String amendedTracePayload = getPayload(TRACE_AMENDED_RESULT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", correlationId)
                .replaceAll("DIVISION_CODE", divisionCode)
                .replaceAll("DEEMED_SERVED", deemedServed.toString())
                .replaceAll("FINANCIAL", financial.toString())
                .replaceAll("HEARING_SITTING_DAY", HEARING_SITTING_DAY_VALUE)
                .replaceAll("HEARING_COURT_CENTRE_NAME", HEARING_COURT_CENTRE_NAME_VALUE)
                .replaceAll("DEFENDANT_DATE_OF_BIRTH", DEFENDANT_DATE_OF_BIRTH_VALUE)
                .replaceAll("DEFENDANT_ADDRESS", DEFENDANT_ADDRESS_VALUE)
                .replaceAll("DEFENDANT_EMAIL", DEFENDANT_EMAIL_VALUE)
                .replaceAll("DEFENDANT_CONTACT_NUMBER", DEFENDANT_CONTACT_NUMBER_VALUE)
                .replaceAll("HEARING_ID", hearingId);
        whenResultsAreTraced(amendedTracePayload);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(correlationIdAndMasterDefendantIdAddedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(correlationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
    }

    @Test
    public void shouldRaiseEmailForDeemedServed() {
        final String masterDefendantId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();
        final String accountCorrelationId2 = randomUUID().toString();
        final String accountNumber = "AER123451";
        final String accountNumber2 = "AER123452";

        String payload = getPayload(TRACE_RESULT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", accountCorrelationId)
                .replaceAll("FINANCIAL", "true")
                .replaceAll("DEEMED_SERVED", "true")
                .replaceAll("HEARING_SITTING_DAY", HEARING_SITTING_DAY_VALUE)
                .replaceAll("HEARING_COURT_CENTRE_NAME", HEARING_COURT_CENTRE_NAME_VALUE)
                .replaceAll("DEFENDANT_DATE_OF_BIRTH", DEFENDANT_DATE_OF_BIRTH_VALUE)
                .replaceAll("DEFENDANT_ADDRESS", DEFENDANT_ADDRESS_VALUE)
                .replaceAll("DEFENDANT_EMAIL", DEFENDANT_EMAIL_VALUE)
                .replaceAll("DEFENDANT_CONTACT_NUMBER", DEFENDANT_CONTACT_NUMBER_VALUE)
                .replaceAll("HEARING_ID", hearingId);
        whenResultsAreTraced(payload);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(correlationIdAndMasterDefendantIdAddedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));


        JsonObject stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add(REQUEST_ID, accountCorrelationId)
                .add(EXPORT_STATUS, "ENFORCEMENT_ACKNOWLEDGED")
                .add(UPDATED, "2019-12-01T10:00:00Z")
                .add(ACKNOWLEDGEMENT, createObjectBuilder().add(ACCOUNT_NUMBER, accountNumber).build())
                .build();

        raisePublicEventForAcknowledgement(stagingEnforcementAckPayload, PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT);

        jsonResponse = QueueUtil.retrieveMessage(hearingFinancialResultsUpdatedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(ACCOUNT_NUMBER), is(accountNumber));

        List<JsonPath> messages = QueueUtil.retrieveMessages(ncesEmailEventConsumer, 2);
        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase(WRITE_OFF_ONE_DAY_DEEMED_SERVED)).findFirst().orElseGet(() -> JsonPath.from("{}"));
        assertThat(jsonResponse.getString(SUBJECT), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString(DATE_DECISION_MADE), is(nullValue()));
        assertThat(jsonResponse.getString(MATERIAL_ID), is(notNullValue()));

        assertThat(jsonResponse.getString(HEARING_SITTING_DAY), is(ZonedDateTime.parse(HEARING_SITTING_DAY_VALUE).format(DateTimeFormatter.ofPattern(HEARING_SITTING_DAY_PATTERN))));
        assertThat(jsonResponse.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_EMAIL), is(DEFENDANT_EMAIL_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_ADDRESS), is(DEFENDANT_ADDRESS_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_CONTACT_NUMBER), is(DEFENDANT_CONTACT_NUMBER_VALUE));

        payload = getPayload(TRACE_RESULT_AMENDMENT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", accountCorrelationId)
                .replaceAll("FINANCIAL", "false")
                .replaceAll("DEEMED_SERVED", "false")
                .replaceAll("AMENDMENT_REASON", "Amendment1")
                .replaceAll("HEARING_SITTING_DAY", HEARING_SITTING_DAY_VALUE)
                .replaceAll("HEARING_COURT_CENTRE_NAME", HEARING_COURT_CENTRE_NAME_VALUE)
                .replaceAll("DEFENDANT_DATE_OF_BIRTH", DEFENDANT_DATE_OF_BIRTH_VALUE)
                .replaceAll("DEFENDANT_ADDRESS", DEFENDANT_ADDRESS_VALUE)
                .replaceAll("DEFENDANT_EMAIL", DEFENDANT_EMAIL_VALUE)
                .replaceAll("DEFENDANT_CONTACT_NUMBER", DEFENDANT_CONTACT_NUMBER_VALUE);

        whenResultsAreTraced(payload);

        jsonResponse = QueueUtil.retrieveMessage(correlationIdAndMasterDefendantIdAddedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));


        jsonResponse = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        assertThat(jsonResponse, is(nullValue()));

        payload = getPayload(TRACE_RESULT_AMENDMENT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", accountCorrelationId2)
                .replaceAll("DEEMED_SERVED", "true")
                .replaceAll("FINANCIAL", "true")
                .replaceAll("AMENDMENT_REASON", "Amendment2")
                .replaceAll("HEARING_SITTING_DAY", HEARING_SITTING_DAY_VALUE)
                .replaceAll("HEARING_COURT_CENTRE_NAME", HEARING_COURT_CENTRE_NAME_VALUE)
                .replaceAll("DEFENDANT_DATE_OF_BIRTH", DEFENDANT_DATE_OF_BIRTH_VALUE)
                .replaceAll("DEFENDANT_ADDRESS", DEFENDANT_ADDRESS_VALUE)
                .replaceAll("DEFENDANT_EMAIL", DEFENDANT_EMAIL_VALUE)
                .replaceAll("DEFENDANT_CONTACT_NUMBER", DEFENDANT_CONTACT_NUMBER_VALUE);

        whenResultsAreTraced(payload);

        jsonResponse = QueueUtil.retrieveMessage(correlationIdAndMasterDefendantIdAddedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId2));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));

        stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add(REQUEST_ID, accountCorrelationId2)
                .add(EXPORT_STATUS, "ENFORCEMENT_ACKNOWLEDGED")
                .add(UPDATED, "2019-12-01T10:00:00Z")
                .add(ACKNOWLEDGEMENT, createObjectBuilder().add(ACCOUNT_NUMBER, accountNumber2).build())
                .build();

        raisePublicEventForAcknowledgement(stagingEnforcementAckPayload, PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT);

        jsonResponse = QueueUtil.retrieveMessage(hearingFinancialResultsUpdatedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId2));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(ACCOUNT_NUMBER), is(accountNumber2));

        messages = QueueUtil.retrieveMessages(ncesEmailEventConsumer, 2);
        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase(WRITE_OFF_ONE_DAY_DEEMED_SERVED)).findFirst().orElseGet(() -> JsonPath.from("{}"));
        assertThat(jsonResponse.getString(SUBJECT), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber2));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString("amendmentReason"), is(nullValue()));
        assertThat(jsonResponse.getString(DATE_DECISION_MADE), is(nullValue()));
        assertThat(jsonResponse.getString(MATERIAL_ID), is(notNullValue()));

        assertThat(jsonResponse.getString(HEARING_SITTING_DAY), is(ZonedDateTime.parse(HEARING_SITTING_DAY_VALUE).format(DateTimeFormatter.ofPattern(HEARING_SITTING_DAY_PATTERN))));
        assertThat(jsonResponse.getString(DEFENDANT_DATE_OF_BIRTH), is(DEFENDANT_DATE_OF_BIRTH_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_EMAIL), is(DEFENDANT_EMAIL_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_ADDRESS), is(DEFENDANT_ADDRESS_VALUE));
        assertThat(jsonResponse.getString(DEFENDANT_CONTACT_NUMBER), is(DEFENDANT_CONTACT_NUMBER_VALUE));
    }

    private void raisePublicEventForAcknowledgement(final JsonObject payload, final String eventName) {
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);
            messageProducer.sendMessage(eventName, payload);
        }
    }

}
