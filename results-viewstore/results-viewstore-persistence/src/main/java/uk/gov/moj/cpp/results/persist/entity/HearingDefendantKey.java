package uk.gov.moj.cpp.results.persist.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class HearingDefendantKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "hearing_id", nullable = false)
    private UUID hearingId;

    @Column(name = "defendant_id", nullable = false)
    private UUID defendantId;

    public HearingDefendantKey() {
        // for JPA
    }

    public HearingDefendantKey(final UUID hearingId, final UUID defendantId) {
        this.hearingId = hearingId;
        this.defendantId = defendantId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hearingId, defendantId);
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
        final HearingDefendantKey other = (HearingDefendantKey) obj;
        return Objects.equals(this.defendantId, other.defendantId) && Objects.equals(this.hearingId, other.hearingId);
    }
}
