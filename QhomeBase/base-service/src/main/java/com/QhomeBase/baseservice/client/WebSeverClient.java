package com.QhomeBase.baseservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class WebSeverClient {
    
    private final WebClient webClient;

    public WebSeverClient(@Qualifier("iamWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Flux<UUID> getManagersTenantsIDs(UUID tenantID) {
        return webClient.get()
                .uri("/api/tenants/roles/{tenantId}/managers", tenantID)
                .retrieve()
                .bodyToFlux(UUID.class);
    }

    public Mono<Void> unassignAllEmployeesFromTenant(UUID tenantId) {
        return webClient
                .post()
                .uri("/api/employee-roles/{tenantId}/employees/unassign-all", tenantId)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> 
                    log.info("Successfully unassigned all employees from tenant: {}", tenantId))
                .doOnError(error -> 
                    log.error("Failed to unassign employees from tenant: {}", tenantId, error))
                .then();
    }


    public Mono<List<Object>> getEmployeesInTenant(UUID tenantId) {
        return webClient
                .get()
                .uri("/api/employee-roles/tenant/{tenantId}", tenantId)
                .retrieve()
                .bodyToFlux(Object.class)
                .collectList()
                .doOnSuccess(employees -> 
                    log.info("Found {} employees in tenant: {}", employees.size(), tenantId))
                .doOnError(error -> 
                    log.error("Failed to get employees from tenant: {}", tenantId, error));
    }


    public void unassignAllEmployeesFromTenantSync(UUID tenantId) {
        try {
            unassignAllEmployeesFromTenant(tenantId).block();
        } catch (Exception e) {
            log.error("Error unassigning employees from tenant: {}", tenantId, e);
        }
    }
}
