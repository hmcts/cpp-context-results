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

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    public DefendantGobAccountsId() {
    }

    public DefendantGobAccountsId(final UUID masterDefendantId, final UUID correlationId) {
        this.masterDefendantId = masterDefendantId;
        this.correlationId = correlationId;
    }

    public UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    public void setMasterDefendantId(final UUID masterDefendantId) {
        this.masterDefendantId = masterDefendantId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(final UUID correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DefendantGobAccountsId that = (DefendantGobAccountsId) o;
        return Objects.equals(masterDefendantId, that.masterDefendantId) &&
               Objects.equals(correlationId, that.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterDefendantId, correlationId);
    }
}
