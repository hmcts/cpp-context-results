package uk.gov.moj.cpp.results.it.helper;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.results.it.utils.QueueUtilForPrivateEvents.retrieveMessage;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.it.utils.QueueUtil;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;


public class NcesNotificationRequestDocumentRequestHelper {

    private String EVENT_SELECTOR_NCES_NOTIFICATION_REQUEST_DOCUMENT_REQUEST = "results.event.nces-email-notification";
    private String EVENT_SELECTOR_NCES_NOTIFICATION_REQUEST_DOCUMENT_REQUESTED = "results.event.nces-email-notification-requested";

    private static final String ORIGINATOR = "NCES_EMAIL_NOTIFICATION_REQUEST";

    protected MessageConsumer privateEventsConsumer;

    protected MessageProducer publicMessageProducer;

    public NcesNotificationRequestDocumentRequestHelper() {
        privateEventsConsumer = QueueUtil.privateEvents.createPrivateConsumer(EVENT_SELECTOR_NCES_NOTIFICATION_REQUEST_DOCUMENT_REQUEST);
        publicMessageProducer = QueueUtil.publicEvents.createPublicProducer();
    }

    public void sendSystemDocGeneratorPublicEvent(final UUID userId, final UUID materialId, final UUID payloadFileServiceId, final UUID documentFileServiceId) {
        final String commandName = "public.systemdocgenerator.events.document-available";
        final Metadata metadata = getMetadataFrom(userId.toString(), materialId, commandName);
        sendMessage(publicMessageProducer, commandName, documentAvailablePayload(payloadFileServiceId, "OEE_Layout5", materialId.toString(), documentFileServiceId), metadata);
    }

    public void sendSystemDocGeneratorPublicFailedEvent(final UUID userId, final UUID materialId, final UUID payloadFileServiceId) {
        final String commandName = "public.systemdocgenerator.events.generation-failed";
        final Metadata metadata = getMetadataFrom(userId.toString(), materialId, commandName);
        sendMessage(publicMessageProducer, commandName, documentFailedPayload(payloadFileServiceId, "OEE_Layout5", materialId.toString()), metadata);
    }

    private JsonObject documentFailedPayload(final UUID payloadFileServiceId, final String templateIdentifier, final String reportId) {
        return Json.createObjectBuilder()
                .add("payloadFileServiceId", payloadFileServiceId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("conversionFormat", "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("sourceCorrelationId", reportId)
                .add("originatingSource", ORIGINATOR)
                .add("failedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("reason", "Test")
                .build();
    }

    private JsonObject documentAvailablePayload(final UUID payloadFileServiceId, final String templateIdentifier, final String reportId, final UUID generatedDocumentId) {
        return Json.createObjectBuilder()
                .add("payloadFileServiceId", payloadFileServiceId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("conversionFormat", "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("sourceCorrelationId", reportId)
                .add("originatingSource", ORIGINATOR)
                .add("documentFileServiceId", generatedDocumentId.toString())
                .add("generatedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("generateVersion", 1)
                .build();
    }

    private Metadata getMetadataFrom(final String userId, final UUID materialId, final String name) {
        return metadataFrom(Json.createObjectBuilder()
                .add(ORIGINATOR, materialId.toString())
                .add(ID, randomUUID().toString())
                .add(HeaderConstants.USER_ID, userId)
                .add(NAME, name)
                .build()).build();
    }

    public void verifyNcesDocumentRequestRecordedPrivateTopic(final String materialId) {
        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        assertThat(jsonResponse.get("materialId"), is(materialId));
    }
}
