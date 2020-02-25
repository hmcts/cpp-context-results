package uk.gov.moj.cpp.results.event.service;

public interface CacheService {
    String add(String key, String value);

    String get(String key);
}
