package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.EmployeeDto;
import com.QhomeBase.iamservice.service.EmployeeManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeManagementController {

    private final EmployeeManagementService employeeManagementService;

    @GetMapping
    @PreAuthorize("@authz.canViewAllUsers()")
    public ResponseEntity<List<EmployeeDto>> getAllEmployees() {
        List<EmployeeDto> employees = employeeManagementService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<EmployeeDto> getEmployeeDetails(@PathVariable UUID userId) {
        EmployeeDto employee = employeeManagementService.getEmployeeDetails(userId);
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/role/{roleName}")
    @PreAuthorize("@authz.canViewAllUsers()")
    public ResponseEntity<List<EmployeeDto>> getEmployeesByRole(@PathVariable String roleName) {
        List<EmployeeDto> employees = employeeManagementService.getEmployeesByRole(roleName);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/available")
    @PreAuthorize("@authz.canViewAllUsers()")
    public ResponseEntity<List<EmployeeDto>> getAvailableEmployees() {
        List<EmployeeDto> employees = employeeManagementService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/count")
    @PreAuthorize("@authz.canViewAllUsers()")
    public ResponseEntity<Long> countEmployees() {
        long count = employeeManagementService.countEmployees();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/active")
    @PreAuthorize("@authz.canViewAllUsers()")
    public ResponseEntity<List<EmployeeDto>> getActiveEmployees() {
        List<EmployeeDto> employees = employeeManagementService.getActiveEmployees();
        return ResponseEntity.ok(employees);
    }
}
