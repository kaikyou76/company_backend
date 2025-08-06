package com.example.companybackend.security.test.sql;

import com.example.companybackend.security.test.SecurityTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class FileUploadSecurityTest extends SecurityTestBase {

    @Override
    protected String getSecurityTestType() {
        return "FILE_UPLOAD_SECURITY";
    }

    @Test
    public void testMaliciousFileNameInFileUpload() throws Exception {
        String maliciousFileName = MaliciousFileNameFactory.getRandomMaliciousFileName();
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                maliciousFileName,
                MediaType.TEXT_PLAIN_VALUE,
                "This is test content".getBytes()
        );

        ResultActions result = mockMvc.perform(multipart("/api/test/upload")
                .file(file));

        // 预期应该返回400或403错误，而不是200
        result.andExpect(status().is4xxClientError());
    }

    @Test
    public void testAllMaliciousFileNames() throws Exception {
        for (String fileName : MaliciousFileNameFactory.getAllMaliciousFileNames()) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    fileName,
                    MediaType.TEXT_PLAIN_VALUE,
                    "This is test content".getBytes()
            );

            ResultActions result = mockMvc.perform(multipart("/api/test/upload")
                    .file(file));

            // 预期应该返回400或403错误，而不是200
            result.andExpect(status().is4xxClientError());
        }
    }
}