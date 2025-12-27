package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.VehicleCreateDto;
import com.QhomeBase.baseservice.dto.VehicleDto;
import com.QhomeBase.baseservice.dto.VehicleUpdateDto;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.model.Vehicle;
import com.QhomeBase.baseservice.model.VehicleKind;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final UnitRepository unitRepository;

    private OffsetDateTime nowUTC() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    @Transactional
    public VehicleDto createVehicle(VehicleCreateDto dto) {
        validateVehicleCreateDto(dto);
        
        if (vehicleRepository.existsByPlateNo(dto.plateNo())) {
            throw new IllegalStateException("Vehicle with this plate number already exists");
        }

        Unit unit = null;
        if (dto.unitId() != null) {
            unit = unitRepository.findById(dto.unitId())
                    .orElseThrow(() -> new IllegalArgumentException("Unit not found"));
        }

        var vehicle = Vehicle.builder()
                .residentId(dto.residentId())
                .unit(unit)
                .plateNo(dto.plateNo())
                .kind(dto.kind() != null ? dto.kind() : VehicleKind.OTHER)
                .color(dto.color())
                .active(true)
                .createdAt(nowUTC())
                .updatedAt(nowUTC())
                .build();

        var savedVehicle = vehicleRepository.save(vehicle);
        return toDto(savedVehicle);
    }

    @Transactional
    public VehicleDto updateVehicle(VehicleUpdateDto dto, UUID id) {
        validateVehicleUpdateDto(dto);
        
        var vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        if (dto.residentId() != null) {
            vehicle.setResidentId(dto.residentId());
        }
        if (dto.unitId() != null) {
            Unit unit = unitRepository.findById(dto.unitId())
                    .orElseThrow(() -> new IllegalArgumentException("Unit not found"));
            vehicle.setUnit(unit);
        }
        if (dto.plateNo() != null) {
            if (vehicleRepository.existsByPlateNoAndIdNot(dto.plateNo(), id)) {
                throw new IllegalStateException("Vehicle with this plate number already exists");
            }
            vehicle.setPlateNo(dto.plateNo());
        }
        if (dto.kind() != null) {
            vehicle.setKind(dto.kind());
        }
        if (dto.color() != null) {
            vehicle.setColor(dto.color());
        }
        if (dto.active() != null) {
            vehicle.setActive(dto.active());
        }

        vehicle.setUpdatedAt(nowUTC());

        var savedVehicle = vehicleRepository.save(vehicle);
        return toDto(savedVehicle);
    }

    @Transactional
    public void deleteVehicle(UUID id) {
        var vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        vehicle.setActive(false);
        vehicle.setUpdatedAt(nowUTC());
        vehicleRepository.save(vehicle);
    }

    public VehicleDto getVehicleById(UUID id) {
        var vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        return toDto(vehicle);
    }

    public List<VehicleDto> getAllVehicles() {
        var vehicles = vehicleRepository.findAll();
        return vehicles.stream()
                .map(this::toDto)
                .toList();
    }

    public List<VehicleDto> getVehiclesByResidentId(UUID residentId) {
        var vehicles = vehicleRepository.findAllByResidentId(residentId);
        return vehicles.stream()
                .map(this::toDto)
                .toList();
    }

    public List<VehicleDto> getVehiclesByUnitId(UUID unitId) {
        var vehicles = vehicleRepository.findAllByUnitId(unitId);
        return vehicles.stream()
                .map(this::toDto)
                .toList();
    }

    public List<VehicleDto> getActiveVehicles() {
        var vehicles = vehicleRepository.findAllByActiveTrue();
        return vehicles.stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void changeVehicleStatus(UUID id, Boolean active) {
        var vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        
        vehicle.setActive(active);
        vehicle.setUpdatedAt(nowUTC());
        vehicleRepository.save(vehicle);
    }

    public VehicleDto toDto(Vehicle vehicle) {
        UUID unitId = null;
        String unitCode = null;
        
        try {
            if (vehicle.getUnit() != null) {
                unitId = vehicle.getUnit().getId();
                unitCode = vehicle.getUnit().getCode();
            }
        } catch (Exception e) {
            // Lazy loading might fail, that's ok
        }
        
        return new VehicleDto(
                vehicle.getId(),
                vehicle.getResidentId(),
                null, // residentName - should be fetched from IAM service if needed
                unitId,
                unitCode,
                vehicle.getPlateNo(),
                vehicle.getKind(),
                vehicle.getColor(),
                vehicle.getActive(),
                vehicle.getActivatedAt(),
                vehicle.getRegistrationApprovedAt(),
                vehicle.getApprovedBy(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }

    private void validateVehicleCreateDto(VehicleCreateDto dto) {
        if (dto.plateNo() == null) {
            throw new NullPointerException("Plate number cannot be null");
        }
        if (dto.plateNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Plate number cannot be empty");
        }
        if (dto.plateNo().length() > 20) {
            throw new IllegalArgumentException("Plate number cannot exceed 20 characters");
        }
        if (dto.color() != null && dto.color().length() > 50) {
            throw new IllegalArgumentException("Color cannot exceed 50 characters");
        }
    }

    private void validateVehicleUpdateDto(VehicleUpdateDto dto) {
        if (dto.plateNo() == null) {
            throw new NullPointerException("Plate number cannot be null");
        }
        if (dto.plateNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Plate number cannot be empty");
        }
        if (dto.plateNo().length() > 20) {
            throw new IllegalArgumentException("Plate number cannot exceed 20 characters");
        }
        if (dto.color() != null && dto.color().length() > 50) {
            throw new IllegalArgumentException("Color cannot exceed 50 characters");
        }
    }
}
