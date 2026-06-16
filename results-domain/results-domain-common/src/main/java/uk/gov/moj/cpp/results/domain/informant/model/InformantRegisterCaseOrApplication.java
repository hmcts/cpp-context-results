package uk.gov.moj.cpp.results.domain.informant.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InformantRegisterCaseOrApplication {

    private final String caseOrApplicationReference;
    private final String arrestSummonsNumber;
    private final String applicationParticulars;
    private final List<InformantRegisterOffence> offences;
    private final List<InformantRegisterResult> results;

    @JsonCreator
    public InformantRegisterCaseOrApplication(
            @JsonProperty("caseOrApplicationReference") final String caseOrApplicationReference,
            @JsonProperty("arrestSummonsNumber") final String arrestSummonsNumber,
            @JsonProperty("applicationParticulars") final String applicationParticulars,
            @JsonProperty("offences") final List<InformantRegisterOffence> offences,
            @JsonProperty("results") final List<InformantRegisterResult> results) {
        this.caseOrApplicationReference = caseOrApplicationReference;
        this.arrestSummonsNumber = arrestSummonsNumber;
        this.applicationParticulars = applicationParticulars;
        this.offences = offences;
        this.results = results;
    }

    public String getCaseOrApplicationReference() {
        return caseOrApplicationReference;
    }

    public String getArrestSummonsNumber() {
        return arrestSummonsNumber;
    }

    public String getApplicationParticulars() {
        return applicationParticulars;
    }

    public List<InformantRegisterOffence> getOffences() {
        return offences;
    }

    public List<InformantRegisterResult> getResults() {
        return results;
    }

    public static Builder informantRegisterCaseOrApplication() {
        return new Builder();
    }

    public static class Builder {
        private String caseOrApplicationReference;
        private String arrestSummonsNumber;
        private String applicationParticulars;
        private List<InformantRegisterOffence> offences;
        private List<InformantRegisterResult> results;

        public Builder withCaseOrApplicationReference(final String caseOrApplicationReference) {
            this.caseOrApplicationReference = caseOrApplicationReference;
            return this;
        }

        public Builder withArrestSummonsNumber(final String arrestSummonsNumber) {
            this.arrestSummonsNumber = arrestSummonsNumber;
            return this;
        }

        public Builder withApplicationParticulars(final String applicationParticulars) {
            this.applicationParticulars = applicationParticulars;
            return this;
        }

        public Builder withOffences(final List<InformantRegisterOffence> offences) {
            this.offences = offences;
            return this;
        }

        public Builder withResults(final List<InformantRegisterResult> results) {
            this.results = results;
            return this;
        }

        public InformantRegisterCaseOrApplication build() {
            return new InformantRegisterCaseOrApplication(caseOrApplicationReference,
                    arrestSummonsNumber, applicationParticulars, offences, results);
        }
    }
}
