# testContentSecurityPolicyHeader テストケース作成手順書

## 概要
本書は、`HttpSecurityHeadersTest` クラスの `testContentSecurityPolicyHeader` テストケース作成における注釈、モック対象、テスト作成の流れとコツを詳細に説明した手順書です。Content Security Policy (CSP) ヘッダーのセキュリティ検証に特化したテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/security/test/headers/HttpSecurityHeadersTest.java`

### 1.2 基本アノテーション

#### @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
**行**: 30
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@TestMethodOrder(OrderAnnotation.class)
class HttpSecurityHeadersTest {
```

**目的**:
- Spring Boot統合テスト環境の構築
- ランダムポートでのWebサーバー起動
- 実際のHTTPリクエスト・レスポンス処理をテスト
- セキュリティフィルターチェーンの完全な動作確認

**CSPテストの特徴**:
- 実際のHTTPヘッダー設定を検証
- Spring Securityの設定が正しく適用されることを確認
- ブラウザセキュリティ機能の動作をシミュレート

### 1.3 テスト用依存性注入

#### @Autowired MockMvc
**行**: 32
```java
@Autowired
private MockMvc mockMvc;
```

**役割**:
- HTTPリクエストのモック実行
- レスポンスヘッダーの取得と検証
- Spring MVCコントローラーとの統合テスト
- セキュリティフィルターチェーンの動作確認

#### @Autowired TestDataManager
**行**: 34
```java
@Autowired
private TestDataManager testDataManager;
```

**役割**:
- テスト結果の記録と管理
- CSPヘッダー設定の詳細ログ保存
- セキュリティテストの実行履歴管理### 1
.4 テストユーザー設定

#### testNormalUser
**行**: 36-37
```java
private User testNormalUser;
```

**設定内容**:
- 認証済みユーザーとしてのテスト実行
- JWTトークン生成の基礎データ
- セキュリティコンテキストでの認証状態確認

## 2. testContentSecurityPolicyHeader テストケース詳細解析

### 2.1 テストメソッド構造
**メソッド**: `testContentSecurityPolicyHeader`
**行**: 63-108

#### テストアノテーション
```java
@Test
@Order(1)
void testContentSecurityPolicyHeader() throws Exception {
```

**設計思想**:
- **@Order(1)**: セキュリティヘッダーテストの最優先実行
- **CSP優先度**: 最も重要なセキュリティヘッダーとして位置付け
- **基盤テスト**: 他のセキュリティテストの前提条件

### 2.2 テスト実行フロー

#### ステップ1: 認証トークン準備
**行**: 65
```java
// Given
String validToken = createValidJwtToken(testNormalUser);
```

**目的**:
- 認証済みユーザーとしてのリクエスト実行
- セキュリティフィルターチェーンの完全な動作確認
- 実際のユーザーアクセスパターンのシミュレート

#### ステップ2: HTTPリクエスト実行
**行**: 67-72
```java
// When & Then
MvcResult result = mockMvc.perform(
        get("/api/users/profile")
                .header("Authorization", "Bearer " + validToken))
        .andExpect(status().isOk())
        .andReturn();
```

**検証ポイント**:
- **エンドポイント**: `/api/users/profile` - 認証が必要なエンドポイント
- **認証ヘッダー**: Bearer トークンによる認証
- **ステータス確認**: 200 OK レスポンスの確認
- **レスポンス取得**: ヘッダー検証のためのMvcResult取得

#### ステップ3: CSPヘッダー存在確認
**行**: 74-75
```java
// CSPヘッダーの存在確認
String cspHeader = result.getResponse().getHeader("Content-Security-Policy");
assertNotNull(cspHeader, "Content-Security-Policy ヘッダーが設定されていること");
```

**基本検証**:
- ヘッダーの存在確認
- null値でないことの保証
- Spring Securityの設定が正しく適用されていることの確認### 2.3 必須
ディレクティブ検証

#### 基本ディレクティブ群
**行**: 77-85
```java
// 必須ディレクティブの確認
assertTrue(cspHeader.contains("default-src"), "default-src ディレクティブが設定されていること");
assertTrue(cspHeader.contains("script-src"), "script-src ディレクティブが設定されていること");
assertTrue(cspHeader.contains("style-src"), "style-src ディレクティブが設定されていること");
assertTrue(cspHeader.contains("img-src"), "img-src ディレクティブが設定されていること");
assertTrue(cspHeader.contains("connect-src"), "connect-src ディレクティブが設定されていること");
assertTrue(cspHeader.contains("font-src"), "font-src ディレクティブが設定されていること");
assertTrue(cspHeader.contains("object-src"), "object-src ディレクティブが設定されていること");
assertTrue(cspHeader.contains("media-src"), "media-src ディレクティブが設定されていること");
assertTrue(cspHeader.contains("frame-src"), "frame-src ディレクティブが設定されていること");
```

**各ディレクティブの役割**:

##### default-src
- **目的**: 他のディレクティブが指定されていない場合のフォールバック
- **設定値**: `'self'`
- **セキュリティ効果**: 同一オリジンからのリソースのみ許可

##### script-src
- **目的**: JavaScriptの実行元を制限
- **設定値**: `'self'`
- **セキュリティ効果**: XSS攻撃の防止、インラインスクリプトの禁止

##### style-src
- **目的**: CSSスタイルシートの読み込み元を制限
- **設定値**: `'self'`
- **セキュリティ効果**: CSS injection攻撃の防止

##### img-src
- **目的**: 画像リソースの読み込み元を制限
- **設定値**: `'self' data:`
- **セキュリティ効果**: 外部画像による情報漏洩防止、data URIは許可

##### connect-src
- **目的**: Ajax、WebSocket、EventSource接続先を制限
- **設定値**: `'self'`
- **セキュリティ効果**: 外部APIへの不正アクセス防止

##### font-src
- **目的**: Webフォントの読み込み元を制限
- **設定値**: `'self'`
- **セキュリティ効果**: フォントファイルを通じた攻撃防止

##### object-src
- **目的**: プラグイン（Flash、Java applet等）の読み込み制限
- **設定値**: `'none'`
- **セキュリティ効果**: プラグインベースの攻撃を完全に防止

##### media-src
- **目的**: 音声・動画メディアの読み込み元を制限
- **設定値**: `'self'`
- **セキュリティ効果**: メディアファイルを通じた攻撃防止

##### frame-src
- **目的**: iframe内で表示可能なコンテンツの制限
- **設定値**: `'none'`
- **セキュリティ効果**: Clickjacking攻撃の防止#
## 2.4 危険な設定の除外確認

#### セキュリティリスク設定の検証
**行**: 87-91
```java
// 危険な設定が含まれていないことを確認
assertFalse(cspHeader.contains("'unsafe-inline'"), "unsafe-inline が許可されていないこと");
assertFalse(cspHeader.contains("'unsafe-eval'"), "unsafe-eval が許可されていないこと");
assertFalse(cspHeader.contains("script-src") && cspHeader.matches(".*script-src[^;]*data:.*"),
        "script-src で data: スキームが許可されていないこと");
```

**危険な設定の詳細**:

##### 'unsafe-inline'
- **リスク**: インラインスクリプト・スタイルの実行を許可
- **攻撃ベクター**: XSS攻撃の主要な侵入経路
- **検証方法**: ヘッダー文字列に含まれていないことを確認
- **セキュリティ効果**: インラインコードによるXSS攻撃を防止

##### 'unsafe-eval'
- **リスク**: eval()、Function()コンストラクタの使用を許可
- **攻撃ベクター**: 動的コード実行による任意コード実行
- **検証方法**: ヘッダー文字列に含まれていないことを確認
- **セキュリティ効果**: 動的コード実行攻撃を防止

##### script-src での data: スキーム
- **リスク**: data URIによるスクリプト実行を許可
- **攻撃ベクター**: Base64エンコードされた悪意あるスクリプト
- **検証方法**: 正規表現による精密な検証
- **検証ロジック**: `.*script-src[^;]*data:.*`
  - `script-src` ディレクティブ内で
  - セミコロン（`;`）までの範囲で
  - `data:` スキームが含まれていないことを確認

### 2.5 推奨設定の確認

#### セキュリティベストプラクティス検証
**行**: 93-97
```java
// 推奨設定の確認
assertTrue(cspHeader.contains("'self'"), "'self' が設定されていること");
assertTrue(cspHeader.contains("object-src 'none'") ||
        cspHeader.contains("object-src: 'none'"),
        "object-src が 'none' に設定されていること");
```

**推奨設定の詳細**:

##### 'self' キーワード
- **目的**: 同一オリジンからのリソースのみ許可
- **セキュリティ効果**: 外部リソースによる攻撃を防止
- **適用範囲**: 全ディレクティブで基本設定として使用

##### object-src 'none'
- **目的**: プラグインコンテンツを完全に禁止
- **セキュリティ効果**: Flash、Java applet等の脆弱性を回避
- **検証パターン**: スペース区切りまたはコロン区切りの両方に対応

### 2.6 テスト結果記録

#### テストデータ管理
**行**: 99-105
```java
// テスト結果の記録
testDataManager.recordTestResult(
        getClass().getSimpleName(),
        "testContentSecurityPolicyHeader",
        "CSP_HEADER_VALIDATION",
        "PASSED",
        getTestExecutionTime(),
        "CSP header properly configured: " + cspHeader);
```

**記録項目**:
- **テストクラス名**: `HttpSecurityHeadersTest`
- **テストメソッド名**: `testContentSecurityPolicyHeader`
- **テストタイプ**: `CSP_HEADER_VALIDATION`
- **実行結果**: `PASSED`
- **実行時間**: パフォーマンス測定
- **詳細情報**: 実際のCSPヘッダー内容#
# 3. SecurityConfig設定解析

### 3.1 CSP設定の実装
**ファイル**: `src/main/java/com/example/companybackend/config/SecurityConfig.java`
**行**: 107-109

```java
.addHeaderWriter(
    new org.springframework.security.web.header.writers.StaticHeadersWriter(
        "Content-Security-Policy",
        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; font-src 'self'; object-src 'none'; media-src 'self'; frame-src 'none';"))
```

### 3.2 設定値の詳細分析

#### 実際のCSPポリシー
```
default-src 'self'; 
script-src 'self'; 
style-src 'self'; 
img-src 'self' data:; 
connect-src 'self'; 
font-src 'self'; 
object-src 'none'; 
media-src 'self'; 
frame-src 'none';
```

**セキュリティレベル評価**:
- **高セキュリティ**: `script-src`, `object-src`, `frame-src`
- **中セキュリティ**: `default-src`, `style-src`, `connect-src`, `font-src`, `media-src`
- **制限付き許可**: `img-src` (data URIを許可)

## 4. テストケース作成の流れ

### 4.1 CSPテスト専用フロー
```
1. セキュリティ要件分析
   ↓
2. 認証済みユーザーでのリクエスト実行
   ↓
3. CSPヘッダーの存在確認
   ↓
4. 必須ディレクティブの検証
   ↓
5. 危険な設定の除外確認
   ↓
6. 推奨設定の適用確認
   ↓
7. テスト結果の記録
```

### 4.2 詳細手順

#### ステップ1: セキュリティ要件分析
```java
/**
 * テストケース名: Content-Security-Policy ヘッダー検証
 * セキュリティ要件:
 * - CSPヘッダーが適切に設定されていること
 * - 必要なディレクティブが全て含まれていること
 * - 危険な設定が含まれていないこと
 * - セキュリティベストプラクティスに準拠していること
 * 
 * 検証対象:
 * - ヘッダーの存在確認
 * - 9つの必須ディレクティブ
 * - 3つの危険な設定の除外
 * - 2つの推奨設定の確認
 */
```

#### ステップ2: 認証済みリクエストの準備
```java
// レベル1: テストユーザーの準備
String validToken = createValidJwtToken(testNormalUser);

// レベル2: 認証ヘッダーの設定
MockHttpServletRequestBuilder request = get("/api/users/profile")
    .header("Authorization", "Bearer " + validToken);

// レベル3: リクエスト実行とレスポンス取得
MvcResult result = mockMvc.perform(request)
    .andExpect(status().isOk())
    .andReturn();
```

#### ステップ3: 段階的検証
```java
// 段階1: 基本存在確認
String cspHeader = result.getResponse().getHeader("Content-Security-Policy");
assertNotNull(cspHeader, "CSPヘッダーが存在すること");

// 段階2: 必須ディレクティブ検証
String[] requiredDirectives = {
    "default-src", "script-src", "style-src", "img-src", "connect-src",
    "font-src", "object-src", "media-src", "frame-src"
};
for (String directive : requiredDirectives) {
    assertTrue(cspHeader.contains(directive), 
        directive + " ディレクティブが設定されていること");
}

// 段階3: 危険設定の除外確認
String[] dangerousSettings = {"'unsafe-inline'", "'unsafe-eval'"};
for (String dangerous : dangerousSettings) {
    assertFalse(cspHeader.contains(dangerous), 
        dangerous + " が含まれていないこと");
}

// 段階4: script-src での data: スキーム確認
assertFalse(cspHeader.matches(".*script-src[^;]*data:.*"),
    "script-src で data: スキームが許可されていないこと");
```## 5. CSP
テスト特有の注意点とベストプラクティス

### 5.1 ヘッダー検証の精密性

#### 文字列検証の限界と対策
```java
// 問題のあるコード（単純な文字列検索）
assertTrue(cspHeader.contains("script-src")); // 不十分

// 改善されたコード（ディレクティブ値の検証）
assertTrue(cspHeader.matches(".*script-src\\s+'self'.*"), 
    "script-src が 'self' に設定されていること");

// さらに改善されたコード（CSPパーサーの使用）
CSPParser parser = new CSPParser(cspHeader);
assertEquals("'self'", parser.getDirectiveValue("script-src"));
```

#### 正規表現による精密検証
```java
// script-src での data: スキーム検証の詳細
Pattern scriptSrcDataPattern = Pattern.compile("script-src[^;]*data:");
assertFalse(scriptSrcDataPattern.matcher(cspHeader).find(),
    "script-src で data: スキームが許可されていないこと");

// object-src の 'none' 設定確認
Pattern objectSrcNonePattern = Pattern.compile("object-src\\s+'none'");
assertTrue(objectSrcNonePattern.matcher(cspHeader).find(),
    "object-src が 'none' に設定されていること");
```

### 5.2 ブラウザ互換性の考慮

#### 異なるCSP記法への対応
```java
// 複数の記法パターンに対応した検証
private void assertDirectiveValue(String cspHeader, String directive, String expectedValue) {
    // パターン1: スペース区切り
    boolean spaceFormat = cspHeader.contains(directive + " " + expectedValue);
    // パターン2: コロン区切り（古い記法）
    boolean colonFormat = cspHeader.contains(directive + ": " + expectedValue);
    
    assertTrue(spaceFormat || colonFormat,
        String.format("%s が %s に設定されていること", directive, expectedValue));
}
```

### 5.3 セキュリティレベルの段階的検証

#### レベル別検証戦略
```java
// レベル1: 基本セキュリティ（必須）
private void validateBasicSecurity(String cspHeader) {
    assertNotNull(cspHeader, "CSPヘッダーが存在すること");
    assertTrue(cspHeader.contains("default-src"), "default-src が設定されていること");
}

// レベル2: 標準セキュリティ（推奨）
private void validateStandardSecurity(String cspHeader) {
    validateBasicSecurity(cspHeader);
    assertFalse(cspHeader.contains("'unsafe-inline'"), "unsafe-inline が禁止されていること");
    assertFalse(cspHeader.contains("'unsafe-eval'"), "unsafe-eval が禁止されていること");
}

// レベル3: 高度セキュリティ（最適）
private void validateAdvancedSecurity(String cspHeader) {
    validateStandardSecurity(cspHeader);
    assertTrue(cspHeader.contains("object-src 'none'"), "object-src が完全に禁止されていること");
    assertTrue(cspHeader.contains("frame-src 'none'"), "frame-src が完全に禁止されていること");
}
```

## 6. 拡張テストケースの提案

### 6.1 実用的なテストケース

#### CSPポリシー違反のシミュレーション
```java
@Test
void testCSPViolationReporting() throws Exception {
    // CSP違反レポートエンドポイントのテスト
    String violationReport = """
        {
            "csp-report": {
                "document-uri": "https://example.com/page",
                "violated-directive": "script-src 'self'",
                "blocked-uri": "https://malicious.com/script.js"
            }
        }
        """;
    
    mockMvc.perform(post("/api/csp-report")
            .contentType(MediaType.APPLICATION_JSON)
            .content(violationReport))
            .andExpect(status().isOk());
    
    // 違反ログの記録確認
    verify(securityEventLogger).logCSPViolation(any(CSPViolationReport.class));
}
```

#### 動的CSP設定のテスト
```java
@Test
void testDynamicCSPConfiguration() throws Exception {
    // 環境別CSP設定の確認
    String devToken = createValidJwtToken(testNormalUser);
    
    MvcResult result = mockMvc.perform(
            get("/api/users/profile")
                    .header("Authorization", "Bearer " + devToken))
            .andExpect(status().isOk())
            .andReturn();
    
    String cspHeader = result.getResponse().getHeader("Content-Security-Policy");
    
    // 開発環境では 'unsafe-inline' が許可される場合のテスト
    if (isDevEnvironment()) {
        assertTrue(cspHeader.contains("'unsafe-inline'"), 
            "開発環境では unsafe-inline が許可されること");
    } else {
        assertFalse(cspHeader.contains("'unsafe-inline'"), 
            "本番環境では unsafe-inline が禁止されること");
    }
}
```### 6.
2 異常系テストケース

#### 不正なCSP設定の検出
```java
@Test
void testInvalidCSPConfiguration() throws Exception {
    // 設定ミスの検出テスト
    String validToken = createValidJwtToken(testNormalUser);
    
    MvcResult result = mockMvc.perform(
            get("/api/users/profile")
                    .header("Authorization", "Bearer " + validToken))
            .andExpect(status().isOk())
            .andReturn();
    
    String cspHeader = result.getResponse().getHeader("Content-Security-Policy");
    
    // 一般的な設定ミスの検出
    assertFalse(cspHeader.contains("*"), "ワイルドカード（*）が使用されていないこと");
    assertFalse(cspHeader.contains("http:"), "非セキュアなHTTPスキームが許可されていないこと");
    assertFalse(cspHeader.contains("'unsafe-hashes'"), "unsafe-hashes が使用されていないこと");
}
```

#### CSPヘッダーの重複確認
```java
@Test
void testCSPHeaderDuplication() throws Exception {
    String validToken = createValidJwtToken(testNormalUser);
    
    MvcResult result = mockMvc.perform(
            get("/api/users/profile")
                    .header("Authorization", "Bearer " + validToken))
            .andExpect(status().isOk())
            .andReturn();
    
    // 重複ヘッダーの確認
    Collection<String> cspHeaders = result.getResponse().getHeaders("Content-Security-Policy");
    assertEquals(1, cspHeaders.size(), "CSPヘッダーが重複していないこと");
}
```

## 7. 一般的な問題と解決策

### 7.1 CSPテスト特有の問題

#### 文字列検証の不正確性
**問題**: 単純な文字列検索による誤検出
```java
// 問題のあるコード
assertTrue(cspHeader.contains("script-src")); // "script-src-elem" も一致してしまう
```

**解決策**:
```java
// 改善されたコード（境界を考慮）
assertTrue(cspHeader.matches(".*\\bscript-src\\b.*"), 
    "script-src ディレクティブが正確に設定されていること");
```

#### ディレクティブ値の検証不足
**問題**: ディレクティブの存在のみ確認し、値を検証しない
```java
// 問題のあるコード
assertTrue(cspHeader.contains("object-src")); // 値が 'none' かどうか未確認
```

**解決策**:
```java
// 改善されたコード（値も含めて検証）
assertTrue(cspHeader.contains("object-src 'none'") || 
           cspHeader.contains("object-src: 'none'"),
    "object-src が 'none' に設定されていること");
```

### 7.2 環境依存の問題

#### 環境別設定の考慮不足
**問題**: 開発・テスト・本番環境での設定差異を考慮しない
```java
// 問題のあるコード（環境を考慮しない）
assertFalse(cspHeader.contains("'unsafe-inline'")); // 開発環境では必要な場合がある
```

**解決策**:
```java
// 改善されたコード（環境を考慮）
if (isProductionEnvironment()) {
    assertFalse(cspHeader.contains("'unsafe-inline'"), 
        "本番環境では unsafe-inline が禁止されていること");
} else {
    // 開発環境では警告のみ
    if (cspHeader.contains("'unsafe-inline'")) {
        logger.warn("開発環境で unsafe-inline が使用されています");
    }
}
```

## 8. パフォーマンス考慮事項

### 8.1 CSPヘッダーサイズの最適化

#### ヘッダーサイズの監視
```java
@Test
void testCSPHeaderSize() throws Exception {
    String validToken = createValidJwtToken(testNormalUser);
    
    MvcResult result = mockMvc.perform(
            get("/api/users/profile")
                    .header("Authorization", "Bearer " + validToken))
            .andExpect(status().isOk())
            .andReturn();
    
    String cspHeader = result.getResponse().getHeader("Content-Security-Policy");
    
    // ヘッダーサイズの確認（一般的に8KB以下が推奨）
    assertTrue(cspHeader.length() < 8192, 
        "CSPヘッダーサイズが8KB以下であること（現在: " + cspHeader.length() + " bytes）");
}
```

### 8.2 CSP処理のパフォーマンス測定

#### レスポンス時間の監視
```java
@Test
void testCSPPerformanceImpact() throws Exception {
    String validToken = createValidJwtToken(testNormalUser);
    
    long startTime = System.currentTimeMillis();
    
    mockMvc.perform(
            get("/api/users/profile")
                    .header("Authorization", "Bearer " + validToken))
            .andExpect(status().isOk());
    
    long endTime = System.currentTimeMillis();
    long responseTime = endTime - startTime;
    
    // CSP処理によるレスポンス時間への影響を確認
    assertTrue(responseTime < 1000, 
        "CSP処理を含むレスポンス時間が1秒以下であること（現在: " + responseTime + "ms）");
}
```## 
9. セキュリティテストの統合戦略

### 9.1 他のセキュリティヘッダーとの連携

#### 包括的セキュリティヘッダーテスト
```java
@Test
void testComprehensiveSecurityHeaders() throws Exception {
    String validToken = createValidJwtToken(testNormalUser);
    
    MvcResult result = mockMvc.perform(
            get("/api/users/profile")
                    .header("Authorization", "Bearer " + validToken)
                    .secure(true)) // HTTPS環境をシミュレート
            .andExpect(status().isOk())
            .andReturn();
    
    // CSPと他のセキュリティヘッダーの連携確認
    String cspHeader = result.getResponse().getHeader("Content-Security-Policy");
    String xFrameOptions = result.getResponse().getHeader("X-Frame-Options");
    String hstsHeader = result.getResponse().getHeader("Strict-Transport-Security");
    
    // CSPとX-Frame-Optionsの整合性確認
    if (cspHeader.contains("frame-src 'none'")) {
        assertNotNull(xFrameOptions, "frame-src 'none' の場合、X-Frame-Optionsも設定されていること");
    }
    
    // CSPとHSTSの連携確認
    assertNotNull(hstsHeader, "セキュアな通信のためHSTSヘッダーが設定されていること");
}
```

### 9.2 セキュリティスコアリング

#### CSP設定の点数化
```java
private int calculateCSPSecurityScore(String cspHeader) {
    int score = 0;
    
    // 基本ディレクティブ（各5点）
    String[] basicDirectives = {"default-src", "script-src", "style-src", "img-src"};
    for (String directive : basicDirectives) {
        if (cspHeader.contains(directive)) score += 5;
    }
    
    // 高度なディレクティブ（各10点）
    String[] advancedDirectives = {"object-src 'none'", "frame-src 'none'"};
    for (String directive : advancedDirectives) {
        if (cspHeader.contains(directive)) score += 10;
    }
    
    // 危険な設定の減点（各-20点）
    String[] dangerousSettings = {"'unsafe-inline'", "'unsafe-eval'"};
    for (String dangerous : dangerousSettings) {
        if (cspHeader.contains(dangerous)) score -= 20;
    }
    
    return Math.max(0, score); // 最低0点
}

@Test
void testCSPSecurityScore() throws Exception {
    String validToken = createValidJwtToken(testNormalUser);
    
    MvcResult result = mockMvc.perform(
            get("/api/users/profile")
                    .header("Authorization", "Bearer " + validToken))
            .andExpect(status().isOk())
            .andReturn();
    
    String cspHeader = result.getResponse().getHeader("Content-Security-Policy");
    int securityScore = calculateCSPSecurityScore(cspHeader);
    
    // 最低セキュリティスコアの確認
    assertTrue(securityScore >= 60, 
        "CSPセキュリティスコアが60点以上であること（現在: " + securityScore + "点）");
}
```

## 10. まとめ

### 10.1 CSPテストの重要ポイント
1. **ヘッダー存在確認**: CSPヘッダーが適切に設定されていること
2. **必須ディレクティブ**: 9つの基本ディレクティブの完全性
3. **危険設定の除外**: unsafe-inline、unsafe-eval、不適切なdata:スキームの禁止
4. **推奨設定の適用**: 'self'キーワードとobject-src 'none'の確認
5. **精密な検証**: 正規表現による正確なディレクティブ値の確認

### 10.2 テスト品質向上のチェックリスト
- [ ] CSPヘッダーの存在確認
- [ ] 9つの必須ディレクティブの検証
- [ ] 3つの危険な設定の除外確認
- [ ] 推奨設定の適用確認
- [ ] 正規表現による精密な検証
- [ ] 環境別設定の考慮
- [ ] パフォーマンスへの影響測定
- [ ] 他のセキュリティヘッダーとの連携確認
- [ ] セキュリティスコアの定量評価

### 10.3 他のセキュリティテストとの違い
| 項目 | CSPヘッダーテスト | 一般的なセキュリティテスト |
|------|-------------------|---------------------------|
| **検証対象** | HTTPレスポンスヘッダー | 認証・認可・入力検証 |
| **検証方法** | 文字列・正規表現検証 | 機能的動作確認 |
| **セキュリティ効果** | ブラウザレベル保護 | アプリケーションレベル保護 |
| **設定複雑度** | 高（多数のディレクティブ） | 中程度 |
| **ブラウザ依存** | 高（CSP対応状況） | 低 |
| **パフォーマンス影響** | 低（ヘッダー追加のみ） | 中程度 |

### 10.4 実装時の注意事項
1. **段階的実装**: 基本ディレクティブから開始し、徐々に厳格化
2. **環境別設定**: 開発・テスト・本番環境での適切な設定差異
3. **継続的監視**: CSP違反レポートの監視と分析
4. **定期的見直し**: セキュリティ要件の変化に応じた設定更新
5. **チーム教育**: CSPの仕組みと重要性の共有

この手順書に従うことで、Content Security Policyヘッダーの包括的で信頼性の高いテストケースを作成できます。特に文字列検証の精密性、セキュリティ設定の段階的確認、他のセキュリティ機能との連携を適切に扱うことで、実用的なセキュリティテストスイートを構築できます。