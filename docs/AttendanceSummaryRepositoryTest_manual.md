# AttendanceSummaryRepositoryTest テストケース作成手順書

## 概要
本書は、`AttendanceSummaryRepositoryTest` のテストケース作成における注釈、データベース接続、テスト作成の流れとコツを詳細に説明した手順書です。勤怠集計データアクセス層の特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/repository/AttendanceSummaryRepositoryTest.java`

### 1.2 基本アノテーション

#### @SpringBootTest
**行**: 25
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AttendanceSummaryRepositoryTest {
```

**目的**:
- Spring Boot統合テスト環境の構築
- 実際のデータベース接続とトランザクション管理
- テストプロファイルによる設定分離

**集計データテストの特徴**:
- 実際のPostgreSQLデータベース（comsys_test_dump.sql）を使用
- JPA/Hibernateの動作確認
- BigDecimal型の精度保持テスト
- 日付範囲検索とページネーション機能の検証
- 集計データの整合性確認

#### @Transactional
**目的**:
- 各テストメソッド実行後の自動ロールバック
- テスト間のデータ独立性保証
- データベース状態のクリーンアップ

#### @ActiveProfiles("test")
**目的**:
- テスト専用データベース設定の適用
- `application-test.properties` の設定読み込み
- 本番環境との分離

### 1.3 依存性注入

#### @Autowired AttendanceSummaryRepository
**行**: 29-30
```java
@Autowired
private AttendanceSummaryRepository attendanceSummaryRepository;
```

**役割**:
- Spring Data JPAリポジトリの自動注入
- 実際のデータベース操作の実行
- 集計データ専用クエリメソッドのテスト対象

**テスト対象メソッド**:
```java
// 基本検索メソッド
findByUserId(Integer userId)
findBySummaryType(String summaryType)
findByTargetDate(LocalDate targetDate)

// 複合条件検索メソッド
findByUserIdAndTargetDate(Integer userId, LocalDate targetDate)
findByUserIdAndSummaryType(Integer userId, String summaryType)

// 期間検索メソッド
findByUserIdAndTargetDateBetween(Integer userId, LocalDate startDate, LocalDate endDate)
findByTargetDateBetween(LocalDate startDate, LocalDate endDate)
findByTargetDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable)

// 複合期間検索メソッド
findByUserIdAndSummaryTypeAndTargetDateBetween(Integer userId, String summaryType, LocalDate startDate, LocalDate endDate)
findBySummaryTypeAndTargetDateBetween(String summaryType, LocalDate startDate, LocalDate endDate)
```

### 1.4 テスト用定数定義

#### 実データベース対応定数
**行**: 32-39
```java
// テスト用定数（comsys_test_dump.sqlの実際のデータを使用）
private static final Integer TEST_USER_ID_1 = 1; // ceo@company.com
private static final Integer TEST_USER_ID_2 = 2; // director@company.com
private static final LocalDate TEST_DATE = LocalDate.of(2025, 2, 1);
private static final LocalDate START_DATE = LocalDate.of(2025, 2, 1);
private static final LocalDate END_DATE = LocalDate.of(2025, 2, 28);
private static final String SUMMARY_TYPE_DAILY = "daily";
private static final String SUMMARY_TYPE_MONTHLY = "monthly";
```

**設計思想**:
- **実データ活用**: comsys_test_dump.sqlの実際のユーザーIDを使用
- **日付統一**: 2025年2月を基準とした一貫した日付設定
- **テスト一貫性**: 全テストで同じ基準データを使用
- **外部キー制約**: 実在するユーザーIDのみ使用して制約違反を回避
- **集計タイプ**: daily（日次）とmonthly（月次）の2種類をサポート

### 1.5 テストデータ準備

#### @BeforeEach セットアップ
**行**: 45-66
```java
@BeforeEach
void setUp() {
    // テストデータの準備（異なる日付を使用して重複を避ける）
    dailySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE,
            new BigDecimal("8.00"), new BigDecimal("1.00"),
            new BigDecimal("0.50"), new BigDecimal("0.00"),
            SUMMARY_TYPE_DAILY);

    dailySummary2 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE.plusDays(1),
            new BigDecimal("7.50"), new BigDecimal("0.00"),
            new BigDecimal("0.00"), new BigDecimal("2.00"),
            SUMMARY_TYPE_DAILY);

    // 月次サマリーは異なる日付を使用
    monthlySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE.plusDays(2),
            new BigDecimal("160.00"), new BigDecimal("20.00"),
            new BigDecimal("5.00"), new BigDecimal("8.00"),
            SUMMARY_TYPE_MONTHLY);

    // データベースに保存
    dailySummary1 = attendanceSummaryRepository.save(dailySummary1);
    dailySummary2 = attendanceSummaryRepository.save(dailySummary2);
    monthlySummary1 = attendanceSummaryRepository.save(monthlySummary1);
}
```

**重要ポイント**:
- **日付重複回避**: 各サマリーで異なる日付を使用してユニーク制約違反を防止
- **BigDecimal使用**: 時間数の精度保持のためBigDecimal型を使用
- **データ多様性**: 日次と月次の異なる集計タイプでテスト
- **保存後ID取得**: save()後にIDを取得して後続テストで使用

## 2. 主要テストケース解析

### 2.1 基本検索テスト群

#### テストケース1: 正常なユーザーID検索
**メソッド**: `testFindByUserId_WithExistingUser_ShouldReturnSummaries`

##### 空データベース前提の検証
```java
// When
List<AttendanceSummary> result = attendanceSummaryRepository.findByUserId(TEST_USER_ID_1);

// Then
assertNotNull(result);
assertEquals(3, result.size()); // テストデータのみ（データベースは空）
assertTrue(result.stream().allMatch(summary -> summary.getUserId().equals(TEST_USER_ID_1)));
```

**重要ポイント**:
- **空データベース前提**: attendance_summariesテーブルは空のため、正確な件数を期待
- **データ整合性**: 全レコードが指定ユーザーIDと一致することを確認
- **null安全性**: 結果がnullでないことを最初に確認

#### テストケース2: 存在しないユーザーの処理
**メソッド**: `testFindByUserId_WithNonExistentUser_ShouldReturnEmptyList`

##### 境界値テスト
```java
// When
List<AttendanceSummary> result = attendanceSummaryRepository.findByUserId(999);

// Then
assertNotNull(result);
assertTrue(result.isEmpty());
```

**設計思想**:
- **存在しないID**: 999は実データベースに存在しないユーザーID
- **空リスト確認**: nullではなく空のListが返されることを確認
- **例外なし**: 存在しないIDでも例外が発生しないことを確認

#### テストケース3: 集計タイプ別フィルタリング
**メソッド**: `testFindBySummaryType_WithValidType_ShouldReturnFilteredSummaries`

##### タイプ別検索テスト
```java
// When
List<AttendanceSummary> dailyResults = attendanceSummaryRepository.findBySummaryType(SUMMARY_TYPE_DAILY);
List<AttendanceSummary> monthlyResults = attendanceSummaryRepository.findBySummaryType(SUMMARY_TYPE_MONTHLY);

// Then
assertNotNull(dailyResults);
assertEquals(2, dailyResults.size()); // テストデータのみ
assertTrue(dailyResults.stream().allMatch(summary -> SUMMARY_TYPE_DAILY.equals(summary.getSummaryType())));

assertNotNull(monthlyResults);
assertEquals(1, monthlyResults.size()); // テストデータのみ
assertTrue(monthlyResults.stream().allMatch(summary -> SUMMARY_TYPE_MONTHLY.equals(summary.getSummaryType())));
```

**検証ポイント**:
- **フィルタリング精度**: 指定したタイプのレコードのみが返されることを確認
- **データ分離**: 日次と月次のレコードが適切に分離されることを確認
- **Stream API活用**: 全要素が条件を満たすことを効率的に検証

### 2.2 日付検索テスト群

#### テストケース4: 日付指定検索
**メソッド**: `testFindByTargetDate_WithValidDate_ShouldReturnSummaries`

##### 日付完全一致検索
```java
// When
List<AttendanceSummary> result = attendanceSummaryRepository.findByTargetDate(TEST_DATE);

// Then
assertNotNull(result);
assertEquals(1, result.size()); // dailySummary1のみ（monthlySummary1は異なる日付）
assertTrue(result.stream().allMatch(summary -> summary.getTargetDate().equals(TEST_DATE)));
```

**日付処理の特徴**:
- **完全一致**: LocalDateによる日付の完全一致検索
- **重複回避設計**: 異なる日付を使用してテストデータの重複を回避
- **境界値処理**: 指定日付のレコードのみを正確に取得

#### テストケース5: 存在しない日付の検索
**メソッド**: `testFindByTargetDate_WithNonExistentDate_ShouldReturnEmptyList`

##### 未来日付での境界値テスト
```java
// Given
LocalDate nonExistentDate = LocalDate.of(2030, 12, 31);

// When
List<AttendanceSummary> result = attendanceSummaryRepository.findByTargetDate(nonExistentDate);

// Then
assertNotNull(result);
assertTrue(result.isEmpty());
```

**境界値テストの重要性**:
- **未来日付**: 2030年の日付で存在しないデータの検索
- **空結果確認**: 該当データがない場合の適切な空リスト返却
- **例外なし**: 存在しない日付でも例外が発生しないことを確認

#### テストケース6: ユーザーと日付の複合検索
**メソッド**: `testFindByUserIdAndTargetDate_WithValidData_ShouldReturnSummary`

##### Optional型戻り値テスト
```java
// When
Optional<AttendanceSummary> result = attendanceSummaryRepository.findByUserIdAndTargetDate(
        TEST_USER_ID_1, TEST_DATE);

// Then
assertTrue(result.isPresent());
assertEquals(TEST_USER_ID_1, result.get().getUserId());
assertEquals(TEST_DATE, result.get().getTargetDate());
```

**Optional型の活用**:
- **単一結果期待**: ユーザーIDと日付の組み合わせで一意のレコードを期待
- **Optional活用**: 結果が存在しない可能性を考慮したOptional型の使用
- **複合キー検索**: 複数条件での正確な検索結果確認

### 2.3 複合条件検索テスト

#### テストケース7: ユーザーと集計タイプの複合検索
**メソッド**: `testFindByUserIdAndSummaryType_WithValidData_ShouldReturnFilteredSummaries`

##### 複合フィルタリングテスト
```java
// When
List<AttendanceSummary> dailyResults = attendanceSummaryRepository.findByUserIdAndSummaryType(
        TEST_USER_ID_1, SUMMARY_TYPE_DAILY);
List<AttendanceSummary> monthlyResults = attendanceSummaryRepository.findByUserIdAndSummaryType(
        TEST_USER_ID_1, SUMMARY_TYPE_MONTHLY);

// Then
assertNotNull(dailyResults);
assertEquals(2, dailyResults.size()); // dailySummary1 + dailySummary2
assertTrue(dailyResults.stream().allMatch(summary -> summary.getUserId().equals(TEST_USER_ID_1) &&
        SUMMARY_TYPE_DAILY.equals(summary.getSummaryType())));

assertNotNull(monthlyResults);
assertEquals(1, monthlyResults.size()); // monthlySummary1
assertTrue(monthlyResults.stream().allMatch(summary -> summary.getUserId().equals(TEST_USER_ID_1) &&
        SUMMARY_TYPE_MONTHLY.equals(summary.getSummaryType())));
```

**複合条件の検証**:
- **AND条件**: ユーザーIDと集計タイプの両方を満たすレコードのみ取得
- **条件分離**: 日次と月次で異なる結果が返されることを確認
- **全件検証**: Stream APIで全レコードが条件を満たすことを確認

### 2.4 期間検索テスト群

#### テストケース8: ユーザー別期間検索
**メソッド**: `testFindByUserIdAndTargetDateBetween_WithValidRange_ShouldReturnSummaries`

##### 日付範囲検索テスト
```java
// When
List<AttendanceSummary> result = attendanceSummaryRepository.findByUserIdAndTargetDateBetween(
        TEST_USER_ID_1, START_DATE, END_DATE);

// Then
assertNotNull(result);
assertEquals(3, result.size()); // テストデータのみ
assertTrue(result.stream().allMatch(summary -> summary.getUserId().equals(TEST_USER_ID_1) &&
        !summary.getTargetDate().isBefore(START_DATE) &&
        !summary.getTargetDate().isAfter(END_DATE)));
```

**期間検索の特徴**:
- **包含関係**: 開始日と終了日の両方を含む範囲検索
- **境界値処理**: 範囲の境界値での動作確認
- **複合条件**: ユーザーIDと日付範囲の複合条件検索

#### テストケース9: ページネーション機能テスト
**メソッド**: `testFindByTargetDateBetween_WithPageable_ShouldReturnPagedResults`

##### Spring Data JPA ページネーション
```java
// Given
Pageable pageable = PageRequest.of(0, 10);

// When
Page<AttendanceSummary> result = attendanceSummaryRepository.findByTargetDateBetween(
        START_DATE, END_DATE, pageable);

// Then
assertNotNull(result);
assertNotNull(result.getContent());
assertEquals(3, result.getContent().size()); // テストデータのみ
assertTrue(result.getContent().stream().allMatch(summary -> !summary.getTargetDate().isBefore(START_DATE) &&
        !summary.getTargetDate().isAfter(END_DATE)));
```

**ページネーションの検証**:
- **Page型戻り値**: Spring Data JPAのPage型による結果取得
- **コンテンツ確認**: getContent()でページ内容を取得
- **範囲条件**: ページネーション結果も日付範囲条件を満たすことを確認

#### テストケース10: 複合期間検索
**メソッド**: `testFindByUserIdAndSummaryTypeAndTargetDateBetween_WithValidData_ShouldReturnFilteredSummaries`

##### 3条件複合検索テスト
```java
// When
List<AttendanceSummary> dailyResults = attendanceSummaryRepository
        .findByUserIdAndSummaryTypeAndTargetDateBetween(
                TEST_USER_ID_1, SUMMARY_TYPE_DAILY, START_DATE, END_DATE);
List<AttendanceSummary> monthlyResults = attendanceSummaryRepository
        .findByUserIdAndSummaryTypeAndTargetDateBetween(
                TEST_USER_ID_1, SUMMARY_TYPE_MONTHLY, START_DATE, END_DATE);

// Then
assertNotNull(dailyResults);
assertEquals(2, dailyResults.size()); // dailySummary1 + dailySummary2
assertTrue(dailyResults.stream().allMatch(summary -> summary.getUserId().equals(TEST_USER_ID_1) &&
        SUMMARY_TYPE_DAILY.equals(summary.getSummaryType()) &&
        !summary.getTargetDate().isBefore(START_DATE) &&
        !summary.getTargetDate().isAfter(END_DATE)));
```

**複合検索の複雑性**:
- **3条件AND**: ユーザーID、集計タイプ、日付範囲の3条件すべてを満たす検索
- **条件分離**: 日次と月次で異なる結果セットの確認
- **全条件検証**: 全レコードが3つの条件すべてを満たすことを確認

### 2.5 ソート機能テスト

#### テストケース11: ソート順確認テスト
**メソッド**: `testFindBySummaryTypeAndTargetDateBetween_WithValidData_ShouldReturnOrderedResults`

##### 複合ソート検証
```java
// When
List<AttendanceSummary> dailyResults = attendanceSummaryRepository.findBySummaryTypeAndTargetDateBetween(
        SUMMARY_TYPE_DAILY, START_DATE, END_DATE);

// Then
assertNotNull(dailyResults);
assertEquals(2, dailyResults.size()); // テストデータのみ
assertTrue(dailyResults.stream().allMatch(summary -> SUMMARY_TYPE_DAILY.equals(summary.getSummaryType()) &&
        !summary.getTargetDate().isBefore(START_DATE) &&
        !summary.getTargetDate().isAfter(END_DATE)));

// ソート順の確認（userId ASC, targetDate ASC）
if (dailyResults.size() > 1) {
    for (int i = 0; i < dailyResults.size() - 1; i++) {
        AttendanceSummary current = dailyResults.get(i);
        AttendanceSummary next = dailyResults.get(i + 1);

        // ユーザーIDが同じ場合は日付順、異なる場合はユーザーID順
        if (current.getUserId().equals(next.getUserId())) {
            assertTrue(current.getTargetDate().isBefore(next.getTargetDate()) ||
                    current.getTargetDate().equals(next.getTargetDate()));
        } else {
            assertTrue(current.getUserId() <= next.getUserId());
        }
    }
}
```

**ソート処理の検証**:
- **複合ソート**: ユーザーID昇順、日付昇順の複合ソート
- **順序確認**: 隣接レコード間の順序関係を確認
- **条件分岐**: ユーザーIDが同じ場合と異なる場合の処理分岐

### 2.6 データ整合性テスト群

#### テストケース12: 保存と取得の整合性
**メソッド**: `testSaveAndRetrieve_ShouldMaintainDataIntegrity`

##### CRUD操作の整合性確認
```java
// Given
AttendanceSummary newSummary = createAttendanceSummary(null, TEST_USER_ID_2,
        LocalDate.of(2025, 3, 1),
        new BigDecimal("9.25"), new BigDecimal("1.25"),
        new BigDecimal("0.75"), new BigDecimal("0.50"),
        SUMMARY_TYPE_DAILY);

// When
AttendanceSummary savedSummary = attendanceSummaryRepository.save(newSummary);
AttendanceSummary retrievedSummary = attendanceSummaryRepository.findById(savedSummary.getId()).orElse(null);

// Then
assertNotNull(retrievedSummary);
assertEquals(savedSummary.getId(), retrievedSummary.getId());
assertEquals(TEST_USER_ID_2, retrievedSummary.getUserId());
assertEquals(LocalDate.of(2025, 3, 1), retrievedSummary.getTargetDate());
assertEquals(0, new BigDecimal("9.25").compareTo(retrievedSummary.getTotalHours()));
assertEquals(0, new BigDecimal("1.25").compareTo(retrievedSummary.getOvertimeHours()));
assertEquals(0, new BigDecimal("0.75").compareTo(retrievedSummary.getLateNightHours()));
assertEquals(0, new BigDecimal("0.50").compareTo(retrievedSummary.getHolidayHours()));
assertEquals(SUMMARY_TYPE_DAILY, retrievedSummary.getSummaryType());
assertNotNull(retrievedSummary.getCreatedAt());
```

**データ整合性の検証ポイント**:
- **ID自動生成**: データベースでの自動ID生成確認
- **BigDecimal精度**: 時間数の精度保持確認（compareTo使用）
- **日付型**: LocalDate型の正確な保存・取得
- **外部キー制約**: 実在するユーザーIDでの制約遵守
- **作成日時**: createdAtフィールドの自動設定確認

#### テストケース13: 更新処理の確認
**メソッド**: `testUpdateSummary_ShouldReflectChanges`

##### 更新操作テスト
```java
// Given
AttendanceSummary summary = attendanceSummaryRepository.findById(dailySummary1.getId()).orElse(null);
assertNotNull(summary);

// When
summary.setTotalHours(new BigDecimal("9.00"));
summary.setOvertimeHours(new BigDecimal("2.00"));
AttendanceSummary updatedSummary = attendanceSummaryRepository.save(summary);

// Then
assertEquals(dailySummary1.getId(), updatedSummary.getId());
assertEquals(0, new BigDecimal("9.00").compareTo(updatedSummary.getTotalHours()));
assertEquals(0, new BigDecimal("2.00").compareTo(updatedSummary.getOvertimeHours()));
```

**更新処理の特徴**:
- **部分更新**: 特定フィールドのみの更新
- **楽観的ロック**: JPA/Hibernateの楽観的ロック機能
- **変更検知**: Hibernateのダーティチェック機能
- **BigDecimal比較**: compareTo()による精度を考慮した比較

#### テストケース14: 削除処理の確認
**メソッド**: `testDeleteSummary_ShouldRemoveFromDatabase`

##### 削除操作テスト
```java
// Given
Long summaryId = dailySummary1.getId();
assertTrue(attendanceSummaryRepository.existsById(summaryId));

// When
attendanceSummaryRepository.deleteById(summaryId);

// Then
assertFalse(attendanceSummaryRepository.existsById(summaryId));
```

**削除処理の検証**:
- **存在確認**: 削除前の存在確認
- **削除実行**: deleteById()による削除実行
- **削除確認**: existsById()による削除後の確認

### 2.7 BigDecimal精度テスト群

#### テストケース15: BigDecimal精度保持テスト
**メソッド**: `testBigDecimalPrecision_ShouldMaintainAccuracy`

##### 小数点精度検証
```java
// Given
AttendanceSummary precisionSummary = createAttendanceSummary(null, TEST_USER_ID_1,
        LocalDate.of(2025, 3, 15),
        new BigDecimal("8.33"), new BigDecimal("0.67"),
        new BigDecimal("0.17"), new BigDecimal("0.83"),
        SUMMARY_TYPE_DAILY);

// When
AttendanceSummary savedSummary = attendanceSummaryRepository.save(precisionSummary);
AttendanceSummary retrievedSummary = attendanceSummaryRepository.findById(savedSummary.getId()).orElse(null);

// Then
assertNotNull(retrievedSummary);
assertEquals(0, new BigDecimal("8.33").compareTo(retrievedSummary.getTotalHours()));
assertEquals(0, new BigDecimal("0.67").compareTo(retrievedSummary.getOvertimeHours()));
assertEquals(0, new BigDecimal("0.17").compareTo(retrievedSummary.getLateNightHours()));
assertEquals(0, new BigDecimal("0.83").compareTo(retrievedSummary.getHolidayHours()));
```

**BigDecimal精度の重要性**:
- **小数点精度**: 時間計算での小数点以下の精度保持
- **compareTo使用**: equals()ではなくcompareTo()による比較
- **データベース精度**: PostgreSQLのnumeric型との精度整合性
- **計算精度**: 勤怠時間計算での精度要求

#### テストケース16: ゼロ値処理テスト
**メソッド**: `testBigDecimalZeroValues_ShouldHandleCorrectly`

##### ゼロ値の特殊処理
```java
// Given
AttendanceSummary zeroSummary = createAttendanceSummary(null, TEST_USER_ID_1,
        LocalDate.of(2025, 3, 20),
        BigDecimal.ZERO, BigDecimal.ZERO,
        BigDecimal.ZERO, BigDecimal.ZERO,
        SUMMARY_TYPE_DAILY);

// When
AttendanceSummary savedSummary = attendanceSummaryRepository.save(zeroSummary);
AttendanceSummary retrievedSummary = attendanceSummaryRepository.findById(savedSummary.getId()).orElse(null);

// Then
assertNotNull(retrievedSummary);
assertEquals(0, BigDecimal.ZERO.compareTo(retrievedSummary.getTotalHours()));
assertEquals(0, BigDecimal.ZERO.compareTo(retrievedSummary.getOvertimeHours()));
assertEquals(0, BigDecimal.ZERO.compareTo(retrievedSummary.getLateNightHours()));
assertEquals(0, BigDecimal.ZERO.compareTo(retrievedSummary.getHolidayHours()));
```

**ゼロ値処理の特徴**:
- **BigDecimal.ZERO**: ゼロ値の適切な表現
- **ゼロ比較**: ゼロ値でのcompareTo()動作確認
- **デフォルト値**: データベースのデフォルト値との整合性
- **境界値**: ゼロという境界値での動作確認

### 2.8 エッジケース・境界値テスト群

#### テストケース17-19: null値処理テスト
**メソッド**: `testFindByUserId_WithNullUserId_ShouldHandleGracefully` 等

##### null安全性テスト
```java
@Test
void testFindByUserId_WithNullUserId_ShouldHandleGracefully() {
    // When & Then
    assertDoesNotThrow(() -> {
        List<AttendanceSummary> result = attendanceSummaryRepository.findByUserId(null);
        assertNotNull(result);
    });
}

@Test
void testFindBySummaryType_WithNullType_ShouldHandleGracefully() {
    // When & Then
    assertDoesNotThrow(() -> {
        List<AttendanceSummary> result = attendanceSummaryRepository.findBySummaryType(null);
        assertNotNull(result);
    });
}

@Test
void testFindByTargetDate_WithNullDate_ShouldHandleGracefully() {
    // When & Then
    assertDoesNotThrow(() -> {
        List<AttendanceSummary> result = attendanceSummaryRepository.findByTargetDate(null);
        assertNotNull(result);
    });
}
```

**null安全性の重要性**:
- **例外なし**: null値でも例外が発生しないことを確認
- **戻り値確認**: null値でも適切な戻り値（空リスト等）が返されることを確認
- **防御的プログラミング**: 予期しないnull値に対する堅牢性確認

#### テストケース20: 無効な日付範囲テスト
**メソッド**: `testDateRangeQuery_WithInvalidRange_ShouldReturnEmptyList`

##### 逆転日付範囲テスト
```java
// Given - 開始日が終了日より後の無効な範囲
LocalDate invalidStartDate = LocalDate.of(2025, 3, 1);
LocalDate invalidEndDate = LocalDate.of(2025, 2, 1);

// When
List<AttendanceSummary> result = attendanceSummaryRepository.findByTargetDateBetween(
        invalidStartDate, invalidEndDate);

// Then
assertNotNull(result);
assertTrue(result.isEmpty());
```

**無効範囲の処理**:
- **論理的矛盾**: 開始日 > 終了日の論理的に無効な範囲
- **空結果**: 無効な範囲では空の結果が返されることを確認
- **例外なし**: 無効な範囲でも例外が発生しないことを確認

### 2.9 パフォーマンステスト

#### テストケース21: 大量データ処理効率性
**メソッド**: `testLargeDatasetQuery_ShouldPerformEfficiently`

##### パフォーマンス検証テスト
```java
// Given - 大量のテストデータを作成
for (int i = 0; i < 50; i++) {
    AttendanceSummary summary = createAttendanceSummary(null, TEST_USER_ID_1,
            TEST_DATE.plusDays(i % 30 + 10), // 既存テストデータと重複しないように調整
            new BigDecimal("8.00"), new BigDecimal("1.00"),
            new BigDecimal("0.50"), new BigDecimal("0.00"),
            SUMMARY_TYPE_DAILY);
    attendanceSummaryRepository.save(summary);
}

// When
long startTime = System.currentTimeMillis();
List<AttendanceSummary> result = attendanceSummaryRepository.findByUserId(TEST_USER_ID_1);
long endTime = System.currentTimeMillis();

// Then
assertNotNull(result);
assertEquals(53, result.size()); // 元の3件 + 新規50件
assertTrue(endTime - startTime < 1000); // 1秒以内で完了することを確認
```

**パフォーマンス測定の重要性**:
- **レスポンス時間**: 大量データでの検索性能確認
- **メモリ使用量**: 大量データ読み込み時のメモリ効率
- **インデックス効果**: データベースインデックスの効果確認
- **スケーラビリティ**: データ量増加に対する性能劣化の確認

## 3. ヘルパーメソッド活用

### 3.1 テストデータ生成メソッド

#### createAttendanceSummary メソッド
```java
/**
 * テスト用AttendanceSummaryを作成
 */
private AttendanceSummary createAttendanceSummary(Long id, Integer userId, LocalDate targetDate,
        BigDecimal totalHours, BigDecimal overtimeHours,
        BigDecimal lateNightHours, BigDecimal holidayHours,
        String summaryType) {
    AttendanceSummary summary = new AttendanceSummary();
    summary.setId(id);
    summary.setUserId(userId);
    summary.setTargetDate(targetDate);
    summary.setTotalHours(totalHours);
    summary.setOvertimeHours(overtimeHours);
    summary.setLateNightHours(lateNightHours);
    summary.setHolidayHours(holidayHours);
    summary.setSummaryType(summaryType);
    summary.setCreatedAt(OffsetDateTime.now(ZoneOffset.ofHours(9)));
    return summary;
}
```

**設計パターン**:
- **ファクトリーメソッド**: 一貫したテストデータ生成
- **パラメータ化**: 柔軟なデータ生成のための多数のパラメータ
- **タイムゾーン統一**: 日本時間（JST）での作成日時設定
- **BigDecimal活用**: 時間数の精度保持

## 4. 集計データテスト特有の戦略

### 4.1 空データベース使用の利点と課題

#### 利点
- **正確な件数**: attendance_summariesテーブルが空のため、正確な件数を期待可能
- **制約検証**: 外部キー制約、NOT NULL制約等の実際の動作確認
- **BigDecimal精度**: 実際のPostgreSQLのnumeric型での精度確認
- **集計ロジック**: 日次・月次集計の正確性確認

#### 課題と対策
```java
// 課題1: 重複データ作成
// 対策: 異なる日付を使用してユニーク制約違反を回避
monthlySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE.plusDays(2), ...);

// 課題2: BigDecimal比較の複雑性
// 対策: compareTo()メソッドによる精度を考慮した比較
assertEquals(0, new BigDecimal("8.33").compareTo(retrievedSummary.getTotalHours()));

// 課題3: 外部キー制約違反
// 対策: 実在するユーザーIDのみ使用
private static final Integer TEST_USER_ID_1 = 1; // 実在するユーザー
```

### 4.2 BigDecimal処理テストの重要性

#### 精度保持の検証
```java
// 問題のあるコード（精度の問題）
assertEquals(new BigDecimal("8.33"), retrievedSummary.getTotalHours()); // スケールの違いで失敗する可能性

// 改善されたコード（compareTo使用）
assertEquals(0, new BigDecimal("8.33").compareTo(retrievedSummary.getTotalHours())); // 値のみ比較
```

**BigDecimal特有の課題**:
- **スケール違い**: 同じ値でもスケールが異なるとequals()で失敗
- **精度保持**: データベースとの往復での精度保持確認
- **計算精度**: 勤怠時間計算での精度要求への対応

### 4.3 日付処理テストの特徴

#### LocalDate型の活用
```java
// 日付のみの処理（時刻情報なし）
private static final LocalDate TEST_DATE = LocalDate.of(2025, 2, 1);

// 日付範囲検索
List<AttendanceSummary> result = attendanceSummaryRepository.findByTargetDateBetween(START_DATE, END_DATE);

// 日付比較
assertTrue(!summary.getTargetDate().isBefore(START_DATE) && !summary.getTargetDate().isAfter(END_DATE));
```

**日付処理の特徴**:
- **時刻なし**: LocalDate型による日付のみの処理
- **範囲検索**: 期間指定での集計データ検索
- **境界値**: 日付範囲の境界値での動作確認

## 5. テスト作成のベストプラクティス

### 5.1 集計データテスト専用のパターン

#### テストデータ準備パターン
```java
@BeforeEach
void setUp() {
    // 1. 異なる日付でのデータ作成（重複回避）
    dailySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE, ...);
    dailySummary2 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE.plusDays(1), ...);
    monthlySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE.plusDays(2), ...);
    
    // 2. データベース保存とID取得
    dailySummary1 = attendanceSummaryRepository.save(dailySummary1);
    dailySummary2 = attendanceSummaryRepository.save(dailySummary2);
    monthlySummary1 = attendanceSummaryRepository.save(monthlySummary1);
}
```

#### 検証パターン
```java
// パターン1: null安全性 → サイズ確認 → 内容確認
assertNotNull(result);
assertEquals(expectedSize, result.size());
assertTrue(result.stream().allMatch(summary -> condition));

// パターン2: BigDecimal比較
assertEquals(0, expectedValue.compareTo(actualValue)); // compareTo使用

// パターン3: Optional型処理
assertTrue(result.isPresent());
assertEquals(expectedValue, result.get().getProperty());

// パターン4: ページネーション確認
assertNotNull(result);
assertNotNull(result.getContent());
assertEquals(expectedSize, result.getContent().size());
```

### 5.2 空データベース環境での注意点

#### 正確な件数期待
```java
// 改善されたコード（正確な件数期待）
assertEquals(3, result.size()); // テストデータのみ

// 範囲確認での検証
assertTrue(result.size() >= 1 && result.size() <= 10); // 合理的な範囲での確認
```

#### 重複データの回避
```java
// 問題のあるコード（同じ日付使用）
dailySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE, ...);
monthlySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE, ...); // 重複の可能性

// 改善されたコード（異なる日付使用）
dailySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE, ...);
monthlySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE.plusDays(2), ...); // 重複回避
```

#### BigDecimal精度の考慮
```java
// 問題のあるコード（equals使用）
assertEquals(new BigDecimal("8.33"), result.getTotalHours()); // スケール違いで失敗の可能性

// 改善されたコード（compareTo使用）
assertEquals(0, new BigDecimal("8.33").compareTo(result.getTotalHours())); // 値のみ比較
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
ERROR: insert or update on table "attendance_summaries" violates foreign key constraint "attendance_summaries_user_id_fkey"
```

**解決策**:
```java
// 実在するユーザーIDを使用
private static final Integer TEST_USER_ID_1 = 1; // comsys_test_dump.sqlに存在するID

// テストデータ作成時に実在するIDのみ使用
AttendanceSummary summary = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE, ...);
```

### 6.2 BigDecimal処理の問題

#### 問題: BigDecimal比較の失敗
```java
// 問題のあるコード（スケールの違いで失敗）
assertEquals(new BigDecimal("8.33"), result.getTotalHours()); // 失敗する可能性
```

**解決策**:
```java
// compareTo()による値のみの比較
assertEquals(0, new BigDecimal("8.33").compareTo(result.getTotalHours()));

// または許容誤差を設定（必要に応じて）
assertTrue(new BigDecimal("8.33").subtract(result.getTotalHours()).abs().compareTo(new BigDecimal("0.01")) < 0);
```

#### 問題: null値でのBigDecimal処理
```java
// 問題のあるコード（null値でのNullPointerException）
assertEquals(0, expectedValue.compareTo(result.getTotalHours())); // result.getTotalHours()がnullの場合
```

**解決策**:
```java
// null安全性を考慮
assertNotNull(result.getTotalHours());
assertEquals(0, expectedValue.compareTo(result.getTotalHours()));

// またはOptionalの活用
Optional.ofNullable(result.getTotalHours())
    .ifPresent(value -> assertEquals(0, expectedValue.compareTo(value)));
```

### 6.3 日付処理の問題

#### 問題: 日付範囲の境界値処理
```java
// 問題のあるコード（境界値の曖昧性）
assertTrue(summary.getTargetDate().isAfter(START_DATE)); // 境界値を含まない
```

**解決策**:
```java
// 境界値を含む範囲確認
assertTrue(!summary.getTargetDate().isBefore(START_DATE) && 
          !summary.getTargetDate().isAfter(END_DATE));

// または明示的な境界値確認
assertTrue(summary.getTargetDate().compareTo(START_DATE) >= 0 && 
          summary.getTargetDate().compareTo(END_DATE) <= 0);
```

#### 問題: 日付の重複によるユニーク制約違反
```java
// 問題のあるコード（同じユーザーIDと日付の組み合わせ）
dailySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE, ...);
monthlySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE, ...); // 制約違反の可能性
```

**解決策**:
```java
// 異なる日付を使用
dailySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE, ...);
monthlySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE.plusDays(2), ...); // 重複回避
```

### 6.4 Optional型処理の問題

#### 問題: Optional型の不適切な処理
```java
// 問題のあるコード（Optional型の直接get()使用）
Optional<AttendanceSummary> result = repository.findByUserIdAndTargetDate(userId, date);
AttendanceSummary summary = result.get(); // NoSuchElementExceptionの可能性
```

**解決策**:
```java
// 存在確認後のget()使用
Optional<AttendanceSummary> result = repository.findByUserIdAndTargetDate(userId, date);
assertTrue(result.isPresent());
AttendanceSummary summary = result.get();

// またはorElse()の活用
AttendanceSummary summary = result.orElse(null);
assertNotNull(summary);
```

## 7. 実装済みテストケース一覧（23件）

### 7.1 基本検索機能（3件）
- `testFindByUserId_WithExistingUser_ShouldReturnSummaries`
- `testFindByUserId_WithNonExistentUser_ShouldReturnEmptyList`
- `testFindBySummaryType_WithValidType_ShouldReturnFilteredSummaries`

### 7.2 日付検索機能（3件）
- `testFindByTargetDate_WithValidDate_ShouldReturnSummaries`
- `testFindByTargetDate_WithNonExistentDate_ShouldReturnEmptyList`
- `testFindByUserIdAndTargetDate_WithValidData_ShouldReturnSummary`
- `testFindByUserIdAndTargetDate_WithNonExistentData_ShouldReturnEmpty`

### 7.3 複合条件検索（1件）
- `testFindByUserIdAndSummaryType_WithValidData_ShouldReturnFilteredSummaries`

### 7.4 期間検索機能（5件）
- `testFindByUserIdAndTargetDateBetween_WithValidRange_ShouldReturnSummaries`
- `testFindByTargetDateBetween_WithPageable_ShouldReturnPagedResults`
- `testFindByTargetDateBetween_WithoutPageable_ShouldReturnAllResults`
- `testFindByUserIdAndSummaryTypeAndTargetDateBetween_WithValidData_ShouldReturnFilteredSummaries`
- `testFindBySummaryTypeAndTargetDateBetween_WithValidData_ShouldReturnOrderedResults`

### 7.5 データ整合性テスト（3件）
- `testSaveAndRetrieve_ShouldMaintainDataIntegrity`
- `testUpdateSummary_ShouldReflectChanges`
- `testDeleteSummary_ShouldRemoveFromDatabase`

### 7.6 BigDecimal精度テスト（2件）
- `testBigDecimalPrecision_ShouldMaintainAccuracy`
- `testBigDecimalZeroValues_ShouldHandleCorrectly`

### 7.7 エッジケース・境界値テスト（4件）
- `testFindByUserId_WithNullUserId_ShouldHandleGracefully`
- `testFindBySummaryType_WithNullType_ShouldHandleGracefully`
- `testFindByTargetDate_WithNullDate_ShouldHandleGracefully`
- `testDateRangeQuery_WithInvalidRange_ShouldReturnEmptyList`

### 7.8 パフォーマンステスト（1件）
- `testLargeDatasetQuery_ShouldPerformEfficiently`

## 8. まとめ

### 8.1 集計データテストの重要ポイント
1. **空データベース活用**: attendance_summariesテーブルが空のため正確な件数期待が可能
2. **BigDecimal精度**: 時間計算での精度保持とcompareTo()による比較
3. **日付処理**: LocalDate型による日付のみの処理と範囲検索
4. **重複回避**: 異なる日付を使用したユニーク制約違反の回避
5. **Optional型**: 単一結果検索でのOptional型の適切な処理

### 8.2 テスト品質向上のチェックリスト
- [ ] 実データベース（comsys_test_dump.sql）を使用
- [ ] 外部キー制約を遵守した実在するユーザーIDを使用
- [ ] BigDecimal比較はcompareTo()を使用
- [ ] 異なる日付を使用して重複データを回避
- [ ] null安全性を最初に確認
- [ ] Optional型の存在確認後にget()を使用
- [ ] 日付範囲検索の境界値を適切に処理
- [ ] ページネーション機能の動作を確認
- [ ] @Transactionalによる自動ロールバックを活用

### 8.3 他のテストとの違い
| 項目 | 集計データテスト | 記録データテスト | サービステスト |
|------|------------------|------------------|----------------|
| **データベース状態** | 空テーブル | 既存データあり | モック使用 |
| **精度要求** | BigDecimal精度 | 浮動小数点許容誤差 | 精度不要 |
| **日付処理** | LocalDate型 | OffsetDateTime型 | モック |
| **重複制約** | ユニーク制約あり | 制約なし | 制約なし |
| **集計機能** | 集計ロジック確認 | 個別レコード確認 | ビジネスロジック |
| **ページネーション** | Page型対応 | List型のみ | モック |

この手順書に従うことで、勤怠集計データアクセス層の特性を考慮した包括的で信頼性の高いテストケースを作成できます。特に空データベース環境でのBigDecimal精度保持、日付処理の複雑性、重複データ回避を適切に扱うことで、実用的なテストスイートを構築できます。