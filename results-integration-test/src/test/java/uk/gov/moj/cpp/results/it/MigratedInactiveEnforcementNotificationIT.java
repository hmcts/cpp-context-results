package uk.gov.moj.cpp.results.it;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.stub.DocumentGeneratorStub.stubDocumentCreateWithStatusOk;
import static uk.gov.moj.cpp.results.it.utils.ProgressionServiceStub.stubQueryInactiveMigratedCases;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.removeMessagesFromQueue;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.results.it.utils.QueueUtilForPrivateEvents.privateEvents;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrganisationUnit;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupAsSystemUser;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubDocGeneratorEndPoint;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubDocumentCreate;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubMaterialUploadFile;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubNotificationNotifyEndPoint;

import uk.gov.justice.services.messaging.JsonMetadata;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.results.it.utils.QueueUtil;

import java.util.List;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MigratedInactiveEnforcementNotificationIT {


    private static final String MATERIAL_MATERIAL_ADDED = "material.material-added";

    public static final String CORRELATION_ID = "correlationId";
    public static final String MASTER_DEFENDANT_ID = "masterDefendantId";
    public static final String SUBJECT = "subject";
    public static final String DIVISION_CODE = "divisionCode";
    public static final String OLD_DIVISION_CODE = "oldDivisionCode";

    public static final String APPEAL_APPLICATION_RECEIVED = "APPEAL APPLICATION RECEIVED";


    private static final String MATERIAL_ID = "materialId";
    public static final String CASE_ID = "caseId";

    private final String HEARING_COURT_CENTRE_NAME = "hearingCourtCentreName";

    private static final String PUBLIC_EVENT_SEND_NCES_EMAIL_FOR_NEW_APPLICATION = "public.hearing.nces-email-notification-for-application";
    private static final String MIGRATED_INACTIVE_NCES_EMAIL_NOTIFICATION_REQUESTED = "results.event.migrated-inactive-nces-email-notification-requested";
    private static final String PUBLIC_EVENT_TOPIC = "public.event";


    private final String HEARING_COURT_CENTRE_NAME_VALUE = "South West London Magistrates Court";

    static MessageConsumer migratedInactiveNcesEmailEventConsumer;
    static MessageConsumer migratedInactiveNccesEventNotificationConsumer;

    public static final String RESULTS_INACTIVE_MIGRATED_NCES_EMAIL_NOTIFICATION = "results.event.migrated-inactive-nces-email-notification";

    private static final String SOURCE = "originator-nces";
    /**
     * Originator for migrated-inactive flow: "nces:" + masterDefendantId + ":" + caseId (MaterialAddedEventProcessor else branch)
     */
    private static final String ORIGINATOR_VALUE_NCES_CASEID = "nces:";

    static MessageProducer messageProducerClientPublic;


    @BeforeAll
    public static void beforeClass() {
        stubDocGeneratorEndPoint();
        stubNotificationNotifyEndPoint();
        stubDocumentCreate(STRING.next());
        stubDocumentCreateWithStatusOk(STRING.next());
        stubMaterialUploadFile();
        stubGetOrganisationUnit();
        migratedInactiveNcesEmailEventConsumer = privateEvents.createConsumer(MIGRATED_INACTIVE_NCES_EMAIL_NOTIFICATION_REQUESTED);
        migratedInactiveNccesEventNotificationConsumer = privateEvents.createConsumer(RESULTS_INACTIVE_MIGRATED_NCES_EMAIL_NOTIFICATION);
        messageProducerClientPublic = publicEvents.createPublicProducer();
    }

    @BeforeEach
    public void setUp() {
        removeMessagesFromQueue(migratedInactiveNcesEmailEventConsumer);
        removeMessagesFromQueue(migratedInactiveNccesEventNotificationConsumer);
        createMessageConsumers();
    }

    @AfterAll
    public static void tearDown() throws JMSException {
        migratedInactiveNcesEmailEventConsumer.close();
        migratedInactiveNccesEventNotificationConsumer.close();
    }


    @SuppressWarnings("java:S5961")
    @Test
    public void shouldProcessSendNcesMailForNewApplication() {
        final String masterDefendantId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        stubQueryInactiveMigratedCases(caseId, masterDefendantId);
        final UUID userId = getUserId();
        setupAsSystemUser(userId);
        final String HEARING_COURT_CENTRE_ID = "hearingCourtCentreId";
        final String hearingCourtCentreId = "f8254db1-1683-483e-afb3-b87fde5a0a26";


        final JsonObject ncesEmailPayload = createObjectBuilder()
                .add("applicationType", "APPEAL")
                .add(MASTER_DEFENDANT_ID, masterDefendantId)
                .add("listingDate", "01/12/2019")
                .add("caseUrns", createArrayBuilder().add("caseUrn1").build())
                .add("caseIds", createArrayBuilder().add(caseId).build())
                .add(HEARING_COURT_CENTRE_NAME, HEARING_COURT_CENTRE_NAME_VALUE)
                .add(HEARING_COURT_CENTRE_ID, hearingCourtCentreId)
                .build();

        raisePublicEventForAcknowledgement(ncesEmailPayload, PUBLIC_EVENT_SEND_NCES_EMAIL_FOR_NEW_APPLICATION);


        final List<JsonPath> migratedInactiveNcesEmailMessages = QueueUtil.retrieveMessages(migratedInactiveNcesEmailEventConsumer, 1);
        JsonPath jsonResponse = migratedInactiveNcesEmailMessages.stream().filter(jsonPath -> jsonPath.getString(SUBJECT).equalsIgnoreCase(APPEAL_APPLICATION_RECEIVED)).findFirst().orElseGet(() -> JsonPath.from("{}"));
        assertThat(jsonResponse.getString(SUBJECT), is(APPEAL_APPLICATION_RECEIVED));
        assertThat(jsonResponse.getString(DIVISION_CODE), is("6"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(CASE_ID), is(caseId));
        String materialId = jsonResponse.getString(MATERIAL_ID);
        assertThat(materialId, notNullValue());

        sendMaterialFileUploadedPublicEvent(UUID.fromString(materialId), userId, masterDefendantId, caseId);

        final List<JsonPath> migratedInactiveMaterialMessages = QueueUtil.retrieveMessages(migratedInactiveNccesEventNotificationConsumer, 1);
        jsonResponse = migratedInactiveMaterialMessages.stream().filter(jsonPath -> jsonPath.getString(MATERIAL_ID).equalsIgnoreCase(materialId)).findFirst().orElseGet(() -> JsonPath.from("{}"));
        assertThat(jsonResponse.getString(MASTER_DEFENDANT_ID), is(masterDefendantId));
        assertThat(jsonResponse.getString(MATERIAL_ID), is(materialId));

    }


    /**
     * Sends material.material-added with originator "nces:masterDefendantId:caseId" so
     * MaterialAddedEventProcessor hits the else branch (processMigratedInactiveNcesDocumentNotification)
     * and sends result.command.migrated-inactive-nces-document-notification to the command handler.
     */
    private void sendMaterialFileUploadedPublicEvent(final UUID materialId, final UUID userId, final String masterDefendantId, final String caseId) {
        final Metadata metadata = getMetadataFromForMigratedInactive(userId.toString(), masterDefendantId, caseId);
        final JsonObject payload = createObjectBuilder().add(MATERIAL_ID, materialId.toString()).add(
                        "fileDetails",
                        createObjectBuilder().add("alfrescoAssetId", "aGVsbG8=")
                                .add("mimeType", "text/plain").add("fileName", "file.txt"))
                .add("materialAddedDate", "2022-07-17T13:15:787.345").build();
        sendMessage(messageProducerClientPublic, MATERIAL_MATERIAL_ADDED, payload, metadata);
    }

    private Metadata getMetadataFromForMigratedInactive(final String userId, final String masterDefendantId, final String caseId) {
        final String originator = ORIGINATOR_VALUE_NCES_CASEID + masterDefendantId + ":" + caseId;
        return metadataFrom(createObjectBuilder()
                .add(SOURCE, originator)
                .add(ID, randomUUID().toString())
                .add(JsonMetadata.USER_ID, userId)
                .add(NAME, MATERIAL_MATERIAL_ADDED)
                .build()).build();
    }


    private void raisePublicEventForAcknowledgement(final JsonObject payload, final String eventName) {
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);
            messageProducer.sendMessage(eventName, payload);
        }
    }

}
