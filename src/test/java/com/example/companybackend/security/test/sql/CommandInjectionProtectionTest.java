package com.example.companybackend.security.test.sql;

import com.example.companybackend.security.test.SecurityTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class CommandInjectionProtectionTest extends SecurityTestBase {

    @Override
    protected String getSecurityTestType() {
        return "COMMAND_INJECTION_PROTECTION";
    }

    @Test
    public void testCommandInjectionInRequestBody() throws Exception {
        String commandInjectionPattern = CommandInjectionAttackPatternFactory.getRandomPattern();
        
        String maliciousPayload = "{\"name\": \"" + commandInjectionPattern + "\"}";

        ResultActions result = mockMvc.perform(post("/api/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousPayload));

        // 预期应该返回400或403错误，而不是200
        result.andExpect(status().is4xxClientError());
    }

    @Test
    public void testCommandInjectionInQueryString() throws Exception {
        String commandInjectionPattern = CommandInjectionAttackPatternFactory.getRandomPattern();

        ResultActions result = mockMvc.perform(post("/api/test?param=" + commandInjectionPattern));

        // 预期应该返回400或403错误，而不是200
        result.andExpect(status().is4xxClientError());
    }

    @Test
    public void testCommandInjectionInFormData() throws Exception {
        String commandInjectionPattern = CommandInjectionAttackPatternFactory.getRandomPattern();

        ResultActions result = mockMvc.perform(post("/api/test")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("input", commandInjectionPattern));

        // 预期应该返回400或403错误，而不是200
        result.andExpect(status().is4xxClientError());
    }

    @Test
    public void testAllCommandInjectionPatterns() throws Exception {
        for (String pattern : CommandInjectionAttackPatternFactory.getAllPatterns()) {
            String maliciousPayload = "{\"name\": \"" + pattern + "\"}";

            ResultActions result = mockMvc.perform(post("/api/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(maliciousPayload));

            // 预期应该返回400或403错误，而不是200
            result.andExpect(status().is4xxClientError());
        }
    }
}