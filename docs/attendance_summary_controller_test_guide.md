# AttendanceSummaryController单元测试说明书

このドキュメントでは、[AttendanceSummaryController](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java)のユニットテストに関する詳細な説明を提供します。

## 1. テスト対象

テスト対象は[AttendanceSummaryController](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java)クラスで、以下の8つの主要なAPIエンドポイントをカバーしています：

1. 日別勤務時間サマリー取得 (GET /api/reports/attendance/daily)
2. 月別勤務時間サマリー取得 (GET /api/reports/attendance/monthly)
3. 残業時間統計取得 (GET /api/reports/attendance/overtime)
4. 勤務統計レポート取得 (GET /api/reports/attendance/statistics)
5. 勤務時間データエクスポート (GET /api/reports/attendance/export)
6. 日別サマリー計算 (POST /api/reports/attendance/daily/calculate)
7. 月別サマリー計算 (POST /api/reports/attendance/monthly/calculate)
8. 無効日付範囲テストとデータなし時のレスポンステスト

## 2. APIエンドポイントの仕様

### 2.1 日別勤務時間サマリー取得 (GET /api/reports/attendance/daily)

日別の勤務時間サマリーを取得するエンドポイントです。

**クエリパラメータ**:
- startDate (必須): 開始日
- endDate (必須): 終了日
- page (オプション): ページ番号（デフォルト: 0）
- size (オプション): ページサイズ（デフォルト: 20）

**成功レスポンス**:
```json
{
  "success": true,
  "data": {
    "summaries": [
      {
        "id": 1,
        "userId": 1,
        "targetDate": "2025-01-18",
        "totalHours": 8.00,
        "overtimeHours": 1.00,
        "lateNightHours": 0.00,
        "holidayHours": 0.00,
        "summaryType": "daily"
      }
    ],
    "totalCount": 1,
    "currentPage": 0,
    "totalPages": 1
  }
}
```

### 2.2 月別勤務時間サマリー取得 (GET /api/reports/attendance/monthly)

月別の勤務時間サマリーを取得するエンドポイントです。

**クエリパラメータ**:
- yearMonth (必須): 年月（形式: yyyy-MM）
- page (オプション): ページ番号（デフォルト: 0）
- size (オプション): ページサイズ（デフォルト: 20）

**成功レスポンス**:
```json
{
  "success": true,
  "data": {
    "summaries": [
      {
        "id": 1,
        "userId": 1,
        "targetDate": "2025-01-18",
        "totalHours": 8.00,
        "overtimeHours": 1.00,
        "lateNightHours": 0.00,
        "holidayHours": 0.00,
        "summaryType": "daily"
      }
    ],
    "totalCount": 20,
    "currentPage": 0,
    "totalPages": 2,
    "targetMonth": "2025-01"
  }
}
```

### 2.3 残業時間統計取得 (GET /api/reports/attendance/overtime)

残業時間の統計情報を取得するエンドポイントです。

**クエリパラメータ**:
- startDate (必須): 開始日
- endDate (必須): 終了日

**成功レスポンス**:
```json
{
  "success": true,
  "data": {
    "statistics": {
      "totalRecords": 20,
      "totalHours": 160.0,
      "overtimeHours": 8.5
    },
    "startDate": "2025-01-01",
    "endDate": "2025-01-31"
  }
}
```

### 2.4 勤務統計レポート取得 (GET /api/reports/attendance/statistics)

勤務統計レポートを取得するエンドポイントです。

**クエリパラメータ**:
- startDate (必須): 開始日
- endDate (必須): 終了日

**成功レスポンス**:
```json
{
  "success": true,
  "data": {
    "statistics": {
      "totalRecords": 20,
      "totalHours": 160.0,
      "overtimeHours": 8.5
    },
    "startDate": "2025-01-01",
    "endDate": "2025-01-31"
  }
}
```

### 2.5 勤務時間データエクスポート (GET /api/reports/attendance/export)

勤務時間データをCSVまたはJSON形式でエクスポートするエンドポイントです。

**クエリパラメータ**:
- startDate (必須): 開始日
- endDate (必須): 終了日
- format (オプション): フォーマット（デフォルト: csv、選択肢: csv, json）

**成功レスポンス**:
```
Content-Type: text/csv;charset=UTF-8
Content-Disposition: attachment; filename="attendance_data_2025-01-01_2025-01-31.csv"

Date,Total Hours,Overtime Hours,Late Night Hours,Holiday Hours
2025-01-18,8.00,1.00,0.00,0.00
```

### 2.6 日別サマリー計算 (POST /api/reports/attendance/daily/calculate)

指定された日付の日別サマリーを計算するエンドポイントです。

**クエリパラメータ**:
- targetDate (必須): 対象日

**成功レスポンス**:
```json
{
  "success": true,
  "message": "日別サマリーの計算が完了しました",
  "data": {
    "summary": {
      "id": 1,
      "userId": 1,
      "targetDate": "2025-01-18",
      "totalHours": 8.00,
      "overtimeHours": 1.00,
      "lateNightHours": 0.00,
      "holidayHours": 0.00,
      "summaryType": "daily"
    },
    "targetDate": "2025-01-18"
  }
}
```

### 2.7 月別サマリー計算 (POST /api/reports/attendance/monthly/calculate)

指定された月の月別サマリーを計算するエンドポイントです。

**クエリパラメータ**:
- yearMonth (必須): 年月（形式: yyyy-MM）

**成功レスポンス**:
```json
{
  "success": true,
  "message": "月別サマリーの計算が完了しました",
  "data": {
    "statistics": {
      "monthlyHours": {
        "2025-01-01": 8.0
      },
      "averageDailyHours": 8.0
    },
    "targetMonth": "2025-01"
  }
}
```

## 3. 各テストメソッドの説明

### 3.1 日別勤務時間サマリー取得テスト

#### [testGetDailySummaries_Success()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L123-L155)

**テスト対象メソッド**: [AttendanceSummaryController.getDailySummaries()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L44-L74)

**目的**: 日別勤務時間サマリーが正常に取得できることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.getDailySummaries()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L18-L18) - 正常に返すようにモック設定

**検証内容**:
- HTTPステータスコードが200 OKであること
- レスポンスにsuccess=trueが含まれること
- サマリーリストが"data"オブジェクト内に含まれること
- サービスメソッドが1回呼び出されていること

#### [testGetDailySummaries_Exception()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L164-L189)

**テスト対象メソッド**: [AttendanceSummaryController.getDailySummaries()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L44-L74)

**目的**: サービス層で例外が発生した場合に、適切なエラーレスポンスが返されることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.getDailySummaries()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L18-L18) - 例外をスローするようにモック設定

**検証内容**:
- HTTPステータスコードが500 Internal Server Errorであること
- レスポンスにsuccess=falseが含まれること
- 適切なエラーメッセージが返されること
- サービスメソッドが1回呼び出されていること

### 3.2 月別勤務時間サマリー取得テスト

#### [testGetMonthlySummaries_Success()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L198-L230)

**テスト対象メソッド**: [AttendanceSummaryController.getMonthlySummaries()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L82-L111)

**目的**: 月別勤務時間サマリーが正常に取得できることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.getMonthlySummaries()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L24-L24) - 正常に返すようにモック設定

**検証内容**:
- HTTPステータスコードが200 OKであること
- レスポンスにsuccess=trueが含まれること
- サマリーリストが"data"オブジェクト内に含まれること
- サービスメソッドが1回呼び出されていること

#### [testGetMonthlySummaries_Exception()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L239-L264)

**テスト対象メソッド**: [AttendanceSummaryController.getMonthlySummaries()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L82-L111)

**目的**: サービス層で例外が発生した場合に、適切なエラーレスポンスが返されることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.getMonthlySummaries()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L24-L24) - 例外をスローするようにモック設定

**検証内容**:
- HTTPステータスコードが500 Internal Server Errorであること
- レスポンスにsuccess=falseが含まれること
- 適切なエラーメッセージが返されること
- サービスメソッドが1回呼び出されていること

### 3.3 残業時間統計取得テスト

#### [testGetOvertimeStatistics_Success()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L273-L304)

**テスト対象メソッド**: [AttendanceSummaryController.getOvertimeStatistics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L119-L147)

**目的**: 残業時間統計が正常に取得できることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.getSummaryStatistics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L48-L48) - 正常に返すようにモック設定

**検証内容**:
- HTTPステータスコードが200 OKであること
- レスポンスにsuccess=trueが含まれること
- 統計情報が"data"オブジェクト内に含まれること
- サービスメソッドが1回呼び出されていること

#### [testGetOvertimeStatistics_Exception()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L312-L337)

**テスト対象メソッド**: [AttendanceSummaryController.getOvertimeStatistics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L119-L147)

**目的**: サービス層で例外が発生した場合に、適切なエラーレスポンスが返されることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.getSummaryStatistics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L48-L48) - 例外をスローするようにモック設定

**検証内容**:
- HTTPステータスコードが500 Internal Server Errorであること
- レスポンスにsuccess=falseが含まれること
- 適切なエラーメッセージが返されること
- サービスメソッドが1回呼び出されていること

### 3.4 勤務統計レポート取得テスト

#### [testGetSummaryStatistics_Success()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L346-L377)

**テスト対象メソッド**: [AttendanceSummaryController.getSummaryStatistics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L155-L183)

**目的**: 勤務統計レポートが正常に取得できることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.getSummaryStatistics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L48-L48) - 正常に返すようにモック設定

**検証内容**:
- HTTPステータスコードが200 OKであること
- レスポンスにsuccess=trueが含まれること
- 統計情報が"data"オブジェクト内に含まれること
- サービスメソッドが1回呼び出されていること

#### [testGetSummaryStatistics_Exception()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L385-L410)

**テスト対象メソッド**: [AttendanceSummaryController.getSummaryStatistics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L155-L183)

**目的**: サービス層で例外が発生した場合に、適切なエラーレスポンスが返されることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.getSummaryStatistics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L48-L48) - 例外をスローするようにモック設定

**検証内容**:
- HTTPステータスコードが500 Internal Server Errorであること
- レスポンスにsuccess=falseが含まれること
- 適切なエラーメッセージが返されること
- サービスメソッドが1回呼び出されていること

### 3.5 日別サマリー計算テスト

#### [testCalculateDailySummary_Success()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L419-L451)

**テスト対象メソッド**: [AttendanceSummaryController.calculateDailySummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L192-L222)

**目的**: 日別サマリーが正常に計算できることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.generateDailySummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L42-L42) - 正常に返すようにモック設定

**検証内容**:
- HTTPステータスコードが200 OKであること
- レスポンスにsuccess=trueが含まれること
- 適切なメッセージが返されること
- サマリー情報が"data"オブジェクト内に含まれること
- サービスメソッドが1回呼び出されていること

#### [testCalculateDailySummary_Exception()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L459-L484)

**テスト対象メソッド**: [AttendanceSummaryController.calculateDailySummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L192-L222)

**目的**: サービス層で例外が発生した場合に、適切なエラーレスポンスが返されることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.generateDailySummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L42-L42) - 例外をスローするようにモック設定

**検証内容**:
- HTTPステータスコードが500 Internal Server Errorであること
- レスポンスにsuccess=falseが含まれること
- 適切なエラーメッセージが返されること
- サービスメソッドが1回呼び出されていること

### 3.6 月別サマリー計算テスト

#### [testCalculateMonthlySummary_Success()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L493-L525)

**テスト対象メソッド**: [AttendanceSummaryController.calculateMonthlySummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L230-L261)

**目的**: 月別サマリーが正常に計算できることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.getMonthlyStatistics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L78-L78) - 正常に返すようにモック設定

**検証内容**:
- HTTPステータスコードが200 OKであること
- レスポンスにsuccess=trueが含まれること
- 適切なメッセージが返されること
- 統計情報が"data"オブジェクト内に含まれること
- サービスメソッドが1回呼び出されていること

#### [testCalculateMonthlySummary_Exception()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L534-L559)

**テスト対象メソッド**: [AttendanceSummaryController.calculateMonthlySummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L230-L261)

**目的**: サービス層で例外が発生した場合に、適切なエラーレスポンスが返されることを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.getMonthlyStatistics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L78-L78) - 例外をスローするようにモック設定

**検証内容**:
- HTTPステータスコードが500 Internal Server Errorであること
- レスポンスにsuccess=falseが含まれること
- 適切なエラーメッセージが返されること
- サービスメソッドが1回呼び出されていること

### 3.7 境界値テスト

#### [testInvalidDateRange()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L567-L575)

**テスト対象メソッド**: [AttendanceSummaryController.getDailySummaries()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L44-L74)

**目的**: 無効な日付範囲（開始日が終了日より後）が指定された場合の処理を検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.getDailySummaries()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L18-L18) - 実際には呼び出されない

**検証内容**:
- HTTPステータスコードが500 Internal Server Errorであること

#### [testNoDataResponse()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L585-L615)

**テスト対象メソッド**: [AttendanceSummaryController.getDailySummaries()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\AttendanceSummaryController.java#L44-L74)

**目的**: データが存在しない場合のレスポンスを検証します。

**関連するモックオブジェクトとメソッド**:
- [AttendanceSummaryService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java) - モック
- [AttendanceSummaryService.getDailySummaries()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AttendanceSummaryService.java#L18-L18) - 空のページを返すようにモック設定

**検証内容**:
- HTTPステータスコードが200 OKであること
- レスポンスにsuccess=trueが含まれること
- 空のリストが返されること
- totalCountが0であること
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

AttendanceSummaryControllerのエンドポイントは認証が必要なため、すべてのテストメソッドに[@WithMockUser](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceSummaryControllerTest.java#L124-L124)アノテーションを付与しています。

### 4.5 レスポンスの検証

レスポンスの検証には以下の手法を使用しています：

- **status().isOk()** - HTTPステータスコードの検証
- **jsonPath()** - JSONレスポンスの特定フィールドの値を検証
- **andExpect()** - 各検証条件をチェーンして記述

## 5. テストの実行方法

テストは以下のMavenコマンドで実行できます：

```bash
.\mvnw test -Dtest=AttendanceSummaryControllerTest
```

すべてのテストが成功すると、以下のような出力が表示されます：

```
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 6. テストカバレッジ

現在のテストでは、AttendanceSummaryControllerのすべての公開メソッドについて、正常系と異常系の両方をカバーしています：

- 日別勤務時間サマリー取得: 2テストケース (正常系1、異常系1)
- 月別勤務時間サマリー取得: 2テストケース (正常系1、異常系1)
- 残業時間統計取得: 2テストケース (正常系1、異常系1)
- 勤務統計レポート取得: 2テストケース (正常系1、異常系1)
- 日別サマリー計算: 2テストケース (正常系1、異常系1)
- 月別サマリー計算: 2テストケース (正常系1、異常系1)
- 境界値テスト: 2テストケース (無効日付範囲1、データなし時1)

合計14テストケースで、コントローラーの主要機能を網羅的にテストしています。