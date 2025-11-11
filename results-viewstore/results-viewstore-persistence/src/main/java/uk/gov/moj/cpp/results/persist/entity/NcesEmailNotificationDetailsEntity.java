package uk.gov.moj.cpp.results.persist.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "nces_email_notification")
public class NcesEmailNotificationDetailsEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "master_defendant_id", nullable = false)
    private UUID masterDefendantId;

    @Column(name = "send_to")
    private String sendTo;

    @Column(name = "subject")
    private String subject;

    public NcesEmailNotificationDetailsEntity() {
    }

    public NcesEmailNotificationDetailsEntity(final UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public void setMaterialId(final UUID materialId) {
        this.materialId = materialId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(final UUID notificationId) {
        this.notificationId = notificationId;
    }

    public UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    public void setMasterDefendantId(final UUID masterDefendantId) {
        this.masterDefendantId = masterDefendantId;
    }

    public String getSendTo() {
        return sendTo;
    }

    public void setSendTo(final String sendTo) {
        this.sendTo = sendTo;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }
}
