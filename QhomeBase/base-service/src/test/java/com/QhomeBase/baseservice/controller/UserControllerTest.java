package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.ResidentAccountDto;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import com.QhomeBase.baseservice.security.JwtAuthFilter;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.IamClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IamClientService iamClientService;

    @MockitoBean
    private ResidentRepository residentRepository;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private UUID userId;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        // Arrange
        userId = UUID.randomUUID();
        var principal = new UserPrincipal(
                userId,
                "residentUser",
                List.of("RESIDENT"),
                List.of(),
                "token-123");
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_RESIDENT"));
        auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    @DisplayName("shouldReturnUserInfo_whenAuthenticatedAndResidentExists")
    void shouldReturnUserInfo_whenAuthenticatedAndResidentExists() throws Exception {
        // Arrange
        var account = new ResidentAccountDto(userId, "residentUser", "user@example.com", List.of("RESIDENT"), true);
        Mockito.when(iamClientService.getUserAccountInfo(eq(userId))).thenReturn(account);

        var resident = Resident.builder()
                .id(UUID.randomUUID())
                .fullName("John Doe")
                .phone("0123456789")
                .email("user@example.com")
                .nationalId("CIT123456")
                .dob(LocalDate.of(1990, 1, 15))
                .userId(userId)
                .build();
        Mockito.when(residentRepository.findByUserId(eq(userId))).thenReturn(Optional.of(resident));

        // Act
        var ctx = new org.springframework.security.core.context.SecurityContextImpl();
        ctx.setAuthentication(auth);
        mockMvc.perform(get("/api/users/me")
                .with(securityContext(ctx))
                .principal(auth)
                .accept(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("residentUser"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("RESIDENT"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.residentId").value(resident.getId().toString()))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("0123456789"))
                .andExpect(jsonPath("$.citizenId").value("CIT123456"))
                .andExpect(jsonPath("$.identityNumber").value("CIT123456"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-01-15"));

        verify(iamClientService, times(1)).getUserAccountInfo(eq(userId));
        verify(residentRepository, times(1)).findByUserId(eq(userId));
    }

    @Test
    @DisplayName("shouldReturnNotFound_whenAccountNotFound")
    void shouldReturnNotFound_whenAccountNotFound() throws Exception {
        // Arrange
        Mockito.when(iamClientService.getUserAccountInfo(eq(userId))).thenReturn(null);

        // Act
        var ctx = new org.springframework.security.core.context.SecurityContextImpl();
        ctx.setAuthentication(auth);
        mockMvc.perform(get("/api/users/me")
                .with(securityContext(ctx))
                .principal(auth))
                // Assert
                .andExpect(status().isNotFound());

        verify(iamClientService, times(1)).getUserAccountInfo(eq(userId));
        verify(residentRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("shouldReturnUserInfoWithoutResident_whenResidentMissing")
    void shouldReturnUserInfoWithoutResident_whenResidentMissing() throws Exception {
        // Arrange
        var account = new ResidentAccountDto(userId, "residentUser", "user@example.com", List.of("RESIDENT"), true);
        Mockito.when(iamClientService.getUserAccountInfo(eq(userId))).thenReturn(account);
        Mockito.when(residentRepository.findByUserId(eq(userId))).thenReturn(Optional.empty());

        // Act
        var ctx = new org.springframework.security.core.context.SecurityContextImpl();
        ctx.setAuthentication(auth);
        mockMvc.perform(get("/api/users/me")
                .with(securityContext(ctx))
                .principal(auth))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("residentUser"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("RESIDENT"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.residentId").doesNotExist())
                .andExpect(jsonPath("$.fullName").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.citizenId").doesNotExist())
                .andExpect(jsonPath("$.identityNumber").doesNotExist())
                .andExpect(jsonPath("$.dateOfBirth").doesNotExist());

        verify(iamClientService, times(1)).getUserAccountInfo(eq(userId));
        verify(residentRepository, times(1)).findByUserId(eq(userId));
    }

    @Test
    @DisplayName("shouldContinueWithoutResident_whenRepositoryThrows")
    void shouldContinueWithoutResident_whenRepositoryThrows() throws Exception {
        // Arrange
        var account = new ResidentAccountDto(userId, "residentUser", "user@example.com", List.of("RESIDENT"), true);
        Mockito.when(iamClientService.getUserAccountInfo(eq(userId))).thenReturn(account);
        Mockito.when(residentRepository.findByUserId(eq(userId))).thenThrow(new RuntimeException("DB error"));

        // Act
        var ctx = new org.springframework.security.core.context.SecurityContextImpl();
        ctx.setAuthentication(auth);
        mockMvc.perform(get("/api/users/me")
                .with(securityContext(ctx))
                .principal(auth))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.residentId").doesNotExist());

        verify(iamClientService, times(1)).getUserAccountInfo(eq(userId));
        verify(residentRepository, times(1)).findByUserId(eq(userId));
    }

    @Test
    @DisplayName("shouldReturn500_whenAuthenticationIsNull")
    void shouldReturn500_whenAuthenticationIsNull() throws Exception {
        // Arrange
        // No authentication

        // Act
        mockMvc.perform(get("/api/users/me"))
                // Assert
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Authentication is null"));

        verify(iamClientService, never()).getUserAccountInfo(any());
        verify(residentRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("shouldReturn500_whenIamClientThrowsRuntimeException")
    void shouldReturn500_whenIamClientThrowsRuntimeException() throws Exception {
        // Arrange
        Mockito.when(iamClientService.getUserAccountInfo(eq(userId))).thenThrow(new RuntimeException("IAM failure"));

        // Act
        var ctx = new org.springframework.security.core.context.SecurityContextImpl();
        ctx.setAuthentication(auth);
        mockMvc.perform(get("/api/users/me")
                .with(securityContext(ctx))
                .principal(auth))
                // Assert
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to get user info: IAM failure"));

        verify(iamClientService, times(1)).getUserAccountInfo(eq(userId));
    }
}
