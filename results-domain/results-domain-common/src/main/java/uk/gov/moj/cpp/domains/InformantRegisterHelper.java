package uk.gov.moj.cpp.domains;

import static java.util.UUID.nameUUIDFromBytes;

import java.util.UUID;

public class InformantRegisterHelper {

    public static UUID getInformantRegisterStreamId(final String prosecutionAuthorityId, final String registerDate) {
        return nameUUIDFromBytes((prosecutionAuthorityId + registerDate).getBytes());
    }

}
