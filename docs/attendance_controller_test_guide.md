# AttendanceController单元测试说明书

このドキュメントでは、[AttendanceController](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceController.java)のユニットテストに関する詳細な説明を提供します。

## 1. テスト対象

テスト対象は[AttendanceController](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceController.java)クラスで、以下の4つの主要なAPIエンドポイントをカバーしています：

1. 出勤打刻 (POST /api/attendance/clock-in)
2. 退勤打刻 (POST /api/attendance/clock-out)
3. 勤怠記録取得 (GET /api/attendance/records)
4. 日次サマリー取得 (GET /api/attendance/daily-summary)

## 2. APIエンドポイントの仕様

### 2.1 出勤打刻 (POST /api/attendance/clock-in)

出勤打刻を行うエンドポイントです。

**リクエストボディ**:
```json
{
  "latitude": 35.6895,
  "longitude": 139.6917
}
```

**成功レスポンス**:
```json
{
  "success": true,
  "message": "出勤打刻が完了しました",
  "data": {
    "recordId": 12345,
    "timestamp": "2025-01-18T09:00:00+09:00",
    "locationVerified": true
  }
}
```

### 2.2 退勤打刻 (POST /api/attendance/clock-out)

退勤打刻を行うエンドポイントです。

**リクエストボディ**:
```json
{
  "latitude": 35.6895,
  "longitude": 139.6917
}
```

**成功レスポンス**:
```json
{
  "success": true,
  "message": "退勤打刻が完了しました",
  "data": {
    "recordId": 12345,
    "timestamp": "2025-01-18T18:00:00+09:00",
    "workingHours": "8.5",
    "overtimeHours": "0.5"
  }
}
```

### 2.3 勤怠記録取得 (GET /api/attendance/records)

ユーザーの勤怠記録を取得するエンドポイントです。

**クエリパラメータ**:
- startDate (オプション): 開始日
- endDate (オプション): 終了日
- page (デフォルト: 0): ページ番号
- size (デフォルト: 10): ページサイズ

**成功レスポンス**:
```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 12345,
        "date": "2025-01-18",
        "clockInTime": "09:00:00",
        "clockOutTime": "18:00:00",
        "workingHours": 8.5,
        "overtimeHours": 0.5,
        "status": "NORMAL"
      }
    ],
    "totalCount": 20,
    "currentPage": 0,
    "totalPages": 2
  }
}
```

### 2.4 日次サマリー取得 (GET /api/attendance/daily-summary)

日次の勤怠サマリーを取得するエンドポイントです。

**クエリパラメータ**:
- date (オプション): 対象日（デフォルトは当日）

**成功レスポンス**:
```json
{
  "success": true,
  "data": {
    "date": "2025-01-18",
    "clockInTime": "09:00:00",
    "clockOutTime": "18:00:00",
    "workingHours": 8.5,
    "overtimeHours": 0.5,
    "breakTime": 1.0,
    "status": "NORMAL",
    "monthlyWorkingHours": 160.5,
    "monthlyOvertimeHours": 8.5
  }
}
```

## 3. 各テストメソッドの説明

### 3.1 出勤打刻テスト

#### [testClockIn_Success()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceControllerTest.java#L119-L151)

**テスト対象メソッド**: [AttendanceController.clockIn()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceController.java#L35-L69)

**目的**: 出勤打刻が正常に処理されることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java) - モック
- [AttendanceService.clockIn()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java#L118-L132) - 正常に返すようにモック設定

**検証内容**:
- HTTPステータスコードが200 OKであること
- レスポンスにsuccess=trueが含まれること
- レスポンスに適切なメッセージが含まれること
- レスポンスに記録IDが"data"オブジェクト内に含まれること
- サービスメソッドが1回呼び出されていること

#### [testClockIn_Exception()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceControllerTest.java#L159-L184)

**テスト対象メソッド**: [AttendanceController.clockIn()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceController.java#L35-L69)

**目的**: サービス層で例外が発生した場合に、適切なエラーレスポンスが返されることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java) - モック
- [AttendanceService.clockIn()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java#L118-L132) - 例外をスローするようにモック設定

**検証内容**:
- HTTPステータスコードが500 Internal Server Errorであること
- レスポンスにsuccess=falseが含まれること
- レスポンスに適切なエラーメッセージが含まれること
- サービスメソッドが1回呼び出されていること

### 3.2 退勤打刻テスト

#### [testClockOut_Success()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceControllerTest.java#L192-L224)

**テスト対象メソッド**: [AttendanceController.clockOut()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceController.java#L71-L105)

**目的**: 退勤打刻が正常に処理されることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java) - モック
- [AttendanceService.clockOut()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java#L151-L165) - 正常に返すようにモック設定

**検証内容**:
- HTTPステータスコードが200 OKであること
- レスポンスにsuccess=trueが含まれること
- レスポンスに適切なメッセージが含まれること
- レスポンスに記録IDが"data"オブジェクト内に含まれること
- サービスメソッドが1回呼び出されていること

#### [testClockOut_Exception()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceControllerTest.java#L232-L257)

**テスト対象メソッド**: [AttendanceController.clockOut()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceController.java#L71-L105)

**目的**: サービス層で例外が発生した場合に、適切なエラーレスポンスが返されることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java) - モック
- [AttendanceService.clockOut()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java#L151-L165) - 例外をスローするようにモック設定

**検証内容**:
- HTTPステータスコードが500 Internal Server Errorであること
- レスポンスにsuccess=falseが含まれること
- レスポンスに適切なエラーメッセージが含まれること
- サービスメソッドが1回呼び出されていること

### 3.3 勤怠記録取得テスト

#### [testGetAttendanceRecords_Success()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceControllerTest.java#L265-L284)

**テスト対象メソッド**: [AttendanceController.getAttendanceRecords()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceController.java#L107-L141)

**目的**: 勤怠記録が正常に取得できることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java) - モック
- [AttendanceService.getTodayRecords()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java#L431-L433) - テストデータを返すようにモック設定

**検証内容**:
- HTTPステータスコードが200 OKであること
- レスポンスにsuccess=trueが含まれること
- レスポンスに記録リストが"data"オブジェクト内に含まれること
- サービスメソッドが1回呼び出されていること

#### [testGetAttendanceRecords_Exception()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceControllerTest.java#L291-L308)

**テスト対象メソッド**: [AttendanceController.getAttendanceRecords()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceController.java#L107-L141)

**目的**: サービス層で例外が発生した場合に、適切なエラーレスポンスが返されることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java) - モック
- [AttendanceService.getTodayRecords()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java#L431-L433) - 例外をスローするようにモック設定

**検証内容**:
- HTTPステータスコードが500 Internal Server Errorであること
- レスポンスにsuccess=falseが含まれること
- サービスメソッドが1回呼び出されていること

### 3.4 日次サマリー取得テスト

#### [testGetDailySummary_Success()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceControllerTest.java#L316-L355)

**テスト対象メソッド**: [AttendanceController.getDailySummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceController.java#L143-L184)

**目的**: 日次サマリーが正常に取得できることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java) - モック
- [AttendanceService.getDailySummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java#L457-L483) - テストデータを返すようにモック設定

**検証内容**:
- HTTPステータスコードが200 OKであること
- レスポンスにsuccess=trueが含まれること
- レスポンスに適切なステータスが"data"オブジェクト内に含まれること
- レスポンスに勤務時間が"data"オブジェクト内に含まれること
- サービスメソッドが1回呼び出されていること

#### [testGetDailySummary_Exception()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceControllerTest.java#L363-L380)

**テスト対象メソッド**: [AttendanceController.getDailySummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceController.java#L143-L184)

**目的**: サービス層で例外が発生した場合に、適切なエラーレスポンスが返されることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java) - モック
- [AttendanceService.getDailySummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceService.java#L457-L483) - 例外をスローするようにモック設定

**検証内容**:
- HTTPステータスコードが500 Internal Server Errorであること
- レスポンスにsuccess=falseが含まれること
- サービスメソッドが1回呼び出されていること

## 4. テスト用例制作规范和技巧

### 4.1 テストの構造

各テストメソッドは以下の構造に従います：

1. **モックの設定** - サービス層の動作を模倣
2. **テスト実行** - MockMvcを使用してHTTPリクエストを送信
3. **結果の検証** - レスポンスの内容とHTTPステータスコードを確認
4. **メソッド呼び出しの検証** - サービスメソッドが適切に呼び出されたことを確認

### 4.2 モックの使用

テストでは以下のモックを使用しています：

- **@MockBean** - Springのモックビーンを使用してサービス層をモック化
- **when().thenReturn()** - 特定のメソッド呼び出しに対して事前定義された戻り値を設定
- **when().thenThrow()** - 特定のメソッド呼び出しに対して例外をスローする設定
- **verify()** - メソッドが実際に呼び出されたことを検証

### 4.3 テストデータの準備

テストデータは@BeforeEachアノテーションが付与されたsetUp()メソッドで準備されます。これにより、各テストケースで共通のテストデータを使用できます。

### 4.4 セキュリティのテスト

AttendanceControllerのエンドポイントは認証が必要なため、すべてのテストメソッドに[@WithMockUser](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceControllerTest.java#L120-L120)アノテーションを付与しています。

### 4.5 レスポンスの検証

レスポンスの検証には以下の手法を使用しています：

- **status().isOk()** - HTTPステータスコードの検証
- **jsonPath()** - JSONレスポンスの特定フィールドの値を検証
- **andExpect()** - 各検証条件をチェーンして記述

## 5. テストの実行方法

テストは以下のMavenコマンドで実行できます：

```bash
.\mvnw test -Dtest=AttendanceControllerTest
```

すべてのテストが成功すると、以下のような出力が表示されます：

```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 6. テストカバレッジ

現在のテストでは、AttendanceControllerのすべての公開メソッドについて、正常系と異常系の両方をカバーしています：

- 出勤打刻: 2テストケース (正常系1、異常系1)
- 退勤打刻: 2テストケース (正常系1、異常系1)
- 勤怠記録取得: 2テストケース (正常系1、異常系1)
- 日次サマリー取得: 2テストケース (正常系1、異常系1)

合計8テストケースで、コントローラーの主要機能を網羅的にテストしています。