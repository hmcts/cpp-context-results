package uk.gov.moj.cpp.results.it.steps.data.hearing;

import java.time.LocalDate;
import java.util.UUID;

public class Offence {
    private final UUID id;
    private final String offenceTitle;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Plea plea;


    public Offence(final UUID id, final String offenceTitle, final LocalDate startDate, final LocalDate endDate, final Plea plea) {
        this.id = id;
        this.offenceTitle = offenceTitle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.plea = plea;
    }

    public UUID getId() {
        return id;
    }

    public String getOffenceTitle() {
        return offenceTitle;
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
}
