package com.QhomeBase.datadocsservice.controller;

import com.QhomeBase.datadocsservice.dto.*;
import com.QhomeBase.datadocsservice.service.ContractService;
import com.QhomeBase.datadocsservice.dto.RenewContractRequest;
import com.QhomeBase.datadocsservice.service.PdfFieldMapper;
import com.QhomeBase.datadocsservice.service.PdfFormFillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Contracts", description = "Contract management APIs")
public class ContractController {

    private final ContractService contractService;
    private final PdfFormFillService pdfFormFillService;

    @PostMapping
    @Operation(summary = "Create contract", description = "Create a new contract")
    public ResponseEntity<ContractDto> createContract(
            @Valid @RequestBody CreateContractRequest request,
            @RequestParam(value = "createdBy", required = false) UUID createdBy) {
        
        if (createdBy == null) {
            createdBy = UUID.randomUUID();
        }
        
        ContractDto contract = contractService.createContract(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(contract);
    }

    @PutMapping("/{contractId}")
    @Operation(summary = "Update contract", description = "Update an existing contract")
    public ResponseEntity<ContractDto> updateContract(
            @PathVariable UUID contractId,
            @Valid @RequestBody UpdateContractRequest request,
            @RequestParam(value = "updatedBy", required = false) UUID updatedBy) {
        
        if (updatedBy == null) {
            updatedBy = UUID.randomUUID();
        }
        
        ContractDto contract = contractService.updateContract(contractId, request, updatedBy);
        return ResponseEntity.ok(contract);
    }

    @GetMapping("/{contractId}")
    @Operation(summary = "Get contract", description = "Get contract by ID")
    public ResponseEntity<ContractDto> getContract(
            @PathVariable UUID contractId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "skipRenewalReminder", required = false, defaultValue = "false") boolean skipRenewalReminder) {
        String accessToken = extractAccessToken(headers);
        UUID userId = extractUserIdFromHeaders(headers);
        ContractDto contract = contractService.getContractById(contractId, userId, accessToken, skipRenewalReminder);
        return ResponseEntity.ok(contract);
    }

    @GetMapping("/unit/{unitId}")
    @Operation(summary = "Get contracts by unit", description = "Get all contracts for a specific unit")
    public ResponseEntity<List<ContractDto>> getContractsByUnit(
            @PathVariable UUID unitId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "skipRenewalReminder", required = false, defaultValue = "false") boolean skipRenewalReminder) {
        String accessToken = extractAccessToken(headers);
        UUID userId = extractUserIdFromHeaders(headers);
        List<ContractDto> contracts = contractService.getContractsByUnitId(unitId, userId, accessToken, skipRenewalReminder);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active contracts", description = "Get all active contracts")
    public ResponseEntity<List<ContractDto>> getActiveContracts() {
        List<ContractDto> contracts = contractService.getActiveContracts();
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/unit/{unitId}/active")
    @Operation(summary = "Get active contracts by unit", description = "Get active contracts for a specific unit")
    public ResponseEntity<List<ContractDto>> getActiveContractsByUnit(
            @PathVariable UUID unitId,
            @RequestHeader HttpHeaders headers) {
        String accessToken = extractAccessToken(headers);
        UUID userId = extractUserIdFromHeaders(headers);
        List<ContractDto> contracts = contractService.getActiveContractsByUnit(unitId, userId, accessToken);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all contracts", description = "Get all contracts (optional filter by contractType)")
    public ResponseEntity<List<ContractDto>> getAllContracts(
            @RequestParam(value = "contractType", required = false) String contractType) {
        try {
            log.info("Getting all contracts with contractType filter: {}", contractType);
            List<ContractDto> contracts;
            if (contractType != null && !contractType.isEmpty()) {
                contracts = contractService.getContractsByType(contractType);
            } else {
                contracts = contractService.getAllContracts();
            }
            log.info("Found {} contracts", contracts.size());
            return ResponseEntity.ok(contracts);
        } catch (IllegalArgumentException ex) {
            log.error("Invalid request: {}", ex.getMessage(), ex);
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("Error getting contracts: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{contractId}")
    @Operation(summary = "Delete contract", description = "Delete a contract")
    public ResponseEntity<Void> deleteContract(@PathVariable UUID contractId) {
        contractService.deleteContract(contractId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{contractId}/checkout")
    @Operation(summary = "Checkout contract", description = "Set checkout date for a contract and change status to CANCELLED. Checkout date must be less than end date.")
    public ResponseEntity<ContractDto> checkoutContract(
            @PathVariable UUID contractId,
            @RequestParam("checkoutDate") java.time.LocalDate checkoutDate,
            @RequestParam(value = "updatedBy", required = false) UUID updatedBy) {
        
        if (updatedBy == null) {
            updatedBy = UUID.randomUUID();
        }
        
        ContractDto contract = contractService.checkoutContract(contractId, checkoutDate, updatedBy);
        return ResponseEntity.ok(contract);
    }

    @PutMapping("/activate-inactive")
    @Operation(summary = "Activate inactive contracts", description = "Activate all contracts with status INACTIVE and startDate = today")
    public ResponseEntity<Map<String, Object>> activateInactiveContracts() {
        int activatedCount = contractService.activateInactiveContracts();
        return ResponseEntity.ok(Map.of(
            "message", "Activated " + activatedCount + " contract(s)",
            "activatedCount", activatedCount
        ));
    }

    @PutMapping("/{contractId}/extend")
    @Operation(summary = "Extend contract", description = "Extend a RENTAL contract by updating the end date. This resets the renewal status to PENDING.")
    public ResponseEntity<ContractDto> extendContract(
            @PathVariable UUID contractId,
            @RequestParam("newEndDate") java.time.LocalDate newEndDate,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "updatedBy", required = false) UUID updatedBy) {
        String accessToken = extractAccessToken(headers);
        UUID userId = extractUserIdFromHeaders(headers);
        
        if (updatedBy == null) {
            updatedBy = userId != null ? userId : UUID.randomUUID();
        }
        
        ContractDto contract = contractService.extendContract(contractId, newEndDate, updatedBy, userId, accessToken);
        return ResponseEntity.ok(contract);
    }

    @PostMapping("/renewal/trigger-reminders")
    @Operation(summary = "Trigger renewal reminders manually", description = "Manually trigger the renewal reminder job (for testing). " +
            "Sends reminders based on current date: " +
            "- Reminder 1: 28-32 days before endDate " +
            "- Reminder 2: Day 8 of endDate month " +
            "- Reminder 3: Day 20 of endDate month (FINAL)")
    public ResponseEntity<Map<String, Object>> triggerRenewalReminders() {
        log.info("Manual trigger: Send renewal reminders via ContractService.triggerRenewalReminders()");
        
        // Use the updated logic in ContractService that checks day of month
        contractService.triggerRenewalReminders();
        
        // Note: The actual counts are logged in ContractService.triggerRenewalReminders()
        // We return a success message here
        java.time.LocalDate today = java.time.LocalDate.now();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Renewal reminders triggered manually. Check logs for details.",
            "currentDate", today.toString(),
            "note", "Reminder 2 is sent on day 8 of endDate month, Reminder 3 is sent on day 20 of endDate month. " +
                    "Today is day " + today.getDayOfMonth() + ", so reminder 3 will only be sent on day 20."
        ));
    }
    
    @PostMapping("/renewal/trigger-reminder-3-test")
    @Operation(summary = "Force trigger reminder 3 for testing", description = "Force send reminder 3 for contracts that have sent reminder 1, " +
            "regardless of current date. This is for testing purposes only.")
    public ResponseEntity<Map<String, Object>> triggerReminder3ForTesting() {
        log.info("Manual trigger: Force send reminder 3 for testing");
        java.time.LocalDate today = java.time.LocalDate.now();
        
        List<com.QhomeBase.datadocsservice.model.Contract> contracts = contractService.findContractsNeedingRenewalReminder();
        int thirdReminderCount = 0;
        
        for (com.QhomeBase.datadocsservice.model.Contract contract : contracts) {
            try {
                if (contract.getEndDate() != null 
                        && "REMINDED".equals(contract.getRenewalStatus())
                        && contract.getRenewalReminderSentAt() != null
                        && contract.getEndDate().getYear() == today.getYear()
                        && contract.getEndDate().getMonth() == today.getMonth()
                        && java.time.temporal.ChronoUnit.DAYS.between(today, contract.getEndDate()) > 0
                        && java.time.temporal.ChronoUnit.DAYS.between(today, contract.getEndDate()) < 30) {
                    
                    // Force send reminder 3 for testing (bypass day 20 check)
                    contractService.sendRenewalReminder(contract.getId());
                    thirdReminderCount++;
                    log.info("✅ [TEST] Force sent THIRD (FINAL) renewal reminder for contract {} (expires on {})", 
                            contract.getContractNumber(), contract.getEndDate());
                }
            } catch (Exception e) {
                log.error("Error force sending reminder 3 for contract {}", contract.getId(), e);
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Force triggered reminder 3 for testing",
            "thirdRemindersSent", thirdReminderCount,
            "note", "This bypasses the day 20 check. For production, reminder 3 is only sent on day 20 of endDate month."
        ));
    }

    @PostMapping("/renewal/trigger-declined")
    @Operation(summary = "Trigger mark renewal declined manually", description = "Manually trigger the job to mark contracts as renewal declined (for testing). This marks contracts that have passed the 20-day deadline.")
    public ResponseEntity<Map<String, Object>> triggerMarkRenewalDeclined() {
        log.info("Manual trigger: Mark renewal declined");
        java.time.OffsetDateTime deadlineDate = java.time.OffsetDateTime.now().minusDays(20);
        
        List<com.QhomeBase.datadocsservice.model.Contract> contracts = contractService.findContractsWithRenewalDeclined(deadlineDate);
        
        int declinedCount = 0;
        for (com.QhomeBase.datadocsservice.model.Contract contract : contracts) {
            try {
                if ("REMINDED".equals(contract.getRenewalStatus())) {
                    long daysSinceFirstReminder = java.time.temporal.ChronoUnit.DAYS.between(
                        contract.getRenewalReminderSentAt().toLocalDate(),
                        java.time.LocalDate.now()
                    );
                    
                    if (daysSinceFirstReminder >= 20) {
                        contractService.markRenewalDeclined(contract.getId());
                        declinedCount++;
                        log.info("Marked contract {} as renewal declined (reminder sent on {}, {} days ago - deadline passed)", 
                                contract.getContractNumber(), contract.getRenewalReminderSentAt(), daysSinceFirstReminder);
                    }
                }
            } catch (Exception e) {
                log.error("Error marking contract {} as renewal declined", contract.getId(), e);
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Mark renewal declined triggered manually",
            "declinedCount", declinedCount
        ));
    }

    @PostMapping("/status/trigger-expired")
    @Operation(summary = "Trigger mark expired contracts manually", description = "Manually trigger the job to mark expired contracts (for testing). This marks ACTIVE contracts with endDate < today as EXPIRED.")
    public ResponseEntity<Map<String, Object>> triggerMarkExpiredContracts() {
        log.info("Manual trigger: Mark expired contracts");
        int expiredCount = contractService.markExpiredContracts();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Mark expired contracts triggered manually",
            "contractsMarkedExpired", expiredCount
        ));
    }

    @PutMapping("/{contractId}/renewal/decline")
    @Operation(summary = "Decline contract renewal manually", description = "Manually decline renewal for a contract. This marks the contract's renewal status as DECLINED immediately, even if it's still within the reminder period. Contract remains ACTIVE until endDate.")
    public ResponseEntity<Map<String, Object>> declineRenewal(
            @PathVariable UUID contractId,
            @RequestParam(value = "updatedBy", required = false) UUID updatedBy) {
        try {
            contractService.markRenewalDeclined(contractId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Contract renewal declined successfully",
                "contractId", contractId.toString()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error declining renewal for contract {}", contractId, e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to decline renewal: " + e.getMessage()
            ));
        }
    }
    
    @PutMapping("/{contractId}/dismiss-reminder")
    @Operation(summary = "Dismiss renewal reminder", description = "Mark that user has dismissed the current reminder. User won't see this reminder again until next reminder count. Only works for reminder 1 and 2, not final reminder.")
    public ResponseEntity<Map<String, Object>> dismissReminder(
            @PathVariable UUID contractId,
            @RequestParam(value = "updatedBy", required = false) UUID updatedBy) {
        try {
            contractService.dismissReminder(contractId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Reminder dismissed successfully",
                "contractId", contractId.toString()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error dismissing reminder for contract {}", contractId, e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to dismiss reminder: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{contractId}/files")
    @Operation(summary = "Upload contract file", description = "Upload a file (PDF, images) for a contract")
    public ResponseEntity<ContractFileDto> uploadContractFile(
            @PathVariable UUID contractId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", required = false, defaultValue = "false") Boolean isPrimary,
            @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy) {
        
        if (uploadedBy == null) {
            uploadedBy = UUID.randomUUID();
        }
        
        ContractFileDto fileDto = contractService.uploadContractFile(contractId, file, uploadedBy, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(fileDto);
    }

    @PostMapping("/{contractId}/files/multiple")
    @Operation(summary = "Upload multiple contract files", description = "Upload multiple files for a contract")
    public ResponseEntity<List<ContractFileDto>> uploadContractFiles(
            @PathVariable UUID contractId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy) {
        
        if (uploadedBy == null) {
            uploadedBy = UUID.randomUUID();
        }
        
        List<ContractFileDto> fileDtos = new java.util.ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            Boolean isPrimary = (i == 0);
            ContractFileDto fileDto = contractService.uploadContractFile(
                    contractId, files[i], uploadedBy, isPrimary);
            fileDtos.add(fileDto);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(fileDtos);
    }

    @GetMapping("/{contractId}/files")
    @Operation(summary = "Get contract files", description = "Get all files for a contract")
    public ResponseEntity<List<ContractFileDto>> getContractFiles(@PathVariable UUID contractId) {
        List<ContractFileDto> files = contractService.getContractFiles(contractId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{contractId}/files/{fileId}/view")
    @Operation(summary = "View contract file", description = "View contract file inline in browser")
    public ResponseEntity<Resource> viewContractFile(
            @PathVariable UUID contractId,
            @PathVariable UUID fileId,
            HttpServletRequest request) {
        
        Resource resource = contractService.viewContractFile(contractId, fileId);
        
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }
        
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{contractId}/files/{fileId}/download")
    @Operation(summary = "Download contract file", description = "Download contract file")
    public ResponseEntity<Resource> downloadContractFile(
            @PathVariable UUID contractId,
            @PathVariable UUID fileId,
            HttpServletRequest request) {
        
        Resource resource = contractService.downloadContractFile(contractId, fileId);
        
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }
        
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{contractId}/files/{fileId}")
    @Operation(summary = "Delete contract file", description = "Delete a contract file")
    public ResponseEntity<Void> deleteContractFile(
            @PathVariable UUID contractId,
            @PathVariable UUID fileId) {
        
        contractService.deleteContractFile(contractId, fileId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{contractId}/files/{fileId}/primary")
    @Operation(summary = "Set primary file", description = "Set a contract file as primary")
    public ResponseEntity<ContractFileDto> setPrimaryFile(
            @PathVariable UUID contractId,
            @PathVariable UUID fileId) {
        
        ContractFileDto fileDto = contractService.setPrimaryFile(contractId, fileId);
        return ResponseEntity.ok(fileDto);
    }

    // ===== Export contract to PDF and store as contract file =====
    @PostMapping("/{contractId}/export-pdf")
    @Operation(summary = "Export contract PDF from template and store as contract file")
    public ResponseEntity<?> exportContractPdf(
            @PathVariable UUID contractId,
            @RequestParam(defaultValue = "templates/contract_template.pdf") String templatePath,
            @RequestParam(defaultValue = "contract.pdf") String filename,
            @RequestParam(defaultValue = "true") boolean flatten,
            @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy,
            @RequestBody(required = false) BuyerRequest buyer
    ) {
        if (uploadedBy == null) uploadedBy = UUID.randomUUID();
        log.info("[ExportPDF] contractId={}, templatePath={}, filename={}, flatten={}",
                contractId, templatePath, filename, flatten);
        try {
            // 1) Load contract
            ContractDto contract = contractService.getContractById(contractId);

            // 2) Map fields for PDF
            PdfFieldMapper.BuyerInfo buyerInfo = buyer == null ? null : new PdfFieldMapper.BuyerInfo(
                    buyer.name(), buyer.idNo(), buyer.idDate(), buyer.idPlace(),
                    buyer.residence(), buyer.address(), buyer.phone(), buyer.fax(),
                    buyer.bankAcc(), buyer.bankName(), buyer.taxCode()
            );
            Map<String, String> fields = PdfFieldMapper.mapFromContract(contract, buyerInfo);

            // 3) Fill PDF
            byte[] pdfBytes = pdfFormFillService.fillTemplate(templatePath, fields, flatten);

            // 4) Store as contract file
            MultipartFile file = new InMemoryMultipartFile(filename, filename, "application/pdf", pdfBytes);
            ContractFileDto fileDto = contractService.uploadContractFile(contractId, file, uploadedBy, false);
            log.info("[ExportPDF] Exported and stored file {}", fileDto.getFileName());
            return ResponseEntity.status(HttpStatus.CREATED).body(fileDto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.error("[ExportPDF] Bad request: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
        } catch (Exception ex) {
            log.error("[ExportPDF] Unexpected error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error"));
        }
    }

    public record BuyerRequest(
            String name,
            String idNo,
            String idDate,
            String idPlace,
            String residence,
            String address,
            String phone,
            String fax,
            String bankAcc,
            String bankName,
            String taxCode
    ) {}

    static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content.clone(); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
    }

    @GetMapping("/unit/{unitId}/popup")
    @Operation(summary = "Get contracts needing popup", description = "Get contracts that need to show popup to resident (renewal reminders)")
    public ResponseEntity<List<ContractDto>> getContractsNeedingPopup(
            @PathVariable UUID unitId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "skipRenewalReminder", required = false, defaultValue = "false") boolean skipRenewalReminder) {
        String accessToken = extractAccessToken(headers);
        UUID userId = extractUserIdFromHeaders(headers);
        List<ContractDto> contracts = contractService.getContractsNeedingPopup(unitId, userId, accessToken, skipRenewalReminder);
        return ResponseEntity.ok(contracts);
    }

    @PostMapping("/{contractId}/renew")
    @Operation(summary = "Renew contract", description = "Create a new contract based on old contract for renewal")
    public ResponseEntity<ContractDto> renewContract(
            @PathVariable UUID contractId,
            @Valid @RequestBody RenewContractRequest request,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "createdBy", required = false) UUID createdBy) {
        
        String accessToken = extractAccessToken(headers);
        UUID userId = extractUserIdFromHeaders(headers);
        
        if (createdBy == null) {
            createdBy = userId != null ? userId : UUID.randomUUID();
        }
        
        ContractDto contract = contractService.renewContract(
                contractId,
                request.getStartDate(),
                request.getEndDate(),
                createdBy,
                userId,
                accessToken
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(contract);
    }

    @PutMapping("/{contractId}/cancel")
    @Operation(summary = "Cancel contract", description = "Cancel a contract (set status to CANCELLED). Optionally provide scheduledDate for asset inspection.")
    public ResponseEntity<ContractDto> cancelContract(
            @PathVariable UUID contractId,
            @RequestBody(required = false) CancelContractRequest request,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "updatedBy", required = false) UUID updatedBy) {
        
        String accessToken = extractAccessToken(headers);
        UUID userId = extractUserIdFromHeaders(headers);
        
        if (updatedBy == null) {
            updatedBy = userId != null ? userId : UUID.randomUUID();
        }
        
        java.time.LocalDate scheduledDate = request != null ? request.scheduledDate() : null;
        ContractDto contract = contractService.cancelContract(contractId, updatedBy, scheduledDate, userId, accessToken);
        return ResponseEntity.ok(contract);
    }

    @PostMapping("/{contractId}/renew/payment")
    @Operation(summary = "Create payment URL for contract renewal", description = "Create VNPay payment URL for contract renewal")
    public ResponseEntity<ContractRenewalResponse> createRenewalPaymentUrl(
            @PathVariable UUID contractId,
            @Valid @RequestBody RenewContractRequest request,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "createdBy", required = false) UUID createdBy,
            HttpServletRequest httpRequest) {
        
        String accessToken = extractAccessToken(headers);
        UUID userId = extractUserIdFromHeaders(headers);
        
        if (createdBy == null) {
            createdBy = userId != null ? userId : UUID.randomUUID();
        }
        
        String clientIp = getClientIp(httpRequest);
        ContractRenewalResponse response = contractService.createRenewalPaymentUrl(
                contractId,
                request.getStartDate(),
                request.getEndDate(),
                createdBy,
                clientIp,
                userId,
                accessToken
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay/callback")
    @Operation(summary = "VNPay callback", description = "Handle VNPay payment callback for contract renewal")
    public void handleVnpayCallback(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "contractId", required = false) UUID contractId) throws IOException {
        try {
            Map<String, String> params = contractService.extractVnpayParams(request);
            
            // If contractId not provided, try to extract from orderInfo or txnRef
            if (contractId == null) {
                log.error("VNPay callback error: contractId parameter is required");
                String fallbackUrl = "qhomeapp://vnpay-result?success=false&message=contractId+parameter+is+required";
                response.sendRedirect(fallbackUrl);
                return;
            }
            
            ContractDto contract = contractService.handleVnpayCallback(params, contractId);
            
            // Redirect to app with success result
            String responseCode = params.get("vnp_ResponseCode");
            String redirectUrl = new StringBuilder("qhomeapp://vnpay-result")
                    .append("?contractId=").append(contract.getId())
                    .append("&responseCode=").append(responseCode != null ? responseCode : "")
                    .append("&success=true")
                    .append("&message=Thanh+toan+thanh+cong")
                    .toString();
            
            log.info("✅ [ContractController] VNPay callback success, redirecting to app: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
        } catch (IllegalArgumentException e) {
            log.error("VNPay callback error: {}", e.getMessage());
            String fallbackUrl = "qhomeapp://vnpay-result?success=false&message=" + 
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(fallbackUrl);
        } catch (Exception e) {
            log.error("Unexpected error in VNPay callback", e);
            String fallbackUrl = "qhomeapp://vnpay-result?success=false&message=Internal+server+error";
            response.sendRedirect(fallbackUrl);
        }
    }

    @PostMapping("/{contractId}/renew/complete")
    @Operation(summary = "Complete contract renewal payment", description = "Complete contract renewal after successful payment")
    public ResponseEntity<ContractDto> completeRenewalPayment(
            @PathVariable UUID contractId,
            @RequestParam("residentId") UUID residentId,
            @RequestParam(value = "vnpayTransactionRef", required = false) String vnpayTransactionRef) {
        
        ContractDto contract = contractService.completeRenewalPayment(contractId, residentId, vnpayTransactionRef);
        return ResponseEntity.ok(contract);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String extractAccessToken(HttpHeaders headers) {
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private UUID extractUserIdFromHeaders(HttpHeaders headers) {
        try {
            // Try to get from SecurityContext first (if available)
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() != null) {
                    String principalStr = auth.getPrincipal().toString();
                    // Try to extract UUID from principal string if it's a UUID
                    if (principalStr.length() == 36) {
                        try {
                            return UUID.fromString(principalStr);
                        } catch (IllegalArgumentException e) {
                            // Not a UUID, continue to JWT parsing
                        }
                    }
                }
            } catch (Exception e) {
                // SecurityContext not available, continue to JWT parsing
                log.debug("SecurityContext not available, trying JWT parsing");
            }
            
            // Fallback: try to parse from JWT token in Authorization header
            String token = extractAccessToken(headers);
            if (token != null && !token.isEmpty()) {
                // Simple JWT parsing - extract from payload
                // Note: This is a simplified version. For production, use proper JWT library
                String[] parts = token.split("\\.");
                if (parts.length == 3) {
                    try {
                        // Decode payload (base64 URL-safe)
                        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                        log.info("🔍 [ContractController] JWT payload: {}", payload);
                        
                        // Try multiple fields: uid, userId, user_id, sub (in that order)
                        String[] fieldsToCheck = {"uid", "userId", "user_id", "sub"};
                        for (String field : fieldsToCheck) {
                            String fieldPattern = "\"" + field + "\"";
                            if (payload.contains(fieldPattern)) {
                                log.info("🔍 [ContractController] Found field '{}' in JWT payload", field);
                                int fieldIndex = payload.indexOf(fieldPattern);
                                int start = fieldIndex + fieldPattern.length();
                                
                            // Skip whitespace and colon
                            while (start < payload.length() && (payload.charAt(start) == ' ' || payload.charAt(start) == ':')) {
                                start++;
                            }
                                
                            // Skip opening quote if present
                            if (start < payload.length() && payload.charAt(start) == '"') {
                                start++;
                            }
                                
                                // Find end of value (quote, comma, or closing brace)
                            int end = start;
                            while (end < payload.length() && payload.charAt(end) != '"' && payload.charAt(end) != ',' && payload.charAt(end) != '}') {
                                end++;
                            }
                                
                            if (end > start) {
                                String userIdStr = payload.substring(start, end);
                                    log.info("🔍 [ContractController] Extracted userId from field '{}': {}", field, userIdStr);
                                    
                                    // Try to parse as UUID
                                    try {
                                        UUID userId = UUID.fromString(userIdStr);
                                        log.info("✅ [ContractController] Successfully extracted userId from JWT field '{}': {}", field, userId);
                                        return userId;
                                    } catch (IllegalArgumentException e) {
                                        log.warn("⚠️ [ContractController] Failed to parse userId string '{}' as UUID: {}", userIdStr, e.getMessage());
                                    }
                                } else {
                                    log.warn("⚠️ [ContractController] Found field '{}' but could not extract value", field);
                                }
                            }
                        }
                        
                        // If no field found, try to find any UUID-like string in payload
                        log.warn("⚠️ [ContractController] Could not find uid, userId, user_id, or sub field in JWT payload");
                        log.warn("⚠️ [ContractController] Attempting to find UUID pattern in payload...");
                        
                        // Try to find UUID pattern (8-4-4-4-12 format)
                        java.util.regex.Pattern uuidPattern = java.util.regex.Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", java.util.regex.Pattern.CASE_INSENSITIVE);
                        java.util.regex.Matcher matcher = uuidPattern.matcher(payload);
                        if (matcher.find()) {
                            String uuidStr = matcher.group();
                            log.warn("⚠️ [ContractController] Found UUID pattern in payload: {} (may not be userId)", uuidStr);
                            try {
                                UUID userId = UUID.fromString(uuidStr);
                                log.warn("⚠️ [ContractController] Using UUID pattern as userId: {} (verify this is correct)", userId);
                                return userId;
                            } catch (IllegalArgumentException e) {
                                log.error("❌ [ContractController] Failed to parse UUID pattern: {}", uuidStr);
                            }
                        }
                    } catch (Exception e) {
                        log.error("❌ [ContractController] Failed to parse JWT token: {}", e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ [ContractController] Could not extract userId from headers: {}", e.getMessage());
        }
        return null;
    }

    record ErrorResponse(int status, String message) {}
}

