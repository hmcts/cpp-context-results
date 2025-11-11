package uk.gov.moj.cpp.results.event.service;

import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@SuppressWarnings({"squid:S00107"})
public class NcesEmailNotificationTemplateData implements Serializable {

    private String amendmentDate;
    private String amendmentReason;
    private String caseReferences;
    private String dateDecisionMade;
    private String defendantName;
    private String divisionCode;
    private String gobAccountNumber;
    private List<ImpositionOffenceDetails> impositionOffenceDetails;
    private String listedDate;
    private String oldDivisionCode;
    private String oldGobAccountNumber;
    private String sendTo;
    private String subject;
    private String masterDefendantId;

    public NcesEmailNotificationTemplateData(final String amendmentDate,
                                             final String amendmentReason,
                                             final String caseReferences,
                                             final String dateDecisionMade,
                                             final String defendantName,
                                             final String divisionCode,
                                             final String gobAccountNumber,
                                             final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                             final String listedDate,
                                             final String oldDivisionCode,
                                             final String oldGobAccountNumber,
                                             final String sendTo,
                                             final String subject,
                                             final String masterDefendantId) {
        this.amendmentDate = amendmentDate;
        this.amendmentReason = amendmentReason;
        this.caseReferences = caseReferences;
        this.dateDecisionMade = dateDecisionMade;
        this.defendantName = defendantName;
        this.divisionCode = divisionCode;
        this.gobAccountNumber = gobAccountNumber;
        this.impositionOffenceDetails = impositionOffenceDetails;
        this.listedDate = listedDate;
        this.oldDivisionCode = oldDivisionCode;
        this.oldGobAccountNumber = oldGobAccountNumber;
        this.sendTo = sendTo;
        this.subject = subject;
        this.masterDefendantId = masterDefendantId;
    }

    public String getAmendmentDate() {
        return amendmentDate;
    }

    public void setAmendmentDate(final String amendmentDate) {
        this.amendmentDate = amendmentDate;
    }

    public String getAmendmentReason() {
        return amendmentReason;
    }

    public void setAmendmentReason(final String amendmentReason) {
        this.amendmentReason = amendmentReason;
    }

    public String getCaseReferences() {
        return caseReferences;
    }

    public void setCaseReferences(final String caseReferences) {
        this.caseReferences = caseReferences;
    }

    public String getDateDecisionMade() {
        return dateDecisionMade;
    }

    public void setDateDecisionMade(final String dateDecisionMade) {
        this.dateDecisionMade = dateDecisionMade;
    }

    public String getDefendantName() {
        return defendantName;
    }

    public void setDefendantName(final String defendantName) {
        this.defendantName = defendantName;
    }

    public String getDivisionCode() {
        return divisionCode;
    }

    public void setDivisionCode(final String divisionCode) {
        this.divisionCode = divisionCode;
    }

    public String getGobAccountNumber() {
        return gobAccountNumber;
    }

    public void setGobAccountNumber(final String gobAccountNumber) {
        this.gobAccountNumber = gobAccountNumber;
    }

    public List<ImpositionOffenceDetails> getImpositionOffenceDetails() {
        return impositionOffenceDetails;
    }

    public void setImpositionOffenceDetails(final List<ImpositionOffenceDetails> impositionOffenceDetails) {
        this.impositionOffenceDetails = impositionOffenceDetails;
    }

    public String getListedDate() {
        return listedDate;
    }

    public void setListedDate(final String listedDate) {
        this.listedDate = listedDate;
    }

    public String getOldDivisionCode() {
        return oldDivisionCode;
    }

    public void setOldDivisionCode(final String oldDivisionCode) {
        this.oldDivisionCode = oldDivisionCode;
    }

    public String getOldGobAccountNumber() {
        return oldGobAccountNumber;
    }

    public void setOldGobAccountNumber(final String oldGobAccountNumber) {
        this.oldGobAccountNumber = oldGobAccountNumber;
    }

    public String getSendTo() {
        return sendTo;
    }

    public void setSendTo(final String sendTo) {
        this.sendTo = sendTo;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public String getMasterDefendantId() {
        return masterDefendantId;
    }

    public void setMasterDefendantId(final String masterDefendantId) {
        this.masterDefendantId = masterDefendantId;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static Builder ncesEmailNotificationTemplateData() {
        return new Builder();
    }

    public static class Builder {
        private String amendmentDate;
        private String amendmentReason;
        private String caseReferences;
        private String dateDecisionMade;
        private String defendantName;
        private String divisionCode;
        private String gobAccountNumber;
        private List<ImpositionOffenceDetails> impositionOffenceDetails;
        private String listedDate;
        private String oldDivisionCode;
        private String oldGobAccountNumber;
        private String sendTo;
        private String subject;
        private String masterDefendantId;

        public Builder withAmendmentDate(final String amendmentDate) {
            this.amendmentDate = amendmentDate;
            return this;
        }

        public Builder withAmendmentReason(final String amendmentReason) {
            this.amendmentReason = amendmentReason;
            return this;
        }

        public Builder withCaseReferences(final String caseReferences) {
            this.caseReferences = caseReferences;
            return this;
        }

        public Builder withDateDecisionMade(final String dateDecisionMade) {
            this.dateDecisionMade = dateDecisionMade;
            return this;
        }

        public Builder withDefendantName(final String defendantName) {
            this.defendantName = defendantName;
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

        public Builder withImpositionOffenceDetails(final List<ImpositionOffenceDetails> impositionOffenceDetails) {
            this.impositionOffenceDetails = impositionOffenceDetails;
            return this;
        }

        public Builder withListedDate(final String listedDate) {
            this.listedDate = listedDate;
            return this;
        }

        public Builder withOldDivisionCode(final String oldDivisionCode) {
            this.oldDivisionCode = oldDivisionCode;
            return this;
        }

        public Builder withOldGobAccountNumber(final String oldGobAccountNumber) {
            this.oldGobAccountNumber = oldGobAccountNumber;
            return this;
        }

        public Builder withSendTo(final String sendTo) {
            this.sendTo = sendTo;
            return this;
        }

        public Builder withSubject(final String subject) {
            this.subject = subject;
            return this;
        }

        public Builder withMasterDefendantId(final String masterDefendantId) {
            this.masterDefendantId = masterDefendantId;
            return this;
        }


        public Builder withValuesFrom(final NcesEmailNotificationTemplateData ncesEmailNotificationTemplateData) {
            this.amendmentDate = ncesEmailNotificationTemplateData.getAmendmentDate();
            this.amendmentReason = ncesEmailNotificationTemplateData.getAmendmentReason();
            this.caseReferences = ncesEmailNotificationTemplateData.getCaseReferences();
            this.dateDecisionMade = ncesEmailNotificationTemplateData.getDateDecisionMade();
            this.defendantName = ncesEmailNotificationTemplateData.getDefendantName();
            this.divisionCode = ncesEmailNotificationTemplateData.getDivisionCode();
            this.gobAccountNumber = ncesEmailNotificationTemplateData.getGobAccountNumber();
            this.impositionOffenceDetails = ncesEmailNotificationTemplateData.getImpositionOffenceDetails();
            this.listedDate = ncesEmailNotificationTemplateData.getListedDate();
            this.oldDivisionCode = ncesEmailNotificationTemplateData.getOldDivisionCode();
            this.oldGobAccountNumber = ncesEmailNotificationTemplateData.getOldGobAccountNumber();
            this.sendTo = ncesEmailNotificationTemplateData.getSendTo();
            this.subject = ncesEmailNotificationTemplateData.getSubject();
            this.masterDefendantId = ncesEmailNotificationTemplateData.getMasterDefendantId();
            return this;
        }

        public NcesEmailNotificationTemplateData build() {
            return new NcesEmailNotificationTemplateData(amendmentDate,
                    amendmentReason,
                    caseReferences,
                    dateDecisionMade,
                    defendantName,
                    divisionCode,
                    gobAccountNumber,
                    impositionOffenceDetails,
                    listedDate,
                    oldDivisionCode,
                    oldGobAccountNumber,
                    sendTo,
                    subject,
                    masterDefendantId);
        }
    }


}
