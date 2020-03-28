package uk.gov.moj.cpp.domains.resultStructure;


import uk.gov.justice.core.courts.AllocationDecision;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Offence implements Serializable {

    private long serialVersionUID = -9176890205806560222L;
    private UUID id;

    private AllocationDecision allocationDecision ;

    private String offenceCode;

    private Integer offenceSequenceNumber;

    private String wording;

    private Integer offenceDateCode;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate arrestDate;

    private LocalDate chargeDate;

    private OffenceFacts offenceFacts;

    private Plea plea;

    private String modeOfTrial;

    private Integer convictingCourt;

    private LocalDate convictionDate;

    private String finalDisposal;

    private List<Result> resultDetails = new ArrayList<>();

    private String finding;

    @SuppressWarnings("squid:S00107")
    public Offence(UUID id, String offenceCode, Integer offenceSequenceNumber, String wording, Integer offenceDateCode, LocalDate startDate, LocalDate endDate, LocalDate arrestDate, LocalDate chargeDate, OffenceFacts offenceFacts, Plea plea, String modeOfTrial, Integer convictingCourt, LocalDate convictionDate, String finalDisposal, List<Result> resultDetails, String finding, final AllocationDecision allocationDecision ) {
        this.id = id;
        this.offenceCode = offenceCode;
        this.offenceSequenceNumber = offenceSequenceNumber;
        this.wording = wording;
        this.offenceDateCode = offenceDateCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.arrestDate = arrestDate;
        this.chargeDate = chargeDate;
        this.offenceFacts = offenceFacts;
        this.plea = plea;
        this.modeOfTrial = modeOfTrial;
        this.convictingCourt = convictingCourt;
        this.convictionDate = convictionDate;
        this.finalDisposal = finalDisposal;
        this.resultDetails = resultDetails;
        this.finding = finding;
        this.allocationDecision = allocationDecision ;
    }

    public UUID getId() {
        return id;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public Integer getOffenceSequenceNumber() {
        return offenceSequenceNumber;
    }

    public String getWording() {
        return wording;
    }

    public Integer getOffenceDateCode() {
        return offenceDateCode;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getArrestDate() {
        return arrestDate;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public OffenceFacts getOffenceFacts() {
        return offenceFacts;
    }

    public Plea getPlea() {
        return plea;
    }

    public String getModeOfTrial() {
        return modeOfTrial;
    }

    public Integer getConvictingCourt() {
        return convictingCourt;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public String getFinalDisposal() {
        return finalDisposal;
    }

    public List<Result> getResultDetails() {
        return resultDetails;
    }

    public AllocationDecision getAllocationDecision() { return allocationDecision; }

    public static Offence.Builder offence() {
        return new Builder();
    }

    @SuppressWarnings("squid:S3776")
    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        Offence offence = (Offence) o;

        if (id != null ? !id.equals(offence.id) : offence.id != null) {
            return false;
        }
        if (offenceCode != null ? !offenceCode.equals(offence.offenceCode) : offence.offenceCode != null) {
            return false;
        }
        if (offenceSequenceNumber != null ? !offenceSequenceNumber.equals(offence.offenceSequenceNumber) : offence.offenceSequenceNumber != null) {
            return false;
        }
        if (wording != null ? !wording.equals(offence.wording) : offence.wording != null) {
            return false;
        }
        if (offenceDateCode != null ? !offenceDateCode.equals(offence.offenceDateCode) : offence.offenceDateCode != null) {
            return false;
        }
        if (startDate != null ? !startDate.equals(offence.startDate) : offence.startDate != null) {
            return false;
        }
        if (endDate != null ? !endDate.equals(offence.endDate) : offence.endDate != null) {
            return false;
        }
        if (arrestDate != null ? !arrestDate.equals(offence.arrestDate) : offence.arrestDate != null) {
            return false;
        }
        if (chargeDate != null ? !chargeDate.equals(offence.chargeDate) : offence.chargeDate != null) {
            return false;
        }
        if (offenceFacts != null ? !offenceFacts.equals(offence.offenceFacts) : offence.offenceFacts != null) {
            return false;
        }
        if (plea != null ? !plea.equals(offence.plea) : offence.plea != null) {
            return false;
        }
        if (modeOfTrial != null ? !modeOfTrial.equals(offence.modeOfTrial) : offence.modeOfTrial != null) {
            return false;
        }
        if (convictingCourt != null ? !convictingCourt.equals(offence.convictingCourt) : offence.convictingCourt != null) {
            return false;
        }
        if (convictionDate != null ? !convictionDate.equals(offence.convictionDate) : offence.convictionDate != null) {
            return false;
        }
        if (finalDisposal != null ? !finalDisposal.equals(offence.finalDisposal) : offence.finalDisposal != null) {
            return false;
        }
        if (finding != null ? !finding.equals(offence.finding) : offence.finding != null) {
            return false;
        }
        if (allocationDecision != null ? !allocationDecision.equals(offence.allocationDecision) : offence.allocationDecision != null) {
            return false;
        }
        return resultDetails != null ? resultDetails.equals(offence.resultDetails) : offence.resultDetails == null;
    }

    @SuppressWarnings("squid:S3776")
    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (offenceCode != null ? offenceCode.hashCode() : 0);
        result = 31 * result + (offenceSequenceNumber != null ? offenceSequenceNumber.hashCode() : 0);
        result = 31 * result + (wording != null ? wording.hashCode() : 0);
        result = 31 * result + (offenceDateCode != null ? offenceDateCode.hashCode() : 0);
        result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
        result = 31 * result + (arrestDate != null ? arrestDate.hashCode() : 0);
        result = 31 * result + (chargeDate != null ? chargeDate.hashCode() : 0);
        result = 31 * result + (offenceFacts != null ? offenceFacts.hashCode() : 0);
        result = 31 * result + (plea != null ? plea.hashCode() : 0);
        result = 31 * result + (modeOfTrial != null ? modeOfTrial.hashCode() : 0);
        result = 31 * result + (convictingCourt != null ? convictingCourt.hashCode() : 0);
        result = 31 * result + (convictionDate != null ? convictionDate.hashCode() : 0);
        result = 31 * result + (finalDisposal != null ? finalDisposal.hashCode() : 0);
        result = 31 * result + (resultDetails != null ? resultDetails.hashCode() : 0);
        result = 31 * result + (finding != null ? finding.hashCode() : 0);
        result = 31 * result + (allocationDecision != null ? allocationDecision.hashCode() : 0);

        return result;
    }

    public static class Builder {
        private LocalDate arrestDate;

        private LocalDate chargeDate;

        private Integer convictingCourt;

        private LocalDate convictionDate;

        private LocalDate endDate;

        private String finalDisposal;

        private UUID id;

        private List<Result> results;

        private String modeOfTrial;

        private String offenceCode;

        private Integer offenceDateCode;

        private uk.gov.justice.core.courts.OffenceFacts offenceFacts;

        private Integer orderSequenceNumber;

        private uk.gov.justice.core.courts.Plea plea;

        private LocalDate startDate;

        private String wording;

        private String finding;

        private AllocationDecision allocationDecision ;

        public Offence.Builder withFinding(final String finding) {
            this.finding = finding;
            return this;
        }

        public Offence.Builder withArrestDate(final LocalDate arrestDate) {
            this.arrestDate = arrestDate;
            return this;
        }

        public Offence.Builder withChargeDate(final LocalDate chargeDate) {
            this.chargeDate = chargeDate;
            return this;
        }

        public Offence.Builder withConvictingCourt(final Integer convictingCourt) {
            this.convictingCourt = convictingCourt;
            return this;
        }

        public Offence.Builder withConvictionDate(final LocalDate convictionDate) {
            this.convictionDate = convictionDate;
            return this;
        }

        public Offence.Builder withEndDate(final LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Offence.Builder withFinalDisposal(final String finalDisposal) {
            this.finalDisposal = finalDisposal;
            return this;
        }

        public Offence.Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Offence.Builder withJudicialResults(final List<Result> results) {
            this.results = results;
            return this;
        }

        public Offence.Builder withModeOfTrial(final String modeOfTrial) {
            this.modeOfTrial = modeOfTrial;
            return this;
        }

        public Offence.Builder withOffenceCode(final String offenceCode) {
            this.offenceCode = offenceCode;
            return this;
        }

        public Offence.Builder withOffenceDateCode(final Integer offenceDateCode) {
            this.offenceDateCode = offenceDateCode;
            return this;
        }

        public Offence.Builder withOffenceFacts(final uk.gov.justice.core.courts.OffenceFacts offenceFacts) {
            this.offenceFacts = offenceFacts;
            return this;
        }

        public Offence.Builder withOrderSequenceNumber(final Integer orderSequenceNumber) {
            this.orderSequenceNumber = orderSequenceNumber;
            return this;
        }

        public Offence.Builder withPlea(final uk.gov.justice.core.courts.Plea plea) {
            this.plea = plea;
            return this;
        }

        public Offence.Builder withStartDate(final LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Offence.Builder withWording(final String wording) {
            this.wording = wording;
            return this;
        }

        public Offence.Builder withAllocationDecision(final AllocationDecision  allocationDecision) {
            this.allocationDecision = allocationDecision;
            return this;
        }

        public Offence build() {
            OffenceFacts offenceFactsResult = null;
            if(null != offenceFacts){
                offenceFactsResult =new OffenceFacts(offenceFacts.getAlcoholReadingAmount(), offenceFacts.getAlcoholReadingMethodCode(), offenceFacts.getVehicleCode(), offenceFacts.getVehicleRegistration());
            }
            Plea pleaResult = null;
            if(null != this.plea){
                pleaResult= new Plea(plea.getOffenceId(), plea.getPleaDate(), plea.getPleaValue(), plea.getOriginatingHearingId());
            }
            return new Offence(id,offenceCode,orderSequenceNumber,wording,offenceDateCode, startDate, endDate, arrestDate, chargeDate, offenceFactsResult, pleaResult, modeOfTrial, convictingCourt, convictionDate, finalDisposal, results, finding, allocationDecision);
        }
    }
}
