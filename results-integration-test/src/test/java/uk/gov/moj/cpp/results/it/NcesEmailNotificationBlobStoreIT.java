package uk.gov.moj.cpp.results.it;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.listAllStubMappings;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.nameUUIDFromBytes;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toSet;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.closeMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenSharedV2;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenResultsAreTraced;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.stub.DocumentGeneratorStub.stubRenderDocumentWithStatusOk;
import static uk.gov.moj.cpp.results.it.stub.SjpStub.setupSjpQueryStub;
import static uk.gov.moj.cpp.results.it.stub.SjpStub.stubUploadCaseDocument;
import static uk.gov.moj.cpp.results.it.stub.SjpStub.uploadCaseDocumentUrlFor;
import static uk.gov.moj.cpp.results.it.utils.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.results.it.utils.FeatureStubUtil.setFeatureToggle;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.convertStringToJson;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.retrieveMessages;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubBailStatuses;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrganisationUnit;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubJudicialResults;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubModeOfTrialReasons;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubProsecutorWithoutSpiOutOrPoliceFlag;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubSpiOutFlag;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.UPLOAD_MATERIAL_COMMAND;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubMaterialUploadFile;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.results.it.utils.WireMockStubUtils;

import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The NCES enforcement document stored in the Azure blob store (Azurite locally) instead of the
 * file service, behind {@code resultsFileStore}. With the toggle on the PDF rendered by
 * system-doc-generator is written to the blob store and every downstream context is handed the
 * blob uri: material fetches it via {@code fileUri}, and an SJP case document carries it as
 * {@code caseDocumentUri} alongside the uuid derived from it. No file service id goes anywhere.
 *
 * <p>The service writes the blob to Azurite, so the stack needs the cpp-developers-docker "azurite"
 * profile (see runIntegrationTests.sh); the test itself only checks what the service sends downstream.</p>
 */
class NcesEmailNotificationBlobStoreIT {

    private static final String RESULTS_FILE_STORE_FEATURE = "resultsFileStore";
    private static final String RESULTS_EVENT_NCES_NOTIFICATION_REQUESTED = "results.event.nces-email-notification-requested";
    private static final String RESULTS_EVENT_HEARING_RESULTS_ADDED_FOR_DAY = "results.events.hearing-results-added-for-day";
    private static final String NCES_PDF_CONTENT = "nces-enforcement-document-" + randomUUID();
    // the NCES document file name is ENFDocumentOrder_<timestamp>.pdf; its blob swaps the timestamp for the material id
    private static final String NCES_DOCUMENT_BLOB_NAME_PREFIX = "ENFDocumentOrder_";
    private static final String PUBLIC_EVENT_TOPIC = "public.event";
    private static final String PUBLIC_EVENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT = "public.stagingenforcement.enforce-financial-imposition-acknowledgement";
    private static final String SJP_HEARING_RESULTED = "json/public.events.hearing.hearing-resulted_for_staging_enforcement_for_sjp.json";
    private static final String TRACE_RESULT = "json/results.api.trace-results.json";
    private static final String SJP_HEARING_PROSECUTOR_CODE = "DVLA";
    private static final String SJP_HEARING_PROSECUTOR_OU_CODE = "G94DV00";
    // the trace fixture's prosecutionCaseReferences become the NCES request's caseReferences
    private static final String TRACE_CASE_URN_1 = "REF1";
    private static final String TRACE_CASE_URN_2 = "REF2";
    // a deemed-served financial imposition makes the aggregate raise two NCES requests once the account is acknowledged
    private static final int DEEMED_SERVED_NCES_REQUESTS = 2;

    private static Set<UUID> stubMappingIdsBeforeThisTest;

    private MessageConsumer correlationIdAndMasterDefendantAddedConsumer;
    private MessageConsumer hearingFinancialResultsUpdatedConsumer;
    private MessageConsumer ncesEmailNotificationRequestedConsumer;
    private MessageConsumer hearingResultsAddedForDayConsumer;

    @BeforeAll
    static void setupStubs() throws ClassNotFoundException {
        // WireMockStubUtils resets WireMock when it is first loaded; load it now so that reset can't
        // happen after the snapshot, then remember what was already stubbed so tearDown can put it back
        Class.forName(WireMockStubUtils.class.getName());
        stubMappingIdsBeforeThisTest = listAllStubMappings().getMappings().stream().map(StubMapping::getId).collect(toSet());

        setupUsersGroupQueryStub();
        stubRenderDocumentWithStatusOk(NCES_PDF_CONTENT);
        stubMaterialUploadFile();
        stubUploadCaseDocument();
        // the resulted SJP hearing also runs the hearing-results-added-for-day / police-results leg, which
        // reads reference data (court centre organisation unit, prosecutor, ...) - same set as HearingResultedIT;
        // no police flag keeps that leg out of the way of this test
        stubCountryNationalities();
        stubGetOrganisationUnit();
        stubJudicialResults();
        stubBailStatuses();
        stubModeOfTrialReasons();
        stubSpiOutFlag(false, false);
        // the SJP hearing fixture's prosecution authority: create-results-for-day looks it up by code
        stubProsecutorWithoutSpiOutOrPoliceFlag(SJP_HEARING_PROSECUTOR_CODE, SJP_HEARING_PROSECUTOR_OU_CODE);
        // last: loading WireMockStubUtils (setupUsersGroupQueryStub above) resets WireMock in its static
        // initialiser, which would wipe a feature stub registered before it
        setFeatureToggle(RESULTS_FILE_STORE_FEATURE, true);
    }

    /**
     * WireMock is shared by the whole suite: remove every stub this class added (the toggle, render,
     * reference-data, sjp and material stubs) so later ITs see exactly what they would have without it,
     * and drop the service's cached feature flags so resultsFileStore doesn't stay on.
     */
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
    }

    @AfterEach
    void tearDown() throws JMSException {
        correlationIdAndMasterDefendantAddedConsumer.close();
        hearingFinancialResultsUpdatedConsumer.close();
        ncesEmailNotificationRequestedConsumer.close();
        hearingResultsAddedForDayConsumer.close();
        closeMessageConsumers();
    }

    /**
     * Driven end to end from the outside: the SJP hearing is resulted (public event), its financial
     * results are tracked through the results command API with a deemed-served imposition, and
     * staging-enforcement acknowledges the account (public event). The aggregate then raises the
     * NCES requests itself - flagged SJP because the hearing was - carrying the trace payload's two
     * case references, so each NCES document must reach both SJP cases by its blob uri.
     */
    @Test
    void shouldStoreNcesDocumentInBlobStoreAndSendItsUriToMaterialAndSjpWhenSjpHearingIsResulted() {
        final String masterDefendantId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();
        final UUID sjpCaseId1 = randomUUID();
        final UUID sjpCaseId2 = randomUUID();
        setupSjpQueryStub(TRACE_CASE_URN_1, sjpCaseId1);
        setupSjpQueryStub(TRACE_CASE_URN_2, sjpCaseId2);

        hearingResultsHaveBeenSharedV2(convertStringToJson(getPayload(SJP_HEARING_RESULTED)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)));
        // track-results takes isSJPHearing from the hearing's ResultsAggregate, so the hearing must be stored first
        awaitHearingResultsAddedForDay(hearingId);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        whenResultsAreTraced(deemedServedTracePayload(masterDefendantId, accountCorrelationId, hearingId));
        assertThat(retrieveMessage(correlationIdAndMasterDefendantAddedConsumer).getString("correlationId"), is(accountCorrelationId));

        raisePublicEvent(PUBLIC_EVENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT, createObjectBuilder()
                .add("originator", "courts")
                .add("requestId", accountCorrelationId)
                .add("exportStatus", "ENFORCEMENT_ACKNOWLEDGED")
                .add("updated", "2019-12-01T10:00:00Z")
                .add("acknowledgement", createObjectBuilder().add("accountNumber", "AER123451"))
                .build());
        assertThat(retrieveMessage(hearingFinancialResultsUpdatedConsumer).getString("correlationId"), is(accountCorrelationId));

        final List<JsonPath> ncesRequests = retrieveMessages(ncesEmailNotificationRequestedConsumer, DEEMED_SERVED_NCES_REQUESTS);
        assertThat(ncesRequests, hasSize(DEEMED_SERVED_NCES_REQUESTS));
        ncesRequests.forEach(ncesRequest -> {
            assertThat(ncesRequest.getString("masterDefendantId"), is(masterDefendantId));
            assertThat(ncesRequest.getBoolean("isSJPHearing"), is(true));
            assertThat(ncesRequest.getString("caseReferences"), is(TRACE_CASE_URN_1 + "," + TRACE_CASE_URN_2));
            assertNcesDocumentSentByUriToMaterialAndSjpCases(ncesRequest.getString("materialId"), sjpCaseId1, sjpCaseId2);
        });
    }

    private void assertNcesDocumentSentByUriToMaterialAndSjpCases(final String materialId, final UUID... sjpCaseIds) {
        // material is asked to fetch the blob by uri, and no file service id goes with it
        final JsonObject uploadFile = awaitSingleRequestBody(postRequestedFor(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .withRequestBody(matchingJsonPath("$.materialId", equalTo(materialId))));
        final String fileUri = uploadFile.getString("fileUri");
        assertThat(uploadFile.containsKey("fileServiceId"), is(false));

        // the blob is named <document name>_<material id>.<extension>
        assertThat(fileUri, endsWith(NCES_DOCUMENT_BLOB_NAME_PREFIX + materialId + ".pdf"));

        // every sjp case gets the document addressed by the same uri; its id is the uuid sjp would derive from it
        for (final UUID sjpCaseId : sjpCaseIds) {
            final JsonObject uploadCaseDocument = awaitSingleRequestBody(postRequestedFor(urlPathMatching(uploadCaseDocumentUrlFor(sjpCaseId)))
                    .withRequestBody(matchingJsonPath("$.caseDocumentUri", equalTo(fileUri))));
            assertThat(uploadCaseDocument.getString("caseDocument"), is(nameUUIDFromBytes(fileUri.getBytes(UTF_8)).toString()));
        }
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
                .replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("CORRELATION_ID", correlationId)
                .replaceAll("FINANCIAL", "true")
                .replaceAll("DEEMED_SERVED", "true")
                .replaceAll("HEARING_SITTING_DAY", "2018-09-30T09:30:00.000Z")
                .replaceAll("HEARING_COURT_CENTRE_NAME", "South West London Magistrates Court")
                .replaceAll("DEFENDANT_DATE_OF_BIRTH", "01-01-1988")
                .replaceAll("DEFENDANT_ADDRESS", "176A Lavender Hill")
                .replaceAll("DEFENDANT_EMAIL", "defendantEmail@gmail.com")
                .replaceAll("DEFENDANT_CONTACT_NUMBER", "0206989859")
                .replaceAll("HEARING_ID", hearingId);
    }

    private static void raisePublicEvent(final String eventName, final JsonObject payload) {
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);
            messageProducer.sendMessage(eventName, payload);
        }
    }

    private static JsonObject awaitSingleRequestBody(final RequestPatternBuilder requestPattern) {
        await().atMost(30, SECONDS).pollInterval(1, SECONDS).until(() -> !findAll(requestPattern).isEmpty());
        final List<LoggedRequest> requests = findAll(requestPattern);
        assertThat(requests, hasSize(1));
        try (JsonReader reader = Json.createReader(new StringReader(requests.get(0).getBodyAsString()))) {
            return reader.readObject();
        }
    }
}
