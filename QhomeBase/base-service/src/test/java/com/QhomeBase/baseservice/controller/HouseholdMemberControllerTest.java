package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.HouseholdMemberDto;
import com.QhomeBase.baseservice.security.JwtAuthFilter;
import com.QhomeBase.baseservice.service.HouseHoldMemberService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = HouseholdMemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class HouseholdMemberControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private HouseHoldMemberService houseHoldMemberService;

        @MockitoBean
        private JwtAuthFilter jwtAuthFilter;

        @Test
        void shouldGetHouseholdMemberById() throws Exception {
                UUID id = UUID.randomUUID();
                var dto = new HouseholdMemberDto(
                                id,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "John",
                                "john@example.com",
                                "0123",
                                "OWNER",
                                null,
                                true,
                                null,
                                null,
                                java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now());
                Mockito.when(houseHoldMemberService.getHouseholdMemberById(any(UUID.class))).thenReturn(dto);

                mockMvc.perform(get("/api/household-members/" + id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()));
        }

        @Test
        void shouldGetActiveMembersByHouseholdId() throws Exception {
                UUID householdId = UUID.randomUUID();
                var dto = new HouseholdMemberDto(
                                UUID.randomUUID(),
                                householdId,
                                UUID.randomUUID(),
                                "John",
                                "john@example.com",
                                "0123",
                                "OWNER",
                                null,
                                true,
                                null,
                                null,
                                java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now());
                Mockito.when(houseHoldMemberService.getActiveMembersByHouseholdId(any(UUID.class)))
                                .thenReturn(List.of(dto));

                mockMvc.perform(get("/api/household-members/households/" + householdId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].householdId").value(householdId.toString()));
        }

        @Test
        void shouldGetActiveMembersByResidentId() throws Exception {
                UUID residentId = UUID.randomUUID();
                var dto = new HouseholdMemberDto(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                residentId,
                                "John",
                                "john@example.com",
                                "0123",
                                "OWNER",
                                null,
                                true,
                                null,
                                null,
                                java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now());
                Mockito.when(houseHoldMemberService.getActiveMembersByResidentId(any(UUID.class)))
                                .thenReturn(List.of(dto));

                mockMvc.perform(get("/api/household-members/residents/" + residentId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].residentId").value(residentId.toString()));
        }
}
