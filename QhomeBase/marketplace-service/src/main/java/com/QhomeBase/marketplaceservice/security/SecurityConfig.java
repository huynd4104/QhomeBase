package com.QhomeBase.marketplaceservice.security;

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
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        // Marketplace endpoints require RESIDENT role
                        .requestMatchers(HttpMethod.GET, "/categories").hasRole("RESIDENT")
                        .requestMatchers(HttpMethod.GET, "/posts").hasRole("RESIDENT")
                        .requestMatchers(HttpMethod.GET, "/posts/{id}").hasRole("RESIDENT")
                        .requestMatchers(HttpMethod.GET, "/posts/{id}/comments").hasRole("RESIDENT")
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll() // Images are public for marketplace posts
                        .requestMatchers(HttpMethod.POST, "/uploads/marketplace/comment/**").hasRole("RESIDENT") // Upload comment images/videos requires RESIDENT role
                        .requestMatchers(HttpMethod.GET, "/media/video").permitAll() // Video proxy endpoint - public access (API Gateway strips /api/marketplace prefix)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

