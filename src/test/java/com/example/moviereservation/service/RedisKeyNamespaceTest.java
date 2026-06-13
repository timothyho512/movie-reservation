package com.example.moviereservation.service;

import com.example.moviereservation.config.RedisKeyProperties;
import com.example.moviereservation.config.SeatLockProperties;
import com.example.moviereservation.config.SeatMapCacheProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisKeyNamespaceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ObjectMapper objectMapper;

    @Test
    void seatMapCacheUsesConfiguredNamespace() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("movie-reservation:prod:v2:seat-map:showtime:7")).thenReturn(null);

        RedisSeatMapCacheService service = new RedisSeatMapCacheService(
                redisTemplate,
                objectMapper,
                new SeatMapCacheProperties(),
                redisKeyProperties()
        );

        assertThat(service.get(7L)).isEmpty();
        verify(valueOperations).get("movie-reservation:prod:v2:seat-map:showtime:7");
    }

    @Test
    void seatLocksUseConfiguredNamespace() {
        when(redisTemplate.keys("movie-reservation:prod:v2:seat-lock:showtime:7:seat:*"))
                .thenReturn(Set.of());

        RedisSeatLockService service = new RedisSeatLockService(
                redisTemplate,
                objectMapper,
                new SeatLockProperties(),
                redisKeyProperties()
        );

        assertThat(service.findLockedSeatIdsForShowtime(7L)).isEmpty();
        verify(redisTemplate).keys("movie-reservation:prod:v2:seat-lock:showtime:7:seat:*");
    }

    private RedisKeyProperties redisKeyProperties() {
        RedisKeyProperties properties = new RedisKeyProperties();
        properties.setKeyNamespace("movie-reservation:prod:v2");
        return properties;
    }
}
