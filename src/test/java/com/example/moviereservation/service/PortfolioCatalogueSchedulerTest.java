package com.example.moviereservation.service;

import com.example.moviereservation.entity.ProgrammeEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioCatalogueSchedulerTest {
    @Mock private TmdbCatalogueSyncService catalogueSyncService;
    @Mock private WeeklyProgrammeService weeklyProgrammeService;
    @Mock private WeeklyShowtimeGenerator weeklyShowtimeGenerator;

    @Test
    void startupOnSundayReconcilesOnlyCurrentProgramme() {
        LocalDate currentFriday = LocalDate.of(2026, 7, 17);
        LocalDateTime now = LocalDateTime.of(2026, 7, 19, 10, 0);
        List<ProgrammeEntry> programme = List.of();
        when(weeklyProgrammeService.ensureProgramme(currentFriday)).thenReturn(programme);

        scheduler().reconcileMissingProgrammes(now);

        verify(weeklyProgrammeService).ensureProgramme(currentFriday);
        verify(weeklyShowtimeGenerator).ensureShowtimes(
                programme, LocalDate.of(2026, 7, 19), LocalDate.of(2026, 7, 23), now
        );
    }

    @Test
    void startupAfterMondayCatchesUpCurrentAndNextProgrammes() {
        LocalDate currentFriday = LocalDate.of(2026, 7, 17);
        LocalDate nextFriday = LocalDate.of(2026, 7, 24);
        LocalDateTime now = LocalDateTime.of(2026, 7, 21, 10, 0);
        List<ProgrammeEntry> current = List.of();
        List<ProgrammeEntry> next = List.of();
        when(weeklyProgrammeService.ensureProgramme(currentFriday)).thenReturn(current);
        when(weeklyProgrammeService.ensureProgramme(nextFriday)).thenReturn(next);

        scheduler().reconcileMissingProgrammes(now);

        verify(weeklyShowtimeGenerator).ensureShowtimes(
                current, LocalDate.of(2026, 7, 21), LocalDate.of(2026, 7, 23), now
        );
        verify(weeklyShowtimeGenerator).ensureShowtimes(
                next, LocalDate.of(2026, 7, 24), LocalDate.of(2026, 7, 30), now
        );
    }

    private PortfolioCatalogueScheduler scheduler() {
        return new PortfolioCatalogueScheduler(
                catalogueSyncService,
                weeklyProgrammeService,
                weeklyShowtimeGenerator
        );
    }
}
