package com.example.moviereservation;


import com.example.moviereservation.entity.LockStatus;
import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.PaymentStatus;
import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.entity.ReservationStatus;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.ScreenType;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.entity.SeatLock;
import com.example.moviereservation.entity.SeatType;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.ShowtimeStatus;
import com.example.moviereservation.entity.Theatre;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.entity.UserRole;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.SeatLockRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.TheatreRepository;
import com.example.moviereservation.repository.UserRepository;
import com.example.moviereservation.service.SeatLockCleanupService;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.moviereservation.entity.CheckoutSession;
import com.example.moviereservation.entity.CheckoutSessionStatus;
import com.example.moviereservation.repository.CheckoutSessionRepository;
import com.example.moviereservation.entity.CurrencyCode;

import com.example.moviereservation.dto.StripeCheckoutSessionResult;
import com.example.moviereservation.dto.StripeCheckoutExpiredEvent;
import com.example.moviereservation.service.StripeCheckoutService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.moviereservation.dto.StripeCheckoutCompletedEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.startsWith;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CheckoutIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SeatLockRepository seatLockRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CheckoutSessionRepository checkoutSessionRepository;

    @Autowired
    private SeatLockCleanupService seatLockCleanupService;


    @MockitoBean
    private StripeCheckoutService stripeCheckoutService; // Mock the StripeCheckoutService to avoid real API calls during tests
    

    private Showtime showtime;
    private Seat seat1;
    private Seat seat2;
    private Seat seat3;
    private User user;

    @BeforeEach
    void setUp() {
        checkoutSessionRepository.deleteAll();
        reservationRepository.deleteAll();
        seatLockRepository.deleteAll();
        seatRepository.deleteAll();
        showtimeRepository.deleteAll();
        screenRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();
        theatreRepository.deleteAll();

        Theatre theatre = new Theatre(
                "Test Theatre",
                "1 Test Street",
                "London",
                "London",
                "UK",
                "SW1A 1AA"
        );
        theatre.setTotalScreens(1);
        theatre.setTotalSeats(50);
        theatre = theatreRepository.save(theatre);

        Screen screen = new Screen("Screen 1", theatre, 50, ScreenType.STANDARD);
        screen = screenRepository.save(screen);

        Movie movie = new Movie("Test Movie", "Test Director");
        movie = movieRepository.save(movie);

        showtime = new Showtime(
                movie,
                screen,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                new BigDecimal("12.50")
        );
        showtime.setStatus(ShowtimeStatus.UPCOMING);
        showtime = showtimeRepository.save(showtime);

        seat1 = seatRepository.save(new Seat(screen, "A", 1, SeatType.REGULAR, new BigDecimal("12.50")));
        seat2 = seatRepository.save(new Seat(screen, "A", 2, SeatType.REGULAR, new BigDecimal("12.50")));
        seat3 = seatRepository.save(new Seat(screen, "A", 3, SeatType.REGULAR, new BigDecimal("12.50")));

        user = new User("Jay", "Doe", "jay@example.com", passwordEncoder.encode("password"), "07123456789");
        user.setRole(UserRole.CUSTOMER);
        user = userRepository.save(user);

        // Mock the StripeCheckoutService to return a predictable result for any checkout session creation
        when(stripeCheckoutService.createHostedCheckoutSession(any()))
        .thenAnswer(invocation -> {
            CheckoutSession checkoutSession = invocation.getArgument(0);
            return new StripeCheckoutSessionResult(
                    "cs_test_" + checkoutSession.getCheckoutReference(),
                    "https://checkout.stripe.test/" + checkoutSession.getCheckoutReference(),
                    "pi_test_" + checkoutSession.getCheckoutReference()
            );
        });
    }

    @Test
    void guestCanLockSeatsSuccessfully() throws Exception {
        JsonNode response = lockAsGuest("guest@example.com", seat1.getId(), seat2.getId());
        
        assertThat(response.get("message").asString()).isEqualTo("Seats locked successfully");
        assertThat(response.get("sessionId").asString()).isNotBlank();
        assertThat(response.get("lockedSeatIds")).hasSize(2);

        List<SeatLock> locks = seatLockRepository.findAll();
        assertThat(locks).hasSize(2);
        assertThat(locks)
                .allMatch(lock -> lock.getGuestEmail().equals("guest@example.com"))
                .allMatch(lock -> lock.getSessionId() != null)
                .allMatch(lock -> lock.getStatus() == LockStatus.LOCKED);
    }

    @Test
    void seatMapReturnsSeatMetadataAndAvailability() throws Exception {
        lockAsGuest("guest@example.com", seat1.getId());

        mockMvc.perform(get("/api/showtimes/{id}/seat-map", showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showtimeId").value(showtime.getId()))
                .andExpect(jsonPath("$.showtimeStatus").value("UPCOMING"))
                .andExpect(jsonPath("$.startTime").isString())
                .andExpect(jsonPath("$.endTime").isString())
                .andExpect(jsonPath("$.movie.id").value(showtime.getMovie().getId()))
                .andExpect(jsonPath("$.movie.title").value("Test Movie"))
                .andExpect(jsonPath("$.movie.director").value("Test Director"))
                .andExpect(jsonPath("$.screen.id").value(showtime.getScreen().getId()))
                .andExpect(jsonPath("$.screen.name").value("Screen 1"))
                .andExpect(jsonPath("$.screen.screenType").value("STANDARD"))
                .andExpect(jsonPath("$.seats[0].id").value(seat1.getId()))
                .andExpect(jsonPath("$.seats[0].rowLabel").value("A"))
                .andExpect(jsonPath("$.seats[0].seatNumber").value(1))
                .andExpect(jsonPath("$.seats[0].seatType").value("REGULAR"))
                .andExpect(jsonPath("$.seats[0].price").value(12.50))
                .andExpect(jsonPath("$.seats[0].available").value(false))
                .andExpect(jsonPath("$.seats[1].id").value(seat2.getId()))
                .andExpect(jsonPath("$.seats[1].available").value(true))
                .andExpect(jsonPath("$.seats[2].id").value(seat3.getId()))
                .andExpect(jsonPath("$.seats[2].available").value(true));
    }

    @Test
    void publicBrowseEndpointsReturnFrontendDtos() throws Exception {
        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(showtime.getMovie().getId()))
                .andExpect(jsonPath("$[0].title").value("Test Movie"))
                .andExpect(jsonPath("$[0].director").value("Test Director"));

        mockMvc.perform(get("/api/movies/{id}", showtime.getMovie().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(showtime.getMovie().getId()))
                .andExpect(jsonPath("$.title").value("Test Movie"))
                .andExpect(jsonPath("$.director").value("Test Director"))
                .andExpect(jsonPath("$.showtimes[0].id").value(showtime.getId()))
                .andExpect(jsonPath("$.showtimes[0].movie.title").value("Test Movie"))
                .andExpect(jsonPath("$.showtimes[0].theatre.name").value("Test Theatre"))
                .andExpect(jsonPath("$.showtimes[0].screen.name").value("Screen 1"))
                .andExpect(jsonPath("$.showtimes[0].basePrice").value(12.50))
                .andExpect(jsonPath("$.showtimes[0].availableSeats").value(50))
                .andExpect(jsonPath("$.showtimes[0].status").value("UPCOMING"));

        mockMvc.perform(get("/api/showtimes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(showtime.getId()))
                .andExpect(jsonPath("$[0].movie.title").value("Test Movie"))
                .andExpect(jsonPath("$[0].theatre.name").value("Test Theatre"))
                .andExpect(jsonPath("$[0].screen.name").value("Screen 1"))
                .andExpect(jsonPath("$[0].basePrice").value(12.50))
                .andExpect(jsonPath("$[0].totalSeats").value(50));

        mockMvc.perform(get("/api/showtimes/{id}", showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(showtime.getId()))
                .andExpect(jsonPath("$.movie.title").value("Test Movie"))
                .andExpect(jsonPath("$.theatre.city").value("London"))
                .andExpect(jsonPath("$.screen.screenType").value("STANDARD"));

        mockMvc.perform(get("/api/theatres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(showtime.getScreen().getTheatre().getId()))
                .andExpect(jsonPath("$[0].name").value("Test Theatre"))
                .andExpect(jsonPath("$[0].city").value("London"))
                .andExpect(jsonPath("$[0].totalScreens").value(1))
                .andExpect(jsonPath("$[0].totalSeats").value(50))
                .andExpect(jsonPath("$[0].active").value(true));

        mockMvc.perform(get("/api/theatres/{id}", showtime.getScreen().getTheatre().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(showtime.getScreen().getTheatre().getId()))
                .andExpect(jsonPath("$.name").value("Test Theatre"))
                .andExpect(jsonPath("$.screens[0].id").value(showtime.getScreen().getId()))
                .andExpect(jsonPath("$.screens[0].name").value("Screen 1"))
                .andExpect(jsonPath("$.screens[0].screenType").value("STANDARD"))
                .andExpect(jsonPath("$.screens[0].active").value(true));
    }

    @Test
    void guestConfirmFailsWithWrongSession() throws Exception {
        lockAsGuest("guest@example.com", seat1.getId());

        mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), "guest@example.com", "wrong-session", "pm_success")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("No valid active lock found for seat " + seat1.getId() + " for this confirmation request"));

        assertThat(reservationRepository.findAll()).isEmpty();
    }

    @Test
    void guestConfirmFailsWithWrongEmail() throws Exception {
        String sessionId = lockAsGuest("guest@example.com", seat1.getId()).get("sessionId").asString();

        mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), "wrong@example.com", sessionId, "pm_success")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("No valid active lock found for seat " + seat1.getId() + " for this confirmation request"));

        assertThat(reservationRepository.findAll()).isEmpty();
    }

    @Test
    void guestConfirmSucceedsWithCorrectSessionAndEmail() throws Exception {
        String guestEmail = "guest@example.com";
        String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

        mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId, "pm_success")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservation confirmed successfully"))
                .andExpect(jsonPath("$.reservationId").isNumber())
                .andExpect(jsonPath("$.bookingReference").isString())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.seatIds[0]").value(seat1.getId()));

        List<Reservation> reservations = reservationRepository.findAll();
        assertThat(reservations).hasSize(1);
        assertThat(reservations.getFirst().getGuestEmail()).isEqualTo(guestEmail);
        assertThat(reservations.getFirst().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservations.getFirst().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(reservations.getFirst().getCurrency()).isEqualTo(CurrencyCode.GBP);

        List<SeatLock> locks = seatLockRepository.findAll();
        assertThat(locks)
                .anyMatch(lock -> lock.getSeat().getId().equals(seat1.getId())
                        && lock.getGuestEmail().equals(guestEmail)
                        && lock.getStatus() == LockStatus.CONVERTED_TO_RESERVATION);
    }

    @Test
        void guestConfirmFailsWhenPaymentFails() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                mockMvc.perform(post("/checkout/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId, "pm_fail")))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value("Payment failed"));

                assertThat(reservationRepository.findAll()).isEmpty();

                List<SeatLock> locks = seatLockRepository.findAll();
                assertThat(locks)
                        .anyMatch(lock -> lock.getSeat().getId().equals(seat1.getId())
                                && lock.getGuestEmail().equals(guestEmail)
                                && lock.getStatus() == LockStatus.LOCKED);
        }

        @Test
        void guestConfirmFailsWhenPaymentTokenMissing() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                mockMvc.perform(post("/checkout/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Payment method token is required"));

                assertThat(reservationRepository.findAll()).isEmpty();
        }



    @Test
    void guestCancelSucceedsAndSeatCanBeLockedAgain() throws Exception {
        String sessionId = lockAsGuest("guest@example.com", seat1.getId()).get("sessionId").asString();

        mockMvc.perform(post("/checkout/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelGuestBody(showtime.getId(), List.of(seat1.getId()), "guest@example.com", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Locks cancelled successfully"));

        List<SeatLock> locks = seatLockRepository.findAll();
        assertThat(locks)
                .anyMatch(lock -> lock.getSeat().getId().equals(seat1.getId())
                        && lock.getStatus() == LockStatus.EXPIRED);

        JsonNode relockResponse = lockAsGuest("another@example.com", seat1.getId());
        assertThat(relockResponse.get("message").asString()).isEqualTo("Seats locked successfully");
    }

    @Test
    void cannotLockAlreadyReservedSeats() throws Exception {
        String sessionId = lockAsGuest("guest@example.com", seat1.getId()).get("sessionId").asString();

        mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), "guest@example.com", sessionId, "pm_success")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/checkout/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockGuestBody(showtime.getId(), List.of(seat1.getId()), "another@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Seat " + seat1.getId() + " is already reserved for this showtime"));
    }

        @Test
        void authenticatedUserCanLockSeatsWithoutRequestUserId() throws Exception {
                String token = loginAndGetToken("jay@example.com", "password");

                mockMvc.perform(post("/checkout/lock")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("Seats locked successfully"))
                        .andExpect(jsonPath("$.lockedSeatIds[0]").value(seat1.getId()));

                List<SeatLock> locks = seatLockRepository.findAll();
                assertThat(locks).hasSize(1);
                assertThat(locks.getFirst().getUser().getId()).isEqualTo(user.getId());
                assertThat(locks.getFirst().getGuestEmail()).isNull();
        }

        @Test
        void authenticatedUserCanConfirmWithoutRequestUserId() throws Exception {
                String token = loginAndGetToken("jay@example.com", "password");

                mockMvc.perform(post("/checkout/lock")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat2.getId())))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/checkout/confirm")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "paymentMethodToken": "pm_success"
                                        }
                                        """.formatted(showtime.getId(), seat2.getId())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("Reservation confirmed successfully"))
                        .andExpect(jsonPath("$.seatIds[0]").value(seat2.getId()));

                List<Reservation> reservations = reservationRepository.findAll();
                assertThat(reservations).hasSize(1);
                assertThat(reservations.getFirst().getUser().getId()).isEqualTo(user.getId());
                assertThat(reservations.getFirst().getCurrency()).isEqualTo(CurrencyCode.GBP);
        }

    @Test
        void authenticatedUserCannotConfirmLockOwnedByAnotherUser() throws Exception {
                User otherUser = new User("Jane", "Doe", "jane@example.com", passwordEncoder.encode("password"), "07999999999");
                otherUser.setRole(UserRole.CUSTOMER);
                otherUser = userRepository.save(otherUser);

                String otherUserToken = loginAndGetToken("jane@example.com", "password");
                String userToken = loginAndGetToken("jay@example.com", "password");

                mockMvc.perform(post("/checkout/lock")
                                .header("Authorization", "Bearer " + otherUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat3.getId())))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/checkout/confirm")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "paymentMethodToken": "pm_success"
                                        }
                                        """.formatted(showtime.getId(), seat3.getId())))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message")
                                .value("No valid active lock found for seat " + seat3.getId() + " for this confirmation request"));
                assertThat(reservationRepository.findAll()).isEmpty();
        }

        @Test
        void authenticatedConfirmFailsWhenPaymentFails() throws Exception {
                String token = loginAndGetToken("jay@example.com", "password");

                mockMvc.perform(post("/checkout/lock")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/checkout/confirm")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "paymentMethodToken": "pm_fail"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value("Payment failed"));

                assertThat(reservationRepository.findAll()).isEmpty();

                List<SeatLock> locks = seatLockRepository.findAll();
                assertThat(locks)
                        .anyMatch(lock -> lock.getSeat().getId().equals(seat1.getId())
                                && lock.getUser() != null
                                && lock.getUser().getId().equals(user.getId())
                                && lock.getStatus() == LockStatus.LOCKED);
        }



    private String loginAndGetToken(String email, String password) throws Exception {
        String requestBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asString();
    }

    private JsonNode lockAsGuest(String guestEmail, Long... seatIds) throws Exception {
        String response = mockMvc.perform(post("/checkout/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockGuestBody(showtime.getId(), List.of(seatIds), guestEmail)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // return the response JSON
        return objectMapper.readTree(response);
    }

    private String lockGuestBody(Long showtimeId, List<Long> seatIds, String guestEmail) throws Exception {
        // Only creates the JSON request body string for guest lock
        return objectMapper.writeValueAsString(new LockGuestRequest(showtimeId, seatIds, guestEmail));
    }

    private String confirmGuestBody(Long showtimeId, List<Long> seatIds, String guestEmail, String sessionId, String paymentMethodToken) throws Exception {
        return objectMapper.writeValueAsString(
                new ConfirmGuestRequest(showtimeId, seatIds, guestEmail, sessionId, paymentMethodToken)
        );
    }

    private String cancelGuestBody(Long showtimeId, List<Long> seatIds, String guestEmail, String sessionId) throws Exception {
        return objectMapper.writeValueAsString(
                new CancelGuestRequest(showtimeId, seatIds, guestEmail, sessionId)
        );
    }

    // Request body classes for cleaner JSON construction in tests
    /**
     * it just gives:
     * {
        "showtimeId": 1,
        "seatIds": [10, 11],
        "guestEmail": "guest@example.com"
        }
     */
    private record LockGuestRequest(Long showtimeId, List<Long> seatIds, String guestEmail) {}
    private record ConfirmGuestRequest(Long showtimeId, List<Long> seatIds, String guestEmail, String sessionId, String paymentMethodToken) {}
    private record CancelGuestRequest(Long showtimeId, List<Long> seatIds, String guestEmail, String sessionId) {}

    // ====== get availability tests ======
    @Test
        void availabilityReturnsAllSeatsForShowtimeScreen() throws Exception {
        mockMvc.perform(get("/api/showtimes/{id}/available-seats", showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showtimeId").value(showtime.getId()))
                .andExpect(jsonPath("$.seats.length()").value(3))
                .andExpect(jsonPath("$.seats[0].seatId").value(seat1.getId()))
                .andExpect(jsonPath("$.seats[1].seatId").value(seat2.getId()))
                .andExpect(jsonPath("$.seats[2].seatId").value(seat3.getId()));
        }

        @Test
        void availabilityReturnsSeatsAsAvailableWhenNoLocksOrReservationsExist() throws Exception {
        mockMvc.perform(get("/api/showtimes/{id}/available-seats", showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats[0].available").value(true))
                .andExpect(jsonPath("$.seats[1].available").value(true))
                .andExpect(jsonPath("$.seats[2].available").value(true));
        }

        @Test
        void availabilityReturnsLockedSeatAsUnavailable() throws Exception {
        lockAsGuest("guest@example.com", seat1.getId());

        mockMvc.perform(get("/api/showtimes/{id}/available-seats", showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats[0].seatId").value(seat1.getId()))
                .andExpect(jsonPath("$.seats[0].available").value(false))
                .andExpect(jsonPath("$.seats[1].available").value(true))
                .andExpect(jsonPath("$.seats[2].available").value(true));
        }

        @Test
        void availabilityReturnsReservedSeatAsUnavailable() throws Exception {
        String guestEmail = "guest@example.com";
        String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asText();

        mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId, "pm_success")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/showtimes/{id}/available-seats", showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats[0].seatId").value(seat1.getId()))
                .andExpect(jsonPath("$.seats[0].available").value(false));
        }

        @Test
        void availabilityReturnsSeatAsAvailableAfterReservationCancellation() throws Exception {
        String guestEmail = "guest@example.com";
        String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asText();

        String confirmResponse = mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId, "pm_success")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long reservationId = objectMapper.readTree(confirmResponse).get("reservationId").asLong();

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "guestEmail": "guest@example.com"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/showtimes/{id}/available-seats", showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats[0].seatId").value(seat1.getId()))
                .andExpect(jsonPath("$.seats[0].available").value(true));
        }

        @Test
        void availabilityReturnsNotFoundWhenShowtimeDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/showtimes/{id}/available-seats", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Showtime not found with id: 999999"));
        }
        
    // ======== Cancel reservation ========

        @Test
        void guestCanCancelOwnReservationSuccessfully() throws Exception {
        String guestEmail = "guestcancel@example.com";
        String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

        String confirmResponse = mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId, "pm_success")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long reservationId = objectMapper.readTree(confirmResponse).get("reservationId").asLong();

        int availableSeatsBeforeCancel = showtimeRepository.findById(showtime.getId()).orElseThrow().getAvailableSeats();

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "guestEmail": "guestcancel@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.message").value("Reservation cancelled successfully"));

        Reservation cancelled = reservationRepository.findById(reservationId).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getCancelledAt()).isNotNull();

        Showtime updatedShowtime = showtimeRepository.findById(showtime.getId()).orElseThrow();
        assertThat(updatedShowtime.getAvailableSeats()).isEqualTo(availableSeatsBeforeCancel + 1);
        }

        @Test
        void authenticatedUserCanCancelReservationWithoutRequestUserId() throws Exception {
                String token = loginAndGetToken("jay@example.com", "password");

                mockMvc.perform(post("/checkout/lock")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk());

                String confirmResponse = mockMvc.perform(post("/checkout/confirm")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "paymentMethodToken": "pm_success"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                Long reservationId = objectMapper.readTree(confirmResponse).get("reservationId").asLong();

                mockMvc.perform(post("/api/reservations/" + reservationId + "/cancel")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("Reservation cancelled successfully"));
        }

        @Test
        void guestCancelFailsWithWrongEmail() throws Exception {
        String guestEmail = "guestcancel@example.com";
        String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

        String confirmResponse = mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId, "pm_success")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long reservationId = objectMapper.readTree(confirmResponse).get("reservationId").asLong();

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "guestEmail": "wrong@example.com"
                                }
                                """))
                .andExpect(status().isConflict());
        }

        @Test
        void authenticatedUserCannotCancelAnotherUsersReservation() throws Exception {
                User otherUser = new User("Jane", "Doe", "jane@example.com", passwordEncoder.encode("password"), "07999999999");
                otherUser.setRole(UserRole.CUSTOMER);
                otherUser = userRepository.save(otherUser);

                String otherUserToken = loginAndGetToken("jane@example.com", "password");
                String userToken = loginAndGetToken("jay@example.com", "password");

                mockMvc.perform(post("/checkout/lock")
                                .header("Authorization", "Bearer " + otherUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat2.getId())))
                        .andExpect(status().isOk());

                String confirmResponse = mockMvc.perform(post("/checkout/confirm")
                                .header("Authorization", "Bearer " + otherUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "paymentMethodToken": "pm_success"
                                        }
                                        """.formatted(showtime.getId(), seat2.getId())))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                Long reservationId = objectMapper.readTree(confirmResponse).get("reservationId").asLong();

                mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value("Reservation does not belong to this user"));
        }


        @Test
        void cancellingAlreadyCancelledReservationFails() throws Exception {
        String guestEmail = "guestcancel@example.com";
        String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

        String confirmResponse = mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId, "pm_success")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long reservationId = objectMapper.readTree(confirmResponse).get("reservationId").asLong();

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "guestEmail": "guestcancel@example.com"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "guestEmail": "guestcancel@example.com"
                                }
                                """))
                .andExpect(status().isConflict());
        }

        @Test
        void cancellingRestoresSeatAvailability() throws Exception {
        String guestEmail = "guestcancel@example.com";
        String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

        String confirmResponse = mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId, "pm_success")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long reservationId = objectMapper.readTree(confirmResponse).get("reservationId").asLong();

        mockMvc.perform(get("/api/showtimes/{id}/available-seats", showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats[0].seatId").value(seat1.getId()))
                .andExpect(jsonPath("$.seats[0].available").value(false));

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "guestEmail": "guestcancel@example.com"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/showtimes/{id}/available-seats", showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats[0].seatId").value(seat1.getId()))
                .andExpect(jsonPath("$.seats[0].available").value(true));
        }

        @Test
        void cancellingAfterCutoffFails() throws Exception {
        showtime.setStartTime(LocalDateTime.now().plusMinutes(30));
        showtimeRepository.save(showtime);

        String guestEmail = "guestcancel@example.com";
        String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

        String confirmResponse = mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId, "pm_success")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long reservationId = objectMapper.readTree(confirmResponse).get("reservationId").asLong();

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "guestEmail": "guestcancel@example.com"
                                }
                                """))
                .andExpect(status().isConflict());
        }

        // New test for ownership refactor (Jwt token)



        @Test
        void authenticatedUserCannotProvideGuestEmail() throws Exception {
        String token = loginAndGetToken("jay@example.com", "password");

        mockMvc.perform(post("/checkout/lock")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "showtimeId": %d,
                                "seatIds": [%d],
                                "guestEmail": "guest@example.com"
                                }
                                """.formatted(showtime.getId(), seat3.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Guest email must not be provided for authenticated users"));
        }


        @Test
        void authenticatedUserCanCancelLockWithoutRequestUserId() throws Exception {
                String token = loginAndGetToken("jay@example.com", "password");

                mockMvc.perform(post("/checkout/lock")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat3.getId())))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/checkout/cancel")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat3.getId())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("Locks cancelled successfully"));

                List<SeatLock> locks = seatLockRepository.findAll();
                assertThat(locks)
                        .anyMatch(lock -> lock.getSeat().getId().equals(seat3.getId())
                                && lock.getUser() != null
                                && lock.getUser().getId().equals(user.getId())
                                && lock.getStatus() == LockStatus.EXPIRED);
        }

        @Test
        void authenticatedUserCannotProvideGuestFieldsOnConfirm() throws Exception {
                String token = loginAndGetToken("jay@example.com", "password");

                mockMvc.perform(post("/checkout/confirm")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "paymentMethodToken": "pm_success",
                                        "guestEmail": "guest@example.com",
                                        "sessionId": "abc"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isBadRequest());
        }

        // =========== checkout session tests ===========
        @Test
        void guestCanCreateCheckoutSessionForLockedSeats() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                JsonNode response = objectMapper.readTree(
                        mockMvc.perform(post("/checkout/session")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {
                                                "showtimeId": %d,
                                                "seatIds": [%d],
                                                "guestEmail": "%s",
                                                "sessionId": "%s"
                                                }
                                                """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.checkoutReference").isString())
                                .andExpect(jsonPath("$.stripeCheckoutSessionId").isString())
                                .andExpect(jsonPath("$.checkoutUrl").isString())
                                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                                .andExpect(jsonPath("$.checkoutUrl").value(startsWith("https://checkout.stripe.test/")))
                                .andExpect(jsonPath("$.message").value("Checkout session created successfully"))
                                .andReturn()
                                .getResponse()
                                .getContentAsString()
                );

                List<CheckoutSession> checkoutSessions = checkoutSessionRepository.findAll();
                assertThat(checkoutSessions).hasSize(1);

                CheckoutSession checkoutSession = checkoutSessions.getFirst();
                assertThat(checkoutSession.getCheckoutReference()).isEqualTo(response.get("checkoutReference").asString());
                assertThat(checkoutSession.getGuestEmail()).isEqualTo(guestEmail);
                assertThat(checkoutSession.getGuestSessionId()).isEqualTo(sessionId);
                assertThat(checkoutSession.getUser()).isNull();
                assertThat(checkoutSession.getStatus()).isEqualTo(CheckoutSessionStatus.PENDING_PAYMENT);
                assertThat(checkoutSession.getItemsSnapshotJson()).contains("\"seatId\":" + seat1.getId());

                assertThat(reservationRepository.findAll()).isEmpty();

                assertThat(checkoutSession.getStripeCheckoutSessionId())
                        .startsWith("cs_test_");
                assertThat(checkoutSession.getCheckoutUrl())
                        .startsWith("https://checkout.stripe.test/");
                assertThat(checkoutSession.getStripePaymentIntentId())
                        .startsWith("pi_test_");
        }

        @Test
        void authenticatedUserCanCreateCheckoutSessionForLockedSeats() throws Exception {
                String token = loginAndGetToken("jay@example.com", "password");

                mockMvc.perform(post("/checkout/lock")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/checkout/session")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.checkoutReference").isString())
                        .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                        .andExpect(jsonPath("$.checkoutUrl").value(startsWith("https://checkout.stripe.test/")));


                List<CheckoutSession> checkoutSessions = checkoutSessionRepository.findAll();
                assertThat(checkoutSessions).hasSize(1);
                
                CheckoutSession checkoutSession = checkoutSessions.getFirst();

                assertThat(checkoutSession.getUser().getId()).isEqualTo(user.getId());
                assertThat(checkoutSession.getGuestEmail()).isNull();
                assertThat(checkoutSession.getGuestSessionId()).isNull();
                assertThat(checkoutSession.getStatus()).isEqualTo(CheckoutSessionStatus.PENDING_PAYMENT);
                assertThat(checkoutSession.getStripeCheckoutSessionId())
                        .startsWith("cs_test_");
                assertThat(checkoutSession.getCheckoutUrl())
                        .startsWith("https://checkout.stripe.test/");
                assertThat(checkoutSession.getStripePaymentIntentId())
                        .startsWith("pi_test_");
                        

                assertThat(reservationRepository.findAll()).isEmpty();
        }

        @Test
        void guestCannotCreateCheckoutSessionWithWrongSession() throws Exception {
                String guestEmail = "guest@example.com";
                lockAsGuest(guestEmail, seat1.getId());

                mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "wrong-session"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail)))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value("No valid active lock found for this checkout session request"));

                assertThat(checkoutSessionRepository.findAll()).isEmpty();
                assertThat(reservationRepository.findAll()).isEmpty();
        }

        @Test
        void authenticatedUserCannotCreateCheckoutSessionForAnotherUsersLock() throws Exception {
                User otherUser = new User("Jane", "Doe", "jane@example.com", passwordEncoder.encode("password"), "07999999999");
                otherUser.setRole(UserRole.CUSTOMER);
                userRepository.save(otherUser);

                String otherUserToken = loginAndGetToken("jane@example.com", "password");
                String userToken = loginAndGetToken("jay@example.com", "password");

                mockMvc.perform(post("/checkout/lock")
                                .header("Authorization", "Bearer " + otherUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/checkout/session")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value("No valid active lock found for this checkout session request"));

                assertThat(checkoutSessionRepository.findAll()).isEmpty();
                assertThat(reservationRepository.findAll()).isEmpty();
        }

        @Test
        void guestCanReadCheckoutSessionStatusByReference() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                String checkoutReference = objectMapper.readTree(createResponse).get("checkoutReference").asString();

                mockMvc.perform(get("/checkout/session/{checkoutReference}", checkoutReference))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.checkoutReference").value(checkoutReference))
                        .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                        .andExpect(jsonPath("$.reservationId").doesNotExist())
                        .andExpect(jsonPath("$.bookingReference").doesNotExist());
        }

        @Test
        void authenticatedUserCannotReadAnotherUsersCheckoutSessionStatus() throws Exception {
                User otherUser = new User("Jane", "Doe", "jane@example.com", passwordEncoder.encode("password"), "07999999999");
                otherUser.setRole(UserRole.CUSTOMER);
                userRepository.save(otherUser);

                String otherUserToken = loginAndGetToken("jane@example.com", "password");
                String userToken = loginAndGetToken("jay@example.com", "password");

                mockMvc.perform(post("/checkout/lock")
                                .header("Authorization", "Bearer " + otherUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk());

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .header("Authorization", "Bearer " + otherUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                String checkoutReference = objectMapper.readTree(createResponse).get("checkoutReference").asString();

                mockMvc.perform(get("/checkout/session/{checkoutReference}", checkoutReference)
                                .header("Authorization", "Bearer " + userToken))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value("Checkout session does not belong to this user"));
        }

        @Test
        void authenticatedUserCanReadOwnCheckoutSessionStatus() throws Exception {
                String token = loginAndGetToken("jay@example.com", "password");

                mockMvc.perform(post("/checkout/lock")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk());

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                String checkoutReference = objectMapper.readTree(createResponse).get("checkoutReference").asString();

                mockMvc.perform(get("/checkout/session/{checkoutReference}", checkoutReference)
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.checkoutReference").value(checkoutReference))
                        .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                        .andExpect(jsonPath("$.message").value("Checkout session is awaiting payment"));
        }


        // Stripe webhook handling tests
        @Test
        void stripeWebhookFinalizesCheckoutSessionAndCreatesReservation() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                String stripeCheckoutSessionId = objectMapper.readTree(createResponse)
                        .get("stripeCheckoutSessionId")
                        .asString();

                when(stripeCheckoutService.parseCheckoutCompletedEvent(any(), any()))
                        .thenReturn(new StripeCheckoutCompletedEvent(
                                stripeCheckoutSessionId,
                                "pi_test_paid"
                        ));

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());

                CheckoutSession checkoutSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(checkoutSession.getStatus()).isEqualTo(CheckoutSessionStatus.FINALIZED);
                assertThat(checkoutSession.getStripePaymentIntentId()).isEqualTo("pi_test_paid");
                assertThat(checkoutSession.getCompletedAt()).isNotNull();
                assertThat(checkoutSession.getReservation()).isNotNull();

                List<Reservation> reservations = reservationRepository.findAll();
                assertThat(reservations).hasSize(1);
                assertThat(reservations.getFirst().getGuestEmail()).isEqualTo(guestEmail);
                assertThat(reservations.getFirst().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
                assertThat(reservations.getFirst().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
                assertThat(reservations.getFirst().getCurrency()).isEqualTo(CurrencyCode.GBP);

                List<SeatLock> locks = seatLockRepository.findAll();
                assertThat(locks)
                        .anyMatch(lock -> lock.getSeat().getId().equals(seat1.getId())
                                && lock.getGuestEmail().equals(guestEmail)
                                && lock.getStatus() == LockStatus.CONVERTED_TO_RESERVATION);
        }


        @Test
        void stripeWebhookIgnoresUnhandledEvent() throws Exception {
                when(stripeCheckoutService.parseCheckoutCompletedEvent(any(), any()))
                        .thenReturn(null);

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());

                assertThat(checkoutSessionRepository.findAll()).isEmpty();
        }

        @Test
        void stripeWebhookDuplicateDoesNotCreateDuplicateReservation() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                String stripeCheckoutSessionId = objectMapper.readTree(createResponse)
                        .get("stripeCheckoutSessionId")
                        .asString();

                when(stripeCheckoutService.parseCheckoutCompletedEvent(any(), any()))
                        .thenReturn(new StripeCheckoutCompletedEvent(
                                stripeCheckoutSessionId,
                                "pi_test_paid"
                        ));

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());

                assertThat(reservationRepository.findAll()).hasSize(1);

                CheckoutSession checkoutSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(checkoutSession.getStatus()).isEqualTo(CheckoutSessionStatus.FINALIZED);
                assertThat(checkoutSession.getReservation()).isNotNull();
        }

        @Test
        void checkoutSessionStatusReturnsReservationAfterWebhookFinalization() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                JsonNode createJson = objectMapper.readTree(createResponse);
                String checkoutReference = createJson.get("checkoutReference").asString();
                String stripeCheckoutSessionId = createJson.get("stripeCheckoutSessionId").asString();

                when(stripeCheckoutService.parseCheckoutCompletedEvent(any(), any()))
                        .thenReturn(new StripeCheckoutCompletedEvent(
                                stripeCheckoutSessionId,
                                "pi_test_paid"
                        ));

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());

                Reservation reservation = reservationRepository.findAll().getFirst();

                mockMvc.perform(get("/checkout/session/{checkoutReference}", checkoutReference))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.checkoutReference").value(checkoutReference))
                        .andExpect(jsonPath("$.status").value("FINALIZED"))
                        .andExpect(jsonPath("$.reservationId").value(reservation.getId()))
                        .andExpect(jsonPath("$.bookingReference").value(reservation.getBookingReference()));
        }

        @Test
        void authenticatedUserCanListAndReadOwnReservationDetails() throws Exception {
                String token = loginAndGetToken("jay@example.com", "password");
                Reservation reservation = finalizeAuthenticatedStripeReservation(token, seat1.getId());

                mockMvc.perform(get("/api/reservations")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].reservationId").value(reservation.getId()))
                        .andExpect(jsonPath("$[0].reservationReference").value(reservation.getBookingReference()))
                        .andExpect(jsonPath("$[0].reservationStatus").value("CONFIRMED"))
                        .andExpect(jsonPath("$[0].paymentStatus").value("PAID"))
                        .andExpect(jsonPath("$[0].showtime.id").value(showtime.getId()))
                        .andExpect(jsonPath("$[0].movie.title").value("Test Movie"))
                        .andExpect(jsonPath("$[0].screen.name").value("Screen 1"))
                        .andExpect(jsonPath("$[0].seats[0].id").value(seat1.getId()))
                        .andExpect(jsonPath("$[0].seats[0].rowLabel").value("A"))
                        .andExpect(jsonPath("$[0].seats[0].seatNumber").value(1))
                        .andExpect(jsonPath("$[0].totalAmount").value(12.50))
                        .andExpect(jsonPath("$[0].currency").value("GBP"))
                        .andExpect(jsonPath("$[0].createdAt").isString());

                mockMvc.perform(get("/api/reservations/{id}", reservation.getId())
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.reservationId").value(reservation.getId()))
                        .andExpect(jsonPath("$.reservationReference").value(reservation.getBookingReference()))
                        .andExpect(jsonPath("$.reservationStatus").value("CONFIRMED"))
                        .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                        .andExpect(jsonPath("$.showtime.id").value(showtime.getId()))
                        .andExpect(jsonPath("$.movie.title").value("Test Movie"))
                        .andExpect(jsonPath("$.screen.name").value("Screen 1"))
                        .andExpect(jsonPath("$.seats[0].id").value(seat1.getId()))
                        .andExpect(jsonPath("$.totalAmount").value(12.50))
                        .andExpect(jsonPath("$.currency").value("GBP"))
                        .andExpect(jsonPath("$.createdAt").isString());
        }

        @Test
        void authenticatedUserCannotReadAnotherUsersReservationDetails() throws Exception {
                User otherUser = new User("Jane", "Doe", "jane@example.com", passwordEncoder.encode("password"), "07999999999");
                otherUser.setRole(UserRole.CUSTOMER);
                userRepository.save(otherUser);

                String otherUserToken = loginAndGetToken("jane@example.com", "password");
                String userToken = loginAndGetToken("jay@example.com", "password");
                Reservation reservation = finalizeAuthenticatedStripeReservation(otherUserToken, seat1.getId());

                mockMvc.perform(get("/api/reservations/{id}", reservation.getId())
                                .header("Authorization", "Bearer " + userToken))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value("Reservation does not belong to this user"));
        }

        @Test
        void guestCanLookupReservationByReferenceAndEmail() throws Exception {
                String guestEmail = "guest@example.com";
                Reservation reservation = finalizeGuestStripeReservation(guestEmail, seat1.getId());

                mockMvc.perform(get("/api/reservations/reference/{reservationReference}", reservation.getBookingReference())
                                .param("guestEmail", guestEmail))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.reservationId").value(reservation.getId()))
                        .andExpect(jsonPath("$.reservationReference").value(reservation.getBookingReference()))
                        .andExpect(jsonPath("$.reservationStatus").value("CONFIRMED"))
                        .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                        .andExpect(jsonPath("$.showtime.id").value(showtime.getId()))
                        .andExpect(jsonPath("$.movie.title").value("Test Movie"))
                        .andExpect(jsonPath("$.screen.name").value("Screen 1"))
                        .andExpect(jsonPath("$.seats[0].id").value(seat1.getId()))
                        .andExpect(jsonPath("$.totalAmount").value(12.50))
                        .andExpect(jsonPath("$.currency").value("GBP"))
                        .andExpect(jsonPath("$.createdAt").isString());
        }

        @Test
        void guestCannotLookupReservationWithWrongEmail() throws Exception {
                Reservation reservation = finalizeGuestStripeReservation("guest@example.com", seat1.getId());

                mockMvc.perform(get("/api/reservations/reference/{reservationReference}", reservation.getBookingReference())
                                .param("guestEmail", "wrong@example.com"))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value("Reservation does not belong to this guest"));
        }

        @Test
        void reservationHistoryEndpointsRequireAuthentication() throws Exception {
                mockMvc.perform(get("/api/reservations"))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.message")
                                .value("Authentication is required to access this resource"));

                mockMvc.perform(get("/api/reservations/{id}", 1L))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.message")
                                .value("Authentication is required to access this resource"));
        }

        @Test
        void stripeExpiredWebhookMarksPendingCheckoutSessionExpired() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                String stripeCheckoutSessionId = objectMapper.readTree(createResponse)
                        .get("stripeCheckoutSessionId")
                        .asString();

                when(stripeCheckoutService.parseCheckoutCompletedEvent(any(), any()))
                        .thenReturn(null);
                when(stripeCheckoutService.parseCheckoutExpiredEvent(any(), any()))
                        .thenReturn(new StripeCheckoutExpiredEvent(stripeCheckoutSessionId));

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());

                CheckoutSession checkoutSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(checkoutSession.getStatus()).isEqualTo(CheckoutSessionStatus.EXPIRED);

                assertThat(reservationRepository.findAll()).isEmpty();

                List<SeatLock> locks = seatLockRepository.findAll();
                assertThat(locks)
                        .anyMatch(lock -> lock.getSeat().getId().equals(seat1.getId())
                                && lock.getGuestEmail().equals(guestEmail)
                                && lock.getStatus() == LockStatus.LOCKED);
        }

        @Test
        void stripeExpiredWebhookDoesNotChangeFinalizedCheckoutSession() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                String stripeCheckoutSessionId = objectMapper.readTree(createResponse)
                        .get("stripeCheckoutSessionId")
                        .asString();

                when(stripeCheckoutService.parseCheckoutCompletedEvent(any(), any()))
                        .thenReturn(new StripeCheckoutCompletedEvent(
                                stripeCheckoutSessionId,
                                "pi_test_paid"
                        ));

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());

                CheckoutSession finalizedSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(finalizedSession.getStatus()).isEqualTo(CheckoutSessionStatus.FINALIZED);
                assertThat(reservationRepository.findAll()).hasSize(1);

                when(stripeCheckoutService.parseCheckoutCompletedEvent(any(), any()))
                        .thenReturn(null);
                when(stripeCheckoutService.parseCheckoutExpiredEvent(any(), any()))
                        .thenReturn(new StripeCheckoutExpiredEvent(stripeCheckoutSessionId));

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());

                CheckoutSession checkoutSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(checkoutSession.getStatus()).isEqualTo(CheckoutSessionStatus.FINALIZED);
                assertThat(reservationRepository.findAll()).hasSize(1);
        }

        // test for cancel checkoutSession
        @Test
        void guestCancelLockMarksPendingCheckoutSessionCancelled() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/checkout/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cancelGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("Locks cancelled successfully"));

                CheckoutSession checkoutSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(checkoutSession.getStatus()).isEqualTo(CheckoutSessionStatus.CANCELLED);
                assertThat(checkoutSession.getCancelledAt()).isNotNull();

                List<SeatLock> locks = seatLockRepository.findAll();
                assertThat(locks)
                        .anyMatch(lock -> lock.getSeat().getId().equals(seat1.getId())
                                && lock.getGuestEmail().equals(guestEmail)
                                && lock.getStatus() == LockStatus.EXPIRED);

                assertThat(reservationRepository.findAll()).isEmpty();
        }

        @Test
        void authenticatedCancelLockMarksPendingCheckoutSessionCancelled() throws Exception {
                String token = loginAndGetToken("jay@example.com", "password");

                mockMvc.perform(post("/checkout/lock")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/checkout/session")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/checkout/cancel")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seat1.getId())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("Locks cancelled successfully"));

                CheckoutSession checkoutSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(checkoutSession.getStatus()).isEqualTo(CheckoutSessionStatus.CANCELLED);
                assertThat(checkoutSession.getCancelledAt()).isNotNull();

                assertThat(reservationRepository.findAll()).isEmpty();
        }

        @Test
        void cancelLockDoesNotChangeFinalizedCheckoutSession() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                String stripeCheckoutSessionId = objectMapper.readTree(createResponse)
                        .get("stripeCheckoutSessionId")
                        .asString();

                when(stripeCheckoutService.parseCheckoutCompletedEvent(any(), any()))
                        .thenReturn(new StripeCheckoutCompletedEvent(
                                stripeCheckoutSessionId,
                                "pi_test_paid"
                        ));

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/checkout/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cancelGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId)))
                        .andExpect(status().isConflict());

                CheckoutSession checkoutSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(checkoutSession.getStatus()).isEqualTo(CheckoutSessionStatus.FINALIZED);
                assertThat(reservationRepository.findAll()).hasSize(1);
        }

        // test for auto cleanup
        @Test
        void cleanupExpiresStalePendingCheckoutSessions() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isOk());

                CheckoutSession checkoutSession = checkoutSessionRepository.findAll().getFirst();
                checkoutSession.setExpiresAt(LocalDateTime.now().minusMinutes(1));
                checkoutSessionRepository.save(checkoutSession);

                int expiredCount = seatLockCleanupService.expireStalePendingCheckoutSessions();

                assertThat(expiredCount).isEqualTo(1);

                CheckoutSession expiredSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(expiredSession.getStatus()).isEqualTo(CheckoutSessionStatus.EXPIRED);

                List<SeatLock> locks = seatLockRepository.findAll();
                assertThat(locks)
                        .anyMatch(lock -> lock.getSeat().getId().equals(seat1.getId())
                                && lock.getGuestEmail().equals(guestEmail)
                                && lock.getStatus() == LockStatus.LOCKED);
        }

        @Test
        void cleanupDoesNotExpireFinalizedCheckoutSessions() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                String stripeCheckoutSessionId = objectMapper.readTree(createResponse)
                        .get("stripeCheckoutSessionId")
                        .asString();

                when(stripeCheckoutService.parseCheckoutCompletedEvent(any(), any()))
                        .thenReturn(new StripeCheckoutCompletedEvent(
                                stripeCheckoutSessionId,
                                "pi_test_paid"
                        ));

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());

                CheckoutSession checkoutSession = checkoutSessionRepository.findAll().getFirst();
                checkoutSession.setExpiresAt(LocalDateTime.now().minusMinutes(1));
                checkoutSessionRepository.save(checkoutSession);

                int expiredCount = seatLockCleanupService.expireStalePendingCheckoutSessions();

                assertThat(expiredCount).isEqualTo(0);

                CheckoutSession finalizedSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(finalizedSession.getStatus()).isEqualTo(CheckoutSessionStatus.FINALIZED);

                List<SeatLock> locks = seatLockRepository.findAll();
                assertThat(locks)
                        .anyMatch(lock -> lock.getSeat().getId().equals(seat1.getId())
                                && lock.getGuestEmail().equals(guestEmail)
                                && lock.getStatus() == LockStatus.CONVERTED_TO_RESERVATION);
        }

        @Test
        void cleanupExpiresTimedOutSeatLocks() throws Exception {
                String guestEmail = "guest@example.com";
                lockAsGuest(guestEmail, seat1.getId());

                SeatLock lock = seatLockRepository.findAll().getFirst();
                lock.setExpiresAt(LocalDateTime.now().minusMinutes(1));
                seatLockRepository.save(lock);

                int expiredCount = seatLockCleanupService.expireTimedOutLocks();

                assertThat(expiredCount).isEqualTo(1);

                SeatLock expiredLock = seatLockRepository.findAll().getFirst();
                assertThat(expiredLock.getStatus()).isEqualTo(LockStatus.EXPIRED);
        }

        @Test
        void cleanupDoesNotExpireConvertedSeatLocks() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                mockMvc.perform(post("/checkout/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId, "pm_success")))
                        .andExpect(status().isOk());

                SeatLock convertedLock = seatLockRepository.findAll().getFirst();
                assertThat(convertedLock.getStatus()).isEqualTo(LockStatus.CONVERTED_TO_RESERVATION);

                convertedLock.setExpiresAt(LocalDateTime.now().minusMinutes(1));
                seatLockRepository.save(convertedLock);

                int expiredCount = seatLockCleanupService.expireTimedOutLocks();

                assertThat(expiredCount).isEqualTo(0);

                SeatLock stillConvertedLock = seatLockRepository.findAll().getFirst();
                assertThat(stillConvertedLock.getStatus()).isEqualTo(LockStatus.CONVERTED_TO_RESERVATION);
        }

        // test for handling completed webhook for edge cases
        @Test
        void completedWebhookAfterExpiredCheckoutMarksSessionFailedWithoutReservation() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                String stripeCheckoutSessionId = objectMapper.readTree(createResponse)
                        .get("stripeCheckoutSessionId")
                        .asString();

                CheckoutSession checkoutSession = checkoutSessionRepository.findAll().getFirst();
                checkoutSession.setStatus(CheckoutSessionStatus.EXPIRED);
                checkoutSessionRepository.save(checkoutSession);

                when(stripeCheckoutService.parseCheckoutCompletedEvent(any(), any()))
                        .thenReturn(new StripeCheckoutCompletedEvent(
                                stripeCheckoutSessionId,
                                "pi_test_paid_after_expiry"
                        ));

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());

                CheckoutSession failedSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(failedSession.getStatus()).isEqualTo(CheckoutSessionStatus.FAILED);
                assertThat(failedSession.getStripePaymentIntentId()).isEqualTo("pi_test_paid_after_expiry");
                assertThat(failedSession.getFailedAt()).isNotNull();

                assertThat(reservationRepository.findAll()).isEmpty();
        }

        @Test
        void completedWebhookAfterCancelledCheckoutMarksSessionFailedWithoutReservation() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                String stripeCheckoutSessionId = objectMapper.readTree(createResponse)
                        .get("stripeCheckoutSessionId")
                        .asString();

                mockMvc.perform(post("/checkout/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cancelGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId)))
                        .andExpect(status().isOk());

                CheckoutSession cancelledSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(cancelledSession.getStatus()).isEqualTo(CheckoutSessionStatus.CANCELLED);

                when(stripeCheckoutService.parseCheckoutCompletedEvent(any(), any()))
                        .thenReturn(new StripeCheckoutCompletedEvent(
                                stripeCheckoutSessionId,
                                "pi_test_paid_after_cancel"
                        ));

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());

                CheckoutSession failedSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(failedSession.getStatus()).isEqualTo(CheckoutSessionStatus.FAILED);
                assertThat(failedSession.getStripePaymentIntentId()).isEqualTo("pi_test_paid_after_cancel");
                assertThat(failedSession.getFailedAt()).isNotNull();

                assertThat(reservationRepository.findAll()).isEmpty();
        }


        @Test
        void completedWebhookWithExpiredLockMarksSessionFailedWithoutReservation() throws Exception {
                String guestEmail = "guest@example.com";
                String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asString();

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seat1.getId(), guestEmail, sessionId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                String stripeCheckoutSessionId = objectMapper.readTree(createResponse)
                        .get("stripeCheckoutSessionId")
                        .asString();

                SeatLock lock = seatLockRepository.findAll().getFirst();
                lock.setExpiresAt(LocalDateTime.now().minusMinutes(1));
                seatLockRepository.save(lock);

                seatLockCleanupService.expireTimedOutLocks();

                when(stripeCheckoutService.parseCheckoutCompletedEvent(any(), any()))
                        .thenReturn(new StripeCheckoutCompletedEvent(
                                stripeCheckoutSessionId,
                                "pi_test_paid_after_lock_expiry"
                        ));

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());

                CheckoutSession failedSession = checkoutSessionRepository.findAll().getFirst();
                assertThat(failedSession.getStatus()).isEqualTo(CheckoutSessionStatus.FAILED);
                assertThat(failedSession.getStripePaymentIntentId()).isEqualTo("pi_test_paid_after_lock_expiry");
                assertThat(failedSession.getFailedAt()).isNotNull();

                assertThat(reservationRepository.findAll()).isEmpty();

                SeatLock expiredLock = seatLockRepository.findAll().getFirst();
                assertThat(expiredLock.getStatus()).isEqualTo(LockStatus.EXPIRED);
        }

        // helper function for reservation finalization in tests
        private Reservation finalizeGuestStripeReservation(String guestEmail, Long seatId) throws Exception {
                String sessionId = lockAsGuest(guestEmail, seatId).get("sessionId").asString();

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d],
                                        "guestEmail": "%s",
                                        "sessionId": "%s"
                                        }
                                        """.formatted(showtime.getId(), seatId, guestEmail, sessionId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                finalizeStripeCheckout(createResponse);

                return reservationRepository.findAll().getFirst();
        }

        private Reservation finalizeAuthenticatedStripeReservation(String token, Long seatId) throws Exception {
                mockMvc.perform(post("/checkout/lock")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seatId)))
                        .andExpect(status().isOk());

                String createResponse = mockMvc.perform(post("/checkout/session")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "showtimeId": %d,
                                        "seatIds": [%d]
                                        }
                                        """.formatted(showtime.getId(), seatId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                finalizeStripeCheckout(createResponse);

                return reservationRepository.findAll().getFirst();
        }

        private void finalizeStripeCheckout(String createResponse) throws Exception {
                String stripeCheckoutSessionId = objectMapper.readTree(createResponse)
                        .get("stripeCheckoutSessionId")
                        .asString();

                when(stripeCheckoutService.parseCheckoutCompletedEvent(any(), any()))
                        .thenReturn(new StripeCheckoutCompletedEvent(
                                stripeCheckoutSessionId,
                                "pi_test_paid"
                        ));

                mockMvc.perform(post("/checkout/webhook/stripe")
                                .header("Stripe-Signature", "test-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());
        }
}
