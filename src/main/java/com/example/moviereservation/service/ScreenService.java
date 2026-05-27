package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.ScreenRequest;
import com.example.moviereservation.dto.ScreenResponse;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Theatre;
import com.example.moviereservation.repository.ScreenRepository;
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

        // Create screen with the found theatre
        Screen screen = new Screen();
        screen.setName(request.getName());
        screen.setTheatre(theatre);  // Set the Theatre object
        screen.setTotalSeats(request.getTotalSeats());
        screen.setScreenType(request.getScreenType());
        screen.setActive(true);

        // @PrePersist will set createdAt and updatedAt automatically
        return screenRepository.save(screen);
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
        if (request.getTotalSeats() != null) {
            screen.setTotalSeats(request.getTotalSeats());
        }
        if (request.getScreenType() != null) {
            screen.setScreenType(request.getScreenType());
        }

        // @PreUpdate will update updatedAt automatically
        return screenRepository.save(screen);
    }

    public void deleteScreen(Long id) {
        Screen screen = getScreenById(id);
        screenRepository.delete(screen);
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
}
