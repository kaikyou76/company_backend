# BatchStatusServiceTest テストケース作成手順書

## 概要
本書は、`BatchStatusServiceTest` のテストケース作成における注釈、モック対象、テスト作成の流れとコツを詳細に説明した手順書です。バッチ監視サービスの特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/service/BatchStatusServiceTest.java`

### 1.2 基本アノテーション

#### @ExtendWith(MockitoExtension.class)
**行**: 21
```java
@ExtendWith(MockitoExtension.class)
class BatchStatusServiceTest {
```

**目的**:
- JUnit 5 でMockitoを統合
- `@InjectMocks` アノテーションの自動初期化
- テストメソッド実行前にモックの初期化を自動実行

**バッチ監視サービステストの特徴**:
- 外部依存なしの単体サービステスト
- 時刻依存処理の制御とテスト
- システム監視指標の検証
- パフォーマンス測定とレスポンス時間監視

### 1.3 依存性注入定義

#### @InjectMocks BatchStatusService
**行**: 24-25
```java
@InjectMocks
private BatchStatusService batchStatusService;
```

**役割**:
- BatchStatusServiceの直接テスト
- 外部Repositoryに依存しない独立したサービス
- モックなしでの実際のビジネスロジック検証
- システム監視機能の統合テスト

**他のサービステストとの違い**:
```java
// 一般的なサービステスト: 複数のRepositoryをモック
@Mock private SomeRepository someRepository;
@InjectMocks private SomeService someService;

// BatchStatusServiceTest: 外部依存なしの直接テスト
@InjectMocks private BatchStatusService batchStatusService;
```

### 1.4 テスト用定数定義

#### 監視指標関連定数
**行**: 27-35
```java
private static final String EXPECTED_SYSTEM_STATUS = "HEALTHY";
private static final String EXPECTED_UPTIME = "5 days, 12 hours";
private static final int EXPECTED_TOTAL_USERS = 50;
private static final int EXPECTED_ACTIVE_USERS = 48;
private static final int EXPECTED_TOTAL_RECORDS = 12450;
private static final String EXPECTED_LATEST_RECORD_DATE = "2025-01-18";
private static final int EXPECTED_CURRENT_MONTH_RECORDS = 520;
private static final int EXPECTED_INCOMPLETE_RECORDS = 2;
```

**設計思想**:
- **監視基準値**: システム健全性を示す期待値を定義
- **現実的な数値**: 実際の運用環境を想定した値
- **健全性指標**: アクティブユーザー率96%、不完全レコード率0.4%

## 2. 主要テストケース解析

### 2.1 バッチステータス取得テスト群

#### テストケース1: 完全なステータスレスポンス取得
**メソッド**: `testGetBatchStatus_ShouldReturnCompleteStatusResponse`
**行**: 43-56

##### 基本レスポンス検証
```java
// When
BatchStatusResponse result = batchStatusService.getBatchStatus();

// Then
assertNotNull(result);
assertEquals(EXPECTED_SYSTEM_STATUS, result.getSystemStatus());
assertNotNull(result.getLastChecked());
assertEquals(EXPECTED_UPTIME, result.getUptime());
```

##### 時刻精度検証
```java
// 現在時刻との差が1秒以内であることを確認
LocalDateTime now = LocalDateTime.now();
long secondsDiff = ChronoUnit.SECONDS.between(result.getLastChecked(), now);
assertTrue(Math.abs(secondsDiff) <= 1, "LastChecked should be within 1 second of current time");
```

**時刻精度テストの重要性**:
- リアルタイム監視システムでは時刻精度が重要
- 1秒以内の精度でシステム状態を把握
- 監視ダッシュボードでの正確な表示保証

#### テストケース2: データベースステータス検証
**メソッド**: `testGetBatchStatus_DatabaseStatus_ShouldReturnCorrectValues`
**行**: 58-73

##### データベース指標検証
```java
DatabaseStatus dbStatus = result.getDatabaseStatus();

assertEquals(EXPECTED_TOTAL_USERS, dbStatus.getTotalUsers());
assertEquals(EXPECTED_ACTIVE_USERS, dbStatus.getActiveUsers());
assertEquals(EXPECTED_TOTAL_RECORDS, dbStatus.getTotalAttendanceRecords());
assertEquals(EXPECTED_LATEST_RECORD_DATE, dbStatus.getLatestRecordDate());
```

##### 健全性指標計算
```java
// アクティブユーザー率の確認
double activeUserRate = (double) dbStatus.getActiveUsers() / dbStatus.getTotalUsers();
assertTrue(activeUserRate >= 0.9, "Active user rate should be at least 90%");
```

**健全性指標の意味**:
- アクティブユーザー率90%以上: システムが正常に利用されている
- 総記録数の妥当性: データ蓄積が適切に行われている
- 最新記録日付: データ更新の継続性確認

#### テストケース3: データ統計情報検証
**メソッド**: `testGetBatchStatus_DataStatistics_ShouldReturnCorrectValues`
**行**: 75-88

##### データ品質指標
```java
DataStatistics dataStats = result.getDataStatistics();

assertEquals(EXPECTED_CURRENT_MONTH_RECORDS, dataStats.getCurrentMonthRecords());
assertEquals(EXPECTED_INCOMPLETE_RECORDS, dataStats.getIncompleteRecords());

// 不完全レコード率の確認
double incompleteRate = (double) dataStats.getIncompleteRecords() / dataStats.getCurrentMonthRecords();
assertTrue(incompleteRate < 0.05, "Incomplete record rate should be less than 5%");
```

**データ品質監視**:
- 不完全レコード率5%未満: データ品質が良好
- 当月記録数の妥当性: 業務活動の正常性確認
- データ整合性の継続監視

### 2.2 バッチ実行履歴テスト群

#### テストケース4: 実行履歴の妥当性検証
**メソッド**: `testGetBatchStatus_RecentBatchExecutions_ShouldReturnValidHistory`
**行**: 90-113

##### 履歴データ構造検証
```java
List<BatchExecutionHistory> executions = result.getRecentBatchExecutions();

assertFalse(executions.isEmpty(), "Recent batch executions should not be empty");
assertTrue(executions.size() >= 2, "Should have at least 2 recent executions");

// 各実行履歴の検証
for (BatchExecutionHistory execution : executions) {
    assertNotNull(execution.getType(), "Execution type should not be null");
    assertNotNull(execution.getExecutedAt(), "Execution time should not be null");
    assertNotNull(execution.getStatus(), "Execution status should not be null");
    assertNotNull(execution.getDuration(), "Execution duration should not be null");
    
    // ステータスが有効な値であることを確認
    assertTrue(execution.getStatus().equals("SUCCESS") || 
              execution.getStatus().equals("FAILED") || 
              execution.getStatus().equals("RUNNING"),
              "Status should be SUCCESS, FAILED, or RUNNING");
}
```

#### テストケース5: 実行時刻順ソート確認
**メソッド**: `testGetBatchStatus_RecentBatchExecutions_ShouldBeSortedByExecutionTime`
**行**: 115-128

##### 時系列ソート検証
```java
if (executions.size() > 1) {
    // 実行時刻が降順（新しい順）でソートされていることを確認
    for (int i = 0; i < executions.size() - 1; i++) {
        LocalDateTime current = executions.get(i).getExecutedAt();
        LocalDateTime next = executions.get(i + 1).getExecutedAt();
        assertTrue(current.isAfter(next) || current.isEqual(next),
                  "Executions should be sorted by execution time in descending order");
    }
}
```

**ソート検証の重要性**:
- 最新の実行状況を優先表示
- 監視ダッシュボードでの時系列表示
- 問題発生時の迅速な原因特定

### 2.3 稼働時間計算テスト群

#### テストケース6: 稼働時間形式検証
**メソッド**: `testCalculateUptime_ShouldReturnValidFormat`
**行**: 132-144

##### 形式パターン検証
```java
String uptime = batchStatusService.calculateUptime();

assertNotNull(uptime);
assertFalse(uptime.trim().isEmpty(), "Uptime should not be empty");
assertEquals(EXPECTED_UPTIME, uptime);

// 稼働時間の形式確認（"X days, Y hours" 形式）
assertTrue(uptime.matches("\\d+ days?, \\d+ hours?"), 
          "Uptime should match format 'X days, Y hours'");
```

#### テストケース7: 異なる時間形式処理
**メソッド**: `testCalculateUptime_ShouldHandleDifferentTimeFormats`
**行**: 146-162

##### 多様な稼働時間パターン
```java
BatchStatusService spyService = spy(batchStatusService);

// 1日未満の場合
when(spyService.calculateUptime()).thenReturn("0 days, 8 hours");
String shortUptime = spyService.calculateUptime();
assertTrue(shortUptime.matches("\\d+ days?, \\d+ hours?"));

// 長期間の場合
when(spyService.calculateUptime()).thenReturn("365 days, 0 hours");
String longUptime = spyService.calculateUptime();
assertTrue(longUptime.matches("\\d+ days?, \\d+ hours?"));
```

### 2.4 システム健全性監視テスト

#### テストケース8: システム健全性指標
**メソッド**: `testGetBatchStatus_SystemHealth_ShouldIndicateHealthyState`
**行**: 220-245

##### 複合健全性評価
```java
assertEquals("HEALTHY", result.getSystemStatus());

// システムが健全であることを示す指標の確認
DatabaseStatus dbStatus = result.getDatabaseStatus();
DataStatistics dataStats = result.getDataStatistics();

// アクティブユーザー率が90%以上
double activeUserRate = (double) dbStatus.getActiveUsers() / dbStatus.getTotalUsers();
assertTrue(activeUserRate >= 0.9, "Active user rate should indicate healthy system");

// 不完全レコード率が5%未満
double incompleteRate = (double) dataStats.getIncompleteRecords() / dataStats.getCurrentMonthRecords();
assertTrue(incompleteRate < 0.05, "Low incomplete record rate should indicate healthy system");

// 最近のバッチ実行が成功していること
List<BatchExecutionHistory> executions = result.getRecentBatchExecutions();
boolean allSuccessful = executions.stream()
        .allMatch(exec -> "SUCCESS".equals(exec.getStatus()));
assertTrue(allSuccessful, "All recent batch executions should be successful for healthy system");
```

**健全性評価の多面性**:
- ユーザー活動率: システム利用状況
- データ品質率: 処理精度
- バッチ成功率: 自動処理の安定性

### 2.5 時刻制御テスト

#### テストケース9: モック時刻での処理確認
**メソッド**: `testGetBatchStatus_WithMockedCurrentTime_ShouldHandleTimeCorrectly`
**行**: 290-302

##### 静的メソッドモック
```java
LocalDateTime fixedTime = LocalDateTime.of(2025, 2, 1, 12, 0, 0);

try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
    mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedTime);
    
    // When
    BatchStatusResponse result = batchStatusService.getBatchStatus();
    
    // Then
    assertEquals(fixedTime, result.getLastChecked());
    mockedLocalDateTime.verify(LocalDateTime::now, atLeastOnce());
}
```

**静的メソッドモックの重要性**:
- 時刻依存処理の確定的テスト
- 現在時刻の制御による再現可能なテスト
- タイムゾーン問題の回避

### 2.6 パフォーマンステスト

#### テストケース10: レスポンス時間測定
**メソッド**: `testGetBatchStatus_PerformanceTest_ShouldCompleteQuickly`
**行**: 320-334

##### 性能要件検証
```java
long startTime = System.currentTimeMillis();

BatchStatusResponse result = batchStatusService.getBatchStatus();

long endTime = System.currentTimeMillis();
long executionTime = endTime - startTime;

assertNotNull(result);
assertTrue(executionTime < 1000, 
          "getBatchStatus should complete within 1 second, took: " + executionTime + "ms");
```

**パフォーマンス要件**:
- ステータス取得: 1秒以内
- 履歴取得: 500ms以内
- 監視ダッシュボードの応答性保証

## 3. テスト作成のベストプラクティス

### 3.1 バッチ監視特有の注意点

#### 時刻精度の管理
```java
// 問題のあるコード（時刻のずれが発生する可能性）
assertEquals(LocalDateTime.now(), result.getLastChecked()); // 実行タイミングでずれる

// 改善されたコード（許容範囲での検証）
LocalDateTime now = LocalDateTime.now();
long secondsDiff = ChronoUnit.SECONDS.between(result.getLastChecked(), now);
assertTrue(Math.abs(secondsDiff) <= 1); // 1秒以内の精度
```

#### 健全性指標の計算
```java
// 比率計算での0除算回避
double activeUserRate = dbStatus.getTotalUsers() > 0 ? 
    (double) dbStatus.getActiveUsers() / dbStatus.getTotalUsers() : 0.0;

// 閾値による健全性判定
assertTrue(activeUserRate >= 0.9, "Active user rate should indicate healthy system");
```

### 3.2 モック戦略の最適化

#### 静的メソッドモックの活用
```java
// LocalDateTime.now()の制御
try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
    LocalDateTime fixedTime = LocalDateTime.of(2025, 2, 1, 12, 0, 0);
    mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedTime);
    
    // テスト実行
    BatchStatusResponse result = batchStatusService.getBatchStatus();
    
    // 検証
    assertEquals(fixedTime, result.getLastChecked());
}
```

#### spyオブジェクトの部分モック
```java
// 一部メソッドのみモック化
BatchStatusService spyService = spy(batchStatusService);
when(spyService.calculateUptime()).thenReturn("test uptime");

// 他のメソッドは実際の実装を使用
BatchStatusResponse result = spyService.getBatchStatus();
```

### 3.3 データ検証の段階化

#### 段階的検証アプローチ
```java
// 段階1: null安全性
assertNotNull(result);
assertNotNull(result.getDatabaseStatus());
assertNotNull(result.getDataStatistics());

// 段階2: 基本値検証
assertEquals(EXPECTED_SYSTEM_STATUS, result.getSystemStatus());
assertEquals(EXPECTED_UPTIME, result.getUptime());

// 段階3: 計算値検証
double activeUserRate = (double) dbStatus.getActiveUsers() / dbStatus.getTotalUsers();
assertTrue(activeUserRate >= 0.9);

// 段階4: 複合条件検証
boolean systemHealthy = "HEALTHY".equals(result.getSystemStatus()) && 
                       activeUserRate >= 0.9 && 
                       incompleteRate < 0.05;
assertTrue(systemHealthy);
```

## 4. 一般的な問題と解決策

### 4.1 時刻関連の問題

#### 時刻同期の問題
**問題**: テスト実行時刻とアプリケーション内部時刻のずれ

**解決策**:
```java
// 許容範囲での時刻比較
LocalDateTime beforeCall = LocalDateTime.now();
BatchStatusResponse result = batchStatusService.getBatchStatus();
LocalDateTime afterCall = LocalDateTime.now();

assertTrue(result.getLastChecked().isAfter(beforeCall.minusSeconds(1)) && 
          result.getLastChecked().isBefore(afterCall.plusSeconds(1)));
```

#### タイムゾーンの問題
**問題**: 異なる環境でのタイムゾーン差異

**解決策**:
```java
// 固定時刻での検証
LocalDateTime fixedTime = LocalDateTime.of(2025, 2, 1, 12, 0, 0);
try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
    mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedTime);
    // テスト実行
}
```

### 4.2 パフォーマンステストの問題

#### 環境依存の性能差
**問題**: 実行環境によるパフォーマンス差異

**解決策**:
```java
// 複数回実行での平均時間測定
long totalTime = 0;
int iterations = 10;

for (int i = 0; i < iterations; i++) {
    long startTime = System.currentTimeMillis();
    batchStatusService.getBatchStatus();
    long endTime = System.currentTimeMillis();
    totalTime += (endTime - startTime);
}

long averageTime = totalTime / iterations;
assertTrue(averageTime < 100, "Average response time should be under 100ms");
```

## 5. まとめ

### 5.1 バッチ監視サービステストの重要ポイント
1. **時刻精度管理**: リアルタイム監視での正確な時刻処理
2. **健全性指標**: 複数指標による総合的なシステム状態評価
3. **履歴管理**: バッチ実行履歴の正確な記録と表示
4. **パフォーマンス**: 監視ダッシュボードの応答性保証
5. **データ整合性**: 監視データの正確性と一貫性確認

### 5.2 テスト品質向上のチェックリスト
- [ ] 時刻精度は1秒以内で検証
- [ ] 健全性指標は適切な閾値で評価
- [ ] パフォーマンス要件を満たすことを確認
- [ ] 静的メソッドモックを適切に使用
- [ ] データ検証は段階的に実施
- [ ] 複数回呼び出しでの一貫性を確認
- [ ] エラーハンドリングを適切にテスト

### 5.3 他のサービステストとの違い
| 項目 | バッチ監視サービステスト | 一般的なサービステスト |
|------|------------------------|----------------------|
| **外部依存** | なし（独立サービス） | 複数Repository依存 |
| **時刻依存性** | 高（リアルタイム監視） | 中程度 |
| **パフォーマンス** | 重要（監視応答性） | 一般的 |
| **健全性評価** | 複合指標による評価 | 単一機能の検証 |
| **モック戦略** | 静的メソッドモック中心 | Repositoryモック中心 |

この手順書に従うことで、バッチ監視サービスの特性を考慮した包括的で信頼性の高いテストケースを作成できます。