package com.QhomeBase.assetmaintenanceservice.controller;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.QhomeBase.assetmaintenanceservice.dto.service.*;
import com.QhomeBase.assetmaintenanceservice.security.AuthzService;
import com.QhomeBase.assetmaintenanceservice.security.JwtAuthFilter;
import com.QhomeBase.assetmaintenanceservice.service.ServiceConfigService;
import com.QhomeBase.assetmaintenanceservice.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ServiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class ServiceControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private ServiceConfigService serviceConfigService;

        @MockitoBean
        private FileStorageService fileStorageService;

        @MockitoBean(name = "authz")
        private AuthzService authzService;

        @MockitoBean
        private JwtAuthFilter jwtAuthFilter;

        private ServiceDto sampleService(UUID id, String code, String name, boolean active) {
                return ServiceDto.builder()
                                .id(id)
                                .code(code)
                                .name(name)
                                .isActive(active)
                                .createdAt(OffsetDateTime.now())
                                .updatedAt(OffsetDateTime.now())
                                .build();
        }

        @Test
        void shouldGetServices() throws Exception {
                Mockito.when(authzService.canViewServiceConfig()).thenReturn(true);
                var s1 = sampleService(UUID.randomUUID(), "SV01", "Gym", true);
                Mockito.when(serviceConfigService.findAll(null)).thenReturn(List.of(s1));

                mockMvc.perform(get("/api/asset-maintenance/services"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].code").value("SV01"));
        }

        @Test
        void shouldCreateService() throws Exception {
                Mockito.when(authzService.canManageServiceConfig()).thenReturn(true);
                var req = CreateServiceRequest.builder()
                                .categoryId(UUID.randomUUID())
                                .code("SV02")
                                .name("Pool")
                                .pricingType(com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePricingType.FREE)
                                .isActive(true)
                                .build();
                var created = sampleService(UUID.randomUUID(), "SV02", "Pool", true);
                Mockito.when(serviceConfigService.create(any(CreateServiceRequest.class))).thenReturn(created);

                mockMvc.perform(post("/api/asset-maintenance/services")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("Pool"));
        }

        @Test
        void shouldGetServiceById() throws Exception {
                Mockito.when(authzService.canViewServiceConfig()).thenReturn(true);
                UUID id = UUID.randomUUID();
                var s = sampleService(id, "SV03", "Spa", true);
                Mockito.when(serviceConfigService.findById(eq(id))).thenReturn(s);

                mockMvc.perform(get("/api/asset-maintenance/services/{id}", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.code").value("SV03"));
        }

        @Test
        void shouldUpdateService() throws Exception {
                Mockito.when(authzService.canManageServiceConfig()).thenReturn(true);
                UUID id = UUID.randomUUID();
                var req = UpdateServiceRequest.builder()
                                .name("Updated")
                                .pricePerHour(BigDecimal.valueOf(50))
                                .pricingType(com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePricingType.HOURLY)
                                .build();
                var updated = sampleService(id, "SV04", "Updated", true);
                Mockito.when(serviceConfigService.update(eq(id), any(UpdateServiceRequest.class))).thenReturn(updated);

                mockMvc.perform(put("/api/asset-maintenance/services/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Updated"));
        }

        @Test
        void shouldToggleStatus() throws Exception {
                Mockito.when(authzService.canManageServiceConfig()).thenReturn(true);
                UUID id = UUID.randomUUID();
                var updated = sampleService(id, "SV05", "Svc", false);
                Mockito.when(serviceConfigService.setActive(eq(id), eq(false))).thenReturn(updated);

                mockMvc.perform(patch("/api/asset-maintenance/services/{id}/status", id)
                                .param("active", "false"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isActive").value(false));
        }

        @Test
        void shouldGetAvailabilities() throws Exception {
                Mockito.when(authzService.canViewServiceConfig()).thenReturn(true);
                UUID id = UUID.randomUUID();
                var a = ServiceAvailabilityDto.builder().id(UUID.randomUUID()).serviceId(id).dayOfWeek(1)
                                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(18, 0)).isAvailable(true).build();
                Mockito.when(serviceConfigService.findAvailability(eq(id))).thenReturn(List.of(a));

                mockMvc.perform(get("/api/asset-maintenance/services/{id}/availabilities", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].dayOfWeek").value(1));
        }

        @Test
        void shouldAddAvailability() throws Exception {
                Mockito.when(authzService.canManageServiceConfig()).thenReturn(true);
                UUID id = UUID.randomUUID();
                var req = ServiceAvailabilityRequest.builder().dayOfWeek(2).startTime(LocalTime.of(10, 0))
                                .endTime(LocalTime.of(12, 0)).isAvailable(true).build();
                var a = ServiceAvailabilityDto.builder().id(UUID.randomUUID()).serviceId(id).dayOfWeek(2)
                                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0)).isAvailable(true).build();
                Mockito.when(serviceConfigService.addAvailability(eq(id), any(ServiceAvailabilityRequest.class)))
                                .thenReturn(List.of(a));

                mockMvc.perform(post("/api/asset-maintenance/services/{id}/availabilities", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$[0].dayOfWeek").value(2));
        }

        @Test
        void shouldDeleteAvailability() throws Exception {
                Mockito.when(authzService.canManageServiceConfig()).thenReturn(true);
                UUID id = UUID.randomUUID();
                UUID avId = UUID.randomUUID();
                var a = ServiceAvailabilityDto.builder().id(avId).serviceId(id).dayOfWeek(3).isAvailable(false).build();
                Mockito.when(serviceConfigService.deleteAvailability(eq(id), eq(avId))).thenReturn(List.of(a));

                mockMvc.perform(delete("/api/asset-maintenance/services/{id}/availabilities/{avId}", id, avId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(avId.toString()));
        }

        @Test
        void shouldGetPublicServices() throws Exception {
                var s1 = sampleService(UUID.randomUUID(), "PUB1", "PublicSvc", true);
                Mockito.when(serviceConfigService.findAll(null)).thenReturn(List.of(s1));

                mockMvc.perform(get("/api/asset-maintenance/services/public"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].code").value("PUB1"));
        }
}
