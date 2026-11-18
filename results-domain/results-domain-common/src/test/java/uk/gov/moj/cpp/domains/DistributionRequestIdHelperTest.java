package uk.gov.moj.cpp.domains;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static uk.gov.moj.cpp.domains.DistributionRequestIdHelper.distributionRequestId;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class DistributionRequestIdHelperTest {

    private static final String HEARING_ID = "0aa5bf35-1e51-45e5-9e42-64bd57e15c11";
    private static final String HEARING_DAY = "2026-08-19";
    private static final String SHARED_TIME = "2026-08-19T16:42:07.512Z";

    /**
     * The expected value is pinned rather than re-derived from the recipe, because every consuming
     * service dedupes on this id across deployments: if the recipe ever changes, every
     * already-published share mints a new id and gets processed a second time. A test that
     * recomputed the recipe would follow such a change silently; this one fails.
     *
     * <p>This is the cross-repo vector. The informant register, YOT results and probation results
     * distribution services each re-implement the recipe (the probation one as
     * {@code RequestIdRecipe}) and pin it with exactly this input and output, with no build
     * dependency on a results artefact. Changing it here is a cross-team contract change.
     */
    @Test
    public void distributionRequestId_forTheCrossRepoVector_should_mintThePinnedId() {
        final UUID requestId = distributionRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME);

        assertThat(requestId, is(UUID.fromString("e74ed270-0b62-36d9-bb10-12bd6baa5bea")));
    }

    @Test
    public void distributionRequestId_should_beANameBasedVersion3Uuid() {
        final UUID requestId = distributionRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME);

        assertThat(requestId.version(), is(3));
        assertThat(requestId.variant(), is(2));
    }

    @Test
    public void distributionRequestId_forTheSameShareTwice_should_mintTheSameId() {
        assertThat(distributionRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME),
                is(distributionRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME)));
    }

    /**
     * A re-share is a legitimate business event carrying a new sharedTime, and must reach the
     * consuming service rather than being swallowed as a duplicate of the first share.
     */
    @Test
    public void distributionRequestId_forAReshare_should_mintANewId() {
        final UUID reshareRequestId =
                distributionRequestId(HEARING_ID, HEARING_DAY, "2026-08-19T19:30:00.000Z");

        assertThat(reshareRequestId, is(UUID.fromString("32475c57-26ec-31e2-9349-a2bc6e1f8411")));
        assertThat(reshareRequestId, is(not(distributionRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME))));
    }

    @Test
    public void distributionRequestId_forADifferentHearingDay_should_mintANewId() {
        assertThat(distributionRequestId(HEARING_ID, "2026-08-20", SHARED_TIME),
                is(not(distributionRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME))));
    }

    @Test
    public void distributionRequestId_forADifferentHearing_should_mintANewId() {
        assertThat(distributionRequestId("11111111-2222-3333-4444-555555555555", HEARING_DAY, SHARED_TIME),
                is(not(distributionRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME))));
    }

    /**
     * The id is a hash of the raw wire strings, so two renderings of the same instant are two
     * different shares as far as the recipe is concerned. Pinned so nobody "normalises" the inputs:
     * the consumers hash the raw payload strings too, and would stop agreeing with this side.
     */
    @Test
    public void distributionRequestId_forADifferentRenderingOfTheSameInstant_should_mintANewId() {
        assertThat(distributionRequestId(HEARING_ID, HEARING_DAY, "2026-08-19T16:42:07.5120Z"),
                is(not(distributionRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME))));
    }

    /**
     * The separator has to be part of the hash, not just cosmetic: concatenated without one,
     * ("2026-08-1" + "92026-...") and ("2026-08-19" + "2026-...") would produce the same bytes, so
     * two different shares could collide onto one requestId.
     */
    @Test
    public void distributionRequestId_forAmbiguouslySplittableParts_should_notCollide() {
        assertThat(distributionRequestId(HEARING_ID, "2026-08-1", "92026-08-19T16:42:07.512Z"),
                is(not(distributionRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME))));
    }
}
