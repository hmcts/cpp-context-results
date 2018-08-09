package uk.gov.moj.cpp.results.domain.event;

import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("results.resultline-ignored")
public class ResultLineIgnored {

    private final UUID hearingResultId;
    private final String reason;

    public ResultLineIgnored(UUID id, String reason) {
        this.hearingResultId = id;
        this.reason = reason;
    }

    public UUID getHearingResultId() {
        return hearingResultId;
    }

    public String getReason() {
        return reason;
    }
}
