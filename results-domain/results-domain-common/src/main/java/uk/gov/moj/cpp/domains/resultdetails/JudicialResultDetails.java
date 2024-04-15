package uk.gov.moj.cpp.domains.resultdetails;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class JudicialResultDetails implements Serializable {
    private UUID resultId;
    private String title;
    private UUID resultTypeId;
    private JudicialResultAmendmentType amendmentType;

    public JudicialResultDetails(UUID resultId, String title, UUID resultTypeId, JudicialResultAmendmentType amendmentType) {
        this.resultId = resultId;
        this.title = title;
        this.resultTypeId = resultTypeId;
        this.amendmentType = amendmentType;
    }

    public UUID getResultId() {
        return resultId;
    }

    public String getTitle() {
        return title;
    }

    public UUID getResultTypeId() {
        return resultTypeId;
    }

    public JudicialResultAmendmentType getAmendmentType() {
        return amendmentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JudicialResultDetails that = (JudicialResultDetails) o;
        return resultId.equals(that.resultId) && title.equals(that.title) && resultTypeId.equals(that.resultTypeId) && amendmentType == that.amendmentType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resultId, title, resultTypeId, amendmentType);
    }
}
