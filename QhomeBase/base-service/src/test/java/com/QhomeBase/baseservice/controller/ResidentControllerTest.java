package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.*;
import com.QhomeBase.baseservice.model.ResidentStatus;
import com.QhomeBase.baseservice.security.JwtAuthFilter;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.ResidentAccountService;
import com.QhomeBase.baseservice.service.ResidentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ResidentController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResidentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private ResidentAccountService residentAccountService;

        @MockitoBean
        private ResidentService residentService;

        @MockitoBean
        private JwtAuthFilter jwtAuthFilter;

        private UsernamePasswordAuthenticationToken authResident;
        private UsernamePasswordAuthenticationToken authAdmin;
        private UUID userId;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                var principalResident = new UserPrincipal(userId, "resident", List.of("RESIDENT"), List.of(), "token");
                authResident = new UsernamePasswordAuthenticationToken(principalResident, null,
                                List.of(new SimpleGrantedAuthority("ROLE_RESIDENT")));

                var principalAdmin = new UserPrincipal(UUID.randomUUID(), "admin", List.of("ADMIN"), List.of(),
                                "token");
                authAdmin = new UsernamePasswordAuthenticationToken(principalAdmin, null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        @Test
        void shouldGetResidentsWithoutAccount() throws Exception {
                UUID unitId = UUID.randomUUID();
                var dto = new ResidentWithoutAccountDto(UUID.randomUUID(), "John", "0123", "john@example.com", "NID",
                                LocalDate.of(1990, 1, 1), ResidentStatus.ACTIVE, "OWNER", true);
                Mockito.when(residentAccountService.getResidentsWithoutAccount(eq(unitId), eq(userId)))
                                .thenReturn(List.of(dto));

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(get("/api/residents/units/" + unitId + "/household/members/without-account")
                                .with(securityContext(ctx))
                                .principal(authResident))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].fullName").value("John"));
        }

        @Test
        void shouldCreateAccountRequest() throws Exception {
                var create = new CreateAccountRequestDto(UUID.randomUUID(), null, null, true, List.of("img1"));
                var result = new AccountCreationRequestDto(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "John",
                                "john@example.com",
                                "0123",
                                null,
                                null,
                                null,
                                "OWNER",
                                userId,
                                "John",
                                "john",
                                "john@example.com",
                                true,
                                com.QhomeBase.baseservice.model.AccountCreationRequest.RequestStatus.PENDING,
                                null,
                                null,
                                null,
                                null,
                                null,
                                java.util.List.of("img1"),
                                null,
                                null,
                                java.time.OffsetDateTime.now());
                Mockito.when(residentAccountService.createAccountRequest(any(CreateAccountRequestDto.class),
                                eq(userId))).thenReturn(result);

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(post("/api/residents/create-account-request")
                                .with(securityContext(ctx))
                                .principal(authResident)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(create)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(result.id().toString()));
        }

        @Test
        void shouldGetMyAccountRequests() throws Exception {
                var item = new AccountCreationRequestDto(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                userId,
                                null,
                                null,
                                null,
                                true,
                                com.QhomeBase.baseservice.model.AccountCreationRequest.RequestStatus.PENDING,
                                null,
                                null,
                                null,
                                null,
                                null,
                                java.util.List.of(),
                                null,
                                null,
                                java.time.OffsetDateTime.now());
                Mockito.when(residentAccountService.getMyRequests(eq(userId))).thenReturn(List.of(item));

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(get("/api/residents/my-account-requests")
                                .with(securityContext(ctx))
                                .principal(authResident))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].requestedBy").value(userId.toString()));
        }

        @Test
        void shouldCancelAccountRequest() throws Exception {
                UUID reqId = UUID.randomUUID();
                var result = new AccountCreationRequestDto(
                                reqId,
                                UUID.randomUUID(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                userId,
                                null,
                                null,
                                null,
                                true,
                                com.QhomeBase.baseservice.model.AccountCreationRequest.RequestStatus.CANCELLED,
                                null,
                                null,
                                null,
                                null,
                                null,
                                java.util.List.of(),
                                null,
                                null,
                                java.time.OffsetDateTime.now());
                Mockito.when(residentAccountService.cancelAccountRequest(eq(reqId), eq(userId))).thenReturn(result);

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(patch("/api/residents/account-requests/" + reqId + "/cancel")
                                .with(securityContext(ctx))
                                .principal(authResident))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(reqId.toString()));
        }

        @Test
        void shouldGetResidentById() throws Exception {
                UUID residentId = UUID.randomUUID();
                var dto = new ResidentDto(residentId, "John", "0123", "john@example.com", "NID",
                                LocalDate.of(1990, 1, 1), ResidentStatus.ACTIVE, userId, java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now());
                Mockito.when(residentService.getById(eq(residentId))).thenReturn(dto);

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(get("/api/residents/" + residentId)
                                .with(securityContext(ctx))
                                .principal(authResident))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(residentId.toString()));
        }

        @Test
        void shouldGetMyUnits() throws Exception {
                UUID unitId = UUID.randomUUID();
                var unit = new UnitDto(unitId, UUID.randomUUID(), "B-01", "Tower", "U-101", 10,
                                java.math.BigDecimal.valueOf(80), 2, com.QhomeBase.baseservice.model.UnitStatus.ACTIVE,
                                null, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now());
                Mockito.when(residentAccountService.getMyUnits(eq(userId))).thenReturn(List.of(unit));

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(get("/api/residents/my-units")
                                .with(securityContext(ctx))
                                .principal(authResident))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(unitId.toString()));
        }

        @Test
        void shouldSyncStaffResident() throws Exception {
                var req = new StaffResidentSyncRequest(UUID.randomUUID(), "Staff A", "staff@example.com", "0123");
                var dto = new ResidentDto(UUID.randomUUID(), "Staff A", "0123", "staff@example.com", null, null,
                                ResidentStatus.ACTIVE, null, java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now());
                Mockito.when(residentService.syncStaffResident(any(StaffResidentSyncRequest.class))).thenReturn(dto);

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authAdmin);
                mockMvc.perform(post("/api/residents/staff/sync")
                                .with(securityContext(ctx))
                                .principal(authAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fullName").value("Staff A"));
        }
}
