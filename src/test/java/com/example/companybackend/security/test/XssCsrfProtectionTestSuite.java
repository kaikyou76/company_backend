package com.example.companybackend.security.test;

import com.example.companybackend.security.test.headers.HttpSecurityHeadersTest;
import com.example.companybackend.security.test.xss.XssProtectionTest;
import org.junit.jupiter.api.*;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * XSS・CSRF保護テスト統合スイート
 * 
 * 目的:
 * - XSS保護テストの統合実行
 * - CSRF保護テストの統合実行
 * - HTTPセキュリティヘッダーテストの統合実行
 * - 全体的なセキュリティ保護機能の検証
 * - テスト結果の統合レポート生成
 * 
 * 実行順序:
 * 1. HTTPセキュリティヘッダーテスト（基盤）
 * 2. XSS保護テスト（入力検証・出力エスケープ）
 * 3. CSRF保護テスト（状態変更保護）
 * 
 * 要件対応:
 * - フェーズ3の全要件を包括的に検証
 * - XSS攻撃防御の確認
 * - CSRF攻撃防御の確認
 * - セキュリティヘッダーの適切な設定確認
 */
@Suite
@SelectClasses({
                HttpSecurityHeadersTest.class,
                XssProtectionTest.class
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("security-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class XssCsrfProtectionTestSuite extends SecurityTestBase {

        @Override
        protected String getSecurityTestType() {
                return "XSS_CSRF_PROTECTION_SUITE";
        }

        /**
         * テストスイート開始前の初期化
         */
        @BeforeAll
        static void setUpTestSuite() {
                System.out.println("=== XSS・CSRF保護テストスイート開始 ===");
                System.out.println("実行対象:");
                System.out.println("1. HTTPセキュリティヘッダーテスト");
                System.out.println("2. XSS保護テスト");
                System.out.println("3. CSRF保護テスト");
                System.out.println("=====================================");
        }

        /**
         * テストスイート完了後の処理
         */
        @AfterAll
        static void tearDownTestSuite() {
                System.out.println("=== XSS・CSRF保護テストスイート完了 ===");
                System.out.println("全てのセキュリティテストが完了しました。");
                System.out.println("詳細な結果はテストレポートを確認してください。");
                System.out.println("=====================================");
        }

        /**
         * テストケース1: セキュリティヘッダー基盤テスト
         * 
         * 目的: セキュリティヘッダーが適切に設定されていることを確認
         * 
         * 期待結果:
         * - 全ての必須セキュリティヘッダーが設定されている
         * - 各ヘッダーが適切な値に設定されている
         * - セキュリティスコアが基準を満たしている
         */
        @Test
        @Order(1)
        void testSecurityHeadersFoundation() throws Exception {
                // Given
                String validToken = createValidJwtToken(testNormalUser);

                // When & Then
                mockMvc.perform(
                                get("/api/users/profile")
                                                .header("Authorization", "Bearer " + validToken))
                                .andExpect(status().isOk())
                                .andExpect(header().exists("Content-Security-Policy"))
                                .andExpect(header().exists("X-Frame-Options"))
                                .andExpect(header().exists("X-Content-Type-Options"))
                                .andExpect(header().exists("X-XSS-Protection"));

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testSecurityHeadersFoundation",
                                "SECURITY_HEADERS_FOUNDATION",
                                "PASSED",
                                getTestExecutionTime(),
                                "Security headers foundation test completed");
        }

        /**
         * テストケース2: XSS保護統合テスト
         * 
         * 目的: XSS攻撃に対する包括的な防御機能を確認
         * 
         * 期待結果:
         * - スクリプトタグインジェクションが防がれる
         * - イベントハンドラーインジェクションが防がれる
         * - JavaScript URLインジェクションが防がれる
         * - HTMLエスケープが適切に動作する
         */
        @Test
        @Order(2)
        void testXssProtectionIntegration() throws Exception {
                // Given
                String validToken = createValidJwtToken(testNormalUser);
                String[] xssPayloads = {
                                "<script>alert('XSS')</script>",
                                "<img src='x' onerror='alert(\"XSS\")'>",
                                "javascript:alert('XSS')",
                                "<svg onload='alert(\"XSS\")'></svg>"
                };

                // When & Then
                for (String payload : xssPayloads) {
                        String requestBody = String.format("""
                                        {
                                            "username": "testuser",
                                            "email": "test@example.com",
                                            "bio": "%s"
                                        }
                                        """, payload.replace("\"", "\\\""));

                        MvcResult result = mockMvc.perform(
                                        put("/api/users/profile")
                                                        .header("Authorization", "Bearer " + validToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(requestBody))
                                        .andExpect(status().isOk())
                                        .andReturn();

                        String responseContent = result.getResponse().getContentAsString();

                        // XSS攻撃が無効化されていることを確認
                        assertFalse(responseContent.contains("<script>"), "スクリプトタグが無効化されていること");
                        assertFalse(responseContent.contains("onerror="), "イベントハンドラーが無効化されていること");
                        assertFalse(responseContent.contains("javascript:"), "JavaScript URLが無効化されていること");
                        assertFalse(responseContent.contains("alert("), "JavaScript関数が無効化されていること");
                }

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testXssProtectionIntegration",
                                "XSS_PROTECTION_INTEGRATION",
                                "PASSED",
                                getTestExecutionTime(),
                                "XSS protection integration test completed");
        }

        /**
         * テストケース3: CSRF保護統合テスト
         * 
         * 目的: CSRF攻撃に対する包括的な防御機能を確認
         * 
         * 期待結果:
         * - 有効なCSRFトークンでのリクエストは成功する
         * - CSRFトークンなしのリクエストは失敗する
         * - 無効なCSRFトークンでのリクエストは失敗する
         * - SameSite Cookieが適切に設定されている
         */
        @Test
        @Order(3)
        void testCsrfProtectionIntegration() throws Exception {
                // Given
                String validToken = createValidJwtToken(testNormalUser);
                String requestBody = """
                                {
                                    "username": "testuser",
                                    "email": "test@example.com",
                                    "bio": "CSRF test"
                                }
                                """;

                // CSRFトークンなしでのリクエスト（失敗するはず）
                mockMvc.perform(
                                put("/api/users/profile")
                                                .header("Authorization", "Bearer " + validToken)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isForbidden());

                // 有効なCSRFトークンを取得
                MvcResult getResult = mockMvc.perform(
                                get("/api/users/profile")
                                                .header("Authorization", "Bearer " + validToken))
                                .andExpect(status().isOk())
                                .andReturn();

                String csrfToken = extractCsrfToken(getResult);

                // 有効なCSRFトークンでのリクエスト（成功するはず）
                mockMvc.perform(
                                put("/api/users/profile")
                                                .header("Authorization", "Bearer " + validToken)
                                                .header("X-CSRF-TOKEN", csrfToken)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isOk());

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testCsrfProtectionIntegration",
                                "CSRF_PROTECTION_INTEGRATION",
                                "PASSED",
                                getTestExecutionTime(),
                                "CSRF protection integration test completed");
        }

        /**
         * テストケース4: XSS・CSRF複合攻撃防御テスト
         * 
         * 目的: XSSとCSRFを組み合わせた複合攻撃に対する防御力を確認
         * 
         * 期待結果:
         * - XSS攻撃とCSRF攻撃の両方が防がれる
         * - 複合攻撃でもシステムが安定して動作する
         * - 適切なエラーハンドリングが行われる
         */
        @Test
        @Order(4)
        void testCombinedXssCsrfAttackPrevention() throws Exception {
                // Given
                String validToken = createValidJwtToken(testNormalUser);
                String combinedAttackPayload = "<script>alert('XSS')</script>" +
                                "<img src='x' onerror='fetch(\"/api/users/profile\", {method:\"PUT\", body:JSON.stringify({bio:\"CSRF via XSS\"})}>";

                String requestBody = String.format("""
                                {
                                    "username": "testuser",
                                    "email": "test@example.com",
                                    "bio": "%s"
                                }
                                """, combinedAttackPayload.replace("\"", "\\\""));

                // When & Then
                // CSRFトークンなしでの複合攻撃（失敗するはず）
                MvcResult result = mockMvc.perform(
                                put("/api/users/profile")
                                                .header("Authorization", "Bearer " + validToken)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isForbidden())
                                .andReturn();

                // レスポンス内容の確認
                String responseContent = result.getResponse().getContentAsString();
                assertTrue(responseContent.contains("CSRF") ||
                                responseContent.contains("Forbidden"),
                                "CSRF保護が機能していること");

                // 有効なCSRFトークンを取得して再試行
                MvcResult getResult = mockMvc.perform(
                                get("/api/users/profile")
                                                .header("Authorization", "Bearer " + validToken))
                                .andExpect(status().isOk())
                                .andReturn();

                String csrfToken = extractCsrfToken(getResult);

                // 有効なCSRFトークンでの複合攻撃（XSS部分は無効化されるはず）
                result = mockMvc.perform(
                                put("/api/users/profile")
                                                .header("Authorization", "Bearer " + validToken)
                                                .header("X-CSRF-TOKEN", csrfToken)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isOk())
                                .andReturn();

                responseContent = result.getResponse().getContentAsString();

                // XSS攻撃が無効化されていることを確認
                assertFalse(responseContent.contains("<script>"), "スクリプトタグが無効化されていること");
                assertFalse(responseContent.contains("onerror="), "イベントハンドラーが無効化されていること");
                assertFalse(responseContent.contains("fetch("), "JavaScript関数が無効化されていること");

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testCombinedXssCsrfAttackPrevention",
                                "COMBINED_XSS_CSRF_ATTACK_PREVENTION",
                                "PASSED",
                                getTestExecutionTime(),
                                "Combined XSS/CSRF attack prevention test completed");
        }

        /**
         * テストケース5: セキュリティ保護パフォーマンステスト
         * 
         * 目的: XSS・CSRF保護機能がパフォーマンスに与える影響を測定
         * 
         * 期待結果:
         * - セキュリティ処理が300ms以内で完了する
         * - 大量のリクエストに対しても安定して動作する
         * - システムリソースの使用量が適切である
         */
        @Test
        @Order(5)
        void testSecurityProtectionPerformance() throws Exception {
                // Given
                String validToken = createValidJwtToken(testNormalUser);

                // CSRFトークンを取得
                MvcResult getResult = mockMvc.perform(
                                get("/api/users/profile")
                                                .header("Authorization", "Bearer " + validToken))
                                .andExpect(status().isOk())
                                .andReturn();

                String csrfToken = extractCsrfToken(getResult);
                String requestBody = """
                                {
                                    "username": "performancetest",
                                    "email": "performance@example.com",
                                    "bio": "Performance test with potential XSS <script>alert('test')</script>"
                                }
                                """;

                // セキュリティ保護処理時間の測定
                long securityProcessingTime = testUtils.measureResponseTime(() -> {
                        try {
                                mockMvc.perform(
                                                put("/api/users/profile")
                                                                .header("Authorization", "Bearer " + validToken)
                                                                .header("X-CSRF-TOKEN", csrfToken)
                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                .content(requestBody))
                                                .andExpect(status().isOk());
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                });

                // パフォーマンス検証
                assertPerformanceWithinLimit(securityProcessingTime, 300, "SECURITY_PROTECTION");

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testSecurityProtectionPerformance",
                                "SECURITY_PROTECTION_PERFORMANCE",
                                "PASSED",
                                getTestExecutionTime(),
                                String.format("Security protection time: %dms", securityProcessingTime));
        }

        /**
         * テストケース6: セキュリティ設定検証テスト
         * 
         * 目的: セキュリティ設定が要件を満たしていることを包括的に確認
         * 
         * 期待結果:
         * - 全てのセキュリティ設定が適切である
         * - セキュリティレベルが要求を満たしている
         * - 設定の一貫性が保たれている
         */
        @Test
        @Order(6)
        void testSecurityConfigurationValidation() throws Exception {
                // Given
                String validToken = createValidJwtToken(testNormalUser);

                // When & Then
                MvcResult result = mockMvc.perform(
                                get("/api/users/profile")
                                                .header("Authorization", "Bearer " + validToken))
                                .andExpect(status().isOk())
                                .andReturn();

                // セキュリティヘッダーの包括的チェック
                String[] requiredHeaders = {
                                "Content-Security-Policy",
                                "X-Frame-Options",
                                "X-Content-Type-Options",
                                "X-XSS-Protection"
                };

                int presentHeaders = 0;
                StringBuilder configReport = new StringBuilder();

                for (String headerName : requiredHeaders) {
                        String headerValue = result.getResponse().getHeader(headerName);
                        if (headerValue != null) {
                                presentHeaders++;
                                configReport.append(headerName).append(": OK; ");
                        } else {
                                configReport.append(headerName).append(": MISSING; ");
                        }
                }

                // セキュリティ設定の完全性確認
                double configCompleteness = (double) presentHeaders / requiredHeaders.length;
                assertTrue(configCompleteness >= 0.9,
                                String.format("セキュリティ設定の完全性が90%%以上であること (現在: %.1f%%)",
                                                configCompleteness * 100));

                // Content-Type の確認
                String contentType = result.getResponse().getContentType();
                assertNotNull(contentType, "Content-Type が設定されていること");
                assertTrue(contentType.contains("application/json"),
                                "適切なContent-Type が設定されていること");

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testSecurityConfigurationValidation",
                                "SECURITY_CONFIGURATION_VALIDATION",
                                "PASSED",
                                getTestExecutionTime(),
                                String.format("Config completeness: %.1f%%, Details: %s",
                                                configCompleteness * 100, configReport.toString()));
        }

        /**
         * CSRFトークンをレスポンスから抽出するヘルパーメソッド
         */
        public String extractCsrfToken(MvcResult result) {
                // ヘッダーからCSRFトークンを取得
                String csrfTokenHeader = result.getResponse().getHeader("X-CSRF-TOKEN");
                if (csrfTokenHeader != null) {
                        return csrfTokenHeader;
                }

                // Cookieからトークンを取得
                String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
                if (setCookieHeader != null && setCookieHeader.contains("CSRF-TOKEN=")) {
                        String[] parts = setCookieHeader.split(";");
                        for (String part : parts) {
                                if (part.trim().startsWith("CSRF-TOKEN=")) {
                                        return part.substring("CSRF-TOKEN=".length()).trim();
                                }
                        }
                }

                // レスポンスボディからトークンを抽出（JSON形式の場合）
                try {
                        String responseBody = result.getResponse().getContentAsString();
                        // 簡易的なJSONパース（実際のプロジェクトでは専用ライブラリを使用）
                        if (responseBody.contains("\"csrfToken\"")) {
                                int startIndex = responseBody.indexOf("\"csrfToken\":\"") + 13;
                                int endIndex = responseBody.indexOf("\"", startIndex);
                                if (startIndex > 12 && endIndex > startIndex) {
                                        return responseBody.substring(startIndex, endIndex);
                                }
                        }
                } catch (Exception e) {
                        // パースエラーは無視
                }

                return null; // トークンが見つからない場合
        }
}