package uk.gov.moj.cpp.results.it.steps.data.hearing;

import uk.gov.moj.cpp.results.it.steps.data.people.Person;

import java.util.List;
import java.util.UUID;

public class DefenceCounsel extends ProsecutionCounsel {

    private final List<UUID> personIds;

    public DefenceCounsel(final UUID attendeeId, final Person person, final String status, final List<UUID> personIds) {
        super(attendeeId, person, status);
        this.personIds = personIds;
    }

    public List<UUID> getPersonIds() {
        return personIds;
    }
}
