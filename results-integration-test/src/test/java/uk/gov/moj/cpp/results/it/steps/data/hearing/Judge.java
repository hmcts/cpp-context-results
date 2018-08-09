package uk.gov.moj.cpp.results.it.steps.data.hearing;

import java.util.UUID;

public class Judge {
    private final UUID id;
    private final String title;
    private final String firstName;
    private final String lastName;

    public Judge(final UUID id, final String title, final String firstName, final String lastName) {
        this.id = id;
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
