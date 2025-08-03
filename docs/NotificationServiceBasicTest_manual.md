# NotificationServiceBasicTest テストケース作成手順書

## 概要
本書は、`NotificationServiceBasicTest` のテストケース作成における注釈、統合テスト戦略、実データベース活用、テスト作成の流れとコツを詳細に説明した手順書です。通知サービスの特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/service/NotificationServiceBasicTest.java`

### 1.2 基本アノテーション

#### @SpringBootTest
**行**: 25
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class NotificationServiceBasicTest {
```

**目的**:
- Spring Boot統合テスト環境の構築
- 実際のSpringコンテキストを使用したテスト実行
- 実データベース（`comsys_test_dump.sql`）との統合

**通知サービステストの特徴**:
- 実データベースを使用した統合テスト
- トランザクション管理による各テストの独立性確保
- 実際のユーザーデータを活用したリアルなテスト環境

#### @Transactional
**行**: 26
```java
@Transactional
```

**役割**:
- 各テストメソッドをトランザクション内で実行
- テスト終了時の自動ロールバック
- テスト間のデータ汚染防止

**重要な注意点**:
- Hibernateの1次キャッシュ問題に対処が必要
- `entityManager.flush()` と `entityManager.clear()` の活用

#### @ActiveProfiles("test")
**行**: 27
```java
@ActiveProfiles("test")
```

**目的**:
- テスト専用プロファイルの有効化
- テスト用データベース設定の適用
- 本番環境との分離

### 1.3 依存性注入

#### @Autowired NotificationService
**行**: 29-30
```java
@Autowired
private NotificationService notificationService;
```

**役割**:
- テスト対象サービスの注入
- 実際のサービス実装を使用した統合テスト
- モックではなく実装を直接テスト

**テスト対象メソッド**:
```java
// 主要なテスト対象メソッド
createNotification(Integer userId, String title, String message, String type, Integer relatedId)
getUserNotifications(Integer userId, boolean unreadOnly)
markAsRead(Long notificationId)
markAllAsRead(Integer userId)
```

#### @Autowired NotificationRepository
**行**: 32-33
```java
@Autowired
private NotificationRepository notificationRepository;
```

**役割**:
- データベース直接アクセスによる検証
- サービス層を経由しない状態確認
- テストデータのクリーンアップ

#### @Autowired UserRepository
**行**: 35-36
```java
@Autowired
private UserRepository userRepository;
```

**役割**:
- 実データベースからのユーザー取得
- 実際のユーザーデータを使用したテスト
- テスト用ユーザーの作成（フォールバック）

#### @Autowired EntityManager
**行**: 38-39
```java
@Autowired
private EntityManager entityManager;
```

**役割**:
- Hibernateの1次キャッシュ制御
- `flush()` と `clear()` によるキャッシュクリア
- トランザクション内での最新データ取得

### 1.4 テスト用フィールド定義

#### 基準時刻とテストユーザー
**行**: 41-42
```java
private OffsetDateTime baseTime;
private User testUser;
```

**設計思想**:
- **基準時刻**: JST（日本標準時）での一貫した時刻管理
- **テストユーザー**: 実データベースから取得または動的作成

### 1.5 セットアップメソッド

#### @BeforeEach setUp()
**行**: 44-52
```java
@BeforeEach
void setUp() {
    // 基準時刻を設定（JST）
    baseTime = OffsetDateTime.now(ZoneOffset.ofHours(9));

    // テスト用ユーザーの準備
    setupTestUser();

    // テスト用ユーザーの通知をクリーンアップ
    cleanupUserNotifications();
}
```

**特徴**:
- 各テスト実行前の環境初期化
- 実データベースとの統合による動的セットアップ
- テスト間の独立性確保

#### setupTestUser() メソッド
**行**: 54-72
```java
private void setupTestUser() {
    // 実データベースから既存のユーザーを取得
    List<User> allUsers = userRepository.findAll();
    if (!allUsers.isEmpty()) {
        testUser = allUsers.get(0);
    } else {
        // フォールバック: 新しいユーザーを作成
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        testUser = new User();
        testUser.setUsername("test_user_" + timestamp);
        testUser.setPasswordHash("$2a$10$test");
        testUser.setLocationType("office");
        testUser.setEmail("test_" + timestamp + "@company.com");
        testUser.setIsActive(true);
        testUser.setCreatedAt(baseTime.minusDays(30));
        testUser.setUpdatedAt(baseTime.minusDays(30));
        testUser = userRepository.save(testUser);
    }
}
```

**実データベース活用戦略**:
- **優先**: 既存ユーザーデータの活用
- **フォールバック**: 動的ユーザー作成
- **一意性保証**: タイムスタンプベースの命名

#### cleanupUserNotifications() メソッド
**行**: 74-81
```java
private void cleanupUserNotifications() {
    if (testUser != null) {
        // テスト用ユーザーの通知を全て削除
        List<Notification> userNotifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(testUser.getId().intValue());
        notificationRepository.deleteAll(userNotifications);
    }
}
```

**クリーンアップ戦略**:
- テスト実行前の通知データクリア
- テスト間の独立性確保
- 実データベースの状態リセット

## 2. テストケース詳細解析

### 2.1 基本通知作成テスト

#### テストケース1: 正常な通知作成
**メソッド**: `testCreateNotification_ValidData_ShouldCreateSuccessfully`
**行**: 85-105

##### テストデータ準備
```java
// Given
String title = "基本テスト通知";
String message = "基本テスト用の通知メッセージです";
String type = "system";
Integer relatedId = 123;
```

##### 実行と検証
```java
// When
Notification notification = notificationService.createNotification(
        testUser.getId().intValue(), title, message, type, relatedId);

// Then - 基本属性検証
assertNotNull(notification, "通知が作成されること");
assertEquals(testUser.getId().intValue(), notification.getUserId(), "ユーザーIDが正しいこと");
assertEquals(title, notification.getTitle(), "タイトルが正しいこと");
assertEquals(message, notification.getMessage(), "メッセージが正しいこと");
assertEquals(type, notification.getType(), "タイプが正しいこと");
assertEquals(relatedId, notification.getRelatedId(), "関連IDが正しいこと");
assertFalse(notification.getIsRead(), "初期状態は未読であること");
assertNotNull(notification.getCreatedAt(), "作成日時が設定されていること");
```

##### データベース永続化検証
```java
// データベースに保存されていることを確認
Optional<Notification> savedNotification = notificationRepository.findById(notification.getId());
assertTrue(savedNotification.isPresent(), "通知がデータベースに保存されていること");
```

**重要ポイント**:
- **型変換**: `User.getId()`（Long）から`Integer`への変換
- **永続化確認**: サービス層とリポジトリ層の両方で検証
- **初期状態**: 未読状態での作成確認

### 2.2 通知取得テスト群

#### テストケース2: 未読通知のみ取得
**メソッド**: `testGetUserNotifications_UnreadOnly_ShouldReturnUnreadNotifications`
**行**: 107-127

##### 複数通知の準備
```java
// Given - 複数の通知を作成（一部を既読にする）
String uniqueId = String.valueOf(System.currentTimeMillis());
Notification notification1 = notificationService.createNotification(
        testUser.getId().intValue(), "未読通知1_" + uniqueId, "未読メッセージ1", "system", null);
Notification notification2 = notificationService.createNotification(
        testUser.getId().intValue(), "未読通知2_" + uniqueId, "未読メッセージ2", "leave", null);
Notification notification3 = notificationService.createNotification(
        testUser.getId().intValue(), "既読予定通知_" + uniqueId, "既読予定メッセージ", "correction", null);

// 1つを既読にする
notificationService.markAsRead(notification3.getId());
```

##### フィルタリング検証
```java
// When - 未読のみ取得
List<Notification> unreadNotifications = notificationService.getUserNotifications(
        testUser.getId().intValue(), true);

// Then
assertEquals(2, unreadNotifications.size(), "未読通知が2件取得されること");
assertTrue(unreadNotifications.stream().allMatch(n -> !n.getIsRead()), "全て未読であること");
assertTrue(unreadNotifications.stream().anyMatch(n -> n.getId().equals(notification1.getId())), 
        "通知1が含まれること");
assertTrue(unreadNotifications.stream().anyMatch(n -> n.getId().equals(notification2.getId())), 
        "通知2が含まれること");
```

**重要ポイント**:
- **一意性保証**: タイムスタンプベースのタイトル生成
- **状態管理**: 既読・未読状態の適切な管理
- **フィルタリング**: 未読のみの正確な取得

#### テストケース7: 存在しないユーザーの処理
**メソッド**: `testGetUserNotifications_NonExistentUser_ShouldReturnEmptyList`
**行**: 207-217

##### 境界値テスト
```java
// Given - 存在しないユーザーID
Integer nonExistentUserId = 99999;

// When
List<Notification> notifications = notificationService.getUserNotifications(nonExistentUserId, false);

// Then
assertNotNull(notifications, "空リストが返されること");
assertTrue(notifications.isEmpty(), "通知リストが空であること");
```

### 2.3 既読化機能テスト群

#### テストケース3: 個別通知の既読化
**メソッド**: `testMarkAsRead_ValidNotification_ShouldMarkAsRead`
**行**: 129-143

##### 状態変更テスト
```java
// Given - 未読通知を作成
String uniqueId = String.valueOf(System.currentTimeMillis());
Notification notification = notificationService.createNotification(
        testUser.getId().intValue(), "既読テスト通知_" + uniqueId, "既読テスト用メッセージ", "system", null);
assertFalse(notification.getIsRead(), "初期状態は未読であること");

// When
notificationService.markAsRead(notification.getId());

// Then
Optional<Notification> updatedNotification = notificationRepository.findById(notification.getId());
assertTrue(updatedNotification.isPresent(), "通知が存在すること");
assertTrue(updatedNotification.get().getIsRead(), "通知が既読になっていること");
```

#### テストケース4: 全通知の一括既読化
**メソッド**: `testMarkAllAsRead_MultipleNotifications_ShouldMarkAllAsRead`
**行**: 145-185

##### 複数通知の一括処理
```java
// Given - 複数の未読通知を作成
String uniqueId = String.valueOf(System.currentTimeMillis());
notificationService.createNotification(testUser.getId().intValue(), "未読通知1_" + uniqueId, "メッセージ1", "system", null);
notificationService.createNotification(testUser.getId().intValue(), "未読通知2_" + uniqueId, "メッセージ2", "leave", null);
notificationService.createNotification(testUser.getId().intValue(), "未読通知3_" + uniqueId, "メッセージ3", "correction", null);

// 未読通知があることを確認
List<Notification> unreadBefore = notificationService.getUserNotifications(testUser.getId().intValue(), true);
assertEquals(3, unreadBefore.size(), "未読通知が3件あること");
```

##### Hibernateキャッシュ対策
```java
// When
notificationService.markAllAsRead(testUser.getId().intValue());

// Hibernateの1次キャッシュをクリアして最新の状態を取得
entityManager.flush();
entityManager.clear();
```

##### 一括処理結果検証
```java
// Then
List<Notification> unreadAfter = notificationService.getUserNotifications(testUser.getId().intValue(), true);
assertEquals(0, unreadAfter.size(), "未読通知が0件になること");

List<Notification> allNotifications = notificationService.getUserNotifications(testUser.getId().intValue(), false);
assertTrue(allNotifications.stream().allMatch(n -> Boolean.TRUE.equals(n.getIsRead())), 
        "全ての通知が既読になっていること");
```

**重要ポイント**:
- **キャッシュ制御**: `entityManager.flush()` と `clear()` の必須使用
- **Boolean比較**: `Boolean.TRUE.equals()` による安全な比較
- **一括処理**: 複数レコードの同時更新確認

### 2.4 実データベース統合テスト

#### テストケース5: 実データベースユーザーとの統合
**メソッド**: `testCreateNotification_WithRealDatabaseData_ShouldWorkCorrectly`
**行**: 187-205

##### 実データ活用テスト
```java
// Given - 実データベースのユーザーを使用
List<User> realUsers = userRepository.findAll();
assertFalse(realUsers.isEmpty(), "実データベースにユーザーが存在すること");

User realUser = realUsers.get(0);
String title = "実データベーステスト通知";
String message = "実データベースのユーザーに対する通知テスト";
String type = "system";
```

##### 実データとの統合検証
```java
// When
Notification notification = notificationService.createNotification(
        realUser.getId().intValue(), title, message, type, null);

// Then
assertNotNull(notification, "通知が作成されること");
assertEquals(realUser.getId().intValue(), notification.getUserId(), "ユーザーIDが正しいこと");

System.out.println("実データベーステスト - ユーザーID: " + realUser.getId() + 
        ", 通知ID: " + notification.getId());
```

**実データベース統合の利点**:
- **リアルなテスト**: 実際のデータ構造での動作確認
- **型整合性**: 実際のデータ型での検証
- **統合性**: データベース制約の実際の動作確認

### 2.5 異常系テスト

#### テストケース6: 存在しない通知の既読化
**メソッド**: `testMarkAsRead_NonExistentNotification_ShouldThrowException`
**行**: 207-217

##### 例外処理テスト
```java
// Given - 存在しない通知ID
Long nonExistentId = 99999L;

// When & Then
assertThrows(RuntimeException.class, () -> {
    notificationService.markAsRead(nonExistentId);
}, "存在しない通知IDで例外が発生すること");
```

**異常系テストの重要性**:
- **エラーハンドリング**: 適切な例外処理の確認
- **境界値**: 存在しないIDでの動作確認
- **堅牢性**: サービスの安定性検証

## 3. 統合テスト特有の戦略

### 3.1 実データベース活用パターン

#### 実データ優先戦略
```java
// パターン1: 既存データの活用
List<User> realUsers = userRepository.findAll();
if (!realUsers.isEmpty()) {
    testUser = realUsers.get(0); // 実データを優先使用
}

// パターン2: フォールバック作成
else {
    testUser = createTestUser(); // 必要時のみ作成
}
```

#### データクリーンアップ戦略
```java
// テスト前クリーンアップ
private void cleanupUserNotifications() {
    if (testUser != null) {
        List<Notification> userNotifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(testUser.getId().intValue());
        notificationRepository.deleteAll(userNotifications);
    }
}
```

### 3.2 型変換問題の対処

#### Long ↔ Integer変換
```java
// 問題のあるコード
assertEquals(user.getId(), notification.getUserId()); // Long vs Integer

// 改善されたコード
assertEquals(user.getId().intValue(), notification.getUserId()); // Integer vs Integer
```

#### Boolean比較の安全性
```java
// 問題のあるコード
assertTrue(notification.getIsRead()); // null の場合NullPointerException

// 改善されたコード
assertTrue(Boolean.TRUE.equals(notification.getIsRead())); // null安全
```

### 3.3 Hibernateキャッシュ制御

#### 1次キャッシュ問題
```java
// 問題: 更新後も古いデータが返される
notificationService.markAllAsRead(userId);
List<Notification> notifications = notificationService.getUserNotifications(userId, false);
// → 古いキャッシュデータが返される可能性
```

#### 解決策: キャッシュクリア
```java
// 解決策: 明示的なキャッシュクリア
notificationService.markAllAsRead(userId);

// Hibernateの1次キャッシュをクリア
entityManager.flush();  // 保留中の変更をデータベースに反映
entityManager.clear();  // 1次キャッシュをクリア

List<Notification> notifications = notificationService.getUserNotifications(userId, false);
// → 最新のデータベース状態が取得される
```

### 3.4 一意性保証戦略

#### タイムスタンプベース命名
```java
// 重複防止のための一意なタイトル生成
String uniqueId = String.valueOf(System.currentTimeMillis());
String title = "テスト通知_" + uniqueId;

// 複数通知での一意性保証
Notification notification1 = notificationService.createNotification(
        userId, "未読通知1_" + uniqueId, "メッセージ1", "system", null);
Notification notification2 = notificationService.createNotification(
        userId, "未読通知2_" + uniqueId, "メッセージ2", "leave", null);
```

## 4. テストケース作成の流れ

### 4.1 統合テスト専用フロー
```
1. 実データベース環境確認
   ↓
2. テストユーザー準備（実データ優先）
   ↓
3. データクリーンアップ
   ↓
4. テストデータ作成（一意性保証）
   ↓
5. サービス層実行
   ↓
6. 多層検証（サービス + リポジトリ）
   ↓
7. キャッシュクリア（必要時）
   ↓
8. 最終状態確認
```

### 4.2 詳細手順

#### ステップ1: 実データベース環境確認
```java
/**
 * テストケース名: 実データベース統合通知作成テスト
 * 統合要件:
 * - comsys_test_dump.sqlの実データを活用
 * - 実際のユーザーデータとの整合性確認
 * - データベース制約の実動作検証
 * 
 * 前提条件:
 * - テストデータベースに実ユーザーデータが存在
 * - 通知テーブルが正常に作成済み
 * - 外部キー制約が適切に設定済み
 */
@Test
void testIntegrationWithRealDatabase() {
    // 実データベース状態の確認
    List<User> realUsers = userRepository.findAll();
    assertFalse(realUsers.isEmpty(), "実データベースにユーザーが存在すること");
}
```

#### ステップ2: テストユーザー準備
```java
// レベル1: 実データ優先取得
private void setupTestUser() {
    List<User> allUsers = userRepository.findAll();
    
    // レベル2: 実データ活用
    if (!allUsers.isEmpty()) {
        testUser = allUsers.get(0);
        log.info("実データベースユーザーを使用: ID={}, Username={}", 
                testUser.getId(), testUser.getUsername());
    }
    
    // レベル3: フォールバック作成
    else {
        testUser = createDynamicTestUser();
        log.info("動的テストユーザーを作成: ID={}", testUser.getId());
    }
}
```

#### ステップ3: 一意性保証データ作成
```java
// 一意性保証のためのデータ準備
String uniqueId = String.valueOf(System.currentTimeMillis());

// 複数通知の作成（重複防止）
List<Notification> testNotifications = Arrays.asList(
    createUniqueNotification(userId, "通知1_" + uniqueId, "system"),
    createUniqueNotification(userId, "通知2_" + uniqueId, "leave"),
    createUniqueNotification(userId, "通知3_" + uniqueId, "correction")
);
```

#### ステップ4: 多層検証
```java
// 実行
Notification result = notificationService.createNotification(userId, title, message, type, relatedId);

// レベル1: サービス層検証
assertNotNull(result);
assertEquals(expectedTitle, result.getTitle());

// レベル2: リポジトリ層検証
Optional<Notification> savedNotification = notificationRepository.findById(result.getId());
assertTrue(savedNotification.isPresent());

// レベル3: データベース直接確認
List<Notification> userNotifications = notificationRepository
        .findByUserIdOrderByCreatedAtDesc(userId);
assertTrue(userNotifications.stream()
        .anyMatch(n -> n.getId().equals(result.getId())));
```

#### ステップ5: キャッシュ制御付き検証
```java
// 更新処理実行
notificationService.markAllAsRead(userId);

// キャッシュクリア（重要）
entityManager.flush();
entityManager.clear();

// 最新状態での検証
List<Notification> updatedNotifications = notificationService.getUserNotifications(userId, false);
assertTrue(updatedNotifications.stream()
        .allMatch(n -> Boolean.TRUE.equals(n.getIsRead())));
```

## 5. テスト作成のコツとベストプラクティス

### 5.1 統合テスト特有の注意点

#### 実データベース依存の管理
```java
// 良い例: 実データ存在確認
@Test
void testWithRealData() {
    List<User> users = userRepository.findAll();
    assumeFalse(users.isEmpty(), "実データベースにユーザーが必要");
    
    // テスト実行
    User testUser = users.get(0);
    // ...
}

// 悪い例: 実データ前提
@Test
void testWithRealData() {
    User testUser = userRepository.findAll().get(0); // IndexOutOfBoundsException の可能性
    // ...
}
```

#### トランザクション境界の理解
```java
// @Transactional テスト内での注意点
@Test
@Transactional
void testTransactionalBehavior() {
    // 作成
    Notification notification = notificationService.createNotification(/*...*/);
    
    // 同一トランザクション内では即座に見える
    Optional<Notification> found = notificationRepository.findById(notification.getId());
    assertTrue(found.isPresent()); // OK
    
    // しかし、別トランザクションからは見えない（ロールバック前提）
}
```

### 5.2 型安全性の確保

#### 型変換の明示化
```java
// 推奨: 明示的な型変換
Integer userId = user.getId().intValue();
Notification notification = notificationService.createNotification(userId, title, message, type, relatedId);
assertEquals(userId, notification.getUserId());

// 非推奨: 暗黙的な型変換依存
assertEquals(user.getId(), notification.getUserId()); // Long vs Integer
```

#### null安全な比較
```java
// 推奨: null安全な Boolean 比較
assertTrue(Boolean.TRUE.equals(notification.getIsRead()));
assertFalse(Boolean.FALSE.equals(notification.getIsRead()));

// 非推奨: null危険な比較
assertTrue(notification.getIsRead()); // null の場合 NullPointerException
```

### 5.3 テストデータ管理

#### 一意性保証パターン
```java
// パターン1: タイムスタンプベース
String uniqueId = String.valueOf(System.currentTimeMillis());
String title = "テスト通知_" + uniqueId;

// パターン2: UUID ベース
String uniqueId = UUID.randomUUID().toString().substring(0, 8);
String title = "テスト通知_" + uniqueId;

// パターン3: テストメソッド名ベース
String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
String title = "テスト通知_" + methodName + "_" + System.currentTimeMillis();
```

#### クリーンアップ戦略
```java
// 戦略1: BeforeEach でのクリーンアップ
@BeforeEach
void setUp() {
    cleanupTestData();
    setupTestEnvironment();
}

// 戦略2: AfterEach でのクリーンアップ
@AfterEach
void tearDown() {
    cleanupTestData();
}

// 戦略3: 両方でのクリーンアップ（推奨）
@BeforeEach
void setUp() {
    cleanupTestData(); // 前回の残骸をクリア
    setupTestEnvironment();
}

@AfterEach
void tearDown() {
    cleanupTestData(); // 今回のデータをクリア
}
```

## 6. 拡張テストケースの提案

### 6.1 実用的なテストケース

#### 大量通知処理テスト
```java
@Test
void testCreateMultipleNotifications_Performance() {
    // 100件の通知を作成
    List<Notification> notifications = new ArrayList<>();
    String uniqueId = String.valueOf(System.currentTimeMillis());
    
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < 100; i++) {
        Notification notification = notificationService.createNotification(
                testUser.getId().intValue(),
                "大量テスト通知_" + uniqueId + "_" + i,
                "メッセージ " + i,
                "system",
                null
        );
        notifications.add(notification);
    }
    
    long endTime = System.currentTimeMillis();
    
    // 性能確認
    assertTrue(endTime - startTime < 5000, "100件作成が5秒以内で完了すること");
    assertEquals(100, notifications.size(), "全ての通知が作成されること");
}
```

#### 同時実行テスト
```java
@Test
void testConcurrentNotificationCreation() throws InterruptedException {
    int threadCount = 10;
    int notificationsPerThread = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);
    List<Notification> allNotifications = Collections.synchronizedList(new ArrayList<>());
    
    // 複数スレッドで同時に通知作成
    for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        new Thread(() -> {
            try {
                String uniqueId = String.valueOf(System.currentTimeMillis()) + "_" + threadId;
                for (int j = 0; j < notificationsPerThread; j++) {
                    Notification notification = notificationService.createNotification(
                            testUser.getId().intValue(),
                            "同時実行テスト_" + uniqueId + "_" + j,
                            "メッセージ",
                            "system",
                            null
                    );
                    allNotifications.add(notification);
                }
            } finally {
                latch.countDown();
            }
        }).start();
    }
    
    // 全スレッドの完了を待機
    assertTrue(latch.await(30, TimeUnit.SECONDS), "30秒以内に全スレッドが完了すること");
    assertEquals(threadCount * notificationsPerThread, allNotifications.size(), 
            "全ての通知が作成されること");
}
```

### 6.2 統合シナリオテスト

#### 通知ライフサイクルテスト
```java
@Test
void testNotificationLifecycle() {
    String uniqueId = String.valueOf(System.currentTimeMillis());
    
    // 1. 通知作成
    Notification notification = notificationService.createNotification(
            testUser.getId().intValue(),
            "ライフサイクルテスト_" + uniqueId,
            "ライフサイクルテストメッセージ",
            "system",
            null
    );
    
    // 2. 未読状態確認
    assertFalse(notification.getIsRead(), "作成直後は未読であること");
    
    // 3. 未読通知リストに含まれることを確認
    List<Notification> unreadNotifications = notificationService
            .getUserNotifications(testUser.getId().intValue(), true);
    assertTrue(unreadNotifications.stream()
            .anyMatch(n -> n.getId().equals(notification.getId())), 
            "未読通知リストに含まれること");
    
    // 4. 既読化
    notificationService.markAsRead(notification.getId());
    
    // 5. 既読状態確認
    Optional<Notification> updatedNotification = notificationRepository
            .findById(notification.getId());
    assertTrue(updatedNotification.isPresent(), "通知が存在すること");
    assertTrue(updatedNotification.get().getIsRead(), "既読状態になっていること");
    
    // 6. 未読通知リストから除外されることを確認
    List<Notification> unreadAfterRead = notificationService
            .getUserNotifications(testUser.getId().intValue(), true);
    assertFalse(unreadAfterRead.stream()
            .anyMatch(n -> n.getId().equals(notification.getId())), 
            "未読通知リストから除外されること");
    
    // 7. 全通知リストには含まれることを確認
    List<Notification> allNotifications = notificationService
            .getUserNotifications(testUser.getId().intValue(), false);
    assertTrue(allNotifications.stream()
            .anyMatch(n -> n.getId().equals(notification.getId())), 
            "全通知リストには含まれること");
}
```

## 7. 一般的な問題と解決策

### 7.1 統合テスト特有の問題

#### データベース状態の不整合
**問題**: テスト間でデータが残留し、期待値と異なる結果
```java
// 問題のあるコード
@Test
void testA() {
    notificationService.createNotification(userId, "テスト", "メッセージ", "system", null);
    // クリーンアップなし
}

@Test
void testB() {
    List<Notification> notifications = notificationService.getUserNotifications(userId, false);
    assertEquals(0, notifications.size()); // 前のテストのデータが残っていて失敗
}
```

**解決策**:
```java
// 改善されたコード
@BeforeEach
void setUp() {
    cleanupUserNotifications(); // 毎回クリーンアップ
    setupTestUser();
}

private void cleanupUserNotifications() {
    if (testUser != null) {
        List<Notification> userNotifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(testUser.getId().intValue());
        notificationRepository.deleteAll(userNotifications);
    }
}
```

#### Hibernateキャッシュによる古いデータ取得
**問題**: 更新後も1次キャッシュの古いデータが返される
```java
// 問題のあるコード
notificationService.markAllAsRead(userId);
List<Notification> notifications = notificationService.getUserNotifications(userId, false);
// 古いキャッシュデータが返される可能性
```

**解決策**:
```java
// 改善されたコード
notificationService.markAllAsRead(userId);

// Hibernateキャッシュクリア
entityManager.flush();
entityManager.clear();

List<Notification> notifications = notificationService.getUserNotifications(userId, false);
// 最新のデータベース状態が取得される
```

### 7.2 型変換問題

#### Long と Integer の不一致
**問題**: データベースの型とJavaの型の不一致
```java
// 問題のあるコード
User user = userRepository.findById(1L).get(); // user.getId() は Long
Notification notification = notificationService.createNotification(
        user.getId(), title, message, type, null); // Integer が期待される
```

**解決策**:
```java
// 改善されたコード
User user = userRepository.findById(1L).get();
Notification notification = notificationService.createNotification(
        user.getId().intValue(), title, message, type, null); // 明示的な変換
```

### 7.3 テストデータの重複

#### 通知タイトルの重複による予期しない動作
**問題**: 重複防止機能により通知が作成されない
```java
// 問題のあるコード
@Test
void testA() {
    notificationService.createNotification(userId, "テスト通知", "メッセージ", "system", null);
}

@Test
void testB() {
    notificationService.createNotification(userId, "テスト通知", "メッセージ", "system", null);
    // 重複防止により作成されない可能性
}
```

**解決策**:
```java
// 改善されたコード
@Test
void testA() {
    String uniqueId = String.valueOf(System.currentTimeMillis());
    notificationService.createNotification(userId, "テスト通知_" + uniqueId, "メッセージ", "system", null);
}

@Test
void testB() {
    String uniqueId = String.valueOf(System.currentTimeMillis());
    notificationService.createNotification(userId, "テスト通知_" + uniqueId, "メッセージ", "system", null);
}
```

## 8. まとめ

### 8.1 統合テストの重要ポイント
1. **実データベース活用**: `comsys_test_dump.sql`の実データを活用したリアルなテスト
2. **型安全性**: Long ↔ Integer変換の明示的な処理
3. **キャッシュ制御**: Hibernateの1次キャッシュ問題への対処
4. **一意性保証**: タイムスタンプベースの重複防止
5. **多層検証**: サービス層とリポジトリ層の両方での確認

### 8.2 テスト品質向上のチェックリスト
- [ ] 実データベースとの統合テストを実装
- [ ] 型変換は明示的に実行（Long → Integer）
- [ ] Boolean比較は null安全な方法を使用
- [ ] 更新系テストではキャッシュクリアを実行
- [ ] テストデータの一意性を保証
- [ ] 各テスト前後でデータクリーンアップを実行
- [ ] 異常系テストも含めて網羅的にテスト
- [ ] パフォーマンスと同時実行性を考慮

### 8.3 他のテストタイプとの違い
| 項目 | 統合テスト | 単体テスト | E2Eテスト |
|------|-----------|-----------|-----------|
| **データベース** | 実DB使用 | モック使用 | 実DB使用 |
| **依存関係** | 実装使用 | モック使用 | 全て実装 |
| **実行速度** | 中程度 | 高速 | 低速 |
| **テスト範囲** | サービス層 | 単一クラス | 全システム |
| **環境依存** | 中程度 | 低い | 高い |
| **デバッグ** | 中程度 | 容易 | 困難 |

この手順書に従うことで、通知サービスの特性を考慮した包括的で信頼性の高い統合テストケースを作成できます。特に実データベースとの統合、型安全性の確保、Hibernateキャッシュ制御の複雑性を適切に扱うことで、実用的なテストスイートを構築できます。