package com.example.moviereservation.service;

import com.example.moviereservation.config.BookingProperties;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.ShowtimeStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BookingWindowService {
    private final BookingProperties bookingProperties;

    public BookingWindowService(BookingProperties bookingProperties) {
        this.bookingProperties = bookingProperties;
    }

    public LocalDateTime bookingCutoffFrom(LocalDateTime now) {
        return now.plusMinutes(bookingProperties.getCutoffMinutes());
    }

    public boolean isBookable(Showtime showtime, LocalDateTime now) {
        return showtime.getStatus() == ShowtimeStatus.UPCOMING
                && showtime.getStartTime().isAfter(bookingCutoffFrom(now));
    }

    public long cutoffMinutes() {
        return bookingProperties.getCutoffMinutes();
    }
}
