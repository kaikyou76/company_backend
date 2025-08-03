# AttendanceRecordRepositoryTest テストケース作成手順書

## 概要
本書は、`AttendanceRecordRepositoryTest` のテストケース作成における注釈、データベース接続、テスト作成の流れとコツを詳細に説明した手順書です。勤怠記録データアクセス層の特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/repository/AttendanceRecordRepositoryTest.java`

### 1.2 基本アノテーション

#### @SpringBootTest
**行**: 20
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AttendanceRecordRepositoryTest {
```

**目的**:
- Spring Boot統合テスト環境の構築
- 実際のデータベース接続とトランザクション管理
- テストプロファイルによる設定分離

**リポジトリテストの特徴**:
- 実際のPostgreSQLデータベース（comsys_test_dump.sql）を使用
- JPA/Hibernateの動作確認
- ネイティブクエリとJPQLクエリの両方をテスト
- データベース制約（外部キー、NOT NULL等）の検証

#### @Transactional
**目的**:
- 各テストメソッド実行後の自動ロールバック
- テスト間のデータ独立性保証
- データベース状態のクリーンアップ

#### @ActiveProfiles("test")
**目的**:
- テスト専用データベース設定の適用
- `application-test.properties` の設定読み込み
- 本番環境との分離### 1.3 依存
性注入

#### @Autowired AttendanceRecordRepository
**行**: 24-25
```java
@Autowired
private AttendanceRecordRepository attendanceRecordRepository;
```

**役割**:
- Spring Data JPAリポジトリの自動注入
- 実際のデータベース操作の実行
- カスタムクエリメソッドのテスト対象

**テスト対象メソッド**:
```java
// 基本検索メソッド
findByUserId(Integer userId)
findByUserIdAndType(Integer userId, String type)
findByUserIdAndDate(Integer userId, LocalDate date)

// 日付範囲検索メソッド
findByUserIdAndDateRange(Integer userId, OffsetDateTime startDate, OffsetDateTime endDate)
findByDateRange(OffsetDateTime startDate, OffsetDateTime endDate)

// 今日・最新記録検索メソッド
findTodayRecordsByUserId(Integer userId)
findTopByUserIdOrderByTimestampDesc(Integer userId)

// 最近の記録検索メソッド
findRecentRecordsByUserIdAndType(Integer userId, String type, OffsetDateTime timestamp)

// タイプ別検索メソッド
findClockInRecordsByUserId(Integer userId)
findClockOutRecordsByUserId(Integer userId)
findByTypeAndTimestampBetween(String type, OffsetDateTime startDate, OffsetDateTime endDate)

// 処理状態検索メソッド
findUnprocessedRecords()
findLatestByUserIdAndType(Integer userId, String type)

// 統計情報メソッド
countTodayClockInUsers()
countTodayRecords()

// 部署別・月次検索メソッド
findByDepartmentAndDate(Integer departmentId, LocalDate date)
findByUserIdAndYearAndMonth(Integer userId, int year, int month)
```

### 1.4 テスト用定数定義

#### 実データベース対応定数
**行**: 27-34
```java
// テスト用定数（comsys_test_dump.sqlの実際のデータを使用）
private static final Integer TEST_USER_ID_1 = 1; // ceo@company.com
private static final Integer TEST_USER_ID_2 = 2; // director@company.com
private static final Integer TEST_DEPARTMENT_ID = 1;
private static final Double TEST_LATITUDE = 35.6812;
private static final Double TEST_LONGITUDE = 139.7671;
private static final String TYPE_IN = "in";
private static final String TYPE_OUT = "out";
```

**設計思想**:
- **実データ活用**: comsys_test_dump.sqlの実際のユーザーIDを使用
- **位置情報**: 東京都心部の実際の緯度経度を使用
- **テスト一貫性**: 全テストで同じ基準データを使用
- **外部キー制約**: 実在するユーザーIDのみ使用して制約違反を回避#
# 2. 主要テストケース解析

### 2.1 基本検索テスト群

#### テストケース1: 正常なユーザーID検索
**メソッド**: `testFindByUserId_WithExistingUser_ShouldReturnRecords`

##### データベース状態の考慮
```java
// When
List<AttendanceRecord> result = attendanceRecordRepository.findByUserId(TEST_USER_ID_1);

// Then
assertNotNull(result);
assertTrue(result.size() >= 2); // 既存データ + テストデータ
assertTrue(result.stream().allMatch(record -> record.getUserId().equals(TEST_USER_ID_1)));
```

**重要ポイント**:
- **既存データ考慮**: 実データベースには既にデータが存在するため、`>=` 比較を使用
- **データ整合性**: 全レコードが指定ユーザーIDと一致することを確認
- **null安全性**: 結果がnullでないことを最初に確認

#### テストケース2: 存在しないユーザーの処理
**メソッド**: `testFindByUserId_WithNonExistentUser_ShouldReturnEmptyList`

##### 境界値テスト
```java
// When
List<AttendanceRecord> result = attendanceRecordRepository.findByUserId(999);

// Then
assertNotNull(result);
assertTrue(result.isEmpty());
```

**設計思想**:
- **存在しないID**: 999は実データベースに存在しないユーザーID
- **空リスト確認**: nullではなく空のListが返されることを確認
- **例外なし**: 存在しないIDでも例外が発生しないことを確認

#### テストケース3: タイプ別フィルタリング
**メソッド**: `testFindByUserIdAndType_WithValidData_ShouldReturnFilteredRecords`

##### 複合条件検索テスト
```java
// When
List<AttendanceRecord> clockInResults = attendanceRecordRepository.findByUserIdAndType(TEST_USER_ID_1, TYPE_IN);
List<AttendanceRecord> clockOutResults = attendanceRecordRepository.findByUserIdAndType(TEST_USER_ID_1, TYPE_OUT);

// Then
assertTrue(clockInResults.size() >= 1); // 既存データ + テストデータ
assertTrue(clockInResults.stream().allMatch(record -> TYPE_IN.equals(record.getType())));

assertTrue(clockOutResults.size() >= 1); // 既存データ + テストデータ
assertTrue(clockOutResults.stream().allMatch(record -> TYPE_OUT.equals(record.getType())));
```

**検証ポイント**:
- **フィルタリング精度**: 指定したタイプのレコードのみが返されることを確認
- **データ分離**: 出勤と退勤のレコードが適切に分離されることを確認
- **Stream API活用**: 全要素が条件を満たすことを効率的に検証

### 2.2 日付検索テスト群

#### テストケース4: 日付指定検索
**メソッド**: `testFindByUserIdAndDate_WithValidDate_ShouldReturnRecords`

##### 日付境界の処理
```java
// Given
LocalDate testDate = baseTime.toLocalDate();

// When
List<AttendanceRecord> result = attendanceRecordRepository.findByUserIdAndDate(TEST_USER_ID_1, testDate);

// Then
assertNotNull(result);
assertEquals(2, result.size());
assertTrue(result.stream().allMatch(record -> 
    record.getTimestamp().toLocalDate().equals(testDate)));
```

**日付処理の特徴**:
- **時刻無視**: LocalDateを使用して時刻部分を無視
- **タイムゾーン考慮**: OffsetDateTimeからLocalDateへの適切な変換
- **境界値処理**: 日付の開始（00:00:00）から終了（23:59:59）までを含む#### 
テストケース5: 日付範囲検索
**メソッド**: `testFindByUserIdAndDateRange_WithValidRange_ShouldReturnRecords`

##### 時間範囲クエリテスト
```java
// Given
OffsetDateTime startDate = baseTime.minusHours(1);
OffsetDateTime endDate = baseTime.plusHours(10);

// When
List<AttendanceRecord> result = attendanceRecordRepository.findByUserIdAndDateRange(
        TEST_USER_ID_1, startDate, endDate);

// Then
assertNotNull(result);
assertEquals(2, result.size());
assertTrue(result.stream().allMatch(record -> 
    !record.getTimestamp().isBefore(startDate) && !record.getTimestamp().isAfter(endDate)));
```

**範囲検索の重要性**:
- **包含関係**: 開始時刻と終了時刻の両方を含む範囲検索
- **境界値処理**: 範囲の境界値での動作確認
- **パフォーマンス**: インデックスを活用した効率的な範囲検索

### 2.3 今日の記録検索テスト

#### テストケース6: 今日の記録取得
**メソッド**: `testFindTodayRecordsByUserId_WithTodayRecords_ShouldReturnRecords`

##### 動的日付処理テスト
```java
// Given - 今日の日付でレコードを作成
OffsetDateTime today = OffsetDateTime.now(ZoneOffset.ofHours(9));
AttendanceRecord todayRecord = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_IN, 
                                                    today, TEST_LATITUDE, TEST_LONGITUDE, false);
attendanceRecordRepository.save(todayRecord);

// When
List<AttendanceRecord> result = attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID_1);

// Then
assertNotNull(result);
assertFalse(result.isEmpty());
assertTrue(result.stream().allMatch(record -> 
    record.getTimestamp().toLocalDate().equals(LocalDate.now())));
```

**動的日付の課題**:
- **テスト実行日依存**: テスト実行日によって結果が変わる
- **タイムゾーン統一**: 日本時間（JST）での統一処理
- **CURRENT_DATE関数**: データベースの現在日付関数の動作確認

### 2.4 最新記録検索テスト

#### テストケース7: 最新記録の取得
**メソッド**: `testFindTopByUserIdOrderByTimestampDesc_ShouldReturnLatestRecord`

##### 時刻順ソートテスト
```java
// When
List<AttendanceRecord> result = attendanceRecordRepository.findTopByUserIdOrderByTimestampDesc(TEST_USER_ID_1);

// Then
assertNotNull(result);
assertFalse(result.isEmpty());

// 最新の記録が最初に来ることを確認（時刻順）
AttendanceRecord latestRecord = result.get(0);
assertNotNull(latestRecord.getTimestamp());
// 既存データがあるため、具体的な時刻ではなく順序のみ確認
if (result.size() > 1) {
    assertTrue(latestRecord.getTimestamp().isAfter(result.get(1).getTimestamp()) ||
              latestRecord.getTimestamp().equals(result.get(1).getTimestamp()));
}
```

**ソート処理の検証**:
- **降順ソート**: 最新の記録が最初に来ることを確認
- **既存データ考慮**: 実データベースの既存データとの整合性確認
- **時刻比較**: OffsetDateTimeの適切な比較処理### 2
.5 最近の記録検索テスト

#### テストケース8: 最近の記録フィルタリング
**メソッド**: `testFindRecentRecordsByUserIdAndType_WithRecentRecords_ShouldReturnRecords`

##### 時刻フィルタリングテスト
```java
// Given
OffsetDateTime recentTime = baseTime.minusMinutes(30);

// When
List<AttendanceRecord> result = attendanceRecordRepository.findRecentRecordsByUserIdAndType(
        TEST_USER_ID_1, TYPE_IN, recentTime);

// Then
assertNotNull(result);
assertFalse(result.isEmpty()); // 既存データがあるため空ではない
assertTrue(result.stream().allMatch(record -> TYPE_IN.equals(record.getType())));
assertTrue(result.stream().allMatch(record -> 
    record.getTimestamp().isAfter(recentTime) || record.getTimestamp().equals(recentTime)));
```

**重複防止機能の検証**:
- **時刻フィルタ**: 指定時刻以降のレコードのみ取得
- **タイプフィルタ**: 指定したタイプ（出勤/退勤）のレコードのみ
- **重複打刻防止**: 5分以内の重複打刻を防ぐためのクエリ

#### テストケース9: 未来時刻での検索
**メソッド**: `testFindRecentRecordsByUserIdAndType_WithNoRecentRecords_ShouldReturnEmptyList`

##### 境界値テスト（未来時刻）
```java
// Given
OffsetDateTime futureTime = OffsetDateTime.now().plusDays(1); // 明日の時刻

// When
List<AttendanceRecord> result = attendanceRecordRepository.findRecentRecordsByUserIdAndType(
        TEST_USER_ID_1, TYPE_IN, futureTime);

// Then
assertNotNull(result);
// 未来の時刻以降のレコードのみが返される
assertTrue(result.stream().allMatch(record -> 
    record.getTimestamp().isAfter(futureTime) || record.getTimestamp().equals(futureTime)));
```

### 2.6 月次検索テスト

#### テストケース10: 年月指定検索
**メソッド**: `testFindByUserIdAndYearAndMonth_WithValidYearMonth_ShouldReturnRecords`

##### EXTRACT関数テスト
```java
// Given
int year = baseTime.getYear();
int month = baseTime.getMonthValue();

// When
List<AttendanceRecord> result = attendanceRecordRepository.findByUserIdAndYearAndMonth(
        TEST_USER_ID_1, year, month);

// Then
assertNotNull(result);
assertEquals(2, result.size());
assertTrue(result.stream().allMatch(record -> 
    record.getTimestamp().getYear() == year && 
    record.getTimestamp().getMonthValue() == month));
```

**SQL EXTRACT関数の活用**:
- **年月抽出**: PostgreSQLのEXTRACT関数による年月抽出
- **月境界処理**: 月の最初から最後までの全レコード取得
- **インデックス効率**: 年月での効率的な検索

### 2.7 統計情報テスト

#### テストケース11: 今日の出勤ユーザー数
**メソッド**: `testCountTodayClockInUsers_WithTodayRecords_ShouldReturnCount`

##### 集計クエリテスト
```java
// Given - 今日の出勤記録を追加
OffsetDateTime today = OffsetDateTime.now(ZoneOffset.ofHours(9));
AttendanceRecord todayClockIn1 = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_IN, 
                                                       today, TEST_LATITUDE, TEST_LONGITUDE, false);
AttendanceRecord todayClockIn2 = createAttendanceRecord(null, TEST_USER_ID_2, TYPE_IN, 
                                                       today.plusMinutes(30), TEST_LATITUDE, TEST_LONGITUDE, false);
attendanceRecordRepository.save(todayClockIn1);
attendanceRecordRepository.save(todayClockIn2);

// When
Long result = attendanceRecordRepository.countTodayClockInUsers();

// Then
assertNotNull(result);
assertEquals(2L, result); // 2人のユーザーが出勤
```

**統計クエリの特徴**:
- **DISTINCT COUNT**: 重複ユーザーを除いた出勤者数
- **日付フィルタ**: CURRENT_DATEによる今日のデータのみ集計
- **Long型戻り値**: COUNT関数の結果はLong型で受け取り#
## 2.8 データ整合性テスト

#### テストケース12: 保存と取得の整合性
**メソッド**: `testSaveAndRetrieve_ShouldMaintainDataIntegrity`

##### CRUD操作の整合性確認
```java
// Given
AttendanceRecord newRecord = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_IN,
        OffsetDateTime.now(ZoneOffset.ofHours(9)),
        35.6895, 139.6917, false);

// When
AttendanceRecord savedRecord = attendanceRecordRepository.save(newRecord);
AttendanceRecord retrievedRecord = attendanceRecordRepository.findById(savedRecord.getId()).orElse(null);

// Then
assertNotNull(retrievedRecord);
assertEquals(savedRecord.getId(), retrievedRecord.getId());
assertEquals(TEST_USER_ID_1, retrievedRecord.getUserId());
assertEquals(TYPE_IN, retrievedRecord.getType());
assertEquals(35.6895, retrievedRecord.getLatitude(), 0.0001);
assertEquals(139.6917, retrievedRecord.getLongitude(), 0.0001);
assertFalse(retrievedRecord.getProcessed());
```

**データ整合性の検証ポイント**:
- **ID自動生成**: データベースでの自動ID生成確認
- **精度保持**: 緯度経度の浮動小数点精度保持
- **Boolean値**: processed フラグの正確な保存・取得
- **外部キー制約**: 実在するユーザーIDでの制約遵守

#### テストケース13: 更新処理の確認
**メソッド**: `testUpdateRecord_ShouldReflectChanges`

##### 更新操作テスト
```java
// Given
AttendanceRecord record = attendanceRecordRepository.findById(clockInRecord1.getId()).orElse(null);
assertNotNull(record);

// When
record.setProcessed(true);
record.setLatitude(35.7000);
AttendanceRecord updatedRecord = attendanceRecordRepository.save(record);

// Then
assertEquals(clockInRecord1.getId(), updatedRecord.getId());
assertTrue(updatedRecord.getProcessed());
assertEquals(35.7000, updatedRecord.getLatitude(), 0.0001);
```

**更新処理の特徴**:
- **部分更新**: 特定フィールドのみの更新
- **楽観的ロック**: JPA/Hibernateの楽観的ロック機能
- **変更検知**: Hibernateのダーティチェック機能

### 2.9 パフォーマンステスト

#### テストケース14: 大量データ処理効率性
**メソッド**: `testLargeDatasetQuery_ShouldPerformEfficiently`

##### パフォーマンス検証テスト
```java
// Given - 大量のテストデータを作成
for (int i = 0; i < 100; i++) {
    AttendanceRecord record = createAttendanceRecord(null, TEST_USER_ID_1, 
                                                   i % 2 == 0 ? TYPE_IN : TYPE_OUT,
                                                   baseTime.plusMinutes(i * 10), 
                                                   TEST_LATITUDE, TEST_LONGITUDE, false);
    attendanceRecordRepository.save(record);
}

// When
long startTime = System.currentTimeMillis();
List<AttendanceRecord> result = attendanceRecordRepository.findByUserId(TEST_USER_ID_1);
long endTime = System.currentTimeMillis();

// Then
assertNotNull(result);
assertTrue(result.size() >= 102); // 既存データ + 元の2件 + 新規100件
assertTrue(endTime - startTime < 1000); // 1秒以内で完了することを確認
```

**パフォーマンス測定の重要性**:
- **レスポンス時間**: 大量データでの検索性能確認
- **メモリ使用量**: 大量データ読み込み時のメモリ効率
- **インデックス効果**: データベースインデックスの効果確認

## 3. ヘルパーメソッド活用

### 3.1 テストデータ生成メソッド

#### createAttendanceRecord メソッド
```java
/**
 * テスト用AttendanceRecordを作成
 */
private AttendanceRecord createAttendanceRecord(Long id, Integer userId, String type, 
                                               OffsetDateTime timestamp, Double latitude, 
                                               Double longitude, Boolean processed) {
    AttendanceRecord record = new AttendanceRecord();
    record.setId(id);
    record.setUserId(userId);
    record.setType(type);
    record.setTimestamp(timestamp);
    record.setLatitude(latitude);
    record.setLongitude(longitude);
    record.setProcessed(processed);
    record.setCreatedAt(timestamp);
    return record;
}
```

**設計パターン**:
- **ファクトリーメソッド**: 一貫したテストデータ生成
- **パラメータ化**: 柔軟なデータ生成のための多数のパラメータ
- **デフォルト値**: createdAtはtimestampと同じ値を設定#
# 4. リポジトリテスト特有の戦略

### 4.1 実データベース使用の利点と課題

#### 利点
- **実環境再現**: 本番環境と同じデータベースエンジン（PostgreSQL）を使用
- **制約検証**: 外部キー制約、NOT NULL制約等の実際の動作確認
- **パフォーマンス**: 実際のインデックス効果とクエリ性能の測定
- **ネイティブクエリ**: @Queryアノテーションのネイティブクエリの動作確認

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
class AttendanceRecordRepositoryTest {
```

### 4.2 ネイティブクエリテストの重要性

#### PostgreSQL固有機能のテスト
```java
// EXTRACT関数のテスト
@Query(nativeQuery = true, value = "SELECT * FROM attendance_records ar WHERE ar.user_id = :userId AND EXTRACT(YEAR FROM ar.timestamp) = :year AND EXTRACT(MONTH FROM ar.timestamp) = :month")
List<AttendanceRecord> findByUserIdAndYearAndMonth(@Param("userId") Integer userId, @Param("year") int year, @Param("month") int month);

// CURRENT_DATE関数のテスト
@Query(nativeQuery = true, value = "SELECT ar.* FROM attendance_records ar WHERE ar.user_id = :userId AND DATE(ar.timestamp) = CURRENT_DATE")
List<AttendanceRecord> findTodayRecordsByUserId(@Param("userId") Integer userId);
```

**ネイティブクエリテストの特徴**:
- **SQL方言**: PostgreSQL固有のSQL構文の動作確認
- **関数テスト**: データベース関数（EXTRACT, CURRENT_DATE等）の動作確認
- **パフォーマンス**: 最適化されたネイティブクエリの性能確認

### 4.3 時刻処理テストの複雑性

#### タイムゾーン統一戦略
```java
// 日本時間での統一
private static final ZoneOffset JST = ZoneOffset.ofHours(9);

// 基準時刻の設定
baseTime = OffsetDateTime.of(2025, 2, 1, 9, 0, 0, 0, JST);

// 今日の時刻取得
OffsetDateTime today = OffsetDateTime.now(JST);
```

#### 時刻比較の精度管理
```java
// 浮動小数点精度での位置情報比較
assertEquals(35.6895, retrievedRecord.getLatitude(), 0.0001);
assertEquals(139.6917, retrievedRecord.getLongitude(), 0.0001);

// 時刻の前後関係確認
assertTrue(latestRecord.getTimestamp().isAfter(olderRecord.getTimestamp()));

// 時刻範囲の包含確認
assertTrue(!record.getTimestamp().isBefore(startDate) && !record.getTimestamp().isAfter(endDate));
```

## 5. テスト作成のベストプラクティス

### 5.1 データベーステスト専用のパターン

#### テストデータ準備パターン
```java
@BeforeEach
void setUp() {
    // 1. 基準時刻設定
    baseTime = OffsetDateTime.of(2025, 2, 1, 9, 0, 0, 0, ZoneOffset.ofHours(9));
    
    // 2. テストデータ作成
    clockInRecord1 = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_IN, baseTime, ...);
    clockOutRecord1 = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_OUT, baseTime.plusHours(8), ...);
    
    // 3. データベース保存
    clockInRecord1 = attendanceRecordRepository.save(clockInRecord1);
    clockOutRecord1 = attendanceRecordRepository.save(clockOutRecord1);
}
```

#### 検証パターン
```java
// パターン1: null安全性 → サイズ確認 → 内容確認
assertNotNull(result);
assertTrue(result.size() >= expectedMinimumSize);
assertTrue(result.stream().allMatch(record -> condition));

// パターン2: 段階的検証
// 基本検証
assertNotNull(result);
assertFalse(result.isEmpty());

// 詳細検証
assertEquals(expectedValue, result.get(0).getProperty());
assertTrue(result.stream().allMatch(record -> record.getType().equals(expectedType)));

// 副作用なし確認（必要に応じて）
verify(someService, never()).someMethod();
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
AttendanceRecord record = createAttendanceRecord(null, 999, TYPE_IN, ...); // 制約違反

// 改善されたコード（実在するユーザーID）
AttendanceRecord record = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_IN, ...); // 制約遵守
```

#### トランザクション境界の理解
```java
// @Transactionalにより各テストメソッド後に自動ロールバック
@Test
@Transactional
void testMethod() {
    // データ変更操作
    attendanceRecordRepository.save(record);
    
    // テスト終了後、変更は自動的にロールバックされる
}
```#
# 6. 一般的な問題と解決策

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
ERROR: insert or update on table "attendance_records" violates foreign key constraint "attendance_records_user_id_fkey"
```

**解決策**:
```java
// 実在するユーザーIDを使用
private static final Integer TEST_USER_ID_1 = 1; // comsys_test_dump.sqlに存在するID

// テストデータ作成時に実在するIDのみ使用
AttendanceRecord record = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_IN, ...);
```

### 6.2 時刻処理の問題

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

#### 問題: 浮動小数点精度の問題
```java
// 問題のあるコード（精度の問題で失敗する可能性）
assertEquals(35.6812, result.getLatitude()); // 精度の問題
```

**解決策**:
```java
// 許容誤差を設定
assertEquals(35.6812, result.getLatitude(), 0.0001); // ±0.0001の誤差を許容
```

### 6.3 テストデータの問題

#### 問題: 既存データとの競合
```java
// 問題のあるコード（既存データを考慮しない）
assertEquals(2, result.size()); // 既存データがあると失敗
```

**解決策**:
```java
// 最小値での検証
assertTrue(result.size() >= 2); // 既存データ + テストデータ

// または条件での検証
long testDataCount = result.stream()
    .filter(record -> record.getTimestamp().isAfter(testStartTime))
    .count();
assertEquals(2, testDataCount);
```

## 7. 実装済みテストケース一覧（31件）

### 7.1 基本検索機能（3件）
- `testFindByUserId_WithExistingUser_ShouldReturnRecords`
- `testFindByUserId_WithNonExistentUser_ShouldReturnEmptyList`
- `testFindByUserIdAndType_WithValidData_ShouldReturnFilteredRecords`

### 7.2 日付検索機能（6件）
- `testFindByUserIdAndDate_WithValidDate_ShouldReturnRecords`
- `testFindByUserIdAndDate_WithDifferentDate_ShouldReturnEmptyList`
- `testFindByUserIdAndDateOrderByTimestampAsc_ShouldReturnOrderedRecords`
- `testFindByUserIdAndDateRange_WithValidRange_ShouldReturnRecords`
- `testFindByDateRange_ShouldReturnAllRecordsInRange`
- `testFindByUserIdAndYearAndMonth_WithValidYearMonth_ShouldReturnRecords`

### 7.3 今日・最新記録検索（3件）
- `testFindTodayRecordsByUserId_WithTodayRecords_ShouldReturnRecords`
- `testFindTopByUserIdOrderByTimestampDesc_ShouldReturnLatestRecord`
- `testFindLatestByUserIdAndType_ShouldReturnLatestByType`

### 7.4 最近の記録検索（3件）
- `testFindRecentRecordsByUserIdAndType_WithRecentRecords_ShouldReturnRecords`
- `testFindRecentRecordsByUserIdAndType_WithNoRecentRecords_ShouldReturnEmptyList`
- `testFindByUserIdAndTimeRange_WithValidRange_ShouldReturnRecords`

### 7.5 タイプ別検索（3件）
- `testFindClockInRecordsByUserId_ShouldReturnOnlyClockInRecords`
- `testFindClockOutRecordsByUserId_ShouldReturnOnlyClockOutRecords`
- `testFindByTypeAndTimestampBetween_ShouldReturnFilteredRecords`

### 7.6 処理状態検索（2件）
- `testFindUnprocessedRecords_ShouldReturnUnprocessedRecords`
- `testFindUnprocessedRecords_WithProcessedRecords_ShouldReturnOnlyUnprocessed`

### 7.7 統計情報取得（2件）
- `testCountTodayClockInUsers_WithTodayRecords_ShouldReturnCount`
- `testCountTodayRecords_WithTodayRecords_ShouldReturnCount`

### 7.8 部署別検索（1件）
- `testFindByDepartmentAndDate_WithValidDepartmentAndDate_ShouldReturnRecords`

### 7.9 エッジケース・境界値テスト（3件）
- `testFindByUserId_WithNullUserId_ShouldHandleGracefully`
- `testFindByUserIdAndType_WithNullType_ShouldHandleGracefully`
- `testFindByUserIdAndDate_WithNullDate_ShouldHandleGracefully`

### 7.10 データ整合性テスト（3件）
- `testSaveAndRetrieve_ShouldMaintainDataIntegrity`
- `testUpdateRecord_ShouldReflectChanges`
- `testDeleteRecord_ShouldRemoveFromDatabase`

### 7.11 パフォーマンステスト（2件）
- `testLargeDatasetQuery_ShouldPerformEfficiently`
- `testFindByUserIdAndYearAndMonth_WithDifferentMonth_ShouldReturnEmptyList`

## 8. まとめ

### 8.1 リポジトリテストの重要ポイント
1. **実データベース活用**: 本番環境と同じPostgreSQLでの動作確認
2. **制約検証**: 外部キー制約、NOT NULL制約等の実際の動作確認
3. **ネイティブクエリ**: PostgreSQL固有のSQL構文とデータベース関数の確認
4. **時刻処理**: タイムゾーン統一と時刻比較の精度管理
5. **パフォーマンス**: 大量データでの検索性能とインデックス効果の確認

### 8.2 テスト品質向上のチェックリスト
- [ ] 実データベース（comsys_test_dump.sql）を使用
- [ ] 外部キー制約を遵守した実在するユーザーIDを使用
- [ ] タイムゾーンは日本時間（JST）で統一
- [ ] 浮動小数点比較は適切な許容誤差を設定
- [ ] 既存データを考慮した検証（>= 比較）
- [ ] null安全性を最初に確認
- [ ] ネイティブクエリの動作を確認
- [ ] パフォーマンスとメモリ使用量を考慮
- [ ] @Transactionalによる自動ロールバックを活用

### 8.3 他のテストとの違い
| 項目 | リポジトリテスト | サービステスト | コントローラーテスト |
|------|------------------|----------------|---------------------|
| **データベース** | 実DB使用 | モック使用 | モック使用 |
| **トランザクション** | 実トランザクション | モック | モック |
| **制約検証** | 実制約確認 | 制約なし | 制約なし |
| **SQL確認** | 実SQL実行 | SQL未実行 | SQL未実行 |
| **パフォーマンス** | 実測定 | 測定不可 | 測定不可 |
| **データ整合性** | 実確認 | モック動作 | モック動作 |

この手順書に従うことで、勤怠記録データアクセス層の特性を考慮した包括的で信頼性の高いテストケースを作成できます。特に実データベース環境での制約検証、ネイティブクエリの動作確認、時刻処理の複雑性を適切に扱うことで、実用的なテストスイートを構築できます。