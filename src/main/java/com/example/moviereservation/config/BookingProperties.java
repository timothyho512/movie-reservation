package com.example.moviereservation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.booking")
public class BookingProperties {
    private long cutoffMinutes = 10;

    public long getCutoffMinutes() {
        return cutoffMinutes;
    }

    public void setCutoffMinutes(long cutoffMinutes) {
        this.cutoffMinutes = cutoffMinutes;
    }
}
