# CSRFProtectionTest测试用例制作流程说明书

## 1. 制作技巧和步骤

### 1.1 准备阶段

#### 1.1.1 理解测试框架结构
- 所有安全测试都继承自[SecurityTestBase](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\SecurityTestBase.java)基类
- 使用Spring Boot Test框架进行集成测试
- 使用MockMvc模拟HTTP请求
- 使用[@ActiveProfiles("security-test")](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\SecurityTestBase.java#L34-L34)激活测试配置

#### 1.1.2 环境配置
- 配置文件: [src/test/resources/application-security-test.yml](file://f:\Company_system_project\company_backend\src\test\resources\application-security-test.yml)
- 数据库: PostgreSQL测试数据库(comsys_test)
- JWT配置: 测试专用密钥和较短的过期时间

#### 1.1.3 测试数据准备
- 使用[testDataManager](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\SecurityTestBase.java#L43-L43)记录测试结果
- 使用[testUtils](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\SecurityTestBase.java#L42-L42)辅助测试工具
- 使用预设的测试用户: [testAdminUser](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\SecurityTestBase.java#L56-L56), [testNormalUser](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\SecurityTestBase.java#L57-L57), [testManagerUser](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\SecurityTestBase.java#L58-L58)

### 1.2 制作步骤

#### 1.2.1 创建测试方法结构
1. 确定测试序号和方法名
2. 添加[@Test](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\controller\AttendanceControllerTest.java#L135-L135)和[@Order](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\csrf\CsrfProtectionTest.java#L58-L58)注解
3. 编写JavaDoc说明测试目的、前提条件和期望结果
4. 实现测试逻辑

#### 1.2.2 编写测试逻辑
1. 准备测试数据和认证令牌
2. 发送初始请求获取CSRF令牌(如果需要)
3. 构造目标请求
4. 执行请求并验证结果
5. 记录测试结果

#### 1.2.3 验证策略
- 使用`andExpect()`进行结果验证
- 根据当前实现情况调整期望值
- 记录测试结果到[testDataManager](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\SecurityTestBase.java#L43-L43)

### 1.3 最佳实践

#### 1.3.1 逐步开发方法
- 一次只开发一个测试方法
- 确保每个测试方法都能成功运行后再进行下一个
- 避免大规模重构导致的问题

#### 1.3.2 错误处理
- 使用适当的断言方法验证结果
- 对于预期的失败情况，使用`andExpect(status().isForbidden())`等方法
- 记录详细的测试结果和错误信息

#### 1.3.3 性能考虑
- 在性能测试中测量执行时间
- 使用[testMetricsRecorder](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\SecurityTestBase.java#L44-L44)记录性能指标

## 2. 测试方法详细说明

### 2.1 [testValidCsrfTokenPost()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\csrf\CsrfProtectionTest.java#L54-L91)
**测试目的**: 验证带有有效CSRF令牌的POST请求是否能被正确处理
**测试要点**: 
- 获取有效的JWT令牌
- 发送GET请求获取CSRF令牌
- 使用CSRF令牌发送POST请求
- 验证请求是否成功处理

**测试的类和方法**:
- 主要测试CSRF保护机制
- 涉及JWT认证过滤器
- 测试API端点: `/api/users/profile`

### 2.2 [testMissingCsrfTokenPost()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\csrf\CsrfProtectionTest.java#L108-L144)
**测试目的**: 验证缺少CSRF令牌的POST请求是否被正确拒绝
**测试要点**:
- 发送不带CSRF令牌的POST请求
- 验证是否返回403 Forbidden错误

**测试的类和方法**:
- 测试CSRF保护拦截器
- 验证请求过滤机制

### 2.3 [testInvalidCsrfTokenPost()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\csrf\CsrfProtectionTest.java#L160-L199)
**测试目的**: 验证带有无效CSRF令牌的POST请求是否被正确拒绝
**测试要点**:
- 构造无效的CSRF令牌
- 发送带有无效令牌的POST请求
- 验证是否返回403 Forbidden错误

**测试的类和方法**:
- 测试CSRF令牌验证逻辑
- 验证令牌有效性检查

### 2.4 [testExpiredCsrfTokenPost()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\csrf\CsrfProtectionTest.java#L215-L253)
**测试目的**: 验证带有过期CSRF令牌的POST请求是否被正确拒绝
**测试要点**:
- 生成过期的CSRF令牌
- 发送带有过期令牌的POST请求
- 验证是否返回403 Forbidden错误

**测试的类和方法**:
- 测试CSRF令牌时效性验证
- 验证令牌过期处理机制

### 2.5 [testCsrfTokenReuse()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\csrf\CsrfProtectionTest.java#L272-L314)
**测试目的**: 验证CSRF令牌重用攻击是否被正确检测和阻止
**测试要点**:
- 使用同一CSRF令牌发送两次请求
- 验证第二次请求是否被拒绝

**测试的类和方法**:
- 测试一次性令牌机制
- 验证令牌重用防护

### 2.6 [testCrossSessionTokenUsage()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\csrf\CsrfProtectionTest.java#L333-L378)
**测试目的**: 验证跨会话使用CSRF令牌是否被正确拒绝
**测试要点**:
- 在一个会话中获取CSRF令牌
- 在另一个会话中使用该令牌
- 验证请求是否被拒绝

**测试的类和方法**:
- 测试会话绑定机制
- 验证令牌与会话的关联性

### 2.7 [testSameSiteCookie()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\csrf\CsrfProtectionTest.java#L394-L432)
**测试目的**: 验证SameSite Cookie设置是否正确
**测试要点**:
- 检查响应中的Set-Cookie头
- 验证SameSite, Secure, HttpOnly标志是否正确设置

**测试的类和方法**:
- 测试Cookie安全设置
- 验证CSRF令牌Cookie配置

### 2.8 [testRefererOriginValidation()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\csrf\CsrfProtectionTest.java#L451-L507)
**测试目的**: 验证Referer/Origin头验证是否正常工作
**测试要点**:
- 使用允许的Origin发送请求
- 使用不允许的Origin发送请求
- 验证请求是否被正确处理或拒绝

**测试的类和方法**:
- 测试Origin/Referer验证机制
- 验证跨域请求防护

### 2.9 [testCsrfProtectionPerformance()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\security\test\csrf\CsrfProtectionTest.java#L520-L554)
**测试目的**: 测试CSRF保护机制的性能影响
**测试要点**:
- 测量CSRF验证处理时间
- 验证性能是否在可接受范围内

**测试的类和方法**:
- 测试CSRF保护性能
- 验证系统响应时间