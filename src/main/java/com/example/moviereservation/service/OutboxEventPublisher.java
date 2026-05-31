package com.example.moviereservation.service;

import com.example.moviereservation.entity.OutboxEvent;

public interface OutboxEventPublisher {
    void publish(OutboxEvent event);
}
