package com.QhomeBase.servicescardservice.controller;

import com.QhomeBase.servicescardservice.dto.RegisterServiceImageDto;
import com.QhomeBase.servicescardservice.dto.RegisterServiceRequestCreateDto;
import com.QhomeBase.servicescardservice.dto.RegisterServiceRequestDto;
import com.QhomeBase.servicescardservice.dto.VehicleRegistrationAdminDecisionRequest;
import com.QhomeBase.servicescardservice.service.VehicleRegistrationService;
import com.QhomeBase.servicescardservice.service.VehicleRegistrationService.VehicleRegistrationPaymentResponse;
import com.QhomeBase.servicescardservice.service.VehicleRegistrationService.VehicleRegistrationPaymentResult;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/register-service")
@RequiredArgsConstructor
@Slf4j
public class VehicleRegistrationController {

    private final VehicleRegistrationService registrationService;
    private final VnpayService vnpayService;
    private final JwtUtil jwtUtil;

    @PostMapping("/upload-images")
    public ResponseEntity<?> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        log.info("📤 [VehicleRegistration] Nhận request upload {} ảnh", files != null ? files.size() : 0);
        
        if (files == null || files.isEmpty()) {
            log.warn("⚠️ [VehicleRegistration] Không có file nào được gửi");
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn ít nhất một ảnh"));
        }
        
        // Validate file sizes before processing
        long maxFileSize = 10 * 1024 * 1024; // 10MB
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                log.warn("⚠️ [VehicleRegistration] File rỗng: {}", file.getOriginalFilename());
                return ResponseEntity.badRequest().body(Map.of("message", 
                    "File \"" + file.getOriginalFilename() + "\" rỗng"));
            }
            if (file.getSize() > maxFileSize) {
                log.warn("⚠️ [VehicleRegistration] File quá lớn: {} ({} bytes)", 
                    file.getOriginalFilename(), file.getSize());
                return ResponseEntity.badRequest().body(Map.of("message", 
                    "File \"" + file.getOriginalFilename() + "\" quá lớn (tối đa 10MB)"));
            }
        }
        
        try {
            log.info("✅ [VehicleRegistration] Bắt đầu xử lý upload {} ảnh", files.size());
            long startTime = System.currentTimeMillis();
            List<String> urls = registrationService.storeImages(files);
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [VehicleRegistration] Upload thành công {} ảnh trong {}ms", urls.size(), duration);
            return ResponseEntity.ok(Map.of("imageUrls", urls));
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [VehicleRegistration] Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            log.error("❌ [VehicleRegistration] Lỗi upload ảnh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể tải ảnh, vui lòng thử lại: " + e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [VehicleRegistration] Lỗi không mong đợi khi upload ảnh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi server khi xử lý upload: " + e.getMessage()));
        }
    }

    @PostMapping("/vnpay-url")
    public ResponseEntity<?> createRegistrationAndPay(@RequestBody RegisterServiceRequestCreateDto dto,
                                                      @RequestHeader HttpHeaders headers,
                                                      HttpServletRequest request) {
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            VehicleRegistrationPaymentResponse response = registrationService.createAndInitiatePayment(userId, dto, request);
            Map<String, Object> body = new HashMap<>();
            body.put("registrationId", response.registrationId().toString());
            body.put("paymentUrl", response.paymentUrl());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [VehicleRegistration] Lỗi tạo đăng ký", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể khởi tạo đăng ký xe"));
        }
    }

    @PostMapping("/{registrationId}/resume-payment")
    public ResponseEntity<?> resumePayment(@PathVariable String registrationId,
                                          @RequestHeader HttpHeaders headers,
                                          HttpServletRequest request) {
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            log.warn("⚠️ [VehicleRegistration] Unauthorized request to resumePayment");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        
        try {
            UUID registrationUuid = UUID.fromString(registrationId);
            log.debug("🔍 [VehicleRegistration] resumePayment request: registrationId={}, userId={}", registrationUuid, userId);
            
            VehicleRegistrationPaymentResponse response = registrationService.initiatePayment(userId, registrationUuid, request);
            Map<String, Object> body = new HashMap<>();
            body.put("registrationId", response.registrationId().toString());
            body.put("paymentUrl", response.paymentUrl());
            
            log.info("✅ [VehicleRegistration] resumePayment success: registrationId={}", registrationUuid);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [VehicleRegistration] Invalid argument: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("⚠️ [VehicleRegistration] IllegalStateException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [VehicleRegistration] Lỗi tiếp tục thanh toán", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể tiếp tục thanh toán: " + e.getMessage()));
        }
    }

    @PostMapping("/{registrationId}/vnpay-url")
    public ResponseEntity<?> initiatePayment(@PathVariable String registrationId,
                                             @RequestBody(required = false) RegisterServiceRequestCreateDto dto,
                                             @RequestHeader HttpHeaders headers,
                                             HttpServletRequest request) {
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        try {
            UUID registrationUuid = UUID.fromString(registrationId);

            if (dto != null) {
                registrationService.updateRegistration(userId, registrationUuid, dto);
            }

            VehicleRegistrationPaymentResponse response = registrationService.initiatePayment(userId, registrationUuid, request);
            Map<String, Object> body = new HashMap<>();
            body.put("registrationId", response.registrationId().toString());
            body.put("paymentUrl", response.paymentUrl());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [VehicleRegistration] Lỗi tạo URL thanh toán", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể tạo URL thanh toán"));
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
            RegisterServiceRequestDto dto = registrationService.getRegistration(userId, registrationUuid);
            return ResponseEntity.ok(toResponse(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/admin/vehicle-registrations")
    public ResponseEntity<?> getRegistrationsForAdmin(@RequestParam(name = "status", required = false) String status,
                                                      @RequestParam(name = "paymentStatus", required = false) String paymentStatus) {
        try {
            // Only filter by status if explicitly provided (not empty string)
            String finalStatus = (status != null && !status.isBlank()) ? status.trim() : null;
            String finalPaymentStatus = (paymentStatus != null && !paymentStatus.isBlank()) ? paymentStatus.trim() : null;
            
            return ResponseEntity.ok(
                    registrationService.getRegistrationsForAdmin(finalStatus, finalPaymentStatus)
            );
        } catch (IllegalArgumentException e) {
            log.warn("❌ [VehicleRegistration] Tham số không hợp lệ khi tải danh sách admin: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [VehicleRegistration] Lỗi lấy danh sách đăng ký cho admin", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể lấy danh sách đăng ký"));
        }
    }

    @GetMapping("/admin/vehicle-registrations/{registrationId}")
    public ResponseEntity<?> getRegistrationForAdmin(@PathVariable String registrationId,
                                                     @RequestHeader HttpHeaders headers) {
        UUID adminId = jwtUtil.getUserIdFromHeaders(headers);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            UUID regUuid = UUID.fromString(registrationId);
            RegisterServiceRequestDto dto = registrationService.getRegistrationForAdmin(regUuid);
            return ResponseEntity.ok(toResponse(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/admin/vehicle-registrations/{registrationId}/approve")
    public ResponseEntity<?> approveRegistration(@PathVariable String registrationId,
                                                 @RequestHeader HttpHeaders headers,
                                                 @Valid @RequestBody(required = false) VehicleRegistrationAdminDecisionRequest request) {
        UUID adminId = jwtUtil.getUserIdFromHeaders(headers);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            UUID regUuid = UUID.fromString(registrationId);
            String adminNote = request != null ? request.getNote() : null;
            String issueMessage = request != null ? request.getIssueMessage() : null;
            java.time.OffsetDateTime issueTime = request != null ? request.getIssueTime() : null;
            RegisterServiceRequestDto dto = registrationService.approveRegistration(regUuid, adminId, adminNote, issueMessage, issueTime);
            return ResponseEntity.ok(toResponse(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/admin/vehicle-registrations/{registrationId}/mark-paid")
    public ResponseEntity<?> markPaymentAsPaid(@PathVariable String registrationId,
                                               @RequestHeader HttpHeaders headers) {
        UUID adminId = jwtUtil.getUserIdFromHeaders(headers);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            UUID regUuid = UUID.fromString(registrationId);
            RegisterServiceRequestDto dto = registrationService.markPaymentAsPaid(regUuid, adminId);
            return ResponseEntity.ok(toResponse(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/admin/vehicle-registrations/{registrationId}/reject")
    public ResponseEntity<?> rejectRegistration(@PathVariable String registrationId,
                                                @RequestHeader HttpHeaders headers,
                                                @Valid @RequestBody(required = false) VehicleRegistrationAdminDecisionRequest request) {
        UUID adminId = jwtUtil.getUserIdFromHeaders(headers);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            UUID regUuid = UUID.fromString(registrationId);
            String reason = request != null ? request.getNote() : null;
            RegisterServiceRequestDto dto = registrationService.rejectRegistration(regUuid, adminId, reason);
            return ResponseEntity.ok(toResponse(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/admin/vehicle-registrations/{registrationId}/cancel")
    public ResponseEntity<?> cancelRegistration(@PathVariable String registrationId,
                                                @RequestHeader HttpHeaders headers,
                                                @Valid @RequestBody(required = false) VehicleRegistrationAdminDecisionRequest request) {
        UUID adminId = jwtUtil.getUserIdFromHeaders(headers);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            UUID regUuid = UUID.fromString(registrationId);
            String adminNote = request != null ? request.getNote() : null;
            RegisterServiceRequestDto dto = registrationService.cancelRegistration(regUuid, adminId, adminNote);
            return ResponseEntity.ok(toResponse(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
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
        VehicleRegistrationPaymentResult result = registrationService.handleVnpayCallback(params);
        Map<String, Object> body = buildVnpayResponse(result, params);
        HttpStatus status = result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body);
        } catch (Exception e) {
            log.error("❌ [VehicleRegistration] Lỗi xử lý callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/vnpay/redirect")
    public ResponseEntity<?> redirectAfterPayment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> params = vnpayService.extractParams(request);
        VehicleRegistrationPaymentResult result;
        try {
            result = registrationService.handleVnpayCallback(params);
        } catch (Exception e) {
            log.error("❌ [VehicleRegistration] Lỗi xử lý callback redirect", e);
            // URL encode message to avoid Unicode characters in HTTP header
            String encodedMessage = java.net.URLEncoder.encode(
                    e.getMessage() != null ? e.getMessage() : "Unknown error",
                    java.nio.charset.StandardCharsets.UTF_8
            );
            String fallback = "qhomeapp://vnpay-registration-result?success=false&message=" + encodedMessage;
            response.sendRedirect(fallback);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }

        Map<String, Object> body = buildVnpayResponse(result, params);
        String registrationId = result.registrationId() != null ? result.registrationId().toString() : "";
        String responseCode = result.responseCode() != null 
                ? java.net.URLEncoder.encode(result.responseCode(), java.nio.charset.StandardCharsets.UTF_8)
                : "";
        String redirectUrl = new StringBuilder("qhomeapp://vnpay-registration-result")
                .append("?registrationId=").append(registrationId)
                .append("&responseCode=").append(responseCode)
                .append("&success=").append(result.success())
                .toString();
        response.sendRedirect(redirectUrl);
        HttpStatus status = result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> buildVnpayResponse(VehicleRegistrationPaymentResult result, Map<String, String> params) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", result.success());
        body.put("registrationId", result.registrationId() != null ? result.registrationId().toString() : null);
        body.put("responseCode", result.responseCode());
        body.put("signatureValid", result.signatureValid());
        body.put("params", params);
        return body;
    }

    private Map<String, Object> toResponse(RegisterServiceRequestDto dto) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", dto.id() != null ? dto.id().toString() : null);
        body.put("userId", dto.userId() != null ? dto.userId().toString() : null);
        body.put("serviceType", dto.serviceType());
        body.put("requestType", dto.requestType());
        body.put("note", dto.note());
        body.put("status", dto.status());
        body.put("vehicleType", dto.vehicleType());
        body.put("licensePlate", dto.licensePlate());
        body.put("vehicleBrand", dto.vehicleBrand());
        body.put("vehicleColor", dto.vehicleColor());
        body.put("apartmentNumber", dto.apartmentNumber());
        body.put("buildingName", dto.buildingName());
        body.put("unitId", dto.unitId() != null ? dto.unitId().toString() : null);
        body.put("paymentStatus", dto.paymentStatus());
        body.put("paymentAmount", dto.paymentAmount());
        body.put("paymentDate", dto.paymentDate());
        body.put("paymentGateway", dto.paymentGateway());
        body.put("vnpayTransactionRef", dto.vnpayTransactionRef());
        body.put("adminNote", dto.adminNote());
        body.put("approvedBy", dto.approvedBy() != null ? dto.approvedBy().toString() : null);
        body.put("approvedByName", dto.approvedByName());
        body.put("approvedAt", dto.approvedAt());
        body.put("rejectionReason", dto.rejectionReason());
        body.put("createdAt", dto.createdAt());
        body.put("updatedAt", dto.updatedAt());
        body.put("imageUrls", dto.images() != null
                ? dto.images().stream().map(RegisterServiceImageDto::imageUrl).toList()
                : List.of());
        body.put("reissuedFromCardId", dto.reissuedFromCardId() != null ? dto.reissuedFromCardId().toString() : null);
        body.put("canReissue", dto.canReissue() != null ? dto.canReissue() : false);
        return body;
    }
}


