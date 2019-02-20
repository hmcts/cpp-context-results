package uk.gov.moj.cpp.results.domain.aggregate;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("results.pending-material-status-update")
public class PendingMaterialStatusUpdate {
    private static final long serialVersionUID = -8186949966428456562L;

    private final UUID materialId;
    private final UUID hearingId;
    private final String status;

    public PendingMaterialStatusUpdate(final UUID hearingId, final UUID materialId, final String status) {
        this.materialId = materialId;
        this.hearingId = hearingId;
        this.status = status;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public String getStatus() {
        return status;
    }
}
