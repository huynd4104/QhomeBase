package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.ErrorResponseDto;
import com.QhomeBase.iamservice.dto.LoginRequestDto;
import com.QhomeBase.iamservice.dto.LoginResponseDto;
import com.QhomeBase.iamservice.dto.OtpVerificationRequestDto;
import com.QhomeBase.iamservice.dto.PasswordResetConfirmRequestDto;
import com.QhomeBase.iamservice.dto.PasswordResetRequestDto;
import com.QhomeBase.iamservice.exception.OtpExpiredException;
import com.QhomeBase.iamservice.exception.OtpInvalidException;
import com.QhomeBase.iamservice.service.AuthService;
import com.QhomeBase.iamservice.service.EmailVerificationService;
import com.QhomeBase.iamservice.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        try {
            LoginResponseDto response = authService.login(loginRequest);
            log.info("Login success for user={}", loginRequest.username());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Login failed for user={} reason={}", loginRequest.username(), e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponseDto(e.getMessage()));
        }
    }

    @PostMapping("/request-reset")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto request) {
        try {
            passwordResetService.requestPasswordReset(request.email());
            return ResponseEntity.ok(Map.of("message", "Mã OTP đã được gửi đến email của bạn"));
        } catch (IllegalArgumentException e) {
            log.warn("Password reset request failed for email={} reason={}", request.email(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException ex) {
            log.warn("OTP request throttled for email={} reason={}", request.email(), ex.getMessage());
            return ResponseEntity.status(429).body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerificationRequestDto request) {
        try {
            passwordResetService.verifyOtp(request.email(), request.otp());
            return ResponseEntity.ok(Map.of("message", "OTP đã được xác thực thành công"));
        } catch (OtpExpiredException e) {
            log.warn("OTP expired for email={}", request.email());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (OtpInvalidException e) {
            log.warn("Invalid OTP for email={}", request.email());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error verifying OTP for email={}", request.email(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP không hợp lệ. Vui lòng thử lại."));
        }
    }

    @PostMapping("/confirm-reset")
    public ResponseEntity<?> confirmReset(@Valid @RequestBody PasswordResetConfirmRequestDto request) {
        try {
            passwordResetService.resetPassword(request.email(), request.otp(), request.newPassword());
            return ResponseEntity.ok(Map.of("message", "Mật khẩu đã được cập nhật thành công"));
        } catch (OtpExpiredException e) {
            log.warn("OTP expired for password reset email={}", request.email());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (OtpInvalidException e) {
            log.warn("Invalid OTP for password reset email={}", request.email());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException ex) {
            log.warn("Password reset failed for email={} reason={}", request.email(), ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error resetting password for email={}", request.email(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Có lỗi xảy ra. Vui lòng thử lại."));
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("@authz.canLogout()")
    public ResponseEntity<Void> logout(@RequestHeader("X-User-ID") UUID userId) {
        try {
            authService.logout(userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/refresh")
    @PreAuthorize("@authz.canRefreshToken()")
    public ResponseEntity<Void> refreshToken(
            @RequestHeader("X-User-ID") UUID userId) {
        try {
            authService.refreshToken(userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/request-email-verification")
    public ResponseEntity<?> requestEmailVerification(@Valid @RequestBody PasswordResetRequestDto request) {
        try {
            emailVerificationService.sendVerificationOtp(request.email());
            return ResponseEntity.ok(Map.of("message", "Mã OTP đã được gửi đến email của bạn"));
        } catch (IllegalArgumentException e) {
            log.warn("Email verification request failed for email={} reason={}", request.email(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException ex) {
            log.warn("Email verification OTP request throttled for email={} reason={}", request.email(), ex.getMessage());
            return ResponseEntity.status(429).body(Map.of("message", ex.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error requesting email verification for email={}", request.email(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Có lỗi xảy ra. Vui lòng thử lại."));
        }
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<?> verifyEmailOtp(@Valid @RequestBody OtpVerificationRequestDto request) {
        try {
            boolean verified = emailVerificationService.verifyOtp(request.email(), request.otp());
            return ResponseEntity.ok(Map.of(
                "message", "Email đã được xác thực thành công",
                "verified", verified
            ));
        } catch (OtpExpiredException e) {
            log.warn("Email verification OTP expired for email={}", request.email());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (OtpInvalidException e) {
            log.warn("Invalid email verification OTP for email={}", request.email());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error verifying email OTP for email={}", request.email(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP không hợp lệ. Vui lòng thử lại."));
        }
    }

    @GetMapping("/check-email-verified/{email}")
    public ResponseEntity<?> checkEmailVerified(@PathVariable String email) {
        try {
            boolean verified = emailVerificationService.isEmailVerified(email);
            return ResponseEntity.ok(Map.of("verified", verified));
        } catch (Exception e) {
            log.error("Unexpected error checking email verification for email={}", email, e);
            return ResponseEntity.badRequest().body(Map.of("verified", false));
        }
    }

    @GetMapping("/check-email-exists/{email}")
    public ResponseEntity<?> checkEmailExists(@PathVariable String email) {
        try {
            boolean exists = authService.emailExists(email);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            log.error("Unexpected error checking email existence for email={}", email, e);
            return ResponseEntity.badRequest().body(Map.of("exists", false));
        }
    }
}

