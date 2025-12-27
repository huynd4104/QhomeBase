package com.QhomeBase.iamservice.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Sent email to {} with subject {}", to, subject);
        } catch (MailException ex) {
            log.error("Failed to send email to {}", to, ex);
            throw ex;
        }
    }
    
    @Value("${app.mail.from:no-reply@qhomebase.com}")
    private String defaultFromAddress;

    public void sendStaffAccountCredentials(String recipientEmail, String username, String rawPassword) {
        if (!StringUtils.hasText(recipientEmail)) {
            log.warn("Recipient email is blank; skip sending staff credentials email");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("Your Qhome Base account is ready");
        message.setText(buildStaffAccountBody(username, rawPassword));
        if (StringUtils.hasText(defaultFromAddress)) {
            message.setFrom(defaultFromAddress);
        }

        mailSender.send(message);
        log.info("Sent credentials email to {}", recipientEmail);
    }

    public void sendResidentAccountCredentials(String recipientEmail, String username, String rawPassword, String buildingName) {
        if (!StringUtils.hasText(recipientEmail)) {
            log.warn("Recipient email is blank; skip sending resident credentials email");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("Your Qhome Base resident account");
        message.setText(buildResidentAccountBody(username, recipientEmail, rawPassword, buildingName));
        if (StringUtils.hasText(defaultFromAddress)) {
            message.setFrom(defaultFromAddress);
        }

        mailSender.send(message);
        log.info("Sent resident credentials email to {}", recipientEmail);
    }

    private String buildStaffAccountBody(String username, String rawPassword) {
        String safeUsername = StringUtils.hasText(username) ? username : "there";
        String safePassword = StringUtils.hasText(rawPassword) ? rawPassword : "(password unavailable)";

        return String.format(
                """
                Hello %s,

                Your Qhome Base account has been created successfully.
                - Username: %s
                - Temporary password: %s

                Please sign in and change your password immediately after login.

                Regards,
                Qhome Base Team
                """,
                safeUsername,
                safeUsername,
                safePassword
        );
    }

    private String buildResidentAccountBody(String username, String email, String rawPassword, String buildingName) {
        String safeUsername = StringUtils.hasText(username) ? username : "resident";
        String safeEmail = StringUtils.hasText(email) ? email : (safeUsername + "@qhome.local");
        String safePassword = StringUtils.hasText(rawPassword) ? rawPassword : "(password unavailable)";
        String buildingInfo = StringUtils.hasText(buildingName) ? "\n- Tòa nhà: " + buildingName : "";

        return String.format(
                """
                Xin chào %s,

                Tài khoản cư dân của bạn trên Qhome Base đã được khởi tạo.
                - Tên đăng nhập: %s
                - Email đăng nhập: %s
                - Mật khẩu tạm thời: %s%s

                Vui lòng đăng nhập và đổi mật khẩu ngay sau khi truy cập lần đầu.

                Trân trọng,
                Đội ngũ Qhome Base
                """,
                safeUsername,
                safeUsername,
                safeEmail,
                safePassword,
                buildingInfo
        );
    }
}

