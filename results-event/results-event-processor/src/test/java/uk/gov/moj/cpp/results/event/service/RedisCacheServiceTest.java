package uk.gov.moj.cpp.results.event.service;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Disabled
public class RedisCacheServiceTest {

    @InjectMocks
    private RedisCacheService redisCacheService;
    private RedisClient redisClient;
    private StatefulRedisConnection statefulRedisConnection;
    private RedisCommands<String, String> redisCommands;

    private void mockRedis() {
        redisClient = mock(RedisClient.class);

        ReflectionUtil.setField(redisCacheService, "redisClient", redisClient);
        ReflectionUtil.setField(redisCacheService, "host", "localhost");
        ReflectionUtil.setField(redisCacheService, "key", "test_key");
        ReflectionUtil.setField(redisCacheService, "port", "6380");
        ReflectionUtil.setField(redisCacheService, "useSsl", "false");

        statefulRedisConnection = mock(StatefulRedisConnection.class);
        when(redisClient.connect()).thenReturn(statefulRedisConnection);
        redisCommands = mock(RedisCommands.class);
        when(statefulRedisConnection.sync()).thenReturn(redisCommands);
    }

    @BeforeEach
    public void setUp() {
        redisCacheService = new RedisCacheService();
        mockRedis();
    }

    @Test
    public void shouldAddToCacheSuccessfully() {
        when(redisCommands.set("12345", "{}")).thenReturn("12345");
        assertThat(redisCacheService.add("12345", "{}"), is("12345"));
    }

    @Test
    public void shouldGetFromCacheSuccessfully() {
        when(redisCommands.get("12345")).thenReturn("{}");
        assertThat(redisCacheService.get("12345"), is("{}"));
    }
}