# AttendanceSummaryServiceTest テストケース作成手順書

## 概要
本書は、`AttendanceSummaryServiceTest` のテストケース作成における注釈、モック対象、テスト作成の流れとコツを詳細に説明した手順書です。勤怠集計サービスの特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/service/AttendanceSummaryServiceTest.java`

### 1.2 基本アノテーション

#### @ExtendWith(MockitoExtension.class)
**行**: 36
```java
@ExtendWith(MockitoExtension.class)
class AttendanceSummaryServiceTest {
```

**目的**:
- JUnit 5 でMockitoを統合
- `@Mock` アノテーションの自動初期化
- テストメソッド実行前にモックの初期化を自動実行

**集計サービステストの特徴**:
- 複数のRepository（サマリー、ユーザー、記録、祝日）を統合的にモック
- 統計計算、データ集計、エクスポート機能など多面的な機能をテスト
- ページング処理とバッチ処理の両方をカバー

### 1.3 モックオブジェクト定義

#### @Mock AttendanceSummaryRepository
**行**: 38-39
```java
@Mock
private AttendanceSummaryRepository attendanceSummaryRepository;
```

**役割**:
- 勤怠サマリーの CRUD 操作をモック化
- `findByTargetDateBetween()` - 期間指定でのサマリー取得
- `findByUserIdAndTargetDateBetween()` - ユーザー・期間指定での取得
- `findByTargetDate()` - 日付指定での取得
- `save()` - サマリー保存処理

**テスト対象メソッド**:
```java
// 主要なモック対象メソッド
when(attendanceSummaryRepository.findByTargetDateBetween(startDate, endDate, pageable))
when(attendanceSummaryRepository.findByUserIdAndTargetDateBetween(userId, startDate, endDate))
when(attendanceSummaryRepository.findByTargetDate(date))
when(attendanceSummaryRepository.save(any(AttendanceSummary.class)))
```

#### @Mock AttendanceRecordRepository
**行**: 41-42
```java
@Mock
private AttendanceRecordRepository attendanceRecordRepository;
```

**役割**:
- 将来の拡張機能に備えたモック準備
- 日別サマリー生成時の元データ取得に使用予定
- 現在の実装では直接使用されていないが、テスト設計の完全性のため含める

#### @Mock HolidayRepository
**行**: 44-45
```java
@Mock
private HolidayRepository holidayRepository;
```

**役割**:
- 祝日情報の取得をモック化
- 休日勤務時間の集計処理で使用予定
- 将来の機能拡張に備えた準備

#### @Mock UserRepository
**行**: 47-48
```java
@Mock
private UserRepository userRepository;
```

**役割**:
- ユーザー情報の取得をモック化
- `findByDepartmentId()` - 部門別統計での部門所属ユーザー取得
- 部門横断的な統計計算で重要な役割

### 1.4 テスト用定数定義

#### 集計処理関連定数
**行**: 52-58
```java
private static final Integer TEST_USER_ID = 1;
private static final Long TEST_USER_ID_LONG = 1L;
private static final Integer TEST_DEPARTMENT_ID = 10;
private static final LocalDate TEST_DATE = LocalDate.of(2025, 2, 1);
private static final LocalDate START_DATE = LocalDate.of(2025, 2, 1);
private static final LocalDate END_DATE = LocalDate.of(2025, 2, 28);
```

**設計思想**:
- **期間設定**: 2月全体（28日間）を基準期間として使用
- **テスト一貫性**: 全テストで同じ基準日付・期間を使用
- **部門テスト**: 部門ID 10を標準的な部門として設定

### 1.5 AttendanceSummaryService インスタンス
**行**: 50, 60-65
```java
private AttendanceSummaryService attendanceSummaryService;

@BeforeEach
void setUp() {
    attendanceSummaryService = new AttendanceSummaryServiceImpl(
            attendanceSummaryRepository,
            attendanceRecordRepository,
            holidayRepository,
            userRepository);
}
```

**特徴**:
- コンストラクタインジェクションによる依存性注入
- `@InjectMocks` を使用せず手動でセットアップ
- テスト実行前に毎回新しいインスタンスを作成
- 全ての依存関係を明示的に注入

## 2. テストケース詳細解析

### 2.1 日別サマリー取得テスト群

#### テストケース1: 正常な日別サマリー取得
**メソッド**: `testGetDailySummaries_WithValidDateRange_ShouldReturnPagedResults`
**行**: 69-91

##### テストデータ準備
```java
// ページング設定 (行71)
Pageable pageable = PageRequest.of(0, 10);

// サマリーデータ準備 (行72-76)
List<AttendanceSummary> summaries = Arrays.asList(
        createAttendanceSummary(1L, TEST_USER_ID, TEST_DATE, "8.00", "1.00", "0.50", "0.00", "daily"),
        createAttendanceSummary(2L, TEST_USER_ID, TEST_DATE.plusDays(1), "7.50", "0.00", "0.00", "0.00", "daily")
);
```

##### モック設定
```java
// 期間指定でのページング取得 (行79-80)
when(attendanceSummaryRepository.findByTargetDateBetween(START_DATE, END_DATE, pageable))
        .thenReturn(expectedPage);
```

##### 検証ポイント
```java
// ページング結果検証 (行86-90)
assertNotNull(result);
assertEquals(2, result.getContent().size());
assertEquals(TEST_USER_ID, result.getContent().get(0).getUserId());
assertEquals(TEST_DATE, result.getContent().get(0).getTargetDate());
assertEquals(new BigDecimal("8.00"), result.getContent().get(0).getTotalHours());
```

#### テストケース2: 空結果の処理
**メソッド**: `testGetDailySummaries_WithEmptyResult_ShouldReturnEmptyPage`
**行**: 93-108

##### 空データ処理テスト
```java
// 空ページの準備 (行96)
Page<AttendanceSummary> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

// 空結果の検証 (行103-105)
assertNotNull(result);
assertTrue(result.getContent().isEmpty());
assertEquals(0, result.getTotalElements());
```

### 2.2 月別サマリー取得テスト群

#### テストケース3: 正常な月別サマリー取得
**メソッド**: `testGetMonthlySummaries_WithValidYearMonth_ShouldReturnPagedResults`
**行**: 112-134

##### YearMonth処理テスト
```java
// 年月指定 (行114)
YearMonth yearMonth = YearMonth.of(2025, 2);

// 月境界の自動計算 (行116-117)
LocalDate expectedStartDate = yearMonth.atDay(1);           // 2025-02-01
LocalDate expectedEndDate = yearMonth.atEndOfMonth();       // 2025-02-28
```

##### 月別データ検証
```java
// 月別サマリーの確認 (行128-130)
assertEquals(1, result.getContent().size());
assertEquals("monthly", result.getContent().get(0).getSummaryType());
assertEquals(new BigDecimal("160.00"), result.getContent().get(0).getTotalHours());
```

#### テストケース4: 閏年処理
**メソッド**: `testGetMonthlySummaries_WithLeapYear_ShouldHandleFebruaryCorrectly`
**行**: 136-150

##### 閏年境界値テスト
```java
// 閏年の2月設定 (行138)
YearMonth leapYearMonth = YearMonth.of(2024, 2); // 2024年は閏年

// 閏年2月の正確な終了日 (行141)
LocalDate expectedEndDate = LocalDate.of(2024, 2, 29); // 閏年の2月29日
```

### 2.3 日付指定サマリー取得テスト

#### テストケース5: 存在するデータの取得
**メソッド**: `testGetSummaryByDate_WithExistingData_ShouldReturnSummary`
**行**: 154-169

##### 単一データ取得テスト
```java
// 期待するサマリーの準備 (行156-157)
AttendanceSummary expectedSummary = createAttendanceSummary(1L, TEST_USER_ID, TEST_DATE, 
        "8.00", "1.00", "0.50", "0.00", "daily");

// 単一結果の検証 (行164-167)
assertNotNull(result);
assertEquals(expectedSummary.getId(), result.getId());
assertEquals(TEST_DATE, result.getTargetDate());
assertEquals(new BigDecimal("8.00"), result.getTotalHours());
```

#### テストケース6: データなしの処理
**メソッド**: `testGetSummaryByDate_WithNoData_ShouldReturnNull`
**行**: 171-181

##### null戻り値テスト
```java
// 空リストのモック (行173-174)
when(attendanceSummaryRepository.findByTargetDate(TEST_DATE))
        .thenReturn(Collections.emptyList());

// null戻り値の確認 (行179)
assertNull(result);
```

### 2.4 統計情報計算テスト群

#### テストケース7: 正常な統計計算
**メソッド**: `testGetSummaryStatistics_WithValidData_ShouldCalculateCorrectly`
**行**: 195-218

##### 複数データ集計テスト
```java
// 3日分のサマリーデータ (行197-201)
List<AttendanceSummary> summaries = Arrays.asList(
        createAttendanceSummary(1L, TEST_USER_ID, TEST_DATE, "8.00", "1.00", "0.50", "0.00", "daily"),
        createAttendanceSummary(2L, TEST_USER_ID, TEST_DATE.plusDays(1), "7.50", "0.00", "0.00", "2.00", "daily"),
        createAttendanceSummary(3L, TEST_USER_ID, TEST_DATE.plusDays(2), "9.00", "2.00", "1.00", "0.00", "daily")
);

// 集計結果の検証 (行210-213)
assertEquals(3, result.get("totalRecords"));
assertEquals(24.5, (Double) result.get("totalHours"), 0.01); // 8.00 + 7.50 + 9.00
assertEquals(3.0, (Double) result.get("overtimeHours"), 0.01); // 1.00 + 0.00 + 2.00
```

#### テストケース8: null値の適切な処理
**メソッド**: `testGetSummaryStatistics_WithNullValues_ShouldHandleGracefully`
**行**: 220-237

##### null値耐性テスト
```java
// null値を含むサマリー (行222-223)
List<AttendanceSummary> summaries = Arrays.asList(
        createAttendanceSummaryWithNulls(1L, TEST_USER_ID, TEST_DATE, "daily")
);

// null値が0として処理されることを確認 (行231-234)
assertEquals(1, result.get("totalRecords"));
assertEquals(0.0, (Double) result.get("totalHours"), 0.01);
assertEquals(0.0, (Double) result.get("overtimeHours"), 0.01);
```

### 2.5 エクスポート機能テスト群

#### テストケース9-10: CSV形式エクスポート
**メソッド**: `testExportSummariesToCSV_WithValidData_ShouldGenerateCorrectFormat`
**行**: 253-278

##### CSV形式検証テスト
```java
// StringWriterを使用したCSV出力テスト (行263-264)
StringWriter stringWriter = new StringWriter();
PrintWriter printWriter = new PrintWriter(stringWriter);

// CSV形式の検証 (行271-274)
assertTrue(csvOutput.contains("Date,Total Hours,Overtime Hours,Late Night Hours,Holiday Hours"));
assertTrue(csvOutput.contains("2025-02-01,8.00,1.00,0.50,0.00"));
assertTrue(csvOutput.contains("2025-02-02,7.50,0.00,0.00,2.00"));

// 行数確認（ヘッダー + データ2行 = 3行） (行276-277)
String[] lines = csvOutput.trim().split("\n");
assertEquals(3, lines.length);
```

#### テストケース11-12: JSON形式エクスポート
**メソッド**: `testExportSummariesToJSON_WithValidData_ShouldGenerateCorrectFormat`
**行**: 295-318

##### JSON形式検証テスト
```java
// JSON形式の検証 (行310-316)
assertTrue(jsonOutput.contains("["));
assertTrue(jsonOutput.contains("]"));
assertTrue(jsonOutput.contains("\"date\": \"2025-02-01\""));
assertTrue(jsonOutput.contains("\"totalHours\": 8.00"));
assertTrue(jsonOutput.contains("\"overtimeHours\": 1.00"));
assertTrue(jsonOutput.contains("\"holidayHours\": 2.00"));
```

### 2.6 月別統計情報テスト

#### テストケース13: 月別統計計算
**メソッド**: `testGetMonthlyStatistics_WithValidData_ShouldCalculateCorrectly`
**行**: 336-365

##### 月別グループ化テスト
```java
// 2月と3月のデータ (行338-343)
List<AttendanceSummary> summaries = Arrays.asList(
        // 2月のデータ
        createAttendanceSummary(1L, TEST_USER_ID, LocalDate.of(2025, 2, 1), "8.00", "1.00", "0.50", "0.00", "daily"),
        createAttendanceSummary(2L, TEST_USER_ID, LocalDate.of(2025, 2, 2), "7.50", "0.00", "0.00", "0.00", "daily"),
        // 3月のデータ
        createAttendanceSummary(3L, TEST_USER_ID, LocalDate.of(2025, 3, 1), "9.00", "2.00", "1.00", "0.00", "daily")
);

// 月別集計の検証 (行355-361)
Map<String, Double> monthlyHours = (Map<String, Double>) result.get("monthlyHours");
assertEquals(15.5, monthlyHours.get("2025-02-01"), 0.01); // 2月: 8.00 + 7.50
assertEquals(9.0, monthlyHours.get("2025-03-01"), 0.01);  // 3月: 9.00

Double averageDailyHours = (Double) result.get("averageDailyHours");
assertEquals(8.17, averageDailyHours, 0.01); // (8.00 + 7.50 + 9.00) / 3
```

### 2.7 個人別統計情報テスト

#### テストケース14: 個人統計計算
**メソッド**: `testGetPersonalAttendanceStatistics_WithValidData_ShouldCalculateCorrectly`
**行**: 369-398

##### 個人別集計テスト
```java
// 個人の3日分データ (行371-375)
List<AttendanceSummary> personalSummaries = Arrays.asList(
        createAttendanceSummary(1L, TEST_USER_ID, TEST_DATE, "8.00", "1.00", "0.50", "0.00", "daily"),
        createAttendanceSummary(2L, TEST_USER_ID, TEST_DATE.plusDays(1), "7.50", "0.00", "0.00", "2.00", "daily"),
        createAttendanceSummary(3L, TEST_USER_ID, TEST_DATE.plusDays(2), "9.00", "2.00", "1.00", "0.00", "daily")
);

// 個人統計の検証 (行384-394)
assertEquals(TEST_USER_ID_LONG, result.get("userId"));
assertEquals(3, result.get("totalRecords"));
assertEquals(24.5, (Double) result.get("totalHours"), 0.01); // 8.00 + 7.50 + 9.00
assertEquals(3.0, (Double) result.get("overtimeHours"), 0.01); // 1.00 + 0.00 + 2.00
assertEquals(1.5, (Double) result.get("lateNightHours"), 0.01); // 0.50 + 0.00 + 1.00
assertEquals(2.0, (Double) result.get("holidayHours"), 0.01); // 0.00 + 2.00 + 0.00
```

### 2.8 部門別統計情報テスト

#### テストケース15: 部門統計計算
**メソッド**: `testGetDepartmentAttendanceStatistics_WithValidData_ShouldCalculateCorrectly`
**行**: 416-465

##### 部門横断集計テスト
```java
// 部門所属ユーザー (行418-422)
List<User> departmentUsers = Arrays.asList(
        createUser(1L, TEST_DEPARTMENT_ID),
        createUser(2L, TEST_DEPARTMENT_ID),
        createUser(3L, TEST_DEPARTMENT_ID)
);

// 各ユーザーのサマリーデータ (行424-434)
List<AttendanceSummary> user1Summaries = Arrays.asList(/* user1のデータ */);
List<AttendanceSummary> user2Summaries = Arrays.asList(/* user2のデータ */);
List<AttendanceSummary> user3Summaries = Collections.emptyList(); // データなし

// 部門統計の検証 (行448-456)
assertEquals(TEST_DEPARTMENT_ID, result.get("departmentId"));
assertEquals(3, result.get("userCount"));
assertEquals(3, result.get("totalRecords")); // user1: 2件, user2: 1件, user3: 0件
assertEquals(24.5, (Double) result.get("totalHours"), 0.01);
assertEquals(8.17, (Double) result.get("averageHoursPerUser"), 0.01); // 24.5 / 3
```

## 3. ヘルパーメソッド解析

### 3.1 テストデータ生成メソッド

#### createAttendanceSummary メソッド
**行**: 485-500
```java
private AttendanceSummary createAttendanceSummary(Long id, Integer userId, LocalDate targetDate,
                                                 String totalHours, String overtimeHours, 
                                                 String lateNightHours, String holidayHours, 
                                                 String summaryType) {
    AttendanceSummary summary = new AttendanceSummary();
    summary.setId(id);
    summary.setUserId(userId);
    summary.setTargetDate(targetDate);
    summary.setTotalHours(new BigDecimal(totalHours));
    summary.setOvertimeHours(new BigDecimal(overtimeHours));
    summary.setLateNightHours(new BigDecimal(lateNightHours));
    summary.setHolidayHours(new BigDecimal(holidayHours));
    summary.setSummaryType(summaryType);
    summary.setCreatedAt(OffsetDateTime.now(ZoneOffset.ofHours(9)));
    return summary;
}
```

**設計パターン**:
- ファクトリーメソッドパターン
- BigDecimal文字列コンストラクタによる精度保証
- 日本時間（JST）での作成日時設定

#### createAttendanceSummaryWithNulls メソッド
**行**: 502-513
```java
private AttendanceSummary createAttendanceSummaryWithNulls(Long id, Integer userId, LocalDate targetDate, 
                                                          String summaryType) {
    AttendanceSummary summary = new AttendanceSummary();
    summary.setId(id);
    summary.setUserId(userId);
    summary.setTargetDate(targetDate);
    summary.setTotalHours(null);
    summary.setOvertimeHours(null);
    summary.setLateNightHours(null);
    summary.setHolidayHours(null);
    summary.setSummaryType(summaryType);
    return summary;
}
```

**null値テスト専用**:
- null値耐性テストのための専用ファクトリー
- 集計処理でのnull値処理確認

#### createUser メソッド
**行**: 515-522
```java
private User createUser(Long id, Integer departmentId) {
    User user = new User();
    user.setId(id);
    user.setDepartmentId(departmentId);
    user.setUsername("user" + id);
    user.setEmail("user" + id + "@example.com");
    return user;
}
```

**部門テスト用**:
- 部門別統計テスト専用のユーザー生成
- 一意性を保証するID基準の命名

## 4. 集計サービス特有のテスト戦略

### 4.1 BigDecimal精度管理

#### 精度保証の重要性
```java
// 問題のあるコード（精度の不一致）
assertEquals(BigDecimal.ZERO, result.getTotalHours()); // scale不一致でエラー

// 改善されたコード（compareTo使用）
assertEquals(0, result.getTotalHours().compareTo(BigDecimal.ZERO));

// または文字列コンストラクタ使用
assertEquals(new BigDecimal("0.00"), result.getTotalHours());
```

#### 浮動小数点計算の検証
```java
// 集計結果の精度検証
assertEquals(24.5, (Double) result.get("totalHours"), 0.01); // 許容誤差0.01
assertEquals(8.17, averageDailyHours, 0.01); // 平均値の精度確認
```

### 4.2 統計計算テストの複雑性

#### 複数データ集計の検証
```java
// 段階的な集計検証
List<AttendanceSummary> summaries = Arrays.asList(
    createAttendanceSummary(1L, userId, date1, "8.00", "1.00", "0.50", "0.00", "daily"),
    createAttendanceSummary(2L, userId, date2, "7.50", "0.00", "0.00", "2.00", "daily"),
    createAttendanceSummary(3L, userId, date3, "9.00", "2.00", "1.00", "0.00", "daily")
);

// 期待値計算
// 総時間: 8.00 + 7.50 + 9.00 = 24.50
// 残業時間: 1.00 + 0.00 + 2.00 = 3.00
// 深夜時間: 0.50 + 0.00 + 1.00 = 1.50
// 休日時間: 0.00 + 2.00 + 0.00 = 2.00
```

#### null値処理の検証
```java
// null値を含むデータでの集計テスト
AttendanceSummary nullSummary = createAttendanceSummaryWithNulls(1L, userId, date, "daily");

// null値が0として処理されることを確認
assertEquals(0.0, (Double) result.get("totalHours"), 0.01);
```

### 4.3 エクスポート機能テストの戦略

#### CSV形式の検証
```java
// CSV出力の構造検証
String csvOutput = stringWriter.toString();

// ヘッダー行の確認
assertTrue(csvOutput.contains("Date,Total Hours,Overtime Hours,Late Night Hours,Holiday Hours"));

// データ行の確認
assertTrue(csvOutput.contains("2025-02-01,8.00,1.00,0.50,0.00"));

// 行数の確認
String[] lines = csvOutput.trim().split("\n");
assertEquals(expectedLineCount, lines.length);
```

#### JSON形式の検証
```java
// JSON構造の確認
assertTrue(jsonOutput.contains("["));
assertTrue(jsonOutput.contains("]"));

// 個別フィールドの確認
assertTrue(jsonOutput.contains("\"date\": \"2025-02-01\""));
assertTrue(jsonOutput.contains("\"totalHours\": 8.00"));

// 配列要素の区切り確認
assertTrue(jsonOutput.contains("},"));
```

## 5. テストケース作成の流れ

### 5.1 集計サービステスト専用フロー
```
1. 集計要件分析
   ↓
2. テストデータ準備（複数レコード）
   ↓
3. 期間・条件指定のモック設定
   ↓
4. 集計処理実行
   ↓
5. 計算結果検証（精度・正確性）
   ↓
6. エッジケース・null値処理追加
```

### 5.2 詳細手順

#### ステップ1: 集計要件分析
```java
/**
 * テストケース名: 月別統計情報の正確な計算
 * 集計要件:
 * - 指定期間内の全サマリーデータを取得
 * - 月別にグループ化して時間を合計
 * - 日平均勤務時間を計算
 * - BigDecimal精度で正確な計算を実行
 * 
 * 入力データ:
 * - 2月: 2件のサマリー（8.00h + 7.50h = 15.50h）
 * - 3月: 1件のサマリー（9.00h）
 * 
 * 期待結果:
 * - 月別時間: {"2025-02-01": 15.50, "2025-03-01": 9.00}
 * - 平均時間: (15.50 + 9.00) / 3 = 8.17h
 */
```

#### ステップ2: 複数レコードテストデータ準備
```java
// レベル1: 基本サマリーデータ
List<AttendanceSummary> summaries = new ArrayList<>();

// レベル2: 異なる月のデータ
summaries.add(createAttendanceSummary(1L, userId, LocalDate.of(2025, 2, 1), "8.00", "1.00", "0.50", "0.00", "daily"));
summaries.add(createAttendanceSummary(2L, userId, LocalDate.of(2025, 2, 2), "7.50", "0.00", "0.00", "0.00", "daily"));
summaries.add(createAttendanceSummary(3L, userId, LocalDate.of(2025, 3, 1), "9.00", "2.00", "1.00", "0.00", "daily"));

// レベル3: 期待結果データ
Map<String, Double> expectedMonthlyHours = Map.of(
    "2025-02-01", 15.50,
    "2025-03-01", 9.00
);
```

#### ステップ3: 期間指定モック設定
```java
// 期間指定での全件取得
when(attendanceSummaryRepository.findByTargetDateBetween(startDate, endDate))
    .thenReturn(summaries);

// ページング対応の取得
when(attendanceSummaryRepository.findByTargetDateBetween(startDate, endDate, pageable))
    .thenReturn(new PageImpl<>(summaries, pageable, summaries.size()));

// ユーザー・期間指定での取得
when(attendanceSummaryRepository.findByUserIdAndTargetDateBetween(userId, startDate, endDate))
    .thenReturn(userSummaries);
```

#### ステップ4: 段階的検証
```java
// 実行
Map<String, Object> result = attendanceSummaryService.getMonthlyStatistics(startDate, endDate);

// 段階1: null安全性
assertNotNull(result);

// 段階2: 基本統計情報
assertTrue(result.containsKey("monthlyHours"));
assertTrue(result.containsKey("averageDailyHours"));

// 段階3: 月別集計精度
@SuppressWarnings("unchecked")
Map<String, Double> monthlyHours = (Map<String, Double>) result.get("monthlyHours");
assertEquals(15.5, monthlyHours.get("2025-02-01"), 0.01);
assertEquals(9.0, monthlyHours.get("2025-03-01"), 0.01);

// 段階4: 平均計算精度
Double averageDailyHours = (Double) result.get("averageDailyHours");
assertEquals(8.17, averageDailyHours, 0.01);

// 段階5: 副作用確認
verify(attendanceSummaryRepository).findByTargetDateBetween(startDate, endDate);
```

## 6. テスト作成のコツとベストプラクティス

### 6.1 集計サービス特有の注意点

#### BigDecimal比較の正確性
```java
// 問題のあるコード（scale不一致）
assertEquals(BigDecimal.ZERO, result.getTotalHours()); // 0 vs 0.00でエラー

// 改善されたコード（compareTo使用）
assertEquals(0, result.getTotalHours().compareTo(BigDecimal.ZERO));

// または明示的なscale指定
assertEquals(new BigDecimal("0.00"), result.getTotalHours());
```

#### 浮動小数点精度の管理
```java
// 集計結果の比較は許容誤差を設定
assertEquals(24.5, (Double) result.get("totalHours"), 0.01);

// 平均値計算の精度確認
assertEquals(8.17, averageDailyHours, 0.01); // 小数点以下2位まで
```

### 6.2 エクスポート機能テストの最適化

#### StringWriterを使用したテスト
```java
// メモリ内でのファイル出力テスト
StringWriter stringWriter = new StringWriter();
PrintWriter printWriter = new PrintWriter(stringWriter);

// エクスポート実行
attendanceSummaryService.exportSummariesToCSV(summaries, printWriter);
printWriter.flush();

// 出力内容の検証
String csvOutput = stringWriter.toString();
assertNotNull(csvOutput);
assertTrue(csvOutput.contains("expected content"));
```

#### 形式別検証戦略
```java
// CSV形式の検証
private void assertCSVFormat(String csvOutput, List<AttendanceSummary> expectedData) {
    String[] lines = csvOutput.trim().split("\n");
    assertEquals(expectedData.size() + 1, lines.length); // ヘッダー + データ行
    
    // ヘッダー確認
    assertTrue(lines[0].contains("Date,Total Hours,Overtime Hours"));
    
    // データ行確認
    for (int i = 0; i < expectedData.size(); i++) {
        AttendanceSummary summary = expectedData.get(i);
        assertTrue(lines[i + 1].contains(summary.getTargetDate().toString()));
    }
}
```

### 6.3 部門別統計テストの複雑性

#### 複数ユーザーデータの準備
```java
// 部門所属ユーザーの準備
List<User> departmentUsers = Arrays.asList(
    createUser(1L, departmentId),
    createUser(2L, departmentId),
    createUser(3L, departmentId)
);

// 各ユーザーのサマリーデータを個別に準備
when(attendanceSummaryRepository.findByUserIdAndTargetDateBetween(1, startDate, endDate))
    .thenReturn(user1Summaries);
when(attendanceSummaryRepository.findByUserIdAndTargetDateBetween(2, startDate, endDate))
    .thenReturn(user2Summaries);
when(attendanceSummaryRepository.findByUserIdAndTargetDateBetween(3, startDate, endDate))
    .thenReturn(user3Summaries);
```

#### 部門統計の検証
```java
// 部門全体の統計確認
assertEquals(departmentId, result.get("departmentId"));
assertEquals(3, result.get("userCount"));

// 集計値の確認
double expectedTotalHours = calculateExpectedTotal(user1Summaries, user2Summaries, user3Summaries);
assertEquals(expectedTotalHours, (Double) result.get("totalHours"), 0.01);

// 平均値の確認
double expectedAverage = expectedTotalHours / departmentUsers.size();
assertEquals(expectedAverage, (Double) result.get("averageHoursPerUser"), 0.01);
```

## 7. 拡張テストケースの提案

### 7.1 実用的なテストケース

#### 大量データ処理テスト
```java
@Test
void testGetSummaryStatistics_LargeDataSet_PerformsEfficiently() {
    // 1000件のサマリーデータを準備
    List<AttendanceSummary> largeDataSet = IntStream.range(0, 1000)
        .mapToObj(i -> createAttendanceSummary(
            (long)i, 1, LocalDate.now().minusDays(i % 30), 
            "8.00", "1.00", "0.50", "0.00", "daily"))
        .collect(Collectors.toList());
    
    when(attendanceSummaryRepository.findByTargetDateBetween(any(), any()))
        .thenReturn(largeDataSet);
    
    long startTime = System.currentTimeMillis();
    Map<String, Object> result = attendanceSummaryService.getSummaryStatistics(startDate, endDate);
    long endTime = System.currentTimeMillis();
    
    assertNotNull(result);
    assertTrue(endTime - startTime < 1000); // 1秒以内で処理完了
}
```

#### 精度検証テスト
```java
@Test
void testStatisticsCalculation_PrecisionVerification() {
    // 小数点計算の精度確認
    List<AttendanceSummary> precisionData = Arrays.asList(
        createAttendanceSummary(1L, 1, LocalDate.now(), "8.33", "0.33", "0.17", "0.00", "daily"),
        createAttendanceSummary(2L, 1, LocalDate.now().minusDays(1), "7.67", "0.67", "0.83", "0.00", "daily")
    );
    
    when(attendanceSummaryRepository.findByTargetDateBetween(any(), any()))
        .thenReturn(precisionData);
    
    Map<String, Object> result = attendanceSummaryService.getSummaryStatistics(startDate, endDate);
    
    // 期待値: 8.33 + 7.67 = 16.00, 0.33 + 0.67 = 1.00
    assertEquals(16.0, (Double) result.get("totalHours"), 0.01);
    assertEquals(1.0, (Double) result.get("overtimeHours"), 0.01);
}
```

### 7.2 異常系テストケース

#### メモリ不足状況のテスト
```java
@Test
void testExportToCSV_MemoryUsage_WithinLimits() {
    // 大量データでのメモリ使用量確認
    List<AttendanceSummary> largeDataSet = createLargeDataSet(10000);
    
    Runtime runtime = Runtime.getRuntime();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
    
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    
    attendanceSummaryService.exportSummariesToCSV(largeDataSet, printWriter);
    
    runtime.gc();
    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // メモリ使用量が許容範囲内であることを確認
    long memoryUsed = afterMemory - beforeMemory;
    assertTrue(memoryUsed < 50 * 1024 * 1024); // 50MB以内
}
```

## 8. 一般的な問題と解決策

### 8.1 集計サービス特有の問題

#### BigDecimal精度の不一致
**問題**: 期待値と実際の値でscaleが異なる
```java
// 問題のあるコード
assertEquals(new BigDecimal("0"), result.getTotalHours()); // scale=0
// 実際の結果: 0.00 (scale=2)
```

**解決策**:
```java
// compareTo() を使用
assertEquals(0, new BigDecimal("0").compareTo(result.getTotalHours()));

// または明示的なscale指定
assertEquals(new BigDecimal("0.00"), result.getTotalHours());
```

#### 浮動小数点計算の精度問題
**問題**: 浮動小数点演算による微小な誤差
```java
// 問題のあるコード
assertEquals(8.17, averageDailyHours); // 精度の問題で失敗する可能性
```

**解決策**:
```java
// 許容誤差を設定
assertEquals(8.17, averageDailyHours, 0.01); // ±0.01の誤差を許容
```

### 8.2 エクスポート機能の問題

#### 文字エンコーディングの問題
**問題**: 日本語文字が正しく出力されない
```java
// 問題のあるコード
PrintWriter printWriter = new PrintWriter(outputStream); // デフォルトエンコーディング
```

**解決策**:
```java
// UTF-8エンコーディングを明示
PrintWriter printWriter = new PrintWriter(
    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
```

#### 大量データ出力時のメモリ問題
**問題**: 大量データを一度にメモリに読み込む
```java
// 問題のあるコード
List<AttendanceSummary> allData = repository.findAll(); // 全件メモリ読み込み
```

**解決策**:
```java
// ストリーミング処理またはページング処理
Pageable pageable = PageRequest.of(0, 1000);
Page<AttendanceSummary> page;
do {
    page = repository.findByTargetDateBetween(startDate, endDate, pageable);
    exportPage(page.getContent(), writer);
    pageable = page.nextPageable();
} while (page.hasNext());
```

## 9. まとめ

### 9.1 集計サービステストの重要ポイント
1. **精度管理**: BigDecimalとdoubleの適切な使い分けと比較方法
2. **統計計算**: 複数データの集計処理の正確性確認
3. **エクスポート機能**: CSV・JSON形式の正確な出力検証
4. **null値処理**: 不完全なデータに対する適切な処理確認
5. **パフォーマンス**: 大量データ処理時の効率性確認

### 9.2 テスト品質向上のチェックリスト
- [ ] 正常系・異常系・境界値テストを網羅
- [ ] BigDecimal比較は compareTo() または明示的scale指定を使用
- [ ] 浮動小数点比較は適切な許容誤差を設定
- [ ] エクスポート機能は実際の出力内容を検証
- [ ] null値処理は0値として適切に処理されることを確認
- [ ] 複数データ集計は段階的に検証
- [ ] パフォーマンスとメモリ使用量を考慮

### 9.3 他のサービステストとの違い
| 項目 | 集計サービステスト | 一般的なサービステスト |
|------|-------------------|----------------------|
| **データ量** | 大量（複数レコード集計） | 少量（単一レコード処理） |
| **計算精度** | 高（BigDecimal必須） | 中程度 |
| **統計処理** | 複雑（合計・平均・グループ化） | 単純 |
| **エクスポート** | 必須（CSV・JSON形式） | 通常不要 |
| **null値処理** | 重要（集計に影響） | 一般的 |
| **パフォーマンス** | 重要（大量データ処理） | 中程度 |

この手順書に従うことで、勤怠集計サービスの特性を考慮した包括的で信頼性の高いテストケースを作成できます。特にBigDecimal精度管理、統計計算、エクスポート機能の複雑性を適切に扱うことで、実用的なテストスイートを構築できます。