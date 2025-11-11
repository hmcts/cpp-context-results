package uk.gov.moj.cpp.results.it.steps.data.factory;

import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.UUID;

import java.util.UUID;

public class HearingResultDataFactory {

    private static final UUID USER_ID = UUID.next();

    public static UUID getUserId() {
        return USER_ID;
    }

}
