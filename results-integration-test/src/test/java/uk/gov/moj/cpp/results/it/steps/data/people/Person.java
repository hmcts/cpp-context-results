package uk.gov.moj.cpp.results.it.steps.data.people;

import java.time.LocalDate;
import java.util.UUID;

public class Person {

    private final UUID personId;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final String address1;
    private final String address2;
    private final String address3;
    private final String address4;
    private final String postCode;

    public Person(final UUID personId, final String firstName, final String lastName, final LocalDate dateOfBirth,
                  final String address1, final String address2, final String address3, final String address4,
                  final String postCode) {
        this.personId = personId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.postCode = postCode;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getAddress3() {
        return address3;
    }

    public String getAddress4() {
        return address4;
    }

    public String getPostCode() {
        return postCode;
    }
}
