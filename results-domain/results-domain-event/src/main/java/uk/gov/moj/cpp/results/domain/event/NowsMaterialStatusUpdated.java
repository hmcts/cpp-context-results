package uk.gov.moj.cpp.results.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.util.UUID;

@Event("results.event.nows-material-status-updated")
public class NowsMaterialStatusUpdated implements Serializable {
    private static final long serialVersionUID = -8186949966428456562L;

    private final UUID materialId;
    private final UUID hearingId;
    private final String status;

    public NowsMaterialStatusUpdated(UUID hearingId, UUID materialId, String status) {
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
