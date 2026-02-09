package uk.gov.moj.cpp.domains.results;

import java.util.Objects;

public final class MigratedMasterDefendantCaseDetails {

    private final String masterDefendantId;
    private final String caseId;
    private final String fineAccountNumber;
    private final String courtEmail;
    private final String division;
    private final String defendantId;
    private final String defendantName;
    private final String defendantAddress;
    private final String originalDateOfConviction;
    private final String defendantEmail;
    private final String defendantDateOfBirth;
    private final String defendantContactNumber;
    private final String migrationSourceSystemCaseIdentifier;
    private final String caseURN;

    private MigratedMasterDefendantCaseDetails(final Builder builder) {
        this.masterDefendantId = builder.masterDefendantId;
        this.caseId = builder.caseId;
        this.fineAccountNumber = builder.fineAccountNumber;
        this.courtEmail = builder.courtEmail;
        this.division = builder.division;
        this.defendantId = builder.defendantId;
        this.defendantName = builder.defendantName;
        this.defendantAddress = builder.defendantAddress;
        this.originalDateOfConviction = builder.originalDateOfConviction;
        this.defendantEmail = builder.defendantEmail;
        this.defendantDateOfBirth = builder.defendantDateOfBirth;
        this.defendantContactNumber = builder.defendantContactNumber;
        this.migrationSourceSystemCaseIdentifier = builder.migrationSourceSystemCaseIdentifier;
        this.caseURN = builder.caseURN;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String masterDefendantId() {
        return masterDefendantId;
    }

    public String caseId() {
        return caseId;
    }

    public String fineAccountNumber() {
        return fineAccountNumber;
    }

    public String courtEmail() {
        return courtEmail;
    }

    public String division() {
        return division;
    }

    public String defendantId() {
        return defendantId;
    }

    public String defendantName() {
        return defendantName;
    }

    public String defendantAddress() {
        return defendantAddress;
    }

    public String originalDateOfConviction() {
        return originalDateOfConviction;
    }

    public String defendantEmail() {
        return defendantEmail;
    }

    public String defendantDateOfBirth() {
        return defendantDateOfBirth;
    }

    public String defendantContactNumber() {
        return defendantContactNumber;
    }

    public String migrationSourceSystemCaseIdentifier() {
        return migrationSourceSystemCaseIdentifier;
    }

    public String caseURN() {
        return caseURN;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MigratedMasterDefendantCaseDetails that = (MigratedMasterDefendantCaseDetails) o;
        return Objects.equals(masterDefendantId, that.masterDefendantId)
                && Objects.equals(caseId, that.caseId)
                && Objects.equals(fineAccountNumber, that.fineAccountNumber)
                && Objects.equals(courtEmail, that.courtEmail)
                && Objects.equals(division, that.division)
                && Objects.equals(defendantId, that.defendantId)
                && Objects.equals(defendantName, that.defendantName)
                && Objects.equals(defendantAddress, that.defendantAddress)
                && Objects.equals(originalDateOfConviction, that.originalDateOfConviction)
                && Objects.equals(defendantEmail, that.defendantEmail)
                && Objects.equals(defendantDateOfBirth, that.defendantDateOfBirth)
                && Objects.equals(defendantContactNumber, that.defendantContactNumber)
                && Objects.equals(migrationSourceSystemCaseIdentifier, that.migrationSourceSystemCaseIdentifier)
                && Objects.equals(caseURN, that.caseURN);
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterDefendantId, caseId, fineAccountNumber, courtEmail, division, defendantId,
                defendantName, defendantAddress, originalDateOfConviction, defendantEmail, defendantDateOfBirth,
                defendantContactNumber, migrationSourceSystemCaseIdentifier, caseURN);
    }

    public static final class Builder {
        private String masterDefendantId;
        private String caseId;
        private String fineAccountNumber;
        private String courtEmail;
        private String division;
        private String defendantId;
        private String defendantName;
        private String defendantAddress;
        private String originalDateOfConviction;
        private String defendantEmail;
        private String defendantDateOfBirth;
        private String defendantContactNumber;
        private String migrationSourceSystemCaseIdentifier;
        private String caseURN;

        private Builder() {
        }

        public Builder withMasterDefendantId(final String masterDefendantId) {
            this.masterDefendantId = masterDefendantId;
            return this;
        }

        public Builder withCaseId(final String caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withFineAccountNumber(final String fineAccountNumber) {
            this.fineAccountNumber = fineAccountNumber;
            return this;
        }

        public Builder withCourtEmail(final String courtEmail) {
            this.courtEmail = courtEmail;
            return this;
        }

        public Builder withDivision(final String division) {
            this.division = division;
            return this;
        }

        public Builder withDefendantId(final String defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public Builder withDefendantName(final String defendantName) {
            this.defendantName = defendantName;
            return this;
        }

        public Builder withDefendantAddress(final String defendantAddress) {
            this.defendantAddress = defendantAddress;
            return this;
        }

        public Builder withOriginalDateOfConviction(final String originalDateOfConviction) {
            this.originalDateOfConviction = originalDateOfConviction;
            return this;
        }

        public Builder withDefendantEmail(final String defendantEmail) {
            this.defendantEmail = defendantEmail;
            return this;
        }

        public Builder withDefendantDateOfBirth(final String defendantDateOfBirth) {
            this.defendantDateOfBirth = defendantDateOfBirth;
            return this;
        }

        public Builder withDefendantContactNumber(final String defendantContactNumber) {
            this.defendantContactNumber = defendantContactNumber;
            return this;
        }

        public Builder withMigrationSourceSystemCaseIdentifier(final String migrationSourceSystemCaseIdentifier) {
            this.migrationSourceSystemCaseIdentifier = migrationSourceSystemCaseIdentifier;
            return this;
        }

        public Builder withCaseURN(final String caseURN) {
            this.caseURN = caseURN;
            return this;
        }

        public MigratedMasterDefendantCaseDetails build() {
            return new MigratedMasterDefendantCaseDetails(this);
        }
    }
}
