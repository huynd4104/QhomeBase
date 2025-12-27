package com.QhomeBase.iamservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class JwtEndToEndTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testApplicationContextLoads() {
        // Basic test to ensure Spring context loads successfully
        // This verifies that the application can start without errors
    }

    @Test
    void testLoginEndpoint_ReturnsBadRequest_WhenNoBody() throws Exception {
        // Test that login endpoint exists and returns appropriate error for invalid request
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().is4xxClientError());
    }
}
