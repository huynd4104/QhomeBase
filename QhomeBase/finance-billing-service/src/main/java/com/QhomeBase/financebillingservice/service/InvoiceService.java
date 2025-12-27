package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.client.BaseServiceClient;
import com.QhomeBase.financebillingservice.config.VnpayProperties;
import com.QhomeBase.financebillingservice.dto.*;
import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.model.InvoiceLine;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import com.QhomeBase.financebillingservice.repository.InvoiceLineRepository;
import com.QhomeBase.financebillingservice.repository.InvoiceRepository;
import com.QhomeBase.financebillingservice.repository.ResidentRepository;
import com.QhomeBase.financebillingservice.repository.ResidentRepository.ResidentContact;
import com.QhomeBase.financebillingservice.service.vnpay.VnpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"NullAway", "null"})
public class InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final ResidentRepository residentRepository;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final NotificationEmailService emailService;
    private final NotificationClient notificationClient;
    private final BaseServiceClient baseServiceClient;

    private final ConcurrentMap<Long, UUID> orderIdToInvoiceIdMap = new ConcurrentHashMap<>();

    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "ELECTRICITY", "Điện",
            "WATER", "Nước",
            "INTERNET", "Internet",
            "ELEVATOR", "Vé thang máy",
            "PARKING", "Vé gửi xe",
            "CONTRACT_RENEWAL", "Gia hạn hợp đồng",
            "OTHER", "Khác"
    );

    private static final List<String> CATEGORY_ORDER = List.of(
            "ELECTRICITY",
            "WATER",
            "INTERNET",
            "ELEVATOR",
            "PARKING",
            "CONTRACT_RENEWAL",
            "OTHER"
    );
    
    public List<InvoiceDto> getInvoicesByResident(UUID residentId) {
        // Lấy invoice theo cả payerResidentId VÀ payerUnitId của resident đó
        // Để tất cả thành viên trong cùng căn hộ có thể xem invoice của căn hộ đó
        // Tìm unitId từ residentId thông qua household
        UUID unitId = baseServiceClient.getUnitIdFromResidentId(residentId);
        
        List<Invoice> invoices;
        if (unitId != null) {
            // Lấy invoice theo cả payerResidentId VÀ payerUnitId
            invoices = invoiceRepository.findByPayerResidentIdOrPayerUnitId(residentId, unitId);
        } else {
            // Fallback: chỉ lấy theo payerResidentId nếu không tìm được unitId
            invoices = invoiceRepository.findByPayerResidentId(residentId);
        }
        
        return invoices.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public List<InvoiceDto> getInvoicesByResidentAndStatus(UUID residentId, InvoiceStatus status) {
        // Lấy invoice theo cả payerResidentId VÀ payerUnitId của resident đó với status
        // Để tất cả thành viên trong cùng căn hộ có thể xem invoice của căn hộ đó
        UUID unitId = baseServiceClient.getUnitIdFromResidentId(residentId);
        
        List<Invoice> invoices;
        if (unitId != null) {
            // Lấy invoice theo cả payerResidentId VÀ payerUnitId với status
            invoices = invoiceRepository.findByPayerResidentIdOrPayerUnitIdAndStatus(residentId, unitId, status);
        } else {
            // Fallback: chỉ lấy theo payerResidentId nếu không tìm được unitId
            invoices = invoiceRepository.findByPayerResidentIdAndStatus(residentId, status);
        }
        
        return invoices.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public InvoiceDto getInvoiceById(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        
        // Auto-fix: If invoice is PAID but paidAt is null, set it to issuedAt (or current time if issuedAt is also null)
        // This handles invoices created before the paidAt logic was added
        if (invoice.getStatus() == InvoiceStatus.PAID && invoice.getPaidAt() == null) {
            log.warn("⚠️ Invoice {} is PAID but paidAt is null, setting it to issuedAt", invoiceId);
            OffsetDateTime paidAt = invoice.getIssuedAt() != null ? invoice.getIssuedAt() : OffsetDateTime.now();
            invoice.setPaidAt(paidAt);
            invoice = invoiceRepository.save(invoice);
            log.info("✅ Fixed paidAt for invoice {}: {}", invoiceId, invoice.getPaidAt());
        }
        
        return toDto(invoice);
    }
    
    public List<InvoiceDto> getInvoicesByUnit(UUID unitId) {
        List<Invoice> invoices = invoiceRepository.findByPayerUnitId(unitId);
        return invoices.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public List<InvoiceDto> getInvoicesByServiceCode(String serviceCode) {
        List<InvoiceLine> lines = invoiceLineRepository.findByServiceCode(serviceCode);
        List<UUID> invoiceIds = lines.stream()
                .map(InvoiceLine::getInvoiceId)
                .distinct()
                .collect(Collectors.toList());
        
        List<Invoice> invoices = invoiceRepository.findAllById(invoiceIds);
        return invoices.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public List<InvoiceDto> getInvoicesByResidentAndServiceCode(UUID residentId, String serviceCode) {
        // Lấy invoice theo cả payerResidentId VÀ payerUnitId của resident đó
        // Để tất cả thành viên trong cùng căn hộ có thể xem invoice của căn hộ đó
        UUID unitId = baseServiceClient.getUnitIdFromResidentId(residentId);
        
        List<Invoice> allInvoices;
        if (unitId != null) {
            // Lấy invoice theo cả payerResidentId VÀ payerUnitId
            allInvoices = invoiceRepository.findByPayerResidentIdOrPayerUnitId(residentId, unitId);
        } else {
            // Fallback: chỉ lấy theo payerResidentId nếu không tìm được unitId
            allInvoices = invoiceRepository.findByPayerResidentId(residentId);
        }
        
        return allInvoices.stream()
                .filter(invoice -> {
                    List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceIdAndServiceCode(
                            invoice.getId(), serviceCode);
                    return !lines.isEmpty();
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public List<InvoiceDto> getAllInvoicesForAdmin(
            String serviceCode,
            String status,
            UUID unitId,
            UUID buildingId,
            String startDate,
            String endDate) {
        List<Invoice> allInvoices = invoiceRepository.findAll();
        
        return allInvoices.stream()
                .filter(invoice -> {
                    // Filter by serviceCode
                    if (serviceCode != null && !serviceCode.isBlank()) {
                        List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
                        boolean hasServiceCode = lines.stream()
                                .anyMatch(line -> serviceCode.equalsIgnoreCase(line.getServiceCode()));
                        if (!hasServiceCode) {
                            return false;
                        }
                    }
                    
                    // Filter by status
                    if (status != null && !status.isBlank()) {
                        try {
                            InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(status.toUpperCase());
                            if (invoice.getStatus() != invoiceStatus) {
                                return false;
                            }
                        } catch (IllegalArgumentException e) {
                            // Invalid status, skip filter
                        }
                    }
                    
                    // Filter by unitId
                    if (unitId != null && !invoice.getPayerUnitId().equals(unitId)) {
                        return false;
                    }
                    
                    // Filter by buildingId (need to check via unit)
                    if (buildingId != null) {
                        // This would require a join with units table, simplified for now
                        // You may need to add a repository method for this
                    }
                    
                    // Filter by date range
                    if (startDate != null && !startDate.isBlank()) {
                        try {
                            LocalDate start = LocalDate.parse(startDate);
                            if (invoice.getIssuedAt() != null && invoice.getIssuedAt().toLocalDate().isBefore(start)) {
                                return false;
                            }
                        } catch (Exception e) {
                            // Invalid date format, skip filter
                        }
                    }
                    
                    if (endDate != null && !endDate.isBlank()) {
                        try {
                            LocalDate end = LocalDate.parse(endDate);
                            if (invoice.getIssuedAt() != null && invoice.getIssuedAt().toLocalDate().isAfter(end)) {
                                return false;
                            }
                        } catch (Exception e) {
                            // Invalid date format, skip filter
                        }
                    }
                    
                    return true;
                })
                .map(this::toDto)
                .sorted((a, b) -> {
                    // Sort by issuedAt descending (newest first)
                    if (a.getIssuedAt() == null && b.getIssuedAt() == null) return 0;
                    if (a.getIssuedAt() == null) return 1;
                    if (b.getIssuedAt() == null) return -1;
                    return b.getIssuedAt().compareTo(a.getIssuedAt());
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public InvoiceDto recordVehicleRegistrationPayment(VehicleRegistrationPaymentRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("Missing userId");
        }

        UUID residentId = residentRepository.findResidentIdByUserId(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + request.getUserId()));

        BigDecimal amount = Optional.ofNullable(request.getAmount())
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .orElse(BigDecimal.valueOf(30000));

        // Use paymentDate from request if available (from callback), otherwise use current time
        OffsetDateTime payDate = Optional.ofNullable(request.getPaymentDate())
                .orElse(OffsetDateTime.now());
        LocalDate serviceDate = payDate.toLocalDate();

        String description = buildVehicleRegistrationDescription(request);
        
       
        String billToName = "Căn hộ";
        if (request.getUnitId() != null) {
            try {
                BaseServiceClient.UnitInfo unitInfo = baseServiceClient.getUnitById(request.getUnitId());
                if (unitInfo != null && unitInfo.getCode() != null) {
                    billToName = String.format("Căn hộ %s", unitInfo.getCode());
                }
            } catch (Exception e) {
                log.warn("Failed to get unit code for billToName: {}", e.getMessage());
            }
        }

        CreateInvoiceRequest createRequest = CreateInvoiceRequest.builder()
                .dueDate(serviceDate)
                .currency("VND")
                .billToName(billToName)
                .payerUnitId(request.getUnitId())
                .payerResidentId(residentId)
                .lines(List.of(CreateInvoiceLineRequest.builder()
                        .serviceDate(serviceDate)
                        .description(description)
                        .quantity(BigDecimal.ONE)
                        .unit("lần")
                        .unitPrice(amount)
                        .taxRate(BigDecimal.ZERO)
                        .serviceCode("VEHICLE_CARD")
                        .externalRefType("VEHICLE_REGISTRATION")
                        .externalRefId(request.getRegistrationId())
                        .build()))
                .build();

        InvoiceDto created = createInvoice(createRequest);

        Invoice invoice = invoiceRepository.findById(created.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + created.getId()));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentGateway("VNPAY");
        invoice.setPaidAt(payDate);
        invoice.setVnpTransactionRef(request.getTransactionRef());
        invoice.setVnpTransactionNo(request.getTransactionNo());
        invoice.setVnpBankCode(request.getBankCode());
        invoice.setVnpCardType(request.getCardType());
        invoice.setVnpResponseCode(request.getResponseCode());
        invoiceRepository.save(invoice);

        Map<String, String> params = new HashMap<>();
        if (request.getTransactionRef() != null) {
            params.put("vnp_TxnRef", request.getTransactionRef());
        }
        notifyPaymentSuccess(invoice, params);

        return toDto(invoice);
    }
    
    @Transactional
    public InvoiceDto createInvoice(CreateInvoiceRequest request) {
        log.info("Creating invoice for unit: {}", request.getPayerUnitId());

        String invoiceCode = generateInvoiceCode();

        InvoiceStatus invoiceStatus = request.getStatus() != null ? request.getStatus() : InvoiceStatus.PUBLISHED;
        OffsetDateTime now = OffsetDateTime.now();

        Invoice invoice = Invoice.builder()
                .code(invoiceCode)
                .issuedAt(now)
                .dueDate(request.getDueDate())
                .status(invoiceStatus)
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .billToName(request.getBillToName())
                .billToAddress(request.getBillToAddress())
                .billToContact(request.getBillToContact())
                .payerUnitId(request.getPayerUnitId())
                .payerResidentId(request.getPayerResidentId())
                .cycleId(request.getCycleId())
                .build();
        
        // Set paidAt after building if status is PAID
        if (invoiceStatus == InvoiceStatus.PAID) {
            invoice.setPaidAt(now);
            log.info("Setting paidAt to current time for PAID invoice: {}", now);
        }
        
        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice created with ID: {}, code: {}, status: {}, paidAt: {}", 
                savedInvoice.getId(), savedInvoice.getCode(), savedInvoice.getStatus(), savedInvoice.getPaidAt());
        
        // Reload invoice to ensure paidAt is persisted correctly
        savedInvoice = invoiceRepository.findById(savedInvoice.getId())
                .orElseThrow(() -> new IllegalStateException("Invoice not found after creation"));
        log.info("Invoice reloaded - paidAt: {}", savedInvoice.getPaidAt());
        
        // If paidAt is still null after reload and status is PAID, set it explicitly
        if (savedInvoice.getStatus() == InvoiceStatus.PAID && savedInvoice.getPaidAt() == null) {
            log.warn("⚠️ paidAt is null for PAID invoice {}, setting it now", savedInvoice.getId());
            savedInvoice.setPaidAt(now);
            savedInvoice = invoiceRepository.save(savedInvoice);
            log.info("✅ Updated paidAt for invoice {}: {}", savedInvoice.getId(), savedInvoice.getPaidAt());
        }
        
        if (request.getLines() != null && !request.getLines().isEmpty()) {
            for (CreateInvoiceLineRequest lineRequest : request.getLines()) {
                BigDecimal taxAmount = calculateTaxAmount(
                        lineRequest.getQuantity(),
                        lineRequest.getUnitPrice(),
                        lineRequest.getTaxRate()
                );
                
                // Validate unitPrice is reasonable (not accidentally set to lineTotal)
                BigDecimal unitPrice = lineRequest.getUnitPrice();
                BigDecimal quantity = lineRequest.getQuantity();
                BigDecimal calculatedLineTotal = quantity.multiply(unitPrice);
                
                // Validate unitPrice for electricity/water - should be reasonable (typically < 100,000 VND/kWh)
                if (lineRequest.getServiceCode() != null && 
                    (lineRequest.getServiceCode().contains("ELECTRIC") || 
                     lineRequest.getServiceCode().contains("WATER"))) {
                    
                    // Check if unitPrice seems too high (might be accidentally set to lineTotal)
                    BigDecimal maxReasonableUnitPrice = new BigDecimal("100000"); // 100,000 VND/kWh
                    if (unitPrice.compareTo(maxReasonableUnitPrice) > 0) {
                        log.error("⚠️ [InvoiceService] SUSPICIOUS unitPrice detected - serviceCode={}, quantity={}, unitPrice={}, calculatedLineTotal={}, description={}. " +
                                "UnitPrice seems too high - might be accidentally set to lineTotal instead of unitPrice!", 
                                lineRequest.getServiceCode(), quantity, unitPrice, calculatedLineTotal, 
                                lineRequest.getDescription());
                    }
                    
                    log.info("💡 [InvoiceService] Creating invoice line - serviceCode={}, quantity={}, unitPrice={}, calculatedLineTotal={}, description={}", 
                            lineRequest.getServiceCode(), quantity, unitPrice, calculatedLineTotal, 
                            lineRequest.getDescription());
                }
                
                InvoiceLine line = InvoiceLine.builder()
                        .invoiceId(savedInvoice.getId())
                        .serviceDate(lineRequest.getServiceDate())
                        .description(lineRequest.getDescription())
                        .quantity(quantity)
                        .unit(lineRequest.getUnit())
                        .unitPrice(unitPrice) // Ensure we use the unitPrice from request, not calculated total
                        .taxRate(lineRequest.getTaxRate() != null ? lineRequest.getTaxRate() : BigDecimal.ZERO)
                        .taxAmount(taxAmount)
                        .serviceCode(lineRequest.getServiceCode())
                        .externalRefType(lineRequest.getExternalRefType())
                        .externalRefId(lineRequest.getExternalRefId())
                        .build();
                
                invoiceLineRepository.save(line);
                
                // Verify saved data
                BigDecimal savedUnitPrice = line.getUnitPrice();
                BigDecimal savedQuantity = line.getQuantity();
                BigDecimal actualLineTotal = savedQuantity.multiply(savedUnitPrice);
                log.debug("✅ [InvoiceService] Saved invoice line - unitPrice={}, quantity={}, lineTotal={}", 
                        savedUnitPrice, savedQuantity, actualLineTotal);
            }
            log.info("Created {} invoice lines for invoice: {}", request.getLines().size(), savedInvoice.getId());
        }
        
        // Send notification to resident (only for electricity and water invoices)
        sendInvoiceNotification(savedInvoice);
        
        return toDto(savedInvoice);
    }
    
    private void sendInvoiceNotification(Invoice invoice) {
        if (invoice.getPayerUnitId() == null) {
            log.warn(" [InvoiceService] Cannot send notification: payerUnitId is null");
            return;
        }
        
        // Only send notification for electricity and water invoices
        // Skip notification for card payment invoices (VEHICLE_CARD, ELEVATOR_CARD, RESIDENT_CARD)
        List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
        boolean shouldSendNotification = false;
        
        for (InvoiceLine line : lines) {
            String serviceCode = line.getServiceCode();
            if (serviceCode != null) {
                String normalized = serviceCode.trim().toUpperCase();
                // Only send notification for electricity and water invoices
                if (normalized.contains("ELECTRICITY") || normalized.contains("WATER")) {
                    shouldSendNotification = true;
                    break;
                }
            }
        }
        
        if (!shouldSendNotification) {
            log.debug(" [InvoiceService] Skipping notification for invoice {} - not an electricity/water invoice", invoice.getId());
            return;
        }
        
        try {
            // Get buildingId from unitId
            UUID buildingId = null;
            BaseServiceClient.UnitInfo unitInfo = null;
            if (invoice.getPayerUnitId() != null) {
                try {
                    unitInfo = baseServiceClient.getUnitById(invoice.getPayerUnitId());
                    if (unitInfo != null && unitInfo.getBuildingId() != null) {
                        buildingId = unitInfo.getBuildingId();
                        log.info(" [InvoiceService] Resolved buildingId={} from unitId={}", buildingId, invoice.getPayerUnitId());
                    }
                } catch (Exception e) {
                    log.warn(" [InvoiceService] Failed to get buildingId from unitId {}: {}", invoice.getPayerUnitId(), e.getMessage());
                }
            }
            
            // Get all residents in the unit (via household)
            List<UUID> residentIds = new ArrayList<>();
            if (invoice.getPayerUnitId() != null) {
                try {
                    BaseServiceClient.ServiceInfo.HouseholdInfo household = baseServiceClient.getCurrentHouseholdByUnitId(invoice.getPayerUnitId());
                    if (household != null && household.getId() != null) {
                        List<BaseServiceClient.ServiceInfo.HouseholdMemberInfo> members = baseServiceClient.getActiveMembersByHouseholdId(household.getId());
                        for (BaseServiceClient.ServiceInfo.HouseholdMemberInfo member : members) {
                            if (member.getResidentId() != null) {
                                residentIds.add(member.getResidentId());
                            }
                        }
                        // Also include primary resident if not already in the list
                        if (household.getPrimaryResidentId() != null && !residentIds.contains(household.getPrimaryResidentId())) {
                            residentIds.add(household.getPrimaryResidentId());
                        }
                        log.info(" [InvoiceService] Found {} residents in unit {} (household {})", 
                                residentIds.size(), invoice.getPayerUnitId(), household.getId());
                    } else {
                        // Fallback: if no household found, use payerResidentId
                        if (invoice.getPayerResidentId() != null) {
                            residentIds.add(invoice.getPayerResidentId());
                            log.warn(" [InvoiceService] No household found for unit {}, using payerResidentId only", invoice.getPayerUnitId());
                        }
                    }
                } catch (Exception e) {
                    log.warn(" [InvoiceService] Failed to get residents for unit {}: {}", invoice.getPayerUnitId(), e.getMessage());
                    // Fallback: use payerResidentId
                    if (invoice.getPayerResidentId() != null) {
                        residentIds.add(invoice.getPayerResidentId());
                    }
                }
            }
            
            if (residentIds.isEmpty()) {
                log.debug(" [InvoiceService] No residents found for unit {}, cannot send notification", 
                        invoice.getPayerUnitId());
                return;
            }
            
            // Calculate total amount
            BigDecimal totalAmount = invoiceLineRepository.findByInvoiceId(invoice.getId()).stream()
                    .map(InvoiceLine::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Format amount
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            currencyFormat.setMaximumFractionDigits(0);
            String amountText = currencyFormat.format(totalAmount);
            
            // Build notification message
            String invoiceCode = invoice.getCode() != null ? invoice.getCode() : invoice.getId().toString();
            String title = "Hóa đơn mới - " + invoiceCode;
            String message = String.format("Bạn có hóa đơn mới với số tiền %s. Hạn thanh toán: %s", 
                    amountText,
                    invoice.getDueDate() != null ? invoice.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A");
            
            // Prepare data payload
            Map<String, String> data = new HashMap<>();
            data.put("invoiceId", invoice.getId().toString());
            data.put("invoiceCode", invoiceCode);
            data.put("amount", totalAmount.toString());
            data.put("dueDate", invoice.getDueDate() != null ? invoice.getDueDate().toString() : "");
            
            // Send PRIVATE notification to EACH resident in the unit (one notification per resident)
            // IMPORTANT: Set buildingId = null to ensure notifications are private (targetResidentId only)
            int successCount = 0;
            for (UUID residentId : residentIds) {
                try {
                    // Set buildingId = null to make notification private (only visible to targetResidentId)
                    notificationClient.sendResidentNotification(
                            residentId,
                            null, // buildingId = null for private notification
                            "BILL",
                            title,
                            message,
                            invoice.getId(),
                            "INVOICE",
                            data
                    );
                    successCount++;
                } catch (Exception e) {
                    log.warn(" [InvoiceService] Failed to send notification to residentId {}: {}", residentId, e.getMessage());
                }
            }
            
            log.info(" [InvoiceService] Sent PRIVATE invoice notification to {}/{} residents in unit {}, invoiceId={}", 
                    successCount, residentIds.size(), invoice.getPayerUnitId(), invoice.getId());
        } catch (Exception e) {
            log.error(" [InvoiceService] Failed to send invoice notification for invoiceId={}: {}", 
                    invoice.getId(), e.getMessage(), e);
        }
    }
    
    @Transactional
    public InvoiceDto updateInvoiceStatus(UUID invoiceId, UpdateInvoiceStatusRequest request) {
        log.info("Updating invoice status: {} to {}", invoiceId, request.getStatus());
        
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        
        InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(request.getStatus());
        
        // If updating to PAID and paidAt is null, set it to current time
        if (request.getStatus() == InvoiceStatus.PAID && invoice.getPaidAt() == null) {
            invoice.setPaidAt(OffsetDateTime.now());
            log.info("Setting paidAt to current time for invoice {} (status updated to PAID)", invoiceId);
        }
        
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice {} status updated from {} to {}, paidAt: {}", 
                invoiceId, oldStatus, request.getStatus(), updatedInvoice.getPaidAt());
        
        return toDto(updatedInvoice);
    }
    
    @Transactional
    public void voidInvoice(UUID invoiceId, String reason) {
        log.info("Voiding invoice: {} with reason: {}", invoiceId, reason);
        
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Cannot void a paid invoice. Create a refund instead.");
        }
        
        invoice.setStatus(InvoiceStatus.VOID);
        invoiceRepository.save(invoice);
        
        log.info("Invoice {} voided successfully", invoiceId);
    }
    
    /**
     * Kiểm tra xem user có thuộc căn hộ (unit) không
     * @throws IllegalArgumentException nếu user không thuộc căn hộ
     */
    /**
     * Validate that the user belongs to the specified unit (căn hộ).
     * Only residents who are members of the household in this unit can access invoices.
     * 
     * @param userId The authenticated user's ID
     * @param unitId The unit ID to check access for
     * @throws IllegalArgumentException if user doesn't belong to the unit
     */
    private void validateUserBelongsToUnit(UUID userId, UUID unitId) {
        if (unitId == null) {
            throw new IllegalArgumentException("unitId is required");
        }
        
        UUID residentId = residentRepository.findResidentIdByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + userId));
        
        // Kiểm tra xem resident có thuộc căn hộ này không thông qua household
        try {
            BaseServiceClient.ServiceInfo.HouseholdInfo household = baseServiceClient.getCurrentHouseholdByUnitId(unitId);
            if (household == null || household.getId() == null) {
                log.warn(" [InvoiceService] No household found for unit {}", unitId);
                throw new IllegalArgumentException("Bạn không có quyền truy cập invoice của căn hộ này");
            }
            
            // Kiểm tra xem resident có là member của household này không
            List<BaseServiceClient.ServiceInfo.HouseholdMemberInfo> members = baseServiceClient.getActiveMembersByHouseholdId(household.getId());
            boolean isMember = members.stream()
                    .anyMatch(member -> member.getResidentId() != null && member.getResidentId().equals(residentId));
            
            // Kiểm tra xem có phải primary resident không
            boolean isPrimaryResident = household.getPrimaryResidentId() != null 
                    && household.getPrimaryResidentId().equals(residentId);
            
            // Chỉ cho phép nếu resident là member HOẶC primary resident của household
            if (!isMember && !isPrimaryResident) {
                log.warn(" [InvoiceService] Resident {} is not a member of unit {} (household {})", 
                        residentId, unitId, household.getId());
                throw new IllegalArgumentException("Bạn không có quyền truy cập invoice của căn hộ này");
            }
            
            log.debug(" [InvoiceService] Resident {} validated for unit {} (isMember: {}, isPrimary: {})", 
                    residentId, unitId, isMember, isPrimaryResident);
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw IllegalArgumentException
        } catch (Exception e) {
            log.error(" [InvoiceService] Error validating user {} belongs to unit {}: {}", 
                    userId, unitId, e.getMessage(), e);
            throw new IllegalArgumentException("Bạn không có quyền truy cập invoice của căn hộ này");
        }
    }
    
    public List<InvoiceLineResponseDto> getMyInvoices(UUID userId, UUID unitFilter, UUID cycleFilter) {
        if (unitFilter == null) {
            throw new IllegalArgumentException("unitId is required");
        }
        
        // Validate: user phải thuộc căn hộ này
        // Chỉ những cư dân thuộc căn hộ này mới có thể xem invoice của căn hộ đó
        validateUserBelongsToUnit(userId, unitFilter);
        
        // Lấy invoice theo payerUnitId (căn hộ) - CHỈ lấy invoice của căn hộ này
        // Tất cả thành viên trong cùng căn hộ (household) đều có thể xem invoice của căn hộ đó
        // Cư dân ở căn hộ khác sẽ KHÔNG thấy được invoice này
        List<Invoice> invoices = invoiceRepository.findByPayerUnitId(unitFilter);
        log.debug(" [InvoiceService] Found {} invoices for unit {} (before filters)", invoices.size(), unitFilter);
        
        invoices = invoices.stream()
                .filter(invoice -> {
                    // Đảm bảo invoice thuộc đúng căn hộ
                    if (!unitFilter.equals(invoice.getPayerUnitId())) {
                        log.warn(" [InvoiceService] Invoice {} has payerUnitId {} but requested unitId is {}", 
                                invoice.getId(), invoice.getPayerUnitId(), unitFilter);
                        return false;
                    }
                    // Filter theo cycle nếu có
                    return cycleFilter == null || cycleFilter.equals(invoice.getCycleId());
                })
                .collect(Collectors.toList());
        
        log.debug(" [InvoiceService] After filters: {} invoices remain for unit {}", invoices.size(), unitFilter);
        List<InvoiceLineResponseDto> result = new ArrayList<>();
        
        for (Invoice invoice : invoices) {
            List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
            // Check permission once per invoice (not per line) - message will be shown at invoice level
            boolean isOwner = false;
            boolean canPay = false;
            String permissionMessage = null;
            
            if (userId != null && invoice.getPayerUnitId() != null) {
                try {
                    isOwner = baseServiceClient.isOwnerOfUnit(userId, invoice.getPayerUnitId());
                    
                    if (isOwner) {
                        // OWNER/TENANT can pay if invoice is not already paid
                        canPay = invoice.getStatus() != InvoiceStatus.PAID && invoice.getStatus() != InvoiceStatus.VOID;
                    } else {
                        // Not OWNER/TENANT - household member
                        canPay = false;
                        // Set permission message - will be shown at invoice level, not per line
                        permissionMessage = "Chỉ chủ căn hộ mới được quyền thanh toán hóa đơn này";
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [InvoiceService] Error checking permission for invoice {}: {}", 
                            invoice.getId(), e.getMessage());
                    // If check fails, default to no permission
                    permissionMessage = "Chỉ chủ căn hộ mới được quyền thanh toán hóa đơn này";
                }
            }
            
            // Add lines - only set permissionMessage for first line (invoice level)
            boolean isFirstLine = true;
            for (InvoiceLine line : lines) {
                InvoiceLineResponseDto dto = toInvoiceLineResponseDto(invoice, line, userId);
                // Only set permission message for first line of each invoice (to display at invoice level)
                if (isFirstLine) {
                    dto.setIsOwner(isOwner);
                    dto.setCanPay(canPay);
                    dto.setPermissionMessage(permissionMessage);
                    isFirstLine = false;
                } else {
                    // For other lines, set permissionMessage to null (message will be shown at invoice level only)
                    dto.setIsOwner(isOwner);
                    dto.setCanPay(canPay);
                    dto.setPermissionMessage(null);
                }
                result.add(dto);
            }
        }
        
        return result;
    }

    public String createVnpayPaymentUrl(UUID invoiceId, UUID userId, HttpServletRequest request, UUID unitFilter) {
        if (userId == null) {
            throw new IllegalArgumentException("Người dùng chưa đăng nhập");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn: " + invoiceId));

        UUID residentId = residentRepository.findResidentIdByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cư dân cho user: " + userId));

        // Kiểm tra quyền OWNER: chỉ OWNER hoặc TENANT mới được thanh toán hóa đơn điện/nước cho căn hộ
        UUID unitIdToCheck = unitFilter != null ? unitFilter : invoice.getPayerUnitId();
        if (unitIdToCheck != null) {
            boolean isOwner = baseServiceClient.isOwnerOfUnit(userId, unitIdToCheck);
            if (!isOwner) {
                throw new IllegalStateException(
                    "Chỉ chủ căn hộ (OWNER hoặc người thuê TENANT) mới được thanh toán hóa đơn điện, nước cho căn hộ. " +
                    "Thành viên hộ gia đình không được phép thanh toán."
                );
            }
        } else if (invoice.getPayerUnitId() == null) {
            // Nếu invoice không có payerUnitId, chỉ cho phép payerResidentId thanh toán
            // Nhưng vẫn cần kiểm tra OWNER nếu có thể
            if (!residentId.equals(invoice.getPayerResidentId())) {
                throw new IllegalArgumentException("Bạn không có quyền thanh toán hóa đơn này");
            }
        }
        
        // Kiểm tra invoice có thuộc căn hộ đã chọn không
        if (unitFilter != null && !unitFilter.equals(invoice.getPayerUnitId())) {
            throw new IllegalArgumentException("Hóa đơn không thuộc căn hộ đã chọn");
        }

        if (InvoiceStatus.PAID.equals(invoice.getStatus())) {
            throw new IllegalStateException("Hóa đơn đã được thanh toán trước đó");
        }

        BigDecimal totalAmount = invoiceLineRepository.findByInvoiceId(invoiceId).stream()
                .map(InvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Hóa đơn không có số tiền cần thanh toán");
        }

        String clientIp = resolveClientIp(request);

        // Tạo orderId unique từ invoiceId và timestamp để tránh collision
        long orderId = Math.abs(invoiceId.hashCode());
        if (orderId == 0) {
            orderId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        }
        // Đảm bảo orderId là unique bằng cách kiểm tra nếu đã tồn tại
        // Nếu đã tồn tại orderId trong map và không phải cùng invoice, tạo mới
        while (orderIdToInvoiceIdMap.containsKey(orderId) && 
               !orderIdToInvoiceIdMap.get(orderId).equals(invoiceId)) {
            orderId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        }
        orderIdToInvoiceIdMap.put(orderId, invoiceId);

        String orderInfo = "Thanh toán hóa đơn " + (invoice.getCode() != null ? invoice.getCode() : invoiceId);
        String returnUrl = vnpayProperties.getReturnUrl();

        // Set vnpayInitiatedAt to track when payment was initiated
        // Note: For invoices, we don't auto-expire payments. Status only changes to PAID when payment succeeds.
        invoice.setVnpayInitiatedAt(OffsetDateTime.now());
        invoice.setPaymentGateway("VNPAY");
        invoiceRepository.save(invoice);

        log.info("💳 [InvoiceService] Creating VNPAY URL for invoice={}, user={}, amount={}, ip={}, orderId={}, initiatedAt={}",
                invoiceId, userId, totalAmount, clientIp, orderId, invoice.getVnpayInitiatedAt());

        return vnpayService.createPaymentUrl(orderId, orderInfo, totalAmount, clientIp, returnUrl);
    }

    public VnpayCallbackResult handleVnpayCallback(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Thiếu dữ liệu callback từ VNPAY");
        }

        boolean signatureValid = vnpayService.validateReturn(new HashMap<>(params));
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String txnRef = params.get("vnp_TxnRef");

        if (txnRef == null || txnRef.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã giao dịch (vnp_TxnRef) từ VNPAY");
        }

        UUID invoiceId = getInvoiceIdFromTxnRef(txnRef);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn cho txnRef: " + txnRef));

        log.info(" [InvoiceService] Processing VNPAY callback for invoice {} (txnRef: {}, responseCode: {}, status: {})", 
                invoiceId, txnRef, responseCode, transactionStatus);

        invoice.setVnpResponseCode(responseCode);
        invoice.setVnpTransactionRef(txnRef);
        invoice.setVnpTransactionNo(params.get("vnp_TransactionNo"));
        invoice.setVnpBankCode(params.get("vnp_BankCode"));
        invoice.setVnpCardType(params.get("vnp_CardType"));

        boolean alreadyPaid = InvoiceStatus.PAID.equals(invoice.getStatus()) && invoice.getPaidAt() != null;

        if (signatureValid && "00".equals(responseCode) && "00".equals(transactionStatus)) {
            if (!alreadyPaid) {
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setPaymentGateway("VNPAY");
                // Use current time for payment date to ensure accurate timestamp
                invoice.setPaidAt(OffsetDateTime.now());
                // Clear vnpayInitiatedAt since payment is now complete
                invoice.setVnpayInitiatedAt(null);
                invoiceRepository.save(invoice);
                
                // Note: Paid invoices are automatically available via getMyInvoices API
                // No separate history table needed - Flutter queries billing.invoices directly
                
                notifyPaymentSuccess(invoice, params);
                log.info(" [InvoiceService] Invoice {} marked as PAID via VNPAY (txnRef: {})", invoiceId, txnRef);
            } else {
                log.info(" [InvoiceService] Duplicate VNPAY callback received for already paid invoice {} (txnRef: {})", 
                        invoiceId, txnRef);
            }
            return new VnpayCallbackResult(
                invoiceId, 
                true, 
                responseCode, 
                true,
                "Đã thanh toán hóa đơn thành công"
            );
        }

        invoiceRepository.save(invoice);
        log.warn(" [InvoiceService] VNPAY payment failed for invoice {} (txnRef: {}) - responseCode={}, validSignature={}",
                invoiceId, txnRef, responseCode, signatureValid);
        return new VnpayCallbackResult(
            invoiceId, 
            false, 
            responseCode, 
            signatureValid,
            "Thanh toán không thành công. Vui lòng thử lại."
        );
    }

    public UUID getInvoiceIdFromTxnRef(String txnRef) {
        if (txnRef == null || txnRef.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã giao dịch không được để trống");
        }
        
        // Ưu tiên tìm invoice theo vnpTransactionRef từ database (chính xác nhất)
        Optional<Invoice> invoiceByRef = invoiceRepository.findByVnpTransactionRef(txnRef);
        if (invoiceByRef.isPresent()) {
            UUID invoiceId = invoiceByRef.get().getId();
            log.info(" [InvoiceService] Found invoice {} by vnpTransactionRef: {}", invoiceId, txnRef);
            return invoiceId;
        }
        
        // Fallback: nếu không tìm thấy theo vnpTransactionRef, thử parse orderId
        if (!txnRef.contains("_")) {
            throw new IllegalArgumentException("Sai định dạng mã giao dịch: " + txnRef);
        }
        
        try {
            Long orderId = Long.parseLong(txnRef.split("_")[0]);
            UUID invoiceId = orderIdToInvoiceIdMap.get(orderId);
            if (invoiceId == null) {
                throw new IllegalArgumentException(
                        "Không tìm thấy hóa đơn tương ứng với orderId: " + orderId + " và txnRef: " + txnRef);
            }
            log.info(" [InvoiceService] Map orderId {} -> invoice {} (from in-memory map)", orderId, invoiceId);
            return invoiceId;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Không thể phân tích mã giao dịch: " + txnRef, ex);
        }
    }

    public List<InvoiceCategoryResponseDto> getUnpaidInvoicesByCategory(UUID userId, UUID unitFilter, UUID cycleFilter) {
        if (unitFilter == null) {
            throw new IllegalArgumentException("unitId is required");
        }
        
        // Validate: user phải thuộc căn hộ này
        // Chỉ những cư dân thuộc căn hộ này mới có thể xem invoice của căn hộ đó
        validateUserBelongsToUnit(userId, unitFilter);
        
        // Lấy buildingId từ unitId để filter invoice theo cả unitId và buildingId
        // Logic động: lấy buildingId từ căn hộ (unit) mà cư dân đang ở
        UUID buildingId;
        try {
            BaseServiceClient.UnitInfo unitInfo = baseServiceClient.getUnitById(unitFilter);
            if (unitInfo != null && unitInfo.getBuildingId() != null) {
                buildingId = unitInfo.getBuildingId();
            } else {
                buildingId = null;
            }
        } catch (Exception e) {
            log.warn(" [InvoiceService] Failed to get buildingId from unitId {}: {}", unitFilter, e.getMessage());
            buildingId = null;
        }
        final UUID finalBuildingId = buildingId; // Make effectively final for lambda
        
        // Lấy invoice từ bảng invoice, filter theo payerUnitId (căn hộ) và buildingId (tòa nhà)
        // Cư dân ở căn hộ nào thuộc tòa nào sẽ chỉ thấy invoice của căn hộ đó thuộc tòa đó
        List<Invoice> invoices = invoiceRepository.findByPayerUnitId(unitFilter);
        log.debug(" [InvoiceService] Found {} invoices for unit {} (before filters)", invoices.size(), unitFilter);
        
        invoices = invoices.stream()
                .filter(invoice -> {
                    // Đảm bảo invoice thuộc đúng căn hộ
                    if (!unitFilter.equals(invoice.getPayerUnitId())) {
                        log.warn(" [InvoiceService] Invoice {} has payerUnitId {} but requested unitId is {}", 
                                invoice.getId(), invoice.getPayerUnitId(), unitFilter);
                        return false;
                    }
                    
                    // Filter theo buildingId: chỉ lấy invoice của căn hộ thuộc cùng tòa nhà với căn hộ của cư dân
                    if (finalBuildingId != null && invoice.getPayerUnitId() != null) {
                        try {
                            BaseServiceClient.UnitInfo invoiceUnitInfo = baseServiceClient.getUnitById(invoice.getPayerUnitId());
                            if (invoiceUnitInfo != null && invoiceUnitInfo.getBuildingId() != null) {
                                if (!finalBuildingId.equals(invoiceUnitInfo.getBuildingId())) {
                                    log.debug(" [InvoiceService] Invoice {} belongs to different building, filtering out", invoice.getId());
                                    return false;
                                }
                            }
                        } catch (Exception e) {
                            log.warn(" [InvoiceService] Failed to get buildingId for invoice unit {}: {}", 
                                    invoice.getPayerUnitId(), e.getMessage());
                        }
                    }
                    
                    // Filter theo cycle nếu có
                    return cycleFilter == null || cycleFilter.equals(invoice.getCycleId());
                })
                .collect(Collectors.toList());
        
        log.debug(" [InvoiceService] After filters: {} invoices remain for unit {} (buildingId: {})", 
                invoices.size(), unitFilter, buildingId);
        Map<String, List<InvoiceLineResponseDto>> grouped = new HashMap<>();

        for (Invoice invoice : invoices) {
            // Include UNPAID invoices - they need to be shown with warning
            // Only exclude PAID and VOID invoices
            if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.VOID) {
                continue;
            }

            List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
            
            // Log for debugging UNPAID invoices
            if (invoice.getStatus() == InvoiceStatus.UNPAID) {
                log.info("🔍 [InvoiceService] Found UNPAID invoice {} (code: {}) with {} lines for unit {}", 
                        invoice.getId(), invoice.getCode(), lines.size(), unitFilter);
                if (lines.isEmpty()) {
                    log.warn("⚠️ [InvoiceService] UNPAID invoice {} has no invoice lines!", invoice.getId());
                }
                for (InvoiceLine line : lines) {
                    log.info("   Line {}: serviceCode={}, description={}", 
                            line.getId(), line.getServiceCode(), line.getDescription());
                }
            }
            
            if (lines.isEmpty()) {
                // Skip invoices without lines
                log.debug("⚠️ [InvoiceService] Invoice {} has no lines, skipping", invoice.getId());
                continue;
            }
            
            // Check permission once per invoice (not per line) - message will be shown at invoice level
            boolean isOwner = false;
            boolean canPay = false;
            String permissionMessage = null;
            
            if (userId != null && invoice.getPayerUnitId() != null) {
                try {
                    isOwner = baseServiceClient.isOwnerOfUnit(userId, invoice.getPayerUnitId());
                    
                    if (isOwner) {
                        // OWNER/TENANT can pay if invoice is not already paid
                        canPay = invoice.getStatus() != InvoiceStatus.PAID && invoice.getStatus() != InvoiceStatus.VOID;
                    } else {
                        // Not OWNER/TENANT - household member
                        canPay = false;
                        // Set permission message - will be shown at invoice level, not per line
                        permissionMessage = "Chỉ chủ căn hộ mới được quyền thanh toán hóa đơn này";
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [InvoiceService] Error checking permission for invoice {}: {}", 
                            invoice.getId(), e.getMessage());
                    // If check fails, default to no permission
                    permissionMessage = "Chỉ chủ căn hộ mới được quyền thanh toán hóa đơn này";
                }
            }
            
            // Add lines - only set permissionMessage for first line (invoice level)
            boolean isFirstLine = true;
            for (InvoiceLine line : lines) {
                InvoiceLineResponseDto dto = toInvoiceLineResponseDto(invoice, line, userId);
                // Only set permission message for first line of each invoice (to display at invoice level)
                if (isFirstLine) {
                    dto.setIsOwner(isOwner);
                    dto.setCanPay(canPay);
                    dto.setPermissionMessage(permissionMessage);
                    isFirstLine = false;
                } else {
                    // For other lines, set permissionMessage to null (message will be shown at invoice level only)
                    dto.setIsOwner(isOwner);
                    dto.setCanPay(canPay);
                    dto.setPermissionMessage(null);
                }
                
                // Include UNPAID invoices - they need to be shown with warning
                // Only exclude PAID invoices
                if ("PAID".equalsIgnoreCase(dto.getStatus())) {
                    continue;
                }

                // Only show electricity and water invoices - skip all others (contract renewal, etc.)
                // This applies to ALL statuses including UNPAID - only ELECTRIC and WATER are shown
                String serviceCode = line.getServiceCode();
                if (serviceCode == null || serviceCode.isBlank()) {
                    log.debug("⚠️ [InvoiceService] Invoice {} line {} has no serviceCode, skipping", invoice.getId(), line.getId());
                    continue;
                }
                String normalized = serviceCode.trim().toUpperCase();
                // Only allow ELECTRICITY and WATER service codes (for all statuses including UNPAID)
                if (!normalized.contains("ELECTRIC") && !normalized.contains("WATER")) {
                    if (invoice.getStatus() == InvoiceStatus.UNPAID) {
                        log.info("ℹ️ [InvoiceService] UNPAID invoice {} line {} serviceCode {} is not ELECTRIC or WATER, skipping (only electricity/water invoices are shown in 'Hóa đơn mới')", 
                                invoice.getId(), line.getId(), serviceCode);
                    } else {
                        log.debug("⚠️ [InvoiceService] Invoice {} line {} serviceCode {} is not ELECTRIC or WATER, skipping", 
                                invoice.getId(), line.getId(), serviceCode);
                    }
                    continue;
                }

                String category = determineCategory(line.getServiceCode());
                grouped.computeIfAbsent(category, key -> new ArrayList<>()).add(dto);
                
                // Log for debugging
                if (invoice.getStatus() == InvoiceStatus.UNPAID) {
                    log.info("✅ [InvoiceService] Added UNPAID invoice {} line {} (serviceCode: {}, status: {}) to category {}", 
                            invoice.getId(), line.getId(), serviceCode, dto.getStatus(), category);
                }
            }
        }
        
        // Log summary
        int totalUnpaidInvoices = grouped.values().stream()
                .mapToInt(list -> (int) list.stream().filter(dto -> "UNPAID".equalsIgnoreCase(dto.getStatus())).count())
                .sum();
        if (totalUnpaidInvoices > 0) {
            log.info("✅ [InvoiceService] Found {} UNPAID invoice lines across {} categories", totalUnpaidInvoices, grouped.size());
        }

        List<InvoiceCategoryResponseDto> response = new ArrayList<>();
        Set<String> processed = new LinkedHashSet<>();

        for (String category : CATEGORY_ORDER) {
            List<InvoiceLineResponseDto> items = grouped.get(category);
            if (items == null || items.isEmpty()) {
                continue;
            }
            response.add(buildCategoryResponse(category, items));
            processed.add(category);
        }

        grouped.forEach((category, items) -> {
            if (items == null || items.isEmpty() || processed.contains(category)) {
                return;
            }
            response.add(buildCategoryResponse(category, items));
        });
        log.debug(" [InvoiceService] Grouped categories: {}", grouped.keySet());
        log.debug(" [InvoiceService] Returning {} categories", response.size());

        return response;
    }

    public List<InvoiceCategoryResponseDto> getPaidInvoicesByCategory(UUID userId, UUID unitFilter, UUID cycleFilter) {
        if (unitFilter == null) {
            throw new IllegalArgumentException("unitId is required");
        }
        
        // Validate: user phải thuộc căn hộ này
        // Chỉ những cư dân thuộc căn hộ này mới có thể xem invoice của căn hộ đó
        validateUserBelongsToUnit(userId, unitFilter);
        
        // Lấy buildingId từ unitId để filter invoice theo cả unitId và buildingId
        // Logic động: lấy buildingId từ căn hộ (unit) mà cư dân đang ở
        UUID buildingId;
        try {
            BaseServiceClient.UnitInfo unitInfo = baseServiceClient.getUnitById(unitFilter);
            if (unitInfo != null && unitInfo.getBuildingId() != null) {
                buildingId = unitInfo.getBuildingId();
            } else {
                buildingId = null;
            }
        } catch (Exception e) {
            log.warn(" [InvoiceService] Failed to get buildingId from unitId {}: {}", unitFilter, e.getMessage());
            buildingId = null;
        }
        final UUID finalBuildingId = buildingId; // Make effectively final for lambda
        
        // Lấy invoice từ bảng invoice, filter theo payerUnitId (căn hộ) và buildingId (tòa nhà)
        // Cư dân ở căn hộ nào thuộc tòa nào sẽ chỉ thấy invoice của căn hộ đó thuộc tòa đó
        List<Invoice> invoices = invoiceRepository.findByPayerUnitId(unitFilter);
        log.debug(" [InvoiceService] Found {} invoices for unit {} (before filters)", invoices.size(), unitFilter);
        
        invoices = invoices.stream()
                .filter(invoice -> {
                    // Đảm bảo invoice thuộc đúng căn hộ
                    if (!unitFilter.equals(invoice.getPayerUnitId())) {
                        log.warn(" [InvoiceService] Invoice {} has payerUnitId {} but requested unitId is {}", 
                                invoice.getId(), invoice.getPayerUnitId(), unitFilter);
                        return false;
                    }
                    
                    // Filter theo buildingId: chỉ lấy invoice của căn hộ thuộc cùng tòa nhà với căn hộ của cư dân
                    if (finalBuildingId != null && invoice.getPayerUnitId() != null) {
                        try {
                            BaseServiceClient.UnitInfo invoiceUnitInfo = baseServiceClient.getUnitById(invoice.getPayerUnitId());
                            if (invoiceUnitInfo != null && invoiceUnitInfo.getBuildingId() != null) {
                                if (!finalBuildingId.equals(invoiceUnitInfo.getBuildingId())) {
                                    log.debug(" [InvoiceService] Invoice {} belongs to different building, filtering out", invoice.getId());
                                    return false;
                                }
                            }
                        } catch (Exception e) {
                            log.warn(" [InvoiceService] Failed to get buildingId for invoice unit {}: {}", 
                                    invoice.getPayerUnitId(), e.getMessage());
                        }
                    }
                    
                    // Filter theo cycle nếu có
                    return cycleFilter == null || cycleFilter.equals(invoice.getCycleId());
                })
                .collect(Collectors.toList());
        
        log.debug(" [InvoiceService] After filters: {} invoices remain for unit {} (buildingId: {})", 
                invoices.size(), unitFilter, buildingId);
        Map<String, List<InvoiceLineResponseDto>> grouped = new HashMap<>();

        for (Invoice invoice : invoices) {
            log.debug(" [InvoiceService] Inspect invoice {} status {}", invoice.getId(), invoice.getStatus());
            if (invoice.getStatus() != InvoiceStatus.PAID) {
                continue;
            }

            List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
            log.debug(" [InvoiceService] Invoice {} has {} lines", invoice.getId(), lines.size());
            for (InvoiceLine line : lines) {
                String serviceCode = line.getServiceCode();
                if (serviceCode == null || serviceCode.isBlank()) {
                    log.debug("🔍 [InvoiceService] Skipping invoice line {} with null/empty serviceCode", line.getId());
                    continue;
                }
                String normalized = serviceCode.trim().toUpperCase();
                
                // For paid invoices: show electricity, water, contract renewal, AND card payments
                boolean isElectricity = normalized.contains("ELECTRIC");
                boolean isWater = normalized.contains("WATER");
                boolean isContractRenewal = normalized.contains("CONTRACT");
                boolean isVehicleCard = normalized.contains("VEHICLE_CARD") || normalized.contains("VEHICLE");
                boolean isElevatorCard = normalized.contains("ELEVATOR_CARD") || normalized.contains("ELEVATOR");
                boolean isResidentCard = normalized.contains("RESIDENT_CARD") || normalized.contains("RESIDENT");
                
                if (!isElectricity && !isWater && !isContractRenewal && !isVehicleCard && !isElevatorCard && !isResidentCard) {
                    log.debug("🔍 [InvoiceService] Skipping invoice line {} with serviceCode: {} (not electricity/water/contract/card)", line.getId(), serviceCode);
                    continue;
                }

                String category = determineCategory(line.getServiceCode());
                InvoiceLineResponseDto dto = toInvoiceLineResponseDto(invoice, line, userId);
                grouped.computeIfAbsent(category, key -> new ArrayList<>()).add(dto);
                log.debug(" [InvoiceService] Added line {} to category {}", line.getId(), category);
            }
        }

        List<InvoiceCategoryResponseDto> response = new ArrayList<>();
        Set<String> processed = new LinkedHashSet<>();

        for (String category : CATEGORY_ORDER) {
            List<InvoiceLineResponseDto> items = grouped.get(category);
            if (items == null || items.isEmpty()) {
                continue;
            }
            response.add(buildCategoryResponse(category, items));
            processed.add(category);
        }

        grouped.forEach((category, items) -> {
            if (items == null || items.isEmpty() || processed.contains(category)) {
                return;
            }
            response.add(buildCategoryResponse(category, items));
        });

        return response;
    }
    
    public List<ElectricityMonthlyDto> getElectricityMonthlyData(UUID userId, UUID unitFilter) {
        UUID residentId = residentRepository.findResidentIdByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + userId));
        
        // Lấy invoice theo payerUnitId (căn hộ) nếu có unitFilter
        // Để tất cả thành viên trong cùng căn hộ có thể xem invoice của căn hộ đó
        List<Invoice> invoices;
        if (unitFilter != null) {
            // Validate: user phải thuộc căn hộ này
            // Chỉ những cư dân thuộc căn hộ này mới có thể xem invoice của căn hộ đó
            validateUserBelongsToUnit(userId, unitFilter);
            invoices = invoiceRepository.findByPayerUnitId(unitFilter);
            // Đảm bảo chỉ lấy invoice của căn hộ này
            invoices = invoices.stream()
                    .filter(invoice -> unitFilter.equals(invoice.getPayerUnitId()))
                    .collect(Collectors.toList());
        } else {
            // Nếu không có unitFilter, lấy theo residentId (fallback - chỉ cho chính resident đó)
            // Lưu ý: Fallback này chỉ nên dùng khi không có unitId, nhưng tốt nhất là luôn cung cấp unitId
            log.warn(" [InvoiceService] getElectricityMonthlyData called without unitId, using residentId fallback");
            invoices = invoiceRepository.findByPayerResidentId(residentId);
        }
        List<InvoiceLine> electricityLines = new ArrayList<>();
        
        for (Invoice invoice : invoices) {
            List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceIdAndServiceCode(
                    invoice.getId(), "ELECTRIC");
            electricityLines.addAll(lines);
        }
        
        // Group by month
        Map<String, List<InvoiceLine>> linesByMonth = electricityLines.stream()
                .collect(Collectors.groupingBy(line -> {
                    LocalDate serviceDate = line.getServiceDate();
                    return String.format("%04d-%02d", serviceDate.getYear(), serviceDate.getMonthValue());
                }));
        
        List<ElectricityMonthlyDto> result = new ArrayList<>();
        for (Map.Entry<String, List<InvoiceLine>> entry : linesByMonth.entrySet()) {
            String month = entry.getKey();
            List<InvoiceLine> lines = entry.getValue();
            
            BigDecimal totalAmount = lines.stream()
                    .map(InvoiceLine::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            LocalDate firstDate = lines.get(0).getServiceDate();
            String monthDisplay = String.format("%02d/%04d", firstDate.getMonthValue(), firstDate.getYear());
            
            result.add(ElectricityMonthlyDto.builder()
                    .month(month)
                    .monthDisplay(monthDisplay)
                    .amount(totalAmount)
                    .year(firstDate.getYear())
                    .monthNumber(firstDate.getMonthValue())
                    .build());
        }
        
        // Sort by month descending (newest first)
        result.sort((a, b) -> {
            int yearCompare = b.getYear().compareTo(a.getYear());
            if (yearCompare != 0) return yearCompare;
            return b.getMonthNumber().compareTo(a.getMonthNumber());
        });
        
        return result;
    }
    
    public record VnpayCallbackResult(UUID invoiceId, boolean success, String responseCode, boolean signatureValid, String message) {
        public VnpayCallbackResult(UUID invoiceId, boolean success, String responseCode, boolean signatureValid) {
            this(invoiceId, success, responseCode, signatureValid, null);
        }
    }


    private void notifyPaymentSuccess(Invoice invoice, Map<String, String> params) {
        if (invoice.getPayerResidentId() == null) {
            return;
        }

        Optional<ResidentContact> contactOpt = residentRepository.findContactByResidentId(invoice.getPayerResidentId());
        if (contactOpt.isEmpty() || contactOpt.get().email() == null || contactOpt.get().email().isBlank()) {
            log.warn(" [InvoiceService] Không tìm thấy email cư dân để gửi thông báo thanh toán");
            return;
        }

        ResidentContact contact = contactOpt.get();
        String email = contact.email();
        String customerName = contact.fullName() != null ? contact.fullName() : email;

        BigDecimal totalAmount = invoiceLineRepository.findByInvoiceId(invoice.getId()).stream()
                .map(InvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        currencyFormat.setMaximumFractionDigits(0);

        String invoiceCode = invoice.getCode() != null ? invoice.getCode() : invoice.getId().toString();
        String amountText = currencyFormat.format(totalAmount);
        OffsetDateTime paidAt = Optional.ofNullable(invoice.getPaidAt()).orElse(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        String paidAtText = paidAt.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String txnRef = params.get("vnp_TxnRef");

        String subject = "Thanh toán thành công - Hóa đơn " + invoiceCode;
        String body = "Xin chào " + customerName + ",\n\n" +
                "Thanh toán hóa đơn của bạn đã được xử lý thành công.\n\n" +
                "Thông tin thanh toán:\n" +
                "- Mã hóa đơn: " + invoiceCode + "\n" +
                "- Số tiền: " + amountText + "\n" +
                "- Ngày thanh toán: " + paidAtText + "\n" +
                "- Phương thức: VNPAY\n" +
                (txnRef != null ? "- Mã giao dịch: " + txnRef + "\n" : "") +
                "\nCảm ơn bạn đã sử dụng dịch vụ của QHomeBase!\n\n" +
                "Trân trọng,\n" +
                "QHomeBase";

        try {
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            log.error(" [InvoiceService] Không thể gửi email thông báo thanh toán cho {}: {}", email, e.getMessage());
        }
    }


    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String generateInvoiceCode() {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("INV-%s", timestamp);
    }
    
    private BigDecimal calculateTaxAmount(BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRate) {
        if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal subtotal = quantity.multiply(unitPrice);
        return subtotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }
    
    private InvoiceDto toDto(Invoice invoice) {
        List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
        
        BigDecimal totalAmount = lines.stream()
                .map(InvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return InvoiceDto.builder()
                .id(invoice.getId())
                .code(invoice.getCode())
                .issuedAt(invoice.getIssuedAt())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .currency(invoice.getCurrency())
                .billToName(invoice.getBillToName())
                .billToAddress(invoice.getBillToAddress())
                .billToContact(invoice.getBillToContact())
                .payerUnitId(invoice.getPayerUnitId())
                .payerResidentId(invoice.getPayerResidentId())
                .cycleId(invoice.getCycleId())
                .totalAmount(totalAmount)
                .paymentGateway(invoice.getPaymentGateway())
                .vnpTransactionRef(invoice.getVnpTransactionRef())
                .vnpTransactionNo(invoice.getVnpTransactionNo())
                .vnpBankCode(invoice.getVnpBankCode())
                .vnpCardType(invoice.getVnpCardType())
                .vnpResponseCode(invoice.getVnpResponseCode())
                .paidAt(invoice.getPaidAt())
                .lines(lines.stream().map(this::lineToDto).collect(Collectors.toList()))
                .build();
    }

    public InvoiceDto mapToDto(Invoice invoice) {
        return toDto(invoice);
    }
    
    private InvoiceLineDto lineToDto(InvoiceLine line) {
        return InvoiceLineDto.builder()
                .id(line.getId())
                .invoiceId(line.getInvoiceId())
                .serviceDate(line.getServiceDate())
                .description(line.getDescription())
                .quantity(line.getQuantity())
                .unit(line.getUnit())
                .unitPrice(line.getUnitPrice())
                .taxRate(line.getTaxRate())
                .taxAmount(line.getTaxAmount())
                .lineTotal(line.getLineTotal())
                .serviceCode(line.getServiceCode())
                .externalRefType(line.getExternalRefType())
                .externalRefId(line.getExternalRefId())
                .build();
    }
    
    private InvoiceLineResponseDto toInvoiceLineResponseDto(Invoice invoice, InvoiceLine line) {
        return toInvoiceLineResponseDto(invoice, line, null);
    }
    
    private InvoiceLineResponseDto toInvoiceLineResponseDto(Invoice invoice, InvoiceLine line, UUID userId) {
        // Check permission: isOwner, canPay
        boolean isOwner = false;
        boolean canPay = false;
        String permissionMessage = null;
        
        if (userId != null && invoice.getPayerUnitId() != null) {
            try {
                isOwner = baseServiceClient.isOwnerOfUnit(userId, invoice.getPayerUnitId());
                
                if (isOwner) {
                    // OWNER/TENANT can pay if invoice is not already paid
                    canPay = invoice.getStatus() != InvoiceStatus.PAID && invoice.getStatus() != InvoiceStatus.VOID;
                } else {
                    // Not OWNER/TENANT - household member
                    canPay = false;
                    // Set permission message - will be shown at invoice level, not per line
                    permissionMessage = "Chỉ chủ căn hộ mới được quyền thanh toán hóa đơn này";
                }
            } catch (Exception e) {
                log.warn("⚠️ [InvoiceService] Error checking permission for invoice {}: {}", 
                        invoice.getId(), e.getMessage());
                // If check fails, default to no permission
                permissionMessage = "Chỉ chủ căn hộ mới được quyền thanh toán hóa đơn này";
            }
        }
        
        // Calculate total after tax for this invoice
        List<InvoiceLine> allLines = invoiceLineRepository.findByInvoiceId(invoice.getId());
        BigDecimal totalAfterTax = allLines.stream()
                .map(InvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return InvoiceLineResponseDto.builder()
                .payerUnitId(invoice.getPayerUnitId() != null ? invoice.getPayerUnitId().toString() : "")
                .invoiceId(invoice.getId().toString())
                .invoiceCode(invoice.getCode()) // ✅ Add invoice code
                .serviceDate(line.getServiceDate().toString())
                .description(line.getDescription())
                .quantity(line.getQuantity() != null ? line.getQuantity().doubleValue() : 0.0)
                .unit(line.getUnit())
                .unitPrice(line.getUnitPrice() != null ? line.getUnitPrice().doubleValue() : 0.0)
                .taxAmount(line.getTaxAmount() != null ? line.getTaxAmount().doubleValue() : 0.0)
                .lineTotal(line.getLineTotal() != null ? line.getLineTotal().doubleValue() : 0.0)
                .totalAfterTax(totalAfterTax.doubleValue()) // ✅ Add total
                .serviceCode(line.getServiceCode())
                .status(invoice.getStatus() != null ? invoice.getStatus().name() : "PUBLISHED")
                .paidAt(invoice.getPaidAt()) // ✅ Add paid date
                .paymentGateway(invoice.getPaymentGateway()) // ✅ Add payment gateway
                .isOwner(isOwner)
                .canPay(canPay)
                .permissionMessage(permissionMessage)
                .build();
    }

    private InvoiceCategoryResponseDto buildCategoryResponse(String category, List<InvoiceLineResponseDto> invoices) {
        double total = invoices.stream()
                .mapToDouble(item -> item.getLineTotal() != null ? item.getLineTotal() : 0.0)
                .sum();

        return InvoiceCategoryResponseDto.builder()
                .categoryCode(category)
                .categoryName(resolveCategoryName(category))
                .totalAmount(total)
                .invoiceCount(invoices.size())
                .invoices(invoices)
                .build();
    }

    private String determineCategory(String serviceCode) {
        if (serviceCode == null || serviceCode.isBlank()) {
            return "OTHER";
        }
        String normalized = serviceCode.trim().toUpperCase();

        if (normalized.contains("ELECTRIC")) {
            return "ELECTRICITY";
        }
        if (normalized.contains("WATER")) {
            return "WATER";
        }
        if (normalized.contains("INTERNET") || normalized.contains("WIFI")) {
            return "INTERNET";
        }
        if (normalized.contains("ELEVATOR")) {
            return "ELEVATOR";
        }
        if (normalized.contains("PARK") || normalized.contains("VEHICLE") || normalized.contains("CAR") || normalized.contains("MOTOR")) {
            return "PARKING";
        }
        if (normalized.contains("CONTRACT")) {
            return "CONTRACT_RENEWAL";
        }

        return "OTHER";
    }

    private String resolveCategoryName(String categoryCode) {
        return CATEGORY_LABELS.getOrDefault(categoryCode, categoryCode);
    }

    private String buildVehicleRegistrationDescription(VehicleRegistrationPaymentRequest request) {
        StringBuilder builder = new StringBuilder("Đăng ký thẻ xe");
        if (request.getLicensePlate() != null && !request.getLicensePlate().isBlank()) {
            builder.append(" - ").append(request.getLicensePlate());
        }
        if (request.getVehicleType() != null && !request.getVehicleType().isBlank()) {
            builder.append(" (").append(request.getVehicleType()).append(")");
        }
        if (request.getNote() != null && !request.getNote().isBlank()) {
            builder.append(" - ").append(request.getNote());
        }
        return builder.toString();
    }

    @Transactional
    public InvoiceDto recordElevatorCardPayment(ElevatorCardPaymentRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("Missing userId");
        }

        UUID residentId = residentRepository.findResidentIdByUserId(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + request.getUserId()));

        BigDecimal amount = Optional.ofNullable(request.getAmount())
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .orElse(BigDecimal.valueOf(30000));

        // Use paymentDate from request if available (from callback), otherwise use current time
        OffsetDateTime payDate = Optional.ofNullable(request.getPaymentDate())
                .orElse(OffsetDateTime.now());
        LocalDate serviceDate = payDate.toLocalDate();

        String description = buildElevatorCardDescription(request);
        
        
        String billToName = "Căn hộ";
        if (request.getUnitId() != null) {
            try {
                BaseServiceClient.UnitInfo unitInfo = baseServiceClient.getUnitById(request.getUnitId());
                if (unitInfo != null && unitInfo.getCode() != null) {
                    billToName = String.format("Căn hộ %s", unitInfo.getCode());
                }
            } catch (Exception e) {
                log.warn("Failed to get unit code for billToName: {}", e.getMessage());
            }
        }

        CreateInvoiceRequest createRequest = CreateInvoiceRequest.builder()
                .dueDate(serviceDate)
                .currency("VND")
                .billToName(billToName)
                .payerUnitId(request.getUnitId())
                .payerResidentId(residentId)
                .lines(List.of(CreateInvoiceLineRequest.builder()
                        .serviceDate(serviceDate)
                        .description(description)
                        .quantity(BigDecimal.ONE)
                        .unit("lần")
                        .unitPrice(amount)
                        .taxRate(BigDecimal.ZERO)
                        .serviceCode("ELEVATOR_CARD")
                        .externalRefType("ELEVATOR_CARD")
                        .externalRefId(request.getRegistrationId())
                        .build()))
                .build();

        InvoiceDto created = createInvoice(createRequest);

        Invoice invoice = invoiceRepository.findById(created.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + created.getId()));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentGateway("VNPAY");
        invoice.setPaidAt(payDate);
        invoice.setVnpTransactionRef(request.getTransactionRef());
        invoice.setVnpTransactionNo(request.getTransactionNo());
        invoice.setVnpBankCode(request.getBankCode());
        invoice.setVnpCardType(request.getCardType());
        invoice.setVnpResponseCode(request.getResponseCode());
        invoiceRepository.save(invoice);

        Map<String, String> params = new HashMap<>();
        if (request.getTransactionRef() != null) {
            params.put("vnp_TxnRef", request.getTransactionRef());
        }
        notifyPaymentSuccess(invoice, params);

        return toDto(invoice);
    }

    private String buildElevatorCardDescription(ElevatorCardPaymentRequest request) {
        StringBuilder builder = new StringBuilder("Đăng ký thẻ thang máy");
        if (request.getApartmentNumber() != null && !request.getApartmentNumber().isBlank()) {
            builder.append(" - Căn ").append(request.getApartmentNumber());
        }
        if (request.getBuildingName() != null && !request.getBuildingName().isBlank()) {
            builder.append(" (").append(request.getBuildingName()).append(")");
        }
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            builder.append(" - ").append(request.getFullName());
        }
        return builder.toString();
    }

    @Transactional
    public InvoiceDto recordResidentCardPayment(ResidentCardPaymentRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("Missing userId");
        }

        UUID residentId = residentRepository.findResidentIdByUserId(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + request.getUserId()));

        BigDecimal amount = Optional.ofNullable(request.getAmount())
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .orElse(BigDecimal.valueOf(30000));

        // Use paymentDate from request if available (from callback), otherwise use current time
        OffsetDateTime payDate = Optional.ofNullable(request.getPaymentDate())
                .orElse(OffsetDateTime.now());
        LocalDate serviceDate = payDate.toLocalDate();

        String description = buildResidentCardDescription(request);
        
     
        String billToName = "Căn hộ";
        if (request.getUnitId() != null) {
            try {
                BaseServiceClient.UnitInfo unitInfo = baseServiceClient.getUnitById(request.getUnitId());
                if (unitInfo != null && unitInfo.getCode() != null) {
                    billToName = String.format("Căn hộ %s", unitInfo.getCode());
                }
            } catch (Exception e) {
                log.warn("Failed to get unit code for billToName: {}", e.getMessage());
            }
        }

        CreateInvoiceRequest createRequest = CreateInvoiceRequest.builder()
                .dueDate(serviceDate)
                .currency("VND")
                .billToName(billToName)
                .payerUnitId(request.getUnitId())
                .payerResidentId(residentId)
                .lines(List.of(CreateInvoiceLineRequest.builder()
                        .serviceDate(serviceDate)
                        .description(description)
                        .quantity(BigDecimal.ONE)
                        .unit("lần")
                        .unitPrice(amount)
                        .taxRate(BigDecimal.ZERO)
                        .serviceCode("RESIDENT_CARD")
                        .externalRefType("RESIDENT_CARD")
                        .externalRefId(request.getRegistrationId())
                        .build()))
                .build();

        InvoiceDto created = createInvoice(createRequest);

        Invoice invoice = invoiceRepository.findById(created.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + created.getId()));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentGateway("VNPAY");
        invoice.setPaidAt(payDate);
        invoice.setVnpTransactionRef(request.getTransactionRef());
        invoice.setVnpTransactionNo(request.getTransactionNo());
        invoice.setVnpBankCode(request.getBankCode());
        invoice.setVnpCardType(request.getCardType());
        invoice.setVnpResponseCode(request.getResponseCode());
        invoiceRepository.save(invoice);

        Map<String, String> params = new HashMap<>();
        if (request.getTransactionRef() != null) {
            params.put("vnp_TxnRef", request.getTransactionRef());
        }
        notifyPaymentSuccess(invoice, params);

        return toDto(invoice);
    }

    private String buildResidentCardDescription(ResidentCardPaymentRequest request) {
        StringBuilder builder = new StringBuilder("Đăng ký thẻ cư dân");
        if (request.getApartmentNumber() != null && !request.getApartmentNumber().isBlank()) {
            builder.append(" - Căn ").append(request.getApartmentNumber());
        }
        if (request.getBuildingName() != null && !request.getBuildingName().isBlank()) {
            builder.append(" (").append(request.getBuildingName()).append(")");
        }
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            builder.append(" - ").append(request.getFullName());
        }
        return builder.toString();
    }
    public List<String> findServiceDoNotHaveInvoiceInCycle(UUID cycleId, String serviceCode) {
        if (cycleId == null) {
            throw new IllegalArgumentException("CycleId cannot be null");
        }
        
        List<Invoice> invoicesInCycle = invoiceRepository.findByCycleId(cycleId);
        
        Set<String> servicesWithInvoice = new LinkedHashSet<>();
        for (Invoice invoice : invoicesInCycle) {
            List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
            for (InvoiceLine line : lines) {
                if (line.getServiceCode() != null && !line.getServiceCode().isBlank()) {
                    servicesWithInvoice.add(line.getServiceCode().toUpperCase().trim());
                }
            }
        }
        
        List<String> allServices;
        try {
            List<BaseServiceClient.ServiceInfo> services = baseServiceClient.getAllServices();
            if (services != null && !services.isEmpty()) {
                allServices = services.stream()
                        .map(BaseServiceClient.ServiceInfo::getCode)
                        .filter(code -> code != null && !code.isBlank())
                        .map(String::toUpperCase)
                        .distinct()
                        .collect(Collectors.toList());
            } else {
                allServices = getDefaultServiceList();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch services from base-service, using default list: {}", e.getMessage());
            allServices = getDefaultServiceList();
        }
        
        List<String> servicesWithoutInvoice = allServices.stream()
                .map(String::toUpperCase)
                .filter(service -> !servicesWithInvoice.contains(service))
                .distinct()
                .collect(Collectors.toList());
        
        if (serviceCode != null && !serviceCode.isBlank()) {
            String normalizedServiceCode = serviceCode.toUpperCase().trim();
            if (servicesWithoutInvoice.contains(normalizedServiceCode)) {
                return List.of(normalizedServiceCode);
            } else {
                return List.of();
            }
        }
        
        return servicesWithoutInvoice;
    }
    
    private List<String> getDefaultServiceList() {
        return List.of(
            "ELECTRIC", "ELECTRICITY", "WATER", "MANAGEMENT", 
            "PARKING", "PARKING_PRORATA", "PARKING_CAR", "PARKING_MOTORBIKE",
            "INTERNET", "CABLE_TV", "VEHICLE_CARD", 
            "ELEVATOR_CARD", "RESIDENT_CARD"
        );
    }

    public List<InvoiceLineResponseDto> getPaidInvoicesForCurrentMonth(UUID userId, UUID unitId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime firstDayOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusNanos(1);

        log.info("📋 [InvoiceService] Getting paid invoices for current month - unitId: {}, date range: {} to {}", 
                unitId, firstDayOfMonth, lastDayOfMonth);

        // Get all invoices for this user/unit
        List<InvoiceLineResponseDto> allInvoices = getMyInvoices(userId, unitId, null);

        // Filter: Only PAID invoices in current month
        List<InvoiceLineResponseDto> paidInvoices = allInvoices.stream()
                .filter(invoice -> {
                    boolean isPaid = InvoiceStatus.PAID.name().equals(invoice.getStatus());
                    if (!isPaid || invoice.getPaidAt() == null) return false;

                    OffsetDateTime paidAt = invoice.getPaidAt();
                    return !paidAt.isBefore(firstDayOfMonth) && !paidAt.isAfter(lastDayOfMonth);
                })
                .collect(Collectors.toList());

        log.info("✅ [InvoiceService] Found {} paid invoices in current month for unit {}", 
                paidInvoices.size(), unitId);

        return paidInvoices;
    }
}

