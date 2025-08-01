# BatchManagementController 单元测试说明书

## 1. 测试概述

### 1.1 测试目标

本测试旨在验证批处理管理控制器（BatchManagementController）的各个API端点是否按预期工作，确保：
1. 所有API端点都能正确处理请求和返回响应
2. 数据验证和错误处理机制正常工作
3. 与批处理监控服务层的交互正确

注意：由于使用standaloneSetup测试配置，Spring Security的@PreAuthorize注解不会生效，
因此权限控制测试不在此测试范围内。

### 1.2 测试范围

测试覆盖以下API端点：
- `GET /api/v1/batch/instances` - 全ジョブインスタンス取得
- `GET /api/v1/batch/executions/{jobName}` - ジョブ実行履歴取得
- `GET /api/v1/batch/steps/{jobExecutionId}` - ステップ実行履歴取得
- `GET /api/v1/batch/statistics` - バッチ実行統計取得
- `GET /api/v1/batch/running` - 実行中ジョブ取得
- `GET /api/v1/batch/job-names` - ジョブ名一覧取得
- `GET /api/v1/batch/latest/{jobName}` - 特定ジョブの最新実行情報取得

### 1.3 被测试的文件

- 控制器文件：BatchManagementController.java
- 测试文件：BatchManagementControllerTest.java
- 服务文件：BatchMonitoringService.java

## 2. 测试方法详解

### 2.1 测试类结构

BatchManagementControllerTest类使用Spring Boot的测试框架，采用以下关键注解和配置：

- `@ExtendWith(MockitoExtension.class)` - 使用Mockito扩展进行单元测试
- `@InjectMocks` - 注入被测试的控制器实例
- `@Mock` - 模拟服务层依赖
- `MockMvc` - 模拟HTTP请求和验证响应，使用standaloneSetup配置

注意：测试使用standaloneSetup而不是@WebMvcTest，因为这样可以更好地控制依赖注入。

### 2.2 测试的具体方法

#### 2.2.1 全ジョブインスタンス取得 ([getJobInstances](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L37-L54))

获取所有批处理作业实例信息，使用batch_job_instance表。

**成功响应**:
```json
{
  "success": true,
  "totalCount": 1,
  "instances": [
    {
      "jobInstanceId": 1,
      "jobName": "testJob"
    }
  ]
}
```

**错误响应**:
```json
{
  "success": false,
  "message": "ジョブインスタンス取得に失敗しました: Database error"
}
```

#### 2.2.2 ジョブ実行履歴取得 ([getJobExecutionHistory](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L62-L86))

根据作业名称获取批处理作业执行历史，使用batch_job_execution表。

**请求参数**:
- jobName (路径参数): 作业名称
- page (可选查询参数): 页码（默认0）
- size (可选查询参数): 每页大小（默认20）

**成功响应**:
```json
{
  "success": true,
  "jobName": "testJob",
  "page": 0,
  "size": 20,
  "totalCount": 1,
  "executions": [
    {
      "jobExecutionId": 1,
      "jobInstanceId": 1,
      "status": "COMPLETED"
    }
  ]
}
```

**错误响应**:
```json
{
  "success": false,
  "message": "ジョブ実行履歴取得に失敗しました: Database error"
}
```

#### 2.2.3 ステップ実行履歴取得 ([getStepExecutionHistory](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L94-L116))

根据作业执行ID获取步骤执行历史，使用batch_step_execution表。

**请求参数**:
- jobExecutionId (路径参数): 作业执行ID

**成功响应**:
```json
{
  "success": true,
  "jobExecutionId": 1,
  "totalCount": 1,
  "steps": [
    {
      "stepExecutionId": 1,
      "stepName": "testStep",
      "status": "COMPLETED"
    }
  ]
}
```

**错误响应**:
```json
{
  "success": false,
  "message": "ステップ実行履歴取得に失敗しました: Database error"
}
```

#### 2.2.4 バッチ実行統計取得 ([getBatchStatistics](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L123-L139))

获取批处理执行统计信息。

**成功响应**:
```json
{
  "success": true,
  "statistics": {
    "totalJobs": 5,
    "successRate": 100.0,
    "errorRate": 0.0
  }
}
```

**错误响应**:
```json
{
  "success": false,
  "message": "バッチ統計取得に失敗しました: Database error"
}
```

#### 2.2.5 実行中ジョブ取得 ([getRunningJobs](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L146-L163))

获取当前正在运行的批处理作业。

**成功响应**:
```json
{
  "success": true,
  "totalCount": 1,
  "runningJobs": [
    {
      "jobExecutionId": 1,
      "jobName": "runningJob",
      "status": "STARTED"
    }
  ]
}
```

**错误响应**:
```json
{
  "success": false,
  "message": "実行中ジョブ取得に失敗しました: Database error"
}
```

#### 2.2.6 ジョブ名一覧取得 ([getJobNames](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L170-L187))

获取所有批处理作业名称列表。

**成功响应**:
```json
{
  "success": true,
  "totalCount": 3,
  "jobNames": [
    "job1",
    "job2", 
    "job3"
  ]
}
```

**错误响应**:
```json
{
  "success": false,
  "message": "ジョブ名一覧取得に失敗しました: Database error"
}
```

#### 2.2.7 特定ジョブの最新実行情報取得 ([getLatestJobExecution](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L195-L213))

获取指定作业的最新执行信息。

**请求参数**:
- jobName (路径参数): 作业名称

**成功响应**:
```json
{
  "success": true,
  "jobName": "testJob",
  "latestExecution": {
    "jobExecutionId": 1,
    "jobName": "testJob",
    "status": "COMPLETED"
  }
}
```

**错误响应**:
```json
{
  "success": false,
  "message": "最新ジョブ実行情報取得に失敗しました: Database error"
}
```

## 3. 各测试方法的说明

### 3.1 全ジョブインスタンス取得测试

#### [testGetJobInstances_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L44-L63)

**测试目标方法**: [BatchManagementController.getJobInstances()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L37-L54)

**目的**: 验证可以成功获取所有批处理作业实例。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getAllJobInstances()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L57-L72) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 返回正确的作业实例数据

#### [testGetJobInstances_Exception()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L69-L82)

**测试目标方法**: [BatchManagementController.getJobInstances()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L37-L54)

**目的**: 验证当服务层发生异常时能正确返回错误响应。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getAllJobInstances()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L57-L72) - 设置为抛出异常

**验证内容**:
- HTTP状态码为500 Internal Server Error
- 响应中包含success=false
- 包含适当的错误消息

### 3.2 ジョブ実行履歴取得测试

#### [testGetJobExecutionHistory_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L88-L112)

**测试目标方法**: [BatchManagementController.getJobExecutionHistory()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L62-L86)

**目的**: 验证可以成功获取指定作业的执行历史。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getJobExecutionHistory()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L80-L97) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 返回正确的作业执行历史数据
- 参数正确传递给服务层

#### [testGetJobExecutionHistory_Exception()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L118-L133)

**测试目标方法**: [BatchManagementController.getJobExecutionHistory()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L62-L86)

**目的**: 验证当服务层发生异常时能正确返回错误响应。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getJobExecutionHistory()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L80-L97) - 设置为抛出异常

**验证内容**:
- HTTP状态码为500 Internal Server Error
- 响应中包含success=false
- 包含适当的错误消息

### 3.3 ステップ実行履歴取得测试

#### [testGetStepExecutionHistory_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L139-L162)

**测试目标方法**: [BatchManagementController.getStepExecutionHistory()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L94-L116)

**目的**: 验证可以成功获取指定作业执行的步骤历史。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getStepExecutionHistory()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L106-L132) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 返回正确的步骤执行历史数据

#### [testGetStepExecutionHistory_Exception()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L168-L183)

**测试目标方法**: [BatchManagementController.getStepExecutionHistory()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L94-L116)

**目的**: 验证当服务层发生异常时能正确返回错误响应。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getStepExecutionHistory()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L106-L132) - 设置为抛出异常

**验证内容**:
- HTTP状态码为500 Internal Server Error
- 响应中包含success=false
- 包含适当的错误消息

### 3.4 バッチ実行統計取得测试

#### [testGetBatchStatistics_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L189-L209)

**测试目标方法**: [BatchManagementController.getBatchStatistics()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L123-L139)

**目的**: 验证可以成功获取批处理执行统计信息。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getBatchExecutionStatistics()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L159-L169) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 返回正确的统计信息数据

#### [testGetBatchStatistics_Exception()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L215-L228)

**测试目标方法**: [BatchManagementController.getBatchStatistics()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L123-L139)

**目的**: 验证当服务层发生异常时能正确返回错误响应。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getBatchExecutionStatistics()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L159-L169) - 设置为抛出异常

**验证内容**:
- HTTP状态码为500 Internal Server Error
- 响应中包含success=false
- 包含适当的错误消息

### 3.5 実行中ジョブ取得测试

#### [testGetRunningJobs_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L234-L254)

**测试目标方法**: [BatchManagementController.getRunningJobs()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L146-L163)

**目的**: 验证可以成功获取当前正在运行的批处理作业。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getRunningJobs()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L139-L152) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 返回正确的运行中作业数据

#### [testGetRunningJobs_Exception()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L260-L273)

**测试目标方法**: [BatchManagementController.getRunningJobs()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L146-L163)

**目的**: 验证当服务层发生异常时能正确返回错误响应。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getRunningJobs()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L139-L152) - 设置为抛出异常

**验证内容**:
- HTTP状态码为500 Internal Server Error
- 响应中包含success=false
- 包含适当的错误消息

### 3.6 ジョブ名一覧取得测试

#### [testGetJobNames_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L279-L294)

**测试目标方法**: [BatchManagementController.getJobNames()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L170-L187)

**目的**: 验证可以成功获取所有批处理作业名称列表。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getJobNames()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L47-L49) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 返回正确的作业名称列表

#### [testGetJobNames_Exception()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L300-L313)

**测试目标方法**: [BatchManagementController.getJobNames()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L170-L187)

**目的**: 验证当服务层发生异常时能正确返回错误响应。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getJobNames()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L47-L49) - 设置为抛出异常

**验证内容**:
- HTTP状态码为500 Internal Server Error
- 响应中包含success=false
- 包含适当的错误消息

### 3.7 特定ジョブの最新実行情報取得测试

#### [testGetLatestJobExecution_Success()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L319-L338)

**测试目标方法**: [BatchManagementController.getLatestJobExecution()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L195-L213)

**目的**: 验证可以成功获取指定作业的最新执行信息。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getLatestJobExecution()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L177-L194) - 设置为正常返回

**验证内容**:
- HTTP状态码为200 OK
- 响应中包含success=true
- 返回正确的最新执行信息

#### [testGetLatestJobExecution_Exception()](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/BatchManagementControllerTest.java#L344-L358)

**测试目标方法**: [BatchManagementController.getLatestJobExecution()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/controller/BatchManagementController.java#L195-L213)

**目的**: 验证当服务层发生异常时能正确返回错误响应。

**相关的模拟对象和方法**:
- [BatchMonitoringService](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java) - 模拟
- [BatchMonitoringService.getLatestJobExecution()](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/BatchMonitoringService.java#L177-L194) - 设置为抛出异常

**验证内容**:
- HTTP状态码为500 Internal Server Error
- 响应中包含success=false
- 包含适当的错误消息

## 4. 测试执行

### 4.1 执行所有测试

```bash
./mvnw test -Dtest=BatchManagementControllerTest
```

### 4.2 测试依赖

测试需要以下依赖：
- Mockito框架用于模拟服务
- Spring Test框架用于Web层测试
- Jackson用于JSON序列化/反序列化

### 4.3 测试环境说明

测试使用standaloneSetup配置，这意味着：
1. 不会加载完整的Spring应用上下文
2. Spring Security的@PreAuthorize注解不会生效
3. 需要手动注入所有依赖项

这种配置使测试更加轻量级，并且可以精确控制测试环境。

## 5. 常见问题及解决方案

### 5.1 权限测试问题

由于使用standaloneSetup配置，无法测试@PreAuthorize注解的效果。
如果需要测试权限控制，应使用集成测试配置（@WebMvcTest + 完整安全配置）。

### 5.2 异常处理测试

异常处理通过mock BatchMonitoringService的方法抛出异常来进行测试，
确保控制器能正确处理服务层异常。