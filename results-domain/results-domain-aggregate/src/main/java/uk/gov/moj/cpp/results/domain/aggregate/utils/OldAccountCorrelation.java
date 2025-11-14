package uk.gov.moj.cpp.results.domain.aggregate.utils;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

public class OldAccountCorrelation implements Serializable {
  private static final long serialVersionUID = 5598428510536811112L;

  private final UUID accountCorrelationId;

  private final ZonedDateTime createdTime;

  private final String divisionCode;

  private final String gobAccountNumber;

  @JsonCreator
  public OldAccountCorrelation(final UUID accountCorrelationId, final ZonedDateTime createdTime, final String divisionCode, final String gobAccountNumber) {
    this.accountCorrelationId = accountCorrelationId;
    this.createdTime = createdTime;
    this.divisionCode = divisionCode;
    this.gobAccountNumber = gobAccountNumber;
  }

  public UUID getAccountCorrelationId() {
    return accountCorrelationId;
  }

  public ZonedDateTime getCreatedTime() {
    return createdTime;
  }

  public String getDivisionCode() {
    return divisionCode;
  }

  public String getGobAccountNumber() {
    return gobAccountNumber;
  }

  public static Builder oldAccountCorrelation() {
    return new Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final OldAccountCorrelation that = (OldAccountCorrelation) obj;

    return java.util.Objects.equals(this.accountCorrelationId, that.accountCorrelationId) &&
    java.util.Objects.equals(this.createdTime, that.createdTime) &&
    java.util.Objects.equals(this.divisionCode, that.divisionCode) &&
    java.util.Objects.equals(this.gobAccountNumber, that.gobAccountNumber);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(accountCorrelationId, createdTime, divisionCode, gobAccountNumber);}

  @Override
  public String toString() {
    return "OldAccountCorrelation{" +
    	"accountCorrelationId='" + accountCorrelationId + "'," +
    	"createdTime='" + createdTime + "'," +
    	"divisionCode='" + divisionCode + "'," +
    	"gobAccountNumber='" + gobAccountNumber + "'" +
    "}";
  }

  public static class Builder {
    private UUID accountCorrelationId;

    private ZonedDateTime createdTime;

    private String divisionCode;

    private String gobAccountNumber;

    public Builder withAccountCorrelationId(final UUID accountCorrelationId) {
      this.accountCorrelationId = accountCorrelationId;
      return this;
    }

    public Builder withCreatedTime(final ZonedDateTime createdTime) {
      this.createdTime = createdTime;
      return this;
    }

    public Builder withDivisionCode(final String divisionCode) {
      this.divisionCode = divisionCode;
      return this;
    }

    public Builder withGobAccountNumber(final String gobAccountNumber) {
      this.gobAccountNumber = gobAccountNumber;
      return this;
    }

    public Builder withValuesFrom(final OldAccountCorrelation oldAccountCorrelation) {
      this.accountCorrelationId = oldAccountCorrelation.getAccountCorrelationId();
      this.createdTime = oldAccountCorrelation.getCreatedTime();
      this.divisionCode = oldAccountCorrelation.getDivisionCode();
      this.gobAccountNumber = oldAccountCorrelation.getGobAccountNumber();
      return this;
    }

    public OldAccountCorrelation build() {
      return new OldAccountCorrelation(accountCorrelationId, createdTime, divisionCode, gobAccountNumber);
    }
  }
}
