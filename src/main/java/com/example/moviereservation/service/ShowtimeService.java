package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.ShowtimeRequest;
import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.entity.ScreenLayoutVersion;
import com.example.moviereservation.entity.ShowtimeStatus;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.ScreenLayoutVersionRepository;
import com.example.moviereservation.dto.GetAvailabilityResponse;
import com.example.moviereservation.dto.SeatAvailabilityDto;
import com.example.moviereservation.dto.SeatMapResponse;
import com.example.moviereservation.dto.ShowtimeSummaryResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ShowtimeService {
    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ScreenLayoutVersionRepository screenLayoutVersionRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RedisSeatLockService redisSeatLockService;

    @Autowired
    private RedisSeatMapCacheService redisSeatMapCacheService;

    public List<Showtime> getAllShowtimes() {
        return showtimeRepository.findAll();
    }

    public List<ShowtimeSummaryResponse> getShowtimeSummaries() {
        return showtimeRepository.findAllByOrderByStartTimeAsc().stream()
                .map(this::toShowtimeSummaryResponse)
                .toList();
    }

    public Showtime getShowtimeById(Long id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + id));
    }

    public ShowtimeSummaryResponse getShowtimeSummary(Long id) {
        return toShowtimeSummaryResponse(getShowtimeById(id));
    }

    public Showtime createShowtime(ShowtimeRequest request) {
        // Find the movie by ID
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + request.getMovieId()));

        // Find the screen by ID
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + request.getScreenId()));

        // Create showtime with both relationships
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        ScreenLayoutVersion layoutVersion = resolveCurrentLayoutVersion(screen);
        showtime.setLayoutVersion(layoutVersion);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(request.getEndTime());
        showtime.setBasePrice(request.getBasePrice());
        int totalSeats = seatRepository.findActiveByScreenIdAndLayoutVersionId(screen.getId(), layoutVersion.getId()).size();
        showtime.setTotalSeats(totalSeats);
        showtime.setAvailableSeats(totalSeats);
        showtime.setStatus(request.getStatus() != null ? request.getStatus() : ShowtimeStatus.UPCOMING);

        Showtime savedShowtime = showtimeRepository.save(showtime);
        redisSeatMapCacheService.evict(savedShowtime.getId());
        return savedShowtime;
    }

    public Showtime updateShowtime(Long id, ShowtimeRequest request) {
        Showtime showtime = getShowtimeById(id);

        // Update movie if provided
        if (request.getMovieId() != null) {
            Movie movie = movieRepository.findById(request.getMovieId())
                    .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + request.getMovieId()));
            showtime.setMovie(movie);
        }

        // Update screen if provided
        if (request.getScreenId() != null) {
            Screen screen = screenRepository.findById(request.getScreenId())
                    .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + request.getScreenId()));
            showtime.setScreen(screen);
            ScreenLayoutVersion layoutVersion = resolveCurrentLayoutVersion(screen);
            showtime.setLayoutVersion(layoutVersion);
            int totalSeats = seatRepository.findActiveByScreenIdAndLayoutVersionId(screen.getId(), layoutVersion.getId()).size();
            showtime.setTotalSeats(totalSeats);
            showtime.setAvailableSeats(totalSeats);
        }

        // Update other fields if provided
        if (request.getStartTime() != null) {
            showtime.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            showtime.setEndTime(request.getEndTime());
        }
        if (request.getBasePrice() != null) {
            showtime.setBasePrice(request.getBasePrice());
        }
        if (request.getStatus() != null) {
            showtime.setStatus(request.getStatus());
        }

        Showtime savedShowtime = showtimeRepository.save(showtime);
        redisSeatMapCacheService.evict(savedShowtime.getId());
        return savedShowtime;
    }

    public void deleteShowtime(Long id) {
        Showtime showtime = getShowtimeById(id);
        showtime.setStatus(ShowtimeStatus.CANCELLED);
        showtimeRepository.save(showtime);
        redisSeatMapCacheService.evict(id);
    }

    public GetAvailabilityResponse checkAvailability(Long showtimeId) {
        // First check if the showtime exists
        Showtime showtime = loadShowtimeById(showtimeId);

        // get showtime's screen
        Screen screen = showtime.getScreen();

        // then get its seats
        List<Seat> seats = getSeatsForShowtime(showtime);

        // then its availability and return the response
        return getSeatsAvailability(showtimeId, seats);
    }

    public SeatMapResponse getSeatMap(Long showtimeId) {
        return redisSeatMapCacheService.get(showtimeId)
                .orElseGet(() -> buildAndCacheSeatMap(showtimeId));
    }

    private SeatMapResponse buildAndCacheSeatMap(Long showtimeId) {
        Showtime showtime = loadShowtimeById(showtimeId);
        Screen screen = showtime.getScreen();
        Movie movie = showtime.getMovie();
        List<Seat> seats = getSeatsForShowtime(showtime);

        Set<Long> reservedSeatIds = new HashSet<>(reservationRepository.findReservedSeatIdsForShowtime(showtimeId));
        Set<Long> lockedSeatIds = new HashSet<>(redisSeatLockService.findLockedSeatIdsForShowtime(showtimeId));

        List<SeatMapResponse.SeatSummary> seatSummaries = seats.stream()
                .sorted(Comparator
                        .comparing(Seat::getRowLabel)
                        .thenComparing(Seat::getSeatNumber)
                        .thenComparing(Seat::getId))
                .map(seat -> new SeatMapResponse.SeatSummary(
                        seat.getId(),
                        seat.getRowLabel(),
                        seat.getSeatNumber(),
                        seat.getSeatType(),
                        seat.getBasePrice(),
                        !reservedSeatIds.contains(seat.getId()) && !lockedSeatIds.contains(seat.getId())
                ))
                .toList();

        SeatMapResponse seatMap = new SeatMapResponse(
                showtime.getId(),
                showtime.getStatus(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                new SeatMapResponse.MovieSummary(
                        movie.getId(),
                        movie.getTitle(),
                        movie.getDirector()
                ),
                new SeatMapResponse.ScreenSummary(
                        screen.getId(),
                        screen.getName(),
                        screen.getScreenType()
                ),
                seatSummaries
        );

        redisSeatMapCacheService.put(showtimeId, seatMap);
        return seatMap;
    }

    private Showtime loadShowtimeById(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + showtimeId));
    }

    private List<Seat> getSeatsForShowtime(Showtime showtime) {
        Long layoutVersionId = showtime.getLayoutVersion() != null ? showtime.getLayoutVersion().getId() : null;
        return seatRepository.findActiveByScreenIdAndLayoutVersionId(showtime.getScreen().getId(), layoutVersionId);
    }

    private ScreenLayoutVersion resolveCurrentLayoutVersion(Screen screen) {
        if (screen.getCurrentLayoutVersion() != null) {
            return screen.getCurrentLayoutVersion();
        }

        ScreenLayoutVersion layoutVersion = screenLayoutVersionRepository
                .findFirstByScreenIdOrderByVersionNumberDesc(screen.getId())
                .orElseGet(() -> screenLayoutVersionRepository.save(new ScreenLayoutVersion(screen, 1)));
        screen.setCurrentLayoutVersion(layoutVersion);
        screenRepository.save(screen);
        return layoutVersion;
    }

    private GetAvailabilityResponse getSeatsAvailability(Long showtimeId, List<Seat> seats) {
        Set<Long> reservedSeatIds = new HashSet<>(reservationRepository.findReservedSeatIdsForShowtime(showtimeId));
        Set<Long> lockedSeatIds = new HashSet<>(redisSeatLockService.findLockedSeatIdsForShowtime(showtimeId));
        List<SeatAvailabilityDto> availability = seats.stream()
            .map(seat -> {
                boolean available = !reservedSeatIds.contains(seat.getId())
                        && !lockedSeatIds.contains(seat.getId());
                return new SeatAvailabilityDto(seat.getId(), available);
            })
            .toList();
        
        return new GetAvailabilityResponse(showtimeId, availability);
    }

    private ShowtimeSummaryResponse toShowtimeSummaryResponse(Showtime showtime) {
        Movie movie = showtime.getMovie();
        Screen screen = showtime.getScreen();
        var theatre = screen.getTheatre();

        return new ShowtimeSummaryResponse(
                showtime.getId(),
                new ShowtimeSummaryResponse.MovieSummary(
                        movie.getId(),
                        movie.getTitle(),
                        movie.getDirector()
                ),
                new ShowtimeSummaryResponse.TheatreSummary(
                        theatre.getId(),
                        theatre.getName(),
                        theatre.getCity(),
                        theatre.getCountry()
                ),
                new ShowtimeSummaryResponse.ScreenSummary(
                        screen.getId(),
                        screen.getName(),
                        screen.getScreenType()
                ),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getBasePrice(),
                showtime.getAvailableSeats(),
                showtime.getTotalSeats(),
                showtime.getStatus()
        );
    }
}
