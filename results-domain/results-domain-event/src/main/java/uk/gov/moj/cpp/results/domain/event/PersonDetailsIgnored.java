package uk.gov.moj.cpp.results.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("results.person-details-ignored")
public class PersonDetailsIgnored {

    private final UUID personId;
    private final UUID hearingId;
    private final String reason;

    public PersonDetailsIgnored(final UUID personId, final UUID hearingId, final String reason) {
        this.personId = personId;
        this.hearingId = hearingId;
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
