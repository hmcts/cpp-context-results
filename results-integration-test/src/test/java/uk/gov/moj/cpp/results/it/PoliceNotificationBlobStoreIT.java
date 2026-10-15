package uk.gov.moj.cpp.results.it;

import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.listAllStubMappings;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.net.URLDecoder.decode;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.closeMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenSharedV2;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPublicEventPoliceResultGeneratedAndReturnPayload;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.stub.DocumentGeneratorStub.stubDocGeneratorEndPoint;
import static uk.gov.moj.cpp.results.it.stub.NotificationNotifyServiceStub.setupNotificationNotifyStubs;
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
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubMaterialUploadFile;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
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
 * The police amend and re-share notification payload stored in the Azure blob store (Azurite
 * locally) instead of the file service, behind {@code resultsFileStore}. With the toggle on the
 * Thymeleaf payload is written under the payload prefix and system-doc-generator is asked to render
 * it by uri, into a destination of the same file name under the document prefix. No file service id
 * goes with the request.
 *
 * <p>The service writes the blob to Azurite, so the stack needs the cpp-developers-docker "azurite"
 * profile (see runIntegrationTests.sh); the test itself only checks what the service sends downstream.</p>
 */
class PoliceNotificationBlobStoreIT {

    private static final String RESULTS_FILE_STORE_FEATURE = "resultsFileStore";
    private static final String GENERATE_DOCUMENT_URL = "/.*/rest/systemdocgenerator/generate-document";
    private static final String POLICE_NOTIFICATION_HEARING_RESULTS = "POLICE_NOTIFICATION_HEARING_RESULTS";
    // AzureFileStoreBlobConfiguration defaults; the local stack does not override them
    private static final String PAYLOAD_BLOB_PREFIX = "sdg/requests/";
    private static final String DOCUMENT_BLOB_PREFIX = "sdg/documents/";
    private static final String FIRST_SHARE = "json/public-hearing-results-shared/firstshare-onecase-onedefendant-oneoffence.json";
    private static final String RESHARE = "json/public-hearing-results-shared/reshare-onecase-onedefendant-oneoffence.json";
    private static final String PROSECUTION_CASE = "stub-data/hearing-resulted/prosecutioncase-onedefendant-oneoffence.json";

    private static Set<UUID> stubMappingIdsBeforeThisTest;

    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
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
        // generate-document answers 202 at the path the service really calls
        stubDocGeneratorEndPoint();
        setFeatureToggle(RESULTS_FILE_STORE_FEATURE, true);
    }

    /**
     * WireMock is shared by the whole suite: remove every stub this class added so later ITs see
     * exactly what they would have without it, and drop the service's cached feature flags so
     * resultsFileStore doesn't stay on.
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
        stubSpiOutFlag(true, true, policeEmailAddress);
        createMessageConsumers();
    }

    @AfterEach
    void tearDown() {
        closeMessageConsumers();
    }

    /**
     * Driven from the outside: a hearing is shared, then re-shared with amended results. The re-share
     * raises the amend and re-share police notification, whose payload goes to the blob store and
     * whose document is requested from system-doc-generator by uri.
     */
    @Test
    void shouldStorePoliceNotificationPayloadInBlobStoreAndRequestItsDocumentByUriWhenHearingIsReshared() {
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

        assertThat(generateDocument.getString("originatingSource"), is(POLICE_NOTIFICATION_HEARING_RESULTS));
        assertThat(generateDocument.getString("templateIdentifier"), is("Police Notification Hearing Results"));
        assertThat(generateDocument.getString("conversionFormat"), is("thymeleaf"));
        // generate-document is a oneOf: the uri pair replaces the file service id
        assertThat(generateDocument.containsKey("payloadFileServiceId"), is(false));

        // the payload is named after the notification, which is also the request's source correlation id
        final String fileName = POLICE_NOTIFICATION_HEARING_RESULTS + generateDocument.getString("sourceCorrelationId") + ".html";
        // the blob client url-encodes the blob name, so the prefix's '/' arrives as %2F
        assertThat(decode(generateDocument.getString("payloadFileUri"), UTF_8), endsWith(PAYLOAD_BLOB_PREFIX + fileName));
        assertThat(decode(generateDocument.getString("destinationFileUri"), UTF_8), endsWith(DOCUMENT_BLOB_PREFIX + fileName));
    }

    private PublicHearingResulted publicHearingResulted(final String path, final UUID caseId, final String caseUrn, final UUID hearingId) {
        try (InputStream inputStream = PoliceNotificationBlobStoreIT.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(inputStream, is(notNullValue()));
            final String payload = IOUtils.toString(inputStream, defaultCharset())
                    .replaceAll("HEARING_ID", hearingId.toString())
                    .replaceAll("CASE_ID", caseId.toString())
                    .replaceAll("CASE_URN", caseUrn);
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
