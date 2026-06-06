package com.example.moviereservation;

import com.example.moviereservation.dto.ShowtimeRequest;
import com.example.moviereservation.entity.*;
import com.example.moviereservation.repository.*;
import com.example.moviereservation.service.JwtService;
import com.example.moviereservation.service.ShowtimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminReportingIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private ScreenLayoutVersionRepository screenLayoutVersionRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private CheckoutSessionRepository checkoutSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ShowtimeService showtimeService;

    private User admin;
    private User customer;
    private Theatre theatre;
    private Screen screen;
    private ScreenLayoutVersion layoutVersion;
    private Movie movie;
    private Showtime showtime;
    private Seat seat1;
    private Seat seat2;

    @BeforeEach
    void setUp() {
        admin = saveUser("report-admin@example.com", "07000000101", UserRole.ADMIN);
        customer = saveUser("report-customer@example.com", "07000000102", UserRole.CUSTOMER);

        theatre = new Theatre("Report Theatre", "1 Report Road", "London", "England", "UK", "SW1A 1AA");
        theatre.setTotalScreens(1);
        theatre.setTotalSeats(2);
        theatre = theatreRepository.save(theatre);

        screen = screenRepository.save(new Screen("Report Screen", theatre, 2, ScreenType.STANDARD));
        layoutVersion = screenLayoutVersionRepository.save(new ScreenLayoutVersion(screen, 1));
        screen.setCurrentLayoutVersion(layoutVersion);
        screen = screenRepository.save(screen);

        seat1 = new Seat(screen, "A", 1, SeatType.REGULAR, new BigDecimal("2.50"));
        seat1.setLayoutVersion(layoutVersion);
        seat2 = new Seat(screen, "A", 2, SeatType.VIP, new BigDecimal("5.00"));
        seat2.setLayoutVersion(layoutVersion);
        seatRepository.saveAll(List.of(seat1, seat2));

        movie = movieRepository.save(new Movie("Report Movie", "Report Director"));

        showtime = new Showtime(
                movie,
                screen,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                new BigDecimal("10.00")
        );
        showtime.setLayoutVersion(layoutVersion);
        showtime.setTotalSeats(2);
        showtime.setAvailableSeats(1);
        showtime = showtimeRepository.save(showtime);
    }

    @Test
    void customerCannotAccessAdminReports() throws Exception {
        mockMvc.perform(get("/api/admin/reports/showtimes/occupancy")
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportsCalculatePaidCancelledPopularAndCheckoutMetrics() throws Exception {
        Reservation paid = new Reservation(
                customer,
                null,
                showtime,
                List.of(seat1),
                "REPORT-PAID-" + showtime.getId(),
                new BigDecimal("12.50")
        );
        paid.setStatus(ReservationStatus.CONFIRMED);
        paid.setPaymentStatus(PaymentStatus.PAID);
        reservationRepository.save(paid);

        Reservation cancelled = new Reservation(
                customer,
                null,
                showtime,
                List.of(seat2),
                "REPORT-CANCELLED-" + showtime.getId(),
                new BigDecimal("15.00")
        );
        cancelled.setStatus(ReservationStatus.CANCELLED);
        cancelled.setPaymentStatus(PaymentStatus.REFUNDED);
        cancelled.setCancelledAt(LocalDateTime.now());
        reservationRepository.save(cancelled);

        checkoutSessionRepository.save(checkout("REPORT-FINALIZED-" + showtime.getId(), CheckoutSessionStatus.FINALIZED));
        checkoutSessionRepository.save(checkout("REPORT-EXPIRED-" + showtime.getId(), CheckoutSessionStatus.EXPIRED));
        reservationRepository.flush();
        checkoutSessionRepository.flush();

        String token = bearer(admin);

        mockMvc.perform(get("/api/admin/reports/showtimes/occupancy")
                        .param("showtimeId", showtime.getId().toString())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reservedSeats").value(1))
                .andExpect(jsonPath("$.content[0].occupancyRate").value(0.5));

        mockMvc.perform(get("/api/admin/reports/movies/revenue")
                        .param("showtimeId", showtime.getId().toString())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reservationCount").value(1))
                .andExpect(jsonPath("$.content[0].ticketsSold").value(1))
                .andExpect(jsonPath("$.content[0].revenue").value(12.5));

        mockMvc.perform(get("/api/admin/reports/bookings/cancelled")
                        .param("showtimeId", showtime.getId().toString())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].bookingReference").value(cancelled.getBookingReference()));

        mockMvc.perform(get("/api/admin/reports/seats/popular")
                        .param("showtimeId", showtime.getId().toString())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rowLabel").value("A"))
                .andExpect(jsonPath("$.content[0].seatNumber").value(1))
                .andExpect(jsonPath("$.content[0].bookingCount").value(1));

        mockMvc.perform(get("/api/admin/reports/checkout/conversion")
                        .param("showtimeId", showtime.getId().toString())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].checkoutCount").value(2))
                .andExpect(jsonPath("$.content[0].paidCheckoutCount").value(1))
                .andExpect(jsonPath("$.content[0].abandonedCheckoutCount").value(1))
                .andExpect(jsonPath("$.content[0].conversionRate").value(0.5))
                .andExpect(jsonPath("$.content[0].abandonedRate").value(0.5));
    }

    @Test
    void replacingLayoutKeepsExistingShowtimeOnOriginalVersion() throws Exception {
        Long originalLayoutId = showtime.getLayoutVersion().getId();

        mockMvc.perform(post("/api/admin/screens/{screenId}/seat-layout", screen.getId())
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "seats": [
                                    {
                                      "rowLabel": "B",
                                      "seatNumber": 1,
                                      "seatType": "REGULAR",
                                      "basePrice": 3.00
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andExpect(jsonPath("$.totalSeats").value(1));

        Showtime persistedOldShowtime = showtimeRepository.findById(showtime.getId()).orElseThrow();
        assertThat(persistedOldShowtime.getLayoutVersion().getId()).isEqualTo(originalLayoutId);
        assertThat(showtimeService.getSeatMap(showtime.getId()).getSeats()).hasSize(2);

        ShowtimeRequest request = new ShowtimeRequest(
                movie.getId(),
                screen.getId(),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(2),
                new BigDecimal("10.00")
        );
        Showtime newShowtime = showtimeService.createShowtime(request);
        assertThat(newShowtime.getLayoutVersion().getVersionNumber()).isEqualTo(2);
        assertThat(showtimeService.getSeatMap(newShowtime.getId()).getSeats()).hasSize(1);
    }

    private User saveUser(String email, String phoneNumber, UserRole role) {
        User user = new User("Report", role.name(), email, passwordEncoder.encode("password"), phoneNumber);
        user.setRole(role);
        return userRepository.save(user);
    }

    private CheckoutSession checkout(String reference, CheckoutSessionStatus status) {
        CheckoutSession session = new CheckoutSession();
        session.setCheckoutReference(reference);
        session.setShowtime(showtime);
        session.setUser(customer);
        session.setItemsSnapshotJson("[]");
        session.setTotalAmount(new BigDecimal("12.50"));
        session.setCurrency(CurrencyCode.GBP);
        session.setStatus(status);
        session.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        return session;
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
    }
}
