package com.example.companybackend.service;

import com.example.companybackend.entity.Notification;
import com.example.companybackend.entity.User;
import com.example.companybackend.repository.NotificationRepository;
import com.example.companybackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NotificationService 基本機能テストクラス
 * comsys_test_dump.sqlの実データを活用した基本的なテスト
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class NotificationServiceBasicTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private OffsetDateTime baseTime;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 基準時刻を設定（JST）
        baseTime = OffsetDateTime.now(ZoneOffset.ofHours(9));

        // テスト用ユーザーの準備
        setupTestUser();

        // テスト用ユーザーの通知をクリーンアップ
        cleanupUserNotifications();
    }

    private void setupTestUser() {
        // 実データベースから既存のユーザーを取得
        List<User> allUsers = userRepository.findAll();
        if (!allUsers.isEmpty()) {
            testUser = allUsers.get(0);
        } else {
            // フォールバック: 新しいユーザーを作成
            String timestamp = String.valueOf(System.currentTimeMillis());

            testUser = new User();
            testUser.setUsername("test_user_" + timestamp);
            testUser.setPasswordHash("$2a$10$test");
            testUser.setLocationType("office");
            testUser.setEmail("test_" + timestamp + "@company.com");
            testUser.setIsActive(true);
            testUser.setCreatedAt(baseTime.minusDays(30));
            testUser.setUpdatedAt(baseTime.minusDays(30));
            testUser = userRepository.save(testUser);
        }
    }

    private void cleanupUserNotifications() {
        if (testUser != null) {
            // テスト用ユーザーの通知を全て削除
            List<Notification> userNotifications = notificationRepository
                    .findByUserIdOrderByCreatedAtDesc(testUser.getId().intValue());
            notificationRepository.deleteAll(userNotifications);
        }
    }

    // ========== 基本通知機能テスト ==========

    @Test
    void testCreateNotification_ValidData_ShouldCreateSuccessfully() {
        // Given
        String title = "基本テスト通知";
        String message = "基本テスト用の通知メッセージです";
        String type = "system";
        Integer relatedId = 123;

        // When
        Notification notification = notificationService.createNotification(
                testUser.getId().intValue(), title, message, type, relatedId);

        // Then
        assertNotNull(notification, "通知が作成されること");
        assertEquals(testUser.getId().intValue(), notification.getUserId(), "ユーザーIDが正しいこと");
        assertEquals(title, notification.getTitle(), "タイトルが正しいこと");
        assertEquals(message, notification.getMessage(), "メッセージが正しいこと");
        assertEquals(type, notification.getType(), "タイプが正しいこと");
        assertEquals(relatedId, notification.getRelatedId(), "関連IDが正しいこと");
        assertFalse(notification.getIsRead(), "初期状態は未読であること");
        assertNotNull(notification.getCreatedAt(), "作成日時が設定されていること");

        // データベースに保存されていることを確認
        Optional<Notification> savedNotification = notificationRepository.findById(notification.getId());
        assertTrue(savedNotification.isPresent(), "通知がデータベースに保存されていること");
    }

    @Test
    void testGetUserNotifications_UnreadOnly_ShouldReturnUnreadNotifications() {
        // Given - 複数の通知を作成（一部を既読にする）
        String uniqueId = String.valueOf(System.currentTimeMillis());
        Notification notification1 = notificationService.createNotification(
                testUser.getId().intValue(), "未読通知1_" + uniqueId, "未読メッセージ1", "system", null);
        Notification notification2 = notificationService.createNotification(
                testUser.getId().intValue(), "未読通知2_" + uniqueId, "未読メッセージ2", "leave", null);
        Notification notification3 = notificationService.createNotification(
                testUser.getId().intValue(), "既読予定通知_" + uniqueId, "既読予定メッセージ", "correction", null);

        // 1つを既読にする
        notificationService.markAsRead(notification3.getId());

        // When - 未読のみ取得
        List<Notification> unreadNotifications = notificationService.getUserNotifications(testUser.getId().intValue(),
                true);

        // Then
        assertEquals(2, unreadNotifications.size(), "未読通知が2件取得されること");
        assertTrue(unreadNotifications.stream().allMatch(n -> !n.getIsRead()), "全て未読であること");
        assertTrue(unreadNotifications.stream().anyMatch(n -> n.getId().equals(notification1.getId())), "通知1が含まれること");
        assertTrue(unreadNotifications.stream().anyMatch(n -> n.getId().equals(notification2.getId())), "通知2が含まれること");
    }

    @Test
    void testMarkAsRead_ValidNotification_ShouldMarkAsRead() {
        // Given - 未読通知を作成
        String uniqueId = String.valueOf(System.currentTimeMillis());
        Notification notification = notificationService.createNotification(
                testUser.getId().intValue(), "既読テスト通知_" + uniqueId, "既読テスト用メッセージ", "system", null);
        assertFalse(notification.getIsRead(), "初期状態は未読であること");

        // When
        notificationService.markAsRead(notification.getId());

        // Then
        Optional<Notification> updatedNotification = notificationRepository.findById(notification.getId());
        assertTrue(updatedNotification.isPresent(), "通知が存在すること");
        assertTrue(updatedNotification.get().getIsRead(), "通知が既読になっていること");
    }

    @Test
    void testMarkAllAsRead_MultipleNotifications_ShouldMarkAllAsRead() {
        // Given - 複数の未読通知を作成
        String uniqueId = String.valueOf(System.currentTimeMillis());
        notificationService.createNotification(testUser.getId().intValue(), "未読通知1_" + uniqueId, "メッセージ1", "system",
                null);
        notificationService.createNotification(testUser.getId().intValue(), "未読通知2_" + uniqueId, "メッセージ2", "leave",
                null);
        notificationService.createNotification(testUser.getId().intValue(), "未読通知3_" + uniqueId, "メッセージ3", "correction",
                null);

        // 未読通知があることを確認
        List<Notification> unreadBefore = notificationService.getUserNotifications(testUser.getId().intValue(), true);
        assertEquals(3, unreadBefore.size(), "未読通知が3件あること");

        // When
        notificationService.markAllAsRead(testUser.getId().intValue());

        // Hibernateの1次キャッシュをクリアして最新の状態を取得
        entityManager.flush();
        entityManager.clear();

        // Then
        List<Notification> unreadAfter = notificationService.getUserNotifications(testUser.getId().intValue(), true);
        assertEquals(0, unreadAfter.size(), "未読通知が0件になること");

        List<Notification> allNotifications = notificationService.getUserNotifications(testUser.getId().intValue(),
                false);

        // デバッグ情報を出力
        System.out.println("=== Debug: All notifications after markAllAsRead ===");
        for (Notification n : allNotifications) {
            System.out.println("ID: " + n.getId() + ", Title: " + n.getTitle() + ", IsRead: " + n.getIsRead());
        }
        System.out.println("=== End Debug ===");

        assertTrue(allNotifications.stream().allMatch(n -> Boolean.TRUE.equals(n.getIsRead())), "全ての通知が既読になっていること");
    }

    @Test
    void testCreateNotification_WithRealDatabaseData_ShouldWorkCorrectly() {
        // Given - 実データベースのユーザーを使用
        List<User> realUsers = userRepository.findAll();
        assertFalse(realUsers.isEmpty(), "実データベースにユーザーが存在すること");

        User realUser = realUsers.get(0);
        String title = "実データベーステスト通知";
        String message = "実データベースのユーザーに対する通知テスト";
        String type = "system";

        // When
        Notification notification = notificationService.createNotification(
                realUser.getId().intValue(), title, message, type, null);

        // Then
        assertNotNull(notification, "通知が作成されること");
        assertEquals(realUser.getId().intValue(), notification.getUserId(), "ユーザーIDが正しいこと");
        assertEquals(title, notification.getTitle(), "タイトルが正しいこと");
        assertEquals(message, notification.getMessage(), "メッセージが正しいこと");
        assertEquals(type, notification.getType(), "タイプが正しいこと");
        assertFalse(notification.getIsRead(), "初期状態は未読であること");
        assertNotNull(notification.getCreatedAt(), "作成日時が設定されていること");

        System.out.println("実データベーステスト - ユーザーID: " + realUser.getId() +
                ", 通知ID: " + notification.getId());
    }

    @Test
    void testMarkAsRead_NonExistentNotification_ShouldThrowException() {
        // Given - 存在しない通知ID
        Long nonExistentId = 99999L;

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            notificationService.markAsRead(nonExistentId);
        }, "存在しない通知IDで例外が発生すること");
    }

    @Test
    void testGetUserNotifications_NonExistentUser_ShouldReturnEmptyList() {
        // Given - 存在しないユーザーID
        Integer nonExistentUserId = 99999;

        // When
        List<Notification> notifications = notificationService.getUserNotifications(nonExistentUserId, false);

        // Then
        assertNotNull(notifications, "空リストが返されること");
        assertTrue(notifications.isEmpty(), "通知リストが空であること");
    }
}