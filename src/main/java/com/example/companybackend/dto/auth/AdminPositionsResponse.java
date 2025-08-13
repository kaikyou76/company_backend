package com.example.companybackend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 管理者役職一覧レスポンスDTO
 */
@Data
@Builder
@AllArgsConstructor
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
    @AllArgsConstructor
    public static class PositionData {
        private Long id;
        private String name;
        private Integer level;
    }
}