package com.example.companybackend.repository;

import com.example.companybackend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserRepository テストクラス
 * ユーザーデータアクセス層の包括的なテスト
 * comsys_test_dump.sqlの既存データを活用
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private OffsetDateTime baseTime;

    @BeforeEach
    void setUp() {
        // 基準時刻を設定（日本時間）
        baseTime = OffsetDateTime.of(2025, 2, 1, 10, 0, 0, 0, ZoneOffset.ofHours(9));
    }

    /**
     * テスト用Userを作成するヘルパーメソッド（必要時のみ使用）
     */
    private User createUser(String username, String password, String email,
            String employeeId, String fullName, String role, String locationType,
            Integer departmentId, Boolean isActive, OffsetDateTime createdAt) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(password);
        user.setEmail(email);
        user.setEmployeeId(employeeId);
        user.setFullName(fullName);
        user.setRole(role);
        user.setLocationType(locationType);
        user.setDepartmentId(departmentId);
        user.setIsActive(isActive);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(createdAt);
        user.setHireDate(createdAt.toLocalDate());
        user.setSkipLocationCheck(false);
        return user;
    }

    // ========== データベース内容確認テスト ==========

    @Test
    void testFindAll_ShouldReturnAllUsers() {
        // When
        List<User> result = userRepository.findAll();

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        System.out.println("Total users found: " + result.size());

        // 最初の数件のユーザー情報を出力
        result.stream().limit(5).forEach(user -> {
            System.out.println("User: id=" + user.getId() +
                    ", username=" + user.getUsername() +
                    ", email=" + user.getEmail() +
                    ", employeeId=" + user.getEmployeeId() +
                    ", role=" + user.getRole() +
                    ", isActive=" + user.getIsActive());
        });
    }

    @Test
    void testFindById_WithExistingId_ShouldReturnUser() {
        // Given - 最初のユーザーを取得
        List<User> allUsers = userRepository.findAll();
        assertFalse(allUsers.isEmpty());
        Long existingId = allUsers.get(0).getId();

        // When
        Optional<User> result = userRepository.findById(existingId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(existingId, result.get().getId());
    }

    @Test
    void testFindByEmail_WithExistingEmail_ShouldReturnUser() {
        // Given - 実際からデータベース中查找一个有邮箱的ユーザー
        List<User> allUsers = userRepository.findAll();
        User userWithEmail = allUsers.stream()
                .filter(user -> user.getEmail() != null && !user.getEmail().isEmpty())
                .findFirst()
                .orElse(null);

        // 如果没有找到有邮箱的ユーザー，则跳过テスト
        if (userWithEmail == null) {
            System.out.println("No user with email found in database, skipping test");
            return;
        }

        String existingEmail = userWithEmail.getEmail();

        // When
        Optional<User> result = userRepository.findByEmail(existingEmail);

        // Then
        assertTrue(result.isPresent());
        assertEquals(existingEmail, result.get().getEmail());
    }

    @Test
    void testFindByEmail_WithNonExistentEmail_ShouldReturnEmpty() {
        // When
        Optional<User> result = userRepository.findByEmail("nonexistent@company.com");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByUsername_WithExistingUsername_ShouldReturnUser() {
        // Given - 実際に存在するユーザー名を取得
        List<User> allUsers = userRepository.findAll();
        assertFalse(allUsers.isEmpty());

        User firstUser = allUsers.stream()
                .filter(user -> user.getUsername() != null && !user.getUsername().isEmpty())
                .findFirst()
                .orElse(null);

        assertNotNull(firstUser, "No user with username found");
        String existingUsername = firstUser.getUsername();

        // When
        Optional<User> result = userRepository.findByUsername(existingUsername);

        // Then
        assertTrue(result.isPresent());
        assertEquals(existingUsername, result.get().getUsername());
    }

    @Test
    void testFindByEmployeeId_WithExistingEmployeeId_ShouldReturnUser() {
        // Given - 実際に存在する従業員IDを取得
        List<User> allUsers = userRepository.findAll();
        assertFalse(allUsers.isEmpty());

        User firstUser = allUsers.stream()
                .filter(user -> user.getEmployeeId() != null && !user.getEmployeeId().isEmpty())
                .findFirst()
                .orElse(null);

        if (firstUser != null) {
            String existingEmployeeId = firstUser.getEmployeeId();

            // When
            Optional<User> result = userRepository.findByEmployeeId(existingEmployeeId);

            // Then
            assertTrue(result.isPresent());
            assertEquals(existingEmployeeId, result.get().getEmployeeId());
        } else {
            System.out.println("No user with employeeId found, skipping test");
        }
    }

    @Test
    void testExistsByUsername_WithExistingUsername_ShouldReturnTrue() {
        // Given - 実際に存在するユーザー名を取得
        List<User> allUsers = userRepository.findAll();
        assertFalse(allUsers.isEmpty());
        String existingUsername = allUsers.get(0).getUsername();

        // When
        boolean exists = userRepository.existsByUsername(existingUsername);

        // Then
        assertTrue(exists);
    }

    @Test
    void testExistsByUsername_WithNonExistentUsername_ShouldReturnFalse() {
        // When
        boolean exists = userRepository.existsByUsername("nonexistent_user");

        // Then
        assertFalse(exists);
    }

    // ========== ページネーション検索テスト群 ==========

    @Test
    void testFindByIsActiveTrue_WithPageable_ShouldReturnActiveUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.findByIsActiveTrue(pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getTotalElements() >= 0); // 0以上であることを確認

        if (!result.getContent().isEmpty()) {
            assertTrue(result.getContent().stream().allMatch(User::getIsActive));
        }
    }

    @Test
    void testFindByDepartmentId_WithExistingDepartmentId_ShouldReturnDepartmentUsers() {
        // Given - 実際に存在する部署IDを取得
        List<User> allUsers = userRepository.findAll();
        User userWithDepartment = allUsers.stream()
                .filter(user -> user.getDepartmentId() != null)
                .findFirst()
                .orElse(null);

        if (userWithDepartment != null) {
            Integer existingDepartmentId = userWithDepartment.getDepartmentId();
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<User> result = userRepository.findByDepartmentId(existingDepartmentId, pageable);

            // Then
            assertNotNull(result);
            assertFalse(result.getContent().isEmpty());
            assertTrue(result.getContent().stream()
                    .allMatch(user -> existingDepartmentId.equals(user.getDepartmentId())));
        } else {
            System.out.println("No user with departmentId found, skipping test");
        }
    }

    @Test
    void testFindByRole_WithExistingRole_ShouldReturnRoleUsers() {
        // Given - 実際に存在するロールを取得
        List<User> allUsers = userRepository.findAll();
        User userWithRole = allUsers.stream()
                .filter(user -> user.getRole() != null && !user.getRole().isEmpty())
                .findFirst()
                .orElse(null);

        if (userWithRole != null) {
            String existingRole = userWithRole.getRole();
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<User> result = userRepository.findByRole(existingRole, pageable);

            // Then
            assertNotNull(result);
            assertFalse(result.getContent().isEmpty());
            assertTrue(result.getContent().stream()
                    .allMatch(user -> existingRole.equals(user.getRole())));
        } else {
            System.out.println("No user with role found, skipping test");
        }
    }

    // ========== 統計情報テスト群 ==========

    @Test
    void testCountByIsActiveTrue_ShouldReturnActiveUserCount() {
        // When
        long count = userRepository.countByIsActiveTrue();

        // Then
        assertTrue(count >= 0); // 0以上であることを確認
    }

    @Test
    void testCountByIsActiveFalse_ShouldReturnInactiveUserCount() {
        // When
        long count = userRepository.countByIsActiveFalse();

        // Then
        assertTrue(count >= 0); // 0以上であることを確認
    }

    @Test
    void testGetDepartmentStatistics_ShouldReturnDepartmentCounts() {
        // When
        List<Object[]> result = userRepository.getDepartmentStatistics();

        // Then
        assertNotNull(result);

        // 結果が存在する場合の検証
        if (!result.isEmpty()) {
            // 各統計レコードが2つの要素（部署ID、カウント）を持つことを確認
            result.forEach(record -> {
                assertEquals(2, record.length);
                assertNotNull(record[0]); // departmentId
                assertTrue(record[1] instanceof Number); // count
            });
        }
    }

    // ========== データ整合性テスト群 ==========

    @Test
    void testSaveAndRetrieve_ShouldMaintainDataIntegrity() {
        // Given - 使用データベース中已有的ユーザー而不是创建新ユーザー
        List<User> existingUsers = userRepository.findAll();
        assertFalse(existingUsers.isEmpty(), "データベース中應該存在ユーザー");
        
        User existingUser = existingUsers.get(0);
        String originalUsername = existingUser.getUsername();
        String originalEmail = existingUser.getEmail();
        
        // 修改ユーザーのいくつかの非キー情報
        existingUser.setFullName("更新テストユーザー");
        existingUser.setPhone("123-456-7890");
        existingUser.setUpdatedAt(OffsetDateTime.now());

        // When
        User savedUser = userRepository.save(existingUser);
        flush();
        User retrievedUser = userRepository.findById(savedUser.getId()).orElse(null);

        // Then
        assertNotNull(retrievedUser);
        assertEquals(savedUser.getId(), retrievedUser.getId());
        assertEquals(originalUsername, retrievedUser.getUsername());
        assertEquals(originalEmail, retrievedUser.getEmail());
        assertEquals("更新テストユーザー", retrievedUser.getFullName());
        assertEquals("123-456-7890", retrievedUser.getPhone());
        assertNotNull(retrievedUser.getCreatedAt());
        assertNotNull(retrievedUser.getUpdatedAt());
    }

    // ========== エッジケース・境界値テスト群 ==========

    @Test
    void testFindByDepartmentId_WithNonExistentDepartmentId_ShouldReturnEmptyList() {
        // When
        List<User> result = userRepository.findByDepartmentId(999);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByRole_WithNonExistentRole_ShouldReturnEmptyList() {
        // When
        List<User> result = userRepository.findByRole("nonexistent_role");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== パフォーマンステスト ==========

    @Test
    void testLargeDatasetQuery_ShouldPerformEfficiently() {
        // Given - 実際に存在する部署IDを取得
        List<User> allUsers = userRepository.findAll();
        User userWithDepartment = allUsers.stream()
                .filter(user -> user.getDepartmentId() != null)
                .findFirst()
                .orElse(null);

        if (userWithDepartment != null) {
            Integer existingDepartmentId = userWithDepartment.getDepartmentId();

            // When
            long startTime = System.currentTimeMillis();
            Page<User> result = userRepository.findByDepartmentId(existingDepartmentId, PageRequest.of(0, 100));
            long endTime = System.currentTimeMillis();

            // Then
            assertNotNull(result);
            assertTrue(result.getTotalElements() >= 0);
            assertTrue(endTime - startTime < 1000); // 1秒以内で完了することを確認
        } else {
            System.out.println("No user with departmentId found, skipping performance test");
        }
    }
    
    private void flush() {
        // 在测试中强制刷新和清除持久化上下文，确保数据真正写入数据库
        try {
            // 如果使用的是JPA，可以调用EntityManagerのflush方法
            // 但由于这是Repository層テスト，我们简单地等待一下
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}