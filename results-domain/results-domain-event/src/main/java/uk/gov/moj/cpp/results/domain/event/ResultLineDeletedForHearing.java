package uk.gov.moj.cpp.results.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("results.resultline-deleted-for-hearing")
public class ResultLineDeletedForHearing {

    private final UUID hearingResultId;

    public ResultLineDeletedForHearing(final UUID hearingResultId) {
        this.hearingResultId = hearingResultId;
    }

    public UUID getHearingResultId() {
        return hearingResultId;
    }
}
