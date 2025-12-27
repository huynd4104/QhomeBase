// package com.QhomeBase.baseservice.controller;

// import com.QhomeBase.baseservice.dto.AdminMaintenanceResponseDto;
// import com.QhomeBase.baseservice.dto.AdminServiceRequestActionDto;
// import com.QhomeBase.baseservice.dto.CreateMaintenanceRequestDto;
// import com.QhomeBase.baseservice.dto.MaintenanceRequestConfigDto;
// import com.QhomeBase.baseservice.dto.MaintenanceRequestDto;
// import com.QhomeBase.baseservice.security.AuthzService;
// import com.QhomeBase.baseservice.security.JwtAuthFilter;
// import com.QhomeBase.baseservice.security.UserPrincipal;
// import com.QhomeBase.baseservice.service.MaintenanceRequestMonitor;
// import com.QhomeBase.baseservice.service.MaintenanceRequestService;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.Mockito;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

// import org.springframework.http.MediaType;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.test.context.bean.override.mockito.MockitoBean;
// import org.springframework.test.web.servlet.MockMvc;

// import java.time.OffsetDateTime;
// import java.util.List;
// import java.util.UUID;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest(controllers = MaintenanceRequestController.class)
// @AutoConfigureMockMvc(addFilters = false)
// class MaintenanceRequestControllerTest {

//         @Autowired
//         private MockMvc mockMvc;

//         @Autowired
//         private ObjectMapper objectMapper;

//         @MockitoBean
//         private MaintenanceRequestService maintenanceRequestService;

//         @MockitoBean
//         private MaintenanceRequestMonitor maintenanceRequestMonitor;

//         @MockitoBean
//         private AuthzService authz;

//         @MockitoBean
//         private JwtAuthFilter jwtAuthFilter;

//         private UsernamePasswordAuthenticationToken authResident;
//         private UUID userId;

//         @BeforeEach
//         void setUp() {
//                 userId = UUID.randomUUID();
//                 var principalResident = new UserPrincipal(userId, "resident", List.of("RESIDENT"), List.of(), "token");
//                 authResident = new UsernamePasswordAuthenticationToken(principalResident, null,
//                                 List.of(new SimpleGrantedAuthority("ROLE_RESIDENT")));
//         }

//         @Test
//         void shouldCreateMaintenanceRequest() throws Exception {
//                 var req = new CreateMaintenanceRequestDto(UUID.randomUUID(), "PLUMBING", "Leak", "desc",
//                                 java.util.List.of(),
//                                 "Kitchen", OffsetDateTime.now(), "John", "0123", "note");
//                 var dto = new MaintenanceRequestDto(UUID.randomUUID(), req.unitId(), null, userId, userId,
//                                 req.category(),
//                                 req.title(), req.description(), java.util.List.of(), req.location(),
//                                 req.preferredDatetime(),
//                                 req.contactName(), req.contactPhone(), req.note(), "PENDING", OffsetDateTime.now(),
//                                 OffsetDateTime.now(), null, false, false, null, null, null, null, null);
//                 Mockito.when(maintenanceRequestService.create(any(UUID.class), any(CreateMaintenanceRequestDto.class)))
//                                 .thenReturn(dto);

//                 var ctx = new org.springframework.security.core.context.SecurityContextImpl();
//                 ctx.setAuthentication(authResident);
//                 mockMvc.perform(post("/api/maintenance-requests")
//                                 .with(securityContext(ctx))
//                                 .principal(authResident)
//                                 .contentType(MediaType.APPLICATION_JSON)
//                                 .accept(MediaType.APPLICATION_JSON)
//                                 .content(objectMapper.writeValueAsString(req)))
//                                 .andExpect(status().isOk());
//         }

//         @Test
//         void shouldGetMyMaintenanceRequests() throws Exception {
//                 var dto = new MaintenanceRequestDto(UUID.randomUUID(), UUID.randomUUID(), null, userId, userId,
//                                 "PLUMBING",
//                                 "Leak", "desc", java.util.List.of(), "Kitchen", OffsetDateTime.now(), "John", "0123",
//                                 "note", "PENDING",
//                                 OffsetDateTime.now(), OffsetDateTime.now(), null, false, false, null, null, null, null,
//                                 null);
//                 Mockito.when(maintenanceRequestService.getMyRequests(any(UUID.class))).thenReturn(List.of(dto));

//                 var ctx = new org.springframework.security.core.context.SecurityContextImpl();
//                 ctx.setAuthentication(authResident);
//                 mockMvc.perform(get("/api/maintenance-requests/my")
//                                 .with(securityContext(ctx))
//                                 .principal(authResident)
//                                 .accept(MediaType.APPLICATION_JSON))
//                                 .andExpect(status().isOk());
//         }

//         @Test
//         void shouldApproveAndComplete_Admin() throws Exception {
//                 Mockito.when(authz.canManageServiceRequests()).thenReturn(true);
//                 var resp = new AdminMaintenanceResponseDto("OK", java.math.BigDecimal.valueOf(100000), "note");
//                 var dto = new MaintenanceRequestDto(UUID.randomUUID(), UUID.randomUUID(), null, userId, userId,
//                                 "PLUMBING",
//                                 "Leak", "desc", List.of(), "Kitchen", OffsetDateTime.now(), "John", "0123", "note",
//                                 "IN_PROGRESS",
//                                 OffsetDateTime.now(), OffsetDateTime.now(), null, false, false, "OK",
//                                 java.math.BigDecimal.valueOf(100000), userId, OffsetDateTime.now(), "APPROVED");
//                 Mockito.when(maintenanceRequestService.respondToRequest(any(UUID.class), any(UUID.class),
//                                 any(AdminMaintenanceResponseDto.class))).thenReturn(dto);

//                 mockMvc.perform(post("/api/maintenance-requests/admin/" + UUID.randomUUID() + "/respond")
//                                 .contentType(MediaType.APPLICATION_JSON)
//                                 .accept(MediaType.APPLICATION_JSON)
//                                 .content(objectMapper.writeValueAsString(resp)))
//                                 .andExpect(status().isOk());

//                 var dto2 = new MaintenanceRequestDto(UUID.randomUUID(), UUID.randomUUID(), null, userId, userId,
//                                 "PLUMBING",
//                                 "Leak", "desc", List.of(), "Kitchen", OffsetDateTime.now(), "John", "0123", "note",
//                                 "DONE",
//                                 OffsetDateTime.now(), OffsetDateTime.now(), null, false, false, "OK",
//                                 java.math.BigDecimal.valueOf(100000), userId, OffsetDateTime.now(), "COMPLETED");
//                 Mockito.when(maintenanceRequestService.completeRequest(any(UUID.class), any(UUID.class),
//                                 any(AdminServiceRequestActionDto.class))).thenReturn(dto2);

//                 mockMvc.perform(patch("/api/maintenance-requests/admin/" + UUID.randomUUID() + "/complete")
//                                 .accept(MediaType.APPLICATION_JSON))
//                                 .andExpect(status().isOk());
//         }

//         @Test
//         void shouldGetConfig() throws Exception {
//                 Mockito.when(maintenanceRequestMonitor.getConfig()).thenReturn(new MaintenanceRequestConfigDto(
//                                 java.time.Duration.ofMinutes(5), java.time.Duration.ofMinutes(15), "0123456789"));
//                 mockMvc.perform(get("/api/maintenance-requests/config")
//                                 .accept(MediaType.APPLICATION_JSON))
//                                 .andExpect(status().isOk())
//                                 .andExpect(jsonPath("$.adminPhone").value("0123456789"));
//         }
// }
