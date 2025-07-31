package com.example.companybackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * トークンリフレッシュリクエストDTO
 */
@Data
public class RefreshTokenRequest {
    
    @NotBlank(message = "リフレッシュトークンは必須です")
    private String refreshToken;
}