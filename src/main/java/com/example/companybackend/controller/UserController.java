package com.example.companybackend.controller;

import com.example.companybackend.entity.User;
import com.example.companybackend.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ユーザー関連コントローラー
 * ユーザー情報の取得、作成、更新などを行う
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "ユーザー", description = "ユーザー管理API")
public class UserController {

    private final UserService userService;
    
    // 创建StringEscapeUtils实例用于HTML转义
    private static final StringEscapeUtils stringEscapeUtils = new StringEscapeUtils();

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse("認証が必要です"));
            }

            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("ユーザーが見つかりません"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", createUserProfileResponse(user));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ユーザー情報の取得に失敗しました"));
        }
    }

    /**
     * 更新当前用户情報
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateCurrentUser(@RequestBody Map<String, Object> updateRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse("認証が必要です"));
            }

            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("ユーザーが見つかりません"));
            }

            // 更新用户信息
            userService.updateUserProfile(user, updateRequest);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", user.getId());
            responseData.put("name", escapeHtml(user.getFullName()));
            responseData.put("email", user.getEmail()); // Email通常不需要转義，因为它有特定格式
            responseData.put("phoneNumber", escapeHtml(user.getPhone()));
            responseData.put("updatedAt", user.getUpdatedAt());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "プロフィールを更新しました");
            response.put("data", responseData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("プロフィールの更新に失敗しました"));
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse("認証が必要です"));
            }

            String username = authentication.getName();
            String oldPassword = passwordRequest.get("oldPassword");
            String newPassword = passwordRequest.get("newPassword");

            if (oldPassword == null || oldPassword.isBlank()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "現在のパスワードを入力してください");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            if (newPassword == null || newPassword.isBlank()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "新しいパスワードを入力してください");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            if (newPassword.length() < 6) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "パスワードは6文字以上である必要があります");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            boolean success = userService.changePassword(username, oldPassword, newPassword);
            
            if (!success) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "現在のパスワードが正しくありません");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "パスワードが正常に変更されました");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("パスワードの変更に失敗しました"));
        }
    }

    /**
     * 获取用户列表 (仅限管理员)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String locationType,
            @RequestParam(required = false) Boolean isActive) {
        try {
            Map<String, Object> result = userService.getUsers(page, size, search, departmentId, role, locationType, isActive);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ユーザー一覧の取得に失敗しました"));
        }
    }

    /**
     * 根据ID获取用户详情 (仅限管理员)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("ユーザーが見つかりません"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", createUserDetailResponse(user));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ユーザー情報の取得に失敗しました"));
        }
    }

    /**
     * 创建新用户 (仅限管理员)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            // 验证必填字段
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("ユーザー名は必須です"));
            }
            
            if (user.getFullName() == null || user.getFullName().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("氏名は必須です"));
            }
            
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("メールアドレスは必須です"));
            }
            
            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("パスワードは必須です"));
            }
            
            if (user.getLocationType() == null || user.getLocationType().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("勤務場所タイプは必須です"));
            }
            
            // location_typeの验证 (officeまたはclient)
            if (!"office".equals(user.getLocationType()) && !"client".equals(user.getLocationType())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("勤務場所タイプは 'office' または 'client' である必要があります"));
            }
            
            // clientの場合、緯度経度はオプションだが、両方設定するか両方nullにする
            if ("client".equals(user.getLocationType())) {
                if ((user.getClientLatitude() == null) != (user.getClientLongitude() == null)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("クライアント勤務地の緯度と経度は両方設定するか、両方nullにする必要があります"));
                }
            } else {
                // officeの場合は緯度経度をnullに設定
                user.setClientLatitude(null);
                user.setClientLongitude(null);
            }
            
            User createdUser = userService.createUser(user);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", createdUser.getId());
            responseData.put("employeeCode", createdUser.getEmployeeId());
            responseData.put("name", createdUser.getFullName());
            responseData.put("email", createdUser.getEmail());
            responseData.put("role", createdUser.getRole());
            responseData.put("departmentId", createdUser.getDepartmentId());
            responseData.put("positionId", createdUser.getPositionId());
            responseData.put("isActive", createdUser.getIsActive());
            responseData.put("hireDate", createdUser.getHireDate());
            responseData.put("phoneNumber", createdUser.getPhone());
            responseData.put("locationType", createdUser.getLocationType());
            responseData.put("clientLatitude", createdUser.getClientLatitude());
            responseData.put("clientLongitude", createdUser.getClientLongitude());
            responseData.put("createdAt", createdUser.getCreatedAt());
            responseData.put("updatedAt", createdUser.getUpdatedAt());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "ユーザーが正常に作成されました");
            response.put("data", responseData);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ユーザー作成中にエラーが発生しました"));
        }
    }

    /**
     * 更新用户信息 (仅限管理员)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userUpdate) {
        try {
            User existingUser = userService.findById(id);
            
            if (existingUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("ユーザーが見つかりません"));
            }

            User updatedUser = userService.updateUser(id, userUpdate);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", updatedUser.getId());
            responseData.put("employeeCode", updatedUser.getEmployeeId());
            responseData.put("name", updatedUser.getFullName());
            responseData.put("email", updatedUser.getEmail());
            responseData.put("role", updatedUser.getRole());
            responseData.put("departmentId", updatedUser.getDepartmentId());
            responseData.put("positionId", updatedUser.getPositionId());
            responseData.put("isActive", updatedUser.getIsActive());
            responseData.put("hireDate", updatedUser.getHireDate());
            responseData.put("phoneNumber", updatedUser.getPhone());
            responseData.put("locationType", updatedUser.getLocationType());
            responseData.put("clientLatitude", updatedUser.getClientLatitude());
            responseData.put("clientLongitude", updatedUser.getClientLongitude());
            responseData.put("createdAt", updatedUser.getCreatedAt());
            responseData.put("updatedAt", updatedUser.getUpdatedAt());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "ユーザー情報が正常に更新されました");
            response.put("data", responseData);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ユーザー情報の更新に失敗しました"));
        }
    }

    /**
     * 删除用户 (仅限管理员)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("ユーザーが見つかりません"));
            }

            userService.deleteUser(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "ユーザーが正常に削除されました");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ユーザー削除中にエラーが発生しました"));
        }
    }

    // 辅助方法
    private Map<String, Object> createUserProfileResponse(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("employeeCode", user.getEmployeeId());
        profile.put("name", user.getFullName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        // 注意：API规范中使用的是department和position字段，但数据库中存储的是ID
        // 在实际实现中，应该通过关联查询获取部门和职位名称
        profile.put("departmentId", user.getDepartmentId());
        profile.put("positionId", user.getPositionId());
        profile.put("hireDate", user.getHireDate());
        profile.put("phoneNumber", user.getPhone());
        // API规范中包含remainingPaidLeave，但User实体中没有该字段
        profile.put("remainingPaidLeave", 0); // 临时値，实际应该从其他表获取
        return profile;
    }

    private Map<String, Object> createUserDetailResponse(User user) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", user.getId());
        detail.put("employeeCode", user.getEmployeeId());
        detail.put("name", user.getFullName());
        detail.put("username", user.getUsername());
        detail.put("email", user.getEmail());
        detail.put("role", user.getRole());
        detail.put("departmentId", user.getDepartmentId());
        detail.put("positionId", user.getPositionId());
        detail.put("isActive", user.getIsActive());
        detail.put("hireDate", user.getHireDate());
        detail.put("phoneNumber", user.getPhone());
        detail.put("locationType", user.getLocationType());
        detail.put("clientLatitude", user.getClientLatitude());
        detail.put("clientLongitude", user.getClientLongitude());
        detail.put("createdAt", user.getCreatedAt());
        detail.put("updatedAt", user.getUpdatedAt());
        return detail;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
    
    /**
     * 对字符串进行HTML转义以防止XSS攻击
     * @param input 输入字符串
     * @return 转義後の字符串
     */
    private String escapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return StringEscapeUtils.escapeHtml4(input);
    }
}