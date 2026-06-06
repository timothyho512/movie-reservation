package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.AdminSeatLayoutRequest;
import com.example.moviereservation.dto.AdminSeatLayoutResponse;
import com.example.moviereservation.dto.SeatResponse;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.ScreenLayoutVersion;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.entity.Theatre;
import com.example.moviereservation.repository.ScreenLayoutVersionRepository;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.TheatreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminSeatLayoutService {
    private final ScreenRepository screenRepository;
    private final ScreenLayoutVersionRepository screenLayoutVersionRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TheatreRepository theatreRepository;
    private final RedisSeatMapCacheService redisSeatMapCacheService;
    private final SeatService seatService;

    public AdminSeatLayoutService(
            ScreenRepository screenRepository,
            ScreenLayoutVersionRepository screenLayoutVersionRepository,
            SeatRepository seatRepository,
            ShowtimeRepository showtimeRepository,
            TheatreRepository theatreRepository,
            RedisSeatMapCacheService redisSeatMapCacheService,
            SeatService seatService
    ) {
        this.screenRepository = screenRepository;
        this.screenLayoutVersionRepository = screenLayoutVersionRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
        this.theatreRepository = theatreRepository;
        this.redisSeatMapCacheService = redisSeatMapCacheService;
        this.seatService = seatService;
    }

    @Transactional
    public AdminSeatLayoutResponse replaceLayout(Long screenId, AdminSeatLayoutRequest request) {
        if (request.getSeats() == null || request.getSeats().isEmpty()) {
            throw new IllegalArgumentException("Seat layout must include at least one seat");
        }

        validateSeatDefinitions(request.getSeats());

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + screenId));

        ScreenLayoutVersion previousLayoutVersion = screen.getCurrentLayoutVersion();
        int nextVersion = screenLayoutVersionRepository.findMaxVersionNumberForScreen(screenId) + 1;
        ScreenLayoutVersion layoutVersion = screenLayoutVersionRepository.save(new ScreenLayoutVersion(screen, nextVersion));

        List<Seat> seats = request.getSeats().stream()
                .map(definition -> {
                    Seat seat = new Seat();
                    seat.setScreen(screen);
                    seat.setLayoutVersion(layoutVersion);
                    seat.setRowLabel(definition.getRowLabel().trim().toUpperCase());
                    seat.setSeatNumber(definition.getSeatNumber());
                    seat.setSeatType(definition.getSeatType());
                    seat.setBasePrice(definition.getBasePrice());
                    seat.setActive(true);
                    return seat;
                })
                .toList();

        List<Seat> savedSeats = seatRepository.saveAll(seats);

        if (previousLayoutVersion != null) {
            previousLayoutVersion.setActive(false);
            screenLayoutVersionRepository.save(previousLayoutVersion);
        }
        screen.setCurrentLayoutVersion(layoutVersion);
        screen.setTotalSeats(savedSeats.size());
        screenRepository.save(screen);

        Theatre theatre = screen.getTheatre();
        theatre.setTotalScreens((int) screenRepository.countByTheatreIdAndActiveTrue(theatre.getId()));
        theatre.setTotalSeats((int) seatRepository.countActiveCurrentLayoutSeatsByTheatreId(theatre.getId()));
        theatreRepository.save(theatre);

        showtimeRepository.findAllByScreenId(screenId)
                .forEach(showtime -> redisSeatMapCacheService.evict(showtime.getId()));

        List<SeatResponse> seatResponses = savedSeats.stream()
                .map(seatService::toSeatResponse)
                .toList();

        return new AdminSeatLayoutResponse(
                screen.getId(),
                layoutVersion.getId(),
                layoutVersion.getVersionNumber(),
                savedSeats.size(),
                seatResponses
        );
    }

    @Transactional(readOnly = true)
    public AdminSeatLayoutResponse getCurrentLayout(Long screenId) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + screenId));
        ScreenLayoutVersion layoutVersion = screen.getCurrentLayoutVersion();
        if (layoutVersion == null) {
            throw new ResourceNotFoundException("Screen has no current seat layout");
        }
        List<Seat> seats = seatRepository.findActiveByScreenIdAndLayoutVersionId(screenId, layoutVersion.getId());
        return new AdminSeatLayoutResponse(
                screenId,
                layoutVersion.getId(),
                layoutVersion.getVersionNumber(),
                seats.size(),
                seats.stream().map(seatService::toSeatResponse).toList()
        );
    }

    private void validateSeatDefinitions(List<AdminSeatLayoutRequest.SeatDefinition> seats) {
        Set<String> seenPositions = new HashSet<>();
        for (AdminSeatLayoutRequest.SeatDefinition seat : seats) {
            if (seat.getRowLabel() == null || seat.getRowLabel().isBlank()) {
                throw new IllegalArgumentException("Seat rowLabel is required");
            }
            if (seat.getSeatNumber() == null || seat.getSeatNumber() <= 0) {
                throw new IllegalArgumentException("Seat seatNumber must be greater than zero");
            }
            if (seat.getSeatType() == null) {
                throw new IllegalArgumentException("Seat seatType is required");
            }
            if (seat.getBasePrice() == null || seat.getBasePrice().signum() < 0) {
                throw new IllegalArgumentException("Seat basePrice must be zero or greater");
            }

            String position = seat.getRowLabel().trim().toUpperCase() + ":" + seat.getSeatNumber();
            if (!seenPositions.add(position)) {
                throw new IllegalArgumentException("Duplicate seat position: " + position);
            }
        }
    }
}
