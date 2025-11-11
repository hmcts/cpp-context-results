package uk.gov.moj.cpp.domains.results.shareresults;

import java.util.UUID;

@SuppressWarnings({"squid:S2384"})
public class SharedResultLine {

    private UUID id;
    private UUID caseId;
    private UUID defendantId;
    private String level;

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getLevel() {
        return level;
    }

    public SharedResultLine setCaseId(final UUID caseId) {
        this.caseId = caseId;
        return this;
    }

    public SharedResultLine setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
        return this;
    }

}

