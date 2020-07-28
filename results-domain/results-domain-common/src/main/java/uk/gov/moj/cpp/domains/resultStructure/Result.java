package uk.gov.moj.cpp.domains.resultStructure;

import uk.gov.justice.core.courts.Category;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.JudicialResultPrompt;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class Result implements Serializable {

    private final UUID resultId;
    private final long serialVersionUID = -4449907148803214679L;
    private LocalDate amendmentDate;
    private String amendmentReason;
    private LocalDate approvedDate;
    private Category category;
    private String cjsCode;
    private DelegatedPowers courtClerk;
    private DelegatedPowers delegatedPowers;
    private DelegatedPowers fourEyesApproval;
    private Boolean isAdjournmentResult;
    private Boolean isAvailableForCourtExtract;
    private Boolean isConvictedResult;
    private Boolean isFinancialResult;
    private List<JudicialResultPrompt> judicialResultPrompts;
    private String label;
    private String lastSharedDateTime;
    private LocalDate orderedDate;
    private UUID orderedHearingId;
    private BigDecimal rank;
    private List<String> usergroups;
    private String welshLabel;
    private Boolean isDeleted;
    private Boolean lifeDuration;
    private String resultText;
    private Boolean terminatesOffenceProceedings;
    private Boolean publishedAsAPrompt;
    private Boolean excludedFromResults;
    private Boolean alwaysPublished;
    private Boolean urgent;
    private Boolean d20;
    private UUID judicialResultTypeId;

    @SuppressWarnings("squid:S00107")
    public Result(final UUID resultId, final LocalDate amendmentDate, final String amendmentReason, final LocalDate approvedDate, final Category category, final String cjsCode,
                  final DelegatedPowers courtClerk, final DelegatedPowers delegatedPowers, final DelegatedPowers fourEyesApproval, final Boolean isAdjournmentResult,
                  final Boolean isAvailableForCourtExtract, final Boolean isConvictedResult, final Boolean isFinancialResult, final List<JudicialResultPrompt> judicialResultPrompts,
                  final String label, final String lastSharedDateTime, final LocalDate orderedDate, final UUID orderedHearingId, final BigDecimal rank, final List<String> usergroups,
                  final String welshLabel, final Boolean isDeleted, final Boolean lifeDuration, final String resultText, final Boolean terminatesOffenceProceedings, final Boolean publishedAsAPrompt,
                  final Boolean excludedFromResults, final Boolean alwaysPublished, final Boolean urgent, final Boolean d20, final UUID judicialResultTypeId) {
        this.resultId = resultId;
        this.amendmentDate = amendmentDate;
        this.amendmentReason = amendmentReason;
        this.approvedDate = approvedDate;
        this.category = category;
        this.cjsCode = cjsCode;
        this.courtClerk = courtClerk;
        this.delegatedPowers = delegatedPowers;
        this.fourEyesApproval = fourEyesApproval;
        this.isAdjournmentResult = isAdjournmentResult;
        this.isAvailableForCourtExtract = isAvailableForCourtExtract;
        this.isConvictedResult = isConvictedResult;
        this.isFinancialResult = isFinancialResult;
        this.judicialResultPrompts = judicialResultPrompts;
        this.label = label;
        this.lastSharedDateTime = lastSharedDateTime;
        this.orderedDate = orderedDate;
        this.orderedHearingId = orderedHearingId;
        this.rank = rank;
        this.usergroups = usergroups;
        this.welshLabel = welshLabel;
        this.lifeDuration = lifeDuration;
        this.resultText = resultText;
        this.terminatesOffenceProceedings = terminatesOffenceProceedings;
        this.publishedAsAPrompt = publishedAsAPrompt;
        this.excludedFromResults = excludedFromResults;
        this.alwaysPublished = alwaysPublished;
        this.urgent = urgent;
        this.d20 = d20;
        this.judicialResultTypeId = judicialResultTypeId;
    }

    public static Builder result() {
        return new Result.Builder();
    }

    public LocalDate getAmendmentDate() {
        return amendmentDate;
    }

    public Result setAmendmentDate(final LocalDate amendmentDate) {
        this.amendmentDate = amendmentDate;
        return this;
    }

    public String getAmendmentReason() {
        return amendmentReason;
    }

    public Result setAmendmentReason(final String amendmentReason) {
        this.amendmentReason = amendmentReason;
        return this;
    }

    public LocalDate getApprovedDate() {
        return approvedDate;
    }

    public Result setApprovedDate(final LocalDate approvedDate) {
        this.approvedDate = approvedDate;
        return this;
    }

    public Category getCategory() {
        return category;
    }

    public Result setCategory(final Category category) {
        this.category = category;
        return this;
    }

    public String getCjsCode() {
        return cjsCode;
    }

    public Result setCjsCode(final String cjsCode) {
        this.cjsCode = cjsCode;
        return this;
    }

    public DelegatedPowers getCourtClerk() {
        return courtClerk;
    }

    public Result setCourtClerk(final DelegatedPowers courtClerk) {
        this.courtClerk = courtClerk;
        return this;
    }

    public DelegatedPowers getDelegatedPowers() {
        return delegatedPowers;
    }

    public Result setDelegatedPowers(final DelegatedPowers delegatedPowers) {
        this.delegatedPowers = delegatedPowers;
        return this;
    }

    public DelegatedPowers getFourEyesApproval() {
        return fourEyesApproval;
    }

    public Result setFourEyesApproval(final DelegatedPowers fourEyesApproval) {
        this.fourEyesApproval = fourEyesApproval;
        return this;
    }

    public Boolean getIsAdjournmentResult() {
        return isAdjournmentResult;
    }

    public Result setIsAdjournmentResult(final Boolean isAdjournmentResult) {
        this.isAdjournmentResult = isAdjournmentResult;
        return this;
    }

    public Boolean getIsAvailableForCourtExtract() {
        return isAvailableForCourtExtract;
    }

    public Result setIsAvailableForCourtExtract(final Boolean isAvailableForCourtExtract) {
        this.isAvailableForCourtExtract = isAvailableForCourtExtract;
        return this;
    }

    public Boolean getIsConvictedResult() {
        return isConvictedResult;
    }

    public Result setIsConvictedResult(final Boolean isConvictedResult) {
        this.isConvictedResult = isConvictedResult;
        return this;
    }

    public Boolean getIsFinancialResult() {
        return isFinancialResult;
    }

    public Result setIsFinancialResult(final Boolean isFinancialResult) {
        this.isFinancialResult = isFinancialResult;
        return this;
    }

    public List<JudicialResultPrompt> getJudicialResultPrompts() {
        return judicialResultPrompts;
    }

    public Result setJudicialResultPrompts(final List<JudicialResultPrompt> judicialResultPrompts) {
        this.judicialResultPrompts = judicialResultPrompts;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public Result setLabel(final String label) {
        this.label = label;
        return this;
    }

    public String getLastSharedDateTime() {
        return lastSharedDateTime;
    }

    public Result setLastSharedDateTime(final String lastSharedDateTime) {
        this.lastSharedDateTime = lastSharedDateTime;
        return this;
    }

    public LocalDate getOrderedDate() {
        return orderedDate;
    }

    public Result setOrderedDate(final LocalDate orderedDate) {
        this.orderedDate = orderedDate;
        return this;
    }

    public UUID getOrderedHearingId() {
        return orderedHearingId;
    }

    public Result setOrderedHearingId(final UUID orderedHearingId) {
        this.orderedHearingId = orderedHearingId;
        return this;
    }

    public BigDecimal getRank() {
        return rank;
    }

    public Result setRank(final BigDecimal rank) {
        this.rank = rank;
        return this;
    }

    public List<String> getUsergroups() {
        return usergroups;
    }

    public Result setUsergroups(final List<String> usergroups) {
        this.usergroups = usergroups;
        return this;
    }

    public String getWelshLabel() {
        return welshLabel;
    }

    public Result setWelshLabel(final String welshLabel) {
        this.welshLabel = welshLabel;
        return this;
    }

    public UUID getResultId() {
        return resultId;
    }

    public Boolean getDeleted() {
        return isDeleted;
    }

    public void setDeleted(final Boolean deleted) {
        isDeleted = deleted;
    }

    public Boolean getLifeDuration() {
        return lifeDuration;
    }

    public void setLifeDuration(final Boolean lifeDuration) {
        this.lifeDuration = lifeDuration;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(final String resultText) {
        this.resultText = resultText;
    }

    public Boolean getTerminatesOffenceProceedings() {
        return terminatesOffenceProceedings;
    }

    public Boolean getPublishedAsAPrompt() {
        return publishedAsAPrompt;
    }

    public Boolean getExcludedFromResults() {
        return excludedFromResults;
    }

    public Boolean getAlwaysPublished() {
        return alwaysPublished;
    }

    public Boolean getUrgent() {
        return urgent;
    }

    public Boolean getD20() {
        return d20;
    }

    public UUID getJudicialResultTypeId() {
        return judicialResultTypeId;
    }

    @SuppressWarnings("squid:S3776")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Result that = (Result) o;

        if (serialVersionUID != that.serialVersionUID) {
            return false;
        }
        if (amendmentDate != null ? !amendmentDate.equals(that.amendmentDate) : that.amendmentDate != null) {
            return false;
        }
        if (amendmentReason != null ? !amendmentReason.equals(that.amendmentReason) : that.amendmentReason != null) {
            return false;
        }
        if (approvedDate != null ? !approvedDate.equals(that.approvedDate) : that.approvedDate != null) {
            return false;
        }
        if (category != that.category) {
            return false;
        }
        if (cjsCode != null ? !cjsCode.equals(that.cjsCode) : that.cjsCode != null) {
            return false;
        }
        if (courtClerk != null ? !courtClerk.equals(that.courtClerk) : that.courtClerk != null) {
            return false;
        }
        if (delegatedPowers != null ? !delegatedPowers.equals(that.delegatedPowers) : that.delegatedPowers != null) {
            return false;
        }
        if (fourEyesApproval != null ? !fourEyesApproval.equals(that.fourEyesApproval) : that.fourEyesApproval != null) {
            return false;
        }
        if (isAdjournmentResult != null ? !isAdjournmentResult.equals(that.isAdjournmentResult) : that.isAdjournmentResult != null) {
            return false;
        }
        if (isAvailableForCourtExtract != null ? !isAvailableForCourtExtract.equals(that.isAvailableForCourtExtract) : that.isAvailableForCourtExtract != null) {
            return false;
        }
        if (isConvictedResult != null ? !isConvictedResult.equals(that.isConvictedResult) : that.isConvictedResult != null) {
            return false;
        }
        if (isFinancialResult != null ? !isFinancialResult.equals(that.isFinancialResult) : that.isFinancialResult != null) {
            return false;
        }
        if (judicialResultPrompts != null ? !judicialResultPrompts.equals(that.judicialResultPrompts) : that.judicialResultPrompts != null) {
            return false;
        }
        if (label != null ? !label.equals(that.label) : that.label != null) {
            return false;
        }
        if (lastSharedDateTime != null ? !lastSharedDateTime.equals(that.lastSharedDateTime) : that.lastSharedDateTime != null) {
            return false;
        }
        if (orderedDate != null ? !orderedDate.equals(that.orderedDate) : that.orderedDate != null) {
            return false;
        }
        if (orderedHearingId != null ? !orderedHearingId.equals(that.orderedHearingId) : that.orderedHearingId != null) {
            return false;
        }
        if (rank != null ? !rank.equals(that.rank) : that.rank != null) {
            return false;
        }
        if (isDeleted != null ? !isDeleted.equals(that.isDeleted) : that.isDeleted != null) {
            return false;
        }
        if (usergroups != null ? !usergroups.equals(that.usergroups) : that.usergroups != null) {
            return false;
        }
        if (lifeDuration != null ? !lifeDuration.equals(that.lifeDuration) : that.lifeDuration != null) {
            return false;
        }
        if (resultText != null ? !resultText.equals(that.resultText) : that.resultText != null) {
            return false;
        }
        if (terminatesOffenceProceedings != null ? !terminatesOffenceProceedings.equals(that.terminatesOffenceProceedings) : that.terminatesOffenceProceedings != null) {
            return false;
        }
        if (publishedAsAPrompt != null ? !publishedAsAPrompt.equals(that.publishedAsAPrompt) : that.publishedAsAPrompt != null) {
            return false;
        }
        if (excludedFromResults != null ? !excludedFromResults.equals(that.excludedFromResults) : that.excludedFromResults != null) {
            return false;
        }
        if (alwaysPublished != null ? !alwaysPublished.equals(that.alwaysPublished) : that.alwaysPublished != null) {
            return false;
        }
        if (urgent != null ? !urgent.equals(that.urgent) : that.urgent != null) {
            return false;
        }
        if (d20 != null ? !d20.equals(that.d20) : that.d20 != null) {
            return false;
        }
        if (judicialResultTypeId != null ? !judicialResultTypeId.equals(that.judicialResultTypeId) : that.judicialResultTypeId != null) {
            return false;
        }
        return welshLabel != null ? welshLabel.equals(that.welshLabel) : that.welshLabel == null;
    }

    @SuppressWarnings("squid:S3776")
    @Override
    public int hashCode() {
        int result = amendmentDate != null ? amendmentDate.hashCode() : 0;
        result = 31 * result + (amendmentReason != null ? amendmentReason.hashCode() : 0);
        result = 31 * result + (approvedDate != null ? approvedDate.hashCode() : 0);
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (cjsCode != null ? cjsCode.hashCode() : 0);
        result = 31 * result + (courtClerk != null ? courtClerk.hashCode() : 0);
        result = 31 * result + (delegatedPowers != null ? delegatedPowers.hashCode() : 0);
        result = 31 * result + (fourEyesApproval != null ? fourEyesApproval.hashCode() : 0);
        result = 31 * result + (isAdjournmentResult != null ? isAdjournmentResult.hashCode() : 0);
        result = 31 * result + (isAvailableForCourtExtract != null ? isAvailableForCourtExtract.hashCode() : 0);
        result = 31 * result + (isConvictedResult != null ? isConvictedResult.hashCode() : 0);
        result = 31 * result + (isFinancialResult != null ? isFinancialResult.hashCode() : 0);
        result = 31 * result + (judicialResultPrompts != null ? judicialResultPrompts.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (lastSharedDateTime != null ? lastSharedDateTime.hashCode() : 0);
        result = 31 * result + (orderedDate != null ? orderedDate.hashCode() : 0);
        result = 31 * result + (orderedHearingId != null ? orderedHearingId.hashCode() : 0);
        result = 31 * result + (rank != null ? rank.hashCode() : 0);
        result = 31 * result + (usergroups != null ? usergroups.hashCode() : 0);
        result = 31 * result + (welshLabel != null ? welshLabel.hashCode() : 0);
        result = 31 * result + (isDeleted != null ? isDeleted.hashCode() : 0);
        result = 31 * result + (lifeDuration != null ? lifeDuration.hashCode() : 0);
        result = 31 * result + (resultText != null ? resultText.hashCode() : 0);
        result = 31 * result + (terminatesOffenceProceedings != null ? terminatesOffenceProceedings.hashCode() : 0);
        result = 31 * result + (publishedAsAPrompt != null ? publishedAsAPrompt.hashCode() : 0);
        result = 31 * result + (excludedFromResults != null ? excludedFromResults.hashCode() : 0);
        result = 31 * result + (alwaysPublished != null ? alwaysPublished.hashCode() : 0);
        result = 31 * result + (urgent != null ? urgent.hashCode() : 0);
        result = 31 * result + (d20 != null ? d20.hashCode() : 0);
        result = 31 * result + (judicialResultTypeId != null ? judicialResultTypeId.hashCode() : 0);
        result = 31 * result + (int) (serialVersionUID ^ (serialVersionUID >>> 32));
        return result;
    }

    public static class Builder {
        private UUID resultId;

        private LocalDate amendmentDate;

        private String amendmentReason;

        private LocalDate approvedDate;

        private Category category;

        private String cjsCode;

        private DelegatedPowers courtClerk;

        private DelegatedPowers delegatedPowers;

        private DelegatedPowers fourEyesApproval;

        private Boolean isAdjournmentResult;

        private Boolean isAvailableForCourtExtract;

        private Boolean isConvictedResult;

        private Boolean isFinancialResult;

        private List<JudicialResultPrompt> judicialResultPrompts;

        private String label;

        private String lastSharedDateTime;

        private LocalDate orderedDate;

        private UUID orderedHearingId;

        private BigDecimal rank;

        private List<String> usergroups;

        private String welshLabel;

        private Boolean isDeleted;

        private Boolean lifeDuration;

        private String resultText;

        private Boolean terminatesOffenceProceedings;

        private Boolean publishedAsAPrompt;

        private Boolean excludedFromResults;

        private Boolean alwaysPublished;

        private Boolean urgent;

        private Boolean d20;

        private UUID judicialResultTypeId;

        public Builder withResultId(final UUID resultId) {
            this.resultId = resultId;
            return this;
        }

        public Builder withAmendmentDate(final LocalDate amendmentDate) {
            this.amendmentDate = amendmentDate;
            return this;
        }

        public Builder withAmendmentReason(final String amendmentReason) {
            this.amendmentReason = amendmentReason;
            return this;
        }

        public Builder withApprovedDate(final LocalDate approvedDate) {
            this.approvedDate = approvedDate;
            return this;
        }

        public Builder withCategory(final Category category) {
            this.category = category;
            return this;
        }

        public Builder withCjsCode(final String cjsCode) {
            this.cjsCode = cjsCode;
            return this;
        }

        public Builder withCourtClerk(final DelegatedPowers courtClerk) {
            this.courtClerk = courtClerk;
            return this;
        }

        public Builder withDelegatedPowers(final DelegatedPowers delegatedPowers) {
            this.delegatedPowers = delegatedPowers;
            return this;
        }

        public Builder withFourEyesApproval(final DelegatedPowers fourEyesApproval) {
            this.fourEyesApproval = fourEyesApproval;
            return this;
        }

        public Builder withIsAdjournmentResult(final Boolean isAdjournmentResult) {
            this.isAdjournmentResult = isAdjournmentResult;
            return this;
        }

        public Builder withIsAvailableForCourtExtract(final Boolean isAvailableForCourtExtract) {
            this.isAvailableForCourtExtract = isAvailableForCourtExtract;
            return this;
        }

        public Builder withIsConvictedResult(final Boolean isConvictedResult) {
            this.isConvictedResult = isConvictedResult;
            return this;
        }

        public Builder withIsFinancialResult(final Boolean isFinancialResult) {
            this.isFinancialResult = isFinancialResult;
            return this;
        }

        public Builder withJudicialResultPrompts(final List<JudicialResultPrompt> judicialResultPrompts) {
            this.judicialResultPrompts = judicialResultPrompts;
            return this;
        }

        public Builder withLabel(final String label) {
            this.label = label;
            return this;
        }

        public Builder withLastSharedDateTime(final String lastSharedDateTime) {
            this.lastSharedDateTime = lastSharedDateTime;
            return this;
        }

        public Builder withOrderedDate(final LocalDate orderedDate) {
            this.orderedDate = orderedDate;
            return this;
        }

        public Builder withOrderedHearingId(final UUID orderedHearingId) {
            this.orderedHearingId = orderedHearingId;
            return this;
        }

        public Builder withRank(final BigDecimal rank) {
            this.rank = rank;
            return this;
        }

        public Builder withUsergroups(final List<String> usergroups) {
            this.usergroups = usergroups;
            return this;
        }

        public Builder withWelshLabel(final String welshLabel) {
            this.welshLabel = welshLabel;
            return this;
        }

        public Builder withIsDeleted(final Boolean isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }

        public Builder withLifeDuration(final Boolean lifeDuration) {
            this.lifeDuration = lifeDuration;
            return this;
        }

        public Builder withResultText(final String resultText) {
            this.resultText = resultText;
            return this;
        }

        public Builder withTerminatesOffenceProceedings(final Boolean terminatesOffenceProceedings) {
            this.terminatesOffenceProceedings = terminatesOffenceProceedings;
            return this;
        }
        public Builder withPublishedAsAPrompt(final Boolean publishedAsAPrompt) {
            this.publishedAsAPrompt = publishedAsAPrompt;
            return this;
        }

        public Builder withExcludedFromResults(final Boolean excludedFromResults) {
            this.excludedFromResults = excludedFromResults;
            return this;
        }

        public Builder withAlwaysPublished(final Boolean alwaysPublished) {
            this.alwaysPublished = alwaysPublished;
            return this;
        }

        public Builder withUrgent(final Boolean urgent) {
            this.urgent = urgent;
            return this;
        }

        public Builder withD20(final Boolean d20) {
            this.d20 = d20;
            return this;
        }

        public Builder withJudicialResultTypeId(final UUID judicialResultTypeId) {
            this.judicialResultTypeId = judicialResultTypeId;
            return this;
        }

        public Result build() {
            return new Result(resultId, amendmentDate, amendmentReason, approvedDate, category, cjsCode, courtClerk, delegatedPowers, fourEyesApproval, isAdjournmentResult, isAvailableForCourtExtract, isConvictedResult, isFinancialResult, judicialResultPrompts, label, lastSharedDateTime, orderedDate, orderedHearingId, rank, usergroups, welshLabel, isDeleted, lifeDuration, resultText,
            terminatesOffenceProceedings, publishedAsAPrompt, excludedFromResults,  alwaysPublished, urgent, d20, judicialResultTypeId);
        }
    }
}
