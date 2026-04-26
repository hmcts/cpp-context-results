package uk.gov.moj.cpp.results.domain.aggregate.utils;


import java.io.Serializable;
import java.util.UUID;

public record ApplicationMetadata(UUID applicationId, String applicationType) implements Serializable {
    private static final long serialVersionUID = 1L;
}
