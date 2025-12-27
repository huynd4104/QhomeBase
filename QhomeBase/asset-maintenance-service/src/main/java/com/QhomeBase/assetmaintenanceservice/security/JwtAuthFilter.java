package com.QhomeBase.assetmaintenanceservice.security;

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
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                String token = auth.substring(7).trim();
                if (token.regionMatches(true, 0, "Bearer", 0, 6)) {
                    token = token.substring(6).trim();
                }
                token = token.replaceAll("[^A-Za-z0-9\\-_.]", "");
                Claims claims = jwtVerifier.verify(token);

                UUID uid = UUID.fromString(claims.get("uid", String.class));
                String username = claims.getSubject();
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);
                @SuppressWarnings("unchecked")
                List<String> perms = claims.get("perms", List.class);

                var authorities = new ArrayList<SimpleGrantedAuthority>();
                if (roles != null) {
                    for (String role : roles) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }
                }
                if (perms != null) {
                    for (String perm : perms) {
                        authorities.add(new SimpleGrantedAuthority("PERM_" + perm));
                    }
                }

                var principal = new UserPrincipal(uid, username, roles, perms, token);
                var authn = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authn);
            }
            catch (Exception e) {
                System.err.println("JWT Verification failed: " + e.getMessage());
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

