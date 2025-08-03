# LeaveService 单元测试说明书

## 1. 测试概述

### 1.1 测试目标
本测试说明书旨在详细说明对 [LeaveService.java](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java) 的单元测试，确保其所有方法都能按预期工作，并且能正确处理各种边界条件和异常情况。

### 1.2 测试范围
- 测试文件：[LeaveService.java](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java)
- 测试类：[com.example.companybackend.service.LeaveServiceTest](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java)
- 模拟依赖：
  - [LeaveRequestRepository](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\LeaveRequestRepository.java)（休暇申请仓库）
  - [UserRepository](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\UserRepository.java)（用户仓库）
  - [NotificationService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\NotificationService.java)（通知服务）

### 1.3 被测试功能
- 休暇申请创建、更新、删除
- 休暇申请批准、拒绝处理
- 休暇申请查询（按用户、状态等）
- 重複期間チェック
- 休暇残日数管理
- 申请统计信息

## 2. 测试方法详解

### 2.1 测试框架和工具
- 使用 [Mockito](file://f:\Company_system_project\company_backend\pom.xml) 进行依赖模拟
- 使用 JUnit 5 进行测试编写和执行

### 2.2 测试配置
- 使用 [@ExtendWith(MockitoExtension.class)](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L33-L33) 注解启用 Mockito 功能
- 使用 [@InjectMocks](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L41-L42) 注解注入被测试的服务
- 使用 [@Mock](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L48-L49) 注解创建模拟依赖对象

## 3. 各测试方法说明

### 3.1 testCreateLeaveRequest_Success 测试
测试成功创建休暇申请的情况。

**测试目标方法**：
- [LeaveService.createLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L43-L86)

**模拟对象**：
- [UserRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L115-L115) 返回测试用户
- [LeaveRequestRepository.findOverlappingRequests()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L116-L118) 返回空列表
- [LeaveRequestRepository.save()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L119-L123) 保存申请并返回

**验证内容**：
- 返回的申请对象不为空
- 申请属性正确设置
- 各依赖方法调用次数正确
- 通知服务被调用一次

### 3.2 testCreateLeaveRequest_UserNotFound 测试
测试创建休暇申请时用户不存在的情况。

**测试目标方法**：
- [LeaveService.createLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L43-L86)

**模拟对象**：
- [UserRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L175-L175) 返回空Optional

**验证内容**：
- 抛出 IllegalArgumentException 异常
- 异常消息为"ユーザーが見つかりません: {userId}"
- 各依赖方法调用次数正确

### 3.3 testCreateLeaveRequest_OverlappingPeriod 测试
测试创建休暇申请时存在重複期間的情况。

**测试目标方法**：
- [LeaveService.createLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L43-L86)

**模拟对象**：
- [UserRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L211-L211) 返回测试用户
- [LeaveRequestRepository.findOverlappingRequests()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L212-L215) 返回包含申请的列表

**验证内容**：
- 抛出 IllegalStateException 异常
- 异常消息为"指定期間に既存の申請があります"
- 各依赖方法调用次数正确

### 3.4 testCreateLeaveRequest_InvalidLeaveType 测试
测试创建休暇申请时使用无效休暇类型的情况。

**测试目标方法**：
- [LeaveService.createLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L43-L86)

**模拟对象**：
- [UserRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L250-L250) 返回测试用户

**验证内容**：
- 抛出 IllegalArgumentException 异常
- 异常消息包含"無効な休暇タイプです"
- 各依赖方法调用次数正确

### 3.5 testUpdateLeaveRequest_Success 测试
测试成功更新休暇申请的情况。

**测试目标方法**：
- [LeaveService.updateLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L95-L142)

**模拟对象**：
- [LeaveRequestRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L283-L283) 返回待审批的测试申请
- [LeaveRequestRepository.findOverlappingRequests()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L284-L286) 返回空列表
- [LeaveRequestRepository.save()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L287-L287) 保存申请并返回

**验证内容**：
- 返回的申请对象不为空
- 申请属性正确更新
- 各依赖方法调用次数正确

### 3.6 testUpdateLeaveRequest_RequestNotFound 测试
测试更新休暇申请时申请不存在的情况。

**测试目标方法**：
- [LeaveService.updateLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L95-L142)

**模拟对象**：
- [LeaveRequestRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L324-L324) 返回空Optional

**验证内容**：
- 抛出 IllegalArgumentException 异常
- 异常消息为"休暇申請が見つかりません: {requestId}"
- 各依赖方法调用次数正确

### 3.7 testUpdateLeaveRequest_AlreadyApproved 测试
测试更新已批准的休暇申请的情况。

**测试目标方法**：
- [LeaveService.updateLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L95-L142)

**模拟对象**：
- [LeaveRequestRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L357-L357) 返回已批准的申请

**验证内容**：
- 抛出 IllegalStateException 异常
- 异常消息为"承認済みの申請は更新できません"
- 各依赖方法调用次数正确

### 3.8 testDeleteLeaveRequest_Success 测试
测试成功删除休暇申请的情况。

**测试目标方法**：
- [LeaveService.deleteLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L149-L171)

**模拟对象**：
- [LeaveRequestRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L391-L391) 返回待审批的测试申请

**验证内容**：
- 各依赖方法调用次数正确
- 申请被成功删除

### 3.9 testDeleteLeaveRequest_RequestNotFound 测试
测试删除休暇申请时申请不存在的情况。

**测试目标方法**：
- [LeaveService.deleteLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L149-L171)

**模拟对象**：
- [LeaveRequestRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L421-L421) 返回空Optional

**验证内容**：
- 抛出 IllegalArgumentException 异常
- 异常消息为"休暇申請が見つかりません: {requestId}"
- 各依赖方法调用次数正确

### 3.10 testDeleteLeaveRequest_AlreadyApproved 测试
测试删除已批准的休暇申请的情况。

**测试目标方法**：
- [LeaveService.deleteLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L149-L171)

**模拟对象**：
- [LeaveRequestRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L454-L454) 返回已批准的申请

**验证内容**：
- 抛出 IllegalStateException 异常
- 异常消息为"承認済みの申請は削除できません"
- 各依赖方法调用次数正确

### 3.11 testApproveLeaveRequest_Success 测试
测试成功批准休暇申请的情况。

**测试目标方法**：
- [LeaveService.approveLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L181-L212)

**模拟对象**：
- [LeaveRequestRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L489-L489) 返回待审批的测试申请
- [UserRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L490-L490) 返回审批者用户
- [LeaveRequestRepository.save()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L491-L491) 保存申请并返回

**验证内容**：
- 返回的申请对象不为空
- 申请状态更新为"approved"
- 审批者ID正确设置
- 各依赖方法调用次数正确

### 3.12 testApproveLeaveRequest_RequestNotFound 测试
测试批准休暇申请时申请不存在的情况。

**测试目标方法**：
- [LeaveService.approveLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L181-L212)

**模拟对象**：
- [LeaveRequestRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L526-L526) 返回空Optional

**验证内容**：
- 抛出 IllegalArgumentException 异常
- 异常消息为"休暇申請が見つかりません: {requestId}"
- 各依赖方法调用次数正确

### 3.13 testApproveLeaveRequest_ApproverNotFound 测试
测试批准休暇申请时审批者不存在的情况。

**测试目标方法**：
- [LeaveService.approveLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L181-L212)

**模拟对象**：
- [LeaveRequestRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L559-L559) 返回待审批的测试申请
- [UserRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L560-L560) 返回空Optional

**验证内容**：
- 抛出 IllegalArgumentException 异常
- 异常消息为"承認者が見つかりません: {approverId}"
- 各依赖方法调用次数正确

### 3.14 testApproveLeaveRequest_NotPending 测试
测试批准非待审批状态的休暇申请的情况。

**测试目标方法**：
- [LeaveService.approveLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L181-L212)

**模拟对象**：
- [LeaveRequestRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L596-L596) 返回已批准的申请
- [UserRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L597-L597) 返回审批者用户

**验证内容**：
- 抛出 IllegalStateException 异常
- 异常消息包含"承認待ち状態ではありません"
- 各依赖方法调用次数正确

### 3.15 testRejectLeaveRequest_Success 测试
测试成功拒绝休暇申请的情况。

**测试目标方法**：
- [LeaveService.rejectLeaveRequest()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L222-L253)

**模拟对象**：
- [LeaveRequestRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L635-L635) 返回待审批的测试申请
- [UserRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L636-L636) 返回审批者用户
- [LeaveRequestRepository.save()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L637-L637) 保存申请并返回

**验证内容**：
- 返回的申请对象不为空
- 申请状态更新为"rejected"
- 审批者ID正确设置
- 各依赖方法调用次数正确

### 3.16 testGetUserLeaveRequests 测试
测试获取用户休暇申请列表的情况。

**测试目标方法**：
- [LeaveService.getUserLeaveRequests()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L262-L264)

**模拟对象**：
- [LeaveRequestRepository.findByUserId()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L668-L668) 返回申请列表

**验证内容**：
- 返回的申请列表不为空
- 列表内容与期望值一致
- 依赖方法被调用一次

### 3.17 testGetLeaveRequestsByStatus 测试
测试按状态获取休暇申请列表的情况。

**测试目标方法**：
- [LeaveService.getLeaveRequestsByStatus()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L296-L298)

**模拟对象**：
- [LeaveRequestRepository.findByStatus()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L696-L696) 返回申请列表

**验证内容**：
- 返回的申请列表不为空
- 列表内容与期望值一致
- 依赖方法被调用一次

### 3.18 testGetPendingRequests 测试
测试获取待审批休暇申请列表的情况。

**测试目标方法**：
- [LeaveService.getPendingRequests()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L305-L307)

**模拟对象**：
- [LeaveRequestRepository.findPendingRequests()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L724-L724) 返回申请列表

**验证内容**：
- 返回的申请列表不为空
- 列表内容与期望值一致
- 依赖方法被调用一次

### 3.19 testGetLeaveRequestById_Exists 测试
测试获取存在的休暇申请详情的情况。

**测试目标方法**：
- [LeaveService.getLeaveRequestById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L399-L401)

**模拟对象**：
- [LeaveRequestRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L752-L752) 返回申请Optional

**验证内容**：
- 返回的Optional包含申请
- 申请内容与期望值一致
- 依赖方法被调用一次

### 3.20 testGetLeaveRequestById_NotExists 测试
测试获取不存在的休暇申请详情的情况。

**测试目标方法**：
- [LeaveService.getLeaveRequestById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\LeaveService.java#L399-L401)

**模拟对象**：
- [LeaveRequestRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L784-L784) 返回空Optional

**验证内容**：
- 返回的Optional为空
- 依赖方法被调用一次

## 4. 测试执行方法

```bash
# 执行所有测试
./mvnw test -Dtest=LeaveServiceTest

# 执行特定测试方法
./mvnw test -Dtest=LeaveServiceTest#testCreateLeaveRequest_Success
```

## 5. 测试结果

所有20个测试均已通过，无失败或错误。

## 6. 常见问题及解决方案

### 6.1 依赖注入问题
如果测试中出现依赖注入问题，请确认以下注解是否正确使用：
- [@InjectMocks](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L41-L42) 用于注入被测试的服务
- [@Mock](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\LeaveServiceTest.java#L48-L49) 用于创建模拟对象

### 6.2 模拟对象行为设置问题
如果测试中模拟对象的行为不符合预期，请检查：
- 模拟方法的参数匹配是否正确
- 方法调用次数是否符合预期
- 返回值是否正确设置

### 6.3 测试数据准备问题
如果测试中出现数据准备问题，请检查：
- 测试数据是否完整
- 实体对象的属性是否正确设置
- 时间戳等动态数据是否正确处理