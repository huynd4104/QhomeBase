package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.UnitCreateDto;
import com.QhomeBase.baseservice.dto.UnitDto;
import com.QhomeBase.baseservice.dto.UnitUpdateDto;
import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {
    private final UnitService unitService;

    @PostMapping
    @PreAuthorize("@authz.canCreateUnit(#dto.buildingId())")
    public ResponseEntity<?> createUnit(@Valid @RequestBody UnitCreateDto dto, Authentication auth) {
        try {
            UnitDto result = unitService.createUnit(dto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.canUpdateUnit(#id)")
    public ResponseEntity<?> updateUnit(@PathVariable UUID id, @Valid @RequestBody UnitUpdateDto dto) {
        try {
            UnitDto result = unitService.updateUnit(dto, id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.canDeleteUnit(#id)")
    public ResponseEntity<Void> deleteUnit(@PathVariable UUID id) {
        try {
            unitService.deleteUnit(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canViewUnit(#id)")
    public ResponseEntity<UnitDto> getUnitById(@PathVariable UUID id) {
        try {
            UnitDto result = unitService.getUnitById(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/building/{buildingId}")
    @PreAuthorize("@authz.canViewUnitsByBuilding(#buildingId)")
    public ResponseEntity<List<UnitDto>> getUnitsByBuildingId(@PathVariable UUID buildingId) {
        List<UnitDto> result = unitService.getUnitsByBuildingId(buildingId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/building/{buildingId}/floor/{floor}")
    @PreAuthorize("@authz.canViewUnits()")
    public ResponseEntity<List<UnitDto>> getUnitsByFloor(@PathVariable UUID buildingId, @PathVariable Integer floor) {
        List<UnitDto> result = unitService.getUnitsByFloor(buildingId, floor);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@authz.canManageUnitStatus(#id)")
    public ResponseEntity<Void> changeUnitStatus(@PathVariable UUID id, @RequestParam UnitStatus status) {
        try {
            unitService.changeUnitStatus(id, status);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
