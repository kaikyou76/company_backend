package com.example.companybackend.dto.auth;

import com.example.companybackend.entity.User;

import java.time.OffsetDateTime;

/**
 * 登録レスポンスDTO
 */
public class RegisterResponse {

    private boolean success;
    private String message;
    private UserData data;

    public RegisterResponse() {
    }

    public RegisterResponse(boolean success, String message, UserData data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static RegisterResponse success(User user, String departmentName, String positionName) {
        UserData userData = new UserData(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDepartmentId(),
                departmentName,
                user.getPositionId(),
                positionName,
                user.getRole(),
                user.getLocationType(),
                user.getCreatedAt()
        );
        return new RegisterResponse(true, "User registered successfully", userData);
    }
    
    public static RegisterResponse success(User user) {
        UserData userData = new UserData(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDepartmentId(),
                null,
                user.getPositionId(),
                null,
                user.getRole(),
                user.getLocationType(),
                user.getCreatedAt()
        );
        return new RegisterResponse(true, "User registered successfully", userData);
    }

    public static RegisterResponse error(String message) {
        return new RegisterResponse(false, message, null);
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

    public UserData getData() {
        return data;
    }

    public void setData(UserData data) {
        this.data = data;
    }

    /**
     * ユーザーデータ
     */
    public static class UserData {
        private Long id;
        private String username;
        private String email;
        private Integer departmentId;
        private String departmentName;
        private Integer positionId;
        private String positionName;
        private String role;
        private String locationType;
        private OffsetDateTime createdAt;

        public UserData() {
        }

        public UserData(Long id, String username, String email, Integer departmentId, String departmentName,
                       Integer positionId, String positionName, String role, String locationType, OffsetDateTime createdAt) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.departmentId = departmentId;
            this.departmentName = departmentName;
            this.positionId = positionId;
            this.positionName = positionName;
            this.role = role;
            this.locationType = locationType;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
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

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
    
    public static class Builder {
        private boolean success;
        private String message;
        private UserData data;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder data(UserData data) {
            this.data = data;
            return this;
        }

        public RegisterResponse build() {
            return new RegisterResponse(success, message, data);
        }
    }
}