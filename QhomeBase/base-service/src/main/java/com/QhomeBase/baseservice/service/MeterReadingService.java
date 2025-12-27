package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.MeterReadingCreateReq;
import com.QhomeBase.baseservice.dto.MeterReadingDto;
import com.QhomeBase.baseservice.dto.MeterReadingUpdateReq;
import com.QhomeBase.baseservice.model.Meter;
import com.QhomeBase.baseservice.model.MeterReading;
import com.QhomeBase.baseservice.model.MeterReadingAssignment;
import com.QhomeBase.baseservice.repository.MeterReadingAssignmentRepository;
import com.QhomeBase.baseservice.repository.MeterReadingRepository;
import com.QhomeBase.baseservice.repository.MeterRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;


import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class MeterReadingService {
    private final MeterReadingRepository readingRepo;
    private final MeterRepository meterRepo;
    private final MeterReadingAssignmentRepository assignmentRepo;

    @Transactional
    public MeterReadingDto create(MeterReadingCreateReq meterReadingCreateReq, Authentication auth){
        var p = (UserPrincipal) auth.getPrincipal();
        UUID readerId = meterReadingCreateReq.readerId() != null 
                ? meterReadingCreateReq.readerId() 
                : p.uid();
        
        MeterReadingAssignment meterReadingAssignment = null;
        
        if (meterReadingCreateReq.assignmentId() != null) {
            meterReadingAssignment = assignmentRepo.findById(meterReadingCreateReq.assignmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
        }
        
        Meter meter = meterRepo.findById(meterReadingCreateReq.meterId())
                .orElseThrow(() -> new IllegalArgumentException("Meter not found"));
        
        if (meter.getUnit() == null) {
            throw new IllegalArgumentException("Meter must have a unit");
        }
        
        BigDecimal previousIndex = meterReadingCreateReq.prevIndex() != null
                ? meterReadingCreateReq.prevIndex()
                : getPreviousIndex(meter.getId());
        
        if (previousIndex == null) {
            previousIndex = BigDecimal.ZERO;
        }
        
        if (meterReadingAssignment != null) {
            validateMeterInScope(meterReadingAssignment, meter);
            
            validateMeterInIncludedUnits(meterReadingAssignment.getId(), meter.getId());
            
            Optional<MeterReading> existingReading = readingRepo.findByMeterIdAndAssignmentId(
                meter.getId(), 
                meterReadingAssignment.getId()
            );
            
            if (existingReading.isPresent()) {
                MeterReading reading = existingReading.get();
                reading.setReadingDate(meterReadingCreateReq.readingDate());
                reading.setCurrIndex(meterReadingCreateReq.currIndex());
                reading.setPrevIndex(previousIndex);
                reading.setNote(meterReadingCreateReq.note());
                reading.setPhotoFileId(meterReadingCreateReq.photoFileId());
                reading.setReaderId(readerId);
                reading.setReadAt(java.time.OffsetDateTime.now());
                
                // Update cycleId if provided
                if (meterReadingCreateReq.cycleId() != null) {
                    reading.setCycleId(meterReadingCreateReq.cycleId());
                } else if (meterReadingAssignment.getCycle() != null) {
                    reading.setCycleId(meterReadingAssignment.getCycle().getId());
                }
                
                MeterReading updated = readingRepo.save(reading);
                return toDto(updated);
            }
        }
        
        UUID cycleId = null;
        if (meterReadingCreateReq.cycleId() != null) {
            cycleId = meterReadingCreateReq.cycleId();
        } else if (meterReadingAssignment != null && meterReadingAssignment.getCycle() != null) {
            cycleId = meterReadingAssignment.getCycle().getId();
        }
        
        MeterReading meterReading = MeterReading.builder()
                .meter(meter)
                .unit(meter.getUnit())
                .assignment(meterReadingAssignment)
                .cycleId(cycleId)
                .readingDate(meterReadingCreateReq.readingDate())
                .prevIndex(previousIndex)
                .currIndex(meterReadingCreateReq.currIndex())
                .note(meterReadingCreateReq.note())
                .readerId(readerId)
                .photoFileId(meterReadingCreateReq.photoFileId())
                .build();
        
        MeterReading saved = readingRepo.save(meterReading);
        return toDto(saved);
    }

    public BigDecimal getPreviousIndex (UUID meterId){
        List<MeterReading> meterReadingList = readingRepo.findByMeterId(meterId);
        MeterReading latest = meterReadingList.stream()
                .filter(r -> r != null && r.getReadingDate() != null)
                .sorted(
                        Comparator.comparing(MeterReading::getReadingDate).reversed()
                                .thenComparing(MeterReading::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .findFirst()
                .orElse(null);
        return latest != null && latest.getCurrIndex() != null ? latest.getCurrIndex() : BigDecimal.ZERO;
    }
    public void validateMeterInScope(MeterReadingAssignment a, Meter m) {
        if (m.getUnit() == null) {
            throw new IllegalArgumentException("Meter must have a unit");
        }
        if (m.getUnit().getBuilding() == null) {
            throw new IllegalArgumentException("Unit must have a building");
        }
        
        UUID assBuilding = a.getBuilding().getId();
        UUID mBuilding = m.getUnit().getBuilding().getId();
        if (!assBuilding.equals(mBuilding)) {
            throw new IllegalArgumentException("Not same building");
        }
        
        Integer unitFloor = m.getUnit().getFloor();
        if (a.getFloor() != null && !a.getFloor().equals(unitFloor)) {
            throw new IllegalArgumentException("Unit floor does not match assignment floor");
        }
    }
    public void validateMeterInIncludedUnits(UUID assignmentId, UUID meterId) {
        Meter meter = meterRepo.findById(meterId)
                .orElseThrow(() -> new IllegalArgumentException("Meter not found: " + meterId));
        
        if (meter.getUnit() == null) {
            throw new IllegalArgumentException("Meter must have a unit");
        }
        
        UUID unitId = meter.getUnit().getId();
        
        MeterReadingAssignment assignment = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));
        
        List<UUID> includedUnitIds = assignment.getUnitIds();
        
        if (includedUnitIds != null && !includedUnitIds.isEmpty()) {
            if (!includedUnitIds.contains(unitId)) {
                throw new IllegalArgumentException(
                    String.format("Unit %s is not in included units list for assignment %s. This meter reading is not allowed.", 
                        unitId, assignmentId)
                );
            }
        }
    }

    @Transactional
    public MeterReadingDto update(UUID readingId, MeterReadingUpdateReq updateReq, Authentication auth) {
        var p = (UserPrincipal) auth.getPrincipal();
        
        MeterReading reading = readingRepo.findById(readingId)
                .orElseThrow(() -> new IllegalArgumentException("Meter reading not found: " + readingId));
        
        if (updateReq.readingDate() != null) {
            reading.setReadingDate(updateReq.readingDate());
        }
        
        if (updateReq.prevIndex() != null) {
            reading.setPrevIndex(updateReq.prevIndex());
        }
        
        if (updateReq.currIndex() != null) {
            reading.setCurrIndex(updateReq.currIndex());
        }
        
        if (updateReq.photoFileId() != null) {
            reading.setPhotoFileId(updateReq.photoFileId());
        }
        
        if (updateReq.note() != null) {
            reading.setNote(updateReq.note());
        }
        
        reading.setReadAt(java.time.OffsetDateTime.now());
        
        MeterReading updated = readingRepo.save(reading);
        return toDto(updated);
    }
    
    @Transactional(readOnly = true)
    public List<MeterReadingDto> getByCycleAndAssignmentAndUnitId(UUID cycleId, UUID assignmentId, UUID unitId) {
        List<MeterReading> readings = readingRepo.findByCycleAndAssignmentAndUnitId(cycleId, assignmentId, unitId);
        return readings.stream()
                .map(this::toDto)
                .toList();
    }

    public  MeterReadingDto toDto(MeterReading r) {
        BigDecimal prev   = r.getPrevIndex();
        BigDecimal curr   = r.getCurrIndex();
        BigDecimal usage  = null;
        if (curr != null && prev != null) {
            usage = curr.subtract(prev);
            if (usage.signum() < 0) {
                throw new IllegalArgumentException("Current index must be >= previous index");
            }
        }
        return new MeterReadingDto(
                r.getId(),
                r.getAssignment() != null ? r.getAssignment().getId() : null,
                r.getCycleId(),
                r.getMeter().getId(),
                r.getMeter().getMeterCode(),
                r.getUnit() != null ? r.getUnit().getId() : null,
                r.getUnit() != null ? r.getUnit().getCode() : null,
                r.getUnit() != null ? r.getUnit().getFloor() : null,
                r.getPrevIndex(),
                r.getCurrIndex(),
                usage,
                r.getReadingDate(),
                r.getNote(),
                r.getReaderId(),
                r.getPhotoFileId(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
