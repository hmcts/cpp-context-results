package uk.gov.moj.cpp.results.event.service;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationNotifyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(uk.gov.moj.cpp.results.event.service.NotificationNotifyService.class);

    private static final String NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE = "notificationnotify.send-email-notification";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;


    public void sendEmailNotification(final JsonEnvelope event, final JsonObject emailNotification) {

        LOGGER.info("sending - {} ", emailNotification);

        final Envelope<JsonObject> jsonObjectEnvelope = envelop(emailNotification)
                .withName(NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE)
                .withMetadataFrom(event);
        sender.sendAsAdmin(jsonObjectEnvelope);

    }
}
