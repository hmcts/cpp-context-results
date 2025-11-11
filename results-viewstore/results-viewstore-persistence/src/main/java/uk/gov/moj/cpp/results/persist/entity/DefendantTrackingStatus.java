package uk.gov.moj.cpp.results.persist.entity;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "defendant_tracking_status")
public class DefendantTrackingStatus {

    @Id
    @Column(name = "offence_id")
    private UUID offenceId;

    @Column(name = "defendant_id")
    private UUID defendantId;

    @Column(name = "em_last_modified_time")
    private ZonedDateTime emLastModifiedTime;

    @Column(name = "em_status")
    private Boolean emStatus;

    @Column(name = "woa_status")
    private Boolean woaStatus;

    @Column(name = "woa_last_modified_time")
    private ZonedDateTime woaLastModifiedTime;


    public UUID getOffenceId() {
        return offenceId;
    }

    public void setOffenceId(final UUID offenceId) {
        this.offenceId = offenceId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public ZonedDateTime getEmLastModifiedTime() {
        return emLastModifiedTime;
    }

    public void setEmLastModifiedTime(final ZonedDateTime emLastModifiedTime) {
        this.emLastModifiedTime = emLastModifiedTime;
    }

    public Boolean getEmStatus() {
        return emStatus;
    }

    public void setEmStatus(final Boolean emStatus) {
        this.emStatus = emStatus;
    }

    public Boolean getWoaStatus() {
        return woaStatus;
    }

    public void setWoaStatus(final Boolean woaStatus) {
        this.woaStatus = woaStatus;
    }

    public ZonedDateTime getWoaLastModifiedTime() {
        return woaLastModifiedTime;
    }

    public void setWoaLastModifiedTime(final ZonedDateTime woaLastModifiedTime) {
        this.woaLastModifiedTime = woaLastModifiedTime;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefendantTrackingStatus that = (DefendantTrackingStatus) o;
        return Objects.equals(offenceId, that.offenceId) && Objects.equals(defendantId, that.defendantId)
                && equalsStatusAndLastModified(that);
    }

    private boolean equalsStatusAndLastModified(final DefendantTrackingStatus that) {
        return Objects.equals(emLastModifiedTime, that.emLastModifiedTime) &&
                Objects.equals(emStatus, that.emStatus) && Objects.equals(woaStatus, that.woaStatus)
                && Objects.equals(woaLastModifiedTime, that.woaLastModifiedTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offenceId, defendantId, emLastModifiedTime, emStatus, woaStatus, woaLastModifiedTime);
    }

    @Override
    public String toString() {
        return "DefendantTrackingStatus{" +
                "offenceId=" + offenceId +
                ", defendantId=" + defendantId +
                ", emLastModifiedTime=" + emLastModifiedTime +
                ", eomStatus=" + emStatus +
                ", woaStatus=" + woaStatus +
                ", waLastModifiedTime=" + woaLastModifiedTime +
                '}';
    }
}

