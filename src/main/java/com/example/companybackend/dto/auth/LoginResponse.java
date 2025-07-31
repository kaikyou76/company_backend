package com.example.companybackend.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * ログインレスポンスDTO
 */
@Data
@Builder
public class LoginResponse {
    
    private boolean success;
    private String message;
    private String token;
    private String username;
}
