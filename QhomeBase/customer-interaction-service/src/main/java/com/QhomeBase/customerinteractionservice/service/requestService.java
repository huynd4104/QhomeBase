package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.client.BaseServiceClient;
import com.QhomeBase.customerinteractionservice.client.dto.ResidentResponse;
import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.model.ProcessingLog;
import com.QhomeBase.customerinteractionservice.model.Request;
import com.QhomeBase.customerinteractionservice.repository.processingLogRepository;
import com.QhomeBase.customerinteractionservice.repository.requestRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class requestService {
    private final requestRepository requestRepository;
    private final processingLogRepository processingLogRepository;
    private final BaseServiceClient baseServiceClient;
    private static final int PAGE_SIZE = 5;

    @Value("${request.rate-limit.max-per-hour:5}")
    private int maxRequestsPerHour;

    @Value("${request.rate-limit.max-per-day:10}")
    private int maxRequestsPerDay;

    public requestService(requestRepository requestRepository,
                          processingLogRepository processingLogRepository,
                          BaseServiceClient baseServiceClient) {
        this.requestRepository = requestRepository;
        this.processingLogRepository = processingLogRepository;
        this.baseServiceClient = baseServiceClient;
    }

    public Request createRequest(Request newRequest) {
        return requestRepository.save(newRequest);
    }

    public RequestDTO mapToDto(Request entity) {
        return new RequestDTO(
            entity.getId(),
            entity.getRequestCode(),
            entity.getResidentId(),
            entity.getResidentName(),
            entity.getImagePath(),
            entity.getTitle(),
            entity.getContent(),
            entity.getStatus(),
            entity.getType(),
            entity.getFee(),
            entity.getRepairedDate() != null ? entity.getRepairedDate().toString() : null,
            entity.getServiceBookingId(),
            entity.getCreatedAt().toString().replace("T", " "),
            entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString().replace("T", " ") : null
        );
    }

    public RequestDTO getRequestById(UUID id) {
        return requestRepository.findById(id).map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + id));
    }

    // Method to get all requests without filter, no pagination
    public List<RequestDTO> getAllRequests() {
        return requestRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Method to get filtered requests with pagination
    // Only returns requests related to service bookings (amenities feedback)
    public Page<RequestDTO> getFilteredRequests(
            String status,
            int pageNo,
            String dateFrom,
            String dateTo) {

        Specification<Request> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only show feedback for amenities services (has serviceBookingId)
            predicates.add(cb.isNotNull(root.get("serviceBookingId")));

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (StringUtils.hasText(dateFrom)) {
                LocalDate fromDate = LocalDate.parse(dateFrom);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }

            if (StringUtils.hasText(dateTo)) {
                LocalDate toDate = LocalDate.parse(dateTo);
                predicates.add(cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE); 

        return requestRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    public Map<String, Long> getRequestCounts(String dateFrom, String dateTo) {
        // Only count requests related to service bookings (amenities feedback)
        Specification<Request> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotNull(root.get("serviceBookingId")));
            
            if (StringUtils.hasText(dateFrom)) {
                LocalDate fromDate = LocalDate.parse(dateFrom);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }

            if (StringUtils.hasText(dateTo)) {
                LocalDate toDate = LocalDate.parse(dateTo);
                predicates.add(cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Count by status manually since we need to filter by serviceBookingId
        List<Request> allRequests = requestRepository.findAll(spec);
        Map<String, Long> result = allRequests.stream()
                .collect(Collectors.groupingBy(
                    Request::getStatus,
                    Collectors.counting()
                ));

        long total = result.values().stream().mapToLong(Long::longValue).sum();
        result.put("total", total);

        return result;
    }


    private String generateRequestCode() {
        String prefix = "REQ";
        String year = String.valueOf(LocalDateTime.now().getYear());
        long count = requestRepository.count() + 1;
        return String.format("%s-%s-%05d", prefix, year, count);
    }


    public RequestDTO createNewRequest(RequestDTO dto, Authentication authentication) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        UUID userId = extractUserId(auth);
        if (userId == null) {
            throw new IllegalArgumentException("User information is required to create a request");
        }

        ResidentResponse resident = baseServiceClient.getResidentByUserId(userId);
        if (resident == null) {
            throw new IllegalArgumentException("Resident information could not be resolved for user: " + userId);
        }

        // Kiểm tra rate limit: số lượng request trong 1 giờ và 24 giờ
        validateRequestRateLimit(resident.id());

        // Validate resident name (họ tên)
        String residentName = resident.fullName();
        if (residentName != null) {
            // Validate: không có ký tự đặc biệt, không có quá nhiều khoảng cách
            if (!residentName.matches("^[\\p{L}\\s]+$")) {
                throw new IllegalArgumentException("Họ và tên không được chứa ký tự đặc biệt hoặc số");
            }
            // Kiểm tra không có quá nhiều khoảng cách liên tiếp (nhiều hơn 1 khoảng cách)
            if (residentName.matches(".*\\s{2,}.*")) {
                throw new IllegalArgumentException("Họ và tên không được có quá nhiều khoảng cách");
            }
            // Trim và normalize khoảng cách
            residentName = residentName.trim().replaceAll("\\s+", " ");
        }

        // Validate phone number (số điện thoại)
        String phone = resident.phone();
        if (phone != null && !phone.trim().isEmpty()) {
            // Remove all non-digit characters
            String phoneDigits = phone.replaceAll("[^0-9]", "");
            // Validate: phải đúng 10 số
            if (phoneDigits.length() != 10) {
                throw new IllegalArgumentException("Số điện thoại phải có đúng 10 chữ số");
            }
        }

        Request entity = new Request();
        entity.setId(dto.getId());
        entity.setRequestCode(dto.getRequestCode() != null ? dto.getRequestCode() : generateRequestCode());
        entity.setResidentId(resident.id());
        entity.setResidentName(residentName);
        // Normalize imagePath if it contains multiple images/videos (JSON array), preserving order
        entity.setImagePath(normalizeImagePath(dto.getImagePath()));
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        // Đảm bảo status mặc định là "Pending" (PENDING) khi tạo request
        entity.setStatus(StringUtils.hasText(dto.getStatus()) 
                ? (dto.getStatus().equalsIgnoreCase("PENDING") ? "Pending" : dto.getStatus())
                : "Pending");
        entity.setType(dto.getType());
        entity.setFee(dto.getFee());
        entity.setServiceBookingId(dto.getServiceBookingId());
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        Request savedRequest = requestRepository.save(entity);

        ProcessingLog newLog = new ProcessingLog();
        newLog.setRecordId(savedRequest.getId());
        newLog.setStaffInCharge(null);
        newLog.setStaffInChargeName(null);
        newLog.setContent("Request created by: " + savedRequest.getResidentName());
        newLog.setRequestStatus(savedRequest.getStatus());
        newLog.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        processingLogRepository.save(newLog);
        return this.mapToDto(savedRequest);
    }

    private void validateRequestRateLimit(UUID residentId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Kiểm tra số lượng request trong 1 giờ qua
        LocalDateTime oneHourAgo = now.minusHours(1);
        long requestsInLastHour = requestRepository.countRequestsByResidentSince(residentId, oneHourAgo);
        
        if (requestsInLastHour >= maxRequestsPerHour) {
            throw new IllegalStateException(
                String.format("Bạn đã tạo quá nhiều yêu cầu. Vui lòng đợi 60 phút trước khi tạo yêu cầu mới. " +
                            "(Giới hạn: %d yêu cầu/giờ. Bạn đã tạo %d yêu cầu trong giờ qua)", 
                            maxRequestsPerHour, requestsInLastHour)
            );
        }

        // Kiểm tra số lượng request trong 24 giờ qua
        LocalDateTime oneDayAgo = now.minusDays(1);
        long requestsInLastDay = requestRepository.countRequestsByResidentSince(residentId, oneDayAgo);
        
        if (requestsInLastDay >= maxRequestsPerDay) {
            throw new IllegalStateException(
                String.format("Bạn đã đạt giới hạn số lượng yêu cầu trong ngày. Vui lòng thử lại sau. " +
                            "(Giới hạn: %d yêu cầu/ngày. Bạn đã tạo %d yêu cầu trong 24 giờ qua)", 
                            maxRequestsPerDay, requestsInLastDay)
            );
        }

        log.debug("✅ [Request Rate Limit] Resident {}: {} requests/hour, {} requests/day", 
                residentId, requestsInLastHour, requestsInLastDay);
    }

    /**
     * Normalize imagePath if it contains multiple images/videos
     * If imagePath is a JSON array, validate and return as JSON (preserving original order)
     * If imagePath is a single string or null, return as is
     * Note: This method preserves the order sent by the client to maintain user's selection order
     */
    private String normalizeImagePath(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return imagePath;
        }
        
        try {
            // Try to parse as JSON array to validate format
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> imageList = objectMapper.readValue(imagePath, new TypeReference<List<String>>() {});
            
            if (imageList == null || imageList.isEmpty()) {
                return imagePath;
            }
            
            // Filter out null or empty URLs, but preserve original order
            List<String> filteredList = new ArrayList<>();
            for (String url : imageList) {
                if (url != null && !url.trim().isEmpty()) {
                    filteredList.add(url);
                }
            }
            
            // Return as JSON array string, preserving original order
            return objectMapper.writeValueAsString(filteredList);
        } catch (Exception e) {
            // If parsing fails, assume it's a single string or invalid JSON, return as is
            log.debug("imagePath is not a JSON array or parsing failed, returning as is: {}", e.getMessage());
            return imagePath;
        }
    }

    public RequestDTO updateFee(UUID requestId, java.math.BigDecimal fee) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        
        // Only update fee, type is set during creation and should not be updated here
        request.setFee(fee);
        request.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        
        Request updatedRequest = requestRepository.save(request);
        return mapToDto(updatedRequest);
    }

    public RequestDTO updateStatus(UUID requestId, String status) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        
        request.setStatus(status);
        request.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        
        Request updatedRequest = requestRepository.save(request);
        return mapToDto(updatedRequest);
    }

    @org.springframework.transaction.annotation.Transactional
    public RequestDTO acceptOrDenyRequest(UUID requestId, 
                                          String action, 
                                          java.math.BigDecimal fee, 
                                          LocalDate repairedDate, 
                                          String note, 
                                          String staffName,
                                          Authentication authentication) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        
        // Validate working hours: 8:00 - 20:00
        if ("accept".equalsIgnoreCase(action)) {
            int currentHour = now.getHour();
            if (currentHour < 8 || currentHour >= 20) {
                throw new IllegalStateException(
                    "Yêu cầu sửa chữa chỉ có thể được xử lý trong giờ hành chính (8:00 - 20:00). " +
                    "Thời gian hiện tại: " + now.getHour() + ":" + String.format("%02d", now.getMinute())
                );
            }
        }
        
        if ("deny".equalsIgnoreCase(action)) {
            // Deny: update status to Done, create log with note
            request.setStatus("Done");
            request.setUpdatedAt(now);
            Request savedRequest = requestRepository.save(request);
            
            // Create log entry
            ProcessingLog log = new ProcessingLog();
            log.setRecordId(savedRequest.getId());
            log.setStaffInChargeName(staffName);
            log.setContent("Từ chối: " + note);
            log.setRequestStatus("Done");
            log.setCreatedAt(now);
            processingLogRepository.save(log);
            
            return mapToDto(savedRequest);
        } else if ("accept".equalsIgnoreCase(action)) {
            // Accept: update fee, repairedDate, status, create log
            if (fee == null || repairedDate == null) {
                throw new IllegalArgumentException("Fee and repaired date are required for accept action");
            }
            
            request.setFee(fee);
            request.setRepairedDate(repairedDate);
            request.setUpdatedAt(now);
            Request savedRequest = requestRepository.save(request);
            

            String logContent = String.format("%s sẽ tới sửa chữa vào ngày %s với giá %s VND với ghi chú là: %s",
                    staffName,
                    repairedDate.toString(),
                    fee.toPlainString(),
                    note);
            
            ProcessingLog log = new ProcessingLog();
            log.setRecordId(savedRequest.getId());
            log.setStaffInChargeName(staffName);
            log.setContent(logContent);
            log.setRequestStatus(savedRequest.getStatus());
            log.setCreatedAt(now);
            processingLogRepository.save(log);
            
            return mapToDto(savedRequest);
        } else {
            throw new IllegalArgumentException("Action must be 'accept' or 'deny'");
        }
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.QhomeBase.customerinteractionservice.security.UserPrincipal userPrincipal) {
            return userPrincipal.uid();
        }
        return null;
    }
}
