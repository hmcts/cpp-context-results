package uk.gov.moj.cpp.domains.resultStructure;

import uk.gov.justice.core.courts.AssociatedIndividual;
import uk.gov.justice.core.courts.BailStatus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Defendant implements Serializable {

    private static final long serialVersionUID = -9176890205806560222L;
    private final UUID id;
    @SuppressWarnings("squid:S1948")
    protected List<Offence> offences = new ArrayList<>();
    private String prosecutorReference; //sjp send urn
    private Person person;
    private CorporateDefendant corporateDefendant;
    private BailStatus bailStatus;
    private String bailCondition;
    private String reasonForBailConditionsOrCustody;
    private String presentAtHearing;
    private List<AssociatedIndividual> associatedIndividuals;
    private String pncId;
    private List<Result> results = new ArrayList<>();

    private List<AttendanceDay> attendanceDays = new ArrayList<>();

    public Defendant(final UUID id) {
        this.id = id;
    }

    public String getPresentAtHearing() {
        return presentAtHearing;
    }

    public void setPresentAtHearing(final String presentAtHearing) {
        this.presentAtHearing = presentAtHearing;
    }

    public UUID getId() {
        return id;
    }

    public String getProsecutorReference() {
        return prosecutorReference;
    }

    public void setProsecutorReference(final String prosecutorReference) {
        this.prosecutorReference = prosecutorReference;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(final Person person) {
        this.person = person;
    }

    public CorporateDefendant getCorporateDefendant() {
        return corporateDefendant;
    }

    public void setCorporateDefendant(final CorporateDefendant corporateDefendant) {
        this.corporateDefendant = corporateDefendant;
    }

    public BailStatus getBailStatus() {
        return bailStatus;
    }

    public void setBailStatus(final BailStatus bailStatus) {
        this.bailStatus = bailStatus;
    }

    public String getReasonForBailConditionsOrCustody() {
        return reasonForBailConditionsOrCustody;
    }

    public void setReasonForBailConditionsOrCustody(final String reasonForBailConditionsOrCustody) {
        this.reasonForBailConditionsOrCustody = reasonForBailConditionsOrCustody;
    }

    public List<AssociatedIndividual> getAssociatedIndividuals() {
        return associatedIndividuals;
    }

    public void setAssociatedIndividuals(final List<AssociatedIndividual> associatedIndividuals) {
        this.associatedIndividuals = associatedIndividuals;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    public void setOffences(final List<Offence> offences) {
        this.offences = offences;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(final List<Result> results) {
        this.results = results;
    }

    public String getBailCondition() {
        return bailCondition;
    }

    public void setBailCondition(final String bailCondition) {
        this.bailCondition = bailCondition;
    }

    public List<AttendanceDay> getAttendanceDays() {
        return attendanceDays;
    }

    public void setAttendanceDays(final List<AttendanceDay> attendanceDays) {
        this.attendanceDays = attendanceDays;
    }

    public String getPncId() {
        return pncId;
    }

    public void setPncId(final String pncId) {
        this.pncId = pncId;
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

        final Defendant defendant = (Defendant) o;

        if (id != null ? !id.equals(defendant.id) : defendant.id != null) {
            return false;
        }
        if (prosecutorReference != null ? !prosecutorReference.equals(defendant.prosecutorReference) : defendant.prosecutorReference != null) {
            return false;
        }
        if (person != null ? !person.equals(defendant.person) : defendant.person != null) {
            return false;
        }
        if (corporateDefendant != null ? !corporateDefendant.equals(defendant.corporateDefendant) : defendant.corporateDefendant != null) {
            return false;
        }
        if (bailStatus != null ? !bailStatus.equals(defendant.bailStatus) : defendant.bailStatus != null) {
            return false;
        }
        if (bailCondition != null ? !bailCondition.equals(defendant.bailCondition) : defendant.bailCondition != null) {
            return false;
        }
        if (reasonForBailConditionsOrCustody != null ? !reasonForBailConditionsOrCustody.equals(defendant.reasonForBailConditionsOrCustody) : defendant.reasonForBailConditionsOrCustody != null) {
            return false;
        }
        if (associatedIndividuals != null ? !associatedIndividuals.equals(defendant.associatedIndividuals) : defendant.associatedIndividuals != null) {
            return false;
        }
        if (offences != null ? !offences.equals(defendant.offences) : defendant.offences != null) {
            return false;
        }
        if (results != null ? !results.equals(defendant.results) : defendant.results != null) {
            return false;
        }
        if (pncId != null ? !pncId.equals(defendant.pncId) : defendant.pncId != null) {
            return false;
        }
        if (presentAtHearing != null ? !presentAtHearing.equals(defendant.presentAtHearing) : defendant.presentAtHearing != null) {
            return false;
        }
        return attendanceDays != null ? attendanceDays.equals(defendant.attendanceDays) : defendant.attendanceDays == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (prosecutorReference != null ? prosecutorReference.hashCode() : 0);
        result = 31 * result + (person != null ? person.hashCode() : 0);
        result = 31 * result + (corporateDefendant != null ? corporateDefendant.hashCode() : 0);
        result = 31 * result + (bailStatus != null ? bailStatus.hashCode() : 0);
        result = 31 * result + (bailCondition != null ? bailCondition.hashCode() : 0);
        result = 31 * result + (reasonForBailConditionsOrCustody != null ? reasonForBailConditionsOrCustody.hashCode() : 0);
        result = 31 * result + (associatedIndividuals != null ? associatedIndividuals.hashCode() : 0);
        result = 31 * result + (offences != null ? offences.hashCode() : 0);
        result = 31 * result + (results != null ? results.hashCode() : 0);
        result = 31 * result + (attendanceDays != null ? attendanceDays.hashCode() : 0);
        result = 31 * result + (pncId != null ? pncId.hashCode() : 0);
        result = 31 * result + (presentAtHearing != null ? presentAtHearing.hashCode() : 0);
        return result;
    }
}
