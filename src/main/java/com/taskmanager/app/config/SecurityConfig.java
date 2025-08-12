package com.taskmanager.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Step 1: Configure which endpoints are secured
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers("/api/auth/**").permitAll()          // Login/Register endpoints
                        .requestMatchers("/hello").permitAll()               // Hello endpoint for testing
                        .requestMatchers("/api/users/check/**").permitAll()  // Username/email availability check

                        // Protected endpoints (authentication required)
                        .requestMatchers("/api/users/**").authenticated()    // User management
                        .requestMatchers("/api/tasks/**").authenticated()    // Task management

                        // All other requests need authentication
                        .anyRequest().authenticated()
                )

                // Use HTTP Basic Authentication (for now)
                .httpBasic(basic -> {})

                // Disable CSRF for REST APIs (we'll use JWT later)
                .csrf(csrf -> csrf.disable())

                // Disable form login (we're building REST API)
                .formLogin(form -> form.disable());

        return http.build();
    }

    // Step 2: Password encoder bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}