package com.QhomeBase.customerinteractionservice.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.QhomeBase.customerinteractionservice.dto.AcceptDenyRequest;
import com.QhomeBase.customerinteractionservice.dto.RequestApproveRequest;
import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.dto.UpdateFeeRequest;
import com.QhomeBase.customerinteractionservice.dto.UpdateStatusRequest;
import com.QhomeBase.customerinteractionservice.service.processingLogService;
import com.QhomeBase.customerinteractionservice.service.requestService;
import com.QhomeBase.customerinteractionservice.security.UserPrincipal;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/customer-interaction/requests")
public class requestController {

    private final requestService requestService;
    private final processingLogService processingLogService;

    public requestController(requestService requestService, processingLogService processingLogService) {
        this.requestService = requestService;
        this.processingLogService = processingLogService;
    }

   @GetMapping()
   public Page<RequestDTO> getRequestsList(
           @RequestParam(required = false) String status,
           @RequestParam(defaultValue = "0") int pageNo,
           @RequestParam(required = false) String dateFrom,
           @RequestParam(required = false) String dateTo)
   {

       Page<RequestDTO> requestPage = requestService.getFilteredRequests(
               status, pageNo, dateFrom, dateTo
       );

       return requestPage;
   }

   @GetMapping("/all")
   public List<RequestDTO> getAllRequests()
   {
       return requestService.getAllRequests();
   }

    @GetMapping("/counts")
    public Map<String, Long> getRequestCounts(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo)
    {
        return requestService.getRequestCounts(
                dateFrom, dateTo
        );
    }

    @GetMapping("/{id}")
    public RequestDTO getRequest(@PathVariable UUID id)
    {
        return requestService.getRequestById(id);
    }

    @PostMapping("/createRequest")
    public ResponseEntity<?> addNewRequest(@RequestBody RequestDTO requestDTO, Authentication auth)
    {
        try {
            RequestDTO savedRequest = requestService.createNewRequest(requestDTO, auth);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedRequest);
        } catch (IllegalStateException e) {
            // Rate limit exception - sẽ được GlobalExceptionHandler xử lý
            throw e;
        } catch (IllegalArgumentException e) {
            // Validation exception - sẽ được GlobalExceptionHandler xử lý
            throw e;
        }
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<RequestDTO> approveRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody RequestApproveRequest approveRequest,
            Authentication authentication) {
        try {
            RequestDTO approvedRequest = processingLogService.approveRequest(requestId, approveRequest, authentication);
            return ResponseEntity.ok(approvedRequest);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(null);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PutMapping("/{requestId}/fee")
    public ResponseEntity<RequestDTO> updateFee(
            @PathVariable UUID requestId,
            @Valid @RequestBody UpdateFeeRequest updateFeeRequest) {
        try {
            RequestDTO updatedRequest = requestService.updateFee(
                    requestId, 
                    updateFeeRequest.getFee()
            );
            return ResponseEntity.ok(updatedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PutMapping("/{requestId}/status")
    public ResponseEntity<RequestDTO> updateStatus(
            @PathVariable UUID requestId,
            @Valid @RequestBody UpdateStatusRequest updateStatusRequest) {
        try {
            RequestDTO updatedRequest = requestService.updateStatus(
                    requestId,
                    updateStatusRequest.getStatus()
            );
            return ResponseEntity.ok(updatedRequest);
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

    @PostMapping("/{requestId}/accept-deny")
    public ResponseEntity<RequestDTO> acceptOrDenyRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody AcceptDenyRequest acceptDenyRequest,
            Authentication authentication) {
        try {
            // Get staff name from authentication
            String staffName = "Staff";
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
                // Try to get username, fallback to uid string
                staffName = principal.username() != null ? principal.username() : principal.uid().toString();
            }
            
            RequestDTO updatedRequest = requestService.acceptOrDenyRequest(
                    requestId,
                    acceptDenyRequest.getAction(),
                    acceptDenyRequest.getFee(),
                    acceptDenyRequest.getRepairedDate(),
                    acceptDenyRequest.getNote(),
                    staffName,
                    authentication
            );
            return ResponseEntity.ok(updatedRequest);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}
