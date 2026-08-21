package uk.gov.moj.cpp.results.event.service;

public interface InformantRegisterQueueService {

    boolean sendDistributionCommand(final String hearingId, final String hearingDay, final String sharedTime);

}
