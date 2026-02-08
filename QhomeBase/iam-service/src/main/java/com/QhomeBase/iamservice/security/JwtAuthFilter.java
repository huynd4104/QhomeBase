package com.QhomeBase.iamservice.security;

import com.QhomeBase.iamservice.service.token.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
    private final TokenBlacklistService tokenBlacklistService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                String token = auth.substring(7);
                Claims claims = jwtVerifier.verify(token);

                String jti = claims.getId();
                if (jti == null) {
                    throw new IllegalStateException("Missing jti");
                }
                if (tokenBlacklistService.isBlacklisted(jti)) {
                    throw new SecurityException("Token revoked");
                }
                UUID uid = UUID.fromString(claims.get("uid", String.class));
                String username = claims.getSubject();
                Object tenantClaim = claims.get("tenant");
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);
                @SuppressWarnings("unchecked")
                List<String> perms = claims.get("perms", List.class);

                var authorities = new ArrayList<SimpleGrantedAuthority>();
                if (roles != null) {
                    for (String role : roles) {
                        String normalizedRole = role != null ? role.toUpperCase() : null;
                        if (normalizedRole != null) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
                        }
                    }
                }
                if (perms != null) {
                    for (String perm : perms) {
                        authorities.add(new SimpleGrantedAuthority("PERM_" + perm));
                    }
                }
                
                var principal = new UserPrincipal(uid, username, jti, roles != null ? roles : new ArrayList<>(), perms != null ? perms : new ArrayList<>(), token);
                var authn = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authn);
            }
            catch (Exception e) {

                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        

        filterChain.doFilter(request, response);
    }
}
