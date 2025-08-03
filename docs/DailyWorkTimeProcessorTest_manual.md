# DailyWorkTimeProcessorTest テストケース作成手順書

## 概要
本書は、`DailyWorkTimeProcessorTest` のテストケース作成における注釈、モック対象、テスト作成の流れとコツを詳細に説明した手順書です。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/batch/processor/DailyWorkTimeProcessorTest.java`

### 1.2 基本アノテーション

#### @ExtendWith(MockitoExtension.class)
**行**: 27
```java
@ExtendWith(MockitoExtension.class)
public class DailyWorkTimeProcessorTest {
```

**目的**:
- JUnit 5 でMockitoを統合
- `@Mock` アノテーションの自動初期化
- テストメソッド実行前にモックの初期化を自動実行

**従来との違い**:
```java
// 従来の方法 (JUnit 4 または手動初期化)
@BeforeEach
public void setUp() {
    MockitoAnnotations.openMocks(this);
}

// 現在の方法 (JUnit 5 + MockitoExtension)
@ExtendWith(MockitoExtension.class)
```

### 1.3 モックオブジェクト定義

#### @Mock HolidayRepository
**行**: 30-31
```java
@Mock
private HolidayRepository holidayRepository;
```

**役割**:
- 休日データの取得処理をモック化
- `findAll()` メソッドの戻り値を制御
- 休日判定ロジックのテストを可能にする

#### @Mock AttendanceRecordRepository  
**行**: 33-34
```java
@Mock
private AttendanceRecordRepository attendanceRecordRepository;
```

**役割**:
- 出席レコードの取得処理をモック化
- `findByUserIdAndDate()` メソッドの戻り値を制御
- 日次レコード取得ロジックのテストを可能にする

#### @InjectMocks DailyWorkTimeProcessor
**行**: 36-37
```java
@InjectMocks
private DailyWorkTimeProcessor dailyWorkTimeProcessor;
```

**役割**:
- テスト対象クラスのインスタンス生成
- `@Mock` で定義されたオブジェクトを自動注入
- 実際のビジネスロジックをテスト

## 2. テストケース詳細解析

### 2.1 テストケース1: 正常な入退勤記録処理
**メソッド**: `testProcess_withValidInAndOutRecords_shouldReturnSummary`
**行**: 45-80

#### テストデータ準備
```java
// 入勤記録作成 (行47-51)
AttendanceRecord inRecord = new AttendanceRecord();
inRecord.setId(1L);
inRecord.setUserId(1);
inRecord.setType("in");
inRecord.setTimestamp(OffsetDateTime.of(2025, 1, 1, 9, 0, 0, 0, ZoneOffset.UTC));

// 退勤記録作成 (行53-57)
AttendanceRecord outRecord = new AttendanceRecord();
outRecord.setId(2L);
outRecord.setUserId(1);
outRecord.setType("out");
outRecord.setTimestamp(OffsetDateTime.of(2025, 1, 1, 18, 0, 0, 0, ZoneOffset.UTC));
```

#### モック設定
```java
// 日次レコード取得のモック化 (行64-65)
when(attendanceRecordRepository.findByUserIdAndDate(1, LocalDate.of(2025, 1, 1)))
        .thenReturn(dailyRecords);

// 休日取得のモック化 (行66)
when(holidayRepository.findAll()).thenReturn(new ArrayList<>());
```

#### 検証ポイント
```java
// 基本情報検証 (行72-74)
assertEquals(1, summary.getUserId());
assertEquals(LocalDate.of(2025, 1, 1), summary.getTargetDate());

// 時間計算検証 (行75-78)
assertEquals(new BigDecimal("9.00"), summary.getTotalHours());  // 9時間勤務
assertEquals(new BigDecimal("1.00"), summary.getOvertimeHours()); // 1時間残業
assertEquals(0, summary.getLateNightHours().compareTo(BigDecimal.ZERO)); // 深夜なし
assertEquals(0, summary.getHolidayHours().compareTo(BigDecimal.ZERO));   // 平日
```

### 2.2 テストケース2: 深夜勤務時間計算
**メソッド**: `testProcess_withLateNightWork_shouldCalculateLateNightHours`
**行**: 83-116

#### 特徴的なテストデータ
```java
// 21:00 入勤 (行89)
inRecord.setTimestamp(OffsetDateTime.of(2025, 1, 1, 21, 0, 0, 0, ZoneOffset.UTC));

// 翌日02:00 退勤 (行95)
outRecord.setTimestamp(OffsetDateTime.of(2025, 1, 2, 2, 0, 0, 0, ZoneOffset.UTC));
```

#### 深夜時間計算の検証
```java
// 跨日勤務時間: 21:00-02:00 = 5時間 (行112)
assertEquals(new BigDecimal("5.00"), summary.getTotalHours());

// 深夜時間: 22:00-24:00(2時間) + 00:00-02:00(2時間) = 4時間理論値 (行115)
assertTrue(summary.getLateNightHours().compareTo(BigDecimal.ZERO) > 0);
```

### 2.3 テストケース3: 休日勤務計算
**メソッド**: `testProcess_withHolidayWork_shouldCalculateHolidayHours`
**行**: 119-157

#### 休日データ設定
```java
// 休日オブジェクト作成 (行139-142)
Holiday holiday = new Holiday();
holiday.setDate(LocalDate.of(2025, 1, 4));  // 土曜日
holiday.setName("周末");
holidays.add(holiday);
```

#### Lenient モック設定
```java
// 条件付き使用のためlenient()を使用 (行147)
lenient().when(holidayRepository.findAll()).thenReturn(holidays);
```

**Lenient の必要性**:
- 一部のテストケースでのみ使用されるモック
- 未使用時の `UnnecessaryStubbingException` を回避
- 条件分岐のあるロジックで有効

#### 休日勤務検証
```java
// 全勤務時間が休日勤務として計算される (行156)
assertEquals(new BigDecimal("9.00"), summary.getHolidayHours());
```

### 2.4 テストケース4: 退勤レコード処理
**メソッド**: `testProcess_withOutRecord_shouldReturnNull`
**行**: 160-173

#### 境界値テスト
- 入力: "out"タイプのレコード
- 期待結果: `null` 返却（処理スキップ）
- 目的: 入勤レコードのみ処理する仕様の確認

### 2.5 テストケース5: 不完全レコード処理
**メソッド**: `testProcess_withInsufficientRecords_shouldReturnNull`
**行**: 176-196

#### エッジケース対応
- 入力: 入勤レコードのみ（退勤レコードなし）
- 期待結果: `null` 返却（ペア不足）
- 目的: データ不整合時の安全な処理確認

## 3. テストケース作成の流れ

### 3.1 基本フロー
```
1. テストケース要件定義
   ↓
2. テストデータ準備
   ↓
3. モック設定 (when-thenReturn)
   ↓
4. テスト対象メソッド実行
   ↓
5. 結果検証 (assert)
   ↓
6. エッジケース・異常系追加
```

### 3.2 詳細手順

#### ステップ1: テストケース要件定義
```java
/**
 * テストケース名: 正常な入退勤記録の処理
 * 目的: 9時-18時の標準勤務パターンの時間計算確認
 * 入力: in(09:00), out(18:00)
 * 期待結果: 総時間9時間、残業1時間、深夜0時間、休日0時間
 */
```

#### ステップ2: テストデータ準備
```java
// Arrange (準備)
AttendanceRecord inRecord = createInRecord(userId, date, time);
AttendanceRecord outRecord = createOutRecord(userId, date, time);
List<AttendanceRecord> dailyRecords = Arrays.asList(inRecord, outRecord);
```

#### ステップ3: モック設定
```java
// モック動作定義
when(repository.method(parameters)).thenReturn(expected);

// 複数パターン
when(holidayRepository.findAll())
    .thenReturn(Collections.emptyList());  // 平日テスト用
    .thenReturn(holidays);                 // 休日テスト用
```

#### ステップ4: テスト実行
```java
// Act (実行)
AttendanceSummary result = processor.process(inRecord);
```

#### ステップ5: 結果検証
```java
// Assert (検証)
assertNotNull(result);
assertEquals(expected, actual);
assertTrue(condition);

// BigDecimal 比較の注意点
assertEquals(0, bigDecimal1.compareTo(bigDecimal2));  // 正しい
assertEquals(bigDecimal1, bigDecimal2);               // 不適切
```

## 4. テスト作成のコツとベストプラクティス

### 4.1 BigDecimal テスト時の注意点

#### 問題のあるパターン
```java
// NG: スケールの違いで失敗する可能性
assertEquals(new BigDecimal("0.00"), summary.getLateNightHours());
```

#### 推奨パターン
```java
// OK: compareTo() を使用
assertEquals(0, summary.getLateNightHours().compareTo(BigDecimal.ZERO));

// OK: 許容誤差を指定
assertEquals(expectedValue, actualValue, 0.01);
```

### 4.2 日時テストデータの作成

#### 一貫したタイムゾーン使用
```java
// 推奨: UTC で統一
OffsetDateTime.of(2025, 1, 1, 9, 0, 0, 0, ZoneOffset.UTC)

// 避ける: システムタイムゾーン依存
LocalDateTime.now().atOffset(ZoneOffset.systemDefault())
```

#### 意味のある日付選択
```java
LocalDate.of(2025, 1, 1)   // 水曜日 - 平日テスト
LocalDate.of(2025, 1, 4)   // 土曜日 - 週末テスト
LocalDate.of(2025, 1, 1)   // 元日   - 祝日テスト
```

### 4.3 モック設定のベストプラクティス

#### 必要最小限のモック
```java
// Good: 実際に使用される部分のみモック
when(repository.findByUserIdAndDate(1, targetDate)).thenReturn(records);

// Bad: 過度なモック設定
when(repository.findAll()).thenReturn(allRecords);
when(repository.findById(anyLong())).thenReturn(Optional.of(record));
when(repository.save(any())).thenReturn(record);
```

#### Lenient モックの適切な使用
```java
// 条件分岐で使用される可能性がある場合
lenient().when(holidayRepository.findAll()).thenReturn(holidays);

// 常に使用される場合
when(attendanceRecordRepository.findByUserIdAndDate(anyInt(), any()))
    .thenReturn(dailyRecords);
```

### 4.4 テストケース命名規則

#### 推奨パターン
```java
// パターン1: test[Method]_with[Condition]_should[ExpectedResult]
testProcess_withValidInAndOutRecords_shouldReturnSummary()

// パターン2: test[Method]_[Scenario]_[ExpectedResult]
testProcess_lateNightWork_calculatesLateNightHours()

// パターン3: [Method]_[Condition]_[Result]
process_withHolidayWork_returnsHolidayHours()
```

### 4.5 アサーション戦略

#### 階層的検証
```java
// 1. null チェック
assertNotNull(summary);

// 2. 基本プロパティ
assertEquals(expectedUserId, summary.getUserId());
assertEquals(expectedDate, summary.getTargetDate());

// 3. 計算結果
assertEquals(expectedTotalHours, summary.getTotalHours());
assertEquals(expectedOvertimeHours, summary.getOvertimeHours());

// 4. 特殊ケース
assertTrue(summary.getLateNightHours().compareTo(BigDecimal.ZERO) >= 0);
```

## 5. 一般的な問題と解決策

### 5.1 UnnecessaryStubbingException
**原因**: 定義したモックが使用されない
**解決策**:
```java
// lenient() を使用
lenient().when(repository.method()).thenReturn(value);

// または、不要なstubを削除
// when(unnecessaryRepository.method()).thenReturn(value); // 削除
```

### 5.2 BigDecimal 比較エラー
**原因**: スケールや精度の違い
**解決策**:
```java
// compareTo() を使用
assertEquals(0, actual.compareTo(expected));

// または setScale() で統一
expected.setScale(2, RoundingMode.HALF_UP)
```

### 5.3 日時計算の不正確性
**原因**: タイムゾーンやサマータイムの影響
**解決策**:
```java
// UTC を明示的に指定
OffsetDateTime.of(year, month, day, hour, minute, second, nano, ZoneOffset.UTC)

// またはテスト用の固定タイムゾーン設定
@Test
void testWithFixedClock() {
    Clock fixedClock = Clock.fixed(instant, ZoneOffset.UTC);
    // テストロジック
}
```

## 6. 拡張テストケースの提案

### 6.1 境界値テスト
```java
@Test
void testProcess_exactlyEightHours_noOvertime() {
    // 8時間ちょうどの勤務
}

@Test  
void testProcess_midnightSpanning_calculatesBothDays() {
    // 日付をまたぐ勤務
}
```

### 6.2 異常系テスト
```java
@Test
void testProcess_withNullRecord_throwsException() {
    // null入力のハンドリング
}

@Test
void testProcess_withInvalidTimeOrder_returnsNull() {
    // 退勤時刻が入勤時刻より早い場合
}
```

### 6.3 パフォーマンステスト
```java
@Test
void testProcess_withLargeRecordSet_performsWithinTimeLimit() {
    // 大量データでのパフォーマンス確認
}
```

## 7. まとめ

### 7.1 重要ポイント
1. **アノテーション**: `@ExtendWith(MockitoExtension.class)` でJUnit5統合
2. **モック対象**: 外部依存（Repository）のみをモック化
3. **BigDecimal**: `compareTo()` で比較
4. **日時**: UTC統一でタイムゾーン問題回避
5. **Lenient**: 条件付き使用モックで例外回避

### 7.2 テスト品質向上のチェックリスト
- [ ] 正常系・異常系・境界値テストを網羅
- [ ] モック設定は必要最小限
- [ ] BigDecimal比較は `compareTo()` 使用
- [ ] 日時テストデータは意味を持つ値を選択
- [ ] テストメソッド名は処理内容を明確に表現
- [ ] アサーションは階層的に実装

この手順書に従うことで、保守性が高く信頼性のあるテストケースを作成できます。