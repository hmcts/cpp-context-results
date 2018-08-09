package uk.gov.moj.cpp.results.it.steps.data;

import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.UUID;

import uk.gov.moj.cpp.domains.results.result.ResultLevel;

import java.util.List;
import java.util.UUID;

public class ResultLine {
    private final UUID id = UUID.next();
    private final UUID personId;
    private final UUID caseId;
    private final String urn;
    private final String court;
    private final String courtRoom;
    private final UUID clerkOfTheCourtId;
    private final String clerkOfTheCourtFirstName;
    private final String clerkOfTheCourtLastName;
    private final UUID offenceId;
    private final ResultLevel level;
    private final String resultLabel;
    private final List<ResultPrompt> prompts;

    public ResultLine(final UUID personId, final UUID caseId, final String urn, final UUID offenceId,
                      final String resultLabel, final ResultLevel level, final List<ResultPrompt> prompts,
                      final String court, final String courtRoom, final UUID clerkOfTheCourtId, final String clerkOfTheCourtFirstName,
                      final String clerkOfTheCourtLastName) {
        this.personId = personId;
        this.caseId = caseId;
        this.urn = urn;
        this.offenceId = offenceId;
        this.resultLabel = resultLabel;
        this.prompts = prompts;
        this.level = level;
        this.court = court;
        this.courtRoom = courtRoom;
        this.clerkOfTheCourtId = clerkOfTheCourtId;
        this.clerkOfTheCourtFirstName = clerkOfTheCourtFirstName;
        this.clerkOfTheCourtLastName = clerkOfTheCourtLastName;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPersonId() {
        return personId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public ResultLevel getLevel() {
        return level;
    }

    public String getResultLabel() {
        return resultLabel;
    }

    public List<ResultPrompt> getPrompts() {
        return prompts;
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

    @Override
    public String toString() {
        return "ResultLine{" +
                "id=" + id +
                ", personId=" + personId +
                ", caseId=" + caseId +
                ", urn='" + urn + '\'' +
                ", court='" + court + '\'' +
                ", courtRoom='" + courtRoom + '\'' +
                ", clerkOfTheCourtId=" + clerkOfTheCourtId +
                ", clerkOfTheCourtFirstName='" + clerkOfTheCourtFirstName + '\'' +
                ", clerkOfTheCourtLastName='" + clerkOfTheCourtLastName + '\'' +
                ", offenceId=" + offenceId +
                ", level=" + level +
                ", resultLabel='" + resultLabel + '\'' +
                ", prompts=" + prompts +
                '}';
    }
}
