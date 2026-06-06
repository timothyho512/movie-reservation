package com.example.moviereservation.controller;

import com.example.moviereservation.dto.AdminSeatLayoutRequest;
import com.example.moviereservation.dto.AdminSeatLayoutResponse;
import com.example.moviereservation.service.AdminSeatLayoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/screens")
public class AdminSeatLayoutController {
    private final AdminSeatLayoutService adminSeatLayoutService;

    public AdminSeatLayoutController(AdminSeatLayoutService adminSeatLayoutService) {
        this.adminSeatLayoutService = adminSeatLayoutService;
    }

    @GetMapping("/{screenId}/seat-layout")
    public ResponseEntity<AdminSeatLayoutResponse> getCurrentSeatLayout(@PathVariable Long screenId) {
        return ResponseEntity.ok(adminSeatLayoutService.getCurrentLayout(screenId));
    }

    @PostMapping("/{screenId}/seat-layout")
    public ResponseEntity<AdminSeatLayoutResponse> replaceSeatLayout(
            @PathVariable Long screenId,
            @RequestBody AdminSeatLayoutRequest request
    ) {
        return ResponseEntity.ok(adminSeatLayoutService.replaceLayout(screenId, request));
    }
}
