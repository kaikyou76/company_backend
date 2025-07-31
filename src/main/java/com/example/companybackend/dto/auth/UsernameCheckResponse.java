package com.example.companybackend.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * ユーザー名チェックレスポンスDTO
 */
@Data
@Builder
public class UsernameCheckResponse {
    
    private boolean success;
    private String message;
    private boolean available;
    
    public static UsernameCheckResponse of(boolean available, String message) {
        return UsernameCheckResponse.builder()
                .success(true)
                .available(available)
                .message(message)
                .build();
    }
}