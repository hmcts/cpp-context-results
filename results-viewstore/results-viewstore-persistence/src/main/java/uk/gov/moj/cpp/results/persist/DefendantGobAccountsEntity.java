package uk.gov.moj.cpp.results.persist;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "defendant_gob_accounts")
public class DefendantGobAccountsEntity {

    @EmbeddedId
    private DefendantGobAccountsId id;

    @Column(name = "hearing_id", nullable = false)
    private UUID hearingId;

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

    public DefendantGobAccountsEntity(final UUID masterDefendantId, final UUID accountCorrelationId) {
        this.id = new DefendantGobAccountsId(masterDefendantId, accountCorrelationId);
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

    public DefendantGobAccountsId getId() {
        return id;
    }

    public void setId(final DefendantGobAccountsId id) {
        this.id = id;
    }

    public UUID getMasterDefendantId() {
        return id != null ? id.getMasterDefendantId() : null;
    }

    public void setMasterDefendantId(final UUID masterDefendantId) {
        if (id == null) {
            id = new DefendantGobAccountsId();
        }
        id.setMasterDefendantId(masterDefendantId);
    }

    public UUID getAccountCorrelationId() {
        return id != null ? id.getAccountCorrelationId() : null;
    }

    public void setCorrelationId(final UUID accountCorrelationId) {
        if (id == null) {
            id = new DefendantGobAccountsId();
        }
        id.setAccountCorrelationId(accountCorrelationId);
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
