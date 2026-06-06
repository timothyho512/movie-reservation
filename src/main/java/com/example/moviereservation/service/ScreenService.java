package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.ScreenRequest;
import com.example.moviereservation.dto.ScreenResponse;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.ScreenLayoutVersion;
import com.example.moviereservation.entity.Theatre;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.ScreenLayoutVersionRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.TheatreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScreenService {
    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private ScreenLayoutVersionRepository screenLayoutVersionRepository;

    @Autowired
    private SeatRepository seatRepository;

    public List<Screen> getAllScreens() {
        return screenRepository.findAll();
    }

    public List<ScreenResponse> getScreenResponses() {
        return screenRepository.findAll().stream()
                .map(this::toScreenResponse)
                .toList();
    }

    public Screen getScreenById(Long id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + id));
    }

    public ScreenResponse getScreenResponse(Long id) {
        return toScreenResponse(getScreenById(id));
    }

    public Screen createScreen(ScreenRequest request) {
        // Find the theatre by ID
        Theatre theatre = theatreRepository.findById(request.getTheatreId())
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + request.getTheatreId()));

        Screen screen = new Screen();
        screen.setName(request.getName());
        screen.setTheatre(theatre);
        screen.setTotalSeats(request.getTotalSeats() != null ? request.getTotalSeats() : 0);
        screen.setScreenType(request.getScreenType());
        screen.setActive(true);

        Screen savedScreen = screenRepository.save(screen);
        ScreenLayoutVersion layoutVersion = screenLayoutVersionRepository.save(new ScreenLayoutVersion(savedScreen, 1));
        savedScreen.setCurrentLayoutVersion(layoutVersion);
        savedScreen = screenRepository.save(savedScreen);
        recalculateTheatreStats(theatre.getId());
        return savedScreen;
    }

    public Screen updateScreen(Long id, ScreenRequest request) {
        Screen screen = getScreenById(id);

        // Update theatre if provided
        if (request.getTheatreId() != null) {
            Theatre theatre = theatreRepository.findById(request.getTheatreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + request.getTheatreId()));
            screen.setTheatre(theatre);
        }

        // Update other fields if provided
        if (request.getName() != null) {
            screen.setName(request.getName());
        }
        if (request.getScreenType() != null) {
            screen.setScreenType(request.getScreenType());
        }

        Screen savedScreen = screenRepository.save(screen);
        recalculateTheatreStats(savedScreen.getTheatre().getId());
        return savedScreen;
    }

    public void deleteScreen(Long id) {
        Screen screen = getScreenById(id);
        screen.setActive(false);
        Screen savedScreen = screenRepository.save(screen);
        recalculateTheatreStats(savedScreen.getTheatre().getId());
    }

    public ScreenResponse toScreenResponse(Screen screen) {
        Theatre theatre = screen.getTheatre();

        return new ScreenResponse(
                screen.getId(),
                screen.getName(),
                new ScreenResponse.TheatreSummary(
                        theatre.getId(),
                        theatre.getName(),
                        theatre.getCity(),
                        theatre.getCountry()
                ),
                screen.getTotalSeats(),
                screen.getScreenType(),
                screen.isActive()
        );
    }

    private void recalculateTheatreStats(Long theatreId) {
        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + theatreId));
        theatre.setTotalScreens((int) screenRepository.countByTheatreIdAndActiveTrue(theatreId));
        theatre.setTotalSeats((int) seatRepository.countActiveCurrentLayoutSeatsByTheatreId(theatreId));
        theatreRepository.save(theatre);
    }
}
