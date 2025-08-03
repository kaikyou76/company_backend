# SystemLogRepositoryTest テストケース作成手順書

## 概要
本書は、`SystemLogRepositoryTest` のテストケース作成における注釈、データベース接続、テスト作成の流れとコツを詳細に説明した手順書です。システムログデータアクセス層の特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/repository/SystemLogRepositoryTest.java`

### 1.2 基本アノテーション

#### @SpringBootTest
**行**: 25
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SystemLogRepositoryTest {
```

**目的**:
- Spring Boot統合テスト環境の構築
- 実際のデータベース接続とトランザクション管理
- テストプロファイルによる設定分離

**システムログテストの特徴**:
- 実際のPostgreSQLデータベース（comsys_test_dump.sql）を使用
- JPA/Hibernateの動作確認
- ネイティブクエリとJPQLクエリの両方をテスト
- 複雑な統計クエリとセキュリティ関連クエリの検証
- JSON形式の詳細情報（details）フィールドの処理確認

#### @Transactional
**目的**:
- 各テストメソッド実行後の自動ロールバック
- テスト間のデータ独立性保証
- データベース状態のクリーンアップ

#### @ActiveProfiles("test")
**目的**:
- テスト専用データベース設定の適用
- `application-test.properties` の設定読み込み
- 本番環境との分離#
## 1.3 依存性注入

#### @Autowired SystemLogRepository
**行**: 29-30
```java
@Autowired
private SystemLogRepository systemLogRepository;
```

**役割**:
- Spring Data JPAリポジトリの自動注入
- 実際のデータベース操作の実行
- システムログ専用クエリメソッドのテスト対象

**テスト対象メソッド**:
```java
// 基本検索メソッド
findByUserId(Integer userId)
findByAction(String action)
findByIpAddress(String ipAddress)
findByUserIdAndAction(Integer userId, String action)

// 日時検索メソッド
findByCreatedAtBetween(OffsetDateTime startDate, OffsetDateTime endDate)
findTodayLogs()
findLatestLogs(int limit)
findLatestLogsByUser(Integer userId, int limit)

// 統計情報メソッド
getActionStatistics(OffsetDateTime startDate, OffsetDateTime endDate)
getUserActivityStatistics(OffsetDateTime startDate, OffsetDateTime endDate)
getIpAccessStatistics(OffsetDateTime startDate, OffsetDateTime endDate)
getHourlyActivityStatistics(OffsetDateTime startDate, OffsetDateTime endDate)

// セキュリティ関連メソッド
findSuspiciousActivity(int hours, long threshold)
findFailedLoginAttempts(int hours)

// 検索・フィルタリングメソッド
findByFilters(String action, String status, OffsetDateTime startDate, OffsetDateTime endDate, Pageable pageable)
searchByKeyword(String keyword, Pageable pageable)

// 集計統計メソッド
countByActionGrouped()
countByStatusGrouped()
countByUserGrouped()
countByDateGrouped()

// バッチ処理・メンテナンスメソッド
countByCreatedAtBetween(OffsetDateTime startDate, OffsetDateTime endDate)
findLogsOlderThan(OffsetDateTime cutoffDate)
countByCreatedAtBefore(OffsetDateTime cutoffDate)
findForBatchProcessing(int offset, int limit)
```

### 1.4 テスト用定数定義

#### 実データベース対応定数
**行**: 32-40
```java
// テスト用定数（comsys_test_dump.sqlの実際のデータを使用）
private static final Integer TEST_USER_ID_1 = 1; // ceo@company.com
private static final Integer TEST_USER_ID_2 = 2; // director@company.com
private static final String TEST_ACTION_LOGIN = "LOGIN";
private static final String TEST_ACTION_LOGOUT = "LOGOUT";
private static final String TEST_STATUS_SUCCESS = "success";
private static final String TEST_STATUS_ERROR = "error";
private static final String TEST_IP_ADDRESS = "192.168.1.200";
private static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
```

**設計思想**:
- **実データ活用**: comsys_test_dump.sqlの実際のユーザーIDを使用
- **ログアクション**: LOGIN/LOGOUTなど実際のシステムアクションを模擬
- **ステータス管理**: success/errorの二分化でログの成功/失敗を表現
- **ネットワーク情報**: 実際のIPアドレスとUser-Agentを使用
- **外部キー制約**: 実在するユーザーIDのみ使用して制約違反を回避#
## 1.5 テストデータ準備

#### @BeforeEach セットアップ
**行**: 47-71
```java
@BeforeEach
void setUp() {
    // 基準時刻設定（日本時間）
    baseTime = OffsetDateTime.of(2025, 2, 1, 10, 0, 0, 0, ZoneOffset.ofHours(9));

    // テストデータの準備
    loginLog1 = createSystemLog(null, TEST_USER_ID_1, TEST_ACTION_LOGIN, TEST_STATUS_SUCCESS,
            TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"method\": \"password\"}", baseTime);

    loginLog2 = createSystemLog(null, TEST_USER_ID_2, TEST_ACTION_LOGIN, TEST_STATUS_SUCCESS,
            "192.168.1.201", TEST_USER_AGENT, "{\"method\": \"password\"}", baseTime.plusMinutes(5));

    logoutLog1 = createSystemLog(null, TEST_USER_ID_1, TEST_ACTION_LOGOUT, TEST_STATUS_SUCCESS,
            TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"duration\": \"30min\"}", baseTime.plusMinutes(30));

    errorLog1 = createSystemLog(null, TEST_USER_ID_1, "ACCESS_DENIED", TEST_STATUS_ERROR,
            TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"reason\": \"insufficient_permissions\"}", 
            baseTime.plusMinutes(10));

    // データベースに保存
    loginLog1 = systemLogRepository.save(loginLog1);
    loginLog2 = systemLogRepository.save(loginLog2);
    logoutLog1 = systemLogRepository.save(logoutLog1);
    errorLog1 = systemLogRepository.save(errorLog1);
}
```

**重要ポイント**:
- **時刻統一**: 日本時間（JST）での基準時刻設定
- **多様なログタイプ**: ログイン、ログアウト、エラーログの多様性
- **JSON詳細情報**: detailsフィールドでのJSON形式データ格納
- **時系列データ**: 異なる時刻でのログ作成による時系列テスト対応
- **保存後ID取得**: save()後にIDを取得して後続テストで使用

## 2. 主要テストケース解析

### 2.1 基本検索テスト群

#### テストケース1: 正常なユーザーID検索
**メソッド**: `testFindByUserId_WithExistingUser_ShouldReturnLogs`

##### 既存データ考慮の検証
```java
// When
List<SystemLog> result = systemLogRepository.findByUserId(TEST_USER_ID_1);

// Then
assertNotNull(result);
assertTrue(result.size() >= 3); // 既存データ + テストデータ
assertTrue(result.stream().allMatch(log -> log.getUserId().equals(TEST_USER_ID_1)));
```

**重要ポイント**:
- **既存データ考慮**: system_logsテーブルには既存データが存在するため、`>=` 比較を使用
- **データ整合性**: 全レコードが指定ユーザーIDと一致することを確認
- **null安全性**: 結果がnullでないことを最初に確認

#### テストケース2: 存在しないユーザーの処理
**メソッド**: `testFindByUserId_WithNonExistentUser_ShouldReturnEmptyList`

##### 境界値テスト
```java
// When
List<SystemLog> result = systemLogRepository.findByUserId(999);

// Then
assertNotNull(result);
assertTrue(result.isEmpty());
```

**設計思想**:
- **存在しないID**: 999は実データベースに存在しないユーザーID
- **空リスト確認**: nullではなく空のListが返されることを確認
- **例外なし**: 存在しないIDでも例外が発生しないことを確認#### テスト
ケース3: アクション別フィルタリング
**メソッド**: `testFindByAction_WithValidAction_ShouldReturnFilteredLogs`

##### アクション種別検索テスト
```java
// When
List<SystemLog> loginResults = systemLogRepository.findByAction(TEST_ACTION_LOGIN);
List<SystemLog> logoutResults = systemLogRepository.findByAction(TEST_ACTION_LOGOUT);

// Then
assertNotNull(loginResults);
assertTrue(loginResults.size() >= 2); // 既存データ + テストデータ
assertTrue(loginResults.stream().allMatch(log -> TEST_ACTION_LOGIN.equals(log.getAction())));

assertNotNull(logoutResults);
assertTrue(logoutResults.size() >= 1); // テストデータ
assertTrue(logoutResults.stream().allMatch(log -> TEST_ACTION_LOGOUT.equals(log.getAction())));
```

**検証ポイント**:
- **フィルタリング精度**: 指定したアクションのレコードのみが返されることを確認
- **データ分離**: ログインとログアウトのレコードが適切に分離されることを確認
- **Stream API活用**: 全要素が条件を満たすことを効率的に検証

#### テストケース4: IPアドレス別検索
**メソッド**: `testFindByIpAddress_WithValidIp_ShouldReturnLogs`

##### ネットワーク情報による検索
```java
// When
List<SystemLog> result = systemLogRepository.findByIpAddress(TEST_IP_ADDRESS);

// Then
assertNotNull(result);
assertTrue(result.size() >= 3); // loginLog1 + logoutLog1 + errorLog1
assertTrue(result.stream().allMatch(log -> TEST_IP_ADDRESS.equals(log.getIpAddress())));
```

**IPアドレス検索の特徴**:
- **ネットワーク監視**: 特定IPからのアクセス履歴追跡
- **セキュリティ分析**: 不正アクセス検知のための基盤機能
- **アクセス統計**: IPアドレス別のアクセス頻度分析

#### テストケース5: 複合条件検索
**メソッド**: `testFindByUserIdAndAction_WithValidData_ShouldReturnFilteredLogs`

##### ユーザーとアクションの複合検索
```java
// When
List<SystemLog> result = systemLogRepository.findByUserIdAndAction(TEST_USER_ID_1, TEST_ACTION_LOGIN);

// Then
assertNotNull(result);
assertTrue(result.size() >= 1); // loginLog1 + 既存データ
assertTrue(result.stream().allMatch(log -> 
    log.getUserId().equals(TEST_USER_ID_1) && TEST_ACTION_LOGIN.equals(log.getAction())));
```

**複合条件の検証**:
- **AND条件**: ユーザーIDとアクションの両方を満たすレコードのみ取得
- **条件精度**: 複数条件での正確なフィルタリング確認
- **全件検証**: Stream APIで全レコードが条件を満たすことを確認

### 2.2 日時検索テスト群

#### テストケース6: 日時範囲検索
**メソッド**: `testFindByCreatedAtBetween_WithValidRange_ShouldReturnLogs`

##### 時間範囲クエリテスト
```java
// Given
OffsetDateTime startDate = baseTime.minusMinutes(10);
OffsetDateTime endDate = baseTime.plusHours(1);

// When
List<SystemLog> result = systemLogRepository.findByCreatedAtBetween(startDate, endDate);

// Then
assertNotNull(result);
assertTrue(result.size() >= 4); // 全テストデータ
assertTrue(result.stream().allMatch(log -> 
    !log.getCreatedAt().isBefore(startDate) && !log.getCreatedAt().isAfter(endDate)));
```

**範囲検索の重要性**:
- **包含関係**: 開始時刻と終了時刻の両方を含む範囲検索
- **境界値処理**: 範囲の境界値での動作確認
- **パフォーマンス**: インデックスを活用した効率的な範囲検索####
 テストケース7: 今日のログ取得
**メソッド**: `testFindTodayLogs_ShouldReturnTodayLogs`

##### 動的日付処理テスト
```java
// Given - 今日の日付でログを作成
OffsetDateTime today = OffsetDateTime.now(ZoneOffset.ofHours(9));
SystemLog todayLog = createSystemLog(null, TEST_USER_ID_1, "TODAY_ACTION", TEST_STATUS_SUCCESS,
        TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"test\": \"today\"}", today);
systemLogRepository.save(todayLog);

// When
List<SystemLog> result = systemLogRepository.findTodayLogs();

// Then
assertNotNull(result);
assertFalse(result.isEmpty());
// 今日作成されたログが含まれていることを確認
assertTrue(result.stream().anyMatch(log -> "TODAY_ACTION".equals(log.getAction())));
```

**動的日付の課題**:
- **テスト実行日依存**: テスト実行日によって結果が変わる
- **タイムゾーン統一**: 日本時間（JST）での統一処理
- **CURRENT_DATE関数**: データベースの現在日付関数の動作確認

#### テストケース8: 最新ログ取得（件数制限）
**メソッド**: `testFindLatestLogs_WithLimit_ShouldReturnLimitedResults`

##### 時刻順ソートテスト
```java
// When
List<SystemLog> result = systemLogRepository.findLatestLogs(5);

// Then
assertNotNull(result);
assertTrue(result.size() <= 5);
// 最新順にソートされていることを確認
if (result.size() > 1) {
    for (int i = 0; i < result.size() - 1; i++) {
        assertTrue(result.get(i).getCreatedAt().isAfter(result.get(i + 1).getCreatedAt()) ||
                  result.get(i).getCreatedAt().equals(result.get(i + 1).getCreatedAt()));
    }
}
```

**ソート処理の検証**:
- **降順ソート**: 最新の記録が最初に来ることを確認
- **件数制限**: LIMIT句による結果件数制限の確認
- **時刻比較**: OffsetDateTimeの適切な比較処理

#### テストケース9: ユーザー別最新ログ取得
**メソッド**: `testFindLatestLogsByUser_WithValidUser_ShouldReturnUserLogs`

##### ユーザー別時刻順ソートテスト
```java
// When
List<SystemLog> result = systemLogRepository.findLatestLogsByUser(TEST_USER_ID_1, 3);

// Then
assertNotNull(result);
assertTrue(result.size() <= 3);
assertTrue(result.stream().allMatch(log -> log.getUserId().equals(TEST_USER_ID_1)));
// 最新順にソートされていることを確認
if (result.size() > 1) {
    for (int i = 0; i < result.size() - 1; i++) {
        assertTrue(result.get(i).getCreatedAt().isAfter(result.get(i + 1).getCreatedAt()) ||
                  result.get(i).getCreatedAt().equals(result.get(i + 1).getCreatedAt()));
    }
}
```

**複合条件ソートの特徴**:
- **ユーザーフィルタ**: 特定ユーザーのログのみ取得
- **時刻順ソート**: ユーザー内での最新順ソート
- **件数制限**: ユーザー別の最新N件取得

### 2.3 統計情報テスト群

#### テストケース10: アクション別統計
**メソッド**: `testGetActionStatistics_WithValidRange_ShouldReturnStatistics`

##### 集計クエリテスト
```java
// Given
OffsetDateTime startDate = baseTime.minusMinutes(10);
OffsetDateTime endDate = baseTime.plusHours(1);

// When
List<Map<String, Object>> result = systemLogRepository.getActionStatistics(startDate, endDate);

// Then
assertNotNull(result);
assertFalse(result.isEmpty());
// 各統計レコードが必要なキーを持つことを確認
result.forEach(stat -> {
    assertTrue(stat.containsKey("action"));
    assertTrue(stat.containsKey("count"));
    assertNotNull(stat.get("action"));
    assertTrue(stat.get("count") instanceof Number);
});
```

**統計クエリの特徴**:
- **GROUP BY集計**: アクション別のカウント集計
- **Map型戻り値**: 動的なキー・値ペアでの結果取得
- **期間フィルタ**: 指定期間内のデータのみ集計#### テストケース1
1: ユーザー別アクティビティ統計
**メソッド**: `testGetUserActivityStatistics_WithValidRange_ShouldReturnStatistics`

##### 複合統計クエリテスト
```java
// Given
OffsetDateTime startDate = baseTime.minusMinutes(10);
OffsetDateTime endDate = baseTime.plusHours(1);

// When
List<Map<String, Object>> result = systemLogRepository.getUserActivityStatistics(startDate, endDate);

// Then
assertNotNull(result);
assertFalse(result.isEmpty());
// 各統計レコードが必要なキーを持つことを確認
result.forEach(stat -> {
    assertTrue(stat.containsKey("userId"));
    assertTrue(stat.containsKey("actionCount"));
    assertTrue(stat.containsKey("uniqueActions"));
    assertTrue(stat.get("actionCount") instanceof Number);
    assertTrue(stat.get("uniqueActions") instanceof Number);
});
```

**複合統計の特徴**:
- **複数集計**: COUNT()とCOUNT(DISTINCT)の組み合わせ
- **ユーザー別分析**: ユーザーごとのアクティビティ分析
- **多次元統計**: 総アクション数とユニークアクション数の両方を取得

#### テストケース12: IPアクセス統計
**メソッド**: `testGetIpAccessStatistics_WithValidRange_ShouldReturnStatistics`

##### ネットワーク統計テスト
```java
// Given
OffsetDateTime startDate = baseTime.minusMinutes(10);
OffsetDateTime endDate = baseTime.plusHours(1);

// When
List<Map<String, Object>> result = systemLogRepository.getIpAccessStatistics(startDate, endDate);

// Then
assertNotNull(result);
assertFalse(result.isEmpty());
// 各統計レコードが必要なキーを持つことを確認
result.forEach(stat -> {
    assertTrue(stat.containsKey("ipAddress"));
    assertTrue(stat.containsKey("accessCount"));
    assertTrue(stat.containsKey("uniqueUsers"));
    assertNotNull(stat.get("ipAddress"));
    assertTrue(stat.get("accessCount") instanceof Number);
    assertTrue(stat.get("uniqueUsers") instanceof Number);
});
```

**ネットワーク統計の重要性**:
- **セキュリティ分析**: IPアドレス別のアクセス頻度分析
- **異常検知**: 特定IPからの大量アクセス検知
- **ユーザー分散**: IP別のユニークユーザー数分析

#### テストケース13: 時間別アクティビティ統計
**メソッド**: `testGetHourlyActivityStatistics_WithValidRange_ShouldReturnStatistics`

##### 時系列統計テスト
```java
// Given
OffsetDateTime startDate = baseTime.minusMinutes(10);
OffsetDateTime endDate = baseTime.plusHours(1);

// When
List<Map<String, Object>> result = systemLogRepository.getHourlyActivityStatistics(startDate, endDate);

// Then
assertNotNull(result);
assertFalse(result.isEmpty());
// 各統計レコードが必要なキーを持つことを確認
result.forEach(stat -> {
    assertTrue(stat.containsKey("hour"));
    assertTrue(stat.containsKey("activityCount"));
    assertTrue(stat.get("hour") instanceof Number);
    assertTrue(stat.get("activityCount") instanceof Number);
});
```

**時系列統計の特徴**:
- **EXTRACT関数**: PostgreSQLのEXTRACT(HOUR)関数による時間抽出
- **時間別分析**: 24時間での活動パターン分析
- **負荷分散**: システム負荷の時間別分布確認

### 2.4 セキュリティ関連テスト群

#### テストケース14: 疑わしいアクティビティ検索
**メソッド**: `testFindSuspiciousActivity_WithThreshold_ShouldReturnSuspiciousIps`

##### セキュリティ監視テスト
```java
// Given - 同一IPから複数のログを作成
for (int i = 0; i < 5; i++) {
    SystemLog suspiciousLog = createSystemLog(null, TEST_USER_ID_1, "SUSPICIOUS_ACTION", TEST_STATUS_SUCCESS,
            "192.168.1.100", TEST_USER_AGENT, "{\"attempt\": " + i + "}", 
            OffsetDateTime.now(ZoneOffset.ofHours(9)).minusMinutes(i));
    systemLogRepository.save(suspiciousLog);
}

// When
List<Object[]> result = systemLogRepository.findSuspiciousActivity(1, 3);

// Then
assertNotNull(result);
// 結果があれば、各レコードが2つの要素（IP、カウント）を持つことを確認
result.forEach(record -> {
    assertEquals(2, record.length);
    assertNotNull(record[0]); // IP address
    assertTrue(record[1] instanceof Number); // count
});
```

**セキュリティ監視の特徴**:
- **閾値ベース検知**: 指定時間内の指定回数以上のアクセスを検知
- **INTERVAL演算**: PostgreSQLのINTERVAL演算による時間計算
- **HAVING句**: GROUP BY後の条件フィルタリング####
 テストケース15: 失敗ログイン試行検索
**メソッド**: `testFindFailedLoginAttempts_WithRecentHours_ShouldReturnFailedAttempts`

##### 認証失敗監視テスト
```java
// Given - 失敗ログイン試行を作成
SystemLog failedLogin = createSystemLog(null, TEST_USER_ID_1, "login_failed", TEST_STATUS_ERROR,
        TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"reason\": \"invalid_password\"}", 
        OffsetDateTime.now(ZoneOffset.ofHours(9)).minusMinutes(10));
systemLogRepository.save(failedLogin);

// When
List<SystemLog> result = systemLogRepository.findFailedLoginAttempts(1);

// Then
assertNotNull(result);
// 失敗ログインが含まれていることを確認
assertTrue(result.stream().anyMatch(log -> 
    log.getAction().contains("login") && log.getAction().contains("failed")));
```

**認証失敗監視の重要性**:
- **ブルートフォース攻撃検知**: 連続した認証失敗の検知
- **LIKE演算子**: パターンマッチングによる柔軟な検索
- **時間制限**: 最近の失敗試行のみを対象とした効率的な検索

### 2.5 検索・フィルタリングテスト群

#### テストケース16: キーワード検索
**メソッド**: `testSearchByKeyword_WithValidKeyword_ShouldReturnMatchingLogs`

##### 全文検索テスト
```java
// Given
Pageable pageable = PageRequest.of(0, 10);

// When
Page<SystemLog> result = systemLogRepository.searchByKeyword("LOGIN", pageable);

// Then
assertNotNull(result);
assertNotNull(result.getContent());
assertFalse(result.getContent().isEmpty());
// キーワードが含まれていることを確認
assertTrue(result.getContent().stream().anyMatch(log -> 
    log.getAction().contains("LOGIN") || 
    (log.getStatus() != null && log.getStatus().contains("LOGIN")) ||
    (log.getIpAddress() != null && log.getIpAddress().contains("LOGIN")) ||
    (log.getUserAgent() != null && log.getUserAgent().contains("LOGIN"))));
```

**全文検索の特徴**:
- **複数フィールド検索**: action、status、ipAddress、userAgentの横断検索
- **ILIKE演算子**: 大文字小文字を区別しない部分一致検索
- **ページネーション**: Spring Data JPAのPageable対応

### 2.6 集計統計テスト群

#### テストケース17-20: 各種集計統計
**メソッド**: `testCountByActionGrouped_ShouldReturnActionCounts` 等

##### 集計統計テスト群
```java
// アクション別カウント
@Test
void testCountByActionGrouped_ShouldReturnActionCounts() {
    List<Map<String, Object>> result = systemLogRepository.countByActionGrouped();
    // 各統計レコードが必要なキーを持つことを確認
    result.forEach(stat -> {
        assertTrue(stat.containsKey("action"));
        assertTrue(stat.containsKey("count"));
        assertNotNull(stat.get("action"));
        assertTrue(stat.get("count") instanceof Number);
    });
}

// ステータス別カウント
@Test
void testCountByStatusGrouped_ShouldReturnStatusCounts() {
    // 同様の検証パターン
}

// ユーザー別カウント
@Test
void testCountByUserGrouped_ShouldReturnUserCounts() {
    // 同様の検証パターン
}

// 日別カウント
@Test
void testCountByDateGrouped_ShouldReturnDateCounts() {
    // 同様の検証パターン
}
```

**集計統計の特徴**:
- **GROUP BY集計**: 各カテゴリ別のカウント集計
- **ORDER BY**: カウント降順でのソート
- **Map型戻り値**: 動的なキー・値ペアでの結果取得
- **統一検証パターン**: 各統計で共通の検証ロジック

### 2.7 データ整合性テスト群

#### テストケース21: 保存と取得の整合性
**メソッド**: `testSaveAndRetrieve_ShouldMaintainDataIntegrity`

##### CRUD操作の整合性確認
```java
// Given
SystemLog newLog = createSystemLog(null, TEST_USER_ID_2, "TEST_ACTION", TEST_STATUS_SUCCESS,
        "192.168.1.202", "Test User Agent", "{\"test\": \"data\"}", 
        OffsetDateTime.now(ZoneOffset.ofHours(9)));

// When
SystemLog savedLog = systemLogRepository.save(newLog);
SystemLog retrievedLog = systemLogRepository.findById(savedLog.getId().intValue()).orElse(null);

// Then
assertNotNull(retrievedLog);
assertEquals(savedLog.getId(), retrievedLog.getId());
assertEquals(TEST_USER_ID_2, retrievedLog.getUserId());
assertEquals("TEST_ACTION", retrievedLog.getAction());
assertEquals(TEST_STATUS_SUCCESS, retrievedLog.getStatus());
assertEquals("192.168.1.202", retrievedLog.getIpAddress());
assertEquals("Test User Agent", retrievedLog.getUserAgent());
assertEquals("{\"test\": \"data\"}", retrievedLog.getDetails());
assertNotNull(retrievedLog.getCreatedAt());
```

**データ整合性の検証ポイント**:
- **ID自動生成**: データベースでの自動ID生成確認
- **JSON詳細情報**: detailsフィールドのJSON文字列保持確認
- **外部キー制約**: 実在するユーザーIDでの制約遵守
- **作成日時**: createdAtフィールドの自動設定確認##
## テストケース22: 更新処理の確認
**メソッド**: `testUpdateLog_ShouldReflectChanges`

##### 更新操作テスト
```java
// Given
SystemLog log = systemLogRepository.findById(loginLog1.getId().intValue()).orElse(null);
assertNotNull(log);

// When
log.setStatus("updated");
log.setDetails("{\"updated\": \"true\"}");
SystemLog updatedLog = systemLogRepository.save(log);

// Then
assertEquals(loginLog1.getId(), updatedLog.getId());
assertEquals("updated", updatedLog.getStatus());
assertEquals("{\"updated\": \"true\"}", updatedLog.getDetails());
```

**更新処理の特徴**:
- **部分更新**: 特定フィールドのみの更新
- **楽観的ロック**: JPA/Hibernateの楽観的ロック機能
- **変更検知**: Hibernateのダーティチェック機能
- **JSON更新**: detailsフィールドのJSON内容更新

#### テストケース23: 削除処理の確認
**メソッド**: `testDeleteLog_ShouldRemoveFromDatabase`

##### 削除操作テスト
```java
// Given
Long logId = loginLog1.getId();
assertTrue(systemLogRepository.existsById(logId.intValue()));

// When
systemLogRepository.deleteById(logId.intValue());

// Then
assertFalse(systemLogRepository.existsById(logId.intValue()));
```

**削除処理の検証**:
- **存在確認**: 削除前の存在確認
- **削除実行**: deleteById()による削除実行
- **削除確認**: existsById()による削除後の確認

### 2.8 バッチ処理・メンテナンステスト群

#### テストケース24: 期間別カウント
**メソッド**: `testCountByCreatedAtBetween_WithValidRange_ShouldReturnCount`

##### 期間集計テスト
```java
// Given
OffsetDateTime startDate = baseTime.minusMinutes(10);
OffsetDateTime endDate = baseTime.plusHours(1);

// When
long count = systemLogRepository.countByCreatedAtBetween(startDate, endDate);

// Then
assertTrue(count >= 4); // テストデータ分
```

**期間集計の特徴**:
- **COUNT関数**: 指定期間内のレコード数カウント
- **long型戻り値**: 大量データに対応したlong型での件数取得
- **期間フィルタ**: BETWEEN句による効率的な期間検索

#### テストケース25: 古いログ検索
**メソッド**: `testFindLogsOlderThan_WithCutoffDate_ShouldReturnOldLogs`

##### データクリーンアップ支援テスト
```java
// Given
OffsetDateTime cutoffDate = OffsetDateTime.now(ZoneOffset.ofHours(9)).minusDays(1);

// When
List<SystemLog> result = systemLogRepository.findLogsOlderThan(cutoffDate);

// Then
assertNotNull(result);
// 古いログがあれば、すべてカットオフ日時より前であることを確認
result.forEach(log -> assertTrue(log.getCreatedAt().isBefore(cutoffDate)));
```

**データクリーンアップの重要性**:
- **ストレージ管理**: 古いログデータの効率的な特定
- **パフォーマンス維持**: 大量データによる性能劣化の防止
- **コンプライアンス**: データ保持期間の遵守

#### テストケース26: バッチ処理用検索
**メソッド**: `testFindForBatchProcessing_WithOffsetAndLimit_ShouldReturnLimitedResults`

##### バッチ処理支援テスト
```java
// When
List<SystemLog> result = systemLogRepository.findForBatchProcessing(0, 5);

// Then
assertNotNull(result);
assertTrue(result.size() <= 5);
// ID順にソートされていることを確認
if (result.size() > 1) {
    for (int i = 0; i < result.size() - 1; i++) {
        assertTrue(result.get(i).getId() <= result.get(i + 1).getId());
    }
}
```

**バッチ処理の特徴**:
- **OFFSET/LIMIT**: ページング処理による大量データの分割処理
- **ID順ソート**: 一意キーでの安定したソート順
- **メモリ効率**: 大量データを小分けにして処理#
## 2.9 エッジケース・境界値テスト群

#### テストケース27-29: null値処理テスト
**メソッド**: `testFindByUserId_WithNullUserId_ShouldHandleGracefully` 等

##### null安全性テスト
```java
@Test
void testFindByUserId_WithNullUserId_ShouldHandleGracefully() {
    // When & Then
    assertDoesNotThrow(() -> {
        List<SystemLog> result = systemLogRepository.findByUserId(null);
        assertNotNull(result);
    });
}

@Test
void testFindByAction_WithNullAction_ShouldHandleGracefully() {
    // When & Then
    assertDoesNotThrow(() -> {
        List<SystemLog> result = systemLogRepository.findByAction(null);
        assertNotNull(result);
    });
}

@Test
void testFindByIpAddress_WithNullIp_ShouldHandleGracefully() {
    // When & Then
    assertDoesNotThrow(() -> {
        List<SystemLog> result = systemLogRepository.findByIpAddress(null);
        assertNotNull(result);
    });
}
```

**null安全性の重要性**:
- **例外なし**: null値でも例外が発生しないことを確認
- **戻り値確認**: null値でも適切な戻り値（空リスト等）が返されることを確認
- **防御的プログラミング**: 予期しないnull値に対する堅牢性確認

#### テストケース30: 無効な日付範囲テスト
**メソッド**: `testFindByCreatedAtBetween_WithInvalidRange_ShouldReturnEmptyList`

##### 逆転日付範囲テスト
```java
// Given - 開始日が終了日より後の無効な範囲
OffsetDateTime invalidStartDate = baseTime.plusHours(2);
OffsetDateTime invalidEndDate = baseTime.plusHours(1);

// When
List<SystemLog> result = systemLogRepository.findByCreatedAtBetween(invalidStartDate, invalidEndDate);

// Then
assertNotNull(result);
assertTrue(result.isEmpty());
```

**無効範囲の処理**:
- **論理的矛盾**: 開始日 > 終了日の論理的に無効な範囲
- **空結果**: 無効な範囲では空の結果が返されることを確認
- **例外なし**: 無効な範囲でも例外が発生しないことを確認

### 2.10 パフォーマンステスト

#### テストケース31: 大量データ処理効率性
**メソッド**: `testLargeDatasetQuery_ShouldPerformEfficiently`

##### パフォーマンス検証テスト
```java
// Given - 大量のテストデータを作成
for (int i = 0; i < 50; i++) {
    SystemLog log = createSystemLog(null, TEST_USER_ID_1, "BULK_ACTION_" + i, TEST_STATUS_SUCCESS,
            TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"bulk\": " + i + "}", 
            baseTime.plusMinutes(i));
    systemLogRepository.save(log);
}

// When
long startTime = System.currentTimeMillis();
List<SystemLog> result = systemLogRepository.findByUserId(TEST_USER_ID_1);
long endTime = System.currentTimeMillis();

// Then
assertNotNull(result);
assertTrue(result.size() >= 53); // 既存データ + 元の3件 + 新規50件
assertTrue(endTime - startTime < 1000); // 1秒以内で完了することを確認
```

**パフォーマンス測定の重要性**:
- **レスポンス時間**: 大量データでの検索性能確認
- **メモリ使用量**: 大量データ読み込み時のメモリ効率
- **インデックス効果**: データベースインデックスの効果確認
- **スケーラビリティ**: データ量増加に対する性能劣化の確認

## 3. ヘルパーメソッド活用

### 3.1 テストデータ生成メソッド

#### createSystemLog メソッド
```java
/**
 * テスト用SystemLogを作成
 */
private SystemLog createSystemLog(Long id, Integer userId, String action, String status,
                                 String ipAddress, String userAgent, String details, 
                                 OffsetDateTime createdAt) {
    SystemLog log = new SystemLog();
    log.setId(id);
    log.setUserId(userId);
    log.setAction(action);
    log.setStatus(status);
    log.setIpAddress(ipAddress);
    log.setUserAgent(userAgent);
    log.setDetails(details);
    log.setCreatedAt(createdAt);
    return log;
}
```

**設計パターン**:
- **ファクトリーメソッド**: 一貫したテストデータ生成
- **パラメータ化**: 柔軟なデータ生成のための多数のパラメータ
- **JSON詳細情報**: detailsフィールドでのJSON文字列設定
- **タイムゾーン統一**: 日本時間（JST）での作成日時設定# 4.
 システムログテスト特有の戦略

### 4.1 既存データ使用の利点と課題

#### 利点
- **実環境再現**: 本番環境と同じデータベースエンジン（PostgreSQL）を使用
- **制約検証**: 外部キー制約、NOT NULL制約等の実際の動作確認
- **統計クエリ**: 実際のデータでの複雑な統計クエリの動作確認
- **セキュリティ機能**: ログ監視とセキュリティ分析機能のテスト

#### 課題と対策
```java
// 課題1: 既存データとの競合
// 対策: >= 比較や既存データを考慮した検証
assertTrue(result.size() >= expectedMinimumSize);

// 課題2: 外部キー制約違反
// 対策: 実在するユーザーIDのみ使用
private static final Integer TEST_USER_ID_1 = 1; // 実在するユーザー

// 課題3: テスト実行順序依存
// 対策: @Transactionalによる自動ロールバック
@Transactional
class SystemLogRepositoryTest {
```

### 4.2 ネイティブクエリテストの重要性

#### PostgreSQL固有機能のテスト
```java
// EXTRACT関数のテスト
@Query(nativeQuery = true, value = """
    SELECT EXTRACT(HOUR FROM sl.created_at) as hour, COUNT(sl) as activityCount
    FROM system_logs sl
    WHERE sl.created_at BETWEEN :startDate AND :endDate
    GROUP BY EXTRACT(HOUR FROM sl.created_at)
    ORDER BY EXTRACT(HOUR FROM sl.created_at) ASC
    """)
List<Map<String, Object>> getHourlyActivityStatistics(@Param("startDate") OffsetDateTime startDate,
                                                      @Param("endDate") OffsetDateTime endDate);

// INTERVAL演算のテスト
@Query(nativeQuery = true, value = """
    SELECT sl.ip_address, COUNT(sl) as accessCount
    FROM system_logs sl
    WHERE sl.created_at >= CURRENT_TIMESTAMP - INTERVAL '1 hour' * :hours
    AND sl.ip_address IS NOT NULL
    GROUP BY sl.ip_address
    HAVING COUNT(sl) > :threshold
    ORDER BY COUNT(sl) DESC
    """)
List<Object[]> findSuspiciousActivity(@Param("hours") int hours, @Param("threshold") long threshold);
```

**ネイティブクエリテストの特徴**:
- **SQL方言**: PostgreSQL固有のSQL構文の動作確認
- **関数テスト**: データベース関数（EXTRACT, INTERVAL等）の動作確認
- **パフォーマンス**: 最適化されたネイティブクエリの性能確認
- **統計処理**: 複雑な集計処理の正確性確認

### 4.3 JSON詳細情報処理テストの複雑性

#### JSON文字列処理戦略
```java
// JSON詳細情報の設定
private static final String LOGIN_DETAILS = "{\"method\": \"password\"}";
private static final String LOGOUT_DETAILS = "{\"duration\": \"30min\"}";
private static final String ERROR_DETAILS = "{\"reason\": \"insufficient_permissions\"}";

// テストデータでのJSON使用
SystemLog loginLog = createSystemLog(null, TEST_USER_ID_1, TEST_ACTION_LOGIN, TEST_STATUS_SUCCESS,
        TEST_IP_ADDRESS, TEST_USER_AGENT, LOGIN_DETAILS, baseTime);
```

#### JSON検索クエリ（将来拡張用）
```java
// JSONB検索クエリの例（PostgreSQL）
@Query(nativeQuery = true, value = "SELECT sl.* FROM system_logs sl WHERE jsonb_extract_path_text(sl.details, :jsonPath) = :value")
List<SystemLog> findByJsonDetails(@Param("jsonPath") String jsonPath, @Param("value") String value);
```

**JSON処理の特徴**:
- **構造化詳細情報**: ログの詳細情報をJSON形式で構造化
- **柔軟な検索**: JSONB型による柔軟な検索機能（PostgreSQL）
- **拡張性**: 新しい詳細情報フィールドの追加が容易

### 4.4 統計クエリテストの複雑性

#### Map型戻り値の処理
```java
// 統計クエリの結果検証パターン
List<Map<String, Object>> result = systemLogRepository.getActionStatistics(startDate, endDate);

// 各統計レコードの検証
result.forEach(stat -> {
    assertTrue(stat.containsKey("action"));
    assertTrue(stat.containsKey("count"));
    assertNotNull(stat.get("action"));
    assertTrue(stat.get("count") instanceof Number);
});
```

**統計クエリの特徴**:
- **動的結果**: Map型による動的なキー・値ペア
- **型安全性**: Number型での数値結果の確認
- **null安全性**: キーの存在確認とnull値チェック

## 5. テスト作成のベストプラクティス

### 5.1 システムログテスト専用のパターン

#### テストデータ準備パターン
```java
@BeforeEach
void setUp() {
    // 1. 基準時刻設定
    baseTime = OffsetDateTime.of(2025, 2, 1, 10, 0, 0, 0, ZoneOffset.ofHours(9));
    
    // 2. 多様なログタイプの作成
    loginLog1 = createSystemLog(null, TEST_USER_ID_1, TEST_ACTION_LOGIN, TEST_STATUS_SUCCESS, ...);
    logoutLog1 = createSystemLog(null, TEST_USER_ID_1, TEST_ACTION_LOGOUT, TEST_STATUS_SUCCESS, ...);
    errorLog1 = createSystemLog(null, TEST_USER_ID_1, "ACCESS_DENIED", TEST_STATUS_ERROR, ...);
    
    // 3. データベース保存
    loginLog1 = systemLogRepository.save(loginLog1);
    logoutLog1 = systemLogRepository.save(logoutLog1);
    errorLog1 = systemLogRepository.save(errorLog1);
}
```

#### 検証パターン
```java
// パターン1: null安全性 → サイズ確認 → 内容確認
assertNotNull(result);
assertTrue(result.size() >= expectedMinimumSize);
assertTrue(result.stream().allMatch(log -> condition));

// パターン2: 統計結果検証
assertNotNull(result);
assertFalse(result.isEmpty());
result.forEach(stat -> {
    assertTrue(stat.containsKey("expectedKey"));
    assertTrue(stat.get("expectedKey") instanceof ExpectedType);
});

// パターン3: ソート順確認
if (result.size() > 1) {
    for (int i = 0; i < result.size() - 1; i++) {
        assertTrue(sortCondition);
    }
}

// パターン4: セキュリティ関連検証
result.forEach(record -> {
    assertEquals(2, record.length); // IP address + count
    assertNotNull(record[0]);
    assertTrue(record[1] instanceof Number);
});
```

### 5.2 実データベース環境での注意点

#### 既存データとの共存
```java
// 問題のあるコード（固定値期待）
assertEquals(2, result.size()); // 既存データがあると失敗

// 改善されたコード（最小値期待）
assertTrue(result.size() >= 2); // 既存データ + テストデータ
```

#### 外部キー制約の遵守
```java
// 問題のあるコード（存在しないユーザーID）
SystemLog log = createSystemLog(null, 999, "ACTION", ...); // 制約違反

// 改善されたコード（実在するユーザーID）
SystemLog log = createSystemLog(null, TEST_USER_ID_1, "ACTION", ...); // 制約遵守
```

#### トランザクション境界の理解
```java
// @Transactionalにより各テストメソッド後に自動ロールバック
@Test
@Transactional
void testMethod() {
    // データ変更操作
    systemLogRepository.save(log);
    
    // テスト終了後、変更は自動的にロールバックされる
}
```

## 6. 一般的な問題と解決策

### 6.1 データベース接続の問題

#### 問題: テストデータベース接続失敗
```
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'dataSource'
```

**解決策**:
```properties
# application-test.properties の確認
spring.datasource.url=jdbc:postgresql://localhost:5432/comsys_test
spring.datasource.username=postgres
spring.datasource.password=AM2013japan
spring.datasource.driver-class-name=org.postgresql.Driver
```

#### 問題: 外部キー制約違反
```
ERROR: insert or update on table "system_logs" violates foreign key constraint "system_logs_user_id_fkey"
```

**解決策**:
```java
// 実在するユーザーIDを使用
private static final Integer TEST_USER_ID_1 = 1; // comsys_test_dump.sqlに存在するID

// テストデータ作成時に実在するIDのみ使用
SystemLog log = createSystemLog(null, TEST_USER_ID_1, "TEST_ACTION", ...);
```

### 6.2 ネイティブクエリの問題

#### 問題: INTERVAL構文エラー
```java
// 問題のあるコード（パラメータ埋め込み失敗）
@Query(nativeQuery = true, value = "... INTERVAL ':hours hours' ...")
```

**解決策**:
```java
// PostgreSQL互換の構文使用
@Query(nativeQuery = true, value = "... INTERVAL '1 hour' * :hours ...")

// または文字列結合を避けた固定値使用
@Query(nativeQuery = true, value = "... INTERVAL '1 hour' ...")
```

#### 問題: パラメータ型推定エラー
```java
// 問題のあるコード（型推定失敗）
@Query(nativeQuery = true, value = "WHERE (:param IS NULL OR column = :param)")
```

**解決策**:
```java
// 明示的な型キャスト
@Query(nativeQuery = true, value = "WHERE (:param IS NULL OR column = CAST(:param AS TEXT))")

// またはJPQLの使用を検討
@Query("SELECT s FROM SystemLog s WHERE (:param IS NULL OR s.column = :param)")
```

### 6.3 時系列データ処理の問題

#### 問題: タイムゾーンの不整合
```java
// 問題のあるコード（タイムゾーン不明）
OffsetDateTime now = OffsetDateTime.now(); // システム依存
```

**解決策**:
```java
// 明示的なタイムゾーン指定
OffsetDateTime now = OffsetDateTime.now(ZoneOffset.ofHours(9)); // 日本時間

// または統一された基準時刻の使用
private static final ZoneOffset JST = ZoneOffset.ofHours(9);
OffsetDateTime baseTime = OffsetDateTime.of(2025, 2, 1, 10, 0, 0, 0, JST);
```

#### 問題: 時系列順序の検証失敗
```java
// 問題のあるコード（同一時刻の考慮不足）
assertTrue(result.get(i).getCreatedAt().isAfter(result.get(i + 1).getCreatedAt()));
```

**解決策**:
```java
// 同一時刻も許容する比較
assertTrue(result.get(i).getCreatedAt().isAfter(result.get(i + 1).getCreatedAt()) ||
          result.get(i).getCreatedAt().equals(result.get(i + 1).getCreatedAt()));

// またはcompareTo()の使用
assertTrue(result.get(i).getCreatedAt().compareTo(result.get(i + 1).getCreatedAt()) >= 0);
```

### 6.4 統計クエリの問題

#### 問題: Map型結果の型安全性
```java
// 問題のあるコード（型チェック不足）
Map<String, Object> stat = result.get(0);
Integer count = (Integer) stat.get("count"); // ClassCastExceptionの可能性
```

**解決策**:
```java
// 型安全な確認
Map<String, Object> stat = result.get(0);
assertTrue(stat.get("count") instanceof Number);
Number count = (Number) stat.get("count");

// またはより安全な取得
Optional.ofNullable(stat.get("count"))
    .filter(Number.class::isInstance)
    .map(Number.class::cast)
    .ifPresent(count -> {
        // 処理
    });
```

#### 問題: 空の統計結果
```java
// 問題のあるコード（空結果の考慮不足）
List<Map<String, Object>> result = repository.getStatistics();
Map<String, Object> firstStat = result.get(0); // IndexOutOfBoundsExceptionの可能性
```

**解決策**:
```java
// 空結果の確認
List<Map<String, Object>> result = repository.getStatistics();
assertNotNull(result);
assertFalse(result.isEmpty());
// その後で個別要素にアクセス
```

### 6.5 JSON詳細情報の問題

#### 問題: JSON形式の不正
```java
// 問題のあるコード（不正なJSON）
String details = "{invalid: json}"; // クォートなし
```

**解決策**:
```java
// 正しいJSON形式
String details = "{\"key\": \"value\"}"; // 適切なクォート

// またはJSONライブラリの使用
ObjectMapper mapper = new ObjectMapper();
String details = mapper.writeValueAsString(Map.of("key", "value"));
```

#### 問題: JSON検索の複雑性
```java
// 将来的なJSONB検索の準備
@Query(nativeQuery = true, value = """
    SELECT sl.* FROM system_logs sl 
    WHERE sl.details::jsonb @> :jsonFilter::jsonb
    """)
List<SystemLog> findByJsonFilter(@Param("jsonFilter") String jsonFilter);
```

## 7. 実装済みテストケース一覧（32件）

### 7.1 基本検索機能（5件）
- `testFindByUserId_WithExistingUser_ShouldReturnLogs`
- `testFindByUserId_WithNonExistentUser_ShouldReturnEmptyList`
- `testFindByAction_WithValidAction_ShouldReturnFilteredLogs`
- `testFindByIpAddress_WithValidIp_ShouldReturnLogs`
- `testFindByUserIdAndAction_WithValidData_ShouldReturnFilteredLogs`

### 7.2 日時検索機能（4件）
- `testFindByCreatedAtBetween_WithValidRange_ShouldReturnLogs`
- `testFindTodayLogs_ShouldReturnTodayLogs`
- `testFindLatestLogs_WithLimit_ShouldReturnLimitedResults`
- `testFindLatestLogsByUser_WithValidUser_ShouldReturnUserLogs`

### 7.3 統計情報機能（4件）
- `testGetActionStatistics_WithValidRange_ShouldReturnStatistics`
- `testGetUserActivityStatistics_WithValidRange_ShouldReturnStatistics`
- `testGetIpAccessStatistics_WithValidRange_ShouldReturnStatistics`
- `testGetHourlyActivityStatistics_WithValidRange_ShouldReturnStatistics`

### 7.4 セキュリティ関連機能（2件）
- `testFindSuspiciousActivity_WithThreshold_ShouldReturnSuspiciousIps`
- `testFindFailedLoginAttempts_WithRecentHours_ShouldReturnFailedAttempts`

### 7.5 検索・フィルタリング機能（1件）
- `testSearchByKeyword_WithValidKeyword_ShouldReturnMatchingLogs`

### 7.6 集計統計機能（4件）
- `testCountByActionGrouped_ShouldReturnActionCounts`
- `testCountByStatusGrouped_ShouldReturnStatusCounts`
- `testCountByUserGrouped_ShouldReturnUserCounts`
- `testCountByDateGrouped_ShouldReturnDateCounts`

### 7.7 データ整合性テスト（3件）
- `testSaveAndRetrieve_ShouldMaintainDataIntegrity`
- `testUpdateLog_ShouldReflectChanges`
- `testDeleteLog_ShouldRemoveFromDatabase`

### 7.8 バッチ処理・メンテナンステスト（4件）
- `testCountByCreatedAtBetween_WithValidRange_ShouldReturnCount`
- `testFindLogsOlderThan_WithCutoffDate_ShouldReturnOldLogs`
- `testCountByCreatedAtBefore_WithCutoffDate_ShouldReturnCount`
- `testFindForBatchProcessing_WithOffsetAndLimit_ShouldReturnLimitedResults`

### 7.9 エッジケース・境界値テスト（4件）
- `testFindByUserId_WithNullUserId_ShouldHandleGracefully`
- `testFindByAction_WithNullAction_ShouldHandleGracefully`
- `testFindByIpAddress_WithNullIp_ShouldHandleGracefully`
- `testFindByCreatedAtBetween_WithInvalidRange_ShouldReturnEmptyList`

### 7.10 パフォーマンステスト（1件）
- `testLargeDatasetQuery_ShouldPerformEfficiently`

## 8. システムログテスト特有の高度な機能

### 8.1 セキュリティ監視機能のテスト

#### 異常アクセスパターンの検知
```java
// 短時間での大量アクセス検知テスト
@Test
void testDetectAbnormalAccessPattern() {
    // Given - 短時間で大量のアクセスログを作成
    OffsetDateTime baseTime = OffsetDateTime.now(ZoneOffset.ofHours(9));
    String suspiciousIp = "192.168.1.100";
    
    for (int i = 0; i < 10; i++) {
        SystemLog log = createSystemLog(null, TEST_USER_ID_1, "PAGE_ACCESS", TEST_STATUS_SUCCESS,
                suspiciousIp, TEST_USER_AGENT, "{\"page\": \"/admin\"}", 
                baseTime.minusMinutes(i));
        systemLogRepository.save(log);
    }
    
    // When
    List<Object[]> result = systemLogRepository.findSuspiciousActivity(1, 5);
    
    // Then
    assertNotNull(result);
    assertTrue(result.stream().anyMatch(record -> 
        suspiciousIp.equals(record[0]) && ((Number) record[1]).intValue() >= 5));
}
```

#### ブルートフォース攻撃の検知
```java
// 連続ログイン失敗の検知テスト
@Test
void testDetectBruteForceAttack() {
    // Given - 連続したログイン失敗を作成
    OffsetDateTime baseTime = OffsetDateTime.now(ZoneOffset.ofHours(9));
    String attackerIp = "192.168.1.999";
    
    for (int i = 0; i < 5; i++) {
        SystemLog failedLogin = createSystemLog(null, TEST_USER_ID_1, "login_failed", TEST_STATUS_ERROR,
                attackerIp, TEST_USER_AGENT, "{\"reason\": \"invalid_password\", \"attempt\": " + (i+1) + "}", 
                baseTime.minusMinutes(i));
        systemLogRepository.save(failedLogin);
    }
    
    // When
    List<SystemLog> result = systemLogRepository.findFailedLoginAttempts(1);
    
    // Then
    assertNotNull(result);
    long failedCount = result.stream()
        .filter(log -> attackerIp.equals(log.getIpAddress()))
        .count();
    assertTrue(failedCount >= 5);
}
```

### 8.2 ログ分析機能のテスト

#### ユーザー行動パターン分析
```java
// ユーザーの活動パターン分析テスト
@Test
void testAnalyzeUserBehaviorPattern() {
    // Given - 様々な時間帯でのユーザー活動を作成
    OffsetDateTime morning = OffsetDateTime.of(2025, 2, 1, 9, 0, 0, 0, ZoneOffset.ofHours(9));
    OffsetDateTime afternoon = OffsetDateTime.of(2025, 2, 1, 14, 0, 0, 0, ZoneOffset.ofHours(9));
    OffsetDateTime evening = OffsetDateTime.of(2025, 2, 1, 18, 0, 0, 0, ZoneOffset.ofHours(9));
    
    systemLogRepository.save(createSystemLog(null, TEST_USER_ID_1, "LOGIN", TEST_STATUS_SUCCESS,
            TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"location\": \"office\"}", morning));
    systemLogRepository.save(createSystemLog(null, TEST_USER_ID_1, "DOCUMENT_ACCESS", TEST_STATUS_SUCCESS,
            TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"document\": \"report.pdf\"}", afternoon));
    systemLogRepository.save(createSystemLog(null, TEST_USER_ID_1, "LOGOUT", TEST_STATUS_SUCCESS,
            TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"duration\": \"9hours\"}", evening));
    
    // When
    List<Map<String, Object>> result = systemLogRepository.getHourlyActivityStatistics(
            morning.minusHours(1), evening.plusHours(1));
    
    // Then
    assertNotNull(result);
    assertTrue(result.size() >= 3); // 朝、昼、夕方の活動
    
    // 時間別の活動分布を確認
    Map<Integer, Long> hourlyActivity = result.stream()
        .collect(Collectors.toMap(
            stat -> ((Number) stat.get("hour")).intValue(),
            stat -> ((Number) stat.get("activityCount")).longValue()
        ));
    
    assertTrue(hourlyActivity.containsKey(9));  // 朝の活動
    assertTrue(hourlyActivity.containsKey(14)); // 昼の活動
    assertTrue(hourlyActivity.containsKey(18)); // 夕方の活動
}
```

### 8.3 システム監視機能のテスト

#### システム負荷パターンの分析
```java
// システム負荷の時間別分析テスト
@Test
void testAnalyzeSystemLoadPattern() {
    // Given - ピーク時間とオフピーク時間のログを作成
    OffsetDateTime peakTime = OffsetDateTime.of(2025, 2, 1, 10, 0, 0, 0, ZoneOffset.ofHours(9));
    OffsetDateTime offPeakTime = OffsetDateTime.of(2025, 2, 1, 3, 0, 0, 0, ZoneOffset.ofHours(9));
    
    // ピーク時間の大量アクセス
    for (int i = 0; i < 20; i++) {
        SystemLog peakLog = createSystemLog(null, TEST_USER_ID_1, "API_CALL", TEST_STATUS_SUCCESS,
                TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"endpoint\": \"/api/data\"}", 
                peakTime.plusMinutes(i));
        systemLogRepository.save(peakLog);
    }
    
    // オフピーク時間の少量アクセス
    for (int i = 0; i < 3; i++) {
        SystemLog offPeakLog = createSystemLog(null, TEST_USER_ID_2, "API_CALL", TEST_STATUS_SUCCESS,
                TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"endpoint\": \"/api/health\"}", 
                offPeakTime.plusMinutes(i * 10));
        systemLogRepository.save(offPeakLog);
    }
    
    // When
    List<Map<String, Object>> result = systemLogRepository.getHourlyActivityStatistics(
            offPeakTime.minusHours(1), peakTime.plusHours(1));
    
    // Then
    assertNotNull(result);
    
    // 時間別の負荷分布を確認
    Map<Integer, Long> loadByHour = result.stream()
        .collect(Collectors.toMap(
            stat -> ((Number) stat.get("hour")).intValue(),
            stat -> ((Number) stat.get("activityCount")).longValue()
        ));
    
    // ピーク時間の負荷がオフピーク時間より高いことを確認
    Long peakLoad = loadByHour.get(10);
    Long offPeakLoad = loadByHour.get(3);
    
    if (peakLoad != null && offPeakLoad != null) {
        assertTrue(peakLoad > offPeakLoad);
    }
}
```

### 8.4 コンプライアンス機能のテスト

#### データ保持期間の管理
```java
// データ保持期間管理テスト
@Test
void testDataRetentionManagement() {
    // Given - 異なる期間のログを作成
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.ofHours(9));
    OffsetDateTime oldLog = now.minusDays(365); // 1年前
    OffsetDateTime recentLog = now.minusDays(30); // 1ヶ月前
    
    SystemLog expiredLog = createSystemLog(null, TEST_USER_ID_1, "OLD_ACTION", TEST_STATUS_SUCCESS,
            TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"note\": \"expired\"}", oldLog);
    SystemLog validLog = createSystemLog(null, TEST_USER_ID_1, "RECENT_ACTION", TEST_STATUS_SUCCESS,
            TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"note\": \"valid\"}", recentLog);
    
    systemLogRepository.save(expiredLog);
    systemLogRepository.save(validLog);
    
    // When - 6ヶ月より古いログを検索
    OffsetDateTime cutoffDate = now.minusDays(180);
    List<SystemLog> expiredLogs = systemLogRepository.findLogsOlderThan(cutoffDate);
    long expiredCount = systemLogRepository.countByCreatedAtBefore(cutoffDate);
    
    // Then
    assertNotNull(expiredLogs);
    assertTrue(expiredCount >= 1); // 期限切れログが存在
    
    // 期限切れログが正しく特定されることを確認
    assertTrue(expiredLogs.stream().anyMatch(log -> 
        "OLD_ACTION".equals(log.getAction()) && log.getCreatedAt().isBefore(cutoffDate)));
}
```

#### 監査ログの完全性確認
```java
// 監査ログの完全性確認テスト
@Test
void testAuditLogIntegrity() {
    // Given - 重要な操作のログを作成
    OffsetDateTime auditTime = OffsetDateTime.now(ZoneOffset.ofHours(9));
    
    SystemLog adminLogin = createSystemLog(null, TEST_USER_ID_1, "ADMIN_LOGIN", TEST_STATUS_SUCCESS,
            TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"role\": \"admin\", \"session\": \"abc123\"}", auditTime);
    SystemLog dataAccess = createSystemLog(null, TEST_USER_ID_1, "SENSITIVE_DATA_ACCESS", TEST_STATUS_SUCCESS,
            TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"table\": \"users\", \"operation\": \"SELECT\"}", auditTime.plusMinutes(5));
    SystemLog dataModify = createSystemLog(null, TEST_USER_ID_1, "DATA_MODIFICATION", TEST_STATUS_SUCCESS,
            TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"table\": \"users\", \"operation\": \"UPDATE\", \"record_id\": 123}", auditTime.plusMinutes(10));
    
    systemLogRepository.save(adminLogin);
    systemLogRepository.save(dataAccess);
    systemLogRepository.save(dataModify);
    
    // When - 監査対象の操作を検索
    List<SystemLog> auditLogs = systemLogRepository.findByUserId(TEST_USER_ID_1);
    
    // Then - 監査ログの完全性を確認
    assertNotNull(auditLogs);
    
    // 管理者ログインが記録されていることを確認
    assertTrue(auditLogs.stream().anyMatch(log -> 
        "ADMIN_LOGIN".equals(log.getAction()) && log.getDetails().contains("admin")));
    
    // データアクセスが記録されていることを確認
    assertTrue(auditLogs.stream().anyMatch(log -> 
        "SENSITIVE_DATA_ACCESS".equals(log.getAction()) && log.getDetails().contains("users")));
    
    // データ変更が記録されていることを確認
    assertTrue(auditLogs.stream().anyMatch(log -> 
        "DATA_MODIFICATION".equals(log.getAction()) && log.getDetails().contains("UPDATE")));
}
```

## 9. まとめ

### 9.1 システムログテストの重要ポイント
1. **セキュリティ監視**: 不正アクセスや異常なアクティビティパターンの検知機能
2. **統計分析**: 複雑な集計クエリによるシステム利用状況の分析
3. **時系列処理**: OffsetDateTimeによる正確な時刻管理と時系列データの処理
4. **JSON詳細情報**: 構造化された詳細情報の格納と検索
5. **コンプライアンス**: データ保持期間管理と監査ログの完全性確保

### 9.2 テスト品質向上のチェックリスト
- [ ] 実データベース（comsys_test_dump.sql）を使用
- [ ] 外部キー制約を遵守した実在するユーザーIDを使用
- [ ] タイムゾーンは日本時間（JST）で統一
- [ ] JSON詳細情報の形式を正しく設定
- [ ] 既存データを考慮した検証（>= 比較）
- [ ] null安全性を最初に確認
- [ ] ネイティブクエリの動作を確認
- [ ] 統計クエリの型安全性を確保
- [ ] セキュリティ機能の動作を検証
- [ ] パフォーマンスとメモリ使用量を考慮
- [ ] @Transactionalによる自動ロールバックを活用

### 9.3 他のリポジトリテストとの違い

| 項目 | SystemLogRepositoryTest | AttendanceRecordRepositoryTest | UserRepositoryTest |
|------|-------------------------|--------------------------------|-------------------|
| **主要機能** | ログ監視・セキュリティ分析 | 勤怠管理・時刻処理 | ユーザー管理・認証 |
| **データ特性** | 時系列・JSON詳細情報 | 位置情報・時刻データ | 個人情報・権限データ |
| **統計処理** | 複雑な集計・セキュリティ統計 | 勤怠統計・時間集計 | ユーザー統計・部署集計 |
| **セキュリティ** | 不正アクセス検知 | 位置情報検証 | 認証・認可 |
| **特殊機能** | ログ分析・監査機能 | GPS処理・重複防止 | パスワード管理・役割管理 |

### 9.4 実装時の重要な考慮事項

#### セキュリティ面
- **ログインジェクション対策**: パラメータ化クエリの使用
- **機密情報の保護**: 詳細情報での機密データの適切な処理
- **アクセス制御**: ログ閲覧権限の適切な管理

#### パフォーマンス面
- **インデックス戦略**: 時刻、ユーザーID、IPアドレスでのインデックス
- **データ量管理**: 古いログの定期的なアーカイブ
- **クエリ最適化**: 統計クエリの効率的な実装

#### 運用面
- **ログローテーション**: 大量データの効率的な管理
- **監視アラート**: 異常パターンの自動検知
- **バックアップ**: 監査ログの確実な保存

この手順書に従うことで、システムログデータアクセス層の特性を考慮した包括的で信頼性の高いテストケースを作成できます。特にセキュリティ監視機能、複雑な統計処理、JSON詳細情報の処理を適切にテストすることで、実用的で堅牢なシステムログ管理機能を構築できます。sult.forEach(record -> {
    assertEquals(expectedArrayLength, record.length);
    assertNotNull(record[0]); // 必須フィールド
    assertTrue(record[1] instanceof Number); // 数値フィールド
});
```

### 5.2 既存データベース環境での注意点

#### 既存データとの共存
```java
// 問題のあるコード（固定値期待）
assertEquals(2, result.size()); // 既存データがあると失敗

// 改善されたコード（最小値期待）
assertTrue(result.size() >= 2); // 既存データ + テストデータ
```

#### 外部キー制約の遵守
```java
// 問題のあるコード（存在しないユーザーID）
SystemLog log = createSystemLog(null, 999, "ACTION", "status", ...); // 制約違反

// 改善されたコード（実在するユーザーID）
SystemLog log = createSystemLog(null, TEST_USER_ID_1, "ACTION", "status", ...); // 制約遵守
```

#### JSON詳細情報の適切な設定
```java
// 問題のあるコード（不正なJSON）
String invalidJson = "{invalid json}"; // JSON解析エラーの可能性

// 改善されたコード（有効なJSON）
String validJson = "{\"key\": \"value\"}"; // 有効なJSON文字列
```# 6. 一般的な問題
と解決策

### 6.1 データベース接続の問題

#### 問題: テストデータベース接続失敗
```
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'dataSource'
```

**解決策**:
```properties
# application-test.properties の確認
spring.datasource.url=jdbc:postgresql://localhost:5432/comsys_test
spring.datasource.username=postgres
spring.datasource.password=AM2013japan
spring.datasource.driver-class-name=org.postgresql.Driver
```

#### 問題: 外部キー制約違反
```
ERROR: insert or update on table "system_logs" violates foreign key constraint "system_logs_user_id_fkey"
```

**解決策**:
```java
// 実在するユーザーIDを使用
private static final Integer TEST_USER_ID_1 = 1; // comsys_test_dump.sqlに存在するID

// テストデータ作成時に実在するIDのみ使用
SystemLog log = createSystemLog(null, TEST_USER_ID_1, "ACTION", "status", ...);
```

### 6.2 ネイティブクエリの問題

#### 問題: INTERVAL構文エラー
```java
// 問題のあるコード（パラメータ埋め込みエラー）
@Query(nativeQuery = true, value = "... INTERVAL ':hours hours' ...")
```

**解決策**:
```java
// 修正されたコード（正しいパラメータ埋め込み）
@Query(nativeQuery = true, value = "... INTERVAL '1 hour' * :hours ...")
```

#### 問題: パラメータ型推定エラー
```
ERROR: could not determine data type of parameter $5
```

**解決策**:
```java
// 問題のあるコード（型推定失敗）
WHERE (:param IS NULL OR column = :param)

// 改善されたコード（明示的型キャスト）
WHERE (:param IS NULL OR column = CAST(:param AS TEXT))
```

### 6.3 統計クエリの問題

#### 問題: Map型結果の型安全性
```java
// 問題のあるコード（型チェックなし）
Object count = stat.get("count");
int countValue = (Integer) count; // ClassCastExceptionの可能性
```

**解決策**:
```java
// 改善されたコード（型安全性確保）
assertTrue(stat.get("count") instanceof Number);
Number count = (Number) stat.get("count");
long countValue = count.longValue();
```

#### 問題: 空の統計結果
```java
// 問題のあるコード（空結果の未考慮）
Map<String, Object> firstStat = result.get(0); // IndexOutOfBoundsExceptionの可能性
```

**解決策**:
```java
// 改善されたコード（空結果の考慮）
assertNotNull(result);
assertFalse(result.isEmpty());
if (!result.isEmpty()) {
    Map<String, Object> firstStat = result.get(0);
    // 処理続行
}
```

### 6.4 JSON処理の問題

#### 問題: 不正なJSON文字列
```java
// 問題のあるコード（不正なJSON）
String details = "{invalid: json}"; // JSON解析エラー
```

**解決策**:
```java
// 改善されたコード（有効なJSON）
String details = "{\"key\": \"value\"}"; // 有効なJSON文字列

// またはJSON検証
private boolean isValidJson(String json) {
    try {
        new ObjectMapper().readTree(json);
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

### 6.5 時刻処理の問題

#### 問題: タイムゾーンの不一致
```java
// 問題のあるコード（システムデフォルトタイムゾーン使用）
OffsetDateTime now = OffsetDateTime.now(); // システム依存
```

**解決策**:
```java
// 明示的な日本時間指定
OffsetDateTime now = OffsetDateTime.now(ZoneOffset.ofHours(9));

// または定数として定義
private static final ZoneOffset JST = ZoneOffset.ofHours(9);
OffsetDateTime now = OffsetDateTime.now(JST);
```

#### 問題: 動的日付テストの不安定性
```java
// 問題のあるコード（実行日依存）
List<SystemLog> todayLogs = systemLogRepository.findTodayLogs(); // 実行日によって結果が変わる
```

**解決策**:
```java
// 改善されたコード（テストデータ作成）
OffsetDateTime today = OffsetDateTime.now(ZoneOffset.ofHours(9));
SystemLog todayLog = createSystemLog(null, TEST_USER_ID_1, "TODAY_ACTION", TEST_STATUS_SUCCESS,
        TEST_IP_ADDRESS, TEST_USER_AGENT, "{\"test\": \"today\"}", today);
systemLogRepository.save(todayLog);

// 特定のテストデータの存在確認
assertTrue(result.stream().anyMatch(log -> "TODAY_ACTION".equals(log.getAction())));
```

## 7. 実装済みテストケース一覧（32件）

### 7.1 基本検索機能（5件）
- `testFindByUserId_WithExistingUser_ShouldReturnLogs`
- `testFindByUserId_WithNonExistentUser_ShouldReturnEmptyList`
- `testFindByAction_WithValidAction_ShouldReturnFilteredLogs`
- `testFindByIpAddress_WithValidIp_ShouldReturnLogs`
- `testFindByUserIdAndAction_WithValidData_ShouldReturnFilteredLogs`

### 7.2 日時検索機能（4件）
- `testFindByCreatedAtBetween_WithValidRange_ShouldReturnLogs`
- `testFindTodayLogs_ShouldReturnTodayLogs`
- `testFindLatestLogs_WithLimit_ShouldReturnLimitedResults`
- `testFindLatestLogsByUser_WithValidUser_ShouldReturnUserLogs`

### 7.3 統計情報取得（4件）
- `testGetActionStatistics_WithValidRange_ShouldReturnStatistics`
- `testGetUserActivityStatistics_WithValidRange_ShouldReturnStatistics`
- `testGetIpAccessStatistics_WithValidRange_ShouldReturnStatistics`
- `testGetHourlyActivityStatistics_WithValidRange_ShouldReturnStatistics`

### 7.4 セキュリティ関連（2件）
- `testFindSuspiciousActivity_WithThreshold_ShouldReturnSuspiciousIps`
- `testFindFailedLoginAttempts_WithRecentHours_ShouldReturnFailedAttempts`

### 7.5 検索・フィルタリング（1件）
- `testSearchByKeyword_WithValidKeyword_ShouldReturnMatchingLogs`

### 7.6 集計統計（4件）
- `testCountByActionGrouped_ShouldReturnActionCounts`
- `testCountByStatusGrouped_ShouldReturnStatusCounts`
- `testCountByUserGrouped_ShouldReturnUserCounts`
- `testCountByDateGrouped_ShouldReturnDateCounts`

### 7.7 データ整合性テスト（3件）
- `testSaveAndRetrieve_ShouldMaintainDataIntegrity`
- `testUpdateLog_ShouldReflectChanges`
- `testDeleteLog_ShouldRemoveFromDatabase`

### 7.8 バッチ処理・メンテナンス（4件）
- `testCountByCreatedAtBetween_WithValidRange_ShouldReturnCount`
- `testFindLogsOlderThan_WithCutoffDate_ShouldReturnOldLogs`
- `testCountByCreatedAtBefore_WithCutoffDate_ShouldReturnCount`
- `testFindForBatchProcessing_WithOffsetAndLimit_ShouldReturnLimitedResults`

### 7.9 エッジケース・境界値テスト（4件）
- `testFindByUserId_WithNullUserId_ShouldHandleGracefully`
- `testFindByAction_WithNullAction_ShouldHandleGracefully`
- `testFindByIpAddress_WithNullIp_ShouldHandleGracefully`
- `testFindByCreatedAtBetween_WithInvalidRange_ShouldReturnEmptyList`

### 7.10 パフォーマンステスト（1件）
- `testLargeDatasetQuery_ShouldPerformEfficiently`

## 8. まとめ

### 8.1 システムログテストの重要ポイント
1. **既存データ活用**: system_logsテーブルの既存データを考慮したテスト設計
2. **統計クエリ**: 複雑な集計処理とセキュリティ分析機能の確認
3. **ネイティブクエリ**: PostgreSQL固有のSQL構文とデータベース関数の確認
4. **JSON処理**: detailsフィールドでのJSON形式データの処理確認
5. **セキュリティ機能**: ログ監視と異常検知機能のテスト

### 8.2 テスト品質向上のチェックリスト
- [ ] 実データベース（comsys_test_dump.sql）を使用
- [ ] 外部キー制約を遵守した実在するユーザーIDを使用
- [ ] タイムゾーンは日本時間（JST）で統一
- [ ] JSON詳細情報は有効なJSON文字列を使用
- [ ] 既存データを考慮した検証（>= 比較）
- [ ] null安全性を最初に確認
- [ ] 統計クエリの結果型を適切に検証
- [ ] ネイティブクエリの動作を確認
- [ ] パフォーマンスとメモリ使用量を考慮
- [ ] @Transactionalによる自動ロールバックを活用

### 8.3 他のテストとの違い
| 項目 | システムログテスト | 勤怠記録テスト | 集計データテスト |
|------|-------------------|----------------|------------------|
| **データベース状態** | 既存データあり | 既存データあり | 空テーブル |
| **統計処理** | 複雑な統計クエリ | 基本的な集計 | 数値精度重視 |
| **セキュリティ** | 監視・分析機能 | 基本的な検索 | なし |
| **JSON処理** | 詳細情報JSON | なし | なし |
| **時系列分析** | 時間別統計 | 日付範囲検索 | 期間集計 |
| **ネイティブクエリ** | 多用 | 多用 | 基本的なJPQL |

この手順書に従うことで、システムログデータアクセス層の特性を考慮した包括的で信頼性の高いテストケースを作成できます。特に既存データベース環境での統計クエリ、セキュリティ関連機能、JSON詳細情報処理を適切に扱うことで、実用的なテストスイートを構築できます。