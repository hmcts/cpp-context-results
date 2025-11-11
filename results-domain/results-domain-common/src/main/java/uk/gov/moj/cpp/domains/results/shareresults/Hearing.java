package uk.gov.moj.cpp.domains.results.shareresults;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S2384"})
public class Hearing {

    private UUID id;
    private String hearingType;
    private ZonedDateTime startDateTime;
    private List<ZonedDateTime> hearingDates;
    private CourtCentre courtCentre;
    private YouthCourt youthCourt;
    private List<UUID> youthCourtDefendantIds;
    private List<Attendee> attendees;
    private List<SharedResultLine> sharedResultLines;

    public UUID getId() {
        return id;
    }

    public Hearing setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getHearingType() {
        return hearingType;
    }

    public Hearing setHearingType(String hearingType) {
        this.hearingType = hearingType;
        return this;
    }

    public ZonedDateTime getStartDateTime() {
        return startDateTime;
    }

    public Hearing setStartDateTime(ZonedDateTime startDateTime) {
        this.startDateTime = startDateTime;
        return this;
    }

    public List<ZonedDateTime> getHearingDates() {
        return hearingDates;
    }

    public Hearing setHearingDates(List<ZonedDateTime> hearingDates) {
        this.hearingDates = hearingDates;
        return this;
    }

    public CourtCentre getCourtCentre() {
        return courtCentre;
    }

    public Hearing setCourtCentre(CourtCentre courtCentre) {
        this.courtCentre = courtCentre;
        return this;
    }

    public List<Attendee> getAttendees() {
        return attendees;
    }

    public Hearing setAttendees(List<Attendee> attendees) {
        this.attendees = attendees;
        return this;
    }

    public List<SharedResultLine> getSharedResultLines() {
        return sharedResultLines;
    }

    public Hearing setSharedResultLines(List<SharedResultLine> sharedResultLines) {
        this.sharedResultLines = sharedResultLines;
        return this;
    }

    public YouthCourt getYouthCourt() {
        return youthCourt;
    }

    public void setYouthCourt(YouthCourt youthCourt) {
        this.youthCourt = youthCourt;
    }

    public List<UUID> getYouthCourtDefendantIds() {
        return youthCourtDefendantIds;
    }

    public void setYouthCourtDefendantIds(List<UUID> youthCourtDefendantIds) {
        this.youthCourtDefendantIds = youthCourtDefendantIds;
    }

    public static Hearing hearing() {
        return new Hearing();
    }
}
