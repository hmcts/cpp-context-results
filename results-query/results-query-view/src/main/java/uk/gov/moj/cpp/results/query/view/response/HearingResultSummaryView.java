package uk.gov.moj.cpp.results.query.view.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import uk.gov.moj.cpp.results.persist.entity.HearingResultSummary;

public class HearingResultSummaryView {

    private final UUID hearingId;
    private final String hearingType;
    private final LocalDate hearingDate;
    private final List<String> urns;
    private final DefendantView defendant;

    protected HearingResultSummaryView(final UUID hearingId, final String hearingType,
                                    final LocalDate hearingDate, final List<String> urns,
                                    final DefendantView defendant) {
        this.hearingId = hearingId;
        this.hearingType = hearingType;
        this.hearingDate = hearingDate;
        this.urns = urns;
        this.defendant = defendant;
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

    public Builder builder() {
        return new Builder(this.hearingId, this.hearingType, this.hearingDate, this.defendant);
    }

    private static DefendantView defendantView(final HearingResultSummary resultSummary) {
        return new DefendantView(resultSummary.getPersonId(),
                resultSummary.getDefendantFirstName(),
                resultSummary.getDefendantLastName());
    }

    @Override
    public String toString() {
        return "HearingResultView{" +
                "hearingId='" + hearingId + '\'' +
                ", hearingType='" + hearingType + '\'' +
                ", hearingDate='" + hearingDate + '\'' +
                ", urns='" + urns + '\'' +
                ", defendant=" + defendant +
                '}';
    }

    public static final class Builder {

        private final UUID hearingId;
        private final String hearingType;
        private final LocalDate hearingDate;
        private final DefendantView defendant;

        private List<String> urns;

        public Builder(final HearingResultSummary resultSummary) {
            this(resultSummary.getHearingId(), resultSummary.getHearingType(),
                    resultSummary.getHearingDate(), defendantView(resultSummary));
        }

        public Builder(final UUID hearingId, final String hearingType, final LocalDate hearingDate,
                                         final DefendantView defendant) {

            this.hearingId = hearingId;
            this.hearingType = hearingType;
            this.hearingDate = hearingDate;
            this.defendant = defendant;
            this.urns = new ArrayList<>();
        }

        public HearingResultSummaryView build() {
            return new HearingResultSummaryView(hearingId, hearingType, hearingDate, urns, defendant);
        }

        public Builder urns(final List<String> urns) {
            this.urns = urns;
            return this;
        }
    }
}
