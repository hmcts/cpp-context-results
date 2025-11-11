package uk.gov.moj.cpp.results.event.service;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Long.parseLong;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisCacheService implements CacheService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCacheService.class);

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

    @Inject
    @Value(key = "redisExternalCacheKeyTTL", defaultValue = "7890000")
    private String extTTLSeconds;

    @Inject
    @Value(key = "redisInternalCacheKeyTTL", defaultValue = "86400")
    private String intTTLSeconds;

    private static final String CACHE_KEY_EXTERNAL_PREFIX = "EXT_";

    private RedisClient redisClient;

    private static final String DB_NAME = "0";

    @Override
    public String add(final String key, final String value) {
        if ("localhost".equals(host)) {
            return null;
        }
        setRedisClient();
        LOGGER.info("Addding hearing id to Redis key {} value {}", key, value);
        return executeAddCommand(key, value);
    }

    private void setRedisClient() {
        final String keyPart = ("none".equals(this.key) ? "" : this.key + "@");
        final RedisURI redisURI = RedisURI.create("redis://" + keyPart + host + ":" + port + "/" + DB_NAME);
        redisURI.setSsl(parseBoolean(useSsl));
        LOGGER.info("setting redisclient with Key {} host {} port {} and connected to database {}", keyPart, host, port, DB_NAME);
        if (redisClient == null) {
            redisClient = RedisClient.create(redisURI);
        }
    }

    @Override
    public String get(final String hearingId) {
        setRedisClient();
        return executeGetCommand(hearingId);
    }

    private String executeAddCommand(final String key, final String value) {

        try (final StatefulRedisConnection<String, String> connection = this.redisClient.connect()) {

            //Obtain the command API for synchronous execution.
            final RedisCommands<String, String> command = connection.sync();
            final String ttlSeconds = key.startsWith(CACHE_KEY_EXTERNAL_PREFIX) ? extTTLSeconds : intTTLSeconds;
            final SetArgs args = new SetArgs().ex(parseLong(ttlSeconds));
            return command.set(key, value, args);

        }
    }

    private String executeGetCommand(final String hearingId) {

        try (final StatefulRedisConnection<String, String> connection = this.redisClient.connect()) {

            //Obtain the command API for synchronous execution.
            final RedisCommands<String, String> command = connection.sync();

            return command.get(hearingId);
        }
    }
}