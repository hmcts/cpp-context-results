package uk.gov.moj.cpp.results.event.helper;

import uk.gov.moj.cpp.results.domain.event.AmendmentType;

import java.util.UUID;

public class PreviousResult {

    private UUID id;
    private boolean isQualifying;
    private UUID offenceId;
    private String offenceCode;

    public PreviousResult(UUID id, boolean isQualifying, UUID offenceId, String offenceCode) {
        this.id = id;
        this.isQualifying = isQualifying;
        this.offenceId = offenceId;
        this.offenceCode = offenceCode;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public boolean isQualifying() {
        return isQualifying;
    }

    public void setQualifying(final boolean qualifying) {
        isQualifying = qualifying;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public void setOffenceId(final UUID offenceId) {
        this.offenceId = offenceId;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public void setOffenceCode(final String offenceCode) {
        this.offenceCode = offenceCode;
    }

}
