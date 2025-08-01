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

```
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

# 考勤管理控制器测试说明

## 1. 测试概述

### 1.1 测试目标

本测试旨在验证考勤管理控制器（[AttendanceController](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/AttendanceController.java)）的各个API端点是否按预期工作，确保：
1. 所有API端点都能正确处理请求和返回响应
2. 位置验证功能正常工作
3. 数据验证和错误处理机制正常工作
4. 服务层交互正确

### 1.2 测试范围

测试覆盖以下API端点：
- `POST /api/attendance/clock-in` - 出勤打卡
- `POST /api/attendance/clock-out` - 退勤打卡
- `GET /api/attendance/records` - 获取考勤记录
- `GET /api/attendance/daily-summary` - 获取日次汇总

### 1.3 被测试的文件

- 控制器文件：[AttendanceController.java](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/AttendanceController.java)
- 测试文件：[AttendanceControllerTest.java](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/AttendanceControllerTest.java)
- 服务文件：[AttendanceService.java](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/AttendanceService.java)
- 实体文件：[AttendanceRecord.java](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/entity/AttendanceRecord.java)

## 2. 测试方法详解

### 2.1 测试类结构

[AttendanceControllerTest](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/AttendanceControllerTest.java)类使用Spring Boot的测试框架，采用以下关键注解和配置：

- `@WebMvcTest(AttendanceController.class)` - 仅加载Web层组件进行测试
- `@MockBean` - 模拟服务层依赖
- `@WithMockUser` - 模拟用户认证信息
- `MockMvc` - 模拟HTTP请求和验证响应

### 2.2 测试的具体方法

#### 2.2.1 出勤打卡测试 ([clockIn](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/AttendanceController.java#L50-L83))

测试方法：
- `testClockIn_Success` - 测试成功出勤打卡
- `testClockIn_InvalidLocation` - 测试出勤打卡位置验证失败
- `testClockIn_SkipLocationCheck` - 测试跳过位置检查的出勤打卡

被测试的控制器方法：
- [AttendanceController.clockIn()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/AttendanceController.java#L50-L83)

模拟的服务方法：
- [AttendanceService.clockIn()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/AttendanceService.java#L78-L139)

#### 2.2.2 退勤打卡测试 ([clockOut](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/AttendanceController.java#L91-L124))

测试方法：
- `testClockOut_Success` - 测试成功退勤打卡
- `testClockOut_InvalidLocation` - 测试退勤打卡位置验证失败
- `testClockOut_SkipLocationCheck` - 测试跳过位置检查的退勤打卡

被测试的控制器方法：
- [AttendanceController.clockOut()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/AttendanceController.java#L91-L124)

模拟的服务方法：
- [AttendanceService.clockOut()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/AttendanceService.java#L142-L199)

#### 2.2.3 获取考勤记录测试 ([getAttendanceRecords](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/AttendanceController.java#L132-L157))

测试方法：
- `testGetAttendanceRecords_Success` - 测试成功获取考勤记录
- `testGetAttendanceRecords_Exception` - 测试获取考勤记录时服务异常

被测试的控制器方法：
- [AttendanceController.getAttendanceRecords()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/AttendanceController.java#L132-L157)

模拟的服务方法：
- [AttendanceService.getTodayRecords()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/AttendanceService.java#L283-L285)

#### 2.2.4 获取日次汇总测试 ([getDailySummary](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/AttendanceController.java#L165-L189))

测试方法：
- `testGetDailySummary_Success` - 测试成功获取日次汇总
- `testGetDailySummary_Exception` - 测试获取日次汇总时服务异常

被测试的控制器方法：
- [AttendanceController.getDailySummary()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/AttendanceController.java#L165-L189)

模拟的服务方法：
- [AttendanceService.getDailySummary()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/AttendanceService.java#L334-L360)

## 3. 测试规范和技巧

### 3.1 测试规范

1. **使用@WebMvcTest进行切片测试**
   - 仅加载Web层相关组件，提高测试效率
   - 避免加载整个Spring上下文

2. **使用@MockBean模拟依赖**
   - 隔离被测试组件与其他组件的依赖关系
   - 可以控制模拟对象的行为和返回值

3. **使用@WithMockUser模拟认证**
   - 模拟用户访问API
   - 验证权限控制是否正确

4. **遵循Given-When-Then模式**
   - Given: 准备测试数据和模拟行为
   - When: 执行被测试的方法
   - Then: 验证结果是否符合预期

### 3.2 测试技巧

1. **全面覆盖成功和失败场景**
   - 为每个API端点编写成功场景测试
   - 编写各种失败场景测试（验证错误、权限错误等）

2. **验证关键响应数据**
   - 检查HTTP状态码是否正确
   - 验证响应结构和关键字段值
   - 确保错误消息清晰明确

3. **使用真实数据**
   - 使用合理的经纬度坐标
   - 模拟真实业务场景的数据

4. **验证方法调用**
   - 使用Mockito的verify方法验证服务层方法是否被正确调用
   - 验证方法调用次数是否符合预期

## 4. 运行测试

使用Maven命令运行测试：

```
./mvnw test -Dtest=AttendanceControllerTest
```

或者运行所有测试：

```
./mvnw test
```

## 5. 测试结果分析

所有测试通过表明：
1. 控制器方法能正确处理请求和响应
2. 位置验证功能按预期工作
3. 数据验证和错误处理机制正常
4. 与服务层的交互正确

如果测试失败，应检查：
1. API端点路径是否正确
2. 请求参数和响应结构是否匹配
3. 模拟的服务方法行为是否正确
4. 位置验证逻辑是否正确实现
