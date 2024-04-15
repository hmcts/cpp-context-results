package uk.gov.moj.cpp.domains.resultdetails;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S2384"})
public class ApplicationResultDetails implements Serializable {
    private UUID applicationId;
    private String applicationTitle;
    private List<JudicialResultDetails> results;
    private List<OffenceResultDetails> courtOrderOffenceResultDetails;
    private String applicationSubjectFirstName;
    private String applicationSubjectLastName;

    public ApplicationResultDetails(final UUID applicationId,
                                    final String applicationTitle,
                                    final List<JudicialResultDetails> results,
                                    final List<OffenceResultDetails> courtOrderOffenceResultDetails,
                                    final String applicationSubjectFirstName,
                                    final String applicationSubjectLastName) {
        this.applicationId = applicationId;
        this.applicationTitle = applicationTitle;
        this.results = results;
        this.courtOrderOffenceResultDetails = courtOrderOffenceResultDetails;
        this.applicationSubjectFirstName = applicationSubjectFirstName;
        this.applicationSubjectLastName = applicationSubjectLastName;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getApplicationTitle() {
        return applicationTitle;
    }

    public List<JudicialResultDetails> getResults() {
        return results;
    }

    public List<OffenceResultDetails> getCourtOrderOffenceResultDetails() {
        return courtOrderOffenceResultDetails;
    }

    public String getApplicationSubjectFirstName() {
        return applicationSubjectFirstName;
    }

    public String getApplicationSubjectLastName() {
        return applicationSubjectLastName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ApplicationResultDetails that = (ApplicationResultDetails) o;
        return applicationId.equals(that.applicationId)
                && applicationTitle.equals(that.applicationTitle)
                && Objects.equals(results, that.results)
                && Objects.equals(courtOrderOffenceResultDetails, that.courtOrderOffenceResultDetails)
                && Objects.equals(applicationSubjectFirstName, that.applicationSubjectFirstName)
                && Objects.equals(applicationSubjectLastName, that.applicationSubjectLastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, applicationTitle, results, courtOrderOffenceResultDetails, applicationSubjectFirstName, applicationSubjectLastName);
    }
}
