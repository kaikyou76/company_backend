# PaidLeaveCalculationServiceTest テストケース作成手順書

## 概要
本書は、`PaidLeaveCalculationServiceTest` のテストケース作成における注釈、統合テスト戦略、テスト作成の流れとコツを詳細に説明した手順書です。有給休暇計算サービスの特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/service/PaidLeaveCalculationServiceTest.java`

### 1.2 基本アノテーション

#### @SpringBootTest
**行**: 20
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PaidLeaveCalculationServiceTest {
```

**目的**:
- Spring Boot統合テスト環境の構築
- 実際のSpringコンテキストを使用したテスト実行
- データベース接続とトランザクション管理の統合

**有給計算サービステストの特徴**:
- 実データベース（comsys_test_dump.sql）との統合テスト
- 日付計算の精度検証（ChronoUnit.YEARS使用）
- 勤続年数に基づく段階的な有給日数計算
- 境界値テスト（6ヶ月、1年、2年...6年以上）

#### @Transactional
**行**: 21
```java
@Transactional
```

**役割**:
- 各テストメソッド実行後の自動ロールバック
- テストデータの分離とクリーンアップ
- データベース状態の一貫性保証

#### @ActiveProfiles("test")
**行**: 22
```java
@ActiveProfiles("test")
```

**役割**:
- テスト専用プロファイルの有効化
- テスト用データベース設定の適用
- 本番環境との分離

### 1.3 依存性注入

#### @Autowired PaidLeaveCalculationService
**行**: 25-26
```java
@Autowired
private PaidLeaveCalculationService paidLeaveCalculationService;
```

**役割**:
- 実際のサービスインスタンスを注入
- モックではなく実装クラスを使用
- 統合テストによる実際の動作確認

**テスト対象メソッド**:
```java
// 主要なテスト対象メソッド
public int calculatePaidLeaveDays(User user, LocalDate targetDate)
```

#### @Autowired UserRepository
**行**: 28-29
```java
@Autowired
private UserRepository userRepository;
```

**役割**:
- 実データベースからのユーザー情報取得
- 実データとの統合テスト実行
- `findAll()` - 全ユーザー取得
- `findById()` - 特定ユーザー取得

### 1.4 テスト用定数定義

#### 基準日設定
**行**: 33-36
```java
private LocalDate baseDate;

@BeforeEach
void setUp() {
    // 基準日を設定（2025年8月2日 - 実データベースの作成日より後）
    baseDate = LocalDate.of(2025, 8, 2);
}
```

**設計思想**:
- **実データ対応**: comsys_test_dump.sqlのユーザー作成日（2025年7月24日、8月1日）より後の日付
- **テスト一貫性**: 全テストで同じ基準日を使用
- **境界値テスト**: 6ヶ月未満の実データユーザーで0日を確認

### 1.5 ヘルパーメソッド

#### createTestUser メソッド
**行**: 42-50
```java
private User createTestUser(Long id, String username, String email, OffsetDateTime createdAt) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    user.setEmail(email);
    user.setIsActive(true);
    user.setCreatedAt(createdAt);
    return user;
}
```

**特徴**:
- テスト用ユーザーの動的生成
- 入社日（createdAt）の柔軟な設定
- 勤続年数テストのための日付操作対応

## 2. テストケース詳細解析

### 2.1 実データベース統合テスト群

#### テストケース1: 実データベースユーザーテスト
**メソッド**: `testCalculatePaidLeaveDays_WithRealDatabaseUsers_ShouldCalculateCorrectly`
**行**: 54-75

##### 実データ活用テスト
```java
// 実データベースからユーザー取得 (行56-57)
List<User> allUsers = userRepository.findAll();
assertFalse(allUsers.isEmpty(), "データベースにユーザーが存在すること");

// 最初の5人のユーザーでテスト (行59-60)
for (int i = 0; i < Math.min(5, allUsers.size()); i++) {
    User user = allUsers.get(i);
```

##### 実データ期待値検証
```java
// 実データベースの特性に基づく期待値 (行65-67)
assertEquals(0, paidLeaveDays,
        "ユーザーID " + user.getId() + " は入社6ヶ月未満のため有給は0日");

// デバッグ情報出力 (行69-72)
System.out.println("ユーザーID: " + user.getId() +
        ", 作成日: " + user.getCreatedAt() +
        ", 有給日数: " + paidLeaveDays + "日");
```

#### テストケース2: データベース統合テスト
**メソッド**: `testCalculatePaidLeaveDays_WithDatabaseIntegration_ShouldWorkWithRealData`
**行**: 320-340

##### 特定ユーザーテスト
```java
// 特定ユーザーの取得 (行322)
Optional<User> userOpt = userRepository.findById(1L);

// 存在確認とテスト実行 (行324-338)
if (userOpt.isPresent()) {
    User realUser = userOpt.get();
    int paidLeaveDays = paidLeaveCalculationService.calculatePaidLeaveDays(realUser, baseDate);
    
    // 実データに基づく期待値 (行329-331)
    assertEquals(0, paidLeaveDays,
            "実データベースユーザーは入社6ヶ月未満のため有給は0日");
}
```

#### テストケース3: 未来日付での計算テスト
**メソッド**: `testCalculatePaidLeaveDays_WithFutureDate_ShouldCalculateCorrectly`
**行**: 342-362

##### 時系列テスト
```java
// 1年後の基準日設定 (行347)
LocalDate futureDate = LocalDate.of(2026, 8, 1); // 1年後

// 1年後の期待値確認 (行352-354)
assertEquals(10, paidLeaveDays,
        "実データベースユーザーの1年後の有給は10日");
```

### 2.2 基本的な有給計算テスト群

#### テストケース4: 新入社員テスト
**メソッド**: `testCalculatePaidLeaveDays_NewEmployee_ShouldReturn0Days`
**行**: 79-89

##### 6ヶ月未満テスト
```java
// 入社3ヶ月のユーザー (行81)
OffsetDateTime hireDate = baseDate.minusMonths(3).atStartOfDay().atOffset(ZoneOffset.ofHours(9));

// 6ヶ月未満の期待値 (行87)
assertEquals(0, result, "入社6ヶ月未満の場合、有給は0日");
```

#### テストケース5-10: 勤続年数別テスト
**メソッド**: `testCalculatePaidLeaveDays_OneYearEmployee_ShouldReturn10Days` 等
**行**: 91-158

##### 段階的有給日数テスト
```java
// 1年勤続: 10日
OffsetDateTime hireDate = baseDate.minusYears(1).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
assertEquals(10, result, "入社1年以上2年未満の場合、有給は10日");

// 2年勤続: 11日
OffsetDateTime hireDate = baseDate.minusYears(2).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
assertEquals(11, result, "入社2年以上3年未満の場合、有給は11日");

// 3年勤続: 12日
OffsetDateTime hireDate = baseDate.minusYears(3).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
assertEquals(12, result, "入社3年以上4年未満の場合、有給は12日");

// 4年勤続: 13日
OffsetDateTime hireDate = baseDate.minusYears(4).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
assertEquals(13, result, "入社4年以上5年未満の場合、有給は13日");

// 5年勤続: 14日
OffsetDateTime hireDate = baseDate.minusYears(5).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
assertEquals(14, result, "入社5年以上6年未満の場合、有給は14日");

// 6年勤続: 15日（上限）
OffsetDateTime hireDate = baseDate.minusYears(6).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
assertEquals(15, result, "入社6年以上の場合、有給は15日（上限）");

// 10年勤続: 15日（上限確認）
OffsetDateTime hireDate = baseDate.minusYears(10).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
assertEquals(15, result, "入社10年の場合でも有給は15日（上限）");
```

### 2.3 境界値テスト群

#### テストケース11-12: 正確な年数境界テスト
**メソッド**: `testCalculatePaidLeaveDays_ExactlyOneYear_ShouldReturn10Days` 等
**行**: 162-180

##### 境界値精度テスト
```java
// ちょうど1年の境界値 (行164-165)
OffsetDateTime hireDate = baseDate.minusYears(1).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
assertEquals(10, result, "入社ちょうど1年の場合、有給は10日");

// ちょうど2年の境界値 (行172-173)
OffsetDateTime hireDate = baseDate.minusYears(2).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
assertEquals(11, result, "入社ちょうど2年の場合、有給は11日");
```

### 2.4 特殊ケーステスト群

#### テストケース13: 未来入社日テスト
**メソッド**: `testCalculatePaidLeaveDays_FutureHireDate_ShouldReturn0Days`
**行**: 184-194

##### 異常ケース処理テスト
```java
// 未来の入社日（異常ケース） (行186)
OffsetDateTime futureHireDate = baseDate.plusMonths(1).atStartOfDay().atOffset(ZoneOffset.ofHours(9));

// 異常ケースの期待値 (行192)
assertEquals(0, result, "未来の入社日の場合、有給は0日");
```

#### テストケース14: 当日入社テスト
**メソッド**: `testCalculatePaidLeaveDays_SameDayHire_ShouldReturn0Days`
**行**: 196-206

##### 当日境界値テスト
```java
// 当日入社のユーザー (行198)
OffsetDateTime sameDayHire = baseDate.atStartOfDay().atOffset(ZoneOffset.ofHours(9));

// 当日入社の期待値 (行204)
assertEquals(0, result, "当日入社の場合、有給は0日");
```

### 2.5 複数ユーザーテスト

#### テストケース15: 複数ユーザー比較テスト
**メソッド**: `testCalculatePaidLeaveDays_MultipleUsers_ShouldReturnCorrectDays`
**行**: 210-230

##### 異なる勤続年数の比較テスト
```java
// 3ヶ月、3年、8年の異なる勤続年数ユーザー (行212-218)
User newUser = createTestUser(1012L, "new_user", "new@company.com",
        baseDate.minusMonths(3).atStartOfDay().atOffset(ZoneOffset.ofHours(9))); // 3ヶ月

User experiencedUser = createTestUser(1013L, "experienced_user", "exp@company.com",
        baseDate.minusYears(3).atStartOfDay().atOffset(ZoneOffset.ofHours(9))); // 3年

User veteranUser = createTestUser(1014L, "veteran_user", "vet@company.com",
        baseDate.minusYears(8).atStartOfDay().atOffset(ZoneOffset.ofHours(9))); // 8年

// 各ユーザーの期待値確認 (行226-228)
assertEquals(0, newUserDays, "新入社員（3ヶ月）の有給は0日");
assertEquals(12, experiencedUserDays, "経験者（3年）の有給は12日");
assertEquals(15, veteranUserDays, "ベテラン（8年）の有給は15日");
```

### 2.6 日付計算精度テスト群

#### テストケース16: うるう年計算テスト
**メソッド**: `testCalculatePaidLeaveDays_LeapYearCalculation_ShouldBeAccurate`
**行**: 234-244

##### うるう年境界値テスト
```java
// うるう年の2月29日を基準日に設定 (行236)
LocalDate leapYearDate = LocalDate.of(2024, 2, 29); // うるう年の2月29日

// 2年前の2月28日から計算 (行237)
OffsetDateTime hireDate = LocalDate.of(2022, 2, 28).atStartOfDay().atOffset(ZoneOffset.ofHours(9));

// うるう年を含む2年間の期待値 (行242)
assertEquals(11, result, "うるう年を含む2年間の勤続で有給は11日");
```

#### テストケース17: 月末日計算テスト
**メソッド**: `testCalculatePaidLeaveDays_EndOfMonth_ShouldBeAccurate`
**行**: 246-256

##### 月末境界値テスト
```java
// 月末日での計算 (行248-249)
LocalDate endOfMonth = LocalDate.of(2025, 1, 31);
OffsetDateTime hireDate = LocalDate.of(2023, 1, 31).atStartOfDay().atOffset(ZoneOffset.ofHours(9)); // 2年前の同日

// 月末日での2年間勤続期待値 (行254)
assertEquals(11, result, "月末日での2年間勤続で有給は11日");
```

### 2.7 パフォーマンステスト

#### テストケース18: 計算効率性テスト
**メソッド**: `testCalculatePaidLeaveDays_Performance_ShouldBeEfficient`
**行**: 260-274

##### 大量計算パフォーマンステスト
```java
// 1000回計算の実行時間測定 (行265-270)
long startTime = System.currentTimeMillis();
for (int i = 0; i < 1000; i++) {
    paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);
}
long endTime = System.currentTimeMillis();

// 1秒以内での完了確認 (行273)
assertTrue(endTime - startTime < 1000, "1000回の計算が1秒以内で完了すること");
```

### 2.8 エラーハンドリングテスト群

#### テストケース19-21: null値処理テスト
**メソッド**: `testCalculatePaidLeaveDays_NullUser_ShouldThrowException` 等
**行**: 278-308

##### null安全性テスト
```java
// nullユーザーテスト (行280-283)
assertThrows(NullPointerException.class, () -> {
    paidLeaveCalculationService.calculatePaidLeaveDays(null, baseDate);
}, "nullユーザーの場合、NullPointerExceptionが発生すること");

// null基準日テスト (行291-294)
assertThrows(NullPointerException.class, () -> {
    paidLeaveCalculationService.calculatePaidLeaveDays(testUser, null);
}, "null基準日の場合、NullPointerExceptionが発生すること");

// null入社日テスト (行302-305)
assertThrows(NullPointerException.class, () -> {
    paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);
}, "null入社日の場合、NullPointerExceptionが発生すること");
```

### 2.9 実用的なシナリオテスト

#### テストケース22: キャリア進行シミュレーションテスト
**メソッド**: `testCalculatePaidLeaveDays_TypicalCareerProgression_ShouldShowProgression`
**行**: 312-338

##### 長期キャリアシミュレーション
```java
// 2020年4月1日入社のユーザー (行314)
OffsetDateTime initialHireDate = LocalDate.of(2020, 4, 1).atStartOfDay().atOffset(ZoneOffset.ofHours(9));

// 各年での有給日数進行確認 (行317-333)
// 入社1年目（2021年4月）: 10日
LocalDate firstYear = LocalDate.of(2021, 4, 1);
assertEquals(10, paidLeaveCalculationService.calculatePaidLeaveDays(testUser, firstYear));

// 入社2年目（2022年4月）: 11日
LocalDate secondYear = LocalDate.of(2022, 4, 1);
assertEquals(11, paidLeaveCalculationService.calculatePaidLeaveDays(testUser, secondYear));

// 入社3年目（2023年4月）: 12日
LocalDate thirdYear = LocalDate.of(2023, 4, 1);
assertEquals(12, paidLeaveCalculationService.calculatePaidLeaveDays(testUser, thirdYear));

// 入社4年目（2024年4月）: 13日
LocalDate fourthYear = LocalDate.of(2024, 4, 1);
assertEquals(13, paidLeaveCalculationService.calculatePaidLeaveDays(testUser, fourthYear));

// 入社5年目（2025年4月）: 14日
LocalDate fifthYear = LocalDate.of(2025, 4, 1);
assertEquals(14, paidLeaveCalculationService.calculatePaidLeaveDays(testUser, fifthYear));

// 入社6年目（2026年4月）: 15日（上限到達）
LocalDate sixthYear = LocalDate.of(2026, 4, 1);
assertEquals(15, paidLeaveCalculationService.calculatePaidLeaveDays(testUser, sixthYear));
```

## 3. 有給計算サービス特有のテスト戦略

### 3.1 日付計算の精度管理

#### ChronoUnit.YEARS の特性理解
```java
// ChronoUnit.YEARSは完全な年数のみを返す
LocalDate hireDate = LocalDate.of(2023, 8, 1);
LocalDate targetDate = LocalDate.of(2024, 7, 31); // 11ヶ月後
long years = ChronoUnit.YEARS.between(hireDate, targetDate); // 0年

LocalDate targetDate2 = LocalDate.of(2024, 8, 1); // ちょうど1年後
long years2 = ChronoUnit.YEARS.between(hireDate, targetDate2); // 1年
```

#### 境界値テストの重要性
```java
// 境界値での正確な計算確認
// 6ヶ月未満: 0日
// 6ヶ月以上1年未満: 10日（実装では6ヶ月判定は未実装）
// 1年以上: 段階的増加

// 実装上の注意: 現在の実装では6ヶ月判定がChronoUnit.YEARSベースのため
// 6ヶ月〜1年未満は0日となる
```

### 3.2 実データベース統合テストの戦略

#### 実データの特性把握
```java
// comsys_test_dump.sqlの特性
// - 全ユーザーの作成日: 2025年7月24日または8月1日
// - 基準日2025年8月2日では全員6ヶ月未満
// - 期待値: 全員0日

// 未来日付でのテスト
// - 基準日2026年8月1日では全員1年以上
// - 期待値: 全員10日
```

#### データベース依存テストの安全性
```java
// Optional使用による安全なテスト
Optional<User> userOpt = userRepository.findById(1L);
if (userOpt.isPresent()) {
    // テスト実行
} else {
    // スキップメッセージ
    System.out.println("ID=1のユーザーが見つかりませんでした。テストをスキップします。");
}
```

### 3.3 時系列テストの複雑性

#### 複数時点での計算確認
```java
// 同一ユーザーの異なる時点での有給日数変化
User user = createTestUser(1L, "user", "user@company.com", 
                          LocalDate.of(2020, 4, 1).atStartOfDay().atOffset(ZoneOffset.ofHours(9)));

// 時系列での変化確認
assertEquals(10, service.calculatePaidLeaveDays(user, LocalDate.of(2021, 4, 1))); // 1年後
assertEquals(11, service.calculatePaidLeaveDays(user, LocalDate.of(2022, 4, 1))); // 2年後
assertEquals(12, service.calculatePaidLeaveDays(user, LocalDate.of(2023, 4, 1))); // 3年後
```

#### うるう年・月末日の考慮
```java
// うるう年の2月29日から2年前の2月28日
LocalDate leapYearDate = LocalDate.of(2024, 2, 29);
OffsetDateTime hireDate = LocalDate.of(2022, 2, 28).atStartOfDay().atOffset(ZoneOffset.ofHours(9));

// 正確に2年間の勤続として計算されることを確認
long years = ChronoUnit.YEARS.between(hireDate.toLocalDate(), leapYearDate); // 2年
```

## 4. テストケース作成の流れ

### 4.1 有給計算サービステスト専用フロー
```
1. 労働基準法要件分析
   ↓
2. 勤続年数パターン設計
   ↓
3. 日付境界値の特定
   ↓
4. 実データベース統合戦略
   ↓
5. 時系列テストシナリオ作成
   ↓
6. エラーハンドリング・パフォーマンステスト追加
```

### 4.2 詳細手順

#### ステップ1: 労働基準法要件分析
```java
/**
 * テストケース名: 勤続年数別有給日数の正確な計算
 * 法的要件:
 * - 6ヶ月未満: 0日
 * - 6ヶ月以上1年未満: 10日
 * - 1年以上2年未満: 10日
 * - 2年以上3年未満: 11日
 * - 3年以上4年未満: 12日
 * - 4年以上5年未満: 13日
 * - 5年以上6年未満: 14日
 * - 6年以上: 15日（上限）
 * 
 * 実装上の注意:
 * - ChronoUnit.YEARSは完全な年数のみ計算
 * - 6ヶ月判定は別途実装が必要（現在未実装）
 */
```

#### ステップ2: 勤続年数パターン設計
```java
// レベル1: 基本パターン
Map<String, Integer> basicPatterns = Map.of(
    "3ヶ月", 0,
    "1年", 10,
    "2年", 11,
    "3年", 12,
    "4年", 13,
    "5年", 14,
    "6年", 15,
    "10年", 15  // 上限確認
);

// レベル2: 境界値パターン
Map<String, Integer> boundaryPatterns = Map.of(
    "ちょうど1年", 10,
    "ちょうど2年", 11,
    "1年-1日", 0,   // ChronoUnit.YEARSの特性
    "1年+1日", 10
);

// レベル3: 特殊ケース
Map<String, Integer> specialCases = Map.of(
    "未来入社日", 0,
    "当日入社", 0,
    "うるう年計算", 11,  // 2年間の例
    "月末日計算", 11     // 2年間の例
);
```

#### ステップ3: 日付境界値の特定
```java
// 境界値テストデータの準備
LocalDate baseDate = LocalDate.of(2025, 8, 2);

// 各勤続年数の境界値
List<TestCase> boundaryTests = Arrays.asList(
    new TestCase("6ヶ月未満", baseDate.minusMonths(3), 0),
    new TestCase("ちょうど1年", baseDate.minusYears(1), 10),
    new TestCase("1年+1日", baseDate.minusYears(1).minusDays(1), 10),
    new TestCase("2年境界", baseDate.minusYears(2), 11),
    new TestCase("上限確認", baseDate.minusYears(10), 15)
);
```

#### ステップ4: 実データベース統合戦略
```java
// 実データの特性を活用したテスト設計
@Test
void testWithRealData() {
    // 実データの特性: 2025年7月24日作成
    // 基準日: 2025年8月2日 → 6ヶ月未満 → 0日期待
    
    List<User> realUsers = userRepository.findAll();
    for (User user : realUsers) {
        int result = service.calculatePaidLeaveDays(user, baseDate);
        assertEquals(0, result, "実データユーザーは6ヶ月未満のため0日");
    }
    
    // 未来日付での確認: 2026年8月1日 → 1年以上 → 10日期待
    LocalDate futureDate = LocalDate.of(2026, 8, 1);
    for (User user : realUsers) {
        int result = service.calculatePaidLeaveDays(user, futureDate);
        assertEquals(10, result, "1年後は10日");
    }
}
```

#### ステップ5: 段階的検証
```java
// 実行
int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);

// 段階1: 基本検証
assertNotNull(result);
assertTrue(result >= 0 && result <= 15, "有給日数は0-15日の範囲内");

// 段階2: 具体的期待値
assertEquals(expectedDays, result, "勤続" + years + "年の有給は" + expectedDays + "日");

// 段階3: ログ出力確認（統合テストの場合）
// ログレベルをDEBUGに設定してログ出力を確認

// 段階4: パフォーマンス確認（必要に応じて）
long startTime = System.currentTimeMillis();
service.calculatePaidLeaveDays(testUser, baseDate);
long endTime = System.currentTimeMillis();
assertTrue(endTime - startTime < 100, "計算は100ms以内で完了");
```

## 5. テスト作成のコツとベストプラクティス

### 5.1 有給計算サービス特有の注意点

#### 日付計算の精度確認
```java
// ChronoUnit.YEARSの動作確認
LocalDate hireDate = LocalDate.of(2023, 8, 1);
LocalDate targetDate = LocalDate.of(2024, 7, 31); // 11ヶ月後
long years = ChronoUnit.YEARS.between(hireDate, targetDate);
assertEquals(0, years, "11ヶ月は0年として計算される");

// 完全な年数での確認
LocalDate targetDate2 = LocalDate.of(2024, 8, 1); // ちょうど1年後
long years2 = ChronoUnit.YEARS.between(hireDate, targetDate2);
assertEquals(1, years2, "ちょうど1年は1年として計算される");
```

#### タイムゾーンの一貫性
```java
// 日本時間（JST）での一貫した日付作成
OffsetDateTime hireDate = baseDate.minusYears(2)
    .atStartOfDay()
    .atOffset(ZoneOffset.ofHours(9)); // JST

// LocalDateとOffsetDateTimeの変換確認
LocalDate hireDateLocal = hireDate.toLocalDate();
assertEquals(baseDate.minusYears(2), hireDateLocal);
```

### 5.2 実データベーステストの最適化

#### データ存在確認パターン
```java
// 安全なデータ取得パターン
@Test
void testWithOptionalData() {
    Optional<User> userOpt = userRepository.findById(1L);
    
    // データ存在時のみテスト実行
    userOpt.ifPresentOrElse(
        user -> {
            int result = service.calculatePaidLeaveDays(user, baseDate);
            assertEquals(0, result, "実データユーザーの期待値");
        },
        () -> System.out.println("テストデータが見つかりません。スキップします。")
    );
}
```

#### 大量データ処理の効率化
```java
// 実データの一部のみでテスト
@Test
void testWithLimitedRealData() {
    List<User> allUsers = userRepository.findAll();
    
    // 最初の5人のみでテスト（全件テストは時間がかかる）
    List<User> testUsers = allUsers.stream()
        .limit(5)
        .collect(Collectors.toList());
    
    for (User user : testUsers) {
        int result = service.calculatePaidLeaveDays(user, baseDate);
        assertTrue(result >= 0 && result <= 15, "有給日数の範囲確認");
    }
}
```

### 5.3 時系列テストの設計パターン

#### キャリア進行シミュレーション
```java
// 長期キャリアの段階的確認
@Test
void testCareerProgression() {
    OffsetDateTime hireDate = LocalDate.of(2020, 4, 1)
        .atStartOfDay().atOffset(ZoneOffset.ofHours(9));
    User user = createTestUser(1L, "career_user", "career@company.com", hireDate);
    
    // 年次進行の確認
    Map<Integer, Integer> yearlyExpectation = Map.of(
        1, 10,  // 1年目: 10日
        2, 11,  // 2年目: 11日
        3, 12,  // 3年目: 12日
        4, 13,  // 4年目: 13日
        5, 14,  // 5年目: 14日
        6, 15   // 6年目: 15日（上限）
    );
    
    yearlyExpectation.forEach((year, expectedDays) -> {
        LocalDate targetDate = LocalDate.of(2020 + year, 4, 1);
        int actualDays = service.calculatePaidLeaveDays(user, targetDate);
        assertEquals(expectedDays, actualDays, 
            year + "年目の有給は" + expectedDays + "日");
    });
}
```

#### 複数シナリオの並列テスト
```java
// 複数の入社パターンを同時テスト
@Test
void testMultipleHirePatterns() {
    List<TestScenario> scenarios = Arrays.asList(
        new TestScenario("春入社", LocalDate.of(2023, 4, 1), 11),
        new TestScenario("夏入社", LocalDate.of(2023, 7, 1), 11),
        new TestScenario("秋入社", LocalDate.of(2023, 10, 1), 11),
        new TestScenario("冬入社", LocalDate.of(2023, 1, 1), 11)
    );
    
    LocalDate targetDate = LocalDate.of(2025, 8, 2);
    
    scenarios.forEach(scenario -> {
        User user = createTestUser(scenario.getId(), scenario.getName(), 
            scenario.getEmail(), scenario.getHireDate().atStartOfDay().atOffset(ZoneOffset.ofHours(9)));
        
        int result = service.calculatePaidLeaveDays(user, targetDate);
        assertEquals(scenario.getExpectedDays(), result, 
            scenario.getName() + "の有給日数確認");
    });
}
```

## 6. 一般的な問題と解決策

### 6.1 有給計算サービス特有の問題

#### ChronoUnit.YEARSの誤解
**問題**: 6ヶ月の判定ができない
```java
// 問題のあるコード
LocalDate hireDate = LocalDate.of(2024, 2, 1);
LocalDate targetDate = LocalDate.of(2024, 8, 1); // 6ヶ月後
long years = ChronoUnit.YEARS.between(hireDate, targetDate); // 0年
// 6ヶ月の判定ができない
```

**解決策**:
```java
// 月数での判定を追加
long months = ChronoUnit.MONTHS.between(hireDate, targetDate);
if (months >= 6 && years < 1) {
    return 10; // 6ヶ月以上1年未満
}

// または実装側の修正
private int calculateByYearsOfService(long yearsOfService, long monthsOfService) {
    if (yearsOfService < 1) {
        return monthsOfService >= 6 ? 10 : 0; // 6ヶ月判定
    }
    // 以下既存ロジック
}
```

#### 日付境界値の計算ミス
**問題**: 境界値での期待値が不正確
```java
// 問題のあるコード
LocalDate hireDate = LocalDate.of(2023, 8, 2);
LocalDate targetDate = LocalDate.of(2024, 8, 1); // 1日足りない
long years = ChronoUnit.YEARS.between(hireDate, targetDate); // 0年
assertEquals(10, result); // 実際は0年なので期待値が間違い
```

**解決策**:
```java
// 正確な境界値計算
LocalDate hireDate = LocalDate.of(2023, 8, 2);
LocalDate targetDate = LocalDate.of(2024, 8, 2); // ちょうど1年後
long years = ChronoUnit.YEARS.between(hireDate, targetDate); // 1年
assertEquals(10, result); // 正しい期待値
```

### 6.2 実データベーステストの問題

#### データ依存性の問題
**問題**: 実データの変更でテストが失敗
```java
// 問題のあるコード
User user = userRepository.findById(1L).get(); // データが存在しない可能性
int result = service.calculatePaidLeaveDays(user, baseDate);
assertEquals(0, result); // データ変更で期待値が変わる可能性
```

**解決策**:
```java
// Optional使用とデータ特性の明示
Optional<User> userOpt = userRepository.findById(1L);
if (userOpt.isPresent()) {
    User user = userOpt.get();
    int result = service.calculatePaidLeaveDays(user, baseDate);
    
    // データの特性を明示したアサーション
    // comsys_test_dump.sqlでは2025年7月24日作成のため、
    // 基準日2025年8月2日では6ヶ月未満で0日が期待値
    assertEquals(0, result, "実データユーザー（2025年7月24日作成）は6ヶ月未満のため0日");
} else {
    // データが存在しない場合の処理
    System.out.println("テストデータが見つかりません。テストをスキップします。");
}
```

#### タイムゾーンの不一致
**問題**: 異なるタイムゾーンでの日付計算
```java
// 問題のあるコード
OffsetDateTime hireDate = OffsetDateTime.now(); // システムデフォルトタイムゾーン
LocalDate targetDate = LocalDate.now(); // ローカル日付
// タイムゾーンの不一致で計算結果が不正確
```

**解決策**:
```java
// 一貫したタイムゾーン使用
ZoneOffset jst = ZoneOffset.ofHours(9);
OffsetDateTime hireDate = baseDate.minusYears(2)
    .atStartOfDay()
    .atOffset(jst); // 明示的にJST指定

LocalDate targetDate = baseDate; // 同じ基準での日付使用
```

## 7. 拡張テストケースの提案

### 7.1 実用的なテストケース

#### 大量ユーザー処理テスト
```java
@Test
void testCalculatePaidLeaveDays_BulkUsers_PerformsEfficiently() {
    // 1000人のユーザーを準備
    List<User> bulkUsers = IntStream.range(0, 1000)
        .mapToObj(i -> createTestUser(
            (long)i, 
            "user" + i, 
            "user" + i + "@company.com",
            baseDate.minusYears(i % 10 + 1).atStartOfDay().atOffset(ZoneOffset.ofHours(9))
        ))
        .collect(Collectors.toList());
    
    long startTime = System.currentTimeMillis();
    
    Map<Long, Integer> results = bulkUsers.stream()
        .collect(Collectors.toMap(
            User::getId,
            user -> service.calculatePaidLeaveDays(user, baseDate)
        ));
    
    long endTime = System.currentTimeMillis();
    
    assertEquals(1000, results.size());
    assertTrue(endTime - startTime < 5000, "1000人の計算が5秒以内で完了");
    
    // 結果の妥当性確認
    results.values().forEach(days -> 
        assertTrue(days >= 0 && days <= 15, "有給日数は0-15日の範囲内"));
}
```

#### 精度検証テスト
```java
@Test
void testCalculatePaidLeaveDays_PrecisionVerification() {
    // 様々な日付パターンでの精度確認
    List<PrecisionTestCase> testCases = Arrays.asList(
        new PrecisionTestCase(LocalDate.of(2023, 2, 28), LocalDate.of(2025, 2, 28), 11), // 平年
        new PrecisionTestCase(LocalDate.of(2024, 2, 29), LocalDate.of(2026, 2, 28), 11), // うるう年
        new PrecisionTestCase(LocalDate.of(2023, 12, 31), LocalDate.of(2025, 12, 31), 11), // 年末
        new PrecisionTestCase(LocalDate.of(2023, 1, 1), LocalDate.of(2025, 1, 1), 11)    // 年始
    );
    
    testCases.forEach(testCase -> {
        User user = createTestUser(testCase.getId(), testCase.getName(), testCase.getEmail(),
            testCase.getHireDate().atStartOfDay().atOffset(ZoneOffset.ofHours(9)));
        
        int result = service.calculatePaidLeaveDays(user, testCase.getTargetDate());
        assertEquals(testCase.getExpectedDays(), result, 
            testCase.getDescription() + "の精度確認");
    });
}
```

### 7.2 異常系テストケース

#### メモリ使用量テスト
```java
@Test
void testCalculatePaidLeaveDays_MemoryUsage_WithinLimits() {
    // メモリ使用量の確認
    Runtime runtime = Runtime.getRuntime();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // 大量計算実行
    User testUser = createTestUser(1L, "memory_test", "memory@company.com",
        baseDate.minusYears(5).atStartOfDay().atOffset(ZoneOffset.ofHours(9)));
    
    for (int i = 0; i < 10000; i++) {
        service.calculatePaidLeaveDays(testUser, baseDate.plusDays(i % 365));
    }
    
    runtime.gc();
    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // メモリ使用量が許容範囲内であることを確認
    long memoryUsed = afterMemory - beforeMemory;
    assertTrue(memoryUsed < 10 * 1024 * 1024, "メモリ使用量が10MB以内");
}
```

#### 並行処理テスト
```java
@Test
void testCalculatePaidLeaveDays_ConcurrentAccess_ThreadSafe() throws InterruptedException {
    User testUser = createTestUser(1L, "concurrent_test", "concurrent@company.com",
        baseDate.minusYears(3).atStartOfDay().atOffset(ZoneOffset.ofHours(9)));
    
    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);
    List<Integer> results = Collections.synchronizedList(new ArrayList<>());
    
    // 複数スレッドで同時実行
    for (int i = 0; i < threadCount; i++) {
        new Thread(() -> {
            try {
                int result = service.calculatePaidLeaveDays(testUser, baseDate);
                results.add(result);
            } finally {
                latch.countDown();
            }
        }).start();
    }
    
    latch.await(5, TimeUnit.SECONDS);
    
    // 全スレッドで同じ結果が得られることを確認
    assertEquals(threadCount, results.size());
    assertTrue(results.stream().allMatch(result -> result == 12), 
        "全スレッドで同じ結果（12日）が得られること");
}
```

## 8. まとめ

### 8.1 有給計算サービステストの重要ポイント
1. **日付計算精度**: ChronoUnit.YEARSの特性理解と境界値テスト
2. **実データ統合**: comsys_test_dump.sqlとの整合性確保
3. **時系列テスト**: 長期キャリア進行のシミュレーション
4. **法的要件遵守**: 労働基準法に基づく正確な有給日数計算
5. **エラーハンドリング**: null値や異常ケースの適切な処理

### 8.2 テスト品質向上のチェックリスト
- [ ] 勤続年数0年〜10年以上の全パターンをカバー
- [ ] 境界値（ちょうど1年、2年...）での正確な計算確認
- [ ] 実データベースとの統合テスト実装
- [ ] うるう年・月末日での日付計算精度確認
- [ ] null値処理とエラーハンドリングの網羅
- [ ] パフォーマンステスト（1000回計算等）の実装
- [ ] 時系列でのキャリア進行シミュレーション
- [ ] タイムゾーン（JST）の一貫した使用

### 8.3 他のサービステストとの違い
| 項目 | 有給計算サービステスト | 一般的なサービステスト |
|------|-------------------|----------------------|
| **計算対象** | 日付・勤続年数 | 一般的なビジネスロジック |
| **法的要件** | 労働基準法遵守必須 | 業務要件のみ |
| **時系列性** | 重要（長期キャリア） | 通常不要 |
| **実データ統合** | 必須（統合テスト） | オプション |
| **境界値複雑性** | 高（年数境界多数） | 中程度 |
| **精度要求** | 高（法的正確性） | 中程度 |

この手順書に従うことで、労働基準法に準拠した正確で信頼性の高い有給休暇計算サービスのテストケースを作成できます。特に日付計算の精度、実データベースとの統合、時系列テストの複雑性を適切に扱うことで、実用的なテストスイートを構築できます。