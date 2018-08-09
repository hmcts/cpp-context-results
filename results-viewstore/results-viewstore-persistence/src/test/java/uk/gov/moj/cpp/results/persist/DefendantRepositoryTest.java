package uk.gov.moj.cpp.results.persist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.results.persist.entity.Defendant;
import uk.gov.moj.cpp.results.persist.entity.PersonKey;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("CdiInjectionPointsInspection")
@RunWith(CdiTestRunner.class)
public class DefendantRepositoryTest extends BaseTransactionalTest {

    private static final UUID PERSON_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();

    private static final String FIRST_NAME = STRING.next();
    private static final String LAST_NAME = STRING.next();
    private static final String ADDRESS_1 = STRING.next();
    private static final String ADDRESS_2 = STRING.next();
    private static final String ADDRESS_3 = STRING.next();
    private static final String ADDRESS_4 = STRING.next();
    private static final String POST_CODE = RandomGenerator.POST_CODE.next();
    private static final LocalDate DATE_OF_BIRTH = PAST_LOCAL_DATE.next();

    @Inject
    private DefendantRepository defendantRepository;

    @Test
    public void shouldGetPersonOnlyIfHearingResultsAreAvailableForThePerson() {
        givenHearingResultsAreSharedForPersonAndHearing(PERSON_ID, HEARING_ID);

        final Defendant actualPerson = defendantRepository.findPersonByPersonIdAndHearingId(PERSON_ID, HEARING_ID);

        assertThat(actualPerson, is(notNullValue()));
        assertThat(actualPerson.getId(), is(PERSON_ID));
        assertThat(actualPerson.getFirstName(), is(FIRST_NAME));
        assertThat(actualPerson.getLastName(), is(LAST_NAME));
        assertThat(actualPerson.getDateOfBirth(), is(DATE_OF_BIRTH));
        assertThat(actualPerson.getAddress1(), is(ADDRESS_1));
        assertThat(actualPerson.getAddress2(), is(ADDRESS_2));
        assertThat(actualPerson.getAddress3(), is(ADDRESS_3));
        assertThat(actualPerson.getAddress4(), is(ADDRESS_4));
        assertThat(actualPerson.getPostCode(), is(POST_CODE));
    }

    @Test
    public void shouldNotGetPersonWhenHearingResultsAreNotAvailableForThePerson() {
        givenNoHearingResultsAreSharedForPerson(PERSON_ID, HEARING_ID);

        final Defendant actualPerson = defendantRepository.findPersonByPersonIdAndHearingId(PERSON_ID, HEARING_ID);

        assertThat(actualPerson, is(nullValue()));
    }

    private void givenHearingResultsAreSharedForPersonAndHearing(final UUID personId, final UUID hearingId) {
        defendantRepository.save(Defendant.builder().withId(personId).withHearingId(hearingId).withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME).withDateOfBirth(DATE_OF_BIRTH).withAddress1(ADDRESS_1).withAddress2(ADDRESS_2)
                .withAddress3(ADDRESS_3).withAddress4(ADDRESS_4).withPostCode(POST_CODE).build());

        final Defendant defendant = defendantRepository.findBy(new PersonKey(personId, hearingId));
        assertThat(defendant, is(notNullValue()));
        assertThat(defendant.getId(), is(personId));
        assertThat(defendant.getHearingId(), is(hearingId));
    }

    private void givenNoHearingResultsAreSharedForPerson(final UUID personId, final UUID hearingId) {
        final UUID randomPerson = randomUUID();
        givenHearingResultsAreSharedForPersonAndHearing(randomPerson, hearingId);

        final Defendant defendant = defendantRepository.findBy(new PersonKey(personId, hearingId));
        assertThat(defendant, is(nullValue()));
    }

}