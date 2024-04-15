package uk.gov.moj.cpp.domains.resultdetails;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S2384"})
public class OffenceResultDetails implements Serializable {
    private UUID offenceId;
    private Integer offenceNo;
    private Integer offenceCount;
    private String offenceTitle;
    private List<JudicialResultDetails> results;

    public OffenceResultDetails(final UUID offenceId,
                                final Integer offenceNo,
                                final Integer offenceCount,
                                final String offenceTitle,
                                final List<JudicialResultDetails> results) {
        this.offenceId = offenceId;
        this.offenceNo = offenceNo;
        this.offenceCount = offenceCount;
        this.offenceTitle = offenceTitle;
        this.results = results;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public Integer getOffenceNo() {
        return offenceNo;
    }

    public Integer getOffenceCount() {
        return offenceCount;
    }

    public String getOffenceTitle() {
        return offenceTitle;
    }

    public List<JudicialResultDetails> getResults() {
        return results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final OffenceResultDetails that = (OffenceResultDetails) o;
        return offenceId.equals(that.offenceId) && Objects.equals(offenceNo, that.offenceNo) && Objects.equals(offenceCount, that.offenceCount)
                && offenceTitle.equals(that.offenceTitle) && results.equals(that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offenceId, offenceNo, offenceCount, offenceTitle, results);
    }
}
