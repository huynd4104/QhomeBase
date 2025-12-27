package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.VehicleCreateDto;
import com.QhomeBase.baseservice.dto.VehicleDto;
import com.QhomeBase.baseservice.dto.VehicleUpdateDto;
import com.QhomeBase.baseservice.model.VehicleKind;
import com.QhomeBase.baseservice.security.AuthzService;
import com.QhomeBase.baseservice.security.JwtAuthFilter;
import com.QhomeBase.baseservice.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = VehicleController.class)
@AutoConfigureMockMvc(addFilters = false)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VehicleService vehicleService;

    @MockitoBean
    private AuthzService authz;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private UUID id;
    private UUID residentId;
    private UUID unitId;
    private VehicleDto sample;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        residentId = UUID.randomUUID();
        unitId = UUID.randomUUID();
        sample = new VehicleDto(
                id,
                residentId,
                null,
                unitId,
                "U-101",
                "ABC123",
                VehicleKind.CAR,
                "Red",
                true,
                null,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now());
    }

    @Test
    void shouldCreateVehicle() throws Exception {
        Mockito.when(authz.canCreateVehicle()).thenReturn(true);
        Mockito.when(vehicleService.createVehicle(any(VehicleCreateDto.class))).thenReturn(sample);

        var body = new VehicleCreateDto(residentId, unitId, "ABC123", VehicleKind.CAR, "Red");

        mockMvc.perform(post("/api/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.plateNo").value("ABC123"))
                .andExpect(jsonPath("$.kind").value("CAR"));
    }

    @Test
    void shouldUpdateVehicle() throws Exception {
        Mockito.when(authz.canUpdateVehicle(any(UUID.class))).thenReturn(true);
        Mockito.when(vehicleService.updateVehicle(any(VehicleUpdateDto.class), any(UUID.class))).thenReturn(sample);

        var body = new VehicleUpdateDto(residentId, unitId, "DEF456", VehicleKind.MOTORBIKE, "Blue", true);

        mockMvc.perform(put("/api/vehicles/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.plateNo").value("ABC123"));
    }

    @Test
    void shouldDeleteVehicle() throws Exception {
        Mockito.when(authz.canDeleteVehicle(any(UUID.class))).thenReturn(true);
        Mockito.doNothing().when(vehicleService).deleteVehicle(any(UUID.class));

        mockMvc.perform(delete("/api/vehicles/" + id))
                .andExpect(status().isOk());

        verify(vehicleService, times(1)).deleteVehicle(any(UUID.class));
    }

    @Test
    void shouldGetVehicleById() throws Exception {
        Mockito.when(authz.canViewVehicle(any(UUID.class))).thenReturn(true);
        Mockito.when(vehicleService.getVehicleById(any(UUID.class))).thenReturn(sample);

        mockMvc.perform(get("/api/vehicles/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.plateNo").value("ABC123"));
    }

    @Test
    void shouldGetAllVehicles() throws Exception {
        Mockito.when(authz.canViewVehicles()).thenReturn(true);
        Mockito.when(vehicleService.getAllVehicles()).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    void shouldGetVehiclesByResident() throws Exception {
        Mockito.when(authz.canViewVehiclesByResident(any(UUID.class))).thenReturn(true);
        Mockito.when(vehicleService.getVehiclesByResidentId(any(UUID.class))).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/vehicles/resident/" + residentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].residentId").value(residentId.toString()));
    }

    @Test
    void shouldGetVehiclesByUnit() throws Exception {
        Mockito.when(authz.canViewVehiclesByUnit(any(UUID.class))).thenReturn(true);
        Mockito.when(vehicleService.getVehiclesByUnitId(any(UUID.class))).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/vehicles/unit/" + unitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].unitId").value(unitId.toString()));
    }

    @Test
    void shouldGetActiveVehicles() throws Exception {
        Mockito.when(authz.canViewVehicles()).thenReturn(true);
        Mockito.when(vehicleService.getActiveVehicles()).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/vehicles/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void shouldChangeVehicleStatus() throws Exception {
        Mockito.when(authz.canManageVehicleStatus(any(UUID.class))).thenReturn(true);
        Mockito.doNothing().when(vehicleService).changeVehicleStatus(any(UUID.class), Mockito.anyBoolean());

        mockMvc.perform(patch("/api/vehicles/" + id + "/status").param("active", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetVehicleKinds() throws Exception {
        mockMvc.perform(get("/api/vehicles/kinds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("CAR"))
                .andExpect(jsonPath("$[1]").value("MOTORBIKE"))
                .andExpect(jsonPath("$[2]").value("BICYCLE"))
                .andExpect(jsonPath("$[3]").value("OTHER"));
    }
}
