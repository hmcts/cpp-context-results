package uk.gov.moj.cpp.domains.results.shareresults;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S2384"})
public class SharedResultLine {

    private UUID id;
    private UUID caseId;
    private UUID defendantId;
    private UUID offenceId;
    private String level;
    private String label;
    private CourtClerk courtClerk;
    private ZonedDateTime lastSharedDateTime;
    private LocalDate orderedDate;

    private List<Prompt> prompts = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public String getLevel() {
        return level;
    }

    public String getLabel() {
        return label;
    }

    public CourtClerk getCourtClerk() {
        return courtClerk;
    }

    public ZonedDateTime getLastSharedDateTime() {
        return lastSharedDateTime;
    }

    public List<Prompt> getPrompts() {
        return prompts;
    }

    public LocalDate getOrderedDate() {
        return orderedDate;
    }
    public SharedResultLine setId(final UUID id) {
        this.id = id;
        return this;
    }

    public SharedResultLine setCaseId(final UUID caseId) {
        this.caseId = caseId;
        return this;
    }

    public SharedResultLine setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
        return this;
    }

    public SharedResultLine setOffenceId(final UUID offenceId) {
        this.offenceId = offenceId;
        return this;
    }

    public SharedResultLine setLevel(final String level) {
        this.level = level;
        return this;
    }

    public SharedResultLine setLabel(final String label) {
        this.label = label;
        return this;
    }

    public SharedResultLine setPrompts(final List<Prompt> prompts) {
        this.prompts = prompts;
        return this;
    }

    public SharedResultLine setLastSharedDateTime(final ZonedDateTime lastSharedDateTime) {
        this.lastSharedDateTime = lastSharedDateTime;
        return this;
    }

    public SharedResultLine setCourtClerk(final CourtClerk courtClerk) {
        this.courtClerk = courtClerk;
        return this;
    }

    public SharedResultLine setOrderedDate(final LocalDate orderedDate) {
        this.orderedDate = orderedDate;
        return this;
    }

    public static SharedResultLine sharedResultLine() {
        return new SharedResultLine();
    }
}

