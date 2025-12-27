package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.dto.service.AcceptServiceBookingTermsRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.AdminApproveServiceBookingRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.AdminCompleteServiceBookingRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.AdminRejectServiceBookingRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.AdminUpdateServiceBookingPaymentRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.CancelServiceBookingRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceBookingItemRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceBookingRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceBookingCatalogDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceBookingDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceBookingItemDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceBookingSlotDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceBookingSlotRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceComboDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceOptionDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceTicketDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceBookingItemRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceBookingSlotsRequest;
import com.QhomeBase.assetmaintenanceservice.model.service.*;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingStatus;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePaymentStatus;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePricingType;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingItemType;
import com.QhomeBase.assetmaintenanceservice.repository.*;
import com.QhomeBase.assetmaintenanceservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"NullAway", "DataFlowIssue"})
public class ServiceBookingService {

    private final ServiceRepository serviceRepository;
    private final ServiceBookingRepository serviceBookingRepository;
    private final ServiceBookingItemRepository serviceBookingItemRepository;
    private final ServiceBookingSlotRepository serviceBookingSlotRepository;
    private final ServiceComboRepository serviceComboRepository;
    private final ServiceOptionRepository serviceOptionRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final ServiceConfigService serviceConfigService;

    private static final List<ServicePaymentStatus> UNPAID_STATUSES =
            List.of(ServicePaymentStatus.UNPAID, ServicePaymentStatus.PENDING);
    private static final List<ServicePaymentStatus> PAID_STATUSES =
            List.of(ServicePaymentStatus.PAID);
    private UserPrincipal principal(Object authenticationPrincipal) {
        if (authenticationPrincipal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        throw new IllegalStateException("Unsupported authentication principal");
    }

    private UUID requireUserId(UserPrincipal principal) {
        return Objects.requireNonNull(principal.uid(), "Authenticated user is missing an id");
    }

    @Transactional
    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    public ServiceBookingDto createBooking(CreateServiceBookingRequest request, Object authenticationPrincipal) {
        UserPrincipal principal = principal(authenticationPrincipal);
        UUID userId = requireUserId(principal);

        // Check for unpaid bookings, but exclude CANCELLED payment status
        // CANCELLED payment status means the booking and payment are both cancelled
        List<ServiceBooking> unpaidBookings = serviceBookingRepository
                .findAllByUserIdAndPaymentStatusInOrderByCreatedAtDesc(userId, UNPAID_STATUSES);
        boolean hasActiveUnpaid = unpaidBookings.stream()
                .anyMatch(booking -> booking.getPaymentStatus() != ServicePaymentStatus.CANCELLED);
        if (hasActiveUnpaid) {
            throw new IllegalStateException("Bạn đang có dịch vụ chưa được thanh toán. Vui lòng thanh toán hoặc hủy trước khi đặt lịch mới.");
        }

        var service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + request.getServiceId()));

        ServiceBookingSlotRequest slotRequest = request.getSlot();

        LocalDate bookingDate = request.getBookingDate();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        ServicePricingType pricingType = service.getPricingType() != null ? service.getPricingType() : ServicePricingType.FREE;

        boolean allowCustomSlot = pricingType == ServicePricingType.HOURLY || pricingType == ServicePricingType.FREE;
        if (!allowCustomSlot && slotRequest != null) {
            throw new IllegalArgumentException("Custom slot is only supported for hourly services");
        }

        if (allowCustomSlot && slotRequest != null) {
            if (bookingDate == null && slotRequest.getSlotDate() != null) {
                bookingDate = slotRequest.getSlotDate();
            }
            if (startTime == null && slotRequest.getStartTime() != null) {
                startTime = slotRequest.getStartTime();
            }
            if (endTime == null && slotRequest.getEndTime() != null) {
                endTime = slotRequest.getEndTime();
            }
        }

        if (bookingDate == null) {
            throw new IllegalArgumentException("Booking date is required");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }

        BigDecimal durationHours = resolveDurationHours(startTime, endTime, request.getDurationHours());

        if (pricingType == ServicePricingType.SESSION) {
            validateSlotWithinAvailability(service, bookingDate, startTime, endTime);
        }

        // Validate slot booking including capacity check
        validateSlotBooking(service.getId(), bookingDate, startTime, endTime, null, 
                request.getNumberOfPeople(), service.getMaxCapacity());

        ServiceBooking booking = new ServiceBooking();
        booking.setService(service);
        booking.setUserId(userId);
        booking.setBookingDate(bookingDate);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setDurationHours(durationHours);
        booking.setNumberOfPeople(request.getNumberOfPeople());
        booking.setPurpose(trimToNull(request.getPurpose()));
        booking.setTermsAccepted(Boolean.TRUE.equals(request.getTermsAccepted()));
        booking.setPaymentStatus(ServicePaymentStatus.UNPAID);
        booking.setStatus(ServiceBookingStatus.PENDING);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            booking.getBookingItems().addAll(buildItems(booking, request.getItems()));
        }

        if (allowCustomSlot) {
            booking.getBookingSlots().add(buildSlotFromRequest(booking, slotRequest,
                    bookingDate, startTime, endTime));
        } else {
            booking.getBookingSlots().add(buildSlot(booking, bookingDate, startTime, endTime, booking.getId()));
        }

        booking.setTotalAmount(calculateTotalAmount(booking));

        ServiceBooking saved = serviceBookingRepository.save(booking);
        return toDto(saved);
    }
    public boolean validateBookingTime(CreateServiceBookingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create booking request must not be null");
        }
        UUID serviceId = request.getServiceId();
        if (serviceId == null) {
            throw new IllegalArgumentException("Service ID is required");
        }

        com.QhomeBase.assetmaintenanceservice.model.service.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        ServiceBookingSlotRequest slotRequest = request.getSlot();

        LocalDate bookingDate = request.getBookingDate();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        if (slotRequest != null) {
            if (bookingDate == null) {
                bookingDate = slotRequest.getSlotDate();
            }
            if (startTime == null) {
                startTime = slotRequest.getStartTime();
            }
            if (endTime == null) {
                endTime = slotRequest.getEndTime();
            }
        }

        if (bookingDate == null) {
            throw new IllegalArgumentException("Booking date is required");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if (!endTime.isAfter(startTime)) {
            return false;
        }

        BigDecimal calculatedDuration = resolveDurationHours(startTime, endTime, request.getDurationHours());
        Integer minDurationHours = service.getMinDurationHours();
        if (minDurationHours != null && calculatedDuration != null
                && calculatedDuration.compareTo(BigDecimal.valueOf(minDurationHours)) < 0) {
            return false;
        }

        try {
            validateSlotBooking(serviceId, bookingDate, startTime, endTime, null, 
                    request.getNumberOfPeople(), service.getMaxCapacity());
        } catch (IllegalArgumentException ex) {
            return false;
        }

        return true;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    public List<ServiceBookingDto> getMyBookings(Object authenticationPrincipal,
                                                ServiceBookingStatus status,
                                                LocalDate fromDate,
                                                LocalDate toDate) {
        UserPrincipal principal = principal(authenticationPrincipal);
        UUID userId = requireUserId(principal);
        List<ServiceBooking> bookings = serviceBookingRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return bookings.stream()
                .filter(booking -> matchesStatus(booking, status))
                .filter(booking -> matchesDateRange(booking, fromDate, toDate))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    public List<ServiceBookingDto> getMyUnpaidBookings(Object authenticationPrincipal) {
        UserPrincipal principal = principal(authenticationPrincipal);
        UUID userId = requireUserId(principal);
        // Get all bookings with UNPAID or PENDING payment status
        // Exclude bookings with CANCELLED payment status (these are fully cancelled)
        // UNPAID_STATUSES only contains UNPAID and PENDING, so we don't need to filter CANCELLED
        // But we keep the filter for safety in case of data inconsistency
        List<ServiceBooking> bookings = serviceBookingRepository
                .findAllByUserIdAndPaymentStatusInOrderByCreatedAtDesc(userId, UNPAID_STATUSES);
        return bookings.stream()
                .filter(booking -> booking.getPaymentStatus() != ServicePaymentStatus.CANCELLED)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceBookingDto> getMyPaidBookings(Object authenticationPrincipal) {
        UserPrincipal principal = principal(authenticationPrincipal);
        UUID userId = requireUserId(principal);
        List<ServiceBooking> bookings = serviceBookingRepository
                .findAllByUserIdAndPaymentStatusInOrderByCreatedAtDesc(userId, PAID_STATUSES);
        return bookings.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    public ServiceBookingDto getMyBooking(UUID bookingId, Object authenticationPrincipal) {
        UserPrincipal principal = principal(authenticationPrincipal);
        UUID userId = requireUserId(principal);
        ServiceBooking booking = serviceBookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        return toDto(booking);
    }

    private BigDecimal resolveDurationHours(LocalTime start, LocalTime end, BigDecimal requestedDuration) {
        if (start != null && end != null) {
            Duration duration = Duration.between(start, end);
            if (!duration.isNegative() && !duration.isZero()) {
                BigDecimal minutes = BigDecimal.valueOf(duration.toMinutes());
                return minutes.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            }
        }
        return requestedDuration;
    }

    @Transactional
    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    public ServiceBookingDto cancelMyBooking(UUID bookingId,
                                             CancelServiceBookingRequest request,
                                             Object authenticationPrincipal) {
        UserPrincipal principal = principal(authenticationPrincipal);
        UUID userId = requireUserId(principal);
        ServiceBooking booking = serviceBookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        // If already cancelled, return the booking without error
        if (booking.getStatus() == ServiceBookingStatus.CANCELLED) {
            return toDto(booking);
        }

        if (!canCancel(booking.getStatus())) {
            throw new IllegalStateException("Booking cannot be cancelled in current status: " + booking.getStatus());
        }

        booking.setStatus(ServiceBookingStatus.CANCELLED);
        booking.setRejectionReason(trimToNull(request.getReason()));
        // Cancel payment status if not yet paid (UNPAID or PENDING)
        // This ensures cancelled bookings don't block new bookings
        if (booking.getPaymentStatus() == ServicePaymentStatus.UNPAID 
                || booking.getPaymentStatus() == ServicePaymentStatus.PENDING) {
            booking.setPaymentStatus(ServicePaymentStatus.CANCELLED);
        } else if (booking.getPaymentStatus() == ServicePaymentStatus.PAID) {
            // If already paid, also set payment status to CANCELLED
            booking.setPaymentStatus(ServicePaymentStatus.CANCELLED);
        }

        // Remove booking slots when cancelling to free up the time slot for new bookings
        // This prevents unique constraint violations when creating new bookings with the same slot
        booking.getBookingSlots().clear();

        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto acceptTerms(UUID bookingId,
                                         AcceptServiceBookingTermsRequest request,
                                         Object authenticationPrincipal) {
        UserPrincipal principal = principal(authenticationPrincipal);
        ServiceBooking booking = serviceBookingRepository.findByIdAndUserId(bookingId, principal.uid())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        booking.setTermsAccepted(Boolean.TRUE.equals(request.getAccepted()));
        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto addBookingItem(UUID bookingId,
                                            CreateServiceBookingItemRequest request,
                                            Object authenticationPrincipal,
                                            boolean allowManageAny) {
        ServiceBooking booking = loadBookingForMutation(bookingId, authenticationPrincipal, allowManageAny);
        ensurePending(booking);
        ServiceBookingItem item = buildItem(booking, request);
        booking.getBookingItems().add(item);
        booking.setTotalAmount(calculateTotalAmount(booking));

        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto updateBookingItem(UUID bookingId,
                                               UUID itemId,
                                               UpdateServiceBookingItemRequest request,
                                               Object authenticationPrincipal,
                                               boolean allowManageAny) {
        ServiceBooking booking = loadBookingForMutation(bookingId, authenticationPrincipal, allowManageAny);
        ensurePending(booking);

        ServiceBookingItem item = serviceBookingItemRepository.findByIdAndBookingId(itemId, booking.getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking item not found: " + itemId));

        if (StringUtils.hasText(request.getItemName())) {
            item.setItemName(request.getItemName().trim());
        }
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        if (request.getUnitPrice() != null) {
            item.setUnitPrice(request.getUnitPrice());
        }
        if (request.getTotalPrice() != null) {
            item.setTotalPrice(request.getTotalPrice());
        } else {
            item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        if (request.getMetadata() != null) {
            item.setMetadata(request.getMetadata());
        }

        booking.setTotalAmount(calculateTotalAmount(booking));
        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto deleteBookingItem(UUID bookingId,
                                               UUID itemId,
                                               Object authenticationPrincipal,
                                               boolean allowManageAny) {
        ServiceBooking booking = loadBookingForMutation(bookingId, authenticationPrincipal, allowManageAny);
        ensurePending(booking);

        ServiceBookingItem item = serviceBookingItemRepository.findByIdAndBookingId(itemId, booking.getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking item not found: " + itemId));

        booking.getBookingItems().remove(item);
        booking.setTotalAmount(calculateTotalAmount(booking));

        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto updateBookingSlots(UUID bookingId,
                                                UpdateServiceBookingSlotsRequest request,
                                                Object authenticationPrincipal,
                                                boolean allowManageAny) {
        ServiceBooking booking = loadBookingForMutation(bookingId, authenticationPrincipal, allowManageAny);
        ensurePending(booking);

        if (booking.getService() != null
                && booking.getService().getPricingType() == ServicePricingType.SESSION) {
            throw new IllegalStateException("Cannot update slots manually for session-based services");
        }

        booking.getBookingSlots().clear();
        booking.getBookingSlots().add(buildSlotFromRequest(booking, request.getSlot(),
                booking.getBookingDate(), booking.getStartTime(), booking.getEndTime()));

        return toDto(booking);
    }

    @Transactional(readOnly = true)
    public List<ServiceBookingDto> searchBookings(ServiceBookingStatus status,
                                                  UUID serviceId,
                                                  UUID userId,
                                                  LocalDate fromDate,
                                                  LocalDate toDate) {
        List<ServiceBooking> bookings = serviceBookingRepository.findAll();
        bookings.sort(Comparator.comparing(ServiceBooking::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return bookings.stream()
                .filter(booking -> status == null || booking.getStatus() == status)
                .filter(booking -> serviceId == null || booking.getService().getId().equals(serviceId))
                .filter(booking -> userId == null || booking.getUserId().equals(userId))
                .filter(booking -> matchesDateRange(booking, fromDate, toDate))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceBookingDto getBooking(UUID bookingId) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        return toDto(booking);
    }

    @Transactional(readOnly = true)
    public ServiceBookingCatalogDto getBookingCatalog(UUID serviceId) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        List<ServiceComboDto> combos = service.getCombos().stream()
                .filter(Objects::nonNull)
                .filter(combo -> Boolean.TRUE.equals(combo.getIsActive()))
                .map(serviceConfigService::toComboDto)
                .collect(Collectors.toList());

        List<ServiceOptionDto> options = service.getOptions().stream()
                .filter(Objects::nonNull)
                .filter(option -> Boolean.TRUE.equals(option.getIsActive()))
                .map(serviceConfigService::toOptionDto)
                .collect(Collectors.toList());

        List<ServiceTicketDto> tickets = service.getTickets().stream()
                .filter(Objects::nonNull)
                .filter(ticket -> Boolean.TRUE.equals(ticket.getIsActive()))
                .map(serviceConfigService::toTicketDto)
                .collect(Collectors.toList());

        return ServiceBookingCatalogDto.builder()
                .serviceId(service.getId())
                .serviceCode(service.getCode())
                .serviceName(service.getName())
                .pricingType(service.getPricingType())
                .combos(combos)
                .options(options)
                .tickets(tickets)
                .build();
    }

    @Transactional
    public ServiceBookingDto approveBooking(UUID bookingId,
                                            AdminApproveServiceBookingRequest request,
                                            UUID approverId) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != ServiceBookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be approved");
        }
        booking.setStatus(ServiceBookingStatus.APPROVED);
        booking.setApprovedBy(approverId);
        booking.setApprovedAt(OffsetDateTime.now());
        booking.setRejectionReason(null);
        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto rejectBooking(UUID bookingId,
                                           AdminRejectServiceBookingRequest request,
                                           UUID approverId) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() == ServiceBookingStatus.REJECTED || booking.getStatus() == ServiceBookingStatus.CANCELLED) {
            return toDto(booking);
        }
        booking.setStatus(ServiceBookingStatus.REJECTED);
        booking.setApprovedBy(approverId);
        booking.setApprovedAt(OffsetDateTime.now());
        booking.setRejectionReason(trimToNull(request.getRejectionReason()));
        if (booking.getPaymentStatus() == ServicePaymentStatus.UNPAID) {
            booking.setPaymentStatus(ServicePaymentStatus.CANCELLED);
        }
        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto completeBooking(UUID bookingId,
                                             AdminCompleteServiceBookingRequest request,
                                             UUID operatorId) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != ServiceBookingStatus.APPROVED) {
            throw new IllegalStateException("Only approved bookings can be completed");
        }
        booking.setStatus(ServiceBookingStatus.COMPLETED);
        booking.setApprovedBy(operatorId);
        booking.setApprovedAt(OffsetDateTime.now());
        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto updatePayment(UUID bookingId,
                                           AdminUpdateServiceBookingPaymentRequest request) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        booking.setPaymentStatus(request.getPaymentStatus());
        booking.setPaymentDate(request.getPaymentDate());
        booking.setPaymentGateway(trimToNull(request.getPaymentGateway()));
        booking.setVnpayTransactionRef(trimToNull(request.getTransactionReference()));

        return toDto(booking);
    }

    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    private ServiceBooking loadBookingForMutation(UUID bookingId,
                                                  Object authenticationPrincipal,
                                                  boolean allowManageAny) {
        if (allowManageAny) {
            return serviceBookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        }
        UserPrincipal principal = principal(authenticationPrincipal);
        UUID userId = requireUserId(principal);
        return serviceBookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
    }

    private boolean matchesStatus(ServiceBooking booking, ServiceBookingStatus status) {
        return status == null || booking.getStatus() == status;
    }

    private boolean matchesDateRange(ServiceBooking booking, LocalDate from, LocalDate to) {
        LocalDate bookingDate = booking.getBookingDate();
        if (from != null && bookingDate.isBefore(from)) {
            return false;
        }
        if (to != null && bookingDate.isAfter(to)) {
            return false;
        }
        return true;
    }

    private boolean canCancel(ServiceBookingStatus status) {
        return status == ServiceBookingStatus.PENDING || status == ServiceBookingStatus.APPROVED;
    }

    private void ensurePending(ServiceBooking booking) {
        if (booking.getStatus() != ServiceBookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be modified");
        }
    }

    private List<ServiceBookingItem> buildItems(ServiceBooking booking,
                                                List<CreateServiceBookingItemRequest> requests) {
        ServiceItemCatalog catalog = loadItemCatalog(booking);
        return requests.stream()
                .map(request -> buildItem(booking, request, catalog))
                .collect(Collectors.toList());
    }

    private ServiceBookingItem buildItem(ServiceBooking booking,
                                         CreateServiceBookingItemRequest request) {
        return buildItem(booking, request, null);
    }

    private ServiceBookingItem buildItem(ServiceBooking booking,
                                         CreateServiceBookingItemRequest request,
                                         ServiceItemCatalog catalog) {
        ServiceBookingItem item = new ServiceBookingItem();
        item.setBooking(booking);
        item.setItemType(request.getItemType());
        item.setItemId(request.getItemId());
        ItemSource source = resolveItemSource(booking.getService().getId(), request, catalog);
        String code = StringUtils.hasText(request.getItemCode()) ? request.getItemCode().trim() : source.code();
        String name = StringUtils.hasText(request.getItemName()) ? request.getItemName().trim() : source.name();

        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Booking item code is required");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Booking item name is required");
        }
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        if (quantity <= 0) {
            throw new IllegalArgumentException("Booking item quantity must be positive");
        }
        BigDecimal unitPrice = source.unitPrice;
        if (unitPrice == null) {
            throw new IllegalArgumentException("Booking item unit price is required");
        }
        BigDecimal totalPrice = request.getTotalPrice() != null
                ? request.getTotalPrice()
                : unitPrice.multiply(BigDecimal.valueOf(quantity));
        item.setItemCode(code);
        item.setItemName(name);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setTotalPrice(totalPrice);
        item.setMetadata(request.getMetadata());
        return item;
    }
    private ServiceBookingSlot buildSlotFromRequest(ServiceBooking booking,
                                                    ServiceBookingSlotRequest request,
                                                    LocalDate fallbackDate,
                                                    LocalTime fallbackStart,
                                                    LocalTime fallbackEnd) {
        LocalDate slotDate = fallbackDate;
        LocalTime slotStart = fallbackStart;
        LocalTime slotEnd = fallbackEnd;

        if (request != null) {
            if (request.getSlotDate() != null) {
                slotDate = request.getSlotDate();
            }
            if (request.getStartTime() != null) {
                slotStart = request.getStartTime();
            }
            if (request.getEndTime() != null) {
                slotEnd = request.getEndTime();
            }
        }

        return buildSlot(booking, slotDate, slotStart, slotEnd, booking.getId());
    }

    private ServiceBookingSlot buildSlot(ServiceBooking booking,
                                         LocalDate date,
                                         LocalTime start,
                                         LocalTime end,
                                         UUID currentBookingId) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = booking.getService();
        validateSlotBooking(service.getId(), date, start, end, currentBookingId);
        if (service.getPricingType() == ServicePricingType.SESSION) {
            validateSlotWithinAvailability(service, date, start, end);
        }
        
        // Check for duplicate slot before creating (unique constraint: service_id, slot_date, start_time, end_time)
        // Only check for exact match when creating new booking (currentBookingId == null)
        if (currentBookingId == null) {
            List<ServiceBookingSlot> existingSlots = serviceBookingSlotRepository
                    .findAllByServiceIdAndSlotDateOrderByStartTimeAsc(service.getId(), date);
            boolean exactMatch = existingSlots.stream()
                    .anyMatch(s -> s.getService().getId().equals(service.getId())
                            && s.getSlotDate().equals(date)
                            && s.getStartTime().equals(start)
                            && s.getEndTime().equals(end));
            if (exactMatch) {
                throw new IllegalStateException(
                        String.format("Slot đã tồn tại cho dịch vụ này: %s - %s đến %s", 
                                date, start, end));
            }
        }
        
        ServiceBookingSlot slot = new ServiceBookingSlot();
        slot.setBooking(booking);
        slot.setService(service);
        slot.setSlotDate(date);
        slot.setStartTime(start);
        slot.setEndTime(end);
        return slot;
    }

    private BigDecimal calculateTotalAmount(ServiceBooking booking) {
        BigDecimal baseCharge = calculateBaseCharge(booking);
        BigDecimal itemsTotal = booking.getBookingItems().stream()
                .map(item -> {
                    if (item.getTotalPrice() != null) {
                        return item.getTotalPrice();
                    }
                    BigDecimal unitPrice = item.getUnitPrice();
                    Integer quantity = item.getQuantity();
                    if (unitPrice == null || quantity == null) {
                        return BigDecimal.ZERO;
                    }
                    return unitPrice.multiply(BigDecimal.valueOf(quantity));
                })
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return baseCharge.add(itemsTotal);
    }

    private BigDecimal calculateBaseCharge(ServiceBooking booking) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = booking.getService();
        if (service == null || service.getPricingType() == null) {
            return BigDecimal.ZERO;
        }
        return switch (service.getPricingType()) {
            case HOURLY -> calculateHourlyCharge(service.getPricePerHour(), booking);
            case SESSION -> defaultCharge(service.getPricePerSession());
            case FREE -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calculateHourlyCharge(BigDecimal pricePerHour, ServiceBooking booking) {
        if (pricePerHour == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal durationHours = booking.getDurationHours();
        if (durationHours == null) {
            if (booking.getStartTime() != null && booking.getEndTime() != null && booking.getEndTime().isAfter(booking.getStartTime())) {
                long minutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
                if (minutes > 0) {
                    durationHours = BigDecimal.valueOf(minutes)
                            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                }
            }
        }
        if (durationHours == null || durationHours.compareTo(BigDecimal.ZERO) <= 0) {
            durationHours = BigDecimal.ONE;
        }
        return pricePerHour.multiply(durationHours);
    }

    private BigDecimal defaultCharge(BigDecimal price) {
        return price != null ? price : BigDecimal.ZERO;
    }


    private void validateSlotBooking(UUID serviceId,
                                     LocalDate date,
                                     LocalTime start,
                                     LocalTime end,
                                     UUID bookingId) {
        validateSlotBooking(serviceId, date, start, end, bookingId, null, null);
    }

    private void validateSlotBooking(UUID serviceId,
                                     LocalDate date,
                                     LocalTime start,
                                     LocalTime end,
                                     UUID bookingId,
                                     Integer requestedNumberOfPeople,
                                     Integer maxCapacity) {
        if (date == null) {
            throw new IllegalArgumentException("Slot date is required");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Slot start time and end time are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Slot end time must be after start time");
        }

        // ✅ TICKET-BASED BOOKING: Removed time slot conflict validation
        // Residents book using tickets, not specific time slots
        // Multiple bookings can use the same time slot as long as they have valid tickets
        
        // Note: Time slot validation is now disabled to support ticket-based system
        // Previous logic checked for overlapping bookings and capacity limits
        // Now bookings are validated based on ticket availability instead
    }

    private void validateSlotWithinAvailability(com.QhomeBase.assetmaintenanceservice.model.service.Service service,
                                                LocalDate date,
                                                LocalTime start,
                                                LocalTime end) {
        List<ServiceAvailability> availabilities = service.getAvailabilities();
        if (availabilities == null || availabilities.isEmpty()) {
            throw new IllegalStateException("Service does not have configured availability to support session bookings");
        }
        int dayOfWeek = date.getDayOfWeek().getValue();
        boolean match = availabilities.stream()
                .filter(av -> Boolean.TRUE.equals(av.getIsAvailable()))
                .filter(av -> av.getDayOfWeek() != null && av.getDayOfWeek() == dayOfWeek)
                .anyMatch(av -> !av.getStartTime().isAfter(start) && !av.getEndTime().isBefore(end));
        if (!match) {
            throw new IllegalArgumentException("Selected slot is outside the service availability for the chosen date");
        }
    }

    @Transactional(readOnly = true)
    public List<ServiceBookingSlotDto> getServiceSlots(UUID serviceId,
                                                       LocalDate fromDate,
                                                       LocalDate toDate) {
        ensureServiceExists(serviceId);

        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate must be on or after fromDate");
        }

        List<ServiceBookingSlot> slots;
        if (fromDate != null && toDate != null) {
            slots = serviceBookingSlotRepository
                    .findAllByServiceIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(serviceId, fromDate, toDate);
        } else if (fromDate != null) {
            slots = serviceBookingSlotRepository
                    .findAllByServiceIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(serviceId, fromDate, fromDate);
        } else if (toDate != null) {
            slots = serviceBookingSlotRepository
                    .findAllByServiceIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(serviceId, toDate, toDate);
        } else {
            slots = serviceBookingSlotRepository.findAllByServiceIdOrderBySlotDateAscStartTimeAsc(serviceId);
        }
        return toSlotDtos(slots);
    }

    @Transactional(readOnly = true)
    public List<ServiceBookingSlotDto> getServiceSlotsByDate(UUID serviceId, LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        ensureServiceExists(serviceId);
        List<ServiceBookingSlot> slots = serviceBookingSlotRepository
                .findAllByServiceIdAndSlotDateOrderByStartTimeAsc(serviceId, date);
        return toSlotDtos(slots);
    }

    private ServiceBookingDto toDto(ServiceBooking booking) {
        if (booking == null) {
            return null;
        }
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = booking.getService();
        return ServiceBookingDto.builder()
                .id(booking.getId())
                .serviceId(service != null ? service.getId() : null)
                .serviceCode(service != null ? service.getCode() : null)
                .serviceName(service != null ? service.getName() : null)
                .servicePricingType(service != null ? service.getPricingType() : null)
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .durationHours(booking.getDurationHours())
                .numberOfPeople(booking.getNumberOfPeople())
                .purpose(booking.getPurpose())
                .totalAmount(booking.getTotalAmount())
                .paymentStatus(booking.getPaymentStatus())
                .paymentDate(booking.getPaymentDate())
                .paymentGateway(booking.getPaymentGateway())
                .vnpayTransactionRef(booking.getVnpayTransactionRef())
                .status(booking.getStatus())
                .userId(booking.getUserId())
                .approvedBy(booking.getApprovedBy())
                .approvedAt(booking.getApprovedAt())
                .rejectionReason(booking.getRejectionReason())
                .termsAccepted(booking.getTermsAccepted())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .bookingItems(toItemDtos(booking.getBookingItems()))
                .bookingSlots(toSlotDtos(booking.getBookingSlots()))
                .build();
    }

    private void ensureServiceExists(UUID serviceId) {
        serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
    }

    private List<ServiceBookingItemDto> toItemDtos(List<ServiceBookingItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(this::toItemDto)
                .collect(Collectors.toList());
    }

    private ServiceBookingItemDto toItemDto(ServiceBookingItem item) {
        return ServiceBookingItemDto.builder()
                .id(item.getId())
                .bookingId(item.getBooking() != null ? item.getBooking().getId() : null)
                .itemType(item.getItemType())
                .itemId(item.getItemId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .metadata(normalizeMetadata(item.getMetadata()))
                .createdAt(item.getCreatedAt())
                .build();
    }

    private List<ServiceBookingSlotDto> toSlotDtos(List<ServiceBookingSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        return slots.stream()
                .filter(Objects::nonNull)
                .map(this::toSlotDto)
                .collect(Collectors.toList());
    }

    private ServiceBookingSlotDto toSlotDto(ServiceBookingSlot slot) {
        return ServiceBookingSlotDto.builder()
                .id(slot.getId())
                .bookingId(slot.getBooking() != null ? slot.getBooking().getId() : null)
                .serviceId(slot.getService() != null ? slot.getService().getId() : null)
                .slotDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .createdAt(slot.getCreatedAt())
                .build();
    }

    private java.util.Map<String, Object> normalizeMetadata(Object metadata) {
        if (metadata == null) {
            return null;
        }
        if (metadata instanceof java.util.Map<?, ?> map) {
            return map.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            entry -> String.valueOf(entry.getKey()),
                            java.util.Map.Entry::getValue
                    ));
        }
        return Collections.singletonMap("value", metadata);
    }


    private ServiceItemCatalog loadItemCatalog(ServiceBooking booking) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = booking.getService();
        if (service == null || service.getId() == null) {
            throw new IllegalStateException("Booking must reference a persisted service");
        }
        UUID serviceId = service.getId();

        Map<UUID, ServiceCombo> combos = serviceComboRepository.findAllByServiceId(serviceId).stream()
                .collect(Collectors.toMap(ServiceCombo::getId, combo -> combo));
        Map<UUID, ServiceOption> options = serviceOptionRepository.findAllByServiceId(serviceId).stream()
                .collect(Collectors.toMap(ServiceOption::getId, option -> option));
        Map<UUID, ServiceTicket> tickets = serviceTicketRepository.findAllByServiceId(serviceId).stream()
                .collect(Collectors.toMap(ServiceTicket::getId, ticket -> ticket));

        return new ServiceItemCatalog(combos, options, tickets);
    }

    private ItemSource resolveItemSource(UUID serviceId,
                                         CreateServiceBookingItemRequest request,
                                         ServiceItemCatalog catalog) {
        ServiceBookingItemType itemType = request.getItemType();
        UUID itemId = request.getItemId();
        if (itemType == null || itemId == null) {
            throw new IllegalArgumentException("Booking item type and item ID are required");
        }

        switch (itemType) {
            case COMBO -> {
                ServiceCombo combo = catalog != null
                        ? catalog.combos().get(itemId)
                        : serviceComboRepository.findById(itemId).orElse(null);
                if (combo == null || combo.getService() == null || !combo.getService().getId().equals(serviceId)) {
                    throw new IllegalArgumentException("Combo does not belong to the service: " + itemId);
                }
                BigDecimal priceOfItem =  combo.getPrice();
                return new ItemSource(combo.getCode(), combo.getName(), priceOfItem);
            }
            case OPTION -> {
                ServiceOption option = catalog != null
                        ? catalog.options().get(itemId)
                        : serviceOptionRepository.findById(itemId).orElse(null);
                if (option == null || option.getService() == null || !option.getService().getId().equals(serviceId)) {
                    throw new IllegalArgumentException("Option does not belong to the service: " + itemId);
                }
                BigDecimal priceOfItem =  option.getPrice();
                return new ItemSource(option.getCode(), option.getName(), priceOfItem);
            }
            case TICKET -> {
                ServiceTicket ticket = catalog != null
                        ? catalog.tickets().get(itemId)
                        : serviceTicketRepository.findById(itemId).orElse(null);
                if (ticket == null || ticket.getService() == null || !ticket.getService().getId().equals(serviceId)) {
                    throw new IllegalArgumentException("Ticket does not belong to the service: " + itemId);
                }
                BigDecimal priceOfItem =  ticket.getPrice();
                return new ItemSource(ticket.getCode(),ticket.getName(), priceOfItem);
            }

            default -> throw new IllegalArgumentException("Unsupported booking item type: " + itemType);
        }
    }

    private record ServiceItemCatalog(
            Map<UUID, ServiceCombo> combos,
            Map<UUID, ServiceOption> options,
            Map<UUID, ServiceTicket> tickets
    ) {
    }

    private record ItemSource(String code, String name, BigDecimal unitPrice) {
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}


 