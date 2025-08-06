package com.example.companybackend.security.test.jwt;

import com.example.companybackend.entity.User;
import com.example.companybackend.repository.UserRepository;
import com.example.companybackend.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * JWTテスト用データファクトリー
 * 
 * 目的:
 * - テスト用JWTトークンの生成
 * - 有効・無効・期限切れトークンの作成
 * - テスト用ユーザーデータの準備
 * - セキュリティテストで使用する各種トークンパターンの提供
 * 
 * 使用方法:
 * - 正常系テスト: createValidToken()
 * - 異常系テスト: createExpiredToken(), createInvalidSignatureToken()
 * - 境界値テスト: createTokenWithCustomExpiration()
 * - 攻撃シミュレーション: createMaliciousToken()
 * 
 * 機能:
 * - 実データベースとの統合
 * - テストデータの自動管理
 * - セキュリティテスト用の特殊トークン生成
 */
@Component
public class JwtTestDataFactory {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.jwt.secret:security-test-jwt-secret-key-for-company-backend-system-at-least-32-chars-long}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:300000}")
    private long jwtExpiration;

    private User testUser;
    private SecretKey secretKey;

    /**
     * 初期化処理
     */
    private void initialize() {
        if (secretKey == null) {
            secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * テスト用ユーザーの準備
     * 実データベースから既存ユーザーを取得、存在しない場合は作成
     * 
     * @return テスト用ユーザー
     */
    public User getOrCreateTestUser() {
        if (testUser != null) {
            return testUser;
        }

        // 実データベースから既存ユーザーを取得
        List<User> existingUsers = userRepository.findAll();
        if (!existingUsers.isEmpty()) {
            testUser = existingUsers.get(0);
            return testUser;
        }

        // フォールバック: 新しいテストユーザーを作成
        testUser = new User();
        testUser.setUsername("jwt_test_user_" + System.currentTimeMillis());
        testUser.setPasswordHash("$2a$10$test.hash.for.jwt.testing");
        testUser.setEmail("jwt.test@company.com");
        testUser.setFullName("JWT Test User");
        testUser.setIsActive(true);
        testUser.setRole("USER");
        testUser.setLocationType("office");
        testUser = userRepository.save(testUser);
        return testUser;
    }

    /**
     * 有効なJWTトークンを生成
     * 
     * @param username ユーザー名（nullの場合はテストユーザーを使用）
     * @return 有効なJWTトークン
     */
    public String createValidToken(String username) {
        if (username == null) {
            User user = getOrCreateTestUser();
            username = user.getUsername();
        }
        return jwtTokenProvider.generateToken(createMockAuthentication(username));
    }

    /**
     * 有効なJWTトークンを生成（デフォルトテストユーザー使用）
     * 
     * @return 有効なJWTトークン
     */
    public String createValidToken() {
        return createValidToken((String) null);
    }

    /**
     * ユーザーオブジェクトから有効なJWTトークンを生成
     * 
     * @param user ユーザーオブジェクト
     * @return 有効なJWTトークン
     */
    public String createValidToken(User user) {
        return jwtTokenProvider.createToken(user);
    }

    /**
     * 期限切れJWTトークンを生成
     * 
     * @param username ユーザー名
     * @return 期限切れJWTトークン
     */
    public String createExpiredToken(String username) {
        initialize();

        if (username == null) {
            User user = getOrCreateTestUser();
            username = user.getUsername();
        }

        // 1時間前に期限切れのトークンを作成
        Instant expiration = Instant.now().minus(1, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 期限切れJWTトークンを生成（デフォルトテストユーザー使用）
     * 
     * @return 期限切れJWTトークン
     */
    public String createExpiredToken() {
        return createExpiredToken(null);
    }

    /**
     * 不正な署名を持つJWTトークンを生成
     * 
     * @param username ユーザー名
     * @return 不正署名JWTトークン
     */
    public String createInvalidSignatureToken(String username) {
        if (username == null) {
            User user = getOrCreateTestUser();
            username = user.getUsername();
        }

        // 異なる秘密鍵で署名
        String wrongSecret = "wrong-secret-key-for-testing-invalid-signature-at-least-32-chars-long";
        SecretKey wrongKey = Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(wrongKey)
                .compact();
    }

    /**
     * 不正な署名を持つJWTトークンを生成（デフォルトテストユーザー使用）
     * 
     * @return 不正署名JWTトークン
     */
    public String createInvalidSignatureToken() {
        return createInvalidSignatureToken(null);
    }

    /**
     * 不正な形式のJWTトークンを生成
     * 
     * @return 不正形式JWTトークン
     */
    public String createMalformedToken() {
        return "invalid.jwt.token.format";
    }

    /**
     * カスタム有効期限を持つJWTトークンを生成
     * 
     * @param username          ユーザー名
     * @param expirationMinutes 有効期限（分）
     * @return カスタム有効期限JWTトークン
     */
    public String createTokenWithCustomExpiration(String username, long expirationMinutes) {
        initialize();

        if (username == null) {
            User user = getOrCreateTestUser();
            username = user.getUsername();
        }

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 空のクレームを持つJWTトークンを生成
     * 
     * @return 空クレームJWTトークン
     */
    public String createTokenWithEmptyClaims() {
        initialize();

        return Jwts.builder()
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 存在しないユーザーのJWTトークンを生成
     * 
     * @return 存在しないユーザーのJWTトークン
     */
    public String createTokenForNonExistentUser() {
        initialize();

        String nonExistentUsername = "non_existent_user_" + System.currentTimeMillis();

        return Jwts.builder()
                .subject(nonExistentUsername)
                .claim("userId", 99999L)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 悪意のあるクレームを含むJWTトークンを生成
     * 
     * @return 悪意のあるクレームを含むJWTトークン
     */
    public String createMaliciousToken() {
        initialize();

        User user = getOrCreateTestUser();

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", "ADMIN") // 権限昇格を試行
                .claim("malicious", "<script>alert('XSS')</script>")
                .claim("sql_injection", "'; DROP TABLE users; --")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 異常に大きなクレームを含むJWTトークンを生成（DoS攻撃テスト用）
     * 
     * @return 大きなクレームを含むJWTトークン
     */
    public String createLargeClaimsToken() {
        initialize();

        User user = getOrCreateTestUser();
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeData.append("Large data payload for DoS testing ");
        }

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("largeData", largeData.toString())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 特殊文字を含むクレームのJWTトークンを生成
     * 
     * @return 特殊文字を含むJWTトークン
     */
    public String createSpecialCharacterToken() {
        initialize();

        return Jwts.builder()
                .subject("test_user_special_chars")
                .claim("special_chars", "!@#$%^&*()_+-=[]{}|;':\",./<>?`~")
                .claim("unicode", "🚀🔒💻")
                .claim("null_bytes", "\u0000\u0001\u0002")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 管理者権限を持つJWTトークンを生成
     * 
     * @return 管理者JWTトークン
     */
    public String createAdminToken() {
        // 管理者ユーザーを取得または作成
        Optional<User> adminUser = userRepository.findByUsername("security_test_admin");
        if (adminUser.isPresent()) {
            return createValidToken(adminUser.get());
        }

        // 管理者ユーザーが存在しない場合は作成
        User admin = new User();
        admin.setUsername("security_test_admin");
        admin.setPasswordHash("$2a$10$test.hash.for.admin.testing");
        admin.setEmail("admin.test@company.com");
        admin.setFullName("Security Test Admin");
        admin.setIsActive(true);
        admin.setRole("ADMIN");
        admin.setLocationType("office");
        admin = userRepository.save(admin);

        return createValidToken(admin);
    }

    /**
     * マネージャー権限を持つJWTトークンを生成
     * 
     * @return マネージャーJWTトークン
     */
    public String createManagerToken() {
        // マネージャーユーザーを取得または作成
        Optional<User> managerUser = userRepository.findByUsername("security_test_manager");
        if (managerUser.isPresent()) {
            return createValidToken(managerUser.get());
        }

        // マネージャーユーザーが存在しない場合は作成
        User manager = new User();
        manager.setUsername("security_test_manager");
        manager.setPasswordHash("$2a$10$test.hash.for.manager.testing");
        manager.setEmail("manager.test@company.com");
        manager.setFullName("Security Test Manager");
        manager.setIsActive(true);
        manager.setRole("MANAGER");
        manager.setLocationType("office");
        manager = userRepository.save(manager);

        return createValidToken(manager);
    }

    /**
     * テストデータのクリーンアップ
     * テスト終了後に作成したテストユーザーを削除
     */
    public void cleanup() {
        if (testUser != null && testUser.getId() != null) {
            // 作成したテストユーザーのみ削除（実データは保持）
            if (testUser.getUsername().startsWith("jwt_test_user_")) {
                userRepository.delete(testUser);
            }
            testUser = null;
        }
    }

    /**
     * JWTトークンからユーザー名を抽出（テスト用）
     * 
     * @param token JWTトークン
     * @return ユーザー名
     */
    public String extractUsernameFromToken(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }

    /**
     * JWTトークンの有効性を検証（テスト用）
     * 
     * @param token JWTトークン
     * @return 有効性
     */
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    /**
     * JWTトークンの残り有効時間を取得（テスト用）
     * 
     * @param token JWTトークン
     * @return 残り有効時間（ミリ秒）
     */
    public long getRemainingValidityTime(String token) {
        return jwtTokenProvider.getRemainingValidityTime(token);
    }

    /**
     * JWTトークンが期限切れかどうかを確認（テスト用）
     * 
     * @param token JWTトークン
     * @return 期限切れかどうか
     */
    public boolean isTokenExpired(String token) {
        return jwtTokenProvider.isTokenExpired(token);
    }

    /**
     * モック認証オブジェクトを作成（内部使用）
     * 
     * @param username ユーザー名
     * @return モック認証オブジェクト
     */
    private org.springframework.security.core.Authentication createMockAuthentication(String username) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                username, null, java.util.Collections.emptyList());
    }

    /**
     * テスト用のランダムなJWTトークンを生成
     * 
     * @return ランダムJWTトークン
     */
    public String createRandomToken() {
        String randomUsername = "random_user_" + System.currentTimeMillis();
        return createTokenWithCustomExpiration(randomUsername, 60); // 1時間有効
    }

    /**
     * 複数の異なるJWTトークンを一括生成
     * 
     * @param count 生成するトークン数
     * @return JWTトークンのリスト
     */
    public List<String> createMultipleTokens(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createRandomToken())
                .collect(java.util.stream.Collectors.toList());
    }
}