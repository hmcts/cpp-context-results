package uk.gov.moj.cpp.results.domain.informant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InformantRegisterResultData {

    private final String amount;
    private final String nextHearingDate;
    private final String nextCourtLocation;
    private final String durationValue;
    private final String durationUnit;
    private final String durationStartDate;
    private final String durationEndDate;
    private final String secondaryDurationValue;
    private final String secondaryDurationUnit;

    @JsonCreator
    public InformantRegisterResultData(
            @JsonProperty("amount") final String amount,
            @JsonProperty("nextHearingDate") final String nextHearingDate,
            @JsonProperty("nextCourtLocation") final String nextCourtLocation,
            @JsonProperty("durationValue") final String durationValue,
            @JsonProperty("durationUnit") final String durationUnit,
            @JsonProperty("durationStartDate") final String durationStartDate,
            @JsonProperty("durationEndDate") final String durationEndDate,
            @JsonProperty("secondaryDurationValue") final String secondaryDurationValue,
            @JsonProperty("secondaryDurationUnit") final String secondaryDurationUnit) {
        this.amount = amount;
        this.nextHearingDate = nextHearingDate;
        this.nextCourtLocation = nextCourtLocation;
        this.durationValue = durationValue;
        this.durationUnit = durationUnit;
        this.durationStartDate = durationStartDate;
        this.durationEndDate = durationEndDate;
        this.secondaryDurationValue = secondaryDurationValue;
        this.secondaryDurationUnit = secondaryDurationUnit;
    }

    public String getAmount() {
        return amount;
    }

    public String getNextHearingDate() {
        return nextHearingDate;
    }

    public String getNextCourtLocation() {
        return nextCourtLocation;
    }

    public String getDurationValue() {
        return durationValue;
    }

    public String getDurationUnit() {
        return durationUnit;
    }

    public String getDurationStartDate() {
        return durationStartDate;
    }

    public String getDurationEndDate() {
        return durationEndDate;
    }

    public String getSecondaryDurationValue() {
        return secondaryDurationValue;
    }

    public String getSecondaryDurationUnit() {
        return secondaryDurationUnit;
    }

    public static Builder informantRegisterResultData() {
        return new Builder();
    }

    public static class Builder {
        private String amount;
        private String nextHearingDate;
        private String nextCourtLocation;
        private String durationValue;
        private String durationUnit;
        private String durationStartDate;
        private String durationEndDate;
        private String secondaryDurationValue;
        private String secondaryDurationUnit;

        public Builder withAmount(final String amount) {
            this.amount = amount;
            return this;
        }

        public Builder withNextHearingDate(final String nextHearingDate) {
            this.nextHearingDate = nextHearingDate;
            return this;
        }

        public Builder withNextCourtLocation(final String nextCourtLocation) {
            this.nextCourtLocation = nextCourtLocation;
            return this;
        }

        public Builder withDurationValue(final String durationValue) {
            this.durationValue = durationValue;
            return this;
        }

        public Builder withDurationUnit(final String durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        public Builder withDurationStartDate(final String durationStartDate) {
            this.durationStartDate = durationStartDate;
            return this;
        }

        public Builder withDurationEndDate(final String durationEndDate) {
            this.durationEndDate = durationEndDate;
            return this;
        }

        public Builder withSecondaryDurationValue(final String secondaryDurationValue) {
            this.secondaryDurationValue = secondaryDurationValue;
            return this;
        }

        public Builder withSecondaryDurationUnit(final String secondaryDurationUnit) {
            this.secondaryDurationUnit = secondaryDurationUnit;
            return this;
        }

        public InformantRegisterResultData build() {
            return new InformantRegisterResultData(amount, nextHearingDate, nextCourtLocation,
                    durationValue, durationUnit, durationStartDate, durationEndDate,
                    secondaryDurationValue, secondaryDurationUnit);
        }
    }
}
