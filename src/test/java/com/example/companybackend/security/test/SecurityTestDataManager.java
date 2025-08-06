package com.example.companybackend.security.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * セキュリティテストデータ管理クラス
 * 
 * 目的:
 * - セキュリティテスト用データの作成・管理・削除
 * - テスト実行結果の記録
 * - セキュリティ設定の管理
 * - テストデータの整合性確保
 * 
 * 機能:
 * - テストユーザーの管理
 * - セキュリティログの管理
 * - レート制限カウンターの管理
 * - テスト結果の記録
 */
@Component
@Transactional
public class SecurityTestDataManager {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * セキュリティ設定の初期化
     */
    public void initializeSecurityConfig() {
        // セキュリティテスト用設定の確認・初期化
        String checkConfigSql = "SELECT COUNT(*) FROM security_test_config WHERE config_key = ?";

        // JWT設定
        if (jdbcTemplate.queryForObject(checkConfigSql, Integer.class, "jwt.test.expiration") == 0) {
            insertConfig("jwt.test.expiration", "300000", "INTEGER", "JWT テスト用有効期限 (5分)");
        }

        // レート制限設定
        if (jdbcTemplate.queryForObject(checkConfigSql, Integer.class, "rate.limit.requests.per.minute") == 0) {
            insertConfig("rate.limit.requests.per.minute", "10", "INTEGER", "レート制限: 1分あたりのリクエスト数");
        }

        if (jdbcTemplate.queryForObject(checkConfigSql, Integer.class, "rate.limit.requests.per.hour") == 0) {
            insertConfig("rate.limit.requests.per.hour", "100", "INTEGER", "レート制限: 1時間あたりのリクエスト数");
        }

        // セキュリティ保護機能設定
        if (jdbcTemplate.queryForObject(checkConfigSql, Integer.class, "xss.protection.enabled") == 0) {
            insertConfig("xss.protection.enabled", "true", "BOOLEAN", "XSS保護機能の有効/無効");
        }

        if (jdbcTemplate.queryForObject(checkConfigSql, Integer.class, "csrf.protection.enabled") == 0) {
            insertConfig("csrf.protection.enabled", "true", "BOOLEAN", "CSRF保護機能の有効/無効");
        }

        if (jdbcTemplate.queryForObject(checkConfigSql, Integer.class, "sql.injection.protection.enabled") == 0) {
            insertConfig("sql.injection.protection.enabled", "true", "BOOLEAN", "SQLインジェクション保護機能の有効/無効");
        }
    }

    /**
     * 設定値の挿入
     */
    private void insertConfig(String key, String value, String type, String description) {
        String sql = "INSERT INTO security_test_config (config_key, config_value, config_type, description) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, key, value, type, description);
    }

    /**
     * レート制限カウンターのリセット
     */
    public void resetRateLimitCounters() {
        String sql = "DELETE FROM security_rate_limit_log WHERE window_end < NOW()";
        jdbcTemplate.update(sql);
    }

    /**
     * セキュリティログのクリーンアップ
     */
    public void cleanupSecurityLogs() {
        // 古いログエントリを削除（24時間以上前）
        String[] logTables = {
                "security_xss_test_log",
                "security_csrf_test_log",
                "security_sql_injection_test_log",
                "security_rate_limit_log"
        };

        for (String table : logTables) {
            String sql = "DELETE FROM " + table + " WHERE created_at < NOW() - INTERVAL '24 hours'";
            jdbcTemplate.update(sql);
        }
    }

    /**
     * テスト結果の記録
     */
    public void recordTestResult(String testSuiteName, String testCaseName, String testType,
            String status, long executionTimeMs, String errorMessage) {
        String sql = "INSERT INTO security_test_results (test_suite_name, test_case_name, test_type, status, execution_time_ms, error_message) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, testSuiteName, testCaseName, testType, status, executionTimeMs, errorMessage);
    }

    /**
     * テスト失敗の記録
     */
    public void recordTestFailure(String testName, String errorMessage) {
        recordTestResult("SecurityTest", testName, "FAILURE", "FAILED", 0, errorMessage);
    }

    /**
     * XSS攻撃テストログの記録
     */
    public void recordXssTestLog(String attackPattern, String inputData, String sanitizedData,
            boolean isBlocked, String endpoint, Long userId) {
        String sql = "INSERT INTO security_xss_test_log (attack_pattern, input_data, sanitized_data, is_blocked, endpoint, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, attackPattern, inputData, sanitizedData, isBlocked, endpoint, userId);
    }

    /**
     * CSRF攻撃テストログの記録
     */
    public void recordCsrfTestLog(String csrfToken, boolean isValidToken, String originHeader,
            String refererHeader, boolean isBlocked, String endpoint, Long userId) {
        String sql = "INSERT INTO security_csrf_test_log (csrf_token, is_valid_token, origin_header, referer_header, is_blocked, endpoint, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, csrfToken, isValidToken, originHeader, refererHeader, isBlocked, endpoint, userId);
    }

    /**
     * SQLインジェクション攻撃テストログの記録
     */
    public void recordSqlInjectionTestLog(String attackPattern, String inputParameter, String queryAttempted,
            boolean isBlocked, String endpoint, Long userId) {
        String sql = "INSERT INTO security_sql_injection_test_log (attack_pattern, input_parameter, query_attempted, is_blocked, endpoint, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, attackPattern, inputParameter, queryAttempted, isBlocked, endpoint, userId);
    }

    /**
     * レート制限テストログの記録
     */
    public void recordRateLimitTestLog(String ipAddress, String endpoint, String method,
            int requestCount, LocalDateTime windowStart, LocalDateTime windowEnd,
            boolean isBlocked, Long userId) {
        String sql = "INSERT INTO security_rate_limit_log (ip_address, endpoint, method, request_count, window_start, window_end, is_blocked, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, ipAddress, endpoint, method, requestCount, windowStart, windowEnd, isBlocked, userId);
    }

    /**
     * テストデータのクリーンアップ
     */
    public void cleanupTestData() {
        // セキュリティテスト専用データの削除
        // 注意: 実データは削除しない

        // テスト用セッションの削除
        jdbcTemplate.update("DELETE FROM security_test_sessions WHERE created_at < NOW() - INTERVAL '1 hour'");

        // 古いテスト結果の削除（7日以上前）
        jdbcTemplate.update("DELETE FROM security_test_results WHERE created_at < NOW() - INTERVAL '7 days'");
    }

    /**
     * セキュリティ設定値の取得
     */
    public String getSecurityConfig(String configKey) {
        String sql = "SELECT config_value FROM security_test_config WHERE config_key = ? AND is_active = true";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, configKey);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * セキュリティ設定値の更新
     */
    public void updateSecurityConfig(String configKey, String configValue) {
        String sql = "UPDATE security_test_config SET config_value = ?, updated_at = NOW() WHERE config_key = ?";
        jdbcTemplate.update(sql, configValue, configKey);
    }

    /**
     * テスト用ユーザーの作成
     */
    public void createTestUser(String username, String passwordHash, String email, String fullName, String role) {
        String sql = "INSERT INTO security_test_users (username, password_hash, email, full_name, role) VALUES (?, ?, ?, ?, ?) ON CONFLICT (username) DO NOTHING";
        jdbcTemplate.update(sql, username, passwordHash, email, fullName, role);
    }

    /**
     * テスト用セッションの作成
     */
    public void createTestSession(Long userId, String tokenHash, String tokenType, LocalDateTime expiresAt) {
        String sql = "INSERT INTO security_test_sessions (user_id, token_hash, token_type, expires_at) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, userId, tokenHash, tokenType, expiresAt);
    }

    /**
     * テスト用セッションの無効化
     */
    public void revokeTestSession(String tokenHash) {
        String sql = "UPDATE security_test_sessions SET is_revoked = true, revoked_at = NOW() WHERE token_hash = ?";
        jdbcTemplate.update(sql, tokenHash);
    }

    /**
     * レート制限状況の確認
     */
    public int getCurrentRequestCount(String ipAddress, String endpoint, LocalDateTime windowStart) {
        String sql = "SELECT COALESCE(SUM(request_count), 0) FROM security_rate_limit_log WHERE ip_address = ?::inet AND endpoint = ? AND window_start >= ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, ipAddress, endpoint, windowStart);
    }

    /**
     * テスト統計情報の取得
     */
    public Map<String, Object> getTestStatistics() {
        String sql = """
                SELECT
                    test_type,
                    COUNT(*) as total_tests,
                    SUM(CASE WHEN status = 'PASSED' THEN 1 ELSE 0 END) as passed_tests,
                    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_tests,
                    AVG(execution_time_ms) as avg_execution_time
                FROM security_test_results
                WHERE created_at >= NOW() - INTERVAL '24 hours'
                GROUP BY test_type
                """;

        return jdbcTemplate.queryForMap(sql);
    }

    /**
     * データベース接続テスト
     */
    public boolean testDatabaseConnection() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * セキュリティテーブルの存在確認
     */
    public boolean verifySecurityTablesExist() {
        String[] requiredTables = {
                "security_test_users",
                "security_test_sessions",
                "security_rate_limit_log",
                "security_xss_test_log",
                "security_csrf_test_log",
                "security_sql_injection_test_log",
                "security_test_results",
                "security_test_config"
        };

        for (String table : requiredTables) {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, table);
            if (count == null || count == 0) {
                return false;
            }
        }
        return true;
    }
}