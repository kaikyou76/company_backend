package com.example.companybackend.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * 登録レスポンスDTO
 */
@Data
@Builder
public class RegisterResponse {
    
    private boolean success;
    private String message;
    private Long userId;
    private String username;
    private Integer registeredCount;
}
