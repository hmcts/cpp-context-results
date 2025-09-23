package uk.gov.moj.cpp.results.event.service;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonCreator;

public class DcsHearing {
    private String courtCentre;
    private LocalDate hearingDate;

    @JsonCreator
    public DcsHearing(final String courtCentre, final LocalDate hearingDate) {
        this.courtCentre = courtCentre;
        this.hearingDate = hearingDate;
    }

    private DcsHearing(final Builder builder) {
        courtCentre = builder.courtCentre;
        hearingDate = builder.hearingDate;
    }

    public String getCourtCentre() {
        return courtCentre;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public static final class Builder {
        private String courtCentre;
        private LocalDate hearingDate;

        private Builder() {
        }

        public static Builder newHearing() {
            return new Builder();
        }

        public Builder withCourtCentre(final String val) {
            courtCentre = val;
            return this;
        }

        public Builder withHearingDate(final LocalDate val) {
            hearingDate = val;
            return this;
        }

        public DcsHearing build() {
            return new DcsHearing(this);
        }
    }
}
