package uk.gov.moj.cpp.results.event.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DcsCreateCaseRequest {

    private UUID caseId;
    private String caseUrn;
    private String prosecutionAuthority;
    private List<DcsDefendant> defendants;

    public UUID getCaseId() {
        return caseId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public String getProsecutionAuthority() {
        return prosecutionAuthority;
    }

    public List<DcsDefendant> getDefendants() {
        return defendants;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DcsCreateCaseRequest that = (DcsCreateCaseRequest) o;
        return Objects.equals(caseId, that.caseId) && Objects.equals(caseUrn, that.caseUrn) && Objects.equals(prosecutionAuthority, that.prosecutionAuthority)  && Objects.equals(defendants, that.defendants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, caseUrn, prosecutionAuthority, defendants);
    }

    public static final class DcsCreateCaseRequestBuilder {
        private UUID caseId;
        private String caseUrn;
        private String prosecutionAuthority;
        private List<DcsDefendant> defendants;

        private DcsCreateCaseRequestBuilder() {
        }

        public static DcsCreateCaseRequestBuilder aDcsCreateCaseRequest() {
            return new DcsCreateCaseRequestBuilder();
        }

        public DcsCreateCaseRequestBuilder withCaseId(UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public DcsCreateCaseRequestBuilder withCaseUrn(String caseUrn) {
            this.caseUrn = caseUrn;
            return this;
        }

        public DcsCreateCaseRequestBuilder withProsecutionAuthority(String prosecutionAuthority) {
            this.prosecutionAuthority = prosecutionAuthority;
            return this;
        }

        public DcsCreateCaseRequestBuilder withDefendants(List<DcsDefendant> defendants) {
            this.defendants = defendants;
            return this;
        }

        public DcsCreateCaseRequest build() {
            DcsCreateCaseRequest dcsCreateCaseRequest = new DcsCreateCaseRequest();
            dcsCreateCaseRequest.prosecutionAuthority = this.prosecutionAuthority;
            dcsCreateCaseRequest.caseId = this.caseId;
            dcsCreateCaseRequest.caseUrn = this.caseUrn;
            dcsCreateCaseRequest.defendants = this.defendants;
            return dcsCreateCaseRequest;
        }
    }
}
