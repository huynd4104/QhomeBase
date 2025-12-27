package com.QhomeBase.servicescardservice.service;

import com.QhomeBase.servicescardservice.config.VnpayProperties;
import com.QhomeBase.servicescardservice.dto.BatchCardPaymentRequest;
import com.QhomeBase.servicescardservice.dto.CardRegistrationAdminDecisionRequest;
import com.QhomeBase.servicescardservice.dto.ResidentCardRegistrationCreateDto;
import com.QhomeBase.servicescardservice.dto.ResidentCardRegistrationDto;
import com.QhomeBase.servicescardservice.model.ResidentCardRegistration;
import com.QhomeBase.servicescardservice.repository.ResidentCardRegistrationRepository;
import com.QhomeBase.servicescardservice.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"NullAway", "DataFlowIssue"})
public class ResidentCardRegistrationService {

    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_READY_FOR_PAYMENT = "READY_FOR_PAYMENT";
    private static final String STATUS_PAYMENT_PENDING = "PAYMENT_PENDING";
    private static final String STATUS_PENDING_REVIEW = "PENDING";
    private static final String STATUS_REJECTED = "REJECTED";
    
    private final CardPricingService cardPricingService;
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String PAYMENT_VNPAY = "VNPAY";

    private final ResidentCardRegistrationRepository repository;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final BillingClient billingClient;
    private final ResidentUnitLookupService residentUnitLookupService;
    private final NotificationClient notificationClient;
    private final CardFeeReminderService cardFeeReminderService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final BaseServiceClient baseServiceClient;
    private final ConcurrentMap<Long, UUID> orderIdToRegistrationId = new ConcurrentHashMap<>();

    @Transactional
    public ResidentCardRegistrationDto createRegistration(UUID userId, ResidentCardRegistrationCreateDto dto) {
        return createRegistration(userId, dto, null);
    }

    @Transactional
    public ResidentCardRegistrationDto createRegistration(UUID userId, ResidentCardRegistrationCreateDto dto, String accessToken) {
        validatePayload(dto);

        // Kiểm tra user có phải là thành viên household không
        // Chỉ cần là thành viên household (có trong household_members với left_at IS NULL) thì có thể đăng ký thẻ
        UUID requesterResidentId = residentUnitLookupService.resolveByUser(userId, dto.unitId())
                .map(info -> info.residentId())
                .orElse(null);
        
        // Nếu không tìm thấy residentId từ household_members, thử tìm từ bảng residents
        if (requesterResidentId == null) {
            log.debug("🔍 [ResidentCard] User {} not found in household_members, trying to find residentId from residents table", userId);
            requesterResidentId = baseServiceClient.findResidentIdByUserId(userId, accessToken);
            if (requesterResidentId != null) {
                log.info("✅ [ResidentCard] Found residentId {} for userId {}", requesterResidentId, userId);
            }
        }
        
        // Nếu vẫn không tìm thấy residentId, throw error
        if (requesterResidentId == null) {
            log.warn("⚠️ [ResidentCard] Cannot find residentId for userId {} in unit {}", userId, dto.unitId());
            throw new IllegalStateException("Không tìm thấy thông tin cư dân của bạn. Vui lòng thử lại sau.");
        }
        
        // Kiểm tra user có phải là thành viên household của unit này không
        if (!isHouseholdMember(requesterResidentId, dto.unitId())) {
            log.warn("⚠️ [ResidentCard] User {} (residentId: {}) is not a household member of unit {}", userId, requesterResidentId, dto.unitId());
            throw new IllegalStateException("Bạn không phải là thành viên của căn hộ này. Chỉ thành viên hộ gia đình mới được đăng ký thẻ.");
        }
        
        // Nếu đăng ký cho người khác, kiểm tra người đó có thuộc cùng household không
        if (!requesterResidentId.equals(dto.residentId())) {
            if (!isHouseholdMember(dto.residentId(), dto.unitId())) {
                throw new IllegalStateException(
                    "Chỉ được đăng ký thẻ cho thành viên trong cùng hộ gia đình. " +
                    "Cư dân này không thuộc căn hộ này."
                );
            }
            // Kiểm tra cả 2 có cùng household không
            if (!areInSameHousehold(requesterResidentId, dto.residentId(), dto.unitId())) {
                throw new IllegalStateException(
                    "Chỉ được đăng ký thẻ cho thành viên trong cùng hộ gia đình. " +
                    "Cư dân này không thuộc cùng hộ gia đình với bạn."
                );
            }
        }

        // Kiểm tra xem cư dân đã được duyệt thành thành viên chưa
        // OWNER (primary member) hoặc có household_member_requests APPROVED thì không cần account
        // Chỉ check account nếu không phải OWNER và không có request APPROVED
        if (dto.residentId() != null) {
            boolean isPrimaryOrApproved = isPrimaryMemberOrHasApprovedRequest(dto.residentId(), dto.unitId());
            if (!isPrimaryOrApproved) {
                // Nếu không phải primary member và không có request APPROVED, thì cần có account
                boolean hasAccount = baseServiceClient.isResidentMemberApproved(dto.residentId(), accessToken);
                if (!hasAccount) {
                    log.warn("⚠️ [ResidentCard] Resident {} không phải primary member, không có request APPROVED, và chưa có account", dto.residentId());
                throw new IllegalStateException(
                    "Cư dân chưa được duyệt thành thành viên. Vui lòng đợi admin duyệt yêu cầu tạo tài khoản trước khi đăng ký thẻ cư dân."
                );
                }
            } else {
                log.info("✅ [ResidentCard] Resident {} là primary member hoặc có request APPROVED, không cần check account", dto.residentId());
            }
        }

        // Normalize citizenId: loại bỏ tất cả ký tự không phải số
        String normalizedCitizenId = dto.citizenId() != null 
                ? dto.citizenId().replaceAll("[^0-9]", "") 
                : null;

        ResidentCardRegistration registration = ResidentCardRegistration.builder()
                .userId(userId)
                .unitId(dto.unitId())
                .requestType(resolveRequestType(dto.requestType()))
                .residentId(dto.residentId())
                .fullName(normalize(dto.fullName()))
                .apartmentNumber(normalize(dto.apartmentNumber()))
                .buildingName(normalize(dto.buildingName()))
                .citizenId(normalizedCitizenId)
                .phoneNumber(normalize(dto.phoneNumber()))
                .note(dto.note())
                .status(STATUS_READY_FOR_PAYMENT)
                .paymentStatus("UNPAID")
                .paymentAmount(cardPricingService.getPrice("RESIDENT"))
                .paymentGateway(null)
                .vnpayTransactionRef(null)
                .adminNote(null)
                .rejectionReason(null)
                .approvedAt(null)
                .approvedBy(null)
                .build();

        try {
            applyResolvedAddressForResident(
                    registration,
                    dto.residentId(),
                    dto.unitId(),
                    dto.fullName(),
                    dto.apartmentNumber(),
                    dto.buildingName()
            );
        } catch (Exception e) {
            log.warn("⚠️ [ResidentCard] Không thể resolve địa chỉ từ database, sử dụng giá trị từ form: {}", e.getMessage());
            // Fallback to form values if lookup fails
            if (!StringUtils.hasText(registration.getFullName())) {
                registration.setFullName(normalize(dto.fullName()));
            }
            if (!StringUtils.hasText(registration.getApartmentNumber())) {
                registration.setApartmentNumber(normalize(dto.apartmentNumber()));
            }
            if (!StringUtils.hasText(registration.getBuildingName())) {
                registration.setBuildingName(normalize(dto.buildingName()));
            }
        }

        ResidentCardRegistration saved = repository.save(registration);
        return toDto(saved);
    }

    @Transactional
    public ResidentCardPaymentResponse createAndInitiatePayment(UUID userId,
                                                               ResidentCardRegistrationCreateDto dto,
                                                               HttpServletRequest request) {
        return createAndInitiatePayment(userId, dto, request, null);
    }

    @Transactional
    public ResidentCardPaymentResponse createAndInitiatePayment(UUID userId,
                                                               ResidentCardRegistrationCreateDto dto,
                                                               HttpServletRequest request,
                                                               String accessToken) {
        ResidentCardRegistrationDto created = createRegistration(userId, dto, accessToken);
        return initiatePayment(userId, created.id(), request);
    }

    @Transactional(readOnly = true)
    public List<ResidentCardRegistrationDto> getRegistrationsForAdmin(String status, String paymentStatus) {
        List<ResidentCardRegistration> registrations = repository.findAllByOrderByCreatedAtDesc();
        return registrations.stream()
                .filter(reg -> !StringUtils.hasText(status) || status.equalsIgnoreCase(reg.getStatus()))
                .filter(reg -> !StringUtils.hasText(paymentStatus) || paymentStatus.equalsIgnoreCase(reg.getPaymentStatus()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResidentCardRegistrationDto getRegistrationForAdmin(UUID registrationId) {
        ResidentCardRegistration registration = repository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thẻ cư dân"));
        return toDto(registration);
    }

    @Transactional
    public ResidentCardRegistrationDto processAdminDecision(UUID adminId,
                                                            UUID registrationId,
                                                            CardRegistrationAdminDecisionRequest request) {
        ResidentCardRegistration registration = repository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thẻ cư dân"));

        String decision = request.decision();
        if (decision == null || decision.isBlank()) {
            throw new IllegalArgumentException("Decision is required");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));

        if ("APPROVE".equalsIgnoreCase(decision) || "APPROVED".equalsIgnoreCase(decision)) {
            // Approve logic
            // Save old status to check if status is actually changing
            String oldStatus = registration.getStatus();
            
            if (!STATUS_PENDING_REVIEW.equalsIgnoreCase(oldStatus) 
                    && !STATUS_READY_FOR_PAYMENT.equalsIgnoreCase(oldStatus)) {
                throw new IllegalStateException("Đăng ký không ở trạng thái chờ duyệt. Trạng thái hiện tại: " + oldStatus);
            }

            // Check if status is actually changing from PENDING/READY_FOR_PAYMENT to APPROVED
            // Only send notification if status is changing (not already APPROVED)
            boolean statusChanging = !STATUS_APPROVED.equalsIgnoreCase(oldStatus);
            
            if (!statusChanging) {
                log.warn("⚠️ [ResidentCard] Registration {} already approved. Status not changing. Skipping notification.", 
                        registrationId);
                // Still allow update of adminNote, issueMessage, issueTime if provided
                if (request.note() != null) {
                    registration.setAdminNote(request.note());
                }
                registration.setUpdatedAt(now);
                ResidentCardRegistration saved = repository.save(registration);
                return toDto(saved);
            }

            // Check payment status - must be PAID before approval
            if (!"PAID".equalsIgnoreCase(registration.getPaymentStatus())) {
                throw new IllegalStateException(
                    String.format("Không thể duyệt thẻ. Thẻ phải đã thanh toán trước khi được duyệt. Trạng thái thanh toán hiện tại: %s", 
                        registration.getPaymentStatus())
                );
            }

            registration.setStatus("APPROVED");
            registration.setApprovedBy(adminId);
            registration.setApprovedAt(now);
            registration.setAdminNote(request.note());
            registration.setUpdatedAt(now);

            ResidentCardRegistration saved = repository.save(registration);

            // Create reminder state if card is already paid (for test mode)
            // In production, reminder state will be created after payment callback
            if ("PAID".equalsIgnoreCase(saved.getPaymentStatus())) {
                try {
                    cardFeeReminderService.resetReminderAfterPayment(
                            CardFeeReminderService.CardFeeType.RESIDENT,
                            saved.getId(),
                            saved.getUnitId(),
                            saved.getResidentId(),
                            saved.getUserId(),
                            saved.getApartmentNumber(),
                            saved.getBuildingName(),
                            saved.getPaymentDate() != null ? saved.getPaymentDate() : now
                    );
                    log.info("✅ [ResidentCard] Đã tạo reminder state cho thẻ {} sau khi approve", saved.getId());
                } catch (Exception e) {
                    log.warn("⚠️ [ResidentCard] Không thể tạo reminder state sau khi approve: {}", e.getMessage());
                }
            }

            // Send notification to resident ONLY if status changed from PENDING/READY_FOR_PAYMENT to APPROVED
            if (statusChanging) {
                sendCardApprovalNotification(saved, request.issueMessage(), request.issueTime());
                log.info("✅ [ResidentCard] Admin {} đã approve đăng ký {} (status changed from {} to APPROVED). Notification sent.", 
                        adminId, registrationId, oldStatus);
            } else {
                log.info("✅ [ResidentCard] Admin {} đã approve đăng ký {} (status unchanged, notification skipped).", 
                        adminId, registrationId);
            }
            
            return toDto(saved);
        } else if ("REJECT".equalsIgnoreCase(decision) || "REJECTED".equalsIgnoreCase(decision)) {
            // Reject logic
            // Save old status to check if status is actually changing
            String oldStatus = registration.getStatus();
            
            if (STATUS_REJECTED.equalsIgnoreCase(oldStatus)) {
                throw new IllegalStateException("Đăng ký đã bị từ chối");
            }

            // Check if status is actually changing from PENDING/READY_FOR_PAYMENT to REJECTED
            // Only send notification if status is changing (not already REJECTED)
            boolean statusChanging = !STATUS_REJECTED.equalsIgnoreCase(oldStatus) 
                    && (STATUS_PENDING_REVIEW.equalsIgnoreCase(oldStatus) 
                        || STATUS_READY_FOR_PAYMENT.equalsIgnoreCase(oldStatus));

            registration.setStatus(STATUS_REJECTED);
            registration.setAdminNote(request.note());
            registration.setUpdatedAt(now);

            ResidentCardRegistration saved = repository.save(registration);

            // Send notification to resident ONLY if status changed from PENDING/READY_FOR_PAYMENT to REJECTED
            if (statusChanging) {
                sendCardRejectionNotification(saved, request.note());
                log.info("✅ [ResidentCard] Admin {} đã reject đăng ký {} (status changed from {} to REJECTED). Notification sent.", 
                        adminId, registrationId, oldStatus);
            } else {
                log.info("✅ [ResidentCard] Admin {} đã reject đăng ký {} (status unchanged, notification skipped).", 
                        adminId, registrationId);
            }
            
            return toDto(saved);
        } else if ("CANCEL".equalsIgnoreCase(decision) || "CANCELLED".equalsIgnoreCase(decision)) {
            // Admin cancel logic - set status to REJECTED (bị từ chối)
            // Note: Cư dân hủy sẽ set status = CANCELLED, admin hủy sẽ set status = REJECTED
            // Save old status to check if status is actually changing
            String oldStatus = registration.getStatus();
            
            if (STATUS_REJECTED.equalsIgnoreCase(oldStatus)) {
                throw new IllegalStateException("Đăng ký đã bị từ chối");
            }

            // Check if status is actually changing from PENDING/READY_FOR_PAYMENT to REJECTED
            // Only send notification if status is changing (not already REJECTED)
            boolean statusChanging = !STATUS_REJECTED.equalsIgnoreCase(oldStatus) 
                    && (STATUS_PENDING_REVIEW.equalsIgnoreCase(oldStatus) 
                        || STATUS_READY_FOR_PAYMENT.equalsIgnoreCase(oldStatus));

            registration.setStatus(STATUS_REJECTED);
            registration.setAdminNote(request.note());
            registration.setUpdatedAt(now);

            ResidentCardRegistration saved = repository.save(registration);

            // Send notification to resident ONLY if status changed from PENDING/READY_FOR_PAYMENT to REJECTED
            if (statusChanging) {
                sendCardRejectionNotification(saved, request.note());
                log.info("✅ [ResidentCard] Admin {} đã cancel (reject) đăng ký {} (status changed from {} to REJECTED). Notification sent.", 
                        adminId, registrationId, oldStatus);
            } else {
                log.info("✅ [ResidentCard] Admin {} đã cancel (reject) đăng ký {} (status unchanged, notification skipped).", 
                        adminId, registrationId);
            }
            
            return toDto(saved);
        } else {
            throw new IllegalArgumentException("Invalid decision: " + decision + ". Must be APPROVE, REJECT, or CANCEL");
        }
    }

    private void sendCardApprovalNotification(ResidentCardRegistration registration, String issueMessage, OffsetDateTime issueTime) {
        try {
            log.info("🔔 [ResidentCard] ========== SENDING APPROVAL NOTIFICATION ==========");
            log.info("🔔 [ResidentCard] Registration ID: {}", registration.getId());
            log.info("🔔 [ResidentCard] UserId: {}", registration.getUserId());
            log.info("🔔 [ResidentCard] UnitId: {}", registration.getUnitId());
            
            // CARD_APPROVED is PRIVATE - only resident who created the request can see
            // Get residentId from userId (người tạo request) instead of residentId (người được đăng ký thẻ)
            log.info("🔔 [ResidentCard] Resolving residentId from userId and unitId...");
            UUID requesterResidentId = residentUnitLookupService.resolveByUser(
                    registration.getUserId(), 
                    registration.getUnitId()
            ).map(ResidentUnitLookupService.AddressInfo::residentId).orElse(null);
            
            // Fallback: Nếu không tìm thấy từ household_members, query trực tiếp từ residents table
            if (requesterResidentId == null) {
                log.warn("⚠️ [ResidentCard] Không tìm thấy residentId từ household_members, thử query trực tiếp từ residents table...");
                log.warn("⚠️ [ResidentCard] UserId: {}, UnitId: {}", registration.getUserId(), registration.getUnitId());
                
                // Query trực tiếp từ residents table bằng userId
                try {
                    requesterResidentId = baseServiceClient.findResidentIdByUserId(registration.getUserId(), null);
                    if (requesterResidentId != null) {
                        log.info("✅ [ResidentCard] Tìm thấy residentId từ residents table: {}", requesterResidentId);
                    } else {
                        log.error("❌ [ResidentCard] Không tìm thấy residentId trong residents table");
                    }
                } catch (Exception e) {
                    log.error("❌ [ResidentCard] Lỗi khi query residentId từ base-service: {}", e.getMessage());
                }
            }
            
            if (requesterResidentId == null) {
                log.error("❌ [ResidentCard] ========== RESIDENT ID RESOLUTION FAILED ==========");
                log.error("❌ [ResidentCard] Không thể tìm thấy residentId cho userId={}, unitId={}", 
                        registration.getUserId(), registration.getUnitId());
                log.error("❌ [ResidentCard] Không thể gửi notification cho registrationId: {}", registration.getId());
                log.error("❌ [ResidentCard] Notification sẽ không được gửi đến resident!");
                return;
            }
            
            log.info("✅ [ResidentCard] ResidentId resolved successfully: {}", requesterResidentId);

            // Get payment amount (use actual payment amount if available, otherwise use current price)
            BigDecimal paymentAmount = registration.getPaymentAmount();
            if (paymentAmount == null) {
                paymentAmount = cardPricingService.getPrice("RESIDENT");
            }
            String formattedPrice = formatVnd(paymentAmount);

            // Get resident full name (người được đăng ký thẻ)
            String residentFullName = registration.getFullName();
            if (residentFullName == null || residentFullName.isBlank()) {
                residentFullName = "cư dân";
            }

            String title = "Thẻ cư dân đã được duyệt";
            
            // Format thời gian nhận thẻ (từ issueTime nếu có, nếu không thì dùng approvedAt)
            String issueTimeFormatted = "";
            OffsetDateTime timeToUse = issueTime != null ? issueTime : registration.getApprovedAt();
            if (timeToUse != null) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"));
                issueTimeFormatted = timeToUse.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))
                        .format(dateFormatter);
            }
            
            String message;
            // Ưu tiên: issueMessage > adminNote (note) > message tự động
            if (issueMessage != null && !issueMessage.isBlank()) {
                // Admin đã ghi issueMessage riêng cho notification
                message = issueMessage;
                log.info("📝 [ResidentCard] Sử dụng issueMessage từ admin: {}", message);
            } else if (registration.getAdminNote() != null && !registration.getAdminNote().isBlank()) {
                // Admin đã ghi note nhưng không ghi issueMessage, dùng note làm notification message
                message = registration.getAdminNote();
                log.info("📝 [ResidentCard] Sử dụng adminNote (note) từ admin: {}", message);
            } else {
                // Tự động tạo message: "Thẻ cư dân của (họ và tên) đã chấp nhận và cư dân sẽ nhận vào (ngày giờ)"
                if (issueTimeFormatted.isEmpty()) {
                    message = String.format("Thẻ cư dân của %s đã chấp nhận.", residentFullName);
                } else {
                    message = String.format("Thẻ cư dân của %s đã chấp nhận và cư dân sẽ nhận vào %s.", 
                            residentFullName, issueTimeFormatted);
                }
                log.info("📝 [ResidentCard] Sử dụng message tự động: {}", message);
            }

            Map<String, String> data = new HashMap<>();
            data.put("cardType", "RESIDENT_CARD");
            data.put("registrationId", registration.getId().toString());
            data.put("price", paymentAmount.toString());
            data.put("formattedPrice", formattedPrice);
            if (registration.getApartmentNumber() != null) {
                data.put("apartmentNumber", registration.getApartmentNumber());
            }
            if (residentFullName != null) {
                data.put("fullName", residentFullName);
            }
            if (!issueTimeFormatted.isEmpty()) {
                data.put("issueTime", issueTimeFormatted);
            }
            if (timeToUse != null) {
                data.put("issueTimeTimestamp", timeToUse.toString());
            }

            log.info("📤 [ResidentCard] ========== CALLING NOTIFICATION CLIENT ==========");
            log.info("📤 [ResidentCard] ResidentId: {}", requesterResidentId);
            log.info("📤 [ResidentCard] BuildingId: null (private notification)");
            log.info("📤 [ResidentCard] Type: CARD_APPROVED");
            log.info("📤 [ResidentCard] Title: {}", title);
            log.info("📤 [ResidentCard] Message: {}", message);
            log.info("📤 [ResidentCard] ReferenceId: {}", registration.getId());
            log.info("📤 [ResidentCard] ReferenceType: RESIDENT_CARD_REGISTRATION");
            log.info("📤 [ResidentCard] Data: {}", data);

            // Send PRIVATE notification to requester (người tạo request) only
            // buildingId = null for private notification
            notificationClient.sendResidentNotification(
                    requesterResidentId, // residentId của người tạo request
                    null, // buildingId = null for private notification
                    "CARD_APPROVED",
                    title,
                    message,
                    registration.getId(),
                    "RESIDENT_CARD_REGISTRATION",
                    data
            );

            log.info("✅ [ResidentCard] ========== NOTIFICATION CLIENT CALLED ==========");
            log.info("✅ [ResidentCard] Đã gọi notificationClient.sendResidentNotification()");
            log.info("✅ [ResidentCard] Requester residentId: {} (userId: {})", 
                    requesterResidentId, registration.getUserId());
        } catch (Exception e) {
            log.error("❌ [ResidentCard] ========== EXCEPTION IN APPROVAL NOTIFICATION ==========");
            log.error("❌ [ResidentCard] Không thể gửi notification approval cho registrationId: {}", registration.getId(), e);
            log.error("❌ [ResidentCard] Exception type: {}", e.getClass().getName());
            log.error("❌ [ResidentCard] Exception message: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("❌ [ResidentCard] Caused by: {}", e.getCause().getMessage());
            }
        }
    }

    private void sendCardRejectionNotification(ResidentCardRegistration registration, String rejectionReason) {
        try {
            log.info("🔔 [ResidentCard] ========== SENDING REJECTION NOTIFICATION ==========");
            log.info("🔔 [ResidentCard] Registration ID: {}", registration.getId());
            log.info("🔔 [ResidentCard] UserId: {}", registration.getUserId());
            log.info("🔔 [ResidentCard] UnitId: {}", registration.getUnitId());
            
            // CARD_REJECTED is PRIVATE - only resident who created the request can see
            // Get residentId from userId (người tạo request) instead of residentId (người được đăng ký thẻ)
            log.info("🔔 [ResidentCard] Resolving residentId from userId and unitId...");
            UUID requesterResidentId = residentUnitLookupService.resolveByUser(
                    registration.getUserId(), 
                    registration.getUnitId()
            ).map(ResidentUnitLookupService.AddressInfo::residentId).orElse(null);
            
            // Fallback: Nếu không tìm thấy từ household_members, query trực tiếp từ residents table
            if (requesterResidentId == null) {
                log.warn("⚠️ [ResidentCard] Không tìm thấy residentId từ household_members, thử query trực tiếp từ residents table...");
                log.warn("⚠️ [ResidentCard] UserId: {}, UnitId: {}", registration.getUserId(), registration.getUnitId());
                
                // Query trực tiếp từ residents table bằng userId
                try {
                    requesterResidentId = baseServiceClient.findResidentIdByUserId(registration.getUserId(), null);
                    if (requesterResidentId != null) {
                        log.info("✅ [ResidentCard] Tìm thấy residentId từ residents table: {}", requesterResidentId);
                    } else {
                        log.error("❌ [ResidentCard] Không tìm thấy residentId trong residents table");
                    }
                } catch (Exception e) {
                    log.error("❌ [ResidentCard] Lỗi khi query residentId từ base-service: {}", e.getMessage());
                }
            }
            
            if (requesterResidentId == null) {
                log.error("❌ [ResidentCard] ========== RESIDENT ID RESOLUTION FAILED ==========");
                log.error("❌ [ResidentCard] Không thể tìm thấy residentId cho userId={}, unitId={}", 
                        registration.getUserId(), registration.getUnitId());
                log.error("❌ [ResidentCard] Không thể gửi notification cho registrationId: {}", registration.getId());
                log.error("❌ [ResidentCard] Notification sẽ không được gửi đến resident!");
                return;
            }
            
            log.info("✅ [ResidentCard] ResidentId resolved successfully: {}", requesterResidentId);

            // Get payment amount (use actual payment amount if available, otherwise use current price)
            BigDecimal paymentAmount = registration.getPaymentAmount();
            if (paymentAmount == null) {
                paymentAmount = cardPricingService.getPrice("RESIDENT");
            }
            String formattedPrice = formatVnd(paymentAmount);

            // Get resident full name (người được đăng ký thẻ)
            String residentFullName = registration.getFullName();
            if (residentFullName == null || residentFullName.isBlank()) {
                residentFullName = "cư dân";
            }

            String title = "Thẻ cư dân bị từ chối";
            String message;
            if (rejectionReason != null && !rejectionReason.isBlank()) {
                message = String.format("Yêu cầu đăng ký thẻ cư dân của %s đã bị từ chối. Phí đăng ký: %s. Lý do: %s", 
                        residentFullName, formattedPrice, rejectionReason);
            } else {
                message = String.format("Yêu cầu đăng ký thẻ cư dân của %s đã bị từ chối. Phí đăng ký: %s. Vui lòng liên hệ quản trị viên để biết thêm chi tiết.", 
                        residentFullName, formattedPrice);
            }

            Map<String, String> data = new HashMap<>();
            data.put("cardType", "RESIDENT_CARD");
            data.put("registrationId", registration.getId().toString());
            data.put("status", "REJECTED");
            data.put("price", paymentAmount.toString());
            data.put("formattedPrice", formattedPrice);
            if (registration.getApartmentNumber() != null) {
                data.put("apartmentNumber", registration.getApartmentNumber());
            }
            if (residentFullName != null) {
                data.put("fullName", residentFullName);
            }
            if (rejectionReason != null) {
                data.put("rejectionReason", rejectionReason);
            }

            // Send PRIVATE notification to requester (người tạo request) only
            // buildingId = null for private notification
            notificationClient.sendResidentNotification(
                    requesterResidentId, // residentId của người tạo request
                    null, // buildingId = null for private notification
                    "CARD_REJECTED",
                    title,
                    message,
                    registration.getId(),
                    "RESIDENT_CARD_REGISTRATION",
                    data
            );

            log.info("✅ [ResidentCard] Đã gửi notification rejection riêng tư cho requester residentId: {} (userId: {})", 
                    requesterResidentId, registration.getUserId());
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Không thể gửi notification rejection cho registrationId: {}", 
                    registration.getId(), e);
        }
    }


    @Transactional
    public ResidentCardPaymentResponse initiatePayment(UUID userId,
                                                       UUID registrationId,
                                                       HttpServletRequest request) {
        ResidentCardRegistration registration = repository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thẻ cư dân"));

        if (STATUS_REJECTED.equalsIgnoreCase(registration.getStatus())) {
            throw new IllegalStateException("Đăng ký đã bị từ chối");
        }
        if ("CANCELLED".equalsIgnoreCase(registration.getStatus())) {
            throw new IllegalStateException("Đăng ký này đã bị hủy do không thanh toán. Vui lòng tạo đăng ký mới.");
        }
        
        String currentStatus = registration.getStatus();
        String paymentStatus = registration.getPaymentStatus();
        
        // Cho phép gia hạn nếu status = NEEDS_RENEWAL hoặc SUSPENDED (đã thanh toán trước đó)
        if ("NEEDS_RENEWAL".equalsIgnoreCase(currentStatus) || "SUSPENDED".equalsIgnoreCase(currentStatus)) {
            if (!"PAID".equalsIgnoreCase(paymentStatus)) {
                throw new IllegalStateException("Thẻ chưa thanh toán, không thể gia hạn");
            }
            // Cho phép thanh toán để gia hạn
        } else {
            // Cho phép tiếp tục thanh toán nếu payment_status là UNPAID hoặc PAYMENT_PENDING
            // (PAYMENT_PENDING có thể xảy ra khi user chưa hoàn tất thanh toán trong 10 phút)
            if (!Objects.equals(paymentStatus, "UNPAID") && !Objects.equals(paymentStatus, "PAYMENT_PENDING")) {
                throw new IllegalStateException("Đăng ký đã thanh toán hoặc không thể tiếp tục thanh toán");
            }
        }

        registration.setStatus(STATUS_PAYMENT_PENDING);
        registration.setPaymentStatus("PAYMENT_PENDING");
        registration.setPaymentGateway(PAYMENT_VNPAY);
        ResidentCardRegistration saved = repository.save(registration);

        long orderId = Math.abs(saved.getId().hashCode());
        if (orderId == 0) {
            orderId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        }
        orderIdToRegistrationId.put(orderId, saved.getId());

        String clientIp = resolveClientIp(request);
        String orderInfo = "Thanh toán thẻ cư dân " +
                (saved.getApartmentNumber() != null ? saved.getApartmentNumber() : saved.getId());
        String returnUrl = StringUtils.hasText(vnpayProperties.getResidentReturnUrl())
                ? vnpayProperties.getResidentReturnUrl()
                : vnpayProperties.getReturnUrl();
        BigDecimal registrationFee = cardPricingService.getPrice("RESIDENT");
        var paymentResult = vnpayService.createPaymentUrlWithRef(orderId, orderInfo, registrationFee, clientIp, returnUrl);
        
        // Save transaction reference to database for fallback lookup
        saved.setVnpayTransactionRef(paymentResult.transactionRef());
        repository.save(saved);

        return new ResidentCardPaymentResponse(saved.getId(), paymentResult.paymentUrl());
    }

    @Transactional
    public ResidentCardPaymentResponse batchInitiatePayment(UUID userId,
                                                           BatchCardPaymentRequest request,
                                                           HttpServletRequest httpRequest) {
        if (request.registrationIds() == null || request.registrationIds().isEmpty()) {
            throw new IllegalArgumentException("Danh sách đăng ký không được để trống");
        }

        // Validate all registrations belong to user and are in valid state
        List<ResidentCardRegistration> registrations = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (UUID registrationId : request.registrationIds()) {
            ResidentCardRegistration registration = repository.findByIdAndUserId(registrationId, userId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Không tìm thấy đăng ký thẻ cư dân: %s", registrationId)));

            // Validate unitId matches
            if (!registration.getUnitId().equals(request.unitId())) {
                throw new IllegalArgumentException(
                        String.format("Đăng ký %s không thuộc căn hộ %s", registrationId, request.unitId()));
            }

            // Validate status
            if (STATUS_REJECTED.equalsIgnoreCase(registration.getStatus())) {
                throw new IllegalStateException(
                        String.format("Đăng ký %s đã bị từ chối", registrationId));
            }
            if ("CANCELLED".equalsIgnoreCase(registration.getStatus())) {
                throw new IllegalStateException(
                        String.format("Đăng ký %s đã bị hủy. Vui lòng tạo đăng ký mới.", registrationId));
            }

            String currentStatus = registration.getStatus();
            String paymentStatus = registration.getPaymentStatus();
            
            if (!"NEEDS_RENEWAL".equalsIgnoreCase(currentStatus) && 
                !"SUSPENDED".equalsIgnoreCase(currentStatus)) {
                if (!Objects.equals(paymentStatus, "UNPAID") && 
                    !Objects.equals(paymentStatus, "PAYMENT_PENDING")) {
                    throw new IllegalStateException(
                            String.format("Đăng ký %s đã thanh toán hoặc không thể tiếp tục thanh toán", registrationId));
                }
            }

            // Use paymentAmount from registration, fallback to pricing service if null
            BigDecimal registrationAmount = registration.getPaymentAmount();
            if (registrationAmount == null || registrationAmount.compareTo(BigDecimal.ZERO) <= 0) {
                registrationAmount = cardPricingService.getPrice("RESIDENT");
                log.warn("⚠️ [ResidentCard] Registration {} has no paymentAmount, using default price: {}", 
                        registrationId, registrationAmount);
            }

            registrations.add(registration);
            totalAmount = totalAmount.add(registrationAmount);
        }

        // Update all registrations to PAYMENT_PENDING
        String apartmentNumber = registrations.get(0).getApartmentNumber();
        for (ResidentCardRegistration registration : registrations) {
            registration.setStatus(STATUS_PAYMENT_PENDING);
            registration.setPaymentStatus("PAYMENT_PENDING");
            registration.setPaymentGateway(PAYMENT_VNPAY);
            repository.save(registration);
        }

        // Create single payment URL for all cards
        // Use first registration ID for orderId, but include count in orderInfo
        UUID firstRegistrationId = registrations.get(0).getId();
        long orderId = Math.abs(firstRegistrationId.hashCode());
        if (orderId == 0) {
            orderId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        }
        
        // Store mapping: orderId -> list of registration IDs (comma-separated)
        String registrationIdsStr = registrations.stream()
                .map(r -> r.getId().toString())
                .collect(java.util.stream.Collectors.joining(","));
        orderIdToRegistrationId.put(orderId, firstRegistrationId); // Store first ID for backward compatibility
        
        // Store batch mapping separately (we'll need to add a new map for this)
        // For now, we'll encode it in the orderInfo or use a different approach
        
        String clientIp = resolveClientIp(httpRequest);
        int cardCount = registrations.size();
        String orderInfo = String.format("Thanh toán %d thẻ cư dân %s", 
                cardCount, 
                apartmentNumber != null ? apartmentNumber : firstRegistrationId.toString().substring(0, 8));
        
        String returnUrl = StringUtils.hasText(vnpayProperties.getResidentReturnUrl())
                ? vnpayProperties.getResidentReturnUrl()
                : vnpayProperties.getReturnUrl();
        
        log.info("💰 [ResidentCard] Batch payment calculation: {} cards, totalAmount={} VND", 
                cardCount, totalAmount);
        
        var paymentResult = vnpayService.createPaymentUrlWithRef(orderId, orderInfo, totalAmount, clientIp, returnUrl);
        
        // Save transaction reference to all registrations and set payment status
        String txnRef = paymentResult.transactionRef();
        OffsetDateTime now = OffsetDateTime.now();
        for (ResidentCardRegistration registration : registrations) {
            registration.setVnpayTransactionRef(txnRef);
            registration.setPaymentStatus("PAYMENT_IN_PROGRESS");
            registration.setVnpayInitiatedAt(now);
            repository.save(registration);
        }

        log.info("✅ [ResidentCard] Batch payment initiated: {} cards, total amount: {} VND, txnRef: {}", 
                cardCount, totalAmount, txnRef);
        
        return new ResidentCardPaymentResponse(firstRegistrationId, paymentResult.paymentUrl());
    }

    @Transactional(readOnly = true)
    public ResidentCardRegistrationDto getRegistration(UUID userId, UUID registrationId) {
        // Get registration without userId check first (to check permission)
        ResidentCardRegistration registration = repository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thẻ cư dân"));
        
        // Check permission: Owner can view any household member's registration, household members can only view their own
        if (registration.getUnitId() != null) {
            boolean isOwner = baseServiceClient.isOwnerOfUnit(userId, registration.getUnitId(), null);
            
            if (!isOwner) {
                // Not Owner - household member can only view their own registration
                // Check by userId first
                boolean canView = userId.equals(registration.getUserId());
                
                // If userId doesn't match, check by residentId (Owner might have created registration for household member)
                if (!canView && registration.getResidentId() != null) {
                    UUID currentUserResidentId = baseServiceClient.findResidentIdByUserId(userId, null);
                    if (currentUserResidentId != null && currentUserResidentId.equals(registration.getResidentId())) {
                        canView = true;
                        log.debug("✅ [ResidentCard] Household member {} can view registration {} (matched by residentId: {})", 
                                userId, registrationId, currentUserResidentId);
                    }
                }
                
                if (!canView) {
                    log.warn("⚠️ [ResidentCard] User {} không phải Owner và không phải chủ sở hữu đăng ký {}, không được phép xem", 
                            userId, registrationId);
                    throw new IllegalArgumentException("Không tìm thấy đăng ký thẻ cư dân");
                }
            }
        } else {
            // Fallback: if no unitId, only allow viewing own registration
            // Check by userId first
            boolean canView = userId.equals(registration.getUserId());
            
            // If userId doesn't match, check by residentId
            if (!canView && registration.getResidentId() != null) {
                UUID currentUserResidentId = baseServiceClient.findResidentIdByUserId(userId, null);
                if (currentUserResidentId != null && currentUserResidentId.equals(registration.getResidentId())) {
                    canView = true;
                    log.debug("✅ [ResidentCard] User {} can view registration {} (matched by residentId: {})", 
                            userId, registrationId, currentUserResidentId);
                }
            }
            
            if (!canView) {
                throw new IllegalArgumentException("Không tìm thấy đăng ký thẻ cư dân");
            }
        }
        
        return toDto(registration);
    }

    @Transactional
    public void cancelRegistration(UUID userId, UUID registrationId) {
        // Get registration without userId check first (to check permission)
        ResidentCardRegistration registration = repository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thẻ cư dân"));
        
        // Check permission: Owner can cancel any household member's card, household members can only cancel their own
        if (registration.getUnitId() != null) {
            boolean isOwner = baseServiceClient.isOwnerOfUnit(userId, registration.getUnitId(), null);
            
            if (isOwner) {
                // Owner can cancel any household member's card in the same unit
                log.info("✅ [ResidentCard] Owner {} đã hủy đăng ký {} của household member trong unit {}", 
                        userId, registrationId, registration.getUnitId());
            } else {
                // Not Owner - household member can only cancel their own card
                // Check by userId first
                boolean canCancel = userId.equals(registration.getUserId());
                
                // If userId doesn't match, check by residentId (Owner might have created registration for household member)
                if (!canCancel && registration.getResidentId() != null) {
                    UUID currentUserResidentId = baseServiceClient.findResidentIdByUserId(userId, null);
                    if (currentUserResidentId != null && currentUserResidentId.equals(registration.getResidentId())) {
                        canCancel = true;
                        log.debug("✅ [ResidentCard] Household member {} can cancel registration {} (matched by residentId: {})", 
                                userId, registrationId, currentUserResidentId);
                    }
                }
                
                if (!canCancel) {
                    log.warn("⚠️ [ResidentCard] User {} không phải Owner và không phải người tạo đăng ký {}, không được phép hủy", 
                            userId, registrationId);
                    log.warn("⚠️ [ResidentCard] Registration userId: {}, current userId: {}, registration residentId: {}", 
                            registration.getUserId(), userId, registration.getResidentId());
                    throw new IllegalStateException("Chỉ chủ căn hộ mới được quyền hủy thẻ của các thành viên. Bạn chỉ có thể hủy thẻ của chính mình.");
                }
                log.info("✅ [ResidentCard] Household member {} đã hủy đăng ký {} của chính mình", userId, registrationId);
            }
        } else {
            // Fallback: if no unitId, only allow canceling own registration
            // Check by userId first
            boolean canCancel = userId.equals(registration.getUserId());
            
            // If userId doesn't match, check by residentId
            if (!canCancel && registration.getResidentId() != null) {
                UUID currentUserResidentId = baseServiceClient.findResidentIdByUserId(userId, null);
                if (currentUserResidentId != null && currentUserResidentId.equals(registration.getResidentId())) {
                    canCancel = true;
                    log.debug("✅ [ResidentCard] User {} can cancel registration {} (matched by residentId: {})", 
                            userId, registrationId, currentUserResidentId);
                }
            }
            
            if (!canCancel) {
                throw new IllegalStateException("Bạn chỉ có thể hủy thẻ của chính mình.");
            }
        }
        
        if (STATUS_CANCELLED.equalsIgnoreCase(registration.getStatus())) {
            log.info("ℹ️ [ResidentCard] Đăng ký {} đã được hủy trước đó", registrationId);
            return;
        }
        
        registration.setStatus(STATUS_CANCELLED);
        registration.setUpdatedAt(OffsetDateTime.now());
        repository.save(registration);
        log.info("✅ [ResidentCard] Đăng ký {} đã được hủy thành công", registrationId);
    }

    @Transactional
    public ResidentCardPaymentResult handleVnpayCallback(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Missing callback data from VNPAY");
        }

        String txnRef = params.get("vnp_TxnRef");
        if (txnRef == null || !txnRef.contains("_")) {
            throw new IllegalArgumentException("Invalid transaction reference");
        }

        Long orderId;
        try {
            orderId = Long.parseLong(txnRef.split("_")[0]);
        } catch (NumberFormatException e) {
            log.error("❌ [ResidentCard] Cannot parse orderId from txnRef: {}", txnRef);
            throw new IllegalArgumentException("Invalid transaction reference format");
        }

        UUID registrationId = orderIdToRegistrationId.get(orderId);
        ResidentCardRegistration registration = null;

        // Try to find registration by orderId map first
        if (registrationId != null) {
            registration = repository.findById(registrationId).orElse(null);
            if (registration != null) {
                log.info("✅ [ResidentCard] Found registration by orderId map: registrationId={}, orderId={}", 
                        registrationId, orderId);
            }
        }

        // Fallback: try to find by transaction reference
        if (registration == null) {
            var optionalByTxnRef = repository.findByVnpayTransactionRef(txnRef);
            if (optionalByTxnRef.isPresent()) {
                registration = optionalByTxnRef.get();
                log.info("✅ [ResidentCard] Found registration by txnRef: registrationId={}, txnRef={}", 
                        registration.getId(), txnRef);
            }
        }

        // If still not found, throw exception with orderId for debugging
        if (registration == null) {
            log.error("❌ [ResidentCard] Cannot find registration: orderId={}, txnRef={}, mapSize={}", 
                    orderId, txnRef, orderIdToRegistrationId.size());
            throw new IllegalArgumentException(
                    String.format("Registration not found for orderId: %d, txnRef: %s", orderId, txnRef)
            );
        }

        boolean signatureValid = vnpayService.validateReturn(params);
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        registration.setVnpayTransactionRef(txnRef);

        if (signatureValid && "00".equals(responseCode) && "00".equals(transactionStatus)) {
            // Handle batch payment: find all registrations with the same txnRef
            List<ResidentCardRegistration> allRegistrations = repository.findAllByVnpayTransactionRef(txnRef);
            if (allRegistrations.isEmpty()) {
                allRegistrations = List.of(registration);
            }
            
            log.info("✅ [ResidentCard] Processing payment for {} registration(s) with txnRef: {}", 
                    allRegistrations.size(), txnRef);
            
            // Use current time for payment date to ensure accurate timestamp
            OffsetDateTime payDate = OffsetDateTime.now();
            
            for (ResidentCardRegistration reg : allRegistrations) {
                reg.setPaymentStatus("PAID");
                reg.setPaymentGateway(PAYMENT_VNPAY);
                reg.setPaymentDate(payDate);
                reg.setVnpayTransactionRef(txnRef);
                
                try {
                    applyResolvedAddressForResident(
                            reg,
                            reg.getResidentId(),
                            reg.getUnitId(),
                            reg.getFullName(),
                            reg.getApartmentNumber(),
                            reg.getBuildingName()
                    );
                } catch (Exception e) {
                    log.warn("⚠️ [ResidentCard] Không thể resolve địa chỉ sau thanh toán cho registration {}, giữ nguyên giá trị hiện tại: {}", 
                            reg.getId(), e.getMessage());
                }
                
                // Nếu là gia hạn (status = NEEDS_RENEWAL hoặc SUSPENDED), sau khi thanh toán thành công → set status = APPROVED
                // Nếu là đăng ký mới, sau khi thanh toán → set status = PENDING_REVIEW (chờ admin duyệt)
                String currentStatus = reg.getStatus();
                if ("NEEDS_RENEWAL".equals(currentStatus) || "SUSPENDED".equals(currentStatus)) {
                    reg.setStatus(STATUS_APPROVED);
                    reg.setApprovedAt(OffsetDateTime.now()); // Cập nhật lại approved_at khi gia hạn
                    log.info("✅ [ResidentCard] Gia hạn thành công, thẻ {} đã được set lại status = APPROVED", reg.getId());
                    
                    // Reset reminder cycle sau khi gia hạn (approved_at đã được set ở trên)
                    cardFeeReminderService.resetReminderAfterPayment(
                            CardFeeReminderService.CardFeeType.RESIDENT,
                            reg.getId(),
                            reg.getUnitId(),
                            reg.getResidentId(),
                            reg.getUserId(),
                            reg.getApartmentNumber(),
                            reg.getBuildingName(),
                            payDate // payment_date mới (approved_at sẽ được lấy từ registration.getApprovedAt())
                    );
                } else {
                    reg.setStatus(STATUS_PENDING_REVIEW);
                }
                repository.save(reg);

                log.info("✅ [ResidentCard] Thanh toán thành công cho đăng ký {}", reg.getId());
                billingClient.recordResidentCardPayment(
                        reg.getId(),
                        reg.getUserId(),
                        reg.getUnitId(),
                        reg.getFullName(),
                        reg.getApartmentNumber(),
                        reg.getBuildingName(),
                        reg.getRequestType(),
                        reg.getNote(),
                        reg.getPaymentAmount(),
                        payDate,
                        txnRef,
                        params.get("vnp_TransactionNo"),
                        params.get("vnp_BankCode"),
                        params.get("vnp_CardType"),
                        responseCode
                );

                cardFeeReminderService.resetReminderAfterPayment(
                        CardFeeReminderService.CardFeeType.RESIDENT,
                        reg.getId(),
                        reg.getUnitId(),
                        reg.getResidentId(),
                        reg.getUserId(),
                        reg.getApartmentNumber(),
                        reg.getBuildingName(),
                        payDate
                );
            }

            orderIdToRegistrationId.remove(orderId);
            
            // Tạo thông báo thành công dựa trên loại yêu cầu
            String requestType = registration.getRequestType();
            String successMessage;
            if ("RENEWAL".equals(requestType)) {
                successMessage = "Gia hạn thẻ cư dân thành công";
            } else {
                successMessage = "Đăng ký thẻ cư dân thành công";
            }
            
            return new ResidentCardPaymentResult(
                registration.getId(), 
                true, 
                responseCode, 
                true,
                requestType,
                successMessage
            );
        }

        registration.setStatus(STATUS_READY_FOR_PAYMENT);
        registration.setPaymentStatus("UNPAID");
        repository.save(registration);
        orderIdToRegistrationId.remove(orderId);
        
        String errorMessage = "Thanh toán không thành công. Vui lòng thử lại.";
        return new ResidentCardPaymentResult(
            registration.getId(), 
            false, 
            responseCode, 
            signatureValid,
            registration.getRequestType(),
            errorMessage
        );
    }

    private void applyResolvedAddressForResident(ResidentCardRegistration registration,
                                                 UUID residentId,
                                                 UUID unitId,
                                                 String fallbackFullName,
                                                 String fallbackApartment,
                                                 String fallbackBuilding) {
        residentUnitLookupService.resolveByResident(residentId, unitId).ifPresentOrElse(info -> {
            if (StringUtils.hasText(info.residentFullName())) {
                registration.setFullName(normalize(info.residentFullName()));
            } else {
                registration.setFullName(normalize(fallbackFullName));
            }

            String apartment = info.apartmentNumber() != null ? info.apartmentNumber() : fallbackApartment;
            String building = info.buildingName() != null ? info.buildingName() : fallbackBuilding;
            registration.setApartmentNumber(normalize(apartment));
            registration.setBuildingName(normalize(building));
        }, () -> {
            registration.setFullName(normalize(fallbackFullName));
            registration.setApartmentNumber(normalize(fallbackApartment));
            registration.setBuildingName(normalize(fallbackBuilding));
        });
    }


    private void validatePayload(ResidentCardRegistrationCreateDto dto) {
        if (dto.unitId() == null) {
            throw new IllegalArgumentException("Căn hộ là bắt buộc");
        }
        if (dto.residentId() == null) {
            throw new IllegalArgumentException("Cư dân là bắt buộc");
        }
        
        // Validate số lượng thẻ cư dân không vượt quá số người trong căn hộ
        validateResidentCardLimitByUnit(dto.unitId());
        
        // Validate CCCD phải có ít nhất 12 số
        if (!StringUtils.hasText(dto.citizenId())) {
            throw new IllegalArgumentException("CCCD/CMND là bắt buộc");
        }
        
        // Normalize CCCD: loại bỏ tất cả khoảng trắng và ký tự không phải số
        String normalizedCitizenId = dto.citizenId().replaceAll("[^0-9]", "");
        
        // Validate format: phải có ít nhất 12 số
        if (normalizedCitizenId.length() < 12) {
            throw new IllegalArgumentException("CCCD/CMND phải có ít nhất 12 số");
        }
        
        // Kiểm tra CCCD có thuộc căn hộ không
        validateCitizenIdBelongsToUnit(normalizedCitizenId, dto.unitId());
        
        // Kiểm tra CCCD đã được sử dụng chưa
        if (repository.existsByCitizenId(normalizedCitizenId)) {
            throw new IllegalStateException(
                String.format("CCCD/CMND %s đã được sử dụng để đăng ký thẻ cư dân. " +
                            "Mỗi CCCD/CMND chỉ được phép đăng ký 1 thẻ cư dân.",
                            normalizedCitizenId)
            );
        }
        log.debug("✅ [ResidentCard] CCCD {} chưa được sử dụng và thuộc căn hộ", normalizedCitizenId);
    }

    /**
     * Kiểm tra số thẻ cư dân đã đăng ký không vượt quá số người trong căn hộ
     */
    private void validateResidentCardLimitByUnit(UUID unitId) {
        // Đếm số household members (số người) trong căn hộ
        long numberOfResidents = countHouseholdMembersByUnit(unitId);
        
        // Đếm số thẻ cư dân đã đăng ký cho căn hộ này (bao gồm cả chưa thanh toán)
        // Đếm TẤT CẢ các registration trừ REJECTED và CANCELLED
        // Logic: Nếu đã đăng ký đủ số lượng thẻ (kể cả chưa thanh toán), không cho phép đăng ký thêm
        // Chỉ khi một thẻ bị hủy (CANCELLED) hoặc từ chối (REJECTED) thì mới có thể đăng ký thêm
        long registeredCards = repository.countAllResidentCardsByUnitId(unitId, List.of("REJECTED", "CANCELLED"));
        
        if (registeredCards >= numberOfResidents) {
            throw new IllegalStateException(
                String.format("Căn hộ này chỉ được phép đăng ký tối đa %d thẻ cư dân (theo số người trong căn hộ). " +
                            "Hiện tại đã đăng ký %d thẻ (bao gồm cả các thẻ chưa thanh toán). " +
                            "Vui lòng thanh toán hoặc hủy các thẻ đã đăng ký trước khi đăng ký thẻ mới.",
                            numberOfResidents, registeredCards)
            );
        }
        
        log.debug("✅ [ResidentCard] Unit {}: {} residents, {} registered cards (including unpaid)", 
                unitId, numberOfResidents, registeredCards);
    }

    /**
     * Kiểm tra CCCD có thuộc căn hộ đó không
     */
    private void validateCitizenIdBelongsToUnit(String citizenId, UUID unitId) {
        if (!StringUtils.hasText(citizenId) || unitId == null) {
            return;
        }
        
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("citizenId", citizenId)
                    .addValue("unitId", unitId);
            
            log.debug("🔍 [ResidentCard] Đang kiểm tra CCCD {} có thuộc căn hộ {} không", citizenId, unitId);
            
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(DISTINCT hm.resident_id)
                    FROM data.household_members hm
                    JOIN data.households h ON h.id = hm.household_id
                    JOIN data.residents r ON r.id = hm.resident_id
                    WHERE h.unit_id = :unitId
                      AND r.national_id = :citizenId
                      AND (hm.left_at IS NULL OR hm.left_at >= CURRENT_DATE)
                      AND (h.end_date IS NULL OR h.end_date >= CURRENT_DATE)
                    """, params, Long.class);
            
            if (count == null || count == 0) {
                throw new IllegalStateException(
                    String.format("CCCD/CMND %s không thuộc căn hộ này. " +
                                "Vui lòng kiểm tra lại thông tin CCCD/CMND và căn hộ.",
                                citizenId)
                );
            }
            
            log.debug("✅ [ResidentCard] CCCD {} thuộc căn hộ {}", citizenId, unitId);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Không thể kiểm tra CCCD {} có thuộc căn hộ {} không", citizenId, unitId, e);
            throw new IllegalStateException(
                String.format("Không thể xác thực CCCD/CMND. Vui lòng thử lại sau."), e);
        }
    }

    /**
     * Lấy danh sách thành viên trong căn hộ (bao gồm citizenId và fullName)
     * Chỉ cần là thành viên household thì có thể xem danh sách
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHouseholdMembersByUnit(UUID unitId, UUID userId, String accessToken) {
        if (unitId == null) {
            log.warn("⚠️ [ResidentCard] getHouseholdMembersByUnit called with null unitId");
            return List.of();
        }
        
        // Kiểm tra user có phải là thành viên household không
        UUID requesterResidentId = residentUnitLookupService.resolveByUser(userId, unitId)
                .map(info -> info.residentId())
                .orElse(null);
        
        if (requesterResidentId == null) {
            requesterResidentId = baseServiceClient.findResidentIdByUserId(userId, accessToken);
        }
        
        if (requesterResidentId == null || !isHouseholdMember(requesterResidentId, unitId)) {
            log.warn("⚠️ [ResidentCard] User {} is not a household member of unit {}, cannot get household members list", userId, unitId);
            throw new IllegalStateException("Bạn không phải là thành viên của căn hộ này. Chỉ thành viên hộ gia đình mới được xem danh sách thành viên.");
        }
        
        log.info("✅ [ResidentCard] User {} (residentId: {}) là thành viên household của unit {}", userId, requesterResidentId, unitId);
        
        // Kiểm tra xem user có phải Owner không (dựa trên is_primary)
        boolean isOwner = false;
        try {
            MapSqlParameterSource checkParams = new MapSqlParameterSource()
                    .addValue("residentId", requesterResidentId)
                    .addValue("unitId", unitId);
            
            Long primaryCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM data.household_members hm
                    JOIN data.households h ON h.id = hm.household_id
                    WHERE hm.resident_id = :residentId
                      AND h.unit_id = :unitId
                      AND hm.is_primary = true
                      AND (hm.left_at IS NULL OR hm.left_at >= CURRENT_DATE)
                      AND (h.end_date IS NULL OR h.end_date >= CURRENT_DATE)
                    """, checkParams, Long.class);
            
            isOwner = primaryCount != null && primaryCount > 0;
            log.debug("🔍 [ResidentCard] User {} isOwner of unit {}: {}", userId, unitId, isOwner);
        } catch (Exception e) {
            log.warn("⚠️ [ResidentCard] Lỗi check Owner status: {}", e.getMessage());
        }
        
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("unitId", unitId);
            
            if (!isOwner && requesterResidentId != null) {
                // Nếu không phải Owner, chỉ lấy thông tin của chính user đó
                params.addValue("requesterResidentId", requesterResidentId);
            }
            
            log.debug("🔍 [ResidentCard] Đang lấy danh sách thành viên trong căn hộ unitId: {} (isOwner: {})", unitId, isOwner);
            
            // Query để lấy danh sách thành viên và check xem họ đã có thẻ được approve chưa
            // Thêm thông tin về household kind để Flutter có thể verify
            // Chỉ lấy những household members đã được admin approve (có request với status APPROVED)
            // OWNER (primary member) luôn được phép, không cần request
            // Nếu không phải Owner, chỉ lấy thông tin của chính user đó
            String query = """
                    SELECT DISTINCT
                        r.id AS resident_id,
                        r.full_name AS full_name,
                        r.national_id AS citizen_id,
                        r.phone AS phone_number,
                        r.email AS email,
                        r.dob AS date_of_birth,
                        hm.is_primary AS is_primary,
                        CASE 
                            WHEN EXISTS (
                                SELECT 1 FROM card.resident_card_registration rcr
                                WHERE rcr.citizen_id = r.national_id
                                  AND rcr.status IN ('APPROVED', 'ACTIVE', 'ISSUED', 'COMPLETED')
                            ) THEN true
                            ELSE false
                        END AS has_approved_card,
                        CASE
                            WHEN EXISTS (
                                SELECT 1 FROM card.resident_card_registration rcr
                                WHERE rcr.citizen_id = r.national_id
                                  AND rcr.status IN ('PENDING', 'REVIEW_PENDING', 'PROCESSING', 'IN_PROGRESS')
                                  AND rcr.payment_status = 'PAID'
                            ) THEN true
                            ELSE false
                        END AS waiting_for_approval
                    FROM data.household_members hm
                    JOIN data.households h ON h.id = hm.household_id
                    JOIN data.residents r ON r.id = hm.resident_id
                    WHERE h.unit_id = :unitId
                      AND (hm.left_at IS NULL OR hm.left_at >= CURRENT_DATE)
                      AND (h.end_date IS NULL OR h.end_date >= CURRENT_DATE)
                      AND (
                          -- OWNER (primary member) luôn được phép
                          hm.is_primary = true
                          OR
                          -- Hoặc có request đã được approve
                          EXISTS (
                              SELECT 1 FROM data.household_member_requests hmr
                              WHERE hmr.household_id = hm.household_id
                                AND (hmr.resident_id = r.id 
                                     OR (hmr.resident_id IS NULL 
                                         AND hmr.resident_national_id = r.national_id
                                         AND hmr.resident_phone = r.phone))
                                AND hmr.status = 'APPROVED'
                          )
                      )
            """;
            
            // Nếu không phải Owner, chỉ lấy thông tin của chính user đó
            if (!isOwner && requesterResidentId != null) {
                query += " AND r.id = :requesterResidentId";
            }
            
            query += " ORDER BY r.full_name";
            
            List<Map<String, Object>> members = jdbcTemplate.query(query, params, (rs, rowNum) -> {
                Map<String, Object> member = new HashMap<>();
                member.put("residentId", rs.getObject("resident_id", UUID.class).toString());
                member.put("fullName", rs.getString("full_name"));
                member.put("citizenId", rs.getString("citizen_id"));
                member.put("phoneNumber", rs.getString("phone_number"));
                member.put("email", rs.getString("email"));
                member.put("dateOfBirth", rs.getDate("date_of_birth") != null 
                    ? rs.getDate("date_of_birth").toString() : null);
                member.put("hasApprovedCard", rs.getBoolean("has_approved_card"));
                member.put("waitingForApproval", rs.getBoolean("waiting_for_approval"));
                // Phân biệt Owner và household member dựa trên is_primary
                boolean isPrimary = rs.getBoolean("is_primary");
                String householdKind = isPrimary ? "OWNER" : "HOUSEHOLD_MEMBER";
                member.put("householdKind", householdKind);
                return member;
            });
            
            // Log để debug
            if (!members.isEmpty()) {
                long ownerCount = members.stream()
                    .filter(m -> "OWNER".equals(m.get("householdKind")))
                    .count();
                long memberCount = members.size() - ownerCount;
                log.info("✅ [ResidentCard] Căn hộ {} có {} thành viên ({} Owner, {} Household Member)", 
                    unitId, members.size(), ownerCount, memberCount);
            } else {
                log.warn("⚠️ [ResidentCard] Căn hộ {} không có thành viên nào trong household_members", unitId);
            }
            return members;
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Không thể lấy danh sách thành viên trong căn hộ unitId: {}", unitId, e);
            return List.of();
        }
    }

    /**
     * Đếm số household members (số người) đang ở trong căn hộ
     */
    private long countHouseholdMembersByUnit(UUID unitId) {
        if (unitId == null) {
            log.warn("⚠️ [ResidentCard] unitId is null, returning 0");
            return 0;
        }
        
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("unitId", unitId);
            
            log.debug("🔍 [ResidentCard] Đang đếm số người trong căn hộ unitId: {}", unitId);
            
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(DISTINCT hm.resident_id)
                    FROM data.household_members hm
                    JOIN data.households h ON h.id = hm.household_id
                    WHERE h.unit_id = :unitId
                      AND (hm.left_at IS NULL OR hm.left_at >= CURRENT_DATE)
                      AND (h.end_date IS NULL OR h.end_date >= CURRENT_DATE)
                    """, params, Long.class);
            
            long result = count != null ? count : 0;
            log.info("✅ [ResidentCard] Căn hộ {} có {} người đang ở", unitId, result);
            return result;
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Không thể đếm số người trong căn hộ unitId: {}", unitId, e);
            throw new IllegalStateException(
                String.format("Không thể đếm số người trong căn hộ. Vui lòng thử lại sau. UnitId: %s", unitId), e);
        }
    }

    private String resolveRequestType(String requestType) {
        if (!StringUtils.hasText(requestType)) {
            return "NEW_CARD";
        }
        String normalized = requestType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "REPLACE_CARD", "NEW_CARD" -> normalized;
            default -> "NEW_CARD";
        };
    }

    /**
     * Kiểm tra resident có phải là thành viên household của unit không
     */
    private boolean isHouseholdMember(UUID residentId, UUID unitId) {
        if (residentId == null || unitId == null) {
            return false;
        }
        
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("residentId", residentId)
                    .addValue("unitId", unitId);
            
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(DISTINCT hm.resident_id)
                    FROM data.household_members hm
                    JOIN data.households h ON h.id = hm.household_id
                    WHERE hm.resident_id = :residentId
                      AND h.unit_id = :unitId
                      AND (hm.left_at IS NULL OR hm.left_at >= CURRENT_DATE)
                      AND (h.end_date IS NULL OR h.end_date >= CURRENT_DATE)
                    """, params, Long.class);
            
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Error checking if resident {} is household member of unit {}: {}", 
                    residentId, unitId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Kiểm tra xem resident có phải là primary member hoặc có household_member_requests APPROVED không
     * Nếu có thì không cần check account
     */
    private boolean isPrimaryMemberOrHasApprovedRequest(UUID residentId, UUID unitId) {
        if (residentId == null || unitId == null) {
            return false;
        }
        
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("residentId", residentId)
                    .addValue("unitId", unitId);
            
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(DISTINCT hm.resident_id)
                    FROM data.household_members hm
                    JOIN data.households h ON h.id = hm.household_id
                    JOIN data.residents r ON r.id = hm.resident_id
                    WHERE hm.resident_id = :residentId
                      AND h.unit_id = :unitId
                      AND (hm.left_at IS NULL OR hm.left_at >= CURRENT_DATE)
                      AND (h.end_date IS NULL OR h.end_date >= CURRENT_DATE)
                      AND (
                          -- OWNER (primary member) luôn được phép
                          hm.is_primary = true
                          OR
                          -- Hoặc có request đã được approve
                          EXISTS (
                              SELECT 1 FROM data.household_member_requests hmr
                              WHERE hmr.household_id = hm.household_id
                                AND (hmr.resident_id = r.id 
                                     OR (hmr.resident_id IS NULL 
                                         AND hmr.resident_national_id = r.national_id
                                         AND hmr.resident_phone = r.phone))
                                AND hmr.status = 'APPROVED'
                          )
                      )
                    """, params, Long.class);
            
            boolean result = count != null && count > 0;
            log.debug("🔍 [ResidentCard] Resident {} isPrimaryMemberOrHasApprovedRequest in unit {}: {}", 
                    residentId, unitId, result);
            return result;
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Error checking if resident {} is primary member or has approved request in unit {}: {}", 
                    residentId, unitId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Kiểm tra 2 resident có thuộc cùng household không
     */
    private boolean areInSameHousehold(UUID residentId1, UUID residentId2, UUID unitId) {
        if (residentId1 == null || residentId2 == null || unitId == null) {
            return false;
        }
        
        // Nếu cùng một người thì return true
        if (residentId1.equals(residentId2)) {
            return true;
        }
        
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("residentId1", residentId1)
                    .addValue("residentId2", residentId2)
                    .addValue("unitId", unitId);
            
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(DISTINCT h.id)
                    FROM data.household_members hm1
                    JOIN data.household_members hm2 ON hm1.household_id = hm2.household_id
                    JOIN data.households h ON h.id = hm1.household_id
                    WHERE hm1.resident_id = :residentId1
                      AND hm2.resident_id = :residentId2
                      AND h.unit_id = :unitId
                      AND (hm1.left_at IS NULL OR hm1.left_at >= CURRENT_DATE)
                      AND (hm2.left_at IS NULL OR hm2.left_at >= CURRENT_DATE)
                      AND (h.end_date IS NULL OR h.end_date >= CURRENT_DATE)
                    """, params, Long.class);
            
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Error checking if residents {} and {} are in same household: {}", 
                    residentId1, residentId2, e.getMessage());
            return false;
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String header = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(header)) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private ResidentCardRegistrationDto toDto(ResidentCardRegistration entity) {
        return new ResidentCardRegistrationDto(
                entity.getId(),
                entity.getUserId(),
                entity.getUnitId(),
                entity.getRequestType(),
                entity.getResidentId(),
                entity.getFullName(),
                entity.getApartmentNumber(),
                entity.getBuildingName(),
                entity.getCitizenId(),
                entity.getPhoneNumber(),
                entity.getNote(),
                entity.getStatus(),
                entity.getPaymentStatus(),
                entity.getPaymentAmount(),
                entity.getPaymentDate(),
                entity.getPaymentGateway(),
                entity.getVnpayTransactionRef(),
                entity.getAdminNote(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectionReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public record ResidentCardPaymentResponse(UUID registrationId, String paymentUrl) {}

    public record ResidentCardPaymentResult(UUID registrationId, boolean success, String responseCode, boolean signatureValid, String requestType, String message) {
        public ResidentCardPaymentResult(UUID registrationId, boolean success, String responseCode, boolean signatureValid) {
            this(registrationId, success, responseCode, signatureValid, null, null);
        }
    }

    /**
     * Format BigDecimal price to VND string (e.g., 30000 -> "30.000 VND")
     */
    private String formatVnd(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        String digits = amount.toBigInteger().toString();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            buffer.append(digits.charAt(i));
            int remaining = digits.length() - i - 1;
            if (remaining % 3 == 0 && remaining != 0) {
                buffer.append(".");
            }
        }
        buffer.append(" VND");
        return buffer.toString();
    }
}


