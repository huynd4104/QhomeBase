package com.QhomeBase.customerinteractionservice.controller;

import com.QhomeBase.customerinteractionservice.dto.news.*;
import com.QhomeBase.customerinteractionservice.model.NewsStatus;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import com.QhomeBase.customerinteractionservice.security.AuthzService;
import com.QhomeBase.customerinteractionservice.security.JwtAuthFilter;
import com.QhomeBase.customerinteractionservice.service.NewsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NewsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.QhomeBase.customerinteractionservice.security.SecurityConfig.class)
class NewsControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private NewsService newsService;

        @MockitoBean(name = "authz")
        private AuthzService authzService;

        @MockitoBean
        private JwtAuthFilter jwtAuthFilter;

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldCreateNews() throws Exception {
                Mockito.when(authzService.canCreateNews()).thenReturn(true);
                var req = CreateNewsRequest.builder()
                                .title("Hello")
                                .summary("Summary")
                                .bodyHtml("<p>Body</p>")
                                .status(NewsStatus.DRAFT)
                                .scope(NotificationScope.INTERNAL)
                                .build();

                var resp = NewsManagementResponse.builder()
                                .id(UUID.randomUUID())
                                .title("Hello")
                                .status(NewsStatus.DRAFT)
                                .createdAt(Instant.now())
                                .build();
                Mockito.when(newsService.createNews(any(CreateNewsRequest.class), any())).thenReturn(resp);

                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "admin",
                                java.util.List.of("ADMIN"),
                                java.util.List.of(),
                                "dummy-token");
                var authn = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

                mockMvc.perform(post("/api/news")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .with(authentication(authn)))
                                .andDo(print())
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title").value("Hello"))
                                .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldUpdateNews() throws Exception {
                Mockito.when(authzService.canUpdateNews()).thenReturn(true);
                UUID id = UUID.randomUUID();
                var req = UpdateNewsRequest.builder().title("Updated").status(NewsStatus.PUBLISHED).build();
                var resp = NewsManagementResponse.builder().id(id).title("Updated").status(NewsStatus.PUBLISHED)
                                .updatedAt(Instant.now()).build();
                Mockito.when(newsService.updateNews(eq(id), any(UpdateNewsRequest.class), any())).thenReturn(resp);

                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "admin",
                                java.util.List.of("ADMIN"),
                                java.util.List.of(),
                                "dummy-token");
                var authn = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

                mockMvc.perform(put("/api/news/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .with(authentication(authn)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.title").value("Updated"))
                                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldDeleteNews() throws Exception {
                Mockito.when(authzService.canDeleteNews()).thenReturn(true);
                UUID id = UUID.randomUUID();
                var resp = NewsManagementResponse.builder().id(id).status(NewsStatus.ARCHIVED).updatedAt(Instant.now())
                                .build();
                Mockito.when(newsService.deleteNews(eq(id), any(UUID.class))).thenReturn(resp);
                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "admin",
                                java.util.List.of("ADMIN"),
                                java.util.List.of(),
                                "dummy-token");
                var authn = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

                mockMvc.perform(delete("/api/news/{id}", id).with(authentication(authn)).principal(authn))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGetAllNews() throws Exception {
                Mockito.when(authzService.canViewNews()).thenReturn(true);
                var item = NewsManagementResponse.builder().id(UUID.randomUUID()).title("Hello")
                                .status(NewsStatus.DRAFT).build();
                Mockito.when(newsService.getAllNews()).thenReturn(List.of(item));

                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "admin",
                                java.util.List.of("ADMIN"),
                                java.util.List.of(),
                                "dummy-token");
                var authn = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

                mockMvc.perform(get("/api/news").with(authentication(authn)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Hello"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGetNewsDetail() throws Exception {
                Mockito.when(authzService.canViewNews()).thenReturn(true);
                UUID id = UUID.randomUUID();
                var item = NewsManagementResponse.builder().id(id).title("Detail").status(NewsStatus.DRAFT).build();
                Mockito.when(newsService.getNewsDetail(eq(id))).thenReturn(item);

                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "admin",
                                java.util.List.of("ADMIN"),
                                java.util.List.of(),
                                "dummy-token");
                var authn = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

                mockMvc.perform(get("/api/news/{id}", id).with(authentication(authn)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.title").value("Detail"));
        }

        @Test
        void shouldGetNewsForResidentPaged() throws Exception {
                UUID residentId = UUID.randomUUID();
                var detail = NewsDetailResponse.builder().id(UUID.randomUUID()).title("RTitle")
                                .status(NewsStatus.PUBLISHED).build();
                var page = NewsPagedResponse.builder().content(List.of(detail)).currentPage(0).pageSize(7)
                                .totalElements(1).totalPages(1).isFirst(true).isLast(true).hasNext(false)
                                .hasPrevious(false).build();
                Mockito.when(newsService.getNewsForResidentPaged(eq(residentId), eq(0), eq(7))).thenReturn(page);

                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "resident",
                                java.util.List.of("RESIDENT"),
                                java.util.List.of(),
                                "dummy-token");
                var authn = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                java.util.List.of(new SimpleGrantedAuthority("ROLE_RESIDENT")));

                mockMvc.perform(get("/api/news/resident")
                                .param("residentId", residentId.toString())
                                .param("page", "0")
                                .param("size", "7")
                                .with(authentication(authn)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].title").value("RTitle"))
                                .andExpect(jsonPath("$.pageSize").value(7));
        }

        @Test
        void shouldGetNewsDetailForResident() throws Exception {
                UUID newsId = UUID.randomUUID();
                UUID residentId = UUID.randomUUID();
                var detail = NewsDetailResponse.builder().id(newsId).title("ResidentDetail")
                                .status(NewsStatus.PUBLISHED).isRead(false).build();
                Mockito.when(newsService.getNewsForResident(eq(newsId), eq(residentId))).thenReturn(detail);

                var principal = new com.QhomeBase.customerinteractionservice.security.UserPrincipal(
                                UUID.randomUUID(),
                                "resident",
                                java.util.List.of("RESIDENT"),
                                java.util.List.of(),
                                "dummy-token");
                var authn = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                java.util.List.of(new SimpleGrantedAuthority("ROLE_RESIDENT")));

                mockMvc.perform(get("/api/news/{id}/resident", newsId)
                                .param("residentId", residentId.toString())
                                .with(authentication(authn)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(newsId.toString()))
                                .andExpect(jsonPath("$.title").value("ResidentDetail"));
        }
}
