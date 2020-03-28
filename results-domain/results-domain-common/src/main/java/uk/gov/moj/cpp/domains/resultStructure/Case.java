package uk.gov.moj.cpp.domains.resultStructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Case implements Serializable {

    private final UUID caseId;

    private final String urn;

    private final String prosecutionAuthorityCode;

    private final List<Defendant> defendants = new ArrayList<>();

    public Case(final UUID caseId, final String urn, final String prosecutionAuthorityCode) {
        this.caseId = caseId;
        this.urn = urn;
        this.prosecutionAuthorityCode = prosecutionAuthorityCode;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }

    public String getProsecutionAuthorityCode() {
        return prosecutionAuthorityCode;
    }

    public List<Defendant> getDefendants() {
        return defendants;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Case aCase = (Case) o;

        if (caseId != null ? !caseId.equals(aCase.caseId) : aCase.caseId != null) {
            return false;
        }
        if (urn != null ? !urn.equals(aCase.urn) : aCase.urn != null) {
            return false;
        }
        if (prosecutionAuthorityCode != null ? !prosecutionAuthorityCode.equals(aCase.prosecutionAuthorityCode) : aCase.prosecutionAuthorityCode != null) {
            return false;
        }
        return defendants.equals(aCase.defendants);
    }

    @Override
    public int hashCode() {
        int result = caseId != null ? caseId.hashCode() : 0;
        result = 31 * result + (urn != null ? urn.hashCode() : 0);
        result = 31 * result + (defendants.hashCode());
        result = 31 * result + (prosecutionAuthorityCode != null ? prosecutionAuthorityCode.hashCode() : 0);
        return result;
    }
}
