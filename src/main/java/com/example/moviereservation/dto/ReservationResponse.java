package com.example.moviereservation.dto;

import com.example.moviereservation.entity.CurrencyCode;
import com.example.moviereservation.entity.PaymentStatus;
import com.example.moviereservation.entity.ReservationStatus;
import com.example.moviereservation.entity.ScreenType;
import com.example.moviereservation.entity.SeatType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationResponse {
    private Long reservationId;
    private String reservationReference;
    private ReservationStatus reservationStatus;
    private PaymentStatus paymentStatus;
    private ShowtimeSummary showtime;
    private MovieSummary movie;
    private ScreenSummary screen;
    private List<SeatSummary> seats;
    private BigDecimal totalAmount;
    private CurrencyCode currency;
    private LocalDateTime createdAt;

    public ReservationResponse() {
    }

    public ReservationResponse(
            Long reservationId,
            String reservationReference,
            ReservationStatus reservationStatus,
            PaymentStatus paymentStatus,
            ShowtimeSummary showtime,
            MovieSummary movie,
            ScreenSummary screen,
            List<SeatSummary> seats,
            BigDecimal totalAmount,
            CurrencyCode currency,
            LocalDateTime createdAt
    ) {
        this.reservationId = reservationId;
        this.reservationReference = reservationReference;
        this.reservationStatus = reservationStatus;
        this.paymentStatus = paymentStatus;
        this.showtime = showtime;
        this.movie = movie;
        this.screen = screen;
        this.seats = seats;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getReservationReference() {
        return reservationReference;
    }

    public void setReservationReference(String reservationReference) {
        this.reservationReference = reservationReference;
    }

    public ReservationStatus getReservationStatus() {
        return reservationStatus;
    }

    public void setReservationStatus(ReservationStatus reservationStatus) {
        this.reservationStatus = reservationStatus;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public ShowtimeSummary getShowtime() {
        return showtime;
    }

    public void setShowtime(ShowtimeSummary showtime) {
        this.showtime = showtime;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyCode currency) {
        this.currency = currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class ShowtimeSummary {
        private Long id;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        public ShowtimeSummary() {
        }

        public ShowtimeSummary(Long id, LocalDateTime startTime, LocalDateTime endTime) {
            this.id = id;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public SeatSummary() {
        }

        public SeatSummary(Long id, String rowLabel, Integer seatNumber, SeatType seatType) {
            this.id = id;
            this.rowLabel = rowLabel;
            this.seatNumber = seatNumber;
            this.seatType = seatType;
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
    }
}
