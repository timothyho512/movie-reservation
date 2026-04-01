package com.example.moviereservation.config;

import com.example.moviereservation.entity.*;
import com.example.moviereservation.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("dev") // Only run this in the 'dev' profile
public class DemoDataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    public DemoDataSeeder(
            UserRepository userRepository,
            TheatreRepository theatreRepository,
            ScreenRepository screenRepository,
            SeatRepository seatRepository,
            MovieRepository movieRepository,
            ShowtimeRepository showtimeRepository
    ) {
        this.userRepository = userRepository;
        this.theatreRepository = theatreRepository;
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0
                || theatreRepository.count() > 0
                || movieRepository.count() > 0
                || showtimeRepository.count() > 0) {
            return;
        }

        // Create demo users
        User customer = new User("Alice", "Customer", "alice@example.com", "password123", "07111111111");
        customer.setRole(UserRole.CUSTOMER);

        User admin = new User("Bob", "Admin", "bob@example.com", "password123", "07222222222");
        admin.setRole(UserRole.ADMIN);

        User manager = new User("Carol", "Manager", "carol@example.com", "password123", "07333333333");
        manager.setRole(UserRole.MANAGER);

        User inactiveCustomer = new User("Dave", "Inactive", "dave@example.com", "password123", "07444444444");
        inactiveCustomer.setRole(UserRole.CUSTOMER);
        inactiveCustomer.setActive(false);

        userRepository.saveAll(List.of(customer, admin, manager, inactiveCustomer));


        // Create demo theaters
        Theatre theatre1 = new Theatre(
        "Odeon Leicester Square",
        "24-26 Leicester Square",
        "London",
        "England",
        "United Kingdom",
        "WC2H 7JY"
        );
        theatre1.setTotalScreens(2);
        theatre1.setTotalSeats(12);

        Theatre theatre2 = new Theatre(
                "Vue Manchester Printworks",
                "Withy Grove",
                "Manchester",
                "England",
                "United Kingdom",
                "M4 2BS"
        );
        theatre2.setTotalScreens(2);
        theatre2.setTotalSeats(10);

        Theatre theatre3 = new Theatre(
                "Cineworld Glasgow Renfrew Street",
                "Renfrew Street",
                "Glasgow",
                "Scotland",
                "United Kingdom",
                "G2 3BW"
        );
        theatre3.setTotalScreens(1);
        theatre3.setTotalSeats(8);
        theatre3.setActive(false);

        theatreRepository.saveAll(List.of(theatre1, theatre2, theatre3));


        // Create demo screen
        Screen screen1 = new Screen("Screen 1", theatre1, 6, ScreenType.STANDARD);
        Screen screen2 = new Screen("Screen 2", theatre1, 6, ScreenType.IMAX);
        Screen screen3 = new Screen("Screen 1", theatre2, 5, ScreenType.THREE_D);
        Screen screen4 = new Screen("Screen 2", theatre2, 5, ScreenType.VIP);
        Screen screen5 = new Screen("Screen 1", theatre3, 8, ScreenType.DOLBY_ATMOS);
        screen5.setActive(false);

        screenRepository.saveAll(List.of(screen1, screen2, screen3, screen4, screen5));


        // Create demo seats
        List<Seat> seats1 = buildSeats(screen1, 6);
        List<Seat> seats2 = buildSeats(screen2, 6);
        List<Seat> seats3 = buildSeats(screen3, 5);
        List<Seat> seats4 = buildSeats(screen4, 5);
        List<Seat> seats5 = buildSeats(screen5, 8);
        
        seatRepository.saveAll(seats1);
        seatRepository.saveAll(seats2);
        seatRepository.saveAll(seats3);
        seatRepository.saveAll(seats4);
        seatRepository.saveAll(seats5);


        // Create demo movie
        Movie movie1 = new Movie("Inception", "Christopher Nolan");
        Movie movie2 = new Movie("Interstellar", "Christopher Nolan");
        Movie movie3 = new Movie("Spirited Away", "Hayao Miyazaki");
        Movie movie4 = new Movie("The Grand Budapest Hotel", "Wes Anderson");

        movieRepository.saveAll(List.of(movie1, movie2, movie3, movie4));


        // Create demo showtime
        LocalDateTime now = LocalDateTime.now();

        Showtime st1 = new Showtime(
                movie1,
                screen1,
                now.plusDays(1).withHour(19).withMinute(0).withSecond(0).withNano(0),
                now.plusDays(1).withHour(21).withMinute(30).withSecond(0).withNano(0),
                new BigDecimal("14.00")
        );

        Showtime st2 = new Showtime(
                movie2,
                screen2,
                now.plusDays(1).withHour(20).withMinute(0).withSecond(0).withNano(0),
                now.plusDays(1).withHour(22).withMinute(45).withSecond(0).withNano(0),
                new BigDecimal("16.00")
        );

        Showtime st3 = new Showtime(
                movie3,
                screen3,
                now.minusMinutes(30),
                now.plusHours(2),
                new BigDecimal("13.50")
        );
        st3.setStatus(ShowtimeStatus.ONGOING);

        Showtime st4 = new Showtime(
                movie4,
                screen4,
                now.minusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0),
                now.minusDays(1).withHour(20).withMinute(0).withSecond(0).withNano(0),
                new BigDecimal("15.00")
        );
        st4.setStatus(ShowtimeStatus.COMPLETED);

        Showtime st5 = new Showtime(
                movie1,
                screen1,
                now.plusDays(2).withHour(17).withMinute(30).withSecond(0).withNano(0),
                now.plusDays(2).withHour(20).withMinute(0).withSecond(0).withNano(0),
                new BigDecimal("14.50")
        );
        st5.setStatus(ShowtimeStatus.CANCELLED);

        showtimeRepository.saveAll(List.of(st1, st2, st3, st4, st5));
    }

    // helper function to build seats for a screen
    private List<Seat> buildSeats(Screen screen, int seatsPerRow) {
        return List.of(
                new Seat(screen, "A", 1, SeatType.REGULAR, new BigDecimal("12.50")),
                new Seat(screen, "A", 2, SeatType.REGULAR, new BigDecimal("12.50")),
                new Seat(screen, "A", 3, SeatType.REGULAR, new BigDecimal("12.50")),
                new Seat(screen, "B", 1, SeatType.VIP, new BigDecimal("18.00")),
                new Seat(screen, "B", 2, SeatType.VIP, new BigDecimal("18.00")),
                new Seat(screen, "B", 3, SeatType.WHEELCHAIR, new BigDecimal("10.00"))
        );
    }

}