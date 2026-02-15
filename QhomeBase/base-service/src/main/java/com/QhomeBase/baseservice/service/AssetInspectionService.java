package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.client.ContractClient;
import com.QhomeBase.baseservice.client.FinanceBillingClient;
import com.QhomeBase.baseservice.dto.*;
import com.QhomeBase.baseservice.dto.finance.CreateInvoiceLineRequest;
import com.QhomeBase.baseservice.dto.finance.CreateInvoiceRequest;
import com.QhomeBase.baseservice.model.*;
import com.QhomeBase.baseservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetInspectionService {

    private final AssetInspectionRepository inspectionRepository;
    private final AssetInspectionItemRepository inspectionItemRepository;
    private final AssetRepository assetRepository;
    private final UnitRepository unitRepository;
    private final FinanceBillingClient financeBillingClient;
    private final HouseholdService householdService;
    private final ContractClient contractClient;

    @Transactional
    public AssetInspectionDto createInspection(CreateAssetInspectionRequest request, UUID createdBy) {
        inspectionRepository.findByContractId(request.contractId())
                .ifPresent(inspection -> {
                    throw new IllegalArgumentException("Inspection already exists for contract: " + request.contractId());
                });

        ContractDetailDto contract = contractClient.getContractById(request.contractId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + request.contractId()));

        String contractStatus = contract.status();
        if (!"EXPIRED".equalsIgnoreCase(contractStatus) 
                && !"CANCELLED".equalsIgnoreCase(contractStatus)
                && !"ACTIVE".equalsIgnoreCase(contractStatus)) {
            throw new IllegalArgumentException("Can only create inspection for active, expired, or cancelled contracts. Contract status: " + contractStatus);
        }
        
        if ("ACTIVE".equalsIgnoreCase(contractStatus)) {
            log.info("Creating inspection for ACTIVE contract {} (likely being cancelled by data-docs-service)", request.contractId());
        }

        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + request.unitId()));

        LocalDate inspectionDate = request.inspectionDate();
        if (inspectionDate == null) {
            inspectionDate = contract.endDate();
            if (inspectionDate == null) {
                throw new IllegalArgumentException("Contract has no end date. Please specify inspection date.");
            }
        }

        LocalDate scheduledDate = request.scheduledDate();
        if (scheduledDate == null) {
            scheduledDate = contract.endDate();
        }

        AssetInspection inspection = AssetInspection.builder()
                .contractId(request.contractId())
                .unit(unit)
                .inspectionDate(inspectionDate)
                .scheduledDate(scheduledDate)
                .status(InspectionStatus.PENDING)
                .inspectorName(request.inspectorName())
                .inspectorId(request.inspectorId())
                .createdBy(createdBy)
                .build();

        log.info("Creating inspection with inspectorId: {} for contract: {}", request.inspectorId(), request.contractId());
        inspection = inspectionRepository.save(inspection);
        log.info("Created inspection: {} with inspectorId: {}", inspection.getId(), inspection.getInspectorId());

        List<Asset> assets = assetRepository.findByUnitId(request.unitId())
                .stream()
                .filter(Asset::getActive)
                .collect(Collectors.toList());

        log.info("Found {} active assets in unit {} for inspection {}", assets.size(), request.unitId(), inspection.getId());
        
        for (Asset asset : assets) {
            AssetInspectionItem item = AssetInspectionItem.builder()
                    .inspection(inspection)
                    .asset(asset)
                    .checked(false)
                    .build();
            inspectionItemRepository.save(item);
            log.debug("Created inspection item for asset: {} (type: {})", asset.getAssetCode(), asset.getAssetType());
        }

        log.info("Created asset inspection: {} for contract: {} with {} items", inspection.getId(), request.contractId(), assets.size());
        return toDto(inspection);
    }

    @Transactional(readOnly = true)
    public AssetInspectionDto getInspectionByContractId(UUID contractId) {
        AssetInspection inspection = inspectionRepository.findByContractId(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found for contract: " + contractId));
        return toDto(inspection);
    }

    @Transactional
    public AssetInspectionItemDto updateInspectionItem(UUID itemId, UpdateAssetInspectionItemRequest request, UUID checkedBy) {
        AssetInspectionItem item = inspectionItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection item not found: " + itemId));

        if (request.conditionStatus() != null) {
            item.setConditionStatus(request.conditionStatus());
        }
        if (request.damageCost() != null) {
            item.setDamageCost(request.damageCost());
        } else {
            if (item.getDamageCost() == null) {
                item.setDamageCost(BigDecimal.ZERO);
            }
        }
        if (request.notes() != null) {
            item.setNotes(request.notes());
        }
        if (request.checked() != null) {
            item.setChecked(request.checked());
            if (request.checked()) {
                item.setCheckedAt(OffsetDateTime.now());
                item.setCheckedBy(checkedBy);
            } else {
                item.setCheckedAt(null);
                item.setCheckedBy(null);
            }
        }

        item = inspectionItemRepository.save(item);

        AssetInspection inspection = item.getInspection();
        updateTotalDamageCost(inspection);

        List<AssetInspectionItem> allItems = inspectionItemRepository.findByInspectionId(inspection.getId());
        boolean allChecked = allItems.stream().allMatch(AssetInspectionItem::getChecked);
        if (allChecked && inspection.getStatus() == InspectionStatus.IN_PROGRESS) {
            inspection.setStatus(InspectionStatus.COMPLETED);
            inspection.setCompletedAt(OffsetDateTime.now());
            inspection.setCompletedBy(checkedBy);
            inspectionRepository.save(inspection);
        }

        return toItemDto(item);
    }

    @Transactional
    public AssetInspectionDto startInspection(UUID inspectionId, UUID userId) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        if (inspection.getStatus() != InspectionStatus.PENDING) {
            throw new IllegalArgumentException("Inspection is not in PENDING status");
        }

        inspection.setStatus(InspectionStatus.IN_PROGRESS);
        inspection = inspectionRepository.save(inspection);

        log.info("Started inspection: {}", inspectionId);
        return toDto(inspection);
    }

    @Transactional
    public AssetInspectionDto completeInspection(UUID inspectionId, String inspectorNotes, UUID userId) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        updateTotalDamageCost(inspection);
        
        inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        inspection.setStatus(InspectionStatus.COMPLETED);
        inspection.setInspectorNotes(inspectorNotes);
        inspection.setCompletedAt(OffsetDateTime.now());
        inspection.setCompletedBy(userId);
        inspection = inspectionRepository.save(inspection);

        log.info("Completed inspection: {} with total damage cost: {}", inspectionId, inspection.getTotalDamageCost());
        
        return toDto(inspection);
    }

    @Transactional(readOnly = true)
    public List<AssetInspectionDto> getAllInspections(UUID inspectorId, InspectionStatus status) {
        List<AssetInspection> inspections;
        
        if (inspectorId != null && status != null) {
            inspections = inspectionRepository.findByInspectorIdAndStatus(inspectorId, status);
        } else if (inspectorId != null) {
            inspections = inspectionRepository.findByInspectorId(inspectorId);
        } else if (status != null) {
            inspections = inspectionRepository.findByStatus(status);
        } else {
            inspections = inspectionRepository.findAll();
        }
        
        return inspections.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetInspectionDto> getInspectionsByTechnicianId(UUID technicianId) {
        List<AssetInspection> inspections = inspectionRepository.findByInspectorId(technicianId);
        return inspections.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetInspectionDto> getMyAssignments(UUID userId) {
        if (userId == null) {
            log.warn("getMyAssignments called with null userId");
            return java.util.Collections.emptyList();
        }
        
        List<InspectionStatus> activeStatuses = List.of(
                InspectionStatus.PENDING,
                InspectionStatus.IN_PROGRESS
        );
        
        try {
            log.info("Getting assignments for userId: {}", userId);
            List<AssetInspection> inspections = inspectionRepository.findByInspectorIdAndStatusIn(userId, activeStatuses);
            log.info("Found {} inspections for userId: {}", inspections.size(), userId);
            
            if (!inspections.isEmpty()) {
                inspections.forEach(ins -> log.debug("Inspection {} has inspectorId: {}", ins.getId(), ins.getInspectorId()));
            }
            
            return inspections.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Error getting assignments for userId: {}", userId, ex);
            throw new RuntimeException("Failed to get assignments: " + ex.getMessage(), ex);
        }
    }

    private AssetInspectionDto toDto(AssetInspection inspection) {
        List<AssetInspectionItem> items = inspectionItemRepository.findByInspectionIdWithAsset(inspection.getId());
        log.info("Loading {} items for inspection: {}", items.size(), inspection.getId());
        
        return new AssetInspectionDto(
                inspection.getId(),
                inspection.getContractId(),
                inspection.getUnit() != null ? inspection.getUnit().getId() : null,
                inspection.getUnit() != null ? inspection.getUnit().getCode() : null,
                inspection.getInspectionDate(),
                inspection.getScheduledDate(),
                inspection.getStatus(),
                inspection.getInspectorName(),
                inspection.getInspectorId(),
                inspection.getInspectorNotes(),
                inspection.getCompletedAt(),
                inspection.getCompletedBy(),
                inspection.getCreatedAt(),
                inspection.getUpdatedAt(),
                items.stream().map(this::toItemDto).collect(Collectors.toList()),
                inspection.getTotalDamageCost() != null ? inspection.getTotalDamageCost() : BigDecimal.ZERO,
                inspection.getInvoiceId()
        );
    }

    private AssetInspectionItemDto toItemDto(AssetInspectionItem item) {
        Asset asset = item.getAsset();
        return new AssetInspectionItemDto(
                item.getId(),
                asset != null ? asset.getId() : null,
                asset != null ? asset.getAssetCode() : null,
                asset != null ? asset.getName() : null,
                asset != null ? asset.getAssetType().name() : null,
                item.getConditionStatus(),
                item.getNotes(),
                item.getChecked(),
                item.getCheckedAt(),
                item.getCheckedBy(),
                item.getDamageCost() != null ? item.getDamageCost() : BigDecimal.ZERO
        );
    }

    private void updateTotalDamageCost(AssetInspection inspection) {
        List<AssetInspectionItem> items = inspectionItemRepository.findByInspectionId(inspection.getId());
        log.debug("Calculating total damage cost for inspection {} with {} items", inspection.getId(), items.size());
        
        BigDecimal total = items.stream()
                .map(item -> {
                    BigDecimal damageCost = item.getDamageCost() != null ? item.getDamageCost() : BigDecimal.ZERO;
                    log.debug("Item {} has damageCost: {}", item.getId(), damageCost);
                    return damageCost;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        inspection.setTotalDamageCost(total);
        inspectionRepository.save(inspection);
        
        log.info("Updated total damage cost for inspection {}: {} (from {} items)", inspection.getId(), total, items.size());
    }

    @Transactional
    public AssetInspectionDto recalculateDamageCost(UUID inspectionId) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));
        
        updateTotalDamageCost(inspection);
        return toDto(inspection);
    }

    @Transactional
    public AssetInspectionDto updateScheduledDate(UUID inspectionId, LocalDate scheduledDate) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));
        
        if (scheduledDate == null) {
            throw new IllegalArgumentException("Scheduled date cannot be null");
        }
        
        inspection.setScheduledDate(scheduledDate);
        inspection = inspectionRepository.save(inspection);
        
        log.info("Updated scheduled date for inspection {} to {}", inspectionId, scheduledDate);
        return toDto(inspection);
    }

    @Transactional
    public AssetInspectionDto assignInspector(UUID inspectionId, UUID inspectorId, String inspectorName) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));
        
        if (inspectorId == null) {
            throw new IllegalArgumentException("Inspector ID cannot be null");
        }
        
        inspection.setInspectorId(inspectorId);
        inspection.setInspectorName(inspectorName);
        inspection = inspectionRepository.save(inspection);
        
        log.info("Assigned inspector {} ({}) to inspection {}", inspectorName, inspectorId, inspectionId);
        return toDto(inspection);
    }

   
    @Transactional
    public AssetInspectionDto generateInvoice(UUID inspectionId, UUID createdBy) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));
        
        if (inspection.getStatus() != InspectionStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only generate invoice for completed inspections");
        }
        
        updateTotalDamageCost(inspection);
        inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));
        
        if (inspection.getTotalDamageCost() == null || inspection.getTotalDamageCost().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Cannot generate invoice for inspection {}: totalDamageCost is {} (null or zero)", 
                    inspectionId, inspection.getTotalDamageCost());
            throw new IllegalArgumentException("No damage cost to invoice. Total damage cost is zero.");
        }
        
        if (inspection.getInvoiceId() != null) {
            throw new IllegalArgumentException("Invoice already generated for this inspection. Invoice ID: " + inspection.getInvoiceId());
        }
        
        Unit unit = inspection.getUnit();
        if (unit == null) {
            throw new IllegalArgumentException("Unit not found for inspection: " + inspectionId);
        }

        UUID unitId = unit.getId();

        UUID payerResidentId = householdService.getPayerForUnit(unitId);
        if (payerResidentId == null) {
            log.warn("No primary resident found for unit: {}. Proceeding with null payerResidentId.", unitId);
        }

        String unitCode = unit.getCode() != null ? unit.getCode() : "";
        String billToName = String.format("Căn hộ %s", unitCode);
        BigDecimal totalDamageCost = inspection.getTotalDamageCost();

        String description = String.format("Tiền thiệt hại thiết bị - Kiểm tra ngày %s", 
                inspection.getInspectionDate().toString());

        CreateInvoiceLineRequest invoiceLine = CreateInvoiceLineRequest.builder()
                .serviceDate(inspection.getInspectionDate())
                .description(description)
                .quantity(BigDecimal.ONE)
                .unit("lần")
                .unitPrice(totalDamageCost)
                .taxRate(BigDecimal.ZERO)
                .serviceCode("ASSET_DAMAGE")
                .externalRefType("ASSET_INSPECTION")
                .externalRefId(inspectionId)
                .build();

        List<CreateInvoiceLineRequest> invoiceLines = new java.util.ArrayList<>(List.of(invoiceLine));
        
        try {
            log.info("Attempting to get invoices for unit {} when generating asset inspection invoice (inspection date: {})", 
                    unitId, inspection.getInspectionDate());
            List<com.QhomeBase.baseservice.dto.finance.InvoiceDto> allUnitInvoices = 
                    financeBillingClient.getInvoicesByUnitSync(unitId);
            
            log.info("Found {} total invoices for unit {}", allUnitInvoices.size(), unitId);
            
            String inspectionMarker = "Đo cùng với kiểm tra thiết bị";
            LocalDate inspectionDate = inspection.getInspectionDate();
            
          
            List<com.QhomeBase.baseservice.dto.finance.InvoiceDto> relevantInvoices = allUnitInvoices.stream()
                    .filter(inv -> "PUBLISHED".equals(inv.getStatus()) || "PAID".equals(inv.getStatus()))
                    .filter(inv -> inv.getLines() != null && !inv.getLines().isEmpty())
                    .filter(inv -> {
                        return inv.getLines().stream()
                                .anyMatch(line -> line != null 
                                        && line.getServiceCode() != null
                                        && ("WATER".equals(line.getServiceCode()) || "ELECTRIC".equals(line.getServiceCode()))
                                        && line.getDescription() != null
                                        && line.getDescription().contains(inspectionMarker));
                    })
                    .filter(inv -> {
                        if (inv.getIssuedAt() != null && inspectionDate != null) {
                            LocalDate issuedDate = inv.getIssuedAt().toLocalDate();
                            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(inspectionDate, issuedDate);
                            return Math.abs(daysDiff) <= 30;
                        }
                        return true;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            log.info("Filtered to {} relevant invoices (with WATER/ELECTRIC lines from this inspection) out of {} total invoices", 
                    relevantInvoices.size(), allUnitInvoices.size());
            
            if (relevantInvoices.isEmpty()) {
                log.warn("No relevant invoices found for unit {} with inspection marker - this might be expected if water/electric invoices haven't been created yet", unitId);
            }
            
            int waterElectricCount = 0;
            
            java.util.Map<String, java.math.BigDecimal> waterConsumptions = new java.util.HashMap<>();
            java.util.Map<String, java.math.BigDecimal> electricConsumptions = new java.util.HashMap<>();
            
            for (com.QhomeBase.baseservice.dto.finance.InvoiceDto invoice : relevantInvoices) {
                List<com.QhomeBase.baseservice.dto.finance.InvoiceLineDto> relevantLines = invoice.getLines().stream()
                        .filter(line -> line != null && line.getServiceCode() != null)
                        .filter(line -> "WATER".equals(line.getServiceCode()) || "ELECTRIC".equals(line.getServiceCode()))
                        .filter(line -> line.getDescription() != null && line.getDescription().contains(inspectionMarker))
                        .collect(java.util.stream.Collectors.toList());
                
                if (relevantLines.isEmpty()) {
                    continue;
                }
                
                log.info("Invoice {} has {} relevant water/electric lines from this inspection (status: {}, totalLines: {})", 
                        invoice.getId(), relevantLines.size(), invoice.getStatus(), invoice.getLines().size());
                
                for (com.QhomeBase.baseservice.dto.finance.InvoiceLineDto line : relevantLines) {
                    String serviceCode = line.getServiceCode();
                    
                    // Get unitPrice and quantity separately - DO NOT multiply them!
                    BigDecimal unitPrice = line.getUnitPrice() != null ? line.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal quantity = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ONE;
                    BigDecimal invoiceLineTotal = line.getLineTotal() != null ? line.getLineTotal() : BigDecimal.ZERO;
                    BigDecimal taxAmount = line.getTaxAmount() != null ? line.getTaxAmount() : BigDecimal.ZERO;
                    
                    // Log original values for debugging
                    log.debug("🔍 [AssetInspectionService] Processing {} line - unitPrice: {}, quantity: {}, lineTotal: {}, taxAmount: {}",
                            serviceCode, unitPrice, quantity, invoiceLineTotal, taxAmount);
                    
                    // FIX: Detect and correct unitPrice if it seems wrong
                    // If unitPrice is unreasonably high (> 100,000 VND/kWh), it's likely set to lineTotal by mistake
                    // Note: We can't use lineTotal mismatch check because lineTotal is calculated from unitPrice,
                    // so if unitPrice is wrong, lineTotal will also be wrong and they'll still match
                    BigDecimal maxReasonableUnitPrice = new BigDecimal("100000"); // 100,000 VND/kWh
                    
                    if (unitPrice.compareTo(maxReasonableUnitPrice) > 0 && quantity.compareTo(BigDecimal.ZERO) > 0) {
                        // Recalculate unitPrice from lineTotal and quantity
                        // Since lineTotal might also be wrong (calculated from wrong unitPrice),
                        // we need to reverse-calculate: if unitPrice looks like lineTotal, divide by quantity
                        // But first, try to get correct lineTotal by checking if unitPrice equals lineTotal
                        BigDecimal recalculatedUnitPrice;
                        
                        // If unitPrice equals lineTotal (or very close), it was definitely set wrong
                        BigDecimal tolerance = new BigDecimal("1.0"); // Allow 1 VND difference
                        if (unitPrice.subtract(invoiceLineTotal).abs().compareTo(tolerance) < 0) {
                            // unitPrice was set to lineTotal - recalculate from lineTotal
                            BigDecimal subtotal = invoiceLineTotal.subtract(taxAmount);
                            recalculatedUnitPrice = subtotal.divide(quantity, 4, java.math.RoundingMode.HALF_UP);
                            
                            log.warn("⚠️ [AssetInspectionService] Detected unitPrice {} equals lineTotal {} for {} line. " +
                                    "Recalculating from lineTotal {} - taxAmount {} / quantity {} = {} VND/kWh",
                                    unitPrice, invoiceLineTotal, serviceCode, invoiceLineTotal, taxAmount, quantity, recalculatedUnitPrice);
                        } else {
                            // unitPrice is too high but not equal to lineTotal - might be tierAmount (quantity × unitPrice)
                            // Try dividing by quantity to get unitPrice
                            recalculatedUnitPrice = unitPrice.divide(quantity, 4, java.math.RoundingMode.HALF_UP);
                            
                            log.warn("⚠️ [AssetInspectionService] Detected suspicious unitPrice {} for {} line (quantity: {}). " +
                                    "Recalculating by dividing by quantity: {} / {} = {} VND/kWh",
                                    unitPrice, serviceCode, quantity, unitPrice, quantity, recalculatedUnitPrice);
                        }
                        
                        unitPrice = recalculatedUnitPrice;
                    }
                    
                    String originalDescription = line.getDescription() != null ? line.getDescription() : "";
                    String serviceName = "WATER".equals(serviceCode) ? "nước" : "điện";
                    String finalDescription;
                    if (originalDescription.trim().startsWith("Tiền " + serviceName)) {
                        finalDescription = originalDescription;
                    } else {
                        finalDescription = String.format("Tiền %s - %s", serviceName, originalDescription);
                    }
                    
                    CreateInvoiceLineRequest waterElectricLine = CreateInvoiceLineRequest.builder()
                            .serviceDate(line.getServiceDate() != null ? line.getServiceDate() : inspection.getInspectionDate())
                            .description(finalDescription)
                            .quantity(quantity)
                            .unit(line.getUnit() != null ? line.getUnit() : "kWh")
                            .unitPrice(unitPrice) // Use corrected unitPrice
                            .taxRate(BigDecimal.ZERO)
                            .serviceCode(serviceCode)
                            .externalRefType("WATER_ELECTRIC_INVOICE")
                            .externalRefId(invoice.getId())
                            .build();
                    
                    invoiceLines.add(waterElectricLine);
                    waterElectricCount++;
                    
                    java.math.BigDecimal consumption = quantity;
                    if ("WATER".equals(serviceCode)) {
                        String meterCode = extractMeterCodeFromDescription(line.getDescription());
                        waterConsumptions.put(meterCode != null ? meterCode : "UNKNOWN", 
                                waterConsumptions.getOrDefault(meterCode != null ? meterCode : "UNKNOWN", java.math.BigDecimal.ZERO).add(consumption));
                    } else if ("ELECTRIC".equals(serviceCode)) {
                        String meterCode = extractMeterCodeFromDescription(line.getDescription());
                        electricConsumptions.put(meterCode != null ? meterCode : "UNKNOWN", 
                                electricConsumptions.getOrDefault(meterCode != null ? meterCode : "UNKNOWN", java.math.BigDecimal.ZERO).add(consumption));
                    }
                    
                    // Calculate lineTotal for logging
                    java.math.BigDecimal lineTotal = unitPrice.multiply(quantity);
                    log.info("Including {} line from invoice {} (unitPrice: {}, quantity: {}, lineTotal: {}) in asset inspection invoice", 
                            serviceCode, invoice.getId(), unitPrice, quantity, lineTotal);
                }
            }
            
            if (waterElectricCount == 0) {
                log.warn("No water/electric invoices found for unit {} when generating asset inspection invoice. " +
                        "This might be because invoices haven't been created yet from meter readings.", unitId);
            } else {
                log.info("Added {} water/electric invoice lines to asset inspection invoice", waterElectricCount);
                
                if (!waterConsumptions.isEmpty()) {
                    log.info("=== SỐ NƯỚC ĐÃ ĐO TRONG LẦN KIỂM TRA NÀY (Unit: {}) ===", unitId);
                    waterConsumptions.forEach((meterCode, total) -> {
                        log.info("Đồng hồ nước {}: {}", meterCode, total);
                    });
                    java.math.BigDecimal totalWater = waterConsumptions.values().stream()
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    log.info("TỔNG SỐ NƯỚC: {}", totalWater);
                }
                
                if (!electricConsumptions.isEmpty()) {
                    log.info("=== SỐ ĐIỆN ĐÃ ĐO TRONG LẦN KIỂM TRA NÀY (Unit: {}) ===", unitId);
                    electricConsumptions.forEach((meterCode, total) -> {
                        log.info("Đồng hồ điện {}: {}", meterCode, total);
                    });
                    java.math.BigDecimal totalElectric = electricConsumptions.values().stream()
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    log.info("TỔNG SỐ ĐIỆN: {}", totalElectric);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get water/electricity invoices for unit {}: {}. Proceeding with damage cost only.", 
                    unitId, e.getMessage(), e);
        }

        CreateInvoiceRequest invoiceRequest = CreateInvoiceRequest.builder()
                .dueDate(LocalDate.now().plusDays(7))
                .currency("VND")
                .billToName(billToName)
                .billToAddress(null)
                .billToContact(null)
                .payerUnitId(unitId)
                .payerResidentId(payerResidentId)
                .cycleId(null)
                .status("PAID") 
                .lines(invoiceLines)
                .build();
        try {
            com.QhomeBase.baseservice.dto.finance.InvoiceDto invoiceDto = financeBillingClient.createInvoiceSync(invoiceRequest);
            
            UUID invoiceId = invoiceDto.getId();
            inspection.setInvoiceId(invoiceId);
            inspection = inspectionRepository.save(inspection);
            
            log.info("Generated invoice {} for inspection {} with total damage cost: {}", 
                    invoiceId, inspectionId, inspection.getTotalDamageCost());
            
            return toDto(inspection);
        } catch (Exception e) {
            log.error("Failed to create invoice in finance-billing-service for inspection: {}", inspectionId, e);
            throw new RuntimeException("Failed to create invoice: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<AssetInspectionDto> getInspectionsPendingApproval() {
        List<AssetInspection> inspections = inspectionRepository.findCompletedInspectionsPendingApproval(InspectionStatus.COMPLETED);
        List<AssetInspection> filteredInspections = inspections.stream()
                .filter(ai -> ai.getTotalDamageCost() != null 
                        && ai.getTotalDamageCost().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        log.info("Found {} inspections pending approval (filtered from {} total)", 
                filteredInspections.size(), inspections.size());
        return filteredInspections.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AssetInspectionDto approveInspection(UUID inspectionId, UUID approvedBy) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        if (inspection.getStatus() != InspectionStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only approve completed inspections");
        }

        if (inspection.getTotalDamageCost() == null || inspection.getTotalDamageCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cannot approve inspection with no damage cost");
        }

        if (inspection.getInvoiceId() != null) {
            throw new IllegalArgumentException("Invoice already generated for this inspection");
        }

        log.info("Admin {} approving inspection {} and generating invoice", approvedBy, inspectionId);
        return generateInvoice(inspectionId, approvedBy);
    }

    @Transactional
    public AssetInspectionDto rejectInspection(UUID inspectionId, String rejectionNotes, UUID rejectedBy) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        if (inspection.getStatus() != InspectionStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only reject completed inspections");
        }

        if (inspection.getInvoiceId() != null) {
            throw new IllegalArgumentException("Cannot reject inspection that already has an invoice");
        }

        String currentNotes = inspection.getInspectorNotes() != null ? inspection.getInspectorNotes() : "";
        String rejectionMessage = String.format("\n\n[Admin rejection - %s]: %s", 
                java.time.OffsetDateTime.now().toString(), 
                rejectionNotes != null ? rejectionNotes : "Rejected by admin");
        inspection.setInspectorNotes(currentNotes + rejectionMessage);
        
        inspection = inspectionRepository.save(inspection);
        log.info("Admin {} rejected inspection {}: {}", rejectedBy, inspectionId, rejectionNotes);
        
        return toDto(inspection);
    }


    private String extractMeterCodeFromDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        int meterIndex = description.indexOf("Meter: ");
        if (meterIndex >= 0) {
            int start = meterIndex + "Meter: ".length();
            int end = description.indexOf(" - ", start);
            if (end < 0) {
                end = description.indexOf(" (", start);
            }
            if (end < 0) {
                end = description.length();
            }
            return description.substring(start, end).trim();
        }
        return null;
    }

    @Transactional
    public int createInspectionsForExpiredContracts(LocalDate endOfMonth) {
        log.info("Creating inspections for contracts expired in month ending: {}", endOfMonth);
        
        int createdCount = 0;
        
        List<Unit> allUnits = unitRepository.findAll();
        log.info("Checking {} units for expired contracts", allUnits.size());
        
        for (Unit unit : allUnits) {
            try {
                List<ContractSummary> contracts = contractClient.getContractsByUnit(unit.getId());
                
                for (ContractSummary contract : contracts) {
                    if (contract.endDate() == null) {
                        continue;
                    }
                    
                    LocalDate contractEndDate = contract.endDate();
                    boolean expiredInMonth = contractEndDate.getYear() == endOfMonth.getYear() &&
                                            contractEndDate.getMonth() == endOfMonth.getMonth();
                    
                    if (!expiredInMonth) {
                        continue;
                    }
                    
                    if (inspectionRepository.findByContractId(contract.id()).isPresent()) {
                        log.debug("Inspection already exists for contract: {}", contract.id());
                        continue;
                    }
                    
                    if (!"EXPIRED".equalsIgnoreCase(contract.status())) {
                        continue;
                    }
                    
                    try {
                        LocalDate inspectionDate = contractEndDate;
                        CreateAssetInspectionRequest request = new CreateAssetInspectionRequest(
                                contract.id(),
                                unit.getId(),
                                inspectionDate,
                                null,
                                null,
                                null
                        );
                        
                        createInspection(request, null);
                        createdCount++;
                        log.info("Created automatic inspection for expired contract: {} in unit: {}", 
                                contract.id(), unit.getCode());
                    } catch (IllegalArgumentException e) {
                        log.debug("Skipping contract {}: {}", contract.id(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error processing unit {} for expired contracts: {}", unit.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Created {} automatic inspections for expired contracts ending in month: {}", createdCount, endOfMonth);
        return createdCount;
    }
}

