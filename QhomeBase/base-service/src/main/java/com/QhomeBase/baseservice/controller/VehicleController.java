package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.VehicleCreateDto;
import com.QhomeBase.baseservice.dto.VehicleDto;
import com.QhomeBase.baseservice.dto.VehicleUpdateDto;
import com.QhomeBase.baseservice.model.VehicleKind;
import com.QhomeBase.baseservice.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("@authz.canCreateVehicle()")
    public ResponseEntity<VehicleDto> createVehicle(@Valid @RequestBody VehicleCreateDto dto) {
        VehicleDto result = vehicleService.createVehicle(dto);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.canUpdateVehicle(#id)")
    public ResponseEntity<VehicleDto> updateVehicle(@PathVariable UUID id, @Valid @RequestBody VehicleUpdateDto dto) {
        VehicleDto result = vehicleService.updateVehicle(dto, id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.canDeleteVehicle(#id)")
    public ResponseEntity<Void> deleteVehicle(@PathVariable UUID id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canViewVehicle(#id)")
    public ResponseEntity<VehicleDto> getVehicleById(@PathVariable UUID id) {
        VehicleDto result = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @PreAuthorize("@authz.canViewVehicles()")
    public ResponseEntity<List<VehicleDto>> getAllVehicles() {
        List<VehicleDto> result = vehicleService.getAllVehicles();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/resident/{residentId}")
    @PreAuthorize("@authz.canViewVehiclesByResident(#residentId)")
    public ResponseEntity<List<VehicleDto>> getVehiclesByResidentId(@PathVariable UUID residentId) {
        List<VehicleDto> result = vehicleService.getVehiclesByResidentId(residentId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unit/{unitId}")
    @PreAuthorize("@authz.canViewVehiclesByUnit(#unitId)")
    public ResponseEntity<List<VehicleDto>> getVehiclesByUnitId(@PathVariable UUID unitId) {
        List<VehicleDto> result = vehicleService.getVehiclesByUnitId(unitId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/active")
    @PreAuthorize("@authz.canViewVehicles()")
    public ResponseEntity<List<VehicleDto>> getActiveVehicles() {
        List<VehicleDto> result = vehicleService.getActiveVehicles();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@authz.canManageVehicleStatus(#id)")
    public ResponseEntity<Void> changeVehicleStatus(@PathVariable UUID id, @RequestParam Boolean active) {
        vehicleService.changeVehicleStatus(id, active);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/kinds")
    public ResponseEntity<VehicleKind[]> getVehicleKinds() {
        return ResponseEntity.ok(VehicleKind.values());
    }
}
