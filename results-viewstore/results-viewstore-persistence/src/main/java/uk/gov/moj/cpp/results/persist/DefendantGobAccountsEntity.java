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

    @Column(name = "hearing_id", nullable = false)
    private UUID hearingId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "case_references", nullable = false)
    private String caseReferences;

    @Column(name = "account_request_time")
    private ZonedDateTime accountRequestTime;

    @Column(name = "created_time", nullable = false)
    private ZonedDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private ZonedDateTime updatedTime;

    public DefendantGobAccountsEntity() {
    }

    public DefendantGobAccountsEntity(final UUID id) {
        this.id = id;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    public ZonedDateTime getAccountRequestTime() {
        return accountRequestTime;
    }

    public void setAccountRequestTime(final ZonedDateTime accountRequestTime) {
        this.accountRequestTime = accountRequestTime;
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

    public ZonedDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final ZonedDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public ZonedDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final ZonedDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}
