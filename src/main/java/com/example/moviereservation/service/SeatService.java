package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.SeatRequest;
import com.example.moviereservation.dto.SeatResponse;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
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
        seat.setRowLabel(request.getRowLabel());
        seat.setSeatNumber(request.getSeatNumber());
        seat.setSeatType(request.getSeatType());
        seat.setBasePrice(request.getBasePrice());
        seat.setActive(true);

        Seat savedSeat = seatRepository.save(seat);
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
        evictSeatMapsForScreen(originalScreenId);
        evictSeatMapsForScreen(savedSeat.getScreen().getId());
        return savedSeat;
    }

    public void deleteSeat(Long id) {
        Seat seat = getSeatById(id);
        Long screenId = seat.getScreen().getId();
        seatRepository.delete(seat);
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
}
