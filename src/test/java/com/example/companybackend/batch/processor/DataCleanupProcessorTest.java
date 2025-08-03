package com.example.companybackend.batch.processor;

import com.example.companybackend.entity.SystemLog;
import com.example.companybackend.repository.SystemLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataCleanupProcessorTest {

    @Mock
    private SystemLogRepository systemLogRepository;

    private DataCleanupProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new DataCleanupProcessor();
        processor.setSystemLogRepository(systemLogRepository);
    }

    @Test
    void testProcess_WithOldData_ShouldReturnForDeletion() throws Exception {
        // Given
        SystemLog oldLog = new SystemLog();
        oldLog.setId(1L);
        oldLog.setUserId(1);
        oldLog.setAction("login");
        oldLog.setStatus("success");
        oldLog.setCreatedAt(OffsetDateTime.now().minusMonths(15)); // 15ヶ月前（削除対象）

        // When
        SystemLog result = processor.process(oldLog);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("login", result.getAction());
    }

    @Test
    void testProcess_WithRecentData_ShouldReturnNull() throws Exception {
        // Given
        SystemLog recentLog = new SystemLog();
        recentLog.setId(2L);
        recentLog.setUserId(1);
        recentLog.setAction("logout");
        recentLog.setStatus("success");
        recentLog.setCreatedAt(OffsetDateTime.now().minusMonths(6)); // 6ヶ月前（保持対象）

        // When
        SystemLog result = processor.process(recentLog);

        // Then
        assertNull(result); // 削除対象外はnullを返す
    }

    @Test
    void testProcess_WithBoundaryData_ShouldReturnNull() throws Exception {
        // Given
        SystemLog boundaryLog = new SystemLog();
        boundaryLog.setId(3L);
        boundaryLog.setUserId(1);
        boundaryLog.setAction("access");
        boundaryLog.setStatus("success");
        boundaryLog.setCreatedAt(OffsetDateTime.now().minusMonths(12).plusDays(1)); // 12ヶ月-1日前（保持対象）

        // When
        SystemLog result = processor.process(boundaryLog);

        // Then
        assertNull(result); // 境界値は保持対象
    }

    @Test
    void testProcess_WithExactBoundaryData_ShouldReturnForDeletion() throws Exception {
        // Given
        SystemLog exactBoundaryLog = new SystemLog();
        exactBoundaryLog.setId(4L);
        exactBoundaryLog.setUserId(1);
        exactBoundaryLog.setAction("error");
        exactBoundaryLog.setStatus("error");
        exactBoundaryLog.setCreatedAt(OffsetDateTime.now().minusMonths(12).minusMinutes(1)); // 12ヶ月+1分前（削除対象）

        // When
        SystemLog result = processor.process(exactBoundaryLog);

        // Then
        assertNotNull(result); // 境界値を超えているので削除対象
        assertEquals(4L, result.getId());
    }

    @Test
    void testProcess_WithCurrentData_ShouldReturnNull() throws Exception {
        // Given
        SystemLog currentLog = new SystemLog();
        currentLog.setId(5L);
        currentLog.setUserId(1);
        currentLog.setAction("current_action");
        currentLog.setStatus("success");
        currentLog.setCreatedAt(OffsetDateTime.now()); // 現在時刻（保持対象）

        // When
        SystemLog result = processor.process(currentLog);

        // Then
        assertNull(result); // 現在のデータは保持対象
    }

    @Test
    void testGetDeleteTargetCount_ShouldReturnCount() {
        // Given
        when(systemLogRepository.countByCreatedAtBefore(any(OffsetDateTime.class)))
                .thenReturn(1500L);

        // When
        long count = processor.getDeleteTargetCount();

        // Then
        assertEquals(1500L, count);
    }

    @Test
    void testGetDeleteTargetCount_WithException_ShouldReturnZero() {
        // Given
        when(systemLogRepository.countByCreatedAtBefore(any(OffsetDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When
        long count = processor.getDeleteTargetCount();

        // Then
        assertEquals(0L, count);
    }

    @Test
    void testGetRetentionMonths_ShouldReturn12() {
        // When
        int retentionMonths = processor.getRetentionMonths();

        // Then
        assertEquals(12, retentionMonths);
    }

    @Test
    void testGetCutoffDate_ShouldReturnCorrectDate() {
        // When
        var cutoffDate = processor.getCutoffDate();

        // Then
        assertNotNull(cutoffDate);
        // 12ヶ月前の日付であることを確認（日付のみ比較）
        var expectedDate = OffsetDateTime.now().minusMonths(12).toLocalDate();
        assertEquals(expectedDate, cutoffDate);
    }

    @Test
    void testProcess_WithNullCreatedAt_ShouldHandleGracefully() throws Exception {
        // Given
        SystemLog logWithNullDate = new SystemLog();
        logWithNullDate.setId(6L);
        logWithNullDate.setUserId(1);
        logWithNullDate.setAction("test");
        logWithNullDate.setStatus("success");
        logWithNullDate.setCreatedAt(null); // null日時

        // When & Then
        assertThrows(Exception.class, () -> {
            processor.process(logWithNullDate);
        });
    }

    @Test
    void testProcess_WithVeryOldData_ShouldReturnForDeletion() throws Exception {
        // Given
        SystemLog veryOldLog = new SystemLog();
        veryOldLog.setId(7L);
        veryOldLog.setUserId(1);
        veryOldLog.setAction("ancient_action");
        veryOldLog.setStatus("success");
        veryOldLog.setCreatedAt(OffsetDateTime.now().minusYears(5)); // 5年前（削除対象）

        // When
        SystemLog result = processor.process(veryOldLog);

        // Then
        assertNotNull(result);
        assertEquals(7L, result.getId());
        assertEquals("ancient_action", result.getAction());
    }

    @Test
    void testProcess_WithDifferentStatuses_ShouldProcessCorrectly() throws Exception {
        // Given - エラーステータスの古いログ
        SystemLog errorLog = new SystemLog();
        errorLog.setId(8L);
        errorLog.setUserId(1);
        errorLog.setAction("failed_action");
        errorLog.setStatus("error");
        errorLog.setCreatedAt(OffsetDateTime.now().minusMonths(15));

        // Given - 警告ステータスの古いログ
        SystemLog warningLog = new SystemLog();
        warningLog.setId(9L);
        warningLog.setUserId(2);
        warningLog.setAction("warning_action");
        warningLog.setStatus("warning");
        warningLog.setCreatedAt(OffsetDateTime.now().minusMonths(18));

        // When
        SystemLog errorResult = processor.process(errorLog);
        SystemLog warningResult = processor.process(warningLog);

        // Then
        assertNotNull(errorResult);
        assertEquals("error", errorResult.getStatus());

        assertNotNull(warningResult);
        assertEquals("warning", warningResult.getStatus());
    }
}