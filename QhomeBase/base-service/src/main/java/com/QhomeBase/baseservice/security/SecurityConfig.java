package com.QhomeBase.baseservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/test/public").permitAll()
                        .requestMatchers("/api/test/generate-token").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/buildings/**").permitAll()
                        .requestMatchers("/api/services/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/contracts/*/files/*/view").permitAll()
                        .requestMatchers("/api/reading-cycles/**").permitAll()
                        .requestMatchers("/api/meter-reading-assignments/**").permitAll()
                        .requestMatchers("/api/units/**").permitAll()
                        // VNPay callback endpoint - must be public for payment gateway redirect
                        .requestMatchers(HttpMethod.GET, "/api/maintenance-requests/vnpay/redirect").permitAll()
                        // Internal service calls - allow service-to-service communication
                        // Allow finance-billing-service and other services to get household/unit/building info
                        .requestMatchers("/api/household-members/residents/**").permitAll()
                        .requestMatchers("/api/household-members/households/**").permitAll() // Allow service-to-service calls
                        .requestMatchers("/api/households/**").permitAll()
                        // Allow service-to-service calls for asset inspections (from data-docs-service)
                        .requestMatchers("/api/asset-inspections").permitAll()
                        // Allow service-to-service calls for residents (from asset-maintenance-service for invoice creation)
                        .requestMatchers("/api/residents/by-user/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
