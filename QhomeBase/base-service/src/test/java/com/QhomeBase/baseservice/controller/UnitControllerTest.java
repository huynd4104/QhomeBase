package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.UnitCreateDto;
import com.QhomeBase.baseservice.dto.UnitDto;
import com.QhomeBase.baseservice.dto.UnitUpdateDto;
import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.security.AuthzService;
import com.QhomeBase.baseservice.security.JwtAuthFilter;
import com.QhomeBase.baseservice.service.UnitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UnitController.class)
@AutoConfigureMockMvc(addFilters = false)
class UnitControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private UnitService unitService;

        @MockitoBean(name = "authz")
        private AuthzService authzService;

        @MockitoBean
        private JwtAuthFilter jwtAuthFilter;

        private UUID unitId;
        private UUID buildingId;

        @BeforeEach
        void setup() {
                unitId = UUID.fromString("6a5b4c3d-2e1f-0a9b-8c7d-6e5f4a3b2c1d");
                buildingId = UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75");
        }

        @Test
        @WithMockUser
        void shouldCreateUnit_whenAuthorized() throws Exception {
                Mockito.when(authzService.canCreateUnit(eq(buildingId))).thenReturn(true);
                var dto = new UnitCreateDto(buildingId, "A-101", 10, java.math.BigDecimal.valueOf(90), 2);
                var result = new UnitDto(
                                unitId,
                                buildingId,
                                "BLD-A",
                                "Tower A",
                                "A-101",
                                10,
                                java.math.BigDecimal.valueOf(90),
                                2,
                                UnitStatus.ACTIVE,
                                null,
                                java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now());
                Mockito.when(unitService.createUnit(eq(dto))).thenReturn(result);

                mockMvc.perform(post("/api/units")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(unitId.toString())))
                                .andExpect(jsonPath("$.code", is("A-101")))
                                .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @WithMockUser
        void shouldReturnBadRequest_whenCreateUnitIllegalArgument() throws Exception {
                Mockito.when(authzService.canCreateUnit(eq(buildingId))).thenReturn(true);
                var dto = new UnitCreateDto(buildingId, "A-101", 10, java.math.BigDecimal.valueOf(90), 2);
                Mockito.when(unitService.createUnit(eq(dto))).thenThrow(new IllegalArgumentException("bad"));

                mockMvc.perform(post("/api/units")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        void shouldReturnBadRequest_whenCreateUnitIllegalState() throws Exception {
                Mockito.when(authzService.canCreateUnit(eq(buildingId))).thenReturn(true);
                var dto = new UnitCreateDto(buildingId, "A-101", 10, java.math.BigDecimal.valueOf(90), 2);
                Mockito.when(unitService.createUnit(eq(dto))).thenThrow(new IllegalStateException("bad"));

                mockMvc.perform(post("/api/units")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        void shouldUpdateUnit_whenAuthorized() throws Exception {
                Mockito.when(authzService.canUpdateUnit(eq(unitId))).thenReturn(true);
                var update = new UnitUpdateDto(11, java.math.BigDecimal.valueOf(95), 2);
                var result = new UnitDto(
                                unitId,
                                buildingId,
                                "BLD-A",
                                "Tower A",
                                "A-102",
                                11,
                                java.math.BigDecimal.valueOf(95),
                                2,
                                UnitStatus.VACANT,
                                null,
                                java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now());
                Mockito.when(unitService.updateUnit(eq(update), eq(unitId))).thenReturn(result);

                mockMvc.perform(put("/api/units/{id}", unitId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(update)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is("A-102")))
                                .andExpect(jsonPath("$.status", is("VACANT")));
        }

        @Test
        @WithMockUser
        void shouldReturn404_whenUpdateUnitNotFound() throws Exception {
                Mockito.when(authzService.canUpdateUnit(eq(unitId))).thenReturn(true);
                var update = new UnitUpdateDto(11, java.math.BigDecimal.valueOf(95), 2);
                Mockito.when(unitService.updateUnit(eq(update), eq(unitId)))
                                .thenThrow(new IllegalArgumentException("Unit not found: " + unitId));

                mockMvc.perform(put("/api/units/{id}", unitId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(update)))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldDeleteUnit_whenAuthorized() throws Exception {
                Mockito.when(authzService.canDeleteUnit(eq(unitId))).thenReturn(true);

                mockMvc.perform(delete("/api/units/{id}", unitId))
                                .andExpect(status().isOk());

                verify(unitService, times(1)).deleteUnit(eq(unitId));
        }

        @Test
        @WithMockUser
        void shouldReturn404_whenDeleteUnitNotFound() throws Exception {
                Mockito.when(authzService.canDeleteUnit(eq(unitId))).thenReturn(true);
                Mockito.doThrow(new IllegalArgumentException("nf")).when(unitService).deleteUnit(eq(unitId));

                mockMvc.perform(delete("/api/units/{id}", unitId))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldGetUnitById_whenAuthorized() throws Exception {
                Mockito.when(authzService.canViewUnit(eq(unitId))).thenReturn(true);
                var result = new UnitDto(
                                unitId,
                                buildingId,
                                "BLD-A",
                                "Tower A",
                                "A-101",
                                10,
                                java.math.BigDecimal.valueOf(90),
                                2,
                                UnitStatus.ACTIVE,
                                null,
                                java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now());
                Mockito.when(unitService.getUnitById(eq(unitId))).thenReturn(result);

                mockMvc.perform(get("/api/units/{id}", unitId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(unitId.toString())));
        }

        @Test
        @WithMockUser
        void shouldReturn404_whenGetUnitByIdNotFound() throws Exception {
                Mockito.when(authzService.canViewUnit(eq(unitId))).thenReturn(true);
                Mockito.when(unitService.getUnitById(eq(unitId))).thenThrow(new IllegalArgumentException("nf"));

                mockMvc.perform(get("/api/units/{id}", unitId))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldGetUnitsByBuildingId_whenAuthorized() throws Exception {
                Mockito.when(authzService.canViewUnitsByBuilding(eq(buildingId))).thenReturn(true);
                var result = List.of(
                                new UnitDto(UUID.randomUUID(), buildingId, "BLD-A", "Tower A", "A-101", 10,
                                                java.math.BigDecimal.valueOf(90), 2, UnitStatus.ACTIVE, null,
                                                java.time.OffsetDateTime.now(),
                                                java.time.OffsetDateTime.now()),
                                new UnitDto(UUID.randomUUID(), buildingId, "BLD-A", "Tower A", "A-102", 10,
                                                java.math.BigDecimal.valueOf(95), 2, UnitStatus.VACANT, null,
                                                java.time.OffsetDateTime.now(),
                                                java.time.OffsetDateTime.now()));
                Mockito.when(unitService.getUnitsByBuildingId(eq(buildingId))).thenReturn(result);

                mockMvc.perform(get("/api/units/building/{buildingId}", buildingId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].code", is("A-101")))
                                .andExpect(jsonPath("$[1].code", is("A-102")));
        }

        @Test
        @WithMockUser
        void shouldGetUnitsByFloor_whenAuthorized() throws Exception {
                Mockito.when(authzService.canViewUnits()).thenReturn(true);
                var result = List.of(
                                new UnitDto(UUID.randomUUID(), buildingId, "BLD-A", "Tower A", "A-201", 20,
                                                java.math.BigDecimal.valueOf(90), 2, UnitStatus.ACTIVE, null,
                                                java.time.OffsetDateTime.now(),
                                                java.time.OffsetDateTime.now()));
                Mockito.when(unitService.getUnitsByFloor(eq(buildingId), eq(20))).thenReturn(result);

                mockMvc.perform(get("/api/units/building/{buildingId}/floor/{floor}", buildingId, 20))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].code", is("A-201")));
        }

        @Test
        @WithMockUser
        void shouldChangeUnitStatus_whenAuthorized() throws Exception {
                Mockito.when(authzService.canManageUnitStatus(eq(unitId))).thenReturn(true);

                mockMvc.perform(patch("/api/units/{id}/status", unitId)
                                .param("status", UnitStatus.MAINTENANCE.name()))
                                .andExpect(status().isOk());

                verify(unitService, times(1)).changeUnitStatus(eq(unitId), eq(UnitStatus.MAINTENANCE));
        }

        @Test
        @WithMockUser
        void shouldReturn404_whenChangeUnitStatusInvalid() throws Exception {
                Mockito.when(authzService.canManageUnitStatus(eq(unitId))).thenReturn(true);
                Mockito.doThrow(new IllegalArgumentException("nf")).when(unitService).changeUnitStatus(eq(unitId),
                                eq(UnitStatus.MAINTENANCE));

                mockMvc.perform(patch("/api/units/{id}/status", unitId)
                                .param("status", UnitStatus.MAINTENANCE.name()))
                                .andExpect(status().isNotFound());
        }
}
