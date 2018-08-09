package uk.gov.moj.cpp.results.it.steps.data.hearing;

import static java.lang.String.format;

import uk.gov.moj.cpp.results.it.steps.data.people.Person;

import java.util.UUID;

public class ProsecutionCounsel {

    private final UUID attendeeId;
    private final Person person;
    private final String status;

    public ProsecutionCounsel(final UUID attendeeId, final Person person, final String status) {
        this.attendeeId = attendeeId;
        this.person = person;
        this.status = status;
    }

    public UUID getAttendeeId() {
        return attendeeId;
    }

    public UUID getPersonId() {
        return person.getPersonId();
    }

    public String getStatus() {
        return status;
    }

    public Person getPerson() {
        return person;
    }

    public String getName() {
        return format("%s %s %s", person.getFirstName(), person.getLastName(), status);
    }
}
