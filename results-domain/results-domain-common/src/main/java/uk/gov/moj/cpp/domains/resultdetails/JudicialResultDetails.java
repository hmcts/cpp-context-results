package uk.gov.moj.cpp.domains.resultdetails;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class JudicialResultDetails implements Serializable {
    private UUID resultId;
    private String title;
    private UUID resultTypeId;
    private JudicialResultAmendmentType amendmentType;
    private boolean isQualifyingResult;

    public JudicialResultDetails(final UUID resultId, final String title, final UUID resultTypeId, final JudicialResultAmendmentType amendmentType, final boolean isQualifyingResult) {
        this.resultId = resultId;
        this.title = title;
        this.resultTypeId = resultTypeId;
        this.amendmentType = amendmentType;
        this.isQualifyingResult = isQualifyingResult;
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

    public boolean isQualifyingResult() {
        return isQualifyingResult;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final JudicialResultDetails that = (JudicialResultDetails) o;
        return isQualifyingResult == that.isQualifyingResult && Objects.equals(resultId, that.resultId) && Objects.equals(title, that.title) && Objects.equals(resultTypeId, that.resultTypeId) && amendmentType == that.amendmentType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resultId, title, resultTypeId, amendmentType, isQualifyingResult);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("resultId", resultId)
                .append("title", title)
                .append("resultTypeId", resultTypeId)
                .append("amendmentType", amendmentType)
                .toString();
    }
}
