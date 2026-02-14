package com.QhomeBase.assetmaintenanceservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class FinanceBillingClient {

    @Qualifier("financeWebClient")
    private final WebClient financeWebClient;

    public FinanceBillingClient(WebClient financeWebClient) {
        this.financeWebClient = financeWebClient;
    }

    public Mono<Map<String, Object>> createInvoice(Map<String, Object> request) {
        return financeWebClient
                .post()
                .uri("/api/invoices")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    log.error("❌ [FinanceBillingClient] Finance service returned error status: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                            .doOnNext(body -> log.error("❌ [FinanceBillingClient] Error response body: {}", body))
                            .flatMap(body -> Mono.error(new RuntimeException("Finance service error: " + response.statusCode() + " - " + body)));
                })
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientRequestException)
                        .doBeforeRetry(retrySignal -> 
                            log.warn("🔄 [FinanceBillingClient] Retrying finance service invoice creation (attempt {}/3): {}", 
                                retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                .doOnSuccess(result -> log.info("✅ [FinanceBillingClient] Finance returned invoice {}", result != null ? result.get("id") : "null"))
                .doOnError(error -> log.error("❌ [FinanceBillingClient] Finance invoice creation failed: {}", error.getMessage(), error));
    }

    public Map<String, Object> createInvoiceSync(Map<String, Object> request) {
        try {
            Map<String, Object> invoice = createInvoice(request).block();
            log.info("Created invoice {} in finance-billing for service booking", 
                    invoice != null ? invoice.get("id") : "null");
            return invoice;
        } catch (Exception e) {
            log.error("❌ FAILED to create invoice in finance-billing", e);
            throw new RuntimeException("Failed to create invoice in finance-billing: " + e.getMessage(), e);
        }
    }
}
