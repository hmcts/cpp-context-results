package uk.gov.moj.cpp.results.event.service;

import java.util.Objects;

public class DefendantOrganisation {
    private String name;

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DefendantOrganisation that = (DefendantOrganisation) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public static final class DefendantOrganisationBuilder {
        private String name;

        private DefendantOrganisationBuilder() {
        }

        public static DefendantOrganisationBuilder aDefendantOrganisation() {
            return new DefendantOrganisationBuilder();
        }

        public DefendantOrganisationBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public DefendantOrganisation build() {
            DefendantOrganisation defendantOrganisation = new DefendantOrganisation();
            defendantOrganisation.name = this.name;
            return defendantOrganisation;
        }
    }
}
