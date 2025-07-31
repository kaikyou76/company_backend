package com.example.companybackend.dto.auth;

import com.example.companybackend.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * ログインレスポンスDTO
 */
@Data
@Builder
public class LoginResponse {

    private boolean success;
    private String message;
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private LoginUserData user;
    
    public static LoginResponse error(String message) {
        return LoginResponse.builder()
                .success(false)
                .message(message)
                .token(null)
                .refreshToken(null)
                .expiresIn(null)
                .user(null)
                .build();
    }

    public static LoginResponse success(String accessToken, String refreshToken, User user,
                                       String departmentName, String positionName) {
        LoginUserData userData = LoginUserData.builder()
                .id(user.getId())
                .name(user.getUsername())
                .departmentId(user.getDepartmentId())
                .departmentName(departmentName)
                .positionId(user.getPositionId())
                .positionName(positionName)
                .role(user.getRole())
                .locationType(user.getLocationType())
                .build();

        return LoginResponse.builder()
                .success(true)
                .message(null)
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(86400L) // 24時間
                .user(userData)
                .build();
    }

    /**
     * ログインユーザーデータ
     */
    @Data
    @Builder
    public static class LoginUserData {
        private Long id;
        private String name;
        private Integer departmentId;
        private String departmentName;
        private Integer positionId;
        private String positionName;
        private String role;
        private String locationType;
    }
}