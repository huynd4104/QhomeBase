package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.dto.supplier.CreateSupplierRequest;
import com.QhomeBase.assetmaintenanceservice.dto.supplier.SupplierResponse;
import com.QhomeBase.assetmaintenanceservice.dto.supplier.UpdateSupplierRequest;
import com.QhomeBase.assetmaintenanceservice.model.Supplier;
import com.QhomeBase.assetmaintenanceservice.model.SupplierType;
import com.QhomeBase.assetmaintenanceservice.repository.SupplierRepository;
import com.QhomeBase.assetmaintenanceservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SupplierService {
    
    private final SupplierRepository supplierRepository;

    public SupplierResponse getSupplierById(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + id));
        return toDto(supplier);
    }

    public Page<SupplierResponse> getAllSuppliers(Boolean isActive, String type, Pageable pageable) {
        Page<Supplier> suppliers;
        
        if (isActive != null || type != null) {
            suppliers = supplierRepository.findWithFilters(isActive, type, pageable);
        } else {
            suppliers = supplierRepository.findAll(pageable);
        }
        
        return suppliers.map(this::toDto);
    }

    public List<SupplierResponse> getActiveSuppliers() {
        List<Supplier> suppliers = supplierRepository.findByIsActiveTrue();
        return suppliers.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<SupplierResponse> getActiveSuppliersByType(String type) {
        List<Supplier> suppliers = supplierRepository.findByTypeAndIsActiveTrue(type);
        return suppliers.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<SupplierResponse> searchSuppliers(String query, int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        List<Supplier> suppliers = supplierRepository.searchSuppliers(query, pageable);
        return suppliers.stream().map(this::toDto).collect(Collectors.toList());
    }

    public SupplierResponse create(CreateSupplierRequest request, Authentication authentication) {
        validateSupplierRequest(request);
        
        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();
        
        Supplier supplier = Supplier.builder()
                .name(request.getName())
                .type(request.getType() != null ? request.getType() : SupplierType.SUPPLIER.name())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .taxId(request.getTaxId())
                .website(request.getWebsite())
                .notes(request.getNotes())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdBy(userId.toString())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        Supplier savedSupplier = supplierRepository.save(supplier);
        return toDto(savedSupplier);
    }

    public SupplierResponse update(UUID id, UpdateSupplierRequest request, Authentication authentication) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + id));
        
        validateUpdateRequest(supplier, request);
        
        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();
        
        if (request.getName() != null) {
            supplier.setName(request.getName());
        }
        if (request.getType() != null) {
            supplier.setType(request.getType());
        }
        if (request.getContactPerson() != null) {
            supplier.setContactPerson(request.getContactPerson());
        }
        if (request.getPhone() != null) {
            supplier.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            supplier.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            supplier.setAddress(request.getAddress());
        }
        if (request.getTaxId() != null) {
            supplier.setTaxId(request.getTaxId());
        }
        if (request.getWebsite() != null) {
            supplier.setWebsite(request.getWebsite());
        }
        if (request.getNotes() != null) {
            supplier.setNotes(request.getNotes());
        }
        if (request.getIsActive() != null) {
            supplier.setIsActive(request.getIsActive());
        }
        
        supplier.setUpdatedBy(userId.toString());
        supplier.setUpdatedAt(Instant.now());
        
        Supplier updatedSupplier = supplierRepository.save(supplier);
        return toDto(updatedSupplier);
    }

    public void delete(UUID id, Authentication authentication) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + id));
        
        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();
        
        // Soft delete: set isActive to false
        supplier.setIsActive(false);
        supplier.setUpdatedBy(userId.toString());
        supplier.setUpdatedAt(Instant.now());
        
        supplierRepository.save(supplier);
    }

    public SupplierResponse restore(UUID id, Authentication authentication) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + id));
        
        if (supplier.getIsActive()) {
            throw new IllegalArgumentException("Supplier is already active");
        }
        
        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();
        
        supplier.setIsActive(true);
        supplier.setUpdatedBy(userId.toString());
        supplier.setUpdatedAt(Instant.now());
        
        Supplier restoredSupplier = supplierRepository.save(supplier);
        return toDto(restoredSupplier);
    }

    private void validateSupplierRequest(CreateSupplierRequest request) {
        if (supplierRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Supplier with name '" + request.getName() + "' already exists");
        }
        
        if (request.getType() != null) {
            try {
                SupplierType.valueOf(request.getType());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid supplier type: " + request.getType());
            }
        }
        
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!isValidEmail(request.getEmail())) {
                throw new IllegalArgumentException("Invalid email format: " + request.getEmail());
            }
        }
    }

    private void validateUpdateRequest(Supplier existingSupplier, UpdateSupplierRequest request) {
        if (request.getName() != null && !request.getName().equals(existingSupplier.getName())) {
            if (supplierRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), existingSupplier.getId())) {
                throw new IllegalArgumentException("Supplier with name '" + request.getName() + "' already exists");
            }
        }
        
        if (request.getType() != null) {
            try {
                SupplierType.valueOf(request.getType());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid supplier type: " + request.getType());
            }
        }
        
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!isValidEmail(request.getEmail())) {
                throw new IllegalArgumentException("Invalid email format: " + request.getEmail());
            }
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private SupplierResponse toDto(Supplier supplier) {
        if (supplier == null) {
            return null;
        }

        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .type(supplier.getType())
                .contactPerson(supplier.getContactPerson())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .taxId(supplier.getTaxId())
                .website(supplier.getWebsite())
                .notes(supplier.getNotes())
                .isActive(supplier.getIsActive())
                .createdBy(supplier.getCreatedBy())
                .createdAt(supplier.getCreatedAt())
                .updatedBy(supplier.getUpdatedBy())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}



