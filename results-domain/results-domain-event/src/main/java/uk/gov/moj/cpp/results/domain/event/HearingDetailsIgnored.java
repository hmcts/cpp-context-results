package uk.gov.moj.cpp.results.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("results.hearing-details-ignored")
public class HearingDetailsIgnored {

    private final UUID hearingId;
    private final UUID personId;
    private final String reason;

    public HearingDetailsIgnored(final UUID hearingId, final UUID personId, final String reason) {
        this.hearingId = hearingId;
        this.personId = personId;
        this.reason = reason;
    }

    public UUID getPersonId() {
        return personId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public String getReason() {
        return reason;
    }
}
