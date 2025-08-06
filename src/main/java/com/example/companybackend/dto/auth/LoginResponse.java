package com.example.companybackend.dto.auth;

import com.example.companybackend.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;

/**
 * ログインレスポンスDTO
 */
public class LoginResponse {

    private boolean success;
    private String message;
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private LoginUserData user;
    
    public LoginResponse() {
    }
    
    public LoginResponse(boolean success, String message, String token, String refreshToken, 
                        Long expiresIn, LoginUserData user) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }
    
    public static LoginResponse error(String message) {
        return new LoginResponse(false, message, null, null, null, null);
    }

    public static LoginResponse success(String accessToken, String refreshToken, User user,
                                       String departmentName, String positionName) {
        LoginUserData userData = new LoginUserData(
                user.getId(),
                user.getUsername(),
                user.getDepartmentId(),
                departmentName,
                user.getPositionId(),
                positionName,
                user.getRole(),
                user.getLocationType());

        return new LoginResponse(true, null, accessToken, refreshToken, 3600L, userData);
    }
    
    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public LoginUserData getUser() {
        return user;
    }

    public void setUser(LoginUserData user) {
        this.user = user;
    }

    /**
     * ログインユーザーデータ
     */
    public static class LoginUserData {
        private Long id;
        private String name;
        private Integer departmentId;
        private String departmentName;
        private Integer positionId;
        private String positionName;
        private String role;
        private String locationType;

        public LoginUserData() {
        }

        public LoginUserData(Long id, String name, Integer departmentId, String departmentName,
                            Integer positionId, String positionName, String role, String locationType) {
            this.id = id;
            this.name = name;
            this.departmentId = departmentId;
            this.departmentName = departmentName;
            this.positionId = positionId;
            this.positionName = positionName;
            this.role = role;
            this.locationType = locationType;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(Integer departmentId) {
            this.departmentId = departmentId;
        }

        public String getDepartmentName() {
            return departmentName;
        }

        public void setDepartmentName(String departmentName) {
            this.departmentName = departmentName;
        }

        public Integer getPositionId() {
            return positionId;
        }

        public void setPositionId(Integer positionId) {
            this.positionId = positionId;
        }

        public String getPositionName() {
            return positionName;
        }

        public void setPositionName(String positionName) {
            this.positionName = positionName;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getLocationType() {
            return locationType;
        }

        public void setLocationType(String locationType) {
            this.locationType = locationType;
        }
    }
    
    public static class Builder {
        private boolean success;
        private String message;
        private String token;
        private String refreshToken;
        private Long expiresIn;
        private LoginUserData user;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder expiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public Builder user(LoginUserData user) {
            this.user = user;
            return this;
        }

        public LoginResponse build() {
            return new LoginResponse(success, message, token, refreshToken, expiresIn, user);
        }
    }
}