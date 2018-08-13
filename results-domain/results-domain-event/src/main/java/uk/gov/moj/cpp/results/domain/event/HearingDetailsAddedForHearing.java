package uk.gov.moj.cpp.results.domain.event;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

@Event("results.hearing-details-added-for-hearing")
@SuppressWarnings("squid:S00107")
public class HearingDetailsAddedForHearing {

    private final UUID hearingId;
    private final UUID personId;
    private final String hearingType;
    private final LocalDate startDate;
    private final String courtCentreName;
    private final String courtCode;
    private final String judgeName;
    private final String prosecutorName;
    private final String defenceName;

    public HearingDetailsAddedForHearing(final UUID hearingId, final UUID personId,
                    final String hearingType,
                                         final LocalDate startDate, final String courtCentreName, final String courtCode,
                                         final String judgeName, final String prosecutorName, final String defenceName) {
        this.hearingId = hearingId;
        this.personId = personId;
        this.hearingType = hearingType;
        this.startDate = startDate;
        this.courtCentreName = courtCentreName;
        this.courtCode = courtCode;
        this.judgeName = judgeName;
        this.prosecutorName = prosecutorName;
        this.defenceName = defenceName;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public String getHearingType() {
        return hearingType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public String getCourtCode() {
        return courtCode;
    }

    public String getJudgeName() {
        return judgeName;
    }

    public String getProsecutorName() {
        return prosecutorName;
    }

    public String getDefenceName() {
        return defenceName;
    }

}
