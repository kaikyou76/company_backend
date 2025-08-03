# DataCleanupProcessorTest テストケース作成手順書

## 概要
本書は、`DataCleanupProcessorTest` のテストケース作成における注釈、モック対象、テスト作成の流れとコツを詳細に説明した手順書です。データクリーンアップ処理の特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/batch/processor/DataCleanupProcessorTest.java`

### 1.2 基本アノテーション

#### @ExtendWith(MockitoExtension.class)
**行**: 18
```java
@ExtendWith(MockitoExtension.class)
class DataCleanupProcessorTest {
```

**目的**:
- JUnit 5 でMockitoを統合
- `@Mock` アノテーションの自動初期化
- テストメソッド実行前にモックの初期化を自動実行

**他の処理テストとの違い**:
- 日次処理: AttendanceRecordRepository, HolidayRepository をモック
- 月次処理: AttendanceSummaryRepository をモック
- データクリーンアップ処理: SystemLogRepository をモック（システムログ操作）

### 1.3 モックオブジェクト定義

#### @Mock SystemLogRepository
**行**: 20-21
```java
@Mock
private SystemLogRepository systemLogRepository;
```

**役割**:
- システムログデータの取得処理をモック化
- `countByCreatedAtBefore()` メソッドの戻り値を制御
- 削除対象データ数の統計取得をテスト可能にする
- データクリーンアップ処理の基盤となるデータアクセスをモック化

**他の処理との違い**:
```java
// 日次処理: 打刻データと休日データを扱う
@Mock private AttendanceRecordRepository attendanceRecordRepository;
@Mock private HolidayRepository holidayRepository;

// 月次処理: 集計データを扱う
@Mock private AttendanceSummaryRepository attendanceSummaryRepository;

// データクリーンアップ処理: システムログデータを扱う
@Mock private SystemLogRepository systemLogRepository;
```

#### DataCleanupProcessor インスタンス
**行**: 23, 26-29
```java
private DataCleanupProcessor processor;

@BeforeEach
void setUp() {
    processor = new DataCleanupProcessor();
    processor.setSystemLogRepository(systemLogRepository);
}
```

**特徴**:
- `@InjectMocks` を使用せず手動でセットアップ
- `setSystemLogRepository()` で依存性注入
- テスト実行前に毎回新しいインスタンスを作成

## 2. テストケース詳細解析

### 2.1 テストケース1: 古いデータの削除対象判定
**メソッド**: `testProcess_WithOldData_ShouldReturnForDeletion`
**行**: 31-50

#### テストデータ準備
```java
// 古いシステムログ作成 (行33-39)
SystemLog oldLog = new SystemLog();
oldLog.setId(1L);
oldLog.setUserId(1);
oldLog.setAction("login");
oldLog.setStatus("success");
oldLog.setCreatedAt(OffsetDateTime.now().minusMonths(15)); // 15ヶ月前（削除対象）
```

#### 検証ポイント
```java
// 削除対象として返されることを確認 (行45-48)
assertNotNull(result);
assertEquals(1L, result.getId());
assertEquals("login", result.getAction());
```

**テストの意図**:
- 保持期間（12ヶ月）を超えたデータが削除対象として識別される
- 削除対象データは元のSystemLogオブジェクトがそのまま返される
- 15ヶ月前のデータは確実に削除対象となる

### 2.2 テストケース2: 最近のデータの保持判定
**メソッド**: `testProcess_WithRecentData_ShouldReturnNull`
**行**: 52-67

#### 保持対象データテスト
```java
// 最近のシステムログ作成 (行54-60)
SystemLog recentLog = new SystemLog();
recentLog.setId(2L);
recentLog.setUserId(1);
recentLog.setAction("logout");
recentLog.setStatus("success");
recentLog.setCreatedAt(OffsetDateTime.now().minusMonths(6)); // 6ヶ月前（保持対象）

// 期待結果: null返却 (行65-67)
SystemLog result = processor.process(recentLog);
assertNull(result); // 削除対象外はnullを返す
```

**テストの重要性**:
- 保持期間内のデータが適切に保持される
- 削除対象外データはnullを返すことで処理をスキップ
- 6ヶ月前のデータは保持対象として正しく判定される

### 2.3 テストケース3: 境界値テスト（保持側）
**メソッド**: `testProcess_WithBoundaryData_ShouldReturnNull`
**行**: 69-84

#### 境界値での保持判定
```java
// 境界値データ作成 (行71-77)
SystemLog boundaryLog = new SystemLog();
boundaryLog.setId(3L);
boundaryLog.setUserId(1);
boundaryLog.setAction("access");
boundaryLog.setStatus("success");
boundaryLog.setCreatedAt(OffsetDateTime.now().minusMonths(12).plusDays(1)); // 12ヶ月-1日前（保持対象）

// 期待結果: null返却 (行82-84)
SystemLog result = processor.process(boundaryLog);
assertNull(result); // 境界値は保持対象
```

**境界値テストの重要性**:
- 保持期間の境界での正確な判定確認
- 12ヶ月ちょうどより新しいデータは保持対象
- オフバイワンエラーの防止

### 2.4 テストケース4: 境界値テスト（削除側）
**メソッド**: `testProcess_WithExactBoundaryData_ShouldReturnForDeletion`
**行**: 86-101

#### 境界値での削除判定
```java
// 境界値超過データ作成 (行88-94)
SystemLog exactBoundaryLog = new SystemLog();
exactBoundaryLog.setId(4L);
exactBoundaryLog.setUserId(1);
exactBoundaryLog.setAction("error");
exactBoundaryLog.setStatus("error");
exactBoundaryLog.setCreatedAt(OffsetDateTime.now().minusMonths(12).minusMinutes(1)); // 12ヶ月+1分前（削除対象）

// 期待結果: 削除対象として返却 (行99-101)
SystemLog result = processor.process(exactBoundaryLog);
assertNotNull(result); // 境界値を超えているので削除対象
assertEquals(4L, result.getId());
```

**精密な境界値テスト**:
- 保持期間を1分でも超えた場合の削除判定
- 時間単位での正確な境界値処理確認
- 厳密な時間比較ロジックの検証

### 2.5 テストケース5: 現在データの保持判定
**メソッド**: `testProcess_WithCurrentData_ShouldReturnNull`
**行**: 103-118

#### 現在時刻データの処理
```java
// 現在時刻データ作成 (行105-111)
SystemLog currentLog = new SystemLog();
currentLog.setId(5L);
currentLog.setUserId(1);
currentLog.setAction("current_action");
currentLog.setStatus("success");
currentLog.setCreatedAt(OffsetDateTime.now()); // 現在時刻（保持対象）

// 期待結果: null返却 (行116-118)
SystemLog result = processor.process(currentLog);
assertNull(result); // 現在のデータは保持対象
```

**現在データテストの意義**:
- 最新データの確実な保持
- リアルタイムデータの保護
- 現在進行中の処理データの安全性確保

### 2.6 テストケース6: 削除対象データ数取得
**メソッド**: `testProcess_GetDeleteTargetCount_ShouldReturnCount`
**行**: 120-130

#### 統計情報取得テスト
```java
// モック設定 (行122-124)
when(systemLogRepository.countByCreatedAtBefore(any(OffsetDateTime.class)))
        .thenReturn(1500L);

// 統計取得実行 (行126-130)
long count = processor.getDeleteTargetCount();
assertEquals(1500L, count);
```

**統計機能の重要性**:
- 削除前の事前確認機能
- 大量削除の事前警告
- 処理規模の把握

### 2.7 テストケース7: 例外時の統計取得
**メソッド**: `testGetDeleteTargetCount_WithException_ShouldReturnZero`
**行**: 132-142

#### エラーハンドリングテスト
```java
// 例外発生のモック設定 (行134-136)
when(systemLogRepository.countByCreatedAtBefore(any(OffsetDateTime.class)))
        .thenThrow(new RuntimeException("Database error"));

// 例外時の安全な処理確認 (行138-142)
long count = processor.getDeleteTargetCount();
assertEquals(0L, count);
```

**エラー耐性の確認**:
- データベースエラー時の安全な処理
- 例外時のデフォルト値返却
- システムの堅牢性確保

### 2.8 テストケース8: 設定値取得テスト
**メソッド**: `testGetRetentionMonths_ShouldReturn12`, `testGetCutoffDate_ShouldReturnCorrectDate`
**行**: 144-158

#### 設定値の正確性確認
```java
// 保持期間取得テスト (行144-149)
int retentionMonths = processor.getRetentionMonths();
assertEquals(12, retentionMonths);

// カットオフ日付取得テスト (行151-158)
var cutoffDate = processor.getCutoffDate();
assertNotNull(cutoffDate);
var expectedDate = OffsetDateTime.now().minusMonths(12).toLocalDate();
assertEquals(expectedDate, cutoffDate);
```

**設定値テストの意義**:
- 保持期間設定の正確性確認
- カットオフ日付計算の検証
- 設定変更時の影響範囲確認

### 2.9 テストケース9: null値処理
**メソッド**: `testProcess_WithNullCreatedAt_ShouldHandleGracefully`
**行**: 160-175

#### null値耐性テスト
```java
// null日時データ作成 (行162-168)
SystemLog logWithNullDate = new SystemLog();
logWithNullDate.setId(6L);
logWithNullDate.setUserId(1);
logWithNullDate.setAction("test");
logWithNullDate.setStatus("success");
logWithNullDate.setCreatedAt(null); // null日時

// 例外発生の確認 (行170-175)
assertThrows(Exception.class, () -> {
    processor.process(logWithNullDate);
});
```

**null値処理の重要性**:
- 不正データに対する適切な例外処理
- データ整合性の確保
- 予期しないnull値への対応

### 2.10 テストケース10: 非常に古いデータの処理
**メソッド**: `testProcess_WithVeryOldData_ShouldReturnForDeletion`
**行**: 177-192

#### 極端に古いデータのテスト
```java
// 5年前のデータ作成 (行179-185)
SystemLog veryOldLog = new SystemLog();
veryOldLog.setId(7L);
veryOldLog.setUserId(1);
veryOldLog.setAction("ancient_action");
veryOldLog.setStatus("success");
veryOldLog.setCreatedAt(OffsetDateTime.now().minusYears(5)); // 5年前（削除対象）

// 削除対象確認 (行190-192)
SystemLog result = processor.process(veryOldLog);
assertNotNull(result);
assertEquals(7L, result.getId());
assertEquals("ancient_action", result.getAction());
```

**極端ケーステストの価値**:
- 長期間蓄積されたデータの処理確認
- 年単位での古いデータの適切な削除判定
- システム長期運用時の動作保証

### 2.11 テストケース11: 様々なステータスでの処理
**メソッド**: `testProcess_WithDifferentStatuses_ShouldProcessCorrectly`
**行**: 194-220

#### 多様なステータスデータのテスト
```java
// エラーステータスの古いログ (行196-202)
SystemLog errorLog = new SystemLog();
errorLog.setId(8L);
errorLog.setUserId(1);
errorLog.setAction("failed_action");
errorLog.setStatus("error");
errorLog.setCreatedAt(OffsetDateTime.now().minusMonths(15));

// 警告ステータスの古いログ (行204-210)
SystemLog warningLog = new SystemLog();
warningLog.setId(9L);
warningLog.setUserId(2);
warningLog.setAction("warning_action");
warningLog.setStatus("warning");
warningLog.setCreatedAt(OffsetDateTime.now().minusMonths(18));

// 両方とも削除対象として処理されることを確認 (行215-220)
SystemLog errorResult = processor.process(errorLog);
SystemLog warningResult = processor.process(warningLog);

assertNotNull(errorResult);
assertEquals("error", errorResult.getStatus());

assertNotNull(warningResult);
assertEquals("warning", warningResult.getStatus());
```

**多様性テストの重要性**:
- 異なるステータスでの一貫した処理
- ログの種類に関係ない削除判定
- 包括的なデータ処理の確認

## 3. データクリーンアップ処理特有のテスト戦略

### 3.1 時間ベース判定テストの特徴

#### 保持期間の精密テスト
```java
// 複数の時間パターンでのテスト
@ParameterizedTest
@ValueSource(ints = {6, 11, 12, 13, 18, 24})
void testProcess_VariousMonthsOld_ProcessesCorrectly(int monthsOld) {
    SystemLog log = new SystemLog();
    log.setId(1L);
    log.setCreatedAt(OffsetDateTime.now().minusMonths(monthsOld));
    
    SystemLog result = processor.process(log);
    
    if (monthsOld >= 12) {
        assertNotNull(result); // 12ヶ月以上は削除対象
    } else {
        assertNull(result); // 12ヶ月未満は保持対象
    }
}
```

#### 時間精度テスト
```java
// 分・秒レベルでの境界値テスト
@Test
void testProcess_PreciseTimeBoundary_HandlesCorrectly() {
    OffsetDateTime cutoff = OffsetDateTime.now().minusMonths(12);
    
    // 境界値より1秒新しい
    SystemLog newerLog = createLogWithTime(cutoff.plusSeconds(1));
    assertNull(processor.process(newerLog));
    
    // 境界値より1秒古い
    SystemLog olderLog = createLogWithTime(cutoff.minusSeconds(1));
    assertNotNull(processor.process(olderLog));
}
```

### 3.2 データクリーンアップ固有のエッジケース

#### タイムゾーン考慮テスト
```java
@Test
void testProcess_DifferentTimeZones_HandlesCorrectly() {
    // 異なるタイムゾーンでのデータ処理
    OffsetDateTime utcTime = OffsetDateTime.now(ZoneOffset.UTC).minusMonths(13);
    OffsetDateTime jstTime = OffsetDateTime.now(ZoneOffset.ofHours(9)).minusMonths(13);
    
    SystemLog utcLog = createLogWithTime(utcTime);
    SystemLog jstLog = createLogWithTime(jstTime);
    
    // 両方とも削除対象として処理されることを確認
    assertNotNull(processor.process(utcLog));
    assertNotNull(processor.process(jstLog));
}
```

#### 大量データ統計テスト
```java
@Test
void testGetDeleteTargetCount_LargeNumbers_HandlesCorrectly() {
    // 大量データでの統計処理
    when(systemLogRepository.countByCreatedAtBefore(any(OffsetDateTime.class)))
            .thenReturn(1_000_000L); // 100万件
    
    long count = processor.getDeleteTargetCount();
    assertEquals(1_000_000L, count);
    
    // 大量データでも正確に処理されることを確認
    assertTrue(count > 0);
}
```

## 4. テストケース作成の流れ

### 4.1 データクリーンアップ処理テスト専用フロー
```
1. データクリーンアップ要件定義
   ↓
2. システムログテストデータ準備
   ↓
3. 時間ベース判定テストケース作成
   ↓
4. 境界値テスト（保持期間前後）
   ↓
5. 統計情報取得テスト
   ↓
6. エラーハンドリング・異常系テスト
   ↓
7. パフォーマンス・大量データテスト
```

### 4.2 詳細手順

#### ステップ1: データクリーンアップ要件定義
```java
/**
 * テストケース名: 12ヶ月保持期間でのデータクリーンアップ
 * 目的: 保持期間を超えたシステムログの削除対象判定
 * 入力: 様々な作成日時のSystemLogデータ
 * 期待結果: 12ヶ月以前は削除対象、以内は保持対象
 */
```

#### ステップ2: システムログテストデータ準備
```java
// システムログ生成ヘルパーメソッド
private SystemLog createSystemLogWithAge(int monthsOld, String action, String status) {
    SystemLog log = new SystemLog();
    log.setId(System.currentTimeMillis()); // 一意ID
    log.setUserId(1);
    log.setAction(action);
    log.setStatus(status);
    log.setCreatedAt(OffsetDateTime.now().minusMonths(monthsOld));
    return log;
}

// 使用例
SystemLog oldLog = createSystemLogWithAge(15, "login", "success");
SystemLog recentLog = createSystemLogWithAge(6, "logout", "success");
```

#### ステップ3: 時間ベース判定の段階的検証
```java
// 1. 基本的な削除判定
SystemLog result = processor.process(systemLog);

// 2. 削除対象の場合
if (systemLog.getCreatedAt().isBefore(cutoffDate)) {
    assertNotNull(result);
    assertEquals(systemLog.getId(), result.getId());
    assertEquals(systemLog.getAction(), result.getAction());
}

// 3. 保持対象の場合
else {
    assertNull(result);
}

// 4. 統計情報の確認
long deleteCount = processor.getDeleteTargetCount();
assertTrue(deleteCount >= 0);
```

#### ステップ4: モック設定の最適化
```java
@BeforeEach
void setUp() {
    processor = new DataCleanupProcessor();
    processor.setSystemLogRepository(systemLogRepository);
    
    // デフォルトのモック設定
    lenient().when(systemLogRepository.countByCreatedAtBefore(any(OffsetDateTime.class)))
            .thenReturn(0L);
}
```

## 5. テスト作成のコツとベストプラクティス

### 5.1 データクリーンアップ処理特有の注意点

#### 時間計算の正確性
```java
// 保持期間の定数使用
private static final int RETENTION_MONTHS = 12;

// テストでの時間計算
OffsetDateTime cutoffDate = OffsetDateTime.now().minusMonths(RETENTION_MONTHS);
OffsetDateTime oldDate = cutoffDate.minusDays(1); // 削除対象
OffsetDateTime newDate = cutoffDate.plusDays(1);  // 保持対象

// 時間比較の検証
assertTrue(oldDate.isBefore(cutoffDate));
assertFalse(newDate.isBefore(cutoffDate));
```

#### null値とエラーハンドリング
```java
// null値の適切な処理確認
SystemLog nullDateLog = new SystemLog();
nullDateLog.setCreatedAt(null);

// 例外が適切にスローされることを確認
assertThrows(NullPointerException.class, () -> {
    processor.process(nullDateLog);
});
```

### 5.2 テストデータ生成の効率化

#### ファクトリーメソッドパターン
```java
// システムログ生成ファクトリー
private SystemLog createSystemLog(Long id, int userId, String action, 
                                 String status, OffsetDateTime createdAt) {
    SystemLog log = new SystemLog();
    log.setId(id);
    log.setUserId(userId);
    log.setAction(action);
    log.setStatus(status);
    log.setCreatedAt(createdAt);
    return log;
}

// 時間ベースファクトリー
private SystemLog createOldLog(String action) {
    return createSystemLog(1L, 1, action, "success", 
                          OffsetDateTime.now().minusMonths(15));
}

private SystemLog createRecentLog(String action) {
    return createSystemLog(2L, 1, action, "success", 
                          OffsetDateTime.now().minusMonths(6));
}
```

#### ビルダーパターンの活用
```java
public class SystemLogTestBuilder {
    private SystemLog log = new SystemLog();
    
    public SystemLogTestBuilder id(Long id) {
        log.setId(id);
        return this;
    }
    
    public SystemLogTestBuilder userId(int userId) {
        log.setUserId(userId);
        return this;
    }
    
    public SystemLogTestBuilder action(String action) {
        log.setAction(action);
        return this;
    }
    
    public SystemLogTestBuilder status(String status) {
        log.setStatus(status);
        return this;
    }
    
    public SystemLogTestBuilder createdMonthsAgo(int months) {
        log.setCreatedAt(OffsetDateTime.now().minusMonths(months));
        return this;
    }
    
    public SystemLog build() {
        return log;
    }
}

// 使用例
SystemLog log = new SystemLogTestBuilder()
    .id(1L)
    .userId(1)
    .action("login")
    .status("success")
    .createdMonthsAgo(15)
    .build();
```

### 5.3 モック設定の最適化

#### 条件付きモック設定
```java
@BeforeEach
void setUp() {
    processor = new DataCleanupProcessor();
    processor.setSystemLogRepository(systemLogRepository);
    
    // デフォルトの統計モック
    lenient().when(systemLogRepository.countByCreatedAtBefore(any(OffsetDateTime.class)))
            .thenReturn(100L);
}

// 特定テスト用のモック上書き
@Test
void testLargeDataSet() {
    when(systemLogRepository.countByCreatedAtBefore(any(OffsetDateTime.class)))
            .thenReturn(1_000_000L);
    
    // テスト実行...
}
```

#### パラメータ化テストの活用
```java
@ParameterizedTest
@CsvSource({
    "15, login, success, true",      // 15ヶ月前 → 削除対象
    "6, logout, success, false",     // 6ヶ月前 → 保持対象
    "12, error, error, false",       // 12ヶ月前 → 保持対象
    "13, access, warning, true"      // 13ヶ月前 → 削除対象
})
void testProcess_VariousAges_ProcessesCorrectly(int monthsOld, String action, 
                                               String status, boolean shouldDelete) {
    SystemLog log = createSystemLogWithAge(monthsOld, action, status);
    
    SystemLog result = processor.process(log);
    
    if (shouldDelete) {
        assertNotNull(result);
        assertEquals(log.getId(), result.getId());
    } else {
        assertNull(result);
    }
}
```

## 6. 拡張テストケースの提案

### 6.1 実用的なテストケース

#### 複数ユーザーデータ処理テスト
```java
@Test
void testProcess_MultipleUsers_ProcessesCorrectly() {
    // 複数ユーザーの古いデータ
    List<SystemLog> oldLogs = Arrays.asList(
        createSystemLogForUser(1, 15), // ユーザー1の15ヶ月前データ
        createSystemLogForUser(2, 18), // ユーザー2の18ヶ月前データ
        createSystemLogForUser(3, 24)  // ユーザー3の24ヶ月前データ
    );
    
    List<SystemLog> results = oldLogs.stream()
        .map(log -> {
            try {
                return processor.process(log);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })
        .collect(Collectors.toList());
    
    // 全て削除対象として処理されることを確認
    results.forEach(result -> {
        assertNotNull(result);
    });
}
```

#### 時間境界の詳細テスト
```java
@Test
void testProcess_TimeBoundaryDetails_HandlesCorrectly() {
    OffsetDateTime cutoff = OffsetDateTime.now().minusMonths(12);
    
    // 境界値周辺の詳細テスト
    SystemLog[] testLogs = {
        createLogWithTime(cutoff.minusHours(1)),   // 1時間古い → 削除
        createLogWithTime(cutoff.minusMinutes(1)), // 1分古い → 削除
        createLogWithTime(cutoff),                 // ちょうど → 保持
        createLogWithTime(cutoff.plusMinutes(1)),  // 1分新しい → 保持
        createLogWithTime(cutoff.plusHours(1))     // 1時間新しい → 保持
    };
    
    boolean[] expectedDeletion = {true, true, false, false, false};
    
    for (int i = 0; i < testLogs.length; i++) {
        SystemLog result = processor.process(testLogs[i]);
        if (expectedDeletion[i]) {
            assertNotNull(result, "Index " + i + " should be deleted");
        } else {
            assertNull(result, "Index " + i + " should be retained");
        }
    }
}
```

### 6.2 異常系テストケース

#### データベース接続エラーテスト
```java
@Test
void testGetDeleteTargetCount_DatabaseError_HandlesGracefully() {
    // データベース接続エラーのシミュレーション
    when(systemLogRepository.countByCreatedAtBefore(any(OffsetDateTime.class)))
            .thenThrow(new DataAccessException("Connection timeout") {});
    
    // エラー時の安全な処理確認
    long count = processor.getDeleteTargetCount();
    assertEquals(0L, count);
}
```

#### 不正データ処理テスト
```java
@Test
void testProcess_InvalidData_HandlesCorrectly() {
    // 不正なIDを持つデータ
    SystemLog invalidLog = new SystemLog();
    invalidLog.setId(null); // null ID
    invalidLog.setUserId(1);
    invalidLog.setAction("test");
    invalidLog.setStatus("success");
    invalidLog.setCreatedAt(OffsetDateTime.now().minusMonths(15));
    
    // 不正データでも適切に処理されることを確認
    SystemLog result = processor.process(invalidLog);
    assertNotNull(result); // 削除対象として処理される
    assertNull(result.getId()); // null IDも保持される
}
```

## 7. パフォーマンステスト戦略

### 7.1 処理時間測定
```java
@Test
void testProcess_ProcessingTime_WithinExpectedRange() {
    SystemLog testLog = createSystemLogWithAge(15, "test", "success");
    
    // 複数回実行して平均処理時間を測定
    long totalTime = 0;
    int iterations = 1000;
    
    for (int i = 0; i < iterations; i++) {
        long startTime = System.nanoTime();
        processor.process(testLog);
        long endTime = System.nanoTime();
        totalTime += (endTime - startTime);
    }
    
    long averageTime = totalTime / iterations;
    long averageTimeMs = averageTime / 1_000_000;
    
    // 平均処理時間が1ms以内であることを確認
    assertTrue(averageTimeMs < 1);
}
```

### 7.2 メモリ使用量テスト
```java
@Test
void testProcess_MemoryUsage_WithinLimits() {
    Runtime runtime = Runtime.getRuntime();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // 大量データでの処理
    for (int i = 0; i < 10000; i++) {
        SystemLog log = createSystemLogWithAge(15, "action" + i, "success");
        processor.process(log);
    }
    
    runtime.gc(); // ガベージコレクション実行
    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // メモリ使用量が許容範囲内であることを確認
    long memoryUsed = afterMemory - beforeMemory;
    assertTrue(memoryUsed < 1024 * 1024); // 1MB以内
}
```

## 8. 一般的な問題と解決策

### 8.1 データクリーンアップ処理特有の問題

#### 時間計算の不一致
**問題**: テストの期待値と実際の時間計算結果が一致しない
```java
// 問題のあるコード
OffsetDateTime testTime = OffsetDateTime.now().minusMonths(12);
// 実際の処理では異なる現在時刻が使用される可能性
```

**解決策**:
```java
// 固定時刻を使用したテスト
OffsetDateTime fixedNow = OffsetDateTime.of(2025, 2, 8, 10, 0, 0, 0, ZoneOffset.UTC);
OffsetDateTime testTime = fixedNow.minusMonths(12);

// または、時間の差分のみをテスト
Duration timeDiff = Duration.between(testTime, OffsetDateTime.now());
assertTrue(timeDiff.toDays() > 365); // 1年以上古い
```

#### null値処理の不一致
**問題**: null値に対する期待動作の不明確さ
```java
// 問題のあるコード
SystemLog nullLog = new SystemLog();
nullLog.setCreatedAt(null);
// null値の場合の期待動作が不明確
```

**解決策**:
```java
// 明確な例外処理の確認
assertThrows(NullPointerException.class, () -> {
    processor.process(nullLog);
});

// または、null値の適切な処理確認
SystemLog result = processor.process(nullLog);
// 仕様に応じた適切な検証
```

### 8.2 モック設定の問題

#### 統計情報モックの不整合
**問題**: 統計情報のモック設定が実際の処理と一致しない
```java
// 問題のあるコード
when(systemLogRepository.countByCreatedAtBefore(any()))
    .thenReturn(100L);
// 実際の削除対象データ数と一致しない可能性
```

**解決策**:
```java
// 一貫性のあるモック設定
OffsetDateTime cutoff = OffsetDateTime.now().minusMonths(12);
when(systemLogRepository.countByCreatedAtBefore(eq(cutoff)))
    .thenReturn(expectedCount);

// または、any()を使用して柔軟に対応
when(systemLogRepository.countByCreatedAtBefore(any(OffsetDateTime.class)))
    .thenReturn(expectedCount);
```

## 9. まとめ

### 9.1 データクリーンアップ処理テストの重要ポイント
1. **時間ベース判定**: 保持期間に基づく正確な削除判定
2. **境界値処理**: 保持期間境界での精密な判定
3. **統計情報**: 削除対象データ数の正確な取得
4. **エラーハンドリング**: null値や例外に対する適切な処理
5. **パフォーマンス**: 大量データでの効率的な処理

### 9.2 テスト品質向上のチェックリスト
- [ ] 正常系・異常系・境界値テストを網羅
- [ ] 様々な時間パターンでのテスト
- [ ] null値処理の適切な検証
- [ ] モック設定は必要最小限で一貫性を保持
- [ ] テストデータは現実的なシステムログパターンを使用
- [ ] 統計情報取得の正確性を確認
- [ ] パフォーマンスとメモリ使用量を考慮
- [ ] 時間計算の精度を検証

### 9.3 他の処理テストとの違い
| 項目 | 日次処理テスト | 月次処理テスト | データクリーンアップ処理テスト |
|------|----------------|----------------|-----------------------------|
| **入力データ** | AttendanceRecord | AttendanceSummary(daily) | SystemLog |
| **モック対象** | AttendanceRecordRepository, HolidayRepository | AttendanceSummaryRepository | SystemLogRepository |
| **テスト焦点** | 時間計算ロジック | データ集計ロジック | 時間ベース削除判定 |
| **検証項目** | 打刻ペア処理、深夜・休日計算 | 日次データ合計、重複チェック | 保持期間判定、統計取得、削除対象識別 |
| **特殊考慮** | 打刻データの整合性 | 月境界処理 | 時間境界処理、大量データ統計 |

この手順書に従うことで、データクリーンアップ処理の特性を考慮した包括的で信頼性の高いテストケースを作成できます。システムの長期運用におけるデータ管理の品質保証が実現されます。