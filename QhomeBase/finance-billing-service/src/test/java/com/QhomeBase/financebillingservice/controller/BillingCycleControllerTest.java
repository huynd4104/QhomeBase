package com.QhomeBase.financebillingservice.controller;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.QhomeBase.financebillingservice.dto.BillingCycleDto;
import com.QhomeBase.financebillingservice.dto.CreateBillingCycleRequest;
import com.QhomeBase.financebillingservice.dto.ReadingCycleDto;
import com.QhomeBase.financebillingservice.service.BillingCycleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BillingCycleController.class)
@AutoConfigureMockMvc(addFilters = false)
class BillingCycleControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private BillingCycleService billingCycleService;

        @Test
        void shouldCreateBillingCycle() throws Exception {
                var id = UUID.randomUUID();
                var dto = BillingCycleDto.builder()
                                .id(id)
                                .name("Jan 2025")
                                .periodFrom(LocalDate.of(2025, 1, 1))
                                .periodTo(LocalDate.of(2025, 1, 31))
                                .status("OPEN")
                                .build();
                Mockito.when(billingCycleService.createBillingCycle(any(CreateBillingCycleRequest.class)))
                                .thenReturn(dto);

                var req = CreateBillingCycleRequest.builder()
                                .name("Jan 2025")
                                .periodFrom(LocalDate.of(2025, 1, 1))
                                .periodTo(LocalDate.of(2025, 1, 31))
                                .status("OPEN")
                                .build();

                mockMvc.perform(post("/api/billing-cycles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.status").value("OPEN"));
        }

        @Test
        void shouldUpdateBillingCycleStatus() throws Exception {
                var id = UUID.randomUUID();
                var dto = BillingCycleDto.builder().id(id).status("CLOSED").build();
                Mockito.when(billingCycleService.updateBillingCycleStatus(any(UUID.class), any(String.class)))
                                .thenReturn(dto);

                mockMvc.perform(put("/api/billing-cycles/" + id + "/status").param("status", "CLOSED"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CLOSED"));
        }

        @Test
        void shouldGetMissingReadingCycles() throws Exception {
                var rc = new ReadingCycleDto(
                                UUID.randomUUID(),
                                "Cycle",
                                LocalDate.of(2025, 1, 1),
                                LocalDate.of(2025, 1, 31),
                                "OPEN",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null);
                Mockito.when(billingCycleService.getMissingReadingCyclesInfo()).thenReturn(List.of(rc));

                mockMvc.perform(get("/api/billing-cycles/missing"))
                                .andExpect(status().isOk());
        }

        @Test
        void shouldLoadPeriodByYear() throws Exception {
                var dto = BillingCycleDto.builder().name("Jan").build();
                Mockito.when(billingCycleService.loadPeriod(any(Integer.class))).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/billing-cycles/loadPeriod").param("year", "2025"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("Jan"));
        }

        @Test
        void shouldGetListByTime() throws Exception {
                var dto = BillingCycleDto.builder().name("Range").build();
                Mockito.when(billingCycleService.getListByTime(any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(List.of(dto));

                mockMvc.perform(get("/api/billing-cycles")
                                .param("startDate", "2025-01-01")
                                .param("endDate", "2025-01-31"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("Range"));
        }

        @Test
        void shouldGetByExternalCycleId() throws Exception {
                var dto = BillingCycleDto.builder().name("Ext").build();
                Mockito.when(billingCycleService.findByExternalCycleId(any(UUID.class))).thenReturn(List.of(dto));

                var externalId = UUID.randomUUID();
                mockMvc.perform(get("/api/billing-cycles/external/" + externalId))
                                .andExpect(status().isOk());
        }
}
