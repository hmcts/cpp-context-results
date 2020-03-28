package uk.gov.moj.cpp.domains.resultStructure;

import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.Address;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public class Person implements Serializable {

    private long serialVersionUID = -9176890205806560222L;
    private String title;

    private UUID personId;

    private String firstName;

    private String lastName;

    private Gender gender;

    private LocalDate dateOfBirth;

    private ContactNumber contact;

    private Address address;

    private String middleName;

    private String nationality;

    public String getTitle() {
        return title;
    }

    public UUID getPersonId() {
        return personId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public ContactNumber getContact() {
        return contact;
    }

    public Address getAddress() {
        return address;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getNationality() {
        return nationality;
    }

    @SuppressWarnings("squid:S00107")
    private Person(String title, String firstName, String lastName, Gender gender, LocalDate dateOfBirth, ContactNumber contact, Address address, String middleName, String nationality) {
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.contact = contact;
        this.address = address;
        this.middleName = middleName;
        this.nationality = nationality;
    }
    public static Builder person() {
        return new Builder();
    }

    @SuppressWarnings("squid:S3776")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Person person = (Person) o;

        if (title != person.title) {
            return false;
        }
        if (personId != null ? !personId.equals(person.personId) : person.personId != null) {
            return false;
        }
        if (firstName != null ? !firstName.equals(person.firstName) : person.firstName != null) {
            return false;
        }
        if (lastName != null ? !lastName.equals(person.lastName) : person.lastName != null) {
            return false;
        }
        if (middleName != null ? !middleName.equals(person.middleName) : person.middleName != null) {
            return false;
        }
        if (nationality != null ? !nationality.equals(person.nationality) : person.nationality != null) {
            return false;
        }
        if (gender != person.gender) {
            return false;
        }
        if (dateOfBirth != null ? !dateOfBirth.equals(person.dateOfBirth) : person.dateOfBirth != null) {
            return false;
        }
        if (contact != null ? !contact.equals(person.contact) : person.contact != null) {
            return false;
        }
        return address != null ? address.equals(person.address) : person.address == null;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (personId != null ? personId.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (middleName != null ? middleName.hashCode() : 0);
        result = 31 * result + (nationality != null ? nationality.hashCode() : 0);
        result = 31 * result + (gender != null ? gender.hashCode() : 0);
        result = 31 * result + (dateOfBirth != null ? dateOfBirth.hashCode() : 0);
        result = 31 * result + (contact != null ? contact.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private uk.gov.justice.core.courts.Address address;

        private uk.gov.justice.core.courts.ContactNumber contact;

        private LocalDate dateOfBirth;

        private String firstName;

        private Gender gender;

        private String lastName;

        private String middleName;

        private String nationality;

        private String title;

        public Person.Builder withAddress(final uk.gov.justice.core.courts.Address address) {
            this.address = address;
            return this;
        }

        public Person.Builder withContact(final uk.gov.justice.core.courts.ContactNumber contact) {
            this.contact = contact;
            return this;
        }

        public Person.Builder withDateOfBirth(final LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Person.Builder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Person.Builder withGender(final Gender gender) {
            this.gender = gender;
            return this;
        }

        public Person.Builder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Person.Builder withMiddleName(final String middleName) {
            this.middleName = middleName;
            return this;
        }


        public Person.Builder withNationality(final String  nationality) {
            this.nationality = nationality;
            return this;
        }

        public Person.Builder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public Person build() {
            return new Person(title,firstName,lastName, gender, dateOfBirth, contact, address, middleName, nationality);
        }
    }
}


