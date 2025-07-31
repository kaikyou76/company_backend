package com.example.companybackend.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * CSV一括登録レスポンスDTO
 */
@Data
@Builder
public class CsvRegisterResponse {
    
    private boolean success;
    private String message;
    private CsvRegisterData data;
    
    public static CsvRegisterResponse error(String message) {
        return CsvRegisterResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
    
    public static CsvRegisterResponse success(int successCount, int errorCount) {
        CsvRegisterData data = CsvRegisterData.builder()
                .successCount(successCount)
                .errorCount(errorCount)
                .build();
        
        return CsvRegisterResponse.builder()
                .success(true)
                .message("CSV一括登録が正常に完了しました")
                .data(data)
                .build();
    }
    
    /**
     * CSV登録データ
     */
    @Data
    @Builder
    public static class CsvRegisterData {
        private int successCount;
        private int errorCount;
    }
}