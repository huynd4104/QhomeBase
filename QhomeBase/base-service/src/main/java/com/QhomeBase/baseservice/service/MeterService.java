package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.*;
import com.QhomeBase.baseservice.model.Meter;
import com.QhomeBase.baseservice.model.MeterReading;
import com.QhomeBase.baseservice.model.MeterReadingAssignment;
import com.QhomeBase.baseservice.model.ReadingCycle;
import com.QhomeBase.baseservice.model.Service;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.MeterReadingAssignmentRepository;
import com.QhomeBase.baseservice.repository.MeterReadingRepository;
import com.QhomeBase.baseservice.repository.MeterRepository;
import com.QhomeBase.baseservice.repository.ReadingCycleRepository;
import com.QhomeBase.baseservice.repository.ServiceRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class MeterService {

    private final MeterRepository meterRepository;
    private final MeterReadingAssignmentRepository assignmentRepository;
    private final UnitRepository unitRepository;
    private final ServiceRepository serviceRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final ReadingCycleRepository readingCycleRepository;

    @Transactional(readOnly = true)
    public List<MeterDto> getMetersByAssignment(UUID assignmentId) {
        MeterReadingAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        List<Meter> meters;
        
        UUID buildingId = assignment.getBuilding() != null ? assignment.getBuilding().getId() : null;
        UUID serviceId = assignment.getService().getId();
        Integer floor = assignment.getFloor();
        List<UUID> unitIds = assignment.getUnitIds();
        
        if (buildingId == null) {
            throw new IllegalArgumentException("Assignment must have a building");
        }
        
        if (unitIds != null && !unitIds.isEmpty()) {
            List<Meter> allMeters = floor != null 
                ? meterRepository.findByBuildingServiceAndFloor(buildingId, serviceId, floor)
                : meterRepository.findByBuildingAndService(buildingId, serviceId);
            
            meters = allMeters.stream()
                .filter(m -> m.getUnit() != null && unitIds.contains(m.getUnit().getId()))
                .toList();
        } else if (floor != null) {
            meters = meterRepository.findByBuildingServiceAndFloor(buildingId, serviceId, floor);
        } else {
            meters = meterRepository.findByBuildingAndService(buildingId, serviceId);
        }

        return meters.stream()
                .map(this::toDto)
                .toList();
    }
    public List<UnitWithoutMeterDto> getUnitsDoNotHaveMeter(UUID serviceId, UUID buildingId) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        Set<UUID> unitsWithMeter = new HashSet<>(meterRepository.findUnitIdsByServiceId(serviceId));
        List<Unit> units = buildingId != null
                ? unitRepository.findAllByBuildingId(buildingId)
                : unitRepository.findAllWithBuilding();
        return units.stream()
                .filter(unit -> !unitsWithMeter.contains(unit.getId()))
                .map(unit -> new UnitWithoutMeterDto(
                        unit.getId(),
                        unit.getCode(),
                        unit.getFloor(),
                        unit.getBuilding() != null ? unit.getBuilding().getId() : null,
                        unit.getBuilding() != null ? unit.getBuilding().getCode() : null,
                        unit.getBuilding() != null ? unit.getBuilding().getName() : null,
                        service.getId(),
                        service.getCode(),
                        service.getName()
                ))
                .toList();
    }

    @Transactional
    public List<MeterDto> createMissingMeters(UUID serviceId, UUID buildingId) {
        List<UnitWithoutMeterDto> missingUnits = getUnitsDoNotHaveMeter(serviceId, buildingId);
        List<MeterDto> created = new ArrayList<>();
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
        for (UnitWithoutMeterDto dto : missingUnits) {
            Unit unit = unitRepository.findById(dto.unitId())
                    .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + dto.unitId()));
            String meterCode = generateMeterCode(unit, service);
            Meter meter = Meter.builder()
                    .unit(unit)
                    .service(service)
                    .meterCode(meterCode)
                    .active(true)
                    .installedAt(LocalDate.now())
                    .build();
            Meter saved = meterRepository.save(meter);
            created.add(toDto(saved));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<MeterDto> getMetersByStaffAndCycle(UUID staffId, UUID cycleId) {
        List<MeterReadingAssignment> assignments = assignmentRepository.findByAssignedToAndCycleId(staffId, cycleId);
        
        if (assignments.isEmpty()) {
            return List.of();
        }
        
        java.util.Set<UUID> uniqueMeterIds = new java.util.HashSet<>();
        List<Meter> allMeters = new java.util.ArrayList<>();
        
        for (MeterReadingAssignment assignment : assignments) {
            UUID buildingId = assignment.getBuilding() != null ? assignment.getBuilding().getId() : null;
            UUID serviceId = assignment.getService().getId();
            Integer floor = assignment.getFloor();
            List<UUID> unitIds = assignment.getUnitIds();
            
            if (buildingId == null) {
                continue;
            }
            
            List<Meter> meters;
            if (unitIds != null && !unitIds.isEmpty()) {
                List<Meter> allMetersForAssignment = floor != null 
                    ? meterRepository.findByBuildingServiceAndFloor(buildingId, serviceId, floor)
                    : meterRepository.findByBuildingAndService(buildingId, serviceId);
                
                meters = allMetersForAssignment.stream()
                    .filter(m -> m.getUnit() != null && unitIds.contains(m.getUnit().getId()))
                    .toList();
            } else if (floor != null) {
                meters = meterRepository.findByBuildingServiceAndFloor(buildingId, serviceId, floor);
            } else {
                meters = meterRepository.findByBuildingAndService(buildingId, serviceId);
            }
            
            for (Meter meter : meters) {
                if (!uniqueMeterIds.contains(meter.getId())) {
                    uniqueMeterIds.add(meter.getId());
                    allMeters.add(meter);
                }
            }
        }
        
        return allMeters.stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeterWithReadingDto> getMetersWithReadingByStaffAndCycle(UUID staffId, UUID cycleId) {
        ReadingCycle cycle = readingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + cycleId));
        
        List<MeterReadingAssignment> assignments = assignmentRepository.findByAssignedToAndCycleId(staffId, cycleId);
        
        if (assignments.isEmpty()) {
            return List.of();
        }
        
        java.util.Set<UUID> uniqueMeterIds = new java.util.HashSet<>();
        List<Meter> allMeters = new java.util.ArrayList<>();
        
        for (MeterReadingAssignment assignment : assignments) {
            UUID buildingId = assignment.getBuilding() != null ? assignment.getBuilding().getId() : null;
            UUID serviceId = assignment.getService().getId();
            Integer floor = assignment.getFloor();
            List<UUID> unitIds = assignment.getUnitIds();
            
            if (buildingId == null) {
                continue;
            }
            
            List<Meter> meters;
            if (unitIds != null && !unitIds.isEmpty()) {
                List<Meter> allMetersForAssignment = floor != null 
                    ? meterRepository.findByBuildingServiceAndFloor(buildingId, serviceId, floor)
                    : meterRepository.findByBuildingAndService(buildingId, serviceId);
                
                meters = allMetersForAssignment.stream()
                    .filter(m -> m.getUnit() != null && unitIds.contains(m.getUnit().getId()))
                    .toList();
            } else if (floor != null) {
                meters = meterRepository.findByBuildingServiceAndFloor(buildingId, serviceId, floor);
            } else {
                meters = meterRepository.findByBuildingAndService(buildingId, serviceId);
            }
            
            for (Meter meter : meters) {
                if (!uniqueMeterIds.contains(meter.getId())) {
                    uniqueMeterIds.add(meter.getId());
                    allMeters.add(meter);
                }
            }
        }
        
        return allMeters.stream()
                .map(meter -> toMeterWithReadingDto(meter, cycle))
                .toList();
    }

    private MeterWithReadingDto toMeterWithReadingDto(Meter meter, ReadingCycle cycle) {
        java.math.BigDecimal prevIndex = null;
        java.math.BigDecimal currIndex = null;
        Boolean hasReading = false;
        UUID readingId = null;
        LocalDate readingDate = null;
        
        List<MeterReading> cycleReadings = meterReadingRepository.findByMeterIdAndCycleId(meter.getId(), cycle.getId());
        MeterReading cycleReading = cycleReadings.stream()
                .max(Comparator.comparing(MeterReading::getReadingDate)
                        .thenComparing((MeterReading mr) -> 
                            mr.getCreatedAt() != null ? mr.getCreatedAt() : OffsetDateTime.MIN))
                .orElse(null);
        
        if (cycleReading != null) {
            hasReading = true;
            readingId = cycleReading.getId();
            readingDate = cycleReading.getReadingDate();
            prevIndex = cycleReading.getPrevIndex();
            currIndex = cycleReading.getCurrIndex();
        } else {
            List<MeterReading> previousReadings = meterReadingRepository.findPreviousReadings(
                    meter.getId(), cycle.getPeriodFrom());
            if (!previousReadings.isEmpty()) {
                MeterReading previousReading = previousReadings.get(0);
                prevIndex = previousReading.getCurrIndex();
            }
        }
        
        return new MeterWithReadingDto(
                meter.getId(),
                meter.getUnit() != null ? meter.getUnit().getId() : null,
                meter.getUnit() != null ? meter.getUnit().getCode() : null,
                meter.getUnit() != null ? meter.getUnit().getFloor() : null,
                meter.getService() != null ? meter.getService().getId() : null,
                meter.getService() != null ? meter.getService().getCode() : null,
                meter.getService() != null ? meter.getService().getName() : null,
                meter.getMeterCode(),
                meter.getActive(),
                meter.getInstalledAt(),
                meter.getRemovedAt(),
                prevIndex,
                currIndex,
                hasReading,
                readingId,
                readingDate,
                meter.getCreatedAt(),
                meter.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public MeterDto getById(UUID id) {
        return meterRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Meter not found"));
    }

    @Transactional(readOnly = true)
    public List<MeterDto> getByUnitId(UUID unitId) {
        return meterRepository.findByUnitId(unitId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeterDto> getByServiceId(UUID serviceId) {
        return meterRepository.findByServiceId(serviceId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeterDto> getAll() {
        return meterRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeterDto> getByBuildingId(UUID buildingId) {
        return meterRepository.findByBuildingId(buildingId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeterDto> getByActive(Boolean active) {
        return meterRepository.findByActive(active).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public MeterDto create(MeterCreateReq req) {
        var unit = unitRepository.findById(req.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + req.unitId()));

        var service = serviceRepository.findById(req.serviceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + req.serviceId()));

        if (meterRepository.findByMeterCode(req.meterCode()).isPresent()) {
            throw new IllegalStateException("Meter code already exists: " + req.meterCode());
        }

        var existingMeter = meterRepository.findByUnitAndService(req.unitId(), req.serviceId());
        if (existingMeter.isPresent() && existingMeter.get().getActive()) {
            throw new IllegalStateException(
                    "Active meter already exists for unit " + req.unitId() + " and service " + req.serviceId());
        }

        String meterCode = req.meterCode();
        if (meterCode == null || meterCode.isBlank()) {
            meterCode = generateMeterCode(unit, service);
        }

        var meter = Meter.builder()
                .unit(unit)
                .service(service)
                .meterCode(meterCode)
                .active(true)
                .installedAt(req.installedAt() != null ? req.installedAt() : LocalDate.now())
                .build();

        var savedMeter = meterRepository.save(meter);
        log.info("Created meter: {} for unit: {} service: {}", savedMeter.getId(), req.unitId(), req.serviceId());

        return toDto(savedMeter);
    }

    private String generateMeterCode(Unit unit, Service service) {
        String unitCode = unit.getCode() != null ? unit.getCode() : unit.getId().toString().substring(0, 8);
        String serviceCode = service.getCode() != null ? service.getCode() : service.getId().toString().substring(0, 8);
        String base = (unitCode + "-" + serviceCode).toUpperCase().replaceAll("\\s+", "");
        String candidate = base;
        int suffix = 1;
        while (meterRepository.findByMeterCode(candidate).isPresent()) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    @Transactional
    public MeterDto update(UUID id, MeterUpdateReq req) {
        var meter = meterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meter not found: " + id));

        if (req.meterCode() != null && !req.meterCode().isBlank()) {
            var existingMeter = meterRepository.findByMeterCode(req.meterCode());
            if (existingMeter.isPresent() && !existingMeter.get().getId().equals(id)) {
                throw new IllegalStateException("Meter code already exists: " + req.meterCode());
            }
            meter.setMeterCode(req.meterCode());
        }

        if (req.active() != null) {
            meter.setActive(req.active());
            
            if (!req.active() && meter.getRemovedAt() == null) {
                meter.setRemovedAt(LocalDate.now());
            }
            if (req.active() && meter.getRemovedAt() != null) {
                meter.setRemovedAt(null);
            }
        }

        if (req.removedAt() != null) {
            meter.setRemovedAt(req.removedAt());
            if (meter.getActive()) {
                meter.setActive(false);
            }
        }

        var updatedMeter = meterRepository.save(meter);
        log.info("Updated meter: {}", id);

        return toDto(updatedMeter);
    }

    @Transactional
    public void delete(UUID id) {
        var meter = meterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meter not found: " + id));

        var readings = meterReadingRepository.findByMeterId(id);
        if (!readings.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete meter with existing readings. Please deactivate instead. Meter has " 
                    + readings.size() + " reading(s)");
        }

        meterRepository.delete(meter);
        log.info("Deleted meter: {}", id);
    }

    @Transactional
    public void deactivate(UUID id) {
        var meter = meterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meter not found: " + id));

        meter.setActive(false);
        if (meter.getRemovedAt() == null) {
            meter.setRemovedAt(LocalDate.now());
        }
        meterRepository.save(meter);
        log.info("Deactivated meter: {}", id);
    }

    private MeterDto toDto(Meter meter) {
        var readings = meterReadingRepository.findByMeterId(meter.getId());
        Double lastReading = null;
        LocalDate lastReadingDate = null;

        if (!readings.isEmpty()) {
            var latestReading = readings.stream()
                    .max(Comparator.comparing(MeterReading::getReadingDate)
                            .thenComparing((MeterReading mr) -> 
                                mr.getCreatedAt() != null ? mr.getCreatedAt() : OffsetDateTime.MIN))
                    .orElse(null);

            if (latestReading != null) {
                lastReading = latestReading.getCurrIndex() != null 
                        ? latestReading.getCurrIndex().doubleValue() 
                        : null;
                lastReadingDate = latestReading.getReadingDate();
            }
        }

        UUID buildingId = meter.getUnit() != null && meter.getUnit().getBuilding() != null
                ? meter.getUnit().getBuilding().getId() : null;
        String buildingCode = meter.getUnit() != null && meter.getUnit().getBuilding() != null
                ? meter.getUnit().getBuilding().getCode() : null;

        return new MeterDto(
                meter.getId(),
                meter.getUnit() != null ? meter.getUnit().getId() : null,
                buildingId,
                buildingCode,
                meter.getUnit() != null ? meter.getUnit().getCode() : null,
                meter.getUnit() != null ? meter.getUnit().getFloor() : null,
                meter.getService() != null ? meter.getService().getId() : null,
                meter.getService() != null ? meter.getService().getCode() : null,
                meter.getService() != null ? meter.getService().getName() : null,
                meter.getMeterCode(),
                meter.getActive(),
                meter.getInstalledAt(),
                meter.getRemovedAt(),
                lastReading,
                lastReadingDate,
                meter.getCreatedAt(),
                meter.getUpdatedAt()
        );
    }
}

