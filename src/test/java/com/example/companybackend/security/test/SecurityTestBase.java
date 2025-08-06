package com.example.companybackend.security.test;

import com.example.companybackend.entity.User;
import com.example.companybackend.repository.UserRepository;
import com.example.companybackend.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * セキュリティテスト基盤クラス
 * 
 * 目的:
 * - 全セキュリティテストの共通基盤を提供
 * - テストデータの管理と初期化
 * - セキュリティテスト用のユーティリティメソッド提供
 * - テスト環境の一貫性確保
 * 
 * 使用方法:
 * - 各セキュリティテストクラスでこのクラスを継承
 * - @BeforeEach, @AfterEachメソッドで自動的にセットアップ・クリーンアップ
 * - 提供されるユーティリティメソッドを活用
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("security-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@Transactional
@Sql(scripts = "/sql/security-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class SecurityTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected SecurityTestDataManager testDataManager;

    @Autowired
    protected SecurityTestUtils testUtils;

    @Autowired
    protected ObjectMapper objectMapper;

    // テスト用ユーザー
    protected User testAdminUser;
    protected User testNormalUser;
    protected User testManagerUser;

    // テスト実行情報
    protected LocalDateTime testStartTime;
    protected String currentTestName;

    /**
     * 各テストメソッド実行前の共通セットアップ
     */
    @BeforeEach
    void setUpSecurityTest() {
        testStartTime = LocalDateTime.now();
        currentTestName = getCurrentTestMethodName();

        // テストデータの初期化
        initializeTestData();

        // セキュリティテスト環境の準備
        prepareSecurityTestEnvironment();

        // カスタムセットアップの実行
        customSetUp();
    }

    /**
     * 各テストメソッド実行後の共通クリーンアップ
     */
    @AfterEach
    void tearDownSecurityTest() {
        try {
            // カスタムクリーンアップの実行
            customTearDown();

            // テスト結果の記録
            recordTestResult();

            // テストデータのクリーンアップ
            cleanupTestData();

        } catch (Exception e) {
            // クリーンアップエラーをログに記録（テスト失敗にはしない）
            System.err.println("セキュリティテストクリーンアップエラー: " + e.getMessage());
        }
    }

    /**
     * テストデータの初期化
     */
    private void initializeTestData() {
        // テスト用ユーザーの取得または作成
        testAdminUser = getOrCreateTestUser("security_test_admin", "ADMIN");
        testNormalUser = getOrCreateTestUser("security_test_user", "USER");
        testManagerUser = getOrCreateTestUser("security_test_manager", "MANAGER");
    }

    /**
     * セキュリティテスト環境の準備
     */
    private void prepareSecurityTestEnvironment() {
        // セキュリティ設定の初期化
        testDataManager.initializeSecurityConfig();

        // レート制限カウンターのリセット
        testDataManager.resetRateLimitCounters();

        // セキュリティログテーブルのクリーンアップ
        testDataManager.cleanupSecurityLogs();
    }

    /**
     * テスト用ユーザーの取得または作成
     */
    private User getOrCreateTestUser(String username, String role) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // 実データベースから既存ユーザーを取得してテスト用に使用
        List<User> allUsers = userRepository.findAll();
        if (!allUsers.isEmpty()) {
            // 既存のユーザーをテスト用として使用（新規作成を避ける）
            User realUser = allUsers.get(0);
            return realUser;
        }

        // フォールバック: 最小限のテストユーザーを作成
        User testUser = new User();
        testUser.setUsername(username);
        testUser.setPasswordHash("$2a$10$test.hash.for.security.testing");
        testUser.setEmail(username + "@security.test");
        testUser.setFullName("Security Test " + role);
        testUser.setRole(role);
        testUser.setLocationType("office");
        testUser.setIsActive(true);
        testUser.setCreatedAt(java.time.OffsetDateTime.now());
        testUser.setUpdatedAt(java.time.OffsetDateTime.now());

        return userRepository.save(testUser);
    }

    /**
     * テスト結果の記録
     */
    private void recordTestResult() {
        // テスト実行結果をデータベースに記録
        // 実装は SecurityTestDataManager で行う
        testDataManager.recordTestResult(
                getClass().getSimpleName(),
                currentTestName,
                getSecurityTestType(),
                "COMPLETED", // 実際のステータスは継承クラスで設定
                System.currentTimeMillis()
                        - testStartTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                null);
    }

    /**
     * テストデータのクリーンアップ
     */
    private void cleanupTestData() {
        // セキュリティテスト専用データのクリーンアップ
        testDataManager.cleanupTestData();
    }

    /**
     * 現在のテストメソッド名を取得
     */
    private String getCurrentTestMethodName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getMethodName().startsWith("test")) {
                return element.getMethodName();
            }
        }
        return "unknown_test";
    }

    // --- 継承クラスでオーバーライド可能なメソッド ---

    /**
     * カスタムセットアップ処理
     * 継承クラスで必要に応じてオーバーライド
     */
    protected void customSetUp() {
        // デフォルトは何もしない
    }

    /**
     * カスタムクリーンアップ処理
     * 継承クラスで必要に応じてオーバーライド
     */
    protected void customTearDown() {
        // デフォルトは何もしない
    }

    /**
     * セキュリティテストタイプの取得
     * 継承クラスで実装必須
     */
    protected abstract String getSecurityTestType();

    // --- ユーティリティメソッド ---

    /**
     * 有効なJWTトークンを生成
     */
    protected String createValidJwtToken(User user) {
        return jwtTokenProvider.createToken(user);
    }

    /**
     * 管理者用JWTトークンを生成
     */
    protected String createAdminJwtToken() {
        return createValidJwtToken(testAdminUser);
    }

    /**
     * 一般ユーザー用JWTトークンを生成
     */
    protected String createUserJwtToken() {
        return createValidJwtToken(testNormalUser);
    }

    /**
     * マネージャー用JWTトークンを生成
     */
    protected String createManagerJwtToken() {
        return createValidJwtToken(testManagerUser);
    }

    /**
     * HTTPヘッダーにJWTトークンを設定
     */
    protected String createAuthorizationHeader(String token) {
        return "Bearer " + token;
    }

    /**
     * テスト実行時間を取得
     */
    protected long getTestExecutionTime() {
        return System.currentTimeMillis()
                - testStartTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * セキュリティテスト用のアサーションヘルパー
     */
    protected void assertSecurityTestPassed(String testName, boolean condition, String message) {
        if (!condition) {
            testDataManager.recordTestFailure(testName, message);
            throw new AssertionError("セキュリティテスト失敗: " + testName + " - " + message);
        }
    }

    /**
     * セキュリティ攻撃のシミュレーション結果を検証
     */
    protected void assertAttackBlocked(String attackType, boolean wasBlocked, String attackPayload) {
        String message = String.format("攻撃タイプ: %s, ペイロード: %s", attackType, attackPayload);
        assertSecurityTestPassed(attackType + "_blocked", wasBlocked,
                "攻撃がブロックされませんでした - " + message);
    }

    /**
     * レスポンス時間のパフォーマンステスト
     */
    protected void assertPerformanceWithinLimit(long responseTimeMs, long limitMs, String operation) {
        boolean withinLimit = responseTimeMs <= limitMs;
        String message = String.format("操作: %s, 実行時間: %dms, 制限: %dms", operation, responseTimeMs, limitMs);
        assertSecurityTestPassed(operation + "_performance", withinLimit,
                "パフォーマンス制限を超過しました - " + message);
    }

    /**
     * CSRFトークンをレスポンスから抽出
     */
    public String extractCsrfToken(org.springframework.test.web.servlet.MvcResult result) {
        // レスポンスヘッダーからCSRFトークンを取得
        String headerToken = result.getResponse().getHeader("X-CSRF-TOKEN");
        if (headerToken != null && !headerToken.isEmpty()) {
            return headerToken;
        }

        // Cookieからトークンを取得
        jakarta.servlet.http.Cookie[] cookies = result.getResponse().getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("CSRF-TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // テスト用のダミートークンを生成
        return "test-csrf-token-" + System.currentTimeMillis();
    }

    /**
     * CSRFトークンを取得するためのAPIを呼び出し
     */
    protected String getCsrfTokenFromApi(String jwtToken) throws Exception {
        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/csrf/token")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();

        // レスポンスヘッダーからトークンを取得
        String headerToken = result.getResponse().getHeader("X-CSRF-TOKEN");
        if (headerToken != null && !headerToken.isEmpty()) {
            return headerToken;
        }

        // レスポンスボディからトークンを取得
        String responseContent = result.getResponse().getContentAsString();
        if (responseContent.contains("csrfToken")) {
            // JSONパースしてトークンを抽出（簡易実装）
            int startIndex = responseContent.indexOf("\"csrfToken\":\"") + 13;
            int endIndex = responseContent.indexOf("\"", startIndex);
            if (startIndex > 12 && endIndex > startIndex) {
                return responseContent.substring(startIndex, endIndex);
            }
        }

        return null;
    }
}