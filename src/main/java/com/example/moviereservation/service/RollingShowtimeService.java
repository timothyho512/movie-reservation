package com.example.moviereservation.service;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.ScreenType;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.repository.MovieRepository;
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
public class RollingShowtimeService {
    private static final Logger logger = LoggerFactory.getLogger(RollingShowtimeService.class);
    private static final int HORIZON_DAYS = 14;
    private static final int DEFAULT_RUNTIME_MINUTES = 120;
    private static final int TURNAROUND_MINUTES = 20;
    private static final List<LocalTime> DAILY_START_TIMES = List.of(
            LocalTime.of(14, 0),
            LocalTime.of(19, 30)
    );

    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final ShowtimeRepository showtimeRepository;

    public RollingShowtimeService(
            MovieRepository movieRepository,
            ScreenRepository screenRepository,
            ShowtimeRepository showtimeRepository
    ) {
        this.movieRepository = movieRepository;
        this.screenRepository = screenRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Transactional
    public GenerationResult ensureFutureShowtimes(LocalDateTime now) {
        List<Movie> movies = movieRepository.findAllByActiveTrueAndNowPlayingTrueOrderByTitleAsc();
        if (movies.isEmpty()) {
            movies = movieRepository.findAllByActiveTrueOrderByTitleAsc();
        }
        List<Screen> screens = screenRepository.findActiveSchedulingScreens();
        if (movies.isEmpty() || screens.isEmpty()) {
            logger.warn(
                    "event=rolling_showtimes_skipped movieCount={} screenCount={}",
                    movies.size(), screens.size()
            );
            return new GenerationResult(0, movies.size(), screens.size());
        }

        List<Showtime> generated = new ArrayList<>();
        LocalDate firstDate = now.toLocalDate().plusDays(1);
        for (int dayOffset = 0; dayOffset < HORIZON_DAYS; dayOffset++) {
            LocalDate date = firstDate.plusDays(dayOffset);
            for (int screenIndex = 0; screenIndex < screens.size(); screenIndex++) {
                Screen screen = screens.get(screenIndex);
                for (int slotIndex = 0; slotIndex < DAILY_START_TIMES.size(); slotIndex++) {
                    int movieIndex = Math.floorMod(dayOffset + screenIndex + slotIndex, movies.size());
                    Movie movie = movies.get(movieIndex);
                    LocalDateTime startTime = date.atTime(DAILY_START_TIMES.get(slotIndex));
                    LocalDateTime endTime = startTime.plusMinutes(runtimeMinutes(movie) + TURNAROUND_MINUTES);
                    if (showtimeRepository.existsOverlappingShowtime(screen.getId(), startTime, endTime)) {
                        continue;
                    }
                    generated.add(new Showtime(movie, screen, startTime, endTime, basePrice(screen.getScreenType())));
                }
            }
        }

        showtimeRepository.saveAll(generated);
        logger.info(
                "event=rolling_showtimes_ensured generated={} movieCount={} screenCount={} horizonDays={}",
                generated.size(), movies.size(), screens.size(), HORIZON_DAYS
        );
        return new GenerationResult(generated.size(), movies.size(), screens.size());
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

    public record GenerationResult(int generatedCount, int movieCount, int screenCount) {
    }
}
