package uk.gov.moj.cpp.results.domain.informant.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InformantRegisterOffence {

    private final String offenceCode;
    private final String offenceTitle;
    private final Integer orderIndex;
    private final String originatingCaseUrn;
    private final String pleaValue;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Verdict verdict;
    private final List<InformantRegisterResult> offenceResults;

    @JsonCreator
    public InformantRegisterOffence(
            @JsonProperty("offenceCode") final String offenceCode,
            @JsonProperty("offenceTitle") final String offenceTitle,
            @JsonProperty("orderIndex") final Integer orderIndex,
            @JsonProperty("originatingCaseUrn") final String originatingCaseUrn,
            @JsonProperty("pleaValue") final String pleaValue,
            @JsonProperty("verdict") final Verdict verdict,
            @JsonProperty("offenceResults") final List<InformantRegisterResult> offenceResults) {
        this.offenceCode = offenceCode;
        this.offenceTitle = offenceTitle;
        this.orderIndex = orderIndex;
        this.originatingCaseUrn = originatingCaseUrn;
        this.pleaValue = pleaValue;
        this.verdict = verdict;
        this.offenceResults = offenceResults;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public String getOffenceTitle() {
        return offenceTitle;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public String getOriginatingCaseUrn() {
        return originatingCaseUrn;
    }

    public String getPleaValue() {
        return pleaValue;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public List<InformantRegisterResult> getOffenceResults() {
        return offenceResults;
    }

    public static Builder informantRegisterOffence() {
        return new Builder();
    }

    public static class Builder {
        private String offenceCode;
        private String offenceTitle;
        private Integer orderIndex;
        private String originatingCaseUrn;
        private String pleaValue;
        private Verdict verdict;
        private List<InformantRegisterResult> offenceResults;

        public Builder withOffenceCode(final String offenceCode) {
            this.offenceCode = offenceCode;
            return this;
        }

        public Builder withOffenceTitle(final String offenceTitle) {
            this.offenceTitle = offenceTitle;
            return this;
        }

        public Builder withOrderIndex(final Integer orderIndex) {
            this.orderIndex = orderIndex;
            return this;
        }

        public Builder withOriginatingCaseUrn(final String originatingCaseUrn) {
            this.originatingCaseUrn = originatingCaseUrn;
            return this;
        }

        public Builder withPleaValue(final String pleaValue) {
            this.pleaValue = pleaValue;
            return this;
        }

        public Builder withVerdict(final Verdict verdict) {
            this.verdict = verdict;
            return this;
        }

        public Builder withOffenceResults(final List<InformantRegisterResult> offenceResults) {
            this.offenceResults = offenceResults;
            return this;
        }

        public InformantRegisterOffence build() {
            return new InformantRegisterOffence(offenceCode, offenceTitle, orderIndex,
                    originatingCaseUrn, pleaValue, verdict, offenceResults);
        }
    }
}
