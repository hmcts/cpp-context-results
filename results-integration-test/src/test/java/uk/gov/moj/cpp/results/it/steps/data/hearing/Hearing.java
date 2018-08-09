package uk.gov.moj.cpp.results.it.steps.data.hearing;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class Hearing {

    private final UUID hearingId;
    private final LocalDate startDate;
    private final LocalTime startTime;
    private final String hearingType;
    private final int duration;
    private String courtCenterName;
    private Judge judge;

    public Hearing(final UUID hearingId, final LocalDate startDate, final LocalTime startTime,
                   final String hearingType, final int duration, final String courtCenterName,
                   final Judge judge) {
        this.hearingId = hearingId;
        this.startDate = startDate;
        this.startTime = startTime;
        this.hearingType = hearingType;
        this.duration = duration;
        this.courtCenterName = courtCenterName;
        this.judge = judge;
    }


    public UUID getHearingId() {
        return hearingId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public String getHearingType() {
        return hearingType;
    }

    public int getDuration() {
        return duration;
    }

    public Judge getJudge() {
        return judge;
    }

    public String getCourtCenterName() {
        return courtCenterName;
    }
}
