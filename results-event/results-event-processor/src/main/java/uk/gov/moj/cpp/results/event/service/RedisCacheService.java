package uk.gov.moj.cpp.results.event.service;

import uk.gov.justice.services.common.configuration.Value;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;

import javax.inject.Inject;

public class RedisCacheService implements CacheService {

    @Inject
    @Value(key = "redisCacheHost", defaultValue = "RC-STE-COMMON.redis.cache.windows.net")
    private String host;

    @Inject
    @Value(key = "redisCacheKey", defaultValue = "iievMedi1N12exPmAn7SDH+KXtarprio6Bfh7tI1wQg=")
    private String key;

    @Inject
    @Value(key = "redisCachePort", defaultValue = "6380")
    private String port;

    @Inject
    @Value(key = "redisCacheUseSsl", defaultValue = "true")
    private String useSsl;

    private RedisClient redisClient;

    @Override
    public String add(final String hearingId, final String hearingJson) {
        if ("localhost".equals(host)) {
            return null;
        }
        final String keyPart = ("none".equals(key) ? "" : key + "@");
        final RedisURI redisURI = RedisURI.create("redis://" + keyPart + host + ":" + port);
        redisURI.setSsl(Boolean.valueOf(useSsl));
        redisClient = RedisClient.create(redisURI);
        return getRedisCommand().set(hearingId, hearingJson);
    }

    @Override
    public String get(final String hearingId) {
        return getRedisCommand().get(hearingId);
    }

    private RedisCommands<String, String> getRedisCommand() {
        return redisClient.connect().sync();
    }
}
