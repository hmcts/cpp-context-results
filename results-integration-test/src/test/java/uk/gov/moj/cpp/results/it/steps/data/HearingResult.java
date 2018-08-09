package uk.gov.moj.cpp.results.it.steps.data;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class HearingResult {

    private final UUID hearingId;
    private final ZonedDateTime sharedTime;
    private final List<ResultLine> resultLines;

    public HearingResult(final UUID hearingId, final ZonedDateTime sharedTime, final List<ResultLine> resultLines) {
        this.hearingId = hearingId;
        this.resultLines = resultLines;
        this.sharedTime = sharedTime;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public ZonedDateTime getSharedTime() {
        return sharedTime;
    }

    public List<ResultLine> getResultLines() {
        return resultLines;
    }

    @Override
    public String toString() {
        return "HearingResult{" +
                "hearingId=" + hearingId +
                ", sharedTime=" + sharedTime +
                ", resultLines=" + resultLines +
                '}';
    }
}
