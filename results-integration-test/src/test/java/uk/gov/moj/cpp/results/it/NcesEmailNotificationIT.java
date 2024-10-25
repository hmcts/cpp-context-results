package uk.gov.moj.cpp.results.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getEmailNotificationDetails;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.results.it.stub.DocumentGeneratorStub.verifyCreate;
import static uk.gov.moj.cpp.results.it.stub.NotificationServiceStub.verifyEmailNotificationIsRaised;
import static uk.gov.moj.cpp.results.it.stub.ProgressionStub.stubGetProgressionCaseExistsByUrn;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUsersGroupQueryStub;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonMetadata;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.it.stub.NotificationServiceStub;

import java.util.Arrays;
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
    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String RESULTS_EVENT_NCES_NOTIFICATION_REQUESTED = "results.event.nces-email-notification-requested";
    private static final String SOURCE = "originator-nces";
    private static final String ORIGINATOR_VALUE = "nces";

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    private MessageProducer messageProducerClientPrivate;
    private MessageProducer messageProducerClientPublic;
    private MessageConsumer messagePrivateConsumer;
    private UUID userId;

    @BeforeAll
    public static void setupStubs() {
        setupUsersGroupQueryStub();
        stubGetProgressionCaseExistsByUrn("CaseReference",  randomUUID());
    }

    @BeforeEach
    public void setup() {
        userId = getUserId();

        messageProducerClientPrivate = privateEvents.createProducer();
        messageProducerClientPublic = publicEvents.createPublicProducer();
        messagePrivateConsumer = privateEvents.createConsumer("results.event.nces-email-notification");
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @AfterEach
    public void tearDown() throws JMSException {
        messageProducerClientPrivate.close();
        messageProducerClientPublic.close();
    }

    @Test
    public void shouldSendNcesNotificationRequested() {

        stubDocumentCreate(DOCUMENT_TEXT);
        NotificationServiceStub.setUp();


        final NcesEmailNotificationRequested ncesEmailNotificationRequested = generateNcesNotificationRequested();

        final JsonObject requestAsJson = objectToJsonObjectConverter.convert(ncesEmailNotificationRequested);

        final Metadata metadata = createMetadata(userId);

        sendMessage(messageProducerClientPrivate, RESULTS_EVENT_NCES_NOTIFICATION_REQUESTED, requestAsJson, metadata);

        verifyCreate(singletonList(ncesEmailNotificationRequested.getGobAccountNumber()));
        verifyNcesEmailNotificationDetails(userId, ncesEmailNotificationRequested);

        sendMaterialFileUploadedPublicEvent(ncesEmailNotificationRequested.getMaterialId(), userId, true);
        assertNotNull(retrieveMessage(messagePrivateConsumer));

        List<String> details = Arrays.asList("templateId","sendToAddress", "materialUrl", "subject", ncesEmailNotificationRequested.getSubject());
        verifyEmailNotificationIsRaised(details);

    }

    @Test
    public void shouldNotSendNcesNotificationRequestedForNonNcesOriginator() {

        stubDocumentCreate(DOCUMENT_TEXT);

        final NcesEmailNotificationRequested ncesEmailNotificationRequested = generateNcesNotificationRequested();

        final JsonObject requestAsJson = objectToJsonObjectConverter.convert(ncesEmailNotificationRequested);
        final Metadata metadata = createMetadata(userId);

        sendMessage(messageProducerClientPrivate, RESULTS_EVENT_NCES_NOTIFICATION_REQUESTED, requestAsJson, metadata);

        verifyCreate(singletonList(ncesEmailNotificationRequested.getGobAccountNumber()));

        sendMaterialFileUploadedPublicEvent(ncesEmailNotificationRequested.getMaterialId(), userId, false);
        assertNull(retrieveMessage(messagePrivateConsumer));


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

    private static void verifyNcesEmailNotificationDetails(final UUID userId, NcesEmailNotificationRequested ncesEmailNotificationRequested) {

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

