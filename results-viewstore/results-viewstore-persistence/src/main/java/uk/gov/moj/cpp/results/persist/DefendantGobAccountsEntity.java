package uk.gov.moj.cpp.results.persist;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "defendant_gob_accounts")
public class DefendantGobAccountsEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "master_defendant_id", nullable = false)
    private UUID masterDefendantId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "case_references")
    private String caseReferences;

    @Column(name = "created_date_time")
    private ZonedDateTime createdDateTime;

    public DefendantGobAccountsEntity() {
    }

    public DefendantGobAccountsEntity(final UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(final String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getCaseReferences() {
        return caseReferences;
    }

    public void setCaseReferences(final String caseReferences) {
        this.caseReferences = caseReferences;
    }

    public ZonedDateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(final ZonedDateTime createdDateTime) {
        this.createdDateTime = createdDateTime;
    }
}
