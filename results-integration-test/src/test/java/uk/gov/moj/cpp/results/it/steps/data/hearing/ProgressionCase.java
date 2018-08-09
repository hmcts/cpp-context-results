package uk.gov.moj.cpp.results.it.steps.data.hearing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProgressionCase {

    private final UUID caseId;
    private final String urn;
    private final List<UUID> personIds;
    private final List<UUID> defendantIds;
    private final List<Offence> offences;

    public ProgressionCase(final UUID caseId, final String urn, List<UUID> personIds, List<UUID> defendantIds) {
        this(caseId, urn, personIds, defendantIds, new ArrayList<>());
    }

    public ProgressionCase(final UUID caseId, final String urn, List<UUID> personIds, List<UUID> defendantIds, List<Offence> offences) {
        this.caseId = caseId;
        this.urn = urn;
        this.personIds = personIds;
        this.defendantIds = defendantIds;
        this.offences = offences;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }

    public List<UUID> getPersonIds() {
        return personIds;
    }

    public List<UUID> getDefendantIds() {
        return defendantIds;
    }

    public List<Offence> getOffences() {
        return offences;
    }
}
