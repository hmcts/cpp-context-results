package uk.gov.moj.cpp.results.domain.informant.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProsecutorResult {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final UUID prosecutionAuthorityId;
    private final String prosecutionAuthorityCode;
    private final String prosecutionAuthorityName;
    private final String prosecutionAuthorityOuCode;
    private final String majorCreditorCode;
    private final List<InformantRegisterHearingVenue> hearingVenues;

    @JsonCreator
    public ProsecutorResult(
            @JsonProperty("startDate") final LocalDate startDate,
            @JsonProperty("endDate") final LocalDate endDate,
            @JsonProperty("prosecutionAuthorityId") final UUID prosecutionAuthorityId,
            @JsonProperty("prosecutionAuthorityCode") final String prosecutionAuthorityCode,
            @JsonProperty("prosecutionAuthorityName") final String prosecutionAuthorityName,
            @JsonProperty("prosecutionAuthorityOuCode") final String prosecutionAuthorityOuCode,
            @JsonProperty("majorCreditorCode") final String majorCreditorCode,
            @JsonProperty("hearingVenues") final List<InformantRegisterHearingVenue> hearingVenues) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.prosecutionAuthorityId = prosecutionAuthorityId;
        this.prosecutionAuthorityCode = prosecutionAuthorityCode;
        this.prosecutionAuthorityName = prosecutionAuthorityName;
        this.prosecutionAuthorityOuCode = prosecutionAuthorityOuCode;
        this.majorCreditorCode = majorCreditorCode;
        this.hearingVenues = hearingVenues;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public UUID getProsecutionAuthorityId() { return prosecutionAuthorityId; }
    public String getProsecutionAuthorityCode() { return prosecutionAuthorityCode; }
    public String getProsecutionAuthorityName() { return prosecutionAuthorityName; }
    public String getProsecutionAuthorityOuCode() { return prosecutionAuthorityOuCode; }
    public String getMajorCreditorCode() { return majorCreditorCode; }
    public List<InformantRegisterHearingVenue> getHearingVenues() { return hearingVenues; }

    public static Builder prosecutorResult() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate startDate;
        private LocalDate endDate;
        private UUID prosecutionAuthorityId;
        private String prosecutionAuthorityCode;
        private String prosecutionAuthorityName;
        private String prosecutionAuthorityOuCode;
        private String majorCreditorCode;
        private List<InformantRegisterHearingVenue> hearingVenues;

        public Builder withStartDate(final LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder withEndDate(final LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder withProsecutionAuthorityId(final UUID prosecutionAuthorityId) { this.prosecutionAuthorityId = prosecutionAuthorityId; return this; }
        public Builder withProsecutionAuthorityCode(final String prosecutionAuthorityCode) { this.prosecutionAuthorityCode = prosecutionAuthorityCode; return this; }
        public Builder withProsecutionAuthorityName(final String prosecutionAuthorityName) { this.prosecutionAuthorityName = prosecutionAuthorityName; return this; }
        public Builder withProsecutionAuthorityOuCode(final String prosecutionAuthorityOuCode) { this.prosecutionAuthorityOuCode = prosecutionAuthorityOuCode; return this; }
        public Builder withMajorCreditorCode(final String majorCreditorCode) { this.majorCreditorCode = majorCreditorCode; return this; }
        public Builder withHearingVenues(final List<InformantRegisterHearingVenue> hearingVenues) { this.hearingVenues = hearingVenues; return this; }

        public ProsecutorResult build() {
            return new ProsecutorResult(startDate, endDate, prosecutionAuthorityId,
                    prosecutionAuthorityCode, prosecutionAuthorityName, prosecutionAuthorityOuCode,
                    majorCreditorCode, hearingVenues);
        }
    }
}
