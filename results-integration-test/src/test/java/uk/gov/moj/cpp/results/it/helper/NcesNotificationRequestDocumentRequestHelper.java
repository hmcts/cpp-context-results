package uk.gov.moj.cpp.results.it.helper;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.sendMessage;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.messaging.Metadata;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;


public class NcesNotificationRequestDocumentRequestHelper {

    private static final String ORIGINATOR = "NCES_EMAIL_NOTIFICATION_REQUEST";

    protected MessageProducer publicMessageProducer;

    public NcesNotificationRequestDocumentRequestHelper() {
        publicMessageProducer = publicEvents.createPublicProducer();
    }

    public void sendSystemDocGeneratorPublicEvent(final UUID userId, final UUID materialId, final UUID payloadFileServiceId, final UUID documentFileServiceId, final String originatingSource, final Map<String, String> additionalInformation) {
        final String commandName = "public.systemdocgenerator.events.document-available";
        final Metadata metadata = getMetadataFrom(userId.toString(), materialId, commandName);
        sendMessage(publicMessageProducer, commandName, documentAvailablePayload(payloadFileServiceId, "OEE_Layout5", materialId.toString(), documentFileServiceId, originatingSource, additionalInformation), metadata);
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

    private JsonObject documentAvailablePayload(final UUID payloadFileServiceId, final String templateIdentifier, final String reportId, final UUID generatedDocumentId, final String originatingSource, final Map<String, String> additionalInformation) {
        final JsonArrayBuilder additionalInformationBuilder =  createArrayBuilder();
        additionalInformation.forEach((k, v) -> additionalInformationBuilder.add(createObjectBuilder()
                .add("propertyName", k)
                .add("propertyValue", v)
                .build()));
        return Json.createObjectBuilder()
                .add("payloadFileServiceId", payloadFileServiceId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("conversionFormat", "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("sourceCorrelationId", reportId)
                .add("originatingSource", originatingSource)
                .add("documentFileServiceId", generatedDocumentId.toString())
                .add("generatedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("generateVersion", 1)
                .add("additionalInformation", additionalInformationBuilder.build())
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

    public void closeMessageConsumers() throws JMSException {
        publicMessageProducer.close();
    }
}
