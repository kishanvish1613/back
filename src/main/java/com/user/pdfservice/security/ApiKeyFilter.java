package com.user.pdfservice.security;

import com.user.pdfservice.model.entity.User;
import com.user.pdfservice.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${API_KEY}")
    private String apiKey;

    private final AuthService authService;

    public ApiKeyFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        // Handle CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Allow public authentication and health routes without requiring API key
        if (path.startsWith("/api/auth/") ||
            path.equals("/pdf/health") ||
            path.equals("/pdf/templates") ||
            path.equals("/pdf/preview")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Check API Key header
        String keyFromRequest = request.getHeader("X-API-KEY");
        if (keyFromRequest != null && keyFromRequest.equals(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Check Bearer Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            User user = authService.validateSession(token);
            if (user != null) {
                // Attach authenticated user info as request attributes
                request.setAttribute("authenticatedUser", user);
                request.setAttribute("username", user.getUsername());
                request.setAttribute("userRole", user.getRole());
                filterChain.doFilter(request, response);
                return;
            } else {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid or expired session. Please log in again.\"}");
                return;
            }
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Authentication required. Please provide a valid token or API key.\"}");
    }
}
