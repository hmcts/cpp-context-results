package uk.gov.moj.cpp.results.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getEmailNotificationDetails;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.stub.DocumentGeneratorStub.stubDocGeneratorEndPoint;
import static uk.gov.moj.cpp.results.it.stub.NotificationNotifyServiceStub.verifyEmailNotificationIsRaised;
import static uk.gov.moj.cpp.results.it.stub.ProgressionStub.stubGetProgressionCaseExistsByUrn;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubMaterialUploadFile;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonMetadata;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.it.helper.NcesNotificationRequestDocumentRequestHelper;
import uk.gov.moj.cpp.results.it.stub.NotificationNotifyServiceStub;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class NcesEmailNotificationIT {

    private static final String MATERIAL_ID = "materialId";
    private static final String MATERIAL_MATERIAL_ADDED = "material.material-added";
    private static final String RESULTS_EVENT_NCES_NOTIFICATION_REQUESTED = "results.event.nces-email-notification-requested";
    private static final String SOURCE = "originator-nces";
    private static final String ORIGINATOR_VALUE = "nces";

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    private MessageProducer messageProducerClientPrivate;
    private MessageProducer messageProducerClientPublic;
    private MessageConsumer messagePrivateConsumer;
    private UUID userId;

    private NcesNotificationRequestDocumentRequestHelper ncesNotificationRequestDocumentRequestHelper;

    @BeforeAll
    public static void setupStubs() {
        setupUsersGroupQueryStub();
        stubGetProgressionCaseExistsByUrn("CaseReference",  randomUUID());
        stubDocGeneratorEndPoint();
        NotificationNotifyServiceStub.setupNotificationNotifyStubs();
        stubMaterialUploadFile();
    }

    @BeforeEach
    public void setup() {
        userId = getUserId();
        ncesNotificationRequestDocumentRequestHelper = new NcesNotificationRequestDocumentRequestHelper();

        messageProducerClientPrivate = privateEvents.createProducer();
        messageProducerClientPublic = publicEvents.createPublicProducer();
        messagePrivateConsumer = privateEvents.createConsumer("results.event.nces-email-notification");
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @AfterEach
    public void tearDown() throws JMSException {
        messageProducerClientPrivate.close();
        messageProducerClientPublic.close();
        messagePrivateConsumer.close();
        ncesNotificationRequestDocumentRequestHelper.closeMessageConsumers();

    }

    @Test
    public void shouldSendNcesNotificationRequestedAndCreateDocumentWithAsynchronousFlow() {

        final UUID USER_ID_VALUE_AS_ADMIN = randomUUID();

        final NcesEmailNotificationRequested ncesEmailNotificationRequested = generateNcesNotificationRequested();

        final JsonObject requestAsJson = objectToJsonObjectConverter.convert(ncesEmailNotificationRequested);

        final Metadata metadata = createMetadata(userId);

        sendMessage(messageProducerClientPrivate, RESULTS_EVENT_NCES_NOTIFICATION_REQUESTED, requestAsJson, metadata);

        final UUID documentFileServiceId = randomUUID();
        final UUID payloadFileServiceId = randomUUID();

        ncesNotificationRequestDocumentRequestHelper.sendSystemDocGeneratorPublicEvent(USER_ID_VALUE_AS_ADMIN,
                ncesEmailNotificationRequested.getMaterialId(), payloadFileServiceId, documentFileServiceId, "NCES_EMAIL_NOTIFICATION_REQUEST", new HashMap<>());

        verifyNcesEmailNotificationDetails(userId, ncesEmailNotificationRequested);

        sendMaterialFileUploadedPublicEvent(ncesEmailNotificationRequested.getMaterialId(), userId, true);
        assertNotNull(retrieveMessage(messagePrivateConsumer));

        List<String> details = Arrays.asList("templateId","sendToAddress", "materialUrl", "subject", ncesEmailNotificationRequested.getSubject());
        verifyEmailNotificationIsRaised(details);

    }

    public NcesEmailNotificationRequested generateNcesNotificationRequested() {
        return NcesEmailNotificationRequested.ncesEmailNotificationRequested()
                .withNotificationId(randomUUID())
                .withAmendmentDate("AmendmentDate")
                .withCaseReferences("CaseReference")
                .withAmendmentReason("AmendmentReason")
                .withDateDecisionMade("DateDecisionMade")
                .withDefendantName("DefendantName")
                .withDivisionCode("DivisionCode")
                .withGobAccountNumber("GobAccountNumber")
                .withListedDate("ListedDate")
                .withMasterDefendantId(UUID.randomUUID())
                .withMaterialId(randomUUID())
                .withOldGobAccountNumber("OldGobAccountNumber")
                .withSubject("Subject")
                .withSendTo("SendTo@gmail.com")
                .withOldDivisionCode("OldDivisionCode").build();
    }

    private Metadata createMetadata(UUID userId) {
        return metadataBuilder()
                .withId(randomUUID())
                .withStreamId(randomUUID())
                .withPosition(1)
                .withPreviousEventNumber(123)
                .withEventNumber(new Random().nextLong())
                .withSource("event-indexer-test")
                .withName(RESULTS_EVENT_NCES_NOTIFICATION_REQUESTED)
                .withUserId(userId.toString())
                .build();
    }

    private void sendMaterialFileUploadedPublicEvent(final UUID materialId, final UUID userId, final boolean originator) {
        final Metadata metadata;
        if (originator) {
            metadata = getMetadataFrom(userId.toString());
         } else {
            metadata = getMetadataFromWithoutOriginator(userId.toString());
        }
        final JsonObject payload = createObjectBuilder().add(MATERIAL_ID, materialId.toString()).add(
                "fileDetails",
                createObjectBuilder().add("alfrescoAssetId", "aGVsbG8=")
                        .add("mimeType", "text/plain").add("fileName", "file.txt"))
                .add("materialAddedDate", "2022-07-17T13:15:787.345").build();
        sendMessage(messageProducerClientPublic, MATERIAL_MATERIAL_ADDED, payload, metadata);
    }

    private Metadata getMetadataFrom(final String userId) {
        return metadataFrom(createObjectBuilder()
                .add(SOURCE, ORIGINATOR_VALUE)
                .add(ID, randomUUID().toString())
                .add(JsonMetadata.USER_ID, userId)
                .add(NAME, MATERIAL_MATERIAL_ADDED)
                .build()).build();
    }

    private Metadata getMetadataFromWithoutOriginator(final String userId) {
        return metadataFrom(createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(JsonMetadata.USER_ID, userId)
                .add(NAME, MATERIAL_MATERIAL_ADDED)
                .build()).build();
    }

    private void verifyNcesEmailNotificationDetails(final UUID userId, NcesEmailNotificationRequested ncesEmailNotificationRequested) {

        final Matcher[] matcher = {
                withJsonPath("$.id", notNullValue()),
                withJsonPath("$.materialId", equalTo(ncesEmailNotificationRequested.getMaterialId().toString())),
                withJsonPath("$.notificationId", equalTo(ncesEmailNotificationRequested.getNotificationId().toString())),
                withJsonPath("$.masterDefendantId", equalTo(ncesEmailNotificationRequested.getMasterDefendantId().toString())),
                withJsonPath("$.sendTo", equalTo(ncesEmailNotificationRequested.getSendTo())),
                withJsonPath("$.subject", equalTo(ncesEmailNotificationRequested.getSubject()))
        };

        getEmailNotificationDetails(userId, ncesEmailNotificationRequested.getMaterialId(), matcher);

    }
}

