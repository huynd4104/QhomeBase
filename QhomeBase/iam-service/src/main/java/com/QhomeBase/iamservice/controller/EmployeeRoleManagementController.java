package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.AvailablePermissionDto;
import com.QhomeBase.iamservice.dto.AvailableRoleDto;
import com.QhomeBase.iamservice.dto.EmployeeRoleDto;
import com.QhomeBase.iamservice.security.UserPrincipal;
import com.QhomeBase.iamservice.service.EmployeeRoleManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employee-roles")
@RequiredArgsConstructor
public class EmployeeRoleManagementController {

    private final EmployeeRoleManagementService employeeRoleManagementService;

    @GetMapping
    @PreAuthorize("@authz.canViewAllUsers()")
    public ResponseEntity<List<EmployeeRoleDto>> getAllEmployees() {
        List<EmployeeRoleDto> employees = employeeRoleManagementService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/employee/{userId}")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<EmployeeRoleDto> getEmployeeDetails(@PathVariable UUID userId) {
        EmployeeRoleDto employee = employeeRoleManagementService.getEmployeeDetails(userId);
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/available-roles")
    @PreAuthorize("@authz.canViewAllRoles()")
    public ResponseEntity<List<AvailableRoleDto>> getAvailableRoles() {
        List<AvailableRoleDto> roles = employeeRoleManagementService.getAvailableRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/available-permissions")
    @PreAuthorize("@authz.canViewAllPermissions()")
    public ResponseEntity<List<AvailablePermissionDto>> getAvailablePermissionsGroupedByService() {
        List<AvailablePermissionDto> permissions = employeeRoleManagementService.getAvailablePermissionsGroupedByService();
        return ResponseEntity.ok(permissions);
    }

    @PostMapping("/assign")
    @PreAuthorize("@authz.canAssignEmployeeRole(null, #userId)")
    public ResponseEntity<String> assignRolesToEmployee(
            @RequestParam UUID userId,
            @RequestBody List<String> roleNames,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        employeeRoleManagementService.assignRolesToEmployee(userId, roleNames, principal.username());
        return ResponseEntity.ok("Roles assigned successfully");
    }

    @PostMapping("/remove")
    @PreAuthorize("@authz.canRemoveEmployeeRole(null, #userId)")
    public ResponseEntity<String> removeRolesFromEmployee(
            @RequestParam UUID userId,
            @RequestBody List<String> roleNames,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        employeeRoleManagementService.removeRolesFromEmployee(userId, roleNames, principal.username());
        return ResponseEntity.ok("Roles removed successfully");
    }

    @GetMapping("/employee/{userId}/permissions")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<List<String>> getEmployeePermissions(@PathVariable UUID userId) {
        List<String> permissions = employeeRoleManagementService.getEmployeePermissions(userId);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/role/{roleName}")
    @PreAuthorize("@authz.canViewAllUsers()")
    public ResponseEntity<List<EmployeeRoleDto>> getEmployeesByRole(@PathVariable String roleName) {
        List<EmployeeRoleDto> employees = employeeRoleManagementService.getEmployeesByRole(roleName);
        return ResponseEntity.ok(employees);
    }
}
