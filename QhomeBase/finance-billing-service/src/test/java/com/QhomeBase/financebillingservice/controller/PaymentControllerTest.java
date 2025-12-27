package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.dto.CreatePaymentRequest;
import com.QhomeBase.financebillingservice.dto.PaymentDto;
import com.QhomeBase.financebillingservice.model.PaymentMethod;
import com.QhomeBase.financebillingservice.model.PaymentStatus;
import com.QhomeBase.financebillingservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private PaymentService paymentService;

        @Test
        void shouldCreatePayment() throws Exception {
                UUID id = UUID.randomUUID();
                UUID residentId = UUID.randomUUID();
                var dto = PaymentDto.builder()
                                .id(id)
                                .receiptNo("RCPT-001")
                                .method(PaymentMethod.CASH)
                                .paidAt(OffsetDateTime.now())
                                .amountTotal(BigDecimal.valueOf(100000))
                                .currency("VND")
                                .status(PaymentStatus.SUCCEEDED)
                                .payerResidentId(residentId)
                                .build();
                Mockito.when(paymentService.createPayment(any(CreatePaymentRequest.class))).thenReturn(dto);

                var req = CreatePaymentRequest.builder()
                                .method(PaymentMethod.CASH)
                                .amountTotal(BigDecimal.valueOf(100000))
                                .currency("VND")
                                .payerResidentId(residentId)
                                .build();

                mockMvc.perform(post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
        }

        @Test
        void shouldGetPaymentById() throws Exception {
                UUID id = UUID.randomUUID();
                var dto = PaymentDto.builder().id(id).status(PaymentStatus.SUCCEEDED).build();
                Mockito.when(paymentService.getPaymentById(any(UUID.class))).thenReturn(dto);

                mockMvc.perform(get("/api/payments/" + id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()));
        }

        @Test
        void shouldGetPaymentsByResident() throws Exception {
                UUID residentId = UUID.randomUUID();
                var dto = PaymentDto.builder().payerResidentId(residentId).build();
                Mockito.when(paymentService.getPaymentsByResident(any(UUID.class))).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/payments/resident/" + residentId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].payerResidentId").value(residentId.toString()));
        }
}
