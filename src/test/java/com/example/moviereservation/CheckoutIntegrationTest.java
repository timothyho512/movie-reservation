package com.example.moviereservation;


import com.example.moviereservation.entity.LockStatus;
import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Reservation;
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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


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

    private Showtime showtime;
    private Seat seat1;
    private Seat seat2;
    private Seat seat3;
    private User user;

    @BeforeEach
    void setUp() {
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

        user = new User("Jay", "Doe", "Jay@example.com", "password", "07123456789");
        user.setRole(UserRole.CUSTOMER);
        user = userRepository.save(user);
    }

    @Test
    void guestCanLockSeatsSuccessfully() throws Exception {
        JsonNode response = lockAsGuest("guest@example.com", seat1.getId(), seat2.getId());
        
        assertThat(response.get("message").asText()).isEqualTo("Seats locked successfully");
        assertThat(response.get("sessionId").asText()).isNotBlank();
        assertThat(response.get("lockedSeatIds")).hasSize(2);

        List<SeatLock> locks = seatLockRepository.findAll();
        assertThat(locks).hasSize(2);
        assertThat(locks)
                .allMatch(lock -> lock.getGuestEmail().equals("guest@example.com"))
                .allMatch(lock -> lock.getSessionId() != null)
                .allMatch(lock -> lock.getStatus() == LockStatus.LOCKED);
    }

    @Test
    void guestConfirmFailsWithWrongSession() throws Exception {
        lockAsGuest("guest@example.com", seat1.getId());

        mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), "guest@example.com", "wrong-session")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("No valid active lock found for seat " + seat1.getId() + " for this confirmation request"));

        assertThat(reservationRepository.findAll()).isEmpty();
    }

    @Test
    void guestConfirmFailsWithWrongEmail() throws Exception {
        String sessionId = lockAsGuest("guest@example.com", seat1.getId()).get("sessionId").asText();

        mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), "wrong@example.com", sessionId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("No valid active lock found for seat " + seat1.getId() + " for this confirmation request"));

        assertThat(reservationRepository.findAll()).isEmpty();
    }

    @Test
    void guestConfirmSucceedsWithCorrectSessionAndEmail() throws Exception {
        String guestEmail = "guest@example.com";
        String sessionId = lockAsGuest(guestEmail, seat1.getId()).get("sessionId").asText();

        mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), guestEmail, sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservation confirmed successfully"))
                .andExpect(jsonPath("$.reservationId").isNumber())
                .andExpect(jsonPath("$.bookingReference").isString())
                .andExpect(jsonPath("$.seatIds[0]").value(seat1.getId()));

        List<Reservation> reservations = reservationRepository.findAll();
        assertThat(reservations).hasSize(1);
        assertThat(reservations.getFirst().getGuestEmail()).isEqualTo(guestEmail);

        List<SeatLock> locks = seatLockRepository.findAll();
        assertThat(locks)
                .anyMatch(lock -> lock.getSeat().getId().equals(seat1.getId())
                        && lock.getGuestEmail().equals(guestEmail)
                        && lock.getStatus() == LockStatus.CONVERTED_TO_RESERVATION);
    }

    @Test
    void guestCancelSucceedsAndSeatCanBeLockedAgain() throws Exception {
        String sessionId = lockAsGuest("guest@example.com", seat1.getId()).get("sessionId").asText();

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
        assertThat(relockResponse.get("message").asText()).isEqualTo("Seats locked successfully");
    }

    @Test
    void cannotLockAlreadyReservedSeats() throws Exception {
        String sessionId = lockAsGuest("guest@example.com", seat1.getId()).get("sessionId").asText();

        mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmGuestBody(showtime.getId(), List.of(seat1.getId()), "guest@example.com", sessionId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/checkout/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockGuestBody(showtime.getId(), List.of(seat1.getId()), "another@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Seat " + seat1.getId() + " is already reserved for this showtime"));
    }

    @Test
    void registeredUserCanLockAndConfirmSuccessfully() throws Exception {
        lockAsUser(user.getId(), seat2.getId());

        mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmUserBody(showtime.getId(), List.of(seat2.getId()), user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservation confirmed successfully"))
                .andExpect(jsonPath("$.seatIds[0]").value(seat2.getId()));

        List<Reservation> reservations = reservationRepository.findAll();
        assertThat(reservations).hasSize(1);
        assertThat(reservations.getFirst().getUser().getId()).isEqualTo(user.getId());
        assertThat(reservations.getFirst().getGuestEmail()).isNull();
    }

    @Test
    void registeredUserConfirmFailsWhenLockBelongsToAnotherUser() throws Exception {
        User otherUser = new User("Jane", "Doe", "jane@example.com", "password", "07999999999");
        otherUser.setRole(UserRole.CUSTOMER);
        otherUser = userRepository.save(otherUser);

        lockAsUser(user.getId(), seat3.getId());

        mockMvc.perform(post("/checkout/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmUserBody(showtime.getId(), List.of(seat3.getId()), otherUser.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("No valid active lock found for seat " + seat3.getId() + " for this confirmation request"));

        assertThat(reservationRepository.findAll()).isEmpty();
    }



    private String lockSeatAndGetSessionId(String guestEmail, Long seatId) throws Exception {
        String requestBody = """
            {
              "showtimeId": %d,
              "seatIds": [%d],
              "guestEmail": "%s"
            }
            """.formatted(showtime.getId(), seatId, guestEmail);

        String response = mockMvc.perform(post("/checkout/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("sessionId").asText();
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

    private JsonNode lockAsUser(Long userId, Long... seatIds) throws Exception {
        String response = mockMvc.perform(post("/checkout/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockUserBody(showtime.getId(), List.of(seatIds), userId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private String lockGuestBody(Long showtimeId, List<Long> seatIds, String guestEmail) throws Exception {
        // Only creates the JSON request body string for guest lock
        return objectMapper.writeValueAsString(new LockGuestRequest(showtimeId, seatIds, guestEmail));
    }

    private String lockUserBody(Long showtimeId, List<Long> seatIds, Long userId) throws Exception {
        return objectMapper.writeValueAsString(
                new LockUserRequest(showtimeId, seatIds, userId)
        );
    }

    private String confirmGuestBody(Long showtimeId, List<Long> seatIds, String guestEmail, String sessionId) throws Exception {
        return objectMapper.writeValueAsString(
                new ConfirmGuestRequest(showtimeId, seatIds, guestEmail, sessionId)
        );
    }

    private String confirmUserBody(Long showtimeId, List<Long> seatIds, Long userId) throws Exception {
        return objectMapper.writeValueAsString(
                new ConfirmUserRequest(showtimeId, seatIds, userId)
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
    private record LockUserRequest(Long showtimeId, List<Long> seatIds, Long userId) {}
    private record ConfirmGuestRequest(Long showtimeId, List<Long> seatIds, String guestEmail, String sessionId) {}
    private record ConfirmUserRequest(Long showtimeId, List<Long> seatIds, Long userId) {}
    private record CancelGuestRequest(Long showtimeId, List<Long> seatIds, String guestEmail, String sessionId) {}

}
