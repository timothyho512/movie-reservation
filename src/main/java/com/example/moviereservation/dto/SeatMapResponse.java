package com.example.moviereservation.dto;

import com.example.moviereservation.entity.ScreenType;
import com.example.moviereservation.entity.SeatType;
import com.example.moviereservation.entity.ShowtimeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class SeatMapResponse {
    private Long showtimeId;
    private ShowtimeStatus showtimeStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private MovieSummary movie;
    private ScreenSummary screen;
    private List<SeatSummary> seats;

    public SeatMapResponse() {
    }

    public SeatMapResponse(
            Long showtimeId,
            ShowtimeStatus showtimeStatus,
            LocalDateTime startTime,
            LocalDateTime endTime,
            MovieSummary movie,
            ScreenSummary screen,
            List<SeatSummary> seats
    ) {
        this.showtimeId = showtimeId;
        this.showtimeStatus = showtimeStatus;
        this.startTime = startTime;
        this.endTime = endTime;
        this.movie = movie;
        this.screen = screen;
        this.seats = seats;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
    }

    public ShowtimeStatus getShowtimeStatus() {
        return showtimeStatus;
    }

    public void setShowtimeStatus(ShowtimeStatus showtimeStatus) {
        this.showtimeStatus = showtimeStatus;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public MovieSummary getMovie() {
        return movie;
    }

    public void setMovie(MovieSummary movie) {
        this.movie = movie;
    }

    public ScreenSummary getScreen() {
        return screen;
    }

    public void setScreen(ScreenSummary screen) {
        this.screen = screen;
    }

    public List<SeatSummary> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatSummary> seats) {
        this.seats = seats;
    }

    public static class MovieSummary {
        private Long id;
        private String title;
        private String director;

        public MovieSummary() {
        }

        public MovieSummary(Long id, String title, String director) {
            this.id = id;
            this.title = title;
            this.director = director;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDirector() {
            return director;
        }

        public void setDirector(String director) {
            this.director = director;
        }
    }

    public static class ScreenSummary {
        private Long id;
        private String name;
        private ScreenType screenType;

        public ScreenSummary() {
        }

        public ScreenSummary(Long id, String name, ScreenType screenType) {
            this.id = id;
            this.name = name;
            this.screenType = screenType;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ScreenType getScreenType() {
            return screenType;
        }

        public void setScreenType(ScreenType screenType) {
            this.screenType = screenType;
        }
    }

    public static class SeatSummary {
        private Long id;
        private String rowLabel;
        private Integer seatNumber;
        private SeatType seatType;
        private BigDecimal price;
        private boolean available;

        public SeatSummary() {
        }

        public SeatSummary(
                Long id,
                String rowLabel,
                Integer seatNumber,
                SeatType seatType,
                BigDecimal price,
                boolean available
        ) {
            this.id = id;
            this.rowLabel = rowLabel;
            this.seatNumber = seatNumber;
            this.seatType = seatType;
            this.price = price;
            this.available = available;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getRowLabel() {
            return rowLabel;
        }

        public void setRowLabel(String rowLabel) {
            this.rowLabel = rowLabel;
        }

        public Integer getSeatNumber() {
            return seatNumber;
        }

        public void setSeatNumber(Integer seatNumber) {
            this.seatNumber = seatNumber;
        }

        public SeatType getSeatType() {
            return seatType;
        }

        public void setSeatType(SeatType seatType) {
            this.seatType = seatType;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }
    }
}
