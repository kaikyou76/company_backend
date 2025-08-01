# 个人和部门考勤统计功能说明

## 1. 功能概述

本文档说明了新增的两个考勤统计功能：

1. 个人考勤统计 (F-902)
2. 部门考勤统计 (F-903)

这些功能允许管理员和用户查看特定个人或部门在指定时间范围内的考勤统计数据。

## 2. API 详情

### 2.1 个人考勤统计 API

#### 端点

```
GET /api/reports/attendance/personal
```

#### 参数

| 参数名    | 类型                 | 必填 | 说明         |
| --------- | -------------------- | ---- | ------------ |
| userId    | Long                 | 是   | 用户 ID      |
| startDate | LocalDate (ISO 格式) | 是   | 统计开始日期 |
| endDate   | LocalDate (ISO 格式) | 是   | 统计结束日期 |

#### 响应示例

```json
{
  "success": true,
  "message": "个人别勤怠统计的取得が完了しました",
  "data": {
    "statistics": {
      "userId": 1,
      "totalRecords": 20,
      "totalHours": 160.0,
      "overtimeHours": 10.0,
      "lateNightHours": 0.0,
      "holidayHours": 0.0,
      "startDate": "2025-07-01",
      "endDate": "2025-07-31"
    }
  }
}
```

#### 错误响应示例

```json
{
  "success": false,
  "message": "个人别勤怠统计的取得に失敗しました"
}
```

### 2.2 部门考勤统计 API

#### 端点

```
GET /api/reports/attendance/department
```

#### 参数

| 参数名       | 类型                 | 必填 | 说明         |
| ------------ | -------------------- | ---- | ------------ |
| departmentId | Integer              | 是   | 部门 ID      |
| startDate    | LocalDate (ISO 格式) | 是   | 统计开始日期 |
| endDate      | LocalDate (ISO 格式) | 是   | 统计结束日期 |

#### 响应示例

```json
{
  "success": true,
  "message": "部门别勤怠统计的取得が完了しました",
  "data": {
    "statistics": {
      "departmentId": 1,
      "userCount": 10,
      "totalRecords": 200,
      "totalHours": 1600.0,
      "averageHoursPerUser": 160.0,
      "overtimeHours": 100.0,
      "lateNightHours": 0.0,
      "holidayHours": 0.0,
      "startDate": "2025-07-01",
      "endDate": "2025-07-31"
    }
  }
}
```

#### 错误响应示例

```json
{
  "success": false,
  "message": "部门别勤怠统计的取得に失敗しました"
}
```

## 3. 实现详情

### 3.1 服务层实现

在 [AttendanceSummaryServiceImpl](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/impl/AttendanceSummaryServiceImpl.java) 中实现了以下两个方法：

1. `getPersonalAttendanceStatistics(Long userId, LocalDate startDate, LocalDate endDate)`

   - 获取指定用户在日期范围内的所有考勤摘要
   - 计算总工时、加班时间、深夜工时和节假日工时

2. `getDepartmentAttendanceStatistics(Integer departmentId, LocalDate startDate, LocalDate endDate)`
   - 获取指定部门的所有用户
   - 获取这些用户在日期范围内的所有考勤摘要
   - 计算部门级别的统计数据，包括平均每人工作时间

### 3.2 控制器层实现

在 [AttendanceSummaryController](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/AttendanceSummaryController.java) 中添加了两个新的端点：

1. `getPersonalAttendanceStatistics()` - 处理个人考勤统计请求
2. `getDepartmentAttendanceStatistics()` - 处理部门考勤统计请求

两个端点都包含完整的日志记录和错误处理。

## 4. 测试

### 4.1 单元测试

在 [AttendanceSummaryControllerTest](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/AttendanceSummaryControllerTest.java) 中添加了以下测试用例：

1. `testGetPersonalAttendanceStatistics_Success` - 测试个人考勤统计成功场景
2. `testGetPersonalAttendanceStatistics_Exception` - 测试个人考勤统计异常场景
3. `testGetDepartmentAttendanceStatistics_Success` - 测试部门考勤统计成功场景
4. `testGetDepartmentAttendanceStatistics_Exception` - 测试部门考勤统计异常场景

所有测试均已通过，验证了功能的正确性和健壮性。

## 5. 使用示例

### 5.1 获取个人考勤统计

```bash
curl -X GET "http://localhost:8080/api/reports/attendance/personal?userId=1&startDate=2025-07-01&endDate=2025-07-31"
```

### 5.2 获取部门考勤统计

```bash
curl -X GET "http://localhost:8080/api/reports/attendance/department?departmentId=1&startDate=2025-07-01&endDate=2025-07-31"
```

## 6. 权限和安全

这些端点继承了系统的安全配置，需要有效的身份验证才能访问。根据系统安全策略，只有具有适当权限的用户（如管理员或经理）才能访问其他用户或部门的统计信息。

## 7. 性能考虑

对于大型部门或长日期范围的查询，可能需要考虑性能优化，例如：

- 添加数据库索引
- 实现分页或限制日期范围
- 使用缓存机制

## 8. 未来改进

可能的未来改进包括：

- 添加更多统计维度（如按职位、按工作类型等）
- 支持导出功能（CSV、Excel 等格式）
- 添加图表可视化功能
- 实现实时统计更新
