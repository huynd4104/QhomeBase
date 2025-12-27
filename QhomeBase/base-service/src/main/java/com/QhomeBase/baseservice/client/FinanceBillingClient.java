package com.QhomeBase.baseservice.client;

import com.QhomeBase.baseservice.dto.BillingImportedReadingDto;
import com.QhomeBase.baseservice.dto.MeterReadingImportResponse;
import com.QhomeBase.baseservice.dto.VehicleActivatedEvent;
import com.QhomeBase.baseservice.dto.finance.BillingCycleDto;
import com.QhomeBase.baseservice.dto.finance.CreateBillingCycleRequest;
import com.QhomeBase.baseservice.dto.finance.CreateInvoiceRequest;
import com.QhomeBase.baseservice.dto.finance.InvoiceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class FinanceBillingClient {
    
    private final WebClient financeWebClient;

    public FinanceBillingClient(@Qualifier("financeWebClient") WebClient financeWebClient) {
        this.financeWebClient = financeWebClient;
    }

    public Mono<Void> notifyVehicleActivated(VehicleActivatedEvent event) {
        return financeWebClient
                .post()
                .uri("/api/parking/invoices/generate-prorata")
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    public void notifyVehicleActivatedSync(VehicleActivatedEvent event) {
        log.info("Notifying finance service: Vehicle activated - vehicleId={}, plateNo={}, residentId={}", 
                event.getVehicleId(), event.getPlateNo(), event.getResidentId());
        
        try {
            notifyVehicleActivated(event).block();
            log.info("Finance service notified successfully - Invoice should be created for vehicle: {}", 
                    event.getPlateNo());
        } catch (Exception e) {
            log.error("FAILED to notify finance service for vehicle: {} ({}). Invoice NOT created!", 
                    event.getPlateNo(), event.getVehicleId(), e);
            log.error("   Error details: {}", e.getMessage());
            log.error("   Finance service may be down. Please create invoice manually or retry.");
        }
    }

    public Mono<MeterReadingImportResponse> importMeterReadings(List<BillingImportedReadingDto> readings) {
        return financeWebClient
                .post()
                .uri("/api/meter-readings/import")
                .bodyValue(readings)
                .retrieve()
                .bodyToMono(MeterReadingImportResponse.class);
    }

    public MeterReadingImportResponse importMeterReadingsSync(List<BillingImportedReadingDto> readings) {
        try {
            MeterReadingImportResponse response = importMeterReadings(readings).block();
            log.info("Imported {} readings to finance-billing. Invoices created: {}", 
                    readings != null ? readings.size() : 0,
                    response != null ? response.getInvoicesCreated() : 0);
            return response;
        } catch (Exception e) {
            log.error("❌ FAILED to import meter readings to finance-billing", e);
            throw new RuntimeException("Failed to import meter readings to finance-billing: " + e.getMessage(), e);
        }
    }

    public Mono<BillingCycleDto> createBillingCycle(CreateBillingCycleRequest request) {
        log.debug("Calling finance service to create billing cycle: {}", request);
        return financeWebClient
                .post()
                .uri("/api/billing-cycles")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BillingCycleDto.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientRequestException)
                        .doBeforeRetry(retrySignal -> 
                            log.warn("Retrying finance service call (attempt {}/3): {}", 
                                retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                .doOnSuccess(dto -> log.debug("Finance returned billing cycle {}", dto != null ? dto.getId() : "null"))
                .doOnError(error -> log.error("Finance billing cycle creation failed after retries", error));
    }

    public Mono<List<BillingCycleDto>> findBillingCyclesByExternalId(UUID externalCycleId) {
        log.debug("Checking finance for existing billing cycle: {}", externalCycleId);
        return financeWebClient
                .get()
                .uri("/api/billing-cycles/external/{externalCycleId}", externalCycleId)
                .retrieve()
                .bodyToFlux(BillingCycleDto.class)
                .collectList()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientRequestException)
                        .doBeforeRetry(retrySignal -> 
                            log.warn("Retrying finance service query (attempt {}/3): {}", 
                                retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                .doOnError(error -> log.error("Failed to query billing cycles for external id {} after retries", externalCycleId, error));
    }

    public Mono<InvoiceDto> createInvoice(CreateInvoiceRequest request) {
        log.debug("Calling finance service to create invoice for unit: {}", request.getPayerUnitId());
        return financeWebClient
                .post()
                .uri("/api/invoices")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InvoiceDto.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientRequestException)
                        .doBeforeRetry(retrySignal -> 
                            log.warn("Retrying finance service invoice creation (attempt {}/3): {}", 
                                retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                .doOnSuccess(dto -> log.debug("Finance returned invoice {}", dto != null ? dto.getId() : "null"))
                .doOnError(error -> log.error("Finance invoice creation failed after retries", error));
    }

    public InvoiceDto createInvoiceSync(CreateInvoiceRequest request) {
        try {
            InvoiceDto invoice = createInvoice(request).block();
            log.info("Created invoice {} in finance-billing for unit: {}", 
                    invoice != null ? invoice.getId() : "null",
                    request.getPayerUnitId());
            return invoice;
        } catch (Exception e) {
            log.error("❌ FAILED to create invoice in finance-billing", e);
            throw new RuntimeException("Failed to create invoice in finance-billing: " + e.getMessage(), e);
        }
    }

    public Mono<List<InvoiceDto>> getInvoicesByUnit(UUID unitId) {
        log.debug("Getting invoices for unit: {}", unitId);
        return financeWebClient
                .get()
                .uri("/api/invoices/unit/{unitId}", unitId)
                .retrieve()
                .bodyToFlux(InvoiceDto.class)
                .collectList()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientRequestException)
                        .doBeforeRetry(retrySignal -> 
                            log.warn("Retrying finance service invoice query (attempt {}/3): {}", 
                                retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                .doOnError(error -> log.error("Failed to get invoices for unit {} after retries", unitId, error));
    }

    public List<InvoiceDto> getInvoicesByUnitSync(UUID unitId) {
        try {
            List<InvoiceDto> invoices = getInvoicesByUnit(unitId).block();
            log.debug("Retrieved {} invoices for unit: {}", 
                    invoices != null ? invoices.size() : 0, unitId);
            return invoices != null ? invoices : List.of();
        } catch (Exception e) {
            log.error("❌ FAILED to get invoices for unit: {}", unitId, e);
            // Return empty list instead of throwing - we can still create invoice without water/electric costs
            return List.of();
        }
    }
}