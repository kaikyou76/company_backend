# MonthlyWorkTimeProcessorTest テストケース作成手順書

## 概要
本書は、`MonthlyWorkTimeProcessorTest` のテストケース作成における注釈、モック対象、テスト作成の流れとコツを詳細に説明した手順書です。月次集計処理の特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/batch/processor/MonthlyWorkTimeProcessorTest.java`

### 1.2 基本アノテーション

#### @ExtendWith(MockitoExtension.class)
**行**: 18
```java
@ExtendWith(MockitoExtension.class)
class MonthlyWorkTimeProcessorTest {
```

**目的**:
- JUnit 5 でMockitoを統合
- `@Mock` アノテーションの自動初期化
- テストメソッド実行前にモックの初期化を自動実行

**日次処理テストとの違い**:
- 月次処理は日次データを入力とするため、モック対象が異なる
- AttendanceRecordRepository ではなく AttendanceSummaryRepository をモック

### 1.3 モックオブジェクト定義

#### @Mock AttendanceSummaryRepository
**行**: 20-21
```java
@Mock
private AttendanceSummaryRepository attendanceSummaryRepository;
```

**役割**:
- 日次集計データの取得処理をモック化
- `findByUserIdAndSummaryTypeAndTargetDateBetween()` メソッドの戻り値を制御
- 既存月次データの存在チェック処理をモック化
- 月次集計の基となる日次データ取得をテスト可能にする

**日次処理との違い**:
```java
// 日次処理: 打刻データを扱う
@Mock private AttendanceRecordRepository attendanceRecordRepository;
@Mock private HolidayRepository holidayRepository;

// 月次処理: 集計データを扱う
@Mock private AttendanceSummaryRepository attendanceSummaryRepository;
```

#### MonthlyWorkTimeProcessor インスタンス
**行**: 23, 26-29
```java
private MonthlyWorkTimeProcessor processor;

@BeforeEach
void setUp() {
    processor = new MonthlyWorkTimeProcessor();
    processor.setAttendanceSummaryRepository(attendanceSummaryRepository);
}
```

**特徴**:
- `@InjectMocks` を使用せず手動でセットアップ
- `setAttendanceSummaryRepository()` で依存性注入
- テスト実行前に毎回新しいインスタンスを作成

## 2. テストケース詳細解析

### 2.1 テストケース1: 正常な月次集計作成
**メソッド**: `testProcess_WithValidInRecord_ShouldCreateMonthlySummary`
**行**: 31-75

#### テストデータ準備
```java
// 入力レコード作成 (行33-36)
AttendanceRecord inRecord = new AttendanceRecord();
inRecord.setUserId(1);
inRecord.setType("in");
inRecord.setTimestamp(OffsetDateTime.now());
```

#### 既存月次データのモック設定
```java
// 既存月次サマリーなしのモック (行38-41)
when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
        eq(1), eq("monthly"), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());
```

#### 日次サマリーデータの準備
```java
// 日次サマリー1: 前日のデータ (行43-50)
AttendanceSummary dailySummary1 = new AttendanceSummary();
dailySummary1.setUserId(1);
dailySummary1.setTargetDate(LocalDate.now().minusDays(1));
dailySummary1.setTotalHours(new BigDecimal("8.00"));
dailySummary1.setOvertimeHours(new BigDecimal("1.00"));
dailySummary1.setLateNightHours(new BigDecimal("0.50"));
dailySummary1.setHolidayHours(new BigDecimal("0.00"));
dailySummary1.setSummaryType("daily");

// 日次サマリー2: 当日のデータ (行52-59)
AttendanceSummary dailySummary2 = new AttendanceSummary();
dailySummary2.setUserId(1);
dailySummary2.setTargetDate(LocalDate.now());
dailySummary2.setTotalHours(new BigDecimal("7.50"));
dailySummary2.setOvertimeHours(new BigDecimal("0.00"));
dailySummary2.setLateNightHours(new BigDecimal("0.00"));
dailySummary2.setHolidayHours(new BigDecimal("0.00"));
dailySummary2.setSummaryType("daily");
```

#### 日次データ取得のモック設定
```java
// 日次データ取得のモック (行63-66)
when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
        eq(1), eq("daily"), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(dailySummaries);
```

#### 検証ポイント
```java
// 基本情報検証 (行72-74)
assertNotNull(result);
assertEquals(1, result.getUserId());
assertEquals("monthly", result.getSummaryType());

// 集計結果検証 (行75-78)
assertEquals(new BigDecimal("15.50"), result.getTotalHours());    // 8.00 + 7.50
assertEquals(new BigDecimal("1.00"), result.getOvertimeHours());  // 1.00 + 0.00
assertEquals(new BigDecimal("0.50"), result.getLateNightHours()); // 0.50 + 0.00
assertEquals(new BigDecimal("0.00"), result.getHolidayHours());   // 0.00 + 0.00
```

### 2.2 テストケース2: 退勤レコード処理
**メソッド**: `testProcess_WithOutRecord_ShouldReturnNull`
**行**: 81-93

#### 境界値テスト
```java
// 退勤レコード作成 (行83-86)
AttendanceRecord outRecord = new AttendanceRecord();
outRecord.setUserId(1);
outRecord.setType("out");
outRecord.setTimestamp(OffsetDateTime.now());

// 期待結果: null返却 (行91-93)
AttendanceSummary result = processor.process(outRecord);
assertNull(result);
```

**テストの意図**:
- 月次処理は'in'タイプのレコードのみ処理
- 'out'レコードは処理をスキップする仕様の確認
- 重複処理防止ロジックのテスト

### 2.3 テストケース3: 既存月次サマリー存在時の処理
**メソッド**: `testProcess_WithExistingMonthlySummary_ShouldReturnNull`
**行**: 95-115

#### 重複処理防止テスト
```java
// 既存月次サマリーの準備 (行103-105)
AttendanceSummary existingMonthlySummary = new AttendanceSummary();
existingMonthlySummary.setUserId(1);
existingMonthlySummary.setSummaryType("monthly");

// 既存データありのモック設定 (行107-110)
when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
        eq(1), eq("monthly"), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(Arrays.asList(existingMonthlySummary));
```

**テストの重要性**:
- 月次集計の重複実行防止
- データ整合性の保証
- バッチ処理の冪等性確保

### 2.4 テストケース4: 日次サマリーデータなし
**メソッド**: `testProcess_WithNoDailySummaries_ShouldReturnNull`
**行**: 117-137

#### データ不足時の処理
```java
// 既存月次サマリーなし (行125-128)
when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
        eq(1), eq("monthly"), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());

// 日次サマリーデータなし (行130-133)
when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
        eq(1), eq("daily"), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());
```

**エラーハンドリングの確認**:
- 日次データが存在しない場合の安全な処理
- 例外を発生させずにnullを返却する仕様

### 2.5 テストケース5: null値処理
**メソッド**: `testProcess_WithNullValues_ShouldHandleGracefully`
**行**: 139-171

#### null値耐性テスト
```java
// null値を含む日次サマリー (行151-157)
AttendanceSummary dailySummary = new AttendanceSummary();
dailySummary.setUserId(1);
dailySummary.setTargetDate(LocalDate.now());
dailySummary.setTotalHours(null);
dailySummary.setOvertimeHours(null);
dailySummary.setLateNightHours(null);
dailySummary.setHolidayHours(null);
dailySummary.setSummaryType("daily");
```

#### null値処理の検証
```java
// null値が0.00として処理されることを確認 (行166-170)
assertNotNull(result);
assertEquals(new BigDecimal("0.00"), result.getTotalHours());
assertEquals(new BigDecimal("0.00"), result.getOvertimeHours());
assertEquals(new BigDecimal("0.00"), result.getLateNightHours());
assertEquals(new BigDecimal("0.00"), result.getHolidayHours());
```

## 3. 月次処理特有のテスト戦略

### 3.1 データ集計テストの特徴

#### 入力データの複雑性
```java
// 複数日次データの準備
List<AttendanceSummary> createMonthlyDailySummaries() {
    List<AttendanceSummary> summaries = new ArrayList<>();
    
    // 平日データ
    for (int day = 1; day <= 20; day++) {
        AttendanceSummary summary = new AttendanceSummary();
        summary.setTargetDate(LocalDate.of(2025, 2, day));
        summary.setTotalHours(new BigDecimal("8.00"));
        summary.setOvertimeHours(new BigDecimal("1.00"));
        summaries.add(summary);
    }
    
    // 休日データ
    AttendanceSummary holidaySummary = new AttendanceSummary();
    holidaySummary.setTargetDate(LocalDate.of(2025, 2, 21)); // 土曜日
    holidaySummary.setTotalHours(new BigDecimal("4.00"));
    holidaySummary.setHolidayHours(new BigDecimal("4.00"));
    summaries.add(holidaySummary);
    
    return summaries;
}
```

#### 集計計算の検証
```java
// 期待値計算
BigDecimal expectedTotal = new BigDecimal("164.00");    // 20日 × 8時間 + 4時間
BigDecimal expectedOvertime = new BigDecimal("20.00");  // 20日 × 1時間
BigDecimal expectedHoliday = new BigDecimal("4.00");    // 1日 × 4時間

// 実際の集計結果と比較
assertEquals(expectedTotal, result.getTotalHours());
assertEquals(expectedOvertime, result.getOvertimeHours());
assertEquals(expectedHoliday, result.getHolidayHours());
```

### 3.2 月次処理固有のエッジケース

#### 月境界のテスト
```java
@Test
void testProcess_CrossMonthBoundary_HandlesCorrectly() {
    // 前月末と当月初のデータが混在する場合
    AttendanceRecord record = createRecordForDate(LocalDate.of(2025, 2, 1));
    
    // 1月のデータは除外、2月のデータのみ集計されることを確認
}
```

#### 部分月データのテスト
```java
@Test
void testProcess_PartialMonthData_CalculatesCorrectly() {
    // 月の途中からのデータのみ存在する場合
    List<AttendanceSummary> partialData = createDailySummariesForDays(15, 28);
    
    // 存在する日数分のみ集計されることを確認
}
```

## 4. テストケース作成の流れ

### 4.1 月次処理テスト専用フロー
```
1. 月次処理要件定義
   ↓
2. 日次サマリーテストデータ準備
   ↓
3. 既存月次データチェックのモック設定
   ↓
4. 日次データ取得のモック設定
   ↓
5. 月次処理実行
   ↓
6. 集計結果検証
   ↓
7. エッジケース・異常系追加
```

### 4.2 詳細手順

#### ステップ1: 月次処理要件定義
```java
/**
 * テストケース名: 通常月の月次集計処理
 * 目的: 平日20日、休日2日の標準月パターンの集計確認
 * 入力: 22日分の日次サマリーデータ
 * 期待結果: 総時間176時間、残業20時間、休日8時間
 */
```

#### ステップ2: 日次サマリーテストデータ準備
```java
// 日次データ生成ヘルパーメソッド
private List<AttendanceSummary> createDailySummariesForMonth(int year, int month) {
    List<AttendanceSummary> summaries = new ArrayList<>();
    YearMonth yearMonth = YearMonth.of(year, month);
    
    for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
        LocalDate date = LocalDate.of(year, month, day);
        AttendanceSummary summary = createDailySummary(1, date);
        summaries.add(summary);
    }
    
    return summaries;
}
```

#### ステップ3: モック設定の階層化
```java
// 1. 既存月次データチェック
when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
        eq(userId), eq("monthly"), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());

// 2. 日次データ取得
when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
        eq(userId), eq("daily"), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(dailySummaries);
```

#### ステップ4: 集計結果の段階的検証
```java
// 1. 基本プロパティ検証
assertNotNull(result);
assertEquals(expectedUserId, result.getUserId());
assertEquals("monthly", result.getSummaryType());

// 2. 日付設定検証（月初日であることを確認）
LocalDate expectedTargetDate = YearMonth.from(inputDate).atDay(1);
assertEquals(expectedTargetDate, result.getTargetDate());

// 3. 集計値検証
assertEquals(expectedTotalHours, result.getTotalHours());
assertEquals(expectedOvertimeHours, result.getOvertimeHours());
assertEquals(expectedLateNightHours, result.getLateNightHours());
assertEquals(expectedHolidayHours, result.getHolidayHours());

// 4. 精度検証（小数点以下2位）
assertEquals(2, result.getTotalHours().scale());
```

## 5. テスト作成のコツとベストプラクティス

### 5.1 月次処理特有の注意点

#### 日付範囲の正確性
```java
// 月の境界を正確に設定
YearMonth targetMonth = YearMonth.of(2025, 2);
LocalDate monthStart = targetMonth.atDay(1);           // 2025-02-01
LocalDate monthEnd = targetMonth.atEndOfMonth();       // 2025-02-28

// any()を使用する場合の注意
any(LocalDate.class)  // OK: 日付範囲チェックをスキップ
eq(monthStart)        // NG: 実装の内部ロジックに依存
```

#### BigDecimal集計の精度管理
```java
// 集計前の精度統一
BigDecimal value1 = new BigDecimal("8.00");    // scale=2
BigDecimal value2 = new BigDecimal("7.5");     // scale=1

// 集計後の精度確認
BigDecimal sum = value1.add(value2);           // scale=2
assertEquals(2, sum.scale());                  // 精度検証
```

### 5.2 テストデータ生成の効率化

#### ファクトリーメソッドパターン
```java
// 日次サマリー生成ファクトリー
private AttendanceSummary createDailySummary(int userId, LocalDate date, 
                                           String totalHours, String overtimeHours) {
    AttendanceSummary summary = new AttendanceSummary();
    summary.setUserId(userId);
    summary.setTargetDate(date);
    summary.setTotalHours(new BigDecimal(totalHours));
    summary.setOvertimeHours(new BigDecimal(overtimeHours));
    summary.setLateNightHours(BigDecimal.ZERO);
    summary.setHolidayHours(BigDecimal.ZERO);
    summary.setSummaryType("daily");
    return summary;
}

// 使用例
AttendanceSummary summary = createDailySummary(1, LocalDate.of(2025, 2, 1), "8.00", "1.00");
```

#### ビルダーパターンの活用
```java
public class AttendanceSummaryTestBuilder {
    private AttendanceSummary summary = new AttendanceSummary();
    
    public AttendanceSummaryTestBuilder userId(int userId) {
        summary.setUserId(userId);
        return this;
    }
    
    public AttendanceSummaryTestBuilder date(LocalDate date) {
        summary.setTargetDate(date);
        return this;
    }
    
    public AttendanceSummaryTestBuilder totalHours(String hours) {
        summary.setTotalHours(new BigDecimal(hours));
        return this;
    }
    
    public AttendanceSummary build() {
        return summary;
    }
}

// 使用例
AttendanceSummary summary = new AttendanceSummaryTestBuilder()
    .userId(1)
    .date(LocalDate.of(2025, 2, 1))
    .totalHours("8.00")
    .build();
```

### 5.3 モック設定の最適化

#### 条件付きモック設定
```java
// 複数のテストケースで異なる戻り値が必要な場合
@BeforeEach
void setUp() {
    // 共通設定
    processor = new MonthlyWorkTimeProcessor();
    processor.setAttendanceSummaryRepository(attendanceSummaryRepository);
    
    // デフォルトのモック設定
    lenient().when(attendanceSummaryRepository
        .findByUserIdAndSummaryTypeAndTargetDateBetween(anyInt(), eq("monthly"), 
                                                       any(), any()))
        .thenReturn(Collections.emptyList());
}
```

#### パラメータ化テストの活用
```java
@ParameterizedTest
@ValueSource(ints = {1, 2, 3, 4, 5})
void testProcess_MultipleUsers_CreatesCorrectSummaries(int userId) {
    // 複数ユーザーでの処理確認
    AttendanceRecord record = createInRecord(userId);
    List<AttendanceSummary> dailyData = createDailySummariesForUser(userId);
    
    when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
            eq(userId), eq("daily"), any(), any()))
            .thenReturn(dailyData);
    
    AttendanceSummary result = processor.process(record);
    
    assertNotNull(result);
    assertEquals(userId, result.getUserId());
}
```

## 6. 拡張テストケースの提案

### 6.1 実用的なテストケース

#### 大量データ処理テスト
```java
@Test
void testProcess_WithLargeDataSet_PerformsEfficiently() {
    // 31日分の日次データで処理性能確認
    List<AttendanceSummary> largeDataSet = createDailySummariesForDays(1, 31);
    
    long startTime = System.currentTimeMillis();
    AttendanceSummary result = processor.process(inRecord);
    long endTime = System.currentTimeMillis();
    
    assertNotNull(result);
    assertTrue(endTime - startTime < 1000); // 1秒以内で処理完了
}
```

#### 精度検証テスト
```java
@Test
void testProcess_PrecisionCalculation_MaintainsAccuracy() {
    // 小数点計算の精度確認
    List<AttendanceSummary> precisionData = Arrays.asList(
        createDailySummary(1, LocalDate.now(), "8.33", "0.33"),
        createDailySummary(1, LocalDate.now().minusDays(1), "7.67", "0.67")
    );
    
    AttendanceSummary result = processor.process(inRecord);
    
    // 期待値: 8.33 + 7.67 = 16.00, 0.33 + 0.67 = 1.00
    assertEquals(new BigDecimal("16.00"), result.getTotalHours());
    assertEquals(new BigDecimal("1.00"), result.getOvertimeHours());
}
```

### 6.2 異常系テストケース

#### 不正データ処理テスト
```java
@Test
void testProcess_WithNegativeValues_HandlesCorrectly() {
    // 負の値を含む日次データ
    AttendanceSummary negativeData = createDailySummary(1, LocalDate.now(), "-1.00", "0.00");
    
    when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
            anyInt(), eq("daily"), any(), any()))
            .thenReturn(Arrays.asList(negativeData));
    
    AttendanceSummary result = processor.process(inRecord);
    
    // 負の値も含めて正確に集計されることを確認
    assertEquals(new BigDecimal("-1.00"), result.getTotalHours());
}
```

#### 例外処理テスト
```java
@Test
void testProcess_RepositoryException_HandlesGracefully() {
    // リポジトリ例外発生時の処理
    when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
            anyInt(), eq("monthly"), any(), any()))
            .thenThrow(new RuntimeException("Database connection failed"));
    
    // 例外が適切に伝播されることを確認
    assertThrows(RuntimeException.class, () -> {
        processor.process(inRecord);
    });
}
```

## 7. パフォーマンステスト戦略

### 7.1 メモリ使用量テスト
```java
@Test
void testProcess_MemoryUsage_WithinLimits() {
    Runtime runtime = Runtime.getRuntime();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // 大量データでの処理
    List<AttendanceSummary> largeDataSet = createLargeDataSet(1000);
    processor.process(inRecord);
    
    runtime.gc(); // ガベージコレクション実行
    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // メモリ使用量が許容範囲内であることを確認
    long memoryUsed = afterMemory - beforeMemory;
    assertTrue(memoryUsed < 10 * 1024 * 1024); // 10MB以内
}
```

### 7.2 処理時間測定
```java
@Test
void testProcess_ProcessingTime_WithinExpectedRange() {
    List<AttendanceSummary> monthlyData = createDailySummariesForMonth(2025, 2);
    
    // 複数回実行して平均処理時間を測定
    long totalTime = 0;
    int iterations = 10;
    
    for (int i = 0; i < iterations; i++) {
        long startTime = System.nanoTime();
        processor.process(inRecord);
        long endTime = System.nanoTime();
        totalTime += (endTime - startTime);
    }
    
    long averageTime = totalTime / iterations;
    long averageTimeMs = averageTime / 1_000_000;
    
    // 平均処理時間が100ms以内であることを確認
    assertTrue(averageTimeMs < 100);
}
```

## 8. 一般的な問題と解決策

### 8.1 月次処理特有の問題

#### 日付範囲の不一致
**問題**: モックの日付範囲と実際の処理範囲が一致しない
```java
// 問題のあるコード
when(repository.findByUserIdAndSummaryTypeAndTargetDateBetween(
        eq(1), eq("daily"), eq(LocalDate.of(2025, 2, 1)), eq(LocalDate.of(2025, 2, 28))))
        .thenReturn(dailyData);

// 実際の処理では2025-02-01から2025-02-28の範囲で検索されるが、
// 2月は28日までなので2025-02-29は存在しない
```

**解決策**:
```java
// any()を使用して日付範囲チェックを回避
when(repository.findByUserIdAndSummaryTypeAndTargetDateBetween(
        eq(1), eq("daily"), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(dailyData);
```

#### BigDecimal精度の不一致
**問題**: 期待値と実際の値でスケールが異なる
```java
// 問題のあるコード
assertEquals(new BigDecimal("15.5"), result.getTotalHours()); // scale=1
// 実際の結果: 15.50 (scale=2)
```

**解決策**:
```java
// スケールを統一して比較
assertEquals(new BigDecimal("15.50"), result.getTotalHours());
// または compareTo() を使用
assertEquals(0, new BigDecimal("15.5").compareTo(result.getTotalHours()));
```

### 8.2 モック設定の問題

#### 重複するモック設定
**問題**: 同じメソッドに対する複数のモック設定が競合
```java
// 問題のあるコード
when(repository.findByUserIdAndSummaryTypeAndTargetDateBetween(anyInt(), eq("monthly"), any(), any()))
    .thenReturn(Collections.emptyList());
when(repository.findByUserIdAndSummaryTypeAndTargetDateBetween(eq(1), eq("monthly"), any(), any()))
    .thenReturn(existingData);
```

**解決策**:
```java
// より具体的な条件を後に設定
when(repository.findByUserIdAndSummaryTypeAndTargetDateBetween(anyInt(), eq("monthly"), any(), any()))
    .thenReturn(Collections.emptyList());
when(repository.findByUserIdAndSummaryTypeAndTargetDateBetween(eq(1), eq("monthly"), any(), any()))
    .thenReturn(existingData); // この設定が優先される
```

## 9. まとめ

### 9.1 月次処理テストの重要ポイント
1. **データ集計**: 日次データの合計処理が正確であることを確認
2. **重複防止**: 既存月次データのチェック機能をテスト
3. **null値処理**: 不完全な日次データに対する耐性を確認
4. **精度管理**: BigDecimalの精度が適切に保たれることを確認
5. **境界値**: 月境界や部分データでの正確な処理を確認

### 9.2 テスト品質向上のチェックリスト
- [ ] 正常系・異常系・境界値テストを網羅
- [ ] 日次データの様々なパターンをテスト
- [ ] BigDecimal比較は適切な方法を使用
- [ ] モック設定は必要最小限で重複なし
- [ ] テストデータは意味を持つ現実的な値を使用
- [ ] 集計計算の精度と正確性を確認
- [ ] パフォーマンスと メモリ使用量を考慮

### 9.3 日次処理テストとの違い
| 項目 | 日次処理テスト | 月次処理テスト |
|------|----------------|----------------|
| **入力データ** | AttendanceRecord (打刻データ) | AttendanceSummary (日次集計データ) |
| **モック対象** | AttendanceRecordRepository, HolidayRepository | AttendanceSummaryRepository |
| **テスト焦点** | 時間計算ロジック | データ集計ロジック |
| **検証項目** | 打刻ペアの処理、深夜・休日計算 | 日次データの合計、重複チェック |
| **データ量** | 1日分の打刻データ | 1ヶ月分の日次データ |

この手順書に従うことで、月次集計処理の特性を考慮した包括的で信頼性の高いテストケースを作成できます。
