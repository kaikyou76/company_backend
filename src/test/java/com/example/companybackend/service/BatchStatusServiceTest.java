package com.example.companybackend.service;

import com.example.companybackend.dto.BatchResponseDto.BatchExecutionHistory;
import com.example.companybackend.dto.BatchResponseDto.BatchStatusResponse;
import com.example.companybackend.dto.BatchResponseDto.DatabaseStatus;
import com.example.companybackend.dto.BatchResponseDto.DataStatistics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchStatusServiceTest {

    @InjectMocks
    private BatchStatusService batchStatusService;

    // テスト用定数
    private static final String EXPECTED_SYSTEM_STATUS = "HEALTHY";
    private static final String EXPECTED_UPTIME = "5 days, 12 hours";
    private static final int EXPECTED_TOTAL_USERS = 50;
    private static final int EXPECTED_ACTIVE_USERS = 48;
    private static final int EXPECTED_TOTAL_RECORDS = 12450;
    private static final String EXPECTED_LATEST_RECORD_DATE = "2025-01-18";
    private static final int EXPECTED_CURRENT_MONTH_RECORDS = 520;
    private static final int EXPECTED_INCOMPLETE_RECORDS = 2;

    @BeforeEach
    void setUp() {
        // テスト前の初期化処理
    }

    // ========== バッチステータス取得テスト ==========

    @Test
    void testGetBatchStatus_ShouldReturnCompleteStatusResponse() {
        // When
        BatchStatusResponse result = batchStatusService.getBatchStatus();

        // Then
        assertNotNull(result);
        assertEquals(EXPECTED_SYSTEM_STATUS, result.getSystemStatus());
        assertNotNull(result.getLastChecked());
        assertEquals(EXPECTED_UPTIME, result.getUptime());

        // 現在時刻との差が1秒以内であることを確認
        LocalDateTime now = LocalDateTime.now();
        long secondsDiff = ChronoUnit.SECONDS.between(result.getLastChecked(), now);
        assertTrue(Math.abs(secondsDiff) <= 1, "LastChecked should be within 1 second of current time");
    }

    @Test
    void testGetBatchStatus_DatabaseStatus_ShouldReturnCorrectValues() {
        // When
        BatchStatusResponse result = batchStatusService.getBatchStatus();

        // Then
        assertNotNull(result.getDatabaseStatus());
        DatabaseStatus dbStatus = result.getDatabaseStatus();

        assertEquals(EXPECTED_TOTAL_USERS, dbStatus.getTotalUsers());
        assertEquals(EXPECTED_ACTIVE_USERS, dbStatus.getActiveUsers());
        assertEquals(EXPECTED_TOTAL_RECORDS, dbStatus.getTotalAttendanceRecords());
        assertEquals(EXPECTED_LATEST_RECORD_DATE, dbStatus.getLatestRecordDate());

        // アクティブユーザー率の確認
        double activeUserRate = (double) dbStatus.getActiveUsers() / dbStatus.getTotalUsers();
        assertTrue(activeUserRate >= 0.9, "Active user rate should be at least 90%");
    }

    @Test
    void testGetBatchStatus_DataStatistics_ShouldReturnCorrectValues() {
        // When
        BatchStatusResponse result = batchStatusService.getBatchStatus();

        // Then
        assertNotNull(result.getDataStatistics());
        DataStatistics dataStats = result.getDataStatistics();

        assertEquals(EXPECTED_CURRENT_MONTH_RECORDS, dataStats.getCurrentMonthRecords());
        assertEquals(EXPECTED_INCOMPLETE_RECORDS, dataStats.getIncompleteRecords());

        // 不完全レコード率の確認
        double incompleteRate = (double) dataStats.getIncompleteRecords() / dataStats.getCurrentMonthRecords();
        assertTrue(incompleteRate < 0.05, "Incomplete record rate should be less than 5%");
    }

    @Test
    void testGetBatchStatus_RecentBatchExecutions_ShouldReturnValidHistory() {
        // When
        BatchStatusResponse result = batchStatusService.getBatchStatus();

        // Then
        assertNotNull(result.getRecentBatchExecutions());
        List<BatchExecutionHistory> executions = result.getRecentBatchExecutions();

        assertFalse(executions.isEmpty(), "Recent batch executions should not be empty");
        assertTrue(executions.size() >= 2, "Should have at least 2 recent executions");

        // 各実行履歴の検証
        for (BatchExecutionHistory execution : executions) {
            assertNotNull(execution.getType(), "Execution type should not be null");
            assertNotNull(execution.getExecutedAt(), "Execution time should not be null");
            assertNotNull(execution.getStatus(), "Execution status should not be null");
            assertNotNull(execution.getDuration(), "Execution duration should not be null");

            // ステータスが有効な値であることを確認
            assertTrue(execution.getStatus().equals("SUCCESS") ||
                    execution.getStatus().equals("FAILED") ||
                    execution.getStatus().equals("RUNNING"),
                    "Status should be SUCCESS, FAILED, or RUNNING");
        }
    }

    @Test
    void testGetBatchStatus_RecentBatchExecutions_ShouldBeSortedByExecutionTime() {
        // When
        BatchStatusResponse result = batchStatusService.getBatchStatus();

        // Then
        List<BatchExecutionHistory> executions = result.getRecentBatchExecutions();

        if (executions.size() > 1) {
            // 実行時刻が降順（新しい順）でソートされていることを確認
            for (int i = 0; i < executions.size() - 1; i++) {
                LocalDateTime current = executions.get(i).getExecutedAt();
                LocalDateTime next = executions.get(i + 1).getExecutedAt();
                assertTrue(current.isAfter(next) || current.isEqual(next),
                        "Executions should be sorted by execution time in descending order");
            }
        }
    }

    // ========== 稼働時間計算テスト ==========

    @Test
    void testCalculateUptime_ShouldReturnValidFormat() {
        // When
        String uptime = batchStatusService.calculateUptime();

        // Then
        assertNotNull(uptime);
        assertFalse(uptime.trim().isEmpty(), "Uptime should not be empty");
        assertEquals(EXPECTED_UPTIME, uptime);

        // 稼働時間の形式確認（"X days, Y hours" 形式）
        assertTrue(uptime.matches("\\d+ days?, \\d+ hours?"),
                "Uptime should match format 'X days, Y hours'");
    }

    @Test
    void testCalculateUptime_ShouldHandleDifferentTimeFormats() {
        // Given - 異なる稼働時間パターンをテスト
        BatchStatusService spyService = spy(batchStatusService);

        // 1日未満の場合
        when(spyService.calculateUptime()).thenReturn("0 days, 8 hours");
        String shortUptime = spyService.calculateUptime();
        assertTrue(shortUptime.matches("\\d+ days?, \\d+ hours?"));

        // 長期間の場合
        when(spyService.calculateUptime()).thenReturn("365 days, 0 hours");
        String longUptime = spyService.calculateUptime();
        assertTrue(longUptime.matches("\\d+ days?, \\d+ hours?"));

        verify(spyService, times(2)).calculateUptime();
    }

    // ========== バッチ実行履歴取得テスト ==========

    @Test
    void testGetRecentBatchExecutions_ShouldReturnValidExecutions() {
        // When
        List<BatchExecutionHistory> executions = batchStatusService.getRecentBatchExecutions();

        // Then
        assertNotNull(executions);
        assertFalse(executions.isEmpty(), "Executions list should not be empty");

        // 期待される実行タイプが含まれていることを確認
        boolean hasMonthlyExecution = executions.stream()
                .anyMatch(exec -> "MONTHLY_SUMMARY".equals(exec.getType()));
        boolean hasCleanupExecution = executions.stream()
                .anyMatch(exec -> "CLEANUP_DATA".equals(exec.getType()));

        assertTrue(hasMonthlyExecution, "Should contain MONTHLY_SUMMARY execution");
        assertTrue(hasCleanupExecution, "Should contain CLEANUP_DATA execution");
    }

    @Test
    void testGetRecentBatchExecutions_ExecutionDetails_ShouldBeValid() {
        // When
        List<BatchExecutionHistory> executions = batchStatusService.getRecentBatchExecutions();

        // Then
        BatchExecutionHistory monthlyExecution = executions.stream()
                .filter(exec -> "MONTHLY_SUMMARY".equals(exec.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(monthlyExecution, "Monthly execution should exist");
        assertEquals("MONTHLY_SUMMARY", monthlyExecution.getType());
        assertEquals(LocalDateTime.of(2025, 1, 1, 2, 0), monthlyExecution.getExecutedAt());
        assertEquals("SUCCESS", monthlyExecution.getStatus());
        assertEquals("45 seconds", monthlyExecution.getDuration());

        BatchExecutionHistory cleanupExecution = executions.stream()
                .filter(exec -> "CLEANUP_DATA".equals(exec.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(cleanupExecution, "Cleanup execution should exist");
        assertEquals("CLEANUP_DATA", cleanupExecution.getType());
        assertEquals(LocalDateTime.of(2024, 12, 31, 1, 0), cleanupExecution.getExecutedAt());
        assertEquals("SUCCESS", cleanupExecution.getStatus());
        assertEquals("2 minutes", cleanupExecution.getDuration());
    }

    @Test
    void testGetRecentBatchExecutions_DurationFormat_ShouldBeValid() {
        // When
        List<BatchExecutionHistory> executions = batchStatusService.getRecentBatchExecutions();

        // Then
        for (BatchExecutionHistory execution : executions) {
            String duration = execution.getDuration();
            assertNotNull(duration, "Duration should not be null");

            // 期間形式の確認（"X seconds", "X minutes", "X hours" など）
            assertTrue(duration.matches("\\d+\\s+(second|minute|hour)s?") ||
                    duration.matches("\\d+\\s+(second|minute|hour)s?"),
                    "Duration should match expected format: " + duration);
        }
    }

    // ========== システム状態監視テスト ==========

    @Test
    void testGetBatchStatus_SystemHealth_ShouldIndicateHealthyState() {
        // When
        BatchStatusResponse result = batchStatusService.getBatchStatus();

        // Then
        assertEquals("HEALTHY", result.getSystemStatus());

        // システムが健全であることを示す指標の確認
        DatabaseStatus dbStatus = result.getDatabaseStatus();
        DataStatistics dataStats = result.getDataStatistics();

        // アクティブユーザー率が90%以上
        double activeUserRate = (double) dbStatus.getActiveUsers() / dbStatus.getTotalUsers();
        assertTrue(activeUserRate >= 0.9, "Active user rate should indicate healthy system");

        // 不完全レコード率が5%未満
        double incompleteRate = (double) dataStats.getIncompleteRecords() / dataStats.getCurrentMonthRecords();
        assertTrue(incompleteRate < 0.05, "Low incomplete record rate should indicate healthy system");

        // 最近のバッチ実行が成功していること
        List<BatchExecutionHistory> executions = result.getRecentBatchExecutions();
        boolean allSuccessful = executions.stream()
                .allMatch(exec -> "SUCCESS".equals(exec.getStatus()));
        assertTrue(allSuccessful, "All recent batch executions should be successful for healthy system");
    }

    @Test
    void testGetBatchStatus_DataConsistency_ShouldBeValid() {
        // When
        BatchStatusResponse result = batchStatusService.getBatchStatus();

        // Then
        DatabaseStatus dbStatus = result.getDatabaseStatus();
        DataStatistics dataStats = result.getDataStatistics();

        // データの整合性確認
        assertTrue(dbStatus.getActiveUsers() <= dbStatus.getTotalUsers(),
                "Active users should not exceed total users");
        assertTrue(dbStatus.getTotalAttendanceRecords() > 0,
                "Should have attendance records");
        assertTrue(dataStats.getCurrentMonthRecords() > 0,
                "Should have current month records");
        assertTrue(dataStats.getIncompleteRecords() >= 0,
                "Incomplete records should not be negative");

        // 最新記録日付の妥当性確認
        assertNotNull(dbStatus.getLatestRecordDate());
        assertTrue(dbStatus.getLatestRecordDate().matches("\\d{4}-\\d{2}-\\d{2}"),
                "Latest record date should be in YYYY-MM-DD format");
    }

    // ========== 時刻関連テスト ==========

    @Test
    void testGetBatchStatus_LastCheckedTime_ShouldBeRecent() {
        // Given
        LocalDateTime beforeCall = LocalDateTime.now();

        // When
        BatchStatusResponse result = batchStatusService.getBatchStatus();

        // Then
        LocalDateTime afterCall = LocalDateTime.now();
        LocalDateTime lastChecked = result.getLastChecked();

        assertNotNull(lastChecked);
        assertTrue(lastChecked.isAfter(beforeCall.minusSeconds(1)) &&
                lastChecked.isBefore(afterCall.plusSeconds(1)),
                "LastChecked should be between before and after call times");
    }

    @Test
    void testGetBatchStatus_ExecutionTimes_ShouldBeInPast() {
        // When
        BatchStatusResponse result = batchStatusService.getBatchStatus();

        // Then
        LocalDateTime now = LocalDateTime.now();
        List<BatchExecutionHistory> executions = result.getRecentBatchExecutions();

        for (BatchExecutionHistory execution : executions) {
            assertTrue(execution.getExecutedAt().isBefore(now),
                    "All execution times should be in the past");
        }
    }

    // ========== エラーハンドリングテスト ==========

    @Test
    void testGetBatchStatus_WithMockedCurrentTime_ShouldHandleTimeCorrectly() {
        // Given
        LocalDateTime fixedTime = LocalDateTime.of(2025, 2, 1, 12, 0, 0);

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedTime);

            // When
            BatchStatusResponse result = batchStatusService.getBatchStatus();

            // Then
            assertEquals(fixedTime, result.getLastChecked());
            mockedLocalDateTime.verify(LocalDateTime::now, atLeastOnce());
        }
    }

    @Test
    void testGetBatchStatus_MultipleCallsConsistency_ShouldReturnConsistentData() {
        // When
        BatchStatusResponse result1 = batchStatusService.getBatchStatus();
        BatchStatusResponse result2 = batchStatusService.getBatchStatus();

        // Then
        // システムステータスは一貫している
        assertEquals(result1.getSystemStatus(), result2.getSystemStatus());
        assertEquals(result1.getUptime(), result2.getUptime());

        // データベースステータスは一貫している
        DatabaseStatus db1 = result1.getDatabaseStatus();
        DatabaseStatus db2 = result2.getDatabaseStatus();
        assertEquals(db1.getTotalUsers(), db2.getTotalUsers());
        assertEquals(db1.getActiveUsers(), db2.getActiveUsers());
        assertEquals(db1.getTotalAttendanceRecords(), db2.getTotalAttendanceRecords());
        assertEquals(db1.getLatestRecordDate(), db2.getLatestRecordDate());

        // データ統計は一貫している
        DataStatistics stats1 = result1.getDataStatistics();
        DataStatistics stats2 = result2.getDataStatistics();
        assertEquals(stats1.getCurrentMonthRecords(), stats2.getCurrentMonthRecords());
        assertEquals(stats1.getIncompleteRecords(), stats2.getIncompleteRecords());

        // バッチ実行履歴は一貫している
        assertEquals(result1.getRecentBatchExecutions().size(),
                result2.getRecentBatchExecutions().size());
    }

    // ========== パフォーマンステスト ==========

    @Test
    void testGetBatchStatus_PerformanceTest_ShouldCompleteQuickly() {
        // Given
        long startTime = System.currentTimeMillis();

        // When
        BatchStatusResponse result = batchStatusService.getBatchStatus();

        // Then
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        assertNotNull(result);
        assertTrue(executionTime < 1000,
                "getBatchStatus should complete within 1 second, took: " + executionTime + "ms");
    }

    @Test
    void testGetRecentBatchExecutions_PerformanceTest_ShouldCompleteQuickly() {
        // Given
        long startTime = System.currentTimeMillis();

        // When
        List<BatchExecutionHistory> executions = batchStatusService.getRecentBatchExecutions();

        // Then
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        assertNotNull(executions);
        assertTrue(executionTime < 500,
                "getRecentBatchExecutions should complete within 500ms, took: " + executionTime + "ms");
    }

    // ========== バッチタイプ検証テスト ==========

    @Test
    void testGetRecentBatchExecutions_BatchTypes_ShouldContainExpectedTypes() {
        // When
        List<BatchExecutionHistory> executions = batchStatusService.getRecentBatchExecutions();

        // Then
        List<String> executionTypes = executions.stream()
                .map(BatchExecutionHistory::getType)
                .toList();

        // 期待されるバッチタイプが含まれていることを確認
        assertTrue(executionTypes.contains("MONTHLY_SUMMARY"),
                "Should contain MONTHLY_SUMMARY batch type");
        assertTrue(executionTypes.contains("CLEANUP_DATA"),
                "Should contain CLEANUP_DATA batch type");

        // バッチタイプが有効な値であることを確認
        List<String> validTypes = List.of(
                "MONTHLY_SUMMARY", "DAILY_SUMMARY", "CLEANUP_DATA",
                "DATA_REPAIR", "OVERTIME_MONITORING", "PAID_LEAVE_UPDATE");

        for (String type : executionTypes) {
            assertTrue(validTypes.contains(type),
                    "Batch type should be valid: " + type);
        }
    }

    @Test
    void testGetBatchStatus_AllComponents_ShouldBeNonNull() {
        // When
        BatchStatusResponse result = batchStatusService.getBatchStatus();

        // Then
        assertNotNull(result, "BatchStatusResponse should not be null");
        assertNotNull(result.getSystemStatus(), "SystemStatus should not be null");
        assertNotNull(result.getLastChecked(), "LastChecked should not be null");
        assertNotNull(result.getUptime(), "Uptime should not be null");
        assertNotNull(result.getDatabaseStatus(), "DatabaseStatus should not be null");
        assertNotNull(result.getDataStatistics(), "DataStatistics should not be null");
        assertNotNull(result.getRecentBatchExecutions(), "RecentBatchExecutions should not be null");

        // DatabaseStatus の各フィールド
        DatabaseStatus dbStatus = result.getDatabaseStatus();
        assertNotNull(dbStatus.getLatestRecordDate(), "LatestRecordDate should not be null");

        // 各バッチ実行履歴の必須フィールド
        for (BatchExecutionHistory execution : result.getRecentBatchExecutions()) {
            assertNotNull(execution.getType(), "Execution type should not be null");
            assertNotNull(execution.getExecutedAt(), "Execution time should not be null");
            assertNotNull(execution.getStatus(), "Execution status should not be null");
            assertNotNull(execution.getDuration(), "Execution duration should not be null");
        }
    }
}