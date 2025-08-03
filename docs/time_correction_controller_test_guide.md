# TimeCorrectionController 单元测试说明书

## 1. 测试概述

### 1.1 测试目标
本测试说明书旨在详细说明对 [TimeCorrectionController.java](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\TimeCorrectionController.java) 的单元测试，确保其所有端点都能按预期工作，并且响应格式与 API_ENDPOINTS.md 中定义的通用响应格式保持一致。

### 1.2 测试范围
- 测试文件：[TimeCorrectionController.java](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\TimeCorrectionController.java)
- 测试类：[com.example.companybackend.controller.TimeCorrectionControllerTest](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\TimeCorrectionControllerTest.java)
- 模拟依赖：
  - [TimeCorrectionService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java)（打刻修正服务类）

### 1.3 被测试功能
- 打刻修正申请创建功能
- 打刻修正申请承认功能
- 打刻修正申请拒绝功能
- 用户打刻修正申请列表获取功能
- 待审批申请列表获取功能
- 申请详情获取功能
- 用户待审批申请数获取功能
- 全体待审批申请数获取功能

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

### 3.1 testCreateTimeCorrection_Success 测试
测试用户成功创建打刻修正申请的场景。

**测试目标方法**：
- [TimeCorrectionController.createTimeCorrection()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\TimeCorrectionController.java#L43-L74)

**模拟对象**：
- [TimeCorrectionService.createTimeCorrection()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L59-L91) 返回一个成功的 [CreateTimeCorrectionResponse](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L209-L223) 对象

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含 success=true
- 响应内容包含成功消息
- 响应内容包含创建的申请数据

### 3.2 testCreateTimeCorrection_Failure 测试
测试用户创建打刻修正申请失败的场景。

**测试目标方法**：
- [TimeCorrectionController.createTimeCorrection()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\TimeCorrectionController.java#L43-L74)

**模拟对象**：
- [TimeCorrectionService.createTimeCorrection()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L59-L91) 返回一个失败的 [CreateTimeCorrectionResponse](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L209-L223) 对象

**验证内容**：
- HTTP 状态码为 400 Bad Request
- 响应内容包含 success=false
- 响应内容包含错误消息

### 3.3 testApproveTimeCorrection_Success 测试
测试管理员成功批准打刻修正申请的场景。

**测试目标方法**：
- [TimeCorrectionController.approveTimeCorrection()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\TimeCorrectionController.java#L76-L107)

**模拟对象**：
- [TimeCorrectionService.approveTimeCorrection()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L94-L131) 返回一个成功的 [ApproveTimeCorrectionResponse](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L225-L242) 对象

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含 success=true
- 响应内容包含成功消息
- 响应内容包含被批准的申请数据

### 3.4 testApproveTimeCorrection_Failure 测试
测试管理员批准打刻修正申请失败的场景。

**测试目标方法**：
- [TimeCorrectionController.approveTimeCorrection()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\TimeCorrectionController.java#L76-L107)

**模拟对象**：
- [TimeCorrectionService.approveTimeCorrection()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L94-L131) 返回一个失败的 [ApproveTimeCorrectionResponse](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L225-L242) 对象

**验证内容**：
- HTTP 状态码为 400 Bad Request
- 响应内容包含 success=false
- 响应内容包含错误消息

### 3.5 testRejectTimeCorrection_Success 测试
测试管理员成功拒绝打刻修正申请的场景。

**测试目标方法**：
- [TimeCorrectionController.rejectTimeCorrection()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\TimeCorrectionController.java#L109-L140)

**模拟对象**：
- [TimeCorrectionService.rejectTimeCorrection()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L133-L170) 返回一个成功的 [RejectTimeCorrectionResponse](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L244-L261) 对象

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含 success=true
- 响应内容包含成功消息
- 响应内容包含被拒绝的申请数据

### 3.6 testGetUserTimeCorrections_Success 测试
测试用户成功获取自己的打刻修正申请列表的场景。

**测试目标方法**：
- [TimeCorrectionController.getUserTimeCorrections()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\TimeCorrectionController.java#L142-L172)

**模拟对象**：
- [TimeCorrectionService.getUserTimeCorrections()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L173-L175) 返回一个包含申请列表的 List

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含 success=true
- 响应内容包含成功消息
- 响应内容包含用户ID和申请列表数据

### 3.7 testGetPendingTimeCorrections_Success 测试
测试管理员成功获取待审批申请列表的场景。

**测试目标方法**：
- [TimeCorrectionController.getPendingTimeCorrections()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\TimeCorrectionController.java#L174-L199)

**模拟对象**：
- [TimeCorrectionService.getPendingTimeCorrections()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L178-L180) 返回一个包含待审批申请列表的 List

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含 success=true
- 响应内容包含成功消息
- 响应内容包含申请列表数据

### 3.8 testGetTimeCorrectionById_Success 测试
测试用户成功获取特定申请详情的场景。

**测试目标方法**：
- [TimeCorrectionController.getTimeCorrectionById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\TimeCorrectionController.java#L201-L232)

**模拟对象**：
- [TimeCorrectionService.getTimeCorrectionById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L183-L185) 返回一个包含申请的 Optional

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含 success=true
- 响应内容包含成功消息
- 响应内容包含申请数据

### 3.9 testGetTimeCorrectionById_NotFound 测试
测试用户尝试获取不存在的申请详情的场景。

**测试目标方法**：
- [TimeCorrectionController.getTimeCorrectionById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\TimeCorrectionController.java#L201-L232)

**模拟对象**：
- [TimeCorrectionService.getTimeCorrectionById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L183-L185) 返回一个空的 Optional

**验证内容**：
- HTTP 状态码为 404 Not Found

### 3.10 testGetUserPendingCount_Success 测试
测试用户成功获取自己的待审批申请数的场景。

**测试目标方法**：
- [TimeCorrectionController.getUserPendingCount()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\TimeCorrectionController.java#L234-L262)

**模拟对象**：
- [TimeCorrectionService.getUserPendingCount()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L188-L190) 返回待审批申请数

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含 success=true
- 响应内容包含成功消息
- 响应内容包含用户ID和待审批申请数

### 3.11 testGetAllPendingCount_Success 测试
测试管理员成功获取全体待审批申请数的场景。

**测试目标方法**：
- [TimeCorrectionController.getAllPendingCount()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\controller\TimeCorrectionController.java#L264-L289)

**模拟对象**：
- [TimeCorrectionService.getAllPendingCount()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\TimeCorrectionService.java#L193-L195) 返回全体待审批申请数

**验证内容**：
- HTTP 状态码为 200 OK
- 响应内容包含 success=true
- 响应内容包含成功消息
- 响应内容包含全体待审批申请数

## 4. 测试执行方法

```bash
# 执行所有测试
./mvnw test -Dtest=TimeCorrectionControllerTest

# 执行特定测试方法
./mvnw test -Dtest=TimeCorrectionControllerTest#testCreateTimeCorrection_Success
```

## 5. 响应格式一致性验证

所有测试方法都验证了控制器返回的响应格式与 API_ENDPOINTS.md 中定义的通用响应格式一致：

```json
// 成功レスポンス
{
  "success": true,
  "message": "処理が完了しました",
  "data": { /* レスポンスデータ */ }
}

// エラーレポート
{
  "success": false,
  "message": "エラーメッセージ"
}
```

## 6. 常见问题及解决方案

### 6.1 依赖注入问题
如果测试中出现依赖注入问题，请确认以下注解是否正确使用：
- [@InjectMocks](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\BatchManagementControllerTest.java#L37-L38) 用于注入被测试的控制器
- [@Mock](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\BatchManagementControllerTest.java#L44-L45) 用于创建模拟对象

### 6.2 HTTP 状态码验证失败
如果 HTTP 状态码验证失败，请检查：
- 控制器方法中的返回状态码是否与测试中期望的状态码一致
- 控制器方法中的异常处理逻辑是否正确

### 6.3 响应内容验证失败
如果响应内容验证失败，请检查：
- 控制器方法返回的内容是否与测试中期望的内容一致
- 是否需要更新模拟对象的行为以匹配控制器的逻辑