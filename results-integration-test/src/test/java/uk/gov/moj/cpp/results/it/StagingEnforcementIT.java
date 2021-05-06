package uk.gov.moj.cpp.results.it;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenResultsAreTraced;
import static uk.gov.moj.cpp.results.it.utils.QueueUtilForPrivateEvents.privateEvents;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubDocGeneratorEndPoint;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubNotificationNotifyEndPoint;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.results.it.utils.FileUtil;
import uk.gov.moj.cpp.results.it.utils.QueueUtil;

import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class StagingEnforcementIT {

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
    public static final String SEND_TO = "sendTo";
    public static final String GOB_ACCOUNT_NUMBER = "gobAccountNumber";
    public static final String OLD_GOB_ACCOUNT_NUMBER = "oldGobAccountNumber";
    public static final String CASE_REFERENCE = "caseReferences";
    public static final String DIVISION_CODE = "divisionCode";
    public static final String OLD_DIVISION_CODE = "oldDivisionCode";
    public static final String LISTED_DATE = "listedDate";
    public static final String APPEAL_APPLICATION_RECEIVED = "APPEAL APPLICATION RECEIVED";
    public static final String IMPOSITION_OFFENCE_DETAILS = "impositionOffenceDetails";
    private static final String PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT = "public.stagingenforcement.enforce-financial-imposition-acknowledgement";
    private static final String PUBLIC_EVENT_SEND_NCES_EMAIL_FOR_NEW_APPLICATION = "public.hearing.nces-email-notification-for-application";
    private static final String HEARING_FINANCIAL_RESULT_UPDATED = "results.event.hearing-financial-results-updated";
    private static final String PRIVATE_EMAIL_EVENT = "results.event.nces-email-notification-requested";
    public static final String WRITE_OFF_ONE_DAY_DEEMED_SERVED = "WRITE OFF ONE DAY DEEMED SERVED";
    public static final String AMEND_RESULT_INPUT_ERROR = "AMEND RESULT/INPUT ERROR";
    private static final String PUBLIC_EVENT_TOPIC = "public.event";
    private static final String ACON_SUBJECT = "ACCOUNTS TO BE CONSOLIDATED";

    private static final String TRACE_RESULT = "json/results.api.trace-results.json";
    private static final String TRACE_RESULT_AMENDMENT = "json/results.api.trace-results-amendment.json";
    private static final String REJECTED_APPLICATION = "json/results.api.rejected-application.json";
    public static final String DATE_DECISION_MADE = "dateDecisionMade";

    private static final String TRACE_AMENDED_RESULT = "json/results.api.trace-amended-results.json";

    static MessageConsumer correlationIdAndMasterDefendantIdAddedConsumer;
    static MessageConsumer hearingFinancialResultsUpdatedConsumer;
    static MessageConsumer ncesEmailEventConsumer;

    @BeforeClass
    public static void beforeClass(){
        stubDocGeneratorEndPoint();
        stubNotificationNotifyEndPoint();
        correlationIdAndMasterDefendantIdAddedConsumer = privateEvents.createConsumer(CORRELATION_ID_AND_MASTERDEFENDANT_ADDED);
        hearingFinancialResultsUpdatedConsumer = privateEvents.createConsumer(HEARING_FINANCIAL_RESULT_UPDATED);
        ncesEmailEventConsumer = privateEvents.createConsumer(PRIVATE_EMAIL_EVENT);

    }

    @Before
    public void setUp() {
        QueueUtil.removeMessagesFromQueue(correlationIdAndMasterDefendantIdAddedConsumer);
        QueueUtil.removeMessagesFromQueue(hearingFinancialResultsUpdatedConsumer);
        QueueUtil.removeMessagesFromQueue(ncesEmailEventConsumer);
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        correlationIdAndMasterDefendantIdAddedConsumer.close();
        hearingFinancialResultsUpdatedConsumer.close();
        ncesEmailEventConsumer.close();
    }

    @Test
    public void shouldSendNcesEmailForNewApplicationThatWasRejectedOrGranted(){
        final String masterDefendantId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();
        final String accountNumber = "AER123451";

        final String payload = FileUtil.getPayload(TRACE_RESULT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", accountCorrelationId)
                .replaceAll("DEEMED_SERVED", "false")
                .replaceAll("FINANCIAL", "true");
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
                .build();
        raisePublicEventForAcknowledgement(ncesEmailPayload, PUBLIC_EVENT_SEND_NCES_EMAIL_FOR_NEW_APPLICATION);

        final List<JsonPath> messages = QueueUtil.retrieveMessages(ncesEmailEventConsumer, 2);
        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase(APPEAL_APPLICATION_RECEIVED)).findFirst().orElseGet(()->JsonPath.from("{}"));
        assertThat(jsonResponse.getString(SUBJECT), is(APPEAL_APPLICATION_RECEIVED));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("caseUrn1"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString(LISTED_DATE), is("2019-12-01"));

        final String rejectPayload = FileUtil.getPayload(REJECTED_APPLICATION).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId);
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
        Map impositionOffenceDetails =  (Map)jsonResponse.getList(IMPOSITION_OFFENCE_DETAILS).get(0);
        assertThat(impositionOffenceDetails.get("details"), is(nullValue()));
        assertThat(impositionOffenceDetails.get("title"), is("Title 1"));


    }

    @Test
    public void shouldSendNcesEmailForNewApplicationThenRejectAfterReceivedAccountNumber() {
        final String masterDefendantId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();
        final String accountNumber = "AER123451";

        final String payload = FileUtil.getPayload(TRACE_RESULT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", accountCorrelationId)
                .replaceAll("DEEMED_SERVED", "false")
                .replaceAll("FINANCIAL", "true");
        whenResultsAreTraced(payload);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(correlationIdAndMasterDefendantIdAddedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));


        final JsonObject ncesEmailPayload = createObjectBuilder()
                .add("applicationType", "APPEAL")
                .add(MASTER_DEFENDANT_ID, masterDefendantId)
                .add("listingDate", "01/12/2019")
                .add("caseUrns", createArrayBuilder().add("caseUrn1").build())
                .build();
        raisePublicEventForAcknowledgement(ncesEmailPayload, PUBLIC_EVENT_SEND_NCES_EMAIL_FOR_NEW_APPLICATION);

        final String rejectPayload = FileUtil.getPayload(REJECTED_APPLICATION).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId);
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
        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase(APPEAL_APPLICATION_RECEIVED)).findFirst().orElseGet(()->JsonPath.from("{}"));

        assertThat(jsonResponse.getString(SUBJECT), is(APPEAL_APPLICATION_RECEIVED));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("caseUrn1"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString(LISTED_DATE), is("2019-12-01"));

        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase("APPEAL WITHDRAWN")).findFirst().orElseGet(()->JsonPath.from("{}"));
        assertThat(jsonResponse.getString(SUBJECT), is("APPEAL WITHDRAWN"));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("caseUrn1"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString(DATE_DECISION_MADE), is("01/01/2021"));
        Map impositionOffenceDetails =  (Map)jsonResponse.getList(IMPOSITION_OFFENCE_DETAILS).get(0);
        assertThat(impositionOffenceDetails.get("details"), is(nullValue()));
        assertThat(impositionOffenceDetails.get("title"), is("Title 1"));
    }

    @Test
    public void shouldSendNcesEmailForAfterUpdatingGobAccountNumber() {
        final String masterDefendantId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();
        final String accountNumber = "AER123451";

        final String payload = FileUtil.getPayload(TRACE_RESULT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", accountCorrelationId)
                .replaceAll("DEEMED_SERVED", "false")
                .replaceAll("FINANCIAL", "true");

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
    public void shouldSendNcesEmailWhenResultAmendedWithDeemedServed(){
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

        initiateResultBeforeAmendment(masterDefendantId, accountCorrelationId1, true, true);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId1, accountNumber1);
        JsonPath response1 = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        String subject1 = response1.getString(SUBJECT);
        assertThat(subject1, is(WRITE_OFF_ONE_DAY_DEEMED_SERVED));

        //Financial + Deemed served result amended to Financial + Deemed served -> Two Nces Email
        whenResultAmended(masterDefendantId, accountCorrelationId2, divisionCode2,true, true);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId2, accountNumber2);

        final List<JsonPath> responses = validateTwoEmailNotificationRequested();
        JsonPath writeOffOneDayDeemedServed = null;
        JsonPath amendResultInputError = null;

        final String subject = responses.get(0).getString(SUBJECT);
        if (subject.equals(WRITE_OFF_ONE_DAY_DEEMED_SERVED)){
            writeOffOneDayDeemedServed = responses.get(0);
            amendResultInputError = responses.get(1);
        } else {
            amendResultInputError = responses.get(0);
            writeOffOneDayDeemedServed = responses.get(1);
        }

        assertThat(amendResultInputError.getString(SUBJECT), is(AMEND_RESULT_INPUT_ERROR));
        assertThat(amendResultInputError.getString(DEFENDANT_NAME), is("John Doe"));
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
        whenResultAmended(masterDefendantId, accountCorrelationId3, divisionCode3,false, true);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId3, accountNumber3);

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        assertThat(jsonResponse.getString(SUBJECT), is(AMEND_RESULT_INPUT_ERROR));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is(divisionCode3));
        assertThat(jsonResponse.getString(OLD_DIVISION_CODE), is(divisionCode2));
        assertThat(jsonResponse.getString(OLD_GOB_ACCOUNT_NUMBER), is(accountNumber2));
    }


    @Test
    public void shouldSendNcesEmailWhenFinancialResultAmendedWithFinancial(){
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

        //Initial Financial Result
        initiateResultBeforeAmendment(masterDefendantId, accountCorrelationId1, false, true);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId1, accountNumber1);

        //Financial Result amended to Financial -> One Nces Email
        whenResultAmended(masterDefendantId, accountCorrelationId2, divisionCode2, false, true);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId2, accountNumber2);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        assertThat(jsonResponse.getString(SUBJECT), is(AMEND_RESULT_INPUT_ERROR));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is(divisionCode2));
        assertThat(jsonResponse.getString(OLD_DIVISION_CODE), is(divisionCode1));
        assertThat(jsonResponse.getString(OLD_GOB_ACCOUNT_NUMBER), is(accountNumber1));

        //Financial Result amended to Financial + Deemed Served -> Two Nces Email
        whenResultAmended(masterDefendantId, accountCorrelationId3, divisionCode3, true, true);
        whenAccountNumberRetrieved(masterDefendantId, accountCorrelationId3, accountNumber3);

        final List<JsonPath> responses = validateTwoEmailNotificationRequested();
        JsonPath writeOffOneDayDeemedServed = null;
        JsonPath amendResultInputError = null;

        final String subject = responses.get(0).getString(SUBJECT);
        if (subject.equals(WRITE_OFF_ONE_DAY_DEEMED_SERVED)){
            writeOffOneDayDeemedServed = responses.get(0);
            amendResultInputError = responses.get(1);
        } else {
            amendResultInputError = responses.get(0);
            writeOffOneDayDeemedServed = responses.get(1);
        }

        assertThat(amendResultInputError.getString(SUBJECT), is(AMEND_RESULT_INPUT_ERROR));
        assertThat(amendResultInputError.getString(DEFENDANT_NAME), is("John Doe"));
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


    @Test
    public void shouldSendNcesEmailWhenAccountNumberReceivedUnordered(){
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

        //Initial Financial Result
        initiateResultBeforeAmendment(masterDefendantId, accountCorrelationId1, false, true);

        //Financial Result amended to Financial -> One Nces Email
        whenResultAmended(masterDefendantId, accountCorrelationId2, divisionCode2, false, true);

        //Financial Result amended to Financial + Deemed Served -> Two Nces Email
        whenResultAmended(masterDefendantId, accountCorrelationId3, divisionCode3, true, true);



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

        jsonResponse = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        assertThat(jsonResponse.getString(SUBJECT), is(AMEND_RESULT_INPUT_ERROR));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));

        jsonResponse = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        assertThat(jsonResponse.getString(SUBJECT), is(AMEND_RESULT_INPUT_ERROR));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));

    }

    private List<JsonPath> validateTwoEmailNotificationRequested(){
        JsonPath response1 = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        String subject1 = response1.getString(SUBJECT);
        assertThat(subject1, anyOf(is(AMEND_RESULT_INPUT_ERROR), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED)));

        JsonPath response2 = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        String subject2 = response2.getString(SUBJECT);
        assertThat(subject2, anyOf(is(AMEND_RESULT_INPUT_ERROR), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED)));

        assertThat(subject1, is(not(subject2)));
        return asList(response1, response2);
    }

    private void initiateResultBeforeAmendment(final String masterDefendantId, final String correlationId, final Boolean deemedServed, final Boolean financial){
        final String tracePayload = FileUtil.getPayload(TRACE_RESULT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", correlationId)
                .replaceAll("ACON", "CODE01")
                .replaceAll("DEEMED_SERVED", deemedServed.toString())
                .replaceAll("FINANCIAL", financial.toString());

        whenResultsAreTraced(tracePayload);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(correlationIdAndMasterDefendantIdAddedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(correlationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
    }

    private void whenAccountNumberRetrieved(final String masterDefendantId, final String correlationId, final String accountNumber){
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

    private void  whenResultAmended(final String masterDefendantId, final String correlationId, final String divisionCode, final Boolean deemedServed, final Boolean financial){

        final String amendedTracePayload = FileUtil.getPayload(TRACE_AMENDED_RESULT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", correlationId)
                .replaceAll("DIVISION_CODE", divisionCode)
                .replaceAll("DEEMED_SERVED", deemedServed.toString())
                .replaceAll("FINANCIAL", financial.toString());
        whenResultsAreTraced(amendedTracePayload);

        JsonPath jsonResponse = QueueUtil.retrieveMessage(correlationIdAndMasterDefendantIdAddedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(correlationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
    }

    @Test
    public void shouldRaiseEmailForDeemedServed(){
        final String masterDefendantId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();
        final String accountCorrelationId2 = randomUUID().toString();
        final String accountNumber = "AER123451";
        final String accountNumber2 = "AER123452";

        String payload = FileUtil.getPayload(TRACE_RESULT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", accountCorrelationId)
                .replaceAll("FINANCIAL", "true")
                .replaceAll("DEEMED_SERVED", "true");
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
        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase(WRITE_OFF_ONE_DAY_DEEMED_SERVED)).findFirst().orElseGet(()->JsonPath.from("{}"));
        assertThat(jsonResponse.getString(SUBJECT), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString(DATE_DECISION_MADE), is("01/01/2021"));

       payload = FileUtil.getPayload(TRACE_RESULT_AMENDMENT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", accountCorrelationId)
               .replaceAll("FINANCIAL", "false")
                .replaceAll("DEEMED_SERVED", "false")
                .replaceAll("AMENDMENT_REASON", "Amendment1");
        whenResultsAreTraced(payload);

        jsonResponse = QueueUtil.retrieveMessage(correlationIdAndMasterDefendantIdAddedConsumer);
        assertThat(jsonResponse.getString(CORRELATION_ID), is(accountCorrelationId));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));


        jsonResponse = QueueUtil.retrieveMessage(ncesEmailEventConsumer);
        assertThat(jsonResponse.getString(SUBJECT), is(AMEND_RESULT_INPUT_ERROR));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString("amendmentReason"), is("Amendment1"));
        assertThat(jsonResponse.getString("amendmentDate"), is("01/01/2021"));

        payload = FileUtil.getPayload(TRACE_RESULT_AMENDMENT).replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", accountCorrelationId2)
                .replaceAll("DEEMED_SERVED", "true")
                .replaceAll("FINANCIAL", "true")
                .replaceAll("AMENDMENT_REASON", "Amendment2");
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
        jsonResponse = messages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase(WRITE_OFF_ONE_DAY_DEEMED_SERVED)).findFirst().orElseGet(()->JsonPath.from("{}"));
        assertThat(jsonResponse.getString(SUBJECT), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED));
        assertThat(jsonResponse.getString(DEFENDANT_NAME), is("John Doe"));
        assertThat(jsonResponse.getString(SEND_TO), is("John.Doe@xxx.com"));
        assertThat(jsonResponse.getString(GOB_ACCOUNT_NUMBER), is(accountNumber2));
        assertThat(jsonResponse.getString(CASE_REFERENCE), is("REF1,REF2"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("DIV01"));
        assertThat(jsonResponse.getString("amendmentReason"), is(nullValue()));
        assertThat(jsonResponse.getString(DATE_DECISION_MADE), is("01/01/2021"));
    }

    private void raisePublicEventForAcknowledgement(final JsonObject payload, final String eventName) {
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);
            messageProducer.sendMessage(eventName, payload);
        }
    }

}
