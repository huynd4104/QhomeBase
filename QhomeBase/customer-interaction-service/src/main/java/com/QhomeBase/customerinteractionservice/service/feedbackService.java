package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.client.BaseServiceClient;
import com.QhomeBase.customerinteractionservice.client.dto.ResidentResponse;
import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.dto.StatusCountDTO;
import com.QhomeBase.customerinteractionservice.model.Request;
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

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class feedbackService {
    private final requestRepository requestRepository;
    private final BaseServiceClient baseServiceClient;
    private static final int PAGE_SIZE = 5;
    private static final String FEEDBACK_TYPE = "feedback";

    @Value("${feedback.rate-limit.max-per-hour:5}")
    private int maxFeedbacksPerHour;

    @Value("${feedback.rate-limit.max-per-day:10}")
    private int maxFeedbacksPerDay;

    public feedbackService(requestRepository requestRepository,
                          BaseServiceClient baseServiceClient) {
        this.requestRepository = requestRepository;
        this.baseServiceClient = baseServiceClient;
    }

    public Request createFeedback(Request newFeedback) {
        return requestRepository.save(newFeedback);
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

    public RequestDTO getFeedbackById(UUID id) {
        Request feedback = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found with id: " + id));
        // Verify it's a feedback
        if (!FEEDBACK_TYPE.equals(feedback.getType())) {
            throw new RuntimeException("Request with id: " + id + " is not a feedback");
        }
        return mapToDto(feedback);
    }

    // Method to get all feedbacks without filter, no pagination
    public List<RequestDTO> getAllFeedbacks() {
        return requestRepository.findAll().stream()
                .filter(r -> FEEDBACK_TYPE.equals(r.getType()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Method to get filtered feedbacks with pagination
    public Page<RequestDTO> getFilteredFeedbacks(
            String status,
            int pageNo,
            String dateFrom,
            String dateTo) {

        Specification<Request> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by type = "feedback"
            predicates.add(cb.equal(root.get("type"), FEEDBACK_TYPE));

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

    public Map<String, Long> getFeedbackCounts(String dateFrom, String dateTo) {
        // Get all feedbacks and count by status
        List<Request> allFeedbacks = requestRepository.findAll().stream()
                .filter(r -> FEEDBACK_TYPE.equals(r.getType()))
                .collect(Collectors.toList());

        // Apply date filters if provided
        if (StringUtils.hasText(dateFrom) || StringUtils.hasText(dateTo)) {
            LocalDate fromDate = StringUtils.hasText(dateFrom) ? LocalDate.parse(dateFrom) : null;
            LocalDate toDate = StringUtils.hasText(dateTo) ? LocalDate.parse(dateTo) : null;
            
            allFeedbacks = allFeedbacks.stream()
                    .filter(f -> {
                        LocalDate createdAt = f.getCreatedAt().toLocalDate();
                        if (fromDate != null && createdAt.isBefore(fromDate)) {
                            return false;
                        }
                        if (toDate != null && createdAt.isAfter(toDate)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        Map<String, Long> result = allFeedbacks.stream()
                .collect(Collectors.groupingBy(
                    Request::getStatus,
                    Collectors.counting()
                ));

        long total = result.values().stream().mapToLong(Long::longValue).sum();
        result.put("total", total);

        return result;
    }

    private String generateFeedbackCode() {
        String prefix = "FDB";
        String year = String.valueOf(LocalDateTime.now().getYear());
        long count = requestRepository.findAll().stream()
                .filter(r -> FEEDBACK_TYPE.equals(r.getType()))
                .count() + 1;
        return String.format("%s-%s-%05d", prefix, year, count);
    }

    public RequestDTO createNewFeedback(RequestDTO dto, Authentication authentication) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        UUID userId = extractUserId(auth);
        if (userId == null) {
            throw new IllegalArgumentException("User information is required to create a feedback");
        }

        ResidentResponse resident = baseServiceClient.getResidentByUserId(userId);
        if (resident == null) {
            throw new IllegalArgumentException("Resident information could not be resolved for user: " + userId);
        }

        // Kiểm tra rate limit: số lượng feedback trong 1 giờ và 24 giờ
        validateFeedbackRateLimit(resident.id());

        Request entity = new Request();
        entity.setId(dto.getId());
        entity.setRequestCode(dto.getRequestCode() != null ? dto.getRequestCode() : generateFeedbackCode());
        entity.setResidentId(resident.id());
        entity.setResidentName(resident.fullName());
        entity.setImagePath(dto.getImagePath());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        // Đảm bảo status mặc định là "Pending" khi tạo feedback
        entity.setStatus(StringUtils.hasText(dto.getStatus()) 
                ? (dto.getStatus().equalsIgnoreCase("PENDING") ? "Pending" : dto.getStatus())
                : "Pending");
        entity.setType(FEEDBACK_TYPE); // Always set type to "feedback"
        entity.setFee(dto.getFee());
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        Request savedFeedback = requestRepository.save(entity);

        // No log creation for feedback
        return this.mapToDto(savedFeedback);
    }

    private void validateFeedbackRateLimit(UUID residentId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Kiểm tra số lượng feedback trong 1 giờ qua
        LocalDateTime oneHourAgo = now.minusHours(1);
        long feedbacksInLastHour = requestRepository.findAll().stream()
                .filter(r -> FEEDBACK_TYPE.equals(r.getType()) 
                        && r.getResidentId().equals(residentId)
                        && r.getCreatedAt().isAfter(oneHourAgo))
                .count();
        
        if (feedbacksInLastHour >= maxFeedbacksPerHour) {
            throw new IllegalStateException(
                String.format("Bạn đã tạo quá nhiều phản hồi. Vui lòng đợi 60 phút trước khi tạo phản hồi mới. " +
                            "(Giới hạn: %d phản hồi/giờ. Bạn đã tạo %d phản hồi trong giờ qua)", 
                            maxFeedbacksPerHour, feedbacksInLastHour)
            );
        }

        // Kiểm tra số lượng feedback trong 24 giờ qua
        LocalDateTime oneDayAgo = now.minusDays(1);
        long feedbacksInLastDay = requestRepository.findAll().stream()
                .filter(r -> FEEDBACK_TYPE.equals(r.getType()) 
                        && r.getResidentId().equals(residentId)
                        && r.getCreatedAt().isAfter(oneDayAgo))
                .count();
        
        if (feedbacksInLastDay >= maxFeedbacksPerDay) {
            throw new IllegalStateException(
                String.format("Bạn đã đạt giới hạn số lượng phản hồi trong ngày. Vui lòng thử lại sau. " +
                            "(Giới hạn: %d phản hồi/ngày. Bạn đã tạo %d phản hồi trong 24 giờ qua)", 
                            maxFeedbacksPerDay, feedbacksInLastDay)
            );
        }

        log.debug("✅ [Feedback Rate Limit] Resident {}: {} feedbacks/hour, {} feedbacks/day", 
                residentId, feedbacksInLastHour, feedbacksInLastDay);
    }

    public RequestDTO updateStatus(UUID feedbackId, String status) {
        Request feedback = requestRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found with id: " + feedbackId));
        
        // Verify it's a feedback
        if (!FEEDBACK_TYPE.equals(feedback.getType())) {
            throw new RuntimeException("Request with id: " + feedbackId + " is not a feedback");
        }
        
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        
        feedback.setStatus(status);
        feedback.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        
        Request updatedFeedback = requestRepository.save(feedback);
        return mapToDto(updatedFeedback);
    }

    public RequestDTO replyFeedback(UUID feedbackId, String note) {
        Request feedback = requestRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found with id: " + feedbackId));
        
        // Verify it's a feedback
        if (!FEEDBACK_TYPE.equals(feedback.getType())) {
            throw new RuntimeException("Request with id: " + feedbackId + " is not a feedback");
        }
        
        if (note == null || note.trim().isEmpty()) {
            throw new IllegalArgumentException("Note cannot be null or empty");
        }
        
        // Append reply note to content
        String currentContent = feedback.getContent() != null ? feedback.getContent() : "";
        String replyNote = "\n\n[Phản hồi từ nhân viên]: " + note.trim();
        feedback.setContent(currentContent + replyNote);
        feedback.setStatus("Done");
        feedback.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        
        Request updatedFeedback = requestRepository.save(feedback);
        return mapToDto(updatedFeedback);
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

