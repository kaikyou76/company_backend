package com.example.companybackend.dto.auth;

import com.example.companybackend.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 登録レスポンスDTO
 */
@Data
@Builder
public class RegisterResponse {
    
    private boolean success;
    private String message;
    private UserData data;
    
    public static RegisterResponse success(User user) {
        UserData userData = new UserData(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getLocationType(),
            user.getDepartmentId(),
            user.getPositionId(),
            user.getManagerId(),
            user.getCreatedAt()
        );
        
        return RegisterResponse.builder()
            .success(true)
            .message("ユーザー登録が正常に完了しました")
            .data(userData)
            .build();
    }
    
    public static RegisterResponse error(String message) {
        return RegisterResponse.builder()
            .success(false)
            .message(message)
            .data(null)
            .build();
    }
    
    /**
     * ユーザーデータ
     */
    @Data
    @Builder
    public static class UserData {
        private Long id;
        private String username;
        private String fullName;
        private String locationType;
        private Integer departmentId;
        private Integer positionId;
        private Integer managerId;
        private OffsetDateTime createdAt;
        
        public UserData() {}
        
        public UserData(Long id, String username, String fullName, String locationType,
                       Integer departmentId, Integer positionId, Integer managerId, OffsetDateTime createdAt) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.locationType = locationType;
            this.departmentId = departmentId;
            this.positionId = positionId;
            this.managerId = managerId;
            this.createdAt = createdAt;
        }
    }
}