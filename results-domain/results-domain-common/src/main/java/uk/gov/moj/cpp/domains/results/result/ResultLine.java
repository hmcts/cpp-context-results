package uk.gov.moj.cpp.domains.results.result;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("squid:S00107")
public class ResultLine {
    private final UUID id;
    private final UUID lastSharedResultId;
    private final ZonedDateTime sharedTime;
    private final UUID caseId;
    private final String urn;
    private final UUID hearingId;
    private final UUID personId;
    private final UUID offenceId;
    private final String offenceTitle;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final ResultLevel level;
    private final String label;
    private final List<Prompt> prompts;
    private final Plea plea;
    private String court;
    private String courtRoom;
    private final UUID clerkOfTheCourtId;
    private final String clerkOfTheCourtFirstName;
    private final String clerkOfTheCourtLastName;

    public ResultLine(final UUID id, final UUID lastSharedResultId, final ZonedDateTime dateTime, final UUID caseId, final String urn, final UUID hearingId, final UUID personId, final UUID offenceId,
                      final String offenceTitle, final ResultLevel level, final String label, final List<Prompt> prompts, final LocalDate startDate, final LocalDate endDate,
                      final Plea plea, final String court, final String courtRoom, final UUID clerkOfTheCourtId, final String clerkOfTheCourtFirstName, final String clerkOfTheCourtLastName) {
        this.id = id;
        this.lastSharedResultId = lastSharedResultId;
        this.sharedTime = dateTime;
        this.caseId = caseId;
        this.urn = urn;
        this.hearingId = hearingId;
        this.personId = personId;
        this.offenceId = offenceId;
        this.offenceTitle = offenceTitle;
        this.level = level;
        this.label = label;
        this.prompts = prompts;
        this.startDate = startDate;
        this.endDate = endDate;
        this.plea = plea;
        this.court = court;
        this.courtRoom = courtRoom;
        this.clerkOfTheCourtId = clerkOfTheCourtId;
        this.clerkOfTheCourtFirstName = clerkOfTheCourtFirstName;
        this.clerkOfTheCourtLastName = clerkOfTheCourtLastName;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLastSharedResultId() {
        return lastSharedResultId;
    }

    public ZonedDateTime getSharedTime() {
        return sharedTime;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public String getOffenceTitle() {
        return offenceTitle;
    }

    public ResultLevel getLevel() {
        return level;
    }

    public String getLabel() {
        return label;
    }

    public List<Prompt> getPrompts() {
        return prompts;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Plea getPlea() {
        return plea;
    }

    public String getCourt() {
        return court;
    }

    public String getCourtRoom() {
        return courtRoom;
    }

    public UUID getClerkOfTheCourtId() {
        return clerkOfTheCourtId;
    }

    public String getClerkOfTheCourtFirstName() {
        return clerkOfTheCourtFirstName;
    }

    public String getClerkOfTheCourtLastName() {
        return clerkOfTheCourtLastName;
    }
}
