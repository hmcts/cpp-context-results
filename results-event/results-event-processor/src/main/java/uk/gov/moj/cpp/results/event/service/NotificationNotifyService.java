package uk.gov.moj.cpp.results.event.service;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.results.event.service.ConversionFormat.PDF;
import static uk.gov.moj.cpp.results.event.service.NcesEmailNotificationTemplateData.ncesEmailNotificationTemplateData;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationNotifyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(uk.gov.moj.cpp.results.event.service.NotificationNotifyService.class);
    private static final String NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE = "notificationnotify.send-email-notification";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private FileStorer fileStorer;

    @Inject
    private SystemDocGenerator systemDocGenerator;


    public void sendEmailNotification(final JsonEnvelope event, final JsonObject emailNotification) {

        LOGGER.info("sending email notification for event: {}", event.toObfuscatedDebugString());

        final Envelope<JsonObject> jsonObjectEnvelope = envelop(emailNotification)
                .withName(NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE)
                .withMetadataFrom(event);
        sender.sendAsAdmin(jsonObjectEnvelope);

    }

    public void sendNcesEmail(final EmailNotification emailNotification, final JsonEnvelope envelope) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("notificationId", emailNotification.getNotificationId().toString())
                .add("templateId", emailNotification.getTemplateId().toString())
                .add("sendToAddress", emailNotification.getSendToAddress())
                .add("materialUrl", emailNotification.getMaterialUrl());

        emailNotification.getSubject().ifPresent(subject ->
                payload.add("personalisation", createObjectBuilder().add("subject", subject)));

        final JsonEnvelope envelopeToSend = envelopeFrom(
                JsonEnvelope.metadataFrom(envelope.metadata()).withName(NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE), payload);
        sender.send(envelopeToSend);
    }

    public UUID generateNotification(final NcesEmailNotificationRequested ncesEmailNotificationRequested, final JsonEnvelope envelope) throws FileServiceException {
        final NcesEmailNotificationTemplateData templatePayload = createTemplatePayload(ncesEmailNotificationRequested);
        final UUID masterDefendantId = ncesEmailNotificationRequested.getMasterDefendantId();
        final UUID fileId = storeEmailAttachmentTemplatePayload(masterDefendantId, templatePayload);
        requestEmailAttachmentGeneration(masterDefendantId.toString(), fileId, envelope);
        return fileId;
    }

    private NcesEmailNotificationTemplateData createTemplatePayload(final NcesEmailNotificationRequested initiated) {

        final NcesEmailNotificationTemplateData data = ncesEmailNotificationTemplateData()
                .withSendTo(initiated.getSendTo())
                .withSubject(initiated.getSubject())
                .withCaseReferences(initiated.getCaseReferences())
                .withDefendantName(initiated.getDefendantName())
                .withMasterDefendantId(initiated.getMasterDefendantId().toString())
                .build();

        if (isNotEmpty(initiated.getListedDate())) {
            data.setListedDate(initiated.getListedDate());
        }

        if (isNotEmpty(initiated.getDivisionCode())) {
            data.setDivisionCode(initiated.getDivisionCode());
        }

        if (isNotEmpty(initiated.getOldDivisionCode())) {
            data.setOldDivisionCode(initiated.getOldDivisionCode());
        }

        if (isNotEmpty(initiated.getGobAccountNumber())) {
            data.setGobAccountNumber(initiated.getGobAccountNumber());
        }

        if (isNotEmpty(initiated.getOldGobAccountNumber())) {
            data.setOldGobAccountNumber(initiated.getOldGobAccountNumber());
        }

        if (isNotEmpty(initiated.getAmendmentDate())) {
            data.setAmendmentDate(initiated.getAmendmentDate());
        }

        if (isNotEmpty(initiated.getAmendmentReason())) {
            data.setAmendmentReason(initiated.getAmendmentReason());
        }

        if (initiated.getImpositionOffenceDetails() != null) {
            data.setImpositionOffenceDetails(initiated.getImpositionOffenceDetails());
        }

        if (isNotEmpty(initiated.getDateDecisionMade())) {
            data.setDateDecisionMade(initiated.getDateDecisionMade());
        }

        return data;
    }

    private UUID storeEmailAttachmentTemplatePayload(final UUID masterDefendantId,
                                                     final NcesEmailNotificationTemplateData templateData) throws FileServiceException {
        final JsonObject metadata = metadata(fileName(masterDefendantId));
        return fileStorer.store(metadata, toInputStream(templateData));
    }

    private JsonObject metadata(final String fileName) {
        return createObjectBuilder()
                .add("fileName", fileName)
                .add("conversionFormat", PDF.getValue())
                .add("templateName", TemplateIdentifier.NCES_EMAIL_NOTIFICATION_TEMPLATE_ID.getValue())
                .build();
    }

    private String fileName(final UUID masterDefendantId) {
        return format("nces-pending-email-%s.json", masterDefendantId);
    }

    private ByteArrayInputStream toInputStream(final NcesEmailNotificationTemplateData templateData) {
        final byte[] jsonPayloadInBytes = objectToJsonObjectConverter.convert(templateData).toString().getBytes(UTF_8);
        return new ByteArrayInputStream(jsonPayloadInBytes);
    }

    private void requestEmailAttachmentGeneration(final String sourceCorrelationId, final UUID fileId, final JsonEnvelope envelope) {
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                TemplateIdentifier.NCES_EMAIL_NOTIFICATION_TEMPLATE_ID,
                PDF,
                sourceCorrelationId,
                fileId
        );
        systemDocGenerator.generateDocument(request, envelope);
    }
}
