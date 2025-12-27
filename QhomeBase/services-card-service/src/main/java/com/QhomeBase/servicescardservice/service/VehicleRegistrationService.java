package com.QhomeBase.servicescardservice.service;

import com.QhomeBase.servicescardservice.dto.RegisterServiceImageDto;
import com.QhomeBase.servicescardservice.dto.RegisterServiceRequestCreateDto;
import com.QhomeBase.servicescardservice.dto.RegisterServiceRequestDto;
import com.QhomeBase.servicescardservice.model.RegisterServiceImage;
import com.QhomeBase.servicescardservice.model.RegisterServiceRequest;
import com.QhomeBase.servicescardservice.repository.RegisterServiceImageRepository;
import com.QhomeBase.servicescardservice.repository.RegisterServiceRequestRepository;
import com.QhomeBase.servicescardservice.config.VnpayProperties;
import com.QhomeBase.servicescardservice.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleRegistrationService {

    private static final int MAX_IMAGES = 6;
    private static final String SERVICE_TYPE = "VEHICLE_REGISTRATION";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_READY_FOR_PAYMENT = "READY_FOR_PAYMENT";
    
    private final CardPricingService cardPricingService;
    private static final String STATUS_PAYMENT_PENDING = "PAYMENT_PENDING";
    private static final String STATUS_PENDING_REVIEW = "PENDING";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String PAYMENT_VNPAY = "VNPAY";

    private final RegisterServiceRequestRepository requestRepository;
    private final RegisterServiceImageRepository imageRepository;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final BillingClient billingClient;
    private final ResidentUnitLookupService residentUnitLookupService;
    private final NotificationClient notificationClient;
    private final CardFeeReminderService cardFeeReminderService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final BaseServiceClient baseServiceClient;
    private final ConcurrentMap<Long, UUID> orderIdToRegistrationId = new ConcurrentHashMap<>();

    private Path ensureUploadDir() throws IOException {
        Path uploadDir = Paths.get("uploads", "vehicle");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        return uploadDir;
    }

    public List<String> storeImages(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            log.warn("⚠️ [VehicleRegistration] storeImages: Danh sách file rỗng");
            return List.of();
        }
        if (files.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("Chỉ được tải tối đa " + MAX_IMAGES + " ảnh");
        }
        
        log.info("📤 [VehicleRegistration] storeImages: Bắt đầu lưu {} file", files.size());
        Path uploadDir = ensureUploadDir();
        log.debug("📁 [VehicleRegistration] Upload directory: {}", uploadDir.toAbsolutePath());
        
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            try {
                String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                log.debug("📄 [VehicleRegistration] Đang xử lý file {}/{}: {} ({} bytes)", 
                    i + 1, files.size(), originalFilename, file.getSize());
                
                String extension = "";
                int dot = originalFilename.lastIndexOf('.');
                if (dot >= 0) {
                    extension = originalFilename.substring(dot);
                }
                String filename = UUID.randomUUID() + extension;
                Path target = uploadDir.resolve(filename);
                
                long startTime = System.currentTimeMillis();
                Files.copy(file.getInputStream(), target);
                long duration = System.currentTimeMillis() - startTime;
                log.debug("✅ [VehicleRegistration] Đã lưu file {} trong {}ms: {}", 
                    i + 1, duration, filename);
                
                urls.add("/uploads/vehicle/" + filename);
            } catch (IOException e) {
                log.error("❌ [VehicleRegistration] Lỗi khi lưu file {}/{}: {}", 
                    i + 1, files.size(), file.getOriginalFilename(), e);
                throw new IOException("Không thể lưu file \"" + file.getOriginalFilename() + "\": " + e.getMessage(), e);
            }
        }
        
        log.info("✅ [VehicleRegistration] storeImages: Đã lưu thành công {} file", urls.size());
        return urls;
    }

    @Transactional
    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    public RegisterServiceRequestDto createRegistration(UUID userId, RegisterServiceRequestCreateDto dto) {
        validatePayload(dto);
        validateLicensePlateNotDuplicate(dto.licensePlate(), null);

        String requestType = resolveRequestType(dto.requestType());
        
        // Validate REPLACE_CARD request
        if ("REPLACE_CARD".equalsIgnoreCase(requestType)) {
            if (dto.originalCardId() == null) {
                throw new IllegalArgumentException("Yêu cầu cấp lại thẻ phải có ID thẻ gốc (originalCardId)");
            }
            
            // Validate thẻ gốc
            RegisterServiceRequest originalCard = requestRepository.findById(dto.originalCardId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thẻ gốc với ID: " + dto.originalCardId()));
            
            // Thẻ gốc phải ở trạng thái CANCELLED
            if (!STATUS_CANCELLED.equalsIgnoreCase(originalCard.getStatus())) {
                throw new IllegalStateException(
                    String.format("Thẻ gốc phải ở trạng thái CANCELLED trước khi yêu cầu cấp lại. Trạng thái hiện tại: %s", 
                        originalCard.getStatus())
                );
            }
            
            // Thẻ gốc chưa được cấp lại (chưa có thẻ nào có reissuedFromCardId = originalCardId)
            if (requestRepository.existsReissuedCard(dto.originalCardId())) {
                throw new IllegalStateException("Thẻ gốc đã được cấp lại rồi. Mỗi thẻ chỉ được phép cấp lại đúng 1 lần.");
            }
            
            // Kiểm tra quyền: Owner hoặc chủ sở hữu thẻ gốc
            if (originalCard.getUnitId() != null) {
                boolean isOwner = baseServiceClient.isOwnerOfUnit(userId, originalCard.getUnitId(), null);
                if (!isOwner && !userId.equals(originalCard.getUserId())) {
                    throw new IllegalStateException("Chỉ chủ căn hộ hoặc chủ sở hữu thẻ mới được yêu cầu cấp lại thẻ này.");
                }
            } else {
                // Fallback: chỉ chủ sở hữu
                if (!userId.equals(originalCard.getUserId())) {
                    throw new IllegalStateException("Chỉ chủ sở hữu thẻ mới được yêu cầu cấp lại thẻ này.");
                }
            }
            
            log.info("✅ [VehicleRegistration] Validated REPLACE_CARD request: originalCardId={}, userId={}", 
                    dto.originalCardId(), userId);
        }

        RegisterServiceRequest request = RegisterServiceRequest.builder()
                .userId(userId)
                .serviceType(Optional.ofNullable(dto.serviceType()).orElse(SERVICE_TYPE))
                .requestType(requestType)
                .note(dto.note())
                .unitId(dto.unitId())
                .vehicleType(resolveVehicleType(dto.vehicleType()))
                .licensePlate(normalize(dto.licensePlate()))
                .vehicleBrand(normalize(dto.vehicleBrand()))
                .vehicleColor(normalize(dto.vehicleColor()))
                .apartmentNumber(normalize(dto.apartmentNumber()))
                .buildingName(normalize(dto.buildingName()))
                .status(STATUS_READY_FOR_PAYMENT)
                .paymentStatus("UNPAID")
                .paymentAmount(cardPricingService.getPrice("VEHICLE"))
                .reissuedFromCardId("REPLACE_CARD".equalsIgnoreCase(requestType) ? dto.originalCardId() : null)
                .build();

        applyResolvedAddressForUser(
                request,
                userId,
                dto.unitId(),
                dto.apartmentNumber() != null ? dto.apartmentNumber() : request.getApartmentNumber(),
                dto.buildingName() != null ? dto.buildingName() : request.getBuildingName()
        );

        if (dto.imageUrls() != null && !dto.imageUrls().isEmpty()) {
            int imageCount = 0;
            for (String url : dto.imageUrls()) {
                if (url != null && !url.trim().isEmpty() && imageCount < MAX_IMAGES) {
                    RegisterServiceImage image = RegisterServiceImage.builder()
                            .imageUrl(url.trim())
                            .registerServiceRequest(request)
                            .build();
                    request.addImage(image);
                    imageCount++;
                }
            }
            log.info("✅ [VehicleRegistration] Đã thêm {} ảnh vào registration mới (requestType: {})", 
                    imageCount, dto.requestType());
        } else {
            log.warn("⚠️ [VehicleRegistration] Không có ảnh trong request (requestType: {})", 
                    dto.requestType());
        }

        RegisterServiceRequest saved = requestRepository.save(request);
        log.info("✅ [VehicleRegistration] Đã tạo registration mới với ID: {}, có {} ảnh", 
                saved.getId(), saved.getImages().size());
        return toDto(saved);
    }

    @Transactional
    public RegisterServiceRequestDto updateRegistration(UUID userId, UUID registrationId, RegisterServiceRequestCreateDto dto) {
        RegisterServiceRequest request = requestRepository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));

        if (!Objects.equals(request.getPaymentStatus(), "UNPAID")) {
            throw new IllegalStateException("Đăng ký đã thanh toán, không thể chỉnh sửa");
        }

        validatePayload(dto);
        // Kiểm tra trùng biển số xe (exclude registration hiện tại)
        validateLicensePlateNotDuplicate(dto.licensePlate(), registrationId);

        request.setServiceType(Optional.ofNullable(dto.serviceType()).orElse(SERVICE_TYPE));
        request.setRequestType(resolveRequestType(dto.requestType()));
        request.setNote(dto.note());
        request.setUnitId(dto.unitId());
        request.setVehicleType(resolveVehicleType(dto.vehicleType()));
        request.setLicensePlate(normalize(dto.licensePlate()));
        request.setVehicleBrand(normalize(dto.vehicleBrand()));
        request.setVehicleColor(normalize(dto.vehicleColor()));
        request.setStatus(STATUS_READY_FOR_PAYMENT);
        request.setAdminNote(null);
        request.setApprovedAt(null);
        request.setApprovedBy(null);
        request.setRejectionReason(null);

        applyResolvedAddressForUser(
                request,
                userId,
                dto.unitId(),
                dto.apartmentNumber(),
                dto.buildingName()
        );

        imageRepository.deleteByRegisterServiceRequestId(request.getId());
        request.getImages().clear();
        if (dto.imageUrls() != null) {
            dto.imageUrls().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .limit(MAX_IMAGES)
                    .map(url -> RegisterServiceImage.builder().imageUrl(url).registerServiceRequest(request).build())
                    .forEach(request::addImage);
        }

        RegisterServiceRequest saved = Objects.requireNonNull(requestRepository.save(request));
        return toDto(saved);
    }

    @Transactional
    public VehicleRegistrationPaymentResponse initiatePayment(UUID userId, UUID registrationId, HttpServletRequest request) {
        RegisterServiceRequest registration = requestRepository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));

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
            // Cho phép tiếp tục thanh toán nếu payment_status là UNPAID, PAYMENT_PENDING, hoặc PAYMENT_IN_PROGRESS
            // (PAYMENT_IN_PROGRESS xảy ra khi user đang thanh toán dở bằng VNPay trong vòng 10 phút)
            if (!Objects.equals(paymentStatus, "UNPAID") && 
                !Objects.equals(paymentStatus, "PAYMENT_PENDING") && 
                !Objects.equals(paymentStatus, "PAYMENT_IN_PROGRESS")) {
                throw new IllegalStateException("Đăng ký đã thanh toán hoặc không thể tiếp tục thanh toán");
            }
        }
        registration.setStatus(STATUS_PAYMENT_PENDING);
        registration.setPaymentStatus("PAYMENT_IN_PROGRESS");
        registration.setPaymentGateway(PAYMENT_VNPAY);
        registration.setVnpayInitiatedAt(OffsetDateTime.now());
        RegisterServiceRequest saved = requestRepository.save(registration);

        long orderId = Math.abs(saved.getId().hashCode());
        if (orderId == 0) {
            orderId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        }
        orderIdToRegistrationId.put(orderId, saved.getId());

        String clientIp = resolveClientIp(request);
        String orderInfo = "Thanh toán đăng ký xe " + (saved.getLicensePlate() != null ? saved.getLicensePlate() : saved.getId());
        String returnUrl = vnpayProperties.getReturnUrl();
        BigDecimal registrationFee = cardPricingService.getPrice("VEHICLE");
        var paymentResult = vnpayService.createPaymentUrlWithRef(orderId, orderInfo, registrationFee, clientIp, returnUrl);
        
        // Save transaction reference to database for fallback lookup
        saved.setVnpayTransactionRef(paymentResult.transactionRef());
        requestRepository.save(saved);

        return new VehicleRegistrationPaymentResponse(saved.getId(), paymentResult.paymentUrl());
    }

    @Transactional
    public VehicleRegistrationPaymentResponse createAndInitiatePayment(UUID userId, RegisterServiceRequestCreateDto dto, HttpServletRequest request) {
        RegisterServiceRequestDto created = createRegistration(userId, dto);
        return initiatePayment(userId, created.id(), request);
    }

    @Transactional(readOnly = true)
    public RegisterServiceRequestDto getRegistration(UUID userId, UUID registrationId) {
        // Get registration without userId check first (to check permission)
        RegisterServiceRequest registration = requestRepository.findByIdWithImages(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));
        
        // Check permission: Owner can view any household member's registration, household members can only view their own
        if (registration.getUnitId() != null) {
            boolean isOwner = baseServiceClient.isOwnerOfUnit(userId, registration.getUnitId(), null);
            
            if (!isOwner) {
                // Not Owner - household member can only view their own registration
                // Check by userId (RegisterServiceRequest doesn't have residentId field)
                if (!userId.equals(registration.getUserId())) {
                    log.warn("⚠️ [VehicleRegistration] User {} không phải Owner và không phải chủ sở hữu đăng ký {}, không được phép xem", 
                            userId, registrationId);
                    throw new IllegalArgumentException("Không tìm thấy đăng ký xe");
                }
            }
        } else {
            // Fallback: if no unitId, only allow viewing own registration
            if (!userId.equals(registration.getUserId())) {
                throw new IllegalArgumentException("Không tìm thấy đăng ký xe");
            }
        }
        
        return toDto(registration);
    }

    @Transactional(readOnly = true)
    public List<RegisterServiceRequestDto> getRegistrationsForAdmin(String status, String paymentStatus) {
        List<RegisterServiceRequest> registrations =
                requestRepository.findAllByServiceTypeWithImages(SERVICE_TYPE);
        return registrations.stream()
                .filter(reg -> {
                    if (status == null || status.isBlank()) {
                        return true; // No status filter
                    }
                    String regStatus = reg.getStatus();
                    // If filtering for PENDING, also include READY_FOR_PAYMENT and PAYMENT_PENDING
                    // as these are also pending admin approval
                    if ("PENDING".equalsIgnoreCase(status)) {
                        return "PENDING".equalsIgnoreCase(regStatus) 
                            || STATUS_READY_FOR_PAYMENT.equalsIgnoreCase(regStatus)
                            || STATUS_PAYMENT_PENDING.equalsIgnoreCase(regStatus);
                    }
                    return status.equalsIgnoreCase(regStatus);
                })
                .filter(reg -> paymentStatus == null || paymentStatus.isBlank() || paymentStatus.equalsIgnoreCase(reg.getPaymentStatus()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RegisterServiceRequestDto getRegistrationForAdmin(UUID registrationId) {
        RegisterServiceRequest registration = requestRepository.findByIdWithImages(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));
        return toDto(registration);
    }

    @Transactional
    public void cancelRegistration(UUID userId, UUID registrationId) {
        // Get registration without userId check first (to check permission)
        RegisterServiceRequest registration = requestRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));
        
        // Check permission: Owner can cancel any household member's card, household members can only cancel their own
        if (registration.getUnitId() != null) {
            boolean isOwner = baseServiceClient.isOwnerOfUnit(userId, registration.getUnitId(), null);
            
            if (isOwner) {
                // Owner can cancel any household member's card in the same unit
                log.info("✅ [VehicleRegistration] Owner {} đã hủy đăng ký {} của household member trong unit {}", 
                        userId, registrationId, registration.getUnitId());
            } else {
                // Not Owner - household member can only cancel their own card
                // Check by userId first
                boolean canCancel = userId.equals(registration.getUserId());
                
                // Note: RegisterServiceRequest doesn't have residentId field, so we can only check by userId
                // If userId doesn't match, cannot cancel
                if (!canCancel) {
                    log.warn("⚠️ [VehicleRegistration] User {} không phải Owner và không phải người tạo đăng ký {}, không được phép hủy", 
                            userId, registrationId);
                    log.warn("⚠️ [VehicleRegistration] Registration userId: {}, current userId: {}", 
                            registration.getUserId(), userId);
                    throw new IllegalStateException("Chỉ chủ căn hộ mới được quyền hủy thẻ của các thành viên. Bạn chỉ có thể hủy thẻ của chính mình.");
                }
                log.info("✅ [VehicleRegistration] Household member {} đã hủy đăng ký {} của chính mình", userId, registrationId);
            }
        } else {
            // Fallback: if no unitId, only allow canceling own registration
            // Check by userId (RegisterServiceRequest doesn't have residentId field)
            if (!userId.equals(registration.getUserId())) {
                throw new IllegalStateException("Bạn chỉ có thể hủy thẻ của chính mình.");
            }
        }
        
        if (STATUS_CANCELLED.equalsIgnoreCase(registration.getStatus())) {
            log.info("ℹ️ [VehicleRegistration] Đăng ký {} đã được hủy trước đó", registrationId);
            return;
        }
        
        registration.setStatus(STATUS_CANCELLED);
        registration.setUpdatedAt(OffsetDateTime.now());
        requestRepository.save(registration);
        log.info("✅ [VehicleRegistration] Đăng ký {} đã được hủy thành công", registrationId);
    }

    @Transactional
    public RegisterServiceRequestDto approveRegistration(UUID registrationId, UUID adminId, String adminNote, String issueMessage, OffsetDateTime issueTime) {
        RegisterServiceRequest registration = requestRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));

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
            log.warn("⚠️ [VehicleRegistration] Registration {} already approved. Status not changing. Skipping notification.", 
                    registrationId);
            // Still allow update of adminNote, issueMessage, issueTime if provided
            if (adminNote != null) {
                registration.setAdminNote(adminNote);
            }
            registration.setUpdatedAt(OffsetDateTime.now(ZoneId.of("UTC")));
            RegisterServiceRequest saved = requestRepository.save(registration);
            return toDto(saved);
        }

        // Check payment status - must be PAID before approval
        if (!"PAID".equalsIgnoreCase(registration.getPaymentStatus())) {
            throw new IllegalStateException(
                String.format("Không thể duyệt thẻ. Thẻ phải đã thanh toán trước khi được duyệt. Trạng thái thanh toán hiện tại: %s", 
                    registration.getPaymentStatus())
            );
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        registration.setStatus("APPROVED");
        registration.setApprovedBy(adminId);
        registration.setApprovedAt(now);
        registration.setAdminNote(adminNote);
        registration.setUpdatedAt(now);

        RegisterServiceRequest saved = requestRepository.save(registration);

        // Create reminder state if card is already paid (for test mode)
        // In production, reminder state will be created after payment callback
        if ("PAID".equalsIgnoreCase(saved.getPaymentStatus())) {
            try {
                // Resolve residentId from userId and unitId
                UUID residentId = residentUnitLookupService.resolveByUser(saved.getUserId(), saved.getUnitId())
                        .map(ResidentUnitLookupService.AddressInfo::residentId)
                        .orElse(null);
                
                cardFeeReminderService.resetReminderAfterPayment(
                        CardFeeReminderService.CardFeeType.VEHICLE,
                        saved.getId(),
                        saved.getUnitId(),
                        residentId,
                        saved.getUserId(),
                        saved.getApartmentNumber(),
                        saved.getBuildingName(),
                        saved.getPaymentDate() != null ? saved.getPaymentDate() : now
                );
                log.info("✅ [VehicleRegistration] Đã tạo reminder state cho thẻ {} sau khi approve", saved.getId());
            } catch (Exception e) {
                log.warn("⚠️ [VehicleRegistration] Không thể tạo reminder state sau khi approve: {}", e.getMessage());
            }
        }

        // Send notification to resident ONLY if status changed from PENDING/READY_FOR_PAYMENT to APPROVED
        if (statusChanging) {
            sendVehicleCardApprovalNotification(saved, issueMessage, issueTime);
            log.info("✅ [VehicleRegistration] Admin {} đã approve đăng ký {} (status changed from {} to APPROVED). Notification sent.", 
                    adminId, registrationId, oldStatus);
        } else {
            log.info("✅ [VehicleRegistration] Admin {} đã approve đăng ký {} (status unchanged, notification skipped).", 
                    adminId, registrationId);
        }
        
        return toDto(saved);
    }

    @Transactional
    public RegisterServiceRequestDto cancelRegistration(UUID registrationId, UUID adminId, String adminNote) {
        RegisterServiceRequest registration = requestRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));

        // Save old status to check if status is actually changing
        String oldStatus = registration.getStatus();
        
        // Admin cancel logic - set status to REJECTED (bị từ chối)
        // Note: Cư dân hủy sẽ set status = CANCELLED, admin hủy sẽ set status = REJECTED
        if (STATUS_REJECTED.equalsIgnoreCase(oldStatus)) {
            throw new IllegalStateException("Đăng ký đã bị từ chối");
        }

        // Check if status is actually changing from PENDING/READY_FOR_PAYMENT to REJECTED
        // Only send notification if status is changing (not already REJECTED)
        boolean statusChanging = !STATUS_REJECTED.equalsIgnoreCase(oldStatus) 
                && (STATUS_PENDING_REVIEW.equalsIgnoreCase(oldStatus) 
                    || STATUS_READY_FOR_PAYMENT.equalsIgnoreCase(oldStatus));

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        registration.setStatus(STATUS_REJECTED);
        registration.setAdminNote(adminNote);
        registration.setRejectionReason(adminNote);
        registration.setUpdatedAt(now);

        RegisterServiceRequest saved = requestRepository.save(registration);

        // Send notification to resident ONLY if status changed from PENDING/READY_FOR_PAYMENT to REJECTED
        if (statusChanging) {
            sendVehicleCardRejectionNotification(saved, adminNote);
            log.info("✅ [VehicleRegistration] Admin {} đã reject đăng ký {} (status changed from {} to REJECTED). Notification sent.", 
                    adminId, registrationId, oldStatus);
        } else {
            log.info("✅ [VehicleRegistration] Admin {} đã reject đăng ký {} (status unchanged, notification skipped).", 
                    adminId, registrationId);
        }

        log.info("✅ [VehicleRegistration] Admin {} đã cancel (reject) đăng ký {}", adminId, registrationId);
        return toDto(saved);
    }

    private void sendVehicleCardApprovalNotification(RegisterServiceRequest registration, String issueMessage, OffsetDateTime issueTime) {
        try {
            log.info("🔔 [VehicleRegistration] ========== SENDING APPROVAL NOTIFICATION ==========");
            log.info("🔔 [VehicleRegistration] Registration ID: {}", registration.getId());
            log.info("🔔 [VehicleRegistration] UserId: {}", registration.getUserId());
            log.info("🔔 [VehicleRegistration] UnitId: {}", registration.getUnitId());
            
            // Resolve residentId from userId and unitId - CARD_APPROVED is PRIVATE (only resident who created the request can see)
            log.info("🔔 [VehicleRegistration] Resolving residentId from userId and unitId...");
            UUID residentId = residentUnitLookupService.resolveByUser(registration.getUserId(), registration.getUnitId())
                    .map(ResidentUnitLookupService.AddressInfo::residentId)
                    .orElse(null);

            // Fallback: Nếu không tìm thấy từ household_members, query trực tiếp từ residents table
            if (residentId == null) {
                log.warn("⚠️ [VehicleRegistration] Không tìm thấy residentId từ household_members, thử query trực tiếp từ residents table...");
                log.warn("⚠️ [VehicleRegistration] UserId: {}, UnitId: {}", registration.getUserId(), registration.getUnitId());
                
                // Query trực tiếp từ residents table bằng userId
                try {
                    residentId = baseServiceClient.findResidentIdByUserId(registration.getUserId(), null);
                    if (residentId != null) {
                        log.info("✅ [VehicleRegistration] Tìm thấy residentId từ residents table: {}", residentId);
                    } else {
                        log.error("❌ [VehicleRegistration] Không tìm thấy residentId trong residents table");
                    }
                } catch (Exception e) {
                    log.error("❌ [VehicleRegistration] Lỗi khi query residentId từ base-service: {}", e.getMessage());
                }
            }

            if (residentId == null) {
                log.error("❌ [VehicleRegistration] ========== RESIDENT ID RESOLUTION FAILED ==========");
                log.error("❌ [VehicleRegistration] Không thể tìm thấy residentId cho userId={}, unitId={}", 
                        registration.getUserId(), registration.getUnitId());
                log.error("❌ [VehicleRegistration] Không thể gửi notification cho registrationId: {}", registration.getId());
                log.error("❌ [VehicleRegistration] Notification sẽ không được gửi đến resident!");
                return;
            }
            
            log.info("✅ [VehicleRegistration] ResidentId resolved successfully: {}", residentId);

            // Get current card price from database
            BigDecimal currentPrice = cardPricingService.getPrice("VEHICLE");
            String formattedPrice = formatVnd(currentPrice);

            String title = "Thẻ xe đã được duyệt";
            
            // Format thời gian nhận thẻ (từ issueTime nếu có, nếu không thì dùng approvedAt)
            String issueTimeFormatted = "";
            OffsetDateTime timeToUse = issueTime != null ? issueTime : registration.getApprovedAt();
            if (timeToUse != null) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"));
                issueTimeFormatted = timeToUse.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))
                        .format(dateFormatter);
            }
            
            // Lấy biển số xe
            String licensePlate = registration.getLicensePlate() != null ? registration.getLicensePlate() : "";
            
            String message;
            // Ưu tiên: issueMessage > adminNote (note) > message tự động
            if (issueMessage != null && !issueMessage.isBlank()) {
                // Admin đã ghi issueMessage riêng cho notification
                message = issueMessage;
                log.info("📝 [VehicleRegistration] Sử dụng issueMessage từ admin: {}", message);
            } else if (registration.getAdminNote() != null && !registration.getAdminNote().isBlank()) {
                // Admin đã ghi note nhưng không ghi issueMessage, dùng note làm notification message
                message = registration.getAdminNote();
                log.info("📝 [VehicleRegistration] Sử dụng adminNote (note) từ admin: {}", message);
            } else {
                // Tự động tạo message: "Thẻ xe với biển số (biển số) được tạo thành công và sẽ nhận vào (ngày giờ)"
                if (issueTimeFormatted.isEmpty()) {
                    message = String.format("Thẻ xe với biển số %s được tạo thành công.", licensePlate);
                } else {
                    message = String.format("Thẻ xe với biển số %s được tạo thành công và sẽ nhận vào %s.", 
                            licensePlate, issueTimeFormatted);
                }
                log.info("📝 [VehicleRegistration] Sử dụng message tự động: {}", message);
            }

            Map<String, String> data = new HashMap<>();
            data.put("cardType", "VEHICLE_CARD");
            data.put("registrationId", registration.getId().toString());
            data.put("price", currentPrice.toString());
            data.put("formattedPrice", formattedPrice);
            if (registration.getLicensePlate() != null) {
                data.put("licensePlate", registration.getLicensePlate());
            }
            if (registration.getApartmentNumber() != null) {
                data.put("apartmentNumber", registration.getApartmentNumber());
            }
            if (!issueTimeFormatted.isEmpty()) {
                data.put("issueTime", issueTimeFormatted);
            }
            if (timeToUse != null) {
                data.put("issueTimeTimestamp", timeToUse.toString());
            }

            log.info("📤 [VehicleRegistration] ========== CALLING NOTIFICATION CLIENT ==========");
            log.info("📤 [VehicleRegistration] ResidentId: {}", residentId);
            log.info("📤 [VehicleRegistration] BuildingId: null (private notification)");
            log.info("📤 [VehicleRegistration] Type: CARD_APPROVED");
            log.info("📤 [VehicleRegistration] Title: {}", title);
            log.info("📤 [VehicleRegistration] Message: {}", message);
            log.info("📤 [VehicleRegistration] ReferenceId: {}", registration.getId());
            log.info("📤 [VehicleRegistration] ReferenceType: VEHICLE_CARD_REGISTRATION");
            log.info("📤 [VehicleRegistration] Data: {}", data);

            // Send PRIVATE notification to specific resident (residentId = residentId, buildingId = null)
            notificationClient.sendResidentNotification(
                    residentId, // residentId for private notification
                    null, // buildingId = null for private notification
                    "CARD_APPROVED",
                    title,
                    message,
                    registration.getId(),
                    "VEHICLE_CARD_REGISTRATION",
                    data
            );

            log.info("✅ [VehicleRegistration] ========== NOTIFICATION CLIENT CALLED ==========");
            log.info("✅ [VehicleRegistration] Đã gọi notificationClient.sendResidentNotification()");
            log.info("✅ [VehicleRegistration] ResidentId: {}", residentId);
        } catch (Exception e) {
            log.error("❌ [VehicleRegistration] ========== EXCEPTION IN APPROVAL NOTIFICATION ==========");
            log.error("❌ [VehicleRegistration] Không thể gửi notification approval cho registrationId: {}", 
                    registration.getId(), e);
            log.error("❌ [VehicleRegistration] Exception type: {}", e.getClass().getName());
            log.error("❌ [VehicleRegistration] Exception message: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("❌ [VehicleRegistration] Caused by: {}", e.getCause().getMessage());
            }
        }
    }

    private void sendVehicleCardRejectionNotification(RegisterServiceRequest registration, String rejectionReason) {
        try {
            log.info("🔔 [VehicleRegistration] ========== SENDING REJECTION NOTIFICATION ==========");
            log.info("🔔 [VehicleRegistration] Registration ID: {}", registration.getId());
            log.info("🔔 [VehicleRegistration] UserId: {}", registration.getUserId());
            log.info("🔔 [VehicleRegistration] UnitId: {}", registration.getUnitId());
            
            // Resolve residentId from userId and unitId - CARD_REJECTED is PRIVATE (only resident who created the request can see)
            log.info("🔔 [VehicleRegistration] Resolving residentId from userId and unitId...");
            UUID residentId = residentUnitLookupService.resolveByUser(registration.getUserId(), registration.getUnitId())
                    .map(ResidentUnitLookupService.AddressInfo::residentId)
                    .orElse(null);

            // Fallback: Nếu không tìm thấy từ household_members, query trực tiếp từ residents table
            if (residentId == null) {
                log.warn("⚠️ [VehicleRegistration] Không tìm thấy residentId từ household_members, thử query trực tiếp từ residents table...");
                log.warn("⚠️ [VehicleRegistration] UserId: {}, UnitId: {}", registration.getUserId(), registration.getUnitId());
                
                // Query trực tiếp từ residents table bằng userId
                try {
                    residentId = baseServiceClient.findResidentIdByUserId(registration.getUserId(), null);
                    if (residentId != null) {
                        log.info("✅ [VehicleRegistration] Tìm thấy residentId từ residents table: {}", residentId);
                    } else {
                        log.error("❌ [VehicleRegistration] Không tìm thấy residentId trong residents table");
                    }
                } catch (Exception e) {
                    log.error("❌ [VehicleRegistration] Lỗi khi query residentId từ base-service: {}", e.getMessage());
                }
            }

            if (residentId == null) {
                log.error("❌ [VehicleRegistration] ========== RESIDENT ID RESOLUTION FAILED ==========");
                log.error("❌ [VehicleRegistration] Không thể tìm thấy residentId cho userId={}, unitId={}", 
                        registration.getUserId(), registration.getUnitId());
                log.error("❌ [VehicleRegistration] Không thể gửi notification cho registrationId: {}", registration.getId());
                log.error("❌ [VehicleRegistration] Notification sẽ không được gửi đến resident!");
                return;
            }
            
            log.info("✅ [VehicleRegistration] ResidentId resolved successfully: {}", residentId);

            // Get current card price from database
            BigDecimal currentPrice = cardPricingService.getPrice("VEHICLE");
            String formattedPrice = formatVnd(currentPrice);

            String title = "Thẻ xe bị từ chối";
            String message = rejectionReason != null && !rejectionReason.isBlank() 
                    ? String.format("Yêu cầu đăng ký thẻ xe %s của bạn đã bị từ chối. Phí đăng ký: %s. Lý do: %s", 
                            registration.getLicensePlate() != null ? registration.getLicensePlate() : "",
                            formattedPrice, rejectionReason)
                    : String.format("Yêu cầu đăng ký thẻ xe %s của bạn đã bị từ chối. Phí đăng ký: %s. Vui lòng liên hệ quản trị viên để biết thêm chi tiết.", 
                            registration.getLicensePlate() != null ? registration.getLicensePlate() : "",
                            formattedPrice);

            Map<String, String> data = new HashMap<>();
            data.put("cardType", "VEHICLE_CARD");
            data.put("registrationId", registration.getId().toString());
            data.put("status", "REJECTED");
            data.put("price", currentPrice.toString());
            data.put("formattedPrice", formattedPrice);
            if (registration.getLicensePlate() != null) {
                data.put("licensePlate", registration.getLicensePlate());
            }
            if (registration.getApartmentNumber() != null) {
                data.put("apartmentNumber", registration.getApartmentNumber());
            }
            if (rejectionReason != null) {
                data.put("rejectionReason", rejectionReason);
            }

            // Send PRIVATE notification to specific resident (residentId = residentId, buildingId = null)
            notificationClient.sendResidentNotification(
                    residentId, // residentId for private notification
                    null, // buildingId = null for private notification
                    "CARD_REJECTED",
                    title,
                    message,
                    registration.getId(),
                    "VEHICLE_CARD_REGISTRATION",
                    data
            );

            log.info("✅ [VehicleRegistration] Đã gửi notification rejection riêng tư cho residentId: {}", residentId);
        } catch (Exception e) {
            log.error("❌ [VehicleRegistration] Không thể gửi notification rejection cho registrationId: {}", 
                    registration.getId(), e);
        }
    }


    @Transactional
    public RegisterServiceRequestDto markPaymentAsPaid(UUID registrationId, UUID adminId) {
        RegisterServiceRequest registration = requestRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));

        String currentPaymentStatus = registration.getPaymentStatus();
        if ("PAID".equalsIgnoreCase(currentPaymentStatus)) {
            throw new IllegalStateException("Đăng ký đã được đánh dấu là đã thanh toán");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        registration.setPaymentStatus("PAID");
        registration.setPaymentDate(now);
        registration.setPaymentGateway("MANUAL");
        registration.setUpdatedAt(now);

        // Nếu là gia hạn (status = NEEDS_RENEWAL hoặc SUSPENDED), sau khi thanh toán → set status = APPROVED
        // Nếu là đăng ký mới, sau khi thanh toán → set status = PENDING_REVIEW (chờ admin duyệt)
        String currentStatus = registration.getStatus();
        if ("NEEDS_RENEWAL".equals(currentStatus) || "SUSPENDED".equals(currentStatus)) {
            registration.setStatus(STATUS_APPROVED);
            registration.setApprovedAt(now);
            log.info("✅ [VehicleRegistration] Admin đánh dấu thanh toán thành công (gia hạn), thẻ {} đã được set status = APPROVED", registration.getId());
        } else {
            registration.setStatus(STATUS_PENDING_REVIEW);
            log.info("✅ [VehicleRegistration] Admin đánh dấu thanh toán thành công (đăng ký mới), thẻ {} đã được set status = PENDING", registration.getId());
        }

        RegisterServiceRequest saved = requestRepository.save(registration);

        // Record payment in billing service
        try {
            billingClient.recordVehicleRegistrationPayment(
                    saved.getId(),
                    saved.getUserId(),
                    saved.getUnitId(),
                    saved.getVehicleType(),
                    saved.getLicensePlate(),
                    saved.getRequestType(),
                    saved.getNote(),
                    saved.getPaymentAmount(),
                    now,
                    "MANUAL_" + saved.getId().toString(), // transactionRef
                    null, // transactionNo
                    "MANUAL", // bankCode
                    null, // cardType
                    "00" // responseCode (success)
            );
            log.info("✅ [VehicleRegistration] Đã ghi nhận thanh toán vào billing service");
        } catch (Exception e) {
            log.warn("⚠️ [VehicleRegistration] Không thể ghi nhận thanh toán vào billing service: {}", e.getMessage());
        }

        return toDto(saved);
    }

    @Transactional
    public RegisterServiceRequestDto rejectRegistration(UUID registrationId, UUID adminId, String reason) {
        RegisterServiceRequest registration = requestRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));

        if ("REJECTED".equalsIgnoreCase(registration.getStatus())) {
            throw new IllegalStateException("Đăng ký đã bị từ chối");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        registration.setStatus("REJECTED");
        registration.setAdminNote(reason);
        registration.setRejectionReason(reason);
        registration.setUpdatedAt(now);

        RegisterServiceRequest saved = requestRepository.save(registration);

        // Send notification to resident
        sendVehicleCardRejectionNotification(saved, reason);

        log.info("✅ [VehicleRegistration] Admin {} đã reject đăng ký {}", adminId, registrationId);
        return toDto(saved);
    }

    @Transactional
    public VehicleRegistrationPaymentResult handleVnpayCallback(Map<String, String> params) {
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
            log.error("❌ [VehicleRegistration] Cannot parse orderId from txnRef: {}", txnRef);
            throw new IllegalArgumentException("Invalid transaction reference format");
        }

        UUID registrationId = orderIdToRegistrationId.get(orderId);
        RegisterServiceRequest registration = null;

        // Try to find registration by orderId map first
        if (registrationId != null) {
            var optional = requestRepository.findById(registrationId);
            if (optional.isPresent()) {
                registration = optional.get();
                log.info("✅ [VehicleRegistration] Found registration by orderId map: registrationId={}, orderId={}", 
                        registrationId, orderId);
            }
        }

        // Fallback: try to find by transaction reference
        if (registration == null) {
            var optionalByTxnRef = requestRepository.findByVnpayTransactionRef(txnRef);
            if (optionalByTxnRef.isPresent()) {
                registration = optionalByTxnRef.get();
                log.info("✅ [VehicleRegistration] Found registration by txnRef: registrationId={}, txnRef={}", 
                        registration.getId(), txnRef);
            }
        }

        // If still not found, throw exception with orderId for debugging
        if (registration == null) {
            log.error("❌ [VehicleRegistration] Cannot find registration: orderId={}, txnRef={}, mapSize={}", 
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
            registration.setPaymentStatus("PAID");
            applyResolvedAddressForUser(
                    registration,
                    registration.getUserId(),
                    registration.getUnitId(),
                    registration.getApartmentNumber(),
                    registration.getBuildingName()
            );
            
            registration.setPaymentGateway(PAYMENT_VNPAY);
            // Use current time for payment date to ensure accurate timestamp
            OffsetDateTime payDate = OffsetDateTime.now();
            registration.setPaymentDate(payDate);
            
            // Nếu là gia hạn (status = NEEDS_RENEWAL hoặc SUSPENDED), sau khi thanh toán thành công → set status = APPROVED
            // Nếu là đăng ký mới, sau khi thanh toán → set status = PENDING_REVIEW (chờ admin duyệt)
            String currentStatus = registration.getStatus();
            if ("NEEDS_RENEWAL".equals(currentStatus) || "SUSPENDED".equals(currentStatus)) {
                registration.setStatus(STATUS_APPROVED);
                registration.setApprovedAt(OffsetDateTime.now()); // Cập nhật lại approved_at khi gia hạn
                log.info("✅ [VehicleRegistration] Gia hạn thành công, thẻ {} đã được set lại status = APPROVED", registration.getId());
                
                // Reset reminder cycle sau khi gia hạn (approved_at đã được set ở trên)
                cardFeeReminderService.resetReminderAfterPayment(
                        CardFeeReminderService.CardFeeType.VEHICLE,
                        registration.getId(),
                        registration.getUnitId(),
                        null, // Vehicle card không có residentId
                        registration.getUserId(),
                        registration.getApartmentNumber(),
                        registration.getBuildingName(),
                        payDate // payment_date mới (approved_at sẽ được lấy từ registration.getApprovedAt())
                );
            } else {
                registration.setStatus(STATUS_PENDING_REVIEW);
            }
            requestRepository.save(registration);

            // Email placeholder – actual implementation depends on user info lookup
            log.info("✅ [VehicleRegistration] Thanh toán thành công cho đăng ký {}", registrationId);
            java.math.BigDecimal amount = registration.getPaymentAmount();
            billingClient.recordVehicleRegistrationPayment(
                    registrationId,
                    registration.getUserId(),
                    registration.getUnitId(),
                    registration.getVehicleType(),
                    registration.getLicensePlate(),
                    registration.getRequestType(),
                    registration.getNote(),
                    amount,
                    payDate,
                    txnRef,
                    params.get("vnp_TransactionNo"),
                    params.get("vnp_BankCode"),
                    params.get("vnp_CardType"),
                    responseCode
            );

            UUID residentId = residentUnitLookupService.resolveByUser(registration.getUserId(), registration.getUnitId())
                    .map(ResidentUnitLookupService.AddressInfo::residentId)
                    .orElse(null);

            cardFeeReminderService.resetReminderAfterPayment(
                    CardFeeReminderService.CardFeeType.VEHICLE,
                    registration.getId(),
                    registration.getUnitId(),
                    residentId,
                    registration.getUserId(),
                    registration.getApartmentNumber(),
                    registration.getBuildingName(),
                    payDate
            );
            orderIdToRegistrationId.remove(orderId);
            return new VehicleRegistrationPaymentResult(registrationId, true, responseCode, signatureValid);
        }

        registration.setStatus(STATUS_READY_FOR_PAYMENT);
        registration.setPaymentStatus("UNPAID");
        requestRepository.save(registration);
        orderIdToRegistrationId.remove(orderId);
        return new VehicleRegistrationPaymentResult(registrationId, false, responseCode, signatureValid);
    }

    private void applyResolvedAddressForUser(RegisterServiceRequest request,
                                             UUID userId,
                                             UUID unitId,
                                             String fallbackApartment,
                                             String fallbackBuilding) {
        residentUnitLookupService.resolveByUser(userId, unitId).ifPresentOrElse(info -> {
            String resolvedApartment = info.apartmentNumber();
            String resolvedBuilding = info.buildingName();
            request.setApartmentNumber(normalize(resolvedApartment != null ? resolvedApartment : fallbackApartment));
            request.setBuildingName(normalize(resolvedBuilding != null ? resolvedBuilding : fallbackBuilding));
        }, () -> {
            request.setApartmentNumber(normalize(fallbackApartment));
            request.setBuildingName(normalize(fallbackBuilding));
        });
    }


    private void validatePayload(RegisterServiceRequestCreateDto dto) {
        if (dto.unitId() == null) {
            throw new IllegalArgumentException("Căn hộ là bắt buộc");
        }
        if (dto.imageUrls() != null && dto.imageUrls().size() > MAX_IMAGES) {
            throw new IllegalArgumentException("Chỉ được chọn tối đa " + MAX_IMAGES + " ảnh");
        }
        if (dto.licensePlate() == null || dto.licensePlate().isBlank()) {
            throw new IllegalArgumentException("Biển số xe là bắt buộc");
        }
        if (dto.vehicleType() == null || dto.vehicleType().isBlank()) {
            throw new IllegalArgumentException("Loại phương tiện là bắt buộc");
        }
    }

    /**
     * Kiểm tra biển số xe đã tồn tại trong database chưa
     * Chỉ kiểm tra với các registration đã được approve hoặc đã thanh toán (không bị reject/cancel)
     */
    private void validateLicensePlateNotDuplicate(String licensePlate, UUID excludeRegistrationId) {
        if (licensePlate == null || licensePlate.isBlank()) {
            return; // Đã được validate trong validatePayload
        }

        String normalizedLicensePlate = normalize(licensePlate);
        if (normalizedLicensePlate == null || normalizedLicensePlate.isBlank()) {
            return;
        }

        List<RegisterServiceRequest> existingRegistrations;
        if (excludeRegistrationId != null) {
            // Khi update, exclude registration hiện tại
            existingRegistrations = requestRepository.findByServiceTypeAndLicensePlateIgnoreCaseExcludingId(
                    SERVICE_TYPE, normalizedLicensePlate, excludeRegistrationId);
        } else {
            // Khi create, kiểm tra tất cả
            existingRegistrations = requestRepository.findByServiceTypeAndLicensePlateIgnoreCase(
                    SERVICE_TYPE, normalizedLicensePlate);
        }

        if (!existingRegistrations.isEmpty()) {
            String existingStatus = existingRegistrations.get(0).getStatus();
            String existingPaymentStatus = existingRegistrations.get(0).getPaymentStatus();
            throw new IllegalArgumentException(
                    String.format("Biển số xe '%s' đã được đăng ký trong hệ thống. Trạng thái: %s, Thanh toán: %s",
                            normalizedLicensePlate, existingStatus, existingPaymentStatus));
        }
    }

    private String resolveRequestType(String requestType) {
        if (requestType == null) {
            return "NEW_CARD";
        }
        return switch (requestType.toUpperCase(Locale.ROOT)) {
            case "REPLACE_CARD", "NEW_CARD" -> requestType.toUpperCase(Locale.ROOT);
            default -> "NEW_CARD";
        };
    }

    private String resolveVehicleType(String vehicleType) {
        if (vehicleType == null) {
            return null;
        }
        String normalized = vehicleType.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("CAR") || normalized.contains("Ô TÔ")) {
            return "CAR";
        }
        if (normalized.contains("MOTOR") || normalized.contains("XE MÁY")) {
            return "MOTORBIKE";
        }
        return vehicleType;
    }

    private String normalize(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
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

    private RegisterServiceRequestDto toDto(RegisterServiceRequest entity) {
        List<RegisterServiceImageDto> images = entity.getImages().stream()
                .map(img -> new RegisterServiceImageDto(img.getId(), entity.getId(), img.getImageUrl(), img.getCreatedAt()))
                .toList();

       
        String normalizedStatus = "COMPLETED".equalsIgnoreCase(entity.getStatus()) 
                ? STATUS_APPROVED 
                : entity.getStatus();

        
        String approvedByName = resolveUsernameById(entity.getApprovedBy());

        // Calculate canReissue: only if card is CANCELLED, PAID, and hasn't been reissued yet
        boolean canReissue = false;
        if (STATUS_CANCELLED.equalsIgnoreCase(normalizedStatus) 
                && "PAID".equalsIgnoreCase(entity.getPaymentStatus())
                && entity.getReissuedFromCardId() == null) { // Not already a reissued card
            // Check if this card has already been reissued
            canReissue = !requestRepository.existsReissuedCard(entity.getId());
        }

        return new RegisterServiceRequestDto(
                entity.getId(),
                entity.getUserId(),
                entity.getServiceType(),
                entity.getRequestType(),
                entity.getNote(),
                normalizedStatus,
                entity.getVehicleType(),
                entity.getLicensePlate(),
                entity.getVehicleBrand(),
                entity.getVehicleColor(),
                entity.getApartmentNumber(),
                entity.getBuildingName(),
                entity.getUnitId(),
                entity.getPaymentStatus(),
                entity.getPaymentAmount(),
                entity.getPaymentDate(),
                entity.getPaymentGateway(),
                entity.getVnpayTransactionRef(),
                entity.getAdminNote(),
                entity.getApprovedBy(),
                approvedByName,
                entity.getApprovedAt(),
                entity.getRejectionReason(),
                images,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getReissuedFromCardId(),
                canReissue
        );
    }

    /**
     * Resolve username from iam.users table by userId
     */
    private String resolveUsernameById(UUID userId) {
        if (userId == null) {
            return null;
        }
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId);
            
            List<String> results = jdbcTemplate.queryForList("""
                    SELECT username
                    FROM iam.users
                    WHERE id = :userId
                    LIMIT 1
                    """, params, String.class);
            
            if (results != null && !results.isEmpty()) {
                return results.get(0);
            }
            return null;
        } catch (Exception e) {
            log.warn("⚠️ [VehicleRegistration] Không thể lấy username cho userId {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public record VehicleRegistrationPaymentResponse(UUID registrationId, String paymentUrl) {}

    public record VehicleRegistrationPaymentResult(UUID registrationId, boolean success, String responseCode, boolean signatureValid) {}

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


