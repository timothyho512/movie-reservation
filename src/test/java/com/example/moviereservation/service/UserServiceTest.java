package com.example.moviereservation.service;

import com.example.moviereservation.dto.UserRequest;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createUserHashesPasswordBeforeSaving() {
        UserRequest request = new UserRequest(
                "Jay",
                "Doe",
                "jay@example.com",
                "password123",
                "07911111111"
        );
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserService userService = new UserService(userRepository, passwordEncoder);
        User savedUser = userService.createUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("bcrypt-hash");
        assertThat(savedUser.getPassword()).isEqualTo("bcrypt-hash");
    }
}
