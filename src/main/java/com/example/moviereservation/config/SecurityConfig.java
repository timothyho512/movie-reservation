package com.example.moviereservation.config;

import com.example.moviereservation.security.JwtAuthenticationFilter;
import com.example.moviereservation.security.RestAuthenticationEntryPoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    private static final String[] ADMIN_ROLES = {"ADMIN", "MANAGER"};

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/health").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/admin/**").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers("/api/users/**").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/movies/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/movies").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/api/movies/**").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.DELETE, "/api/movies/**").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/theatres/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/theatres").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/api/theatres/**").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.DELETE, "/api/theatres/**").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/screens/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/screens").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/api/screens/**").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.DELETE, "/api/screens/**").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/seats/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/seats").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/api/seats/**").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.DELETE, "/api/seats/**").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/showtimes/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/showtimes").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/api/showtimes/**").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.DELETE, "/api/showtimes/**").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers("/checkout/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reservations/*/cancel").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/reservations/reference/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reservations").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/reservations/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reservations").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/**").hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/**").hasAnyRole(ADMIN_ROLES)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Stripe-Signature", "Idempotency-Key"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
