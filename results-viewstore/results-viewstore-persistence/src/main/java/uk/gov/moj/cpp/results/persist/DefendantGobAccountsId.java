package uk.gov.moj.cpp.results.persist;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class DefendantGobAccountsId implements Serializable {

    @Column(name = "master_defendant_id", nullable = false)
    private UUID masterDefendantId;

    @Column(name = "account_correlation_id", nullable = false)
    private UUID accountCorrelationId;

    public DefendantGobAccountsId() {
    }

    public DefendantGobAccountsId(final UUID masterDefendantId, final UUID accountCorrelationId) {
        this.masterDefendantId = masterDefendantId;
        this.accountCorrelationId = accountCorrelationId;
    }

    public UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    public void setMasterDefendantId(final UUID masterDefendantId) {
        this.masterDefendantId = masterDefendantId;
    }

    public UUID getAccountCorrelationId() {
        return accountCorrelationId;
    }

    public void setAccountCorrelationId(final UUID accountCorrelationId) {
        this.accountCorrelationId = accountCorrelationId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DefendantGobAccountsId that = (DefendantGobAccountsId) o;
        return Objects.equals(masterDefendantId, that.masterDefendantId) &&
               Objects.equals(accountCorrelationId, that.accountCorrelationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterDefendantId, accountCorrelationId);
    }
}
