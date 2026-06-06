package com.example.moviereservation.controller;

import com.example.moviereservation.dto.*;
import com.example.moviereservation.entity.ShowtimeStatus;
import com.example.moviereservation.service.AdminManagementService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
public class AdminManagementController {
    private final AdminManagementService service;

    public AdminManagementController(AdminManagementService service) {
        this.service = service;
    }

    @GetMapping("/movies")
    public Page<AdminMovieResponse> movies(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return service.movies(active, search, page, size, sort);
    }

    @PatchMapping("/movies/{id}/status")
    public AdminMovieResponse movieStatus(@PathVariable Long id, @RequestBody ActiveStatusRequest request) {
        return service.setMovieActive(id, request.active());
    }

    @GetMapping("/theatres")
    public Page<AdminTheatreResponse> theatres(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return service.theatres(active, search, page, size, sort);
    }

    @PatchMapping("/theatres/{id}/status")
    public AdminTheatreResponse theatreStatus(@PathVariable Long id, @RequestBody ActiveStatusRequest request) {
        return service.setTheatreActive(id, request.active());
    }

    @GetMapping("/screens")
    public Page<AdminScreenResponse> screens(
            @RequestParam(required = false) Long theatreId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return service.screens(theatreId, active, search, page, size, sort);
    }

    @PatchMapping("/screens/{id}/status")
    public AdminScreenResponse screenStatus(@PathVariable Long id, @RequestBody ActiveStatusRequest request) {
        return service.setScreenActive(id, request.active());
    }

    @GetMapping("/showtimes")
    public Page<AdminShowtimeResponse> showtimes(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long theatreId,
            @RequestParam(required = false) Long screenId,
            @RequestParam(required = false) ShowtimeStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return service.showtimes(movieId, theatreId, screenId, status, from, to, page, size, sort);
    }

    @PatchMapping("/showtimes/{id}/status")
    public AdminShowtimeResponse showtimeStatus(@PathVariable Long id, @RequestBody ShowtimeStatusRequest request) {
        return service.setShowtimeStatus(id, request.status());
    }
}
