package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.BuildingCreateReq;
import com.QhomeBase.baseservice.dto.BuildingDto;
import com.QhomeBase.baseservice.dto.BuildingUpdateReq;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.model.BuildingStatus;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BuildingService {

    private final BuildingRepository respo;
    private final UnitRepository unitRepository;

    public BuildingService(BuildingRepository respo, UnitRepository unitRepository) {
        this.respo = respo;
        this.unitRepository = unitRepository;
    }

    public List<Building> findAllOrderByCodeAsc() {
        return respo.findAllByOrderByCodeAsc().stream()
                .sorted(Comparator.comparing(Building::getId))
                .collect(Collectors.toList());
    }

    // NOTE: ép sắp xếp code ASC khi client không gửi sort để giữ thứ tự toàn cục trước khi phân trang.
    public Page<Building> findAllOrderByCodeAsc(Pageable pageable) {
        Pageable sortedPageable = pageable;
        if (pageable == null || pageable.getSort().isUnsorted()) {
            int pageNumber = pageable != null ? pageable.getPageNumber() : 0;
            int pageSize = pageable != null ? pageable.getPageSize() : 20;
            sortedPageable = PageRequest.of(pageNumber, pageSize, Sort.by("code").ascending());
        }
        return respo.findAllByOrderByCodeAsc(sortedPageable);
    }

    public BuildingDto getBuildingById(UUID buildingId) {
        Building building = respo.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found with id: " + buildingId));
        return toDto(building);
    }

    private String generateNextCode() {
        List<Building> buildings = respo.findAllByOrderByCodeAsc();

        int maxIndex = -1;
        for (Building building : buildings) {
            String code = building.getCode();
            if (code == null || code.isBlank()) {
                continue;
            }
            int idx = parseAlphabeticCode(code);
            if (idx > maxIndex) {
                maxIndex = idx;
            }
        }

        return buildAlphabeticCode(maxIndex + 1);
    }

    private int parseAlphabeticCode(String code) {
        if (code == null) {
            return -1;
        }

        String normalized = code.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return -1;
        }

        StringBuilder letters = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                letters.append(ch);
            } else {
                // stop at first non-letter so codes like "B01" still count as "B"
                break;
            }
        }

        if (letters.length() == 0) {
            return -1;
        }

        int value = 0;
        for (int i = 0; i < letters.length(); i++) {
            char ch = letters.charAt(i);
            value = value * 26 + (ch - 'A' + 1);
        }
        return value - 1;
    }

    private String buildAlphabeticCode(int index) {
        int value = index;
        StringBuilder sb = new StringBuilder();
        do {
            int remainder = value % 26;
            sb.insert(0, (char) ('A' + remainder));
            value = (value / 26) - 1;
        } while (value >= 0);
        return sb.toString();
    }

    public BuildingDto toDto(Building building) {
        return new BuildingDto(
                building.getId(),
                building.getCode(),
                building.getName(),
                building.getAddress(),
                building.getNumberOfFloors(),
                0,
                0
        );
    }

    public BuildingDto createBuilding(BuildingCreateReq req, String createdBy) {
        String newCode = generateNextCode();

        var b = Building.builder()
                .code(newCode)
                .name(req.name())
                .address(req.address())
                .numberOfFloors(req.numberOfFloors())
                .createdBy(createdBy)
                .build();
        Building saved = respo.save(b);

        return toDto(saved);
    }

    public BuildingDto updateBuilding(UUID buildingId, BuildingUpdateReq req, Authentication auth) {
        var u = (UserPrincipal) auth.getPrincipal();

        var existing = respo.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));

        existing.setName(req.name());
        existing.setAddress(req.address());
        if (req.numberOfFloors() != null) {
            existing.setNumberOfFloors(req.numberOfFloors());
        }
        existing.setUpdatedBy(u.username());

        Building saved = respo.save(existing);
        return toDto(saved);
    }

    @Transactional
    public void changeBuildingStatus(UUID buildingId, BuildingStatus newStatus, Authentication auth) {
        var u = (UserPrincipal) auth.getPrincipal();

        Building building = respo.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found with id: " + buildingId));

        building.setStatus(newStatus);
        building.setUpdatedBy(u.username());

        respo.save(building);

        // When building status changes to INACTIVE, set all units to INACTIVE
        // When building status changes to ACTIVE, do nothing (keep units' current status)
        if (newStatus == BuildingStatus.INACTIVE) {
            List<Unit> units = unitRepository.findAllByBuildingId(buildingId);
            boolean changed = false;
            for (Unit unit : units) {
                if (unit.getStatus() != UnitStatus.INACTIVE) {
                    unit.setStatus(UnitStatus.INACTIVE);
                    changed = true;
                }
            }
            if (changed) {
                unitRepository.saveAll(units);
            }
        }
    }
}
