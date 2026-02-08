package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.dto.LoginRequestDto;
import com.QhomeBase.iamservice.dto.LoginResponseDto;
import com.QhomeBase.iamservice.dto.UserInfoDto;
import com.QhomeBase.iamservice.model.Permission;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.repository.PermissionRepository;
import com.QhomeBase.iamservice.repository.UserRepository;
import com.QhomeBase.iamservice.repository.RolePermissionRepository;
import com.QhomeBase.iamservice.security.JwtIssuer;
import com.QhomeBase.iamservice.security.JwtVerifier;
import com.QhomeBase.iamservice.service.token.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;
    private final JwtVerifier jwtVerifier;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final TokenBlacklistService tokenBlacklistService;
    public void putIntoBlackList(String accessToken) {
        Claims claims = jwtVerifier.verify(accessToken);

        String jti = claims.getId();

        long ttlSeconds = claims.getExpiration().toInstant().getEpochSecond()
                - Instant.now().getEpochSecond();

        if (ttlSeconds > 0) {
            tokenBlacklistService.blacklist(jti, ttlSeconds);
        }
    }
    @Transactional
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        // Tìm kiếm người dùng bằng username, email hoặc số điện thoại
        User user = userRepository.findByLoginIdentifier(loginRequestDto.username())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + loginRequestDto.username()));

        log.debug("Found user id={} active={} locked={} failedAttempts={} for username={}",
                user.getId(), user.isActive(), user.isAccountLocked(), user.getFailedLoginAttempts(), loginRequestDto.username());

        // Kiểm tra mật khẩu (hỗ trợ cả so sánh hash và so sánh trực tiếp cho môi trường dev/test)
        boolean passwordMatches = passwordEncoder.matches(loginRequestDto.password(), user.getPasswordHash())
                || loginRequestDto.password().equals(user.getPasswordHash());

        if (!passwordMatches) {
            handleFailedLogin(user);
            log.warn("Password mismatch for user={} (failedAttempts={})", user.getUsername(), user.getFailedLoginAttempts());
            throw new IllegalArgumentException("Password mismatch for user: " + loginRequestDto.username());
        }

        // Kiểm tra trạng thái kích hoạt của tài khoản
        if (!user.isActive()) {
            log.warn("User account disabled: {}", user.getUsername());
            throw new IllegalArgumentException("User account is disabled: " + user.getUsername());
        }

        // Kiểm tra tài khoản có bị khóa không
        if (user.isAccountLocked()) {
            log.warn("User account locked: {}", user.getUsername());
            throw new IllegalArgumentException("User account is locked: " + user.getUsername());
        }

        // Lấy danh sách vai trò
        List<UserRole> userRoles = user.getRoles();
        if (userRoles == null || userRoles.isEmpty()) {
            log.warn("User has no roles: {}", user.getUsername());
            throw new IllegalArgumentException("User has no roles assigned: " + user.getUsername());
        }

        List<String> roleNames = userRoles.stream()
                .map(UserRole::getRoleName)
                .collect(Collectors.toList());

        log.debug("User {} has roles: {}", user.getUsername(), roleNames);

        // Reset số lần đăng nhập sai và cập nhật thời gian đăng nhập cuối
        user.resetFailedLoginAttempts();
        user.updateLastLogin();
        userRepository.save(user);

        // Lấy toàn bộ quyền hạn dựa trên các vai trò của user
        List<String> userPermissions = getUserPermissions(userRoles);
        log.debug("User {} has permissions: {}", user.getUsername(), userPermissions.size());

        String jti = UUID.randomUUID().toString();
        // Phát hành Access Token (JWT)
        String accessToken = jwtIssuer.issueForService(
                user.getId(),
                user.getUsername(),
                jti,
                roleNames,
                userPermissions,
                "base-service,finance-service,customer-service,asset-maintenance-service,iam-service"
        ).trim();

        return new LoginResponseDto(
                accessToken,
                "Bearer",
                3600L,
                java.time.Instant.now().plusSeconds(3600),
                new UserInfoDto(
                        user.getId().toString(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getPhone(),
                        roleNames,
                        userPermissions
                )
        );
    }

    private List<String> getUserPermissions(List<UserRole> userRoles) {
        // Nếu là ADMIN thì trả về tất cả các quyền có trong hệ thống
        if (userRoles.contains(UserRole.ADMIN)) {
            return getAllPermissions();
        }

        // Tổng hợp quyền từ tất cả các role của user
        Set<String> permissions = new HashSet<>();
        for (UserRole role : userRoles) {
            List<String> rolePerms = rolePermissionRepository.findPermissionCodesByRole(role.name());
            permissions.addAll(rolePerms);
        }

        return new ArrayList<>(permissions);
    }

    private List<String> getAllPermissions() {
        return permissionRepository.findAll()
                .stream()
                .map(Permission::getCode)
                .collect(Collectors.toList());
    }

    private void handleFailedLogin(User user) {
        user.incrementFailedLoginAttempts();
        userRepository.save(user);
    }

    public void logout(String accessToken) {
        putIntoBlackList(accessToken);
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

}