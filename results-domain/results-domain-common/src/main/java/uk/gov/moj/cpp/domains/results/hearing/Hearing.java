package uk.gov.moj.cpp.domains.results.hearing;

import java.time.LocalDate;
import java.util.UUID;
@SuppressWarnings("squid:S00107")
public class Hearing {

    private final UUID id;
    private final String hearingType;
    private final LocalDate startDate;
    private final String courtCentreName;
    private final String courtCode;
    private final String judgeName;
    private final String prosecutorName;
    private final String defenceName;

    public Hearing(final UUID id, final String hearingType, final LocalDate startDate,
                   final String courtName, final String courtCode, final String judgeName,
                   final String prosecutorName, final String defenceName) {
        this.id = id;
        this.hearingType = hearingType;
        this.startDate = startDate;
        this.courtCentreName = courtName;
        this.courtCode = courtCode;
        this.judgeName = judgeName;
        this.prosecutorName = prosecutorName;
        this.defenceName = defenceName;
    }

    public UUID getId() {
        return id;
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
