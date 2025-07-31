package com.example.companybackend.dto.auth;

import com.example.companybackend.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.AllArgsConstructor;

public class AdminRegisterResponse {

    private boolean success;
    private String message;
    private AdminUserData data;

    public AdminRegisterResponse() {}

    public AdminRegisterResponse(boolean success, String message, AdminUserData data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static AdminRegisterResponse success(com.example.companybackend.entity.User user, 
                                               String positionName, String departmentName, 
                                               String createdByUsername) {
        AdminUserData userData = AdminUserData.builder()
                .id(user.getId())  // UserエンティティのidはLong型
                .username(user.getUsername())
                .locationType(user.getLocationType())
                .departmentId(user.getDepartmentId())
                .departmentName(departmentName)
                .positionId(user.getPositionId())
                .positionName(positionName)
                .managerId(user.getManagerId())
                .createdAt(user.getCreatedAt())
                .createdByUsername(createdByUsername)
                .build();
        return new AdminRegisterResponse(true, "管理者ユーザーの登録が正常に完了しました", userData);
    }

    public static AdminRegisterResponse error(String message) {
        return new AdminRegisterResponse(false, message, null);
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

    public AdminUserData getData() {
        return data;
    }

    public void setData(AdminUserData data) {
        this.data = data;
    }

    // 内部クラス：AdminUserData
    @Builder
    @AllArgsConstructor
    public static class AdminUserData {
        private Long id;  // UserエンティティのidはLong型
        private String username;
        private String locationType;
        private Integer departmentId;
        private String departmentName;
        private Integer positionId;
        private String positionName;
        private Integer managerId;
        private String createdByUsername;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private OffsetDateTime createdAt;

        public AdminUserData() {}

        public AdminUserData(Long id, String username, String locationType,  // idはLong型
                           Integer departmentId, String departmentName, Integer positionId, String positionName,
                           Integer managerId, OffsetDateTime createdAt, String createdByUsername) {
            this.id = id;
            this.username = username;
            this.locationType = locationType;
            this.departmentId = departmentId;
            this.departmentName = departmentName;
            this.positionId = positionId;
            this.positionName = positionName;
            this.managerId = managerId;
            this.createdAt = createdAt;
            this.createdByUsername = createdByUsername;
        }

        // Getters and Setters
        public Long getId() {  // idはLong型
            return id;
        }

        public void setId(Long id) {  // idはLong型
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getLocationType() {
            return locationType;
        }

        public void setLocationType(String locationType) {
            this.locationType = locationType;
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

        public Integer getManagerId() {
            return managerId;
        }

        public void setManagerId(Integer managerId) {
            this.managerId = managerId;
        }

        public String getCreatedByUsername() {
            return createdByUsername;
        }

        public void setCreatedByUsername(String createdByUsername) {
            this.createdByUsername = createdByUsername;
        }

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
        }

        @Override
        public String toString() {
            return "AdminUserData{" +
                    "id=" + id +
                    ", username='" + username + '\'' +
                    ", locationType=" + locationType +
                    ", departmentId=" + departmentId +
                    ", departmentName='" + departmentName + '\'' +
                    ", positionId=" + positionId +
                    ", positionName='" + positionName + '\'' +
                    ", managerId=" + managerId +
                    ", createdByUsername='" + createdByUsername + '\'' +
                    ", createdAt=" + createdAt +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "AdminRegisterResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}