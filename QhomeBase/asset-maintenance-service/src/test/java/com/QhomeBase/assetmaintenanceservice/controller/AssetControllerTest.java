package com.QhomeBase.assetmaintenanceservice.controller;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.QhomeBase.assetmaintenanceservice.dto.asset.AssetResponse;
import com.QhomeBase.assetmaintenanceservice.dto.asset.CreateAssetRequest;
import com.QhomeBase.assetmaintenanceservice.dto.asset.UpdateAssetRequest;
import com.QhomeBase.assetmaintenanceservice.model.AssetStatus;
import com.QhomeBase.assetmaintenanceservice.model.AssetType;
import com.QhomeBase.assetmaintenanceservice.security.AuthzService;
import com.QhomeBase.assetmaintenanceservice.security.UserPrincipal;
import com.QhomeBase.assetmaintenanceservice.security.JwtAuthFilter;
import com.QhomeBase.assetmaintenanceservice.service.AssetService;
import com.QhomeBase.assetmaintenanceservice.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AssetController.class)
@AutoConfigureMockMvc(addFilters = false)
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssetService assetService;

    @MockitoBean(name = "authz")
    private AuthzService authzService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private FileStorageService fileStorageService;

    private AssetResponse sampleAsset(UUID id, UUID buildingId, UUID unitId, String code, String name) {
        return AssetResponse.builder()
                .id(id)
                .buildingId(buildingId)
                .unitId(unitId)
                .code(code)
                .name(name)
                .assetType(AssetType.WATER_PUMP)
                .status(AssetStatus.ACTIVE)
                .currentValue(BigDecimal.valueOf(100))
                .createdBy("admin")
                .createdAt(Instant.now())
                .updatedBy("admin")
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldGetAssets_page() throws Exception {
        Mockito.when(authzService.canViewAsset()).thenReturn(true);
        UUID buildingId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        var asset = sampleAsset(UUID.randomUUID(), buildingId, unitId, "AS01", "Pump");
        var page = new PageImpl<>(List.of(asset), PageRequest.of(0, 20), 1);
        Mockito.when(assetService.getAllAssets(any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/asset-maintenance/assets")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("AS01"));
    }

    @Test
    void shouldGetAssetById() throws Exception {
        Mockito.when(authzService.canViewAsset()).thenReturn(true);
        UUID id = UUID.randomUUID();
        var asset = sampleAsset(id, UUID.randomUUID(), UUID.randomUUID(), "AS02", "Sensor");
        Mockito.when(assetService.getAssetById(eq(id))).thenReturn(asset);

        mockMvc.perform(get("/api/asset-maintenance/assets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("AS02"));
    }

    @Test
    void shouldCreateAsset() throws Exception {
        Mockito.when(authzService.canCreateAsset()).thenReturn(true);
        UUID userId = UUID.randomUUID();
        var principal = new UserPrincipal(userId, "admin", java.util.List.of("ADMIN"), java.util.List.of(), "token");
        var authorities = java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        var authAdmin = new UsernamePasswordAuthenticationToken(principal, "token", authorities);
        var ctx = new org.springframework.security.core.context.SecurityContextImpl();
        ctx.setAuthentication(authAdmin);

        var req = CreateAssetRequest.builder()
                .buildingId(UUID.randomUUID())
                .code("AS01")
                .name("Pump")
                .assetType(AssetType.WATER_PUMP)
                .status(AssetStatus.ACTIVE)
                .build();

        var result = sampleAsset(UUID.randomUUID(), req.getBuildingId(), null, "AS01", "Pump");
        Mockito.when(assetService.create(any(CreateAssetRequest.class), any())).thenReturn(result);

        mockMvc.perform(post("/api/asset-maintenance/assets")
                .with(securityContext(ctx))
                .principal(authAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("AS01"));
    }

    @Test
    void shouldUpdateAsset() throws Exception {
        Mockito.when(authzService.canUpdateAsset()).thenReturn(true);
        UUID id = UUID.randomUUID();
        var req = UpdateAssetRequest.builder().name("New Name").build();
        var updated = sampleAsset(id, UUID.randomUUID(), null, "AS01", "New Name");
        Mockito.when(assetService.update(eq(id), any(UpdateAssetRequest.class), any())).thenReturn(updated);

        mockMvc.perform(put("/api/asset-maintenance/assets/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void shouldDeleteAsset() throws Exception {
        Mockito.when(authzService.canDeleteAsset()).thenReturn(true);
        UUID id = UUID.randomUUID();
        Mockito.doNothing().when(assetService).delete(eq(id), any());

        mockMvc.perform(delete("/api/asset-maintenance/assets/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRestoreAsset() throws Exception {
        Mockito.when(authzService.canUpdateAsset()).thenReturn(true);
        UUID id = UUID.randomUUID();
        var restored = sampleAsset(id, UUID.randomUUID(), null, "AS03", "Restored");
        Mockito.when(assetService.restore(eq(id), any())).thenReturn(restored);

        mockMvc.perform(put("/api/asset-maintenance/assets/{id}/restore", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AS03"));
    }

    @Test
    void shouldGetAssetsByBuilding() throws Exception {
        Mockito.when(authzService.canViewAsset()).thenReturn(true);
        UUID buildingId = UUID.randomUUID();
        var a1 = sampleAsset(UUID.randomUUID(), buildingId, null, "A1", "Asset1");
        var a2 = sampleAsset(UUID.randomUUID(), buildingId, null, "A2", "Asset2");
        Mockito.when(assetService.getAssetsByBuildingId(eq(buildingId))).thenReturn(List.of(a1, a2));

        mockMvc.perform(get("/api/asset-maintenance/assets/by-building/{buildingId}", buildingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].buildingId").value(buildingId.toString()))
                .andExpect(jsonPath("$[1].code").value("A2"));
    }

    @Test
    void shouldGetAssetsByUnit() throws Exception {
        Mockito.when(authzService.canViewAsset()).thenReturn(true);
        UUID unitId = UUID.randomUUID();
        var a1 = sampleAsset(UUID.randomUUID(), UUID.randomUUID(), unitId, "U1", "Asset1");
        Mockito.when(assetService.getAssetsByUnitId(eq(unitId))).thenReturn(List.of(a1));

        mockMvc.perform(get("/api/asset-maintenance/assets/by-unit/{unitId}", unitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].unitId").value(unitId.toString()))
                .andExpect(jsonPath("$[0].code").value("U1"));
    }

    @Test
    void shouldSearchAssets() throws Exception {
        Mockito.when(authzService.canViewAsset()).thenReturn(true);
        UUID buildingId = UUID.randomUUID();
        var a1 = sampleAsset(UUID.randomUUID(), buildingId, null, "S1", "SearchResult");
        Mockito.when(assetService.searchAssets(eq("pump"), eq(buildingId), eq(10))).thenReturn(List.of(a1));

        mockMvc.perform(get("/api/asset-maintenance/assets/search")
                .param("q", "pump")
                .param("buildingId", buildingId.toString())
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("S1"));
    }
}
