package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.ServiceDto;
import com.QhomeBase.baseservice.model.Service;
import com.QhomeBase.baseservice.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceRepository serviceRepository;

    @GetMapping
    public ResponseEntity<List<ServiceDto>> getAllServices() {
        List<ServiceDto> services = serviceRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceDto> getServiceById(@PathVariable UUID id) {
        return serviceRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ServiceDto> getServiceByCode(@PathVariable String code) {
        return serviceRepository.findByCode(code)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ServiceDto>> getActiveServices() {
        List<ServiceDto> services = serviceRepository.findByActive(true).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(services);
    }

    private ServiceDto toDto(Service service) {
        return new ServiceDto(
                service.getId(),
                service.getCode(),
                service.getName(),
                service.getNameEn(),
                service.getType(),
                service.getUnit(),
                service.getUnitLabel(),
                service.getBillable(),
                service.getRequiresMeter(),
                service.getActive(),
                service.getDescription(),
                service.getDisplayOrder(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}

