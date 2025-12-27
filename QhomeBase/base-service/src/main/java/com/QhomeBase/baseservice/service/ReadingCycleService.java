package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.client.FinanceBillingClient;
import com.QhomeBase.baseservice.dto.ReadingCycleCreateReq;
import com.QhomeBase.baseservice.dto.ReadingCycleDto;
import com.QhomeBase.baseservice.dto.ReadingCycleUnassignedInfoDto;
import com.QhomeBase.baseservice.dto.ReadingCycleUpdateReq;
import com.QhomeBase.baseservice.dto.UnitWithoutMeterDto;
import com.QhomeBase.baseservice.dto.finance.BillingCycleDto;
import com.QhomeBase.baseservice.dto.finance.CreateBillingCycleRequest;
import com.QhomeBase.baseservice.model.*;
import com.QhomeBase.baseservice.repository.*;
import com.QhomeBase.baseservice.security.UserPrincipal;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReadingCycleService {
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ReadingCycleRepository readingCycleRepository;
    private final MeterReadingAssignmentRepository assignmentRepository;
    private final ServiceRepository serviceRepository;
    private final MeterRepository meterRepository;
    private final UnitRepository unitRepository;
    private final MeterService meterService;
    private final FinanceBillingClient financeBillingClient;
    private final HouseholdService householdService;
    public ReadingCycleDto createCycle(ReadingCycleCreateReq req, Authentication authentication) {
        var principal = (UserPrincipal) authentication.getPrincipal();
        UUID createdBy = principal.uid();

        com.QhomeBase.baseservice.model.Service service = serviceRepository.findById(req.serviceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + req.serviceId()));

        YearMonth targetMonth = validateMonthlyWindow(req.periodFrom(), req.periodTo());
        ensureCycleDoesNotExist(targetMonth, req.serviceId());

        LocalDate periodTo = req.periodTo();
        if (periodTo == null || periodTo.equals(targetMonth.atEndOfMonth())) {
            periodTo = targetMonth.atDay(15);
        }

        ReadingCycle cycle = ReadingCycle.builder()
                .name(buildCycleName(targetMonth))
                .service(service)
                .periodFrom(targetMonth.atDay(1))
                .periodTo(periodTo)
                .status(ReadingCycleStatus.OPEN)
                .description(req.description())
                .createdBy(createdBy)
                .build();

        ReadingCycle saved = readingCycleRepository.save(cycle);
        pushBillingCycle(saved);
        return toDto(saved);
    }

    public List<ReadingCycleDto> getAllCycles() {
        return readingCycleRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ReadingCycleDto getCycleById(UUID cycleId) {
        ReadingCycle cycle = readingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Reading cycle not found with id: " + cycleId));
        return toDto(cycle);
    }

    public List<ReadingCycleDto> getCyclesByStatus(ReadingCycleStatus status) {
        return readingCycleRepository.findAll().stream()
                .filter(cycle -> cycle.getStatus() == status)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ReadingCycleDto> getCyclesByStatusAndService(ReadingCycleStatus status, UUID serviceId) {
        return readingCycleRepository.findByStatusAndServiceId(status, serviceId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ReadingCycleDto> getCyclesByService(UUID serviceId) {
        return readingCycleRepository.findByServiceId(serviceId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ReadingCycleDto> getCyclesByPeriod(LocalDate from, LocalDate to) {
        return readingCycleRepository.findAll().stream()
                .filter(cycle -> !cycle.getPeriodFrom().isAfter(to)
                        && !cycle.getPeriodTo().isBefore(from))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ReadingCycleDto updateCycle(UUID cycleId, ReadingCycleUpdateReq req, Authentication authentication) {
        ReadingCycle existing = readingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Reading cycle not found with id: " + cycleId));

        if (req.description() != null) {
            existing.setDescription(req.description());
        }
        if (req.status() != null) {
            validateStatusTransition(existing.getStatus(), req.status());
            existing.setStatus(req.status());
        }

        ReadingCycle saved = readingCycleRepository.save(existing);
        return toDto(saved);
    }

    public ReadingCycleDto changeCycleStatus(UUID cycleId, ReadingCycleStatus newStatus) {
        log.debug("Changing cycle {} status to {}", cycleId, newStatus);

        ReadingCycle existing = readingCycleRepository.findById(cycleId)
                .orElseThrow(() -> {
                    log.warn("Reading cycle not found with id: {}", cycleId);
                    return new IllegalArgumentException("Reading cycle not found with id: " + cycleId);
                });

        log.debug("Current cycle status: {}, requested status: {}", existing.getStatus(), newStatus);

        try {
            validateStatusTransition(existing.getStatus(), newStatus);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            log.warn("Status transition validation failed for cycle {}: {} -> {}: {}",
                    cycleId, existing.getStatus(), newStatus, ex.getMessage());
            throw ex;
        }

        if (newStatus == ReadingCycleStatus.COMPLETED) {
            validateAllAssigned(cycleId);
            validateAllAssignmentsCompleted(cycleId);
        }

        ReadingCycleStatus oldStatus = existing.getStatus();
        existing.setStatus(newStatus);
        ReadingCycle saved = readingCycleRepository.save(existing);

        log.info("Successfully changed cycle {} status from {} to {}",
                cycleId, oldStatus, newStatus);

        return toDto(saved);
    }

    private boolean pushBillingCycle(ReadingCycle cycle) {
        if (cycle.getService() == null) {
            log.warn("Skipping billing cycle push because service is null for reading cycle {}", cycle.getId());
            return false;
        }

        LocalDate periodFrom = cycle.getPeriodFrom();
        LocalDate periodTo = periodFrom.withDayOfMonth(24);

        CreateBillingCycleRequest request = CreateBillingCycleRequest.builder()
                .name(cycle.getName() + " • " + cycle.getService().getCode())
                .periodFrom(periodFrom)
                .periodTo(periodTo)
                .status("OPEN")
                .externalCycleId(cycle.getId())
                .build();

        log.info("Creating billing cycle for reading cycle {} (service {}): {} → {}",
                cycle.getId(),
                cycle.getService().getCode(),
                periodFrom,
                periodTo);

        try {
            List<BillingCycleDto> existing = financeBillingClient
                    .findBillingCyclesByExternalId(cycle.getId())
                    .block();
            if (existing != null && !existing.isEmpty()) {
                log.info("Skipped creating billing cycle for reading cycle {} because {} existing billing(s) found",
                        cycle.getId(), existing.size());
                return false;
            }

            BillingCycleDto billingCycle = financeBillingClient.createBillingCycle(request).block();
            log.info("Billing cycle created for reading cycle {} → billingId={}", cycle.getId(),
                    billingCycle != null ? billingCycle.getId() : "unknown");
            return true;
        } catch (Exception ex) {
            log.error("Failed to create billing cycle for reading cycle {}", cycle.getId(), ex);
            return false;
        }
    }

    public void deleteCycle(UUID cycleId) {
        ReadingCycle cycle = readingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Reading cycle not found with id: " + cycleId));

        if (cycle.getStatus() != ReadingCycleStatus.OPEN) {
            throw new IllegalStateException("Can only delete cycles with status OPEN");
        }

        readingCycleRepository.delete(cycle);
    }

    public ReadingCycle ensureMonthlyCycle(YearMonth month, UUID serviceId) {
        com.QhomeBase.baseservice.model.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        String cycleName = buildCycleName(month);
        return readingCycleRepository.findByNameAndServiceId(cycleName, serviceId)
                .orElseGet(() -> {
                    ReadingCycle cycle = ReadingCycle.builder()
                            .name(cycleName)
                            .service(service)
                            .periodFrom(month.atDay(1))
                            .periodTo(month.atDay(15))
                            .status(ReadingCycleStatus.OPEN)
                            .description("Auto-generated cycle for " + cycleName + " (reading period: 1-15)")
                            .build();
                    ReadingCycle saved = readingCycleRepository.save(cycle);
                    log.info("Auto-created reading cycle {} for service {} (period: {} to {})", 
                            cycleName, serviceId, month.atDay(1), month.atDay(15));
                    return saved;
                });
    }

    public void ensureBillingCycleFor(ReadingCycle cycle) {
        if (cycle == null) {
            return;
        }
        pushBillingCycle(cycle);
    }

    public BillingSyncResult syncBillingCycles() {
        List<ReadingCycle> cycles = readingCycleRepository.findAll();
        int created = 0;
        int skipped = 0;
        int failed = 0;
        for (ReadingCycle cycle : cycles) {
            try {
                boolean result = pushBillingCycle(cycle);
                if (result) {
                    created++;
                } else {
                    skipped++;
                }
            } catch (Exception ex) {
                log.error("Failed to sync billing cycle for reading cycle {}", cycle.getId(), ex);
                failed++;
            }
        }
        return new BillingSyncResult(cycles.size(), created, skipped, failed);
    }

    public record BillingSyncResult(int scanned, int created, int skipped, int failed) {}

    private void validateStatusTransition(ReadingCycleStatus current, ReadingCycleStatus next) {
        if (current == null || next == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        switch (current) {
            case CLOSED:
                throw new IllegalStateException("Cannot change status of a closed cycle");
            case OPEN:
                if (next != ReadingCycleStatus.IN_PROGRESS && next != ReadingCycleStatus.CLOSED) {
                    throw new IllegalStateException("OPEN cycle can only transition to IN_PROGRESS or CLOSED");
                }
                break;
            case IN_PROGRESS:
                if (next != ReadingCycleStatus.COMPLETED && next != ReadingCycleStatus.CLOSED) {
                    throw new IllegalStateException("IN_PROGRESS cycle can only transition to COMPLETED or CLOSED");
                }
                break;
            case COMPLETED:
                if (next != ReadingCycleStatus.CLOSED) {
                    throw new IllegalStateException("COMPLETED cycle can only transition to CLOSED");
                }
                break;
            default:
                throw new IllegalStateException("Unknown current status: " + current);
        }
    }

    private ReadingCycleDto toDto(ReadingCycle cycle) {
        return new ReadingCycleDto(
                cycle.getId(),
                cycle.getName(),
                cycle.getPeriodFrom(),
                cycle.getPeriodTo(),
                cycle.getStatus(),
                cycle.getService() != null ? cycle.getService().getId() : null,
                cycle.getService() != null ? cycle.getService().getCode() : null,
                cycle.getService() != null ? cycle.getService().getName() : null,
                cycle.getDescription(),
                cycle.getCreatedBy(),
                cycle.getCreatedAt(),
                cycle.getUpdatedAt()
        );
    }

    private YearMonth validateMonthlyWindow(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Period from/to cannot be null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Period from must be before period to");
        }

        YearMonth yearMonth = YearMonth.from(from);
        boolean isValidPeriod = (from.equals(yearMonth.atDay(1)) && to.equals(yearMonth.atDay(15))) ||
                                (from.equals(yearMonth.atDay(1)) && to.equals(yearMonth.atEndOfMonth()));
        
        if (!isValidPeriod) {
            throw new IllegalArgumentException("Reading cycles must be from day 1 to day 15, or from day 1 to end of month");
        }
        return yearMonth;
    }

    private void ensureCycleDoesNotExist(YearMonth month, UUID serviceId) {
        String cycleName = buildCycleName(month);
        readingCycleRepository.findByNameAndServiceId(cycleName, serviceId).ifPresent(existing -> {
            throw new IllegalStateException("Reading cycle already exists for " + cycleName + " and service " + serviceId);
        });
        List<ReadingCycle> overlaps = readingCycleRepository.findOverlappingCyclesByService(
                month.atDay(1), month.atDay(15), serviceId);
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("Reading cycle overlaps with an existing period for this service");
        }
    }

    private String buildCycleName(YearMonth month) {
        return month.format(MONTH_FORMATTER);
    }
    public ReadingCycleUnassignedInfoDto getUnassignedUnitsInfo(UUID cycleId, boolean onlyWithOwner) {
        return buildUnassignedUnitsInfo(cycleId, onlyWithOwner);
    }

    private ReadingCycleUnassignedInfoDto buildUnassignedUnitsInfo(UUID cycleId, boolean onlyWithOwner) {
        ReadingCycle readingCycle = readingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Reading cycle not found: " + cycleId));

        if (readingCycle.getService() == null) {
            throw new IllegalStateException("Reading cycle must have a service");
        }

        UUID serviceId = readingCycle.getService().getId();

        Set<UUID> assignedUnits = new HashSet<>();
        List<MeterReadingAssignment> allAssignments = assignmentRepository.findByCycleId(cycleId);
        log.debug("Total assignments for cycle {}: {}", cycleId, allAssignments.size());
        
        List<MeterReadingAssignment> filteredAssignments = allAssignments.stream()
                .filter(a -> a.getService() != null && Objects.equals(a.getService().getId(), serviceId))
                .collect(Collectors.toList());
        log.debug("Filtered assignments for service {}: {}", serviceId, filteredAssignments.size());
        
        for (MeterReadingAssignment assignment : filteredAssignments) {
            int beforeSize = assignedUnits.size();
            collectAssignedUnits(assignment, assignedUnits);
            int afterSize = assignedUnits.size();
            log.debug("Assignment {}: collected {} units (total: {})", assignment.getId(), afterSize - beforeSize, afterSize);
        }
        log.debug("Total assigned units collected: {}", assignedUnits.size());

        Set<UUID> unitsWithMeters = meterRepository.findByServiceId(serviceId).stream()
                .filter(meter -> Boolean.TRUE.equals(meter.getActive()))
                .map(Meter::getUnit)
                .filter(Objects::nonNull)
                .map(Unit::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> unassignedUnits = unitsWithMeters.stream()
                .filter(unitId -> !assignedUnits.contains(unitId))
                .collect(Collectors.toSet());
        log.debug("Units with meters: {}, Assigned units: {}, Unassigned units: {}", 
                unitsWithMeters.size(), assignedUnits.size(), unassignedUnits.size());
        if (!unassignedUnits.isEmpty()) {
            log.debug("Unassigned unit IDs: {}", unassignedUnits);
            for (UUID unitId : unassignedUnits) {
                unitRepository.findById(unitId).ifPresent(unit -> {
                    log.debug("Unassigned unit: {} (code: {}, building: {}, floor: {})", 
                            unitId, unit.getCode(), 
                            unit.getBuilding() != null ? unit.getBuilding().getCode() : "null",
                            unit.getFloor());
                });
            }
        }

        List<UnitWithoutMeterDto> missingMeterUnits = meterService.getUnitsDoNotHaveMeter(serviceId, null);

        List<UnassignedUnit> collectedUnits = new ArrayList<>();
        List<UnassignedUnit> collectedUnitsWithMeter = new ArrayList<>(); // Only units with meters (for validation)
        for (UUID unitId : unassignedUnits) {
            unitRepository.findById(unitId).ifPresent(unit -> {
                // Filter by primary resident if onlyWithOwner is true
                if (onlyWithOwner) {
                    Optional<UUID> primaryResidentId = householdService.getPrimaryResidentForUnit(unitId);
                    if (primaryResidentId.isEmpty()) {
                        return; // Skip units without primary resident
                    }
                }
                
                String buildingLabel = determineBuildingLabel(unit);
                UUID buildingId = unit.getBuilding() != null ? unit.getBuilding().getId() : null;
                String buildingCode = unit.getBuilding() != null ? unit.getBuilding().getCode() : null;
                String unitCode = unit.getCode() != null ? unit.getCode() : unitId.toString();
                UnassignedUnit unassignedUnit = new UnassignedUnit(
                        unitId, unitCode, unit.getFloor(), buildingId, buildingCode, buildingLabel, false);
                collectedUnits.add(unassignedUnit);
                collectedUnitsWithMeter.add(unassignedUnit); // Units with meters count for validation
            });
        }

        // Filter missingMeterUnits if onlyWithOwner is true
        List<UnitWithoutMeterDto> filteredMissingMeterUnits = missingMeterUnits;
        if (onlyWithOwner) {
            filteredMissingMeterUnits = missingMeterUnits.stream()
                    .filter(dto -> householdService.getPrimaryResidentForUnit(dto.unitId()).isPresent())
                    .collect(Collectors.toList());
        }

        // Add missing meter units to collectedUnits for display, but NOT to collectedUnitsWithMeter for validation
        for (UnitWithoutMeterDto dto : filteredMissingMeterUnits) {
            collectedUnits.add(new UnassignedUnit(
                    dto.unitId(),
                    dto.unitCode() != null ? dto.unitCode() : dto.unitId().toString(),
                    dto.floor(),
                    dto.buildingId(),
                    dto.buildingCode(),
                    determineBuildingLabel(dto.buildingCode(), dto.buildingName()),
                    true
            ));
        }

        // For validation: only count units with meters (not missingMeterUnits)
        int totalUnassignedForValidation = collectedUnitsWithMeter.size();

        // If no unassigned units at all (neither with meters nor missing meters), return empty result
        if (collectedUnitsWithMeter.isEmpty() && filteredMissingMeterUnits.isEmpty()) {
            return new ReadingCycleUnassignedInfoDto(cycleId, serviceId, 0, List.of(), "", filteredMissingMeterUnits);
        }

        // Group units for display - only show units with meters in the message since those are what matter for validation
        Map<FloorGroup, List<String>> groupedUnits = new HashMap<>();
        for (UnassignedUnit candidate : collectedUnitsWithMeter) {
            FloorGroup floorGroup = new FloorGroup(
                    candidate.buildingId,
                    candidate.buildingCode,
                    candidate.buildingLabel,
                    candidate.floor);
            List<String> unitCodes = groupedUnits.computeIfAbsent(floorGroup, group -> new ArrayList<>());
            String label = candidate.unitCode != null ? candidate.unitCode : candidate.unitId.toString();
            // Units in collectedUnitsWithMeter always have meters, so no need to check missingMeter flag
            unitCodes.add(label);
        }
        
        log.debug("Grouped {} units with meters into {} floor groups for cycle {}", 
                collectedUnitsWithMeter.size(), groupedUnits.size(), cycleId);
        
        if (collectedUnitsWithMeter.size() > 0 && groupedUnits.isEmpty()) {
            log.warn("⚠️ [ReadingCycleService] Found {} unassigned units but groupedUnits is empty for cycle {}. " +
                    "This may indicate an issue with building/floor grouping.", 
                    collectedUnitsWithMeter.size(), cycleId);
        }

        Comparator<Map.Entry<FloorGroup, List<String>>> entryComparator =
                Comparator.comparing((Map.Entry<FloorGroup, List<String>> entry) -> entry.getKey().buildingName,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(entry -> Optional.ofNullable(entry.getKey().floor).orElse(Integer.MIN_VALUE));

        List<ReadingCycleUnassignedInfoDto.ReadingCycleUnassignedFloorDto> floorDtos = groupedUnits.entrySet().stream()
                .sorted(entryComparator)
                .map(entry -> {
                    List<String> unitCodes = entry.getValue();
                    unitCodes.sort(String::compareTo);
                    return new ReadingCycleUnassignedInfoDto.ReadingCycleUnassignedFloorDto(
                            entry.getKey().buildingId,
                            entry.getKey().buildingCode,
                            entry.getKey().buildingName,
                            entry.getKey().floor,
                            unitCodes
                    );
                })
                .collect(Collectors.toList());

        // Build message using only units with meters for validation count
        // Always build message if there are unassigned units with meters (for validation errors)
        String message = "";
        if (totalUnassignedForValidation > 0) {
            if (!floorDtos.isEmpty()) {
                message = buildUnassignedMessage(totalUnassignedForValidation, floorDtos);
                log.debug("Built unassigned message for cycle {}: {} units across {} floors", 
                        cycleId, totalUnassignedForValidation, floorDtos.size());
            } else {
                // Fallback message if floorDtos is empty (should not happen, but handle gracefully)
                log.warn("⚠️ [ReadingCycleService] Found {} unassigned units but floorDtos is empty for cycle {}. " +
                        "Using fallback message. This may indicate an issue with unit grouping.", 
                        totalUnassignedForValidation, cycleId);
                // Build a more detailed fallback message using collectedUnitsWithMeter directly
                if (!collectedUnitsWithMeter.isEmpty()) {
                    Map<String, Integer> buildingCounts = new LinkedHashMap<>();
                    for (UnassignedUnit unit : collectedUnitsWithMeter) {
                        String buildingKey = unit.buildingCode != null ? unit.buildingCode :
                                (unit.buildingLabel != null ? unit.buildingLabel : "Unknown");
                        buildingCounts.merge(buildingKey, 1, Integer::sum);
                    }
                    StringBuilder fallbackMsg = new StringBuilder();
                    fallbackMsg.append("Còn ").append(totalUnassignedForValidation).append(" căn hộ/phòng chưa được assign:");
                    for (Map.Entry<String, Integer> entry : buildingCounts.entrySet()) {
                        fallbackMsg.append("\n").append(entry.getKey()).append(": ").append(entry.getValue()).append(" căn hộ");
                    }
                    message = fallbackMsg.toString();
                } else {
                    message = String.format("Còn %d căn hộ/phòng chưa được assign", totalUnassignedForValidation);
                }
            }
        }
        
        log.debug("Returning unassigned info for cycle {}: totalUnassigned={}, floors={}, missingMeterUnits={}, messageLength={}", 
                cycleId, totalUnassignedForValidation, floorDtos.size(), filteredMissingMeterUnits.size(), 
                message.length());
        
        return new ReadingCycleUnassignedInfoDto(cycleId, serviceId, totalUnassignedForValidation, floorDtos, message, filteredMissingMeterUnits);
    }

    private void collectAssignedUnits(MeterReadingAssignment assignment, Set<UUID> assignedUnits) {
        // First, collect units from explicit unitIds list if present
        if (assignment.getUnitIds() != null && !assignment.getUnitIds().isEmpty()) {
            assignedUnits.addAll(assignment.getUnitIds());
            log.debug("Assignment {}: collected {} units from unitIds", assignment.getId(), assignment.getUnitIds().size());
            // Even if unitIds exist, also check building/floor to ensure we don't miss any units
            // This handles cases where assignment might have both unitIds and building/floor info
        }
        
        // Then, collect units from building/floor if present
        if (assignment.getBuilding() != null) {
            if (assignment.getFloor() != null) {
                List<Unit> unitsInFloor = unitRepository.findByBuildingIdAndFloorNumber(
                        assignment.getBuilding().getId(), assignment.getFloor());
                int floorUnitsCollected = 0;
                for (Unit unit : unitsInFloor) {
                    if (unit.getId() != null && assignedUnits.add(unit.getId())) {
                        floorUnitsCollected++;
                    }
                }
                log.debug("Assignment {}: collected {} additional units from building {} floor {}", 
                        assignment.getId(), floorUnitsCollected, assignment.getBuilding().getId(), assignment.getFloor());
            } else {
                List<Unit> unitsInBuilding = unitRepository.findAllByBuildingId(
                        assignment.getBuilding().getId());
                int buildingUnitsCollected = 0;
                for (Unit unit : unitsInBuilding) {
                    if (unit.getId() != null && assignedUnits.add(unit.getId())) {
                        buildingUnitsCollected++;
                    }
                }
                log.debug("Assignment {}: collected {} additional units from building {}", 
                        assignment.getId(), buildingUnitsCollected, assignment.getBuilding().getId());
            }
        }
    }

    private String determineBuildingLabel(Unit unit) {
        if (unit == null) {
            return "Unknown";
        }
        String code = unit.getBuilding() != null ? unit.getBuilding().getCode() : null;
        String name = unit.getBuilding() != null ? unit.getBuilding().getName() : null;
        return determineBuildingLabel(code, name);
    }

    private String determineBuildingLabel(String code, String name) {
        if (code != null && !code.isBlank()) {
            return code;
        }
        if (name != null && !name.isBlank()) {
            return name;
        }
        return "Unknown";
    }

    private String buildUnassignedMessage(int total, List<ReadingCycleUnassignedInfoDto.ReadingCycleUnassignedFloorDto> floors) {
        // Aggregate units by building, separating those with/without meters
        Map<String, Integer> buildingUnassigned = new LinkedHashMap<>();
        Map<String, Integer> buildingMissingMeter = new LinkedHashMap<>();
        
        for (ReadingCycleUnassignedInfoDto.ReadingCycleUnassignedFloorDto floor : floors) {
            String buildingKey = floor.buildingCode() != null ? floor.buildingCode() :
                    floor.buildingName() != null ? floor.buildingName() : "Unknown";
            
            if (floor.unitCodes() != null) {
                for (String unitCode : floor.unitCodes()) {
                    buildingUnassigned.merge(buildingKey, 1, Integer::sum);
                    if (unitCode.contains("(chưa có công tơ)")) {
                        buildingMissingMeter.merge(buildingKey, 1, Integer::sum);
                    }
                }
            }
        }
        
        // Build optimized message showing only building totals
        StringBuilder message = new StringBuilder();
        message.append("Còn ").append(total).append(" căn hộ/phòng chưa được assign:");
        for (Map.Entry<String, Integer> entry : buildingUnassigned.entrySet()) {
            String buildingKey = entry.getKey();
            int totalUnits = entry.getValue();
            int missingMeterCount = buildingMissingMeter.getOrDefault(buildingKey, 0);
            
            message.append("\n").append(buildingKey).append(": ").append(totalUnits).append(" căn hộ");
            if (missingMeterCount > 0) {
                message.append(" (").append(missingMeterCount).append(" chưa có công tơ)");
            }
        }
        return message.toString();
    }

    private record FloorGroup(UUID buildingId, String buildingCode, String buildingName, Integer floor) {}

    private record UnassignedUnit(UUID unitId, String unitCode, Integer floor, UUID buildingId,
                                  String buildingCode, String buildingLabel, boolean missingMeter) {}

    private void validateAllAssigned(UUID cycleId) {
        // For validation, we only check units with primary resident (owner) and meters
        // Units without owner cannot have invoices, so they should not block COMPLETED status
        ReadingCycleUnassignedInfoDto info = getUnassignedUnitsInfo(cycleId, true);
        if (info.totalUnassigned() > 0) {
            throw new IllegalStateException(info.message());
        }
    }

    private void validateAllAssignmentsCompleted(UUID cycleId) {
        ReadingCycle readingCycle = readingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Reading cycle not found: " + cycleId));

        if (readingCycle.getService() == null) {
            throw new IllegalStateException("Reading cycle must have a service");
        }

        UUID serviceId = readingCycle.getService().getId();

        List<MeterReadingAssignment> incompleteAssignments =
                assignmentRepository.findByCycleId(cycleId).stream()
                        .filter(a -> a.getService() != null && Objects.equals(a.getService().getId(), serviceId))
                        .filter(a -> a.getCompletedAt() == null)
                        .collect(Collectors.toList());
        
        if (!incompleteAssignments.isEmpty()) {
            int incompleteCount = incompleteAssignments.size();
            log.warn("Cannot mark cycle {} as COMPLETED: {} assignment(s) are not completed for service {}", 
                    cycleId, incompleteCount, serviceId);
            throw new IllegalStateException(
                    String.format("Không thể đánh dấu cycle là COMPLETED. Còn %d assignment chưa hoàn thành.", 
                            incompleteCount)
            );
        }
        
        log.debug("All assignments for cycle {} (service {}) are completed", cycleId, serviceId);
    }
}
