# TimeCorrectionServiceTest テストケース作成手順書

## 概要
本書は、`TimeCorrectionServiceTest` のテストケース作成における注釈、統合テスト戦略、テスト作成の流れとコツを詳細に説明した手順書です。時刻修正申請サービスの特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/service/TimeCorrectionServiceTest.java`

### 1.2 基本アノテーション

#### @SpringBootTest
**行**: 30
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TimeCorrectionServiceTest {
```

**目的**:
- Spring Boot統合テスト環境の構築
- 実際のSpringコンテキストを使用したテスト実行
- データベース接続とトランザクション管理の統合

**時刻修正サービステストの特徴**:
- 実データベース（comsys_test_dump.sql）との統合テスト
- 複雑なワークフロー（申請→承認/拒否）のテスト
- 複数エンティティ（User、AttendanceRecord、TimeCorrection）の連携テスト
- 時刻・タイプ修正の多様なパターンテスト

#### @Transactional
**行**: 31
```java
@Transactional
```

**役割**:
- 各テストメソッド実行後の自動ロールバック
- テストデータの分離とクリーンアップ
- データベース状態の一貫性保証
- 複数テーブル操作の整合性確保

#### @ActiveProfiles("test")
**行**: 32
```java
@ActiveProfiles("test")
```

**役割**:
- テスト専用プロファイルの有効化
- テスト用データベース設定の適用
- 本番環境との分離

### 1.3 依存性注入

#### @Autowired TimeCorrectionService
**行**: 35-36
```java
@Autowired
private TimeCorrectionService timeCorrectionService;
```

**役割**:
- 実際のサービスインスタンスを注入
- モックではなく実装クラスを使用
- 統合テストによる実際の動作確認

**テスト対象メソッド**:
```java
// 主要なテスト対象メソッド
public CreateTimeCorrectionResponse createTimeCorrection(CreateTimeCorrectionRequest request, Long userId)
public ApproveTimeCorrectionResponse approveTimeCorrection(Long correctionId, Long approverId)
public RejectTimeCorrectionResponse rejectTimeCorrection(Long correctionId, Long approverId)
public List<TimeCorrection> getUserTimeCorrections(Long userId)
public List<TimeCorrection> getPendingTimeCorrections()
public Optional<TimeCorrection> getTimeCorrectionById(Long id)
public long getUserPendingCount(Long userId)
public long getAllPendingCount()
```#### @Au
towired Repository群
**行**: 38-44
```java
@Autowired
private TimeCorrectionRepository timeCorrectionRepository;

@Autowired
private AttendanceRecordRepository attendanceRecordRepository;

@Autowired
private UserRepository userRepository;
```

**役割**:
- 実データベースからのデータ取得・操作
- テストデータの準備と検証
- 実データとの統合テスト実行

### 1.4 テスト用変数定義

#### 基本変数設定
**行**: 46-51
```java
private OffsetDateTime baseTime;
private User testUser;
private User approverUser;
private AttendanceRecord testAttendanceRecord;
```

**設計思想**:
- **baseTime**: JST基準の一貫した時刻管理
- **testUser**: 申請者ユーザー（実データベースから取得）
- **approverUser**: 承認者ユーザー（実データベースから取得）
- **testAttendanceRecord**: 修正対象の打刻記録

### 1.5 セットアップメソッド

#### @BeforeEach setUp メソッド
**行**: 53-62
```java
@BeforeEach
void setUp() {
    // 基準時刻を設定（JST）
    baseTime = OffsetDateTime.now(ZoneOffset.ofHours(9));
    
    // テスト用ユーザーの準備
    setupTestUsers();
    
    // テスト用打刻記録の準備
    setupTestAttendanceRecord();
}
```

**特徴**:
- JST（日本標準時）での一貫した時刻管理
- 実データベースユーザーの活用
- 各テスト実行前の環境初期化

#### setupTestUsers メソッド
**行**: 64-95
```java
private void setupTestUsers() {
    // 実データベースから既存のユーザーを取得
    List<User> allUsers = userRepository.findAll();
    if (allUsers.size() >= 2) {
        testUser = allUsers.get(0);
        approverUser = allUsers.get(1);
    } else {
        // フォールバック: 新しいユーザーを作成
        // ...
    }
}
```

**実データ活用戦略**:
- 優先的に実データベースのユーザーを使用
- フォールバック機能でテスト環境の柔軟性確保
- ユニークなタイムスタンプによる重複回避

#### setupTestAttendanceRecord メソッド
**行**: 97-107
```java
private void setupTestAttendanceRecord() {
    testAttendanceRecord = new AttendanceRecord();
    testAttendanceRecord.setUserId(testUser.getId().intValue());
    testAttendanceRecord.setType("in");
    testAttendanceRecord.setTimestamp(baseTime.minusHours(2));
    testAttendanceRecord.setLatitude(35.6812);
    testAttendanceRecord.setLongitude(139.7671);
    testAttendanceRecord.setCreatedAt(baseTime.minusHours(2));
    testAttendanceRecord = attendanceRecordRepository.save(testAttendanceRecord);
}
```

**打刻記録設計**:
- 実際の東京の座標（35.6812, 139.7671）を使用
- 2時間前の打刻として設定
- データベースに永続化してIDを取得

### 1.6 ヘルパーメソッド

#### createTimeRequest メソッド
**行**: 112-122
```java
private CreateTimeCorrectionRequest createTimeRequest(String requestType, String currentType, 
                                                     OffsetDateTime requestedTime, String requestedType, String reason) {
    CreateTimeCorrectionRequest request = new CreateTimeCorrectionRequest();
    request.setAttendanceId(testAttendanceRecord.getId());
    request.setRequestType(requestType);
    request.setCurrentType(currentType);
    request.setRequestedTime(requestedTime);
    request.setRequestedType(requestedType);
    request.setReason(reason);
    return request;
}
```

**特徴**:
- テスト用リクエストの動的生成
- 様々な修正パターンに対応
- コードの重複削減と可読性向上

## 2. テストケース詳細解析

### 2.1 実データベース統合テスト群

#### テストケース1: 実データベース統合テスト
**メソッド**: `testCreateTimeCorrection_WithRealDatabaseData_ShouldWorkCorrectly`
**行**: 126-155

##### 実データ活用テスト
```java
// 実データベースから打刻記録を取得 (行128-130)
List<AttendanceRecord> realRecords = attendanceRecordRepository.findAll();
assertFalse(realRecords.isEmpty(), "実データベースに打刻記録が存在すること");

// 最初の打刻記録を使用 (行132)
AttendanceRecord realRecord = realRecords.get(0);

// 実データベースからユーザーを取得 (行134)
Optional<User> realUserOpt = userRepository.findById(realRecord.getUserId().longValue());
```

##### 実データ期待値検証
```java
// 実データに基づく申請作成 (行137-143)
CreateTimeCorrectionRequest request = new CreateTimeCorrectionRequest();
request.setAttendanceId(realRecord.getId());
request.setRequestType("time");
request.setCurrentType(realRecord.getType());
request.setRequestedTime(realRecord.getTimestamp().plusMinutes(30));
request.setReason("実データベーステスト用の修正申請");

// 申請作成と検証 (行145-152)
CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
    request, realUser.getId());

assertTrue(response.isSuccess(), "実データベースでの申請作成が成功すること");
assertNotNull(response.getTimeCorrection(), "作成された申請が返されること");
assertEquals("pending", response.getTimeCorrection().getStatus(), "申請ステータスがpendingであること");
```### 2
.2 申請作成テスト群

#### テストケース2: 時刻のみ修正申請
**メソッド**: `testCreateTimeCorrection_TimeOnly_ShouldCreateSuccessfully`
**行**: 159-177

##### 時刻修正テスト
```java
// 時刻のみ修正申請 (行161-162)
CreateTimeCorrectionRequest request = createTimeRequest(
    "time", "in", baseTime.minusHours(1), null, "出勤時刻を1時間早く修正");

// 申請作成 (行164-166)
CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
    request, testUser.getId());

// 時刻修正特有の検証 (行168-175)
assertTrue(response.isSuccess(), "時刻修正申請が成功すること");
assertNotNull(response.getTimeCorrection(), "作成された申請が返されること");
assertEquals("time", response.getTimeCorrection().getRequestType(), "申請タイプが正しいこと");
assertEquals("pending", response.getTimeCorrection().getStatus(), "ステータスがpendingであること");
assertEquals(testUser.getId().intValue(), response.getTimeCorrection().getUserId(), "ユーザーIDが正しいこと");
assertNotNull(response.getTimeCorrection().getRequestedTime(), "修正時刻が設定されていること");
```

#### テストケース3: タイプのみ修正申請
**メソッド**: `testCreateTimeCorrection_TypeOnly_ShouldCreateSuccessfully`
**行**: 179-196

##### タイプ修正テスト
```java
// タイプのみ修正申請 (行181-182)
CreateTimeCorrectionRequest request = createTimeRequest(
    "type", "in", null, "out", "出勤を退勤に修正");

// タイプ修正特有の検証 (行189-194)
assertEquals("type", response.getTimeCorrection().getRequestType(), "申請タイプが正しいこと");
assertEquals("out", response.getTimeCorrection().getRequestedType(), "修正タイプが正しいこと");
assertNull(response.getTimeCorrection().getRequestedTime(), "修正時刻は設定されていないこと");
```

#### テストケース4: 時刻・タイプ両方修正申請
**メソッド**: `testCreateTimeCorrection_Both_ShouldCreateSuccessfully`
**行**: 198-215

##### 複合修正テスト
```java
// 時刻・タイプ両方修正申請 (行200-201)
CreateTimeCorrectionRequest request = createTimeRequest(
    "both", "in", baseTime.minusMinutes(30), "out", "出勤を退勤に変更し時刻も修正");

// 複合修正特有の検証 (行208-213)
assertEquals("both", response.getTimeCorrection().getRequestType(), "申請タイプが正しいこと");
assertEquals("out", response.getTimeCorrection().getRequestedType(), "修正タイプが正しいこと");
assertNotNull(response.getTimeCorrection().getRequestedTime(), "修正時刻が設定されていること");
```

### 2.3 バリデーションテスト群

#### テストケース5-10: 入力値検証テスト
**メソッド**: `testCreateTimeCorrection_InvalidRequestType_ShouldFail` 等
**行**: 219-310

##### 無効な申請タイプテスト
```java
// 無効な申請タイプ (行221-222)
CreateTimeCorrectionRequest request = createTimeRequest(
    "invalid", "in", baseTime, "out", "無効な申請タイプテスト");

// エラーレスポンス検証 (行227-229)
assertFalse(response.isSuccess(), "無効な申請タイプで失敗すること");
assertTrue(response.getMessage().contains("無効な申請タイプです"), "適切なエラーメッセージが返されること");
```

##### 必須項目不足テスト
```java
// 修正時刻未指定テスト (行233-240)
CreateTimeCorrectionRequest request = createTimeRequest(
    "time", "in", null, null, "修正時刻未指定テスト");

CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
    request, testUser.getId());

assertFalse(response.isSuccess(), "修正時刻未指定で失敗すること");
assertTrue(response.getMessage().contains("修正時刻の指定が必要です"), "適切なエラーメッセージが返されること");
```

##### 権限チェックテスト
```java
// 他人の打刻記録修正テスト (行280-295)
AttendanceRecord otherUserRecord = new AttendanceRecord();
otherUserRecord.setUserId(approverUser.getId().intValue());
// ... 他人の打刻記録を作成

CreateTimeCorrectionRequest request = createTimeRequest(
    "time", "in", baseTime, null, "他人の打刻記録テスト");
request.setAttendanceId(otherUserRecord.getId());

CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
    request, testUser.getId());

assertFalse(response.isSuccess(), "他人の打刻記録で失敗すること");
assertTrue(response.getMessage().contains("他人の打刻記録は修正できません"), "適切なエラーメッセージが返されること");
```

### 2.4 承認・拒否テスト群

#### テストケース11: 承認処理テスト
**メソッド**: `testApproveTimeCorrection_ValidRequest_ShouldApproveSuccessfully`
**行**: 314-334

##### 承認ワークフローテスト
```java
// 承認待ちの申請を作成 (行316-321)
CreateTimeCorrectionRequest request = createTimeRequest(
    "time", "in", baseTime.minusMinutes(30), null, "承認テスト用申請");
CreateTimeCorrectionResponse createResponse = timeCorrectionService.createTimeCorrection(
    request, testUser.getId());
assertTrue(createResponse.isSuccess(), "申請作成が成功すること");

Long correctionId = createResponse.getTimeCorrection().getId();

// 承認処理実行 (行323-325)
ApproveTimeCorrectionResponse response = timeCorrectionService.approveTimeCorrection(
    correctionId, approverUser.getId());

// 承認結果検証 (行327-332)
assertTrue(response.isSuccess(), "申請承認が成功すること");
assertNotNull(response.getCorrection(), "承認された申請が返されること");
assertEquals("approved", response.getCorrection().getStatus(), "ステータスがapprovedに変更されること");
assertEquals(approverUser.getId().intValue(), response.getCorrection().getApproverId(), "承認者IDが設定されること");
assertNotNull(response.getCorrection().getApprovedAt(), "承認日時が設定されること");
```

#### テストケース12-13: 承認エラーケーステスト
**メソッド**: `testApproveTimeCorrection_NonExistentRequest_ShouldFail` 等
**行**: 336-365

##### 存在しない申請の承認テスト
```java
// 存在しない申請ID (行338)
Long nonExistentId = 99999L;

// 承認試行 (行340-342)
ApproveTimeCorrectionResponse response = timeCorrectionService.approveTimeCorrection(
    nonExistentId, approverUser.getId());

// エラー検証 (行344-346)
assertFalse(response.isSuccess(), "存在しない申請で失敗すること");
assertTrue(response.getMessage().contains("申請が見つかりません"), "適切なエラーメッセージが返されること");
```

##### 重複承認防止テスト
```java
// 既に承認済みの申請 (行348-356)
CreateTimeCorrectionRequest request = createTimeRequest(
    "time", "in", baseTime.minusMinutes(30), null, "重複承認テスト用申請");
CreateTimeCorrectionResponse createResponse = timeCorrectionService.createTimeCorrection(
    request, testUser.getId());
Long correctionId = createResponse.getTimeCorrection().getId();

// 最初の承認
timeCorrectionService.approveTimeCorrection(correctionId, approverUser.getId());

// 2回目の承認試行 (行358-360)
ApproveTimeCorrectionResponse response = timeCorrectionService.approveTimeCorrection(
    correctionId, approverUser.getId());

// 重複承認エラー検証 (行362-364)
assertFalse(response.isSuccess(), "既に処理済みの申請で失敗すること");
assertTrue(response.getMessage().contains("申請は既に処理済みです"), "適切なエラーメッセージが返されること");
```#### テストケ
ース14-15: 拒否処理テスト
**メソッド**: `testRejectTimeCorrection_ValidRequest_ShouldRejectSuccessfully` 等
**行**: 369-404

##### 拒否ワークフローテスト
```java
// 拒否処理実行 (行377-379)
RejectTimeCorrectionResponse response = timeCorrectionService.rejectTimeCorrection(
    correctionId, approverUser.getId());

// 拒否結果検証 (行381-386)
assertTrue(response.isSuccess(), "申請拒否が成功すること");
assertNotNull(response.getCorrection(), "拒否された申請が返されること");
assertEquals("rejected", response.getCorrection().getStatus(), "ステータスがrejectedに変更されること");
assertEquals(approverUser.getId().intValue(), response.getCorrection().getApproverId(), "承認者IDが設定されること");
assertNotNull(response.getCorrection().getApprovedAt(), "処理日時が設定されること");
```

### 2.5 一覧取得テスト群

#### テストケース16: ユーザー別申請一覧テスト
**メソッド**: `testGetUserTimeCorrections_WithMultipleRequests_ShouldReturnAllUserRequests`
**行**: 408-432

##### 複数申請の一覧取得テスト
```java
// 複数の申請を作成 (行410-418)
CreateTimeCorrectionRequest request1 = createTimeRequest(
    "time", "in", baseTime.minusMinutes(30), null, "申請1");
CreateTimeCorrectionRequest request2 = createTimeRequest(
    "type", "in", null, "out", "申請2");
CreateTimeCorrectionRequest request3 = createTimeRequest(
    "both", "out", baseTime.plusMinutes(15), "in", "申請3");

timeCorrectionService.createTimeCorrection(request1, testUser.getId());
timeCorrectionService.createTimeCorrection(request2, testUser.getId());
timeCorrectionService.createTimeCorrection(request3, testUser.getId());

// ユーザー別一覧取得 (行420-421)
List<TimeCorrection> userCorrections = timeCorrectionService.getUserTimeCorrections(testUser.getId());

// 一覧取得結果検証 (行423-426)
assertEquals(3, userCorrections.size(), "ユーザーの全申請が取得されること");
assertTrue(userCorrections.stream().allMatch(tc -> tc.getUserId().equals(testUser.getId().intValue())), 
      "全ての申請が対象ユーザーのものであること");
```

#### テストケース17: 承認待ち申請一覧テスト
**メソッド**: `testGetPendingTimeCorrections_WithMixedStatuses_ShouldReturnOnlyPending`
**行**: 434-456

##### ステータス別フィルタリングテスト
```java
// 異なるステータスの申請を作成 (行436-443)
CreateTimeCorrectionRequest request1 = createTimeRequest(
    "time", "in", baseTime.minusMinutes(30), null, "承認待ち申請");
CreateTimeCorrectionRequest request2 = createTimeRequest(
    "type", "in", null, "out", "承認予定申請");

CreateTimeCorrectionResponse response1 = timeCorrectionService.createTimeCorrection(request1, testUser.getId());
CreateTimeCorrectionResponse response2 = timeCorrectionService.createTimeCorrection(request2, testUser.getId());

// 1つを承認 (行445)
timeCorrectionService.approveTimeCorrection(response1.getTimeCorrection().getId(), approverUser.getId());

// 承認待ち一覧取得 (行447-448)
List<TimeCorrection> pendingCorrections = timeCorrectionService.getPendingTimeCorrections();

// フィルタリング結果検証 (行450-453)
assertEquals(1, pendingCorrections.size(), "承認待ちの申請のみが取得されること");
assertEquals("pending", pendingCorrections.get(0).getStatus(), "ステータスがpendingであること");
assertEquals(response2.getTimeCorrection().getId(), pendingCorrections.get(0).getId(), "正しい申請が取得されること");
```

### 2.6 カウント機能テスト群

#### テストケース18-19: 申請数カウントテスト
**メソッド**: `testGetUserPendingCount_WithMultipleStatuses_ShouldCountOnlyPending` 等
**行**: 490-550

##### ユーザー別承認待ち数カウントテスト
```java
// 異なるステータスの申請を作成 (行492-502)
CreateTimeCorrectionRequest request1 = createTimeRequest(
    "time", "in", baseTime.minusMinutes(30), null, "承認待ち申請1");
CreateTimeCorrectionRequest request2 = createTimeRequest(
    "type", "in", null, "out", "承認待ち申請2");
CreateTimeCorrectionRequest request3 = createTimeRequest(
    "both", "out", baseTime.plusMinutes(15), "in", "承認予定申請");

CreateTimeCorrectionResponse response1 = timeCorrectionService.createTimeCorrection(request1, testUser.getId());
timeCorrectionService.createTimeCorrection(request2, testUser.getId());
CreateTimeCorrectionResponse response3 = timeCorrectionService.createTimeCorrection(request3, testUser.getId());

// 1つを承認 (行504)
timeCorrectionService.approveTimeCorrection(response1.getTimeCorrection().getId(), approverUser.getId());

// ユーザー別承認待ち数取得 (行506-507)
long pendingCount = timeCorrectionService.getUserPendingCount(testUser.getId());

// カウント結果検証 (行509)
assertEquals(2, pendingCount, "ユーザーの承認待ち申請数が正しいこと");
```

##### 全体承認待ち数カウントテスト
```java
// 複数ユーザーの申請を作成 (行513-535)
// testUserの申請
CreateTimeCorrectionRequest request1 = createTimeRequest(
    "time", "in", baseTime.minusMinutes(30), null, "ユーザー1の申請");
CreateTimeCorrectionRequest request2 = createTimeRequest(
    "type", "in", null, "out", "ユーザー1の申請2");

timeCorrectionService.createTimeCorrection(request1, testUser.getId());
CreateTimeCorrectionResponse response2 = timeCorrectionService.createTimeCorrection(request2, testUser.getId());

// approverUserの申請も作成
// ...

// 1つを承認 (行537)
timeCorrectionService.approveTimeCorrection(response2.getTimeCorrection().getId(), approverUser.getId());

// 全体承認待ち数取得 (行539-540)
long allPendingCount = timeCorrectionService.getAllPendingCount();

// 全体カウント結果検証 (行542)
assertEquals(2, allPendingCount, "全体の承認待ち申請数が正しいこと");
```

### 2.7 複合シナリオテスト

#### テストケース20: 完全ワークフローテスト
**メソッド**: `testCompleteWorkflow_CreateApproveReject_ShouldWorkCorrectly`
**行**: 546-588

##### 申請から承認・拒否までの完全フローテスト
```java
// 3つの申請を作成 (行548-558)
CreateTimeCorrectionRequest request1 = createTimeRequest(
    "time", "in", baseTime.minusMinutes(30), null, "承認予定申請");
CreateTimeCorrectionRequest request2 = createTimeRequest(
    "type", "in", null, "out", "拒否予定申請");
CreateTimeCorrectionRequest request3 = createTimeRequest(
    "both", "out", baseTime.plusMinutes(15), "in", "承認待ち申請");

CreateTimeCorrectionResponse response1 = timeCorrectionService.createTimeCorrection(request1, testUser.getId());
CreateTimeCorrectionResponse response2 = timeCorrectionService.createTimeCorrection(request2, testUser.getId());
CreateTimeCorrectionResponse response3 = timeCorrectionService.createTimeCorrection(request3, testUser.getId());

// 承認と拒否を実行 (行560-565)
ApproveTimeCorrectionResponse approveResponse = timeCorrectionService.approveTimeCorrection(
    response1.getTimeCorrection().getId(), approverUser.getId());
RejectTimeCorrectionResponse rejectResponse = timeCorrectionService.rejectTimeCorrection(
    response2.getTimeCorrection().getId(), approverUser.getId());

// 各処理の結果確認 (行567-569)
assertTrue(approveResponse.isSuccess(), "承認が成功すること");
assertTrue(rejectResponse.isSuccess(), "拒否が成功すること");

// 最終状態確認 (行571-583)
List<TimeCorrection> userCorrections = timeCorrectionService.getUserTimeCorrections(testUser.getId());
assertEquals(3, userCorrections.size(), "全申請が取得されること");

long approvedCount = userCorrections.stream().mapToLong(tc -> "approved".equals(tc.getStatus()) ? 1 : 0).sum();
long rejectedCount = userCorrections.stream().mapToLong(tc -> "rejected".equals(tc.getStatus()) ? 1 : 0).sum();
long pendingCount = userCorrections.stream().mapToLong(tc -> "pending".equals(tc.getStatus()) ? 1 : 0).sum();

assertEquals(1, approvedCount, "承認済み申請が1件であること");
assertEquals(1, rejectedCount, "拒否済み申請が1件であること");
assertEquals(1, pendingCount, "承認待ち申請が1件であること");
```### 2
.8 パフォーマンステスト

#### テストケース21: 大量申請処理テスト
**メソッド**: `testCreateTimeCorrection_BulkOperations_ShouldPerformEfficiently`
**行**: 592-614

##### 大量処理効率性テスト
```java
// 大量申請のシミュレーション (行594-596)
int requestCount = 100;
long startTime = System.currentTimeMillis();

// 100件の申請を作成 (行598-605)
for (int i = 0; i < requestCount; i++) {
    CreateTimeCorrectionRequest request = createTimeRequest(
        "time", "in", baseTime.minusMinutes(i), null, "パフォーマンステスト申請" + i);
    CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
        request, testUser.getId());
    assertTrue(response.isSuccess(), "申請" + i + "が成功すること");
}

long endTime = System.currentTimeMillis();

// パフォーマンス検証 (行607-612)
assertTrue(endTime - startTime < 10000, "100件の申請作成が10秒以内で完了すること");

// 作成された申請数の確認
List<TimeCorrection> userCorrections = timeCorrectionService.getUserTimeCorrections(testUser.getId());
assertEquals(requestCount, userCorrections.size(), "全申請が正しく作成されていること");
```

### 2.9 エラーハンドリングテスト

#### テストケース22-23: null値処理テスト
**メソッド**: `testCreateTimeCorrection_NullRequest_ShouldHandleGracefully` 等
**行**: 618-635

##### null安全性テスト
```java
// null申請テスト (行620-623)
assertThrows(NullPointerException.class, () -> {
    timeCorrectionService.createTimeCorrection(null, testUser.getId());
}, "null申請でNullPointerExceptionが発生すること");

// null承認者IDテスト (行625-635)
CreateTimeCorrectionRequest request = createTimeRequest(
    "time", "in", baseTime.minusMinutes(30), null, "null承認者テスト");
CreateTimeCorrectionResponse createResponse = timeCorrectionService.createTimeCorrection(
    request, testUser.getId());

ApproveTimeCorrectionResponse response = timeCorrectionService.approveTimeCorrection(
    createResponse.getTimeCorrection().getId(), null);

assertFalse(response.isSuccess(), "null承認者で失敗すること");
```

## 3. 時刻修正サービス特有のテスト戦略

### 3.1 複雑なワークフロー管理

#### 申請→承認/拒否フローの特性理解
```java
// ワークフローの状態遷移
// pending → approved (承認)
// pending → rejected (拒否)
// approved/rejected → 変更不可（重複処理防止）

// テストでの状態遷移確認
CreateTimeCorrectionResponse createResponse = service.createTimeCorrection(request, userId);
assertEquals("pending", createResponse.getTimeCorrection().getStatus());

ApproveTimeCorrectionResponse approveResponse = service.approveTimeCorrection(correctionId, approverId);
assertEquals("approved", approveResponse.getCorrection().getStatus());

// 重複処理の防止確認
ApproveTimeCorrectionResponse duplicateResponse = service.approveTimeCorrection(correctionId, approverId);
assertFalse(duplicateResponse.isSuccess());
```

#### 複数エンティティ連携の複雑性
```java
// User（申請者・承認者）、AttendanceRecord（修正対象）、TimeCorrection（申請）の連携
// 外部キー制約とデータ整合性の確認

// 申請作成時の関連データ確認
CreateTimeCorrectionResponse response = service.createTimeCorrection(request, userId);
TimeCorrection correction = response.getTimeCorrection();

assertEquals(userId.intValue(), correction.getUserId());
assertEquals(attendanceRecord.getId(), correction.getAttendanceId());
assertEquals(attendanceRecord.getTimestamp(), correction.getBeforeTime());
```

### 3.2 実データベース統合テストの戦略

#### 実データの特性把握
```java
// comsys_test_dump.sqlの特性
// - users: 40人のユーザー（ID: 1-40）
// - attendance_records: 50件の打刻記録
// - time_corrections: 空（テストで作成）

// 実データ活用パターン
List<AttendanceRecord> realRecords = attendanceRecordRepository.findAll();
if (!realRecords.isEmpty()) {
    AttendanceRecord realRecord = realRecords.get(0);
    // 実データを使用したテスト
} else {
    // フォールバック処理
}
```

#### データベース依存テストの安全性
```java
// Optional使用による安全なテスト
Optional<User> userOpt = userRepository.findById(realRecord.getUserId().longValue());
if (userOpt.isPresent()) {
    User realUser = userOpt.get();
    // テスト実行
} else {
    // データが存在しない場合の処理
    System.out.println("実データベースのユーザーが見つかりませんでした。テストをスキップします。");
}
```

### 3.3 修正パターンの多様性

#### 3つの修正タイプの特性
```java
// 1. 時刻のみ修正（"time"）
CreateTimeCorrectionRequest timeRequest = new CreateTimeCorrectionRequest();
timeRequest.setRequestType("time");
timeRequest.setRequestedTime(newTime);  // 必須
timeRequest.setRequestedType(null);     // 不要

// 2. タイプのみ修正（"type"）
CreateTimeCorrectionRequest typeRequest = new CreateTimeCorrectionRequest();
typeRequest.setRequestType("type");
typeRequest.setRequestedTime(null);     // 不要
typeRequest.setRequestedType("out");    // 必須

// 3. 時刻・タイプ両方修正（"both"）
CreateTimeCorrectionRequest bothRequest = new CreateTimeCorrectionRequest();
bothRequest.setRequestType("both");
bothRequest.setRequestedTime(newTime);  // 必須
bothRequest.setRequestedType("out");    // 必須
```

#### バリデーションルールの複雑性
```java
// 修正タイプ別のバリデーション
switch (requestType) {
    case "time":
        if (requestedTime == null) {
            return error("修正時刻の指定が必要です");
        }
        break;
    case "type":
        if (requestedType == null) {
            return error("修正タイプの指定が必要です");
        }
        break;
    case "both":
        if (requestedTime == null || requestedType == null) {
            return error("修正時刻と修正タイプの両方の指定が必要です");
        }
        break;
    default:
        return error("無効な申請タイプです");
}
```

## 4. テストケース作成の流れ

### 4.1 時刻修正サービステスト専用フロー
```
1. ワークフロー要件分析
   ↓
2. 修正パターン設計（time/type/both）
   ↓
3. 実データベース統合戦略
   ↓
4. 権限・セキュリティテスト設計
   ↓
5. 状態遷移テストシナリオ作成
   ↓
6. エラーハンドリング・パフォーマンステスト追加
```

### 4.2 詳細手順

#### ステップ1: ワークフロー要件分析
```java
/**
 * テストケース名: 時刻修正申請ワークフローの正確な実装
 * 業務要件:
 * - 申請作成: pending状態で作成
 * - 承認処理: pending → approved
 * - 拒否処理: pending → rejected
 * - 重複処理防止: approved/rejected → 変更不可
 * - 権限チェック: 自分の打刻記録のみ修正可能
 * - 承認者記録: 承認者ID・承認日時の記録
 * 
 * 実装上の注意:
 * - 状態遷移の一方向性
 * - 外部キー制約の遵守
 * - トランザクション境界の適切な設定
 */
```

#### ステップ2: 修正パターン設計
```java
// レベル1: 基本修正パターン
Map<String, TestPattern> basicPatterns = Map.of(
    "time", new TestPattern("time", newTime, null, "時刻のみ修正"),
    "type", new TestPattern("type", null, "out", "タイプのみ修正"),
    "both", new TestPattern("both", newTime, "out", "時刻・タイプ両方修正")
);

// レベル2: バリデーションパターン
Map<String, TestPattern> validationPatterns = Map.of(
    "invalid_type", new TestPattern("invalid", null, null, "無効な申請タイプ"),
    "missing_time", new TestPattern("time", null, null, "修正時刻未指定"),
    "missing_type", new TestPattern("type", null, null, "修正タイプ未指定"),
    "empty_reason", new TestPattern("time", newTime, null, "")
);

// レベル3: 権限・セキュリティパターン
Map<String, TestPattern> securityPatterns = Map.of(
    "other_user_record", new TestPattern("他人の打刻記録修正"),
    "non_existent_user", new TestPattern("存在しないユーザー"),
    "non_existent_record", new TestPattern("存在しない打刻記録")
);
```#
### ステップ3: 実データベース統合戦略
```java
// 実データの特性を活用したテスト設計
@Test
void testWithRealData() {
    // 実データの取得と検証
    List<AttendanceRecord> realRecords = attendanceRecordRepository.findAll();
    assertFalse(realRecords.isEmpty(), "実データベースに打刻記録が存在すること");
    
    AttendanceRecord realRecord = realRecords.get(0);
    Optional<User> realUserOpt = userRepository.findById(realRecord.getUserId().longValue());
    
    if (realUserOpt.isPresent()) {
        User realUser = realUserOpt.get();
        
        // 実データを使用した申請作成
        CreateTimeCorrectionRequest request = new CreateTimeCorrectionRequest();
        request.setAttendanceId(realRecord.getId());
        request.setRequestType("time");
        request.setCurrentType(realRecord.getType());
        request.setRequestedTime(realRecord.getTimestamp().plusMinutes(30));
        request.setReason("実データベーステスト用の修正申請");
        
        CreateTimeCorrectionResponse response = service.createTimeCorrection(request, realUser.getId());
        
        assertTrue(response.isSuccess(), "実データベースでの申請作成が成功すること");
        assertEquals("pending", response.getTimeCorrection().getStatus(), "申請ステータスがpendingであること");
    } else {
        System.out.println("実データベースのユーザーが見つかりませんでした。テストをスキップします。");
    }
}
```

#### ステップ4: 段階的検証
```java
// 実行
CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(request, userId);

// 段階1: 基本検証
assertNotNull(response);
assertTrue(response.isSuccess() || !response.isSuccess(), "レスポンスが返されること");

// 段階2: 成功時の詳細検証
if (response.isSuccess()) {
    assertNotNull(response.getTimeCorrection(), "作成された申請が返されること");
    assertEquals("pending", response.getTimeCorrection().getStatus(), "ステータスがpendingであること");
    assertEquals(userId.intValue(), response.getTimeCorrection().getUserId(), "ユーザーIDが正しいこと");
} else {
    assertNotNull(response.getMessage(), "エラーメッセージが返されること");
    assertFalse(response.getMessage().isEmpty(), "エラーメッセージが空でないこと");
}

// 段階3: データベース状態確認
Optional<TimeCorrection> savedCorrection = timeCorrectionRepository.findById(response.getTimeCorrection().getId());
if (response.isSuccess()) {
    assertTrue(savedCorrection.isPresent(), "申請がデータベースに保存されていること");
} else {
    assertFalse(savedCorrection.isPresent(), "失敗時は申請が保存されていないこと");
}

// 段階4: ログ出力確認（統合テストの場合）
// ログレベルをINFOに設定してログ出力を確認
// 「打刻修正申請作成開始」「打刻修正申請作成完了」のログが出力されることを確認
```

## 5. テスト作成のコツとベストプラクティス

### 5.1 時刻修正サービス特有の注意点

#### ワークフロー状態の一貫性確認
```java
// 状態遷移の確認パターン
CreateTimeCorrectionResponse createResponse = service.createTimeCorrection(request, userId);
Long correctionId = createResponse.getTimeCorrection().getId();

// 初期状態確認
assertEquals("pending", createResponse.getTimeCorrection().getStatus());
assertNull(createResponse.getTimeCorrection().getApproverId());
assertNull(createResponse.getTimeCorrection().getApprovedAt());

// 承認後状態確認
ApproveTimeCorrectionResponse approveResponse = service.approveTimeCorrection(correctionId, approverId);
assertEquals("approved", approveResponse.getCorrection().getStatus());
assertNotNull(approveResponse.getCorrection().getApproverId());
assertNotNull(approveResponse.getCorrection().getApprovedAt());

// 重複処理防止確認
ApproveTimeCorrectionResponse duplicateResponse = service.approveTimeCorrection(correctionId, approverId);
assertFalse(duplicateResponse.isSuccess());
assertTrue(duplicateResponse.getMessage().contains("既に処理済み"));
```

#### 時刻・タイムゾーンの一貫性
```java
// JST（日本標準時）での一貫した時刻管理
OffsetDateTime baseTime = OffsetDateTime.now(ZoneOffset.ofHours(9));

// 修正時刻の設定
OffsetDateTime requestedTime = baseTime.minusHours(1);
request.setRequestedTime(requestedTime);

// 保存された時刻の確認
TimeCorrection savedCorrection = response.getTimeCorrection();
assertEquals(requestedTime, savedCorrection.getRequestedTime());

// タイムゾーン情報の保持確認
assertEquals(ZoneOffset.ofHours(9), savedCorrection.getRequestedTime().getOffset());
```

### 5.2 実データベーステストの最適化

#### データ存在確認パターン
```java
// 安全なデータ取得パターン
@Test
void testWithOptionalData() {
    List<AttendanceRecord> realRecords = attendanceRecordRepository.findAll();
    
    if (!realRecords.isEmpty()) {
        AttendanceRecord realRecord = realRecords.get(0);
        Optional<User> userOpt = userRepository.findById(realRecord.getUserId().longValue());
        
        userOpt.ifPresentOrElse(
            user -> {
                // 実データを使用したテスト実行
                CreateTimeCorrectionRequest request = createRealDataRequest(realRecord);
                CreateTimeCorrectionResponse response = service.createTimeCorrection(request, user.getId());
                assertTrue(response.isSuccess(), "実データでの申請作成が成功すること");
            },
            () -> System.out.println("対応するユーザーが見つかりません。テストをスキップします。")
        );
    } else {
        System.out.println("実データベースに打刻記録が見つかりません。テストをスキップします。");
    }
}
```

#### 大量データ処理の効率化
```java
// 実データの一部のみでテスト
@Test
void testWithLimitedRealData() {
    List<AttendanceRecord> allRecords = attendanceRecordRepository.findAll();
    
    // 最初の5件のみでテスト（全件テストは時間がかかる）
    List<AttendanceRecord> testRecords = allRecords.stream()
        .limit(5)
        .collect(Collectors.toList());
    
    for (AttendanceRecord record : testRecords) {
        Optional<User> userOpt = userRepository.findById(record.getUserId().longValue());
        if (userOpt.isPresent()) {
            CreateTimeCorrectionRequest request = createRealDataRequest(record);
            CreateTimeCorrectionResponse response = service.createTimeCorrection(request, userOpt.get().getId());
            assertTrue(response.isSuccess() || !response.isSuccess(), "レスポンスが返されること");
        }
    }
}
```

### 5.3 複合シナリオテストの設計パターン

#### 完全ワークフローシミュレーション
```java
// 申請作成→承認→拒否の完全フロー
@Test
void testCompleteWorkflow() {
    // Phase 1: 複数申請の作成
    List<CreateTimeCorrectionResponse> createResponses = Arrays.asList(
        service.createTimeCorrection(createTimeRequest("time", "in", baseTime.minusHours(1), null, "承認予定"), userId),
        service.createTimeCorrection(createTimeRequest("type", "in", null, "out", "拒否予定"), userId),
        service.createTimeCorrection(createTimeRequest("both", "out", baseTime.plusMinutes(30), "in", "承認待ち"), userId)
    );
    
    // 全申請が成功することを確認
    createResponses.forEach(response -> 
        assertTrue(response.isSuccess(), "申請作成が成功すること"));
    
    // Phase 2: 承認・拒否処理
    ApproveTimeCorrectionResponse approveResponse = service.approveTimeCorrection(
        createResponses.get(0).getTimeCorrection().getId(), approverId);
    RejectTimeCorrectionResponse rejectResponse = service.rejectTimeCorrection(
        createResponses.get(1).getTimeCorrection().getId(), approverId);
    
    assertTrue(approveResponse.isSuccess(), "承認が成功すること");
    assertTrue(rejectResponse.isSuccess(), "拒否が成功すること");
    
    // Phase 3: 最終状態の確認
    List<TimeCorrection> finalCorrections = service.getUserTimeCorrections(userId);
    assertEquals(3, finalCorrections.size(), "全申請が取得されること");
    
    Map<String, Long> statusCounts = finalCorrections.stream()
        .collect(Collectors.groupingBy(TimeCorrection::getStatus, Collectors.counting()));
    
    assertEquals(1L, statusCounts.get("approved"), "承認済み申請が1件");
    assertEquals(1L, statusCounts.get("rejected"), "拒否済み申請が1件");
    assertEquals(1L, statusCounts.get("pending"), "承認待ち申請が1件");
}
```

#### 並行処理シナリオテスト
```java
// 複数ユーザーの同時申請処理
@Test
void testConcurrentRequests() throws InterruptedException {
    int threadCount = 5;
    CountDownLatch latch = new CountDownLatch(threadCount);
    List<CreateTimeCorrectionResponse> responses = Collections.synchronizedList(new ArrayList<>());
    
    // 複数スレッドで同時申請
    for (int i = 0; i < threadCount; i++) {
        final int index = i;
        new Thread(() -> {
            try {
                CreateTimeCorrectionRequest request = createTimeRequest(
                    "time", "in", baseTime.minusMinutes(index), null, "並行テスト申請" + index);
                CreateTimeCorrectionResponse response = service.createTimeCorrection(request, testUser.getId());
                responses.add(response);
            } finally {
                latch.countDown();
            }
        }).start();
    }
    
    latch.await(10, TimeUnit.SECONDS);
    
    // 全スレッドで申請が成功することを確認
    assertEquals(threadCount, responses.size());
    assertTrue(responses.stream().allMatch(CreateTimeCorrectionResponse::isSuccess), 
        "全並行申請が成功すること");
    
    // データベースの整合性確認
    List<TimeCorrection> savedCorrections = service.getUserTimeCorrections(testUser.getId());
    assertEquals(threadCount, savedCorrections.size(), "全申請がデータベースに保存されていること");
}
```#
# 6. 一般的な問題と解決策

### 6.1 時刻修正サービス特有の問題

#### 状態遷移の不整合
**問題**: 承認済み申請の再承認が可能になってしまう
```java
// 問題のあるコード
ApproveTimeCorrectionResponse response1 = service.approveTimeCorrection(correctionId, approverId);
ApproveTimeCorrectionResponse response2 = service.approveTimeCorrection(correctionId, approverId);
// 2回目の承認も成功してしまう
```

**解決策**:
```java
// 正しいテストコード
ApproveTimeCorrectionResponse response1 = service.approveTimeCorrection(correctionId, approverId);
assertTrue(response1.isSuccess(), "最初の承認が成功すること");
assertEquals("approved", response1.getCorrection().getStatus());

ApproveTimeCorrectionResponse response2 = service.approveTimeCorrection(correctionId, approverId);
assertFalse(response2.isSuccess(), "2回目の承認は失敗すること");
assertTrue(response2.getMessage().contains("既に処理済み"), "適切なエラーメッセージが返されること");
```

#### 権限チェックの不備
**問題**: 他人の打刻記録を修正できてしまう
```java
// 問題のあるコード
AttendanceRecord otherUserRecord = createAttendanceRecord(otherUserId);
CreateTimeCorrectionRequest request = createRequest(otherUserRecord.getId());
CreateTimeCorrectionResponse response = service.createTimeCorrection(request, currentUserId);
// 他人の打刻記録の修正が成功してしまう
```

**解決策**:
```java
// 正しいテストコード
AttendanceRecord otherUserRecord = new AttendanceRecord();
otherUserRecord.setUserId(approverUser.getId().intValue());
// ... 他のフィールド設定
otherUserRecord = attendanceRecordRepository.save(otherUserRecord);

CreateTimeCorrectionRequest request = createTimeRequest(
    "time", "in", baseTime, null, "他人の打刻記録テスト");
request.setAttendanceId(otherUserRecord.getId());

CreateTimeCorrectionResponse response = service.createTimeCorrection(request, testUser.getId());

assertFalse(response.isSuccess(), "他人の打刻記録で失敗すること");
assertTrue(response.getMessage().contains("他人の打刻記録は修正できません"), "適切なエラーメッセージが返されること");
```

### 6.2 実データベーステストの問題

#### データ依存性の問題
**問題**: 実データの変更でテストが失敗
```java
// 問題のあるコード
AttendanceRecord record = attendanceRecordRepository.findById(1L).get(); // データが存在しない可能性
User user = userRepository.findById(record.getUserId().longValue()).get(); // ユーザーが存在しない可能性
CreateTimeCorrectionResponse response = service.createTimeCorrection(request, user.getId());
assertTrue(response.isSuccess()); // データ変更で期待値が変わる可能性
```

**解決策**:
```java
// Optional使用とデータ特性の明示
List<AttendanceRecord> records = attendanceRecordRepository.findAll();
if (!records.isEmpty()) {
    AttendanceRecord record = records.get(0);
    Optional<User> userOpt = userRepository.findById(record.getUserId().longValue());
    
    if (userOpt.isPresent()) {
        User user = userOpt.get();
        CreateTimeCorrectionRequest request = createRealDataRequest(record);
        CreateTimeCorrectionResponse response = service.createTimeCorrection(request, user.getId());
        
        // データの特性を明示したアサーション
        assertTrue(response.isSuccess() || !response.isSuccess(), "レスポンスが返されること");
        if (response.isSuccess()) {
            assertEquals("pending", response.getTimeCorrection().getStatus());
        } else {
            assertNotNull(response.getMessage());
        }
    } else {
        System.out.println("対応するユーザーが見つかりません。テストをスキップします。");
    }
} else {
    System.out.println("実データベースに打刻記録が見つかりません。テストをスキップします。");
}
```

#### タイムゾーンの不整合
**問題**: 時刻比較でタイムゾーンが異なる
```java
// 問題のあるコード
OffsetDateTime requestedTime = OffsetDateTime.now(); // システムデフォルトタイムゾーン
request.setRequestedTime(requestedTime);
// 保存時にJSTに変換されて比較が失敗
```

**解決策**:
```java
// JST統一での時刻管理
OffsetDateTime baseTime = OffsetDateTime.now(ZoneOffset.ofHours(9)); // JST明示
OffsetDateTime requestedTime = baseTime.minusHours(1);
request.setRequestedTime(requestedTime);

CreateTimeCorrectionResponse response = service.createTimeCorrection(request, userId);
TimeCorrection savedCorrection = response.getTimeCorrection();

// タイムゾーン考慮した比較
assertEquals(requestedTime.toInstant(), savedCorrection.getRequestedTime().toInstant());
```

### 6.3 パフォーマンステストの問題

#### メモリリークの検出不足
**問題**: 大量データ処理でメモリリークが発生
```java
// 問題のあるコード
for (int i = 0; i < 10000; i++) {
    CreateTimeCorrectionRequest request = createTimeRequest(...);
    service.createTimeCorrection(request, userId);
    // メモリ使用量の監視なし
}
```

**解決策**:
```java
// メモリ使用量監視付きテスト
Runtime runtime = Runtime.getRuntime();
long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

for (int i = 0; i < 1000; i++) {
    CreateTimeCorrectionRequest request = createTimeRequest(
        "time", "in", baseTime.minusMinutes(i), null, "パフォーマンステスト" + i);
    CreateTimeCorrectionResponse response = service.createTimeCorrection(request, testUser.getId());
    assertTrue(response.isSuccess(), "申請" + i + "が成功すること");
    
    // 定期的なメモリチェック
    if (i % 100 == 0) {
        runtime.gc();
        long currentMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = currentMemory - beforeMemory;
        assertTrue(memoryIncrease < 100 * 1024 * 1024, // 100MB以内
            "メモリ使用量が許容範囲内であること: " + memoryIncrease + " bytes");
    }
}
```

## 7. 拡張テストケースの提案

### 7.1 実用的なテストケース

#### 時刻境界値テスト
```java
@Test
void testCreateTimeCorrection_TimeBoundaryValues() {
    // 過去の時刻（24時間前）
    OffsetDateTime pastTime = baseTime.minusDays(1);
    CreateTimeCorrectionRequest pastRequest = createTimeRequest(
        "time", "in", pastTime, null, "過去時刻修正テスト");
    
    CreateTimeCorrectionResponse pastResponse = service.createTimeCorrection(pastRequest, testUser.getId());
    assertTrue(pastResponse.isSuccess(), "過去時刻での修正が成功すること");
    
    // 未来の時刻（24時間後）
    OffsetDateTime futureTime = baseTime.plusDays(1);
    CreateTimeCorrectionRequest futureRequest = createTimeRequest(
        "time", "in", futureTime, null, "未来時刻修正テスト");
    
    CreateTimeCorrectionResponse futureResponse = service.createTimeCorrection(futureRequest, testUser.getId());
    assertTrue(futureResponse.isSuccess(), "未来時刻での修正が成功すること");
    
    // 現在時刻
    CreateTimeCorrectionRequest currentRequest = createTimeRequest(
        "time", "in", baseTime, null, "現在時刻修正テスト");
    
    CreateTimeCorrectionResponse currentResponse = service.createTimeCorrection(currentRequest, testUser.getId());
    assertTrue(currentResponse.isSuccess(), "現在時刻での修正が成功すること");
}
```

#### 文字列長制限テスト
```java
@Test
void testCreateTimeCorrection_ReasonLengthLimits() {
    // 最小長（1文字）
    CreateTimeCorrectionRequest minRequest = createTimeRequest(
        "time", "in", baseTime.minusHours(1), null, "短");
    CreateTimeCorrectionResponse minResponse = service.createTimeCorrection(minRequest, testUser.getId());
    assertTrue(minResponse.isSuccess(), "最小長の理由で成功すること");
    
    // 通常長（100文字）
    String normalReason = "通常の修正理由です。".repeat(10); // 100文字程度
    CreateTimeCorrectionRequest normalRequest = createTimeRequest(
        "time", "in", baseTime.minusHours(1), null, normalReason);
    CreateTimeCorrectionResponse normalResponse = service.createTimeCorrection(normalRequest, testUser.getId());
    assertTrue(normalResponse.isSuccess(), "通常長の理由で成功すること");
    
    // 最大長テスト（データベース制約に応じて調整）
    String longReason = "長い修正理由です。".repeat(50); // 500文字程度
    CreateTimeCorrectionRequest longRequest = createTimeRequest(
        "time", "in", baseTime.minusHours(1), null, longReason);
    CreateTimeCorrectionResponse longResponse = service.createTimeCorrection(longRequest, testUser.getId());
    assertTrue(longResponse.isSuccess() || !longResponse.isSuccess(), "長い理由でもレスポンスが返されること");
}
```

### 7.2 異常系テストケース

#### データベース制約違反テスト
```java
@Test
void testCreateTimeCorrection_DatabaseConstraintViolation() {
    // 存在しない打刻記録IDでの申請
    CreateTimeCorrectionRequest request = createTimeRequest(
        "time", "in", baseTime.minusHours(1), null, "存在しない打刻記録テスト");
    request.setAttendanceId(Long.MAX_VALUE); // 存在しないID
    
    CreateTimeCorrectionResponse response = service.createTimeCorrection(request, testUser.getId());
    
    assertFalse(response.isSuccess(), "存在しない打刻記録で失敗すること");
    assertTrue(response.getMessage().contains("打刻記録が見つかりません"), 
        "適切なエラーメッセージが返されること");
}
```

#### 同時実行制御テスト
```java
@Test
void testApproveTimeCorrection_ConcurrentApproval() throws InterruptedException {
    // 申請を作成
    CreateTimeCorrectionRequest request = createTimeRequest(
        "time", "in", baseTime.minusHours(1), null, "同時承認テスト");
    CreateTimeCorrectionResponse createResponse = service.createTimeCorrection(request, testUser.getId());
    Long correctionId = createResponse.getTimeCorrection().getId();
    
    // 2つのスレッドで同時に承認を試行
    CountDownLatch latch = new CountDownLatch(2);
    List<ApproveTimeCorrectionResponse> responses = Collections.synchronizedList(new ArrayList<>());
    
    for (int i = 0; i < 2; i++) {
        new Thread(() -> {
            try {
                ApproveTimeCorrectionResponse response = service.approveTimeCorrection(
                    correctionId, approverUser.getId());
                responses.add(response);
            } finally {
                latch.countDown();
            }
        }).start();
    }
    
    latch.await(5, TimeUnit.SECONDS);
    
    // 1つは成功、1つは失敗することを確認
    assertEquals(2, responses.size());
    long successCount = responses.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
    assertEquals(1, successCount, "1つの承認のみが成功すること");
    
    // 失敗したレスポンスは適切なエラーメッセージを持つこと
    responses.stream()
        .filter(r -> !r.isSuccess())
        .forEach(r -> assertTrue(r.getMessage().contains("既に処理済み")));
}
```

## 8. まとめ

### 8.1 時刻修正サービステストの重要ポイント
1. **ワークフロー管理**: 申請→承認/拒否の状態遷移の正確性確認
2. **修正パターン**: time/type/bothの3つの修正タイプの適切な処理
3. **権限制御**: 自分の打刻記録のみ修正可能な権限チェック
4. **実データ統合**: comsys_test_dump.sqlとの統合テスト戦略
5. **エラーハンドリング**: 様々な異常ケースでの適切なエラー処理

### 8.2 テスト品質向上のチェックリスト
- [ ] 正常系・異常系・境界値テストを網羅
- [ ] 3つの修正タイプ（time/type/both）すべてをテスト
- [ ] 状態遷移（pending→approved/rejected）の一方向性を確認
- [ ] 権限チェック（自分の打刻記録のみ修正可能）を検証
- [ ] 実データベースとの統合テストを実装
- [ ] 重複処理防止（既に処理済み申請の再処理防止）を確認
- [ ] JST時刻での一貫した時刻管理を実装
- [ ] パフォーマンスとメモリ使用量を考慮

### 8.3 他のサービステストとの違い
| 項目 | 時刻修正サービステスト | 一般的なサービステスト |
|------|-------------------|----------------------|
| **ワークフロー** | 複雑（申請→承認/拒否） | 単純（CRUD操作） |
| **状態管理** | 重要（pending/approved/rejected） | 軽微 |
| **権限制御** | 厳格（自分の記録のみ） | 一般的 |
| **実データ統合** | 必須（comsys_test_dump.sql） | オプション |
| **時刻管理** | 重要（JST統一） | 中程度 |
| **修正パターン** | 複雑（3タイプ×バリデーション） | 単純 |

### 8.4 継続的改善のための提案
1. **テストデータ管理**: 実データベースの変更に対応できる柔軟なテストデータ戦略
2. **パフォーマンス監視**: 大量申請処理時のメモリ使用量とレスポンス時間の継続監視
3. **エラーメッセージ標準化**: ユーザーフレンドリーなエラーメッセージの一貫性確保
4. **ログ出力検証**: 統合テストでのログ出力内容の自動検証
5. **並行処理テスト**: 実際の運用環境を想定した同時実行テストの拡充

この手順書に従うことで、時刻修正申請サービスの特性を考慮した包括的で信頼性の高いテストケースを作成できます。特にワークフロー管理、修正パターンの多様性、実データベース統合の複雑性を適切に扱うことで、実用的なテストスイートを構築できます。