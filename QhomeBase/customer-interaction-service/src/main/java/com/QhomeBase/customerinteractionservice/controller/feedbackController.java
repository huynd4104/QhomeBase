package com.QhomeBase.customerinteractionservice.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.dto.ReplyFeedbackRequest;
import com.QhomeBase.customerinteractionservice.dto.UpdateStatusRequest;
import com.QhomeBase.customerinteractionservice.service.feedbackService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/customer-interaction/feedbacks")
public class feedbackController {

    private final feedbackService feedbackService;

    public feedbackController(feedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

   @GetMapping()
   public Page<RequestDTO> getFeedbacksList(
           @RequestParam(required = false) String status,
           @RequestParam(defaultValue = "0") int pageNo,
           @RequestParam(required = false) String dateFrom,
           @RequestParam(required = false) String dateTo)
   {
       Page<RequestDTO> feedbackPage = feedbackService.getFilteredFeedbacks(
               status, pageNo, dateFrom, dateTo
       );

       return feedbackPage;
   }

   @GetMapping("/all")
   public List<RequestDTO> getAllFeedbacks()
   {
       return feedbackService.getAllFeedbacks();
   }

    @GetMapping("/counts")
    public Map<String, Long> getFeedbackCounts(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo)
    {
        return feedbackService.getFeedbackCounts(
                dateFrom, dateTo
        );
    }

    @GetMapping("/{id}")
    public RequestDTO getFeedback(@PathVariable UUID id)
    {
        return feedbackService.getFeedbackById(id);
    }

    @PostMapping("/createFeedback")
    public ResponseEntity<?> addNewFeedback(@RequestBody RequestDTO feedbackDTO, Authentication auth)
    {
        try {
            RequestDTO savedFeedback = feedbackService.createNewFeedback(feedbackDTO, auth);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedFeedback);
        } catch (IllegalStateException e) {
            // Rate limit exception - sẽ được GlobalExceptionHandler xử lý
            throw e;
        } catch (IllegalArgumentException e) {
            // Validation exception - sẽ được GlobalExceptionHandler xử lý
            throw e;
        }
    }

    @PutMapping("/{feedbackId}/status")
    public ResponseEntity<RequestDTO> updateStatus(
            @PathVariable UUID feedbackId,
            @Valid @RequestBody UpdateStatusRequest updateStatusRequest) {
        try {
            RequestDTO updatedFeedback = feedbackService.updateStatus(
                    feedbackId,
                    updateStatusRequest.getStatus()
            );
            return ResponseEntity.ok(updatedFeedback);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/{feedbackId}/reply")
    @PreAuthorize("hasRole('SUPPORTER')")
    public ResponseEntity<RequestDTO> replyFeedback(
            @PathVariable UUID feedbackId,
            @Valid @RequestBody ReplyFeedbackRequest replyRequest) {
        try {
            RequestDTO updatedFeedback = feedbackService.replyFeedback(
                    feedbackId,
                    replyRequest.getNote()
            );
            return ResponseEntity.ok(updatedFeedback);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

}

