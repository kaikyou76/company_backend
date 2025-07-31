package com.example.companybackend.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 管理者役職一覧レスポンスDTO
 */
@Data
@Builder
public class AdminPositionsResponse {
    
    private boolean success;
    private String message;
    private List<PositionData> data;
    
    public static AdminPositionsResponse of(List<PositionData> positions) {
        return AdminPositionsResponse.builder()
                .success(true)
                .message("管理者役職一覧を取得しました")
                .data(positions)
                .build();
    }
    
    public static AdminPositionsResponse error(String message) {
        return AdminPositionsResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
    
    /**
     * 役職データ
     */
    @Data
    @Builder
    public static class PositionData {
        private Long id;
        private String name;
        private Integer level;
        
        public PositionData() {}
        
        public PositionData(Long id, String name, Integer level) {
            this.id = id;
            this.name = name;
            this.level = level;
        }
    }
}