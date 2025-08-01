# SystemLogController 单元测试说明书

## 1. 测试概述

### 1.1 测试目标

本测试旨在验证系统日志管理控制器（[SystemLogController](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java)）的各个API端点是否按预期工作，确保：
1. 所有API端点都能正确处理请求和返回响应
2. 数据验证和错误处理机制正常工作
3. 权限控制机制正确实施
4. 与数据访问层的交互正确

### 1.2 测试范围

测试覆盖以下API端点：
- `GET /api/system-logs` - 获取系统日志列表
- `GET /api/system-logs/{id}` - 根据ID获取系统日志详情
- `DELETE /api/system-logs/{id}` - 删除系统日志
- `GET /api/system-logs/export/csv` - 导出系统日志（CSV格式）
- `GET /api/system-logs/export/json` - 导出系统日志（JSON格式）
- `GET /api/system-logs/statistics` - 获取系统日志统计信息
- `GET /api/system-logs/search` - 搜索系统日志

### 1.3 被测试的文件

- 控制器文件：[SystemLogController.java](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java)
- 测试文件：[SystemLogControllerTest.java](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/SystemLogControllerTest.java)
- 仓库文件：[SystemLogRepository.java](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java)
- 实体文件：[SystemLog.java](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/entity/SystemLog.java)

## 2. 测试方法详解

### 2.1 测试类结构

[SystemLogControllerTest](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/SystemLogControllerTest.java)类使用Spring Boot的测试框架，采用以下关键注解和配置：

- `@WebMvcTest(SystemLogController.class)` - 仅加载Web层组件进行测试
- `@MockBean` - 模拟数据访问层依赖
- `@WithMockUser` - 模拟用户认证信息
- `MockMvc` - 模拟HTTP请求和验证响应

### 2.2 测试的具体方法

#### 2.2.1 获取系统日志列表 ([getSystemLogs](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L56-L104))

获取系统日志的分页列表，支持过滤和排序功能。

**请求参数**:
- page (可选): 页码（默认0）
- size (可选): 每页大小（默认20）
- action (可选): 动作过滤器
- status (可选): 状态过滤器
- startDate (可选): 开始日期时间
- endDate (可选): 结束日期时间

**成功响应**:
```json
{
  "success": true,
  "message": "システムログ一覧の取得が完了しました",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "action": "LOGIN",
      "status": "success",
      "ipAddress": "192.168.1.1",
      "userAgent": "Mozilla/5.0",
      "details": "Login successful",
      "createdAt": "2025-01-18T10:30:00+09:00"
    }
  ],
  "currentPage": 0,
  "totalItems": 1,
  "totalPages": 1
}
```

#### 2.2.2 获取系统日志详情 ([getSystemLogById](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L114-L152))

根据ID获取特定系统日志的详细信息。

**路径参数**:
- id: 系统日志ID

**成功响应**:
```json
{
  "success": true,
  "message": "システムログ詳細の取得が完了しました",
  "data": {
    "id": 1,
    "userId": 1,
    "action": "LOGIN",
    "status": "success",
    "ipAddress": "192.168.1.1",
    "userAgent": "Mozilla/5.0",
    "details": "Login successful",
    "createdAt": "2025-01-18T10:30:00+09:00"
  }
}
```

#### 2.2.3 删除系统日志 ([deleteSystemLog](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L162-L203))

根据ID删除特定的系统日志。

**路径参数**:
- id: 系统日志ID

**成功响应**:
```json
{
  "success": true,
  "message": "システムログを削除しました"
}
```

#### 2.2.4 导出系统日志（CSV格式）([exportSystemLogsCsv](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L215-L273))

以CSV格式导出系统日志数据。

**请求参数**:
- action (可选): 动作过滤器
- status (可选): 状态过滤器
- startDate (可选): 开始日期时间
- endDate (可选): 结束日期时间

**成功响应**:
```
Content-Type: text/csv; charset=UTF-8
Content-Disposition: form-data; name="attachment"; filename="system_logs.csv"

ID,ユーザーID,アクション,ステータス,IPアドレス,ユーザーエージェント,詳細,作成日時
1,1,LOGIN,success,192.168.1.1,Mozilla/5.0,Login successful,2025-01-18T10:30:00+09:00
```

#### 2.2.5 导出系统日志（JSON格式）([exportSystemLogsJson](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L285-L338))

以JSON格式导出系统日志数据。

**请求参数**:
- action (可选): 动作过滤器
- status (可选): 状态过滤器
- startDate (可选): 开始日期时间
- endDate (可选): 结束日期时间

**成功响应**:
```json
{
  "success": true,
  "message": "システムログのエクスポートが完了しました",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "action": "LOGIN",
      "status": "success",
      "ipAddress": "192.168.1.1",
      "userAgent": "Mozilla/5.0",
      "details": "Login successful",
      "createdAt": "2025-01-18T10:30:00+09:00"
    }
  ],
  "exportedAt": "2025-01-18T10:30:00+09:00",
  "count": 1
}
```

#### 2.2.6 获取系统日志统计信息 ([getSystemLogStatistics](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L349-L415))

获取系统日志的各种统计信息。

**请求参数**:
- startDate (可选): 开始日期时间
- endDate (可选): 结束日期时间

**成功响应**:
```json
{
  "success": true,
  "message": "システムログ統計情報の取得が完了しました",
  "data": {
    "actionStats": [
      {
        "action": "LOGIN",
        "count": 5
      }
    ],
    "statusStats": [
      {
        "status": "success",
        "count": 5
      }
    ],
    "userStats": [
      {
        "userId": 1,
        "count": 5
      }
    ],
    "dateStats": [
      {
        "date": "2025-01-18",
        "count": 5
      }
    ]
  },
  "period": {
    "startDate": "2024-12-19T10:30:00+09:00",
    "endDate": "2025-01-18T10:30:00+09:00"
  }
}
```

#### 2.2.7 搜索系统日志 ([searchSystemLogs](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L427-L476))

根据关键词搜索系统日志。

**请求参数**:
- keyword: 搜索关键词
- page (可选): 页码（默认0）
- size (可选): 每页大小（默认20）

**成功响应**:
```json
{
  "success": true,
  "message": "システムログの検索が完了しました",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "action": "LOGIN",
      "status": "success",
      "ipAddress": "192.168.1.1",
      "userAgent": "Mozilla/5.0",
      "details": "Login successful",
      "createdAt": "2025-01-18T10:30:00+09:00"
    }
  ],
  "currentPage": 0,
  "totalItems": 1,
  "totalPages": 1,
  "keyword": "LOGIN"
}
```

## 3. 各测试方法的说明

### 3.1 系统日志列表获取测试

#### [testGetSystemLogs_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/SystemLogControllerTest.java#L127-L162)

**测试目标方法**: [SystemLogController.getSystemLogs()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L56-L104)

**目的**: 验证管理员可以成功获取系统日志列表。

**相关的模拟对象和方法**:
- [SystemLogRepository](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java) - 模拟
- [SystemLogRepository.findAll()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L30-L30) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 日志列表在"data"对象中
- 服务方法被调用一次

#### [testGetSystemLogs_WithBoundaryPageParameters()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/SystemLogControllerTest.java#L513-L537)

**测试目标方法**: [SystemLogController.getSystemLogs()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L56-L104)

**目的**: 验证使用边界值分页参数时也能正确处理。

**相关的模拟对象和方法**:
- [SystemLogRepository](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java) - 模拟
- [SystemLogRepository.findAll()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L30-L30) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 服务方法被调用一次

### 3.2 系统日志详情获取测试

#### [testGetSystemLogById_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/SystemLogControllerTest.java#L173-L204)

**测试目标方法**: [SystemLogController.getSystemLogById()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L114-L152)

**目的**: 验证管理员可以成功获取指定ID的系统日志详情。

**相关的模拟对象和方法**:
- [SystemLogRepository](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java) - 模拟
- [SystemLogRepository.findById()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L30-L30) - 设置为返回指定日志

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 指定日志在"data"对象中
- 服务方法被调用一次

#### [testGetSystemLogById_NotFound()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/SystemLogControllerTest.java#L216-L245)

**测试目标方法**: [SystemLogController.getSystemLogById()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L114-L152)

**目的**: 验证当请求不存在的系统日志时返回404错误。

**相关的模拟对象和方法**:
- [SystemLogRepository](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java) - 模拟
- [SystemLogRepository.findById()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L30-L30) - 设置为返回空Optional

**验证内容**:
- HTTP状态码为404 NOT FOUND
- 响应中包含success=false
- 包含适当的错误消息
- 服务方法被调用一次

### 3.3 系统日志删除测试

#### [testDeleteSystemLog_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/SystemLogControllerTest.java#L256-L288)

**测试目标方法**: [SystemLogController.deleteSystemLog()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L162-L203)

**目的**: 验证管理员可以成功删除指定的系统日志。

**相关的模拟对象和方法**:
- [SystemLogRepository](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java) - 模拟
- [SystemLogRepository.findById()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L30-L30) - 设置为返回指定日志
- [SystemLogRepository.deleteById()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L30-L30) - 模拟删除操作

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 包含适当的成功消息
- 服务方法被正确调用

#### [testDeleteSystemLog_NotFound()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/SystemLogControllerTest.java#L300-L332)

**测试目标方法**: [SystemLogController.deleteSystemLog()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L162-L203)

**目的**: 验证当尝试删除不存在的系统日志时返回404错误。

**相关的模拟对象和方法**:
- [SystemLogRepository](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java) - 模拟
- [SystemLogRepository.findById()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L30-L30) - 设置为返回空Optional

**验证内容**:
- HTTP状态码为404 NOT FOUND
- 响应中包含success=false
- 包含适当的错误消息
- deleteById方法未被调用

### 3.4 系统日志导出测试

#### [testExportSystemLogsCsv_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/SystemLogControllerTest.java#L344-L369)

**测试目标方法**: [SystemLogController.exportSystemLogsCsv()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L215-L273)

**目的**: 验证管理员可以成功以CSV格式导出系统日志。

**相关的模拟对象和方法**:
- [SystemLogRepository](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java) - 模拟
- [SystemLogRepository.findForExport()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L319-L330) - 设置为返回日志列表

**验证内容**:
- HTTP状态码为200 OK
- Content-Type为text/csv
- 响应体包含CSV数据
- 服务方法被调用一次

#### [testExportSystemLogsJson_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/SystemLogControllerTest.java#L381-L411)

**测试目标方法**: [SystemLogController.exportSystemLogsJson()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L285-L338)

**目的**: 验证管理员可以成功以JSON格式导出系统日志。

**相关的模拟对象和方法**:
- [SystemLogRepository](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java) - 模拟
- [SystemLogRepository.findForExport()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L319-L330) - 设置为返回日志列表

**验证内容**:
- HTTP状态码为200 OK
- Content-Type为application/json
- 响应中包含success=true
- 日志数据在"data"对象中
- 服务方法被调用一次

### 3.5 系统日志统计信息获取测试

#### [testGetSystemLogStatistics_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/SystemLogControllerTest.java#L423-L464)

**测试目标方法**: [SystemLogController.getSystemLogStatistics()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L349-L415)

**目的**: 验证管理员可以成功获取系统日志统计信息。

**相关的模拟对象和方法**:
- [SystemLogRepository](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java) - 模拟
- [SystemLogRepository.countByActionGrouped()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L339-L350) - 设置为返回统计数据
- [SystemLogRepository.countByStatusGrouped()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L359-L370) - 设置为返回统计数据
- [SystemLogRepository.countByUserGrouped()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L379-L391) - 设置为返回统计数据
- [SystemLogRepository.countByDateGrouped()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L401-L413) - 设置为返回统计数据

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 统计数据在"data"对象中
- 服务方法被调用四次

### 3.6 系统日志搜索测试

#### [testSearchSystemLogs_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/SystemLogControllerTest.java#L475-L506)

**测试目标方法**: [SystemLogController.searchSystemLogs()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java#L427-L476)

**目的**: 验证管理员可以成功搜索系统日志。

**相关的模拟对象和方法**:
- [SystemLogRepository](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java) - 模拟
- [SystemLogRepository.searchByKeyword()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/SystemLogRepository.java#L423-L434) - 设置为返回搜索结果

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 搜索结果在"data"对象中
- 服务方法被调用一次

### 3.7 权限测试

#### [testAccessDenied_ForNonAdminUser()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/SystemLogControllerTest.java#L547-L557)

**测试目标方法**: 所有需要管理员权限的[SystemLogController](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java)方法

**目的**: 验证非管理员用户无法访问需要管理员权限的端点。

**相关的模拟对象和方法**:
- 无（权限控制由Spring Security处理）

**验证内容**:
- HTTP状态码为403 FORBIDDEN

## 4. 测试执行结果

所有测试均已通过，验证了[SystemLogController](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java)的正确性和健壮性。

```bash
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 5. 测试覆盖率

当前测试涵盖了[SystemLogController](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/SystemLogController.java)的所有公开方法，包括正常系、异常系和边界值测试：

- 系统日志列表获取: 2测试用例 (正常系1、边界值1)
- 系统日志详情获取: 2测试用例 (正常系1、异常系1)
- 系统日志删除: 2测试用例 (正常系1、异常系1)
- 系统日志CSV导出: 1测试用例 (正常系)
- 系统日志JSON导出: 1测试用例 (正常系)
- 系统日志统计信息获取: 1测试用例 (正常系)
- 系统日志搜索: 1测试用例 (正常系)
- 权限测试: 1测试用例 (安全测试)

合计11个测试用例，全面覆盖了控制器的主要功能。

