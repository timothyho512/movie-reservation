package com.example.moviereservation.service;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.ProgrammeEntry;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.ScreenType;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WeeklyShowtimeGenerator {
    private static final Logger logger = LoggerFactory.getLogger(WeeklyShowtimeGenerator.class);
    private static final int DEFAULT_RUNTIME_MINUTES = 120;
    private static final int TURNAROUND_MINUTES = 20;
    private static final List<LocalTime> DAILY_START_TIMES = List.of(
            LocalTime.of(14, 0),
            LocalTime.of(19, 30)
    );

    private final ScreenRepository screenRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingWindowService bookingWindowService;

    public WeeklyShowtimeGenerator(
            ScreenRepository screenRepository,
            ShowtimeRepository showtimeRepository,
            BookingWindowService bookingWindowService
    ) {
        this.screenRepository = screenRepository;
        this.showtimeRepository = showtimeRepository;
        this.bookingWindowService = bookingWindowService;
    }

    @Transactional
    public GenerationResult ensureShowtimes(
            List<ProgrammeEntry> programme,
            LocalDate firstDate,
            LocalDate lastDate,
            LocalDateTime now
    ) {
        List<Screen> screens = screenRepository.findActiveSchedulingScreens();
        if (programme.isEmpty() || screens.isEmpty() || firstDate.isAfter(lastDate)) {
            logger.warn(
                    "event=weekly_showtimes_skipped programmeCount={} screenCount={} firstDate={} lastDate={}",
                    programme.size(), screens.size(), firstDate, lastDate
            );
            return new GenerationResult(0, programme.size(), screens.size());
        }

        LocalDateTime bookingCutoff = bookingWindowService.bookingCutoffFrom(now);
        List<Showtime> generated = new ArrayList<>();
        int dayOffset = 0;
        for (LocalDate date = firstDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
            for (int screenIndex = 0; screenIndex < screens.size(); screenIndex++) {
                Screen screen = screens.get(screenIndex);
                for (int slotIndex = 0; slotIndex < DAILY_START_TIMES.size(); slotIndex++) {
                    LocalDateTime startTime = date.atTime(DAILY_START_TIMES.get(slotIndex));
                    if (!startTime.isAfter(bookingCutoff)) {
                        continue;
                    }

                    int programmeIndex = Math.floorMod(dayOffset + screenIndex + slotIndex, programme.size());
                    ProgrammeEntry entry = programme.get(programmeIndex);
                    Movie movie = entry.getMovie();
                    LocalDateTime endTime = startTime.plusMinutes(runtimeMinutes(movie) + TURNAROUND_MINUTES);
                    if (showtimeRepository.existsOverlappingShowtime(screen.getId(), startTime, endTime)
                            || overlapsGenerated(generated, screen, startTime, endTime)) {
                        continue;
                    }

                    Showtime showtime = new Showtime(
                            movie,
                            screen,
                            startTime,
                            endTime,
                            basePrice(screen.getScreenType())
                    );
                    showtime.setProgrammeEntry(entry);
                    generated.add(showtime);
                }
            }
            dayOffset++;
        }

        if (!generated.isEmpty()) {
            showtimeRepository.saveAll(generated);
        }
        logger.info(
                "event=weekly_showtimes_ensured generated={} programmeCount={} screenCount={} firstDate={} lastDate={}",
                generated.size(), programme.size(), screens.size(), firstDate, lastDate
        );
        return new GenerationResult(generated.size(), programme.size(), screens.size());
    }

    private boolean overlapsGenerated(
            List<Showtime> generated,
            Screen screen,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        return generated.stream().anyMatch(existing ->
                existing.getScreen().getId().equals(screen.getId())
                        && existing.getStartTime().isBefore(endTime)
                        && existing.getEndTime().isAfter(startTime)
        );
    }

    private int runtimeMinutes(Movie movie) {
        Integer runtime = movie.getRuntimeMinutes();
        return runtime != null && runtime > 0 ? runtime : DEFAULT_RUNTIME_MINUTES;
    }

    private BigDecimal basePrice(ScreenType screenType) {
        if (screenType == ScreenType.IMAX || screenType == ScreenType.FOUR_DX) {
            return new BigDecimal("19.00");
        }
        if (screenType == ScreenType.VIP || screenType == ScreenType.DOLBY_ATMOS) {
            return new BigDecimal("18.50");
        }
        return new BigDecimal("14.00");
    }

    public record GenerationResult(int generatedCount, int programmeCount, int screenCount) {
    }
}
