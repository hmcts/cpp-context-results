package uk.gov.moj.cpp.results.persist.entity;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.EAGER;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import uk.gov.moj.cpp.domains.results.result.ResultLevel;

@Entity
@Table(name = "hearing_result")
@SuppressWarnings("squid:S00107")
public class HearingResult {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "offence_id")
    private UUID offenceId;

    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "urn")
    private String urn;

    @Column(name = "hearing_id")
    private UUID hearingId;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "offence_title")
    private String offenceTitle;

    @Column(name = "offence_start_date")
    private LocalDate startDate;

    @Column(name = "offence_end_date")
    private LocalDate endDate;

    @Column(name = "plea_value")
    private String pleaValue;

    @Column(name = "plea_date")
    private LocalDate pleaDate;

    @Column(name = "court")
    private String court;

    @Column(name = "court_room")
    private String courtRoom;

    @Column(name = "clerk_of_court_id")
    private UUID clerkOfTheCourtId;

    @Column(name = "clerk_of_court_firstname")
    private String clerkOfTheCourtFirstName;

    @Column(name = "clerk_of_court_lastname")
    private String clerkOfTheCourtLastName;

    @Column(name = "result_level")
    @Enumerated(EnumType.STRING)
    private ResultLevel resultLevel;

    @Column(name = "result_label")
    private String resultLabel;

    @Column(name = "conviction_date")
    private LocalDate convictionDate;

    @Column(name = "verdict_date")
    private LocalDate verdictDate;

    @Column(name = "verdict_category")
    private String verdictCategory;

    @Column(name = "verdict_description")
    private String verdictDescription;

    @OneToMany(cascade = ALL, fetch = EAGER, orphanRemoval = true)
    @JoinColumn(name = "hearing_result_id", referencedColumnName = "id", insertable = false, updatable = false)
    private List<ResultPrompt> resultPrompts = new ArrayList<>();

    @Column(name = "last_shared_datetime")
    private ZonedDateTime lastSharedDateTime;

    @Column(name = "ordered_date")
    private LocalDate orderedDate;

    public HearingResult() {
        // for JPA
    }

    private HearingResult(final UUID id, final UUID caseId, final String urn, final UUID hearingId,
                          final UUID personId, final UUID offenceId, final String offenceTitle, final String pleaValue,
                          final LocalDate pleaDate, final String resultLabel, final List<ResultPrompt> resultPrompts,
                          final LocalDate startDate, final LocalDate endDate, final ResultLevel resultLevel,
                          final String court, final String courtRoom,
                          final UUID clerkOfTheCourtId, final String clerkOfTheCourtFirstName,
                    final String clerkOfTheCourtLastName, final ZonedDateTime lastSharedDateTime,
                    final LocalDate convictionDate,
                    final LocalDate verdictDate, final String verdictCategory,
                    final String verdictDescription, final LocalDate orderedDate) {
        this.id = id;
        this.caseId = caseId;
        this.urn = urn;
        this.hearingId = hearingId;
        this.personId = personId;
        this.offenceId = offenceId;
        this.offenceTitle = offenceTitle;
        this.resultLevel = resultLevel;
        this.resultLabel = resultLabel;
        this.resultPrompts = resultPrompts;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pleaValue = pleaValue;
        this.pleaDate = pleaDate;
        this.court = court;
        this.courtRoom = courtRoom;
        this.clerkOfTheCourtId = clerkOfTheCourtId;
        this.clerkOfTheCourtFirstName = clerkOfTheCourtFirstName;
        this.clerkOfTheCourtLastName = clerkOfTheCourtLastName;
        this.convictionDate = convictionDate;
        this.verdictDate = verdictDate;
        this.verdictCategory = verdictCategory;
        this.verdictDescription = verdictDescription;
        this.lastSharedDateTime = lastSharedDateTime;
        this.orderedDate = orderedDate;
    }

    public UUID getId() {
        return id;
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

    public UUID getOffenceId() {
        return offenceId;
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

    public ResultLevel getResultLevel() {
        return resultLevel;
    }

    public String getResultLabel() {
        return resultLabel;
    }

    public List<ResultPrompt> getResultPrompts() {
        return resultPrompts;
    }

    public String getPleaValue() {
        return pleaValue;
    }

    public LocalDate getPleaDate() {
        return pleaDate;
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

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public LocalDate getVerdictDate() {
        return verdictDate;
    }

    public String getVerdictCategory() {
        return verdictCategory;
    }

    public String getVerdictDescription() {
        return verdictDescription;
    }

    public ZonedDateTime getLastSharedDateTime() { return lastSharedDateTime; }

    public LocalDate getOrderedDate() {
        return orderedDate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HearingResult other = (HearingResult) obj;
        return Objects.equals(this.id, other.id);
    }

    public static Builder of(final HearingResult hearingResult) {
        return new Builder(hearingResult.getId(),
                hearingResult.getOffenceId(),
                hearingResult.getCaseId(),
                hearingResult.getUrn(),
                hearingResult.getHearingId(),
                hearingResult.getPersonId(),
                hearingResult.getOffenceTitle(),
                hearingResult.getResultLevel(),
                hearingResult.getResultLabel(),
                hearingResult.getResultPrompts(),
                hearingResult.getStartDate(),
                hearingResult.getEndDate(),
                hearingResult.getPleaValue(),
                hearingResult.getPleaDate(),
                hearingResult.getCourt(),
                hearingResult.getCourtRoom(),
                hearingResult.getClerkOfTheCourtId(),
                hearingResult.getClerkOfTheCourtFirstName(),
                        hearingResult.getClerkOfTheCourtLastName(),
                        hearingResult.getLastSharedDateTime(),
                        hearingResult.getConvictionDate(), hearingResult.getVerdictDate(),
                        hearingResult.getVerdictCategory(), hearingResult.getVerdictDescription(),
                        hearingResult.getOrderedDate());
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("WeakerAccess")
    public static class Builder {
        private UUID id;
        private UUID offenceId;
        private UUID caseId;
        private String urn;
        private UUID hearingId;
        private UUID personId;
        private String offenceTitle;
        private LocalDate startDate;
        private LocalDate endDate;
        private ResultLevel resultLevel;
        private String resultLabel;
        private String pleaValue;
        private LocalDate pleaDate;
        private String court;
        private String courtRoom;
        private UUID clerkOfTheCourtId;
        private String clerkOfTheCourtFirstName;
        private String clerkOfTheCourtLastName;
        private ZonedDateTime lastSharedDateTime;
        private List<ResultPrompt> resultPrompts = new ArrayList<>();
        private LocalDate convictionDate;
        private LocalDate verdictDate;
        private String verdictCategory;
        private String verdictDescription;
        private LocalDate orderedDate;

        private Builder() {

        }

        private Builder(final UUID id, final UUID offenceId, final UUID caseId, final String urn, final UUID hearingId,
                        final UUID personId, final String offenceTitle, final ResultLevel resultLevel, final String resultLabel,
                        final List<ResultPrompt> resultPrompts, final LocalDate startDate, final LocalDate endDate,
                        final String pleaValue, final LocalDate pleaDate,
                        final String court, final String courtRoom, final UUID clerkOfTheCourtId, final String clerkOfTheCourtFirstName,
                        final String clerkOfTheCourtLastName, final ZonedDateTime lastSharedDateTime, final LocalDate convictionDate,
                        final LocalDate verdictDate, final String verdictCategory,
                        final String verdictDescription, final LocalDate orderedDate) {

            this.id = id;
            this.offenceId = offenceId;
            this.caseId = caseId;
            this.urn = urn;
            this.hearingId = hearingId;
            this.personId = personId;
            this.offenceTitle = offenceTitle;
            this.resultLevel = resultLevel;
            this.resultLabel = resultLabel;
            this.resultPrompts = resultPrompts;
            this.startDate = startDate;
            this.endDate = endDate;
            this.pleaValue = pleaValue;
            this.pleaDate = pleaDate;
            this.court = court;
            this.courtRoom = courtRoom;
            this.clerkOfTheCourtId = clerkOfTheCourtId;
            this.clerkOfTheCourtFirstName = clerkOfTheCourtFirstName;
            this.clerkOfTheCourtLastName = clerkOfTheCourtLastName;
            this.convictionDate = convictionDate;
            this.verdictDate = verdictDate;
            this.verdictCategory = verdictCategory;
            this.lastSharedDateTime = lastSharedDateTime;
            this.verdictDescription = verdictDescription;
            this.orderedDate = orderedDate;
        }

        public HearingResult build() {
            return new HearingResult(this.id, this.caseId, this.urn, this.hearingId, this.personId, this.offenceId,
                    this.offenceTitle, this.pleaValue, this.pleaDate, this.resultLabel,
                    this.resultPrompts,
                    this.startDate, this.endDate, this.resultLevel, this.court, this.courtRoom, this.clerkOfTheCourtId,
                            clerkOfTheCourtFirstName, clerkOfTheCourtLastName, lastSharedDateTime, this.convictionDate,
                            this.verdictDate, this.verdictCategory, this.verdictDescription,
                            this.orderedDate);
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withOffenceId(final UUID offenceId) {
            this.offenceId = offenceId;
            return this;
        }

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withUrn(final String urn) {
            this.urn = urn;
            return this;
        }

        public Builder withHearingId(final UUID hearingId) {
            this.hearingId = hearingId;
            return this;
        }

        public Builder withPersonId(final UUID personId) {
            this.personId = personId;
            return this;
        }

        public Builder withOffenceTitle(final String offenceTitle) {
            this.offenceTitle = offenceTitle;
            return this;
        }

        public Builder withStartDate(final LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder withEndDate(final LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder withResultLevel(final ResultLevel resultLevel) {
            this.resultLevel = resultLevel;
            return this;
        }

        public Builder withResultLabel(final String resultLabel) {
            this.resultLabel = resultLabel;
            return this;
        }

        public Builder withPleaValue(final String pleaValue) {
            this.pleaValue = pleaValue;
            return this;
        }

        public Builder withPleaDate(final LocalDate pleaDate) {
            this.pleaDate = pleaDate;
            return this;
        }

        public Builder withCourt(final String court) {
            this.court = court;
            return this;
        }

        public Builder withCourtRoom(final String courtRoom) {
            this.courtRoom = courtRoom;
            return this;
        }

        public Builder withClerkOfTheCourtId(final UUID clerkOfTheCourtId) {
            this.clerkOfTheCourtId = clerkOfTheCourtId;
            return this;
        }

        public Builder withClerkOfTheCourtFirstName(final String clerkOfTheCourtFirstName) {
            this.clerkOfTheCourtFirstName = clerkOfTheCourtFirstName;
            return this;
        }

        public Builder withClerkOfTheCourtLastName(final String clerkOfTheCourtLastName) {
            this.clerkOfTheCourtLastName = clerkOfTheCourtLastName;
            return this;
        }

        public Builder withResultPrompts(final List<ResultPrompt> resultPrompts) {
            this.resultPrompts = resultPrompts;
            return this;
        }

        public Builder withConvictionDate(final LocalDate convictionDate) {
            this.convictionDate = convictionDate;
            return this;
        }
        public Builder withLastSharedDateTime(final ZonedDateTime lastSharedDateTime) {
            this.lastSharedDateTime = lastSharedDateTime;
            return this;
        }

        public Builder withVerdictDate(final LocalDate verdictDate) {
            this.verdictDate = verdictDate;
            return this;
        }

        public Builder withVerdictCategory(final String verdictCategory) {
            this.verdictCategory = verdictCategory;
            return this;
        }

        public Builder withVerdictDescription(final String verdictDesciption) {
            this.verdictDescription = verdictDesciption;
            return this;
        }

        public Builder withOrderedDate(final LocalDate orderedDate) {
            this.orderedDate = orderedDate;
            return this;
        }
    }
}
