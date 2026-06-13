package com.example.moviereservation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.redis")
public class RedisKeyProperties {
    private String keyNamespace = "movie-reservation:local:v1";

    public String getKeyNamespace() {
        return keyNamespace;
    }

    public void setKeyNamespace(String keyNamespace) {
        this.keyNamespace = keyNamespace;
    }
}
