package uk.gov.moj.cpp.results.query.view.response;

import java.util.UUID;

public class DefendantView {

    private final UUID personId;
    private final String firstName;
    private final String lastName;

    public DefendantView(final UUID personId, final String firstName, final String lastName) {
        this.personId = personId;
        this.firstName = firstName;
        this.lastName = lastName;
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

    @Override
    public String toString() {
        return "DefendantView{" +
                "personId='" + personId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName=" + lastName +
                '}';
    }
}
