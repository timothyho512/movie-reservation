package com.example.moviereservation.config;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ScreenLayoutVersionRepository;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.TheatreRepository;
import com.example.moviereservation.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    @Mock private UserRepository userRepository;
    @Mock private TheatreRepository theatreRepository;
    @Mock private ScreenRepository screenRepository;
    @Mock private ScreenLayoutVersionRepository screenLayoutVersionRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private MovieRepository movieRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void productionModeSeedsCustomerButNotAdministrator() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("hash");
        when(movieRepository.findByTitle("Inception")).thenReturn(Optional.of(new Movie()));

        seeder(false).run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("demo.customer@example.com");
    }

    @Test
    void existingCatalogueIsNotDuplicated() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(new User()));
        when(movieRepository.findByTitle("Inception")).thenReturn(Optional.of(new Movie()));

        seeder(false).run();

        verify(theatreRepository, never()).saveAll(any());
    }

    private DemoDataSeeder seeder(boolean adminEnabled) {
        return new DemoDataSeeder(
                userRepository,
                theatreRepository,
                screenRepository,
                screenLayoutVersionRepository,
                seatRepository,
                movieRepository,
                passwordEncoder,
                adminEnabled
        );
    }
}
