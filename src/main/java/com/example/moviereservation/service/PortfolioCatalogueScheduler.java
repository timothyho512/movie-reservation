package com.example.moviereservation.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

@Component
@Order(2)
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class PortfolioCatalogueScheduler implements ApplicationRunner {
    private static final ZoneId PROGRAMME_ZONE = ZoneId.of("Europe/London");
    private final TmdbCatalogueSyncService catalogueSyncService;
    private final WeeklyProgrammeService weeklyProgrammeService;
    private final WeeklyShowtimeGenerator weeklyShowtimeGenerator;

    public PortfolioCatalogueScheduler(
            TmdbCatalogueSyncService catalogueSyncService,
            WeeklyProgrammeService weeklyProgrammeService,
            WeeklyShowtimeGenerator weeklyShowtimeGenerator
    ) {
        this.catalogueSyncService = catalogueSyncService;
        this.weeklyProgrammeService = weeklyProgrammeService;
        this.weeklyShowtimeGenerator = weeklyShowtimeGenerator;
    }

    @Override
    public void run(ApplicationArguments args) {
        LocalDateTime now = currentTime();
        catalogueSyncService.synchronizeIfStale(now);
        reconcileMissingProgrammes(now);
    }

    @Scheduled(
            cron = "${app.demo-data.catalogue-cron:0 0 4 * * *}",
            zone = "${app.demo-data.maintenance-zone:Europe/London}"
    )
    public void scheduledCatalogueRefresh() {
        catalogueSyncService.synchronize();
    }

    @Scheduled(
            cron = "${app.demo-data.programme-cron:0 15 4 * * MON}",
            zone = "${app.demo-data.maintenance-zone:Europe/London}"
    )
    public void scheduledWeeklyProgramme() {
        reconcileMissingProgrammes(currentTime());
    }

    void reconcileMissingProgrammes(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        ensureProgrammeAndShowtimes(currentWeekStart, today, now);

        LocalDate nextWeekStart = currentWeekStart.plusWeeks(1);
        LocalDate publicationDate = nextWeekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (!today.isBefore(publicationDate)) {
            ensureProgrammeAndShowtimes(nextWeekStart, nextWeekStart, now);
        }
    }

    private void ensureProgrammeAndShowtimes(
            LocalDate programmeStartsOn,
            LocalDate firstShowtimeDate,
            LocalDateTime now
    ) {
        var programme = weeklyProgrammeService.ensureProgramme(programmeStartsOn);
        LocalDate programmeEndsOn = programmeStartsOn.plusDays(WeeklyProgrammeService.PROGRAMME_DAYS - 1L);
        weeklyShowtimeGenerator.ensureShowtimes(programme, firstShowtimeDate, programmeEndsOn, now);
    }

    private LocalDateTime currentTime() {
        return LocalDateTime.now(PROGRAMME_ZONE);
    }
}
