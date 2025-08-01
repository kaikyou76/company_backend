# BatchJobController 单元测试说明书

## 1. 测试概述

### 1.1 测试目标
本测试说明书旨在详细说明对 [BatchJobController.java](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java) 的单元测试，确保其所有端点都能按预期工作。

### 1.2 测试范围
- 测试文件：[BatchJobController.java](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java)
- 测试类：[com.example.companybackend.controller.BatchJobControllerTest](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\BatchJobControllerTest.java)
- 模拟依赖：
  - [JobLauncher](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\batch\service\BatchJobService.java#L12-L12)（Spring Batch作业启动器）
  - [Job](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\batch\config\DailyAttendanceSummaryBatchConfig.java#L35-L78)（Spring Batch作业）
  - [BatchJobService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\batch\service\BatchJobService.java)（批处理作业服务）

### 1.3 被测试功能
- 批处理作业启动功能
- 批处理作业状态查询功能
- 批处理作业停止功能
- 批处理作业列表获取功能
- 批处理系统健康检查功能
- 批处理指标获取功能
- 批处理执行历史获取功能
- 批处理诊断信息获取功能
- 特定功能批处理作业启动（日次勤務時間集計、月次勤務時間集計）

## 2. 测试方法详解

### 2.1 测试框架和工具
- 使用 [Mockito](file://f:\Company_system_project\company_backend\pom.xml) 进行依赖模拟
- 使用 [MockMvc](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\BatchControllerTest.java#L28-L28) 进行 Web 层测试
- 使用 JUnit 5 进行测试编写和执行

### 2.2 测试配置
- 使用 [@ExtendWith(MockitoExtension.class)](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\BatchControllerTest.java#L25-L25) 注解启用 Mockito 功能
- 使用 [@InjectMocks](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\BatchManagementControllerTest.java#L37-L38) 注解注入被测试的控制器
- 使用 [@Mock](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\BatchManagementControllerTest.java#L44-L45) 注解创建模拟依赖对象
- 使用 MockMvcBuilders.standaloneSetup() 进行轻量级测试配置

## 3. 各测试方法说明

### 3.1 testStartJob_Success 测试
测试使用已知作业名称启动批处理作业的成功场景。

**测试目标方法**：
- [BatchJobController.startJob()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L28-L37)

**模拟对象**：
- [JobLauncher.run()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\batch\service\BatchJobService.java#L27-L27) 返回一个模拟的 [JobExecution](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\BatchMonitoringService.java#L110-L110) 对象

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含成功消息

### 3.2 testStartJob_UnknownJob 测试
测试使用未知作业名称启动批处理作业的失败场景。

**测试目标方法**：
- [BatchJobController.startJob()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L28-L37)

**模拟对象**：
- 无

**验证内容**：
- HTTP 状态码为 500 INTERNAL SERVER ERROR
- 响应内容包含错误消息

### 3.3 testStartJob_Exception 测试
测试启动批处理作业过程中发生异常的场景。

**测试目标方法**：
- [BatchJobController.startJob()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L28-L37)

**模拟对象**：
- [JobLauncher.run()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\batch\service\BatchJobService.java#L27-L27) 抛出 RuntimeException

**验证内容**：
- HTTP 状态码为 500 INTERNAL SERVER ERROR
- 响应内容包含错误消息

### 3.4 testGetJobStatus 测试
测试获取批处理作业状态的功能。

**测试目标方法**：
- [BatchJobController.getJobStatus()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L40-L44)

**模拟对象**：
- 无

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含作业状态信息

### 3.5 testStopJob 测试
测试停止批处理作业的功能。

**测试目标方法**：
- [BatchJobController.stopJob()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L46-L50)

**模拟对象**：
- 无

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含作业停止信息

### 3.6 testGetAllJobs 测试
测试获取所有批处理作业列表的功能。

**测试目标方法**：
- [BatchJobController.getAllJobs()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L52-L56)

**模拟对象**：
- 无

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含作业列表信息

### 3.7 testGetBatchHealth 测试
测试获取批处理系统健康状态的功能。

**测试目标方法**：
- [BatchJobController.getBatchHealth()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L58-L62)

**模拟对象**：
- 无

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含健康状态信息

### 3.8 testGetBatchMetrics 测试
测试获取批处理系统指标的功能。

**测试目标方法**：
- [BatchJobController.getBatchMetrics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L64-L68)

**模拟对象**：
- 无

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含指标信息

### 3.9 testGetBatchExecutions 测试
测试获取批处理执行历史的功能。

**测试目标方法**：
- [BatchJobController.getBatchExecutions()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L70-L74)

**模拟对象**：
- 无

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含执行历史信息

### 3.10 testGetBatchDiagnostics 测试
测试获取批处理诊断信息的功能。

**测试目标方法**：
- [BatchJobController.getBatchDiagnostics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L76-L80)

**模拟对象**：
- 无

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含诊断信息

### 3.11 testRunDailyWorkSummary_Success 测试
测试启动日次勤務時間集計作业的成功场景。

**测试目标方法**：
- [BatchJobController.runDailyWorkSummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L87-L94)

**模拟对象**：
- [JobLauncher.run()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\batch\service\BatchJobService.java#L27-L27) 返回一个模拟的 [JobExecution](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\BatchMonitoringService.java#L110-L110) 对象

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含成功消息

### 3.12 testRunDailyWorkSummary_Exception 测试
测试启动日次勤務時間集計作业时发生异常的场景。

**测试目标方法**：
- [BatchJobController.runDailyWorkSummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L87-L94)

**模拟对象**：
- [JobLauncher.run()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\batch\service\BatchJobService.java#L27-L27) 抛出 RuntimeException

**验证内容**：
- HTTP 状态码为 500 INTERNAL SERVER ERROR
- 响应内容包含错误消息

### 3.13 testRunMonthlyWorkSummary_Success 测试
测试启动月次勤務時間集計作业的成功场景。

**测试目标方法**：
- [BatchJobController.runMonthlyWorkSummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L98-L105)

**模拟对象**：
- [JobLauncher.run()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\batch\service\BatchJobService.java#L27-L27) 返回一个模拟的 [JobExecution](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\BatchMonitoringService.java#L110-L110) 对象

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含成功消息

### 3.14 testRunMonthlyWorkSummary_Exception 测试
测试启动月次勤務時間集計作业时发生异常的场景。

**测试目标方法**：
- [BatchJobController.runMonthlyWorkSummary()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\BatchJobController.java#L98-L105)

**模拟对象**：
- [JobLauncher.run()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\batch\service\BatchJobService.java#L27-L27) 抛出 RuntimeException

**验证内容**：
- HTTP 状态码为 500 INTERNAL SERVER ERROR
- 响应内容包含错误消息

## 4. 测试执行方法

```bash
# 执行所有测试
./mvnw test -Dtest=BatchJobControllerTest

# 执行特定测试方法
./mvnw test -Dtest=BatchJobControllerTest#testStartJob_Success
```

## 5. 常见问题及解决方案

### 5.1 依赖注入问题
如果测试中出现依赖注入问题，请确认以下注解是否正确使用：
- [@InjectMocks](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\BatchManagementControllerTest.java#L37-L38) 用于注入被测试的控制器
- [@Mock](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\BatchManagementControllerTest.java#L44-L45) 用于创建模拟对象

### 5.2 HTTP 状态码验证失败
如果 HTTP 状态码验证失败，请检查：
- 控制器方法中的返回状态码是否与测试中期望的状态码一致
- 控制器方法中的异常处理逻辑是否正确

### 5.3 响应内容验证失败
如果响应内容验证失败，请检查：
- 控制器方法返回的内容是否与测试中期望的内容一致
- 是否需要更新模拟对象的行为以匹配控制器的逻辑