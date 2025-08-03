# AttendanceServiceTest テストケース作成手順書

## 概要
本書は、`AttendanceServiceTest` のテストケース作成における注釈、モック対象、テスト作成の流れとコツを詳細に説明した手順書です。勤怠管理サービスの特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/service/AttendanceServiceTest.java`

### 1.2 基本アノテーション

#### @ExtendWith(MockitoExtension.class)
**行**: 31
```java
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {
```

**目的**:
- JUnit 5 でMockitoを統合
- `@Mock` アノテーションの自動初期化
- テストメソッド実行前にモックの初期化を自動実行

**勤怠サービステストの特徴**:
- 複数のRepository（勤怠記録、ユーザー、勤務地、サマリー）を統合的にモック
- 位置情報検証、時刻計算、状態管理など多面的な機能をテスト
- リアルタイム処理（現在時刻）とバッチ処理（集計）の両方をカバー

### 1.3 モックオブジェクト定義

#### @Mock AttendanceRecordRepository
**行**: 33-34
```java
@Mock
private AttendanceRecordRepository attendanceRecordRepository;
```

**役割**:
- 勤怠記録の CRUD 操作をモック化
- `findTodayRecordsByUserId()` - 今日の勤怠記録取得
- `findRecentRecordsByUserIdAndType()` - 重複打刻防止チェック
- `save()` - 打刻記録保存
- `findByUserIdAndDate()` - 指定日の勤怠記録取得

#### @Mock UserRepository
**行**: 39-40
```java
@Mock
private UserRepository userRepository;
```

**役割**:
- ユーザー情報の取得をモック化
- `findById()` - ユーザー存在確認
- ユーザーの勤務地タイプ（office/client）と位置チェック設定を提供

#### @Mock WorkLocationRepository
**行**: 42-43
```java
@Mock
private WorkLocationRepository workLocationRepository;
```

**役割**:
- 勤務地情報の取得をモック化
- `findByType()` - 勤務地タイプ別の位置情報取得
- 位置情報検証（GPS座標と許可範囲の照合）をテスト可能にする

### 1.4 テスト用定数定義

#### 位置情報関連定数
**行**: 47-52
```java
private static final Integer TEST_USER_ID = 1;
private static final Long TEST_USER_ID_LONG = 1L;
private static final Double VALID_LATITUDE = 35.6812;
private static final Double VALID_LONGITUDE = 139.7671;
private static final Double OFFICE_LATITUDE = 35.6812;
private static final Double OFFICE_LONGITUDE = 139.7671;
```

**設計思想**:
- **現実的な座標**: 東京都心部の実際の緯度経度を使用
- **テスト一貫性**: 全テストで同じ基準位置を使用
- **境界値テスト**: 有効範囲内外の判定テストに活用

## 2. 主要テストケース解析

### 2.1 出勤打刻テスト群

#### テストケース1: 正常な出勤打刻
**メソッド**: `testClockIn_WithValidData_ShouldCreateRecord`

##### モック設定の階層構造
```java
// 1. ユーザー存在確認
when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));

// 2. 勤務地情報取得
when(workLocationRepository.findByType("office")).thenReturn(Arrays.asList(officeLocation));

// 3. 今日の勤怠記録チェック（重複防止）
when(attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());

// 4. 最近の同種打刻チェック（5分以内重複防止）
when(attendanceRecordRepository.findRecentRecordsByUserIdAndType(eq(TEST_USER_ID), eq("in"),
        any(OffsetDateTime.class)))
        .thenReturn(Collections.emptyList());

// 5. 保存処理のモック
when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> {
    AttendanceRecord record = invocation.getArgument(0);
    record.setId(1L);
    return record;
});
```

##### 検証ポイント
```java
// 基本情報検証
assertNotNull(result);
assertEquals(TEST_USER_ID, result.getUserId());
assertEquals("in", result.getType());
assertEquals(VALID_LATITUDE, result.getLatitude());
assertEquals(VALID_LONGITUDE, result.getLongitude());
assertNotNull(result.getTimestamp());

// 副作用検証
verify(attendanceRecordRepository).save(any(AttendanceRecord.class));
```

#### テストケース2: ユーザー不存在エラー
**メソッド**: `testClockIn_WithNonExistentUser_ShouldThrowException`

##### 異常系テストの設計
```java
// ユーザー不存在のモック設定
when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.empty());

// 例外発生の検証
IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
    attendanceService.clockIn(TEST_USER_ID, VALID_LATITUDE, VALID_LONGITUDE);
});

assertEquals("ユーザーが見つかりません: " + TEST_USER_ID, exception.getMessage());
verify(attendanceRecordRepository, never()).save(any(AttendanceRecord.class));
```

### 2.2 位置情報検証テスト

#### 境界値テスト
```java
// 緯度範囲外（91.0度）での打刻試行
IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
    attendanceService.clockIn(TEST_USER_ID, 91.0, VALID_LONGITUDE); // 緯度の範囲外
});

assertEquals("位置情報が無効です", exception.getMessage());
```

**境界値設計**:
- 緯度有効範囲: -90.0 ≤ lat ≤ 90.0
- 経度有効範囲: -180.0 ≤ lon ≤ 180.0

#### 距離計算による位置検証
```java
// オフィスから200m離れた位置
Double farLatitude = 35.6830; // 約200m北
Double farLongitude = 139.7671;

// 距離制限エラーの確認
IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
    attendanceService.clockIn(TEST_USER_ID, farLatitude, farLongitude);
});

assertEquals("オフィスから100m以上離れた場所での打刻はできません", exception.getMessage());
```

### 2.3 時間制約テスト

#### 重複打刻防止
```java
// 2分前の出勤記録を準備
AttendanceRecord recentRecord = createAttendanceRecord(1L, TEST_USER_ID, "in",
        OffsetDateTime.now().minusMinutes(2));

// 5分以内重複チェックのモック
when(attendanceRecordRepository.findRecentRecordsByUserIdAndType(eq(TEST_USER_ID), eq("in"),
        any(OffsetDateTime.class)))
        .thenReturn(Arrays.asList(recentRecord));

// 重複エラーの確認
IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
    attendanceService.clockIn(TEST_USER_ID, VALID_LATITUDE, VALID_LONGITUDE);
});

assertEquals("5分以内に重複する出勤打刻があります", exception.getMessage());
```

### 2.4 退勤処理とサマリー更新

#### 退勤処理の複合テスト
```java
// 既存出勤記録の準備
AttendanceRecord clockInRecord = createAttendanceRecord(1L, TEST_USER_ID, "in",
        OffsetDateTime.now().minusHours(8));

// 退勤後の記録リスト準備
when(attendanceRecordRepository.findByUserIdAndDate(eq(TEST_USER_ID), any(LocalDate.class)))
        .thenReturn(Arrays.asList(clockInRecord,
                createAttendanceRecord(2L, TEST_USER_ID, "out", OffsetDateTime.now())));

// サマリー更新の確認
verify(attendanceSummaryRepository).save(any(AttendanceSummary.class));
```

### 2.5 日次サマリー計算

#### 時間計算テスト
```java
// 9時間勤務のテストデータ
OffsetDateTime clockInTime = OffsetDateTime.now().minusHours(9);
OffsetDateTime clockOutTime = OffsetDateTime.now();

// 勤務時間計算の検証
assertEquals(new BigDecimal("9.00"), summary.getTotalHours());
assertEquals(new BigDecimal("1.00"), summary.getOvertimeHours()); // 8時間超過分
assertEquals("completed", summary.getStatus());
```

**時間計算ロジック**:
- 総勤務時間 = 退勤時刻 - 出勤時刻
- 残業時間 = max(総勤務時間 - 8時間, 0)
- 精度: 小数点以下2位（BigDecimal）

## 3. ヘルパーメソッド活用

### 3.1 テストデータ生成メソッド

#### createTestUser メソッド
```java
private User createTestUser(Long id, String locationType, boolean skipLocationCheck) {
    User user = new User();
    user.setId(id);
    user.setLocationType(locationType);
    user.setSkipLocationCheck(skipLocationCheck);
    return user;
}
```

#### createWorkLocation メソッド
```java
private WorkLocation createWorkLocation(String type, Double latitude, Double longitude, Integer radius) {
    WorkLocation location = new WorkLocation();
    location.setType(type);
    location.setLatitude(latitude);
    location.setLongitude(longitude);
    location.setRadius(radius);
    return location;
}
```

## 4. テスト作成のベストプラクティス

### 4.1 時刻処理の注意点

#### 現在時刻の扱い
```java
// 問題のあるコード（時刻依存）
OffsetDateTime now = OffsetDateTime.now();
AttendanceRecord record = new AttendanceRecord();
record.setTimestamp(now);

// 改善されたコード（相対時刻）
OffsetDateTime baseTime = OffsetDateTime.now();
AttendanceRecord clockIn = createRecord(baseTime.minusHours(8));
AttendanceRecord clockOut = createRecord(baseTime);
```

### 4.2 位置情報テストの戦略

#### GPS座標の現実性
```java
// 実際の東京都心部の座標を使用
private static final Double TOKYO_STATION_LAT = 35.6812;
private static final Double TOKYO_STATION_LON = 139.7671;

// 距離計算テスト用の座標
private static final Double NEARBY_LAT = 35.6820;  // 約100m北
private static final Double FAR_LAT = 35.6900;     // 約1km北
```

### 4.3 モック設定の最適化

#### 条件分岐に対応したモック
```java
@BeforeEach
void setUp() {
    // デフォルト設定（正常系）
    lenient().when(userRepository.findById(anyLong()))
        .thenReturn(Optional.of(createTestUser(1L, "office", false)));
    
    lenient().when(workLocationRepository.findByType(anyString()))
        .thenReturn(Arrays.asList(createWorkLocation("office", OFFICE_LAT, OFFICE_LON, 100)));
}
```

## 5. 一般的な問題と解決策

### 5.1 時刻同期の問題

**問題**: テスト実行時刻とアプリケーション内部時刻のずれ

**解決策**:
```java
// 時刻を固定
OffsetDateTime fixedTime = OffsetDateTime.of(2025, 2, 1, 9, 0, 0, 0, ZoneOffset.ofHours(9));
when(clockProvider.now()).thenReturn(fixedTime);

AttendanceRecord result = attendanceService.clockIn(1, lat, lon);
assertEquals(fixedTime, result.getTimestamp());
```

### 5.2 位置情報精度の問題

**問題**: GPS座標の浮動小数点精度による比較エラー

**解決策**:
```java
// 適切な精度での比較
assertEquals(35.6812, result.getLatitude(), 0.0001); // 約10m精度
```

## 6. まとめ

### 6.1 勤怠サービステストの重要ポイント
1. **時刻管理**: 現在時刻に依存する処理の適切なテスト設計
2. **位置検証**: GPS座標と距離計算の精度を考慮したテスト
3. **状態管理**: 勤怠状態の遷移と整合性の確認
4. **業務ルール**: 重複防止、時間制約などの制約条件テスト
5. **データ整合性**: 打刻記録とサマリーデータの連携確認

### 6.2 テスト品質向上のチェックリスト
- [ ] 正常系・異常系・境界値テストを網羅
- [ ] 時刻処理は固定時刻または相対時刻を使用
- [ ] 位置情報は適切な精度で比較
- [ ] モック設定は必要最小限で重複なし
- [ ] テストデータは現実的で意味のある値を使用
- [ ] 業務ルールの制約条件を全て確認
- [ ] パフォーマンスとメモリ使用量を考慮

この手順書に従うことで、勤怠管理サービスの特性を考慮した包括的で信頼性の高いテストケースを作成できます。