package uk.gov.moj.cpp.domains.results.notification;

import java.util.Map;
import java.util.UUID;

public class Notification {

    private final UUID notificationId;
    private final UUID templateId;
    private final String sendToAddress;
    private final Map<String, String> personalisation;

    public Notification(final UUID notificationId, final UUID templateId, final String sendToAddress, final Map<String, String> personalisation) {
        this.notificationId = notificationId;
        this.templateId = templateId;
        this.sendToAddress = sendToAddress;
        this.personalisation = personalisation;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getSendToAddress() {
        return sendToAddress;
    }

    public Map<String, String> getPersonalisation() {
        return personalisation;
    }

    public UUID getNotificationId() {
        return notificationId;
    }
}
