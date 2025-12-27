package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.dto.CreateInvoiceLineRequest;
import com.QhomeBase.financebillingservice.dto.CreateInvoiceRequest;
import com.QhomeBase.financebillingservice.dto.InvoiceCategoryResponseDto;
import com.QhomeBase.financebillingservice.dto.InvoiceDto;
import com.QhomeBase.financebillingservice.dto.InvoiceLineResponseDto;
import com.QhomeBase.financebillingservice.dto.UpdateInvoiceStatusRequest;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import com.QhomeBase.financebillingservice.service.InvoiceExportService;
import com.QhomeBase.financebillingservice.service.InvoiceService;
import com.QhomeBase.financebillingservice.service.vnpay.VnpayService;
import com.QhomeBase.financebillingservice.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = InvoiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvoiceControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private InvoiceService invoiceService;

        @MockitoBean
        private InvoiceExportService invoiceExportService;

        @MockitoBean
        private JwtUtil jwtUtil;

        @MockitoBean
        private VnpayService vnpayService;

        @Test
        void shouldCreateInvoice() throws Exception {
                UUID id = UUID.randomUUID();
                var dto = InvoiceDto.builder()
                                .id(id)
                                .code("INV-001")
                                .dueDate(LocalDate.now())
                                .status(InvoiceStatus.DRAFT)
                                .currency("VND")
                                .totalAmount(BigDecimal.valueOf(100000))
                                .build();
                Mockito.when(invoiceService.createInvoice(any(CreateInvoiceRequest.class))).thenReturn(dto);

                var line = CreateInvoiceLineRequest.builder()
                                .description("Service")
                                .quantity(BigDecimal.ONE)
                                .unit("EA")
                                .unitPrice(BigDecimal.valueOf(100000))
                                .serviceCode("SVC")
                                .build();
                var req = CreateInvoiceRequest.builder()
                                .currency("VND")
                                .lines(List.of(line))
                                .build();

                mockMvc.perform(post("/api/invoices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.code").value("INV-001"));
        }

        @Test
        void shouldGetInvoiceById() throws Exception {
                UUID id = UUID.randomUUID();
                var dto = InvoiceDto.builder().id(id).status(InvoiceStatus.DRAFT).build();
                Mockito.when(invoiceService.getInvoiceById(any(UUID.class))).thenReturn(dto);

                mockMvc.perform(get("/api/invoices/" + id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()));
        }

        @Test
        void shouldUpdateInvoiceStatus() throws Exception {
                UUID id = UUID.randomUUID();
                var dto = InvoiceDto.builder().id(id).status(InvoiceStatus.PUBLISHED).build();
                Mockito.when(invoiceService.updateInvoiceStatus(any(UUID.class), any(UpdateInvoiceStatusRequest.class)))
                                .thenReturn(dto);

                var req = UpdateInvoiceStatusRequest.builder().status(InvoiceStatus.PUBLISHED).build();
                mockMvc.perform(put("/api/invoices/" + id + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        }

        @Test
        void shouldVoidInvoice() throws Exception {
                UUID id = UUID.randomUUID();
                Mockito.doNothing().when(invoiceService).voidInvoice(any(UUID.class), any(String.class));

                mockMvc.perform(delete("/api/invoices/" + id + "/void"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void shouldExportInvoices() throws Exception {
                Mockito.when(invoiceExportService.exportInvoicesToExcel(any(), any(), any(), any(), any(), any()))
                                .thenReturn(new byte[] { 1, 2, 3 });
                mockMvc.perform(get("/api/invoices/admin/export"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
        }

        @Test
        void shouldReturn401ForMyInvoicesWhenTokenInvalid() throws Exception {
                Mockito.when(jwtUtil.getUserIdFromHeader(any(String.class))).thenReturn(null);
                mockMvc.perform(get("/api/invoices/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid")
                                .param("unitId", UUID.randomUUID().toString()))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error").value("Invalid or missing authentication token"));
        }

        @Test
        void shouldReturnMyInvoices() throws Exception {
                UUID userId = UUID.randomUUID();
                Mockito.when(jwtUtil.getUserIdFromHeader(any(String.class))).thenReturn(userId);
                var line = InvoiceLineResponseDto.builder().description("Service").lineTotal(100.0).build();
                Mockito.when(invoiceService.getMyInvoices(any(UUID.class), any(UUID.class), nullable(UUID.class)))
                                .thenReturn(List.of(line));

                mockMvc.perform(get("/api/invoices/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .param("unitId", UUID.randomUUID().toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].description").value("Service"));
        }

        @Test
        void shouldCreateVnpayUrl() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID invoiceId = UUID.randomUUID();
                Mockito.when(jwtUtil.getUserIdFromHeader(any(String.class))).thenReturn(userId);
                Mockito.when(invoiceService.createVnpayPaymentUrl(any(UUID.class), any(UUID.class), any(),
                                any(UUID.class))).thenReturn("http://pay");

                mockMvc.perform(post("/api/invoices/" + invoiceId + "/vnpay-url")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .param("unitId", UUID.randomUUID().toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.paymentUrl").value("http://pay"))
                                .andExpect(jsonPath("$.invoiceId").value(invoiceId.toString()));

        }
}
