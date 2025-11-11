package uk.gov.moj.cpp.domains.results.structure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Case implements Serializable {

    private final UUID caseId;

    private final String urn;

    private final String prosecutionAuthorityCode;

    private final Boolean isCivil;

    private final UUID groupId;

    private final Boolean isGroupMember;

    private final Boolean isGroupMaster;

    private final List<Defendant> defendants = new ArrayList<>();

    public Case(final UUID caseId, final String urn, final String prosecutionAuthorityCode,
                final Boolean isCivil, final UUID groupId, final Boolean isGroupMember, final Boolean isGroupMaster) {
        this.caseId = caseId;
        this.urn = urn;
        this.prosecutionAuthorityCode = prosecutionAuthorityCode;
        this.isCivil = isCivil;
        this.groupId = groupId;
        this.isGroupMember = isGroupMember;
        this.isGroupMaster = isGroupMaster;
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
        return Collections.synchronizedList(defendants);
    }

    public Boolean getIsCivil() {
        return isCivil;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public Boolean getIsGroupMember() {
        return isGroupMember;
    }

    public Boolean getIsGroupMaster() {
        return isGroupMaster;
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

        if (!Objects.equals(caseId, aCase.caseId)) {
            return false;
        }

        if (!Objects.equals(urn, aCase.urn)) {
            return false;
        }

        if (!Objects.equals(prosecutionAuthorityCode, aCase.prosecutionAuthorityCode)) {
            return false;
        }

        if (!Objects.equals(isCivil, aCase.isCivil)) {
            return false;
        }

        if (!Objects.equals(groupId, aCase.groupId)) {
            return false;
        }

        if (!Objects.equals(isGroupMember, aCase.isGroupMember)) {
            return false;
        }

        if (!Objects.equals(isGroupMaster, aCase.isGroupMaster)) {
            return false;
        }

        return Objects.equals(defendants, aCase.defendants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, urn, prosecutionAuthorityCode, isCivil, groupId, isGroupMember, isGroupMaster, defendants);
    }
}
