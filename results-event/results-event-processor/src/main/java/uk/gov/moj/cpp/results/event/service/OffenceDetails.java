package uk.gov.moj.cpp.results.event.service;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;

public class OffenceDetails {
    private Set<DcsOffence> addedOffences;
    private Set<DcsOffence> removedOffences;

    @JsonCreator
    public OffenceDetails(final Set<DcsOffence> addedOffences, final Set<DcsOffence> removedOffences) {
        this.addedOffences = addedOffences;
        this.removedOffences = removedOffences;
    }

    private OffenceDetails(final Builder builder) {
        addedOffences = builder.addedOffences;
        removedOffences = builder.removedOffences;
    }

    public Set<DcsOffence> getAddedOffences() {
        return addedOffences;
    }

    public Set<DcsOffence> getRemovedOffences() {
        return removedOffences;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final OffenceDetails offenceDetails = (OffenceDetails) o;
        return Objects.equals(addedOffences, offenceDetails.addedOffences) && Objects.equals(removedOffences, offenceDetails.removedOffences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addedOffences, removedOffences);
    }

    public static final class Builder {
        private Set<DcsOffence> addedOffences;
        private Set<DcsOffence> removedOffences;

        private Builder() {
        }

        public static Builder newOffence() {
            return new Builder();
        }

        public Builder withAddedOffences(final Set<DcsOffence> value) {
            addedOffences = value;
            return this;
        }

        public Builder withRemovedOffences(final Set<DcsOffence> value) {
            removedOffences = value;
            return this;
        }

        public OffenceDetails build() {
            return new OffenceDetails(this);
        }
    }
}
