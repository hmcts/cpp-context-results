package uk.gov.moj.cpp.domains.results.defendanttracking;

import uk.gov.justice.core.courts.Offence;

import java.util.List;
import java.util.UUID;

public class UpdateDefendantTracking {
    private UUID defendantId;
    private List<Offence> offences;

    public UpdateDefendantTracking(final UUID defendantId, final List<Offence> offences) {
        this.defendantId = defendantId;
        this.offences = offences;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public List<Offence> getOffences() {
        return offences;
    }
}
