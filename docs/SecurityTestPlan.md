# セキュリティテスト検証計画書

## 概要
本書は、Company Backend システムのセキュリティテスト検証計画を詳細に定義します。JWT認証、XSS保護、CSRF保護、レート制限、SQLインジェクション保護の5つの主要セキュリティ領域について、包括的なテスト戦略を提供します。

## 1. テスト対象システム概要

### 1.1 システム構成
- **フレームワーク**: Spring Boot 3.x + Spring Security 6.x
- **認証方式**: JWT (JSON Web Token)
- **データベース**: PostgreSQL
- **セキュリティ設定**: `SecurityConfig.java`
- **JWT実装**: `JwtTokenProvider.java`, `JwtAuthenticationFilter.java`

### 1.2 現在のセキュリティ実装
```java
// 主要セキュリティコンポーネント
- SecurityConfig.java           // メインセキュリティ設定
- JwtAuthenticationFilter.java  // JWT認証フィルター
- JwtTokenProvider.java         // JWTトークン管理
- JwtAuthenticationEntryPoint.java // 認証エラーハンドリング
- CustomUserDetailsService.java // ユーザー詳細サービス
```

## 2. JWT認証テスト

### 2.1 テスト目標
- JWTトークンの生成・検証機能の確認
- トークンの有効期限管理
- 不正トークンの検出・拒否
- トークンリフレッシュ機能

### 2.2 テストケース設計

#### 2.2.1 正常系テスト
```java
/**
 * JWT認証正常系テストケース
 */
@TestMethodOrder(OrderAnnotation.class)
class JwtAuthenticationTest {
    
    @Test
    @Order(1)
    void testValidTokenGeneration() {
        // 有効なユーザー認証情報でトークン生成
        // 期待結果: 正常なJWTトークンが生成される
    }
    
    @Test
    @Order(2)
    void testValidTokenAuthentication() {
        // 有効なJWTトークンでAPI認証
        // 期待結果: 認証成功、リソースアクセス可能
    }
    
    @Test
    @Order(3)
    void testTokenRefresh() {
        // トークンリフレッシュ機能
        // 期待結果: 新しいトークンが正常に発行される
    }
}
```

#### 2.2.2 異常系テスト
```java
/**
 * JWT認証異常系テストケース
 */
class JwtAuthenticationSecurityTest {
    
    @Test
    void testExpiredToken() {
        // 期限切れトークンでのアクセス
        // 期待結果: 401 Unauthorized
    }
    
    @Test
    void testInvalidSignature() {
        // 署名が改ざんされたトークン
        // 期待結果: 401 Unauthorized
    }
    
    @Test
    void testMalformedToken() {
        // 不正な形式のトークン
        // 期待結果: 401 Unauthorized
    }
    
    @Test
    void testMissingToken() {
        // トークンなしでの保護されたリソースアクセス
        // 期待結果: 401 Unauthorized
    }
}
```

#### 2.2.3 境界値テスト
```java
/**
 * JWT境界値テストケース
 */
class JwtBoundaryTest {
    
    @Test
    void testTokenExpirationBoundary() {
        // トークン有効期限の境界値テスト
        // 期待結果: 期限直前は有効、期限後は無効
    }
    
    @Test
    void testMaximumTokenSize() {
        // 最大サイズのトークン処理
        // 期待結果: 適切に処理される
    }
    
    @Test
    void testConcurrentTokenValidation() {
        // 同時多重トークン検証
        // 期待結果: 全て正常に処理される
    }
}
```

### 2.3 実装計画

#### 2.3.1 テストクラス構造
```
src/test/java/com/example/companybackend/security/
├── JwtAuthenticationTest.java           // 正常系テスト
├── JwtAuthenticationSecurityTest.java   // 異常系テスト
├── JwtBoundaryTest.java                // 境界値テスト
├── JwtPerformanceTest.java             // パフォーマンステスト
└── JwtIntegrationTest.java             // 統合テスト
```

#### 2.3.2 テストデータ準備
```java
/**
 * JWTテスト用データファクトリー
 */
@Component
public class JwtTestDataFactory {
    
    public String createValidToken(String username) {
        // 有効なテストトークン生成
    }
    
    public String createExpiredToken(String username) {
        // 期限切れテストトークン生成
    }
    
    public String createInvalidSignatureToken(String username) {
        // 不正署名テストトークン生成
    }
    
    public String createMalformedToken() {
        // 不正形式テストトークン生成
    }
}
```

## 3. XSS保護テスト

### 3.1 テスト目標
- クロスサイトスクリプティング攻撃の防御
- 入力値サニタイゼーション機能
- HTTPヘッダーセキュリティ設定
- Content Security Policy (CSP) の有効性

### 3.2 テストケース設計

#### 3.2.1 入力値検証テスト
```java
/**
 * XSS保護入力値検証テスト
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class XssProtectionTest {
    
    @Test
    void testScriptTagInjection() {
        // <script>タグを含む入力値
        String maliciousInput = "<script>alert('XSS')</script>";
        // 期待結果: サニタイズされるか拒否される
    }
    
    @Test
    void testEventHandlerInjection() {
        // イベントハンドラーを含む入力値
        String maliciousInput = "<img src='x' onerror='alert(1)'>";
        // 期待結果: サニタイズされるか拒否される
    }
    
    @Test
    void testJavaScriptUrlInjection() {
        // javascript:スキームを含む入力値
        String maliciousInput = "javascript:alert('XSS')";
        // 期待結果: サニタイズされるか拒否される
    }
}
```

#### 3.2.2 HTTPヘッダーセキュリティテスト
```java
/**
 * HTTPヘッダーセキュリティテスト
 */
@WebMvcTest
class HttpHeaderSecurityTest {
    
    @Test
    void testContentSecurityPolicyHeader() {
        // CSPヘッダーの存在確認
        // 期待結果: 適切なCSPヘッダーが設定されている
    }
    
    @Test
    void testXFrameOptionsHeader() {
        // X-Frame-Optionsヘッダーの確認
        // 期待結果: DENY または SAMEORIGIN が設定されている
    }
    
    @Test
    void testXContentTypeOptionsHeader() {
        // X-Content-Type-Optionsヘッダーの確認
        // 期待結果: nosniff が設定されている
    }
    
    @Test
    void testXXssProtectionHeader() {
        // X-XSS-Protectionヘッダーの確認
        // 期待結果: 1; mode=block が設定されている
    }
}
```

#### 3.2.3 レスポンス検証テスト
```java
/**
 * XSSレスポンス検証テスト
 */
class XssResponseValidationTest {
    
    @Test
    void testHtmlEscaping() {
        // HTMLエスケープ処理の確認
        // 期待結果: 特殊文字が適切にエスケープされる
    }
    
    @Test
    void testJsonResponseSafety() {
        // JSONレスポンスの安全性確認
        // 期待結果: スクリプト実行可能な内容が含まれない
    }
}
```

### 3.3 実装計画

#### 3.3.1 XSS保護設定の強化
```java
/**
 * XSS保護設定の追加
 */
@Configuration
public class XssProtectionConfig {
    
    @Bean
    public FilterRegistrationBean<XssFilter> xssFilter() {
        // XSSフィルターの設定
    }
    
    @Bean
    public HttpFirewall httpFirewall() {
        // HTTPファイアウォールの設定
    }
}
```

## 4. CSRF保護テスト

### 4.1 テスト目標
- クロスサイトリクエストフォージェリ攻撃の防御
- CSRFトークンの生成・検証機能
- SameSite Cookieの設定確認
- Referrerヘッダー検証

### 4.2 テストケース設計

#### 4.2.1 CSRF攻撃シミュレーションテスト
```java
/**
 * CSRF攻撃シミュレーションテスト
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CsrfProtectionTest {
    
    @Test
    void testCsrfTokenValidation() {
        // 有効なCSRFトークンでのリクエスト
        // 期待結果: リクエスト成功
    }
    
    @Test
    void testMissingCsrfToken() {
        // CSRFトークンなしでのPOSTリクエスト
        // 期待結果: 403 Forbidden
    }
    
    @Test
    void testInvalidCsrfToken() {
        // 無効なCSRFトークンでのリクエスト
        // 期待結果: 403 Forbidden
    }
    
    @Test
    void testCsrfTokenReuse() {
        // CSRFトークンの再利用攻撃
        // 期待結果: 2回目のリクエストは拒否される
    }
}
```

#### 4.2.2 SameSite Cookie テスト
```java
/**
 * SameSite Cookie テスト
 */
class SameSiteCookieTest {
    
    @Test
    void testSameSiteStrictCookie() {
        // SameSite=Strict設定の確認
        // 期待結果: クロスサイトリクエストでCookieが送信されない
    }
    
    @Test
    void testSecureCookieFlag() {
        // Secure フラグの確認
        // 期待結果: HTTPS接続でのみCookieが送信される
    }
    
    @Test
    void testHttpOnlyCookieFlag() {
        // HttpOnly フラグの確認
        // 期待結果: JavaScriptからCookieにアクセスできない
    }
}
```

### 4.3 実装計画

#### 4.3.1 CSRF保護設定の追加
```java
/**
 * CSRF保護設定（必要に応じて）
 */
@Configuration
public class CsrfProtectionConfig {
    
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        // CSRFトークンリポジトリの設定
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }
}
```

## 5. レート制限テスト

### 5.1 テスト目標
- API呼び出し頻度の制限機能
- DDoS攻撃の防御
- ユーザー別・IP別レート制限
- レート制限超過時の適切なレスポンス

### 5.2 テストケース設計

#### 5.2.1 レート制限機能テスト
```java
/**
 * レート制限機能テスト
 */
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class RateLimitingTest {
    
    @Test
    @Order(1)
    void testNormalRateLimit() {
        // 通常の頻度でのAPIアクセス
        // 期待結果: 全てのリクエストが成功
    }
    
    @Test
    @Order(2)
    void testRateLimitExceeded() {
        // レート制限を超える頻度でのAPIアクセス
        // 期待結果: 429 Too Many Requests
    }
    
    @Test
    @Order(3)
    void testRateLimitReset() {
        // レート制限リセット後のアクセス
        // 期待結果: 再びアクセス可能
    }
    
    @Test
    void testPerUserRateLimit() {
        // ユーザー別レート制限
        // 期待結果: ユーザーごとに独立してカウント
    }
    
    @Test
    void testPerIpRateLimit() {
        // IP別レート制限
        // 期待結果: IPアドレスごとに独立してカウント
    }
}
```

#### 5.2.2 レート制限回避攻撃テスト
```java
/**
 * レート制限回避攻撃テスト
 */
class RateLimitBypassTest {
    
    @Test
    void testIpSpoofingAttempt() {
        // IPスプーフィング攻撃の試行
        // 期待結果: 攻撃が検出され、ブロックされる
    }
    
    @Test
    void testUserAgentRotation() {
        // User-Agentローテーション攻撃
        // 期待結果: レート制限が適用される
    }
    
    @Test
    void testDistributedAttack() {
        // 分散攻撃のシミュレーション
        // 期待結果: 適切にレート制限が適用される
    }
}
```

### 5.3 実装計画

#### 5.3.1 レート制限機能の実装
```java
/**
 * レート制限フィルター
 */
@Component
public class RateLimitingFilter implements Filter {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        // レート制限ロジックの実装
    }
    
    private boolean isRateLimitExceeded(String key, int limit, int windowSeconds) {
        // レート制限チェックロジック
    }
}
```

#### 5.3.2 レート制限設定
```java
/**
 * レート制限設定
 */
@ConfigurationProperties(prefix = "app.rate-limit")
@Data
public class RateLimitProperties {
    private int requestsPerMinute = 60;
    private int requestsPerHour = 1000;
    private int requestsPerDay = 10000;
    private boolean enabled = true;
}
```

## 6. SQLインジェクション保護テスト

### 6.1 テスト目標
- SQLインジェクション攻撃の防御
- パラメータ化クエリの使用確認
- 入力値検証機能
- データベースエラー情報の漏洩防止

### 6.2 テストケース設計

#### 6.2.1 SQLインジェクション攻撃テスト
```java
/**
 * SQLインジェクション攻撃テスト
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SqlInjectionProtectionTest {
    
    @Test
    void testUnionBasedInjection() {
        // UNION ベースのSQLインジェクション
        String maliciousInput = "1' UNION SELECT username, password FROM users--";
        // 期待結果: 攻撃が防がれる
    }
    
    @Test
    void testBooleanBasedInjection() {
        // Boolean ベースのSQLインジェクション
        String maliciousInput = "1' AND 1=1--";
        // 期待結果: 攻撃が防がれる
    }
    
    @Test
    void testTimeBasedInjection() {
        // Time ベースのSQLインジェクション
        String maliciousInput = "1'; WAITFOR DELAY '00:00:05'--";
        // 期待結果: 攻撃が防がれる
    }
    
    @Test
    void testErrorBasedInjection() {
        // Error ベースのSQLインジェクション
        String maliciousInput = "1' AND (SELECT COUNT(*) FROM information_schema.tables)>0--";
        // 期待結果: 攻撃が防がれ、エラー情報が漏洩しない
    }
}
```

#### 6.2.2 パラメータ化クエリ検証テスト
```java
/**
 * パラメータ化クエリ検証テスト
 */
class ParameterizedQueryTest {
    
    @Test
    void testJpaRepositoryQueries() {
        // JPAリポジトリクエリの安全性確認
        // 期待結果: 全てパラメータ化されている
    }
    
    @Test
    void testNativeQueries() {
        // ネイティブクエリの安全性確認
        // 期待結果: パラメータ化されているか適切に検証されている
    }
    
    @Test
    void testDynamicQueries() {
        // 動的クエリの安全性確認
        // 期待結果: 安全に構築されている
    }
}
```

#### 6.2.3 入力値検証テスト
```java
/**
 * 入力値検証テスト
 */
class InputValidationTest {
    
    @Test
    void testSpecialCharacterFiltering() {
        // 特殊文字のフィルタリング
        // 期待結果: 危険な文字が適切に処理される
    }
    
    @Test
    void testInputLengthValidation() {
        // 入力長の検証
        // 期待結果: 異常に長い入力が拒否される
    }
    
    @Test
    void testDataTypeValidation() {
        // データ型の検証
        // 期待結果: 不正なデータ型が拒否される
    }
}
```

### 6.3 実装計画

#### 6.3.1 入力値検証の強化
```java
/**
 * 入力値検証設定
 */
@Component
public class InputValidationConfig {
    
    @Bean
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
    
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }
}
```

#### 6.3.2 SQLインジェクション検出フィルター
```java
/**
 * SQLインジェクション検出フィルター
 */
@Component
public class SqlInjectionDetectionFilter implements Filter {
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript)"
    );
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        // SQLインジェクション検出ロジック
    }
}
```

## 7. テスト実行計画

### 7.1 テスト環境構成

#### 7.1.1 テスト環境要件
```yaml
# application-security-test.yml
spring:
  profiles:
    active: security-test
  datasource:
    url: jdbc:postgresql://localhost:5432/company_security_test
    username: test_user
    password: test_password
  
app:
  jwt:
    secret: test-secret-key-for-security-testing-at-least-32-chars
    expiration: 3600000  # 1時間（テスト用短縮）
  
  rate-limit:
    enabled: true
    requests-per-minute: 10  # テスト用低設定
    requests-per-hour: 100
```

#### 7.1.2 テストデータベース設定
```sql
-- セキュリティテスト用データベース初期化
CREATE DATABASE company_security_test;
CREATE USER test_user WITH PASSWORD 'test_password';
GRANT ALL PRIVILEGES ON DATABASE company_security_test TO test_user;
```

### 7.2 テスト実行順序

#### 7.2.1 フェーズ1: 基本セキュリティテスト
1. JWT認証テスト（正常系）
2. 基本的な入力値検証テスト
3. HTTPヘッダーセキュリティテスト

#### 7.2.2 フェーズ2: 攻撃シミュレーションテスト
1. XSS攻撃テスト
2. CSRF攻撃テスト
3. SQLインジェクション攻撃テスト

#### 7.2.3 フェーズ3: 高度なセキュリティテスト
1. JWT攻撃テスト（異常系）
2. レート制限テスト
3. 境界値・パフォーマンステスト

### 7.3 テスト自動化

#### 7.3.1 CI/CDパイプライン統合
```yaml
# .github/workflows/security-tests.yml
name: Security Tests
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  security-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Security Tests
        run: |
          ./mvnw test -Dtest="**/*SecurityTest,**/*XssTest,**/*CsrfTest,**/*SqlInjectionTest,**/*RateLimitTest"
      
      - name: Generate Security Report
        run: |
          ./mvnw jacoco:report
          ./mvnw site -DgenerateReports=false
```

#### 7.3.2 セキュリティテストスイート
```java
/**
 * セキュリティテストスイート
 */
@Suite
@SelectClasses({
    JwtAuthenticationTest.class,
    JwtAuthenticationSecurityTest.class,
    XssProtectionTest.class,
    CsrfProtectionTest.class,
    RateLimitingTest.class,
    SqlInjectionProtectionTest.class
})
public class SecurityTestSuite {
    // テストスイート設定
}
```

## 8. テスト結果評価基準

### 8.1 成功基準

#### 8.1.1 JWT認証テスト
- [ ] 有効なトークンで認証成功率 100%
- [ ] 無効なトークンで認証拒否率 100%
- [ ] トークン有効期限の正確な管理
- [ ] 同時接続数 1000 での安定動作

#### 8.1.2 XSS保護テスト
- [ ] 既知のXSS攻撃パターン 100% ブロック
- [ ] セキュリティヘッダーの適切な設定
- [ ] 入力値サニタイゼーションの正常動作
- [ ] CSPポリシーの有効性確認

#### 8.1.3 CSRF保護テスト
- [ ] CSRFトークンなしリクエスト 100% 拒否
- [ ] 無効CSRFトークンリクエスト 100% 拒否
- [ ] SameSite Cookie の適切な設定
- [ ] Referrer検証の正常動作

#### 8.1.4 レート制限テスト
- [ ] 設定された制限値での正確な制御
- [ ] レート制限超過時の適切なレスポンス
- [ ] ユーザー別・IP別制限の独立動作
- [ ] 制限リセット機能の正常動作

#### 8.1.5 SQLインジェクション保護テスト
- [ ] 既知のSQLインジェクション攻撃 100% ブロック
- [ ] パラメータ化クエリの使用確認
- [ ] エラー情報漏洩の防止
- [ ] 入力値検証の適切な動作

### 8.2 パフォーマンス基準
- セキュリティチェックによるレスポンス時間増加: 10% 以内
- 同時接続数 1000 での安定動作
- メモリ使用量増加: 20% 以内
- CPU使用率増加: 15% 以内

### 8.3 レポート要件

#### 8.3.1 テスト結果レポート
```markdown
# セキュリティテスト結果レポート

## 実行概要
- 実行日時: YYYY-MM-DD HH:MM:SS
- テスト環境: [環境情報]
- 実行テスト数: XXX
- 成功: XXX, 失敗: XXX, スキップ: XXX

## 各テスト領域結果
### JWT認証テスト
- 実行: XX/XX
- 成功率: XX%
- 主要な問題: [問題があれば記載]

### XSS保護テスト
- 実行: XX/XX
- 成功率: XX%
- 主要な問題: [問題があれば記載]

[他の領域も同様]

## 推奨事項
1. [改善提案1]
2. [改善提案2]
3. [改善提案3]
```

## 9. リスク管理

### 9.1 テスト実行リスク
- **データベース影響**: テスト専用DBの使用
- **本番環境影響**: 完全分離の確保
- **パフォーマンス影響**: 負荷テストの段階的実行
- **セキュリティ情報漏洩**: テストデータの適切な管理

### 9.2 緊急時対応
- テスト失敗時の即座の調査・修正
- セキュリティ脆弱性発見時の緊急パッチ適用
- 本番環境への影響が疑われる場合の即座の報告

## 10. 継続的改善

### 10.1 定期レビュー
- 月次セキュリティテスト結果レビュー
- 四半期セキュリティ要件見直し
- 年次セキュリティテスト計画更新

### 10.2 新脅威への対応
- 最新のセキュリティ脅威情報の収集
- 新しい攻撃手法に対するテストケース追加
- セキュリティツールの定期更新

この計画書に基づいて、包括的で効果的なセキュリティテストを実施し、システムの安全性を確保します。