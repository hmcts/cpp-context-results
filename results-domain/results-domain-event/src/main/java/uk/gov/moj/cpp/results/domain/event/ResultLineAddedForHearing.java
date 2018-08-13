package uk.gov.moj.cpp.results.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.domains.results.result.Plea;
import uk.gov.moj.cpp.domains.results.result.Prompt;
import uk.gov.moj.cpp.domains.results.result.ResultLevel;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Event("results.resultline-added-for-hearing")
@SuppressWarnings("squid:S00107")
public class ResultLineAddedForHearing {

    private final UUID hearingResultId;
    private final UUID caseId;
    private final String urn;
    private final UUID hearingId;
    private final UUID personId;
    private final ZonedDateTime sharedTime;
    private final UUID offenceId;
    private final String offenceTitle;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final ResultLevel level;
    private final String resultLabel;
    private final List<Prompt> prompts;
    private final Plea plea;
    private final String court;
    private final String courtRoom;
    private final UUID clerkOfTheCourtId;
    private final String clerkOfTheCourtFirstName;
    private final String clerkOfTheCourtLastName;

    public ResultLineAddedForHearing(final UUID hearingResultId, final ZonedDateTime sharedTime, final UUID caseId, final String urn, final UUID hearingId,
                                     final UUID personId, final UUID offenceId, final String offenceTitle, final ResultLevel level, final String resultLabel,
                                     final List<Prompt> prompts, final LocalDate startDate, final LocalDate endDate, final Plea plea, final String court,
                                     final String courtRoom, final UUID clerkOfTheCourtId, final String clerkOfTheCourtFirstName, final String clerkOfTheCourtLastName) {
        this.hearingResultId = hearingResultId;
        this.sharedTime = sharedTime;
        this.caseId = caseId;
        this.urn = urn;
        this.hearingId = hearingId;
        this.personId = personId;
        this.offenceId = offenceId;
        this.offenceTitle = offenceTitle;
        this.level = level;
        this.resultLabel = resultLabel;
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

    public UUID getHearingResultId() {
        return hearingResultId;
    }

    public UUID getOffenceId() {
        return offenceId;
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

    public String getOffenceTitle() {
        return offenceTitle;
    }

    public ResultLevel getLevel() {
        return level;
    }

    public String getResultLabel() {
        return resultLabel;
    }

    public List<Prompt> getPrompts() {
        return prompts;
    }

    public ZonedDateTime getSharedTime() {
        return sharedTime;
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
