package uk.gov.moj.cpp.results.it.steps.data.factory;

import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.POST_CODE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.results.it.utils.RandomPersonNameGenerator.firstName;
import static uk.gov.moj.cpp.results.it.utils.RandomPersonNameGenerator.lastName;

import uk.gov.moj.cpp.results.it.steps.data.people.Person;

import java.util.UUID;

public class PeopleDataFactory {

    public static Person personDetails(final UUID personId) {
        return new Person(personId, firstName(), lastName(), PAST_LOCAL_DATE.next(), STRING.next(),
                STRING.next(), STRING.next(), STRING.next(), POST_CODE.next());
    }

    public static Person personDetailsWithRequiredFieldsOnly(final UUID personId) {
        return new Person(personId, firstName(), lastName(), null, STRING.next(),
                null, null, null, null);
    }
}
