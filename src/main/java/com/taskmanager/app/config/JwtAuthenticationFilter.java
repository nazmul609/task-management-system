package com.taskmanager.app.config;

import com.taskmanager.app.service.CustomUserDetailsService;
import com.taskmanager.app.service.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 *
 * This filter runs on every HTTP request and:
 * 1. Extracts JWT token from Authorization header
 * 2. Validates the token
 * 3. Sets authentication in Security Context
 *
 * Flow:
 * Request → Extract Token → Validate Token → Set Authentication → Continue
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Extract Authorization header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Check if Authorization header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token (remove "Bearer " prefix)
        jwt = authHeader.substring(7);

        try {
            // Extract username from JWT token
            username = jwtUtil.extractUsername(jwt);

            // If username exists and user is not already authenticated
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load user details from database
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Validate JWT token against user details
                if (jwtUtil.validateToken(jwt, userDetails)) {

                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null, // No credentials needed for JWT
                                    userDetails.getAuthorities()
                            );

                    // Set authentication details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in Security Context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Log error and continue without authentication
            logger.error("Cannot set user authentication: {}", e);
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}