package uk.gov.moj.cpp.results.persist.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class HearingResultedDocumentKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "hearing_id", nullable = false)
    private UUID hearingId;

    @Column(name = "hearing_day", nullable = false)
    private LocalDate hearingDay;

    public HearingResultedDocumentKey() {
        // for JPA
    }

    public HearingResultedDocumentKey(final UUID hearingId,  LocalDate hearingDay) {
        this.hearingId = hearingId;
        this.hearingDay = hearingDay;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    public LocalDate getHearingDay() {
        return hearingDay;
    }

    public void setHearingDay(final LocalDate hearingDay) {
        this.hearingDay = hearingDay;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hearingId, hearingDay);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HearingResultedDocumentKey other = (HearingResultedDocumentKey) obj;
        return Objects.equals(this.hearingDay, other.hearingDay) && Objects.equals(this.hearingId, other.hearingId);
    }
}
