package uk.gov.moj.cpp.results.event.service;

import java.util.UUID;

public interface ProbationDistributionQueueService {

    /**
     * Publishes the probation results distribution command for one resulted regular hearing.
     *
     * <p>Throws rather than reporting failure, so a broker failure rolls back the event delivery and
     * the framework redelivers and ultimately dead-letters the event.
     *
     * <p>hearingId, hearingDay and sharedTime must be the raw strings carried on the hearing-resulted
     * event. The requestId is a hash of them, so a value parsed and re-rendered on the way here
     * mints a different id for the same share and defeats the consuming service's processed-log.
     *
     * @param userId the CPP user who shared the results, already parsed - see
     *               {@code InformantRegisterQueueService#publishDistributionCommand} for why. May be
     *               null, in which case the field is omitted from the message: the consumer's
     *               contract allows it to be absent but refuses an explicit null.
     * @throws ProbationDistributionPublishException if the message could not be published
     */
    void publishDistributionCommand(final String hearingId, final String hearingDay, final String sharedTime, final UUID userId);

}
