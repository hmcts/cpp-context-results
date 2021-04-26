package uk.gov.moj.cpp.results.event.service;

import java.util.UUID;

public interface EventGridService {

    boolean sendHearingResultedEvent(final UUID userId, final String hearingId, final String eventType);

    boolean sendHearingResultedForDayEvent(final UUID userId, final String hearingId, final String hearingDay, final String eventType);

}
