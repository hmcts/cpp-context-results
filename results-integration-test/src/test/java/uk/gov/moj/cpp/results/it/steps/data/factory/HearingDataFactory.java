package uk.gov.moj.cpp.results.it.steps.data.factory;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalTime.now;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.moj.cpp.results.it.steps.data.hearing.DefenceCounsel;
import uk.gov.moj.cpp.results.it.steps.data.hearing.Hearing;
import uk.gov.moj.cpp.results.it.steps.data.hearing.Judge;
import uk.gov.moj.cpp.results.it.steps.data.hearing.ProsecutionCounsel;

import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

public class HearingDataFactory {

    private static final String[] hearingTypes = {"PTPH", "SENTENCE", "TRIAL"};
    private static final String HEARING_TYPE = hearingTypes[new Random().nextInt(hearingTypes.length)];

    public static Hearing hearingDetails(final UUID hearingId) {
        return hearingDetails(hearingId, PAST_LOCAL_DATE.next());
    }

    public static Hearing hearingDetails(final UUID hearingId, final LocalDate fromDate) {
        final Judge judge = new Judge(randomUUID(), STRING.next(), STRING.next(), STRING.next());
        return new Hearing(hearingId, fromDate, now(), HEARING_TYPE, INTEGER.next(), STRING.next(), judge);
    }

    public static ProsecutionCounsel prosecutionCounsel() {
        return new ProsecutionCounsel(randomUUID(), PeopleDataFactory.personDetails(randomUUID()), "QC");
    }

    public static DefenceCounsel defenceCounselFor(final UUID personId) {
        return new DefenceCounsel(randomUUID(), PeopleDataFactory.personDetails(randomUUID()), "QC", newArrayList(personId));
    }
}
