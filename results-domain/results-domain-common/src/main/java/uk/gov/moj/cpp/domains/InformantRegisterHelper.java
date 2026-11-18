package uk.gov.moj.cpp.domains;

import static java.util.UUID.nameUUIDFromBytes;

import java.util.UUID;

/**
 * The informant register publish requestId recipe lives in {@link DistributionRequestIdHelper},
 * shared with the probation publisher (and the YOT publisher once it lands).
 */
public class InformantRegisterHelper {

    public static UUID getInformantRegisterStreamId(final String prosecutionAuthorityId, final String registerDate) {
        return nameUUIDFromBytes((prosecutionAuthorityId + registerDate).getBytes());
    }

}
