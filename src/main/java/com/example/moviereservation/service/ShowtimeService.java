package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.ShowtimeRequest;
import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.entity.ShowtimeStatus;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.SeatLockRepository;
import com.example.moviereservation.dto.GetAvailabilityResponse;
import com.example.moviereservation.dto.SeatAvailabilityDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
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
    private ReservationRepository reservationRepository;

    @Autowired
    private SeatLockRepository seatLockRepository;

    public List<Showtime> getAllShowtimes() {
        return showtimeRepository.findAll();
    }

    public Showtime getShowtimeById(Long id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + id));
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
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(request.getEndTime());
        showtime.setBasePrice(request.getBasePrice());
        showtime.setTotalSeats(screen.getTotalSeats());
        showtime.setAvailableSeats(screen.getTotalSeats());
        showtime.setStatus(request.getStatus() != null ? request.getStatus() : ShowtimeStatus.UPCOMING);

        return showtimeRepository.save(showtime);
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
            showtime.setTotalSeats(screen.getTotalSeats());
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

        return showtimeRepository.save(showtime);
    }

    public void deleteShowtime(Long id) {
        Showtime showtime = getShowtimeById(id);
        showtimeRepository.delete(showtime);
    }

    public GetAvailabilityResponse checkAvailability(Long showtimeId) {
        // First check if the showtime exists
        Showtime showtime = loadShowtimeById(showtimeId);

        // get showtime's screen
        Screen screen = showtime.getScreen();

        // then get its seats
        List<Seat> seats = getSeatsForScreenId(screen.getId());

        // then its availability and return the response
        return getSeatsAvailability(showtimeId, seats);
    }

    private Showtime loadShowtimeById(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + showtimeId));
    }

    private List<Seat> getSeatsForScreenId(Long screenId) {
        return seatRepository.findByScreenId(screenId);
    }

    private GetAvailabilityResponse getSeatsAvailability(Long showtimeId, List<Seat> seats) {
        Set<Long> reservedSeatIds = new HashSet<>(reservationRepository.findReservedSeatIdsForShowtime(showtimeId));
        Set<Long> lockedSeatIds = new HashSet<>(seatLockRepository.findUnavailableLockedSeatIdsForShowtime(showtimeId));
        List<SeatAvailabilityDto> availability = seats.stream()
            .map(seat -> {
                boolean available = !reservedSeatIds.contains(seat.getId())
                        && !lockedSeatIds.contains(seat.getId());
                return new SeatAvailabilityDto(seat.getId(), available);
            })
            .toList();
        
        return new GetAvailabilityResponse(showtimeId, availability);
    }
}
