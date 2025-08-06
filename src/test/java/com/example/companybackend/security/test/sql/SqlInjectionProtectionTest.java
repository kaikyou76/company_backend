package com.example.companybackend.security.test.sql;

import com.example.companybackend.security.test.SecurityTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class SqlInjectionProtectionTest extends SecurityTestBase {

    @Override
    protected String getSecurityTestType() {
        return "SQL_INJECTION_PROTECTION";
    }

    @Test
    public void testSqlInjectionInRequestBody() throws Exception {
        String sqlInjectionPattern = SqlInjectionAttackPatternFactory.getRandomPattern();
        
        String maliciousPayload = "{\"name\": \"" + sqlInjectionPattern + "\"}";

        ResultActions result = mockMvc.perform(post("/api/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousPayload));

        // 预期应该返回400或403错误，而不是200
        result.andExpect(status().is4xxClientError());
    }

    @Test
    public void testSqlInjectionInQueryString() throws Exception {
        String sqlInjectionPattern = SqlInjectionAttackPatternFactory.getRandomPattern();

        ResultActions result = mockMvc.perform(post("/api/test?param=" + sqlInjectionPattern));

        // 预期应该返回400或403错误，而不是200
        result.andExpect(status().is4xxClientError());
    }

    @Test
    public void testSqlInjectionInFormData() throws Exception {
        String sqlInjectionPattern = SqlInjectionAttackPatternFactory.getRandomPattern();

        ResultActions result = mockMvc.perform(post("/api/test")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("input", sqlInjectionPattern));

        // 预期应该返回400或403错误，而不是200
        result.andExpect(status().is4xxClientError());
    }

    @Test
    public void testAllSqlInjectionPatterns() throws Exception {
        for (String pattern : SqlInjectionAttackPatternFactory.getAllPatterns()) {
            String maliciousPayload = "{\"name\": \"" + pattern + "\"}";

            ResultActions result = mockMvc.perform(post("/api/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(maliciousPayload));

            // 预期应该返回400或403错误，而不是200
            result.andExpect(status().is4xxClientError());
        }
    }
}