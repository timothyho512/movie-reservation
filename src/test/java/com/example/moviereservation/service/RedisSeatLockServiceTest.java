package com.example.moviereservation.service;

import com.example.moviereservation.config.RedisKeyProperties;
import com.example.moviereservation.config.SeatLockProperties;
import com.example.moviereservation.service.RedisSeatLockService.RedisSeatLockValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisSeatLockServiceTest {
    private static final String LOCK_KEY =
            "movie-reservation:test:v1:seat-lock:showtime:7:seat:11";

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ObjectMapper objectMapper;

    private RedisSeatLockService service;

    @BeforeEach
    void setUp() {
        RedisKeyProperties redisKeyProperties = new RedisKeyProperties();
        redisKeyProperties.setKeyNamespace("movie-reservation:test:v1");

        service = new RedisSeatLockService(
                redisTemplate,
                objectMapper,
                new SeatLockProperties(),
                redisKeyProperties
        );
    }

    @Test
    void newlyCreatedLocksHaveUniqueTokens() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString(any(RedisSeatLockValue.class)))
                .thenReturn("first-lock", "second-lock");
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("first-lock"), any(Duration.class)))
                .thenReturn(true);
        when(valueOperations.setIfAbsent(
                eq("movie-reservation:test:v1:seat-lock:showtime:7:seat:12"),
                eq("second-lock"),
                any(Duration.class)
        )).thenReturn(true);

        service.createLocks(7L, List.of(11L, 12L), service.authenticatedOwner(42L));

        ArgumentCaptor<RedisSeatLockValue> lockCaptor =
                ArgumentCaptor.forClass(RedisSeatLockValue.class);
        verify(objectMapper, org.mockito.Mockito.times(2))
                .writeValueAsString(lockCaptor.capture());

        List<String> tokens = lockCaptor.getAllValues().stream()
                .map(RedisSeatLockValue::lockToken)
                .toList();

        assertThat(tokens).allSatisfy(token -> assertThat(token).isNotBlank());
        assertThat(tokens).doesNotHaveDuplicates();
    }

    @Test
    void releaseDoesNotDeleteWhenStoredLockChangedBeforeLuaExecution() throws Exception {
        RedisSeatLockValue lock = new RedisSeatLockValue(
                7L,
                11L,
                RedisSeatLockService.OwnerType.USER,
                42L,
                null,
                null,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now().plusMinutes(15),
                "old-lock-token"
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOCK_KEY)).thenReturn("old-lock-json");
        when(objectMapper.readValue("old-lock-json", RedisSeatLockValue.class)).thenReturn(lock);
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(LOCK_KEY)),
                eq("old-lock-json")
        )).thenReturn(0L);

        int released = service.releaseLocks(
                7L,
                List.of(11L),
                service.authenticatedOwner(42L)
        );

        assertThat(released).isZero();
        verify(redisTemplate, never()).delete(LOCK_KEY);
    }

    @Test
    void releaseAtomicallyDeletesMatchingLockValue() throws Exception {
        RedisSeatLockValue lock = new RedisSeatLockValue(
                7L,
                11L,
                RedisSeatLockService.OwnerType.USER,
                42L,
                null,
                null,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now().plusMinutes(15),
                "current-lock-token"
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOCK_KEY)).thenReturn("current-lock-json");
        when(objectMapper.readValue("current-lock-json", RedisSeatLockValue.class)).thenReturn(lock);
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(LOCK_KEY)),
                eq("current-lock-json")
        )).thenReturn(1L);

        int released = service.releaseLocks(
                7L,
                List.of(11L),
                service.authenticatedOwner(42L)
        );

        assertThat(released).isOne();
        verify(redisTemplate, never()).delete(LOCK_KEY);
    }
}
