package uk.gov.moj.cpp.results.event.service;

import java.time.LocalDate;
import java.util.Objects;

public class DefendantPerson {

    private String forename;
    private String middleName;
    private String surname;
    private LocalDate dateOfBirth;

    public String getForename() {
        return forename;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getSurname() {
        return surname;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DefendantPerson that = (DefendantPerson) o;
        return Objects.equals(forename, that.forename) && Objects.equals(middleName, that.middleName) && Objects.equals(surname, that.surname) && Objects.equals(dateOfBirth, that.dateOfBirth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forename, middleName, surname, dateOfBirth);
    }

    public static final class DefendantPersonBuilder {
        private String forename;
        private String middleName;
        private String surname;
        private LocalDate dateOfBirth;

        private DefendantPersonBuilder() {
        }

        public static DefendantPersonBuilder aDefendantPerson() {
            return new DefendantPersonBuilder();
        }

        public DefendantPersonBuilder withForename(String forename) {
            this.forename = forename;
            return this;
        }

        public DefendantPersonBuilder withMiddleName(String middleName) {
            this.middleName = middleName;
            return this;
        }

        public DefendantPersonBuilder withSurname(String surname) {
            this.surname = surname;
            return this;
        }

        public DefendantPersonBuilder withDateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public DefendantPerson build() {
            DefendantPerson defendantPerson = new DefendantPerson();
            defendantPerson.forename = this.forename;
            defendantPerson.dateOfBirth = this.dateOfBirth;
            defendantPerson.middleName = this.middleName;
            defendantPerson.surname = this.surname;
            return defendantPerson;
        }
    }
}
