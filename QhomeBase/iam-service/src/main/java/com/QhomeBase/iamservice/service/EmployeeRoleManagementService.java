package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.dto.AvailablePermissionDto;
import com.QhomeBase.iamservice.dto.AvailableRoleDto;
import com.QhomeBase.iamservice.dto.EmployeeRoleDto;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.repository.PermissionRepository;
import com.QhomeBase.iamservice.repository.RolePermissionRepository;
import com.QhomeBase.iamservice.repository.RolesRepository;
import com.QhomeBase.iamservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeRoleManagementService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RolesRepository rolesRepository;

    public List<EmployeeRoleDto> getAllEmployees() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToEmployeeRoleDto)
                .collect(Collectors.toList());
    }

    public EmployeeRoleDto getEmployeeDetails(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return mapToEmployeeRoleDto(user);
    }

    public List<EmployeeRoleDto> getEmployeesByRole(String roleName) {
        try {
            UserRole role = UserRole.valueOf(roleName.toUpperCase());
        
            String roleCode = role.name();
            return userRepository.findByRole(roleCode)
                    .stream()
                    .map(this::mapToEmployeeRoleDto)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleName);
        }
    }

    public List<AvailableRoleDto> getAvailableRoles() {
        return getGlobalRoles();
    }

    public List<AvailablePermissionDto> getAvailablePermissionsGroupedByService() {
        return permissionRepository.findAll().stream()
                .collect(Collectors.groupingBy(permission -> {
                    String code = permission.getCode();
                    int dotIndex = code.indexOf('.');
                    return dotIndex > 0 ? code.substring(0, dotIndex) : "general";
                }))
                .entrySet().stream()
                .map(entry -> {
                    String servicePrefix = entry.getKey();
                    String serviceName = getServiceDisplayName(servicePrefix);
                    List<String> permissions = entry.getValue().stream()
                            .map(permission -> permission.getCode())
                            .sorted()
                            .collect(Collectors.toList());
                    
                    return AvailablePermissionDto.builder()
                            .servicePrefix(servicePrefix)
                            .serviceName(serviceName)
                            .permissions(permissions)
                            .build();
                })
                .sorted((a, b) -> a.getServicePrefix().compareTo(b.getServicePrefix()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignRolesToEmployee(UUID userId, List<String> roleNames, String assignedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        for (String roleName : roleNames) {
            try {
                UserRole role = UserRole.valueOf(roleName.toUpperCase());
                if (!user.hasRole(role)) {
                    user.addRole(role);
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + roleName + ". Valid roles are: admin, accountant, technician, supporter, unit_owner, resident");
            }
        }
        
        userRepository.save(user);
    }

    @Transactional
    public void removeRolesFromEmployee(UUID userId, List<String> roleNames, String removedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        for (String roleName : roleNames) {
            try {
                UserRole role = UserRole.valueOf(roleName.toUpperCase());
                user.removeRole(role);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + roleName);
            }
        }
        
        userRepository.save(user);
    }

    public List<String> getEmployeePermissions(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<UserRole> userRoles = user.getRoles();
        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }
        
        return userRoles.stream()
                .map(role -> role.name())
                .flatMap(roleName -> rolePermissionRepository.findPermissionCodesByRole(roleName).stream())
                .distinct()
                .collect(Collectors.toList());
    }

    private EmployeeRoleDto mapToEmployeeRoleDto(User user) {
        List<UserRole> userRoles = user.getRoles();
        
        List<EmployeeRoleDto.RoleAssignmentDto> assignedRoles = userRoles != null && !userRoles.isEmpty()
                ? userRoles.stream()
                    .map(role -> EmployeeRoleDto.RoleAssignmentDto.builder()
                            .roleName(role.getRoleName())
                            .roleDescription(role.getDescription())
                            .assignedAt(Instant.now())
                            .assignedBy("System")
                            .isActive(true)
                            .build())
                    .collect(Collectors.toList())
                : List.of();

        List<String> permissions = getEmployeePermissions(user.getId());
        
        return EmployeeRoleDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(null)
                .email(user.getEmail())
                .phoneNumber(null)
                .department(null)
                .position(null)
                .tenantId(null)
                .tenantName(null)
                .assignedRoles(assignedRoles)
                .allPermissions(permissions)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .build();
    }

    private String getServiceDisplayName(String servicePrefix) {
        return switch (servicePrefix) {
            case "base" -> "Base Service";
            case "iam" -> "IAM Service";
            case "maintenance" -> "Maintenance Service";
            case "finance" -> "Finance Service";
            case "customer" -> "Customer Service";
            case "document" -> "Document Service";
            case "report" -> "Report Service";
            case "system" -> "System Service";
            default -> servicePrefix.toUpperCase() + " Service";
        };
    }

    private List<AvailableRoleDto> getGlobalRoles() {
        List<Object[]> roleData = rolesRepository.findGlobalRoles();
        
        return roleData.stream()
                .map(row -> {
                    String roleName = (String) row[0];
                    String description = (String) row[1];
                    
                    List<String> permissions = rolePermissionRepository.findPermissionCodesByRole(roleName);
                    
                    String category = determineRoleCategory(roleName);
                    
                    return AvailableRoleDto.builder()
                            .roleName(roleName)
                            .description(description != null ? description : getDefaultDescription(roleName))
                            .permissions(permissions)
                            .isAssignable(true)
                            .category(category)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    private String determineRoleCategory(String roleName) {
        return switch (roleName.toLowerCase()) {
            case "admin" -> "ADMIN";
            case "unit_owner" -> "MANAGER";
            case "accountant", "technician", "supporter" -> "STAFF";
            case "resident" -> "RESIDENT";
            default -> "STAFF";
        };
    }
    
    private String getDefaultDescription(String roleName) {
        return switch (roleName.toLowerCase()) {
            case "admin" -> "System Administrator";
            case "accountant" -> "Accountant";
            case "technician" -> "Technician";
            case "supporter" -> "Supporter";
            case "unit_owner" -> "Unit Owner";
            case "resident" -> "Resident";
            default -> roleName + " Role";
        };
    }
}
