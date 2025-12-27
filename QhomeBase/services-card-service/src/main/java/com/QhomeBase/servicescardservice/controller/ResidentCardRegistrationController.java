package com.QhomeBase.servicescardservice.controller;

import com.QhomeBase.servicescardservice.dto.BatchCardPaymentRequest;
import com.QhomeBase.servicescardservice.dto.CardRegistrationAdminDecisionRequest;
import com.QhomeBase.servicescardservice.dto.ResidentCardRegistrationCreateDto;
import com.QhomeBase.servicescardservice.dto.ResidentCardRegistrationDto;
import com.QhomeBase.servicescardservice.service.ResidentCardRegistrationService;
import com.QhomeBase.servicescardservice.service.ResidentCardRegistrationService.ResidentCardPaymentResponse;
import com.QhomeBase.servicescardservice.service.ResidentCardRegistrationService.ResidentCardPaymentResult;
import com.QhomeBase.servicescardservice.service.vnpay.VnpayService;
import com.QhomeBase.servicescardservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/resident-card")
@RequiredArgsConstructor
@Slf4j
public class ResidentCardRegistrationController {

    private final ResidentCardRegistrationService registrationService;
    private final JwtUtil jwtUtil;
    private final VnpayService vnpayService;

    @GetMapping("/admin/registrations")
    public ResponseEntity<?> getRegistrationsForAdmin(@RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String paymentStatus,
                                                      @RequestHeader HttpHeaders headers) {
        UUID adminId = jwtUtil.getUserIdFromHeaders(headers);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            // Mặc định chỉ lấy những thẻ có status = PENDING nếu không có query param
            String finalStatus = (status != null && !status.isBlank()) ? status.trim() : "PENDING";
            String finalPaymentStatus = (paymentStatus != null && !paymentStatus.isBlank()) ? paymentStatus.trim() : null;
            
            return ResponseEntity.ok(
                    registrationService.getRegistrationsForAdmin(finalStatus, finalPaymentStatus)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Lỗi tải danh sách đăng ký", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể lấy danh sách đăng ký"));
        }
    }

    @GetMapping("/admin/registrations/{registrationId}")
    public ResponseEntity<?> getRegistrationForAdmin(@PathVariable String registrationId,
                                                     @RequestHeader HttpHeaders headers) {
        UUID adminId = jwtUtil.getUserIdFromHeaders(headers);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            UUID regUuid = UUID.fromString(registrationId);
            ResidentCardRegistrationDto dto = registrationService.getRegistrationForAdmin(regUuid);
            return ResponseEntity.ok(toResponse(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/admin/registrations/{registrationId}/decision")
    public ResponseEntity<?> processAdminDecision(@PathVariable String registrationId,
                                                  @Valid @RequestBody CardRegistrationAdminDecisionRequest request,
                                                  @RequestHeader HttpHeaders headers) {
        UUID adminId = jwtUtil.getUserIdFromHeaders(headers);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            UUID regUuid = UUID.fromString(registrationId);
            ResidentCardRegistrationDto dto = registrationService.processAdminDecision(adminId, regUuid, request);
            return ResponseEntity.ok(toResponse(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{registrationId}/resume-payment")
    public ResponseEntity<?> resumePayment(@PathVariable String registrationId,
                                          @RequestHeader HttpHeaders headers,
                                          HttpServletRequest request) {
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            log.warn("⚠️ [ResidentCard] Unauthorized request to resumePayment");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        
        try {
            UUID regUuid = UUID.fromString(registrationId);
            log.debug("🔍 [ResidentCard] resumePayment request: registrationId={}, userId={}", regUuid, userId);
            
            ResidentCardPaymentResponse response = registrationService.initiatePayment(userId, regUuid, request);
            Map<String, Object> body = new HashMap<>();
            body.put("registrationId", response.registrationId() != null ? response.registrationId().toString() : null);
            body.put("paymentUrl", response.paymentUrl());
            
            log.info("✅ [ResidentCard] resumePayment success: registrationId={}", regUuid);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [ResidentCard] Invalid argument: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("⚠️ [ResidentCard] IllegalStateException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Lỗi tiếp tục thanh toán", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể tiếp tục thanh toán: " + e.getMessage()));
        }
    }

    @PostMapping("/batch-payment")
    public ResponseEntity<?> batchPayment(@Valid @RequestBody BatchCardPaymentRequest request,
                                         @RequestHeader HttpHeaders headers,
                                         HttpServletRequest httpRequest) {
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            log.warn("⚠️ [ResidentCard] Unauthorized request to batchPayment");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        
        try {
            log.info("📥 [ResidentCard] Batch payment request: unitId={}, registrationIds={}", 
                    request.unitId(), request.registrationIds().size());
            
            ResidentCardPaymentResponse response = registrationService.batchInitiatePayment(userId, request, httpRequest);
            Map<String, Object> body = new HashMap<>();
            body.put("registrationId", response.registrationId() != null ? response.registrationId().toString() : null);
            body.put("paymentUrl", response.paymentUrl());
            body.put("cardCount", request.registrationIds().size());
            
            log.info("✅ [ResidentCard] Batch payment initiated: {} cards", request.registrationIds().size());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [ResidentCard] Invalid argument: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("⚠️ [ResidentCard] IllegalStateException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Lỗi batch payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể khởi tạo thanh toán batch: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createRegistration(@Valid @RequestBody ResidentCardRegistrationCreateDto dto,
                                                @RequestHeader HttpHeaders headers) {
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            String accessToken = extractAccessToken(headers);
            ResidentCardRegistrationDto created = registrationService.createRegistration(userId, dto, accessToken);
            Map<String, Object> body = new HashMap<>();
            body.put("id", created.id() != null ? created.id().toString() : null);
            body.put("status", created.status());
            body.put("paymentStatus", created.paymentStatus());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Lỗi tạo đăng ký", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể tạo đăng ký thẻ cư dân: " + e.getMessage()));
        }
    }

    @PostMapping("/vnpay-url")
    public ResponseEntity<?> createRegistrationAndPay(@Valid @RequestBody ResidentCardRegistrationCreateDto dto,
                                                      @RequestHeader HttpHeaders headers,
                                                      HttpServletRequest request) {
        log.info("📥 [ResidentCard] Nhận request tạo đăng ký và thanh toán: unitId={}, residentId={}", 
                dto.unitId(), dto.residentId());
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            log.warn("⚠️ [ResidentCard] Unauthorized: không tìm thấy userId trong headers");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        log.info("✅ [ResidentCard] UserId: {}", userId);
        try {
            String accessToken = extractAccessToken(headers);
            ResidentCardPaymentResponse response = registrationService.createAndInitiatePayment(userId, dto, request, accessToken);
            Map<String, Object> body = new HashMap<>();
            body.put("registrationId", response.registrationId() != null ? response.registrationId().toString() : null);
            body.put("paymentUrl", response.paymentUrl());
            log.info("✅ [ResidentCard] Tạo đăng ký thành công: registrationId={}", response.registrationId());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("⚠️ [ResidentCard] Lỗi validation/state: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Lỗi tạo đăng ký", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể khởi tạo đăng ký thẻ cư dân: " + e.getMessage()));
        }
    }

    @GetMapping("/household-members")
    public ResponseEntity<?> getHouseholdMembers(@RequestParam UUID unitId,
                                                @RequestHeader HttpHeaders headers) {
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            log.warn("⚠️ [ResidentCard] Unauthorized request to getHouseholdMembers");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        
        try {
            log.debug("🔍 [ResidentCard] getHouseholdMembers request: unitId={}, userId={}", unitId, userId);
            String accessToken = extractAccessToken(headers);
            List<Map<String, Object>> members = registrationService.getHouseholdMembersByUnit(unitId, userId, accessToken);
            log.info("✅ [ResidentCard] getHouseholdMembers success: {} members", members.size());
            return ResponseEntity.ok(members);
        } catch (IllegalStateException e) {
            log.warn("⚠️ [ResidentCard] Permission denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Lỗi lấy danh sách thành viên", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể lấy danh sách thành viên: " + e.getMessage()));
        }
    }

    @GetMapping("/{registrationId}")
    public ResponseEntity<?> getRegistration(@PathVariable String registrationId,
                                             @RequestHeader HttpHeaders headers) {
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            UUID registrationUuid = UUID.fromString(registrationId);
            ResidentCardRegistrationDto dto = registrationService.getRegistration(userId, registrationUuid);
            return ResponseEntity.ok(toResponse(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{registrationId}/cancel")
    public ResponseEntity<?> cancelRegistration(@PathVariable String registrationId,
                                                @RequestHeader HttpHeaders headers) {
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            UUID registrationUuid = UUID.fromString(registrationId);
            registrationService.cancelRegistration(userId, registrationUuid);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<?> handleVnpayReturn(HttpServletRequest request) {
        Map<String, String> params = vnpayService.extractParams(request);
        try {
            ResidentCardPaymentResult result = registrationService.handleVnpayCallback(params);
            Map<String, Object> body = buildVnpayResponse(result, params);
            HttpStatus status = result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(body);
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Lỗi xử lý callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/vnpay/redirect")
    public ResponseEntity<?> redirectAfterPayment(HttpServletRequest request,
                                                  HttpServletResponse response) throws IOException {
        Map<String, String> params = vnpayService.extractParams(request);
        ResidentCardPaymentResult result;
        try {
            result = registrationService.handleVnpayCallback(params);
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Lỗi xử lý callback redirect", e);
            // URL encode message to avoid Unicode characters in HTTP header
            String encodedMessage = java.net.URLEncoder.encode(
                    e.getMessage() != null ? e.getMessage() : "Unknown error",
                    java.nio.charset.StandardCharsets.UTF_8
            );
            String fallback = "qhomeapp://vnpay-resident-card-result?success=false&message=" + encodedMessage;
            response.sendRedirect(fallback);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }

        Map<String, Object> body = buildVnpayResponse(result, params);
        String registrationId = result.registrationId() != null ? result.registrationId().toString() : "";
        String responseCode = result.responseCode() != null 
                ? java.net.URLEncoder.encode(result.responseCode(), java.nio.charset.StandardCharsets.UTF_8)
                : "";
        String message = result.message() != null 
                ? java.net.URLEncoder.encode(result.message(), java.nio.charset.StandardCharsets.UTF_8)
                : "";
        String requestType = result.requestType() != null 
                ? java.net.URLEncoder.encode(result.requestType(), java.nio.charset.StandardCharsets.UTF_8)
                : "";
        
        String redirectUrl = new StringBuilder("qhomeapp://vnpay-resident-card-result")
                .append("?registrationId=").append(registrationId)
                .append("&responseCode=").append(responseCode)
                .append("&success=").append(result.success())
                .append("&requestType=").append(requestType)
                .append("&message=").append(message)
                .toString();
        response.sendRedirect(redirectUrl);
        HttpStatus status = result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> toResponse(ResidentCardRegistrationDto dto) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", dto.id() != null ? dto.id().toString() : null);
        body.put("userId", dto.userId() != null ? dto.userId().toString() : null);
        body.put("unitId", dto.unitId() != null ? dto.unitId().toString() : null);
        body.put("requestType", dto.requestType());
        body.put("residentId", dto.residentId() != null ? dto.residentId().toString() : null);
        body.put("fullName", dto.fullName());
        body.put("apartmentNumber", dto.apartmentNumber());
        body.put("buildingName", dto.buildingName());
        body.put("citizenId", dto.citizenId());
        body.put("phoneNumber", dto.phoneNumber());
        body.put("note", dto.note());
        body.put("status", dto.status());
        body.put("paymentStatus", dto.paymentStatus());
        body.put("paymentAmount", dto.paymentAmount());
        body.put("paymentDate", dto.paymentDate());
        body.put("paymentGateway", dto.paymentGateway());
        body.put("vnpayTransactionRef", dto.vnpayTransactionRef());
        body.put("adminNote", dto.adminNote());
        body.put("approvedBy", dto.approvedBy() != null ? dto.approvedBy().toString() : null);
        body.put("approvedAt", dto.approvedAt());
        body.put("rejectionReason", dto.rejectionReason());
        body.put("createdAt", dto.createdAt());
        body.put("updatedAt", dto.updatedAt());
        return body;
    }

    private Map<String, Object> buildVnpayResponse(ResidentCardPaymentResult result, Map<String, String> params) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", result.success());
        body.put("registrationId", result.registrationId() != null ? result.registrationId().toString() : null);
        body.put("responseCode", result.responseCode());
        body.put("signatureValid", result.signatureValid());
        body.put("requestType", result.requestType());
        body.put("message", result.message());
        body.put("params", params);
        return body;
    }

    private String extractAccessToken(HttpHeaders headers) {
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}


