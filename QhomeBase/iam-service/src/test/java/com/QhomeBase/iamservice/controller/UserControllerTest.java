package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.client.BaseServiceClient;
import com.QhomeBase.iamservice.dto.CreateUserForResidentDto;
import com.QhomeBase.iamservice.dto.StaffImportResponse;
import com.QhomeBase.iamservice.dto.StaffImportRowResult;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.security.AuthzService;
import com.QhomeBase.iamservice.security.JwtAuthFilter;
import com.QhomeBase.iamservice.service.UserService;
import com.QhomeBase.iamservice.service.imports.StaffImportService;
import com.QhomeBase.iamservice.controller.UserController.UpdatePasswordRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private UserService userService;

        @MockitoBean
        private StaffImportService staffImportService;

        @MockitoBean
        private com.QhomeBase.iamservice.repository.RolePermissionRepository rolePermissionRepository;

        @MockitoBean
        private com.QhomeBase.iamservice.repository.UserRepository userRepository;

        @MockitoBean
        private BaseServiceClient baseServiceClient;

        @MockitoBean(name = "authz")
        private AuthzService authzService;

        @MockitoBean
        private JwtAuthFilter jwtAuthFilter;

        @Test
        @DisplayName("shouldReturnUserInfo_whenAuthorizedAndUserExists")
        void shouldReturnUserInfo_whenAuthorizedAndUserExists() throws Exception {
                UUID userId = UUID.randomUUID();
                User user = User.builder()
                                .id(userId)
                                .username("john")
                                .email("john@example.com")
                                .passwordHash("hash")
                                .active(true)
                                .roles(List.of(UserRole.ADMIN, UserRole.TECHNICIAN))
                                .build();
                Mockito.when(userService.findUserWithRolesById(userId)).thenReturn(Optional.of(user));
                Mockito.when(rolePermissionRepository.findPermissionCodesByRole("ADMIN"))
                                .thenReturn(List.of("iam.user.read", "iam.user.create"));
                Mockito.when(rolePermissionRepository.findPermissionCodesByRole("TECHNICIAN"))
                                .thenReturn(List.of("iam.user.read"));
                Mockito.when(authzService.canViewUser(userId)).thenReturn(true);

                mockMvc.perform(get("/api/users/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username", is("john")))
                                .andExpect(jsonPath("$.email", is("john@example.com")))
                                .andExpect(jsonPath("$.roles", containsInAnyOrder("admin", "technician")))
                                .andExpect(jsonPath("$.permissions",
                                                containsInAnyOrder("iam.user.read", "iam.user.create")));
        }

        @Test
        @DisplayName("shouldReturnNotFound_whenGetUserInfoNotFound")
        void shouldReturnNotFound_whenGetUserInfoNotFound() throws Exception {
                UUID userId = UUID.randomUUID();
                Mockito.when(userService.findUserWithRolesById(userId)).thenReturn(Optional.empty());
                Mockito.when(authzService.canViewUser(userId)).thenReturn(true);

                mockMvc.perform(get("/api/users/{userId}", userId))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("shouldReturnUserStatus_whenAuthorized")
        void shouldReturnUserStatus_whenAuthorized() throws Exception {
                UUID userId = UUID.randomUUID();
                User user = User.builder()
                                .id(userId)
                                .username("john")
                                .email("john@example.com")
                                .passwordHash("hash")
                                .active(true)
                                .failedLoginAttempts(2)
                                .lastLogin(LocalDateTime.now())
                                .build();
                Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));
                Mockito.when(authzService.canViewUser(userId)).thenReturn(true);

                mockMvc.perform(get("/api/users/{userId}/status", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.active", is(true)))
                                .andExpect(jsonPath("$.failedLoginAttempts", is(2)))
                                .andExpect(jsonPath("$.accountLocked", is(false)))
                                .andExpect(jsonPath("$.lastLogin").exists());
        }

        @Test
        @DisplayName("shouldUpdatePassword_whenAuthorized")
        void shouldUpdatePassword_whenAuthorized() throws Exception {
                UUID userId = UUID.randomUUID();
                Mockito.doNothing().when(userService).updatePassword(userId, "StrongPass123");
                Mockito.when(authzService.canUpdateUser(userId)).thenReturn(true);

                String body = objectMapper.writeValueAsString(new UpdatePasswordRequest("StrongPass123"));

                mockMvc.perform(patch("/api/users/{userId}/password", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenUpdatePasswordTooShort")
        void shouldReturnBadRequest_whenUpdatePasswordTooShort() throws Exception {
                UUID userId = UUID.randomUUID();
                Mockito.when(authzService.canUpdateUser(userId)).thenReturn(true);

                String body = objectMapper.writeValueAsString(new UpdatePasswordRequest("short"));

                mockMvc.perform(put("/api/users/{userId}/password", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("shouldCreateUserForResident_whenAdminRole")
        void shouldCreateUserForResident_whenAdminRole() throws Exception {
                UUID residentId = UUID.randomUUID();
                var req = new CreateUserForResidentDto("newresident", "res@example.com", "Password123", false,
                                residentId, null);
                User created = User.builder()
                                .id(UUID.randomUUID())
                                .username("newresident")
                                .email("res@example.com")
                                .passwordHash("hash")
                                .active(true)
                                .roles(List.of(UserRole.RESIDENT))
                                .build();
                Mockito.when(userService.createUserForResident("newresident", "res@example.com", "Password123",
                                residentId, null))
                                .thenReturn(created);

                String body = objectMapper.writeValueAsString(req);

                mockMvc.perform(post("/api/users/create-for-resident")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.username", is("newresident")))
                                .andExpect(jsonPath("$.email", is("res@example.com")))
                                .andExpect(jsonPath("$.roles[0]", is("resident")))
                                .andExpect(jsonPath("$.active", is(true)));
        }

        @Test
        @DisplayName("shouldImportStaffAccounts_whenAuthorized")
        void shouldImportStaffAccounts_whenAuthorized() throws Exception {
                Mockito.when(authzService.canCreateUser()).thenReturn(true);

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "staff.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                new byte[] { 1, 2, 3 });
                StaffImportResponse response = new StaffImportResponse(
                                1,
                                1,
                                0,
                                java.util.Collections.emptyList());
                Mockito.when(staffImportService.importStaffAccounts(any())).thenReturn(response);

                mockMvc.perform(multipart("/api/users/staff/import").file(file))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalRows", is(1)))
                                .andExpect(jsonPath("$.successCount", is(1)))
                                .andExpect(jsonPath("$.failureCount", is(0)));
                verify(staffImportService, times(1)).importStaffAccounts(any());
        }
}
