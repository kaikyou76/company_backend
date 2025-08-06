package com.example.companybackend.security.test.sql;

import com.example.companybackend.security.test.SecurityTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class PathTraversalProtectionTest extends SecurityTestBase {

    @Override
    protected String getSecurityTestType() {
        return "PATH_TRAVERSAL_PROTECTION";
    }

    @Test
    public void testPathTraversalInRequestBody() throws Exception {
        String pathTraversalPattern = PathTraversalAttackPatternFactory.getRandomPattern();
        
        String maliciousPayload = "{\"filename\": \"" + pathTraversalPattern + "\"}";

        ResultActions result = mockMvc.perform(post("/api/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousPayload));

        // 预期应该返回400或403错误，而不是200
        result.andExpect(status().is4xxClientError());
    }

    @Test
    public void testPathTraversalInQueryString() throws Exception {
        String pathTraversalPattern = PathTraversalAttackPatternFactory.getRandomPattern();

        ResultActions result = mockMvc.perform(post("/api/test?file=" + pathTraversalPattern));

        // 预期应该返回400或403错误，而不是200
        result.andExpect(status().is4xxClientError());
    }

    @Test
    public void testPathTraversalInFormData() throws Exception {
        String pathTraversalPattern = PathTraversalAttackPatternFactory.getRandomPattern();

        ResultActions result = mockMvc.perform(post("/api/test")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("file", pathTraversalPattern));

        // 预期应该返回400或403错误，而不是200
        result.andExpect(status().is4xxClientError());
    }

    @Test
    public void testAllPathTraversalPatterns() throws Exception {
        for (String pattern : PathTraversalAttackPatternFactory.getAllPatterns()) {
            String maliciousPayload = "{\"filename\": \"" + pattern + "\"}";

            ResultActions result = mockMvc.perform(post("/api/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(maliciousPayload));

            // 预期应该返回400或403错误，而不是200
            result.andExpect(status().is4xxClientError());
        }
    }
}