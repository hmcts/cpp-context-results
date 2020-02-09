package uk.gov.moj.cpp.results.event.service;

public interface EventGridService {

    boolean sendHearingResultedEvent(String hearingId);

}
