package com.QhomeBase.chatservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtVerifier jwtVerifier;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // DEV LOCAL mode: Skip JWT filter for WebSocket handshake
        // WebSocket handshake must succeed to return 101 Switching Protocols
        // JWT validation happens during STOMP CONNECT frame, not HTTP handshake
        String path = request.getRequestURI();
        if (path != null && (path.startsWith("/ws") || path.startsWith("/ws/"))) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String auth = request.getHeader("Authorization");
        
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                String token = auth.substring(7);
                Claims claims = jwtVerifier.verify(token);

                UUID uid = UUID.fromString(claims.get("uid", String.class));
                String username = claims.getSubject();
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);
                @SuppressWarnings("unchecked")
                List<String> perms = claims.get("perms", List.class);

                System.out.println("=== JWT AUTH FILTER DEBUG ===");
                System.out.println("Request path: " + request.getRequestURI());
                System.out.println("UID: " + uid);
                System.out.println("Username: " + username);
                System.out.println("Roles from JWT: " + roles);
                System.out.println("Perms from JWT: " + perms);

                var authorities = new ArrayList<SimpleGrantedAuthority>();
                if (roles != null) {
                    for (String role : roles) {
                        if (role != null) {
                            // Normalize role: convert to uppercase and ensure ROLE_ prefix
                            String normalizedRole = role.toUpperCase();
                            // If role already has ROLE_ prefix, don't add it again
                            if (!normalizedRole.startsWith("ROLE_")) {
                                normalizedRole = "ROLE_" + normalizedRole;
                            }
                            authorities.add(new SimpleGrantedAuthority(normalizedRole));
                            System.out.println("Added authority: " + normalizedRole);
                        }
                    }
                } else {
                    System.out.println("⚠️ WARNING: Roles list is null!");
                }
                if (perms != null) {
                    for (String perm : perms) {
                        if (perm != null) {
                            authorities.add(new SimpleGrantedAuthority("PERM_" + perm));
                        }
                    }
                }
                System.out.println("All authorities: " + authorities);
                System.out.println("==============================");

                var principal = new UserPrincipal(uid, username, roles, perms, token);
                var authn = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authn);
            }
            catch (Exception e) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Invalid token\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

