package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.CleaningRequestConfigDto;
import com.QhomeBase.baseservice.dto.CleaningRequestDto;
import com.QhomeBase.baseservice.dto.CreateCleaningRequestDto;
import com.QhomeBase.baseservice.security.AuthzService;
import com.QhomeBase.baseservice.security.JwtAuthFilter;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.CleaningRequestMonitor;
import com.QhomeBase.baseservice.service.CleaningRequestService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = CleaningRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class CleaningRequestControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private CleaningRequestService cleaningRequestService;

        @MockitoBean
        private CleaningRequestMonitor cleaningRequestMonitor;

        @MockitoBean
        private AuthzService authz;

        @MockitoBean
        private JwtAuthFilter jwtAuthFilter;

        private UsernamePasswordAuthenticationToken authResident;
        private UUID userId;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                var principalResident = new UserPrincipal(userId, "resident", List.of("RESIDENT"), List.of(), "token");
                authResident = new UsernamePasswordAuthenticationToken(principalResident, null,
                                List.of(new SimpleGrantedAuthority("ROLE_RESIDENT")));
        }

        @Test
        void shouldCreateCleaningRequest() throws Exception {
                var req = new CreateCleaningRequestDto(UUID.randomUUID(), "Basic", LocalDate.now(), LocalTime.NOON,
                                BigDecimal.valueOf(1.5), "Living Room", "note", "0123", List.of("extra"), "CASH");
                var dto = new CleaningRequestDto(UUID.randomUUID(), req.unitId(), null, null, userId,
                                req.cleaningType(),
                                req.cleaningDate(), req.startTime(), req.durationHours(), req.location(), req.note(),
                                req.contactPhone(), req.extraServices(), req.paymentMethod(), "PENDING",
                                OffsetDateTime.now(),
                                OffsetDateTime.now(), null, false);
                org.mockito.Mockito.doReturn(dto)
                                .when(cleaningRequestService)
                                .create(any(UUID.class), any(CreateCleaningRequestDto.class));

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(post("/api/cleaning-requests")
                                .with(securityContext(ctx))
                                .principal(authResident)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk());
        }

        @Test
        void shouldGetCleaningConfig() throws Exception {
                Mockito.when(cleaningRequestMonitor.getConfig()).thenReturn(new CleaningRequestConfigDto(
                                java.time.Duration.ofMinutes(5), java.time.Duration.ofMinutes(5),
                                java.time.Duration.ofMinutes(6)));

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(get("/api/cleaning-requests/config")
                                .with(securityContext(ctx))
                                .principal(authResident)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void shouldGetMyCleaningRequests() throws Exception {
                var dto = new CleaningRequestDto(UUID.randomUUID(), UUID.randomUUID(), null, null, userId, "Basic",
                                LocalDate.now(), LocalTime.NOON, BigDecimal.ONE, "Living Room", null, "0123", List.of(),
                                "CASH",
                                "PENDING", OffsetDateTime.now(), OffsetDateTime.now(), null, false);
                org.mockito.Mockito.doReturn(java.util.List.of(dto))
                                .when(cleaningRequestService)
                                .getMyRequests(any(UUID.class));

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(get("/api/cleaning-requests/my")
                                .with(securityContext(ctx))
                                .principal(authResident)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void shouldResendCleaningRequest() throws Exception {
                UUID reqId = UUID.randomUUID();
                var dto = new CleaningRequestDto(reqId, UUID.randomUUID(), null, null, userId, "Basic", LocalDate.now(),
                                LocalTime.NOON, BigDecimal.ONE, "Living Room", null, "0123", List.of(), "CASH",
                                "PENDING",
                                OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now(), true);
                org.mockito.Mockito.doReturn(dto)
                                .when(cleaningRequestService)
                                .resendRequest(any(UUID.class), eq(reqId));

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(post("/api/cleaning-requests/" + reqId + "/resend")
                                .with(securityContext(ctx))
                                .principal(authResident)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void shouldCancelCleaningRequest() throws Exception {
                UUID reqId = UUID.randomUUID();
                var dto = new CleaningRequestDto(reqId, UUID.randomUUID(), null, null, userId, "Basic", LocalDate.now(),
                                LocalTime.NOON, BigDecimal.ONE, "Living Room", null, "0123", List.of(), "CASH",
                                "CANCELLED",
                                OffsetDateTime.now(), OffsetDateTime.now(), null, false);
                org.mockito.Mockito.doReturn(dto)
                                .when(cleaningRequestService)
                                .cancelRequest(any(UUID.class), eq(reqId));

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authResident);
                mockMvc.perform(patch("/api/cleaning-requests/" + reqId + "/cancel")
                                .with(securityContext(ctx))
                                .principal(authResident)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void shouldGetPendingCleaningRequests_Admin() throws Exception {
                Mockito.when(authz.canManageServiceRequests()).thenReturn(true);
                Mockito.when(cleaningRequestService.getPendingRequests()).thenReturn(List.of());

                mockMvc.perform(get("/api/cleaning-requests/admin/pending")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }
}
