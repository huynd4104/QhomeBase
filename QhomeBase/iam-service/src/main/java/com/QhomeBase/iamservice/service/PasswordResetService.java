package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.exception.OtpExpiredException;
import com.QhomeBase.iamservice.exception.OtpInvalidException;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final Duration OTP_EXPIRY = Duration.ofMinutes(1);
    private static final int OTP_MAX_REQUESTS = 3;
    private static final int OTP_LENGTH = 6;
    private static final String OTP_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    private final Map<String, Integer> otpRequestCount = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpRequestTimes = new ConcurrentHashMap<>();
    private final Map<String, VerifiedOtp> verifiedOtps = new ConcurrentHashMap<>();
    
    private static class VerifiedOtp {
        final String otp;
        final LocalDateTime verifiedAt;
        
        VerifiedOtp(String otp, LocalDateTime verifiedAt) {
            this.otp = otp;
            this.verifiedAt = verifiedAt;
        }
        
        boolean isValid(LocalDateTime now, Duration expiry) {
            return verifiedAt.plus(expiry).isAfter(now);
        }
    }

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existing email {}", email);
            trackOtpRequest(email);
            throw new IllegalArgumentException("Email không tồn tại trong hệ thống. Vui lòng kiểm tra lại email của bạn.");
        }

        if (!canRequestOtp(email)) {
            throw new IllegalStateException("Too many OTP requests. Try again later.");
        }

        verifiedOtps.remove(email.toLowerCase());

        User user = userOpt.get();
        String otp = generateOtp();
        user.setResetOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plus(OTP_EXPIRY));
        userRepository.save(user);

        emailService.sendEmail(
                user.getEmail(),
                "Password Reset OTP",
                "Your OTP is: " + otp + " (valid for " + OTP_EXPIRY.toMinutes() + " minutes)"
        );

        log.info("OTP generated for user {}", user.getEmail());
        trackOtpRequest(email);
    }

    @Transactional
    public void verifyOtp(String email, String otp) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new OtpInvalidException("Mã OTP không hợp lệ. Vui lòng thử lại.");
        }

        User user = userOpt.get();
        
        // Check if OTP exists
        if (user.getResetOtp() == null || user.getOtpExpiry() == null) {
            throw new OtpInvalidException("Mã OTP không hợp lệ. Vui lòng thử lại.");
        }
        
        // Check if OTP has expired
        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new OtpExpiredException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã OTP mới.");
        }
        
        // Check if OTP matches
        if (!user.getResetOtp().equalsIgnoreCase(otp)) {
            throw new OtpInvalidException("Mã OTP không đúng. Vui lòng kiểm tra lại và thử lại.");
        }
        
        // OTP is valid - mark it as verified and invalidate the OTP in database
        verifiedOtps.put(email.toLowerCase(), new VerifiedOtp(otp, LocalDateTime.now()));
        user.setResetOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
        
        log.info("OTP verified and invalidated for user {}", email);
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new OtpInvalidException("Mã OTP không hợp lệ. Vui lòng thử lại."));

        String emailKey = email.toLowerCase();
        VerifiedOtp verifiedOtp = verifiedOtps.get(emailKey);
        
        if (verifiedOtp == null || !verifiedOtp.isValid(LocalDateTime.now(), OTP_EXPIRY)) {
            // OTP was not verified or verification has expired
            if (verifiedOtp != null) {
                verifiedOtps.remove(emailKey);
                throw new OtpExpiredException("Phiên xác thực OTP đã hết hạn. Vui lòng xác thực lại mã OTP.");
            }
            // If OTP was not verified, check if it's still valid in database
            // This allows direct reset password flow (though not recommended)
            try {
                validateOtpForReset(user, otp);
                // If validation passes, mark as verified for consistency
                verifiedOtps.put(emailKey, new VerifiedOtp(otp, LocalDateTime.now()));
                verifiedOtp = verifiedOtps.get(emailKey);
            } catch (OtpExpiredException | OtpInvalidException e) {
                throw e;
            }
        }

        // Verify OTP matches the verified OTP
        if (!verifiedOtp.otp.equalsIgnoreCase(otp)) {
            throw new OtpInvalidException("Mã OTP không đúng. Vui lòng kiểm tra lại và thử lại.");
        }
        
        // Remove verified OTP from cache after successful validation
        verifiedOtps.remove(emailKey);

        if (!isStrongPassword(newPassword)) {
            throw new IllegalArgumentException("Password must be at least 8 characters and contain at least one special character");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from old password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        log.info("Password reset successful for user {}", email);
    }

    private boolean canRequestOtp(String email) {
        otpRequestCount.putIfAbsent(email, 0);
        otpRequestTimes.putIfAbsent(email, LocalDateTime.MIN);

        LocalDateTime lastRequest = otpRequestTimes.get(email);
        boolean withinWindow = lastRequest.plus(OTP_EXPIRY).isAfter(LocalDateTime.now());

        if (!withinWindow) {
            otpRequestCount.put(email, 0);
            return true;
        }

        return otpRequestCount.get(email) < OTP_MAX_REQUESTS;
    }

    private void trackOtpRequest(String email) {
        otpRequestTimes.put(email, LocalDateTime.now());
        otpRequestCount.merge(email, 1, Integer::sum);
    }

    private boolean isOtpValid(User user, String otp) {
        if (user.getResetOtp() == null || user.getOtpExpiry() == null) {
            return false;
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        return user.getResetOtp().equalsIgnoreCase(otp);
    }
    
    private void validateOtpForReset(User user, String otp) {
        if (user.getResetOtp() == null || user.getOtpExpiry() == null) {
            throw new OtpInvalidException("Mã OTP không hợp lệ. Vui lòng thử lại.");
        }
        
        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new OtpExpiredException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã OTP mới.");
        }
        
        if (!user.getResetOtp().equalsIgnoreCase(otp)) {
            throw new OtpInvalidException("Mã OTP không đúng. Vui lòng kiểm tra lại và thử lại.");
        }
    }

    private boolean isStrongPassword(String password) {
        return STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    private String generateOtp() {
        StringBuilder builder = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            int index = RANDOM.nextInt(OTP_CHARS.length());
            builder.append(OTP_CHARS.charAt(index));
        }
        return builder.toString();
    }
}

