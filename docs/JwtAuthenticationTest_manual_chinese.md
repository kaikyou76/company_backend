# JwtAuthenticationTest 测试用例制作流程说明书

## 概要
本文档详细说明了 [JwtAuthenticationTest](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L35-L357) 测试用例制作中的注解使用、模拟对象、测试制作流程和技巧。针对JWT认证服务的特性，提供了专用的测试策略。

## 1. 测试类结构分析

### 1.1 文件位置
**位置**: [src/test/java/com/example/companybackend/security/test/jwt/JwtAuthenticationTest.java](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/jwt/JwtAuthenticationTest.java)

### 1.2 基本注解

#### @SpringBootTest
**行**: 27
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

**目的**:
- 启动完整的Spring Boot应用程序上下文
- 使用随机端口进行Web环境测试
- 集成所有相关的安全配置和过滤器

**JWT认证测试的特点**:
- 需要完整的安全配置环境
- 需要实际的Web服务器来测试过滤器链
- 需要集成JwtTokenProvider和相关安全组件

#### @ActiveProfiles("security-test")
**行**: 28
```java
@ActiveProfiles("security-test")
```

**目的**:
- 激活安全测试专用配置文件
- 使用测试专用的JWT配置（较短的过期时间等）
- 确保测试环境与生产环境隔离

#### @AutoConfigureTestDatabase
**行**: 29
```java
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
```

**目的**:
- 使用实际的测试数据库而非内存数据库
- 确保数据库相关功能（如JWT配置读取）正常工作
- 保持测试环境与实际运行环境一致

#### @AutoConfigureMockMvc
**行**: 30
```java
@AutoConfigureMockMvc
```

**目的**:
- 自动配置MockMvc用于Web层测试
- 模拟HTTP请求和响应
- 测试API端点的安全性

#### @TestMethodOrder
**行**: 31
```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
```

**目的**:
- 按照指定顺序执行测试方法
- 确保依赖关系正确的测试执行顺序
- 便于调试和理解测试流程

### 1.3 继承结构

#### SecurityTestBase
**行**: 32
```java
class JwtAuthenticationTest extends SecurityTestBase
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

### 2.1 testValidTokenAuthentication - 有效令牌认证测试

#### 测试目标
验证有效的JWT令牌能够成功访问受保护的API资源。

#### 关键注解
```java
@Test
@Order(1)
void testValidTokenAuthentication() throws Exception
```

#### 测试流程
1. 创建测试用户的有效JWT令牌
2. 使用该令牌访问受保护的API端点(/api/users/profile)
3. 验证响应状态为200 OK
4. 验证令牌的有效性及相关信息提取

#### 重点验证点
- 令牌生成成功
- 令牌验证通过
- 能够从令牌中正确提取用户名
- 响应包含安全头信息

### 2.2 testAdminTokenAuthentication - 管理员令牌认证测试

#### 测试目标
验证管理员用户的JWT令牌能够正常工作。

#### 关键注解
```java
@Test
@Order(2)
void testAdminTokenAuthentication() throws Exception
```

#### 测试流程
1. 创建管理员用户的有效JWT令牌
2. 使用该令牌访问受保护的API端点
3. 验证令牌的有效性和用户信息提取

#### 重点验证点
- 管理员令牌生成成功
- 管理员令牌验证通过
- 能够从令牌中正确提取管理员用户名

### 2.3 testConcurrentTokenAuthentication - 并发令牌认证测试

#### 测试目标
验证多个用户同时进行JWT认证时系统能正常处理。对应需求1.8。

#### 关键注解
```java
@Test
@Order(3)
void testConcurrentTokenAuthentication() throws Exception
```

#### 测试流程
1. 为普通用户、管理员用户和经理用户分别创建JWT令牌
2. 使用多线程同时执行多个API请求
3. 验证所有令牌在并发环境下仍然有效

#### 重点验证点
- 并发环境下各令牌仍然有效
- 系统能正确处理多个同时进行的认证请求
- 令牌之间不会相互干扰

### 2.4 testTokenInformationExtraction - 令牌信息提取测试

#### 测试目标
验证能从JWT令牌中正确提取各种用户信息。

#### 关键注解
```java
@Test
@Order(4)
void testTokenInformationExtraction() throws Exception
```

#### 测试流程
1. 创建测试用户的JWT令牌
2. 从令牌中提取用户名、用户ID、位置类型、部门ID等信息
3. 验证提取的信息与原始用户信息一致

#### 重点验证点
- 用户名提取正确
- 用户ID提取正确
- 位置类型提取正确
- 部门ID提取正确

### 2.5 testTokenExpirationBoundary - 令牌过期边界测试

#### 测试目标
验证令牌在过期边界值时的行为。对应需求1.7。

#### 关键注解
```java
@Test
@Order(5)
void testTokenExpirationBoundary() throws Exception
```

#### 测试流程
1. 手动创建一个5分钟有效期的JWT令牌
2. 验证令牌在有效期内的状态
3. 检查剩余有效时间计算是否正确

#### 重点验证点
- 有效期内令牌验证通过
- 令牌未过期判断正确
- 剩余有效时间计算正确

### 2.6 testMultipleTokensForSameUser - 同一用户多令牌测试

#### 测试目标
验证同一用户可以生成多个独立的JWT令牌。

#### 关键注解
```java
@Test
@Order(6)
void testMultipleTokensForSameUser() throws Exception
```

#### 测试流程
1. 为同一用户生成两个JWT令牌
2. 验证两个令牌不相同但都有效
3. 验证两个令牌都能正确提取相同的用户信息
4. 验证两个令牌都能成功访问API

#### 重点验证点
- 同一用户可生成多个不同令牌
- 所有令牌都有效
- 所有令牌提取的用户信息一致
- 所有令牌都能成功访问API

### 2.7 testRefreshTokenGeneration - 刷新令牌生成测试

#### 测试目标
验证刷新令牌的生成和验证功能。

#### 关键注解
```java
@Test
@Order(7)
void testRefreshTokenGeneration() throws Exception
```

#### 测试流程
1. 为测试用户生成访问令牌
2. 验证访问令牌的有效性
3. 检查访问令牌的剩余有效时间

#### 重点验证点
- 访问令牌生成成功
- 访问令牌验证通过
- 访问令牌有剩余有效时间

### 2.8 testJwtPerformance - JWT性能测试

#### 测试目标
验证JWT认证的性能在可接受范围内。

#### 关键注解
```java
@Test
@Order(8)
void testJwtPerformance() throws Exception
```

#### 测试流程
1. 测量令牌生成时间
2. 测量令牌验证时间
3. 测量带认证的API访问时间
4. 验证各项时间指标在预设阈值内

#### 重点验证点
- 令牌生成时间小于100ms
- 令牌验证时间小于50ms
- 带认证的API访问时间小于200ms

## 3. 测试工具和辅助方法

### 3.1 createJwtTokenWithExpiration
**行**: 425
```java
private String createJwtTokenWithExpiration(User user, long expirationMillis)
```

**作用**:
- 创建具有指定过期时间的JWT令牌
- 用于测试令牌过期相关功能
- 直接使用JwtTokenProvider的密钥进行签名

### 3.2 createValidJwtToken
**行**: 415
```java
public String createValidJwtToken(User user)
```

**作用**:
- 创建标准的有效JWT令牌
- 使用JwtTokenProvider的标准创建方法
- 用于大多数常规测试场景

## 4. 测试最佳实践

### 4.1 测试数据管理
- 使用SecurityTestBase提供的测试用户
- 每个测试方法独立，不依赖其他测试方法的状态
- 测试结束后自动清理相关数据

### 4.2 安全配置验证
- 验证安全头信息的正确设置
- 确保只有有效的令牌能访问受保护资源
- 验证不同角色用户的访问权限

### 4.3 性能和并发测试
- 包含性能基准测试确保响应时间
- 包含并发测试确保系统稳定性
- 验证边界条件下的系统行为

### 4.4 测试结果记录
- 使用testDataManager记录测试结果
- 记录测试执行时间
- 提供详细的测试结果信息用于后续分析