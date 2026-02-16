package com.platform.job.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security config for Job Service.
 * Authentication is handled by the API Gateway / User Service.
 * Job Service trusts X-User-Id header set by the gateway after JWT validation.
 *
 * The XUserIdAuthFilter creates a Spring Security principal from the header
 * so that .authenticated() rules work correctly.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final XUserIdAuthFilter xUserIdAuthFilter;

    public SecurityConfig(XUserIdAuthFilter xUserIdAuthFilter) {
        this.xUserIdAuthFilter = xUserIdAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(xUserIdAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — anyone can search and view jobs
                        .requestMatchers("/api/v1/jobs/search", "/api/v1/jobs/{jobId}").permitAll()
                        .requestMatchers("/api/v1/jobs/company/**").permitAll()
                        .requestMatchers("/api/v1/jobs/companies", "/api/v1/jobs/companies/**").permitAll()
                        // Actuator endpoints for monitoring
                        .requestMatchers("/actuator/**").permitAll()
                        // All other endpoints require authentication (X-User-Id header)
                        .anyRequest().authenticated())
                // Use anonymous authentication for public endpoints
                .anonymous(anonymous -> {
                });

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
