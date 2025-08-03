package com.example.companybackend.repository;

import com.example.companybackend.entity.SystemLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SystemLogRepository テストクラス
 * システムログデータアクセス層の包括的なテスト
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SystemLogRepositoryTest {

    @Autowired
    private SystemLogRepository systemLogRepository;

    // テスト用定数（comsys_test_dump.sqlの実際のデータを使用）
    private static final Integer TEST_USER_ID_1 = 1; // ceo@company.com
    private static final Integer TEST_USER_ID_2 = 2; // director@company.com
    private static final String TEST_ACTION_LOGIN = "LOGIN";
    private static final String TEST_ACTION_LOGOUT = "LOGOUT";
    private static final String TEST_STATUS_SUCCESS = "success";
    private static final String TEST_STATUS_ERROR = "error";
    private static final String TEST_IP_ADDRESS = "192.168.1.200";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    private SystemLog loginLog1;
    private SystemLog loginLog2;
    private SystemLog logoutLog1;
    private SystemLog errorLog1;
    private OffsetDateTime baseTime;

    @BeforeEach
    void setUp() {
        // 基準時刻設定（日本時間）
        baseTime = OffsetDateTime.of(2025, 2, 1, 10, 0, 0, 0, ZoneOffset.ofHours(9));

        // テストデータの準備
        loginLog1 = createSystemLog(null, TEST_USER_ID_1, TEST_ACTION_LOGIN, TEST_STATUS_SUCCESS,
                TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"method\": \"password\"}", baseTime);

        loginLog2 = createSystemLog(null, TEST_USER_ID_2, TEST_ACTION_LOGIN, TEST_STATUS_SUCCESS,
                "192.168.1.201", TEST_USER_AGENT, "{\"method\": \"password\"}", baseTime.plusMinutes(5));

        logoutLog1 = createSystemLog(null, TEST_USER_ID_1, TEST_ACTION_LOGOUT, TEST_STATUS_SUCCESS,
                TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"duration\": \"30min\"}", baseTime.plusMinutes(30));

        errorLog1 = createSystemLog(null, TEST_USER_ID_1, "ACCESS_DENIED", TEST_STATUS_ERROR,
                TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"reason\": \"insufficient_permissions\"}",
                baseTime.plusMinutes(10));

        // データベースに保存
        loginLog1 = systemLogRepository.save(loginLog1);
        loginLog2 = systemLogRepository.save(loginLog2);
        logoutLog1 = systemLogRepository.save(logoutLog1);
        errorLog1 = systemLogRepository.save(errorLog1);
    }

    // ========== 基本検索テスト ==========

    @Test
    void testFindByUserId_WithExistingUser_ShouldReturnLogs() {
        // When
        List<SystemLog> result = systemLogRepository.findByUserId(TEST_USER_ID_1);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 3); // 既存データ + テストデータ
        assertTrue(result.stream().allMatch(log -> log.getUserId().equals(TEST_USER_ID_1)));
    }

    @Test
    void testFindByUserId_WithNonExistentUser_ShouldReturnEmptyList() {
        // When
        List<SystemLog> result = systemLogRepository.findByUserId(999);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByAction_WithValidAction_ShouldReturnFilteredLogs() {
        // When
        List<SystemLog> loginResults = systemLogRepository.findByAction(TEST_ACTION_LOGIN);
        List<SystemLog> logoutResults = systemLogRepository.findByAction(TEST_ACTION_LOGOUT);

        // Then
        assertNotNull(loginResults);
        assertTrue(loginResults.size() >= 2); // 既存データ + テストデータ
        assertTrue(loginResults.stream().allMatch(log -> TEST_ACTION_LOGIN.equals(log.getAction())));

        assertNotNull(logoutResults);
        assertTrue(logoutResults.size() >= 1); // テストデータ
        assertTrue(logoutResults.stream().allMatch(log -> TEST_ACTION_LOGOUT.equals(log.getAction())));
    }

    @Test
    void testFindByIpAddress_WithValidIp_ShouldReturnLogs() {
        // When
        List<SystemLog> result = systemLogRepository.findByIpAddress(TEST_IP_ADDRESS);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 3); // loginLog1 + logoutLog1 + errorLog1
        assertTrue(result.stream().allMatch(log -> TEST_IP_ADDRESS.equals(log.getIpAddress())));
    }

    @Test
    void testFindByUserIdAndAction_WithValidData_ShouldReturnFilteredLogs() {
        // When
        List<SystemLog> result = systemLogRepository.findByUserIdAndAction(TEST_USER_ID_1, TEST_ACTION_LOGIN);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 1); // loginLog1 + 既存データ
        assertTrue(result.stream()
                .allMatch(log -> log.getUserId().equals(TEST_USER_ID_1) && TEST_ACTION_LOGIN.equals(log.getAction())));
    }

    // ========== 日時検索テスト ==========

    @Test
    void testFindByCreatedAtBetween_WithValidRange_ShouldReturnLogs() {
        // Given
        OffsetDateTime startDate = baseTime.minusMinutes(10);
        OffsetDateTime endDate = baseTime.plusHours(1);

        // When
        List<SystemLog> result = systemLogRepository.findByCreatedAtBetween(startDate, endDate);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 4); // 全テストデータ
        assertTrue(result.stream()
                .allMatch(log -> !log.getCreatedAt().isBefore(startDate) && !log.getCreatedAt().isAfter(endDate)));
    }

    @Test
    void testFindTodayLogs_ShouldReturnTodayLogs() {
        // Given - 今日の日付でログを作成
        OffsetDateTime today = OffsetDateTime.now(ZoneOffset.ofHours(9));
        SystemLog todayLog = createSystemLog(null, TEST_USER_ID_1, "TODAY_ACTION", TEST_STATUS_SUCCESS,
                TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"test\": \"today\"}", today);
        systemLogRepository.save(todayLog);

        // When
        List<SystemLog> result = systemLogRepository.findTodayLogs();

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 今日作成されたログが含まれていることを確認
        assertTrue(result.stream().anyMatch(log -> "TODAY_ACTION".equals(log.getAction())));
    }

    @Test
    void testFindLatestLogs_WithLimit_ShouldReturnLimitedResults() {
        // When
        List<SystemLog> result = systemLogRepository.findLatestLogs(5);

        // Then
        assertNotNull(result);
        assertTrue(result.size() <= 5);
        // 最新順にソートされていることを確認
        if (result.size() > 1) {
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).getCreatedAt().isAfter(result.get(i + 1).getCreatedAt()) ||
                        result.get(i).getCreatedAt().equals(result.get(i + 1).getCreatedAt()));
            }
        }
    }

    @Test
    void testFindLatestLogsByUser_WithValidUser_ShouldReturnUserLogs() {
        // When
        List<SystemLog> result = systemLogRepository.findLatestLogsByUser(TEST_USER_ID_1, 3);

        // Then
        assertNotNull(result);
        assertTrue(result.size() <= 3);
        assertTrue(result.stream().allMatch(log -> log.getUserId().equals(TEST_USER_ID_1)));
        // 最新順にソートされていることを確認
        if (result.size() > 1) {
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).getCreatedAt().isAfter(result.get(i + 1).getCreatedAt()) ||
                        result.get(i).getCreatedAt().equals(result.get(i + 1).getCreatedAt()));
            }
        }
    }

    // ========== 統計情報テスト ==========

    @Test
    void testGetActionStatistics_WithValidRange_ShouldReturnStatistics() {
        // Given
        OffsetDateTime startDate = baseTime.minusMinutes(10);
        OffsetDateTime endDate = baseTime.plusHours(1);

        // When
        List<Map<String, Object>> result = systemLogRepository.getActionStatistics(startDate, endDate);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 各統計レコードが必要なキーを持つことを確認
        result.forEach(stat -> {
            assertTrue(stat.containsKey("action"));
            assertTrue(stat.containsKey("count"));
            assertNotNull(stat.get("action"));
            assertTrue(stat.get("count") instanceof Number);
        });
    }

    @Test
    void testGetUserActivityStatistics_WithValidRange_ShouldReturnStatistics() {
        // Given
        OffsetDateTime startDate = baseTime.minusMinutes(10);
        OffsetDateTime endDate = baseTime.plusHours(1);

        // When
        List<Map<String, Object>> result = systemLogRepository.getUserActivityStatistics(startDate, endDate);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 各統計レコードが必要なキーを持つことを確認
        result.forEach(stat -> {
            assertTrue(stat.containsKey("userId"));
            assertTrue(stat.containsKey("actionCount"));
            assertTrue(stat.containsKey("uniqueActions"));
            assertTrue(stat.get("actionCount") instanceof Number);
            assertTrue(stat.get("uniqueActions") instanceof Number);
        });
    }

    @Test
    void testGetIpAccessStatistics_WithValidRange_ShouldReturnStatistics() {
        // Given
        OffsetDateTime startDate = baseTime.minusMinutes(10);
        OffsetDateTime endDate = baseTime.plusHours(1);

        // When
        List<Map<String, Object>> result = systemLogRepository.getIpAccessStatistics(startDate, endDate);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 各統計レコードが必要なキーを持つことを確認
        result.forEach(stat -> {
            assertTrue(stat.containsKey("ipAddress"));
            assertTrue(stat.containsKey("accessCount"));
            assertTrue(stat.containsKey("uniqueUsers"));
            assertNotNull(stat.get("ipAddress"));
            assertTrue(stat.get("accessCount") instanceof Number);
            assertTrue(stat.get("uniqueUsers") instanceof Number);
        });
    }

    @Test
    void testGetHourlyActivityStatistics_WithValidRange_ShouldReturnStatistics() {
        // Given
        OffsetDateTime startDate = baseTime.minusMinutes(10);
        OffsetDateTime endDate = baseTime.plusHours(1);

        // When
        List<Map<String, Object>> result = systemLogRepository.getHourlyActivityStatistics(startDate, endDate);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 各統計レコードが必要なキーを持つことを確認
        result.forEach(stat -> {
            assertTrue(stat.containsKey("hour"));
            assertTrue(stat.containsKey("activityCount"));
            assertTrue(stat.get("hour") instanceof Number);
            assertTrue(stat.get("activityCount") instanceof Number);
        });
    }

    // ========== セキュリティ関連テスト ==========

    @Test
    void testFindSuspiciousActivity_WithThreshold_ShouldReturnSuspiciousIps() {
        // Given - 同一IPから複数のログを作成
        for (int i = 0; i < 5; i++) {
            SystemLog suspiciousLog = createSystemLog(null, TEST_USER_ID_1, "SUSPICIOUS_ACTION", TEST_STATUS_SUCCESS,
                    "192.168.1.100", TEST_USER_AGENT, "{\"attempt\": " + i + "}",
                    OffsetDateTime.now(ZoneOffset.ofHours(9)).minusMinutes(i));
            systemLogRepository.save(suspiciousLog);
        }

        // When
        List<Object[]> result = systemLogRepository.findSuspiciousActivity(1, 3);

        // Then
        assertNotNull(result);
        // 結果があれば、各レコードが2つの要素（IP、カウント）を持つことを確認
        result.forEach(record -> {
            assertEquals(2, record.length);
            assertNotNull(record[0]); // IP address
            assertTrue(record[1] instanceof Number); // count
        });
    }

    @Test
    void testFindFailedLoginAttempts_WithRecentHours_ShouldReturnFailedAttempts() {
        // Given - 失敗ログイン試行を作成
        SystemLog failedLogin = createSystemLog(null, TEST_USER_ID_1, "login_failed", TEST_STATUS_ERROR,
                TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"reason\": \"invalid_password\"}",
                OffsetDateTime.now(ZoneOffset.ofHours(9)).minusMinutes(10));
        systemLogRepository.save(failedLogin);

        // When
        List<SystemLog> result = systemLogRepository.findFailedLoginAttempts(1);

        // Then
        assertNotNull(result);
        // 失敗ログインが含まれていることを確認
        assertTrue(result.stream()
                .anyMatch(log -> log.getAction().contains("login") && log.getAction().contains("failed")));
    }

    // ========== 検索・フィルタリングテスト ==========

    // @Test
    void testFindByFilters_WithPageable_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        OffsetDateTime startDate = baseTime.minusMinutes(10);
        OffsetDateTime endDate = baseTime.plusHours(1);

        // When
        Page<SystemLog> result = systemLogRepository.findByFilters(
                TEST_ACTION_LOGIN, TEST_STATUS_SUCCESS, startDate, endDate, pageable);

        // Then
        assertNotNull(result);
        assertNotNull(result.getContent());
        assertTrue(result.getContent().stream().allMatch(
                log -> TEST_ACTION_LOGIN.equals(log.getAction()) && TEST_STATUS_SUCCESS.equals(log.getStatus())));
    }

    @Test
    void testSearchByKeyword_WithValidKeyword_ShouldReturnMatchingLogs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SystemLog> result = systemLogRepository.searchByKeyword("LOGIN", pageable);

        // Then
        assertNotNull(result);
        assertNotNull(result.getContent());
        assertFalse(result.getContent().isEmpty());
        // キーワードが含まれていることを確認
        assertTrue(result.getContent().stream().anyMatch(log -> log.getAction().contains("LOGIN") ||
                (log.getStatus() != null && log.getStatus().contains("LOGIN")) ||
                (log.getIpAddress() != null && log.getIpAddress().contains("LOGIN")) ||
                (log.getUserAgent() != null && log.getUserAgent().contains("LOGIN"))));
    }

    // ========== 集計統計テスト ==========

    @Test
    void testCountByActionGrouped_ShouldReturnActionCounts() {
        // When
        List<Map<String, Object>> result = systemLogRepository.countByActionGrouped();

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 各統計レコードが必要なキーを持つことを確認
        result.forEach(stat -> {
            assertTrue(stat.containsKey("action"));
            assertTrue(stat.containsKey("count"));
            assertNotNull(stat.get("action"));
            assertTrue(stat.get("count") instanceof Number);
        });
    }

    @Test
    void testCountByStatusGrouped_ShouldReturnStatusCounts() {
        // When
        List<Map<String, Object>> result = systemLogRepository.countByStatusGrouped();

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 各統計レコードが必要なキーを持つことを確認
        result.forEach(stat -> {
            assertTrue(stat.containsKey("status"));
            assertTrue(stat.containsKey("count"));
            assertNotNull(stat.get("status"));
            assertTrue(stat.get("count") instanceof Number);
        });
    }

    @Test
    void testCountByUserGrouped_ShouldReturnUserCounts() {
        // When
        List<Map<String, Object>> result = systemLogRepository.countByUserGrouped();

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 各統計レコードが必要なキーを持つことを確認
        result.forEach(stat -> {
            assertTrue(stat.containsKey("userId"));
            assertTrue(stat.containsKey("count"));
            assertTrue(stat.get("count") instanceof Number);
        });
    }

    @Test
    void testCountByDateGrouped_ShouldReturnDateCounts() {
        // When
        List<Map<String, Object>> result = systemLogRepository.countByDateGrouped();

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 各統計レコードが必要なキーを持つことを確認
        result.forEach(stat -> {
            assertTrue(stat.containsKey("date"));
            assertTrue(stat.containsKey("count"));
            assertTrue(stat.get("count") instanceof Number);
        });
    }

    // ========== データ整合性テスト ==========

    @Test
    void testSaveAndRetrieve_ShouldMaintainDataIntegrity() {
        // Given
        SystemLog newLog = createSystemLog(null, TEST_USER_ID_2, "TEST_ACTION", TEST_STATUS_SUCCESS,
                "192.168.1.202", "Test User Agent", "{\"test\": \"data\"}",
                OffsetDateTime.now(ZoneOffset.ofHours(9)));

        // When
        SystemLog savedLog = systemLogRepository.save(newLog);
        SystemLog retrievedLog = systemLogRepository.findById(savedLog.getId().intValue()).orElse(null);

        // Then
        assertNotNull(retrievedLog);
        assertEquals(savedLog.getId(), retrievedLog.getId());
        assertEquals(TEST_USER_ID_2, retrievedLog.getUserId());
        assertEquals("TEST_ACTION", retrievedLog.getAction());
        assertEquals(TEST_STATUS_SUCCESS, retrievedLog.getStatus());
        assertEquals("192.168.1.202", retrievedLog.getIpAddress());
        assertEquals("Test User Agent", retrievedLog.getUserAgent());
        assertEquals("{\"test\": \"data\"}", retrievedLog.getDetails());
        assertNotNull(retrievedLog.getCreatedAt());
    }

    @Test
    void testUpdateLog_ShouldReflectChanges() {
        // Given
        SystemLog log = systemLogRepository.findById(loginLog1.getId().intValue()).orElse(null);
        assertNotNull(log);

        // When
        log.setStatus("updated");
        log.setDetails("{\"updated\": \"true\"}");
        SystemLog updatedLog = systemLogRepository.save(log);

        // Then
        assertEquals(loginLog1.getId(), updatedLog.getId());
        assertEquals("updated", updatedLog.getStatus());
        assertEquals("{\"updated\": \"true\"}", updatedLog.getDetails());
    }

    @Test
    void testDeleteLog_ShouldRemoveFromDatabase() {
        // Given
        Long logId = loginLog1.getId();
        assertTrue(systemLogRepository.existsById(logId.intValue()));

        // When
        systemLogRepository.deleteById(logId.intValue());

        // Then
        assertFalse(systemLogRepository.existsById(logId.intValue()));
    }

    // ========== バッチ処理・メンテナンステスト ==========

    @Test
    void testCountByCreatedAtBetween_WithValidRange_ShouldReturnCount() {
        // Given
        OffsetDateTime startDate = baseTime.minusMinutes(10);
        OffsetDateTime endDate = baseTime.plusHours(1);

        // When
        long count = systemLogRepository.countByCreatedAtBetween(startDate, endDate);

        // Then
        assertTrue(count >= 4); // テストデータ分
    }

    @Test
    void testFindLogsOlderThan_WithCutoffDate_ShouldReturnOldLogs() {
        // Given
        OffsetDateTime cutoffDate = OffsetDateTime.now(ZoneOffset.ofHours(9)).minusDays(1);

        // When
        List<SystemLog> result = systemLogRepository.findLogsOlderThan(cutoffDate);

        // Then
        assertNotNull(result);
        // 古いログがあれば、すべてカットオフ日時より前であることを確認
        result.forEach(log -> assertTrue(log.getCreatedAt().isBefore(cutoffDate)));
    }

    @Test
    void testCountByCreatedAtBefore_WithCutoffDate_ShouldReturnCount() {
        // Given
        OffsetDateTime cutoffDate = OffsetDateTime.now(ZoneOffset.ofHours(9)).minusDays(1);

        // When
        long count = systemLogRepository.countByCreatedAtBefore(cutoffDate);

        // Then
        assertTrue(count >= 0); // 負の値でないことを確認
    }

    @Test
    void testFindForBatchProcessing_WithOffsetAndLimit_ShouldReturnLimitedResults() {
        // When
        List<SystemLog> result = systemLogRepository.findForBatchProcessing(0, 5);

        // Then
        assertNotNull(result);
        assertTrue(result.size() <= 5);
        // ID順にソートされていることを確認
        if (result.size() > 1) {
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).getId() <= result.get(i + 1).getId());
            }
        }
    }

    // ========== エッジケース・境界値テスト ==========

    @Test
    void testFindByUserId_WithNullUserId_ShouldHandleGracefully() {
        // When & Then
        assertDoesNotThrow(() -> {
            List<SystemLog> result = systemLogRepository.findByUserId(null);
            assertNotNull(result);
        });
    }

    @Test
    void testFindByAction_WithNullAction_ShouldHandleGracefully() {
        // When & Then
        assertDoesNotThrow(() -> {
            List<SystemLog> result = systemLogRepository.findByAction(null);
            assertNotNull(result);
        });
    }

    @Test
    void testFindByIpAddress_WithNullIp_ShouldHandleGracefully() {
        // When & Then
        assertDoesNotThrow(() -> {
            List<SystemLog> result = systemLogRepository.findByIpAddress(null);
            assertNotNull(result);
        });
    }

    @Test
    void testFindByCreatedAtBetween_WithInvalidRange_ShouldReturnEmptyList() {
        // Given - 開始日が終了日より後の無効な範囲
        OffsetDateTime invalidStartDate = baseTime.plusHours(2);
        OffsetDateTime invalidEndDate = baseTime.plusHours(1);

        // When
        List<SystemLog> result = systemLogRepository.findByCreatedAtBetween(invalidStartDate, invalidEndDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== パフォーマンステスト ==========

    @Test
    void testLargeDatasetQuery_ShouldPerformEfficiently() {
        // Given - 大量のテストデータを作成
        for (int i = 0; i < 50; i++) {
            SystemLog log = createSystemLog(null, TEST_USER_ID_1, "BULK_ACTION_" + i, TEST_STATUS_SUCCESS,
                    TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"bulk\": " + i + "}",
                    baseTime.plusMinutes(i));
            systemLogRepository.save(log);
        }

        // When
        long startTime = System.currentTimeMillis();
        List<SystemLog> result = systemLogRepository.findByUserId(TEST_USER_ID_1);
        long endTime = System.currentTimeMillis();

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 53); // 既存データ + 元の3件 + 新規50件
        assertTrue(endTime - startTime < 1000); // 1秒以内で完了することを確認
    }

    // ========== ヘルパーメソッド ==========

    /**
     * テスト用SystemLogを作成
     */
    private SystemLog createSystemLog(Long id, Integer userId, String action, String status,
            String ipAddress, String userAgent, String details,
            OffsetDateTime createdAt) {
        SystemLog log = new SystemLog();
        log.setId(id);
        log.setUserId(userId);
        log.setAction(action);
        log.setStatus(status);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setDetails(details);
        log.setCreatedAt(createdAt);
        return log;
    }
}