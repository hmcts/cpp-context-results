package uk.gov.moj.cpp.results.event.processor;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.results.courts.NcesDocumentNotification;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.results.event.helper.Originator;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class MaterialAddedEventProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MaterialAddedEventProcessor.class.getName());
    public static final String ORIGINATOR = Originator.SOURCE_NCES;
    public static final String ORIGINATOR_VALUE = Originator.ORIGINATOR_VALUE_NCES;
    public static final String ORIGINATOR_VALUE_NCES_CASEID = Originator.ORIGINATOR_VALUE_NCES_CASEID;
    public static final String RESULTS_COMMAND_INACTIVE_MIGRATED_NCES_DOCUMENT_NOTIFICATION = "result.command.migrated-inactive-nces-document-notification";
    private static final String MASTER_DEFENDANT_ID = "masterDefendantId";

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("material.material-added")
    public void processMaterialAdded(final JsonEnvelope envelope) {
        LOGGER.info("Received MaterialAddedEvent {}", envelope.toObfuscatedDebugString());
        if (envelope.metadata().asJsonObject().containsKey(ORIGINATOR)) {
            final String originator = envelope.metadata().asJsonObject().getString(ORIGINATOR);

            if (ORIGINATOR_VALUE.equalsIgnoreCase(originator)) {
                processNcesDocumentNotification(envelope);
            }
            else if (originator != null && originator.toLowerCase().startsWith(ORIGINATOR_VALUE_NCES_CASEID.toLowerCase())) {
                processMigratedInactiveNcesDocumentNotification(envelope);
            }
        }
    }
    @SuppressWarnings("squid:CallToDeprecatedMethod")
    private void processNcesDocumentNotification(JsonEnvelope envelope) {

        final UUID materialId = UUID.fromString(envelope.payloadAsJsonObject().getString("materialId"));
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);

        final NcesDocumentNotification ncesDocumentNotificationCommand = NcesDocumentNotification.ncesDocumentNotification()
                .withMaterialId(materialId)
                .withMaterialUrl(materialUrl)
                .build();

        this.sender.send(this.enveloper.withMetadataFrom(envelope, "results.command.nces-document-notification")
                .apply(this.objectToJsonObjectConverter.convert(ncesDocumentNotificationCommand)));
    }

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    private void processMigratedInactiveNcesDocumentNotification(JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID materialId = UUID.fromString(payload.getString("materialId"));
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);

        final String originator = envelope.metadata().asJsonObject().getString(ORIGINATOR);
        final String[] splitted = originator.split("-");

        final JsonObject enrichedPayload = createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("materialUrl", materialUrl)
                .add("masterDefendantId", splitted[1])
                .add("caseId", splitted[2])
                .build();

        this.sender.send(this.enveloper
                .withMetadataFrom(envelope, RESULTS_COMMAND_INACTIVE_MIGRATED_NCES_DOCUMENT_NOTIFICATION)
                .apply(enrichedPayload));
    }

}
