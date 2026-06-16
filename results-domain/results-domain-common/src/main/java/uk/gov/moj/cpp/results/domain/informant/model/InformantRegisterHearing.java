package uk.gov.moj.cpp.results.domain.informant.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InformantRegisterHearing {

    private final String courtRoom;
    private final String hearingStartTime;
    private final List<InformantRegisterDefendant> defendants;

    @JsonCreator
    public InformantRegisterHearing(
            @JsonProperty("courtRoom") final String courtRoom,
            @JsonProperty("hearingStartTime") final String hearingStartTime,
            @JsonProperty("defendants") final List<InformantRegisterDefendant> defendants) {
        this.courtRoom = courtRoom;
        this.hearingStartTime = hearingStartTime;
        this.defendants = defendants;
    }

    public String getCourtRoom() {
        return courtRoom;
    }

    public String getHearingStartTime() {
        return hearingStartTime;
    }

    public List<InformantRegisterDefendant> getDefendants() {
        return defendants;
    }

    public static Builder informantRegisterHearing() {
        return new Builder();
    }

    public static class Builder {
        private String courtRoom;
        private String hearingStartTime;
        private List<InformantRegisterDefendant> defendants;

        public Builder withCourtRoom(final String courtRoom) {
            this.courtRoom = courtRoom;
            return this;
        }

        public Builder withHearingStartTime(final String hearingStartTime) {
            this.hearingStartTime = hearingStartTime;
            return this;
        }

        public Builder withDefendants(final List<InformantRegisterDefendant> defendants) {
            this.defendants = defendants;
            return this;
        }

        public InformantRegisterHearing build() {
            return new InformantRegisterHearing(courtRoom, hearingStartTime, defendants);
        }
    }
}
