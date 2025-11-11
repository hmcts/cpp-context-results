package uk.gov.moj.cpp.results.domain.aggregate.utils;

import uk.gov.justice.hearing.courts.OffenceResultsDetails;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

public class CorrelationItem implements Serializable {
    @Serial
    private static final long serialVersionUID = -4327123646461499831L;

    private final UUID accountCorrelationId;
    private final UUID hearingId;

    private final String accountDivisionCode;

    private final String accountNumber;

    private final ZonedDateTime createdTime;

    private final List<String> prosecutionCaseReferences;
    private final List<OffenceResultsDetails> offenceResultsDetailsList;

    @JsonCreator
    public CorrelationItem(final UUID accountCorrelationId, final UUID hearingId, final String accountDivisionCode, final String accountNumber, final ZonedDateTime createdTime,
                           final List<String> prosecutionCaseReferences, final List<OffenceResultsDetails> offenceResultsDetailsList) {
        this.accountCorrelationId = accountCorrelationId;
        this.hearingId = hearingId;
        this.accountDivisionCode = accountDivisionCode;
        this.accountNumber = accountNumber;
        this.createdTime = createdTime;
        this.prosecutionCaseReferences = prosecutionCaseReferences;
        this.offenceResultsDetailsList = offenceResultsDetailsList;
    }

    public UUID getAccountCorrelationId() {
        return accountCorrelationId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public String getAccountDivisionCode() {
        return accountDivisionCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public ZonedDateTime getCreatedTime() {
        return createdTime;
    }

    public List<String> getProsecutionCaseReferences() {
        return prosecutionCaseReferences;
    }

    public List<OffenceResultsDetails> getOffenceResultsDetailsList() {
        return offenceResultsDetailsList;
    }

    public static CorrelationItem.Builder correlationItem() {
        return new CorrelationItem.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final CorrelationItem that = (CorrelationItem) obj;

        return java.util.Objects.equals(this.accountCorrelationId, that.accountCorrelationId) &&
                java.util.Objects.equals(this.hearingId, that.hearingId) &&
                java.util.Objects.equals(this.accountDivisionCode, that.accountDivisionCode) &&
                java.util.Objects.equals(this.accountNumber, that.accountNumber) &&
                java.util.Objects.equals(this.createdTime, that.createdTime);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(accountCorrelationId, hearingId, accountDivisionCode, accountNumber, createdTime);
    }

    @Override
    public String toString() {
        return "CorrelationItem{" +
                "accountCorrelationId='" + accountCorrelationId + "'," +
                "hearingId='" + hearingId + "'," +
                "accountDivisionCode='" + accountDivisionCode + "'," +
                "accountNumber='" + accountNumber + "'," +
                "createdTime='" + createdTime + "'," +
                "prosecutionCaseReferences='" + prosecutionCaseReferences + "'" +
                "offenceResultsDetailsList='" + offenceResultsDetailsList + "'" +
                "}";
    }

    public static class Builder {
        private UUID accountCorrelationId;
        private UUID hearingId;

        private String accountDivisionCode;

        private String accountNumber;

        private ZonedDateTime createdTime;

        private List<String> prosecutionCaseReferences;
        private List<OffenceResultsDetails> offenceResultsDetailsList;

        public CorrelationItem.Builder withAccountCorrelationId(final UUID accountCorrelationId) {
            this.accountCorrelationId = accountCorrelationId;
            return this;
        }

        public CorrelationItem.Builder withHearingId(final UUID hearingId) {
            this.hearingId = hearingId;
            return this;
        }

        public CorrelationItem.Builder withAccountDivisionCode(final String accountDivisionCode) {
            this.accountDivisionCode = accountDivisionCode;
            return this;
        }

        public CorrelationItem.Builder withAccountNumber(final String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public CorrelationItem.Builder withCreatedTime(final ZonedDateTime createdTime) {
            this.createdTime = createdTime;
            return this;
        }

        public CorrelationItem.Builder withProsecutionCaseReferences(final List<String> prosecutionCaseReferences) {
            this.prosecutionCaseReferences = prosecutionCaseReferences;
            return this;
        }

        public CorrelationItem.Builder withOffenceResultsDetailsList(final List<OffenceResultsDetails> offenceResultsDetailsList) {
            this.offenceResultsDetailsList = offenceResultsDetailsList;
            return this;
        }

        public CorrelationItem.Builder withValuesFrom(final CorrelationItem correlationItem) {
            this.accountCorrelationId = correlationItem.getAccountCorrelationId();
            this.hearingId = correlationItem.getHearingId();
            this.accountDivisionCode = correlationItem.getAccountDivisionCode();
            this.accountNumber = correlationItem.getAccountNumber();
            this.createdTime = correlationItem.getCreatedTime();
            this.prosecutionCaseReferences = correlationItem.getProsecutionCaseReferences();
            this.offenceResultsDetailsList = correlationItem.getOffenceResultsDetailsList();
            return this;
        }

        public CorrelationItem build() {
            return new CorrelationItem(accountCorrelationId, hearingId, accountDivisionCode, accountNumber, createdTime, prosecutionCaseReferences, offenceResultsDetailsList);
        }
    }
}