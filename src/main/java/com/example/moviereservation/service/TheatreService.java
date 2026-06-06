package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.TheatreDetailResponse;
import com.example.moviereservation.dto.TheatreRequest;
import com.example.moviereservation.dto.TheatreSummaryResponse;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Theatre;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.TheatreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TheatreService {
    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private ScreenRepository screenRepository;

    public List<Theatre> getAllTheatres() {
        return theatreRepository.findAll();
    }

    public List<TheatreSummaryResponse> getTheatreSummaries() {
        return theatreRepository.findAll().stream()
                .map(this::toTheatreSummaryResponse)
                .toList();
    }

    public Theatre getTheatreById(Long id) {
        return theatreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + id));
    }

    public TheatreDetailResponse getTheatreDetail(Long id) {
        Theatre theatre = getTheatreById(id);
        List<TheatreDetailResponse.ScreenSummary> screens = screenRepository.findAllByTheatreIdOrderByNameAsc(id).stream()
                .map(this::toScreenSummary)
                .toList();

        return new TheatreDetailResponse(
                theatre.getId(),
                theatre.getName(),
                theatre.getAddress(),
                theatre.getCity(),
                theatre.getState(),
                theatre.getCountry(),
                theatre.getPostalCode(),
                theatre.getPhoneNumber(),
                theatre.getTotalScreens(),
                theatre.getTotalSeats(),
                theatre.isActive(),
                screens
        );
    }

    public Theatre createTheatre(TheatreRequest request) {
        Theatre theatre = new Theatre();
        theatre.setName(request.getName());
        theatre.setAddress(request.getAddress());
        theatre.setCity(request.getCity());
        theatre.setState(request.getState());
        theatre.setCountry(request.getCountry());
        theatre.setPostalCode(request.getPostalCode());
        theatre.setPhoneNumber(request.getPhoneNumber());
        theatre.setTotalScreens(request.getTotalScreens() != null ? request.getTotalScreens() : 0);
        theatre.setTotalSeats(request.getTotalSeats() != null ? request.getTotalSeats() : 0);
        theatre.setActive(true);

        return theatreRepository.save(theatre);
    }

    public Theatre updateTheatre(Long id, TheatreRequest request) {
        Theatre theatre = getTheatreById(id);

        if (request.getName() != null) {
            theatre.setName(request.getName());
        }
        if (request.getAddress() != null) {
            theatre.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            theatre.setCity(request.getCity());
        }
        if (request.getState() != null) {
            theatre.setState(request.getState());
        }
        if (request.getCountry() != null) {
            theatre.setCountry(request.getCountry());
        }
        if (request.getPostalCode() != null) {
            theatre.setPostalCode(request.getPostalCode());
        }
        if (request.getPhoneNumber() != null) {
            theatre.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getTotalScreens() != null) {
            theatre.setTotalScreens(request.getTotalScreens());
        }
        if (request.getTotalSeats() != null) {
            theatre.setTotalSeats(request.getTotalSeats());
        }

        theatre.setUpdatedAt(LocalDateTime.now());
        return theatreRepository.save(theatre);
    }

    public void deleteTheatre(Long id) {
        Theatre theatre = getTheatreById(id);
        theatre.setActive(false);
        theatre.setUpdatedAt(LocalDateTime.now());
        theatreRepository.save(theatre);
    }

    private TheatreSummaryResponse toTheatreSummaryResponse(Theatre theatre) {
        return new TheatreSummaryResponse(
                theatre.getId(),
                theatre.getName(),
                theatre.getAddress(),
                theatre.getCity(),
                theatre.getState(),
                theatre.getCountry(),
                theatre.getPostalCode(),
                theatre.getPhoneNumber(),
                theatre.getTotalScreens(),
                theatre.getTotalSeats(),
                theatre.isActive()
        );
    }

    private TheatreDetailResponse.ScreenSummary toScreenSummary(Screen screen) {
        return new TheatreDetailResponse.ScreenSummary(
                screen.getId(),
                screen.getName(),
                screen.getTotalSeats(),
                screen.getScreenType(),
                screen.isActive()
        );
    }
}
