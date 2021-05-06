package uk.gov.moj.cpp.results.it;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.results.it.stub.NotificationServiceStub.verifyEmailNotificationIsRaised;
import static uk.gov.moj.cpp.results.it.utils.QueueUtilForPrivateEvents.privateEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtilForPrivateEvents.sendMessage;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubMaterialUploadFile;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.it.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.results.it.stub.NotificationServiceStub;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({"squid:S1607"})
public class NCESNotificationIT {

    private static final MessageProducer messageProducerClientPrivate = privateEvents.createProducer();

    private static final String DOCUMENT_TEXT = STRING.next();

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    private static final String RESULTS_EVENT_NCES_NOTIFICATION_REQUESTED = "results.event.nces-email-notification-requested";

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldSendNCESNotification() {
        NotificationServiceStub.setUp();

        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);

        final NcesEmailNotificationRequested ncesEmailNotificationRequested = generateNcesNotificationRequested();

        final JsonObject requestAsJson = objectToJsonObjectConverter.convert(ncesEmailNotificationRequested);
        final Metadata metadata = createMetadata(RESULTS_EVENT_NCES_NOTIFICATION_REQUESTED);
        stubMaterialUploadFile();
        sendMessage(messageProducerClientPrivate, RESULTS_EVENT_NCES_NOTIFICATION_REQUESTED, requestAsJson,
                metadata);

        DocumentGeneratorStub.verifyCreate(singletonList(ncesEmailNotificationRequested.getGobAccountNumber()));
        List<String> details = Arrays.asList("subject", ncesEmailNotificationRequested.getSubject());
            verifyEmailNotificationIsRaised(details);
    }

    @After
    public void tearDown() throws JMSException {
        messageProducerClientPrivate.close();
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
                .withOldGobAccountNumber("OldGobAccountNumber")
                .withSubject("Subject")
                .withSendTo("SendTo@gmail.com")
                .withOldDivisionCode("OldDivisionCode").build();
    }

    private Metadata createMetadata(final String eventName) {
        return metadataBuilder()
                .withId(randomUUID())
                .withStreamId(randomUUID())
                .withPosition(1)
                .withPreviousEventNumber(123)
                .withEventNumber(new Random().nextLong())
                .withSource("event-indexer-test")
                .withName(eventName)
                .withUserId(randomUUID().toString())
                .build();
    }
}

