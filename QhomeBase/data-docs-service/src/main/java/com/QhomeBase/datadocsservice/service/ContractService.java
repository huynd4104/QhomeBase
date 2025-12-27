package com.QhomeBase.datadocsservice.service;

import com.QhomeBase.datadocsservice.client.BaseServiceClient;
import com.QhomeBase.datadocsservice.client.InvoiceClient;
import com.QhomeBase.datadocsservice.config.VnpayProperties;
import com.QhomeBase.datadocsservice.dto.*;
import com.QhomeBase.datadocsservice.model.Contract;
import com.QhomeBase.datadocsservice.model.ContractFile;
import com.QhomeBase.datadocsservice.repository.ContractFileRepository;
import com.QhomeBase.datadocsservice.repository.ContractRepository;
import com.QhomeBase.datadocsservice.service.vnpay.VnpayService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractFileRepository contractFileRepository;
    private final FileStorageService fileStorageService;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final InvoiceClient invoiceClient;
    private final BaseServiceClient baseServiceClient;
    private final EntityManager entityManager;

    @Transactional
    public ContractDto createContract(CreateContractRequest request, UUID createdBy) {
        contractRepository.findByContractNumber(request.getContractNumber())
                .ifPresent(contract -> {
                    throw new IllegalArgumentException("Contract number already exists: " + request.getContractNumber());
                });

        String contractType = request.getContractType() != null ? request.getContractType() : "RENTAL";
        
        if (!"RENTAL".equals(contractType) && !"PURCHASE".equals(contractType)) {
            throw new IllegalArgumentException("Invalid contract type. Must be RENTAL or PURCHASE");
        }

        if ("RENTAL".equals(contractType)) {
            if (request.getEndDate() != null && request.getStartDate().isAfter(request.getEndDate())) {
                throw new IllegalArgumentException("Start date must be before or equal to end date");
            }
            if (request.getMonthlyRent() == null) {
                throw new IllegalArgumentException("Monthly rent is required for RENTAL contracts");
            }
            // Validate minimum 3 months for rental contracts
            if (request.getEndDate() != null && request.getStartDate() != null) {
                int months = (request.getEndDate().getYear() - request.getStartDate().getYear()) * 12
                        + (request.getEndDate().getMonthValue() - request.getStartDate().getMonthValue());
                if (months < 3) {
                    throw new IllegalArgumentException("Hợp đồng thuê phải tối thiểu 3 tháng");
                }
            }
        } else if ("PURCHASE".equals(contractType)) {
            if (request.getEndDate() != null) {
                throw new IllegalArgumentException("Purchase contracts cannot have end date");
            }
            if (request.getPurchasePrice() == null) {
                throw new IllegalArgumentException("Purchase price is required for PURCHASE contracts");
            }
            if (request.getPurchaseDate() == null) {
                throw new IllegalArgumentException("Purchase date is required for PURCHASE contracts");
            }
            if (request.getPaymentMethod() != null || request.getPaymentTerms() != null) {
                throw new IllegalArgumentException("Purchase contracts are fully paid. Payment method and terms are not applicable");
            }
        }
        
        Contract contract = Contract.builder()
                .unitId(request.getUnitId())
                .contractNumber(request.getContractNumber())
                .contractType(contractType)
                .startDate(request.getStartDate())
                .endDate("RENTAL".equals(contractType) ? request.getEndDate() : null)
                .monthlyRent("RENTAL".equals(contractType) ? request.getMonthlyRent() : null)
                .purchasePrice("PURCHASE".equals(contractType) ? request.getPurchasePrice() : null)
                .purchaseDate("PURCHASE".equals(contractType) ? request.getPurchaseDate() : null)
                .notes(request.getNotes())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .createdBy(createdBy)
                .build();

        contract = contractRepository.save(contract);
        log.info("Created contract: {} for unit: {}", contract.getId(), request.getUnitId());

        return toDto(contract);
    }

    @Transactional
    public ContractDto updateContract(UUID contractId, UpdateContractRequest request, UUID updatedBy) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        if (request.getContractNumber() != null && !request.getContractNumber().equals(contract.getContractNumber())) {
            // Prevent editing contract number if:
            // 1. Contract has been renewed (renewedContractId != null) - this is the old contract
            // 2. Contract number contains "Gia hạn lần" - this is a renewed contract (new format)
            // 3. Contract number contains "-RENEW-" - this is a renewed contract (old format, for backward compatibility)
            if (contract.getRenewedContractId() != null) {
                throw new IllegalArgumentException("Không thể chỉnh sửa tên hợp đồng đã được gia hạn. Hợp đồng này đã được gia hạn thành công.");
            }
            if (contract.getContractNumber() != null && contract.getContractNumber().contains("Gia hạn lần")) {
                throw new IllegalArgumentException("Không thể chỉnh sửa tên hợp đồng sau khi gia hạn. Tên hợp đồng đã được hệ thống tự động sinh và không thể thay đổi.");
            }
            if (contract.getContractNumber() != null && contract.getContractNumber().contains("-RENEW-")) {
                throw new IllegalArgumentException("Không thể chỉnh sửa tên hợp đồng sau khi gia hạn. Tên hợp đồng đã được hệ thống tự động sinh và không thể thay đổi.");
            }
            
            contractRepository.findByContractNumber(request.getContractNumber())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Contract number already exists: " + request.getContractNumber());
                    });
            contract.setContractNumber(request.getContractNumber());
        }
        
        if (request.getContractType() != null) {
            String newContractType = request.getContractType();
            if (!"RENTAL".equals(newContractType) && !"PURCHASE".equals(newContractType)) {
                throw new IllegalArgumentException("Invalid contract type. Must be RENTAL or PURCHASE");
            }
            
            String oldContractType = contract.getContractType();
            contract.setContractType(newContractType);
            
            if (!oldContractType.equals(newContractType)) {
                if ("RENTAL".equals(newContractType)) {
                    contract.setPurchasePrice(null);
                    contract.setPaymentMethod(null);
                    contract.setPaymentTerms(null);
                    contract.setPurchaseDate(null);
                } else if ("PURCHASE".equals(newContractType)) {
                    contract.setEndDate(null);
                }
            }
        }
        
        String currentType = contract.getContractType();
        
        if (request.getStartDate() != null) {
            contract.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            if ("PURCHASE".equals(currentType)) {
                throw new IllegalArgumentException("Purchase contracts cannot have end date");
            }
            contract.setEndDate(request.getEndDate());
        }
        if (request.getMonthlyRent() != null) {
            if ("PURCHASE".equals(currentType)) {
                throw new IllegalArgumentException("Purchase contracts cannot have monthly rent");
            }
            contract.setMonthlyRent(request.getMonthlyRent());
        }
        if (request.getPurchasePrice() != null) {
            if ("RENTAL".equals(currentType)) {
                throw new IllegalArgumentException("Rental contracts cannot have purchase price");
            }
            contract.setPurchasePrice(request.getPurchasePrice());
        }
        if (request.getPaymentMethod() != null || request.getPaymentTerms() != null) {
            if ("PURCHASE".equals(currentType)) {
                throw new IllegalArgumentException("Purchase contracts are fully paid. Payment method and terms are not applicable");
            }
            
            if (request.getPaymentMethod() != null) {
                contract.setPaymentMethod(request.getPaymentMethod());
            }
            if (request.getPaymentTerms() != null) {
                contract.setPaymentTerms(request.getPaymentTerms());
            }
        }
        if (request.getPurchaseDate() != null) {
            if ("RENTAL".equals(currentType)) {
                throw new IllegalArgumentException("Rental contracts cannot have purchase date");
            }
            contract.setPurchaseDate(request.getPurchaseDate());
        }
        if (request.getNotes() != null) {
            contract.setNotes(request.getNotes());
        }
        String oldStatus = contract.getStatus();
        if (request.getStatus() != null) {
            contract.setStatus(request.getStatus());
        }

        if ("RENTAL".equals(currentType)) {
            if (contract.getEndDate() != null && contract.getStartDate().isAfter(contract.getEndDate())) {
                throw new IllegalArgumentException("Start date must be before or equal to end date");
            }
        } else if ("PURCHASE".equals(currentType)) {
            if (contract.getEndDate() != null) {
                throw new IllegalArgumentException("Purchase contracts cannot have end date");
            }
        }

        contract.setUpdatedBy(updatedBy);
        contract = contractRepository.save(contract);
        
        // If contract status changed to CANCELLED or EXPIRED, handle contract end
        // This ensures household is deactivated when contract is cancelled/expired via updateContract
        String newStatus = contract.getStatus();
        if (oldStatus != null && !oldStatus.equals(newStatus) && 
            ("CANCELLED".equals(newStatus) || "EXPIRED".equals(newStatus)) &&
            "RENTAL".equals(currentType) &&
            contract.getUnitId() != null) {
            log.info("Contract {} status changed from {} to {} via updateContract, handling contract end", 
                    contractId, oldStatus, newStatus);
            // Flush to ensure status change is committed before calling base-service
            entityManager.flush();
            handleContractEnd(contract.getUnitId());
        }
        
        log.info("Updated contract: {}", contractId);

        return toDto(contract);
    }

    public ContractDto getContractById(UUID contractId) {
        Contract contract = contractRepository.findByIdWithFiles(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        return toDto(contract, null, null);
    }
    
    public ContractDto getContractById(UUID contractId, UUID userId, String accessToken) {
        return getContractById(contractId, userId, accessToken, false);
    }
    
    public ContractDto getContractById(UUID contractId, UUID userId, String accessToken, boolean skipRenewalReminder) {
        Contract contract = contractRepository.findByIdWithFiles(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        return toDto(contract, userId, accessToken, null, skipRenewalReminder);
    }

    @Transactional(readOnly = true)
    public List<ContractDto> getContractsByUnitId(UUID unitId) {
        return getContractsByUnitId(unitId, null, null);
    }
    
    @Transactional(readOnly = true)
    public List<ContractDto> getContractsByUnitId(UUID unitId, UUID userId, String accessToken) {
        return getContractsByUnitId(unitId, userId, accessToken, false);
    }
    
    @Transactional(readOnly = true)
    public List<ContractDto> getContractsByUnitId(UUID unitId, UUID userId, String accessToken, boolean skipRenewalReminder) {
        try {
            List<Contract> contracts = contractRepository.findByUnitId(unitId);

            // Cache isOwner check: chỉ gọi 1 lần cho mỗi unitId trong cùng request
            Boolean cachedIsOwner = null;
            if (userId != null && accessToken != null) {
                // Chỉ check nếu có ACTIVE RENTAL contracts cần permissions
                boolean hasActiveRental = contracts.stream()
                        .anyMatch(c -> "RENTAL".equals(c.getContractType()) && "ACTIVE".equals(c.getStatus()));
                
                if (hasActiveRental) {
                    try {
                        cachedIsOwner = baseServiceClient.isOwnerOfUnit(userId, unitId, accessToken);
                        log.info("🔍 [ContractService] Cached isOwner for unit {}: {}", unitId, cachedIsOwner);
                    } catch (RuntimeException e) {
                        // Timeout or base-service unavailable - will use fallback in toDto
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && errorMsg.contains("timeout")) {
                            log.warn("⚠️ [ContractService] Base-service timeout when checking isOwner for unit {}. Will use fallback.", unitId);
                            cachedIsOwner = null; // null = will trigger fallback
                        } else {
                            throw e;
                        }
                    }
                }
            }
            
            final Boolean finalCachedIsOwner = cachedIsOwner;
            return contracts.stream()
                    .map(contract -> {
                        try {
                            return toDto(contract, userId, accessToken, finalCachedIsOwner, skipRenewalReminder);
                        } catch (Exception e) {
                            log.error("[ContractService] Lỗi khi convert contract {} sang DTO: {}", 
                                    contract.getId(), e.getMessage(), e);
                            return ContractDto.builder()
                                    .id(contract.getId())
                                    .unitId(contract.getUnitId())
                                    .contractNumber(contract.getContractNumber())
                                    .contractType(contract.getContractType())
                                    .startDate(contract.getStartDate())
                                    .endDate(contract.getEndDate())
                                    .monthlyRent(contract.getMonthlyRent())
                                    .purchasePrice(contract.getPurchasePrice())
                                    .paymentMethod(contract.getPaymentMethod())
                                    .paymentTerms(contract.getPaymentTerms())
                                    .purchaseDate(contract.getPurchaseDate())
                                    .notes(contract.getNotes())
                                    .status(contract.getStatus())
                                    .createdBy(contract.getCreatedBy())
                                    .createdAt(contract.getCreatedAt())
                                    .updatedAt(contract.getUpdatedAt())
                                    .updatedBy(contract.getUpdatedBy())
                                    .files(List.of())
                                    .build();
                        }
                    })
                    .sorted((c1, c2) -> {
                        // Sort by priority: ACTIVE → INACTIVE → CANCELLED → EXPIRED
                        int priority1 = getStatusPriority(c1.getStatus());
                        int priority2 = getStatusPriority(c2.getStatus());
                        if (priority1 != priority2) {
                            return Integer.compare(priority1, priority2);
                        }
                        // If same priority, sort by endDate (most recent first, nulls last)
                        if (c1.getEndDate() != null && c2.getEndDate() != null) {
                            return c2.getEndDate().compareTo(c1.getEndDate());
                        }
                        if (c1.getEndDate() != null) return -1;
                        if (c2.getEndDate() != null) return 1;
                        // If both null, sort by createdAt (most recent first)
                        if (c1.getCreatedAt() != null && c2.getCreatedAt() != null) {
                            return c2.getCreatedAt().compareTo(c1.getCreatedAt());
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[ContractService] Lỗi khi lấy contracts cho unit {}: {}", unitId, e.getMessage(), e);
            throw new RuntimeException("Không thể lấy danh sách hợp đồng: " + e.getMessage(), e);
        }
    }

    /**
     * Get priority for contract status sorting
     * Lower number = higher priority
     * ACTIVE = 1 (highest priority)
     * INACTIVE = 2
     * CANCELLED = 3
     * EXPIRED = 4 (lowest priority)
     * Other statuses = 5
     */
    private int getStatusPriority(String status) {
        if (status == null) return 99;
        String upperStatus = status.toUpperCase();
        switch (upperStatus) {
            case "ACTIVE":
                return 1;
            case "INACTIVE":
                return 2;
            case "CANCELLED":
                return 3;
            case "EXPIRED":
                return 4;
            default:
                return 5;
        }
    }

    public List<ContractDto> getActiveContracts() {
        List<Contract> contracts = contractRepository.findActiveContracts(LocalDate.now());
        return contracts.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ContractDto> getActiveContractsByUnit(UUID unitId) {
        return getActiveContractsByUnit(unitId, null, null);
    }
    
    @Transactional(readOnly = true)
    public List<ContractDto> getActiveContractsByUnit(UUID unitId, UUID userId, String accessToken) {
        List<Contract> contracts = contractRepository.findActiveContractsByUnit(unitId, LocalDate.now());
        
        // Cache isOwner check: chỉ gọi 1 lần cho mỗi unitId trong cùng request
        Boolean cachedIsOwner = null;
        if (userId != null && accessToken != null && !contracts.isEmpty()) {
            try {
                cachedIsOwner = baseServiceClient.isOwnerOfUnit(userId, unitId, accessToken);
                log.debug("🔍 [ContractService] Cached isOwner for unit {}: {}", unitId, cachedIsOwner);
            } catch (RuntimeException e) {
                // Timeout or base-service unavailable - will use fallback in toDto
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("timeout")) {
                    log.warn("⚠️ [ContractService] Base-service timeout when checking isOwner for unit {}. Will use fallback.", unitId);
                    cachedIsOwner = null; // null = will trigger fallback
                } else {
                    throw e;
                }
            }
        }
        
        final Boolean finalCachedIsOwner = cachedIsOwner;
        return contracts.stream()
                .map(c -> toDto(c, userId, accessToken, finalCachedIsOwner))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractDto> getAllContracts() {
        List<Contract> contracts = contractRepository.findAll();
        return contracts.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toDtoSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractDto> getContractsByType(String contractType) {
        if (contractType == null || contractType.isEmpty()) {
            throw new IllegalArgumentException("Contract type is required");
        }
        String upperContractType = contractType.toUpperCase();
        List<Contract> allContracts = contractRepository.findAll();
        List<Contract> contracts = allContracts.stream()
                .filter(c -> upperContractType.equals(c.getContractType()))
                .collect(Collectors.toList());
        return contracts.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toDtoSummary)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteContract(UUID contractId) {
        Contract contract = contractRepository.findByIdWithFiles(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        contract.getFiles().forEach(file -> {
            if (!file.getIsDeleted()) {
                file.setIsDeleted(true);
                file.setDeletedAt(java.time.OffsetDateTime.now());
                contractFileRepository.save(file);
            }
        });

        contractRepository.delete(contract);
        log.info("Deleted contract: {}", contractId);
    }

    @Transactional
    public ContractFileDto uploadContractFile(UUID contractId, MultipartFile file, UUID uploadedBy, Boolean isPrimary) {
        Contract contract = contractRepository.findByIdWithFiles(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        FileUploadResponse uploadResponse = fileStorageService.uploadContractFile(file, contractId, uploadedBy);

        if (Boolean.TRUE.equals(isPrimary)) {
            contractFileRepository.findPrimaryFileByContractId(contractId)
                    .ifPresent(primaryFile -> {
                        primaryFile.setIsPrimary(false);
                        contractFileRepository.save(primaryFile);
                    });
        } else {
            List<ContractFile> existingFiles = contractFileRepository.findByContractId(contractId);
            if (existingFiles.isEmpty()) {
                isPrimary = true;
            }
        }

        Integer displayOrder = contractFileRepository.findByContractId(contractId).size();
        ContractFile contractFile = ContractFile.builder()
                .contract(contract)
                .fileName(uploadResponse.getFileName())
                .originalFileName(uploadResponse.getOriginalFileName())
                .filePath("contracts/" + contractId + "/" + uploadResponse.getFileName())
                .fileUrl(uploadResponse.getFileUrl())
                .contentType(uploadResponse.getContentType())
                .fileSize(uploadResponse.getFileSize())
                .isPrimary(Boolean.TRUE.equals(isPrimary))
                .displayOrder(displayOrder)
                .uploadedBy(uploadedBy)
                .build();

        contractFile = contractFileRepository.save(contractFile);
        log.info("Uploaded contract file: {} for contract: {}", contractFile.getId(), contractId);

        return toFileDto(contractFile);
    }

    public List<ContractFileDto> getContractFiles(UUID contractId) {
        List<ContractFile> files = contractFileRepository.findByContractId(contractId);
        return files.stream()
                .map(this::toFileDto)
                .collect(Collectors.toList());
    }

    public Resource viewContractFile(UUID contractId, UUID fileId) {
        ContractFile file = contractFileRepository.findByIdNotDeleted(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        if (!file.getContract().getId().equals(contractId)) {
            throw new IllegalArgumentException("File does not belong to contract: " + contractId);
        }

        return fileStorageService.loadContractFileAsResource(contractId, file.getFileName());
    }

    public Resource downloadContractFile(UUID contractId, UUID fileId) {
        return viewContractFile(contractId, fileId);
    }

    @Transactional
    public void deleteContractFile(UUID contractId, UUID fileId) {
        ContractFile file = contractFileRepository.findByIdNotDeleted(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        if (!file.getContract().getId().equals(contractId)) {
            throw new IllegalArgumentException("File does not belong to contract: " + contractId);
        }

        file.setIsDeleted(true);
        file.setDeletedAt(java.time.OffsetDateTime.now());
        contractFileRepository.save(file);

        try {
            fileStorageService.deleteContractFile(contractId, file.getFileName());
        } catch (Exception e) {
            log.error("Failed to delete physical file: {}", file.getFileName(), e);
        }

        log.info("Deleted contract file: {} for contract: {}", fileId, contractId);
    }

    @Transactional
    public ContractFileDto setPrimaryFile(UUID contractId, UUID fileId) {
        ContractFile file = contractFileRepository.findByIdNotDeleted(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        if (!file.getContract().getId().equals(contractId)) {
            throw new IllegalArgumentException("File does not belong to contract: " + contractId);
        }

        contractFileRepository.findPrimaryFileByContractId(contractId)
                .ifPresent(primaryFile -> {
                    primaryFile.setIsPrimary(false);
                    contractFileRepository.save(primaryFile);
                });
        file.setIsPrimary(true);
        file = contractFileRepository.save(file);
        log.info("Set primary file: {} for contract: {}", fileId, contractId);

        return toFileDto(file);
    }

    private ContractDto toDto(Contract contract) {
        return toDto(contract, null, null);
    }
    
    private ContractDto toDto(Contract contract, UUID userId, String accessToken) {
        return toDto(contract, userId, accessToken, null, false);
    }

    private ContractDto toDto(Contract contract, UUID userId, String accessToken, Boolean cachedIsOwner) {
        return toDto(contract, userId, accessToken, cachedIsOwner, false);
    }

    private ContractDto toDto(Contract contract, UUID userId, String accessToken, Boolean cachedIsOwner, boolean skipRenewalReminder) {
        List<ContractFileDto> files = List.of();
        try {
            if (contract.getFiles() != null) {
                files = contract.getFiles().stream()
                        .filter(f -> f != null && !f.getIsDeleted())
                        .map(file -> {
                            try {
                                return toFileDto(file);
                            } catch (Exception e) {
                                log.warn("[ContractService] Lỗi khi convert file {} sang DTO: {}", 
                                        file != null ? file.getId() : "null", e.getMessage());
                                return null;
                            }
                        })
                        .filter(f -> f != null)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("[ContractService] Lỗi khi load files cho contract {}: {}", 
                    contract.getId(), e.getMessage());
        }

        int reminderCount = calculateReminderCount(contract);
        boolean isFinalReminder = reminderCount == 3;
        // ✅ Skip renewal reminder nếu user đang ở màn hình cancel/renew contract
        boolean needsRenewal = skipRenewalReminder ? false : calculateNeedsRenewal(contract);

        // Check permission: isOwner, canRenew, canCancel, canExtend
        boolean isOwner = false;
        boolean canRenew = false;
        boolean canCancel = false;
        boolean canExtend = false;
        String permissionMessage = null;
        
        // ✅ SKIP OWNER CHECK: Don't check permissions, allow all actions based on contract state
        boolean needsPermissionCheck = false; // Set to false to skip OWNER check
        
        log.info("🔍 [ContractService] ========== CHECKING PERMISSIONS ==========");
        log.info("🔍 [ContractService] Contract: {} ({})", contract.getContractNumber(), contract.getId());
        log.info("🔍 [ContractService] userId: {}, unitId: {}", userId, contract.getUnitId());
        log.info("🔍 [ContractService] contractType: {}, status: {}, renewalStatus: {}", 
                contract.getContractType(), contract.getStatus(), contract.getRenewalStatus());
        log.info("🔍 [ContractService] needsPermissionCheck: {}, cachedIsOwner: {}", needsPermissionCheck, cachedIsOwner);
        
        if (needsPermissionCheck && userId != null && contract.getUnitId() != null && accessToken != null) {
            try {
                // Sử dụng cached result nếu có
                if (cachedIsOwner != null) {
                    isOwner = cachedIsOwner;
                    log.info("✅ [ContractService] Using cached isOwner={} for contract {}", isOwner, contract.getId());
                } else {
                    // Gọi API nếu chưa có cache
                    log.info("🔍 [ContractService] Calling baseServiceClient.isOwnerOfUnit(userId={}, unitId={})", 
                            userId, contract.getUnitId());
                isOwner = baseServiceClient.isOwnerOfUnit(userId, contract.getUnitId(), accessToken);
                    log.info("✅ [ContractService] isOwnerOfUnit result: isOwner={}", isOwner);
                }
                
                if (isOwner) {
                    log.info("✅ [ContractService] User is OWNER. Setting permissions...");
                    // OWNER/TENANT can renew, cancel, extend if contract is in valid state
                        // Can renew if contract is renewable (not already renewed, in REMINDED status)
                        canRenew = contract.getRenewedContractId() == null 
                                && ("REMINDED".equals(contract.getRenewalStatus()) || "PENDING".equals(contract.getRenewalStatus()));
                        
                        // Can cancel if contract is active
                        canCancel = true;
                        
                        // Can extend if contract has endDate
                        canExtend = contract.getEndDate() != null;
                    
                    log.info("✅ [ContractService] Permissions set: canRenew={}, canCancel={}, canExtend={}", 
                            canRenew, canCancel, canExtend);
                } else {
                    // Not OWNER/TENANT - household member
                    permissionMessage = "Bạn không phải chủ căn hộ nên không thể gia hạn hay hủy hợp đồng";
                    // Silent - no need to log when user is not owner (expected case)
                }
            } catch (RuntimeException e) {
                // Timeout or base-service unavailable - use fallback for ACTIVE RENTAL contracts
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("timeout")) {
                    log.warn("⚠️ [ContractService] Base-service timeout for contract {}. Using fallback: assuming owner for ACTIVE RENTAL contract.", 
                            contract.getId());
                    // Fallback: assume owner for ACTIVE RENTAL contracts when base-service is unavailable
                    isOwner = true;
                    canRenew = contract.getRenewedContractId() == null 
                            && ("REMINDED".equals(contract.getRenewalStatus()) || "PENDING".equals(contract.getRenewalStatus()));
                    canCancel = true;
                    canExtend = contract.getEndDate() != null;
                    log.info("✅ [ContractService] Fallback permissions: canRenew={}, canCancel={}, canExtend={}", 
                            canRenew, canCancel, canExtend);
                } else {
                    log.warn("[ContractService] Error checking permission for contract {}: {}", 
                            contract.getId(), e.getMessage());
                    permissionMessage = "Bạn không phải chủ căn hộ nên không thể gia hạn hay hủy hợp đồng";
                }
            } catch (Exception e) {
                log.warn("[ContractService] Error checking permission for contract {}: {}", 
                        contract.getId(), e.getMessage());
                permissionMessage = "Bạn không phải chủ căn hộ nên không thể gia hạn hay hủy hợp đồng";
            }
        } else {
            // ✅ SKIP OWNER CHECK: Set permissions based on contract state only
            if ("RENTAL".equals(contract.getContractType()) && "ACTIVE".equals(contract.getStatus())) {
                isOwner = true;
                canRenew = contract.getRenewedContractId() == null 
                        && ("REMINDED".equals(contract.getRenewalStatus()) || "PENDING".equals(contract.getRenewalStatus()));
                canCancel = true;
                canExtend = contract.getEndDate() != null;
                log.debug("✅ [ContractService] Permissions (no OWNER check): canRenew={}, canCancel={}, canExtend={}", 
                        canRenew, canCancel, canExtend);
            }
        }

        LocalDate inspectionDate = null;
        try {
            Optional<LocalDate> inspectionDateOpt = baseServiceClient.getInspectionDateByContractId(contract.getId());
            inspectionDate = inspectionDateOpt.orElse(null);
        } catch (Exception e) {
            log.debug("Could not fetch inspection date for contract {}: {}", contract.getId(), e.getMessage());
            // Don't fail if inspection date cannot be fetched
        }

        return ContractDto.builder()
                .id(contract.getId())
                .unitId(contract.getUnitId())
                .contractNumber(contract.getContractNumber())
                .contractType(contract.getContractType())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .checkoutDate(contract.getCheckoutDate())
                .monthlyRent(contract.getMonthlyRent())
                .totalRent(calculateTotalRent(contract))
                .purchasePrice(contract.getPurchasePrice())
                .paymentMethod(contract.getPaymentMethod())
                .paymentTerms(contract.getPaymentTerms())
                .purchaseDate(contract.getPurchaseDate())
                .notes(contract.getNotes())
                .status(contract.getStatus())
                .createdBy(contract.getCreatedBy())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .updatedBy(contract.getUpdatedBy())
                .renewalReminderSentAt(contract.getRenewalReminderSentAt())
                .renewalDeclinedAt(contract.getRenewalDeclinedAt())
                .renewalStatus(contract.getRenewalStatus())
                .reminderCount(reminderCount > 0 ? reminderCount : null)
                .isFinalReminder(isFinalReminder)
                .needsRenewal(needsRenewal)
                .renewedContractId(contract.getRenewedContractId())
                .files(files)
                .isOwner(isOwner)
                .canRenew(canRenew)
                .canCancel(canCancel)
                .canExtend(canExtend)
                .permissionMessage(permissionMessage)
                .inspectionDate(inspectionDate)
                .build();
    }

    private ContractDto toDtoSummary(Contract contract) {
        
        LocalDate inspectionDate = null;
        try {
            Optional<LocalDate> inspectionDateOpt = baseServiceClient.getInspectionDateByContractId(contract.getId());
            inspectionDate = inspectionDateOpt.orElse(null);
        } catch (Exception e) {
            log.debug("Could not fetch inspection date for contract {} in summary: {}", contract.getId(), e.getMessage());
       
        }

        return ContractDto.builder()
                .id(contract.getId())
                .unitId(contract.getUnitId())
                .contractNumber(contract.getContractNumber())
                .contractType(contract.getContractType())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .monthlyRent(contract.getMonthlyRent())
                .totalRent(calculateTotalRent(contract))
                .purchasePrice(contract.getPurchasePrice())
                .paymentMethod(contract.getPaymentMethod())
                .paymentTerms(contract.getPaymentTerms())
                .purchaseDate(contract.getPurchaseDate())
                .notes(contract.getNotes())
                .status(contract.getStatus())
                .createdBy(contract.getCreatedBy())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .updatedBy(contract.getUpdatedBy())
                .renewalReminderSentAt(contract.getRenewalReminderSentAt())
                .renewalDeclinedAt(contract.getRenewalDeclinedAt())
                .renewalStatus(contract.getRenewalStatus())
                .renewedContractId(contract.getRenewedContractId())
                .files(null)
                .inspectionDate(inspectionDate)
                .build();
    }

    private ContractFileDto toFileDto(ContractFile file) {
        return ContractFileDto.builder()
                .id(file.getId())
                .contractId(file.getContract().getId())
                .fileName(file.getFileName())
                .originalFileName(file.getOriginalFileName())
                .fileUrl(file.getFileUrl())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .isPrimary(file.getIsPrimary())
                .displayOrder(file.getDisplayOrder())
                .uploadedBy(file.getUploadedBy())
                .uploadedAt(file.getUploadedAt())
                .build();
    }

    @Transactional
    public ContractDto checkoutContract(UUID contractId, LocalDate checkoutDate, UUID updatedBy) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        if (contract.getEndDate() != null && checkoutDate.isAfter(contract.getEndDate())) {
            throw new IllegalArgumentException("Checkout date must be less than or equal to end date");
        }

        if (checkoutDate.isBefore(contract.getStartDate())) {
            throw new IllegalArgumentException("Checkout date must be after or equal to start date");
        }

        contract.setCheckoutDate(checkoutDate);
        contract.setStatus("CANCELLED");
        contract.setUpdatedBy(updatedBy);
        
        contract = contractRepository.save(contract);
        log.info("Checked out contract: {} with checkout date: {}", contractId, checkoutDate);

        // Delete household or clear primaryResidentId when contract is cancelled
        handleContractEnd(contract.getUnitId());

        return toDto(contract);
    }

    @Transactional
    public int activateInactiveContracts() {
        LocalDate today = LocalDate.now();
        List<Contract> inactiveContracts = contractRepository.findInactiveContractsByStartDate(today);
        
        int activatedCount = 0;
        for (Contract contract : inactiveContracts) {
            contract.setStatus("ACTIVE");
            contractRepository.save(contract);
            activatedCount++;
            log.info("Activated contract: {} (contract number: {})", contract.getId(), contract.getContractNumber());
        }
        
        if (activatedCount > 0) {
            log.info("Activated {} inactive contract(s) with start date = {}", activatedCount, today);
        }
        
        return activatedCount;
    }

    @Transactional
    public int markExpiredContracts() {
        LocalDate today = LocalDate.now();
        List<Contract> expiredContracts = contractRepository.findContractsNeedingExpired(today);
        
        int expiredCount = 0;
        for (Contract contract : expiredContracts) {
            // When contract expires, set status to EXPIRED
            // renewalStatus remains as is (PENDING, REMINDED, or DECLINED)
            contract.setStatus("EXPIRED");
            contractRepository.save(contract);
            expiredCount++;
            log.info("Marked contract as expired: {} (contract number: {}, endDate: {}, renewalStatus: {})", 
                    contract.getId(), contract.getContractNumber(), contract.getEndDate(), contract.getRenewalStatus());
            
            // Delete household or clear primaryResidentId when contract expires
            handleContractEnd(contract.getUnitId());
        }
        
        if (expiredCount > 0) {
            log.info("Marked {} contract(s) as expired with endDate < {}", expiredCount, today);
        }
        
        return expiredCount;
    }

    public BigDecimal calculateTotalRent(Contract contract) {
        if (!"RENTAL".equals(contract.getContractType())) {
            return null;
        }
        
        if (contract.getMonthlyRent() == null || contract.getStartDate() == null) {
            return null;
        }
        
        if (contract.getEndDate() == null) {
            return null;
        }
        
        LocalDate startDate = contract.getStartDate();
        LocalDate endDate = contract.getEndDate();
        BigDecimal monthlyRent = contract.getMonthlyRent();
        
        if (startDate.isAfter(endDate)) {
            return BigDecimal.ZERO;
        }
        
        // Since endDate always has the same day as startDate, we can simply calculate months
        // Calculate the difference in months
        int months = (endDate.getYear() - startDate.getYear()) * 12 + (endDate.getMonthValue() - startDate.getMonthValue());
        
        // Total rent = number of months * monthly rent
        BigDecimal totalRent = monthlyRent.multiply(BigDecimal.valueOf(months));
        
        return totalRent.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<Contract> findContractsNeedingRenewalReminder() {
        LocalDate today = LocalDate.now();
        // Find contracts with endDate in next 0-32 days (for all 3 reminder levels)
        // Lần 1: 30 ngày trước endDate (28-32 buffer)
        // Lần 2: 22 ngày trước endDate (20-24 buffer) - ngày thứ 8 trong tháng
        // Lần 3: 10 ngày trước endDate (0-30 buffer) - ngày 20 trong tháng
        // Mở rộng range để bao gồm contracts sắp hết hạn (0-7 ngày) cho reminder 3
        LocalDate maxDate = today.plusDays(32);
        
        return contractRepository.findContractsNeedingRenewalReminderByDateRange(today, maxDate);
    }

    @Transactional(readOnly = true)
    public List<Contract> findContractsWithRenewalDeclined(OffsetDateTime deadlineDate) {
        return contractRepository.findContractsWithRenewalDeclined(deadlineDate);
    }

    @Transactional
    public void sendRenewalReminder(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        
        if (!"RENTAL".equals(contract.getContractType())) {
            throw new IllegalArgumentException("Only RENTAL contracts can have renewal reminders");
        }
        
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE contracts can have renewal reminders");
        }
        
        if (contract.getEndDate() == null) {
            throw new IllegalArgumentException("Contract must have end date for renewal reminder");
        }
        
        String currentRenewalStatus = contract.getRenewalStatus();
        if (!"PENDING".equals(currentRenewalStatus) && !"REMINDED".equals(currentRenewalStatus)) {
            throw new IllegalArgumentException("Contract must be in PENDING or REMINDED status to send renewal reminder. Current status: " + currentRenewalStatus);
        }
        
        // Chỉ set renewalReminderSentAt lần đầu tiên (lần 1)
        // Giữ nguyên thời điểm lần 1 để có thể tính toán lần 2 và lần 3
        if (contract.getRenewalReminderSentAt() == null) {
            contract.setRenewalReminderSentAt(OffsetDateTime.now());
        }
        
        contract.setRenewalStatus("REMINDED");
        contractRepository.save(contract);
        
        if (contract.getRenewalReminderSentAt() != null) {
            long daysSinceFirstReminder = ChronoUnit.DAYS.between(
                contract.getRenewalReminderSentAt().toLocalDate(),
                LocalDate.now()
            );
            log.info("Sent renewal reminder for contract: {} (ends on: {}, {} days since first reminder)", 
                    contractId, contract.getEndDate(), daysSinceFirstReminder);
        } else {
            log.info("Sent renewal reminder for contract: {} (ends on: {})", contractId, contract.getEndDate());
        }
    }

    /**
     * Set third reminder sent timestamp when third reminder is sent
     */
    @Transactional
    public void setThirdReminderSentAt(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        if (!"RENTAL".equals(contract.getContractType())) {
            throw new IllegalArgumentException("Only RENTAL contracts can have third reminder timestamp");
        }

        contract.setThirdReminderSentAt(OffsetDateTime.now());
        contractRepository.save(contract);
        log.info("Set third reminder sent timestamp for contract: {}", contractId);
    }

    /**
     * Auto-cancel contract after 24 hours from third reminder if user hasn't taken action
     * This method is called by scheduled task
     */
    @Transactional
    public int autoCancelContractsAfterThirdReminder() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime twentyFourHoursAgo = now.minusHours(24);
        
        // Find contracts that:
        // 1. Are RENTAL type
        // 2. Are ACTIVE status
        // 3. Have thirdReminderSentAt set (third reminder was sent)
        // 4. thirdReminderSentAt was more than 24 hours ago
        // 5. Still in REMINDED status (user hasn't taken action)
        // 6. Not already renewed (renewedContractId is null)
        List<Contract> contractsToCancel = contractRepository.findAll().stream()
                .filter(c -> "RENTAL".equals(c.getContractType()))
                .filter(c -> "ACTIVE".equals(c.getStatus()))
                .filter(c -> "REMINDED".equals(c.getRenewalStatus()))
                .filter(c -> c.getThirdReminderSentAt() != null)
                .filter(c -> c.getThirdReminderSentAt().isBefore(twentyFourHoursAgo))
                .filter(c -> c.getRenewedContractId() == null)
                .filter(c -> c.getEndDate() != null)
                .toList();

        int cancelledCount = 0;
        for (Contract contract : contractsToCancel) {
            try {
                log.info("🔄 Auto-cancelling contract {} after 24 hours from third reminder (thirdReminderSentAt: {})", 
                        contract.getContractNumber(), contract.getThirdReminderSentAt());
                
                // Auto-cancel contract without permission check (system action)
                autoCancelContractWithoutPermissionCheck(contract.getId(), contract.getEndDate());
                cancelledCount++;
                
                log.info("✅ Auto-cancelled contract {} (inspectionDate set to endDate: {})", 
                        contract.getContractNumber(), contract.getEndDate());
            } catch (Exception e) {
                log.error("❌ Error auto-cancelling contract {}: {}", contract.getId(), e.getMessage(), e);
            }
        }

        return cancelledCount;
    }

    /**
     * Auto-cancel contract without permission check (for system scheduled tasks)
     * Sets inspectionDate to the contract's endDate
     */
    @Transactional
    private void autoCancelContractWithoutPermissionCheck(UUID contractId, LocalDate inspectionDate) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        
        if (!"RENTAL".equals(contract.getContractType())) {
            throw new IllegalArgumentException("Only RENTAL contracts can be auto-cancelled");
        }
        
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE contracts can be auto-cancelled");
        }
        
        contract.setStatus("CANCELLED");
        contract.setRenewalStatus("DECLINED");
        contract.setRenewalDeclinedAt(OffsetDateTime.now());
        contract.setUpdatedBy(null); // System action, no user
        contract = contractRepository.save(contract);
        
        // Flush to ensure the status change is committed to database before calling base-service
        entityManager.flush();
        
        log.info("Auto-cancelled contract: {} (renewalStatus set to DECLINED)", contractId);
        
        // Create asset inspection with endDate as inspectionDate
        LocalDate inspectionDateToUse = inspectionDate != null ? inspectionDate : contract.getEndDate();
        if (inspectionDateToUse == null) {
            inspectionDateToUse = LocalDate.now();
        }
        baseServiceClient.createAssetInspection(contractId, contract.getUnitId(), inspectionDateToUse, null);
        
        // Delete household or clear primaryResidentId when contract is cancelled
        handleContractEnd(contract.getUnitId());
    }

    @Transactional(readOnly = true)
    public List<Contract> findContractsNeedingSecondReminder() {
        OffsetDateTime sevenDaysAgo = OffsetDateTime.now().minusDays(7);
        return contractRepository.findContractsNeedingSecondReminder(sevenDaysAgo);
    }

    @Transactional(readOnly = true)
    public List<Contract> findContractsNeedingThirdReminder() {
        OffsetDateTime twentyDaysAgo = OffsetDateTime.now().minusDays(20);
        return contractRepository.findContractsNeedingThirdReminder(twentyDaysAgo);
    }

    @Transactional
    public void markRenewalDeclined(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        
        if (!"RENTAL".equals(contract.getContractType())) {
            throw new IllegalArgumentException("Only RENTAL contracts can have renewal declined");
        }
        
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE contracts can have renewal declined");
        }
        
        String currentRenewalStatus = contract.getRenewalStatus();
        if (!"PENDING".equals(currentRenewalStatus) && !"REMINDED".equals(currentRenewalStatus)) {
            throw new IllegalArgumentException("Contract must be in PENDING or REMINDED status to mark as declined. Current status: " + currentRenewalStatus);
        }
        
        contract.setRenewalDeclinedAt(OffsetDateTime.now());
        contract.setRenewalStatus("DECLINED");
        contractRepository.save(contract);
        
        log.info("Marked contract {} as renewal declined (was: {})", contractId, currentRenewalStatus);
    }
    
    /**
     * Dismiss current reminder - user won't see this reminder again until next reminder count
     * Only works for reminder 1 and 2. Final reminder (3) cannot be dismissed.
     */
    @Transactional
    public void dismissReminder(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        
        if (!"RENTAL".equals(contract.getContractType())) {
            throw new IllegalArgumentException("Only RENTAL contracts can have reminder dismissed");
        }
        
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE contracts can have reminder dismissed");
        }
        
        if (!"REMINDED".equals(contract.getRenewalStatus())) {
            throw new IllegalArgumentException("Contract must be in REMINDED status to dismiss reminder");
        }
        
        // Calculate current reminder count based on reminderCount from toDto logic
        int currentReminderCount = calculateReminderCount(contract);
        
        // Cannot dismiss final reminder (reminder 3)
        if (currentReminderCount >= 3) {
            throw new IllegalArgumentException("Cannot dismiss final reminder. User must take action (renew or cancel).");
        }
        
        // Mark this reminder as dismissed
        contract.setLastDismissedReminderCount(currentReminderCount);
        contractRepository.save(contract);
        
        log.info("✅ Dismissed reminder {} for contract {}", currentReminderCount, contract.getContractNumber());
    }
    
    @Deprecated
    @Transactional
    public ContractDto extendContract(UUID contractId, LocalDate newEndDate, UUID updatedBy, UUID userId, String accessToken) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        
        if (!"RENTAL".equals(contract.getContractType())) {
            throw new IllegalArgumentException("Only RENTAL contracts can be extended");
        }
        
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE contracts can be extended");
        }
        
        if (contract.getEndDate() == null) {
            throw new IllegalArgumentException("Contract must have end date to extend");
        }
        
        if (newEndDate.isBefore(contract.getEndDate()) || newEndDate.isEqual(contract.getEndDate())) {
            throw new IllegalArgumentException("New end date must be after current end date");
        }

        // Kiểm tra quyền OWNER/TENANT: chỉ OWNER hoặc TENANT mới được gia hạn hợp đồng
        if (userId != null && contract.getUnitId() != null) {
            boolean isOwner = baseServiceClient.isOwnerOfUnit(userId, contract.getUnitId(), accessToken);
            if (!isOwner) {
                throw new IllegalStateException(
                    "Chỉ chủ căn hộ (OWNER hoặc người thuê TENANT) mới được gia hạn hợp đồng. " +
                    "Thành viên hộ gia đình không được phép gia hạn."
                );
            }
        }
        
        contract.setEndDate(newEndDate);
        contract.setRenewalStatus("PENDING");
        contract.setRenewalReminderSentAt(null);
        contract.setRenewalDeclinedAt(null);
        contract.setUpdatedBy(updatedBy);
        
        contract = contractRepository.save(contract);
        log.info("Extended contract {} to new end date: {}. Renewal status reset to PENDING for new cycle.", 
                contractId, newEndDate);
        
        return toDto(contract, userId, accessToken);
    }
    
    // Overload method for backward compatibility
    public ContractDto extendContract(UUID contractId, LocalDate newEndDate, UUID updatedBy) {
        return extendContract(contractId, newEndDate, updatedBy, null, null);
    }

    /**
     * Get contracts that need to show popup to resident
     * These are contracts with renewalStatus = REMINDED
     * 
     * IMPORTANT: Reminder chỉ hiển thị khi:
     * - status = "ACTIVE" (chưa gia hạn hoặc hủy)
     * - renewalStatus = "REMINDED" (đang trong giai đoạn nhắc gia hạn)
     * - renewalReminderSentAt != null (đã gửi reminder)
     * 
     * Reminder sẽ tự động tắt khi:
     * - status thay đổi sang "RENEWED" (đã gia hạn) hoặc "CANCELLED"/"TERMINATED" (đã hủy)
     * - renewalStatus thay đổi khác "REMINDED"
     * 
     * Final reminder (isFinalReminder = true) sẽ tiếp tục hiển thị cho đến khi status thay đổi,
     * ngay cả khi user đã từng vào screen gia hạn/hủy nhưng chưa hoàn tất.
     */
    @Transactional(readOnly = true)
    public List<ContractDto> getContractsNeedingPopup(UUID unitId) {
        return getContractsNeedingPopup(unitId, null, null, false);
    }

    public List<ContractDto> getContractsNeedingPopup(UUID unitId, UUID userId, String accessToken) {
        return getContractsNeedingPopup(unitId, userId, accessToken, false);
    }

    public List<ContractDto> getContractsNeedingPopup(UUID unitId, UUID userId, String accessToken, boolean skipRenewalReminder) {
        // ✅ Nếu skipRenewalReminder = true (user đang ở màn hình cancel/renew), trả về empty list
        if (skipRenewalReminder) {
            log.debug("🚫 [ContractService] Skipping renewal reminder popup (user is in cancel/renew screen)");
            return List.of();
        }
        
        // Chỉ lấy contracts với status = "ACTIVE" (chưa gia hạn/hủy)
        // Filter này đảm bảo contracts đã RENEWED hoặc CANCELLED sẽ không được trả về
        List<Contract> contracts = contractRepository.findByUnitIdAndStatus(unitId, "ACTIVE");
        return contracts.stream()
                .filter(c -> "RENTAL".equals(c.getContractType())) // Chỉ RENTAL contracts cần gia hạn
                .filter(c -> "REMINDED".equals(c.getRenewalStatus())) // Đang trong giai đoạn nhắc gia hạn
                .filter(c -> c.getRenewalReminderSentAt() != null) // Đã gửi reminder
                .filter(c -> {
                    // ✅ Kiểm tra nếu contract đã được gia hạn thành công
                    // Nếu renewedContractId != null, nghĩa là contract đã được gia hạn thành công
                    if (c.getRenewedContractId() != null) {
                        log.debug("🚫 Skipping reminder for contract {}: already renewed (renewedContractId={})", 
                                c.getContractNumber(), c.getRenewedContractId());
                        return false;
                    }
                    return true;
                })
                .filter(c -> {
                    // ✅ Kiểm tra nếu contract đã hủy gia hạn thành công
                    // Nếu renewalStatus = "DECLINED", nghĩa là user đã hủy gia hạn hợp đồng
                    if ("DECLINED".equals(c.getRenewalStatus())) {
                        log.debug("🚫 Skipping reminder for contract {}: renewal declined", 
                                c.getContractNumber());
                        return false;
                    }
                    return true;
                })
                .filter(c -> {
                    // ✅ Check if user has dismissed this reminder
                    // Only show reminder if currentReminderCount > lastDismissedReminderCount
                    int currentReminderCount = calculateReminderCount(c);
                    Integer dismissed = c.getLastDismissedReminderCount();
                    boolean shouldShow = dismissed == null || dismissed == 0 || currentReminderCount > dismissed;
                    
                    if (!shouldShow) {
                        log.debug("🚫 Skipping reminder for contract {}: currentCount={}, dismissed={}", 
                                c.getContractNumber(), currentReminderCount, dismissed);
                    }
                    
                    return shouldShow;
                })
                // Reminder chỉ hiển thị khi contract vẫn ACTIVE và renewalStatus = REMINDED
                // Nếu status đã chuyển sang RENEWED hoặc CANCELLED, contract sẽ không có trong list này
                // Nếu contract đã được gia hạn (renewedContractId != null) hoặc đã hủy (renewalStatus = DECLINED), 
                // contract sẽ không được hiển thị popup reminder nữa
                .map(c -> toDto(c, userId, accessToken))
                .collect(Collectors.toList());
    }

    /**
     * Calculate reminder count based on days until end date
     * Lần 1: 30 ngày trước endDate
     * Lần 2: 22 ngày trước endDate (ngày thứ 8 trong tháng)
     * Lần 3: 10 ngày trước endDate (ngày 20 trong tháng)
     */
    /**
     * Calculate if contract needs renewal (within 1 month before expiration)
     * Returns true only when contract is in the same time window as reminder 1 (28-32 days before endDate)
     * This is when the status should show "cần gia hạn" instead of just "đang hoạt động"
     */
    private boolean calculateNeedsRenewal(Contract contract) {
        if (contract.getEndDate() == null || !"ACTIVE".equals(contract.getStatus())) {
            return false;
        }
        
        // Only RENTAL contracts can need renewal
        if (!"RENTAL".equals(contract.getContractType())) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate endDate = contract.getEndDate();
        long daysUntilEndDate = ChronoUnit.DAYS.between(today, endDate);
        
        // Needs renewal only when in the same window as reminder 1: 29-31 days before endDate
        // This is when reminder 1 is sent (same time point)
        return daysUntilEndDate >= 29 && daysUntilEndDate <= 31;
    }

    /**
     * Calculate reminder count based on:
     * - Lần 1: 30 ngày trước khi hết hạn (29-31 ngày trước endDate)
     * - Lần 2: 20 ngày trước khi hết hạn (19-21 ngày trước endDate)
     * - Lần 3: 10 ngày trước khi hết hạn (9-11 ngày trước endDate)
     */
    public int calculateReminderCount(Contract contract) {
        if (contract.getEndDate() == null || contract.getRenewalReminderSentAt() == null) {
            return 0;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate endDate = contract.getEndDate();
        long daysUntilEndDate = ChronoUnit.DAYS.between(today, endDate);
        
        log.debug("Calculating reminder count for contract {}: today={}, endDate={}, daysUntilEndDate={}", 
                contract.getContractNumber(), today, endDate, daysUntilEndDate);
        
        // Tính reminder count dựa vào số ngày trước endDate:
        // Lần 1: 30 ngày trước (29-31 ngày)
        // Lần 2: 20 ngày trước (19-21 ngày)
        // Lần 3: 10 ngày trước (9-11 ngày)
        
        // Lần 3: 10 ngày trước khi hết hạn (9-11 ngày)
        if (daysUntilEndDate >= 9 && daysUntilEndDate <= 11) {
            log.debug("Contract {}: reminderCount = 3 ({} days until endDate - FINAL REMINDER)", 
                    contract.getContractNumber(), daysUntilEndDate);
                return 3;
            }
        
        // Lần 2: 20 ngày trước khi hết hạn (19-21 ngày)
        if (daysUntilEndDate >= 19 && daysUntilEndDate <= 21) {
            log.debug("Contract {}: reminderCount = 2 ({} days until endDate)", 
                    contract.getContractNumber(), daysUntilEndDate);
                return 2;
            }
        
        // Lần 1: 30 ngày trước khi hết hạn (29-31 ngày)
        if (daysUntilEndDate >= 29 && daysUntilEndDate <= 31) {
            log.debug("Contract {}: reminderCount = 1 ({} days until endDate)", 
                    contract.getContractNumber(), daysUntilEndDate);
            return 1;
        }
        
        // Nếu không trong các khoảng trên, nhưng đã gửi reminder và còn > 0 ngày, trả về 1 (đã gửi lần 1)
        if (daysUntilEndDate > 0 && daysUntilEndDate < 32) {
            log.debug("Contract {}: reminderCount = 1 (fallback - {} days until endDate, reminder already sent)", 
                    contract.getContractNumber(), daysUntilEndDate);
            return 1;
        }
        
        log.debug("Contract {}: reminderCount = 0 (daysUntilEndDate={})", contract.getContractNumber(), daysUntilEndDate);
        return 0;
    }

    /**
     * Cancel contract (set status to CANCELLED and renewalStatus to DECLINED)
     * If scheduledDate is provided, creates an asset inspection
     */
    @Transactional
    public ContractDto cancelContract(UUID contractId, UUID updatedBy, java.time.LocalDate scheduledDate, UUID userId, String accessToken) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        
        if (!"RENTAL".equals(contract.getContractType())) {
            throw new IllegalArgumentException("Only RENTAL contracts can be cancelled");
        }
        
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE contracts can be cancelled");
        }
        
        // Kiểm tra quyền OWNER/TENANT: chỉ OWNER hoặc TENANT mới được hủy gia hạn hợp đồng
        if (userId != null && contract.getUnitId() != null) {
            boolean isOwner = baseServiceClient.isOwnerOfUnit(userId, contract.getUnitId(), accessToken);
            if (!isOwner) {
                throw new IllegalStateException(
                    "Chỉ chủ căn hộ (OWNER hoặc người thuê TENANT) mới được hủy gia hạn hợp đồng. " +
                    "Thành viên hộ gia đình không được phép hủy gia hạn."
                );
            }
        }
        
        contract.setStatus("CANCELLED");
        // Set renewalStatus to DECLINED when user cancels the contract
        contract.setRenewalStatus("DECLINED");
        contract.setRenewalDeclinedAt(OffsetDateTime.now());
        contract.setUpdatedBy(updatedBy);
        contract = contractRepository.save(contract);
        
        // Flush to ensure the status change is committed to database before calling base-service
        // This ensures base-service can see the contract as CANCELLED when it queries
        entityManager.flush();
        
        log.info("Cancelled contract: {} (renewalStatus set to DECLINED)", contractId);
        
        // Always create asset inspection when contract is cancelled
        // Use the selected date (scheduledDate) as inspectionDate instead of scheduledDate
        // If scheduledDate is null, use contract endDate as inspectionDate (not today, not last day of month)
        java.time.LocalDate inspectionDate = scheduledDate != null ? scheduledDate : contract.getEndDate();
        if (inspectionDate == null) {
            // Fallback to today only if contract has no endDate (should not happen for RENTAL contracts)
            inspectionDate = java.time.LocalDate.now();
            log.warn("Contract {} has no endDate, using today as inspectionDate", contractId);
        }
        // The selected date is now stored in inspectionDate, not scheduledDate
        // Pass null for scheduledDate since we're using inspectionDate instead
        baseServiceClient.createAssetInspection(contractId, contract.getUnitId(), inspectionDate, null);
        
        // Delete household or clear primaryResidentId when contract is cancelled
        handleContractEnd(contract.getUnitId());
        
        return toDto(contract);
    }
    
    /**
     * Cancel contract without scheduled date (backward compatibility)
     */
    @Transactional
    public ContractDto cancelContract(UUID contractId, UUID updatedBy) {
        return cancelContract(contractId, updatedBy, null, null, null);
    }

    @Transactional
    public ContractDto renewContract(UUID oldContractId, LocalDate newStartDate, LocalDate newEndDate, UUID createdBy, UUID userId, String accessToken) {
        Contract oldContract = contractRepository.findById(oldContractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + oldContractId));
        
        if (!"RENTAL".equals(oldContract.getContractType())) {
            throw new IllegalArgumentException("Only RENTAL contracts can be renewed");
        }
        
        if (!"ACTIVE".equals(oldContract.getStatus()) && !"REMINDED".equals(oldContract.getRenewalStatus())) {
            throw new IllegalArgumentException("Contract must be ACTIVE and in REMINDED status to renew");
        }
        
        // Check if contract has already been renewed
        if (oldContract.getRenewedContractId() != null) {
            throw new IllegalArgumentException("Hợp đồng này đã được gia hạn thành công. Không thể gia hạn lại.");
        }
        
        // Kiểm tra quyền OWNER/TENANT: chỉ OWNER hoặc TENANT mới được gia hạn hợp đồng
        if (userId != null && oldContract.getUnitId() != null) {
            boolean isOwner = baseServiceClient.isOwnerOfUnit(userId, oldContract.getUnitId(), accessToken);
            if (!isOwner) {
                throw new IllegalStateException(
                    "Chỉ chủ căn hộ (OWNER hoặc người thuê TENANT) mới được gia hạn hợp đồng. " +
                    "Thành viên hộ gia đình không được phép gia hạn."
                );
            }
        }
        
        // Validate dates: Ngày kết thúc phải sau ngày bắt đầu và không được trùng nhau
        if (newStartDate.isAfter(newEndDate) || newStartDate.isEqual(newEndDate)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu và không được trùng nhau");
        }
        
        // Validate: Gia hạn phải ít nhất 3 tháng
        // Tính số tháng từ đầu tháng bắt đầu đến đầu tháng kết thúc
        long monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(
            newStartDate.withDayOfMonth(1), 
            newEndDate.withDayOfMonth(1)
        );
        if (monthsBetween < 3) {
            throw new IllegalArgumentException("Gia hạn hợp đồng phải ít nhất 3 tháng. Ngày kết thúc phải cách ngày bắt đầu ít nhất 3 tháng.");
        }
        
        // Check for overlapping contracts (không được trùng thời gian)
        List<Contract> existingContracts = contractRepository.findByUnitId(oldContract.getUnitId());
        String oldContractNumber = oldContract.getContractNumber();
        
        log.debug("Checking overlap for contract renewal. Old contract: {}, Old contract number: {}", 
                oldContractId, oldContractNumber);
        
        for (Contract existing : existingContracts) {
            // Skip the old contract itself and cancelled/expired contracts
            if (existing.getId().equals(oldContractId) || 
                "CANCELLED".equals(existing.getStatus()) || 
                "EXPIRED".equals(existing.getStatus())) {
                log.debug("Skipping contract {} - same ID or cancelled/expired", existing.getId());
                continue;
            }
            
            String existingContractNumber = existing.getContractNumber();
            
            // Skip renewal contracts (RENEW) of the same original contract
            // These are contracts that were created from renewing this same contract
            // Format: {oldContractNumber}-RENEW-{timestamp}
            if (existingContractNumber != null && 
                existingContractNumber.startsWith(oldContractNumber + "-RENEW-")) {
                log.debug("Skipping RENEW contract {} - same original contract", existingContractNumber);
                continue;
            }
            
            // Also skip if this existing contract is the one that the old contract was renewed into
            // (i.e., oldContract.getRenewedContractId() == existing.getId())
            if (oldContract.getRenewedContractId() != null && 
                oldContract.getRenewedContractId().equals(existing.getId())) {
                log.debug("Skipping contract {} - this is the renewed contract", existing.getId());
                continue;
            }
            
            // Also skip if existing contract is a RENEW contract that was created from the same original contract
            // Check by extracting the original contract number from RENEW contract number
            if (existingContractNumber != null && existingContractNumber.contains("-RENEW-")) {
                String originalContractNumber = existingContractNumber.substring(0, existingContractNumber.indexOf("-RENEW-"));
                if (originalContractNumber.equals(oldContractNumber)) {
                    log.debug("Skipping RENEW contract {} - same original contract number {}", 
                            existingContractNumber, originalContractNumber);
                    continue;
                }
            }
            
            // Skip INACTIVE and PENDING contracts - these are renewal contracts that haven't been paid yet
            // Only check overlap with ACTIVE contracts
            if ("INACTIVE".equals(existing.getStatus()) || "PENDING".equals(existing.getStatus())) {
                log.debug("Skipping contract {} - status is {} (not yet active/paid)", 
                        existingContractNumber, existing.getStatus());
                continue;
            }
            
            // Check if dates overlap (only for ACTIVE contracts)
            // Only check overlap if existing contract's end date is in the future (still active)
            // If existing contract has already ended, allow new contract to start
            if (existing.getStartDate() != null && existing.getEndDate() != null) {
                LocalDate today = LocalDate.now();
                
                // Skip if existing contract has already ended (endDate is in the past)
                // This allows new contracts to start after old contracts have expired
                if (existing.getEndDate().isBefore(today)) {
                    log.debug("Skipping contract {} - end date {} is in the past", 
                            existingContractNumber, existing.getEndDate());
                    continue;
                }
                
                // Check if new contract starts before existing contract ends
                // Only consider it an overlap if new start date is before existing end date
                // If new start date equals existing end date, it's considered consecutive (no overlap)
                boolean overlaps = newStartDate.isBefore(existing.getEndDate()) && 
                                 newEndDate.isAfter(existing.getStartDate());
                
                if (overlaps) {
                    log.warn("Overlap detected: Existing contract {} ({}) overlaps with new renewal period {} to {}", 
                            existingContractNumber, existing.getId(), newStartDate, newEndDate);
                    throw new IllegalArgumentException(
                        String.format("Hợp đồng mới trùng thời gian với hợp đồng hiện có (Số hợp đồng: %s, từ %s đến %s). " +
                                    "Vui lòng chọn khoảng thời gian khác.",
                        existing.getContractNumber(),
                        existing.getStartDate(),
                        existing.getEndDate())
                    );
                }
            }
        }
        
        // Check if start date is today - if not, status should be INACTIVE
        LocalDate today = LocalDate.now();
        String newStatus = newStartDate.equals(today) ? "ACTIVE" : "INACTIVE";
        
        // Create new contract based on old contract
        Contract newContract = Contract.builder()
                .unitId(oldContract.getUnitId())
                .contractNumber(oldContract.getContractNumber()) // Same contract number
                .contractType(oldContract.getContractType())
                .startDate(newStartDate)
                .endDate(newEndDate)
                .monthlyRent(oldContract.getMonthlyRent())
                .notes(oldContract.getNotes())
                .status(newStatus)
                .renewalStatus("PENDING")
                .createdBy(createdBy)
                .build();
        
        newContract = contractRepository.save(newContract);
        log.info("Created renewal contract: {} for old contract: {}", newContract.getId(), oldContractId);
        
        return toDto(newContract);
    }

    /**
     * Create VNPay payment URL for contract renewal
     */
    @Transactional
    public ContractRenewalResponse createRenewalPaymentUrl(UUID contractId, 
                                                           LocalDate newStartDate, 
                                                           LocalDate newEndDate,
                                                           UUID createdBy,
                                                           String clientIp,
                                                           UUID userId,
                                                           String accessToken) {
        try {
            Contract oldContract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
            
            if (!"RENTAL".equals(oldContract.getContractType())) {
                throw new IllegalArgumentException("Only RENTAL contracts can be renewed");
            }
            
            if (oldContract.getMonthlyRent() == null) {
                throw new IllegalArgumentException("Contract monthly rent is required for renewal");
            }
            
            // Check if contract has already been renewed
            if (oldContract.getRenewedContractId() != null) {
                throw new IllegalArgumentException("Hợp đồng này đã được gia hạn thành công. Không thể gia hạn lại.");
            }
            
            // Kiểm tra quyền OWNER/TENANT: chỉ OWNER hoặc TENANT mới được gia hạn hợp đồng
            if (userId != null && oldContract.getUnitId() != null) {
                boolean isOwner = baseServiceClient.isOwnerOfUnit(userId, oldContract.getUnitId(), accessToken);
                if (!isOwner) {
                    throw new IllegalStateException(
                        "Chỉ chủ căn hộ (OWNER hoặc người thuê TENANT) mới được gia hạn hợp đồng. " +
                        "Thành viên hộ gia đình không được phép gia hạn."
                    );
                }
            }
            
            // Validate dates: Ngày kết thúc phải sau ngày bắt đầu và không được trùng nhau
            if (newStartDate.isAfter(newEndDate) || newStartDate.isEqual(newEndDate)) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu và không được trùng nhau");
            }
            
            // Validate: Gia hạn phải ít nhất 3 tháng
            long monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(
                newStartDate.withDayOfMonth(1), 
                newEndDate.withDayOfMonth(1)
            );
            if (monthsBetween < 3) {
                throw new IllegalArgumentException("Gia hạn hợp đồng phải ít nhất 3 tháng. Ngày kết thúc phải cách ngày bắt đầu ít nhất 3 tháng.");
            }
            
            // Check for overlapping contracts (không được trùng thời gian)
            List<Contract> existingContracts = contractRepository.findByUnitId(oldContract.getUnitId());
            String oldContractNumber = oldContract.getContractNumber();
            
            log.info("🔍 Checking overlap for contract renewal. Old contract ID: {}, Old contract number: {}", 
                    contractId, oldContractNumber);
            log.info("🔍 Total existing contracts found: {}", existingContracts.size());
            
            for (Contract existing : existingContracts) {
                String existingContractNumber = existing.getContractNumber();
                log.info("🔍 Checking contract: {} ({}), status: {}", 
                        existingContractNumber, existing.getId(), existing.getStatus());
                
                // Skip the old contract itself and cancelled/expired contracts
                if (existing.getId().equals(contractId) || 
                    "CANCELLED".equals(existing.getStatus()) || 
                    "EXPIRED".equals(existing.getStatus())) {
                    log.info("✅ Skipping contract {} - same ID or cancelled/expired", existing.getId());
                    continue;
                }
                
                // Skip renewal contracts (RENEW) of the same original contract
                // These are contracts that were created from renewing this same contract
                // Format: {oldContractNumber}-RENEW-{timestamp}
                if (existingContractNumber != null) {
                    String checkPrefix = oldContractNumber + "-RENEW-";
                    log.info("🔍 Checking if {} starts with {}", existingContractNumber, checkPrefix);
                    if (existingContractNumber.startsWith(checkPrefix)) {
                        log.info("✅ Skipping RENEW contract {} - same original contract (startsWith check)", existingContractNumber);
                        continue;
                    }
                    
                    // Also skip if existing contract is a RENEW contract that was created from the same original contract
                    // Check by extracting the original contract number from RENEW contract number
                    if (existingContractNumber.contains("-RENEW-")) {
                        String originalContractNumber = existingContractNumber.substring(0, existingContractNumber.indexOf("-RENEW-"));
                        log.info("🔍 Extracted original contract number from RENEW: {} (from {}), comparing with {}", 
                                originalContractNumber, existingContractNumber, oldContractNumber);
                        if (originalContractNumber.equals(oldContractNumber)) {
                            log.info("✅ Skipping RENEW contract {} - same original contract number {}", 
                                    existingContractNumber, originalContractNumber);
                            continue;
                        } else {
                            log.info("⚠️ RENEW contract {} has different original contract number: {} vs {}", 
                                    existingContractNumber, originalContractNumber, oldContractNumber);
                        }
                    }
                }
                
                // Also skip if this existing contract is the one that the old contract was renewed into
                // (i.e., oldContract.getRenewedContractId() == existing.getId())
                if (oldContract.getRenewedContractId() != null && 
                    oldContract.getRenewedContractId().equals(existing.getId())) {
                    log.info("✅ Skipping contract {} - this is the renewed contract", existing.getId());
                    continue;
                }
                
                // Skip INACTIVE and PENDING contracts - these are renewal contracts that haven't been paid yet
                // Only check overlap with ACTIVE contracts
                if ("INACTIVE".equals(existing.getStatus()) || "PENDING".equals(existing.getStatus())) {
                    log.info("✅ Skipping contract {} - status is {} (not yet active/paid)", 
                            existingContractNumber, existing.getStatus());
                    continue;
                }
                
                // Check if dates overlap (only for ACTIVE contracts)
                // Only check overlap if existing contract's end date is in the future (still active)
                // If existing contract has already ended, allow new contract to start
                if (existing.getStartDate() != null && existing.getEndDate() != null) {
                    LocalDate today = LocalDate.now();
                    
                    // Skip if existing contract has already ended (endDate is in the past)
                    // This allows new contracts to start after old contracts have expired
                    if (existing.getEndDate().isBefore(today)) {
                        log.info("✅ Skipping contract {} - end date {} is in the past", 
                                existingContractNumber, existing.getEndDate());
                        continue;
                    }
                    
                    // Check if new contract starts before existing contract ends
                    // Only consider it an overlap if new start date is before existing end date
                    // If new start date equals existing end date, it's considered consecutive (no overlap)
                    boolean overlaps = newStartDate.isBefore(existing.getEndDate()) && 
                                     newEndDate.isAfter(existing.getStartDate());
                    
                    if (overlaps) {
                        log.warn("❌ Overlap detected: Existing contract {} ({}) overlaps with new renewal period {} to {}", 
                                existingContractNumber, existing.getId(), newStartDate, newEndDate);
                        throw new IllegalArgumentException(
                            String.format("Hợp đồng mới trùng thời gian với hợp đồng hiện có (Số hợp đồng: %s, từ %s đến %s). " +
                                        "Vui lòng chọn khoảng thời gian khác.",
                            existing.getContractNumber(),
                            existing.getStartDate(),
                            existing.getEndDate())
                        );
                    }
                }
            }
            
            // Create new contract first (with PENDING status, will be activated after payment)
            // Generate new contract number for renewal (append timestamp to avoid duplicate)
            String newContractNumber = oldContract.getContractNumber() + "-RENEW-" + System.currentTimeMillis();
            
            // Check if contract number already exists (very unlikely but safe)
            int retryCount = 0;
            while (contractRepository.findByContractNumber(newContractNumber).isPresent() && retryCount < 5) {
                newContractNumber = oldContract.getContractNumber() + "-RENEW-" + System.currentTimeMillis() + "-" + retryCount;
                retryCount++;
            }
            
            Contract newContract = Contract.builder()
                    .unitId(oldContract.getUnitId())
                    .contractNumber(newContractNumber)
                    .contractType(oldContract.getContractType())
                    .startDate(newStartDate)
                    .endDate(newEndDate)
                    .monthlyRent(oldContract.getMonthlyRent())
                    .notes(oldContract.getNotes())
                    .status("INACTIVE") // Will be activated after payment
                    .renewalStatus("PENDING")
                    .createdBy(createdBy)
                    .build();
            
            newContract = contractRepository.save(newContract);
            
            // Calculate total amount
            BigDecimal totalAmount = calculateTotalRent(newContract);
            if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Invalid contract amount for payment: contractId={}, totalAmount={}", 
                        newContract.getId(), totalAmount);
                throw new IllegalArgumentException("Invalid contract amount for payment: " + totalAmount);
            }
            
            // Create VNPay payment URL
            // Use newContractId as part of orderId to track it
            Long orderId = newContract.getId().getMostSignificantBits() & Long.MAX_VALUE;
            String orderInfo = String.format("Gia hạn hợp đồng %s - ContractId:%s", 
                    oldContract.getContractNumber(), newContract.getId());
            
            String returnUrlBase = vnpayProperties.getContractRenewalReturnUrl();
            if (returnUrlBase == null || returnUrlBase.isEmpty()) {
                log.error("Contract renewal return URL is not configured");
                throw new IllegalStateException("Contract renewal return URL is not configured. Please check vnpay.contract-renewal-return-url or vnpay.base-url in application properties");
            }
            
            String returnUrl = returnUrlBase + "?contractId=" + newContract.getId();
            
            VnpayService.VnpayPaymentResult paymentResult = vnpayService.createPaymentUrl(
                    orderId,
                    orderInfo,
                    totalAmount,
                    clientIp,
                    returnUrl
            );
            
            log.info("Created VNPay payment URL for contract renewal: contractId={}, newContractId={}, amount={}", 
                    contractId, newContract.getId(), totalAmount);
            
            return ContractRenewalResponse.builder()
                    .newContractId(newContract.getId())
                    .contractNumber(newContract.getContractNumber())
                    .totalAmount(totalAmount)
                    .paymentUrl(paymentResult.paymentUrl())
                    .message("Vui lòng thanh toán để hoàn tất gia hạn hợp đồng")
                    .build();
        } catch (IllegalArgumentException e) {
            log.error("Error creating renewal payment URL: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating renewal payment URL for contractId={}", contractId, e);
            throw new RuntimeException("Failed to create payment URL: " + e.getMessage(), e);
        }
    }

    /**
     * Handle VNPay callback for contract renewal payment
     * Note: This requires storing txnRef -> contractId mapping when creating payment URL
     * For now, we'll extract contractId from orderInfo or use a query parameter
     */
    @Transactional
    public ContractDto handleVnpayCallback(Map<String, String> params, UUID contractId) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Missing callback data from VNPAY");
        }
        
        boolean signatureValid = vnpayService.validateReturn(params);
        if (!signatureValid) {
            throw new IllegalArgumentException("Invalid VNPAY signature");
        }
        
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        
        if (txnRef == null || txnRef.isEmpty()) {
            throw new IllegalArgumentException("Missing transaction reference from VNPAY");
        }
        
        log.info("Processing VNPay callback: txnRef={}, responseCode={}, contractId={}", txnRef, responseCode, contractId);
        
        if ("00".equals(responseCode)) {
            // Payment successful - complete the renewal
            // Get contract to retrieve unitId
            Contract newContract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
            
            // Get residentId from contract's unitId
            Optional<UUID> residentIdOpt = baseServiceClient.getPrimaryResidentIdByUnitId(newContract.getUnitId());
            if (residentIdOpt.isEmpty()) {
                log.warn("⚠️ Cannot find resident for unitId: {}. This may happen if unit has no active household.", newContract.getUnitId());
                log.warn("⚠️ Contract createdBy (userId): {}", newContract.getCreatedBy());
                throw new IllegalArgumentException("Cannot find resident for contract unit: " + newContract.getUnitId() + 
                        ". Please ensure the unit has an active household with a primary resident.");
            }
            
            return completeRenewalPayment(contractId, residentIdOpt.get(), txnRef);
        } else {
            throw new IllegalArgumentException("VNPay payment failed with response code: " + responseCode);
        }
    }

    /**
     * Extract VNPay params from HttpServletRequest
     */
    public Map<String, String> extractVnpayParams(jakarta.servlet.http.HttpServletRequest request) {
        return vnpayService.extractParams(request);
    }

    /**
     * Map contract type to Vietnamese name
     */
    private String mapContractTypeToVietnamese(String contractType) {
        if (contractType == null) {
            return "THUÊ";
        }
        String upperType = contractType.toUpperCase();
        return switch (upperType) {
            case "RENTAL" -> "THUÊ";
            case "PURCHASE" -> "MUA";
            case "SERVICE" -> "DỊCH VỤ";
            case "PARKING" -> "GIỮ XE";
            default -> upperType;
        };
    }

    /**
     * Find the original contract (not a renewal) from a given contract
     * Traces back through the renewal chain to find the root contract
     */
    private Contract findOriginalContract(Contract contract) {
        if (contract == null) {
            return null;
        }
        
        // Check if this contract is already the original (not a renewal)
        String contractNumber = contract.getContractNumber();
        boolean isRenewal = (contractNumber != null && contractNumber.contains("Gia hạn lần")) ||
                           (contractNumber != null && contractNumber.contains("-RENEW-"));
        
        if (!isRenewal && contract.getRenewedContractId() == null) {
            // This is the original contract
            return contract;
        }
        
        // Find the original contract by looking for contracts in the same unit that:
        // 1. Don't have "Gia hạn lần" in their name
        // 2. Don't have "-RENEW-" in their name (old format)
        // 3. Don't have renewedContractId set (not a renewal)
        // 4. Have the earliest created date (the first contract)
        List<Contract> allContracts = contractRepository.findByUnitId(contract.getUnitId());
        List<Contract> originalCandidates = allContracts.stream()
                .filter(c -> {
                    String cn = c.getContractNumber();
                    return cn != null && 
                           !cn.contains("Gia hạn lần") && 
                           !cn.contains("-RENEW-") &&
                           c.getRenewedContractId() == null;
                })
                .collect(Collectors.toList());
        
        if (originalCandidates.isEmpty()) {
            // No original found, return the contract itself
            return contract;
        }
        
        // Return the one with earliest created date
        return originalCandidates.stream()
                .min((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                })
                .orElse(contract);
    }

    /**
     * Count the number of renewals from the original contract
     * Returns the renewal count (0 = original, 1 = first renewal, 2 = second renewal, etc.)
     */
    private int countRenewalSequence(Contract contract) {
        if (contract == null) {
            return 0;
        }
        
        Contract originalContract = findOriginalContract(contract);
        if (originalContract == null) {
            return 0;
        }
        
        // If this is the original contract itself, return 0
        if (originalContract.getId().equals(contract.getId())) {
            return 0;
        }
        
        // Count renewals by following the chain from original to current
        // The chain: original -> renewal1 (renewedContractId = original.id) -> renewal2 (renewedContractId = renewal1.id) -> ...
        int renewalCount = 0;
        Contract current = originalContract;
        
        while (current != null && !current.getId().equals(contract.getId())) {
            // Store current contract ID in final variable for use in lambda
            final UUID currentContractId = current.getId();
            final UUID currentUnitId = current.getUnitId();
            
            // Find the contract that was renewed from current (where renewedContractId = current.id)
            List<Contract> renewals = contractRepository.findByUnitId(currentUnitId).stream()
                    .filter(c -> currentContractId.equals(c.getRenewedContractId()))
                    .collect(Collectors.toList());
            
            if (renewals.isEmpty()) {
                // No renewal found, we've reached the end of the chain
                // If we haven't found the target contract, it means it's not in the chain
                break;
            }
            
            // Should only be one renewal per contract, but if there are multiple, use the first one
            current = renewals.get(0);
            renewalCount++;
            
            // Safety check to avoid infinite loop
            if (renewalCount > 100) {
                log.warn("Renewal chain too long for contract {}, stopping at count {}", contract.getId(), renewalCount);
                break;
            }
        }
        
        // If we found the contract in the chain, return the count
        // Otherwise, count all renewals for this unit (fallback)
        if (current != null && current.getId().equals(contract.getId())) {
            return renewalCount;
        } else {
            // Fallback: count all contracts with "Gia hạn lần" for this unit
            List<Contract> allRenewals = contractRepository.findByUnitId(contract.getUnitId()).stream()
                    .filter(c -> {
                        String cn = c.getContractNumber();
                        return cn != null && cn.contains("Gia hạn lần");
                    })
                    .collect(Collectors.toList());
            return allRenewals.size();
        }
    }


    private String generateRenewalContractNumber(UUID unitId, String contractType, LocalDate startDate, Contract oldContract) {
        // Get unit code
        Optional<String> unitCodeOpt = baseServiceClient.getUnitCodeByUnitId(unitId);
        String unitCode = unitCodeOpt.orElse("UNKNOWN");
        
        // Map contract type to Vietnamese
        String contractTypeVi = mapContractTypeToVietnamese(contractType);
        
        // Find original contract
        Contract originalContract = findOriginalContract(oldContract);
        
        // Count how many renewals have been made from the original contract
        int renewalSequence = 0;
        
        if (originalContract != null) {
            // Count all contracts that are renewals of the original contract
            // These are contracts where we can trace back to the original through renewedContractId chain
            List<Contract> allContracts = contractRepository.findByUnitId(unitId);
            List<Contract> renewals = allContracts.stream()
                    .filter(c -> {
                        // Check if this contract is a renewal (has "Gia hạn lần" in name)
                        String cn = c.getContractNumber();
                        if (cn == null) return false;
                        return cn.contains("Gia hạn lần") || cn.contains("-RENEW-");
                    })
                    .collect(Collectors.toList());
            
            renewalSequence = renewals.size() + 1; // Next renewal number
        } else {
            // Fallback: count all renewals for this unit
            List<Contract> allRenewals = contractRepository.findByUnitId(unitId).stream()
                    .filter(c -> {
                        String cn = c.getContractNumber();
                        return cn != null && (cn.contains("Gia hạn lần") || cn.contains("-RENEW-"));
                    })
                    .collect(Collectors.toList());
            renewalSequence = allRenewals.size() + 1;
        }
        
        // Format: HĐ-{LOẠI}-{MÃ_CĂN} – Gia hạn lần {N}
        String contractNumber = String.format("HĐ-%s-%s – Gia hạn lần %d", contractTypeVi, unitCode, renewalSequence);
        
        // Ensure uniqueness (retry if needed)
        int retryCount = 0;
        while (contractRepository.findByContractNumber(contractNumber).isPresent() && retryCount < 10) {
            renewalSequence++;
            contractNumber = String.format("HĐ-%s-%s – Gia hạn lần %d", contractTypeVi, unitCode, renewalSequence);
            retryCount++;
        }
        
        if (retryCount >= 10) {
            // Fallback: use timestamp if too many retries
            contractNumber = String.format("HĐ-%s-%s – Gia hạn lần %d-%d", contractTypeVi, unitCode, renewalSequence, System.currentTimeMillis());
        }
        
        return contractNumber;
    }

    /**
     * Complete contract renewal after successful payment
     */
    @Transactional
    public ContractDto completeRenewalPayment(UUID newContractId, UUID residentId, String vnpayTransactionRef) {
        Contract newContract = contractRepository.findById(newContractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + newContractId));
        
        if (!"PENDING".equals(newContract.getRenewalStatus())) {
            throw new IllegalArgumentException("Contract is not in PENDING renewal status");
        }
        
        // Get unit code
        Optional<String> unitCodeOpt = baseServiceClient.getUnitCodeByUnitId(newContract.getUnitId());
        String unitCode = unitCodeOpt.orElse("N/A");
        
        // Save old contract number before generating new one (to find old contract later)
        String oldContractNumber = null;
        Contract oldContract = null;
        String tempContractNumber = newContract.getContractNumber();
        if (tempContractNumber != null && tempContractNumber.contains("-RENEW-")) {
            // Extract old contract number from temporary format: {oldContractNumber}-RENEW-{timestamp}
            oldContractNumber = tempContractNumber.substring(0, tempContractNumber.indexOf("-RENEW-"));
            // Find the old contract
            Optional<Contract> oldContractOpt = contractRepository.findByContractNumber(oldContractNumber);
            if (oldContractOpt.isPresent()) {
                oldContract = oldContractOpt.get();
            }
        }

        // If we couldn't find old contract by number, try to find it by tracing back through renewedContractId
        if (oldContract == null) {
            // Store newContract properties in final variables for use in lambda
            final UUID newContractUnitId = newContract.getUnitId();
            final UUID currentContractId = newContract.getId();
            
            // Try to find contracts that might be renewed into this one
            List<Contract> possibleOldContracts = contractRepository.findByUnitId(newContractUnitId).stream()
                    .filter(c -> !c.getId().equals(currentContractId))
                    .filter(c -> c.getContractNumber() != null && !c.getContractNumber().contains("Gia hạn lần"))
                    .filter(c -> c.getContractNumber() != null && !c.getContractNumber().contains("-RENEW-"))
                    .collect(Collectors.toList());
            
            // If there's only one possible old contract, use it
            if (possibleOldContracts.size() == 1) {
                oldContract = possibleOldContracts.get(0);
            } else if (!possibleOldContracts.isEmpty()) {
                // Use the most recent one (by created date)
                oldContract = possibleOldContracts.stream()
                        .max((a, b) -> {
                            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                            if (a.getCreatedAt() == null) return -1;
                            if (b.getCreatedAt() == null) return 1;
                            return a.getCreatedAt().compareTo(b.getCreatedAt());
                        })
                        .orElse(null);
            }
        }

        // Generate new standardized contract number for renewal
        // This replaces the temporary "-RENEW-{timestamp}" format with a proper standardized name
        String newContractNumber = generateRenewalContractNumber(
                newContract.getUnitId(),
                newContract.getContractType(),
                newContract.getStartDate(),
                oldContract != null ? oldContract : newContract // Use newContract as fallback
        );
        newContract.setContractNumber(newContractNumber);
        log.info("Generated new contract number for renewal: {} (replacing temporary number: {})", 
                newContractNumber, tempContractNumber);
        
        // Calculate total amount
        BigDecimal totalAmount = calculateTotalRent(newContract);
        
        // Create invoice
        UUID invoiceId = invoiceClient.createContractRenewalInvoice(
                newContract.getId(),
                newContract.getUnitId(),
                residentId,
                newContract.getContractNumber(),
                unitCode,
                totalAmount,
                newContract.getStartDate(),
                newContract.getEndDate()
        );
        
        if (invoiceId == null) {
            log.warn("Failed to create invoice for contract renewal, but continuing...");
        }
        
        // Update contract status
        LocalDate today = LocalDate.now();
        if (newContract.getStartDate().equals(today)) {
            newContract.setStatus("ACTIVE");
        } else {
            newContract.setStatus("INACTIVE"); // Will be activated by scheduler when start date arrives
        }
        
        newContract.setRenewalStatus("PENDING"); // Reset for new cycle
        newContract = contractRepository.save(newContract);
        
        // Find and update the old contract to mark it as renewed
        if (oldContractNumber != null && oldContract == null) {
            // Try to find old contract by number if we haven't found it yet
            Optional<Contract> oldContractOpt = contractRepository.findByContractNumber(oldContractNumber);
            if (oldContractOpt.isPresent()) {
                oldContract = oldContractOpt.get();
            }
        }
        
        if (oldContract != null) {
            oldContract.setRenewedContractId(newContract.getId());
            contractRepository.save(oldContract);
            log.info("Marked old contract {} ({}) as renewed with new contract {} ({})", 
                    oldContract.getId(), oldContract.getContractNumber(), 
                    newContract.getId(), newContract.getContractNumber());
        } else {
            if (oldContractNumber != null) {
                log.warn("Could not find old contract with number: {} to mark as renewed", oldContractNumber);
            } else {
                log.warn("Could not extract old contract number from temporary number: {} for new contract {}", 
                        tempContractNumber, newContractId);
            }
        }
        
        log.info("Completed contract renewal payment: contractId={}, contractNumber={}, invoiceId={}, vnpayTxnRef={}", 
                newContract.getId(), newContract.getContractNumber(), invoiceId, vnpayTransactionRef);
        
        return toDto(newContract);
    }
    public void triggerRenewalReminders() {
        log.info("Manual trigger: Send renewal reminders");
        java.time.LocalDate today = java.time.LocalDate.now();
        
        // Get all active RENTAL contracts that need reminders
        List<com.QhomeBase.datadocsservice.model.Contract> allContracts = findContractsNeedingRenewalReminder();
        log.info("Found {} contract(s) that may need renewal reminders", allContracts.size());
        
        int firstReminderCount = 0;
        int secondReminderCount = 0;
        int thirdReminderCount = 0;
        
        for (com.QhomeBase.datadocsservice.model.Contract contract : allContracts) {
            if (contract.getEndDate() == null || !"RENTAL".equals(contract.getContractType()) 
                    || !"ACTIVE".equals(contract.getStatus())) {
                continue;
            }
            
            java.time.LocalDate endDate = contract.getEndDate();
            
            // Calculate days until end date
            long daysUntilEndDate = java.time.temporal.ChronoUnit.DAYS.between(today, endDate);
            
            log.debug("Checking contract {}: endDate={}, today={}, daysUntilEndDate={}, renewalStatus={}, reminderSentAt={}", 
                    contract.getContractNumber(), endDate, today, daysUntilEndDate,
                    contract.getRenewalStatus(), contract.getRenewalReminderSentAt());
            
            try {
                // Lần 1: 30 ngày trước khi hết hạn hợp đồng
                // Gửi khi còn 29-31 ngày (buffer để đảm bảo không bỏ sót)
                if (daysUntilEndDate >= 29 && daysUntilEndDate <= 31 
                        && contract.getRenewalReminderSentAt() == null) {
                    sendRenewalReminder(contract.getId());
                    firstReminderCount++;
                    log.info("✅ Sent FIRST renewal reminder for contract {} (expires on {}, {} days until end date)", 
                            contract.getContractNumber(), endDate, daysUntilEndDate);
                }
                // Lần 2: 20 ngày trước khi hết hạn hợp đồng
                // Chỉ gửi nếu:
                // - Đã gửi lần 1 (renewalReminderSentAt != null)
                // - Còn 19-21 ngày trước khi hết hạn (buffer)
                // - Lần 1 đã được gửi trước đó (ít nhất 1 ngày trước)
                else if (contract.getRenewalReminderSentAt() != null
                        && daysUntilEndDate >= 19 && daysUntilEndDate <= 21) {
                    java.time.LocalDate firstReminderDate = contract.getRenewalReminderSentAt().toLocalDate();
                    // Đảm bảo lần 1 đã được gửi trước đó (ít nhất 1 ngày)
                    if (firstReminderDate.isBefore(today)) {
                        sendRenewalReminder(contract.getId());
                        secondReminderCount++;
                        log.info("✅ Sent SECOND renewal reminder for contract {} (expires on {}, {} days until end date)", 
                                contract.getContractNumber(), endDate, daysUntilEndDate);
                    } else {
                        log.debug("⏭️ Skipping reminder 3 for contract {}: firstReminderDate={}, today={}", 
                                contract.getContractNumber(), firstReminderDate, today);
                    }
                }
                // Lần 3: 10 ngày trước khi hết hạn hợp đồng - BẮT BUỘC
                // Chỉ gửi nếu:
                // - Đã gửi lần 1 (renewalReminderSentAt != null)
                // - Còn 9-11 ngày trước khi hết hạn (buffer)
                // - Lần 1 đã được gửi trước đó (ít nhất 1 ngày trước)
                else if (contract.getRenewalReminderSentAt() != null
                        && daysUntilEndDate >= 9 && daysUntilEndDate <= 11) {
                    java.time.LocalDate firstReminderDate = contract.getRenewalReminderSentAt().toLocalDate();
                    // Đảm bảo lần 1 đã được gửi trước đó (ít nhất 1 ngày)
                    if (firstReminderDate.isBefore(today)) {
                        sendRenewalReminder(contract.getId());
                        thirdReminderCount++;
                        log.info("✅ Sent THIRD (FINAL) renewal reminder for contract {} (expires on {}, {} days until end date - BẮT BUỘC HỦY HOẶC GIA HẠN)", 
                                contract.getContractNumber(), endDate, daysUntilEndDate);
                    } else {
                        log.debug("⏭️ Skipping reminder 2 for contract {}: firstReminderDate={}, today={}", 
                                contract.getContractNumber(), firstReminderDate, today);
                    }
                }
            } catch (Exception e) {
                log.error("Error sending renewal reminder for contract {}", contract.getId(), e);
            }
        }
        
        log.info("Manual trigger completed: Sent {} first reminder(s), {} second reminder(s), {} third reminder(s)", 
                firstReminderCount, secondReminderCount, thirdReminderCount);
    }

    /**
     * Handle contract end: delete household or clear primaryResidentId
     * This is called when a rental contract is EXPIRED or CANCELLED
     */
    private void handleContractEnd(UUID unitId) {
        try {
            // Get current household for this unit
            Optional<Map<String, Object>> householdOpt = baseServiceClient.getCurrentHouseholdByUnitId(unitId);
            
            if (householdOpt.isPresent()) {
                Map<String, Object> household = householdOpt.get();
                Object householdIdObj = household.get("id");
                
                if (householdIdObj != null) {
                    UUID householdId = householdIdObj instanceof UUID 
                            ? (UUID) householdIdObj 
                            : UUID.fromString(householdIdObj.toString());
                    
                    // Delete household (set endDate to today)
                    baseServiceClient.deleteHousehold(householdId);
                    log.info("✅ Deleted household {} for unit {} after contract ended", householdId, unitId);
                } else {
                    log.warn("⚠️ Household found but no ID for unit: {}", unitId);
                }
            } else {
                log.debug("No active household found for unit: {} (may have already been deleted)", unitId);
            }
        } catch (Exception ex) {
            log.error("❌ Error handling contract end for unit: {}", unitId, ex);
            // Don't throw exception - allow contract processing to proceed even if household deletion fails
        }
    }

}

