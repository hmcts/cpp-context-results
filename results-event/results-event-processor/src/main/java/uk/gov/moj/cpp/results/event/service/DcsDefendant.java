package uk.gov.moj.cpp.results.event.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DcsDefendant {

    private UUID id;
    private String bailStatus;
    private String interpreterLanguage;
    private String interpreterInformation;
    private DefendantPerson defendantPerson;
    private DefendantOrganisation defendantOrganisation;
    private OffenceDetails offencesDetails;
    private List<DcsHearing> hearings;

    public UUID getId() {
        return id;
    }

    public String getBailStatus() {
        return bailStatus;
    }

    public String getInterpreterLanguage() {
        return interpreterLanguage;
    }

    public DefendantPerson getDefendantPerson() {
        return defendantPerson;
    }

    public DefendantOrganisation getDefendantOrganisation() {
        return defendantOrganisation;
    }

    public OffenceDetails getOffencesDetails() {
        return offencesDetails;
    }

    public String getInterpreterInformation() {
        return interpreterInformation;
    }

    public List<DcsHearing> getHearings() {
        return hearings;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DcsDefendant defendant = (DcsDefendant) o;
        return Objects.equals(id, defendant.id) && Objects.equals(bailStatus, defendant.bailStatus) && Objects.equals(interpreterLanguage, defendant.interpreterLanguage) && Objects.equals(defendantPerson, defendant.defendantPerson) && Objects.equals(defendantOrganisation, defendant.defendantOrganisation) && Objects.equals(offencesDetails, defendant.offencesDetails) && Objects.equals(interpreterInformation, defendant.interpreterInformation) && Objects.equals(hearings, defendant.hearings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bailStatus, interpreterLanguage, interpreterInformation, defendantPerson, defendantOrganisation, offencesDetails, hearings);
    }

    public static final class DcsDefendantBuilder {
        private UUID id;
        private String bailStatus;
        private String interpreterLanguage;
        private String interpreterInformation;
        private DefendantPerson defendantPerson;
        private DefendantOrganisation defendantOrganisation;
        private OffenceDetails offenceDetails;
        private List<DcsHearing> hearings;

        private DcsDefendantBuilder() {
        }

        public static DcsDefendantBuilder aDcsDefendant() {
            return new DcsDefendantBuilder();
        }

        public DcsDefendantBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        public DcsDefendantBuilder withBailStatus(String bailStatus) {
            this.bailStatus = bailStatus;
            return this;
        }

        public DcsDefendantBuilder withInterpreterLanguage(String interpreterLanguage) {
            this.interpreterLanguage = interpreterLanguage;
            return this;
        }

        public DcsDefendantBuilder withInterpreterInformation(String interpreterInformation) {
            this.interpreterInformation = interpreterInformation;
            return this;
        }

        public DcsDefendantBuilder withDefendantPerson(DefendantPerson defendantPerson) {
            this.defendantPerson = defendantPerson;
            return this;
        }

        public DcsDefendantBuilder withDefendantOrganisation(DefendantOrganisation defendantOrganisation) {
            this.defendantOrganisation = defendantOrganisation;
            return this;
        }
        public DcsDefendantBuilder withOffenceDetails(OffenceDetails offenceDetails) {
            this.offenceDetails = offenceDetails;
            return this;
        }

        public DcsDefendantBuilder withHearings(List<DcsHearing> hearings) {
            this.hearings = hearings;
            return this;
        }

        public DcsDefendant build() {
            DcsDefendant dcsDefendant = new DcsDefendant();
            dcsDefendant.offencesDetails = this.offenceDetails;
            dcsDefendant.defendantPerson = this.defendantPerson;
            dcsDefendant.defendantOrganisation = this.defendantOrganisation;
            dcsDefendant.interpreterLanguage = this.interpreterLanguage;
            dcsDefendant.interpreterInformation = this.interpreterInformation;
            dcsDefendant.bailStatus = this.bailStatus;
            dcsDefendant.id = this.id;
            dcsDefendant.hearings = this.hearings;
            return dcsDefendant;
        }
    }
}
