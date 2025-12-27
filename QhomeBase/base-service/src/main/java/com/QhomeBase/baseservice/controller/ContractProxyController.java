package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.client.ContractClient;
import com.QhomeBase.baseservice.dto.ContractDetailDto;
import com.QhomeBase.baseservice.dto.ContractSummary;
import com.QhomeBase.baseservice.dto.CreateContractProxyRequest;
import com.QhomeBase.baseservice.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@Slf4j
public class ContractProxyController {

    private final ContractClient contractClient;

    @GetMapping("/units/{unitId}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ContractSummary>> getActiveContractsByUnit(@PathVariable UUID unitId) {
        List<ContractSummary> contracts = contractClient.getActiveContractsByUnit(unitId);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/units/{unitId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ContractSummary>> getContractsByUnit(@PathVariable UUID unitId) {
        List<ContractSummary> contracts = contractClient.getContractsByUnit(unitId);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/{contractId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContractDetailDto> getContractById(@PathVariable UUID contractId) {
        return contractClient.getContractById(contractId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContractDetailDto> createContract(
            @RequestBody @Valid CreateContractProxyRequest request,
            Authentication authentication
    ) {
        UUID userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.uid();
        }
        CreateContractProxyRequest normalizedRequest = new CreateContractProxyRequest(
                request.unitId(),
                request.contractNumber(),
                request.contractType(),
                request.startDate(),
                request.endDate(),
                request.monthlyRent(),
                request.purchasePrice(),
                request.paymentMethod(),
                request.paymentTerms(),
                request.purchaseDate(),
                request.notes(),
                "ACTIVE"
        );

        ContractDetailDto contract = contractClient.createContract(normalizedRequest, userId);
        return ResponseEntity.status(201).body(contract);
    }

    @PostMapping("/{contractId}/files")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.QhomeBase.baseservice.dto.ContractFileDto>> uploadContractFiles(
            @PathVariable UUID contractId,
            @RequestPart("files") MultipartFile[] files,
            Authentication authentication
    ) {
        UUID userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.uid();
        }

        List<com.QhomeBase.baseservice.dto.ContractFileDto> uploaded = contractClient.uploadContractFiles(contractId, files, userId);
        return ResponseEntity.ok(uploaded);
    }

    @GetMapping("/{contractId}/files/{fileId}/view")
    public ResponseEntity<byte[]> viewContractFile(
            @PathVariable UUID contractId,
            @PathVariable UUID fileId
    ) {
        ResponseEntity<byte[]> response = contractClient.viewContractFile(contractId, fileId);
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(response.getBody());
    }
}

