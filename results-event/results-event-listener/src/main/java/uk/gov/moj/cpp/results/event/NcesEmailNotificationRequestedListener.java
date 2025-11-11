package uk.gov.moj.cpp.results.event;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.persist.NcesEmailNotificationDetailsRepository;
import uk.gov.moj.cpp.results.persist.entity.NcesEmailNotificationDetailsEntity;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class NcesEmailNotificationRequestedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NcesEmailNotificationRequestedListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private NcesEmailNotificationDetailsRepository ncesEmailNotificationDetailsRepository;

    @Handles("results.event.nces-email-notification-requested")
    public void handleEmailToNcesNotificationRequested(final JsonEnvelope event) {
        final JsonObject requestJson = event.payloadAsJsonObject();
        final NcesEmailNotificationRequested ncesEmailNotificationRequested = jsonObjectToObjectConverter.convert(requestJson, NcesEmailNotificationRequested.class);
        saveNcesEmailNotificationDetails(ncesEmailNotificationRequested);
    }

    private void saveNcesEmailNotificationDetails(NcesEmailNotificationRequested ncesEmailNotificationRequested) {
        final UUID notificationId = ncesEmailNotificationRequested.getNotificationId();
        final UUID materialId = ncesEmailNotificationRequested.getMaterialId();
        ncesEmailNotificationDetailsRepository.save(createNCESEmailNotificationDetailsEntity(ncesEmailNotificationRequested));
        LOGGER.info("NCES email notification details successfully stored for notification id & material id : {} & {}", notificationId, materialId);
    }

    private NcesEmailNotificationDetailsEntity createNCESEmailNotificationDetailsEntity(NcesEmailNotificationRequested ncesEmailNotificationRequested) {
        final NcesEmailNotificationDetailsEntity ncesEmailNotificationDetailsEntity = new NcesEmailNotificationDetailsEntity();
        ncesEmailNotificationDetailsEntity.setId(randomUUID());
        ncesEmailNotificationDetailsEntity.setNotificationId(ncesEmailNotificationRequested.getNotificationId());
        ncesEmailNotificationDetailsEntity.setMaterialId(ncesEmailNotificationRequested.getMaterialId());
        ncesEmailNotificationDetailsEntity.setMasterDefendantId(ncesEmailNotificationRequested.getMasterDefendantId());
        ncesEmailNotificationDetailsEntity.setSendTo(ncesEmailNotificationRequested.getSendTo());
        ncesEmailNotificationDetailsEntity.setSubject(ncesEmailNotificationRequested.getSubject());
        return ncesEmailNotificationDetailsEntity;
    }

}
