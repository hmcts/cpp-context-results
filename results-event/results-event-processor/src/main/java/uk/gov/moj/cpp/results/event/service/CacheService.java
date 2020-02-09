package uk.gov.moj.cpp.results.event.service;

public interface CacheService {
    String add(String hearingId, String hearingJson);

    String get(String hearingId);
}
