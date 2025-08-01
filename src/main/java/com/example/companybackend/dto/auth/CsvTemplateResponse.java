package com.example.companybackend.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * CSVテンプレートレスポンスDTO
 */
@Data
@Builder
public class CsvTemplateResponse {
    
    private boolean success;
    private String message;
    private TemplateData data;
    
    public static CsvTemplateResponse of(String[] headers, String[] sampleData) {
        TemplateData templateData = TemplateData.builder()
                .headers(headers)
                .sampleData(sampleData)
                .description("CSVファイルはUTF-8エンコーディングで作成してください。1行目はヘッダー行として扱われます。")
                .build();
        
        return CsvTemplateResponse.builder()
                .success(true)
                .message("CSV テンプレートフォーマット")
                .data(templateData)
                .build();
    }
    
    public static CsvTemplateResponse error(String message) {
        return CsvTemplateResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
    
    /**
     * テンプレートデータ
     */
    @Data
    @Builder
    public static class TemplateData {
        private String[] headers;
        private String[] sampleData;
        private String description;
    }
}