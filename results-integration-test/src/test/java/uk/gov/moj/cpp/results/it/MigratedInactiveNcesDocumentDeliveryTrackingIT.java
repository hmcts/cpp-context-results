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
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.results.it.helper.DocumentDeliveryHelper.DOCUMENT_DELETED_FROM_BLOB;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.stub.DocumentGeneratorStub.stubRenderDocumentWithStatusOk;
import static uk.gov.moj.cpp.results.it.stub.NotificationNotifyServiceStub.setupNotificationNotifyStubs;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.RESULT_QUEUE_NAME;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.awaitEmailSentToGateway;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.bodyOf;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.isSendEmailNotification;
import static uk.gov.moj.cpp.results.it.stub.NotifyGatewayStub.publishNotificationSent;
import static uk.gov.moj.cpp.results.it.utils.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.results.it.utils.FeatureStubUtil.enableResultsFileStoreWithDelete;
import static uk.gov.moj.cpp.results.it.utils.ProgressionServiceStub.stubPostForAddCourtDocument;
import static uk.gov.moj.cpp.results.it.utils.ProgressionServiceStub.stubQueryInactiveMigratedCases;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.retrieveMessages;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrganisationUnit;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupAsSystemUser;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubMaterialUploadFile;

import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.results.it.helper.DocumentDeliveryHelper;
import uk.gov.moj.cpp.results.it.helper.DocumentDeliveryHelper.DocumentDelivery;
import uk.gov.moj.cpp.results.it.utils.WireMockStubUtils;

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
 * The delivery tracking of the NCES document of a migrated inactive case stored in the blob store
 * ({@code resultsFileStore} on): results records in its view store where the document stands with
 * material and with the NCES email that carries its link. A crown court case, so no SJP case.
 *
 * <p>Driven by public events only - hearing's application notification, then material reporting
 * back - and the notify gateway's replies; no results private event is raised by the test. It reads
 * a private event only to learn the material ids. Needs the "azurite" profile of the stack, and the
 * "servicebus" one for the notify gateway's queues.</p>
 */
class MigratedInactiveNcesDocumentDeliveryTrackingIT {

    private static final String PUBLIC_EVENT_NCES_EMAIL_NOTIFICATION_FOR_APPLICATION = "public.hearing.nces-email-notification-for-application";
    private static final String MIGRATED_INACTIVE_NCES_EMAIL_NOTIFICATION_REQUESTED = "results.event.migrated-inactive-nces-email-notification-requested";
    private static final String HEARING_COURT_CENTRE_ID = "f8254db1-1683-483e-afb3-b87fde5a0a26";
    private static final String NCES_DOCUMENT_BLOB_NAME_PREFIX = "ENFDocumentOrder_";
    private static final String CLIENT_CONTEXT_PREFIX = "RESULTS_";

    private static final String NCES = "NCES";
    private static final String PENDING = "PENDING";
    private static final String SUCCESS = "SUCCESS";

    private static Set<UUID> stubMappingIdsBeforeThisTest;

    private final DocumentDeliveryHelper documentDeliveryHelper = new DocumentDeliveryHelper();

    private MessageConsumer migratedInactiveNcesEmailNotificationRequestedConsumer;
    private MessageConsumer documentDeletedFromBlobConsumer;

    @BeforeAll
    static void setupStubs() throws ClassNotFoundException {
        // WireMockStubUtils resets WireMock when it is first loaded; load it now so that reset can't
        // happen after the snapshot, then remember what was already stubbed so tearDown can put it back
        Class.forName(WireMockStubUtils.class.getName());
        stubMappingIdsBeforeThisTest = listAllStubMappings().getMappings().stream().map(StubMapping::getId).collect(toSet());

        stubRenderDocumentWithStatusOk("migrated-inactive-nces-document-" + randomUUID());
        stubMaterialUploadFile();
        stubPostForAddCourtDocument();
        setupNotificationNotifyStubs();
        stubGetOrganisationUnit();
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
        migratedInactiveNcesEmailNotificationRequestedConsumer = privateEvents.createConsumer(MIGRATED_INACTIVE_NCES_EMAIL_NOTIFICATION_REQUESTED);
        documentDeletedFromBlobConsumer = privateEvents.createConsumer(DOCUMENT_DELETED_FROM_BLOB);
    }

    @AfterEach
    void tearDown() throws JMSException {
        migratedInactiveNcesEmailNotificationRequestedConsumer.close();
        documentDeletedFromBlobConsumer.close();
    }

    /**
     * Hearing notifies an appeal application for a defendant with two migrated inactive cases, so
     * results generates an NCES document per case. Material then adds each document and the
     * notify gateway sends its email. Each delivery is then complete, so each document's blob is
     * deleted.
     */
    @Test
    void shouldTrackTheNcesDocumentsOfMigratedInactiveCasesDeliveredToMaterialAndEmailAsSucceeded() {
        final String masterDefendantId = randomUUID().toString();
        final String firstCaseId = randomUUID().toString();
        final String secondCaseId = randomUUID().toString();
        final UUID userId = getUserId();
        stubQueryInactiveMigratedCases(firstCaseId, masterDefendantId, secondCaseId);
        setupAsSystemUser(userId);

        documentDeliveryHelper.raisePublicEvent(PUBLIC_EVENT_NCES_EMAIL_NOTIFICATION_FOR_APPLICATION, createObjectBuilder()
                .add("applicationType", "APPEAL")
                .add("masterDefendantId", masterDefendantId)
                .add("listingDate", "01/12/2019")
                .add("caseUrns", createArrayBuilder().add("caseUrn1"))
                .add("caseIds", createArrayBuilder().add(firstCaseId).add(secondCaseId))
                .add("hearingCourtCentreName", "South West London Magistrates Court")
                .add("hearingCourtCentreId", HEARING_COURT_CENTRE_ID)
                .build());

        final List<JsonPath> ncesRequests = retrieveMessages(migratedInactiveNcesEmailNotificationRequestedConsumer, 2);
        assertThat(ncesRequests, hasSize(2));

        for (final JsonPath ncesRequest : ncesRequests) {
            final UUID materialId = fromString(ncesRequest.getString("materialId"));
            final String caseId = ncesRequest.getString("caseId");

            // tracking starts once the document is stored and uploaded to material
            final DocumentDelivery started = documentDeliveryHelper.awaitDelivery(materialId);
            assertThat(started.documentType(), is(NCES));
            assertThat(started.materialStatus(), is(PENDING));
            assertThat(started.payloadBlobUri(), is(nullValue()));
            assertThat(started.documentBlobUri(), endsWith(NCES_DOCUMENT_BLOB_NAME_PREFIX + materialId + ".pdf"));
            assertThat(started.emailStatus(), is(nullValue()));

            // material adds the document: it is delivered to material, and the NCES email with its link goes out
            documentDeliveryHelper.raiseMaterialAddedForMigratedInactiveNcesFileUriMaterial(materialId, userId, masterDefendantId, caseId);
            documentDeliveryHelper.awaitMaterialStatus(materialId, SUCCESS);
            final JsonObject email = awaitNcesEmailSentToTheNotifyGateway(materialId);
            documentDeliveryHelper.awaitEmailStatus(materialId, PENDING);

            // the notify gateway sends the email
            publishNotificationSent(CLIENT_CONTEXT_PREFIX + materialId, fromString(email.getString("notificationId")));
            documentDeliveryHelper.awaitEmailStatus(materialId, SUCCESS);

            final DocumentDelivery delivered = documentDeliveryHelper.awaitDelivery(materialId);
            assertThat(delivered.materialStatus(), is(SUCCESS));
            assertThat(delivered.emailStatus(), is(SUCCESS));
            assertThat(delivered.documentBlobUri(), is(started.documentBlobUri()));

            // delivered to material and by email, with no SJP case: the document blob is deleted
            final JsonPath documentDeletedFromBlob = documentDeliveryHelper.awaitDocumentDeletedFromBlob(documentDeletedFromBlobConsumer, materialId);
            assertThat(documentDeletedFromBlob.getString("documentBlobUri"), is(started.documentBlobUri()));
            assertThat(documentDeletedFromBlob.getString("payloadBlobUri"), is(nullValue()));
        }
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
}
