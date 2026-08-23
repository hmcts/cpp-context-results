package uk.gov.moj.cpp.results.event.service;

import java.util.UUID;

public interface InformantRegisterQueueService {

    /**
     * @param userId the CPP user who shared the results, already parsed. Typed as a UUID for the
     *               same reason {@link EventGridService#sendHearingResultedForDayEvent} is: the
     *               consumer's schema types userId as a canonical uuid string, so a value that is
     *               not one can only dead-letter on arrival. Parsing at the caller keeps the two
     *               legs of this fan-out failing the same way.
     */
    boolean sendDistributionCommand(final String hearingId, final String hearingDay, final String sharedTime, final UUID userId);

}
