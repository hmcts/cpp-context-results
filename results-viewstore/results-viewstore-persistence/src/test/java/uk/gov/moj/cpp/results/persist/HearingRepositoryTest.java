package uk.gov.moj.cpp.results.persist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.results.persist.entity.Hearing;
import uk.gov.moj.cpp.results.persist.entity.HearingKey;
import uk.gov.moj.cpp.results.persist.entity.HearingResult;
import uk.gov.moj.cpp.results.persist.entity.HearingResultSummary;
import uk.gov.moj.cpp.results.persist.entity.Defendant;

@SuppressWarnings("CdiInjectionPointsInspection")
@RunWith(CdiTestRunner.class)
public class HearingRepositoryTest extends BaseTransactionalTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID PERSON_ID = randomUUID();
    private static final String[] hearingTypes = {"PTPH", "SENTENCE", "TRIAL"};
    private static final String HEARING_TYPE =
                    hearingTypes[new Random().nextInt(hearingTypes.length)];
    private static final LocalDate HEARING_START_DATE = PAST_LOCAL_DATE.next();
    private static final String COURT_CENTRE_NAME = STRING.next();
    private static final String COURT_CODE = INTEGER.next().toString();
    private static final String JUDGE_NAME = STRING.next();
    private static final String PROSECUTOR_NAME = STRING.next();
    private static final String DEFENCE_NAME = STRING.next();
    private static final String FIRST_NAME = STRING.next();
    private static final String LAST_NAME = STRING.next();
    private static final LocalDate DATE_OF_BIRTH = PAST_LOCAL_DATE.next();
    private static final String ADDRESS_1 = STRING.next();
    private static final String ADDRESS_2 = STRING.next();
    private static final String ADDRESS_3 = STRING.next();
    private static final String ADDRESS_4 = STRING.next();
    private static final String POST_CODE = RandomGenerator.POST_CODE.next();
    private static final LocalDate TWENTY_NINE_DAYS_AGO = LocalDate.now().minusDays(29);
    private static final LocalDate THIRTY_DAYS_AGO = LocalDate.now().minusDays(30);
    private static final LocalDate THIRTY_ONE_DAYS_AGO = LocalDate.now().minusDays(31);

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private DefendantRepository defendantRepository;


    @Test
    public void shouldPersistDetailsOfAHearing() {
        givenHearingDetailsDoesNotExist(HEARING_ID, PERSON_ID);

        hearingRepository.save(new Hearing(HEARING_ID, PERSON_ID, HEARING_TYPE, HEARING_START_DATE,
                COURT_CENTRE_NAME, COURT_CODE, JUDGE_NAME, PROSECUTOR_NAME, DEFENCE_NAME));

        final Hearing actualHearing = hearingRepository.findHearingByPersonIdAndHearingId(PERSON_ID, HEARING_ID);

        assertThat(actualHearing.getId(), is(HEARING_ID));
        assertThat(actualHearing.getPersonId(), is(PERSON_ID));
        assertThat(actualHearing.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualHearing.getCourtCode(), is(COURT_CODE));
        assertThat(actualHearing.getProsecutorName(), is(PROSECUTOR_NAME));
        assertThat(actualHearing.getDefenceName(), is(DEFENCE_NAME));
        assertThat(actualHearing.getStartDate(), is(HEARING_START_DATE));
        assertThat(actualHearing.getJudgeName(), is(JUDGE_NAME));
    }

    @Test
    public void shouldNotGetHearingDetailsWhenHearingResultsAreNotAvailable() {
        givenHearingDetailsDoesNotExist(HEARING_ID, PERSON_ID);

        final Hearing actualHearing = hearingRepository.findHearingByPersonIdAndHearingId(PERSON_ID, HEARING_ID);

        assertThat(actualHearing, is(nullValue()));
    }

    @Test
    public void shouldNotGetHearingDetailsWhenHearingResultsAreNotAvailableForThePerson() {
        givenHearingDetailsDoesNotExist(HEARING_ID, PERSON_ID);

        final UUID randomPerson = randomUUID();
        hearingRepository.save(new Hearing(HEARING_ID, randomPerson, HEARING_TYPE, HEARING_START_DATE,
                COURT_CENTRE_NAME, COURT_CODE, JUDGE_NAME, PROSECUTOR_NAME, DEFENCE_NAME));

        final Hearing actualHearing = hearingRepository.findHearingByPersonIdAndHearingId(PERSON_ID, HEARING_ID);

        assertThat(actualHearing, is(nullValue()));
    }

    @Test
    public void shouldGetAllHearingSummariesOnOrFromDate() {
        givenHearingDetailsStored(hearingWithStartDate(THIRTY_ONE_DAYS_AGO));
        givenHearingDetailsStored(hearingWithStartDate(THIRTY_DAYS_AGO));
        givenHearingDetailsStored(hearingWithStartDate(TWENTY_NINE_DAYS_AGO));

        final List<HearingResultSummary> hearingSummaries = hearingRepository.findHearingResultSummariesByFromDate(THIRTY_DAYS_AGO);

        hearingSummaries.sort(Comparator.comparing(HearingResultSummary::getHearingDate));

        assertThat(hearingSummaries, hasSize(2));
        assertThat(hearingSummaries.get(0).getHearingDate(), is(THIRTY_DAYS_AGO));
        assertThat(hearingSummaries.get(1).getHearingDate(), is(TWENTY_NINE_DAYS_AGO));
    }

    @Test
    public void noArgsConstructorShouldInstantiateAnObject() {
        assertThat(new HearingResult(), not(CoreMatchers.nullValue()));
    }

    private void givenHearingDetailsDoesNotExist(final UUID hearingId, final UUID personId) {
        MatcherAssert.assertThat(hearingRepository.findBy(new HearingKey(hearingId, personId)), is(nullValue()));
    }

    private void givenHearingDetailsStored(final Hearing hearing) {
        hearingRepository.save(hearing);
        defendantRepository.save(defendant(hearing.getPersonId(), hearing.getId()));
    }

    private Hearing hearingWithStartDate(final LocalDate startDate) {
        return new Hearing(randomUUID(), randomUUID(), HEARING_TYPE, startDate,
                COURT_CENTRE_NAME, COURT_CODE, JUDGE_NAME, PROSECUTOR_NAME, DEFENCE_NAME);
    }

    private Defendant defendant(final UUID personId, final UUID hearingId) {
        return Defendant.builder().withId(personId).withHearingId(hearingId).withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME).withDateOfBirth(DATE_OF_BIRTH).withAddress1(ADDRESS_1).withAddress2(ADDRESS_2)
                .withAddress3(ADDRESS_3).withAddress4(ADDRESS_4).withPostCode(POST_CODE).build();
    }

}