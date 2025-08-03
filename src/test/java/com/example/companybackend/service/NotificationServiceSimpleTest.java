package com.example.companybackend.service;

import com.example.companybackend.entity.Notification;
import com.example.companybackend.entity.User;
import com.example.companybackend.repository.NotificationRepository;
import com.example.companybackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NotificationService 簡単なテストクラス
 * comsys_test_dump.sqlの実データを活用した基本的なテスト
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class NotificationServiceSimpleTest {

        @Autowired
        private NotificationService notificationService;

        @Autowired
        private NotificationRepository notificationRepository;

        @Autowired
        private UserRepository userRepository;

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
                assertEquals(realUser.getId(), notification.getUserId(), "ユーザーIDが正しいこと");
                assertEquals(title, notification.getTitle(), "タイトルが正しいこと");
                assertEquals(message, notification.getMessage(), "メッセージが正しいこと");
                assertEquals(type, notification.getType(), "タイプが正しいこと");
                assertFalse(notification.getIsRead(), "初期状態は未読であること");
                assertNotNull(notification.getCreatedAt(), "作成日時が設定されていること");

                System.out.println("実データベーステスト - ユーザーID: " + realUser.getId() +
                                ", 通知ID: " + notification.getId());
        }

        @Test
        void testGetUserNotifications_ShouldReturnNotifications() {
                // Given - 実データベースのユーザーを使用
                List<User> realUsers = userRepository.findAll();
                assertFalse(realUsers.isEmpty(), "実データベースにユーザーが存在すること");

                User realUser = realUsers.get(0);

                // 通知を作成
                notificationService.createNotification(
                                realUser.getId().intValue(), "テスト通知1", "テストメッセージ1", "system", null);
                notificationService.createNotification(
                                realUser.getId().intValue(), "テスト通知2", "テストメッセージ2", "leave", null);

                // When
                List<Notification> notifications = notificationService.getUserNotifications(realUser.getId().intValue(),
                                false);

                // Then
                assertNotNull(notifications, "通知リストが取得されること");
                assertTrue(notifications.size() >= 2, "作成した通知が含まれること");

                System.out.println("取得した通知数: " + notifications.size());
        }
}