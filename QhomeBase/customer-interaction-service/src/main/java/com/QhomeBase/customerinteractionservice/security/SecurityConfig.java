package com.QhomeBase.customerinteractionservice.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            @Qualifier("corsConfigurationSource")
            CorsConfigurationSource corsConfigurationSource
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/test-websocket.html").permitAll()
                        .requestMatchers("/static/**", "/*.html", "/*.css", "/*.js").permitAll()
                        .requestMatchers("/api/news/resident/**").permitAll()
                        .requestMatchers("/api/news/*/resident").permitAll()
                        .requestMatchers("/api/news/*/read").permitAll()
                        .requestMatchers("/api/news/unread/count").permitAll()
                        // /api/notifications/resident requires authentication and RESIDENT role (handled by @PreAuthorize)
                        .requestMatchers("/api/notifications/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/notifications/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/notifications/internal").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/notifications/push-only").permitAll()
                        // Allow device token registration without authentication (needed for push notifications before login)
                        .requestMatchers(HttpMethod.POST, "/api/notifications/device-tokens").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/notifications/device-tokens/**").permitAll()
                        .requestMatchers("/api/customer-interaction/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
