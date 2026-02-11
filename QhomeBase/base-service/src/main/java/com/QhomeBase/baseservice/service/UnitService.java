package com.QhomeBase.baseservice.service;


import com.QhomeBase.baseservice.dto.UnitCreateDto;
import com.QhomeBase.baseservice.dto.UnitDto;
import com.QhomeBase.baseservice.dto.UnitUpdateDto;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.model.BuildingStatus;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.client.ContractClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnitService {
    /** Maximum number of units allowed per floor in a building. */
    public static final int MAX_UNITS_PER_FLOOR = 5;

    private final UnitRepository unitRepository;
    private final BuildingRepository buildingRepository;
    private final ContractClient contractClient;

    private OffsetDateTime nowUTC() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    @Transactional
    public UnitDto createUnit(UnitCreateDto unitCreateDto) {
        validateUnitCreateDto(unitCreateDto);
        validateMaxUnitsPerFloor(unitCreateDto.buildingId(), unitCreateDto.floor());

        String generatedCode = generateNextCode(unitCreateDto.buildingId(), unitCreateDto.floor());
        var unit = Unit.builder()
                .building(buildingRepository.findById(unitCreateDto.buildingId()).orElseThrow())
                .code(generatedCode)
                .floor(unitCreateDto.floor())
                .areaM2(unitCreateDto.areaM2())
                .bedrooms(unitCreateDto.bedrooms())
                .status(UnitStatus.ACTIVE)
                .createdAt(nowUTC())
                .updatedAt(nowUTC())
                .build();
        var savedUnit = unitRepository.save(unit);
        return toDto(savedUnit);
    }
    
    @Transactional
    public UnitDto updateUnit(UnitUpdateDto unit, UUID id) {
        Unit existingUnit = unitRepository.findByIdWithBuilding(id);
        if (existingUnit == null) {
            throw new IllegalArgumentException("Unit not found: " + id);
        }
        validateUnitUpdateDto(unit, id);

        if (unit.floor() != null && !unit.floor().equals(existingUnit.getFloor())) {
            validateMaxUnitsPerFloor(existingUnit.getBuilding().getId(), unit.floor());
            existingUnit.setFloor(unit.floor());
        } else if (unit.floor() != null) {
            existingUnit.setFloor(unit.floor());
        }
        if (unit.areaM2() != null) {
            existingUnit.setAreaM2(unit.areaM2());
        }
        if (unit.bedrooms() != null) {
            existingUnit.setBedrooms(unit.bedrooms());
        }
        
        existingUnit.setUpdatedAt(nowUTC());
        
        var savedUnit = unitRepository.save(existingUnit);
        return toDto(savedUnit);
    }
    
    @Transactional
    public void deleteUnit(UUID id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow();
        unit.setStatus(UnitStatus.INACTIVE);
        unit.setUpdatedAt(nowUTC());
        
        unitRepository.save(unit);
    }
    
    public UnitDto getUnitById(UUID id) {
        Unit unit = unitRepository.findByIdWithBuilding(id);
        if (unit == null) {
            throw new IllegalArgumentException("Unit not found: " + id);
        }
        return toDto(unit);
    }
    
    public java.util.List<UnitDto> getUnitsByBuildingId(UUID buildingId) {
        var units = unitRepository.findAllByBuildingId(buildingId);
        return units.stream()
                .map(this::toDto)
                .toList();
    }
    
    
    public java.util.List<UnitDto> getUnitsByFloor(UUID buildingId, Integer floor) {
        var units = unitRepository.findByBuildingIdAndFloorNumber(buildingId, floor);
        return units.stream()
                .map(this::toDto)
                .toList();
    }
    
    @Transactional
    public void changeUnitStatus(UUID id, UnitStatus newStatus) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow();

        if (newStatus == UnitStatus.INACTIVE) {
            List<?> activeContracts = contractClient.getActiveContractsByUnit(id);
            if (activeContracts != null && !activeContracts.isEmpty()) {
                throw new IllegalStateException(
                        "Căn hộ đang có hợp đồng thuê/mua còn hiệu lực. Vui lòng kết thúc hợp đồng trước khi ngừng hoạt động căn hộ.");
            }
        }

        unit.setStatus(newStatus);
        unit.setUpdatedAt(nowUTC());

        unitRepository.save(unit);
    }

    public String getPrefix(UUID buildingId) {
        var building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found: " + buildingId));

        String code = building.getCode();
        String extracted = extractAlphabeticPrefix(code);
        if (!extracted.isEmpty()) {
            return extracted;
        }

        var buildings = buildingRepository.findAllByOrderByCodeAsc();
        for (int i = 0; i < buildings.size(); i++) {
            if (buildings.get(i).getId().equals(buildingId)) {
                return alphabeticFromIndex(i);
            }
        }
        return "A";
    }

    public String nextSequence(UUID buildingId, int floorNumber) {
        String expectedPrefix = getPrefix(buildingId);
        String expectedStart  = expectedPrefix + floorNumber;

        var units = unitRepository.findByBuildingIdAndFloorNumber(buildingId, floorNumber);

        int maxNow = 0;

        for (var unit : units) {
            String code = unit.getCode();
            if (code == null) continue;

            if (!code.startsWith(expectedStart)) continue;
            String sequencePart = code.substring(expectedStart.length());
            if (sequencePart.isEmpty()) continue;
            String cleanSequence = cleanSequenceString(sequencePart);
            if (cleanSequence.isEmpty()) continue;
            
            try {
                int now = Integer.parseInt(cleanSequence);
                if (now > maxNow) maxNow = now;
            } catch (Exception e) {
            }
        }
        return String.format("%02d", maxNow + 1);
    }

    private String cleanSequenceString(String sequencePart) {
        if (sequencePart == null || sequencePart.isEmpty()) {
            return "";
        }

        StringBuilder cleanSequence = new StringBuilder();
        for (int i = sequencePart.length() - 1; i >= 0; i--) {
            char c = sequencePart.charAt(i);
            if (Character.isDigit(c)) {
                cleanSequence.insert(0, c);
            } else {
                break;
            }
        }
        
        return cleanSequence.toString();
    }

    private String extractAlphabeticPrefix(String code) {
        if (code == null) {
            return "";
        }
        String normalized = code.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return "";
        }
        StringBuilder letters = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                letters.append(ch);
            } else {
                break;
            }
        }
        return letters.toString();
    }

    private String alphabeticFromIndex(int index) {
        int value = index;
        StringBuilder sb = new StringBuilder();
        do {
            int remainder = value % 26;
            sb.insert(0, (char) ('A' + remainder));
            value = (value / 26) - 1;
        } while (value >= 0);
        return sb.toString();
    }

    public String generateNextCode(UUID buildingId, int floorNumber) {
        String prefix = getPrefix(buildingId);
        String sequence = nextSequence(buildingId, floorNumber);
        return prefix + floorNumber + "---"+ sequence;
    }

    public UnitDto toDto(Unit unit) {
        if (unit == null) {
            return null;
        }
        
        String buildingId = null;
        String buildingCode = null;
        String buildingName = null;
        try {
            if (unit.getBuilding() != null) {
                buildingId = unit.getBuilding().getId().toString();
                buildingCode = unit.getBuilding().getCode();
                buildingName = unit.getBuilding().getName();
            }
        } catch (Exception e) {
        }
        
        return new UnitDto(
                unit.getId(),
                buildingId != null ? UUID.fromString(buildingId) : null,
                buildingCode,
                buildingName,
                unit.getCode(),
                unit.getFloor(),
                unit.getAreaM2(),
                unit.getBedrooms(),
                unit.getStatus(),
                null,  // primaryResidentId - không gắn owner khi tạo unit
                unit.getCreatedAt(),
                unit.getUpdatedAt()
        );
    }

    private void validateUnitCreateDto(UnitCreateDto dto) {
        if (dto.buildingId() == null) {
            throw new NullPointerException("Building ID cannot be null");
        }
        if (dto.floor() == null) {
            throw new NullPointerException("Floor cannot be null");
        }
        if (dto.floor() <= 0) {
            throw new IllegalArgumentException("Floor must be positive");
        }
        if (dto.floor() != Math.floor(dto.floor())) {
            throw new IllegalArgumentException("Floor must be an integer");
        }
        
        Building building = buildingRepository.findById(dto.buildingId())
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));
        
        // Unhappy Case: Building must be ACTIVE to create unit
        if (building.getStatus() != BuildingStatus.ACTIVE) {
            throw new IllegalStateException(
                    String.format("Không thể thêm căn hộ vì tòa nhà '%s' hiện không hoạt động (trạng thái: %s).", 
                            building.getName(), building.getStatus()));
        }
        
        if (building.getNumberOfFloors() != null && dto.floor() > building.getNumberOfFloors()) {
            throw new IllegalArgumentException(
                    String.format("Floor %d vượt quá số tầng của tòa nhà (%d tầng)", 
                            dto.floor(), building.getNumberOfFloors()));
        }
        
        if (dto.areaM2() == null) {
            throw new NullPointerException("Area cannot be null");
        }
        if (dto.areaM2().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Area must be positive");
        }
        if (dto.bedrooms() == null) {
            throw new NullPointerException("Bedrooms cannot be null");
        }
        if (dto.bedrooms() <= 0) {
            throw new IllegalArgumentException("Bedrooms must be positive");
        }
        if (dto.bedrooms() != Math.floor(dto.bedrooms())) {
            throw new IllegalArgumentException("Bedrooms must be an integer");
        }
    }

    private void validateUnitUpdateDto(UnitUpdateDto dto, UUID unitId) {
        if (dto.floor() == null) {
            throw new NullPointerException("Floor cannot be null");
        }
        if (dto.floor() <= 0) {
            throw new IllegalArgumentException("Floor must be positive");
        }
        if (dto.floor() != Math.floor(dto.floor())) {
            throw new IllegalArgumentException("Floor must be an integer");
        }
        
        Unit existingUnit = unitRepository.findByIdWithBuilding(unitId);
        if (existingUnit == null) {
            throw new IllegalArgumentException("Unit not found: " + unitId);
        }
        Building building = existingUnit.getBuilding();
        
        // Unhappy Case: Building must be ACTIVE to update unit
        if (building.getStatus() != BuildingStatus.ACTIVE) {
            throw new IllegalStateException(
                    String.format("Không thể cập nhật căn hộ vì tòa nhà '%s' hiện không hoạt động (trạng thái: %s).", 
                            building.getName(), building.getStatus()));
        }
        
        if (building.getNumberOfFloors() != null && dto.floor() > building.getNumberOfFloors()) {
            throw new IllegalArgumentException(
                    String.format("Floor %d vượt quá số tầng của tòa nhà (%d tầng)", 
                            dto.floor(), building.getNumberOfFloors()));
        }
        
        if (dto.areaM2() != null && dto.areaM2().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Area must be positive");
        }
        
        if (dto.bedrooms() != null) {
            if (dto.bedrooms() <= 0) {
                throw new IllegalArgumentException("Bedrooms must be positive");
            }
            if (dto.bedrooms() != Math.floor(dto.bedrooms())) {
                throw new IllegalArgumentException("Bedrooms must be an integer");
            }
        }
    }

    /**
     * Validates that the given floor in the building has not reached the maximum number of units.
     * @throws IllegalArgumentException if the floor already has {@value #MAX_UNITS_PER_FLOOR} or more units
     */
    private void validateMaxUnitsPerFloor(UUID buildingId, int floorNumber) {
        long count = unitRepository.countByBuildingIdAndFloorNumber(buildingId, Integer.valueOf(floorNumber));
        if (count >= MAX_UNITS_PER_FLOOR) {
            throw new IllegalArgumentException(
                    String.format("Tầng %d đã đủ tối đa %d căn hộ. Không thể thêm căn hộ mới trên tầng này.",
                            floorNumber, MAX_UNITS_PER_FLOOR));
        }
    }
}
