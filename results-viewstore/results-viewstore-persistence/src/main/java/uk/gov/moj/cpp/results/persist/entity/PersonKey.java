package uk.gov.moj.cpp.results.persist.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"unused"})
public class PersonKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private UUID hearingId;

    public PersonKey() {
        // for JPA
    }

    public PersonKey(final UUID personId, final UUID hearingId) {
        this.id = personId;
        this.hearingId = hearingId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hearingId);
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
        final PersonKey other = (PersonKey) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.hearingId, other.hearingId);
    }
}
