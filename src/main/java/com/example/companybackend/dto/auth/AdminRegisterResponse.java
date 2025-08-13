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
    @lombok.Data
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