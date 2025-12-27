package com.QhomeBase.assetmaintenanceservice.controller;

import com.QhomeBase.assetmaintenanceservice.dto.service.*;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingStatus;
import com.QhomeBase.assetmaintenanceservice.security.UserPrincipal;
import com.QhomeBase.assetmaintenanceservice.service.ServiceBookingService;
import com.QhomeBase.assetmaintenanceservice.service.ServiceBookingPaymentService;
import com.QhomeBase.assetmaintenanceservice.service.vnpay.VnpayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/asset-maintenance")
@RequiredArgsConstructor
public class ServiceBookingController {

    private final ServiceBookingService bookingService;
    private final ServiceBookingPaymentService bookingPaymentService;
    private final VnpayService vnpayService;



    @GetMapping("/services/{serviceId}/booking/catalog")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingCatalogDto> getBookingCatalog(@PathVariable UUID serviceId) {
        return ResponseEntity.ok(bookingService.getBookingCatalog(serviceId));
    }

    @GetMapping("/services/{serviceId}/slots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceBookingSlotDto>> getServiceSlots(
            @PathVariable UUID serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(bookingService.getServiceSlots(serviceId, fromDate, toDate));
    }

    @GetMapping("/services/{serviceId}/slots/{date}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceBookingSlotDto>> getServiceSlotsByDate(
            @PathVariable UUID serviceId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(bookingService.getServiceSlotsByDate(serviceId, date));
    }

    @PostMapping("/bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> createBooking(@Valid @RequestBody CreateServiceBookingRequest request,
                                                           Authentication authentication) {
        ServiceBookingDto created = bookingService.createBooking(request, authentication.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceBookingDto>> getMyBookings(
            @RequestParam(required = false) ServiceBookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication
    ) {
        List<ServiceBookingDto> bookings = bookingService.getMyBookings(authentication.getPrincipal(), status, fromDate, toDate);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/bookings/unpaid")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceBookingDto>> getMyUnpaidBookings(Authentication authentication) {
        List<ServiceBookingDto> bookings = bookingService.getMyUnpaidBookings(authentication.getPrincipal());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/bookings/paid")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceBookingDto>> getMyPaidBookings(Authentication authentication) {
        List<ServiceBookingDto> bookings = bookingService.getMyPaidBookings(authentication.getPrincipal());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/bookings/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> getMyBooking(@PathVariable UUID bookingId,
                                                          Authentication authentication) {
        return ResponseEntity.ok(bookingService.getMyBooking(bookingId, authentication.getPrincipal()));
    }

    @PatchMapping("/bookings/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> cancelMyBooking(@PathVariable UUID bookingId,
                                                             @Valid @RequestBody(required = false) CancelServiceBookingRequest request,
                                                             Authentication authentication) {
        CancelServiceBookingRequest effectiveRequest = request != null ? request : new CancelServiceBookingRequest(null);
        return ResponseEntity.ok(bookingService.cancelMyBooking(bookingId, effectiveRequest, authentication.getPrincipal()));
    }

    @PatchMapping("/bookings/{bookingId}/accept-terms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> acceptTerms(@PathVariable UUID bookingId,
                                                         @Valid @RequestBody AcceptServiceBookingTermsRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(bookingService.acceptTerms(bookingId, request, authentication.getPrincipal()));
    }

    @PostMapping("/bookings/{bookingId}/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> addBookingItem(@PathVariable UUID bookingId,
                                                            @Valid @RequestBody CreateServiceBookingItemRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(bookingService.addBookingItem(bookingId, request, authentication.getPrincipal(), false));
    }

    @PutMapping("/bookings/{bookingId}/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> updateBookingItem(@PathVariable UUID bookingId,
                                                               @PathVariable UUID itemId,
                                                               @Valid @RequestBody UpdateServiceBookingItemRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(bookingService.updateBookingItem(bookingId, itemId, request, authentication.getPrincipal(), false));
    }

    @DeleteMapping("/bookings/{bookingId}/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> deleteBookingItem(@PathVariable UUID bookingId,
                                                               @PathVariable UUID itemId,
                                                               Authentication authentication) {
        return ResponseEntity.ok(bookingService.deleteBookingItem(bookingId, itemId, authentication.getPrincipal(), false));
    }

    @PutMapping("/bookings/{bookingId}/slots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> updateBookingSlots(@PathVariable UUID bookingId,
                                                                @Valid @RequestBody UpdateServiceBookingSlotsRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(bookingService.updateBookingSlots(bookingId, request, authentication.getPrincipal(), false));
    }

    @PostMapping("/bookings/{bookingId}/vnpay-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createVnpayPaymentUrl(@PathVariable UUID bookingId,
                                                   Authentication authentication,
                                                   HttpServletRequest request) {
        try {
            var response = bookingPaymentService.initiatePayment(bookingId, authentication.getPrincipal(), request);
            return ResponseEntity.ok(Map.of(
                    "bookingId", response.bookingId().toString(),
                    "paymentUrl", response.paymentUrl(),
                    "transactionRef", response.transactionRef()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể tạo URL thanh toán VNPAY"));
        }
    }

    @GetMapping("/bookings/vnpay/return")
    public ResponseEntity<?> handleVnpayReturn(HttpServletRequest request) {
        Map<String, String> params = vnpayService.extractParams(request);
        try {
            var result = bookingPaymentService.handleCallback(params);
            Map<String, Object> body = Map.of(
                    "success", result.success(),
                    "bookingId", result.bookingId() != null ? result.bookingId().toString() : null,
                    "responseCode", result.responseCode(),
                    "signatureValid", result.signatureValid(),
                    "params", params
            );
            HttpStatus status = result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/bookings/vnpay/redirect")
    public ResponseEntity<?> redirectAfterPayment(HttpServletRequest request,
                                                  HttpServletResponse response) throws IOException {
        Map<String, String> params = vnpayService.extractParams(request);
        ServiceBookingPaymentResult result;
        try {
            result = bookingPaymentService.handleCallback(params);
        } catch (Exception e) {
            String fallback = "qhomeapp://vnpay-service-booking-result?success=false&message=" +
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(fallback);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
        String bookingId = result.bookingId() != null ? result.bookingId().toString() : "";
        String responseCode = result.responseCode() != null 
                ? java.net.URLEncoder.encode(result.responseCode(), java.nio.charset.StandardCharsets.UTF_8)
                : "";
        
        // Build message based on payment result
        String message;
        if (result.success()) {
            message = "Đã thanh toán dịch vụ thành công";
        } else {
            message = "Thanh toán không thành công. Vui lòng thử lại.";
        }
        String encodedMessage = java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
        
        String redirectUrl = new StringBuilder("qhomeapp://vnpay-service-booking-result")
                .append("?bookingId=").append(bookingId)
                .append("&responseCode=").append(responseCode)
                .append("&success=").append(result.success())
                .append("&message=").append(encodedMessage)
                .toString();
        response.sendRedirect(redirectUrl);

        Map<String, Object> body = Map.of(
                "success", result.success(),
                "bookingId", bookingId,
                "responseCode", result.responseCode(),
                "signatureValid", result.signatureValid(),
                "params", params
        );

        HttpStatus status = result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body);
    }

    /* Administrative endpoints */

    @GetMapping("/admin/bookings")
    @PreAuthorize("@authz.canViewServiceBooking()")
    public ResponseEntity<List<ServiceBookingDto>> searchBookings(
            @RequestParam(required = false) ServiceBookingStatus status,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        List<ServiceBookingDto> bookings = bookingService.searchBookings(status, serviceId, userId, fromDate, toDate);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/admin/bookings/{bookingId}")
    @PreAuthorize("@authz.canViewServiceBooking()")
    public ResponseEntity<ServiceBookingDto> getBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.getBooking(bookingId));
    }

    @PatchMapping("/admin/bookings/{bookingId}/approve")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> approveBooking(@PathVariable UUID bookingId,
                                                            @Valid @RequestBody(required = false) AdminApproveServiceBookingRequest request,
                                                            Authentication authentication) {
        AdminApproveServiceBookingRequest effectiveRequest = request != null ? request : new AdminApproveServiceBookingRequest(null);
        return ResponseEntity.ok(bookingService.approveBooking(bookingId, effectiveRequest, resolveUserId(authentication)));
    }

    @PatchMapping("/admin/bookings/{bookingId}/reject")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> rejectBooking(@PathVariable UUID bookingId,
                                                           @Valid @RequestBody AdminRejectServiceBookingRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(bookingService.rejectBooking(bookingId, request, resolveUserId(authentication)));
    }

    @PatchMapping("/admin/bookings/{bookingId}/complete")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> completeBooking(@PathVariable UUID bookingId,
                                                             @Valid @RequestBody(required = false) AdminCompleteServiceBookingRequest request,
                                                             Authentication authentication) {
        AdminCompleteServiceBookingRequest effectiveRequest = request != null ? request : new AdminCompleteServiceBookingRequest(null);
        return ResponseEntity.ok(bookingService.completeBooking(bookingId, effectiveRequest, resolveUserId(authentication)));
    }

    @PatchMapping("/admin/bookings/{bookingId}/payment")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> updatePayment(@PathVariable UUID bookingId,
                                                           @Valid @RequestBody AdminUpdateServiceBookingPaymentRequest request) {
        return ResponseEntity.ok(bookingService.updatePayment(bookingId, request));
    }

    @PutMapping("/admin/bookings/{bookingId}/slots")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> adminUpdateSlots(@PathVariable UUID bookingId,
                                                              @Valid @RequestBody UpdateServiceBookingSlotsRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(bookingService.updateBookingSlots(bookingId, request, authentication.getPrincipal(), true));
    }

    @PostMapping("/admin/bookings/{bookingId}/items")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> adminAddItem(@PathVariable UUID bookingId,
                                                          @Valid @RequestBody CreateServiceBookingItemRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(bookingService.addBookingItem(bookingId, request, authentication.getPrincipal(), true));
    }

    @PutMapping("/admin/bookings/{bookingId}/items/{itemId}")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> adminUpdateItem(@PathVariable UUID bookingId,
                                                             @PathVariable UUID itemId,
                                                             @Valid @RequestBody UpdateServiceBookingItemRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(bookingService.updateBookingItem(bookingId, itemId, request, authentication.getPrincipal(), true));
    }

    @DeleteMapping("/admin/bookings/{bookingId}/items/{itemId}")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> adminDeleteItem(@PathVariable UUID bookingId,
                                                             @PathVariable UUID itemId,
                                                             Authentication authentication) {
        return ResponseEntity.ok(bookingService.deleteBookingItem(bookingId, itemId, authentication.getPrincipal(), true));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", translateErrorMessage(ex.getMessage())));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", ex.getMessage()));
    }

    private String translateErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Yêu cầu đặt dịch vụ không hợp lệ. Vui lòng kiểm tra lại thông tin.";
        }
        if (message.contains("no available slots configured")) {
            return "Dịch vụ không có khung giờ hoạt động cho ngày bạn chọn. Vui lòng chọn ngày khác hoặc liên hệ ban quản lý.";
        }
        if (message.contains("outside service availability")) {
            return "Khung giờ bạn chọn nằm ngoài thời gian phục vụ. Vui lòng chọn lại khung giờ nằm trong giờ hoạt động.";
        }
        if (message.contains("already booked")) {
            return "Khung giờ này đã có người đặt. Vui lòng chọn khung giờ khác.";
        }
        if (message.contains("overlaps with another slot")) {
            return "Khung giờ bạn chọn trùng với khung giờ khác trong yêu cầu hiện tại.";
        }
        return message;
    }

    private UUID resolveUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.uid();
        }
        throw new IllegalStateException("Unsupported authentication principal");
    }
}


