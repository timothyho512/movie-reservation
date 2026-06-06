package com.example.moviereservation.service;

import com.example.moviereservation.Exception.SeatUnavailableException;
import com.example.moviereservation.config.SeatLockProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class RedisSeatLockService {
    private static final String LOCK_KEY_PREFIX = "seat-lock:showtime:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SeatLockProperties seatLockProperties;

    public RedisSeatLockService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            SeatLockProperties seatLockProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.seatLockProperties = seatLockProperties;
    }

    public RedisSeatLockOwner authenticatedOwner(Long userId) {
        return new RedisSeatLockOwner(OwnerType.USER, userId, null, null);
    }

    public RedisSeatLockOwner guestOwner(String guestEmail) {
        return new RedisSeatLockOwner(OwnerType.GUEST, null, UUID.randomUUID().toString(), guestEmail);
    }

    public RedisSeatLockOwner guestOwner(String sessionId, String guestEmail) {
        return new RedisSeatLockOwner(OwnerType.GUEST, null, sessionId, guestEmail);
    }

    public RedisSeatLockBatch createLocks(Long showtimeId, Collection<Long> seatIds, RedisSeatLockOwner owner) {
        LocalDateTime lockedAt = LocalDateTime.now();
        LocalDateTime defaultExpiresAt = lockedAt.plusSeconds(seatLockProperties.getTtlSeconds());
        Duration ttl = Duration.ofSeconds(seatLockProperties.getTtlSeconds());
        List<Long> lockedSeatIds = new ArrayList<>();
        List<Long> createdSeatIds = new ArrayList<>();
        List<LocalDateTime> lockExpiries = new ArrayList<>();

        try {
            for (Long seatId : sortedDistinctSeatIds(seatIds)) {
                RedisSeatLockValue value = RedisSeatLockValue.from(showtimeId, seatId, owner, lockedAt, defaultExpiresAt);
                Boolean created = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey(showtimeId, seatId), serialize(value), ttl);

                if (!Boolean.TRUE.equals(created)) {
                    RedisSeatLockValue existingLock = readLock(showtimeId, seatId);
                    if (existingLock != null && existingLock.isOwnedBy(owner)) {
                        lockedSeatIds.add(seatId);
                        lockExpiries.add(existingLock.expiresAt());
                        continue;
                    }

                    releaseLocks(showtimeId, createdSeatIds, owner);
                    throw new SeatUnavailableException("Seat " + seatId + " is currently locked by another user for this showtime");
                }

                lockedSeatIds.add(seatId);
                createdSeatIds.add(seatId);
                lockExpiries.add(defaultExpiresAt);
            }

            LocalDateTime batchExpiresAt = lockExpiries.stream()
                    .min(LocalDateTime::compareTo)
                    .orElse(defaultExpiresAt);

            return new RedisSeatLockBatch(owner.sessionId(), batchExpiresAt, lockedSeatIds);
        } catch (SeatUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            releaseLocks(showtimeId, createdSeatIds, owner);
            throw e;
        }
    }

    public List<Long> findLockedSeatIdsForShowtime(Long showtimeId) {
        Set<String> keys = redisTemplate.keys(lockKeyPattern(showtimeId));
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        return keys.stream()
                .map(this::parseSeatId)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    public List<RedisSeatLockValue> findOwnedLocks(
            Long showtimeId,
            Collection<Long> seatIds,
            RedisSeatLockOwner owner
    ) {
        return sortedDistinctSeatIds(seatIds).stream()
                .map(seatId -> readLock(showtimeId, seatId))
                .filter(Objects::nonNull)
                .filter(lock -> lock.isOwnedBy(owner))
                .sorted(Comparator.comparing(RedisSeatLockValue::seatId))
                .toList();
    }

    public void validateLocksOwned(Long showtimeId, Collection<Long> seatIds, RedisSeatLockOwner owner) {
        List<Long> expectedSeatIds = sortedDistinctSeatIds(seatIds);
        List<Long> ownedSeatIds = findOwnedLocks(showtimeId, seatIds, owner).stream()
                .map(RedisSeatLockValue::seatId)
                .toList();

        if (!ownedSeatIds.equals(expectedSeatIds)) {
            throw new SeatUnavailableException("No valid active lock found for this request");
        }
    }

    public LocalDateTime earliestExpiry(Long showtimeId, Collection<Long> seatIds, RedisSeatLockOwner owner) {
        List<RedisSeatLockValue> ownedLocks = findOwnedLocks(showtimeId, seatIds, owner);
        if (ownedLocks.isEmpty()) {
            throw new SeatUnavailableException("No valid active locks found");
        }

        return ownedLocks.stream()
                .map(RedisSeatLockValue::expiresAt)
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new SeatUnavailableException("No valid active locks found"));
    }

    public int releaseLocks(Long showtimeId, Collection<Long> seatIds, RedisSeatLockOwner owner) {
        int released = 0;

        for (Long seatId : sortedDistinctSeatIds(seatIds)) {
            RedisSeatLockValue lock = readLock(showtimeId, seatId);
            if (lock != null && lock.isOwnedBy(owner)) {
                Boolean deleted = redisTemplate.delete(lockKey(showtimeId, seatId));
                if (Boolean.TRUE.equals(deleted)) {
                    released++;
                }
            }
        }

        return released;
    }

    public int releaseAllLocksForShowtime(Long showtimeId) {
        Set<String> keys = redisTemplate.keys(lockKeyPattern(showtimeId));
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        return Math.toIntExact(redisTemplate.delete(keys));
    }

    private RedisSeatLockValue readLock(Long showtimeId, Long seatId) {
        String rawValue = redisTemplate.opsForValue().get(lockKey(showtimeId, seatId));
        if (rawValue == null) {
            return null;
        }

        try {
            return objectMapper.readValue(rawValue, RedisSeatLockValue.class);
        } catch (Exception e) {
            throw new IllegalStateException("Could not deserialize Redis seat lock", e);
        }
    }

    private String serialize(RedisSeatLockValue value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize Redis seat lock", e);
        }
    }

    private List<Long> sortedDistinctSeatIds(Collection<Long> seatIds) {
        return seatIds.stream()
                .distinct()
                .sorted()
                .toList();
    }

    private String lockKeyPattern(Long showtimeId) {
        return LOCK_KEY_PREFIX + showtimeId + ":seat:*";
    }

    private String lockKey(Long showtimeId, Long seatId) {
        return LOCK_KEY_PREFIX + showtimeId + ":seat:" + seatId;
    }

    private Long parseSeatId(String key) {
        int seatMarkerIndex = key.lastIndexOf(":seat:");
        if (seatMarkerIndex < 0) {
            return null;
        }

        try {
            return Long.parseLong(key.substring(seatMarkerIndex + 6));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public enum OwnerType {
        USER,
        GUEST
    }

    public record RedisSeatLockOwner(
            OwnerType ownerType,
            Long userId,
            String sessionId,
            String guestEmail
    ) {
    }

    public record RedisSeatLockBatch(
            String sessionId,
            LocalDateTime expiresAt,
            List<Long> lockedSeatIds
    ) {
    }

    public record RedisSeatLockValue(
            Long showtimeId,
            Long seatId,
            OwnerType ownerType,
            Long userId,
            String sessionId,
            String guestEmail,
            LocalDateTime lockedAt,
            LocalDateTime expiresAt
    ) {
        private static RedisSeatLockValue from(
                Long showtimeId,
                Long seatId,
                RedisSeatLockOwner owner,
                LocalDateTime lockedAt,
                LocalDateTime expiresAt
        ) {
            return new RedisSeatLockValue(
                    showtimeId,
                    seatId,
                    owner.ownerType(),
                    owner.userId(),
                    owner.sessionId(),
                    owner.guestEmail(),
                    lockedAt,
                    expiresAt
            );
        }

        private boolean isOwnedBy(RedisSeatLockOwner owner) {
            if (ownerType != owner.ownerType()) {
                return false;
            }

            if (ownerType == OwnerType.USER) {
                return Objects.equals(userId, owner.userId());
            }

            return Objects.equals(sessionId, owner.sessionId())
                    && guestEmail != null
                    && owner.guestEmail() != null
                    && guestEmail.trim().equalsIgnoreCase(owner.guestEmail().trim());
        }
    }
}
