package uk.gov.moj.cpp.domains.results.shareresults;

import java.util.UUID;

public class CourtClerk {

    private UUID id;

    private String firstName;

    private String lastName;

    public static CourtClerk courtClerk() {
        return new CourtClerk();
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

    public CourtClerk setId(UUID id) {
        this.id = id;
        return this;
    }

    public CourtClerk setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public CourtClerk setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }
}
