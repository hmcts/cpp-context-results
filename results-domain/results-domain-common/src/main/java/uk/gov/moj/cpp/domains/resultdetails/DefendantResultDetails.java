package uk.gov.moj.cpp.domains.resultdetails;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;

@SuppressWarnings({"squid:S2384"})
public class DefendantResultDetails implements Serializable {

    private UUID defendantId;

    private String defendantName;

    private List<OffenceResultDetails> offences;



    public DefendantResultDetails(final UUID defendantId, final String defendantName, final List<OffenceResultDetails> offences) {
        this.defendantId = defendantId;
        this.defendantName = defendantName;
        this.offences = offences;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getDefendantName() {
        return defendantName;
    }

    public List<OffenceResultDetails> getOffences() {
        return offences;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefendantResultDetails that = (DefendantResultDetails) o;
        return Objects.equals(defendantId, that.defendantId) && Objects.equals(defendantName, that.defendantName) && Objects.equals(offences, that.offences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defendantId, defendantName, offences);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("defendantId", defendantId)
                .append("defendantName", defendantName)
                .append("offences", offences)
                .toString();
    }
}
