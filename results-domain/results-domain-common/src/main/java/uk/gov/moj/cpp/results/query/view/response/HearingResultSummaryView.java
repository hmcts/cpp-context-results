package uk.gov.moj.cpp.results.query.view.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"squid:S2384"})
public class HearingResultSummaryView {

    private final UUID hearingId;
    private final String hearingType;
    private final LocalDate hearingDate;
    private final List<String> urns;
    private final DefendantView defendant;
    private final UUID courtCentreId;

    public HearingResultSummaryView(@JsonProperty(value = "hearingId", required = true) final UUID hearingId,
                                    @JsonProperty(value = "hearingType", required = true) final String hearingType,
                                    @JsonProperty(value = "hearingDate", required = true) final LocalDate hearingDate,
                                    @JsonProperty(value = "urns", required = true) final List<String> urns,
                                    @JsonProperty(value = "defendant", required = true) final DefendantView defendant,
                                    @JsonProperty(value = "courtCentreId", required = true) final UUID courtCentreId) {
        this.hearingId = hearingId;
        this.hearingType = hearingType;
        this.hearingDate = hearingDate;
        this.urns = urns;
        this.defendant = defendant;
        this.courtCentreId = courtCentreId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public String getHearingType() {
        return hearingType;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public List<String> getUrns() {
        return urns;
    }

    public DefendantView getDefendant() {
        return defendant;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public Builder builder() {
        return new Builder(this.hearingId, this.hearingType, this.hearingDate, this.defendant, this.courtCentreId);
    }

    @Override
    public String toString() {
        return "HearingResultView{" +
                "hearingId='" + hearingId + '\'' +
                ", hearingType='" + hearingType + '\'' +
                ", hearingDate='" + hearingDate + '\'' +
                ", urns='" + urns + '\'' +
                ", defendant=" + defendant +
                ", courtCentreId=" + courtCentreId +
                '}';
    }

    public static final class Builder {

        private final UUID hearingId;
        private final String hearingType;
        private final LocalDate hearingDate;
        private final DefendantView defendant;
        private final UUID courtCentreId;

        private List<String> urns;

        public Builder(final UUID hearingId, final String hearingType, final LocalDate hearingDate,
                       final DefendantView defendant, final UUID courtCentreId) {

            this.hearingId = hearingId;
            this.hearingType = hearingType;
            this.hearingDate = hearingDate;
            this.defendant = defendant;
            this.urns = new ArrayList<>();
            this.courtCentreId = courtCentreId;
        }

        public HearingResultSummaryView build() {
            return new HearingResultSummaryView(hearingId, hearingType, hearingDate, urns, defendant, courtCentreId);
        }

        public Builder urns(final List<String> urns) {
            this.urns = urns;
            return this;
        }
    }
}
