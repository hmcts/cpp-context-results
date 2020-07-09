package uk.gov.moj.cpp.results.event.service;

import java.util.UUID;

public interface EventGridService {

    boolean sendHearingResultedEvent(UUID userId, String hearingId);

}
