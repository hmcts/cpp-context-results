package uk.gov.moj.cpp.results.persist.entity;

import java.time.LocalDate;
import java.util.UUID;

public class HearingResultSummary {

    private final UUID hearingId;
    private final UUID personId;
    private final String hearingType;
    private final LocalDate hearingDate;
    private final String defendantFirstName;
    private final String defendantLastName;

    public HearingResultSummary(final UUID hearingId, final UUID personId, final String hearingType,
                    final LocalDate hearingDate,
                                final String defendantFirstName, final String defendantLastName) {
        this.hearingId = hearingId;
        this.personId = personId;
        this.hearingType = hearingType;
        this.hearingDate = hearingDate;
        this.defendantFirstName = defendantFirstName;
        this.defendantLastName = defendantLastName;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public String getHearingType() {
        return hearingType;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public String getDefendantFirstName() {
        return defendantFirstName;
    }

    public String getDefendantLastName() {
        return defendantLastName;
    }
}
