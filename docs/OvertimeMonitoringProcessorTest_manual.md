# OvertimeMonitoringProcessorTest テストケース作成手順書

## 概要
本書は、`OvertimeMonitoringProcessorTest` のテストケース作成における注釈、モック対象、テスト作成の流れとコツを詳細に説明した手順書です。残業監視処理の特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/batch/processor/OvertimeMonitoringProcessorTest.java`

### 1.2 基本アノテーション

#### @ExtendWith(MockitoExtension.class)
**行**: 18
```java
@ExtendWith(MockitoExtension.class)
class OvertimeMonitoringProcessorTest {
```

**目的**:
- JUnit 5 でMockitoを統合
- `@Mock` アノテーションの自動初期化
- テストメソッド実行前にモックの初期化を自動実行

**他の処理テストとの違い**:
- 日次処理: AttendanceRecordRepository をモック
- 月次処理: AttendanceSummaryRepository をモック
- 残業監視処理: OvertimeReportRepository をモック（残業レポート操作）

### 1.3 モックオブジェクト定義

#### @Mock OvertimeReportRepository
**行**: 20-21
```java
@Mock
private OvertimeReportRepository overtimeReportRepository;
```

**役割**:
- 既存残業レポートの取得処理をモック化
- `findByUserIdAndTargetMonth()` メソッドの戻り値を制御
- 残業レポートの保存処理をテスト可能にする
- 重複チェック機能のテスト支援

**他の処理との違い**:
```java
// 日次処理: 打刻データと休日データを扱う
@Mock private AttendanceRecordRepository attendanceRecordRepository;
@Mock private HolidayRepository holidayRepository;

// 月次処理: 集計データを扱う
@Mock private AttendanceSummaryRepository attendanceSummaryRepository;

// 残業監視処理: 残業レポートデータを扱う
@Mock private OvertimeReportRepository overtimeReportRepository;
```

#### OvertimeMonitoringProcessor インスタンス
**行**: 23, 26-29
```java
private OvertimeMonitoringProcessor processor;

@BeforeEach
void setUp() {
    processor = new OvertimeMonitoringProcessor();
    processor.setOvertimeReportRepository(overtimeReportRepository);
}
```

**特徴**:
- `@InjectMocks` を使用せず手動でセットアップ
- `setOvertimeReportRepository()` で依存性注入
- テスト実行前に毎回新しいインスタンスを作成

## 2. テストケース詳細解析

### 2.1 テストケース1: 正常な残業レポート作成
**メソッド**: `testProcess_WithMonthlySummary_ShouldCreateOvertimeReport`
**行**: 31-60

#### テストデータ準備
```java
// 月次集計データ作成 (行33-39)
AttendanceSummary monthlySummary = new AttendanceSummary();
monthlySummary.setUserId(1);
monthlySummary.setSummaryType("monthly");
monthlySummary.setTargetDate(LocalDate.of(2025, 2, 1));
monthlySummary.setOvertimeHours(new BigDecimal("30.00"));
monthlySummary.setLateNightHours(new BigDecimal("10.00"));
monthlySummary.setHolidayHours(new BigDecimal("5.00"));
```

#### 既存レポートなしのモック設定
```java
// 既存の残業レポートなしのモック (行41-43)
when(overtimeReportRepository.findByUserIdAndTargetMonth(eq(1), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());
```

#### 検証ポイント
```java
// 基本情報検証 (行48-50)
assertNotNull(result);
assertEquals(1, result.getUserId());
assertEquals(LocalDate.of(2025, 2, 1), result.getTargetMonth());

// 残業データ検証 (行51-53)
assertEquals(new BigDecimal("30.00"), result.getTotalOvertime());
assertEquals(new BigDecimal("10.00"), result.getTotalLateNight());
assertEquals(new BigDecimal("5.00"), result.getTotalHoliday());

// ステータス検証 (行54)
assertEquals("draft", result.getStatus()); // 閾値以下なのでdraft
```

### 2.2 テストケース2: 高残業時間での確認ステータス
**メソッド**: `testProcess_WithHighOvertimeHours_ShouldSetConfirmedStatus`
**行**: 62-85

#### 閾値超過テスト
```java
// 閾値超過データ作成 (行64-70)
AttendanceSummary monthlySummary = new AttendanceSummary();
monthlySummary.setUserId(1);
monthlySummary.setSummaryType("monthly");
monthlySummary.setTargetDate(LocalDate.of(2025, 2, 1));
monthlySummary.setOvertimeHours(new BigDecimal("50.00")); // 閾値45時間を超過
monthlySummary.setLateNightHours(new BigDecimal("15.00"));
monthlySummary.setHolidayHours(new BigDecimal("8.00"));
```

**テストの意図**:
- 残業時間が45時間を超過した場合の処理確認
- 自動的に \"confirmed\" ステータスが設定されることを検証
- 労働基準法に基づく監視機能のテスト

### 2.3 テストケース3: 深夜労働時間閾値超過
**メソッド**: `testProcess_WithHighLateNightHours_ShouldSetConfirmedStatus`
**行**: 87-110

#### 深夜労働監視テスト
```java
// 深夜労働時間閾値超過データ (行89-95)
AttendanceSummary monthlySummary = new AttendanceSummary();
monthlySummary.setUserId(1);
monthlySummary.setSummaryType("monthly");
monthlySummary.setTargetDate(LocalDate.of(2025, 2, 1));
monthlySummary.setOvertimeHours(new BigDecimal("20.00"));
monthlySummary.setLateNightHours(new BigDecimal("25.00")); // 閾値20時間を超過
monthlySummary.setHolidayHours(new BigDecimal("5.00"));
```

**健康管理の観点**:
- 深夜労働時間（22:00-05:00）の監視
- 健康への影響を考慮した閾値設定
- 自動アラート機能のテスト

### 2.4 テストケース4: 残業時間なしでの承認ステータス
**メソッド**: `testProcess_WithNoOvertimeHours_ShouldSetApprovedStatus`
**行**: 112-135

#### 正常勤務パターンテスト
```java
// 残業時間なしデータ (行114-120)
AttendanceSummary monthlySummary = new AttendanceSummary();
monthlySummary.setUserId(1);
monthlySummary.setSummaryType("monthly");
monthlySummary.setTargetDate(LocalDate.of(2025, 2, 1));
monthlySummary.setOvertimeHours(BigDecimal.ZERO);
monthlySummary.setLateNightHours(BigDecimal.ZERO);
monthlySummary.setHolidayHours(BigDecimal.ZERO);
```

**テストの重要性**:
- 正常な勤務パターンの確認
- \"approved\" ステータスの自動設定
- 健全な労働環境の証明

### 2.5 テストケース5: 既存レポート更新
**メソッド**: `testProcess_WithExistingReport_ShouldUpdateExistingReport`
**行**: 137-170

#### 更新処理テスト
```java
// 既存レポートの準備 (行149-154)
OvertimeReport existingReport = new OvertimeReport();
existingReport.setId(1L);
existingReport.setUserId(1);
existingReport.setTargetMonth(LocalDate.of(2025, 2, 1));
existingReport.setCreatedAt(OffsetDateTime.now().minusDays(1));

// 既存レポートありのモック設定 (行156-158)
when(overtimeReportRepository.findByUserIdAndTargetMonth(eq(1), any(LocalDate.class)))
        .thenReturn(Arrays.asList(existingReport));
```

#### 更新処理の検証
```java
// 既存レポートのIDが保持されることを確認 (行165)
assertEquals(1L, result.getId());

// 更新日時が設定されることを確認 (行169)
assertNotNull(result.getUpdatedAt());
```

**データ整合性の確保**:
- 重複データの防止
- 既存データの適切な更新
- 作成日時と更新日時の管理

### 2.6 テストケース6: 日次サマリーのスキップ
**メソッド**: `testProcess_WithDailySummary_ShouldReturnNull`
**行**: 172-184

#### 処理対象外データのテスト
```java
// 日次サマリーデータ作成 (行174-178)
AttendanceSummary dailySummary = new AttendanceSummary();
dailySummary.setUserId(1);
dailySummary.setSummaryType("daily"); // 日次サマリー
dailySummary.setTargetDate(LocalDate.of(2025, 2, 1));

// 期待結果: null返却 (行182-184)
OvertimeReport result = processor.process(dailySummary);
assertNull(result); // 日次サマリーは処理対象外
```

**処理効率の確保**:
- 不要な処理のスキップ
- 月次データのみを対象とする仕様確認

### 2.7 テストケース7: null値処理
**メソッド**: `testProcess_WithNullValues_ShouldHandleGracefully`
**行**: 186-210

#### null値耐性テスト
```java
// null値を含む月次サマリー (行188-194)
AttendanceSummary monthlySummary = new AttendanceSummary();
monthlySummary.setUserId(1);
monthlySummary.setSummaryType("monthly");
monthlySummary.setTargetDate(LocalDate.of(2025, 2, 1));
monthlySummary.setOvertimeHours(null);
monthlySummary.setLateNightHours(null);
monthlySummary.setHolidayHours(null);
```

#### null値処理の検証
```java
// null値が0として処理されることを確認 (行205-209)
assertNotNull(result);
assertEquals(BigDecimal.ZERO, result.getTotalOvertime());
assertEquals(BigDecimal.ZERO, result.getTotalLateNight());
assertEquals(BigDecimal.ZERO, result.getTotalHoliday());
assertEquals("approved", result.getStatus()); // null値は0として扱われるのでapproved
```

### 2.8 テストケース8: 休日労働時間閾値超過
**メソッド**: `testProcess_WithHighHolidayHours_ShouldSetConfirmedStatus`
**行**: 212-235

#### 休日労働監視テスト
```java
// 休日労働時間閾値超過データ (行214-220)
AttendanceSummary monthlySummary = new AttendanceSummary();
monthlySummary.setUserId(1);
monthlySummary.setSummaryType("monthly");
monthlySummary.setTargetDate(LocalDate.of(2025, 2, 1));
monthlySummary.setOvertimeHours(new BigDecimal("20.00"));
monthlySummary.setLateNightHours(new BigDecimal("10.00"));
monthlySummary.setHolidayHours(new BigDecimal("20.00")); // 閾値15時間を超過
```

**労働基準法遵守**:
- 休日労働の過度な発生を監視
- 法定休日での労働時間制限
- ワークライフバランスの確保

## 3. 残業監視処理特有のテスト戦略

### 3.1 閾値監視テストの特徴

#### 複数閾値の組み合わせテスト
```java
// 複数閾値を同時に超過するケース
@Test
void testProcess_MultipleThresholdExceeded_ShouldSetConfirmedStatus() {
    AttendanceSummary summary = new AttendanceSummary();
    summary.setOvertimeHours(new BigDecimal("50.00")); // 残業閾値超過
    summary.setLateNightHours(new BigDecimal("25.00")); // 深夜閾値超過
    summary.setHolidayHours(new BigDecimal("20.00"));   // 休日閾値超過
    
    OvertimeReport result = processor.process(summary);
    
    assertEquals("confirmed", result.getStatus()); // 複数閾値超過でもconfirmed
}
```

#### 境界値テスト
```java
// 閾値ちょうどのケース
@Test
void testProcess_ExactThreshold_ShouldSetDraftStatus() {
    AttendanceSummary summary = new AttendanceSummary();
    summary.setOvertimeHours(new BigDecimal("45.00")); // 閾値ちょうど
    
    OvertimeReport result = processor.process(summary);
    
    assertEquals("draft", result.getStatus()); // 閾値ちょうどはdraft
}
```

### 3.2 ステータス判定ロジックのテスト

#### ステータス判定の優先順位テスト
```java
// ステータス判定の優先順位確認
@Test
void testDetermineOvertimeStatus_PriorityOrder() {
    // 1. 閾値超過 → confirmed（最優先）
    // 2. 残業時間存在 → draft
    // 3. 残業時間なし → approved
    
    // テストケースの実装...
}
```

#### 精密な閾値計算テスト
```java
// 小数点を含む閾値計算
@Test
void testProcess_DecimalThreshold_CalculatesCorrectly() {
    AttendanceSummary summary = new AttendanceSummary();
    summary.setOvertimeHours(new BigDecimal("44.99")); // 閾値未満
    
    OvertimeReport result = processor.process(summary);
    
    assertEquals("draft", result.getStatus()); // 44.99は閾値未満
}
```

### 3.3 レポート更新ロジックのテスト

#### 更新タイムスタンプの検証
```java
@Test
void testProcess_UpdateTimestamp_SetsCorrectly() {
    // 既存レポートの更新時刻検証
    OffsetDateTime beforeUpdate = OffsetDateTime.now();
    
    OvertimeReport result = processor.process(monthlySummary);
    
    assertTrue(result.getUpdatedAt().isAfter(beforeUpdate));
}
```

#### 部分更新の検証
```java
@Test
void testProcess_PartialUpdate_PreservesOtherFields() {
    // 既存レポートの他のフィールドが保持されることを確認
    OvertimeReport existingReport = createExistingReport();
    existingReport.setCustomField("preserved_value");
    
    OvertimeReport result = processor.process(monthlySummary);
    
    assertEquals("preserved_value", result.getCustomField());
}
```

## 4. テストケース作成の流れ

### 4.1 残業監視処理テスト専用フロー
```
1. 残業監視要件定義
   ↓
2. 月次集計テストデータ準備
   ↓
3. 既存レポートチェックのモック設定
   ↓
4. 閾値判定テストケース作成
   ↓
5. ステータス判定検証
   ↓
6. レポート更新処理確認
   ↓
7. エッジケース・異常系追加
```

### 4.2 詳細手順

#### ステップ1: 残業監視要件定義
```java
/**
 * テストケース名: 高残業時間での監視処理
 * 目的: 月45時間超過時の自動確認ステータス設定
 * 入力: 残業時間50時間の月次集計データ
 * 期待結果: status="confirmed", 適切なアラート生成
 */
```

#### ステップ2: 月次集計テストデータ準備
```java
// 残業監視用データ生成ヘルパーメソッド
private AttendanceSummary createMonthlySummaryWithOvertime(int userId, String overtimeHours, 
                                                          String lateNightHours, String holidayHours) {
    AttendanceSummary summary = new AttendanceSummary();
    summary.setUserId(userId);
    summary.setSummaryType("monthly");
    summary.setTargetDate(LocalDate.of(2025, 2, 1));
    summary.setOvertimeHours(new BigDecimal(overtimeHours));
    summary.setLateNightHours(new BigDecimal(lateNightHours));
    summary.setHolidayHours(new BigDecimal(holidayHours));
    return summary;
}
```

#### ステップ3: モック設定の階層化
```java
// 1. 既存レポートなしのケース
when(overtimeReportRepository.findByUserIdAndTargetMonth(eq(userId), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());

// 2. 既存レポートありのケース
OvertimeReport existingReport = createExistingReport(userId);
when(overtimeReportRepository.findByUserIdAndTargetMonth(eq(userId), any(LocalDate.class)))
        .thenReturn(Arrays.asList(existingReport));
```

#### ステップ4: 段階的検証
```java
// 1. 基本プロパティ検証
assertNotNull(result);
assertEquals(expectedUserId, result.getUserId());
assertEquals(expectedTargetMonth, result.getTargetMonth());

// 2. 残業データ検証
assertEquals(expectedOvertimeHours, result.getTotalOvertime());
assertEquals(expectedLateNightHours, result.getTotalLateNight());
assertEquals(expectedHolidayHours, result.getTotalHoliday());

// 3. ステータス検証
assertEquals(expectedStatus, result.getStatus());

// 4. タイムスタンプ検証
assertNotNull(result.getCreatedAt());
assertNotNull(result.getUpdatedAt());
```

## 5. テスト作成のコツとベストプラクティス

### 5.1 残業監視処理特有の注意点

#### 閾値の正確性
```java
// 閾値定数の使用
private static final BigDecimal OVERTIME_THRESHOLD = new BigDecimal("45.00");
private static final BigDecimal LATE_NIGHT_THRESHOLD = new BigDecimal("20.00");
private static final BigDecimal HOLIDAY_THRESHOLD = new BigDecimal("15.00");

// テストでの閾値確認
assertEquals(OVERTIME_THRESHOLD, OvertimeMonitoringProcessor.OvertimeThresholds.OVERTIME_THRESHOLD);
```

#### BigDecimal比較の注意
```java
// 正しい比較方法
assertEquals(0, expectedValue.compareTo(actualValue)); // スケール無視
// または
assertEquals(expectedValue.setScale(2), actualValue.setScale(2)); // スケール統一
```

### 5.2 テストデータ生成の効率化

#### ファクトリーメソッドパターン
```java
// 残業レポート生成ファクトリー
private OvertimeReport createOvertimeReport(int userId, LocalDate targetMonth, 
                                          String totalOvertime, String status) {
    OvertimeReport report = new OvertimeReport();
    report.setUserId(userId);
    report.setTargetMonth(targetMonth);
    report.setTotalOvertime(new BigDecimal(totalOvertime));
    report.setStatus(status);
    report.setCreatedAt(OffsetDateTime.now());
    report.setUpdatedAt(OffsetDateTime.now());
    return report;
}
```

#### ビルダーパターンの活用
```java
public class OvertimeReportTestBuilder {
    private OvertimeReport report = new OvertimeReport();
    
    public OvertimeReportTestBuilder userId(int userId) {
        report.setUserId(userId);
        return this;
    }
    
    public OvertimeReportTestBuilder targetMonth(LocalDate targetMonth) {
        report.setTargetMonth(targetMonth);
        return this;
    }
    
    public OvertimeReportTestBuilder totalOvertime(String hours) {
        report.setTotalOvertime(new BigDecimal(hours));
        return this;
    }
    
    public OvertimeReportTestBuilder status(String status) {
        report.setStatus(status);
        return this;
    }
    
    public OvertimeReport build() {
        return report;
    }
}

// 使用例
OvertimeReport report = new OvertimeReportTestBuilder()
    .userId(1)
    .targetMonth(LocalDate.of(2025, 2, 1))
    .totalOvertime("50.00")
    .status("confirmed")
    .build();
```

### 5.3 モック設定の最適化

#### 条件付きモック設定
```java
@BeforeEach
void setUp() {
    processor = new OvertimeMonitoringProcessor();
    processor.setOvertimeReportRepository(overtimeReportRepository);
    
    // デフォルトのモック設定（既存レポートなし）
    lenient().when(overtimeReportRepository
        .findByUserIdAndTargetMonth(anyInt(), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());
}
```

#### パラメータ化テストの活用
```java
@ParameterizedTest
@CsvSource({
    "50.00, 10.00, 5.00, confirmed",   // 残業時間閾値超過
    "30.00, 25.00, 5.00, confirmed",   // 深夜時間閾値超過
    "30.00, 10.00, 20.00, confirmed",  // 休日時間閾値超過
    "30.00, 10.00, 5.00, draft",       // 全て閾値以下
    "0.00, 0.00, 0.00, approved"       // 残業時間なし
})
void testProcess_VariousOvertimeHours_SetsCorrectStatus(String overtime, String lateNight, 
                                                       String holiday, String expectedStatus) {
    // 複数パターンでのステータス判定テスト
    AttendanceSummary summary = createMonthlySummaryWithOvertime(1, overtime, lateNight, holiday);
    
    OvertimeReport result = processor.process(summary);
    
    assertEquals(expectedStatus, result.getStatus());
}
```

## 6. 拡張テストケースの提案

### 6.1 実用的なテストケース

#### 複数ユーザー処理テスト
```java
@Test
void testProcess_MultipleUsers_ProcessesCorrectly() {
    // 複数ユーザーの残業監視処理確認
    List<AttendanceSummary> summaries = Arrays.asList(
        createMonthlySummaryWithOvertime(1, "50.00", "10.00", "5.00"), // confirmed
        createMonthlySummaryWithOvertime(2, "30.00", "15.00", "8.00"), // draft
        createMonthlySummaryWithOvertime(3, "0.00", "0.00", "0.00")    // approved
    );
    
    List<OvertimeReport> results = summaries.stream()
        .map(summary -> {
            try {
                return processor.process(summary);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })
        .collect(Collectors.toList());
    
    assertEquals("confirmed", results.get(0).getStatus());
    assertEquals("draft", results.get(1).getStatus());
    assertEquals("approved", results.get(2).getStatus());
}
```

#### 月境界処理テスト
```java
@Test
void testProcess_MonthBoundary_HandlesCorrectly() {
    // 月末・月初の処理確認
    AttendanceSummary endOfMonth = createMonthlySummaryWithOvertime(1, "40.00", "15.00", "10.00");
    endOfMonth.setTargetDate(LocalDate.of(2025, 1, 31)); // 1月末
    
    AttendanceSummary startOfMonth = createMonthlySummaryWithOvertime(1, "35.00", "12.00", "8.00");
    startOfMonth.setTargetDate(LocalDate.of(2025, 2, 1)); // 2月初
    
    OvertimeReport result1 = processor.process(endOfMonth);
    OvertimeReport result2 = processor.process(startOfMonth);
    
    assertEquals(LocalDate.of(2025, 1, 31), result1.getTargetMonth());
    assertEquals(LocalDate.of(2025, 2, 1), result2.getTargetMonth());
}
```

### 6.2 異常系テストケース

#### 負の値処理テスト
```java
@Test
void testProcess_WithNegativeValues_HandlesCorrectly() {
    // 負の値を含む残業データ（データ修正等で発生する可能性）
    AttendanceSummary negativeData = createMonthlySummaryWithOvertime(1, "-5.00", "10.00", "5.00");
    
    OvertimeReport result = processor.process(negativeData);
    
    // 負の値も含めて正確に処理されることを確認
    assertEquals(new BigDecimal("-5.00"), result.getTotalOvertime());
    assertEquals("draft", result.getStatus()); // 他の残業時間が存在するためdraft
}
```

#### 例外処理テスト
```java
@Test
void testProcess_RepositoryException_HandlesGracefully() {
    // リポジトリ例外発生時の処理
    when(overtimeReportRepository.findByUserIdAndTargetMonth(anyInt(), any(LocalDate.class)))
            .thenThrow(new RuntimeException("Database connection failed"));
    
    AttendanceSummary summary = createMonthlySummaryWithOvertime(1, "40.00", "15.00", "10.00");
    
    // 例外が適切に伝播されることを確認
    assertThrows(RuntimeException.class, () -> {
        processor.process(summary);
    });
}
```

## 7. パフォーマンステスト戦略

### 7.1 処理時間測定
```java
@Test
void testProcess_ProcessingTime_WithinExpectedRange() {
    AttendanceSummary summary = createMonthlySummaryWithOvertime(1, "40.00", "15.00", "10.00");
    
    // 複数回実行して平均処理時間を測定
    long totalTime = 0;
    int iterations = 100;
    
    for (int i = 0; i < iterations; i++) {
        long startTime = System.nanoTime();
        processor.process(summary);
        long endTime = System.nanoTime();
        totalTime += (endTime - startTime);
    }
    
    long averageTime = totalTime / iterations;
    long averageTimeMs = averageTime / 1_000_000;
    
    // 平均処理時間が50ms以内であることを確認
    assertTrue(averageTimeMs < 50);
}
```

### 7.2 メモリ使用量テスト
```java
@Test
void testProcess_MemoryUsage_WithinLimits() {
    Runtime runtime = Runtime.getRuntime();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // 大量データでの処理
    for (int i = 0; i < 1000; i++) {
        AttendanceSummary summary = createMonthlySummaryWithOvertime(i, "40.00", "15.00", "10.00");
        processor.process(summary);
    }
    
    runtime.gc(); // ガベージコレクション実行
    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // メモリ使用量が許容範囲内であることを確認
    long memoryUsed = afterMemory - beforeMemory;
    assertTrue(memoryUsed < 5 * 1024 * 1024); // 5MB以内
}
```

## 8. 一般的な問題と解決策

### 8.1 残業監視処理特有の問題

#### 閾値判定の不一致
**問題**: テストの期待値と実際の閾値判定結果が一致しない
```java
// 問題のあるコード
AssertEquals("confirmed", result.getStatus()); // 期待値
// 実際の結果: "draft" (閾値の理解不足)
```

**解決策**:
```java
// 閾値を明確に確認
BigDecimal overtimeHours = new BigDecimal("44.99"); // 45時間未満
assertEquals("draft", result.getStatus()); // 正しい期待値

// または閾値ちょうどのテスト
BigDecimal overtimeHours = new BigDecimal("45.01"); // 45時間超過
assertEquals("confirmed", result.getStatus());
```

#### ステータス判定の優先順位誤解
**問題**: 複数条件でのステータス判定ロジックの誤解
```java
// 誤解例: 残業時間が少なくても深夜時間が多い場合
AttendanceSummary summary = new AttendanceSummary();
summary.setOvertimeHours(new BigDecimal("10.00")); // 少ない
summary.setLateNightHours(new BigDecimal("25.00")); // 閾値超過

// 期待値の誤解
assertEquals("draft", result.getStatus()); // 間違い
```

**解決策**:
```java
// 正しい理解: いずれかの閾値を超過すれば"confirmed"
assertEquals("confirmed", result.getStatus()); // 正しい
```

### 8.2 モック設定の問題

#### 日付範囲の不一致
**問題**: モックの日付設定と実際の処理で使用される日付が一致しない
```java
// 問題のあるコード
when(overtimeReportRepository.findByUserIdAndTargetMonth(
        eq(1), eq(LocalDate.of(2025, 2, 1))))
        .thenReturn(Collections.emptyList());

// 実際の処理では異なる日付が使用される可能性
```

**解決策**:
```java
// any()を使用して日付チェックを回避
when(overtimeReportRepository.findByUserIdAndTargetMonth(
        eq(1), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());
```

## 9. まとめ

### 9.1 残業監視処理テストの重要ポイント
1. **閾値監視**: 法定基準に基づく正確な閾値判定
2. **ステータス管理**: 適切な自動ステータス設定
3. **レポート更新**: 既存データの適切な更新処理
4. **null値処理**: 不完全なデータに対する耐性
5. **境界値**: 閾値境界での正確な判定

### 9.2 テスト品質向上のチェックリスト
- [ ] 正常系・異常系・境界値テストを網羅
- [ ] 全ての閾値パターンをテスト
- [ ] BigDecimal比較は適切な方法を使用
- [ ] モック設定は必要最小限で重複なし
- [ ] テストデータは現実的な残業パターンを使用
- [ ] ステータス判定の正確性を確認
- [ ] パフォーマンスとメモリ使用量を考慮
- [ ] 法令遵守の観点を含む

### 9.3 他の処理テストとの違い
| 項目 | 日次処理テスト | 月次処理テスト | 残業監視処理テスト |
|------|----------------|----------------|--------------------|
| **入力データ** | AttendanceRecord | AttendanceSummary(daily) | AttendanceSummary(monthly) |
| **モック対象** | AttendanceRecordRepository, HolidayRepository | AttendanceSummaryRepository | OvertimeReportRepository |
| **テスト焦点** | 時間計算ロジック | データ集計ロジック | 閾値監視・ステータス判定 |
| **検証項目** | 打刻ペア処理、深夜・休日計算 | 日次データ合計、重複チェック | 閾値判定、レポート更新、法令遵守 |
| **特殊考慮** | 打刻データの整合性 | 月境界処理 | 労働基準法、健康管理 |

この手順書に従うことで、残業監視処理の特性を考慮した包括的で信頼性の高いテストケースを作成できます。労働基準法遵守と従業員の健康管理という重要な機能を支える品質保証が実現されます。