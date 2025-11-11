package uk.gov.moj.cpp.domains.resultdetails;

import java.util.Optional;

public enum JudicialResultAmendmentType {
    NONE("NONE"),
    ADDED("ADDED"),
    DELETED("DELETED"),
    UPDATED("UPDATED");

    private final String value;

    JudicialResultAmendmentType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static Optional<JudicialResultAmendmentType> valueFor(final String value) {
        if(NONE.value.equals(value)) {
            return Optional.of(NONE);
        }

        if(ADDED.value.equals(value)) {
            return Optional.of(ADDED);
        }

        if(DELETED.value.equals(value)) {
            return Optional.of(DELETED);
        }

        if(UPDATED.value.equals(value)) {
            return Optional.of(UPDATED);
        }

        return Optional.empty();
    }
}
