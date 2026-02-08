package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.LoginResponseDto;
import com.QhomeBase.iamservice.dto.UserInfoDto;
import com.QhomeBase.iamservice.controller.AuthController;
import com.QhomeBase.iamservice.exception.OtpExpiredException;
import com.QhomeBase.iamservice.exception.OtpInvalidException;
import com.QhomeBase.iamservice.security.AuthzService;
import com.QhomeBase.iamservice.service.AuthService;
import com.QhomeBase.iamservice.service.EmailVerificationService;
import com.QhomeBase.iamservice.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private PasswordResetService passwordResetService;

        @MockitoBean
        private EmailVerificationService emailVerificationService;

        @MockitoBean(name = "authz")
        private AuthzService authzService;

        @MockitoBean
        private com.QhomeBase.iamservice.security.JwtAuthFilter jwtAuthFilter;

        private String validEmail;
        private String validOtp;
        private String strongPassword;

        @BeforeEach
        void setUp() {
                // Arrange
                validEmail = "user@example.com";
                validOtp = "ABC123";
                strongPassword = "Str0ng@Pass";

                Mockito.when(authzService.canLogout()).thenReturn(true);
                Mockito.when(authzService.canRefreshToken()).thenReturn(true);
        }

        @Test
        @DisplayName("shouldReturnLoginResponse_whenCredentialsValid")
        void shouldReturnLoginResponse_whenCredentialsValid() throws Exception {
                // Arrange
                var now = Instant.now();
                var userInfo = new UserInfoDto(
                        UUID.randomUUID().toString(),
                        "john",
                        "john@example.com",
                        "0123456789",
                        java.util.List.of("ADMIN"), // = List.of("ADMIN")
                        java.util.List.of("iam.user.read")
                );

                var loginResponse = new LoginResponseDto("token123", "Bearer", 3600L, now.plusSeconds(3600), userInfo);
                Mockito.when(authService.login(any())).thenReturn(loginResponse);
                String body = "{" +
                                "\"username\":\"john\"," +
                                "\"password\":\"password123\"" +
                                "}";

                // Act
                var result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken", is("token123")))
                                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                                .andExpect(jsonPath("$.expiresIn", is(3600)))
                                .andExpect(jsonPath("$.userInfo.username", is("john")))
                                .andReturn();

                ArgumentCaptor<com.QhomeBase.iamservice.dto.LoginRequestDto> captor = ArgumentCaptor
                                .forClass(com.QhomeBase.iamservice.dto.LoginRequestDto.class);
                verify(authService, times(1)).login(captor.capture());
                org.junit.jupiter.api.Assertions.assertEquals("john", captor.getValue().username());
                org.junit.jupiter.api.Assertions.assertEquals("password123", captor.getValue().password());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWithError_whenLoginThrowsIllegalArgument")
        void shouldReturnBadRequestWithError_whenLoginThrowsIllegalArgument() throws Exception {
                // Arrange
                Mockito.when(authService.login(any())).thenThrow(new IllegalArgumentException("User not found: john"));
                String body = "{" +
                                "\"username\":\"john\"," +
                                "\"password\":\"password123\"" +
                                "}";

                // Act
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("User not found: john")));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenLoginValidationFails")
        void shouldReturnBadRequest_whenLoginValidationFails() throws Exception {
                // Arrange
                String body = "{" +
                                "\"username\":\"\"," +
                                "\"password\":\"\"" +
                                "}";

                // Act
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldSendOtp_whenRequestPasswordResetEmailValid")
        void shouldSendOtp_whenRequestPasswordResetEmailValid() throws Exception {
                // Arrange
                String body = objectMapper.writeValueAsString(Map.of("email", validEmail));

                // Act
                mockMvc.perform(post("/api/auth/request-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message", is("If the email exists, OTP has been sent")));

                verify(passwordResetService, times(1)).requestPasswordReset(validEmail);
        }

        @Test
        @DisplayName("shouldReturnTooManyRequests_whenRequestPasswordResetRateLimited")
        void shouldReturnTooManyRequests_whenRequestPasswordResetRateLimited() throws Exception {
                // Arrange
                Mockito.doThrow(new IllegalStateException("Too many OTP requests. Try again later."))
                                .when(passwordResetService)
                                .requestPasswordReset(validEmail);
                String body = objectMapper.writeValueAsString(Map.of("email", validEmail));

                // Act
                mockMvc.perform(post("/api/auth/request-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isTooManyRequests())
                                .andExpect(jsonPath("$.message", is("Too many OTP requests. Try again later.")));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenRequestPasswordResetEmailInvalid")
        void shouldReturnBadRequest_whenRequestPasswordResetEmailInvalid() throws Exception {
                // Arrange
                String body = objectMapper.writeValueAsString(Map.of("email", "invalid-email"));

                // Act
                mockMvc.perform(post("/api/auth/request-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldVerifyOtp_whenOtpValid")
        void shouldVerifyOtp_whenOtpValid() throws Exception {
                // Arrange
                String body = objectMapper.writeValueAsString(Map.of("email", validEmail, "otp", validOtp));

                // Act
                mockMvc.perform(post("/api/auth/verify-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message", is("OTP đã được xác thực thành công")));

                verify(passwordResetService, times(1)).verifyOtp(validEmail, validOtp);
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenVerifyOtpExpired")
        void shouldReturnBadRequest_whenVerifyOtpExpired() throws Exception {
                // Arrange
                Mockito.doThrow(new OtpExpiredException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã OTP mới."))
                                .when(passwordResetService).verifyOtp(validEmail, validOtp);
                String body = objectMapper.writeValueAsString(Map.of("email", validEmail, "otp", validOtp));

                // Act
                mockMvc.perform(post("/api/auth/verify-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message",
                                                is("Mã OTP đã hết hạn. Vui lòng yêu cầu mã OTP mới.")));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenVerifyOtpInvalid")
        void shouldReturnBadRequest_whenVerifyOtpInvalid() throws Exception {
                // Arrange
                Mockito.doThrow(new OtpInvalidException("Mã OTP không đúng. Vui lòng kiểm tra lại và thử lại."))
                                .when(passwordResetService).verifyOtp(validEmail, validOtp);
                String body = objectMapper.writeValueAsString(Map.of("email", validEmail, "otp", validOtp));

                // Act
                mockMvc.perform(post("/api/auth/verify-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message",
                                                is("Mã OTP không đúng. Vui lòng kiểm tra lại và thử lại.")));
        }

        @Test
        @DisplayName("shouldReturnGenericBadRequest_whenVerifyOtpUnexpectedError")
        void shouldReturnGenericBadRequest_whenVerifyOtpUnexpectedError() throws Exception {
                // Arrange
                Mockito.doThrow(new RuntimeException("Unexpected"))
                                .when(passwordResetService).verifyOtp(validEmail, validOtp);
                String body = objectMapper.writeValueAsString(Map.of("email", validEmail, "otp", validOtp));

                // Act
                mockMvc.perform(post("/api/auth/verify-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", is("Mã OTP không hợp lệ. Vui lòng thử lại.")));
        }

        @Test
        @DisplayName("shouldResetPassword_whenConfirmResetValid")
        void shouldResetPassword_whenConfirmResetValid() throws Exception {
                // Arrange
                String body = objectMapper.writeValueAsString(Map.of(
                                "email", validEmail,
                                "otp", validOtp,
                                "newPassword", strongPassword));

                // Act
                mockMvc.perform(post("/api/auth/confirm-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message", is("Password updated successfully")));

                verify(passwordResetService, times(1)).resetPassword(validEmail, validOtp, strongPassword);
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenConfirmResetOtpExpired")
        void shouldReturnBadRequest_whenConfirmResetOtpExpired() throws Exception {
                // Arrange
                Mockito.doThrow(new OtpExpiredException("OTP expired"))
                                .when(passwordResetService).resetPassword(validEmail, validOtp, strongPassword);
                String body = objectMapper.writeValueAsString(Map.of(
                                "email", validEmail,
                                "otp", validOtp,
                                "newPassword", strongPassword));

                // Act
                mockMvc.perform(post("/api/auth/confirm-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", is("OTP expired")));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenConfirmResetOtpInvalid")
        void shouldReturnBadRequest_whenConfirmResetOtpInvalid() throws Exception {
                // Arrange
                Mockito.doThrow(new OtpInvalidException("OTP invalid"))
                                .when(passwordResetService).resetPassword(validEmail, validOtp, strongPassword);
                String body = objectMapper.writeValueAsString(Map.of(
                                "email", validEmail,
                                "otp", validOtp,
                                "newPassword", strongPassword));

                // Act
                mockMvc.perform(post("/api/auth/confirm-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", is("OTP invalid")));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenConfirmResetIllegalArgument")
        void shouldReturnBadRequest_whenConfirmResetIllegalArgument() throws Exception {
                // Arrange
                Mockito.doThrow(new IllegalArgumentException(
                                "Password must contain uppercase, lowercase, number, and special character"))
                                .when(passwordResetService).resetPassword(validEmail, validOtp, strongPassword);
                String body = objectMapper.writeValueAsString(Map.of(
                                "email", validEmail,
                                "otp", validOtp,
                                "newPassword", strongPassword));

                // Act
                mockMvc.perform(post("/api/auth/confirm-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message",
                                                is("Password must contain uppercase, lowercase, number, and special character")));
        }

        @Test
        @DisplayName("shouldReturnGenericBadRequest_whenConfirmResetUnexpectedError")
        void shouldReturnGenericBadRequest_whenConfirmResetUnexpectedError() throws Exception {
                // Arrange
                Mockito.doThrow(new RuntimeException("Unexpected"))
                                .when(passwordResetService).resetPassword(validEmail, validOtp, strongPassword);
                String body = objectMapper.writeValueAsString(Map.of(
                                "email", validEmail,
                                "otp", validOtp,
                                "newPassword", strongPassword));

                // Act
                mockMvc.perform(post("/api/auth/confirm-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", is("Có lỗi xảy ra. Vui lòng thử lại.")));
        }

        @Test
        @WithMockUser
        @DisplayName("shouldLogout_whenAuthorized")
        void shouldLogout_whenAuthorized() throws Exception {
                // Arrange
                UUID uid = UUID.randomUUID();

                // Act
                mockMvc.perform(post("/api/auth/logout")
                                .header("X-User-ID", uid.toString()))
                                // Assert
                                .andExpect(status().isOk());

                verify(authService, times(1)).logout(uid);
        }

        @Test
        @WithMockUser
        @DisplayName("shouldReturnBadRequest_whenLogoutIllegalArgument")
        void shouldReturnBadRequest_whenLogoutIllegalArgument() throws Exception {
                // Arrange
                UUID uid = UUID.randomUUID();
                Mockito.doThrow(new IllegalArgumentException("User not found")).when(authService).logout(uid);

                // Act
                mockMvc.perform(post("/api/auth/logout")
                                .header("X-User-ID", uid.toString()))
                                // Assert
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenLogoutHeaderInvalid")
        void shouldReturnBadRequest_whenLogoutHeaderInvalid() throws Exception {
                // Arrange

                // Act
                mockMvc.perform(post("/api/auth/logout")
                                .header("X-User-ID", "not-a-uuid"))
                                // Assert
                                .andExpect(status().isBadRequest());

                verify(authService, never()).logout(any());
        }

        @Test
        @WithMockUser
        @DisplayName("shouldRefreshToken_whenAuthorized")
        void shouldRefreshToken_whenAuthorized() throws Exception {
                // Arrange
                UUID uid = UUID.randomUUID();

                // Act
                mockMvc.perform(post("/api/auth/refresh")
                                .header("X-User-ID", uid.toString()))
                                // Assert
                                .andExpect(status().isOk());

                verify(authService, times(1)).refreshToken(uid);
        }

        @Test
        @WithMockUser
        @DisplayName("shouldReturnBadRequest_whenRefreshTokenIllegalArgument")
        void shouldReturnBadRequest_whenRefreshTokenIllegalArgument() throws Exception {
                // Arrange
                UUID uid = UUID.randomUUID();
                Mockito.doThrow(new IllegalArgumentException("User not found")).when(authService).refreshToken(uid);

                // Act
                mockMvc.perform(post("/api/auth/refresh")
                                .header("X-User-ID", uid.toString()))
                                // Assert
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldSendEmailVerificationOtp_whenRequestValid")
        void shouldSendEmailVerificationOtp_whenRequestValid() throws Exception {
                // Arrange
                String body = objectMapper.writeValueAsString(Map.of("email", validEmail));

                // Act
                mockMvc.perform(post("/api/auth/request-email-verification")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message", is("Mã OTP đã được gửi đến email của bạn")));

                verify(emailVerificationService, times(1)).sendVerificationOtp(validEmail);
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenRequestEmailVerificationIllegalArgument")
        void shouldReturnBadRequest_whenRequestEmailVerificationIllegalArgument() throws Exception {
                // Arrange
                Mockito.doThrow(new IllegalArgumentException("Email không hợp lệ")).when(emailVerificationService)
                                .sendVerificationOtp(validEmail);
                String body = objectMapper.writeValueAsString(Map.of("email", validEmail));

                // Act
                mockMvc.perform(post("/api/auth/request-email-verification")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", is("Email không hợp lệ")));
        }

        @Test
        @DisplayName("shouldReturnTooManyRequests_whenRequestEmailVerificationThrottled")
        void shouldReturnTooManyRequests_whenRequestEmailVerificationThrottled() throws Exception {
                // Arrange
                Mockito.doThrow(new IllegalStateException("Bạn đã yêu cầu quá nhiều mã OTP. Vui lòng đợi 5 phút nữa."))
                                .when(emailVerificationService).sendVerificationOtp(validEmail);
                String body = objectMapper.writeValueAsString(Map.of("email", validEmail));

                // Act
                mockMvc.perform(post("/api/auth/request-email-verification")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isTooManyRequests())
                                .andExpect(jsonPath("$.message",
                                                is("Bạn đã yêu cầu quá nhiều mã OTP. Vui lòng đợi 5 phút nữa.")));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenRequestEmailVerificationEmailInvalid")
        void shouldReturnBadRequest_whenRequestEmailVerificationEmailInvalid() throws Exception {
                // Arrange
                String body = objectMapper.writeValueAsString(Map.of("email", "invalid-email"));

                // Act
                mockMvc.perform(post("/api/auth/request-email-verification")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldVerifyEmailOtp_whenValid")
        void shouldVerifyEmailOtp_whenValid() throws Exception {
                // Arrange
                Mockito.when(emailVerificationService.verifyOtp(validEmail, validOtp)).thenReturn(true);
                String body = objectMapper.writeValueAsString(Map.of("email", validEmail, "otp", validOtp));

                // Act
                mockMvc.perform(post("/api/auth/verify-email-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message", is("Email đã được xác thực thành công")))
                                .andExpect(jsonPath("$.verified", is(true)));

                verify(emailVerificationService, times(1)).verifyOtp(validEmail, validOtp);
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenVerifyEmailOtpExpired")
        void shouldReturnBadRequest_whenVerifyEmailOtpExpired() throws Exception {
                // Arrange
                Mockito.doThrow(new OtpExpiredException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã OTP mới."))
                                .when(emailVerificationService).verifyOtp(validEmail, validOtp);
                String body = objectMapper.writeValueAsString(Map.of("email", validEmail, "otp", validOtp));

                // Act
                mockMvc.perform(post("/api/auth/verify-email-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message",
                                                is("Mã OTP đã hết hạn. Vui lòng yêu cầu mã OTP mới.")));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenVerifyEmailOtpInvalid")
        void shouldReturnBadRequest_whenVerifyEmailOtpInvalid() throws Exception {
                // Arrange
                Mockito.doThrow(new OtpInvalidException("Mã OTP không đúng. Vui lòng kiểm tra lại và thử lại."))
                                .when(emailVerificationService).verifyOtp(validEmail, validOtp);
                String body = objectMapper.writeValueAsString(Map.of("email", validEmail, "otp", validOtp));

                // Act
                mockMvc.perform(post("/api/auth/verify-email-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message",
                                                is("Mã OTP không đúng. Vui lòng kiểm tra lại và thử lại.")));
        }

        @Test
        @DisplayName("shouldReturnGenericBadRequest_whenVerifyEmailOtpUnexpectedError")
        void shouldReturnGenericBadRequest_whenVerifyEmailOtpUnexpectedError() throws Exception {
                // Arrange
                Mockito.doThrow(new RuntimeException("Unexpected"))
                                .when(emailVerificationService).verifyOtp(validEmail, validOtp);
                String body = objectMapper.writeValueAsString(Map.of("email", validEmail, "otp", validOtp));

                // Act
                mockMvc.perform(post("/api/auth/verify-email-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", is("Mã OTP không hợp lệ. Vui lòng thử lại.")));
        }

        @Test
        @DisplayName("shouldCheckEmailVerified_whenEmailVerifiedTrue")
        void shouldCheckEmailVerified_whenEmailVerifiedTrue() throws Exception {
                // Arrange
                Mockito.when(emailVerificationService.isEmailVerified(validEmail)).thenReturn(true);

                // Act
                mockMvc.perform(get("/api/auth/check-email-verified/{email}", validEmail))
                                // Assert
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.verified", is(true)));
        }

        @Test
        @DisplayName("shouldReturnVerifiedFalse_whenCheckEmailVerifiedUnexpectedError")
        void shouldReturnVerifiedFalse_whenCheckEmailVerifiedUnexpectedError() throws Exception {
                // Arrange
                Mockito.when(emailVerificationService.isEmailVerified(validEmail))
                                .thenThrow(new RuntimeException("Unexpected"));

                // Act
                mockMvc.perform(get("/api/auth/check-email-verified/{email}", validEmail))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.verified", is(false)));
        }

        @Test
        @DisplayName("shouldCheckEmailExists_whenEmailExists")
        void shouldCheckEmailExists_whenEmailExists() throws Exception {
                // Arrange
                Mockito.when(authService.emailExists(validEmail)).thenReturn(true);

                // Act
                mockMvc.perform(get("/api/auth/check-email-exists/{email}", validEmail))
                                // Assert
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.exists", is(true)));
        }

        @Test
        @DisplayName("shouldReturnExistsFalse_whenCheckEmailExistsUnexpectedError")
        void shouldReturnExistsFalse_whenCheckEmailExistsUnexpectedError() throws Exception {
                // Arrange
                Mockito.when(authService.emailExists(validEmail)).thenThrow(new RuntimeException("Unexpected"));

                // Act
                mockMvc.perform(get("/api/auth/check-email-exists/{email}", validEmail))
                                // Assert
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.exists", is(false)));
        }
}
