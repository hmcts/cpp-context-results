package uk.gov.moj.cpp.results.event.processor;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class MaterialAddedEventProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MaterialAddedEventProcessor.class.getName());
    public static final String ORIGINATOR = Originator.SOURCE;
    public static final String ORIGINATOR_VALUE = Originator.ORIGINATOR_VALUE;

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
        LOGGER.info("Received MaterialAddedEvent {}", envelope);
        if (envelope.metadata().asJsonObject().containsKey(ORIGINATOR)
                && ORIGINATOR_VALUE.equalsIgnoreCase(envelope.metadata().asJsonObject().getString(ORIGINATOR))) {
            processNcesDocumentNotification(envelope);
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

}
