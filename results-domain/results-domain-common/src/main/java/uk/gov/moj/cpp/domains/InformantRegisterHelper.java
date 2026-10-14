package uk.gov.moj.cpp.domains;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.nameUUIDFromBytes;

import java.util.UUID;

public class InformantRegisterHelper {

    private static final String REQUEST_ID_SEPARATOR = "|";

    public static UUID getInformantRegisterStreamId(final String prosecutionAuthorityId, final String registerDate) {
        return nameUUIDFromBytes((prosecutionAuthorityId + registerDate).getBytes());
    }

    /**
     * The deterministic identity of one informant register publish request, minted from the share
     * that occasioned it. Republishing the same share - a redelivered command, a replayed event,
     * support tooling - yields the same id, so the consuming service can dedupe on it and the
     * broker can dedupe on the messageId derived from it. A genuine re-share carries a new
     * sharedTime and therefore mints a new id, because a re-share is a legitimate business event
     * that must be published again.
     *
     * <p>The three parts are taken as the raw strings that travel on the wire, and the charset is
     * pinned, because the id is a hash: any reformatting between the hearing-resulted event and
     * here - a rewritten timestamp, a platform-default charset - would mint a different id for the
     * same share and silently defeat both dedupes. Callers must not parse and re-render these
     * values before passing them in.
     *
     * @param hearingId  the resulted hearing's id, canonical lowercase uuid
     * @param hearingDay the hearing day, exactly as carried on the hearing-resulted event
     * @param sharedTime the share timestamp, exactly as carried on the hearing-resulted event
     */
    public static UUID informantRegisterRequestId(final String hearingId,
                                                  final String hearingDay,
                                                  final String sharedTime) {
        return nameUUIDFromBytes(
                (hearingId + REQUEST_ID_SEPARATOR + hearingDay + REQUEST_ID_SEPARATOR + sharedTime).getBytes(UTF_8));
    }

}
