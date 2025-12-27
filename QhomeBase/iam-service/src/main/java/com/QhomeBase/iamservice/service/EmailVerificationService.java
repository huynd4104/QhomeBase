package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.exception.OtpExpiredException;
import com.QhomeBase.iamservice.exception.OtpInvalidException;
import com.QhomeBase.iamservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    // OTP expires in 1 minute
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(1);
    // Max 3 requests per 10 minutes
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(10);
    private static final int OTP_MAX_REQUESTS = 3;
    private static final int OTP_LENGTH = 6;
    // Use same OTP_CHARS as PasswordResetService (alphanumeric)
    private static final String OTP_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    // Store OTPs temporarily (in-memory cache)
    private final ConcurrentHashMap<String, EmailVerificationOtp> otpCache = new ConcurrentHashMap<>();
    // Track OTP requests for rate limiting
    private final ConcurrentHashMap<String, OtpRequestTracker> requestTrackers = new ConcurrentHashMap<>();

    private static class EmailVerificationOtp {
        final String otp;
        final LocalDateTime expiresAt;
        boolean verified;

        EmailVerificationOtp(String otp, LocalDateTime expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
            this.verified = false;
        }

        boolean isExpired(LocalDateTime now) {
            return expiresAt.isBefore(now);
        }
    }

    private static class OtpRequestTracker {
        int requestCount;
        LocalDateTime firstRequestTime;

        OtpRequestTracker() {
            this.requestCount = 0;
            this.firstRequestTime = LocalDateTime.now();
        }

        void addRequest() {
            this.requestCount++;
        }

        boolean isWithinWindow(LocalDateTime now) {
            return firstRequestTime.plus(RATE_LIMIT_WINDOW).isAfter(now);
        }

        void reset() {
            this.requestCount = 0;
            this.firstRequestTime = LocalDateTime.now();
        }
    }

    @Transactional
    public void sendVerificationOtp(String email) {
        String emailKey = email.toLowerCase().trim();

        // 1. Validate email format (basic check)
        if (!isValidEmailFormat(emailKey)) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }

        // 2. Check if email already exists in database
        Optional<com.QhomeBase.iamservice.model.User> existingUser = userRepository.findByEmail(emailKey);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Email này đã được sử dụng. Vui lòng sử dụng email khác.");
        }

        // 3. Check rate limiting (3 requests per 10 minutes)
        OtpRequestTracker tracker = requestTrackers.computeIfAbsent(emailKey, k -> new OtpRequestTracker());
        LocalDateTime now = LocalDateTime.now();

        if (tracker.isWithinWindow(now)) {
            // Still within the 10-minute window
            if (tracker.requestCount >= OTP_MAX_REQUESTS) {
                long minutesRemaining = Duration.between(now, tracker.firstRequestTime.plus(RATE_LIMIT_WINDOW)).toMinutes();
                throw new IllegalStateException(
                    String.format("Bạn đã yêu cầu quá nhiều mã OTP. Vui lòng đợi %d phút nữa.", minutesRemaining)
                );
            }
        } else {
            // Window expired, reset tracker
            tracker.reset();
        }

        // 4. Generate OTP
        String otp = generateOtp();

        // 5. Store OTP in cache with expiry
        otpCache.put(emailKey, new EmailVerificationOtp(otp, now.plus(OTP_EXPIRY)));

        // 6. Track request
        tracker.addRequest();

        // 7. Send email
        emailService.sendEmail(
            email,
            "Xác thực Email - QhomeBase",
            "Mã OTP của bạn là: " + otp + "\n\n" +
            "Mã này có hiệu lực trong " + OTP_EXPIRY.toMinutes() + " phút.\n\n" +
            "Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này."
        );

        log.info("Email verification OTP sent to: {}", email);
    }

    @Transactional
    public boolean verifyOtp(String email, String otp) {
        String emailKey = email.toLowerCase().trim();

        EmailVerificationOtp storedOtp = otpCache.get(emailKey);

        if (storedOtp == null) {
            throw new OtpInvalidException("Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (storedOtp.isExpired(now)) {
            otpCache.remove(emailKey);
            throw new OtpExpiredException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã OTP mới.");
        }

        if (!storedOtp.otp.equalsIgnoreCase(otp)) {
            throw new OtpInvalidException("Mã OTP không đúng. Vui lòng kiểm tra lại và thử lại.");
        }

        // OTP is valid - mark as verified
        storedOtp.verified = true;
        log.info("Email verified successfully: {}", email);
        return true;
    }

    public boolean isEmailVerified(String email) {
        String emailKey = email.toLowerCase().trim();
        EmailVerificationOtp otp = otpCache.get(emailKey);
        return otp != null && otp.verified && !otp.isExpired(LocalDateTime.now());
    }

    private String generateOtp() {
        StringBuilder builder = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            int index = RANDOM.nextInt(OTP_CHARS.length());
            builder.append(OTP_CHARS.charAt(index));
        }
        return builder.toString();
    }

    private boolean isValidEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Basic email format validation
        return email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }
}

