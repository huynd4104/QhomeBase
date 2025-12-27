package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.ResidentDto;
import com.QhomeBase.baseservice.dto.StaffResidentSyncRequest;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.model.ResidentStatus;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ResidentService {

    private final ResidentRepository residentRepository;

    public List<ResidentDto> findAll() {
        return residentRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<ResidentDto> findAllByStatus(ResidentStatus status) {
        if (status == null) {
            return findAll();
        }
        return residentRepository.findAllByStatus(status).stream()
                .map(this::toDto)
                .toList();
    }

    public List<ResidentDto> search(String term) {
        if (!StringUtils.hasText(term)) {
            return Collections.emptyList();
        }
        return residentRepository.searchByTerm(term.trim()).stream()
                .map(this::toDto)
                .toList();
    }

    public List<ResidentDto> searchByPhonePrefix(String phonePrefix) {
        if (!StringUtils.hasText(phonePrefix) || phonePrefix.length() < 3) {
            return Collections.emptyList();
        }
        // Normalize phone: remove all non-digit characters
        String normalizedPhone = phonePrefix.replaceAll("[^0-9]", "");
        
        // Only search if exactly 10 digits (full phone number)
        // This ensures users enter complete phone numbers before seeing suggestions
        if (normalizedPhone.length() != 10) {
            log.debug("Phone prefix length is not 10, skipping search. Length: {}, prefix: '{}'", normalizedPhone.length(), normalizedPhone);
            return Collections.emptyList();
        }
        
        log.debug("Searching residents by exact phone number - original: '{}', normalized: '{}'", phonePrefix, normalizedPhone);
        
        List<Resident> residents = new java.util.ArrayList<>();
        
        // If prefix starts with 0, try with 0 first, then without 0
        if (normalizedPhone.startsWith("0")) {
            // Try with leading zero (as-is)
            log.debug("Trying search with prefix: '{}'", normalizedPhone);
            residents = residentRepository.findByPhonePrefix(normalizedPhone);
            log.debug("Found {} residents with prefix '{}'", residents.size(), normalizedPhone);
            
            // If no results, try without leading zero
            if (residents.isEmpty() && normalizedPhone.length() > 1) {
                String withoutZero = normalizedPhone.substring(1);
                log.debug("Trying search without leading zero: '{}'", withoutZero);
                residents = residentRepository.findByPhonePrefix(withoutZero);
                log.debug("Found {} residents with prefix '{}'", residents.size(), withoutZero);
            }
        } else {
            // If prefix doesn't start with 0, try with 0 first, then without 0
            String withZero = "0" + normalizedPhone;
            log.debug("Trying search with leading zero: '{}'", withZero);
            residents = residentRepository.findByPhonePrefix(withZero);
            log.debug("Found {} residents with prefix '{}'", residents.size(), withZero);
            
            // If no results, try without leading zero
            if (residents.isEmpty()) {
                log.debug("Trying search without leading zero: '{}'", normalizedPhone);
                residents = residentRepository.findByPhonePrefix(normalizedPhone);
                log.debug("Found {} residents with prefix '{}'", residents.size(), normalizedPhone);
            }
        }
        
        // Limit to 10 results for performance
        List<ResidentDto> result = residents.stream()
                .limit(10)
                .map(this::toDto)
                .toList();
        
        log.debug("Returning {} residents for phone prefix '{}'", result.size(), phonePrefix);
        return result;
    }

    public ResidentDto getById(UUID residentId) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found: " + residentId));
        return toDto(resident);
    }

    public Optional<Resident> findEntityById(UUID residentId) {
        return residentRepository.findById(residentId);
    }

    public Optional<ResidentDto> findByUserId(UUID userId) {
        return residentRepository.findByUserId(userId)
                .map(this::toDto);
    }

    public Optional<Resident> findEntityByUserId(UUID userId) {
        return residentRepository.findByUserId(userId);
    }

    public ResidentDto getByUserId(UUID userId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + userId));
        return toDto(resident);
    }

    public ResidentDto getByNationalId(String nationalId) {
        if (!StringUtils.hasText(nationalId)) {
            throw new IllegalArgumentException("National ID is required");
        }
        Resident resident = residentRepository.findByNationalId(nationalId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for national ID: " + nationalId));
        return toDto(resident);
    }

    public ResidentDto getByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new IllegalArgumentException("Phone number is required");
        }
        // Normalize phone: remove all non-digit characters
        String normalizedPhone = phone.replaceAll("[^0-9]", "");
        
        // Try exact match first
        Optional<Resident> residentOpt = residentRepository.findByPhone(normalizedPhone);
        
        // If not found and phone doesn't start with 0, try with leading zero
        if (residentOpt.isEmpty() && !normalizedPhone.startsWith("0") && normalizedPhone.length() > 0) {
            residentOpt = residentRepository.findByPhone("0" + normalizedPhone);
        }
        
        // If not found and phone starts with 0, try without leading zero
        if (residentOpt.isEmpty() && normalizedPhone.startsWith("0") && normalizedPhone.length() > 1) {
            residentOpt = residentRepository.findByPhone(normalizedPhone.substring(1));
        }
        
        Resident resident = residentOpt
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for phone: " + phone));
        return toDto(resident);
    }

    public boolean existsEmail(String email, UUID excludeId) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return excludeId == null
                ? residentRepository.existsByEmail(email)
                : residentRepository.existsByEmailAndIdNot(email, excludeId);
    }

    public boolean existsPhone(String phone, UUID excludeId) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return excludeId == null
                ? residentRepository.existsByPhone(phone)
                : residentRepository.existsByPhoneAndIdNot(phone, excludeId);
    }

    public boolean existsNationalId(String nationalId, UUID excludeId) {
        if (!StringUtils.hasText(nationalId)) {
            return false;
        }
        return excludeId == null
                ? residentRepository.existsByNationalId(nationalId)
                : residentRepository.existsByNationalIdAndIdNot(nationalId, excludeId);
    }

    public ResidentDto toDto(Resident resident) {
        if (resident == null) {
            return null;
        }
        return new ResidentDto(
                resident.getId(),
                resident.getFullName(),
                resident.getPhone(),
                resident.getEmail(),
                resident.getNationalId(),
                resident.getDob(),
                resident.getStatus(),
                resident.getUserId(),
                resident.getCreatedAt(),
                resident.getUpdatedAt()
        );
    }

    @Transactional
    public ResidentDto updateStatus(UUID residentId, ResidentStatus status) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found: " + residentId));
        if (status == null) {
            throw new IllegalArgumentException("Resident status must not be null");
        }
        resident.setStatus(status);
        Resident saved = residentRepository.save(resident);
        return toDto(saved);
    }

    @Transactional
    public ResidentDto syncStaffResident(StaffResidentSyncRequest request) {
        if (request.userId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        Resident resident = residentRepository.findByUserId(request.userId())
                .orElseGet(() -> Resident.builder()
                        .fullName(resolveFullName(request.fullName(), request.email()))
                        .email(request.email())
                        .phone(request.phone())
                        .status(ResidentStatus.ACTIVE)
                        .userId(request.userId())
                        .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                        .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                        .build());

        resident.setFullName(resolveFullName(request.fullName(), request.email()));
        resident.setEmail(request.email());
        resident.setPhone(request.phone());
        resident.setStatus(ResidentStatus.ACTIVE);
        resident.setUserId(request.userId());
        resident.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        Resident saved = residentRepository.save(resident);
        return toDto(saved);
    }

    private String resolveFullName(String fullName, String fallbackEmail) {
        if (StringUtils.hasText(fullName)) {
            return fullName.trim();
        }
        if (StringUtils.hasText(fallbackEmail)) {
            return fallbackEmail;
        }
        return "Staff";
    }
}