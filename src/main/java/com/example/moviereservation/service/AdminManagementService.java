package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.*;
import com.example.moviereservation.entity.*;
import com.example.moviereservation.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AdminManagementService {
    private static final Set<String> MOVIE_SORTS = Set.of("title", "director", "active", "createdAt", "updatedAt");
    private static final Set<String> THEATRE_SORTS = Set.of("name", "city", "country", "active", "createdAt", "updatedAt");
    private static final Set<String> SCREEN_SORTS = Set.of("name", "totalSeats", "active", "createdAt", "updatedAt");
    private static final Set<String> SHOWTIME_SORTS = Set.of("startTime", "endTime", "basePrice", "status", "createdAt");

    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final ShowtimeRepository showtimeRepository;
    private final RedisSeatMapCacheService seatMapCacheService;

    public AdminManagementService(
            MovieRepository movieRepository,
            TheatreRepository theatreRepository,
            ScreenRepository screenRepository,
            ShowtimeRepository showtimeRepository,
            RedisSeatMapCacheService seatMapCacheService
    ) {
        this.movieRepository = movieRepository;
        this.theatreRepository = theatreRepository;
        this.screenRepository = screenRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatMapCacheService = seatMapCacheService;
    }

    public Page<AdminMovieResponse> movies(Boolean active, String search, int page, int size, String sort) {
        Specification<Movie> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (active != null) predicates.add(builder.equal(root.get("active"), active));
            if (hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("director")), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return movieRepository.findAll(specification, pageable(page, size, sort, MOVIE_SORTS, "title"))
                .map(this::movieResponse);
    }

    public Page<AdminTheatreResponse> theatres(Boolean active, String search, int page, int size, String sort) {
        Specification<Theatre> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (active != null) predicates.add(builder.equal(root.get("active"), active));
            if (hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("city")), pattern),
                        builder.like(builder.lower(root.get("country")), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return theatreRepository.findAll(specification, pageable(page, size, sort, THEATRE_SORTS, "name"))
                .map(this::theatreResponse);
    }

    public Page<AdminScreenResponse> screens(Long theatreId, Boolean active, String search, int page, int size, String sort) {
        Specification<Screen> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (theatreId != null) predicates.add(builder.equal(root.get("theatre").get("id"), theatreId));
            if (active != null) predicates.add(builder.equal(root.get("active"), active));
            if (hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("theatre").get("name")), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return screenRepository.findAll(specification, pageable(page, size, sort, SCREEN_SORTS, "name"))
                .map(this::screenResponse);
    }

    public Page<AdminShowtimeResponse> showtimes(
            Long movieId,
            Long theatreId,
            Long screenId,
            ShowtimeStatus status,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size,
            String sort
    ) {
        Specification<Showtime> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (movieId != null) predicates.add(builder.equal(root.get("movie").get("id"), movieId));
            if (theatreId != null) predicates.add(builder.equal(root.get("screen").get("theatre").get("id"), theatreId));
            if (screenId != null) predicates.add(builder.equal(root.get("screen").get("id"), screenId));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("startTime"), from));
            if (to != null) predicates.add(builder.lessThanOrEqualTo(root.get("startTime"), to));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return showtimeRepository.findAll(specification, pageable(page, size, sort, SHOWTIME_SORTS, "startTime"))
                .map(this::showtimeResponse);
    }

    @Transactional
    public AdminMovieResponse setMovieActive(Long id, boolean active) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        movie.setActive(active);
        return movieResponse(movieRepository.save(movie));
    }

    @Transactional
    public AdminTheatreResponse setTheatreActive(Long id, boolean active) {
        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + id));
        theatre.setActive(active);
        theatre.setUpdatedAt(LocalDateTime.now());
        return theatreResponse(theatreRepository.save(theatre));
    }

    @Transactional
    public AdminScreenResponse setScreenActive(Long id, boolean active) {
        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + id));
        if (active && !screen.getTheatre().isActive()) {
            throw new IllegalArgumentException("Cannot activate a screen in an inactive theatre");
        }
        screen.setActive(active);
        return screenResponse(screenRepository.save(screen));
    }

    @Transactional
    public AdminShowtimeResponse setShowtimeStatus(Long id, ShowtimeStatus status) {
        if (status == null) throw new IllegalArgumentException("Showtime status is required");
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + id));
        showtime.setStatus(status);
        Showtime saved = showtimeRepository.save(showtime);
        seatMapCacheService.evict(id);
        return showtimeResponse(saved);
    }

    private Pageable pageable(int page, int size, String sort, Set<String> allowed, String defaultProperty) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String property = defaultProperty;
        Sort.Direction direction = Sort.Direction.ASC;
        if (hasText(sort)) {
            String[] parts = sort.split(",", 2);
            if (allowed.contains(parts[0])) property = parts[0];
            if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1])) direction = Sort.Direction.DESC;
        }
        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }

    private AdminMovieResponse movieResponse(Movie movie) {
        return new AdminMovieResponse(movie.getId(), movie.getTitle(), movie.getDirector(), movie.isActive(), movie.getCreatedAt(), movie.getUpdatedAt());
    }

    private AdminTheatreResponse theatreResponse(Theatre theatre) {
        return new AdminTheatreResponse(
                theatre.getId(), theatre.getName(), theatre.getAddress(), theatre.getCity(), theatre.getState(),
                theatre.getCountry(), theatre.getPostalCode(), theatre.getPhoneNumber(), theatre.getTotalScreens(),
                theatre.getTotalSeats(), theatre.isActive(), theatre.getCreatedAt(), theatre.getUpdatedAt()
        );
    }

    private AdminScreenResponse screenResponse(Screen screen) {
        ScreenLayoutVersion layout = screen.getCurrentLayoutVersion();
        return new AdminScreenResponse(
                screen.getId(), screen.getName(), screen.getTheatre().getId(), screen.getTheatre().getName(),
                screen.getTotalSeats(), screen.getScreenType(), screen.isActive(),
                layout != null ? layout.getId() : null, layout != null ? layout.getVersionNumber() : null,
                screen.getCreatedAt(), screen.getUpdatedAt()
        );
    }

    private AdminShowtimeResponse showtimeResponse(Showtime showtime) {
        Screen screen = showtime.getScreen();
        ScreenLayoutVersion layout = showtime.getLayoutVersion();
        return new AdminShowtimeResponse(
                showtime.getId(), showtime.getMovie().getId(), showtime.getMovie().getTitle(),
                screen.getTheatre().getId(), screen.getTheatre().getName(), screen.getId(), screen.getName(),
                screen.getScreenType(), layout != null ? layout.getId() : null,
                layout != null ? layout.getVersionNumber() : null, showtime.getStartTime(), showtime.getEndTime(),
                showtime.getBasePrice(), showtime.getAvailableSeats(), showtime.getTotalSeats(), showtime.getStatus(),
                showtime.getCreatedAt(), showtime.getUpdatedAt()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
