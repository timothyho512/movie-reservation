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
    private static final String CUSTOMER_EMAIL = "auth.jay@example.com";
    private static final String ADMIN_EMAIL = "auth.admin@example.com";

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
        userRepository.findByEmail(CUSTOMER_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(ADMIN_EMAIL).ifPresent(userRepository::delete);
        userRepository.flush();
    }

    @Test
    void registerReturnsCreatedTokenAndSafeUserResponse() throws Exception {
        String requestBody = """
                {
                  "firstName": "Jay",
                  "lastName": "Doe",
                  "email": "auth.jay@example.com",
                  "password": "password123",
                  "phoneNumber": "07911111111"
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
                .andExpect(jsonPath("$.user.email").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.user.phoneNumber").value("07911111111"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.active").value(true))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        Long userId = json.get("user").get("id").asLong();

        User savedUser = userRepository.findById(userId).orElseThrow();

        assertThat(savedUser.getEmail()).isEqualTo(CUSTOMER_EMAIL);
        assertThat(savedUser.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();
    }

    @Test
    void loginReturnsTokenForValidCredentials() throws Exception {
        User user = new User();
        user.setFirstName("Jay");
        user.setLastName("Doe");
        user.setEmail(CUSTOMER_EMAIL);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setPhoneNumber("07911111111");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        String requestBody = """
                {
                  "email": "auth.jay@example.com",
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
                .andExpect(jsonPath("$.user.email").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void loginFailsForWrongPassword() throws Exception {
        User user = new User();
        user.setFirstName("Jay");
        user.setLastName("Doe");
        user.setEmail(CUSTOMER_EMAIL);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setPhoneNumber("07911111111");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        String requestBody = """
                {
                  "email": "auth.jay@example.com",
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
                  "email": "auth.jay@example.com",
                  "password": "password123",
                  "phoneNumber": "07911111111"
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
                .andExpect(jsonPath("$.email").value(CUSTOMER_EMAIL))
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
        existingUser.setEmail(CUSTOMER_EMAIL);
        existingUser.setPassword(passwordEncoder.encode("password123"));
        existingUser.setPhoneNumber("07911111111");
        existingUser.setRole(UserRole.CUSTOMER);
        existingUser.setActive(true);
        existingUser.setCreatedAt(LocalDateTime.now());
        existingUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(existingUser);

        String requestBody = """
                {
                  "firstName": "Jay",
                  "lastName": "Doe",
                  "email": "auth.jay@example.com",
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
    void usersEndpointReturnsSafeUserDtosForAdminOnly() throws Exception {
        String registerRequest = """
                {
                  "firstName": "Jay",
                  "lastName": "Doe",
                  "email": "auth.jay@example.com",
                  "password": "password123",
                  "phoneNumber": "07911111111"
                }
                """;

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);
        String customerToken = registerJson.get("token").asText();
        Long userId = registerJson.get("user").get("id").asLong();

        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode("password123"));
        admin.setPhoneNumber("07922222222");
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        userRepository.save(admin);

        String adminToken = loginAndGetToken(ADMIN_EMAIL, "password123");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        String usersResponse = mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(usersResponse).contains("\"id\":" + userId);
        assertThat(usersResponse).contains("\"email\":\"" + CUSTOMER_EMAIL + "\"");
        assertThat(usersResponse).doesNotContain("password");

        mockMvc.perform(get("/api/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void meRejectsInvalidBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required to access this resource"));
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

        return objectMapper.readTree(response).get("token").asText();
    }

}
