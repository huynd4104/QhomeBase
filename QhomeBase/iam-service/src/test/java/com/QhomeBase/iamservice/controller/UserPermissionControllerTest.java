package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.UserPermissionDenyRequest;
import com.QhomeBase.iamservice.dto.UserPermissionGrantRequest;
import com.QhomeBase.iamservice.dto.UserPermissionRevokeRequest;
import com.QhomeBase.iamservice.dto.UserPermissionSummaryDto;
import com.QhomeBase.iamservice.security.AuthzService;
import com.QhomeBase.iamservice.security.JwtAuthFilter;
import com.QhomeBase.iamservice.service.UserGrantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = UserPermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserPermissionControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private UserGrantService userGrantService;

        @MockitoBean(name = "authz")
        private AuthzService authzService;

        @MockitoBean
        private JwtAuthFilter jwtAuthFilter;

        private UUID tenantId;
        private UUID userId;

        @BeforeEach
        void setup() {
                tenantId = UUID.randomUUID();
                userId = UUID.randomUUID();
        }

        @Test
        @WithMockUser
        @DisplayName("shouldRevokeGrants_whenAuthorized")
        void shouldRevokeGrants_whenAuthorized() throws Exception {
                // Arrange
                Mockito.doNothing().when(userGrantService)
                                .revokeGrantsFromUser(
                                                org.mockito.ArgumentMatchers.any(UserPermissionRevokeRequest.class));
                var body = objectMapper
                                .writeValueAsString(new UserPermissionRevokeRequest(null, null, List.of("perm.a")));

                // Act
                mockMvc.perform(post("/api/user-permissions/revoke-grants/{tenantId}/{userId}", tenantId, userId)
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                                .user("admin").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isOk());

                ArgumentCaptor<UserPermissionRevokeRequest> captor = ArgumentCaptor
                                .forClass(UserPermissionRevokeRequest.class);
                verify(userGrantService, times(1)).revokeGrantsFromUser(captor.capture());
                UserPermissionRevokeRequest req = captor.getValue();
                org.junit.jupiter.api.Assertions.assertEquals(tenantId, req.getTenantId());
                org.junit.jupiter.api.Assertions.assertEquals(userId, req.getUserId());
                org.junit.jupiter.api.Assertions.assertEquals(List.of("perm.a"), req.getPermissionCodes());
        }

        @Test
        @WithMockUser
        @DisplayName("shouldRevokeDenies_whenAuthorized")
        void shouldRevokeDenies_whenAuthorized() throws Exception {
                // Arrange
                Mockito.doNothing().when(userGrantService)
                                .revokeDeniesFromUser(
                                                org.mockito.ArgumentMatchers.any(UserPermissionRevokeRequest.class));
                var body = objectMapper
                                .writeValueAsString(new UserPermissionRevokeRequest(null, null, List.of("perm.x")));

                // Act
                mockMvc.perform(post("/api/user-permissions/revoke-denies/{tenantId}/{userId}", tenantId, userId)
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                                .user("admin").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                // Assert
                                .andExpect(status().isOk());

                ArgumentCaptor<UserPermissionRevokeRequest> captor = ArgumentCaptor
                                .forClass(UserPermissionRevokeRequest.class);
                verify(userGrantService, times(1)).revokeDeniesFromUser(captor.capture());
                UserPermissionRevokeRequest req = captor.getValue();
                org.junit.jupiter.api.Assertions.assertEquals(tenantId, req.getTenantId());
                org.junit.jupiter.api.Assertions.assertEquals(userId, req.getUserId());
                org.junit.jupiter.api.Assertions.assertEquals(List.of("perm.x"), req.getPermissionCodes());
        }

        @Test
        @WithMockUser
        @DisplayName("shouldReturnSummary_whenAuthorized")
        void shouldReturnSummary_whenAuthorized() throws Exception {
                // Arrange
                var summary = UserPermissionSummaryDto.builder()
                                .userId(userId)
                                .tenantId(tenantId)
                                .effectivePermissions(List.of("a", "b"))
                                .totalEffectivePermissions(2)
                                .build();
                Mockito.when(userGrantService.getUserPermissionSummary(userId, tenantId)).thenReturn(summary);

                // Act
                mockMvc.perform(get("/api/user-permissions/summary/{tenantId}/{userId}", tenantId, userId))
                                // Assert
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId", is(userId.toString())))
                                .andExpect(jsonPath("$.tenantId", is(tenantId.toString())))
                                .andExpect(jsonPath("$.effectivePermissions", hasSize(2)))
                                .andExpect(jsonPath("$.totalEffectivePermissions", is(2)));
        }

        @Test
        @WithMockUser
        @DisplayName("shouldReturnActiveGrants_whenAuthorized")
        void shouldReturnActiveGrants_whenAuthorized() throws Exception {
                // Arrange
                Mockito.when(userGrantService.getActiveGrants(userId, tenantId))
                                .thenReturn(List.of("perm.a", "perm.b"));

                // Act
                mockMvc.perform(get("/api/user-permissions/grants/{tenantId}/{userId}", tenantId, userId))
                                // Assert
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0]", is("perm.a")))
                                .andExpect(jsonPath("$[1]", is("perm.b")));
        }

        @Test
        @WithMockUser
        @DisplayName("shouldReturnActiveDenies_whenAuthorized")
        void shouldReturnActiveDenies_whenAuthorized() throws Exception {
                // Arrange
                Mockito.when(userGrantService.getActiveDenies(userId, tenantId)).thenReturn(List.of("perm.x"));

                // Act
                mockMvc.perform(get("/api/user-permissions/denies/{tenantId}/{userId}", tenantId, userId))
                                // Assert
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0]", is("perm.x")));
        }
}
