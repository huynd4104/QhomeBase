package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceTicketRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceTicketDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceTicketRequest;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceTicket;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceRepository;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceTicketService {

    private final ServiceRepository serviceRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final ServiceConfigService serviceConfigService;

    @Transactional(readOnly = true)
    public List<ServiceTicketDto> getTickets(UUID serviceId, Boolean isActive) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = findServiceOrThrow(serviceId);
        return serviceTicketRepository.findAllByServiceId(service.getId()).stream()
                .filter(ticket -> filterByActive(ticket, isActive))
                .sorted(Comparator.comparing(ServiceTicket::getSortOrder, Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(ServiceTicket::getName, String.CASE_INSENSITIVE_ORDER))
                .map(serviceConfigService::toTicketDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceTicketDto> getAllTickets(Boolean isActive) {
        return serviceTicketRepository.findAll().stream()
                .filter(ticket -> filterByActive(ticket, isActive))
                .map(serviceConfigService::toTicketDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceTicketDto getTicket(UUID ticketId) {
        ServiceTicket ticket = serviceTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Service ticket not found: " + ticketId));
        return serviceConfigService.toTicketDto(ticket);
    }

    @Transactional
    public ServiceTicketDto createTicket(UUID serviceId, CreateServiceTicketRequest request) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = findServiceOrThrow(serviceId);
        validateTicketCode(serviceId, null, request.getCode());

        ServiceTicket ticket = new ServiceTicket();
        ticket.setService(service);
        ticket.setCode(request.getCode().trim());
        ticket.setName(request.getName().trim());
        ticket.setTicketType(request.getTicketType());
        ticket.setDurationHours(request.getDurationHours());
        ticket.setPrice(request.getPrice());
        ticket.setMaxPeople(request.getMaxPeople());
        ticket.setDescription(trimToNull(request.getDescription()));
        ticket.setIsActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE);
        ticket.setSortOrder(resolveTicketSortOrder(serviceId, request.getSortOrder()));

        ServiceTicket saved = serviceTicketRepository.save(ticket);
        return serviceConfigService.toTicketDto(saved);
    }

    @Transactional
    public ServiceTicketDto updateTicket(UUID ticketId, UpdateServiceTicketRequest request) {
        ServiceTicket ticket = serviceTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Service ticket not found: " + ticketId));

        ticket.setName(request.getName().trim());
        ticket.setTicketType(request.getTicketType());
        ticket.setDurationHours(request.getDurationHours());
        ticket.setPrice(request.getPrice());
        ticket.setMaxPeople(request.getMaxPeople());
        ticket.setDescription(trimToNull(request.getDescription()));
        if (request.getIsActive() != null) {
            ticket.setIsActive(request.getIsActive());
        }
        if (request.getSortOrder() != null) {
            ticket.setSortOrder(request.getSortOrder());
        }

        ServiceTicket saved = serviceTicketRepository.save(ticket);
        return serviceConfigService.toTicketDto(saved);
    }

    @Transactional
    public ServiceTicketDto setTicketStatus(UUID ticketId, boolean active) {
        ServiceTicket ticket = serviceTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Service ticket not found: " + ticketId));
        ticket.setIsActive(active);
        ServiceTicket saved = serviceTicketRepository.save(ticket);
        return serviceConfigService.toTicketDto(saved);
    }

    @Transactional
    public void deleteTicket(UUID ticketId) {
        ServiceTicket ticket = serviceTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Service ticket not found: " + ticketId));
        serviceTicketRepository.delete(ticket);
    }

    private com.QhomeBase.assetmaintenanceservice.model.service.Service findServiceOrThrow(UUID serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
    }

    private boolean filterByActive(ServiceTicket ticket, Boolean isActive) {
        if (isActive == null) {
            return true;
        }
        boolean ticketActive = Boolean.TRUE.equals(ticket.getIsActive());
        return Boolean.TRUE.equals(isActive) ? ticketActive : !ticketActive;
    }

    private void validateTicketCode(UUID serviceId, UUID ticketId, String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Ticket code is required");
        }
        String trimmed = code.trim();
        boolean exists = ticketId == null
                ? serviceTicketRepository.existsByServiceIdAndCodeIgnoreCase(serviceId, trimmed)
                : serviceTicketRepository.existsByServiceIdAndCodeIgnoreCaseAndIdNot(serviceId, trimmed, ticketId);
        if (exists) {
            throw new IllegalArgumentException("Ticket code already exists for this service: " + trimmed);
        }
    }

    private int resolveTicketSortOrder(UUID serviceId, Integer sortOrder) {
        if (sortOrder != null) {
            return sortOrder;
        }
        return serviceTicketRepository.findAllByServiceId(serviceId).stream()
                .map(ServiceTicket::getSortOrder)
                .filter(order -> order != null)
                .max(Integer::compareTo)
                .map(max -> max + 1)
                .orElse(1);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}

