package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.dto.EmployeeDto;
import com.QhomeBase.iamservice.dto.EmployeePermissionStatus;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.repository.RolePermissionRepository;
import com.QhomeBase.iamservice.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeManagementService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<EmployeeDto> getAllEmployees() {
        List<User> users = userRepository.findAll();
        users.forEach(user -> Hibernate.initialize(user.getRoles()));
        return users.stream()
                .map(this::mapToEmployeeDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeDetails(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Hibernate.initialize(user.getRoles());
        return mapToEmployeeDto(user);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> getEmployeesByRole(String roleName) {
        try {
            UserRole role = UserRole.valueOf(roleName.toUpperCase());
           
            String roleCode = role.name();
            List<User> users = userRepository.findByRole(roleCode);
            users.forEach(user -> Hibernate.initialize(user.getRoles()));
            return users.stream()
                    .map(this::mapToEmployeeDto)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleName);
        }
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> getActiveEmployees() {
        List<User> users = userRepository.findAll();
        users.forEach(user -> Hibernate.initialize(user.getRoles()));
        return users.stream()
                .filter(User::isActive)
                .map(this::mapToEmployeeDto)
                .collect(Collectors.toList());
    }

    public long countEmployees() {
        return userRepository.count();
    }

    private EmployeeDto mapToEmployeeDto(User user) {
        List<UserRole> userRoles = user.getRoles();
        String roleInfo = userRoles != null && !userRoles.isEmpty() 
                ? userRoles.stream()
                    .map(UserRole::getRoleName)
                    .collect(Collectors.joining(", "))
                : "No roles";
        
        EmployeePermissionStatus permissionStatus = EmployeePermissionStatus.STANDARD;
        int grantedOverrides = 0;
        int deniedOverrides = 0;
        boolean hasTemporaryPermissions = false;
        
        return EmployeeDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(null)
                .email(user.getEmail())
                .phoneNumber(null)
                .department(null)
                .position(roleInfo)
                .tenantId(null)
                .tenantName(null)
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .permissionStatus(permissionStatus)
                .grantedOverrides(grantedOverrides)
                .deniedOverrides(deniedOverrides)
                .totalOverrides(0)
                .hasTemporaryPermissions(hasTemporaryPermissions)
                .lastPermissionChange(null)
                .build();
    }
}
