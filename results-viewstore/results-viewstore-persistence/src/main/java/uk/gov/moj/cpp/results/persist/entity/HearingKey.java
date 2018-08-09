package uk.gov.moj.cpp.results.persist.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class HearingKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private UUID personId;

    public HearingKey() {
        // for JPA
    }

    public HearingKey(final UUID hearingId, final UUID personId) {
        this.id = hearingId;
        this.personId = personId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, personId);
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
        final HearingKey other = (HearingKey) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.personId, other.personId);
    }
}
