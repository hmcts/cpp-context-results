package uk.gov.moj.cpp.domains;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.cpp.domains.InformantRegisterHelper.getInformantRegisterStreamId;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * The informant register publish requestId recipe now lives in {@link DistributionRequestIdHelper},
 * shared with the probation publisher (and the YOT publisher once it lands), and is pinned in
 * {@code DistributionRequestIdHelperTest}.
 */
public class InformantRegisterHelperTest {

    private static final String PROSECUTION_AUTHORITY_ID = "0aa5bf35-1e51-45e5-9e42-64bd57e15c11";
    private static final String REGISTER_DATE = "2026-08-19";

    /**
     * The pre-existing stream-id recipe is left on the platform default charset deliberately -
     * changing it would move the stream ids of every informant register already in the event store.
     * Pinned so that "tidying" it into agreement with
     * {@link DistributionRequestIdHelper#distributionRequestId}, which does pin UTF-8, fails here
     * rather than silently orphaning every existing informant register stream.
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
