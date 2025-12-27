package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.MeterCreateReq;
import com.QhomeBase.baseservice.dto.MeterDto;
import com.QhomeBase.baseservice.dto.MeterUpdateReq;
import com.QhomeBase.baseservice.dto.UnitWithoutMeterDto;
import com.QhomeBase.baseservice.service.MeterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/meters")
@RequiredArgsConstructor
public class MeterController {

    private final MeterService meterService;

    @PostMapping
    public ResponseEntity<MeterDto> createMeter(@Valid @RequestBody MeterCreateReq req) {
        try {
            MeterDto result = meterService.create(req);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeterDto> getMeterById(@PathVariable UUID id) {
        try {
            MeterDto result = meterService.getById(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<MeterDto>> getAllMeters(
            @RequestParam(required = false) UUID unitId,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) Boolean active) {
        
        try {
            List<MeterDto> result;

            if (unitId != null) {
                result = meterService.getByUnitId(unitId);
            } else if (serviceId != null) {
                result = meterService.getByServiceId(serviceId);
            } else if (buildingId != null) {
                result = meterService.getByBuildingId(buildingId);
            } else if (active != null) {
                result = meterService.getByActive(active);
            } else {
                result = meterService.getAll();
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/missing")
    public ResponseEntity<List<UnitWithoutMeterDto>> getUnitsWithoutMeter(
            @RequestParam UUID serviceId,
            @RequestParam(required = false) UUID buildingId) {
        try {
            List<UnitWithoutMeterDto> result = meterService.getUnitsDoNotHaveMeter(serviceId, buildingId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/missing")
    public ResponseEntity<List<MeterDto>> createMissingMeters(
            @RequestParam UUID serviceId,
            @RequestParam(required = false) UUID buildingId) {
        try {
            List<MeterDto> created = meterService.createMissingMeters(serviceId, buildingId);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/unit/{unitId}")
    public ResponseEntity<List<MeterDto>> getMetersByUnit(@PathVariable UUID unitId) {
        try {
            List<MeterDto> result = meterService.getByUnitId(unitId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<MeterDto>> getMetersByService(@PathVariable UUID serviceId) {
        try {
            List<MeterDto> result = meterService.getByServiceId(serviceId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<List<MeterDto>> getMetersByBuilding(@PathVariable UUID buildingId) {
        try {
            List<MeterDto> result = meterService.getByBuildingId(buildingId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<MeterDto> updateMeter(@PathVariable UUID id, @Valid @RequestBody MeterUpdateReq req) {
        try {
            MeterDto result = meterService.update(id, req);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateMeter(@PathVariable UUID id) {
        try {
            meterService.deactivate(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeter(@PathVariable UUID id) {
        try {
            meterService.delete(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

