# BatchController 单元测试说明书

## 1. 测试概述

### 1.1 测试目标

本测试旨在验证批处理控制器（BatchController）的各个API端点是否按预期工作，确保：
1. 所有API端点都能正确处理请求和返回响应
2. 数据验证和错误处理机制正常工作
3. 权限控制机制正确实施
4. 与批处理服务层的交互正确

### 1.2 测试范围

测试覆盖以下API端点：
- `POST /api/batch/monthly-summary` - 月次勤怠集計バッチ実行
- `POST /api/batch/update-paid-leave` - 有給日数更新バッチ実行
- `POST /api/batch/cleanup-data` - データクリーンアップバッチ実行
- `POST /api/batch/repair-data` - データ修復バッチ実行
- `GET /api/batch/status` - バッチステータス取得

### 1.3 被测试的文件

- 控制器文件：BatchController.java
- 测试文件：BatchControllerTest.java
- 服务文件：BatchJobService.java, BatchStatusService.java
- DTO文件：BatchResponseDto.java

## 2. 测试方法详解

### 2.1 测试类结构

BatchControllerTest类使用Spring Boot的测试框架，采用以下关键注解和配置：

- `@WebMvcTest(BatchController.class)` - 仅加载Web层组件进行测试
- `@MockBean` - 模拟服务层依赖
- `@WithMockUser` - 模拟用户认证信息
- `MockMvc` - 模拟HTTP请求和验证响应

### 2.2 测试的具体方法

#### 2.2.1 月次勤怠集計バッチ実行 ([executeMonthlySummaryBatch](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchController.java#L58-L103))

执行月次勤怠集計バッチ作业。

**请求参数**:
- yearMonth (可选): 集计年月（默认为上个月）

**成功响应**:
```json
{
  "success": true,
  "message": "月次勤怠集計バッチを実行しました",
  "data": {
    "targetMonth": "2025-01",
    "processedCount": 156,
    "userCount": 25,
    "totalWorkDays": 520,
    "totalWorkTime": 83200,
    "totalOvertimeTime": 6800
  },
  "executedAt": "2025-01-18T10:30:00+09:00"
}
```

#### 2.2.2 有給日数更新バッチ実行 ([executePaidLeaveUpdateBatch](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchController.java#L114-L157))

执行有給日数更新バッチ作业。

**请求参数**:
- fiscalYear (可选): 会计年度（默认为当前年度）

**成功响应**:
```json
{
  "success": true,
  "message": "有給日数更新バッチを実行しました",
  "data": {
    "fiscalYear": 2025,
    "updatedCount": 48,
    "totalUserCount": 50,
    "successRate": 96.0,
    "errorCount": 2,
    "errorMessages": [
      "ユーザーID: 15, エラー: 入社日が未設定です"
    ]
  },
  "executedAt": "2025-01-18T10:30:00+09:00"
}
```

#### 2.2.3 データクリーンアップバッチ実行 ([executeDataCleanupBatch](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchController.java#L167-L209))

执行数据清理批处理作业。

**请求参数**:
- retentionMonths (可选): 保存月数（默认为12个月）

**成功响应**:
```json
{
  "success": true,
  "message": "データクリーンアップバッチを実行しました",
  "data": {
    "retentionMonths": 12,
    "cutoffDate": "2024-01-18",
    "deletedCount": 1250,
    "deletedDetails": {
      "attendanceRecords": 1200,
      "auditLogs": 50
    }
  },
  "executedAt": "2025-01-18T10:30:00+09:00"
}
```

#### 2.2.4 データ修復バッチ実行 ([executeDataRepairBatch](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchController.java#L219-L255))

执行数据修复批处理作业。

**请求参数**:
- 无参数

**成功响应**:
```json
{
  "success": true,
  "message": "データ修復バッチを実行しました",
  "data": {
    "repairedCount": 8,
    "repairedItems": [
      "勤怠記録ID=12345: 退勤時刻を2025-01-17T18:00:00に設定",
      "勤怠記録ID=12346: 退勤時刻を2025-01-16T17:30:00に設定"
    ]
  },
  "executedAt": "2025-01-18T10:30:00+09:00"
}
```

#### 2.2.5 バッチステータス取得 ([getBatchStatus](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchController.java#L265-L275))

获取批处理作业的当前状态。

**成功响应**:
```json
{
  "success": true,
  "message": "バッチ処理ステータスを取得しました",
  "data": {
    "systemStatus": "HEALTHY",
    "lastChecked": "2025-01-18T10:30:00+09:00",
    "uptime": "5 days, 12 hours",
    "databaseStatus": {
      "totalUsers": 50,
      "activeUsers": 48,
      "totalAttendanceRecords": 12450,
      "latestRecordDate": "2025-01-18"
    },
    "dataStatistics": {
      "currentMonthRecords": 520,
      "incompleteRecords": 2
    },
    "recentBatchExecutions": [
      {
        "type": "MONTHLY_SUMMARY",
        "executedAt": "2025-01-01T02:00:00+09:00",
        "status": "SUCCESS",
        "duration": "45 seconds"
      },
      {
        "type": "CLEANUP_DATA",
        "executedAt": "2024-12-31T01:00:00+09:00",
        "status": "SUCCESS",
        "duration": "2 minutes"
      }
    ]
  }
}
```

## 3. 各测试方法的说明

### 3.1 月次勤怠集計バッチ実行测试

#### [testExecuteMonthlySummaryBatch_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchControllerTest.java#L36-L50)

**测试目标方法**: [BatchController.executeMonthlySummaryBatch()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchController.java#L58-L103)

**目的**: 验证管理员可以成功执行月次勤怠集計バッチ。

**相关的模拟对象和方法**:
- [BatchJobService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/batch/service/BatchJobService.java) - 模拟
- [BatchJobService.runJob()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/batch/service/BatchJobService.java#L28-L50) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 包含适当的成功消息

#### [testExecuteMonthlySummaryBatch_Exception()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchControllerTest.java#L52-L69)

**测试目标方法**: [BatchController.executeMonthlySummaryBatch()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchController.java#L58-L103)

**目的**: 验证当批处理执行失败时返回适当的错误响应。

**相关的模拟对象和方法**:
- [BatchJobService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/batch/service/BatchJobService.java) - 模拟
- [BatchJobService.runJob()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/batch/service/BatchJobService.java#L28-L50) - 设置为抛出异常

**验证内容**:
- HTTP状态码为500 Internal Server Error
- 响应中包含success=false
- 包含适当的错误消息

### 3.2 有給日数更新バッチ実行测试

#### [testExecutePaidLeaveUpdateBatch_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchControllerTest.java#L71-L84)

**测试目标方法**: [BatchController.executePaidLeaveUpdateBatch()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchController.java#L114-L157)

**目的**: 验证管理员可以成功执行有給日数更新バッチ。

**相关的模拟对象和方法**:
- [BatchJobService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/batch/service/BatchJobService.java) - 模拟
- [BatchJobService.runJob()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/batch/service/BatchJobService.java#L28-L50) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 包含适当的更新成功消息

### 3.3 データクリーンアップバッチ実行测试

#### [testExecuteDataCleanupBatch_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchControllerTest.java#L86-L99)

**测试目标方法**: [BatchController.executeDataCleanupBatch()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchController.java#L167-L209)

**目的**: 验证管理员可以成功执行数据清理批处理。

**相关的模拟对象和方法**:
- [BatchJobService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/batch/service/BatchJobService.java) - 模拟
- [BatchJobService.runJob()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/batch/service/BatchJobService.java#L28-L50) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 包含适当的数据清理成功消息

### 3.4 データ修復バッチ実行测试

#### [testExecuteDataRepairBatch_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchControllerTest.java#L101-L111)

**测试目标方法**: [BatchController.executeDataRepairBatch()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchController.java#L219-L255)

**目的**: 验证管理员可以成功执行数据修复批处理。

**相关的模拟对象和方法**:
- [BatchJobService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/batch/service/BatchJobService.java) - 模拟
- [BatchJobService.runJob()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/batch/service/BatchJobService.java#L28-L50) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 包含适当的数据修复成功消息

### 3.5 バッチステータス取得测试

#### [testGetBatchStatus_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchControllerTest.java#L113-L123)

**测试目标方法**: [BatchController.getBatchStatus()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchController.java#L265-L275)

**目的**: 验证管理员可以成功获取批处理状态信息。

**相关的模拟对象和方法**:
- [BatchStatusService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchStatusService.java) - 模拟
- [BatchStatusService.getBatchStatus()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchStatusService.java#L12-L44) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK

### 3.6 权限测试

#### [testExecuteMonthlySummaryBatch_Forbidden()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchControllerTest.java#L125-L137)

**测试目标方法**: [BatchController.executeMonthlySummaryBatch()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchController.java#L58-L103)

**目的**: 验证非管理员用户无法执行批处理作业。

**相关的模拟对象和方法**:
- 无（权限控制由Spring Security处理）

**验证内容**:
- HTTP状态码为403 FORBIDDEN

#### [testGetBatchStatus_Forbidden()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchControllerTest.java#L139-L147)

**测试目标方法**: [BatchController.getBatchStatus()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchController.java#L265-L275)

**目的**: 验证非管理员用户无法获取批处理状态。

**相关的模拟对象和方法**:
- 无（权限控制由Spring Security处理）

**验证内容**:
- HTTP状态码为403 FORBIDDEN

## 4. 测试执行结果

所有测试均已通过，验证了BatchController的正确性和健壮性。

```bash
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 5. 测试覆盖率

当前测试涵盖了BatchController的所有公开方法，包括正常系、异常系和权限测试：

- 月次勤怠集計バッチ実行: 2测试用例 (正常系1、异常系1)
- 有給日数更新バッチ実行: 1测试用例 (正常系)
- データクリーンアップバッチ実行: 1测试用例 (正常系)
- データ修復バッチ実行: 1测试用例 (正常系)
- バッチステータス取得: 1测试用例 (正常系)
- 权限测试: 2测试用例 (安全测试)

合计8个测试用例，全面覆盖了控制器的主要功能。