package com.example.moviereservation.controller;

import com.example.moviereservation.dto.*;
import com.example.moviereservation.service.AdminReportService;
import com.example.moviereservation.service.AdminReportService.ReportFilter;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {
    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping("/showtimes/occupancy")
    public Page<ShowtimeOccupancyReportRow> getShowtimeOccupancy(@ModelAttribute ReportQuery query) {
        return adminReportService.getShowtimeOccupancy(query.toFilter());
    }

    @GetMapping("/movies/revenue")
    public Page<MovieRevenueReportRow> getMovieRevenue(@ModelAttribute ReportQuery query) {
        return adminReportService.getMovieRevenue(query.toFilter());
    }

    @GetMapping("/bookings/cancelled")
    public Page<CancelledBookingReportRow> getCancelledBookings(@ModelAttribute ReportQuery query) {
        return adminReportService.getCancelledBookings(query.toFilter());
    }

    @GetMapping("/seats/popular")
    public Page<PopularSeatReportRow> getPopularSeats(@ModelAttribute ReportQuery query) {
        return adminReportService.getPopularSeats(query.toFilter());
    }

    @GetMapping("/checkout/conversion")
    public Page<CheckoutConversionReportRow> getCheckoutConversion(@ModelAttribute ReportQuery query) {
        return adminReportService.getCheckoutConversion(query.toFilter());
    }

    public static class ReportQuery {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime from;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime to;

        private Long movieId;
        private Long theatreId;
        private Long screenId;
        private Long showtimeId;
        private int page = 0;
        private int size = 20;
        private String sort;

        public ReportFilter toFilter() {
            return new ReportFilter(from, to, movieId, theatreId, screenId, showtimeId, page, size, sort);
        }

        public LocalDateTime getFrom() {
            return from;
        }

        public void setFrom(LocalDateTime from) {
            this.from = from;
        }

        public LocalDateTime getTo() {
            return to;
        }

        public void setTo(LocalDateTime to) {
            this.to = to;
        }

        public Long getMovieId() {
            return movieId;
        }

        public void setMovieId(Long movieId) {
            this.movieId = movieId;
        }

        public Long getTheatreId() {
            return theatreId;
        }

        public void setTheatreId(Long theatreId) {
            this.theatreId = theatreId;
        }

        public Long getScreenId() {
            return screenId;
        }

        public void setScreenId(Long screenId) {
            this.screenId = screenId;
        }

        public Long getShowtimeId() {
            return showtimeId;
        }

        public void setShowtimeId(Long showtimeId) {
            this.showtimeId = showtimeId;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public String getSort() {
            return sort;
        }

        public void setSort(String sort) {
            this.sort = sort;
        }
    }
}
