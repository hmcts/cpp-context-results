package uk.gov.moj.cpp.results.query.view.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class DefendantView {

    private final UUID personId;
    private final String firstName;
    private final String lastName;

    public DefendantView(
            @JsonProperty(value = "personId", required = true) final UUID personId,
            @JsonProperty(value = "firstName", required = true) final String firstName,
            @JsonProperty(value = "lastName", required = true) final String lastName) {
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
