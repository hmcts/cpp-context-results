package uk.gov.moj.cpp.results.it.steps;

import uk.gov.moj.cpp.results.it.steps.data.hearing.DefenceCounsel;

import java.util.Optional;
import java.util.UUID;

public class DefendantDefenceCounselPair {

    private final Optional<DefenceCounsel> defenceCounsel;
    private final UUID personId;
    private final UUID defendantId;

    public DefendantDefenceCounselPair(Optional<DefenceCounsel> defenceCounsel, UUID personId) {
        this.defenceCounsel = defenceCounsel;
        this.personId = personId;
        this.defendantId = defenceCounsel.map(dc -> dc.getPersonIds().get(0)).orElseGet(UUID::randomUUID);
    }

    public Optional<DefenceCounsel> getDefenceCounsel() {
        return defenceCounsel;
    }

    public UUID getPersonId() {
        return personId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }
}
