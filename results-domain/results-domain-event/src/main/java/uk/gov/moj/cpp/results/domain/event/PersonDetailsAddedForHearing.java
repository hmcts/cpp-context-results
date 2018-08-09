package uk.gov.moj.cpp.results.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.domains.results.person.Address;

import java.time.LocalDate;
import java.util.UUID;

@Event("results.person-details-added-for-hearing")
public class PersonDetailsAddedForHearing {

    private final UUID personId;
    private final UUID hearingId;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;

    private final Address address;

    public PersonDetailsAddedForHearing(final UUID personId, final UUID hearingId, final String firstName,
                                        final String lastName, final LocalDate dateOfBirth, final Address address) {
        this.personId = personId;
        this.hearingId = hearingId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }

    public UUID getPersonId() {
        return personId;
    }

    public UUID getHearingId() {
        return hearingId;
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
