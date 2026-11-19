package uk.gov.moj.cpp.domains;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.nameUUIDFromBytes;

import java.util.UUID;

/**
 * The one requestId recipe shared by every Service Bus distribution publisher in this context - the
 * informant register and probation results publishers, and the YOT results publisher (CRA-295,
 * not yet on main) when it lands. It is deliberately not copied per publisher: each consuming
 * service re-implements it (the probation service as
 * {@code RequestIdRecipe}) and pins it with the vector in {@code DistributionRequestIdHelperTest}, so
 * one recipe here means one thing for those services to agree with.
 *
 * <p>The same share therefore mints the same requestId on every sibling queue. That is harmless:
 * each queue has its own consumer and its own (source, requestId) processed-log.
 */
public final class DistributionRequestIdHelper {

    private static final String REQUEST_ID_SEPARATOR = "|";

    private DistributionRequestIdHelper() {
    }

    /**
     * The deterministic identity of one distribution publish request, minted from the share that
     * occasioned it: {@code UUID.nameUUIDFromBytes(hearingId + "|" + hearingDay + "|" + sharedTime)}
     * over UTF-8. Republishing the same share - a redelivered command, a replayed event, support
     * tooling - yields the same id, so the consuming service can dedupe on it. A genuine re-share
     * carries a new sharedTime and therefore mints a new id, because a re-share is a legitimate
     * business event that must be published again.
     *
     * <p>The parts are taken as the raw strings that travel on the wire, and the charset is pinned,
     * because the id is a hash: any reformatting between the hearing-resulted event and here - a
     * rewritten timestamp, a platform-default charset - would mint a different id for the same share
     * and silently defeat the consumer's dedupe. Callers must not parse and re-render these values
     * before passing them in.
     *
     * <p>userId is deliberately not part of the recipe, so the same share republished by a different
     * route still dedupes.
     *
     * @param hearingId  the resulted hearing's id, canonical lowercase uuid
     * @param hearingDay the hearing day, exactly as carried on the hearing-resulted event
     * @param sharedTime the share timestamp, exactly as carried on the hearing-resulted event
     */
    public static UUID distributionRequestId(final String hearingId,
                                             final String hearingDay,
                                             final String sharedTime) {
        return nameUUIDFromBytes(
                (hearingId + REQUEST_ID_SEPARATOR + hearingDay + REQUEST_ID_SEPARATOR + sharedTime).getBytes(UTF_8));
    }
}
