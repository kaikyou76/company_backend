# UserServiceTest テストケース作成手順書

## 概要
本書は、`UserServiceTest` のテストケース作成における注釈、モック対象、テスト作成の流れとコツを詳細に説明した手順書です。ユーザー管理サービスの特性を考慮した専用のテスト戦略を提供します。

## 1. テストクラス構造解析

### 1.1 ファイル場所
**場所**: `src/test/java/com/example/companybackend/service/UserServiceTest.java`

### 1.2 基本アノテーション

#### @ExtendWith(MockitoExtension.class)
**行**: 26
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
```

**目的**:
- JUnit 5 でMockitoを統合
- `@Mock` アノテーションの自動初期化
- テストメソッド実行前にモックの初期化を自動実行

**ユーザーサービステストの特徴**:
- ユーザー情報のCRUD操作を包括的にテスト
- パスワード暗号化・認証機能の検証
- ページネーション機能とフィルタリング機能のテスト
- データ整合性とビジネスルールの検証

### 1.3 モックオブジェクト定義

#### @Mock UserRepository
**行**: 28-29
```java
@Mock
private UserRepository userRepository;
```

**役割**:
- ユーザー情報の CRUD 操作をモック化
- `findByUsername()` - ユーザー名による検索
- `findById()` - IDによる検索
- `findEmployeesWithFilter()` - フィルタリング付き検索
- `save()` - ユーザー情報保存
- `existsByUsername()` - ユーザー名重複チェック

#### @Mock PasswordEncoder
**行**: 31-32
```java
@Mock
private PasswordEncoder passwordEncoder;
```

**役割**:
- パスワード暗号化・検証をモック化
- `encode()` - パスワード暗号化
- `matches()` - パスワード照合
- セキュリティ機能のテストを可能にする

### 1.4 テスト用定数定義

#### ユーザー情報関連定数
**行**: 37-48
```java
private static final Long TEST_USER_ID = 1L;
private static final String TEST_USERNAME = "testuser";
private static final String TEST_PASSWORD = "password123";
private static final String TEST_ENCODED_PASSWORD = "$2a$10$encodedPassword";
private static final String TEST_FULL_NAME = "テスト太郎";
private static final String TEST_EMAIL = "test@example.com";
private static final String TEST_PHONE = "090-1234-5678";
private static final String TEST_EMPLOYEE_ID = "EMP001";
private static final Integer TEST_DEPARTMENT_ID = 1;
private static final Integer TEST_POSITION_ID = 2;
private static final String TEST_ROLE = "employee";
private static final String TEST_LOCATION_TYPE = "office";
```

**設計思想**:
- **現実的なデータ**: 実際の業務で使用される形式のデータを使用
- **テスト一貫性**: 全テストで同じ基準データを使用
- **日本語対応**: 日本の企業環境に適した文字列データ

## 2. 主要テストケース解析

### 2.1 ユーザー検索テスト群

#### テストケース1: 正常なユーザー名検索
**メソッド**: `testFindByUsername_WithExistingUser_ShouldReturnUser`

##### モック設定の構造
```java
// ユーザー存在確認のモック
when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
```

##### 検証ポイント
```java
// 基本情報検証
assertNotNull(result);
assertEquals(TEST_USERNAME, result.getUsername());
assertEquals(TEST_FULL_NAME, result.getFullName());
assertEquals(TEST_EMAIL, result.getEmail());

// メソッド呼び出し検証
verify(userRepository).findByUsername(TEST_USERNAME);
```

#### テストケース2: 存在しないユーザー検索
**メソッド**: `testFindByUsername_WithNonExistentUser_ShouldReturnNull`

##### 異常系テストの設計
```java
// 存在しないユーザーのモック設定
when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

// null返却の検証
User result = userService.findByUsername("nonexistent");
assertNull(result);
verify(userRepository).findByUsername("nonexistent");
```

### 2.2 ユーザープロフィール更新テスト

#### 部分更新テスト
**メソッド**: `testUpdateUserProfile_WithPartialData_ShouldUpdateSpecifiedFields`

##### 更新データの準備
```java
Map<String, Object> updateRequest = new HashMap<>();
updateRequest.put("fullName", "更新太郎");
updateRequest.put("email", "updated@example.com");
// phoneは更新しない（部分更新のテスト）
```

##### 検証ポイント
```java
// 更新された項目の確認
assertEquals("更新太郎", testUser.getFullName());
assertEquals("updated@example.com", testUser.getEmail());

// 更新されていない項目の確認
assertEquals(TEST_PHONE, testUser.getPhone()); // 元の値のまま

// 更新時刻の確認
assertNotNull(testUser.getUpdatedAt());
```

### 2.3 パスワード変更テスト

#### 正常なパスワード変更
**メソッド**: `testChangePassword_WithValidCredentials_ShouldReturnTrue`

##### セキュリティ検証の階層構造
```java
// 1. ユーザー存在確認
when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

// 2. 旧パスワード照合
when(passwordEncoder.matches(oldPassword, TEST_ENCODED_PASSWORD)).thenReturn(true);

// 3. 新パスワード暗号化
when(passwordEncoder.encode(newPassword)).thenReturn(newEncodedPassword);

// 4. 保存処理
when(userRepository.save(any(User.class))).thenReturn(testUser);
```

##### セキュリティ検証ポイント
```java
// 処理結果の確認
assertTrue(result);
assertEquals(newEncodedPassword, testUser.getPasswordHash());
assertNotNull(testUser.getUpdatedAt());

// セキュリティ処理の確認
verify(passwordEncoder).matches(oldPassword, TEST_ENCODED_PASSWORD);
verify(passwordEncoder).encode(newPassword);
verify(userRepository).save(testUser);
```

### 2.4 ユーザー一覧取得とページネーション

#### フィルタリング機能テスト
**メソッド**: `testGetUsers_WithFilters_ShouldReturnFilteredUsers`

##### 複合検索条件の設定
```java
// 検索条件の準備
String searchKeyword = "開発";
Integer departmentId = 2;
String role = "developer";
String locationType = "office";
Boolean isActive = true;

// ページネーション設定
Pageable pageable = PageRequest.of(0, 10);
```

##### フィルタリング結果の検証
```java
// 結果データの確認
assertNotNull(result);
assertEquals(2L, result.get("totalCount"));

@SuppressWarnings("unchecked")
List<Map<String, Object>> userList = (List<Map<String, Object>>) result.get("users");
assertEquals(2, userList.size());

// フィルタ条件に合致することの確認
assertTrue(userList.stream().allMatch(user -> 
    user.get("name").toString().contains("開発") ||
    user.get("departmentId").equals(2)));
```

### 2.5 ユーザー作成とバリデーション

#### 重複チェック機能
**メソッド**: `testCreateUser_WithDuplicateUsername_ShouldThrowException`

##### 重複検証の実装
```java
// 重複ユーザー名のモック設定
when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(true);

// 例外発生の検証
RuntimeException exception = assertThrows(RuntimeException.class, () -> {
    userService.createUser(newUser);
});

assertEquals("このユーザー名は既に使用されています", exception.getMessage());
verify(userRepository, never()).save(any(User.class)); // 保存されないことを確認
```

### 2.6 ユーザー更新とデータ整合性

#### 部分更新の安全性テスト
**メソッド**: `testUpdateUser_WithPartialData_ShouldPreserveUnchangedFields`

##### データ整合性の確認
```java
// 更新前の値を保存
String originalUsername = testUser.getUsername();
String originalPasswordHash = testUser.getPasswordHash();
LocalDate originalHireDate = testUser.getHireDate();

// 部分更新実行
User updateData = new User();
updateData.setFullName("新しい名前");
updateData.setEmail("new@example.com");

User result = userService.updateUser(TEST_USER_ID, updateData);

// 更新された項目の確認
assertEquals("新しい名前", result.getFullName());
assertEquals("new@example.com", result.getEmail());

// 保持された項目の確認
assertEquals(originalUsername, result.getUsername());
assertEquals(originalPasswordHash, result.getPasswordHash());
assertEquals(originalHireDate, result.getHireDate());
```

## 3. ヘルパーメソッド活用

### 3.1 テストデータ生成メソッド

#### createTestUser メソッド
```java
private User createTestUser() {
    return createTestUser(TEST_USER_ID, TEST_USERNAME, TEST_FULL_NAME);
}

private User createTestUser(Long id, String username, String fullName) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    user.setPasswordHash(TEST_ENCODED_PASSWORD);
    user.setFullName(fullName);
    user.setEmail(TEST_EMAIL);
    user.setPhone(TEST_PHONE);
    user.setEmployeeId(TEST_EMPLOYEE_ID);
    user.setRole(TEST_ROLE);
    user.setDepartmentId(TEST_DEPARTMENT_ID);
    user.setPositionId(TEST_POSITION_ID);
    user.setLocationType(TEST_LOCATION_TYPE);
    user.setHireDate(LocalDate.of(2025, 1, 1));
    user.setIsActive(true);
    user.setCreatedAt(OffsetDateTime.now(ZoneOffset.ofHours(9)));
    user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.ofHours(9)));
    return user;
}
```

**設計ポイント**:
- **オーバーロード**: 異なる引数パターンに対応
- **デフォルト値**: 現実的なデフォルト値を設定
- **タイムゾーン**: 日本時間（JST）を明示的に指定

## 4. テスト作成のベストプラクティス

### 4.1 パスワード処理の注意点

#### セキュリティテストの設計
```java
// 問題のあるコード（平文パスワード使用）
user.setPassword("plainPassword");

// 改善されたコード（暗号化済みパスワード）
user.setPasswordHash(TEST_ENCODED_PASSWORD);
when(passwordEncoder.encode(anyString())).thenReturn(TEST_ENCODED_PASSWORD);
```

### 4.2 ページネーション処理の戦略

#### 境界値テストの実装
```java
// 最小サイズでのページネーション
@Test
void testGetUsers_WithMinimumSize_ShouldHandleCorrectly() {
    Pageable pageable = PageRequest.of(0, 1); // サイズ1での処理
    Page<User> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);
    
    when(userRepository.findEmployeesWithFilter(null, null, null, null, null, pageable))
            .thenReturn(emptyPage);
    
    Map<String, Object> result = userService.getUsers(0, 1, null, null, null, null, null);
    
    assertNotNull(result);
    assertEquals(0L, result.get("totalCount"));
}
```

### 4.3 モック設定の最適化

#### 条件分岐に対応したモック
```java
@BeforeEach
void setUp() {
    testUser = createTestUser();
    
    // デフォルト設定（正常系）
    lenient().when(userRepository.findById(anyLong()))
        .thenReturn(Optional.of(testUser));
    
    lenient().when(passwordEncoder.encode(anyString()))
        .thenReturn(TEST_ENCODED_PASSWORD);
}
```

## 5. 一般的な問題と解決策

### 5.1 時刻同期の問題

**問題**: テスト実行時刻とアプリケーション内部時刻のずれ

**解決策**:
```java
// 時刻を相対的に扱う
@Test
void testUpdateUserProfile_WithEmptyRequest_ShouldOnlyUpdateTimestamp() {
    Map<String, Object> updateRequest = new HashMap<>();
    
    userService.updateUserProfile(testUser, updateRequest);
    
    // 更新時刻が設定されていることを確認（具体的な時刻は比較しない）
    assertNotNull(testUser.getUpdatedAt());
}
```

### 5.2 データ型変換の問題

**問題**: Map<String, Object>での型安全性の欠如

**解決策**:
```java
// 適切な型キャストと検証
@SuppressWarnings("unchecked")
List<Map<String, Object>> userList = (List<Map<String, Object>>) result.get("users");

// 型安全性の確認
assertNotNull(userList);
assertFalse(userList.isEmpty());
assertTrue(userList.get(0).get("id") instanceof Long);
assertTrue(userList.get(0).get("name") instanceof String);
```

### 5.3 日本語文字列の処理

**問題**: 文字エンコーディングや文字数制限の問題

**解決策**:
```java
// 日本語文字列のテスト
private static final String JAPANESE_NAME = "田中太郎";
private static final String LONG_JAPANESE_NAME = "非常に長い日本語の名前テストデータ";

@Test
void testCreateUser_WithJapaneseName_ShouldHandleCorrectly() {
    User user = createTestUser();
    user.setFullName(JAPANESE_NAME);
    
    when(userRepository.save(any(User.class))).thenReturn(user);
    
    User result = userService.createUser(user);
    assertEquals(JAPANESE_NAME, result.getFullName());
}
```

## 6. まとめ

### 6.1 ユーザーサービステストの重要ポイント
1. **セキュリティ**: パスワード暗号化・認証機能の適切なテスト
2. **データ整合性**: 部分更新時の既存データ保持確認
3. **バリデーション**: 重複チェックや必須項目の検証
4. **ページネーション**: 大量データ処理とフィルタリング機能
5. **国際化対応**: 日本語文字列と文字エンコーディング

### 6.2 テスト品質向上のチェックリスト
- [ ] 正常系・異常系・境界値テストを網羅
- [ ] パスワード処理は暗号化済みデータを使用
- [ ] 部分更新時の既存データ保持を確認
- [ ] ページネーション機能の境界値をテスト
- [ ] 日本語文字列の処理を適切に検証
- [ ] モック設定は必要最小限で重複なし
- [ ] セキュリティ要件を全て確認
- [ ] データ型の安全性を考慮

### 6.3 実装済みテストケース一覧（25件）

#### ユーザー検索機能（3件）
- `testFindByUsername_WithExistingUser_ShouldReturnUser`
- `testFindByUsername_WithNonExistentUser_ShouldReturnNull`
- `testFindById_WithExistingUser_ShouldReturnUser`

#### プロフィール更新機能（3件）
- `testUpdateUserProfile_WithValidData_ShouldUpdateSuccessfully`
- `testUpdateUserProfile_WithPartialData_ShouldUpdateSpecifiedFields`
- `testUpdateUserProfile_WithEmptyRequest_ShouldOnlyUpdateTimestamp`

#### パスワード変更機能（3件）
- `testChangePassword_WithValidCredentials_ShouldReturnTrue`
- `testChangePassword_WithInvalidOldPassword_ShouldReturnFalse`
- `testChangePassword_WithNonExistentUser_ShouldReturnFalse`

#### ユーザー一覧取得機能（4件）
- `testGetUsers_WithNoFilters_ShouldReturnAllUsers`
- `testGetUsers_WithFilters_ShouldReturnFilteredUsers`
- `testGetUsers_WithPagination_ShouldReturnPagedResults`
- `testGetUsers_WithMinimumSize_ShouldHandleCorrectly`

#### ユーザー作成機能（4件）
- `testCreateUser_WithValidData_ShouldCreateSuccessfully`
- `testCreateUser_WithDuplicateUsername_ShouldThrowException`
- `testCreateUser_WithMinimalData_ShouldCreateWithDefaults`
- `testCreateUser_WithAllOptionalFields_ShouldCreateSuccessfully`

#### ユーザー更新機能（4件）
- `testUpdateUser_WithValidData_ShouldUpdateSuccessfully`
- `testUpdateUser_WithPartialData_ShouldPreserveUnchangedFields`
- `testUpdateUser_WithNonExistentUser_ShouldThrowException`
- `testUpdateUser_WithInvalidData_ShouldThrowException`

#### ユーザー削除機能（2件）
- `testDeleteUser_WithExistingUser_ShouldMarkAsInactive`
- `testDeleteUser_WithNonExistentUser_ShouldThrowException`

#### エッジケース・統合テスト（2件）
- `testFindById_WithNonExistentUser_ShouldReturnNull`
- `testGetUsers_WithEmptyResult_ShouldReturnEmptyList`

この手順書に従うことで、ユーザー管理サービスの特性を考慮した包括的で信頼性の高いテストケースを作成できます。