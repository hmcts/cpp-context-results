package uk.gov.moj.cpp.results.persist.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ResultPromptKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private String label;

    private UUID hearingResultId;

    public ResultPromptKey() {

    }

    public ResultPromptKey(String label, UUID hearingResultId) {
        this.label = label;
        this.hearingResultId = hearingResultId;
    }

    public String getLabel() {
        return label;
    }

    public UUID getHearingResultId() {
        return hearingResultId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, hearingResultId);
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
        final ResultPromptKey other = (ResultPromptKey) obj;
        return Objects.equals(this.label, other.label) && Objects.equals(this.hearingResultId, other.hearingResultId);
    }
}
