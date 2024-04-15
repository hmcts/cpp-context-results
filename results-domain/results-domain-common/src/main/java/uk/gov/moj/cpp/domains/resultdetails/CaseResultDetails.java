package uk.gov.moj.cpp.domains.resultdetails;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S2384"})
public class CaseResultDetails implements Serializable {
    private UUID caseId;

    private List<DefendantResultDetails> defendantResultDetails;
    private List<ApplicationResultDetails> applicationResultDetails;

    public CaseResultDetails(final UUID caseId, final List<DefendantResultDetails> defendantResultDetails, final List<ApplicationResultDetails> applicationResultDetails) {
        this.caseId = caseId;
        this.defendantResultDetails = defendantResultDetails;
        this.applicationResultDetails = applicationResultDetails;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public List<DefendantResultDetails> getDefendantResultDetails() {
        return defendantResultDetails;
    }

    public List<ApplicationResultDetails> getApplicationResultDetails() {
        return applicationResultDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CaseResultDetails that = (CaseResultDetails) o;
        return caseId.equals(that.caseId) && Objects.equals(defendantResultDetails, that.defendantResultDetails)
                && Objects.equals(applicationResultDetails, that.applicationResultDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, defendantResultDetails, applicationResultDetails);
    }
}
