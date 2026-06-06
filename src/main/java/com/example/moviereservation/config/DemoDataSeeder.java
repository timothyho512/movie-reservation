package com.example.moviereservation.config;

import com.example.moviereservation.entity.*;
import com.example.moviereservation.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("dev")
public class DemoDataSeeder implements CommandLineRunner {
    private static final String DEMO_CUSTOMER_EMAIL = "demo.customer@example.com";
    private static final String DEMO_ADMIN_EMAIL = "demo.admin@example.com";
    private static final String DEMO_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final ScreenLayoutVersionRepository screenLayoutVersionRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
            UserRepository userRepository,
            TheatreRepository theatreRepository,
            ScreenRepository screenRepository,
            ScreenLayoutVersionRepository screenLayoutVersionRepository,
            SeatRepository seatRepository,
            MovieRepository movieRepository,
            ShowtimeRepository showtimeRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.theatreRepository = theatreRepository;
        this.screenRepository = screenRepository;
        this.screenLayoutVersionRepository = screenLayoutVersionRepository;
        this.seatRepository = seatRepository;
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(DEMO_CUSTOMER_EMAIL).isPresent()) {
            return;
        }

        User customer = new User(
                "Demo",
                "Customer",
                DEMO_CUSTOMER_EMAIL,
                passwordEncoder.encode(DEMO_PASSWORD),
                "07900000001"
        );
        customer.setRole(UserRole.CUSTOMER);

        User admin = new User(
                "Demo",
                "Admin",
                DEMO_ADMIN_EMAIL,
                passwordEncoder.encode(DEMO_PASSWORD),
                "07900000002"
        );
        admin.setRole(UserRole.ADMIN);

        userRepository.saveAll(List.of(customer, admin));

        Theatre odeon = new Theatre(
                "Odeon Leicester Square",
                "24-26 Leicester Square",
                "London",
                "England",
                "United Kingdom",
                "WC2H 7JY"
        );
        odeon.setPhoneNumber("020 7734 1506");
        odeon.setTotalScreens(2);
        odeon.setTotalSeats(60);

        Theatre vue = new Theatre(
                "Vue Manchester Printworks",
                "Withy Grove",
                "Manchester",
                "England",
                "United Kingdom",
                "M4 2BS"
        );
        vue.setPhoneNumber("0345 308 4620");
        vue.setTotalScreens(1);
        vue.setTotalSeats(30);

        theatreRepository.saveAll(List.of(odeon, vue));

        Screen odeonScreenOne = new Screen("Screen 1", odeon, 30, ScreenType.STANDARD);
        Screen odeonImax = new Screen("IMAX", odeon, 30, ScreenType.IMAX);
        Screen vueVip = new Screen("VIP Screen", vue, 30, ScreenType.VIP);

        screenRepository.saveAll(List.of(odeonScreenOne, odeonImax, vueVip));
        initializeLayoutVersion(odeonScreenOne);
        initializeLayoutVersion(odeonImax);
        initializeLayoutVersion(vueVip);

        seatRepository.saveAll(buildSeats(odeonScreenOne));
        seatRepository.saveAll(buildSeats(odeonImax));
        seatRepository.saveAll(buildSeats(vueVip));

        Movie movie1 = new Movie("Inception", "Christopher Nolan");
        Movie movie2 = new Movie("Dune: Part Two", "Denis Villeneuve");
        Movie movie3 = new Movie("Spirited Away", "Hayao Miyazaki");

        movieRepository.saveAll(List.of(movie1, movie2, movie3));

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        showtimeRepository.saveAll(List.of(
                showtime(movie1, odeonScreenOne, now.plusDays(1).withHour(18).withMinute(30), 148, "14.00"),
                showtime(movie1, vueVip, now.plusDays(2).withHour(20).withMinute(0), 148, "18.50"),
                showtime(movie2, odeonImax, now.plusDays(1).withHour(20).withMinute(15), 166, "19.00"),
                showtime(movie2, odeonImax, now.plusDays(3).withHour(17).withMinute(45), 166, "19.00"),
                showtime(movie3, odeonScreenOne, now.plusDays(2).withHour(15).withMinute(0), 125, "12.50"),
                showtime(movie3, vueVip, now.plusDays(4).withHour(19).withMinute(30), 125, "16.00")
        ));
    }

    private Showtime showtime(Movie movie, Screen screen, LocalDateTime startTime, int durationMinutes, String basePrice) {
        return new Showtime(
                movie,
                screen,
                startTime,
                startTime.plusMinutes(durationMinutes),
                new BigDecimal(basePrice)
        );
    }

    private List<Seat> buildSeats(Screen screen) {
        List<Seat> seats = new ArrayList<>();

        addRow(seats, screen, "A", SeatType.REGULAR, "12.50");
        addRow(seats, screen, "B", SeatType.REGULAR, "12.50");
        addRow(seats, screen, "C", SeatType.VIP, "18.00");
        addRow(seats, screen, "D", SeatType.VIP, "18.00");
        addAccessibleRow(seats, screen);

        return seats;
    }

    private void initializeLayoutVersion(Screen screen) {
        ScreenLayoutVersion layoutVersion = screenLayoutVersionRepository.save(new ScreenLayoutVersion(screen, 1));
        screen.setCurrentLayoutVersion(layoutVersion);
        screenRepository.save(screen);
    }

    private void addRow(List<Seat> seats, Screen screen, String rowLabel, SeatType seatType, String basePrice) {
        for (int seatNumber = 1; seatNumber <= 6; seatNumber++) {
            seats.add(new Seat(screen, rowLabel, seatNumber, seatType, new BigDecimal(basePrice)));
        }
    }

    private void addAccessibleRow(List<Seat> seats, Screen screen) {
        seats.add(new Seat(screen, "E", 1, SeatType.WHEELCHAIR, new BigDecimal("10.00")));
        seats.add(new Seat(screen, "E", 2, SeatType.WHEELCHAIR, new BigDecimal("10.00")));
        seats.add(new Seat(screen, "E", 3, SeatType.REGULAR, new BigDecimal("12.50")));
        seats.add(new Seat(screen, "E", 4, SeatType.REGULAR, new BigDecimal("12.50")));
        seats.add(new Seat(screen, "E", 5, SeatType.REGULAR, new BigDecimal("12.50")));
        seats.add(new Seat(screen, "E", 6, SeatType.REGULAR, new BigDecimal("12.50")));
    }
}
