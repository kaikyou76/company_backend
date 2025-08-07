package com.example.companybackend.service;

import com.example.companybackend.entity.User;
import com.example.companybackend.repository.UserRepository;
import com.example.companybackend.security.HtmlSanitizerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final HtmlSanitizerService htmlSanitizerService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       HtmlSanitizerService htmlSanitizerService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.htmlSanitizerService = htmlSanitizerService;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public void updateUserProfile(User user, Map<String, Object> updateRequest) {
        if (updateRequest.containsKey("fullName")) {
            String fullName = (String) updateRequest.get("fullName");
            // 清理输入数据，防止XSS攻击
            user.setFullName(htmlSanitizerService.sanitizeHtml(fullName));
        }
        if (updateRequest.containsKey("email")) {
            user.setEmail((String) updateRequest.get("email"));
        }
        if (updateRequest.containsKey("phone")) {
            String phone = (String) updateRequest.get("phone");
            // 清理输入数据，防止XSS攻击
            user.setPhone(htmlSanitizerService.sanitizeHtml(phone));
        }
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return false;
        }

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
        return true;
    }

    public Map<String, Object> getUsers(int page, int size, String search, Integer departmentId, 
                                       String role, String locationType, Boolean isActive) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findEmployeesWithFilter(
                search, departmentId, role, locationType, isActive, pageable);

        List<Map<String, Object>> userList = new ArrayList<>();
        for (User user : userPage.getContent()) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("employeeCode", user.getEmployeeId());
            userMap.put("name", user.getFullName());
            userMap.put("email", user.getEmail());
            userMap.put("role", user.getRole());
            // 注意：API规范中使用的是department和position字段（字符串），但数据库中存储的是ID
            // 在实际实现中，应该通过关联查询获取部门和职位名称
            userMap.put("departmentId", user.getDepartmentId());
            userMap.put("positionId", user.getPositionId());
            userMap.put("isActive", user.getIsActive());
            userMap.put("hireDate", user.getHireDate());
            userList.add(userMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("users", userList);
        result.put("totalCount", userPage.getTotalElements());
        result.put("currentPage", userPage.getNumber());
        result.put("totalPages", userPage.getTotalPages());
        
        return result;
    }

    public User createUser(User user) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("このユーザー名は既に使用されています");
        }
        
        // 密码加密
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        
        // 设置默认值
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        if (user.getIsActive() == null) {
            user.setIsActive(true);
        }
        
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userUpdate) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        // 更新允许的字段
        if (userUpdate.getFullName() != null) {
            existingUser.setFullName(userUpdate.getFullName());
        }
        if (userUpdate.getEmail() != null) {
            existingUser.setEmail(userUpdate.getEmail());
        }
        if (userUpdate.getPhone() != null) {
            existingUser.setPhone(userUpdate.getPhone());
        }
        if (userUpdate.getEmployeeId() != null) {
            existingUser.setEmployeeId(userUpdate.getEmployeeId());
        }
        if (userUpdate.getRole() != null) {
            existingUser.setRole(userUpdate.getRole());
        }
        if (userUpdate.getDepartmentId() != null) {
            existingUser.setDepartmentId(userUpdate.getDepartmentId());
        }
        if (userUpdate.getPositionId() != null) {
            existingUser.setPositionId(userUpdate.getPositionId());
        }
        if (userUpdate.getLocationType() != null) {
            existingUser.setLocationType(userUpdate.getLocationType());
        }
        if (userUpdate.getClientLatitude() != null) {
            existingUser.setClientLatitude(userUpdate.getClientLatitude());
        }
        if (userUpdate.getClientLongitude() != null) {
            existingUser.setClientLongitude(userUpdate.getClientLongitude());
        }
        if (userUpdate.getHireDate() != null) {
            existingUser.setHireDate(userUpdate.getHireDate());
        }
        if (userUpdate.getIsActive() != null) {
            existingUser.setIsActive(userUpdate.getIsActive());
        }
        
        existingUser.setUpdatedAt(OffsetDateTime.now());
        
        return userRepository.save(existingUser);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        // 软删除 - 设置为非活跃状态
        user.setIsActive(false);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
    }
}