package uk.gov.moj.cpp.results.command.handler;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultsAggregate;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotification;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class NcesDocumentNotificationCommandHandler extends AbstractCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NcesDocumentNotificationCommandHandler.class);
    private static final String MATERIAL_ID = "materialId";
    private static final String MATERIAL_URL = "materialUrl";

    @Inject
    @Value(key = "ncesEmailNotificationTemplateId")
    private String ncesEmailNotificationTemplateId;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private Requester requester;

    @Inject
    public NcesDocumentNotificationCommandHandler(final EventSource eventSource, final Enveloper enveloper, final AggregateService aggregateService,
                                                final JsonObjectToObjectConverter jsonObjectToObjectConverter, final Requester requester) {
        super(eventSource, enveloper, aggregateService);
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
        this.requester = requester;
    }

    @Handles("results.command.nces-document-notification")
    public void processNcesEmailNotification(final JsonEnvelope envelope) throws EventStreamException {
        LOGGER.info("Received EmailToNcesNotificationEvent {}", envelope.toObfuscatedDebugString());
        final String materialUrl = envelope.payloadAsJsonObject().getString(MATERIAL_URL);

        final JsonObject ncesEmailNotificationDetailsObj = getNcesEmailNotificationDetails(envelope);
        final NcesEmailNotificationRequested ncesEmailNotificationDetails = jsonObjectToObjectConverter.convert(ncesEmailNotificationDetailsObj, NcesEmailNotificationRequested.class);

        final NcesEmailNotification emailNotification = NcesEmailNotification.ncesEmailNotification()
                .withMasterDefendantId(ncesEmailNotificationDetails.getMasterDefendantId())
                .withNotificationId(ncesEmailNotificationDetails.getNotificationId())
                .withMaterialId(ncesEmailNotificationDetails.getMaterialId())
                .withTemplateId(fromString(ncesEmailNotificationTemplateId))
                .withSendToAddress(ncesEmailNotificationDetails.getSendTo())
                .withSubject(ncesEmailNotificationDetails.getSubject())
                .withMaterialUrl(materialUrl)
                .build();

        aggregate(HearingFinancialResultsAggregate.class, emailNotification.getMasterDefendantId(), envelope,
                a -> a.saveNcesEmailNotificationDetails(emailNotification));

    }

    private JsonObject getNcesEmailNotificationDetails(final JsonEnvelope envelope) {
        final UUID materialId = UUID.fromString(envelope.payloadAsJsonObject().getString(MATERIAL_ID));
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("results.query.nces-email-notification-details").build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadata, createObjectBuilder().add(MATERIAL_ID, materialId.toString()).build());

        return requester.requestAsAdmin(requestEnvelope, JsonObject.class).payload();

    }
}
