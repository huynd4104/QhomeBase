package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.client.BaseServiceClient;
import com.QhomeBase.iamservice.dto.CreateUserForResidentDto;
import com.QhomeBase.iamservice.dto.ErrorResponseDto;
import com.QhomeBase.iamservice.dto.StaffImportResponse;
import com.QhomeBase.iamservice.dto.UserAccountDto;
import com.QhomeBase.iamservice.dto.UserInfoDto;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.repository.RolePermissionRepository;
import com.QhomeBase.iamservice.repository.UserRepository;
import com.QhomeBase.iamservice.service.UserService;
import com.QhomeBase.iamservice.service.imports.StaffImportService;
import com.QhomeBase.iamservice.service.exports.AccountExportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class UserController {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserService userService;
    private final StaffImportService staffImportService;
    private final AccountExportService accountExportService;
    private final BaseServiceClient baseServiceClient;

    @GetMapping("/{userId}")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<UserInfoDto> getUserInfo(@PathVariable UUID userId) {
        return userService.findUserWithRolesById(userId)
                .map(user -> {
                    List<UserRole> userRoles = user.getRoles();
                    List<String> roleNames = userRoles != null && !userRoles.isEmpty()
                            ? userRoles.stream()
                                    .map(UserRole::getRoleName)
                                    .collect(Collectors.toList())
                            : List.of();

                    Set<String> permissionsSet = new HashSet<>();
                    if (userRoles != null && !userRoles.isEmpty()) {
                        for (UserRole role : userRoles) {
                            List<String> rolePerms = rolePermissionRepository.findPermissionCodesByRole(role.name());
                            permissionsSet.addAll(rolePerms);
                        }
                    }
                    List<String> permissions = new ArrayList<>(permissionsSet);

                    UserInfoDto userInfo = new UserInfoDto(
                            user.getId().toString(),
                            user.getUsername(),
                            user.getEmail(),
                            userService.getPhone(user.getId()),
                            roleNames,
                            permissions);
                    return ResponseEntity.ok(userInfo);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{userId}/status")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<UserStatusResponse> getUserStatus(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    UserStatusResponse status = new UserStatusResponse(
                            user.isActive(),
                            user.getFailedLoginAttempts(),
                            user.isAccountLocked(),
                            user.getLastLogin());
                    return ResponseEntity.ok(status);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    @PreAuthorize("@authz.canUpdateUser(#userId)")
    public ResponseEntity<UserAccountDto> updateUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            if (request.username() != null && !request.username().isBlank()
                    && !request.username().equalsIgnoreCase(user.getUsername())) {
                String trimmedUsername = request.username().trim();
                userRepository.findByUsername(trimmedUsername).ifPresent(existing -> {
                    if (!existing.getId().equals(userId)) {
                        throw new IllegalArgumentException("Username already exists: " + trimmedUsername);
                    }
                });
                user.setUsername(trimmedUsername);
            }

            if (request.email() != null && !request.email().isBlank()
                    && !request.email().equalsIgnoreCase(user.getEmail())) {
                String trimmedEmail = request.email().trim();
                userRepository.findByEmail(trimmedEmail).ifPresent(existing -> {
                    if (!existing.getId().equals(userId)) {
                        throw new IllegalArgumentException("Email already exists: " + trimmedEmail);
                    }
                });
                user.setEmail(trimmedEmail);
            }

            if (request.active() != null) {
                user.setActive(request.active());
            }

            userRepository.save(user);

            // Reload user with roles initialized to avoid lazy loading issues
            User saved = userService.findUserWithRolesById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            return ResponseEntity.ok(userService.mapToUserAccountDto(saved));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update user profile {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating user profile {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @RequestMapping(value = "/{userId}/password", method = { RequestMethod.PATCH, RequestMethod.PUT })
    @PreAuthorize("@authz.canUpdateUser(#userId)")
    public ResponseEntity<Void> updatePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        try {
            userService.updatePassword(userId, request.newPassword());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update password for user {}: {}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating password for user {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/available-staff")
    @PreAuthorize("@authz.canViewAllUsers()")
    public ResponseEntity<List<UserInfoDto>> getAvailableStaff() {
        try {
            List<UserInfoDto> availableStaff = userService.findAvailableStaffWithRoles()
                    .stream()
                    .map(user -> {
                        List<UserRole> userRoles = user.getRoles();
                        List<String> roleNames = userRoles != null && !userRoles.isEmpty()
                                ? userRoles.stream()
                                        .map(UserRole::getRoleName)
                                        .collect(Collectors.toList())
                                : List.of();

                        Set<String> permissionsSet = new HashSet<>();
                        if (userRoles != null && !userRoles.isEmpty()) {
                            for (UserRole role : userRoles) {
                                List<String> rolePerms = rolePermissionRepository
                                        .findPermissionCodesByRole(role.name());
                                permissionsSet.addAll(rolePerms);
                            }
                        }
                        List<String> permissions = new ArrayList<>(permissionsSet);

                        return new UserInfoDto(
                                user.getId().toString(),
                                user.getUsername(),
                                user.getEmail(),
                                userService.getPhone(user.getId()),
                                roleNames,
                                permissions);
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(availableStaff);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/create-for-resident")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT') or hasAuthority('PERM_iam.user.create') or hasAuthority('PERM_base.resident.approve')")
    public ResponseEntity<?> createUserForResident(
            @Valid @RequestBody CreateUserForResidentDto request) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null
                    && auth.getPrincipal() instanceof com.QhomeBase.iamservice.security.UserPrincipal principal) {
                log.info("Creating user for resident: roles={}, perms={}", principal.roles(), principal.perms());
            } else {
                log.warn("No authentication found when creating user for resident");
            }
            User user;

            String buildingName = request.buildingName();
            if (request.autoGenerate()) {
                if (request.username() == null || request.username().isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponseDto("Username is required when autoGenerate is true"));
                }
                if (request.email() == null || request.email().isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponseDto("Email is required when autoGenerate is true"));
                }
                user = userService.createUserWithAutoGeneratedPassword(
                        request.username(),
                        request.email(),
                        request.residentId(),
                        buildingName);
            } else {
                if (request.username() == null || request.username().isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponseDto("Username is required when autoGenerate is false"));
                }
                if (request.email() == null || request.email().isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponseDto("Email is required when autoGenerate is false"));
                }
                // If password is not provided, auto-generate it (allows custom username with
                // auto-generated password)
                if (request.password() == null || request.password().isEmpty()) {
                    user = userService.createUserWithAutoGeneratedPassword(
                            request.username(),
                            request.email(),
                            request.residentId(),
                            buildingName);
                } else {
                    user = userService.createUserForResident(
                            request.username(),
                            request.email(),
                            request.password(),
                            request.residentId(),
                            buildingName);
                }
            }

            UserAccountDto accountDto = userService.mapToUserAccountDto(user);

            return ResponseEntity.status(HttpStatus.CREATED).body(accountDto);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create user for resident: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponseDto(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating user for resident", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Internal server error";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto(errorMessage));
        }
    }

    @GetMapping("/{userId}/account-info")
    @PreAuthorize("@authz.canViewUser(#userId) or hasRole('RESIDENT')")
    @Transactional(readOnly = true)
    public ResponseEntity<UserAccountDto> getUserAccountInfo(@PathVariable UUID userId) {
        return userService.findUserWithRolesById(userId)
                .map(user -> {
                    UserAccountDto accountDto = userService.mapToUserAccountDto(user);
                    return ResponseEntity.ok(accountDto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-username/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT') or hasAuthority('PERM_iam.user.read') or hasAuthority('PERM_base.resident.approve')")
    public ResponseEntity<UserInfoDto> getUserByUsername(@PathVariable String username) {
        return userService.findUserWithRolesByUsername(username)
                .map(user -> {
                    List<UserRole> userRoles = user.getRoles();
                    List<String> roleNames = userRoles != null && !userRoles.isEmpty()
                            ? userRoles.stream()
                                    .map(UserRole::getRoleName)
                                    .collect(Collectors.toList())
                            : List.of();

                    Set<String> permissionsSet = new HashSet<>();
                    if (userRoles != null && !userRoles.isEmpty()) {
                        for (UserRole role : userRoles) {
                            List<String> rolePerms = rolePermissionRepository.findPermissionCodesByRole(role.name());
                            permissionsSet.addAll(rolePerms);
                        }
                    }
                    List<String> permissions = new ArrayList<>(permissionsSet);

                    UserInfoDto userInfo = new UserInfoDto(
                            user.getId().toString(),
                            user.getUsername(),
                            user.getEmail(),
                            userService.getPhone(user.getId()),
                            roleNames,
                            permissions);
                    return ResponseEntity.ok(userInfo);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-email/{email}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT') or hasAuthority('PERM_iam.user.read') or hasAuthority('PERM_base.resident.approve')")
    public ResponseEntity<UserInfoDto> getUserByEmail(@PathVariable String email) {
        return userService.findUserWithRolesByEmail(email)
                .map(user -> {
                    List<UserRole> userRoles = user.getRoles();
                    List<String> roleNames = userRoles != null && !userRoles.isEmpty()
                            ? userRoles.stream()
                                    .map(UserRole::getRoleName)
                                    .collect(Collectors.toList())
                            : List.of();

                    Set<String> permissionsSet = new HashSet<>();
                    if (userRoles != null && !userRoles.isEmpty()) {
                        for (UserRole role : userRoles) {
                            List<String> rolePerms = rolePermissionRepository.findPermissionCodesByRole(role.name());
                            permissionsSet.addAll(rolePerms);
                        }
                    }
                    List<String> permissions = new ArrayList<>(permissionsSet);

                    UserInfoDto userInfo = new UserInfoDto(
                            user.getId().toString(),
                            user.getUsername(),
                            user.getEmail(),
                            userService.getPhone(user.getId()),
                            roleNames,
                            permissions);
                    return ResponseEntity.ok(userInfo);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/staff")
    @PreAuthorize("@authz.canCreateUser()")
    public ResponseEntity<UserAccountDto> createStaffAccount(@Valid @RequestBody CreateStaffRequest request) {
        try {
            List<UserRole> roles = parseStaffRoles(request.roles());
            User user = userService.createStaffAccount(
                    request.username(),
                    request.email(),
                    roles,
                    request.active() == null || request.active(),
                    request.fullName(),
                    request.phone(),
                    request.nationalId(),
                    request.address());
            baseServiceClient.syncStaffResident(user.getId(), request.username(), request.email(), request.phone());
            return ResponseEntity.status(HttpStatus.CREATED).body(userService.mapToUserAccountDto(user));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create staff account: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating staff account", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/staff")
    @PreAuthorize("@authz.canViewAllUsers()")
    public ResponseEntity<List<UserAccountDto>> listStaffAccounts() {

        List<UserAccountDto> result = userService.findStaffAccountDtos();

        // 🔥 LOG QUYẾT ĐỊNH
        log.error("CONTROLLER -> list class = {}", result.getClass());
        log.error("CONTROLLER -> element class = {}",
                result.isEmpty() ? "EMPTY" : result.get(0).getClass());
        log.error("CONTROLLER -> element = {}",
                result.isEmpty() ? "EMPTY" : result.get(0));

        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/staff/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.canCreateUser()")
    public ResponseEntity<StaffImportResponse> importStaffAccounts(@RequestParam("file") MultipartFile file) {
        try {
            StaffImportResponse response = staffImportService.importStaffAccounts(file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to import staff accounts: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error when importing staff accounts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/staff/import/template")
    @PreAuthorize("@authz.canViewAllUsers()")
    public ResponseEntity<byte[]> downloadStaffImportTemplate() {
        byte[] data = staffImportService.generateTemplateWorkbook();
        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"staff_import_template.xlsx\"")
                .body(data);
    }

    @GetMapping("/export")
    @PreAuthorize("@authz.canViewAllUsers()")
    public ResponseEntity<byte[]> exportAccounts() {
        try {
            byte[] data = accountExportService.exportAccountsToExcel();
            return ResponseEntity.ok()
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"accounts_export.xlsx\"")
                    .body(data);
        } catch (Exception e) {
            log.error("Failed to export accounts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/residents")
    @PreAuthorize("@authz.canViewAllUsers()")
    public ResponseEntity<List<UserAccountDto>> listResidentAccounts(
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) Integer floor) {
        List<User> users;
        if (buildingId != null) {
            List<UUID> userIds = baseServiceClient.getResidentUserIdsByBuilding(buildingId, floor);
            users = userService.findResidentAccountsByUserIds(userIds);
        } else {
            users = userService.findResidentAccounts();
        }
        List<UserAccountDto> residents = users.stream()
                .map(userService::mapToUserAccountDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(residents);
    }

    @GetMapping("/staff/{userId}")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<UserAccountDto> getStaffAccount(@PathVariable UUID userId) {
        return userService.findStaffWithRolesById(userId)
                .map(userService::mapToUserAccountDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/staff/{userId}")
    @PreAuthorize("@authz.canUpdateUser(#userId)")
    public ResponseEntity<UserAccountDto> updateStaffAccount(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateStaffRequest request) {
        try {
            List<UserRole> roles = request.roles() != null ? parseStaffRoles(request.roles()) : null;
            User updated = userService.updateStaffAccount(
                    userId,
                    request.username(),
                    request.email(),
                    request.active(),
                    request.newPassword(),
                    roles,
                    request.fullName(),
                    request.phone(),
                    request.nationalId(),
                    request.address());
            baseServiceClient.syncStaffResident(updated.getId(), updated.getUsername(), updated.getEmail(),
                    request.phone());
            return ResponseEntity.ok(userService.mapToUserAccountDto(updated));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update staff account {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating staff account {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@authz.canDeleteUser(#userId)")
    public ResponseEntity<Void> deleteUserAccount(@PathVariable UUID userId) {
        try {
            User user = userService.findUserWithRolesById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // Check if user is staff or resident
            boolean isStaff = user.getRoles().stream().anyMatch(role -> role == UserRole.ADMIN ||
                    role == UserRole.ACCOUNTANT ||
                    role == UserRole.TECHNICIAN ||
                    role == UserRole.SUPPORTER);

            if (isStaff) {
                userService.deleteStaffAccount(userId);
            } else {
                // For resident accounts, check if inactive
                if (user.isActive()) {
                    throw new IllegalArgumentException(
                            "Cannot delete active resident account. Please deactivate it first.");
                }
                userRepository.delete(user);
                log.info("Deleted resident user account {}", userId);
            }

            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete account {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error deleting account {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/staff/{userId}")
    @PreAuthorize("@authz.canDeleteUser(#userId)")
    public ResponseEntity<Void> deleteStaffAccount(@PathVariable UUID userId) {
        try {
            userService.deleteStaffAccount(userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete staff account {}: {}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting staff account {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    public record UserStatusResponse(
            boolean active,
            int failedLoginAttempts,
            boolean accountLocked,
            java.time.LocalDateTime lastLogin) {
    }

    public record UpdateUserProfileRequest(
            String username,
            String email,
            Boolean active) {
    }

    public record UpdatePasswordRequest(
            @NotBlank(message = "New password is required") @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters") @Pattern(regexp = "^(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", message = "Password must be at least 8 characters and contain at least one special character") String newPassword) {
    }

    public record CreateStaffRequest(
            @NotBlank(message = "Username is required") @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") String username,
            @NotBlank(message = "Email is required") @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.com$", message = "Email phải có đuôi .com. Ví dụ: user@example.com") String email,
            @NotEmpty(message = "Staff roles are required") List<@NotBlank(message = "Role value cannot be blank") String> roles,
            Boolean active,
            @NotBlank(message = "Full name is required") String fullName,
            String phone,
            String nationalId,
            String address) {
    }

    public record UpdateStaffRequest(
            @NotBlank(message = "Username is required") @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") String username,
            @NotBlank(message = "Email is required") @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.com$", message = "Email phải có đuôi .com. Ví dụ: user@example.com") String email,
            Boolean active,
            List<@NotBlank(message = "Role value cannot be blank") String> roles,
            @Size(min = 8, message = "New password must be at least 8 characters") String newPassword,
            String fullName,
            String phone,
            String nationalId,
            String address) {
    }

    private List<UserRole> parseStaffRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Staff roles are required");
        }
        return roles.stream()
                .map(roleName -> {
                    try {
                        return UserRole.valueOf(roleName.trim().toUpperCase());
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid role: " + roleName);
                    }
                })
                .distinct()
                .collect(Collectors.toList());
    }
}
