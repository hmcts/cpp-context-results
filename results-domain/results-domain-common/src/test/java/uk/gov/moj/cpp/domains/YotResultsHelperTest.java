package uk.gov.moj.cpp.domains;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static uk.gov.moj.cpp.domains.YotResultsHelper.yotResultsRequestId;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class YotResultsHelperTest {

    private static final String HEARING_ID = "0aa5bf35-1e51-45e5-9e42-64bd57e15c11";
    private static final String HEARING_DAY = "2026-08-19";
    private static final String SHARED_TIME = "2026-08-19T16:42:07.512Z";

    /**
     * Pinned rather than re-derived from the recipe: the consuming service dedupes on this id across
     * deployments, so a recipe change would mint new ids for every already-published share.
     */
    @Test
    public void yotResultsRequestId_forAKnownShare_should_mintThePinnedId() {
        assertThat(yotResultsRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME),
                is(UUID.fromString("52ba4dc9-fd12-3c7e-98d6-34da80ce7be8")));
    }

    @Test
    public void yotResultsRequestId_forTheSameShareTwice_should_mintTheSameId() {
        assertThat(yotResultsRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME),
                is(yotResultsRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME)));
    }

    @Test
    public void yotResultsRequestId_forAReshare_should_mintANewId() {
        final UUID reshareRequestId = yotResultsRequestId(HEARING_ID, HEARING_DAY, "2026-08-19T19:30:00.000Z");

        assertThat(reshareRequestId, is(UUID.fromString("8962f9db-4ad8-32bf-9bc0-6c9954dd9d63")));
        assertThat(reshareRequestId, is(not(yotResultsRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME))));
    }

    @Test
    public void yotResultsRequestId_forADifferentHearingDay_should_mintANewId() {
        assertThat(yotResultsRequestId(HEARING_ID, "2026-08-20", SHARED_TIME),
                is(not(yotResultsRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME))));
    }

    @Test
    public void yotResultsRequestId_forADifferentHearing_should_mintANewId() {
        assertThat(yotResultsRequestId("11111111-2222-3333-4444-555555555555", HEARING_DAY, SHARED_TIME),
                is(not(yotResultsRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME))));
    }

    @Test
    public void yotResultsRequestId_forAmbiguouslySplittableParts_should_notCollide() {
        assertThat(yotResultsRequestId(HEARING_ID, "2026-08-1", "92026-08-19T16:42:07.512Z"),
                is(not(yotResultsRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME))));
    }
}
