# JwtAuthenticationTest テストケース作成手順書

## 概要
本書は、`JwtAuthenticationTest` のテストケース作成における注釈、モック対象、テスト作成の流れとコツを詳細に説明した手順書です。JWT認証機能の特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/security/test/jwt/JwtAuthenticationTest.java`

### 1.2 基本アノテーション

#### @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
**行**: 32
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("security-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JwtAuthenticationTest extends SecurityTestBase {
```

**目的**:
- Spring Boot統合テスト環境の構築
- ランダムポートでのWebサーバー起動
- セキュリティテスト専用プロファイルの適用
- 実データベースとの統合テスト実行

**JWT認証テストの特徴**:
- セキュリティフィルターチェーンの完全な統合テスト
- 実際のHTTPリクエスト・レスポンスでの認証フロー検証
- データベース連携でのユーザー情報取得・検証
- 複数ユーザー・権限での同時認証テスト

### 1.3 継承クラス: SecurityTestBase

#### SecurityTestBase の役割
**行**: 36
```java
class JwtAuthenticationTest extends SecurityTestBase {
```

**提供される機能**:
- **テストユーザー管理**: `testNormalUser`, `testAdminUser`, `testManagerUser`
- **JWT関連コンポーネント**: `jwtTokenProvider`, `mockMvc`
- **テストデータ管理**: `testDataManager`, `testUtils`
- **セキュリティ設定**: セキュリティテスト用の共通設定

**主要なフィールド**:
```java
// SecurityTestBase から継承される主要フィールド
@Autowired
protected JwtTokenProvider jwtTokenProvider;

@Autowired
protected MockMvc mockMvc;

@Autowired
protected SecurityTestDataManager testDataManager;

@Autowired
protected SecurityTestUtils testUtils;

protected User testNormalUser;
protected User testAdminUser;
protected User testManagerUser;
```

### 1.4 テスト実行順序制御

#### @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
**行**: 36
```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
```

**目的**:
- テストケースの実行順序を制御
- 基本機能から高度な機能へ段階的にテスト
- 依存関係のあるテストケースの順序保証

**実行順序**:
1. `@Order(1)` - 基本認証テスト
2. `@Order(2)` - 管理者認証テスト
3. `@Order(3)` - 同時認証テスト
4. `@Order(4)` - 情報抽出テスト
5. `@Order(5)` - 有効期限テスト
6. `@Order(6)` - 複数トークンテスト
7. `@Order(7)` - リフレッシュトークンテスト
8. `@Order(8)` - パフォーマンステスト

### 1.5 セキュリティテストタイプ識別

#### getSecurityTestType() メソッド
**行**: 38-41
```java
@Override
protected String getSecurityTestType() {
    return "JWT_AUTHENTICATION";
}
```

**役割**:
- テストタイプの識別子を提供
- テスト結果記録時の分類に使用
- セキュリティテストレポート生成での分類基準

## 2. テストケース詳細解析

### 2.1 基本認証テスト群

#### テストケース1: 有効なJWTトークンでの認証成功テスト
**メソッド**: `testValidTokenAuthentication`
**行**: 56-89
**要件対応**: 要件1.1

##### テストデータ準備
```java
// Given (行58-59)
User testUser = testNormalUser;
String validToken = createValidJwtToken(testUser);
```

##### 認証リクエスト実行
```java
// When & Then (行61-65)
MvcResult result = mockMvc.perform(
    testUtils.createAuthenticatedGetRequest("/api/users/profile", validToken)
)
.andExpect(status().isOk())
.andReturn();
```

##### 多段階検証
```java
// 基本検証 (行67-71)
assertNotNull(validToken, "有効なJWTトークンが生成されること");
assertTrue(jwtTokenProvider.validateToken(validToken), "生成されたトークンが有効であること");
assertEquals(testUser.getUsername(), jwtTokenProvider.getUsernameFromToken(validToken), 
            "トークンから正しいユーザー名が抽出されること");

// セキュリティヘッダー検証 (行73-74)
assertTrue(testUtils.hasSecurityHeaders(result), "セキュリティヘッダーが設定されていること");

// テスト結果記録 (行76-83)
testDataManager.recordTestResult(
    getClass().getSimpleName(),
    "testValidTokenAuthentication",
    "JWT_AUTH_VALID",
    "PASSED",
    getTestExecutionTime(),
    null
);
```

#### テストケース2: 管理者トークンでの認証テスト
**メソッド**: `testAdminTokenAuthentication`
**行**: 91-120

##### 管理者権限テスト
```java
// Given (行99)
String adminToken = createAdminJwtToken();

// When & Then (行101-104)
mockMvc.perform(
    testUtils.createAuthenticatedGetRequest("/api/users", adminToken)
)
.andExpect(status().isOk());

// 管理者トークン詳細検証 (行106-109)
assertTrue(jwtTokenProvider.validateToken(adminToken), "管理者トークンが有効であること");
assertEquals(testAdminUser.getUsername(), jwtTokenProvider.getUsernameFromToken(adminToken),
            "管理者トークンから正しいユーザー名が抽出されること");
```

### 2.2 高度な認証テスト群

#### テストケース3: 複数ユーザーでの同時認証テスト
**メソッド**: `testConcurrentTokenAuthentication`
**行**: 122-180
**要件対応**: 要件1.8

##### 複数トークン準備
```java
// Given (行132-134)
String userToken = createUserJwtToken();
String adminToken = createAdminJwtToken();
String managerToken = createManagerJwtToken();
```

##### 同時リクエスト実行
```java
// When & Then - 同時リクエストの実行 (行136-158)
testUtils.executeConcurrentRequests(() -> {
    try {
        // 一般ユーザーのアクセス
        mockMvc.perform(
            testUtils.createAuthenticatedGetRequest("/api/users/profile", userToken)
        ).andExpected(status().isOk());
        
        // 管理者のアクセス
        mockMvc.perform(
            testUtils.createAuthenticatedGetRequest("/api/users", adminToken)
        ).andExpected(status().isOk());
        
        // マネージャーのアクセス
        mockMvc.perform(
            testUtils.createAuthenticatedGetRequest("/api/users/profile", managerToken)
        ).andExpected(status().isOk());
        
    } catch (Exception e) {
        throw new RuntimeException("同時認証テストでエラーが発生", e);
    }
}, 3, 5); // 3スレッド、各5回実行
```

##### 同時実行後の検証
```java
// 全トークンが依然として有効であることを確認 (行160-162)
assertTrue(jwtTokenProvider.validateToken(userToken), "一般ユーザートークンが有効であること");
assertTrue(jwtTokenProvider.validateToken(adminToken), "管理者トークンが有効であること");
assertTrue(jwtTokenProvider.validateToken(managerToken), "マネージャートークンが有効であること");
```

#### テストケース4: トークン情報抽出テスト
**メソッド**: `testTokenInformationExtraction`
**行**: 182-213

##### 情報抽出テスト
```java
// Given (行190-191)
User testUser = testNormalUser;
String token = createValidJwtToken(testUser);

// When & Then (行193-197)
String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);
String extractedLocationType = jwtTokenProvider.getLocationTypeFromToken(token);
Integer extractedDepartmentId = jwtTokenProvider.getDepartmentIdFromToken(token);

// 検証 (行199-202)
assertEquals(testUser.getUsername(), extractedUsername, "ユーザー名が正しく抽出されること");
assertEquals(testUser.getId(), extractedUserId, "ユーザーIDが正しく抽出されること");
assertEquals(testUser.getLocationType(), extractedLocationType, "位置タイプが正しく抽出されること");
assertEquals(testUser.getDepartmentId(), extractedDepartmentId, "部署IDが正しく抽出されること");
```

### 2.3 有効期限・境界値テスト群

#### テストケース5: トークン有効期限境界値テスト
**メソッド**: `testTokenExpirationBoundary`
**行**: 215-248
**要件対応**: 要件1.7

##### 有効期限境界値検証
```java
// Given (行225-226)
User testUser = testNormalUser;
String token = createValidJwtToken(testUser);

// When & Then
// 有効期限内での検証 (行228-230)
assertTrue(jwtTokenProvider.validateToken(token), "有効期限内のトークンは有効であること");
assertFalse(jwtTokenProvider.isTokenExpired(token), "有効期限内のトークンは期限切れでないこと");

// 残り有効時間の確認 (行232-235)
long remainingTime = jwtTokenProvider.getRemainingValidityTime(token);
assertTrue(remainingTime > 0, "有効期限内のトークンは残り時間が正の値であること");
assertTrue(remainingTime <= 300000, "残り時間がテスト用設定値以下であること"); // 5分以下
```

#### テストケース6: 同一ユーザーでの複数トークン生成テスト
**メソッド**: `testMultipleTokensForSameUser`
**行**: 250-300

##### 複数トークン独立性テスト
```java
// Given (行258)
User testUser = testNormalUser;

// When (行260-263)
String token1 = createValidJwtToken(testUser);
Thread.sleep(1000); // 1秒待機してトークンの生成時刻を変える
String token2 = createValidJwtToken(testUser);

// Then
// トークンの独立性確認 (行265)
assertNotEquals(token1, token2, "同一ユーザーでも異なるトークンが生成されること");

// 両方のトークンの有効性確認 (行267-269)
assertTrue(jwtTokenProvider.validateToken(token1), "1つ目のトークンが有効であること");
assertTrue(jwtTokenProvider.validateToken(token2), "2つ目のトークンが有効であること");

// 情報抽出の一貫性確認 (行271-275)
assertEquals(testUser.getUsername(), jwtTokenProvider.getUsernameFromToken(token1),
            "1つ目のトークンから正しいユーザー名が抽出されること");
assertEquals(testUser.getUsername(), jwtTokenProvider.getUsernameFromToken(token2),
            "2つ目のトークンから正しいユーザー名が抽出されること");

// 両方のトークンでのAPIアクセス確認 (行277-284)
mockMvc.perform(
    testUtils.createAuthenticatedGetRequest("/api/users/profile", token1)
).andExpected(status().isOk());

mockMvc.perform(
    testUtils.createAuthenticatedGetRequest("/api/users/profile", token2)
).andExpected(status().isOk());
```

### 2.4 拡張機能テスト群

#### テストケース7: リフレッシュトークン生成テスト
**メソッド**: `testRefreshTokenGeneration`
**行**: 302-334

##### リフレッシュトークンテスト
```java
// Given (行310-311)
User testUser = testNormalUser;
String accessToken = createValidJwtToken(testUser);

// When & Then (行318-319)
assertNotNull(accessToken, "アクセストークンが生成されること");
assertTrue(jwtTokenProvider.validateToken(accessToken), "アクセストークンが有効であること");

// アクセストークンの有効期限確認 (行321-322)
long accessTokenRemainingTime = jwtTokenProvider.getRemainingValidityTime(accessToken);
assertTrue(accessTokenRemainingTime > 0, "アクセストークンに残り有効時間があること");
```

#### テストケース8: パフォーマンステスト
**メソッド**: `testJwtPerformance`
**行**: 336-384

##### パフォーマンス測定テスト
```java
// Given (行344)
User testUser = testNormalUser;

// トークン生成時間の測定 (行346-349)
long tokenGenerationTime = testUtils.measureResponseTime(() -> {
    createValidJwtToken(testUser);
});

// トークン検証時間の測定 (行351-355)
String token = createValidJwtToken(testUser);
long tokenValidationTime = testUtils.measureResponseTime(() -> {
    jwtTokenProvider.validateToken(token);
});

// 認証付きAPIアクセス時間の測定 (行357-367)
long apiAccessTime = testUtils.measureResponseTime(() -> {
    try {
        mockMvc.perform(
            testUtils.createAuthenticatedGetRequest("/api/users/profile", token)
        ).andExpected(status().isOk());
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
});

// パフォーマンス検証 (行369-371)
assertPerformanceWithinLimit(tokenGenerationTime, 100, "JWT_TOKEN_GENERATION");
assertPerformanceWithinLimit(tokenValidationTime, 50, "JWT_TOKEN_VALIDATION");
assertPerformanceWithinLimit(apiAccessTime, 200, "JWT_API_ACCESS");
```

## 3. ヘルパーメソッド解析

### 3.1 JWT トークン生成メソッド群

#### createValidJwtToken メソッド
**SecurityTestBase から継承**
```java
protected String createValidJwtToken(User user) {
    // ユーザー情報を基にJWTトークンを生成
    // 有効期限: テスト用設定（5分）
    // 署名: テスト用秘密鍵
    return jwtTokenProvider.generateToken(user);
}
```

**設計パターン**:
- ファクトリーメソッドパターン
- テスト用の短縮有効期限設定
- 実際のJwtTokenProviderを使用した統合テスト

#### createAdminJwtToken メソッド
**SecurityTestBase から継承**
```java
protected String createAdminJwtToken() {
    return createValidJwtToken(testAdminUser);
}
```

#### createUserJwtToken / createManagerJwtToken メソッド
**SecurityTestBase から継承**
```java
protected String createUserJwtToken() {
    return createValidJwtToken(testNormalUser);
}

protected String createManagerJwtToken() {
    return createValidJwtToken(testManagerUser);
}
```

### 3.2 テストユーティリティメソッド群

#### testUtils.createAuthenticatedGetRequest メソッド
**SecurityTestUtils から提供**
```java
public MockHttpServletRequestBuilder createAuthenticatedGetRequest(String url, String token) {
    return MockMvcRequestBuilders.get(url)
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON);
}
```

**機能**:
- JWT認証ヘッダー付きGETリクエストの作成
- Bearerトークン形式での認証情報設定
- JSON Content-Typeの自動設定

#### testUtils.executeConcurrentRequests メソッド
**SecurityTestUtils から提供**
```java
public void executeConcurrentRequests(Runnable task, int threadCount, int executionCount) {
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount * executionCount);
    
    for (int i = 0; i < threadCount; i++) {
        for (int j = 0; j < executionCount; j++) {
            executor.submit(() -> {
                try {
                    task.run();
                } finally {
                    latch.countDown();
                }
            });
        }
    }
    
    latch.await(30, TimeUnit.SECONDS);
    executor.shutdown();
}
```

**同時実行テストの特徴**:
- 指定されたスレッド数での並行実行
- CountDownLatchによる同期制御
- タイムアウト設定による無限待機防止

#### testUtils.measureResponseTime メソッド
**SecurityTestUtils から提供**
```java
public long measureResponseTime(Runnable task) {
    long startTime = System.currentTimeMillis();
    task.run();
    long endTime = System.currentTimeMillis();
    return endTime - startTime;
}
```

**パフォーマンス測定の特徴**:
- ミリ秒単位での実行時間測定
- 関数型インターフェースによる柔軟な処理指定
- 簡潔なパフォーマンステスト実装

### 3.3 テストデータ管理メソッド群

#### testDataManager.recordTestResult メソッド
**SecurityTestDataManager から提供**
```java
public void recordTestResult(String testClass, String testMethod, String testType, 
                           String result, long executionTime, String details) {
    SecurityTestResult testResult = new SecurityTestResult();
    testResult.setTestClass(testClass);
    testResult.setTestMethod(testMethod);
    testResult.setTestType(testType);
    testResult.setResult(result);
    testResult.setExecutionTime(executionTime);
    testResult.setDetails(details);
    testResult.setExecutedAt(OffsetDateTime.now());
    
    securityTestResultRepository.save(testResult);
}
```

**テスト結果記録の特徴**:
- 全テストケースの実行結果を永続化
- 実行時間の記録によるパフォーマンス追跡
- テストタイプ別の分類・集計機能

## 4. JWT認証テスト特有のテスト戦略

### 4.1 トークン生成・検証の精度管理

#### JWT署名検証の重要性
```java
// 基本的なトークン検証
assertTrue(jwtTokenProvider.validateToken(token), "トークンが有効であること");

// 署名改ざん検出テスト（異常系テストで実装）
String tamperedToken = token.substring(0, token.length() - 10) + "tampered123";
assertFalse(jwtTokenProvider.validateToken(tamperedToken), "改ざんされたトークンは無効であること");
```

#### 有効期限の精密管理
```java
// 有効期限境界値テスト
long remainingTime = jwtTokenProvider.getRemainingValidityTime(token);
assertTrue(remainingTime > 0, "有効期限内のトークンは残り時間が正の値であること");
assertTrue(remainingTime <= 300000, "残り時間がテスト用設定値以下であること"); // 5分以下

// 期限切れ判定の精度確認
assertFalse(jwtTokenProvider.isTokenExpired(token), "有効期限内のトークンは期限切れでないこと");
```

### 4.2 同時認証テストの複雑性

#### マルチスレッド環境での認証テスト
```java
// 複数ユーザーでの同時認証
testUtils.executeConcurrentRequests(() -> {
    // 各スレッドで独立した認証処理を実行
    mockMvc.perform(
        testUtils.createAuthenticatedGetRequest("/api/users/profile", userToken)
    ).andExpected(status().isOk());
}, 3, 5); // 3スレッド × 5回実行

// 同時実行後のトークン状態確認
assertTrue(jwtTokenProvider.validateToken(userToken), "同時実行後もトークンが有効であること");
```

#### トークン間の独立性確認
```java
// 同一ユーザーの複数トークン
String token1 = createValidJwtToken(testUser);
String token2 = createValidJwtToken(testUser);

// トークンの独立性確認
assertNotEquals(token1, token2, "同一ユーザーでも異なるトークンが生成されること");

// 両方のトークンが独立して有効
assertTrue(jwtTokenProvider.validateToken(token1), "1つ目のトークンが有効であること");
assertTrue(jwtTokenProvider.validateToken(token2), "2つ目のトークンが有効であること");
```

### 4.3 情報抽出テストの戦略

#### JWT クレーム情報の検証
```java
// 基本情報の抽出
String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

// 拡張情報の抽出
String extractedLocationType = jwtTokenProvider.getLocationTypeFromToken(token);
Integer extractedDepartmentId = jwtTokenProvider.getDepartmentIdFromToken(token);

// 抽出情報の正確性確認
assertEquals(testUser.getUsername(), extractedUsername, "ユーザー名が正しく抽出されること");
assertEquals(testUser.getId(), extractedUserId, "ユーザーIDが正しく抽出されること");
assertEquals(testUser.getLocationType(), extractedLocationType, "位置タイプが正しく抽出されること");
assertEquals(testUser.getDepartmentId(), extractedDepartmentId, "部署IDが正しく抽出されること");
```

#### null値・不正値の処理確認
```java
// null値処理テスト（異常系テストで実装）
assertThrows(IllegalArgumentException.class, () -> {
    jwtTokenProvider.getUsernameFromToken(null);
}, "null トークンに対して適切な例外が発生すること");

// 不正形式トークンの処理（異常系テストで実装）
assertThrows(JwtException.class, () -> {
    jwtTokenProvider.getUsernameFromToken("invalid.token.format");
}, "不正形式トークンに対して適切な例外が発生すること");
```

## 5. テストケース作成の流れ

### 5.1 JWT認証テスト専用フロー
```
1. セキュリティ要件分析
   ↓
2. テストユーザー・権限設定
   ↓
3. JWT トークン生成・検証
   ↓
4. 認証付きAPIリクエスト実行
   ↓
5. レスポンス・セキュリティヘッダー検証
   ↓
6. 同時実行・パフォーマンステスト
```

### 5.2 詳細手順

#### ステップ1: セキュリティ要件分析
```java
/**
 * テストケース名: 有効なJWTトークンでの認証成功テスト
 * セキュリティ要件:
 * - 有効なJWTトークンでAPIにアクセスする THEN 認証が成功し、リソースにアクセスできること
 * 
 * 検証項目:
 * - トークン生成の正常性
 * - トークン検証の正確性
 * - 認証付きAPIアクセスの成功
 * - セキュリティヘッダーの設定
 * 
 * 入力データ:
 * - 有効なユーザー情報
 * - 適切な権限設定
 * 
 * 期待結果:
 * - 200 OK レスポンス
 * - 適切なセキュリティヘッダー
 * - トークンからの正確な情報抽出
 */
```

#### ステップ2: テストユーザー・権限設定
```java
// レベル1: 基本ユーザー設定
User testUser = testNormalUser; // 一般ユーザー
User adminUser = testAdminUser; // 管理者ユーザー
User managerUser = testManagerUser; // マネージャーユーザー

// レベル2: 権限別トークン生成
String userToken = createValidJwtToken(testUser);
String adminToken = createAdminJwtToken();
String managerToken = createManagerJwtToken();

// レベル3: 権限検証用エンドポイント設定
String publicEndpoint = "/api/users/profile"; // 認証必須
String adminEndpoint = "/api/users"; // 管理者専用
String managerEndpoint = "/api/departments"; // マネージャー以上
```

#### ステップ3: JWT トークン生成・検証
```java
// トークン生成
String token = createValidJwtToken(testUser);

// 基本検証
assertNotNull(token, "トークンが生成されること");
assertTrue(jwtTokenProvider.validateToken(token), "生成されたトークンが有効であること");

// 情報抽出検証
assertEquals(testUser.getUsername(), jwtTokenProvider.getUsernameFromToken(token),
            "トークンから正しいユーザー名が抽出されること");
assertEquals(testUser.getId(), jwtTokenProvider.getUserIdFromToken(token),
            "トークンから正しいユーザーIDが抽出されること");

// 有効期限検証
long remainingTime = jwtTokenProvider.getRemainingValidityTime(token);
assertTrue(remainingTime > 0, "有効期限内のトークンは残り時間が正の値であること");
```

#### ステップ4: 認証付きAPIリクエスト実行
```java
// 認証付きリクエストの作成・実行
MvcResult result = mockMvc.perform(
    testUtils.createAuthenticatedGetRequest("/api/users/profile", token)
)
.andExpect(status().isOk())
.andExpect(content().contentType(MediaType.APPLICATION_JSON))
.andReturn();

// レスポンス内容の検証
String responseContent = result.getResponse().getContentAsString();
assertNotNull(responseContent, "レスポンス内容が存在すること");

// セキュリティヘッダーの検証
assertTrue(testUtils.hasSecurityHeaders(result), "セキュリティヘッダーが設定されていること");
```

#### ステップ5: 段階的検証
```java
// 段階1: null安全性
assertNotNull(result, "レスポンス結果が存在すること");
assertNotNull(token, "JWTトークンが存在すること");

// 段階2: 基本認証成功
assertEquals(200, result.getResponse().getStatus(), "認証が成功すること");

// 段階3: トークン有効性
assertTrue(jwtTokenProvider.validateToken(token), "トークンが有効であること");

// 段階4: 情報抽出精度
assertEquals(testUser.getUsername(), jwtTokenProvider.getUsernameFromToken(token),
            "正確なユーザー名が抽出されること");

// 段階5: セキュリティ設定
assertTrue(testUtils.hasSecurityHeaders(result), "セキュリティヘッダーが適切に設定されること");

// 段階6: テスト結果記録
testDataManager.recordTestResult(
    getClass().getSimpleName(),
    "testValidTokenAuthentication",
    "JWT_AUTH_VALID",
    "PASSED",
    getTestExecutionTime(),
    null
);
```

## 6. テスト作成のコツとベストプラクティス

### 6.1 JWT認証テスト特有の注意点

#### トークン有効期限の管理
```java
// 問題のあるコード（本番用長期有効期限）
String token = jwtTokenProvider.generateToken(user, 86400000); // 24時間

// 改善されたコード（テスト用短期有効期限）
String token = createValidJwtToken(user); // 5分（テスト用設定）

// 有効期限の明示的確認
long remainingTime = jwtTokenProvider.getRemainingValidityTime(token);
assertTrue(remainingTime <= 300000, "テスト用短期有効期限が設定されていること");
```

#### セキュリティヘッダーの検証
```java
// セキュリティヘッダーの包括的確認
assertTrue(testUtils.hasSecurityHeaders(result), "セキュリティヘッダーが設定されていること");

// 個別ヘッダーの詳細確認
MockHttpServletResponse response = result.getResponse();
assertNotNull(response.getHeader("X-Content-Type-Options"), "X-Content-Type-Options ヘッダーが設定されていること");
assertNotNull(response.getHeader("X-Frame-Options"), "X-Frame-Options ヘッダーが設定されていること");
assertNotNull(response.getHeader("X-XSS-Protection"), "X-XSS-Protection ヘッダーが設定されていること");
```

### 6.2 同時実行テストの最適化

#### ExecutorService を使用した並行テスト
```java
// 同時実行テストの実装
testUtils.executeConcurrentRequests(() -> {
    try {
        mockMvc.perform(
            testUtils.createAuthenticatedGetRequest("/api/users/profile", token)
        ).andExpected(status().isOk());
    } catch (Exception e) {
        throw new RuntimeException("同時認証テストでエラーが発生", e);
    }
}, 3, 5); // 3スレッド × 5回実行

// 同時実行後の状態確認
assertTrue(jwtTokenProvider.validateToken(token), "同時実行後もトークンが有効であること");
```

#### スレッドセーフティの確認
```java
// 複数トークンの独立性確認
List<String> tokens = IntStream.range(0, 10)
    .mapToObj(i -> createValidJwtToken(testUser))
    .collect(Collectors.toList());

// 全トークンの独立性確認
for (int i = 0; i < tokens.size(); i++) {
    for (int j = i + 1; j < tokens.size(); j++) {
        assertNotEquals(tokens.get(i), tokens.get(j), 
                       String.format("トークン%dとトークン%dが異なること", i, j));
    }
}

// 全トークンの有効性確認
tokens.forEach(token -> 
    assertTrue(jwtTokenProvider.validateToken(token), "全てのトークンが有効であること"));
```

### 6.3 パフォーマンステストの実装

#### 実行時間測定の精度向上
```java
// ウォームアップ実行（JVM最適化のため）
for (int i = 0; i < 5; i++) {
    createValidJwtToken(testUser);
    jwtTokenProvider.validateToken(token);
}

// 実際の測定
long tokenGenerationTime = testUtils.measureResponseTime(() -> {
    createValidJwtToken(testUser);
});

// 複数回測定による平均値算出
List<Long> measurements = IntStream.range(0, 10)
    .mapToObj(i -> testUtils.measureResponseTime(() -> createValidJwtToken(testUser)))
    .collect(Collectors.toList());

double averageTime = measurements.stream().mapToLong(Long::longValue).average().orElse(0.0);
assertTrue(averageTime < 100, "平均トークン生成時間が100ms以内であること");
```

#### パフォーマンス基準値の設定
```java
// パフォーマンス検証メソッド
private void assertPerformanceWithinLimit(long actualTime, long limitMs, String operation) {
    assertTrue(actualTime <= limitMs, 
              String.format("%s の実行時間 %dms が制限値 %dms を超過", operation, actualTime, limitMs));
    
    // 警告レベルの設定（制限値の80%）
    long warningThreshold = (long) (limitMs * 0.8);
    if (actualTime > warningThreshold) {
        System.out.println(String.format("警告: %s の実行時間 %dms が警告閾値 %dms を超過", 
                                        operation, actualTime, warningThreshold));
    }
}

// 使用例
assertPerformanceWithinLimit(tokenGenerationTime, 100, "JWT_TOKEN_GENERATION");
assertPerformanceWithinLimit(tokenValidationTime, 50, "JWT_TOKEN_VALIDATION");
assertPerformanceWithinLimit(apiAccessTime, 200, "JWT_API_ACCESS");
```

## 7. 拡張テストケースの提案

### 7.1 実用的なテストケース

#### 大量トークン処理テスト
```java
@Test
void testMassiveTokenGeneration_PerformsEfficiently() {
    // 1000個のトークンを生成
    List<String> tokens = IntStream.range(0, 1000)
        .mapToObj(i -> createValidJwtToken(testNormalUser))
        .collect(Collectors.toList());
    
    long startTime = System.currentTimeMillis();
    
    // 全トークンの検証
    tokens.forEach(token -> 
        assertTrue(jwtTokenProvider.validateToken(token), "全てのトークンが有効であること"));
    
    long endTime = System.currentTimeMillis();
    
    // パフォーマンス確認（1000個の検証が5秒以内）
    assertTrue(endTime - startTime < 5000, "大量トークン検証が5秒以内で完了すること");
}
```

#### 権限境界値テスト
```java
@Test
void testRoleBasedAccessControl_BoundaryValues() {
    // 各権限レベルでのアクセステスト
    Map<String, String> roleTokenMap = Map.of(
        "USER", createUserJwtToken(),
        "MANAGER", createManagerJwtToken(),
        "ADMIN", createAdminJwtToken()
    );
    
    // 権限別アクセス可能エンドポイント
    Map<String, List<String>> roleEndpointMap = Map.of(
        "USER", List.of("/api/users/profile"),
        "MANAGER", List.of("/api/users/profile", "/api/departments"),
        "ADMIN", List.of("/api/users/profile", "/api/departments", "/api/users", "/api/admin")
    );
    
    // 権限境界値テスト
    roleTokenMap.forEach((role, token) -> {
        List<String> allowedEndpoints = roleEndpointMap.get(role);
        allowedEndpoints.forEach(endpoint -> {
            try {
                mockMvc.perform(
                    testUtils.createAuthenticatedGetRequest(endpoint, token)
                ).andExpected(status().isOk());
            } catch (Exception e) {
                fail(String.format("%s権限で%sへのアクセスが失敗", role, endpoint));
            }
        });
    });
}
```

### 7.2 異常系テストケース

#### トークン改ざん検出テスト
```java
@Test
void testTokenTampering_DetectedAndRejected() {
    // 有効なトークンを生成
    String validToken = createValidJwtToken(testNormalUser);
    
    // 様々な改ざんパターン
    List<String> tamperedTokens = Arrays.asList(
        validToken.substring(0, validToken.length() - 10) + "tampered123", // 署名改ざん
        "invalid." + validToken.substring(validToken.indexOf('.') + 1), // ヘッダー改ざん
        validToken.substring(0, validToken.indexOf('.')) + ".tampered." + 
            validToken.substring(validToken.lastIndexOf('.') + 1) // ペイロード改ざん
    );
    
    // 改ざんされたトークンは全て無効
    tamperedTokens.forEach(tamperedToken -> {
        assertFalse(jwtTokenProvider.validateToken(tamperedToken), 
                   "改ざんされたトークンは無効であること");
        
        try {
            mockMvc.perform(
                testUtils.createAuthenticatedGetRequest("/api/users/profile", tamperedToken)
            ).andExpected(status().isUnauthorized());
        } catch (Exception e) {
            // 期待される動作
        }
    });
}
```

#### メモリリーク検出テスト
```java
@Test
void testTokenGeneration_NoMemoryLeak() {
    Runtime runtime = Runtime.getRuntime();
    
    // 初期メモリ使用量
    runtime.gc();
    long initialMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // 大量のトークン生成・破棄
    for (int i = 0; i < 10000; i++) {
        String token = createValidJwtToken(testNormalUser);
        jwtTokenProvider.validateToken(token);
        // トークンを意図的にnullにして参照を切る
        token = null;
        
        if (i % 1000 == 0) {
            runtime.gc(); // 定期的なガベージコレクション
        }
    }
    
    // 最終メモリ使用量
    runtime.gc();
    long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // メモリ増加量が許容範囲内（10MB以内）
    long memoryIncrease = finalMemory - initialMemory;
    assertTrue(memoryIncrease < 10 * 1024 * 1024, 
              String.format("メモリ増加量 %d bytes が許容範囲内であること", memoryIncrease));
}
```

## 8. 一般的な問題と解決策

### 8.1 JWT認証テスト特有の問題

#### トークン有効期限の時刻同期問題
**問題**: テスト実行時の時刻ずれによる有効期限判定エラー
```java
// 問題のあるコード
String token = createValidJwtToken(testUser);
Thread.sleep(300000); // 5分待機
assertTrue(jwtTokenProvider.validateToken(token)); // 期限切れで失敗する可能性
```

**解決策**:
```java
// 有効期限を考慮したテスト
String token = createValidJwtToken(testUser);
long remainingTime = jwtTokenProvider.getRemainingValidityTime(token);

// 十分な余裕を持った検証
assertTrue(remainingTime > 10000, "少なくとも10秒の余裕があること");
assertTrue(jwtTokenProvider.validateToken(token), "有効期限内でトークンが有効であること");
```

#### 同時実行時の競合状態
**問題**: 複数スレッドでの同時アクセス時の予期しない動作
```java
// 問題のあるコード
String sharedToken = createValidJwtToken(testUser);
// 複数スレッドで同じトークンを使用 → 競合の可能性
```

**解決策**:
```java
// スレッドローカルなトークン使用
testUtils.executeConcurrentRequests(() -> {
    // 各スレッドで独立したトークンを生成
    String threadLocalToken = createValidJwtToken(testNormalUser);
    
    mockMvc.perform(
        testUtils.createAuthenticatedGetRequest("/api/users/profile", threadLocalToken)
    ).andExpected(status().isOk());
}, 3, 5);
```

### 8.2 MockMvc統合テストの問題

#### セキュリティフィルターチェーンの不完全な初期化
**問題**: セキュリティ設定が正しく適用されない
```java
// 問題のあるコード
@WebMvcTest // セキュリティ設定が不完全
class JwtAuthenticationTest {
```

**解決策**:
```java
// 完全なSpring Boot統合テスト
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("security-test")
class JwtAuthenticationTest {
```

#### テストプロファイル設定の不整合
**問題**: 本番用設定がテストに影響する
```java
// 問題のあるコード
// application.yml の本番用JWT設定が使用される
jwt.expiration=86400000 # 24時間
```

**解決策**:
```java
// application-security-test.yml でテスト用設定を上書き
jwt:
  expiration: 300000 # 5分（テスト用短縮設定）
  secret: test-secret-key-for-security-testing-only
```

## 9. まとめ

### 9.1 JWT認証テストの重要ポイント
1. **トークン管理**: 生成・検証・有効期限の正確な制御
2. **権限制御**: ロールベースアクセス制御の境界値テスト
3. **同時実行**: マルチスレッド環境での認証処理の安全性
4. **情報抽出**: JWTクレームからの正確な情報取得
5. **パフォーマンス**: 認証処理の効率性とスケーラビリティ
6. **セキュリティ**: 改ざん検出・不正アクセス防止の確認

### 9.2 テスト品質向上のチェックリスト
- [ ] 正常系・異常系・境界値テストを網羅
- [ ] テスト用短縮有効期限を設定
- [ ] セキュリティヘッダーの設定を確認
- [ ] 同時実行テストでスレッドセーフティを検証
- [ ] トークン改ざん検出機能を確認
- [ ] パフォーマンス基準値を設定・測定
- [ ] メモリリーク・リソースリークを監視
- [ ] テスト結果の永続化・追跡を実装

### 9.3 他のセキュリティテストとの違い
| 項目 | JWT認証テスト | 一般的なセキュリティテスト |
|------|---------------|---------------------------|
| **対象範囲** | JWT特化（生成・検証・抽出） | 広範囲（認証・認可・暗号化） |
| **テスト複雑度** | 高（同時実行・有効期限管理） | 中程度 |
| **パフォーマンス要件** | 厳格（ms単位での測定） | 一般的 |
| **状態管理** | ステートレス（トークンベース） | ステートフル（セッションベース） |
| **スケーラビリティ** | 重要（分散環境対応） | 中程度 |
| **セキュリティリスク** | トークン改ざん・漏洩 | セッションハイジャック・CSRF |

この手順書に従うことで、JWT認証機能の特性を考慮した包括的で信頼性の高いテストケースを作成できます。特にトークン管理、同時実行処理、パフォーマンス測定の複雑性を適切に扱うことで、実用的なセキュリティテストスイートを構築できます。