package uk.gov.moj.cpp.results.it.steps.data.hearing;

import java.time.LocalDate;
import java.util.UUID;

public class Plea {
    private UUID pleaId;
    private String pleaValue;
    private LocalDate pleaDate;

    public Plea(final UUID pleaId, final String pleaValue, final LocalDate pleaDate) {
        this.pleaId = pleaId;
        this.pleaValue = pleaValue;
        this.pleaDate = pleaDate;
    }

    public String getPleaValue() {
        return pleaValue;
    }

    public LocalDate getPleaDate() {
        return pleaDate;
    }

    public UUID getPleaId() {
        return pleaId;
    }
}
