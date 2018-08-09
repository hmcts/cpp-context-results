package uk.gov.moj.cpp.results.domain.event;

import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("results.resultline-deletion-ignored")
public class ResultLineDeletionIgnored {

    private final UUID hearingResultId;
    private final String reason;

    public ResultLineDeletionIgnored(UUID resultLineId, String reason) {
        this.hearingResultId = resultLineId;
        this.reason = reason;
    }

    public UUID getHearingResultId() {
        return hearingResultId;
    }

    public String getReason() {
        return reason;
    }
}
