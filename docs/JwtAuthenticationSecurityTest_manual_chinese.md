# JwtAuthenticationSecurityTest 测试用例制作流程说明书

## 概要
本文档详细说明了 [JwtAuthenticationSecurityTest](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/jwt/JwtAuthenticationSecurityTest.java#L47-L521) 测试用例制作中的注解使用、测试制作流程和技巧。针对JWT认证安全测试的特性，提供了专用的测试策略。

## 1. 测试类结构分析

### 1.1 文件位置
**位置**: [src/test/java/com/example/companybackend/security/test/jwt/JwtAuthenticationSecurityTest.java](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/jwt/JwtAuthenticationSecurityTest.java)

### 1.2 基本注解

#### @SpringBootTest
**行**: 47
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

**目的**:
- 启动完整的Spring Boot应用程序上下文
- 使用随机端口进行Web环境测试
- 集成所有相关的安全配置和过滤器

**JWT安全测试的特点**:
- 需要完整的安全配置环境
- 需要实际的Web服务器来测试过滤器链
- 需要集成JwtTokenProvider和相关安全组件

#### @ActiveProfiles("security-test")
**行**: 48
```java
@ActiveProfiles("security-test")
```

**目的**:
- 激活安全测试专用配置文件
- 使用测试专用的JWT配置（较短的过期时间等）
- 确保测试环境与生产环境隔离

#### @AutoConfigureTestDatabase
**行**: 49
```java
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
```

**目的**:
- 使用实际的测试数据库而非内存数据库
- 确保数据库相关功能（如JWT配置读取）正常工作
- 保持测试环境与实际运行环境一致

#### @AutoConfigureMockMvc
**行**: 50
```java
@AutoConfigureMockMvc
```

**目的**:
- 自动配置MockMvc用于Web层测试
- 模拟HTTP请求和响应
- 测试API端点的安全性

#### @TestMethodOrder
**行**: 51
```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
```

**目的**:
- 按照指定顺序执行测试方法
- 确保依赖关系正确的测试执行顺序
- 便于调试和理解测试流程

### 1.3 继承结构

#### SecurityTestBase
**行**: 52
```java
class JwtAuthenticationSecurityTest extends SecurityTestBase
```

**作用**:
- 提供通用的安全测试基础设施
- 初始化测试用户（管理员、普通用户、经理）
- 提供JWT工具方法和测试数据管理器
- 处理测试前后的设置和清理工作

**提供的关键组件**:
- [mockMvc](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L62-L62): 用于模拟HTTP请求
- [jwtTokenProvider](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L65-L65): JWT令牌处理服务
- [testAdminUser](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L82-L82), [testNormalUser](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L83-L83), [testManagerUser](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L84-L84): 测试用用户对象

## 2. 核心测试方法详解

### 2.1 testAccessWithoutToken - 无令牌访问测试

#### 测试目标
验证没有JWT令牌访问受保护资源时系统能正确拒绝。对应需求1.6。

#### 关键注解
```java
@Test
@Order(1)
void testAccessWithoutToken() throws Exception
```

#### 测试流程
1. 不使用任何认证信息访问受保护的API端点
2. 验证响应状态为403 Forbidden

#### 重点验证点
- 无令牌访问被正确拒绝
- 返回适当的HTTP状态码（403 Forbidden）

### 2.2 testMalformedTokenAccess - 格式错误令牌访问测试

#### 测试目标
验证格式错误的JWT令牌被正确拒绝。对应需求1.5。

#### 关键注解
```java
@Test
@Order(2)
void testMalformedTokenAccess() throws Exception
```

#### 测试流程
1. 创建多种格式错误的JWT令牌
2. 使用这些令牌访问受保护的API端点
3. 验证所有访问都被拒绝

#### 重点验证点
- 各种格式错误的令牌都被拒绝
- 系统不会因为格式错误的令牌而崩溃
- 返回适当的HTTP状态码（403 Forbidden）

### 2.3 testExpiredTokenAccess - 过期令牌访问测试

#### 测试目标
验证过期的JWT令牌被正确拒绝。对应需求1.3。

#### 关键注解
```java
@Test
@Order(3)
void testExpiredTokenAccess() throws Exception
```

#### 测试流程
1. 手动创建已过期的JWT令牌
2. 使用过期令牌访问受保护的API端点
3. 验证访问被拒绝

#### 重点验证点
- 过期令牌验证失败
- 令牌被正确识别为过期
- 返回适当的HTTP状态码（403 Forbidden）

### 2.4 testInvalidSignatureTokenAccess - 无效签名令牌访问测试

#### 测试目标
验证签名被篡改的JWT令牌被正确拒绝。对应需求1.4。

#### 关键注解
```java
@Test
@Order(4)
void testInvalidSignatureTokenAccess() throws Exception
```

#### 测试流程
1. 创建签名被篡改的JWT令牌
2. 使用该令牌访问受保护的API端点
3. 验证访问被拒绝

#### 重点验证点
- 签名验证正确识别篡改的令牌
- 返回适当的HTTP状态码（403 Forbidden）

### 2.5 testNullTokenAccess - 空令牌访问测试

#### 测试目标
验证null令牌被正确处理。

#### 关键注解
```java
@Test
@Order(5)
void testNullTokenAccess() throws Exception
```

#### 测试流程
1. 使用null作为令牌访问受保护的API端点
2. 验证访问被拒绝且不抛出异常

#### 重点验证点
- null令牌处理不抛出异常
- 访问被正确拒绝

### 2.6 testExcessivelyLongTokenAccess - 超长令牌访问测试

#### 测试目标
验证异常长的令牌被正确处理（DoS攻击防护）。

#### 关键注解
```java
@Test
@Order(6)
void testExcessivelyLongTokenAccess() throws Exception
```

#### 测试流程
1. 创建10KB长度的令牌
2. 使用该令牌访问受保护的API端点
3. 验证访问被拒绝且不抛出异常

#### 重点验证点
- 超长令牌处理不抛出异常
- 系统不会因超长令牌而崩溃
- DoS攻击防护有效

### 2.7 testSpecialCharacterTokenAccess - 特殊字符令牌访问测试

#### 测试目标
验证包含特殊字符的令牌被正确处理（注入攻击防护）。

#### 关键注解
```java
@Test
@Order(7)
void testSpecialCharacterTokenAccess() throws Exception
```

#### 测试流程
1. 创建包含各种特殊字符的令牌（如XSS、SQL注入等）
2. 使用这些令牌访问受保护的API端点
3. 验证所有访问都被拒绝且不抛出异常

#### 重点验证点
- 特殊字符令牌处理不抛出异常
- 注入攻击被有效防护
- 系统稳定运行

### 2.8 testConcurrentInvalidTokenAccess - 并发无效令牌访问测试

#### 测试目标
验证并发无效令牌访问时系统能正确处理。

#### 关键注解
```java
@Test
@Order(8)
void testConcurrentInvalidTokenAccess() throws Exception
```

#### 测试流程
1. 准备多种无效令牌
2. 使用多线程并发访问受保护的API端点
3. 验证所有访问都被拒绝

#### 重点验证点
- 并发无效访问都被正确拒绝
- 系统在并发环境下稳定运行
- 资源枯竭攻击被防护

### 2.9 testNonExistentUserTokenAccess - 不存在用户令牌访问测试

#### 测试目标
验证令牌中包含不存在用户信息时被正确处理。

#### 关键注解
```java
@Test
@Order(9)
void testNonExistentUserTokenAccess() throws Exception
```

#### 测试流程
1. 创建包含不存在用户信息的JWT令牌
2. 使用该令牌访问受保护的API端点
3. 验证访问被拒绝

#### 重点验证点
- 令牌格式有效但用户不存在时能正确处理
- 访问被适当拒绝
- 安全日志被正确记录

### 2.10 testInsufficientPermissionAccess - 权限不足访问测试

#### 测试目标
验证权限不足的用户访问受限资源时被正确拒绝。

#### 关键注解
```java
@Test
@Order(10)
void testInsufficientPermissionAccess() throws Exception
```

#### 测试流程
1. 使用普通用户令牌访问管理员专用API端点
2. 验证访问被拒绝

#### 重点验证点
- 权限检查正确工作
- 权限不足的访问被拒绝
- 返回适当的HTTP状态码（403 Forbidden）

## 3. 测试工具和辅助方法

### 3.1 createExpiredToken
**行**: 457
```java
private String createExpiredToken()
```

**作用**:
- 创建已过期的JWT令牌用于测试
- 使用反射获取JwtTokenProvider的密钥
- 设置令牌过期时间为1秒前

### 3.2 createInvalidSignatureToken
**行**: 488
```java
private String createInvalidSignatureToken()
```

**作用**:
- 创建签名被篡改的JWT令牌用于测试
- 使用错误的密钥进行签名
- 令牌格式正确但签名无效

### 3.3 createTokenForNonExistentUser
**行**: 505
```java
private String createTokenForNonExistentUser()
```

**作用**:
- 创建包含不存在用户信息的JWT令牌用于测试
- 使用反射获取JwtTokenProvider的密钥
- 设置不存在的用户ID和邮箱

## 4. 测试最佳实践

### 4.1 安全测试要点
- 验证所有异常情况都被正确处理
- 确保系统不会因为恶意输入而崩溃
- 检查返回的错误信息不会泄露敏感信息

### 4.2 权限验证
- 验证不同角色用户的访问权限正确实施
- 确保越权访问被正确拒绝
- 检查API端点的权限控制

### 4.3 攻击防护测试
- 测试各种已知攻击向量（XSS、SQL注入等）
- 验证DoS攻击防护机制
- 检查令牌处理的安全性

### 4.4 并发安全测试
- 包含并发访问测试确保系统稳定性
- 验证多线程环境下的安全机制
- 检查资源竞争和死锁情况