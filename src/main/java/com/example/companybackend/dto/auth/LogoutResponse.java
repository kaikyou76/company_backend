package com.example.companybackend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * ログアウトレスポンスDTO
 */
@Data
@Builder
@AllArgsConstructor
public class LogoutResponse {
    
    private boolean success;
    private String message;
    
    public static LogoutResponse success() {
        return LogoutResponse.builder()
                .success(true)
                .message("ログアウトしました")
                .build();
    }
    
    public static LogoutResponse error(String message) {
        return LogoutResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}