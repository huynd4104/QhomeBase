package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.client.BaseServiceClient;
import com.QhomeBase.financebillingservice.constants.ServiceCode;
import com.QhomeBase.financebillingservice.dto.CreateInvoiceLineRequest;
import com.QhomeBase.financebillingservice.dto.CreateInvoiceRequest;
import com.QhomeBase.financebillingservice.dto.ImportedReadingDto;
import com.QhomeBase.financebillingservice.dto.InvoiceDto;
import com.QhomeBase.financebillingservice.dto.MeterReadingImportResponse;
import com.QhomeBase.financebillingservice.dto.ReadingCycleDto;
import com.QhomeBase.financebillingservice.model.BillingCycle;
import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.model.PricingTier;
import com.QhomeBase.financebillingservice.repository.BillingCycleRepository;
import com.QhomeBase.financebillingservice.repository.InvoiceRepository;
import com.QhomeBase.financebillingservice.repository.PricingTierRepository;
import com.QhomeBase.financebillingservice.repository.ServicePricingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeterReadingImportService {

    private final ServicePricingRepository pricingRepository;
    private final PricingTierRepository pricingTierRepository;
    private final InvoiceService invoiceService;
    private final BillingCycleRepository billingCycleRepository;
    private final InvoiceRepository invoiceRepository;
    private final BaseServiceClient baseServiceClient;
    private final NotificationClient notificationClient;
    public int importReadings(List<ImportedReadingDto> readings) {
        MeterReadingImportResponse response = importReadingsWithResponse(readings);
        return response.getInvoicesCreated();
    }

    @Transactional
    public MeterReadingImportResponse importReadingsWithResponse(List<ImportedReadingDto> readings) {
        if (readings == null || readings.isEmpty()) {
            return MeterReadingImportResponse.builder()
                    .totalReadings(0)
                    .invoicesCreated(0)
                    .invoiceIds(Collections.emptyList())
                    .message("No readings to import")
                    .build();
        }

        // Group by unitId + cycleId + serviceCode to create separate invoices for WATER and ELECTRIC
        Map<String, List<ImportedReadingDto>> grouped = readings.stream()
                .collect(Collectors.groupingBy(r -> key(r.getUnitId(), r.getCycleId(), r.getServiceCode())));

        Map<UUID, ReadingCycleDto> cycleCache = new HashMap<>();
        
        int created = 0;
        int skipped = 0;
        List<UUID> invoiceIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (Map.Entry<String, List<ImportedReadingDto>> entry : grouped.entrySet()) {
            List<ImportedReadingDto> group = entry.getValue();
            ImportedReadingDto head = group.get(0);

            UUID unitId = head.getUnitId();
            UUID residentId = head.getResidentId();
            UUID readingCycleId = head.getCycleId(); 

            try {
                ReadingCycleDto readingCycle = cycleCache.computeIfAbsent(readingCycleId, cycleId -> {
                    try {
                        ReadingCycleDto cycle = baseServiceClient.getReadingCycleById(cycleId);
                        if (cycle == null) {
                            log.error("Reading cycle not found: {}", cycleId);
                            throw new IllegalStateException("Reading cycle not found: " + cycleId);
                        }
                        return cycle;
                    } catch (Exception e) {
                        log.error("Error fetching reading cycle {}: {}", cycleId, e.getMessage());
                        throw new RuntimeException("Failed to fetch reading cycle: " + e.getMessage(), e);
                    }
                });

                // Allow creating invoices for any cycle status
                // Removed status validation to allow invoices from OPEN cycles (e.g., from asset inspections)
                String cycleStatus = readingCycle.status();
                log.debug("Processing invoice for unit={}, cycle={}, status={}", 
                        unitId, readingCycleId, cycleStatus);
            } catch (Exception e) {
                String errorMsg = String.format("Unit %s, Cycle %s: %s", unitId, readingCycleId, e.getMessage());
                log.error("Error processing unit={}, cycle={}: {}", unitId, readingCycleId, e.getMessage());
                errors.add(errorMsg);
                skipped++;
                continue;
            }

            ReadingCycleDto readingCycle = cycleCache.get(readingCycleId);
            if (readingCycle == null) {
                String errorMsg = String.format("Unit %s, Cycle %s: Reading cycle not found in cache", unitId, readingCycleId);
                log.error("Reading cycle {} not found in cache for unit={}", readingCycleId, unitId);
                errors.add(errorMsg);
                skipped++;
                continue;
            }

            try {
                BigDecimal totalUsage = group.stream()
                        .map(ImportedReadingDto::getUsageKwh)
                        .filter(Objects::nonNull)
                        .filter(usage -> usage.compareTo(BigDecimal.ZERO) > 0)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (totalUsage.compareTo(BigDecimal.ZERO) == 0) {
                    String errorMsg = String.format("Unit %s, Cycle %s: Total usage is 0", unitId, readingCycleId);
                    log.warn("Total usage is 0 for unit={}, cycle={}. Readings: {}", 
                            unitId, readingCycleId, group.stream()
                                    .map(r -> String.format("usageKwh=%s", r.getUsageKwh()))
                                    .collect(Collectors.joining(", ")));
                    errors.add(errorMsg);
                    skipped++;
                    continue;
                }

                LocalDate serviceDate = group.stream()
                        .map(ImportedReadingDto::getReadingDate)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(LocalDate.now());

                String rawServiceCode = head.getServiceCode();
                String serviceCode = normalizeServiceCode(rawServiceCode);
                
                if (!isServiceCodeValid(serviceCode)) {
                    log.warn("Invalid or unknown service code: {} (normalized from: {}), defaulting to ELECTRIC", 
                            serviceCode, rawServiceCode);
                    serviceCode = ServiceCode.ELECTRIC;
                }
                
                // Build description - add inspection marker if cycle is OPEN (from asset inspection)
                String baseDescription = Optional.ofNullable(head.getDescription())
                        .orElse(getDefaultDescription(serviceCode));
                
                String description = baseDescription;
                String inspectionMarker = "Đo cùng với kiểm tra thiết bị";
                
                // If cycle status is OPEN, this is likely from asset inspection - add marker
                String cycleStatus = readingCycle.status();
                if (cycleStatus != null && "OPEN".equalsIgnoreCase(cycleStatus)) {
                    if (!description.contains(inspectionMarker)) {
                        description = description + " - " + inspectionMarker;
                        log.info("Added inspection marker to description for OPEN cycle {}: {}", readingCycleId, description);
                    }
                }

                UUID billingCycleId = findOrCreateBillingCycle(readingCycleId, serviceDate);

                // Check for existing invoice with same unit, cycle, and serviceCode
                // Since we now create separate invoices for WATER and ELECTRIC, we need to check by serviceCode too
                // Use the repository method that finds invoices by serviceCode and cycleId
                List<Invoice> existingInvoicesForService = invoiceRepository.findByServiceCodeAndAndCycle(billingCycleId, serviceCode);
                Invoice existingInvoiceForService = existingInvoicesForService.stream()
                        .filter(inv -> unitId.equals(inv.getPayerUnitId()))
                        .findFirst()
                        .orElse(null);
                
                if (existingInvoiceForService != null) {
                    log.warn("Invoice already exists for unit={}, cycle={}, serviceCode={}. Invoice ID: {}. Skipping creation.", 
                            unitId, billingCycleId, serviceCode, existingInvoiceForService.getId());
                    invoiceIds.add(existingInvoiceForService.getId());
                    continue;
                }

                List<CreateInvoiceLineRequest> invoiceLines = calculateInvoiceLines(
                        serviceCode, totalUsage, serviceDate, description);
                
                if (invoiceLines.isEmpty()) {
                    String errorMsg = String.format("Unit %s, Cycle %s: No invoice lines calculated (serviceCode=%s, totalUsage=%s)", 
                            unitId, readingCycleId, serviceCode, totalUsage);
                    log.warn("No invoice lines calculated for unit={}, cycle={}, serviceCode={}, totalUsage={}. Skipping invoice creation.", 
                            unitId, readingCycleId, serviceCode, totalUsage);
                    errors.add(errorMsg);
                    skipped++;
                    continue;
                }
                
                boolean hasValidPrice = invoiceLines.stream()
                        .anyMatch(line -> line.getUnitPrice() != null && line.getUnitPrice().compareTo(BigDecimal.ZERO) > 0);
                
                if (!hasValidPrice) {
                    String errorMsg = String.format("Unit %s, Cycle %s: No valid pricing found (serviceCode=%s, totalUsage=%s)", 
                            unitId, readingCycleId, serviceCode, totalUsage);
                    log.warn("No valid pricing found for unit={}, cycle={}, serviceCode={}, totalUsage={}. Invoice lines: {}. Skipping invoice creation.", 
                            unitId, readingCycleId, serviceCode, totalUsage, 
                            invoiceLines.stream()
                                    .map(line -> String.format("quantity=%s, unitPrice=%s", line.getQuantity(), line.getUnitPrice()))
                                    .collect(Collectors.joining(", ")));
                    errors.add(errorMsg);
                    skipped++;
                    continue;
                }

                LocalDate dueDate = calculateDueDate(readingCycle.periodTo());
                
                // Get billToName from primary resident or unit code
                String billToName = getBillToName(unitId, residentId);
                
               
                com.QhomeBase.financebillingservice.model.InvoiceStatus invoiceStatus = 
                        "OPEN".equalsIgnoreCase(readingCycle.status()) 
                        ? com.QhomeBase.financebillingservice.model.InvoiceStatus.PAID 
                        : null; // null will default to PUBLISHED in InvoiceService
                
                CreateInvoiceRequest req = CreateInvoiceRequest.builder()
                        .payerUnitId(unitId)
                        .payerResidentId(residentId)
                        .cycleId(billingCycleId)
                        .currency("VND")
                        .dueDate(dueDate)
                        .billToName(billToName)
                        .status(invoiceStatus)
                        .lines(invoiceLines)
                        .build();

                InvoiceDto invoice = invoiceService.createInvoice(req);
                invoiceIds.add(invoice.getId());
                created++;
                
                log.info("Created invoice {} for unit={}, readingCycle={}, billingCycle={} with usage={} kWh ({} tiers)",
                        invoice.getId(), unitId, readingCycleId, billingCycleId, totalUsage, invoiceLines.size());
                
                // Send notification to resident about new invoice
                try {
                    sendInvoiceNotification(residentId, unitId, invoice, serviceCode, totalUsage);
                } catch (Exception e) {
                    log.warn("Failed to send notification for invoice {}: {}", invoice.getId(), e.getMessage());
                    // Don't fail the import if notification fails
                }
            } catch (Exception e) {
                String errorMsg = String.format("Unit %s, Cycle %s: %s", unitId, readingCycleId, e.getMessage());
                log.error("Error creating invoice for unit={}, cycle={}: {}", unitId, readingCycleId, e.getMessage(), e);
                errors.add(errorMsg);
                skipped++;
            }
        }

        String message;
        if (created > 0 && skipped == 0) {
            message = String.format("Successfully imported %d readings and created %d invoices", 
                    readings.size(), created);
        } else if (created > 0 && skipped > 0) {
            message = String.format("Imported %d readings: created %d invoices, skipped %d units", 
                    readings.size(), created, skipped);
        } else if (skipped > 0) {
            message = String.format("Failed to create invoices for %d units. See errors for details.", skipped);
        } else {
            message = "No invoices created";
        }

        return MeterReadingImportResponse.builder()
                .totalReadings(readings.size())
                .invoicesCreated(created)
                .invoicesSkipped(skipped)
                .invoiceIds(invoiceIds)
                .errors(errors.isEmpty() ? null : errors)
                .message(message)
                .build();
    }

    private String key(UUID unitId, UUID cycleId, String serviceCode) {
        String normalizedServiceCode = serviceCode != null ? normalizeServiceCode(serviceCode) : "UNKNOWN";
        return unitId + "|" + cycleId + "|" + normalizedServiceCode;
    }

    private List<CreateInvoiceLineRequest> calculateInvoiceLines(
            String serviceCode, BigDecimal totalUsage, LocalDate serviceDate, String baseDescription) {
        
        List<PricingTier> tiers = pricingTierRepository.findActiveTiersByServiceAndDate(serviceCode, serviceDate);
        
        if (tiers.isEmpty()) {
            BigDecimal unitPrice = resolveUnitPrice(serviceCode, serviceDate);
            CreateInvoiceLineRequest line = CreateInvoiceLineRequest.builder()
                    .serviceDate(serviceDate)
                    .description(baseDescription)
                    .quantity(totalUsage)
                    .unit("kWh")
                    .unitPrice(unitPrice)
                    .taxRate(BigDecimal.ZERO)
                    .serviceCode(serviceCode)
                    .externalRefType("METER_READING_GROUP")
                    .externalRefId(null)
                    .build();
            return Collections.singletonList(line);
        }
        
        List<CreateInvoiceLineRequest> lines = new ArrayList<>();
        BigDecimal previousMax = BigDecimal.ZERO;
        
        for (PricingTier tier : tiers) {
            if (previousMax.compareTo(totalUsage) >= 0) {
                break;
            }
            
            BigDecimal tierEffectiveMax;
            if (tier.getMaxQuantity() == null) {
                tierEffectiveMax = totalUsage;
            } else {
                tierEffectiveMax = totalUsage.min(tier.getMaxQuantity());
            }
            
            BigDecimal applicableQuantity = tierEffectiveMax.subtract(previousMax).max(BigDecimal.ZERO);
            
            if (applicableQuantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal tierAmount = applicableQuantity.multiply(tier.getUnitPrice());
                
                String maxQtyStr = tier.getMaxQuantity() != null ? tier.getMaxQuantity().toString() : "∞";
                String tierDescription = String.format("%s (Bậc %d: %s-%s kWh)",
                        baseDescription,
                        tier.getTierOrder(),
                        tier.getMinQuantity(),
                        maxQtyStr);
                
                // Ensure unitPrice is from tier, not calculated amount
                BigDecimal unitPrice = tier.getUnitPrice();
                
                CreateInvoiceLineRequest line = CreateInvoiceLineRequest.builder()
                        .serviceDate(serviceDate)
                        .description(tierDescription)
                        .quantity(applicableQuantity)
                        .unit("kWh")
                        .unitPrice(unitPrice) // Use tier.getUnitPrice() directly, not tierAmount
                        .taxRate(BigDecimal.ZERO)
                        .serviceCode(serviceCode)
                        .externalRefType("METER_READING_GROUP")
                        .externalRefId(null)
                        .build();
                
                lines.add(line);
                previousMax = tierEffectiveMax;
                
                log.info("✅ [MeterReadingImportService] Created invoice line - Tier {}: quantity={} kWh, unitPrice={} VND/kWh, lineTotal={} VND", 
                        tier.getTierOrder(), applicableQuantity, unitPrice, tierAmount);
            }
        }
        
        if (lines.isEmpty()) {
            log.warn("No tiers matched for usage {} kWh, using simple pricing", totalUsage);
            BigDecimal unitPrice = resolveUnitPrice(serviceCode, serviceDate);
            CreateInvoiceLineRequest line = CreateInvoiceLineRequest.builder()
                    .serviceDate(serviceDate)
                    .description(baseDescription)
                    .quantity(totalUsage)
                    .unit("kWh")
                    .unitPrice(unitPrice)
                    .taxRate(BigDecimal.ZERO)
                    .serviceCode(serviceCode)
                    .externalRefType("METER_READING_GROUP")
                    .externalRefId(null)
                    .build();
            return Collections.singletonList(line);
        }
        
        return lines;
    }

    private String normalizeServiceCode(String rawServiceCode) {
        if (rawServiceCode == null || rawServiceCode.trim().isEmpty()) {
            return ServiceCode.ELECTRIC;
        }
        return ServiceCode.normalize(rawServiceCode);
    }
    
    private boolean isServiceCodeValid(String serviceCode) {
        return ServiceCode.isValid(serviceCode);
    }
    
    private LocalDate calculateDueDate(LocalDate readingCycleEndDate) {
        if (readingCycleEndDate == null) {
            log.warn("Reading cycle end date is null, using current date + 7 days as fallback");
            return LocalDate.now().plusDays(7);
        }
        return readingCycleEndDate.plusDays(7);
    }

    private String getDefaultDescription(String serviceCode) {
        if (ServiceCode.ELECTRIC.equals(serviceCode)) {
            return "Tiền điện";
        }
        if (ServiceCode.WATER.equals(serviceCode)) {
            return "Tiền nước";
        }
        return "Tiền dịch vụ";
    }
    
    private BigDecimal resolveUnitPrice(String serviceCode, LocalDate date) {
        return pricingRepository.findActivePriceGlobal(serviceCode, date)
                .map(sp -> sp.getBasePrice())
                .orElse(BigDecimal.ZERO);
    }


    private UUID findOrCreateBillingCycle(UUID readingCycleId, LocalDate serviceDate) {
        if (readingCycleId != null) {
            List<BillingCycle> linkedCycles = billingCycleRepository.findByExternalCycleId(readingCycleId);
            if (!linkedCycles.isEmpty()) {
                BillingCycle linked = linkedCycles.get(0);
                log.debug("Reusing billing cycle {} already linked to reading cycle {}", linked.getId(), readingCycleId);
                return linked.getId();
            }
        }

        LocalDate periodFrom = serviceDate.withDayOfMonth(1);
        LocalDate periodTo = periodFrom.withDayOfMonth(24);
        
        List<BillingCycle> existing = billingCycleRepository.findListByTime(periodFrom, periodTo);
        for (BillingCycle cycle : existing) {
            if (readingCycleId != null) {
                if (readingCycleId.equals(cycle.getExternalCycleId())) {
                    log.debug("Found billing cycle {} already linked to reading cycle {}", cycle.getId(), readingCycleId);
                    return cycle.getId();
                }
                if (cycle.getExternalCycleId() == null) {
                    cycle.setExternalCycleId(readingCycleId);
                    BillingCycle saved = billingCycleRepository.save(cycle);
                    log.debug("Linked existing billing cycle {} to reading cycle {}", saved.getId(), readingCycleId);
                    return saved.getId();
                }
            } else {
                log.debug("Reusing existing billing cycle {} for period {} - {} with no external link",
                        cycle.getId(), periodFrom, periodTo);
                return cycle.getId();
            }
        }
        
        BillingCycle newCycle = BillingCycle.builder()
                .name(String.format("Cycle %s (%s)", 
                        periodFrom.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                        readingCycleId != null ? readingCycleId.toString().substring(0, 8) : "AUTO"))
                .periodFrom(periodFrom)
                .periodTo(periodTo)
                .status("ACTIVE")
                .externalCycleId(readingCycleId)
                .build();
        
        BillingCycle saved = billingCycleRepository.save(newCycle);
        log.warn("Created new BillingCycle {} for readingCycleId {} (period {} to {})",
                saved.getId(), readingCycleId, periodFrom, periodTo);
        
        return saved.getId();
    }

    private void sendInvoiceNotification(UUID residentId, UUID unitId, InvoiceDto invoice, 
                                         String serviceCode, BigDecimal totalUsage) {
        if (residentId == null) {
            log.debug("Cannot send notification: residentId is null for invoice {} (unit={})", 
                    invoice.getId(), unitId);
            return;
        }

        try {
            // Get unit info to get buildingId
            BaseServiceClient.UnitInfo unitInfo = baseServiceClient.getUnitById(unitId);
            UUID buildingId = unitInfo != null ? unitInfo.getBuildingId() : null;

            // Determine notification type based on service code
            String notificationType = "BILL";
            String serviceName = "dịch vụ";
            if (ServiceCode.ELECTRIC.equals(serviceCode)) {
                notificationType = "ELECTRICITY";
                serviceName = "điện";
            } else if (ServiceCode.WATER.equals(serviceCode)) {
                notificationType = "WATER";
                serviceName = "nước";
            }

            // Format total amount
            BigDecimal totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
            String amountStr = String.format("%,.0f", totalAmount.doubleValue());
            
            // Create notification title and message
            String title = String.format("Hóa đơn %s mới", serviceName);
            String message = String.format("Hóa đơn %s mới đã được tạo với số tiền %s VND. Số lượng sử dụng: %s kWh. Vui lòng thanh toán trước ngày %s.",
                    serviceName, amountStr, totalUsage.toString(), 
                    invoice.getDueDate() != null ? invoice.getDueDate().toString() : "hết hạn");

            // Prepare data payload
            Map<String, String> data = new HashMap<>();
            data.put("invoiceId", invoice.getId().toString());
            data.put("amount", totalAmount.toString());
            data.put("serviceCode", serviceCode);
            data.put("usage", totalUsage.toString());

            // Send notification
            notificationClient.sendResidentNotification(
                    residentId,
                    buildingId,
                    notificationType,
                    title,
                    message,
                    invoice.getId(),
                    "INVOICE",
                    data
            );

            log.info("✅ Sent notification to resident {} for invoice {} ({} - {} kWh)",
                    residentId, invoice.getId(), serviceName, totalUsage);
        } catch (Exception e) {
            log.error("❌ Error sending notification for invoice {}: {}", invoice.getId(), e.getMessage(), e);
            // Don't throw - notification failure shouldn't break import
        }
    }

    /**
     * Get billToName from primary resident or unit code
     * Format: "Căn hộ {unitCode}" to match asset inspection invoices
     */
    private String getBillToName(UUID unitId, UUID residentId) {
        try {
            // Use unit code format to match asset inspection invoices: "Căn hộ {unitCode}"
            BaseServiceClient.UnitInfo unitInfo = baseServiceClient.getUnitById(unitId);
            if (unitInfo != null && unitInfo.getCode() != null) {
                return String.format("Căn hộ %s", unitInfo.getCode());
            }
            
            // Fallback
            return "Căn hộ";
        } catch (Exception e) {
            log.warn("Error getting billToName for unit {}: {}", unitId, e.getMessage());
            // Fallback to unit code or default
            try {
                BaseServiceClient.UnitInfo unitInfo = baseServiceClient.getUnitById(unitId);
                if (unitInfo != null && unitInfo.getCode() != null) {
                    return String.format("Căn hộ %s", unitInfo.getCode());
                }
            } catch (Exception ex) {
                log.warn("Error getting unit info for billToName: {}", ex.getMessage());
            }
            return "Căn hộ";
        }
    }
}


