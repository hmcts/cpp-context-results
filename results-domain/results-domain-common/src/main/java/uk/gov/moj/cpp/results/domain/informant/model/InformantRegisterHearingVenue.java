package uk.gov.moj.cpp.results.domain.informant.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InformantRegisterHearingVenue {

    private final String ljaName;
    private final String courtHouse;
    private final List<InformantRegisterHearing> courtSessions;

    @JsonCreator
    public InformantRegisterHearingVenue(
            @JsonProperty("ljaName") final String ljaName,
            @JsonProperty("courtHouse") final String courtHouse,
            @JsonProperty("courtSessions") final List<InformantRegisterHearing> courtSessions) {
        this.ljaName = ljaName;
        this.courtHouse = courtHouse;
        this.courtSessions = courtSessions;
    }

    public String getLjaName() {
        return ljaName;
    }

    public String getCourtHouse() {
        return courtHouse;
    }

    public List<InformantRegisterHearing> getCourtSessions() {
        return courtSessions;
    }

    public static Builder informantRegisterHearingVenue() {
        return new Builder();
    }

    public static class Builder {
        private String ljaName;
        private String courtHouse;
        private List<InformantRegisterHearing> courtSessions;

        public Builder withLjaName(final String ljaName) {
            this.ljaName = ljaName;
            return this;
        }

        public Builder withCourtHouse(final String courtHouse) {
            this.courtHouse = courtHouse;
            return this;
        }

        public Builder withCourtSessions(final List<InformantRegisterHearing> courtSessions) {
            this.courtSessions = courtSessions;
            return this;
        }

        public InformantRegisterHearingVenue build() {
            return new InformantRegisterHearingVenue(ljaName, courtHouse, courtSessions);
        }
    }
}
