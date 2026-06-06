package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.SeatRequest;
import com.example.moviereservation.dto.SeatResponse;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.entity.ScreenLayoutVersion;
import com.example.moviereservation.entity.Theatre;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.ScreenLayoutVersionRepository;
import com.example.moviereservation.repository.TheatreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatService {
    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private ScreenLayoutVersionRepository screenLayoutVersionRepository;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private RedisSeatMapCacheService redisSeatMapCacheService;

    public List<Seat> getAllSeats() {
        return seatRepository.findAll();
    }

    public List<SeatResponse> getSeatResponses() {
        return seatRepository.findAll().stream()
                .map(this::toSeatResponse)
                .toList();
    }

    public Seat getSeatById(Long id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
    }

    public SeatResponse getSeatResponse(Long id) {
        return toSeatResponse(getSeatById(id));
    }

    public Seat createSeat(SeatRequest request) {
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + request.getScreenId()));

        Seat seat = new Seat();
        seat.setScreen(screen);
        seat.setLayoutVersion(resolveCurrentLayoutVersion(screen));
        seat.setRowLabel(request.getRowLabel());
        seat.setSeatNumber(request.getSeatNumber());
        seat.setSeatType(request.getSeatType());
        seat.setBasePrice(request.getBasePrice());
        seat.setActive(true);

        Seat savedSeat = seatRepository.save(seat);
        recalculateCounts(savedSeat.getScreen().getId());
        evictSeatMapsForScreen(savedSeat.getScreen().getId());
        return savedSeat;
    }

    public Seat updateSeat(Long id, SeatRequest request) {
        Seat seat = getSeatById(id);
        Long originalScreenId = seat.getScreen().getId();

        // Update screen if provided
        if (request.getScreenId() != null) {
            Screen screen = screenRepository.findById(request.getScreenId())
                    .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + request.getScreenId()));
            seat.setScreen(screen);
            seat.setLayoutVersion(resolveCurrentLayoutVersion(screen));
        }

        // Update other fields if provided
        if (request.getRowLabel() != null) {
            seat.setRowLabel(request.getRowLabel());
        }
        if (request.getSeatNumber() != null) {
            seat.setSeatNumber(request.getSeatNumber());
        }
        if (request.getSeatType() != null) {
            seat.setSeatType(request.getSeatType());
        }
        if (request.getBasePrice() != null) {
            seat.setBasePrice(request.getBasePrice());
        }

        Seat savedSeat = seatRepository.save(seat);
        recalculateCounts(originalScreenId);
        recalculateCounts(savedSeat.getScreen().getId());
        evictSeatMapsForScreen(originalScreenId);
        evictSeatMapsForScreen(savedSeat.getScreen().getId());
        return savedSeat;
    }

    public void deleteSeat(Long id) {
        Seat seat = getSeatById(id);
        Long screenId = seat.getScreen().getId();
        seat.setActive(false);
        seatRepository.save(seat);
        recalculateCounts(screenId);
        evictSeatMapsForScreen(screenId);
    }

    public SeatResponse toSeatResponse(Seat seat) {
        Screen screen = seat.getScreen();

        return new SeatResponse(
                seat.getId(),
                new SeatResponse.ScreenSummary(
                        screen.getId(),
                        screen.getName(),
                        screen.getScreenType()
                ),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getSeatType(),
                seat.getBasePrice(),
                seat.isActive()
        );
    }

    private void evictSeatMapsForScreen(Long screenId) {
        showtimeRepository.findAllByScreenId(screenId)
                .forEach(showtime -> redisSeatMapCacheService.evict(showtime.getId()));
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

    private void recalculateCounts(Long screenId) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + screenId));
        Long currentLayoutVersionId = screen.getCurrentLayoutVersion() != null
                ? screen.getCurrentLayoutVersion().getId()
                : null;
        int activeSeats = currentLayoutVersionId == null
                ? 0
                : (int) seatRepository.countByScreenIdAndLayoutVersionIdAndActiveTrue(screenId, currentLayoutVersionId);
        screen.setTotalSeats(activeSeats);
        screenRepository.save(screen);

        Theatre theatre = screen.getTheatre();
        theatre.setTotalScreens((int) screenRepository.countByTheatreIdAndActiveTrue(theatre.getId()));
        theatre.setTotalSeats((int) seatRepository.countActiveCurrentLayoutSeatsByTheatreId(theatre.getId()));
        theatreRepository.save(theatre);
    }
}
