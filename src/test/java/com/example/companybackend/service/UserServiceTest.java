package com.example.companybackend.service;

import com.example.companybackend.entity.User;
import com.example.companybackend.repository.UserRepository;
import com.example.companybackend.security.HtmlSanitizerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private HtmlSanitizerService htmlSanitizerService;

    @InjectMocks
    private UserService userService;

    // テスト用定数
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_ENCODED_PASSWORD = "$2a$10$encodedPassword";
    private static final String TEST_FULL_NAME = "テスト太郎";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PHONE = "090-1234-5678";
    private static final String TEST_EMPLOYEE_ID = "EMP001";
    private static final Integer TEST_DEPARTMENT_ID = 1;
    private static final Integer TEST_POSITION_ID = 2;
    private static final String TEST_ROLE = "employee";
    private static final String TEST_LOCATION_TYPE = "office";

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
    }

    // ========== ユーザー検索テスト ==========

    @Test
    void testFindByUsername_WithExistingUser_ShouldReturnUser() {
        // Given
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.findByUsername(TEST_USERNAME);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USERNAME, result.getUsername());
        assertEquals(TEST_FULL_NAME, result.getFullName());
        assertEquals(TEST_EMAIL, result.getEmail());

        verify(userRepository).findByUsername(TEST_USERNAME);
    }

    @Test
    void testFindByUsername_WithNonExistentUser_ShouldReturnNull() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        User result = userService.findByUsername("nonexistent");

        // Then
        assertNull(result);
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void testFindById_WithExistingUser_ShouldReturnUser() {
        // Given
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.findById(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getId());
        assertEquals(TEST_USERNAME, result.getUsername());
        assertEquals(TEST_FULL_NAME, result.getFullName());

        verify(userRepository).findById(TEST_USER_ID);
    }

    @Test
    void testFindById_WithNonExistentUser_ShouldReturnNull() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        User result = userService.findById(999L);

        // Then
        assertNull(result);
        verify(userRepository).findById(999L);
    }

    // ========== ユーザープロフィール更新テスト ==========

    @Test
    void testUpdateUserProfile_WithValidData_ShouldUpdateUser() {
        // Given
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("fullName", "更新太郎");
        updateRequest.put("email", "updated@example.com");
        updateRequest.put("phone", "090-9876-5432");
        
        // Mock HtmlSanitizerService
        when(htmlSanitizerService.sanitizeHtml(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUserProfile(testUser, updateRequest);

        // Then
        assertEquals("更新太郎", testUser.getFullName());
        assertEquals("updated@example.com", testUser.getEmail());
        assertEquals("090-9876-5432", testUser.getPhone());
        assertNotNull(testUser.getUpdatedAt());

        verify(userRepository).save(testUser);
        verify(htmlSanitizerService, times(2)).sanitizeHtml(anyString());
    }

    @Test
    void testUpdateUserProfile_WithPartialData_ShouldUpdateOnlySpecifiedFields() {
        // Given
        String originalEmail = testUser.getEmail();
        String originalPhone = testUser.getPhone();

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("fullName", "部分更新太郎");
        
        // Mock HtmlSanitizerService
        when(htmlSanitizerService.sanitizeHtml(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUserProfile(testUser, updateRequest);

        // Then
        assertEquals("部分更新太郎", testUser.getFullName());
        assertEquals(originalEmail, testUser.getEmail()); // 変更されない
        assertEquals(originalPhone, testUser.getPhone()); // 変更されない
        
        verify(htmlSanitizerService).sanitizeHtml(anyString());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUserProfile_WithEmptyRequest_ShouldOnlyUpdateTimestamp() {
        // Given
        String originalFullName = testUser.getFullName();
        String originalEmail = testUser.getEmail();
        OffsetDateTime originalUpdatedAt = testUser.getUpdatedAt();

        Map<String, Object> updateRequest = new HashMap<>();

        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUserProfile(testUser, updateRequest);

        // Then
        assertEquals(originalFullName, testUser.getFullName());
        assertEquals(originalEmail, testUser.getEmail());
        assertNotNull(testUser.getUpdatedAt());

        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUserProfile_WithNullValues_ShouldHandleGracefully() {
        // Given
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("fullName", null);
        updateRequest.put("email", null);
        
        // Mock HtmlSanitizerService
        when(htmlSanitizerService.sanitizeHtml(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUserProfile(testUser, updateRequest);

        // Then
        // null値は設定されるが、エラーは発生しない
        assertNull(testUser.getFullName());
        assertNull(testUser.getEmail());

        verify(userRepository).save(testUser);
        verify(htmlSanitizerService, times(1)).sanitizeHtml(any()); // Only fullName is sanitized, phone is not in updateRequest
    }

    // ========== パスワード変更テスト ==========

    @Test
    void testChangePassword_WithValidCredentials_ShouldReturnTrue() {
        // Given
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        String newEncodedPassword = "$2a$10$newEncodedPassword";

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(oldPassword, TEST_ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(newEncodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        boolean result = userService.changePassword(TEST_USERNAME, oldPassword, newPassword);

        // Then
        assertTrue(result);
        assertEquals(newEncodedPassword, testUser.getPasswordHash());
        assertNotNull(testUser.getUpdatedAt());

        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches(oldPassword, TEST_ENCODED_PASSWORD);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
    }

    @Test
    void testChangePassword_WithInvalidOldPassword_ShouldReturnFalse() {
        // Given
        String oldPassword = "wrongPassword";
        String newPassword = "newPassword";

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(oldPassword, testUser.getPasswordHash())).thenReturn(false);

        // When
        boolean result = userService.changePassword(TEST_USERNAME, oldPassword, newPassword);

        // Then
        assertFalse(result);

        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches(oldPassword, testUser.getPasswordHash());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangePassword_WithNonExistentUser_ShouldReturnFalse() {
        // Given
        String username = "nonexistent";
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When
        boolean result = userService.changePassword(username, oldPassword, newPassword);

        // Then
        assertFalse(result);

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // ========== ユーザー一覧取得テスト ==========

    @Test
    void testGetUsers_WithNoFilters_ShouldReturnAllUsers() {
        // Given
        List<User> users = Arrays.asList(
                createTestUser(1L, "ceo@company.com", "CEO"),
                createTestUser(2L, "director@company.com", "Director"),
                createTestUser(3L, "admin@company.com", "Admin"));
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());

        when(userRepository.findEmployeesWithFilter(null, null, null, null, null, pageable))
                .thenReturn(userPage);

        // When
        Map<String, Object> result = userService.getUsers(0, 10, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(3L, result.get("totalCount"));
        assertEquals(0, result.get("currentPage"));
        assertEquals(1, result.get("totalPages"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> userList = (List<Map<String, Object>>) result.get("users");
        assertEquals(3, userList.size());
        assertEquals(1L, userList.get(0).get("id"));
        assertEquals("EMP001", userList.get(0).get("employeeCode"));
        assertEquals("CEO", userList.get(0).get("name"));

        verify(userRepository).findEmployeesWithFilter(null, null, null, null, null, pageable);
    }

    @Test
    void testGetUsers_WithFilters_ShouldReturnFilteredUsers() {
        // Given
        List<User> filteredUsers = Arrays.asList(
                createTestUser(1L, "manager1", "マネージャー1"));
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(filteredUsers, pageable, filteredUsers.size());

        when(userRepository.findEmployeesWithFilter("manager", 1, "manager", "office", true, pageable))
                .thenReturn(userPage);

        // When
        Map<String, Object> result = userService.getUsers(0, 10, "manager", 1, "manager", "office", true);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.get("totalCount"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> userList = (List<Map<String, Object>>) result.get("users");
        assertEquals(1, userList.size());
        assertEquals("マネージャー1", userList.get(0).get("name"));

        verify(userRepository).findEmployeesWithFilter("manager", 1, "manager", "office", true, pageable);
    }

    @Test
    void testGetUsers_WithPagination_ShouldReturnCorrectPage() {
        // Given
        List<User> users = Arrays.asList(
                createTestUser(6L, "user6", "ユーザー6"),
                createTestUser(7L, "user7", "ユーザー7"));
        Pageable pageable = PageRequest.of(1, 5); // 2ページ目、5件ずつ
        Page<User> userPage = new PageImpl<>(users, pageable, 12); // 全12件

        when(userRepository.findEmployeesWithFilter(null, null, null, null, null, pageable))
                .thenReturn(userPage);

        // When
        Map<String, Object> result = userService.getUsers(1, 5, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(12L, result.get("totalCount"));
        assertEquals(1, result.get("currentPage"));
        assertEquals(3, result.get("totalPages")); // 12件を5件ずつで3ページ

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> userList = (List<Map<String, Object>>) result.get("users");
        assertEquals(2, userList.size());
    }

    // ========== ユーザー作成テスト ==========

    @Test
    void testCreateUser_WithValidData_ShouldCreateUser() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPasswordHash("plainPassword");
        newUser.setFullName("新規ユーザー");
        newUser.setEmail("newuser@example.com");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$10$encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        // When
        User result = userService.createUser(newUser);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("newuser", result.getUsername());
        assertEquals("$2a$10$encodedNewPassword", result.getPasswordHash());
        assertEquals("新規ユーザー", result.getFullName());
        assertEquals("newuser@example.com", result.getEmail());
        assertTrue(result.getIsActive());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        verify(userRepository).existsByUsername("newuser");
        verify(passwordEncoder).encode("plainPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_WithExistingUsername_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setUsername("existinguser");
        newUser.setPasswordHash("plainPassword");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(newUser);
        });

        assertEquals("このユーザー名は既に使用されています", exception.getMessage());

        verify(userRepository).existsByUsername("existinguser");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_WithNullIsActive_ShouldSetDefaultTrue() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPasswordHash("plainPassword");
        newUser.setIsActive(null); // null値

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.createUser(newUser);

        // Then
        assertTrue(result.getIsActive()); // デフォルトでtrueが設定される
        verify(userRepository).save(any(User.class));
    }

    // ========== ユーザー更新テスト ==========

    @Test
    void testUpdateUser_WithValidData_ShouldUpdateUser() {
        // Given
        User userUpdate = new User();
        userUpdate.setFullName("更新された名前");
        userUpdate.setEmail("updated@example.com");
        userUpdate.setPhone("090-9999-8888");
        userUpdate.setRole("manager");

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.updateUser(TEST_USER_ID, userUpdate);

        // Then
        assertNotNull(result);
        assertEquals("更新された名前", result.getFullName());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("090-9999-8888", result.getPhone());
        assertEquals("manager", result.getRole());
        assertNotNull(result.getUpdatedAt());

        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUser_WithPartialData_ShouldUpdateOnlySpecifiedFields() {
        // Given
        String originalEmail = testUser.getEmail();
        String originalPhone = testUser.getPhone();

        User userUpdate = new User();
        userUpdate.setFullName("部分更新名前");
        // email と phone は設定しない

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.updateUser(TEST_USER_ID, userUpdate);

        // Then
        assertEquals("部分更新名前", result.getFullName());
        assertEquals(originalEmail, result.getEmail()); // 変更されない
        assertEquals(originalPhone, result.getPhone()); // 変更されない

        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUser_WithNonExistentUser_ShouldThrowException() {
        // Given
        User userUpdate = new User();
        userUpdate.setFullName("更新名前");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUser(999L, userUpdate);
        });

        assertEquals("ユーザーが見つかりません", exception.getMessage());

        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUser_WithLocationData_ShouldUpdateLocationFields() {
        // Given
        User userUpdate = new User();
        userUpdate.setLocationType("client");
        userUpdate.setClientLatitude(35.6812);
        userUpdate.setClientLongitude(139.7671);

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.updateUser(TEST_USER_ID, userUpdate);

        // Then
        assertEquals("client", result.getLocationType());
        assertEquals(35.6812, result.getClientLatitude());
        assertEquals(139.7671, result.getClientLongitude());

        verify(userRepository).save(testUser);
    }

    // ========== ユーザー削除テスト ==========

    @Test
    void testDeleteUser_WithExistingUser_ShouldSoftDelete() {
        // Given
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.deleteUser(TEST_USER_ID);

        // Then
        assertFalse(testUser.getIsActive()); // ソフト削除でfalseに設定
        assertNotNull(testUser.getUpdatedAt());

        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository).save(testUser);
    }

    @Test
    void testDeleteUser_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.deleteUser(999L);
        });

        assertEquals("ユーザーが見つかりません", exception.getMessage());

        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    // ========== エッジケース・境界値テスト ==========

    @Test
    void testGetUsers_WithMinimumSize_ShouldHandleCorrectly() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);
        Page<User> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        when(userRepository.findEmployeesWithFilter(null, null, null, null, null, pageable))
                .thenReturn(emptyPage);

        // When
        Map<String, Object> result = userService.getUsers(0, 1, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(0L, result.get("totalCount"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> userList = (List<Map<String, Object>>) result.get("users");
        assertTrue(userList.isEmpty());
    }

    @Test
    void testCreateUser_WithAllOptionalFields_ShouldCreateSuccessfully() {
        // Given
        User newUser = new User();
        newUser.setUsername("fulluser");
        newUser.setPasswordHash("password");
        newUser.setFullName("フルユーザー");
        newUser.setEmail("full@example.com");
        newUser.setPhone("090-1111-2222");
        newUser.setEmployeeId("EMP999");
        newUser.setRole("admin");
        newUser.setDepartmentId(5);
        newUser.setPositionId(3);
        newUser.setLocationType("remote");
        newUser.setHireDate(LocalDate.of(2025, 1, 1));
        newUser.setIsActive(true);

        when(userRepository.existsByUsername("fulluser")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(20L);
            return user;
        });

        // When
        User result = userService.createUser(newUser);

        // Then
        assertNotNull(result);
        assertEquals("fulluser", result.getUsername());
        assertEquals("フルユーザー", result.getFullName());
        assertEquals("full@example.com", result.getEmail());
        assertEquals("EMP999", result.getEmployeeId());
        assertEquals("admin", result.getRole());
        assertEquals(5, result.getDepartmentId());
        assertEquals(3, result.getPositionId());
        assertEquals("remote", result.getLocationType());
        assertEquals(LocalDate.of(2025, 1, 1), result.getHireDate());
        assertTrue(result.getIsActive());
    }

    // ========== ヘルパーメソッド ==========

    private User createTestUser() {
        return createTestUser(TEST_USER_ID, TEST_USERNAME, TEST_FULL_NAME);
    }

    private User createTestUser(Long id, String username, String fullName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(TEST_ENCODED_PASSWORD);
        user.setFullName(fullName);
        user.setEmail(TEST_EMAIL);
        user.setPhone(TEST_PHONE);
        user.setEmployeeId(TEST_EMPLOYEE_ID);
        user.setRole(TEST_ROLE);
        user.setDepartmentId(TEST_DEPARTMENT_ID);
        user.setPositionId(TEST_POSITION_ID);
        user.setLocationType(TEST_LOCATION_TYPE);
        user.setHireDate(LocalDate.of(2025, 1, 1));
        user.setIsActive(true);
        user.setCreatedAt(OffsetDateTime.now(ZoneOffset.ofHours(9)));
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.ofHours(9)));
        return user;
    }
}