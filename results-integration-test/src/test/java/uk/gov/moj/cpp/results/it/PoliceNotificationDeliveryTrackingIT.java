package uk.gov.moj.cpp.results.it;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.listAllStubMappings;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.closeMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenSharedV2;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPublicEventPoliceResultGeneratedAndReturnPayload;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.stub.DocumentGeneratorStub.stubDocGeneratorEndPoint;
import static uk.gov.moj.cpp.results.it.stub.NotificationNotifyServiceStub.setupNotificationNotifyStubs;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.RESULT_QUEUE_NAME;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.awaitEmailSentToGateway;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.bodyOf;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.isSendEmailNotification;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.publishNotificationSent;
import static uk.gov.moj.cpp.results.it.stub.ProgressionStub.stubGetProgressionCaseExistsByUrn;
import static uk.gov.moj.cpp.results.it.stub.ProgressionStub.stubGetProgressionProsecutionCasesFromPayload;
import static uk.gov.moj.cpp.results.it.utils.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.results.it.utils.FeatureStubUtil.setFeatureToggle;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubBailStatuses;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrganisationUnit;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubJudicialResults;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubModeOfTrialReasons;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubSpiOutFlag;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.UPLOAD_MATERIAL_COMMAND;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubMaterialUploadFile;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.results.it.helper.DocumentDeliveryHelper;
import uk.gov.moj.cpp.results.it.helper.DocumentDeliveryHelper.DocumentDelivery;
import uk.gov.moj.cpp.results.it.utils.WireMockStubUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The delivery tracking of the police amend and re-share notification whose payload is stored in
 * the blob store ({@code resultsFileStore} on): results records in its view store where the
 * document stands with material and with the police email that carries its link. No SJP case.
 *
 * <p>Driven by public events only - hearing's shares, then system-doc-generator and material
 * reporting back - and the notify gateway's replies; no results private event is raised by the
 * test. Needs the "azurite" profile of the stack, and the "servicebus" one for the notify gateway's
 * queues.</p>
 */
class PoliceNotificationDeliveryTrackingIT {

    private static final String RESULTS_FILE_STORE_FEATURE = "resultsFileStore";
    private static final String GENERATE_DOCUMENT_URL = "/.*/rest/systemdocgenerator/generate-document";
    private static final String PUBLIC_EVENT_DOCUMENT_AVAILABLE = "public.systemdocgenerator.events.document-available";
    private static final String FIRST_SHARE = "json/public-hearing-results-shared/firstshare-onecase-onedefendant-oneoffence.json";
    private static final String RESHARE = "json/public-hearing-results-shared/reshare-onecase-onedefendant-oneoffence.json";
    private static final String PROSECUTION_CASE = "stub-data/hearing-resulted/prosecutioncase-onedefendant-oneoffence.json";
    private static final String CLIENT_CONTEXT_PREFIX = "RESULTS_";

    private static final String POLICE_NOTIFICATION = "POLICE_NOTIFICATION";
    private static final String PENDING = "PENDING";
    private static final String SUCCESS = "SUCCESS";

    private static Set<UUID> stubMappingIdsBeforeThisTest;

    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    private final DocumentDeliveryHelper documentDeliveryHelper = new DocumentDeliveryHelper();
    private final String policeEmailAddress = randomAlphabetic(15) + "@email.com";

    @BeforeAll
    static void setupStubs() throws ClassNotFoundException {
        // WireMockStubUtils resets WireMock when it is first loaded; load it now so that reset can't
        // happen after the snapshot, then remember what was already stubbed so tearDown can put it back
        Class.forName(WireMockStubUtils.class.getName());
        stubMappingIdsBeforeThisTest = listAllStubMappings().getMappings().stream().map(StubMapping::getId).collect(toSet());

        setupUserAsPrisonAdminGroup(getUserId());
        stubCountryNationalities();
        stubGetOrganisationUnit();
        stubJudicialResults();
        stubBailStatuses();
        stubModeOfTrialReasons();
        setupNotificationNotifyStubs();
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        stubMaterialUploadFile();
        stubDocGeneratorEndPoint();
        setFeatureToggle(RESULTS_FILE_STORE_FEATURE, true);
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
        stubSpiOutFlag(true, true, policeEmailAddress);
        createMessageConsumers();
    }

    @AfterEach
    void tearDown() {
        closeMessageConsumers();
    }

    /**
     * A hearing is shared and then re-shared with amended results, so results asks
     * system-doc-generator for the police notification. System-doc-generator then makes the document
     * available, material adds it and the notify gateway sends the police email.
     */
    @Test
    void shouldTrackAPoliceNotificationDeliveredToMaterialAndEmailAsSucceeded() {
        final UUID caseId = randomUUID();
        final String caseUrn = randomAlphanumeric(11).toUpperCase();
        final UUID hearingId = randomUUID();
        stubGetProgressionProsecutionCasesFromPayload(PROSECUTION_CASE, caseId, caseUrn, hearingId, null);
        stubGetProgressionCaseExistsByUrn(caseUrn, caseId);

        hearingResultsHaveBeenSharedV2(publicHearingResulted(FIRST_SHARE, caseId, caseUrn, hearingId));
        // the first share must be processed before the re-share, so that the re-share is an amendment
        assertThat(verifyPublicEventPoliceResultGeneratedAndReturnPayload().isPresent(), is(true));
        hearingResultsHaveBeenSharedV2(publicHearingResulted(RESHARE, caseId, caseUrn, hearingId));

        final JsonObject generateDocument = awaitSingleRequestBody(postRequestedFor(urlPathMatching(GENERATE_DOCUMENT_URL))
                .withRequestBody(matchingJsonPath("$.additionalInformation[?(@.propertyName == 'URN' && @.propertyValue == '" + caseUrn + "')]")));
        // the notification's id is the request's source correlation id and the id of its material
        final UUID materialId = fromString(generateDocument.getString("sourceCorrelationId"));
        final String payloadFileUri = generateDocument.getString("payloadFileUri");
        final String destinationFileUri = generateDocument.getString("destinationFileUri");

        // tracking starts once the document is requested from system-doc-generator
        final DocumentDelivery started = documentDeliveryHelper.awaitDelivery(materialId);
        assertThat(started.documentType(), is(POLICE_NOTIFICATION));
        assertThat(started.materialStatus(), is(PENDING));
        assertThat(started.payloadBlobUri(), is(payloadFileUri));
        assertThat(started.documentBlobUri(), is(destinationFileUri));
        assertThat(started.emailStatus(), is(nullValue()));

        // system-doc-generator makes the document available: results uploads it to material by its uri and
        // sends the police email with its link
        documentDeliveryHelper.raisePublicEvent(PUBLIC_EVENT_DOCUMENT_AVAILABLE, createObjectBuilder()
                .add("payloadFileUri", payloadFileUri)
                .add("destinationFileUri", destinationFileUri)
                .add("templateIdentifier", generateDocument.getString("templateIdentifier"))
                .add("conversionFormat", generateDocument.getString("conversionFormat"))
                .add("requestedTime", "2026-11-12T10:00:00.000Z")
                .add("generatedTime", "2026-11-12T10:00:01.000Z")
                .add("generateVersion", 1)
                .add("sourceCorrelationId", materialId.toString())
                .add("originatingSource", generateDocument.getString("originatingSource"))
                .add("additionalInformation", generateDocument.getJsonArray("additionalInformation"))
                .build());
        awaitSingleRequestBody(postRequestedFor(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .withRequestBody(matchingJsonPath("$.materialId", equalTo(materialId.toString())))
                .withRequestBody(matchingJsonPath("$.fileUri", equalTo(destinationFileUri))));
        // the police email of the file uri path goes to the notify gateway, which replies on results' result queue
        final ServiceBusReceivedMessage emailMessage = awaitEmailSentToGateway(CLIENT_CONTEXT_PREFIX + materialId);
        assertThat(isSendEmailNotification(emailMessage), is(true));
        assertThat(emailMessage.getReplyTo(), is(RESULT_QUEUE_NAME));
        final JsonObject email = bodyOf(emailMessage);
        assertThat(email.getString("notificationId"), is(materialId.toString()));
        assertThat(email.getString("sendToAddress"), is(policeEmailAddress));
        assertThat(email.getString("materialUrl"), endsWith(materialId + "?stream=true&requestPdf=true"));
        documentDeliveryHelper.awaitEmailStatus(materialId, PENDING);

        // material adds the document
        documentDeliveryHelper.raiseMaterialAddedForNcesFileUriMaterial(materialId, getUserId());
        documentDeliveryHelper.awaitMaterialStatus(materialId, SUCCESS);

        // the notify gateway sends the email
        publishNotificationSent(CLIENT_CONTEXT_PREFIX + materialId, materialId);
        documentDeliveryHelper.awaitEmailStatus(materialId, SUCCESS);

        final DocumentDelivery delivered = documentDeliveryHelper.awaitDelivery(materialId);
        assertThat(delivered.documentType(), is(POLICE_NOTIFICATION));
        assertThat(delivered.materialStatus(), is(SUCCESS));
        assertThat(delivered.emailStatus(), is(SUCCESS));
        assertThat(delivered.payloadBlobUri(), is(payloadFileUri));
        assertThat(delivered.documentBlobUri(), is(destinationFileUri));
    }

    private PublicHearingResulted publicHearingResulted(final String path, final UUID caseId, final String caseUrn, final UUID hearingId) {
        try (InputStream inputStream = PoliceNotificationDeliveryTrackingIT.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(inputStream, is(notNullValue()));
            final String payload = IOUtils.toString(inputStream, defaultCharset())
                    .replace("HEARING_ID", hearingId.toString())
                    .replace("CASE_ID", caseId.toString())
                    .replace("CASE_URN", caseUrn);
            try (JsonReader reader = Json.createReader(new StringReader(payload))) {
                return jsonToObjectConverter.convert(reader.readObject(), PublicHearingResulted.class);
            }
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
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
