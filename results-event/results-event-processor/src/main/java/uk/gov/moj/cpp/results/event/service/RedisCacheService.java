package uk.gov.moj.cpp.results.event.service;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;

public class RedisCacheService implements CacheService {

    @Inject
    @Value(key = "redisCacheHost", defaultValue = "localhost")
    private String host;

    @Inject
    @Value(key = "redisCacheKey", defaultValue = "none")
    private String key;

    @Inject
    @Value(key = "redisCachePort", defaultValue = "6380")
    private String port;

    @Inject
    @Value(key = "redisCacheUseSsl", defaultValue = "false")
    private String useSsl;

    private RedisClient redisClient;

    @Override
    public String add(final String key, final String value) {
        if ("localhost".equals(host)) {
            return null;
        }
        final String keyPart = ("none".equals(this.key) ? "" : this.key + "@");
        final RedisURI redisURI = RedisURI.create("redis://" + keyPart + host + ":" + port);
        redisURI.setSsl(Boolean.valueOf(useSsl));
        redisClient = RedisClient.create(redisURI);
        return getRedisCommand().set(key, value);
    }

    @Override
    public String get(final String hearingId) {
        return getRedisCommand().get(hearingId);
    }

    private RedisCommands<String, String> getRedisCommand() {
        return redisClient.connect().sync();
    }
}
