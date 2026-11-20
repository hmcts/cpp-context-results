package uk.gov.moj.cpp.results.it;

import static com.github.tomakehurst.wiremock.client.WireMock.listAllStubMappings;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.results.it.helper.DocumentDeliveryHelper.DOCUMENT_DELETED_FROM_BLOB;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.closeMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenSharedV2;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenResultsAreTraced;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.stub.DocumentGeneratorStub.stubRenderDocumentWithStatusOk;
import static uk.gov.moj.cpp.results.it.stub.NotificationNotifyServiceStub.setupNotificationNotifyStubs;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.RESULT_QUEUE_NAME;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.awaitEmailSentToGateway;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.bodyOf;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.isSendEmailNotification;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.publishNotificationSent;
import static uk.gov.moj.cpp.results.it.stub.SjpStub.setupSjpQueryStub;
import static uk.gov.moj.cpp.results.it.stub.SjpStub.stubUploadCaseDocument;
import static uk.gov.moj.cpp.results.it.utils.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.results.it.utils.FeatureStubUtil.enableResultsFileStoreWithDelete;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.convertStringToJson;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubBailStatuses;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrganisationUnit;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubJudicialResults;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubModeOfTrialReasons;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubProsecutorWithoutSpiOutOrPoliceFlag;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubSpiOutFlag;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubMaterialUploadFile;

import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.results.it.helper.DocumentDeliveryHelper;
import uk.gov.moj.cpp.results.it.helper.DocumentDeliveryHelper.DocumentDelivery;
import uk.gov.moj.cpp.results.it.utils.WireMockStubUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The delivery tracking of an NCES enforcement document stored in the blob store ({@code
 * resultsFileStore} on): results records in its view store where the document stands with
 * material, with every SJP case it is filed on, and with the NCES email that carries its link,
 * as those contexts report back.
 *
 * <p>Driven from the outside only - the results command API and the public events of hearing,
 * staging-enforcement, material and SJP, and the notify gateway's replies; no results private event
 * is raised by the test. It reads private events only to know when the service has reached a step.
 * Like {@link NcesEmailNotificationBlobStoreIT} it needs the "azurite" profile of the stack, and the
 * "servicebus" one for the notify gateway's queues.</p>
 */
class NcesDocumentDeliveryTrackingIT {

    private static final String RESULTS_EVENT_NCES_NOTIFICATION_REQUESTED = "results.event.nces-email-notification-requested";
    private static final String RESULTS_EVENT_HEARING_RESULTS_ADDED_FOR_DAY = "results.events.hearing-results-added-for-day";
    private static final String PUBLIC_EVENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT = "public.stagingenforcement.enforce-financial-imposition-acknowledgement";
    private static final String PUBLIC_EVENT_SJP_CASE_DOCUMENT_ADDED = "public.sjp.case-document-added";
    private static final String PUBLIC_EVENT_SJP_CASE_DOCUMENT_UPLOAD_REJECTED = "public.sjp.events.case-document-upload-rejected";
    private static final String SJP_HEARING_RESULTED = "json/public.events.hearing.hearing-resulted_for_staging_enforcement_for_sjp.json";
    private static final String TRACE_RESULT = "json/results.api.trace-results.json";
    private static final String SJP_HEARING_PROSECUTOR_CODE = "DVLA";
    private static final String SJP_HEARING_PROSECUTOR_OU_CODE = "G94DV00";
    // the trace fixture's prosecutionCaseReferences become the NCES request's caseReferences
    private static final String TRACE_CASE_URN_1 = "REF1";
    private static final String TRACE_CASE_URN_2 = "REF2";
    // once the account is acknowledged the aggregate raises one NCES request per notification rule the traced results
    // meet: the fixture's deemed-served financial results (WRITE OFF ONE DAY DEEMED SERVED) and its ACON results
    // (ACCOUNTS TO BE CONSOLIDATED)
    private static final String CONSOLIDATED_ACCOUNTS_RESULT_CODE = "ACON";
    private static final String OTHER_RESULT_CODE = "CODE02";
    private static final long NO_MORE_NCES_REQUESTS_TIMEOUT_IN_MILLIS = 5000;
    private static final String NCES_DOCUMENT_BLOB_NAME_PREFIX = "ENFDocumentOrder_";
    private static final String CLIENT_CONTEXT_PREFIX = "RESULTS_";

    private static final String CASE_ID = "caseId";
    private static final String MATERIAL_ID = "materialId";

    private static final String NCES = "NCES";
    private static final String PENDING = "PENDING";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";

    private static Set<UUID> stubMappingIdsBeforeThisTest;

    private final DocumentDeliveryHelper documentDeliveryHelper = new DocumentDeliveryHelper();

    private MessageConsumer correlationIdAndMasterDefendantAddedConsumer;
    private MessageConsumer hearingFinancialResultsUpdatedConsumer;
    private MessageConsumer ncesEmailNotificationRequestedConsumer;
    private MessageConsumer hearingResultsAddedForDayConsumer;
    private MessageConsumer documentDeletedFromBlobConsumer;

    @BeforeAll
    static void setupStubs() throws ClassNotFoundException {
        // WireMockStubUtils resets WireMock when it is first loaded; load it now so that reset can't
        // happen after the snapshot, then remember what was already stubbed so tearDown can put it back
        Class.forName(WireMockStubUtils.class.getName());
        stubMappingIdsBeforeThisTest = listAllStubMappings().getMappings().stream().map(StubMapping::getId).collect(toSet());

        setupUsersGroupQueryStub();
        stubRenderDocumentWithStatusOk("nces-enforcement-document-" + randomUUID());
        stubMaterialUploadFile();
        stubUploadCaseDocument();
        setupNotificationNotifyStubs();
        // the resulted SJP hearing also runs the hearing-results-added-for-day / police-results leg, which
        // reads reference data - same set as NcesEmailNotificationBlobStoreIT
        stubCountryNationalities();
        stubGetOrganisationUnit();
        stubJudicialResults();
        stubBailStatuses();
        stubModeOfTrialReasons();
        stubSpiOutFlag(false, false);
        stubProsecutorWithoutSpiOutOrPoliceFlag(SJP_HEARING_PROSECUTOR_CODE, SJP_HEARING_PROSECUTOR_OU_CODE);
        // last: loading WireMockStubUtils resets WireMock, which would wipe a feature stub registered before it
        enableResultsFileStoreWithDelete();
    }

    @AfterAll
    static void removeStubsAddedByThisTest() {
        listAllStubMappings().getMappings().stream()
                .filter(stubMapping -> !stubMappingIdsBeforeThisTest.contains(stubMapping.getId()))
                .forEach(WireMock::removeStub);
        FeatureStubber.clearCache(CONTEXT_NAME);
    }

    @BeforeEach
    void setup() {
        // hearingResultsHaveBeenSharedV2 listens for police-result-generated on these shared consumers
        createMessageConsumers();
        correlationIdAndMasterDefendantAddedConsumer = privateEvents.createConsumer("results.event.correlation-id-and-masterdefendant-added");
        hearingFinancialResultsUpdatedConsumer = privateEvents.createConsumer("results.event.hearing-financial-results-updated");
        ncesEmailNotificationRequestedConsumer = privateEvents.createConsumer(RESULTS_EVENT_NCES_NOTIFICATION_REQUESTED);
        hearingResultsAddedForDayConsumer = privateEvents.createConsumer(RESULTS_EVENT_HEARING_RESULTS_ADDED_FOR_DAY);
        documentDeletedFromBlobConsumer = privateEvents.createConsumer(DOCUMENT_DELETED_FROM_BLOB);
    }

    @AfterEach
    void tearDown() throws JMSException {
        correlationIdAndMasterDefendantAddedConsumer.close();
        hearingFinancialResultsUpdatedConsumer.close();
        ncesEmailNotificationRequestedConsumer.close();
        hearingResultsAddedForDayConsumer.close();
        documentDeletedFromBlobConsumer.close();
        closeMessageConsumers();
    }

    /**
     * An SJP hearing is resulted, its deemed-served financial results are traced through the
     * command API and staging-enforcement acknowledges the account, so results generates the NCES
     * documents and files each on both SJP cases. For one of them every context then reports back:
     * material adds it, SJP adds it to one case and rejects it for the other, and the notify gateway
     * sends the NCES email. With an SJP case failed the delivery is not complete, so neither
     * document's blob is deleted.
     */
    @Test
    void shouldTrackTheDeliveryOfAnNcesDocumentToMaterialSjpAndEmail() {
        final UUID sjpCaseAdded = randomUUID();
        final UUID sjpCaseRejected = randomUUID();
        setupSjpQueryStub(TRACE_CASE_URN_1, sjpCaseAdded);
        setupSjpQueryStub(TRACE_CASE_URN_2, sjpCaseRejected);

        final List<UUID> materialIds = ncesDocumentsAreGeneratedForAnSjpHearing(true);
        assertThat(materialIds, hasSize(2));

        // tracking starts once the document is stored, uploaded to material and filed on the SJP cases
        for (final UUID materialId : materialIds) {
            final DocumentDelivery started = documentDeliveryHelper.awaitDelivery(materialId);
            assertThat(started.documentType(), is(NCES));
            assertThat(started.materialStatus(), is(PENDING));
            assertThat(started.payloadBlobUri(), is(nullValue()));
            assertThat(started.documentBlobUri(), endsWith(NCES_DOCUMENT_BLOB_NAME_PREFIX + materialId + ".pdf"));
            assertThat(started.emailStatus(), is(nullValue()));
            assertSjpCase(materialId, sjpCaseAdded, PENDING);
            assertSjpCase(materialId, sjpCaseRejected, PENDING);
        }

        final UUID materialId = materialIds.get(0);
        final String documentBlobUri = documentDeliveryHelper.awaitDelivery(materialId).documentBlobUri();

        // material adds the document: it is delivered to material, and the NCES email with its link goes out
        documentDeliveryHelper.raiseMaterialAddedForNcesFileUriMaterial(materialId, getUserId());
        documentDeliveryHelper.awaitMaterialStatus(materialId, SUCCESS);
        final JsonObject email = awaitNcesEmailSentToTheNotifyGateway(materialId);
        documentDeliveryHelper.awaitEmailStatus(materialId, PENDING);

        // SJP adds the document to one case ...
        documentDeliveryHelper.raisePublicEvent(PUBLIC_EVENT_SJP_CASE_DOCUMENT_ADDED, createObjectBuilder()
                .add(CASE_ID, sjpCaseAdded.toString())
                .add("id", randomUUID().toString())
                .add(MATERIAL_ID, randomUUID().toString())
                .add("documentUri", documentBlobUri)
                .build());
        documentDeliveryHelper.awaitSjpStatus(materialId, sjpCaseAdded, SUCCESS);

        // ... and rejects it for the other
        documentDeliveryHelper.raisePublicEvent(PUBLIC_EVENT_SJP_CASE_DOCUMENT_UPLOAD_REJECTED, createObjectBuilder()
                .add(CASE_ID, sjpCaseRejected.toString())
                .add("documentId", randomUUID().toString())
                .add("documentReferenceUri", documentBlobUri)
                .add("description", "Case is referred to court for hearing")
                .build());
        documentDeliveryHelper.awaitSjpStatus(materialId, sjpCaseRejected, FAILED);

        // the notify gateway sends the email
        publishNotificationSent(CLIENT_CONTEXT_PREFIX + materialId, fromString(email.getString("notificationId")));
        documentDeliveryHelper.awaitEmailStatus(materialId, SUCCESS);

        // each outcome touched only its own status, and only this document
        final DocumentDelivery delivered = documentDeliveryHelper.awaitDelivery(materialId);
        assertThat(delivered.materialStatus(), is(SUCCESS));
        assertThat(delivered.emailStatus(), is(SUCCESS));
        assertThat(delivered.documentBlobUri(), is(documentBlobUri));
        assertSjpCase(materialId, sjpCaseAdded, SUCCESS);
        assertSjpCase(materialId, sjpCaseRejected, FAILED);

        final UUID otherMaterialId = materialIds.get(1);
        final DocumentDelivery other = documentDeliveryHelper.awaitDelivery(otherMaterialId);
        assertThat(other.materialStatus(), is(PENDING));
        assertThat(other.emailStatus(), is(nullValue()));
        assertSjpCase(otherMaterialId, sjpCaseAdded, PENDING);
        assertSjpCase(otherMaterialId, sjpCaseRejected, PENDING);

        // the blobs are kept: an SJP case failed, and the other document is still pending
        documentDeliveryHelper.assertNoDocumentDeletedFromBlob(documentDeletedFromBlobConsumer, materialId, otherMaterialId);
    }

    /**
     * One NCES document filed on two SJP cases, with every delivery succeeding: material adds it,
     * SJP adds it to both cases and the notify gateway sends the NCES email. The delivery is then
     * complete, so its blob is deleted.
     */
    @Test
    void shouldTrackAnNcesDocumentDeliveredToMaterialBothSjpCasesAndEmailAsSucceeded() {
        final UUID firstSjpCase = randomUUID();
        final UUID secondSjpCase = randomUUID();
        setupSjpQueryStub(TRACE_CASE_URN_1, firstSjpCase);
        setupSjpQueryStub(TRACE_CASE_URN_2, secondSjpCase);

        // without the consolidated accounts results only the deemed-served NCES document is generated
        final List<UUID> materialIds = ncesDocumentsAreGeneratedForAnSjpHearing(false);
        assertThat(materialIds, hasSize(1));
        final UUID materialId = materialIds.get(0);

        final String documentBlobUri = documentDeliveryHelper.awaitDelivery(materialId).documentBlobUri();
        assertSjpCase(materialId, firstSjpCase, PENDING);
        assertSjpCase(materialId, secondSjpCase, PENDING);

        documentDeliveryHelper.raiseMaterialAddedForNcesFileUriMaterial(materialId, getUserId());
        documentDeliveryHelper.awaitMaterialStatus(materialId, SUCCESS);
        final JsonObject email = awaitNcesEmailSentToTheNotifyGateway(materialId);
        documentDeliveryHelper.awaitEmailStatus(materialId, PENDING);

        for (final UUID sjpCase : List.of(firstSjpCase, secondSjpCase)) {
            documentDeliveryHelper.raisePublicEvent(PUBLIC_EVENT_SJP_CASE_DOCUMENT_ADDED, createObjectBuilder()
                    .add(CASE_ID, sjpCase.toString())
                    .add("id", randomUUID().toString())
                    .add(MATERIAL_ID, randomUUID().toString())
                    .add("documentUri", documentBlobUri)
                    .build());
            documentDeliveryHelper.awaitSjpStatus(materialId, sjpCase, SUCCESS);
        }

        publishNotificationSent(CLIENT_CONTEXT_PREFIX + materialId, fromString(email.getString("notificationId")));
        documentDeliveryHelper.awaitEmailStatus(materialId, SUCCESS);

        final DocumentDelivery delivered = documentDeliveryHelper.awaitDelivery(materialId);
        assertThat(delivered.documentType(), is(NCES));
        assertThat(delivered.materialStatus(), is(SUCCESS));
        assertThat(delivered.emailStatus(), is(SUCCESS));
        assertSjpCase(materialId, firstSjpCase, SUCCESS);
        assertSjpCase(materialId, secondSjpCase, SUCCESS);

        // delivered everywhere: the document blob is deleted; an NCES document has no payload blob
        final JsonPath documentDeletedFromBlob = documentDeliveryHelper.awaitDocumentDeletedFromBlob(documentDeletedFromBlobConsumer, materialId);
        assertThat(documentDeletedFromBlob.getString("documentBlobUri"), is(documentBlobUri));
        assertThat(documentDeletedFromBlob.getString("payloadBlobUri"), is(nullValue()));
    }

    /**
     * @param consolidatedAccounts whether the traced results also carry accounts to be consolidated,
     *                             which adds a second NCES document to the deemed-served one
     * @return the material ids of the NCES documents results generated
     */
    private List<UUID> ncesDocumentsAreGeneratedForAnSjpHearing(final boolean consolidatedAccounts) {
        final String masterDefendantId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();

        hearingResultsHaveBeenSharedV2(convertStringToJson(getPayload(SJP_HEARING_RESULTED)
                .replace("HEARING_ID", hearingId)
                .replace("MASTER_DEFENDANT_ID", masterDefendantId)));
        // track-results takes isSJPHearing from the hearing's ResultsAggregate, so the hearing must be stored first
        awaitHearingResultsAddedForDay(hearingId);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        final String tracePayload = deemedServedTracePayload(masterDefendantId, accountCorrelationId, hearingId);
        whenResultsAreTraced(consolidatedAccounts ? tracePayload : tracePayload.replace(CONSOLIDATED_ACCOUNTS_RESULT_CODE, OTHER_RESULT_CODE));
        assertThat(retrieveMessage(correlationIdAndMasterDefendantAddedConsumer).getString("correlationId"), is(accountCorrelationId));

        documentDeliveryHelper.raisePublicEvent(PUBLIC_EVENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT, createObjectBuilder()
                .add("originator", "courts")
                .add("requestId", accountCorrelationId)
                .add("exportStatus", "ENFORCEMENT_ACKNOWLEDGED")
                .add("updated", "2019-12-01T10:00:00Z")
                .add("acknowledgement", createObjectBuilder().add("accountNumber", "AER123451"))
                .build());
        assertThat(retrieveMessage(hearingFinancialResultsUpdatedConsumer).getString("correlationId"), is(accountCorrelationId));

        final List<JsonPath> ncesRequests = new ArrayList<>();
        for (JsonPath ncesRequest = retrieveMessage(ncesEmailNotificationRequestedConsumer); ncesRequest != null;
             ncesRequest = retrieveMessage(ncesEmailNotificationRequestedConsumer, NO_MORE_NCES_REQUESTS_TIMEOUT_IN_MILLIS)) {
            ncesRequests.add(ncesRequest);
        }
        return ncesRequests.stream().map(ncesRequest -> fromString(ncesRequest.getString(MATERIAL_ID))).toList();
    }

    private void assertSjpCase(final UUID materialId, final UUID caseId, final String sjpStatus) {
        assertThat(documentDeliveryHelper.findSjpStatus(materialId, caseId).orElseThrow(), is(sjpStatus));
    }

    /**
     * The NCES email of the file uri path goes to the notify gateway, with the material's link and
     * the client context its outcome comes back with, and names results' result queue to reply on.
     */
    private static JsonObject awaitNcesEmailSentToTheNotifyGateway(final UUID materialId) {
        final ServiceBusReceivedMessage message = awaitEmailSentToGateway(CLIENT_CONTEXT_PREFIX + materialId);
        assertThat(isSendEmailNotification(message), is(true));
        assertThat(message.getReplyTo(), is(RESULT_QUEUE_NAME));
        final JsonObject email = bodyOf(message);
        assertThat(email.getString("notificationId"), is(notNullValue()));
        assertThat(email.getString("sendToAddress"), is(notNullValue()));
        assertThat(email.getString("materialUrl"), endsWith(materialId + "?stream=true&requestPdf=true"));
        return email;
    }

    private void awaitHearingResultsAddedForDay(final String hearingId) {
        JsonPath hearingResultsAddedForDay;
        do {
            hearingResultsAddedForDay = retrieveMessage(hearingResultsAddedForDayConsumer);
            assertThat("hearing-results-added-for-day not raised for hearing " + hearingId, hearingResultsAddedForDay, is(notNullValue()));
        } while (!hearingId.equals(hearingResultsAddedForDay.getString("hearing.id")));
    }

    private static String deemedServedTracePayload(final String masterDefendantId, final String correlationId, final String hearingId) {
        return getPayload(TRACE_RESULT)
                .replace("MASTER_DEFENDANT_ID", masterDefendantId)
                .replace("CORRELATION_ID", correlationId)
                .replace("FINANCIAL", "true")
                .replace("DEEMED_SERVED", "true")
                .replace("HEARING_SITTING_DAY", "2018-09-30T09:30:00.000Z")
                .replace("HEARING_COURT_CENTRE_NAME", "South West London Magistrates Court")
                .replace("DEFENDANT_DATE_OF_BIRTH", "01-01-1988")
                .replace("DEFENDANT_ADDRESS", "176A Lavender Hill")
                .replace("DEFENDANT_EMAIL", "defendantEmail@gmail.com")
                .replace("DEFENDANT_CONTACT_NUMBER", "0206989859")
                .replace("HEARING_ID", hearingId);
    }
}
