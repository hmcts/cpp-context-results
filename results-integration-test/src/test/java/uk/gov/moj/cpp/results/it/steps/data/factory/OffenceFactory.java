package uk.gov.moj.cpp.results.it.steps.data.factory;

import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.results.it.steps.data.factory.PleaFactory.preparePlea;

import uk.gov.moj.cpp.results.it.steps.data.HearingResult;
import uk.gov.moj.cpp.results.it.steps.data.ResultLine;
import uk.gov.moj.cpp.results.it.steps.data.hearing.Offence;
import uk.gov.moj.cpp.results.it.steps.data.hearing.Plea;

import java.time.LocalDate;
import java.util.UUID;

public class OffenceFactory {

    private static final String OFFENCE_TITLE = STRING.next();
    private static final Plea PLEA = preparePlea();

    private static final LocalDate START_DATE = PAST_LOCAL_DATE.next();
    private static final LocalDate END_DATE = PAST_LOCAL_DATE.next().plusWeeks(2);

    public static Offence prepareOffence(final UUID offenceId) {
        return new Offence(offenceId, OFFENCE_TITLE, START_DATE, END_DATE, PLEA);
    }

    public static Offence prepareOffenceWithNoPleas(final UUID offenceId) {
        return new Offence(offenceId, OFFENCE_TITLE, START_DATE, END_DATE, null);
    }

    public static Offence prepareOffence(final HearingResult hearingResults) {
        final UUID offenceId = hearingResults.getResultLines().stream().map(ResultLine::getOffenceId).findFirst().get();
        return new Offence(offenceId, OFFENCE_TITLE, START_DATE, END_DATE, PLEA);
    }
}
