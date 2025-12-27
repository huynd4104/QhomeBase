package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.client.ContractClient;
import com.QhomeBase.baseservice.dto.ContractDetailDto;
import com.QhomeBase.baseservice.dto.ContractFileDto;
import com.QhomeBase.baseservice.dto.ContractSummary;
import com.QhomeBase.baseservice.dto.CreateContractProxyRequest;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ContractProxyController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContractProxyControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private ContractClient contractClient;

        @MockitoBean
        private JwtAuthFilter jwtAuthFilter;

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGetActiveContractsByUnit() throws Exception {
                UUID unitId = UUID.randomUUID();
                var item = new ContractSummary(UUID.randomUUID(), unitId, "C-001", "RENT",
                                LocalDate.now().minusDays(10), LocalDate.now().plusDays(30), "ACTIVE");
                Mockito.when(contractClient.getActiveContractsByUnit(eq(unitId))).thenReturn(List.of(item));

                mockMvc.perform(get("/api/contracts/units/{unitId}/active", unitId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].contractNumber").value("C-001"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGetContractsByUnit() throws Exception {
                UUID unitId = UUID.randomUUID();
                var item = new ContractSummary(UUID.randomUUID(), unitId, "C-002", "RENT", LocalDate.now(), null,
                                "PENDING");
                Mockito.when(contractClient.getContractsByUnit(eq(unitId))).thenReturn(List.of(item));

                mockMvc.perform(get("/api/contracts/units/{unitId}", unitId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGetContractById_success() throws Exception {
                UUID contractId = UUID.randomUUID();
                var detail = new ContractDetailDto(contractId, UUID.randomUUID(), "C-100", "PURCHASE",
                                LocalDate.now().minusDays(5), LocalDate.now().plusDays(90),
                                BigDecimal.valueOf(1000000), BigDecimal.ZERO, "BANK", "MONTHLY",
                                LocalDate.now().minusDays(5), "notes", "ACTIVE",
                                UUID.randomUUID(), OffsetDateTime.now(), OffsetDateTime.now(), null, List.of());
                Mockito.when(contractClient.getContractById(eq(contractId))).thenReturn(java.util.Optional.of(detail));

                mockMvc.perform(get("/api/contracts/{contractId}", contractId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(contractId.toString()))
                                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGetContractById_notFound() throws Exception {
                UUID contractId = UUID.randomUUID();
                Mockito.when(contractClient.getContractById(eq(contractId))).thenReturn(java.util.Optional.empty());

                mockMvc.perform(get("/api/contracts/{contractId}", contractId))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldCreateContract_asAdminPrincipal() throws Exception {
                UUID userId = UUID.randomUUID();
                var principal = new UserPrincipal(userId, "admin", java.util.List.of("ADMIN"), java.util.List.of(),
                                "token");
                var authorities = java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
                var authAdmin = new UsernamePasswordAuthenticationToken(principal, "token", authorities);
                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authAdmin);

                var request = new CreateContractProxyRequest(
                                UUID.randomUUID(),
                                "C-500",
                                "RENT",
                                LocalDate.now(),
                                LocalDate.now().plusDays(365),
                                BigDecimal.valueOf(5000000),
                                BigDecimal.ZERO,
                                "BANK",
                                "MONTHLY",
                                LocalDate.now(),
                                "notes",
                                "ACTIVE");

                UUID contractId = UUID.randomUUID();
                var detail = new ContractDetailDto(contractId, request.unitId(), request.contractNumber(),
                                request.contractType(),
                                request.startDate(), request.endDate(), request.monthlyRent(), request.purchasePrice(),
                                request.paymentMethod(), request.paymentTerms(), request.purchaseDate(),
                                request.notes(),
                                "ACTIVE", userId, OffsetDateTime.now(), OffsetDateTime.now(), userId,
                                java.util.List.of());

                Mockito.when(contractClient.createContract(any(CreateContractProxyRequest.class), eq(userId)))
                                .thenReturn(detail);

                mockMvc.perform(post("/api/contracts")
                                .with(securityContext(ctx))
                                .principal(authAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status").value("ACTIVE"))
                                .andExpect(jsonPath("$.contractNumber").value("C-500"));
        }

        @Test
        void shouldUploadContractFiles_asAdminPrincipal() throws Exception {
                UUID userId = UUID.randomUUID();
                var principal = new UserPrincipal(userId, "admin", java.util.List.of("ADMIN"), java.util.List.of(),
                                "token");
                var authorities = java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
                var authAdmin = new UsernamePasswordAuthenticationToken(principal, "token", authorities);
                var ctx = new org.springframework.security.core.context.SecurityContextImpl();
                ctx.setAuthentication(authAdmin);

                UUID contractId = UUID.randomUUID();
                var file1 = new MockMultipartFile("files", "contract.pdf", MediaType.APPLICATION_PDF_VALUE,
                                new byte[] { 1, 2, 3 });
                var file2 = new MockMultipartFile("files", "terms.txt", MediaType.TEXT_PLAIN_VALUE, "abc".getBytes());

                var uploaded = List.of(new ContractFileDto(UUID.randomUUID(), contractId, "contract.pdf",
                                "contract.pdf",
                                "/files/contract.pdf", "/api/contracts/" + contractId + "/files/xxx/view",
                                MediaType.APPLICATION_PDF_VALUE, 3L, true, 0, userId, OffsetDateTime.now()));

                Mockito.when(contractClient.uploadContractFiles(eq(contractId),
                                any(org.springframework.web.multipart.MultipartFile[].class), eq(userId)))
                                .thenReturn(uploaded);

                mockMvc.perform(multipart("/api/contracts/{contractId}/files", contractId)
                                .file(file1)
                                .file(file2)
                                .with(securityContext(ctx))
                                .principal(authAdmin))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].fileName").value("contract.pdf"));
        }

        @Test
        void shouldViewContractFile_success() throws Exception {
                UUID contractId = UUID.randomUUID();
                UUID fileId = UUID.randomUUID();
                byte[] bytes = new byte[] { 10, 20, 30 };
                Mockito.when(contractClient.viewContractFile(eq(contractId), eq(fileId)))
                                .thenReturn(ResponseEntity.ok(bytes));

                mockMvc.perform(get("/api/contracts/{contractId}/files/{fileId}/view", contractId, fileId))
                                .andExpect(status().isOk())
                                .andExpect(content().bytes(bytes));
        }

        @Test
        void shouldViewContractFile_notFound() throws Exception {
                UUID contractId = UUID.randomUUID();
                UUID fileId = UUID.randomUUID();
                Mockito.when(contractClient.viewContractFile(eq(contractId), eq(fileId)))
                                .thenReturn(ResponseEntity.notFound().build());

                mockMvc.perform(get("/api/contracts/{contractId}/files/{fileId}/view", contractId, fileId))
                                .andExpect(status().isNotFound());
        }
}
