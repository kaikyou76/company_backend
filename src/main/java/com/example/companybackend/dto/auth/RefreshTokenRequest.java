package com.example.companybackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;

/**
 * トークンリフレッシュリクエストDTO
 */
@Data
@Getter
public class RefreshTokenRequest {
    
    @NotBlank(message = "リフレッシュトークンは必須です")
    private String refreshToken;
}