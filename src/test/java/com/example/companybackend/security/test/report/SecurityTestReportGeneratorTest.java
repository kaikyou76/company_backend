package com.example.companybackend.security.test.report;

import com.example.companybackend.security.test.SecurityTestDataManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityTestReportGeneratorTest {

    @Mock
    private SecurityTestDataManager testDataManager;

    @InjectMocks
    private SecurityTestReportGenerator reportGenerator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerateFixSuggestion() {
        // 准备测试数据
        Map<String, Object> failedTest = new HashMap<>();
        failedTest.put("test_case_name", "XssProtectionTest");
        failedTest.put("error_message", "XSS protection failed");
        failedTest.put("test_type", "XSS_PROTECTION");

        // 执行测试
        String suggestion = reportGenerator.generateFixSuggestion(failedTest);

        // 验证结果
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("XssProtectionTest"));
        assertTrue(suggestion.contains("XSS protection failed"));
        assertTrue(suggestion.contains("XSS対策フィルター"));
    }

    @Test
    void testGenerateDetailedTestReport() throws IOException {
        // 准备测试数据
        Map<String, Object> statistics = new HashMap<>();
        Map<String, Object> xssStats = new HashMap<>();
        xssStats.put("total", 10);
        xssStats.put("passed", 8);
        xssStats.put("failed", 2);
        xssStats.put("averageTime", 150);
        statistics.put("XSS_PROTECTION", xssStats);

        List<Map<String, Object>> detailedResults = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("test_suite_name", "SecurityTestSuite");
        result.put("test_case_name", "XssProtectionTest");
        result.put("test_type", "XSS_PROTECTION");
        result.put("status", "PASSED");
        result.put("execution_time_ms", 150);
        result.put("error_message", "");
        detailedResults.add(result);

        List<Map<String, Object>> failedTests = new ArrayList<>();
        Map<String, Object> failedTest = new HashMap<>();
        failedTest.put("test_case_name", "SqlInjectionProtectionTest");
        failedTest.put("error_message", "SQL injection detected");
        failedTest.put("test_type", "SQL_INJECTION_PROTECTION");
        failedTests.add(failedTest);

        // 配置mock行为
        when(testDataManager.getTestStatistics()).thenReturn(statistics);
        when(testDataManager.getDetailedTestResults()).thenReturn(detailedResults);
        when(testDataManager.getFailedTests()).thenReturn(failedTests);

        // 设置临时文件路径
        String tempReportPath = System.getProperty("java.io.tmpdir") + File.separator + "security_test_report.md";

        // 执行测试
        assertDoesNotThrow(() -> reportGenerator.generateDetailedTestReport(tempReportPath));

        // 验证mock调用
        verify(testDataManager, times(1)).getTestStatistics();
        verify(testDataManager, times(1)).getDetailedTestResults();
        verify(testDataManager, times(1)).getFailedTests();

        // 验证文件是否创建
        Path reportPath = Path.of(tempReportPath);
        assertTrue(Files.exists(reportPath));

        // 验证文件内容
        String content = Files.readString(reportPath);
        assertTrue(content.contains("# セキュリティテスト詳細レポート"));
        assertTrue(content.contains("XssProtectionTest"));
        assertTrue(content.contains("SQL injection detected"));
        assertTrue(content.contains("SQLインジェクション対策フィルター"));

        // 清理临时文件
        Files.deleteIfExists(reportPath);
    }

    @Test
    void testGenerateDetailedTestReportWithEmptyData() throws IOException {
        // 准备空的测试数据
        when(testDataManager.getTestStatistics()).thenReturn(new HashMap<>());
        when(testDataManager.getDetailedTestResults()).thenReturn(new ArrayList<>());
        when(testDataManager.getFailedTests()).thenReturn(new ArrayList<>());

        // 设置临时文件路径
        String tempReportPath = System.getProperty("java.io.tmpdir") + File.separator + "security_test_report_empty.md";

        // 执行测试
        assertDoesNotThrow(() -> reportGenerator.generateDetailedTestReport(tempReportPath));

        // 验证文件是否创建
        Path reportPath = Path.of(tempReportPath);
        assertTrue(Files.exists(reportPath));

        // 验证文件内容
        String content = Files.readString(reportPath);
        assertTrue(content.contains("# セキュリティテスト詳細レポート"));
        assertTrue(content.contains("統計情報がありません。"));
        assertTrue(content.contains("詳細テスト結果がありません。"));
        assertTrue(content.contains("失敗したテストはありません。"));

        // 清理临时文件
        Files.deleteIfExists(reportPath);
    }

    @Test
    void testAppendStatisticsWithInvalidData() throws IOException {
        // 准备包含无效数据的统计信息
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("TEST_TYPE", "invalid_data");

        when(testDataManager.getTestStatistics()).thenReturn(statistics);
        when(testDataManager.getDetailedTestResults()).thenReturn(new ArrayList<>());
        when(testDataManager.getFailedTests()).thenReturn(new ArrayList<>());

        // 设置临时文件路径
        String tempReportPath = System.getProperty("java.io.tmpdir") + File.separator + "security_test_report_invalid.md";

        // 执行测试
        assertDoesNotThrow(() -> reportGenerator.generateDetailedTestReport(tempReportPath));

        // 验证文件是否创建
        Path reportPath = Path.of(tempReportPath);
        assertTrue(Files.exists(reportPath));

        // 清理临时文件
        Files.deleteIfExists(reportPath);
    }
}