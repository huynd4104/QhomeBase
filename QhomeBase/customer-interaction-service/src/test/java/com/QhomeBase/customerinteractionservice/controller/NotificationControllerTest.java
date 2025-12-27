package com.QhomeBase.customerinteractionservice.controller;

import com.QhomeBase.customerinteractionservice.dto.notification.*;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import com.QhomeBase.customerinteractionservice.model.NotificationType;
import com.QhomeBase.customerinteractionservice.security.AuthzService;
import com.QhomeBase.customerinteractionservice.security.JwtAuthFilter;
import com.QhomeBase.customerinteractionservice.service.NotificationDeviceTokenService;
import com.QhomeBase.customerinteractionservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import java.security.Principal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ com.QhomeBase.customerinteractionservice.security.SecurityConfig.class,
                com.QhomeBase.customerinteractionservice.controller.NotificationControllerTest.SecurityArgResolverConfig.class })
class NotificationControllerTest {

        @MockitoBean
        private NotificationService notificationService;

        @MockitoBean
        private NotificationDeviceTokenService notificationDeviceTokenService;

        @MockitoBean(name = "authz")
        private AuthzService authzService;

        @MockitoBean
        private JwtAuthFilter jwtAuthFilter;

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldCreateNotification() throws Exception {
                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "admin",
                                List.of("ADMIN"),
                                List.of("content.notification.manage"),
                                "token");
                var authn = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(authn);
                Mockito.when(authzService.canManageNotifications()).thenReturn(true);
                var req = CreateNotificationRequest.builder()
                                .type(NotificationType.INFO)
                                .title("T")
                                .message("M")
                                .scope(NotificationScope.INTERNAL)
                                .build();
                var resp = NotificationResponse.builder()
                                .id(UUID.randomUUID())
                                .type(NotificationType.INFO)
                                .title("T")
                                .message("M")
                                .scope(NotificationScope.INTERNAL)
                                .createdAt(Instant.now())
                                .build();
                Mockito.when(notificationService.createNotification(any(CreateNotificationRequest.class)))
                                .thenReturn(resp);

                mockMvc.perform(post("/api/notifications")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andDo(print())
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title").value("T"))
                                .andExpect(jsonPath("$.scope").value("INTERNAL"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldUpdateNotification() throws Exception {
                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "admin",
                                List.of("ADMIN"),
                                List.of("content.notification.manage"),
                                "token");
                var authn = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(authn);
                Mockito.when(authzService.canManageNotifications()).thenReturn(true);
                UUID id = UUID.randomUUID();
                var req = UpdateNotificationRequest.builder()
                                .title("U")
                                .message("MU")
                                .scope(NotificationScope.EXTERNAL)
                                .build();
                var resp = NotificationResponse.builder()
                                .id(id)
                                .type(NotificationType.INFO)
                                .title("U")
                                .message("MU")
                                .scope(NotificationScope.EXTERNAL)
                                .updatedAt(Instant.now())
                                .build();
                Mockito.when(notificationService.updateNotification(eq(id), any(UpdateNotificationRequest.class)))
                                .thenReturn(resp);

                mockMvc.perform(put("/api/notifications/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.scope").value("EXTERNAL"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldDeleteNotification() throws Exception {
                Mockito.when(authzService.canManageNotifications()).thenReturn(true);
                UUID id = UUID.randomUUID();
                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "admin",
                                List.of("ADMIN"),
                                List.of("content.notification.manage"),
                                "token");
                var authn = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(authn);

                mockMvc.perform(delete("/api/notifications/{id}", id).with(authentication(authn)).principal(authn))
                                .andDo(print())
                                .andExpect(status().isNoContent());
                Mockito.verify(notificationService).deleteNotification(eq(id), any(UUID.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGetAllNotifications() throws Exception {
                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "admin",
                                List.of("ADMIN"),
                                List.of("content.notification.view"),
                                "token");
                var authn = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(authn);
                Mockito.when(authzService.canViewNotifications()).thenReturn(true);
                var item = NotificationResponse.builder().id(UUID.randomUUID()).title("A").type(NotificationType.INFO)
                                .scope(NotificationScope.INTERNAL).build();
                Mockito.when(notificationService.getAllNotifications()).thenReturn(List.of(item));

                mockMvc.perform(get("/api/notifications"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("A"));
        }

        @Test
        void shouldGetNotificationById() throws Exception {
                UUID id = UUID.randomUUID();
                var detail = NotificationDetailResponse.builder()
                                .title("D")
                                .message("MD")
                                .scope(NotificationScope.INTERNAL)
                                .createdAt(Instant.now())
                                .build();
                Mockito.when(notificationService.getNotificationDetailById(eq(id))).thenReturn(detail);

                mockMvc.perform(get("/api/notifications/{id}", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("D"));
        }

        @Test
        void shouldGetNotificationsForResidentPaged() throws Exception {
                UUID residentId = UUID.randomUUID();
                UUID buildingId = UUID.randomUUID();
                var item = NotificationResponse.builder().id(UUID.randomUUID()).title("R").type(NotificationType.INFO)
                                .scope(NotificationScope.EXTERNAL).build();
                var page = NotificationPagedResponse.builder()
                                .content(List.of(item))
                                .currentPage(0)
                                .pageSize(7)
                                .totalElements(1)
                                .totalPages(1)
                                .isFirst(true)
                                .isLast(true)
                                .hasNext(false)
                                .hasPrevious(false)
                                .build();
                Mockito.when(notificationService.getNotificationsForResidentPaged(eq(residentId), eq(buildingId), eq(0),
                                eq(7)))
                                .thenReturn(page);

                mockMvc.perform(get("/api/notifications/resident")
                                .param("residentId", residentId.toString())
                                .param("buildingId", buildingId.toString())
                                .param("page", "0")
                                .param("size", "7"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].title").value("R"))
                                .andExpect(jsonPath("$.pageSize").value(7));
        }

        @Test
        void shouldGetNotificationsCountForResident() throws Exception {
                UUID residentId = UUID.randomUUID();
                UUID buildingId = UUID.randomUUID();
                Mockito.when(notificationService.getNotificationsCountForResident(eq(residentId), eq(buildingId)))
                                .thenReturn(5L);

                mockMvc.perform(get("/api/notifications/resident/count")
                                .param("residentId", residentId.toString())
                                .param("buildingId", buildingId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalCount").value(5));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGetNotificationsForRole() throws Exception {
                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "admin",
                                List.of("ADMIN"),
                                List.of("content.notification.view"),
                                "token");
                var authn = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(authn);
                Mockito.when(authzService.canViewNotifications()).thenReturn(true);
                UUID userId = UUID.randomUUID();
                var item = NotificationResponse.builder().id(UUID.randomUUID()).title("Role")
                                .type(NotificationType.INFO)
                                .scope(NotificationScope.INTERNAL).build();
                Mockito.when(notificationService.getNotificationsForRole(eq("ADMIN"), eq(userId)))
                                .thenReturn(List.of(item));

                mockMvc.perform(get("/api/notifications/role")
                                .param("role", "ADMIN")
                                .param("userId", userId.toString()))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Role"));
        }

        @Test
        @WithMockUser(roles = "RESIDENT")
        void shouldRegisterDeviceToken() throws Exception {
                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "resident",
                                List.of("RESIDENT"),
                                List.of(),
                                "token");
                var authn = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_RESIDENT")));

                SecurityContextHolder.getContext().setAuthentication(authn);

                var req = RegisterDeviceTokenRequest.builder()
                                .token("abc")
                                .platform("android")
                                .appVersion("1.0")
                                .residentId(UUID.randomUUID())
                                .buildingId(UUID.randomUUID())
                                .role("RESIDENT")
                                .build();

                var resp = DeviceTokenResponse.builder()
                                .id(UUID.randomUUID())
                                .token("abc")
                                .platform("android")
                                .appVersion("1.0")
                                .lastSeenAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();
                Mockito.when(notificationDeviceTokenService.registerToken(any(RegisterDeviceTokenRequest.class)))
                                .thenReturn(resp);

                mockMvc.perform(post("/api/notifications/device-tokens")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .with(authentication(authn))
                                .principal(authn))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("abc"));
        }

        @Test
        @WithMockUser(roles = "RESIDENT")
        void shouldDeleteDeviceToken() throws Exception {
                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "resident",
                                List.of("RESIDENT"),
                                List.of(),
                                "token");
                var authn = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_RESIDENT")));

                mockMvc.perform(delete("/api/notifications/device-tokens/{token}", "abc")
                                .with(authentication(authn))
                                .principal(authn))
                                .andDo(print())
                                .andExpect(status().isNoContent());
                Mockito.verify(notificationDeviceTokenService).removeToken(eq("abc"));
        }

        @Test
        void shouldCreateInternalNotification() throws Exception {
                var req = InternalNotificationRequest.builder()
                                .type(NotificationType.INFO)
                                .title("Hello")
                                .message("Body")
                                .build();

                mockMvc.perform(post("/api/notifications/internal")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isCreated());
                Mockito.verify(notificationService).createInternalNotification(any(InternalNotificationRequest.class));
        }

        @org.springframework.boot.test.context.TestConfiguration
        static class SecurityArgResolverConfig
                        implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {
                @Override
                public void addArgumentResolvers(
                                java.util.List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers) {
                        resolvers.add(new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                                @Override
                                public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                                        return org.springframework.security.core.Authentication.class
                                                        .isAssignableFrom(parameter.getParameterType());
                                }

                                @Override
                                public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                                org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                                org.springframework.web.context.request.NativeWebRequest webRequest,
                                                org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                                        var holderAuth = SecurityContextHolder.getContext() != null
                                                        ? SecurityContextHolder.getContext().getAuthentication()
                                                        : null;
                                        if (holderAuth != null)
                                                return holderAuth;

                                        HttpServletRequest req = webRequest.getNativeRequest(HttpServletRequest.class);
                                        if (req != null) {
                                                HttpSession session = req.getSession(false);
                                                if (session != null) {
                                                        Object sc = session.getAttribute("SPRING_SECURITY_CONTEXT");
                                                        if (sc instanceof SecurityContext ctx) {
                                                                var a = ctx.getAuthentication();
                                                                if (a != null)
                                                                        return a;
                                                        }
                                                }
                                                Principal p = req.getUserPrincipal();
                                                if (p instanceof org.springframework.security.core.Authentication a) {
                                                        return a;
                                                }
                                        }
                                        return null;
                                }
                        });
                }
        }
}
