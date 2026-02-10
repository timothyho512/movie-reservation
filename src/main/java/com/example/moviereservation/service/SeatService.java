package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.SeatRequest;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatService {
    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ScreenRepository screenRepository;

    public List<Seat> getAllSeats() {
        return seatRepository.findAll();
    }

    public Seat getSeatById(Long id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
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

        return seatRepository.save(seat);
    }

    public Seat updateSeat(Long id, SeatRequest request) {
        Seat seat = getSeatById(id);

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

        return seatRepository.save(seat);
    }

    public void deleteSeat(Long id) {
        Seat seat = getSeatById(id);
        seatRepository.delete(seat);
    }
}
