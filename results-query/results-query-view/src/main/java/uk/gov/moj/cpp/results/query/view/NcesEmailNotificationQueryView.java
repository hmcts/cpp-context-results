package uk.gov.moj.cpp.results.query.view;

import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.NcesEmailNotificationDetailsRepository;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(QUERY_VIEW)
public class NcesEmailNotificationQueryView {

    private static final Logger LOGGER = LoggerFactory.getLogger(NcesEmailNotificationQueryView.class);
    @Inject
    private NcesEmailNotificationDetailsRepository ncesEmailNotificationDetailsRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("results.query.nces-email-notification-details")
    public JsonEnvelope getNcesEmailNotificationDetails(final JsonEnvelope envelope) {
        LOGGER.info("Received getNcesEmailNotificationDetails view {}", envelope.toObfuscatedDebugString());
        final UUID materialId = UUID.fromString(envelope.payloadAsJsonObject().getString("materialId"));

        final JsonObject jsonObject = ncesEmailNotificationDetailsRepository.findByMaterialId(materialId)
                .map(objectToJsonObjectConverter::convert)
                .orElse(null);
        return envelopeFrom(envelope.metadata(), jsonObject);
    }
}
