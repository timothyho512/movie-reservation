package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.TheatreRequest;
import com.example.moviereservation.entity.Theatre;
import com.example.moviereservation.repository.TheatreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TheatreService {
    @Autowired
    private TheatreRepository theatreRepository;

    public List<Theatre> getAllTheatres() {
        return theatreRepository.findAll();
    }

    public Theatre getTheatreById(Long id) {
        return theatreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + id));
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
        theatre.setTotalScreens(request.getTotalScreens());
        theatre.setTotalSeats(request.getTotalSeats());
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
        theatreRepository.delete(theatre);
    }
}
