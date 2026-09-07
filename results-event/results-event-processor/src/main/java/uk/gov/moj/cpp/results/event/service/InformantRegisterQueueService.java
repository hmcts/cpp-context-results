package uk.gov.moj.cpp.results.event.service;

import java.util.UUID;

public interface InformantRegisterQueueService {

    /**
     * Publishes the informant register distribution command for one resulted regular hearing.
     *
     * <p>Throws rather than reporting failure, so a broker failure rolls back the event delivery and
     * the framework redelivers and ultimately dead-letters the event. The previous boolean return
     * existed only to report a failure no caller read, which is how a Service Bus outage used to
     * lose a register silently.
     *
     * <p>hearingId, hearingDay and sharedTime must be the raw strings carried on the hearing-resulted
     * event. The requestId is a hash of them, so a value parsed and re-rendered on the way here
     * mints a different id for the same share and defeats both the broker's duplicate detection and
     * the consuming service's processed-log.
     *
     * @param userId the CPP user who shared the results, already parsed. Typed as a UUID for the
     *               same reason {@link EventGridService#sendHearingResultedForDayEvent} is: the
     *               consumer's schema types userId as a canonical uuid string, so a value that is
     *               not one can only dead-letter on arrival. Parsing at the caller keeps the two
     *               legs of this fan-out failing the same way.
     * @throws InformantRegisterPublishException if the message could not be published
     */
    void publishDistributionCommand(final String hearingId, final String hearingDay, final String sharedTime, final UUID userId);

}
