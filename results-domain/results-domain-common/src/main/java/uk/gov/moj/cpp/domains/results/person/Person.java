package uk.gov.moj.cpp.domains.results.person;

import java.time.LocalDate;
import java.util.UUID;

public class Person {

    private final UUID id;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final Address address;

    public Person(final UUID id, final String firstName, final String lastName, final LocalDate dateOfBirth,
                  final Address address) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }

    public UUID getId() {
        return id;
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

    public Address getAddress() {
        return address;
    }
}
