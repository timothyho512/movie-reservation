package com.example.moviereservation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.tmdb")
public class TmdbProperties {
    private String accessToken = "";
    private String apiBaseUrl = "https://api.themoviedb.org/3";
    private String region = "GB";
    private String language = "en-GB";
    private int catalogueSize = 12;
    private int requestTimeoutSeconds = 8;

    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getCatalogueSize() {
        return catalogueSize;
    }

    public void setCatalogueSize(int catalogueSize) {
        this.catalogueSize = catalogueSize;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }
}
