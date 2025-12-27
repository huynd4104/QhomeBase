package com.QhomeBase.assetmaintenanceservice.controller;

import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceTicketRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceTicketDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceTicketRequest;
import com.QhomeBase.assetmaintenanceservice.service.ServiceTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asset-maintenance")
@RequiredArgsConstructor
public class ServiceTicketController {

    private final ServiceTicketService serviceTicketService;

    @GetMapping("/service-tickets")
    @PreAuthorize("@authz.canViewServiceConfig()")
    public ResponseEntity<List<ServiceTicketDto>> getAllTickets(@RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(serviceTicketService.getAllTickets(isActive));
    }

    @GetMapping("/services/{serviceId}/tickets")
    @PreAuthorize("@authz.canViewServiceConfig()")
    public ResponseEntity<List<ServiceTicketDto>> getTickets(@PathVariable UUID serviceId,
                                                             @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(serviceTicketService.getTickets(serviceId, isActive));
    }

    @GetMapping("/service-tickets/{ticketId}")
    @PreAuthorize("@authz.canViewServiceConfig()")
    public ResponseEntity<ServiceTicketDto> getTicket(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(serviceTicketService.getTicket(ticketId));
    }

    @PostMapping("/services/{serviceId}/tickets")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceTicketDto> createTicket(@PathVariable UUID serviceId,
                                                         @Valid @RequestBody CreateServiceTicketRequest request) {
        ServiceTicketDto created = serviceTicketService.createTicket(serviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/service-tickets/{ticketId}")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceTicketDto> updateTicket(@PathVariable UUID ticketId,
                                                         @Valid @RequestBody UpdateServiceTicketRequest request) {
        return ResponseEntity.ok(serviceTicketService.updateTicket(ticketId, request));
    }

    @PatchMapping("/service-tickets/{ticketId}/status")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceTicketDto> toggleTicketStatus(@PathVariable UUID ticketId,
                                                               @RequestParam("active") boolean active) {
        return ResponseEntity.ok(serviceTicketService.setTicketStatus(ticketId, active));
    }

    @DeleteMapping("/service-tickets/{ticketId}")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<Void> deleteTicket(@PathVariable UUID ticketId) {
        serviceTicketService.deleteTicket(ticketId);
        return ResponseEntity.noContent().build();
    }
}



