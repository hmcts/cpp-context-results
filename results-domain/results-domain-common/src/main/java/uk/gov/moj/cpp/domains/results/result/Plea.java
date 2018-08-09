package uk.gov.moj.cpp.domains.results.result;

import java.time.LocalDate;

public class Plea {
    private String pleaValue;
    private LocalDate pleaDate;

    public Plea(final String pleaValue, final LocalDate pleaDate) {
        this.pleaValue = pleaValue;
        this.pleaDate = pleaDate;
    }

    public String getPleaValue() {
        return pleaValue;
    }

    public LocalDate getPleaDate() {
        return pleaDate;
    }
}
