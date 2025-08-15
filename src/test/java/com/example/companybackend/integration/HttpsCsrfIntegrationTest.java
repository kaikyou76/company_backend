package com.example.companybackend.integration;

import com.example.companybackend.config.CsrfProperties;
import com.example.companybackend.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTPS/CSRF統合テスト
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class HttpsCsrfIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private CsrfProperties csrfProperties;

    @Test
    public void testCsrfTokenGeneration() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.csrfToken").exists())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("CSRF Token Response: " + responseBody);
    }

    @Test
    public void testCsrfStatus() throws Exception {
        mockMvc.perform(get("/api/csrf/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.csrfEnabled").value(true))
                .andExpect(jsonPath("$.protocol").exists())
                .andExpect(jsonPath("$.serverInfo").exists());
    }

    @Test
    public void testSecurityPropertiesConfiguration() {
        assertNotNull(securityProperties.getAllowedOrigins());
        assertTrue(securityProperties.getAllowedOrigins().length > 0);
        assertTrue(securityProperties.getMaxAge() > 0);
    }

    @Test
    public void testCsrfPropertiesConfiguration() {
        assertNotNull(csrfProperties.getCookieDomain());
        assertNotNull(csrfProperties.getCookiePath());
        assertTrue(csrfProperties.getTokenValiditySeconds() > 0);
    }

    @Test
    public void testCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/csrf/token")
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}