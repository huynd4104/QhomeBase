package com.QhomeBase.iamservice.client;

import com.QhomeBase.iamservice.security.JwtAuthFilter;
import com.QhomeBase.iamservice.security.JwtIssuer;
import com.QhomeBase.iamservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class WebClientService {

    private final JwtIssuer jwtIssuer;

    public String baseWebClient(String targetService) {
        var ctx = SecurityContextHolder.getContext();
        var p = (UserPrincipal) ctx.getAuthentication().getPrincipal();
        String jwt = jwtIssuer.issueForService(
                p.uid(),
                p.username(),
                p.tenant(),
                p.roles(),
                null,
                targetService
        );
        return "Bearer " + jwt;
    }

    public WebClient getWebClient(String baseUrl, String targetService) {
        String jwtBearer = baseWebClient(targetService);
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, jwtBearer)
                .build();
    }
}
