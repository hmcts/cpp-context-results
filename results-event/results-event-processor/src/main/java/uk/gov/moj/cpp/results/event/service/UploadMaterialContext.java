package uk.gov.moj.cpp.results.event.service;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

public class UploadMaterialContext {

    private  Sender sender;

    private  JsonEnvelope originatingEnvelope;

    private  UUID userId;

    private  UUID hearingId;

    private  UUID materialId;

    private  UUID fileId;

    private  UUID caseId;

    private  UUID applicationId;


    public Sender getSender() {
        return sender;
    }

    public JsonEnvelope getOriginatingEnvelope() {
        return originatingEnvelope;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getFileId() {
        return fileId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }


    public void setSender(final Sender sender) {
        this.sender = sender;
    }

    public void setOriginatingEnvelope(final JsonEnvelope originatingEnvelope) {
        this.originatingEnvelope = originatingEnvelope;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    public void setMaterialId(final UUID materialId) {
        this.materialId = materialId;
    }

    public void setFileId(final UUID fileId) {
        this.fileId = fileId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public void setApplicationId(final UUID applicationId) {
        this.applicationId = applicationId;
    }

}
