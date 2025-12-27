package com.QhomeBase.baseservice.controller;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.QhomeBase.baseservice.dto.MeterCreateReq;
import com.QhomeBase.baseservice.dto.MeterDto;
import com.QhomeBase.baseservice.dto.MeterUpdateReq;
import com.QhomeBase.baseservice.dto.UnitWithoutMeterDto;
import com.QhomeBase.baseservice.security.SecurityConfig;
import com.QhomeBase.baseservice.security.JwtAuthFilter;
import com.QhomeBase.baseservice.service.MeterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MeterController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class MeterControllerTest {

        @MockitoBean
        private MeterService meterService;

        @MockitoBean
        private JwtAuthFilter jwtAuthFilter;

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;

        private MeterDto sampleMeter(UUID id, String code, boolean active) {
                UUID unitId = UUID.randomUUID();
                UUID buildingId = UUID.randomUUID();
                UUID serviceId = UUID.randomUUID();
                return new MeterDto(
                                id,
                                unitId,
                                buildingId,
                                "B01",
                                "U01",
                                1,
                                serviceId,
                                "SV01",
                                "Electric",
                                code,
                                active,
                                LocalDate.now().minusDays(10),
                                null,
                                100.0,
                                LocalDate.now().minusDays(1),
                                OffsetDateTime.now().minusDays(10),
                                OffsetDateTime.now());
        }

        @Test
        void shouldCreateMeter() throws Exception {
                var req = new MeterCreateReq(UUID.randomUUID(), UUID.randomUUID(), "M001", LocalDate.now());
                var created = sampleMeter(UUID.randomUUID(), "M001", true);
                Mockito.when(meterService.create(any(MeterCreateReq.class))).thenReturn(created);

                mockMvc.perform(post("/api/meters")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.meterCode").value("M001"));
        }

        @Test
        void shouldReturnBadRequestOnCreate() throws Exception {
                var req = new MeterCreateReq(UUID.randomUUID(), UUID.randomUUID(), "M002", LocalDate.now());
                Mockito.when(meterService.create(any(MeterCreateReq.class)))
                                .thenThrow(new IllegalArgumentException("invalid"));

                mockMvc.perform(post("/api/meters")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldGetMeterById() throws Exception {
                UUID id = UUID.randomUUID();
                var dto = sampleMeter(id, "M100", true);
                Mockito.when(meterService.getById(eq(id))).thenReturn(dto);

                mockMvc.perform(get("/api/meters/{id}", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.meterCode").value("M100"));
        }

        @Test
        void shouldReturnNotFoundOnGetById() throws Exception {
                UUID id = UUID.randomUUID();
                Mockito.when(meterService.getById(eq(id))).thenThrow(new IllegalArgumentException("not found"));

                mockMvc.perform(get("/api/meters/{id}", id))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldGetAllMetersFallback() throws Exception {
                var dto = sampleMeter(UUID.randomUUID(), "M200", true);
                Mockito.when(meterService.getAll()).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/meters"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].meterCode").value("M200"));
        }

        @Test
        void shouldGetAllMetersByUnitId() throws Exception {
                UUID unitId = UUID.randomUUID();
                var dto = sampleMeter(UUID.randomUUID(), "M201", true);
                Mockito.when(meterService.getByUnitId(eq(unitId))).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/meters").param("unitId", unitId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].meterCode").value("M201"));
        }

        @Test
        void shouldGetUnitsWithoutMeter() throws Exception {
                UUID serviceId = UUID.randomUUID();
                var item = new UnitWithoutMeterDto(UUID.randomUUID(), "U01", 1, UUID.randomUUID(), "B01", "Building",
                                serviceId,
                                "SV01", "Electric");
                Mockito.when(meterService.getUnitsDoNotHaveMeter(eq(serviceId), isNull())).thenReturn(List.of(item));

                mockMvc.perform(get("/api/meters/missing").param("serviceId", serviceId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].unitCode").value("U01"));
        }

        @Test
        void shouldReturnBadRequestOnGetUnitsWithoutMeter() throws Exception {
                UUID serviceId = UUID.randomUUID();
                Mockito.when(meterService.getUnitsDoNotHaveMeter(eq(serviceId), isNull()))
                                .thenThrow(new IllegalArgumentException("bad"));

                mockMvc.perform(get("/api/meters/missing").param("serviceId", serviceId.toString()))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldCreateMissingMeters() throws Exception {
                UUID serviceId = UUID.randomUUID();
                var dto = sampleMeter(UUID.randomUUID(), "M300", true);
                Mockito.when(meterService.createMissingMeters(eq(serviceId), isNull())).thenReturn(List.of(dto));

                mockMvc.perform(post("/api/meters/missing").param("serviceId", serviceId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].meterCode").value("M300"));
        }

        @Test
        void shouldUpdateMeter() throws Exception {
                UUID id = UUID.randomUUID();
                var req = new MeterUpdateReq("M400", true, null);
                var updated = sampleMeter(id, "M400", true);
                Mockito.when(meterService.update(eq(id), any(MeterUpdateReq.class))).thenReturn(updated);

                mockMvc.perform(put("/api/meters/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.meterCode").value("M400"));
        }

        @Test
        void shouldReturnNotFoundOnUpdate() throws Exception {
                UUID id = UUID.randomUUID();
                var req = new MeterUpdateReq("M401", true, null);
                Mockito.when(meterService.update(eq(id), any(MeterUpdateReq.class)))
                                .thenThrow(new IllegalArgumentException("nf"));

                mockMvc.perform(put("/api/meters/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldDeactivateMeter() throws Exception {
                UUID id = UUID.randomUUID();

                mockMvc.perform(patch("/api/meters/{id}/deactivate", id))
                                .andExpect(status().isOk());
                Mockito.verify(meterService).deactivate(eq(id));
        }

        @Test
        void shouldReturnNotFoundOnDeactivate() throws Exception {
                UUID id = UUID.randomUUID();
                Mockito.doThrow(new IllegalArgumentException("nf")).when(meterService).deactivate(eq(id));

                mockMvc.perform(patch("/api/meters/{id}/deactivate", id))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldDeleteMeter() throws Exception {
                UUID id = UUID.randomUUID();

                mockMvc.perform(delete("/api/meters/{id}", id))
                                .andExpect(status().isOk());
                Mockito.verify(meterService).delete(eq(id));
        }

        @Test
        void shouldReturnNotFoundOnDelete() throws Exception {
                UUID id = UUID.randomUUID();
                Mockito.doThrow(new IllegalArgumentException("nf")).when(meterService).delete(eq(id));

                mockMvc.perform(delete("/api/meters/{id}", id))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnBadRequestOnDelete() throws Exception {
                UUID id = UUID.randomUUID();
                Mockito.doThrow(new IllegalStateException("bad")).when(meterService).delete(eq(id));

                mockMvc.perform(delete("/api/meters/{id}", id))
                                .andExpect(status().isBadRequest());
        }
}
