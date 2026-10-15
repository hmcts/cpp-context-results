package uk.gov.moj.cpp.domains;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static uk.gov.moj.cpp.domains.InformantRegisterHelper.getInformantRegisterStreamId;
import static uk.gov.moj.cpp.domains.InformantRegisterHelper.informantRegisterRequestId;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class InformantRegisterHelperTest {

    private static final String HEARING_ID = "0aa5bf35-1e51-45e5-9e42-64bd57e15c11";
    private static final String HEARING_DAY = "2026-08-19";
    private static final String SHARED_TIME = "2026-08-19T16:42:07.512Z";

    private static final String PROSECUTION_AUTHORITY_ID = "0aa5bf35-1e51-45e5-9e42-64bd57e15c11";
    private static final String REGISTER_DATE = "2026-08-19";

    /**
     * The expected value is pinned rather than re-derived from the recipe, because the consuming
     * service dedupes on this id across deployments: if the recipe ever changes, every
     * already-published share mints a new id and gets processed a second time. A test that
     * recomputed the recipe would follow such a change silently; this one fails.
     */
    @Test
    public void informantRegisterRequestId_forAKnownShare_should_mintThePinnedId() {
        final UUID requestId = informantRegisterRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME);

        assertThat(requestId, is(UUID.fromString("e74ed270-0b62-36d9-bb10-12bd6baa5bea")));
    }

    @Test
    public void informantRegisterRequestId_forTheSameShareTwice_should_mintTheSameId() {
        assertThat(informantRegisterRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME),
                is(informantRegisterRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME)));
    }

    /**
     * A re-share is a legitimate business event carrying a new sharedTime, and must reach the
     * consuming service rather than being swallowed as a duplicate of the first share.
     */
    @Test
    public void informantRegisterRequestId_forAReshare_should_mintANewId() {
        final UUID reshareRequestId =
                informantRegisterRequestId(HEARING_ID, HEARING_DAY, "2026-08-19T19:30:00.000Z");

        assertThat(reshareRequestId, is(UUID.fromString("32475c57-26ec-31e2-9349-a2bc6e1f8411")));
        assertThat(reshareRequestId, is(not(informantRegisterRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME))));
    }

    @Test
    public void informantRegisterRequestId_forADifferentHearingDay_should_mintANewId() {
        assertThat(informantRegisterRequestId(HEARING_ID, "2026-08-20", SHARED_TIME),
                is(not(informantRegisterRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME))));
    }

    @Test
    public void informantRegisterRequestId_forADifferentHearing_should_mintANewId() {
        assertThat(informantRegisterRequestId("11111111-2222-3333-4444-555555555555", HEARING_DAY, SHARED_TIME),
                is(not(informantRegisterRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME))));
    }

    /**
     * The separator has to be part of the hash, not just cosmetic: concatenated without one,
     * ("2026-08-1" + "92026-...") and ("2026-08-19" + "2026-...") would produce the same bytes, so
     * two different shares could collide onto one requestId.
     */
    @Test
    public void informantRegisterRequestId_forAmbiguouslySplittableParts_should_notCollide() {
        assertThat(informantRegisterRequestId(HEARING_ID, "2026-08-1", "92026-08-19T16:42:07.512Z"),
                is(not(informantRegisterRequestId(HEARING_ID, HEARING_DAY, SHARED_TIME))));
    }

    /**
     * The pre-existing stream-id recipe is left on the platform default charset deliberately -
     * changing it would move the stream ids of every informant register already in the event store.
     * Pinned so that "tidying" it into agreement with informantRegisterRequestId, which does pin
     * UTF-8, fails here rather than silently orphaning every existing informant register stream.
     *
     * <p>The inputs are ASCII, so the pinned value holds under any plausible platform default; that
     * is exactly why the latent bug is invisible until someone runs on a charset where it is not.
     */
    @Test
    public void getInformantRegisterStreamId_forAKnownRegister_should_mintThePinnedId() {
        assertThat(getInformantRegisterStreamId(PROSECUTION_AUTHORITY_ID, REGISTER_DATE),
                is(UUID.fromString("a86a1f3b-11d5-3725-8304-e47ef46515ce")));
    }
}
