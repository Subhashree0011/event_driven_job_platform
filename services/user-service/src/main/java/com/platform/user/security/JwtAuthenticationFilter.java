package com.platform.user.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom authentication filter for handling login requests.
 * In our design, login is handled by AuthController + AuthService,
 * so this serves as the entry point for unauthenticated requests.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.debug("Unauthorized request to: {}", request.getRequestURI());
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("""
            {
                "status": 401,
                "error": "UNAUTHORIZED",
                "message": "Authentication required. Please provide a valid JWT token.",
                "path": "%s"
            }
            """.formatted(request.getRequestURI()));
    }
}
