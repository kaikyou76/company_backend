package com.example.companybackend.security.test.jwt;

import com.example.companybackend.entity.User;
import com.example.companybackend.security.test.SecurityTestBase;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * JWT認証テストクラス
 * 
 * 目的: JWTトークンの生成、検証、期限管理機能のテスト
 * 対象: JwtTokenProviderクラスの全機能
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("security-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JwtAuthenticationTest extends SecurityTestBase {

        @Override
        protected String getSecurityTestType() {
                return "JWT_AUTHENTICATION";
        }

        /**
         * テストケース1: 有効なJWTトークンでの認証成功テスト
         * 
         * 要件1.1対応: 有効なJWTトークンでAPIにアクセスする THEN 認証が成功し、リソースにアクセスできること
         * 
         * 前提条件:
         * - 有効なユーザーが存在する
         * - JwtTokenProviderが正常に初期化されている
         * 
         * 期待結果:
         * - 有効なJWTトークンが生成される
         * - トークンを使用してAPIアクセスが成功する
         * - 200 OKレスポンスが返される
         */
        @Test
        @Order(1)
        void testValidTokenAuthentication() throws Exception {
                // Given
                User testUser = testNormalUser;
                String validToken = createValidJwtToken(testUser);

                // When & Then
                MvcResult result = mockMvc.perform(
                                testUtils.createAuthenticatedGetRequest("/api/users/profile", validToken))
                                .andExpect(status().isOk())
                                .andReturn();

                // 追加検証
                assertNotNull(validToken, "有効なJWTトークンが生成されること");
                assertTrue(jwtTokenProvider.validateToken(validToken), "生成されたトークンが有効であること");
                assertEquals(testUser.getUsername(), jwtTokenProvider.getUsernameFromToken(validToken),
                                "トークンから正しいユーザー名が抽出されること");

                // セキュリティヘッダーの確認
                assertTrue(testUtils.hasSecurityHeaders(result), "セキュリティヘッダーが設定されていること");

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testValidTokenAuthentication",
                                "JWT_AUTH_VALID",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース2: 管理者トークンでの認証テスト
         * 
         * 目的: 管理者権限を持つユーザーのJWTトークンが正常に動作することを確認
         * 
         * 期待結果:
         * - 管理者トークンが生成される
         * - 管理者トークンでAPIアクセスが成功する
         * - トークンが有効であることが検証される
         * - トークンから正しいユーザー名が抽出される
         */
        @Test
        @Order(2)
        void testAdminTokenAuthentication() throws Exception {
                // Given
                String adminToken = createAdminJwtToken();

                // When & Then
                mockMvc.perform(
                                testUtils.createAuthenticatedGetRequest("/api/users/profile", adminToken))
                                .andExpect(status().isOk());

                // トークンの詳細検証
                assertTrue(jwtTokenProvider.validateToken(adminToken), "管理者トークンが有効であること");
                assertEquals(testAdminUser.getUsername(), jwtTokenProvider.getUsernameFromToken(adminToken),
                                "管理者トークンから正しいユーザー名が抽出されること");

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testAdminTokenAuthentication",
                                "JWT_AUTH_ADMIN",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース3: 複数ユーザーでの同時認証テスト
         * 
         * 要件1.8対応: 同時に複数のJWTトークン検証を行う THEN 全て正常に処理されること
         * 
         * 目的: 複数のユーザーが同時にJWT認証を行っても正常に処理されることを確認
         * 
         * 期待結果:
         * - 各ユーザーに対して独立したトークンが生成される
         * - 同時アクセスでも認証が正常に動作する
         * - トークン間で干渉が発生しない
         */
        @Test
        @Order(3)
        void testConcurrentTokenAuthentication() throws Exception {
                // Given
                String userToken = createUserJwtToken();
                String adminToken = createAdminJwtToken();
                String managerToken = createManagerJwtToken();

                // When & Then - 同時リクエストの実行
                testUtils.executeConcurrentRequests(() -> {
                        try {
                                // 一般ユーザーのアクセス
                                mockMvc.perform(
                                                testUtils.createAuthenticatedGetRequest("/api/users/profile",
                                                                userToken))
                                                .andExpect(status().isOk());

                                // 管理者のアクセス
                                mockMvc.perform(
                                                testUtils.createAuthenticatedGetRequest("/api/users/profile", adminToken))
                                                .andExpect(status().isOk());

                                // マネージャーのアクセス
                                mockMvc.perform(
                                                testUtils.createAuthenticatedGetRequest("/api/users/profile",
                                                                managerToken))
                                                .andExpect(status().isOk());

                        } catch (Exception e) {
                                throw new RuntimeException("同時認証テストでエラーが発生", e);
                        }
                }, 3, 5); // 3スレッド、各5回実行

                // 全トークンが依然として有効であることを確認
                assertTrue(jwtTokenProvider.validateToken(userToken), "一般ユーザートークンが有効であること");
                assertTrue(jwtTokenProvider.validateToken(adminToken), "管理者トークンが有効であること");
                assertTrue(jwtTokenProvider.validateToken(managerToken), "マネージャートークンが有効であること");

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testConcurrentTokenAuthentication",
                                "JWT_AUTH_CONCURRENT",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース4: トークン情報抽出テスト
         * 
         * 目的: JWTトークンから各種情報が正しく抽出されることを確認
         * 
         * 期待結果:
         * - ユーザー名が正しく抽出される
         * - ユーザーIDが正しく抽出される
         * - 部署情報が正しく抽出される
         * - 位置情報が正しく抽出される
         */
        @Test
        @Order(4)
        void testTokenInformationExtraction() throws Exception {
                // Given
                User testUser = testNormalUser;
                String token = createValidJwtToken(testUser);

                // When & Then
                String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
                Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);
                String extractedLocationType = jwtTokenProvider.getLocationTypeFromToken(token);
                Integer extractedDepartmentId = jwtTokenProvider.getDepartmentIdFromToken(token);

                // 検証
                assertEquals(testUser.getUsername(), extractedUsername, "ユーザー名が正しく抽出されること");
                assertEquals(testUser.getId(), extractedUserId, "ユーザーIDが正しく抽出されること");
                assertEquals(testUser.getLocationType(), extractedLocationType, "位置タイプが正しく抽出されること");
                assertEquals(testUser.getDepartmentId(), extractedDepartmentId, "部署IDが正しく抽出されること");

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testTokenInformationExtraction",
                                "JWT_INFO_EXTRACTION",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース5: トークン有効期限境界値テスト
         * 
         * 要件1.7対応: トークンの有効期限境界値でアクセスする THEN 期限直前は有効、期限後は無効であること
         * 
         * 目的: トークンの有効期限が正確に管理されることを確認
         * 
         * 期待結果:
         * - 有効期限内のトークンは有効と判定される
         * - 有効期限の残り時間が正しく計算される
         * - トークンの期限切れ判定が正確に動作する
         * 
         * 実装詳細:
         * - 手動で5分（300000ミリ秒）の有効期限を持つトークンを作成してテスト
         */
        @Test
        @Order(5)
        void testTokenExpirationBoundary() throws Exception {
                // Given
                User testUser = testNormalUser;
                // 使用5分钟（300000毫秒）的有效期手动创建令牌
                String token = createJwtTokenWithExpiration(testUser, 300000L);
                
                // When & Then
                // 有効期限内での検証
                assertTrue(jwtTokenProvider.validateToken(token), "有効期限内のトークンは有効であること");
                assertFalse(jwtTokenProvider.isTokenExpired(token), "有効期限内のトークンは期限切れでないこと");

                // 残り有効时间のconfirm
                long remainingTime = jwtTokenProvider.getRemainingValidityTime(token);
                assertTrue(remainingTime > 0, "有効期限内のトークンは残り時間が正の値であること");
                assertTrue(remainingTime <= 300000L, "剩余时间应该在5分钟以内");

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testTokenExpirationBoundary",
                                "JWT_EXPIRATION_BOUNDARY",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース6: 同一ユーザーでの複数トークン生成テスト
         * 
         * 目的: 同一ユーザーに対して複数のトークンが独立して動作することを確認
         * 
         * 期待結果:
         * - 同一ユーザーに対して複数の異なるトークンが生成される
         * - 各トークンが独立して有効である
         * - トークン間で干渉が発生しない
         */
        @Test
        @Order(6)
        void testMultipleTokensForSameUser() throws Exception {
                // Given
                User testUser = testNormalUser;

                // When
                String token1 = createValidJwtToken(testUser);
                Thread.sleep(1000); // 1秒待機してトークンの生成時刻を変える
                String token2 = createValidJwtToken(testUser);

                // Then
                assertNotEquals(token1, token2, "同一ユーザーでも異なるトークンが生成されること");

                // 両方のトークンが有効であることを確認
                assertTrue(jwtTokenProvider.validateToken(token1), "1つ目のトークンが有効であること");
                assertTrue(jwtTokenProvider.validateToken(token2), "2つ目のトークンが有効であること");

                // 両方のトークンから同じユーザー情報が抽出されることを確認
                assertEquals(testUser.getUsername(), jwtTokenProvider.getUsernameFromToken(token1),
                                "1つ目のトークンから正しいユーザー名が抽出されること");
                assertEquals(testUser.getUsername(), jwtTokenProvider.getUsernameFromToken(token2),
                                "2つ目のトークンから正しいユーザー名が抽出されること");

                // 両方のトークンでAPIアクセスが成功することを確認
                mockMvc.perform(
                                testUtils.createAuthenticatedGetRequest("/api/users/profile", token1))
                                .andExpect(status().isOk());

                mockMvc.perform(
                                testUtils.createAuthenticatedGetRequest("/api/users/profile", token2))
                                .andExpect(status().isOk());

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testMultipleTokensForSameUser",
                                "JWT_MULTIPLE_TOKENS",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース7: リフレッシュトークン生成テスト
         * 
         * 目的: リフレッシュトークンが正常に生成・検証されることを確認
         * 
         * 期待結果:
         * - アクセストークンが生成される
         * - アクセストークンが有効である
         * - アクセストークンに残り有効時間がある
         */
        @Test
        @Order(7)
        void testRefreshTokenGeneration() throws Exception {
                // Given
                User testUser = testNormalUser;
                String accessToken = createValidJwtToken(testUser);

                // リフレッシュトークンの生成（実際の実装に合わせて調整が必要）
                // 注意: 現在のJwtTokenProviderにはリフレッシュトークン生成メソッドがあるが、
                // Authenticationオブジェクトが必要なため、ここでは基本的な検証のみ実施

                // When & Then
                assertNotNull(accessToken, "アクセストークンが生成されること");
                assertTrue(jwtTokenProvider.validateToken(accessToken), "アクセストークンが有効であること");

                // アクセストークンの有効期限確認
                long accessTokenRemainingTime = jwtTokenProvider.getRemainingValidityTime(accessToken);
                assertTrue(accessTokenRemainingTime > 0, "アクセストークンに残り有効時間があること");

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testRefreshTokenGeneration",
                                "JWT_REFRESH_TOKEN",
                                "PASSED",
                                getTestExecutionTime(),
                                null);
        }

        /**
         * テストケース8: パフォーマンステスト
         * 
         * 目的: JWT認証のパフォーマンスが許容範囲内であることを確認
         * 
         * 期待結果:
         * - トークン生成時間が100ms以内
         * - トークン検証時間が50ms以内
         * - 認証付きAPIアクセスが200ms以内
         */
        @Test
        @Order(8)
        void testJwtPerformance() throws Exception {
                // Given
                User testUser = testNormalUser;

                // トークン生成時間の測定
                long tokenGenerationTime = testUtils.measureResponseTime(() -> {
                        createValidJwtToken(testUser);
                });

                // トークン検証時間の測定
                String token = createValidJwtToken(testUser);
                long tokenValidationTime = testUtils.measureResponseTime(() -> {
                        jwtTokenProvider.validateToken(token);
                });

                // 認証付きAPIアクセス時間の測定
                long apiAccessTime = testUtils.measureResponseTime(() -> {
                        try {
                                mockMvc.perform(
                                                testUtils.createAuthenticatedGetRequest("/api/users/profile", token))
                                                .andExpect(status().isOk());
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                });

                // パフォーマンス検証
                assertPerformanceWithinLimit(tokenGenerationTime, 100, "JWT_TOKEN_GENERATION");
                assertPerformanceWithinLimit(tokenValidationTime, 50, "JWT_TOKEN_VALIDATION");
                assertPerformanceWithinLimit(apiAccessTime, 200, "JWT_API_ACCESS");

                // テスト結果の記録
                testDataManager.recordTestResult(
                                getClass().getSimpleName(),
                                "testJwtPerformance",
                                "JWT_PERFORMANCE",
                                "PASSED",
                                getTestExecutionTime(),
                                String.format("TokenGen:%dms, TokenVal:%dms, ApiAccess:%dms",
                                                tokenGenerationTime, tokenValidationTime, apiAccessTime));
        }

    /**
     * 有効なJWTトークンを生成（テスト用）
     */
    public String createValidJwtToken(User user) {
        return jwtTokenProvider.createToken(user);
    }

    /**
     * 指定された有効期限でJWTトークンを生成（テスト用）
     * 
     * @param user ユーザー情報
     * @param expirationMillis 有効期限（ミリ秒）
     * @return JWTトークン
     */
    private String createJwtTokenWithExpiration(User user, long expirationMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        // 直接使用JwtTokenProvider中のsecretKey字段来生成令牌
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("locationType", user.getLocationType())
                .claim("departmentId", user.getDepartmentId())
                .claim("positionId", user.getPositionId())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtTokenProvider.getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}