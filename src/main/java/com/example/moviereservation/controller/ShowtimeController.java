package com.example.moviereservation.controller;

import com.example.moviereservation.dto.GetAvailabilityResponse;
import com.example.moviereservation.dto.SeatMapResponse;
import com.example.moviereservation.dto.ShowtimeRequest;
import com.example.moviereservation.dto.ShowtimeSummaryResponse;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.service.ShowtimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeController {
    @Autowired
    private ShowtimeService showtimeService;

    // GET /api/showtimes - Get all showtimes
    @GetMapping
    public ResponseEntity<List<ShowtimeSummaryResponse>> getAllShowtimes() {
        return ResponseEntity.ok(showtimeService.getShowtimeSummaries());
    }

    // Get /api/showtimes/{id} - Get showtime by ID
    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeSummaryResponse> getShowtimeByid(@PathVariable Long id) {
        return ResponseEntity.ok(showtimeService.getShowtimeSummary(id));
    }

    // POST /api/showtimes = Create new showtime
    @PostMapping
    public ResponseEntity<Showtime> createShowtime(@RequestBody ShowtimeRequest request) {
        Showtime showtime = showtimeService.createShowtime(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(showtime);
    }

    // Put /api/showtimes/{id} - Update showtime
    @PutMapping("/{id}")
    public ResponseEntity<Showtime> updateShowtime(@PathVariable Long id, @RequestBody ShowtimeRequest request) {
        Showtime showtime = showtimeService.updateShowtime(id, request);
        return ResponseEntity.ok(showtime);
    }

    // DELETE /api/showtimes/{id} - Delete showtime
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShowtime(@PathVariable Long id) {
        showtimeService.deleteShowtime(id);
        return ResponseEntity.noContent().build();
    }


    // Business logic, check availability
    @GetMapping("/{id}/available-seats")
    public ResponseEntity<GetAvailabilityResponse> checkAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(showtimeService.checkAvailability(id));
    }

    @GetMapping("/{id}/seat-map")
    public ResponseEntity<SeatMapResponse> getSeatMap(@PathVariable Long id) {
        return ResponseEntity.ok(showtimeService.getSeatMap(id));
    }
}
