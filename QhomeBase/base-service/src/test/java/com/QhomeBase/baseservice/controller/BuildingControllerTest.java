package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.*;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.model.BuildingStatus;
import com.QhomeBase.baseservice.security.AuthzService;
import com.QhomeBase.baseservice.security.JwtAuthFilter;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.BuildingDeletionService;
import com.QhomeBase.baseservice.service.BuildingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = buildingController.class)
@AutoConfigureMockMvc(addFilters = false)
class BuildingControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private BuildingService buildingService;

        @MockitoBean
        private BuildingDeletionService buildingDeletionService;

        @MockitoBean(name = "authz")
        private AuthzService authzService;

        @MockitoBean
        private JwtAuthFilter jwtAuthFilter;

        private UsernamePasswordAuthenticationToken auth;
        private UUID buildingId;

        @BeforeEach
        void setup() {
                buildingId = UUID.randomUUID();
                var principal = new UserPrincipal(UUID.randomUUID(), "adminUser", List.of("ADMIN"), List.of(), "token");
                auth = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        @Test
        void shouldFindAll_whenBuildingsExist() throws Exception {
                var b1 = Building.builder().id(UUID.randomUUID()).code("A").name("A1").address("addr1")
                                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
                var b2 = Building.builder().id(UUID.randomUUID()).code("B").name("B1").address("addr2")
                                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
                Mockito.when(buildingService.findAllOrderByCodeAsc()).thenReturn(List.of(b1, b2));

                mockMvc.perform(get("/api/buildings"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].code", is("A")))
                                .andExpect(jsonPath("$[1].code", is("B")));
        }

        @Test
        void shouldGetBuildingById_whenExists() throws Exception {
                var dto = new BuildingDto(buildingId, "A", "A1", "addr1", 10, 100, 90);
                Mockito.when(buildingService.getBuildingById(eq(buildingId))).thenReturn(dto);

                mockMvc.perform(get("/api/buildings/{buildingId}", buildingId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(buildingId.toString())))
                                .andExpect(jsonPath("$.code", is("A")))
                                .andExpect(jsonPath("$.name", is("A1")))
                                .andExpect(jsonPath("$.address", is("addr1")));
        }

        @Test
        void shouldReturn404_whenGetBuildingByIdNotFound() throws Exception {
                Mockito.when(buildingService.getBuildingById(eq(buildingId)))
                                .thenThrow(new IllegalArgumentException("not found"));

                mockMvc.perform(get("/api/buildings/{buildingId}", buildingId))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldCreateBuilding_whenAuthorized() throws Exception {
                Mockito.when(authzService.canCreateBuilding()).thenReturn(true);
                var req = new BuildingCreateReq("New Building", "Addr", 5);
                var dto = new BuildingDto(UUID.randomUUID(), "C", "New Building", "Addr", 5, 0, 0);
                Mockito.when(buildingService.createBuilding(eq(req), eq("adminUser"))).thenReturn(dto);

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);

                mockMvc.perform(post("/api/buildings")
                                .with(securityContext(ctx)).principal(auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is("C")))
                                .andExpect(jsonPath("$.name", is("New Building")));

                ArgumentCaptor<BuildingCreateReq> captor = ArgumentCaptor.forClass(BuildingCreateReq.class);
                verify(buildingService, times(1)).createBuilding(captor.capture(), eq("adminUser"));
        }

        @Test
        void shouldUpdateBuilding_whenAuthorized() throws Exception {
                Mockito.when(authzService.canUpdateBuilding()).thenReturn(true);
                var req = new BuildingUpdateReq("Upd", "Addr2", 7);
                var dto = new BuildingDto(buildingId, "A", "Upd", "Addr2", 7, 100, 95);
                Mockito.when(buildingService.updateBuilding(eq(buildingId), eq(req), any())).thenReturn(dto);

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);

                mockMvc.perform(put("/api/buildings/{buildingId}", buildingId)
                                .with(securityContext(ctx)).principal(auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name", is("Upd")))
                                .andExpect(jsonPath("$.address", is("Addr2")));
        }

        @Test
        void shouldReturn404_whenUpdateBuildingNotFound() throws Exception {
                Mockito.when(authzService.canUpdateBuilding()).thenReturn(true);
                var req = new BuildingUpdateReq("Upd", "Addr2", 7);
                Mockito.when(buildingService.updateBuilding(eq(buildingId), eq(req), any()))
                                .thenThrow(new RuntimeException("nf"));

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);

                mockMvc.perform(put("/api/buildings/{buildingId}", buildingId)
                                .with(securityContext(ctx)).principal(auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldChangeStatus_whenAuthorized() throws Exception {
                Mockito.when(authzService.canUpdateBuilding()).thenReturn(true);
                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);

                mockMvc.perform(patch("/api/buildings/{buildingId}/status", buildingId)
                                .with(securityContext(ctx)).principal(auth)
                                .param("status", BuildingStatus.INACTIVE.name()))
                                .andExpect(status().isOk());

                verify(buildingService, times(1)).changeBuildingStatus(eq(buildingId), eq(BuildingStatus.INACTIVE),
                                any());
        }

        @Test
        void shouldReturn404_whenChangeStatusInvalid() throws Exception {
                Mockito.when(authzService.canUpdateBuilding()).thenReturn(true);
                Mockito.doThrow(new IllegalArgumentException("bad")).when(buildingService).changeBuildingStatus(
                                eq(buildingId),
                                eq(BuildingStatus.INACTIVE), any());

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);

                mockMvc.perform(patch("/api/buildings/{buildingId}/status", buildingId)
                                .with(securityContext(ctx)).principal(auth)
                                .param("status", BuildingStatus.INACTIVE.name()))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldCreateDeletionRequest_whenAuthorized() throws Exception {
                Mockito.when(authzService.canRequestDeleteBuilding(eq(buildingId))).thenReturn(true);
                var dto = new BuildingDeletionRequestDto(UUID.randomUUID(), buildingId, UUID.randomUUID(), "reason",
                                null, null,
                                com.QhomeBase.baseservice.model.BuildingDeletionStatus.PENDING, OffsetDateTime.now(),
                                null);
                Mockito.when(buildingDeletionService.createBuildingDeletionRequest(eq(buildingId), eq("reason"), any()))
                                .thenReturn(dto);

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);
                var body = new BuildingDeletionCreateReq(buildingId, "reason");

                mockMvc.perform(post("/api/buildings/{buildingId}/deletion-request", buildingId)
                                .with(securityContext(ctx)).principal(auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.buildingId", is(buildingId.toString())));
        }

        @Test
        void shouldReturn400_whenCreateDeletionRequestInvalid() throws Exception {
                Mockito.when(authzService.canRequestDeleteBuilding(eq(buildingId))).thenReturn(true);
                Mockito.when(buildingDeletionService.createBuildingDeletionRequest(eq(buildingId), eq("reason"), any()))
                                .thenThrow(new IllegalArgumentException("bad"));

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);
                var body = new BuildingDeletionCreateReq(buildingId, "reason");

                mockMvc.perform(post("/api/buildings/{buildingId}/deletion-request", buildingId)
                                .with(securityContext(ctx)).principal(auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldApproveDeletionRequest_whenAuthorized() throws Exception {
                Mockito.when(authzService.canApproveBuildingDeletion()).thenReturn(true);
                UUID requestId = UUID.randomUUID();
                var dto = new BuildingDeletionRequestDto(requestId, buildingId, UUID.randomUUID(), "r",
                                UUID.randomUUID(),
                                "note", com.QhomeBase.baseservice.model.BuildingDeletionStatus.APPROVED,
                                OffsetDateTime.now(),
                                OffsetDateTime.now());
                Mockito.when(buildingDeletionService.approveBuildingDeletionRequest(eq(requestId), eq("note"), any()))
                                .thenReturn(dto);

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);
                var body = new BuildingDeletionApproveReq("note");

                mockMvc.perform(post("/api/buildings/deletion-requests/{requestId}/approve", requestId)
                                .with(securityContext(ctx)).principal(auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", is("APPROVED")));
        }

        @Test
        void shouldRejectDeletionRequest_whenAuthorized() throws Exception {
                Mockito.when(authzService.canApproveBuildingDeletion()).thenReturn(true);
                UUID requestId = UUID.randomUUID();
                var dto = new BuildingDeletionRequestDto(requestId, buildingId, UUID.randomUUID(), "r",
                                UUID.randomUUID(),
                                "note", com.QhomeBase.baseservice.model.BuildingDeletionStatus.REJECTED,
                                OffsetDateTime.now(),
                                OffsetDateTime.now());
                Mockito.when(buildingDeletionService.rejectBuildingDeletionRequest(eq(requestId), eq("note"), any()))
                                .thenReturn(dto);

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);
                var body = new BuildingDeletionRejectReq("note");

                mockMvc.perform(post("/api/buildings/deletion-requests/{requestId}/reject", requestId)
                                .with(securityContext(ctx)).principal(auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", is("REJECTED")));
        }

        @Test
        void shouldGetPendingDeletionRequests_whenAuthorized() throws Exception {
                Mockito.when(authzService.canViewAllDeleteBuildings()).thenReturn(true);
                var dto = new BuildingDeletionRequestDto(UUID.randomUUID(), buildingId, UUID.randomUUID(), "r", null,
                                null,
                                com.QhomeBase.baseservice.model.BuildingDeletionStatus.PENDING, OffsetDateTime.now(),
                                null);
                Mockito.when(buildingDeletionService.getPendingRequests()).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/buildings/deletion-requests/pending"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void shouldGetBuildingDeletionRequestById() throws Exception {
                UUID requestId = UUID.randomUUID();
                var dto = new BuildingDeletionRequestDto(requestId, buildingId, UUID.randomUUID(), "r", null, null,
                                com.QhomeBase.baseservice.model.BuildingDeletionStatus.PENDING, OffsetDateTime.now(),
                                null);
                Mockito.when(buildingDeletionService.getById(eq(requestId))).thenReturn(dto);

                mockMvc.perform(get("/api/buildings/deletion-requests/{requestId}", requestId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(requestId.toString())));
        }

        @Test
        void shouldReturn404_whenGetBuildingDeletionRequestNotFound() throws Exception {
                UUID requestId = UUID.randomUUID();
                Mockito.when(buildingDeletionService.getById(eq(requestId)))
                                .thenThrow(new IllegalArgumentException("nf"));

                mockMvc.perform(get("/api/buildings/deletion-requests/{requestId}", requestId))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldDoBuildingDeletion_whenAuthorized() throws Exception {
                Mockito.when(authzService.canRequestDeleteBuilding(eq(buildingId))).thenReturn(true);
                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);

                mockMvc.perform(post("/api/buildings/{buildingId}/do", buildingId)
                                .with(securityContext(ctx)).principal(auth))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Building deletion completed successfully"));

                verify(buildingDeletionService, times(1)).doBuildingDeletion(eq(buildingId), any());
        }

        @Test
        void shouldGetDeletingBuildings() throws Exception {
                Mockito.when(authzService.canViewAllDeleteBuildings()).thenReturn(true);
                var dto = new BuildingDeletionRequestDto(UUID.randomUUID(), buildingId, UUID.randomUUID(), "r", null,
                                null,
                                com.QhomeBase.baseservice.model.BuildingDeletionStatus.PENDING, OffsetDateTime.now(),
                                null);
                Mockito.when(buildingDeletionService.getDeletingBuildings()).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/buildings/deleting"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void shouldGetAllDeletionRequests() throws Exception {
                Mockito.when(authzService.canViewAllDeleteBuildings()).thenReturn(true);
                var dto = new BuildingDeletionRequestDto(UUID.randomUUID(), buildingId, UUID.randomUUID(), "r", null,
                                null,
                                com.QhomeBase.baseservice.model.BuildingDeletionStatus.PENDING, OffsetDateTime.now(),
                                null);
                Mockito.when(buildingDeletionService.getAllBuildingDeletionRequests()).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/buildings/all"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void shouldGetMyDeletingBuildings() throws Exception {
                var dto = new BuildingDeletionRequestDto(UUID.randomUUID(), buildingId, UUID.randomUUID(), "r", null,
                                null,
                                com.QhomeBase.baseservice.model.BuildingDeletionStatus.PENDING, OffsetDateTime.now(),
                                null);
                Mockito.when(buildingDeletionService.getDeletingBuildings()).thenReturn(List.of(dto));
                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);

                mockMvc.perform(get("/api/buildings/my-deleting-buildings").with(securityContext(ctx)).principal(auth))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void shouldGetMyAllDeletionRequests() throws Exception {
                var dto = new BuildingDeletionRequestDto(UUID.randomUUID(), buildingId, UUID.randomUUID(), "r", null,
                                null,
                                com.QhomeBase.baseservice.model.BuildingDeletionStatus.PENDING, OffsetDateTime.now(),
                                null);
                Mockito.when(buildingDeletionService.getAllBuildingDeletionRequests()).thenReturn(List.of(dto));
                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);

                mockMvc.perform(get("/api/buildings/my-all").with(securityContext(ctx)).principal(auth))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void shouldGetMyDeletingBuildingsRaw() throws Exception {
                var b = Building.builder().id(UUID.randomUUID()).code("Z").name("Z1").address("adr").build();
                Mockito.when(buildingDeletionService.getDeletingBuildingsRaw()).thenReturn(List.of(b));
                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);

                mockMvc.perform(get("/api/buildings/my-deleting-buildings-raw").with(securityContext(ctx))
                                .principal(auth))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].code", is("Z")));
        }

        @Test
        void shouldGetTargetsStatus() throws Exception {
                Mockito.when(buildingDeletionService.getBuildingDeletionTargetsStatus(eq(buildingId)))
                                .thenReturn(Map.of(
                                                "units", Map.of("INACTIVE", 5L),
                                                "totalUnits", 5,
                                                "unitsInactive", 5,
                                                "unitsReady", true,
                                                "allTargetsReady", true));

                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);

                mockMvc.perform(get("/api/buildings/{buildingId}/targets-status", buildingId).with(securityContext(ctx))
                                .principal(auth))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalUnits", is(5)))
                                .andExpect(jsonPath("$.allTargetsReady", is(true)));
        }

        @Test
        void shouldCompleteDeletion_whenAuthorized() throws Exception {
                Mockito.when(authzService.canCompleteBuildingDeletion()).thenReturn(true);
                UUID requestId = UUID.randomUUID();
                var dto = new BuildingDeletionRequestDto(requestId, buildingId, UUID.randomUUID(), "r", null, null,
                                com.QhomeBase.baseservice.model.BuildingDeletionStatus.COMPLETED, OffsetDateTime.now(),
                                OffsetDateTime.now());
                Mockito.when(buildingDeletionService.completeBuildingDeletion(eq(requestId), any())).thenReturn(dto);
                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(auth);

                mockMvc.perform(post("/api/buildings/{requestId}/complete", requestId)
                                .with(securityContext(ctx)).principal(auth))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", is("COMPLETED")));
        }
}
