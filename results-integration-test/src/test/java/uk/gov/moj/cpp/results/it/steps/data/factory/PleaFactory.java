package uk.gov.moj.cpp.results.it.steps.data.factory;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.moj.cpp.results.it.steps.data.hearing.Plea;

public class PleaFactory {

    public static Plea preparePlea() {
        return new Plea(randomUUID(), STRING.next(), PAST_LOCAL_DATE.next());
    }
}
