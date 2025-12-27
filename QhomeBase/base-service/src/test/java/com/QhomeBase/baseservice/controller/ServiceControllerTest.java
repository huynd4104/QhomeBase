package com.QhomeBase.baseservice.controller;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.QhomeBase.baseservice.model.Service;
import com.QhomeBase.baseservice.model.ServiceType;
import com.QhomeBase.baseservice.model.ServiceUnit;
import com.QhomeBase.baseservice.repository.ServiceRepository;
import com.QhomeBase.baseservice.security.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ServiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceRepository serviceRepository;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private Service sample(UUID id, String code, boolean active) {
        return Service.builder()
                .id(id)
                .code(code)
                .name("Water")
                .nameEn("Water")
                .type(ServiceType.UTILITY)
                .unit(ServiceUnit.M3)
                .unitLabel("m3")
                .billable(true)
                .requiresMeter(true)
                .active(active)
                .description("desc")
                .displayOrder(1)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void shouldGetAllServices() throws Exception {
        var s1 = sample(UUID.randomUUID(), "WATER", true);
        Mockito.when(serviceRepository.findAll()).thenReturn(List.of(s1));

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("WATER"));
    }

    @Test
    void shouldGetServiceById_found() throws Exception {
        UUID id = UUID.randomUUID();
        var s1 = sample(id, "ELECTRIC", true);
        Mockito.when(serviceRepository.findById(eq(id))).thenReturn(Optional.of(s1));

        mockMvc.perform(get("/api/services/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("ELECTRIC"));
    }

    @Test
    void shouldGetServiceById_notFound() throws Exception {
        UUID id = UUID.randomUUID();
        Mockito.when(serviceRepository.findById(eq(id))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/services/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetServiceByCode_found() throws Exception {
        var s1 = sample(UUID.randomUUID(), "CODE1", true);
        Mockito.when(serviceRepository.findByCode(eq("CODE1"))).thenReturn(Optional.of(s1));

        mockMvc.perform(get("/api/services/code/{code}", "CODE1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CODE1"));
    }

    @Test
    void shouldGetServiceByCode_notFound() throws Exception {
        Mockito.when(serviceRepository.findByCode(eq("MISS"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/services/code/{code}", "MISS"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetActiveServices() throws Exception {
        var s1 = sample(UUID.randomUUID(), "ACTIVE1", true);
        Mockito.when(serviceRepository.findByActive(eq(true))).thenReturn(List.of(s1));

        mockMvc.perform(get("/api/services/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ACTIVE1"));
    }
}
