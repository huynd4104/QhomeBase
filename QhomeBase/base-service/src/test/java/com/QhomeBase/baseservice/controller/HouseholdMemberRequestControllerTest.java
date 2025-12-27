package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.HouseholdMemberRequestCreateDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberRequestDecisionDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberRequestDto;
import com.QhomeBase.baseservice.security.JwtAuthFilter;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.HouseholdMemberRequestService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HouseholdMemberRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class HouseholdMemberRequestControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private HouseholdMemberRequestService requestService;

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
        void shouldCreateHouseholdMemberRequest() throws Exception {
                UUID householdId = UUID.randomUUID();
                var req = new HouseholdMemberRequestCreateDto(
                                householdId,
                                "John Doe",
                                "0123",
                                "john@example.com",
                                "NID",
                                java.time.LocalDate.of(1990, 1, 1),
                                "SPOUSE",
                                "img1",
                                "note");
                var dto = new HouseholdMemberRequestDto(
                                UUID.randomUUID(),
                                req.householdId(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                req.residentFullName(),
                                req.residentPhone(),
                                req.residentEmail(),
                                req.residentNationalId(),
                                req.residentDob(),
                                userId,
                                "Resident",
                                req.relation(),
                                req.proofOfRelationImageUrl(),
                                req.note(),
                                com.QhomeBase.baseservice.model.HouseholdMemberRequest.RequestStatus.PENDING,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now());
                Mockito.when(requestService.createRequest(any(HouseholdMemberRequestCreateDto.class),
                                any(UserPrincipal.class))).thenReturn(dto);

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(post("/api/household-member-requests")
                                .with(securityContext(ctx))
                                .principal(authResident)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        void shouldGetMyHouseholdMemberRequests() throws Exception {
                var dto = new HouseholdMemberRequestDto(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "John Doe",
                                "0123",
                                "john@example.com",
                                "NID",
                                java.time.LocalDate.of(1990, 1, 1),
                                userId,
                                "Resident",
                                "SPOUSE",
                                null,
                                null,
                                com.QhomeBase.baseservice.model.HouseholdMemberRequest.RequestStatus.PENDING,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now());
                Mockito.when(requestService.getRequestsForUser(eq(userId))).thenReturn(List.of(dto));

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(get("/api/household-member-requests/my")
                                .with(securityContext(ctx))
                                .principal(authResident))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].requestedBy").value(userId.toString()));
        }

        @Test
        void shouldCancelMyHouseholdMemberRequest() throws Exception {
                UUID id = UUID.randomUUID();
                var dto = new HouseholdMemberRequestDto(
                                id,
                                UUID.randomUUID(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "John Doe",
                                "0123",
                                "john@example.com",
                                "NID",
                                java.time.LocalDate.of(1990, 1, 1),
                                userId,
                                "Resident",
                                "SPOUSE",
                                null,
                                null,
                                com.QhomeBase.baseservice.model.HouseholdMemberRequest.RequestStatus.CANCELLED,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now());
                Mockito.when(requestService.cancelRequest(eq(id), eq(userId))).thenReturn(dto);

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(patch("/api/household-member-requests/" + id + "/cancel")
                                .with(securityContext(ctx))
                                .principal(authResident))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        void shouldDecideHouseholdMemberRequest_Admin() throws Exception {
                UUID id = UUID.randomUUID();
                var decision = new HouseholdMemberRequestDecisionDto(true, "ok");
                var dto = new HouseholdMemberRequestDto(
                                id,
                                UUID.randomUUID(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "John Doe",
                                "0123",
                                "john@example.com",
                                "NID",
                                java.time.LocalDate.of(1990, 1, 1),
                                userId,
                                "Admin",
                                "SPOUSE",
                                null,
                                "ok",
                                com.QhomeBase.baseservice.model.HouseholdMemberRequest.RequestStatus.APPROVED,
                                UUID.randomUUID(),
                                "Admin",
                                null,
                                null,
                                null,
                                java.time.OffsetDateTime.now(),
                                null,
                                java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now());
                Mockito.when(requestService.decideRequest(eq(id), any(HouseholdMemberRequestDecisionDto.class),
                                any(UUID.class))).thenReturn(dto);

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authAdmin);
                mockMvc.perform(post("/api/household-member-requests/" + id + "/decision")
                                .with(securityContext(ctx))
                                .principal(authAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(decision)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("APPROVED"));
        }
}
