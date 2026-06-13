package com.example.moviereservation.service;

import com.example.moviereservation.config.SeatMapCacheProperties;
import com.example.moviereservation.config.RedisKeyProperties;
import com.example.moviereservation.dto.SeatMapResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisSeatMapCacheService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SeatMapCacheProperties seatMapCacheProperties;
    private final String seatMapKeyPrefix;

    public RedisSeatMapCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            SeatMapCacheProperties seatMapCacheProperties,
            RedisKeyProperties redisKeyProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.seatMapCacheProperties = seatMapCacheProperties;
        this.seatMapKeyPrefix = redisKeyProperties.getKeyNamespace() + ":seat-map:showtime:";
    }

    public Optional<SeatMapResponse> get(Long showtimeId) {
        String rawValue = redisTemplate.opsForValue().get(cacheKey(showtimeId));
        if (rawValue == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(rawValue, SeatMapResponse.class));
        } catch (Exception e) {
            evict(showtimeId);
            return Optional.empty();
        }
    }

    public void put(Long showtimeId, SeatMapResponse seatMap) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey(showtimeId),
                    objectMapper.writeValueAsString(seatMap),
                    Duration.ofSeconds(seatMapCacheProperties.getTtlSeconds())
            );
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize Redis seat map cache", e);
        }
    }

    public void evict(Long showtimeId) {
        redisTemplate.delete(cacheKey(showtimeId));
    }

    private String cacheKey(Long showtimeId) {
        return seatMapKeyPrefix + showtimeId;
    }
}
