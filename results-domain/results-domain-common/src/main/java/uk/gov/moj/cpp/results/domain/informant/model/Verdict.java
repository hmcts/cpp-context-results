package uk.gov.moj.cpp.results.domain.informant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Verdict {

    private final String verdictCode;
    private final String verdictDate;
    private final String verdictType;

    @JsonCreator
    public Verdict(
            @JsonProperty("verdictCode") final String verdictCode,
            @JsonProperty("verdictDate") final String verdictDate,
            @JsonProperty("verdictType") final String verdictType) {
        this.verdictCode = verdictCode;
        this.verdictDate = verdictDate;
        this.verdictType = verdictType;
    }

    public String getVerdictCode() {
        return verdictCode;
    }

    public String getVerdictDate() {
        return verdictDate;
    }

    public String getVerdictType() {
        return verdictType;
    }

    public static Builder verdict() {
        return new Builder();
    }

    public static class Builder {
        private String verdictCode;
        private String verdictDate;
        private String verdictType;

        public Builder withVerdictCode(final String verdictCode) {
            this.verdictCode = verdictCode;
            return this;
        }

        public Builder withVerdictDate(final String verdictDate) {
            this.verdictDate = verdictDate;
            return this;
        }

        public Builder withVerdictType(final String verdictType) {
            this.verdictType = verdictType;
            return this;
        }

        public Verdict build() {
            return new Verdict(verdictCode, verdictDate, verdictType);
        }
    }
}
