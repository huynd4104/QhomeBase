package com.QhomeBase.baseservice.client;

import com.QhomeBase.baseservice.dto.ContractDetailDto;
import com.QhomeBase.baseservice.dto.ContractFileDto;
import com.QhomeBase.baseservice.dto.ContractSummary;
import com.QhomeBase.baseservice.dto.CreateContractProxyRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Component
@Slf4j
public class ContractClient {

    @Qualifier("contractWebClient")
    private final WebClient contractWebClient;

    public ContractClient(WebClient contractWebClient){this.contractWebClient = contractWebClient;}

    public Optional<ContractDetailDto> getContractById(UUID contractId) {
        try {
            return contractWebClient.get()
                    .uri("/api/contracts/{contractId}", contractId)
                    .retrieve()
                    .bodyToMono(DataDocsContractDto.class)
                    .map(this::toDetailDto)
                    .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                        log.debug("Contract {} not found", contractId);
                        return Mono.empty();
                    })
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        // Handle 5xx errors gracefully
                        if (ex.getStatusCode().is5xxServerError()) {
                            log.warn("⚠️ [ContractClient] data-docs-service returned 5xx error for contract {}: {}", 
                                    contractId, ex.getStatusCode());
                            return Mono.empty();
                        }
                        log.error("Failed to fetch contract {}: {}", contractId, ex.getResponseBodyAsString());
                        return Mono.empty(); // Return empty instead of throwing
                    })
                    .onErrorResume(Exception.class, ex -> {
                        // Handle connection errors, timeouts, etc.
                        String errorMsg = ex.getMessage();
                        if (errorMsg != null && (errorMsg.contains("Connection reset") 
                                || errorMsg.contains("timeout") 
                                || errorMsg.contains("Connection refused")
                                || errorMsg.contains("Read timed out"))) {
                            log.warn("⚠️ [ContractClient] Connection error fetching contract {}: {} (data-docs-service may be unavailable)", 
                                    contractId, errorMsg);
                        } else {
                            log.error("❌ [ContractClient] Unexpected error fetching contract {}: {}", contractId, ex.getMessage(), ex);
                        }
                        return Mono.empty(); // Return empty instead of throwing
                    })
                    .blockOptional();
        } catch (Exception e) {
            // Catch any remaining exceptions
            log.warn("⚠️ [ContractClient] Exception fetching contract {}: {}", contractId, e.getMessage());
            return Optional.empty();
        }
    }

    public List<ContractSummary> getActiveContractsByUnit(UUID unitId) {
        try {
            return contractWebClient.get()
                    .uri("/api/contracts/unit/{unitId}/active", unitId)
                    .retrieve()
                    .bodyToFlux(DataDocsContractDto.class)
                    .map(this::toSummary)
                    .collectList()
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Collections.emptyList();
            }
            log.error("Failed to fetch active contracts for unit {}: {}", unitId, e.getResponseBodyAsString());
            throw e;
        }
    }

    public Optional<ContractSummary> findFirstActiveContract(UUID unitId) {
        return getActiveContractsByUnit(unitId)
                .stream()
                .filter(contract -> {
                    LocalDate today = LocalDate.now();
                    if (contract.isActiveOn(LocalDate.now())) {
                        return true;
                    }
                    String status = contract.status();
                    boolean isPending = status != null && status.equalsIgnoreCase("PENDING");
                    boolean dateValid = contract.startDate() == null || !today.isBefore(contract.startDate());
                    return isPending && dateValid;
                })
                .findFirst();
    }

    public List<ContractSummary> getContractsByUnit(UUID unitId) {
        try {
            return contractWebClient.get()
                    .uri("/api/contracts/unit/{unitId}", unitId)
                    .retrieve()
                    .bodyToFlux(DataDocsContractDto.class)
                    .map(this::toSummary)
                    .collectList()
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Collections.emptyList();
            }
            log.error("Failed to fetch contracts for unit {}: {}", unitId, e.getResponseBodyAsString());
            throw e;
        }
    }

    public ContractDetailDto createContract(CreateContractProxyRequest request, UUID createdBy) {
        try {
            return contractWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/contracts")
                            .queryParamIfPresent("createdBy", Optional.ofNullable(createdBy))
                            .build())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(DataDocsContractDto.class)
                    .map(this::toDetailDto)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Failed to create contract for unit {}: {}", request.unitId(), e.getResponseBodyAsString());
            throw e;
        }
    }

    public List<ContractFileDto> uploadContractFiles(UUID contractId, MultipartFile[] files, UUID uploadedBy) {
        if (files == null || files.length == 0) {
            return List.of();
        }

        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("contract-file");
                String contentType = Optional.ofNullable(file.getContentType())
                        .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                builder.part("files", file.getResource())
                        .filename(filename)
                        .contentType(MediaType.parseMediaType(contentType));
            }
            if (uploadedBy != null) {
                builder.part("uploadedBy", uploadedBy.toString());
            }

            return contractWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/contracts/{contractId}/files/multiple")
                            .build(contractId))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToFlux(DataDocsContractFileDto.class)
                    .map(this::toFileDto)
                    .collectList()
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Failed to upload files for contract {}: {}", contractId, e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error uploading files for contract {}", contractId, e);
            throw new RuntimeException("Failed to upload contract files", e);
        }
    }

    public ResponseEntity<byte[]> viewContractFile(UUID contractId, UUID fileId) {
        try {
            ResponseEntity<byte[]> response = contractWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/contracts/{contractId}/files/{fileId}/view")
                            .build(contractId, fileId))
                    .accept(MediaType.ALL)
                    .exchangeToMono(clientResponse -> clientResponse.toEntity(byte[].class))
                    .block();
            return response != null ? response : ResponseEntity.notFound().build();
        } catch (WebClientResponseException.NotFound e) {
            return ResponseEntity.notFound().build();
        } catch (WebClientResponseException e) {
            log.error("Failed to view contract file {} for contract {}: {}", fileId, contractId, e.getResponseBodyAsString());
            throw e;
        }
    }

    private ContractSummary toSummary(DataDocsContractDto dto) {
        return new ContractSummary(
                dto.id(),
                dto.unitId(),
                dto.contractNumber(),
                dto.contractType(),
                dto.startDate(),
                dto.endDate(),
                dto.status()
        );
    }

    private ContractDetailDto toDetailDto(DataDocsContractDto dto) {
        List<ContractFileDto> files = dto.files() == null
                ? List.of()
                : dto.files().stream()
                .map(this::toFileDto)
                .toList();
        return new ContractDetailDto(
                dto.id(),
                dto.unitId(),
                dto.contractNumber(),
                dto.contractType(),
                dto.startDate(),
                dto.endDate(),
                dto.monthlyRent(),
                dto.purchasePrice(),
                dto.paymentMethod(),
                dto.paymentTerms(),
                dto.purchaseDate(),
                dto.notes(),
                dto.status(),
                dto.createdBy(),
                dto.createdAt(),
                dto.updatedAt(),
                dto.updatedBy(),
                files
        );
    }

    private ContractFileDto toFileDto(DataDocsContractFileDto file) {
        return new ContractFileDto(
                file.id(),
                file.contractId(),
                file.fileName(),
                file.originalFileName(),
                file.fileUrl(),
                String.format("/api/contracts/%s/files/%s/view", file.contractId(), file.id()),
                file.contentType(),
                file.fileSize(),
                file.isPrimary(),
                file.displayOrder(),
                file.uploadedBy(),
                file.uploadedAt()
        );
    }

    private record DataDocsContractDto(
            UUID id,
            UUID unitId,
            String contractNumber,
            String contractType,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal monthlyRent,
            BigDecimal purchasePrice,
            String paymentMethod,
            String paymentTerms,
            LocalDate purchaseDate,
            String notes,
            String status,
            UUID createdBy,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            UUID updatedBy,
            List<DataDocsContractFileDto> files
    ) {}

    private record DataDocsContractFileDto(
            UUID id,
            UUID contractId,
            String fileName,
            String originalFileName,
            String fileUrl,
            String contentType,
            Long fileSize,
            Boolean isPrimary,
            Integer displayOrder,
            UUID uploadedBy,
            OffsetDateTime uploadedAt
    ) {}
}

