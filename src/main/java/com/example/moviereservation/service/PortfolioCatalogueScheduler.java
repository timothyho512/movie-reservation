package com.example.moviereservation.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(2)
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class PortfolioCatalogueScheduler implements ApplicationRunner {
    private final TmdbCatalogueSyncService catalogueSyncService;
    private final RollingShowtimeService rollingShowtimeService;

    public PortfolioCatalogueScheduler(
            TmdbCatalogueSyncService catalogueSyncService,
            RollingShowtimeService rollingShowtimeService
    ) {
        this.catalogueSyncService = catalogueSyncService;
        this.rollingShowtimeService = rollingShowtimeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        synchronizeCatalogueAndShowtimes();
    }

    @Scheduled(
            cron = "${app.demo-data.maintenance-cron:0 0 4 * * *}",
            zone = "${app.demo-data.maintenance-zone:Europe/London}"
    )
    public void scheduledMaintenance() {
        synchronizeCatalogueAndShowtimes();
    }

    private void synchronizeCatalogueAndShowtimes() {
        catalogueSyncService.synchronize();
        rollingShowtimeService.ensureFutureShowtimes(LocalDateTime.now());
    }
}
