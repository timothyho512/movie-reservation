package com.example.moviereservation.repository;

import com.example.moviereservation.entity.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT event
            FROM OutboxEvent event
            WHERE event.status IN (
                com.example.moviereservation.entity.OutboxEventStatus.PENDING,
                com.example.moviereservation.entity.OutboxEventStatus.FAILED
            )
            AND event.nextAttemptAt <= :now
            AND event.attemptCount < :maxAttempts
            ORDER BY event.createdAt ASC
    """)
    List<OutboxEvent> findDueEventsForUpdate(
            @Param("now") LocalDateTime now,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable
    );

    long countByEventTypeAndAggregateTypeAndAggregateId(
            String eventType,
            String aggregateType,
            String aggregateId
    );
}
