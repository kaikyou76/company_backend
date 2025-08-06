# SqlInjectionProtectionTest 测试用例制作流程说明书

## 概要
本文档详细说明了 [SqlInjectionProtectionTest](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/SqlInjectionProtectionTest.java#L13-L77) 测试用例制作中的注解、模拟对象、测试制作流程和技巧。提供了针对 SQL 注入防护功能的专用测试策略。

## 1. 测试类结构分析

### 1.1 文件位置
**位置**: [src/test/java/com/example/companybackend/security/test/sql/SqlInjectionProtectionTest.java](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/SqlInjectionProtectionTest.java)

### 1.2 基本注解

#### @SpringBootTest
**行**: 11
```java
@SpringBootTest
public class SqlInjectionProtectionTest extends SecurityTestBase {
```

**目的**:
- 启动完整的 Spring Boot 应用程序上下文
- 集成所有安全配置和过滤器
- 提供真实的 Web 环境进行安全测试

**SQL 注入测试特点**:
- 继承 [SecurityTestBase](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L42-L357) 基类以获得统一的安全测试基础设施
- 使用 MockMvc 模拟 HTTP 请求
- 集成 JWT 认证和 CSRF 保护等安全机制
- 验证系统对 SQL 注入攻击的检测和阻止能力

### 1.3 核心组件

#### MockMvc 对象
**来源**: [SecurityTestBase](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L42-L357)
```java
@Autowired
protected MockMvc mockMvc;
```

**作用**:
- 模拟 HTTP 请求，无需启动实际服务器
- 发送包含 SQL 注入攻击模式的请求
- 验证系统响应是否正确阻止攻击

**测试方法**:
```java
// 主要测试方法
mockMvc.perform(post("/api/test")
        .contentType(MediaType.APPLICATION_JSON)
        .content(maliciousPayload))
        .andExpect(status().is4xxClientError());
```

#### SqlInjectionAttackPatternFactory
**行**: 15, 31, 45, 59
```java
String sqlInjectionPattern = SqlInjectionAttackPatternFactory.getRandomPattern();
```

**作用**:
- 提供常见的 SQL 注入攻击模式
- 包含 UNION SELECT、DROP TABLE、INSERT 等危险语句
- 支持随机模式和全部模式测试

**攻击模式示例**:
```java
// 常见的 SQL 注入攻击模式
"UNION SELECT username, password FROM users",
"'; DROP TABLE users; --",
"1'; INSERT INTO users (username, password) VALUES ('hacker', 'p@ssw0rd') --"
```

### 1.4 测试常量

#### 安全测试类型
**行**: 15
```java
@Override
protected String getSecurityTestType() {
    return "SQL_INJECTION_PROTECTION";
}
```

**作用**:
- 标识测试类型为 SQL 注入防护
- 用于日志记录和测试报告分类
- 与安全测试基础设施集成

## 2. 测试用例详解

### 2.1 请求体中的 SQL 注入测试
**方法**: [testSqlInjectionInRequestBody()](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/SqlInjectionProtectionTest.java#L21-L33)

**目的**:
- 验证系统能否检测并阻止在请求体中的 SQL 注入攻击
- 测试 JSON 格式数据的防护能力

**测试步骤**:
1. 从 [SqlInjectionAttackPatternFactory](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/SqlInjectionAttackPatternFactory.java#L9-L50) 获取随机 SQL 注入模式
2. 构造包含恶意代码的 JSON 请求体
3. 使用 MockMvc 发送 POST 请求
4. 验证系统返回 4xx 客户端错误而非 200 成功响应

**代码示例**:
```java
@Test
public void testSqlInjectionInRequestBody() throws Exception {
    String sqlInjectionPattern = SqlInjectionAttackPatternFactory.getRandomPattern();
    
    String maliciousPayload = "{\"name\": \"" + sqlInjectionPattern + "\"}";

    ResultActions result = mockMvc.perform(post("/api/test")
            .contentType(MediaType.APPLICATION_JSON)
            .content(maliciousPayload));

    // 预期应该返回400或403错误，而不是200
    result.andExpect(status().is4xxClientError());
}
```

### 2.2 查询字符串中的 SQL 注入测试
**方法**: [testSqlInjectionInQueryString()](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/SqlInjectionProtectionTest.java#L35-L43)

**目的**:
- 验证系统能否检测并阻止在查询字符串中的 SQL 注入攻击
- 测试 URL 参数的防护能力

**测试步骤**:
1. 从 [SqlInjectionAttackPatternFactory](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/SqlInjectionAttackPatternFactory.java#L9-L50) 获取随机 SQL 注入模式
2. 构造包含恶意代码的查询字符串参数
3. 使用 MockMvc 发送带参数的 POST 请求
4. 验证系统返回 4xx 客户端错误而非 200 成功响应

**代码示例**:
```java
@Test
public void testSqlInjectionInQueryString() throws Exception {
    String sqlInjectionPattern = SqlInjectionAttackPatternFactory.getRandomPattern();

    ResultActions result = mockMvc.perform(post("/api/test?param=" + sqlInjectionPattern));

    // 预期应该返回400或403错误，而不是200
    result.andExpect(status().is4xxClientError());
}
```

### 2.3 表单数据中的 SQL 注入测试
**方法**: [testSqlInjectionInFormData()](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/SqlInjectionProtectionTest.java#L45-L55)

**目的**:
- 验证系统能否检测并阻止在表单数据中的 SQL 注入攻击
- 测试 application/x-www-form-urlencoded 格式数据的防护能力

**测试步骤**:
1. 从 [SqlInjectionAttackPatternFactory](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/SqlInjectionAttackPatternFactory.java#L9-L50) 获取随机 SQL 注入模式
2. 构造包含恶意代码的表单参数
3. 使用 MockMvc 发送带表单数据的 POST 请求
4. 验证系统返回 4xx 客户端错误而非 200 成功响应

**代码示例**:
```java
@Test
public void testSqlInjectionInFormData() throws Exception {
    String sqlInjectionPattern = SqlInjectionAttackPatternFactory.getRandomPattern();

    ResultActions result = mockMvc.perform(post("/api/test")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("input", sqlInjectionPattern));

    // 预期应该返回400或403错误，而不是200
    result.andExpect(status().is4xxClientError());
}
```

### 2.4 所有 SQL 注入模式测试
**方法**: [testAllSqlInjectionPatterns()](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/SqlInjectionProtectionTest.java#L57-L70)

**目的**:
- 验证系统能否检测并阻止所有已知的 SQL 注入攻击模式
- 确保防护机制覆盖所有常见攻击向量

**测试步骤**:
1. 遍历 [SqlInjectionAttackPatternFactory](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/SqlInjectionAttackPatternFactory.java#L9-L50) 中的所有 SQL 注入模式
2. 对每个模式构造包含恶意代码的 JSON 请求体
3. 使用 MockMvc 发送 POST 请求
4. 验证系统对每个模式都返回 4xx 客户端错误

**代码示例**:
```java
@Test
public void testAllSqlInjectionPatterns() throws Exception {
    for (String pattern : SqlInjectionAttackPatternFactory.getAllPatterns()) {
        String maliciousPayload = "{\"name\": \"" + pattern + "\"}";

        ResultActions result = mockMvc.perform(post("/api/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousPayload));

        // 预期应该返回400或403错误，而不是200
        result.andExpect(status().is4xxClientError());
    }
}
```

## 3. 测试技巧与注意事项

### 3.1 测试环境配置
- 确保继承 [SecurityTestBase](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L42-L357) 以获得统一的安全测试基础设施
- 使用 [@SpringBootTest](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/SqlInjectionProtectionTest.java#L11-L12) 注解启动完整的应用程序上下文
- 配置测试专用的 Spring Profile（如 security-test）

### 3.2 攻击模式选择
- 使用 [SqlInjectionAttackPatternFactory](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/SqlInjectionAttackPatternFactory.java#L9-L50) 提供的标准化攻击模式
- 定期更新攻击模式以覆盖新发现的攻击向量
- 结合随机模式测试和全模式测试确保覆盖率

### 3.3 预期结果验证
- 验证系统返回 4xx 客户端错误而非 200 成功响应
- 检查错误日志以确认攻击被正确检测和记录
- 验证原始数据未被恶意 SQL 语句修改

### 3.4 测试扩展建议
- 增加对不同 HTTP 方法（GET、PUT、DELETE 等）的测试
- 添加对多参数、嵌套 JSON 结构的测试
- 测试编码后的 SQL 注入攻击（如 URL 编码、Unicode 编码等）
- 集成实际业务 API 端点进行端到端测试