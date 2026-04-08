package com.example.moviereservation;

import com.example.moviereservation.entity.User;
import com.example.moviereservation.entity.UserRole;
import com.example.moviereservation.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerReturnsCreatedTokenAndSafeUserResponse() throws Exception {
        String requestBody = """
                {
                  "firstName": "Jay",
                  "lastName": "Doe",
                  "email": "Jay@example.com",
                  "password": "password123",
                  "phoneNumber": "07123456789"
                }
                """;

        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.id").isNumber())
                .andExpect(jsonPath("$.user.firstName").value("Jay"))
                .andExpect(jsonPath("$.user.lastName").value("Doe"))
                .andExpect(jsonPath("$.user.email").value("jay@example.com"))
                .andExpect(jsonPath("$.user.phoneNumber").value("07123456789"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.active").value(true))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        Long userId = json.get("user").get("id").asLong();

        User savedUser = userRepository.findById(userId).orElseThrow();

        assertThat(savedUser.getEmail()).isEqualTo("jay@example.com");
        assertThat(savedUser.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();
    }

    @Test
    void loginReturnsTokenForValidCredentials() throws Exception {
        User user = new User();
        user.setFirstName("Jay");
        user.setLastName("Doe");
        user.setEmail("jay@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setPhoneNumber("07123456789");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        String requestBody = """
                {
                  "email": "jay@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.id").isNumber())
                .andExpect(jsonPath("$.user.email").value("jay@example.com"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void loginFailsForWrongPassword() throws Exception {
        User user = new User();
        user.setFirstName("Jay");
        user.setLastName("Doe");
        user.setEmail("jay@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setPhoneNumber("07123456789");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        String requestBody = """
                {
                  "email": "jay@example.com",
                  "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void meReturnsAuthenticatedUserWhenBearerTokenIsValid() throws Exception {
        String registerRequest = """
                {
                  "firstName": "Jay",
                  "lastName": "Doe",
                  "email": "jay@example.com",
                  "password": "password123",
                  "phoneNumber": "07123456789"
                }
                """;

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(registerResponse).get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Jay"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("jay@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void meRejectsRequestWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    @Test
    void registerFailsWhenEmailAlreadyExists() throws Exception {
        User existingUser = new User();
        existingUser.setFirstName("Existing");
        existingUser.setLastName("User");
        existingUser.setEmail("jay@example.com");
        existingUser.setPassword(passwordEncoder.encode("password123"));
        existingUser.setPhoneNumber("07123456789");
        existingUser.setRole(UserRole.CUSTOMER);
        existingUser.setActive(true);
        existingUser.setCreatedAt(LocalDateTime.now());
        existingUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(existingUser);

        String requestBody = """
                {
                  "firstName": "Jay",
                  "lastName": "Doe",
                  "email": "jay@example.com",
                  "password": "password123",
                  "phoneNumber": "07999999999"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void usersEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required to access this resource"));
    }

    @Test
    void meRejectsInvalidBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required to access this resource"));
    }

}
