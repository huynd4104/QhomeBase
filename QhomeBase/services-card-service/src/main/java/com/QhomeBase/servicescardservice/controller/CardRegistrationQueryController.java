package com.QhomeBase.servicescardservice.controller;

import com.QhomeBase.servicescardservice.dto.CardRegistrationSummaryDto;
import com.QhomeBase.servicescardservice.service.ApprovedCardQueryService;
import com.QhomeBase.servicescardservice.service.CardRegistrationQueryService;
import com.QhomeBase.servicescardservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/card-registrations")
@RequiredArgsConstructor
@Slf4j
public class CardRegistrationQueryController {

    private final CardRegistrationQueryService cardRegistrationQueryService;
    private final ApprovedCardQueryService approvedCardQueryService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getCardRegistrations(@RequestHeader HttpHeaders headers,
                                                  @RequestParam(required = false) UUID residentId,
                                                  @RequestParam(required = false) UUID unitId) {
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        try {
            List<CardRegistrationSummaryDto> data =
                    cardRegistrationQueryService.getCardRegistrations(userId, residentId, unitId);
            Map<String, Object> body = new HashMap<>();
            body.put("data", data);
            body.put("total", data.size());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [CardRegistrations] Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [CardRegistrations] Failed to fetch card registrations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể tải trạng thái thẻ"));
        }
    }

    @GetMapping("/admin/approved")
    public ResponseEntity<?> getApprovedCardsForAdmin(
            @RequestHeader HttpHeaders headers,
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) UUID unitId
    ) {
        UUID adminId = jwtUtil.getUserIdFromHeaders(headers);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        try {
            List<CardRegistrationSummaryDto> data = approvedCardQueryService.getApprovedCards(buildingId, unitId);
            Map<String, Object> body = new HashMap<>();
            body.put("data", data);
            body.put("total", data.size());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [ApprovedCards] Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [ApprovedCards] Failed to fetch approved cards", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể tải danh sách thẻ đã duyệt"));
        }
    }
}


