# XssProtectionTest 测试用例制作流程说明书

## 概要
本文档详细说明了 [XssProtectionTest](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/xss/XssProtectionTest.java#L59-L675) 测试用例制作中的注解、安全设置、测试制作流程和技巧。提供针对 XSS（跨站脚本）攻击防御特性的专用测试策略。

## 1. 测试类结构分析

### 1.1 文件位置
**位置**: [src/test/java/com/example/companybackend/security/test/xss/XssProtectionTest.java](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/xss/XssProtectionTest.java)

### 1.2 基本注解

#### [@SpringBootTest](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/test/context/SpringBootTest.html)(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
**行**: 40
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("security-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class XssProtectionTest extends SecurityTestBase {
```

**目的**:
- 构建 Spring Boot 集成测试环境
- 在随机端口启动 Web 服务器
- 应用安全测试专用配置文件
- 执行与实际数据库的集成测试

**XSS 防护测试的特征**:
- 安全过滤器链的完整集成测试
- 在实际 HTTP 请求/响应中模拟 XSS 攻击
- 全面验证安全头
- 测试对多种攻击模式的防御能力

### 1.3 继承类: SecurityTestBase

#### SecurityTestBase 的作用
**行**: 45
```java
public class XssProtectionTest extends SecurityTestBase {
```

**提供的功能**:
- **测试用户管理**: [testNormalUser](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L70-L70), [testAdminUser](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L72-L72), [testManagerUser](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L71-L71)
- **JWT 相关组件**: [jwtTokenProvider](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L52-L52), [mockMvc](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L46-L46)
- **测试数据管理**: [testDataManager](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L50-L50), [testUtils](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L51-L51)
- **安全设置**: 安全测试用的通用设置

**主要字段**:
```java
// 从 SecurityTestBase 继承的主要字段
@Autowired
protected MockMvc mockMvc;

@Autowired
protected SecurityTestDataManager testDataManager;

@Autowired
protected SecurityTestUtils testUtils;

protected User testNormalUser;
protected User testAdminUser;
protected User testManagerUser;
```

### 1.4 测试执行顺序控制

#### [@TestMethodOrder](https://junit.org/junit5/docs/current/api/org.junit.jupiter.api/org/junit/jupiter/api/TestMethodOrder.html)(MethodOrderer.OrderAnnotation.class)
**行**: 44
```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
```

**目的**:
- 控制测试用例的执行顺序
- 从基本功能到高级功能逐步测试
- 考虑安全功能的依赖关系保证顺序

**执行顺序**:
1. `@Order(1)` - 脚本标签注入防御测试
2. `@Order(2)` - 事件处理程序注入防御测试
3. `@Order(3)` - JavaScript URL 注入防御测试
4. `@Order(4)` - Content Security Policy 头测试
5. `@Order(5)` - X-Frame-Options 头测试
6. `@Order(6)` - X-Content-Type-Options 头测试
7. `@Order(7)` - HTML 响应转义测试
8. `@Order(8)` - JSON 响应安全性测试
9. `@Order(9)` - 复合 XSS 攻击防御测试
10. `@Order(10)` - XSS 防护性能测试

### 1.5 安全测试类型识别

#### getSecurityTestType() 方法
**行**: 47-50
```java
@Override
protected String getSecurityTestType() {
    return "XSS_PROTECTION";
}
```

**作用**:
- 提供测试类型标识符
- 用于记录测试结果时的分类
- 作为生成安全测试报告时的分类标准

## 2. 测试用例详细分析

### 2.1 基本 XSS 攻击防御测试群

#### 测试用例1: 脚本标签注入防御测试
**方法**: [testScriptTagInjectionPrevention](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/xss/XssProtectionTest.java#L58-L115)
**行**: 52-110
**需求对应**: 需求2.1

##### 测试数据准备
```java
// Given (行58-59)
String validToken = createValidJwtToken(testNormalUser);
String maliciousScript = "<script>alert('XSS Attack!');</script>";
String requestBody = String.format("""
    {
        "username": "testuser",
        "email": "test@example.com",
        "bio": "%s"
    }
    """, maliciousScript);
```

##### XSS 攻击请求执行
```java
// When & Then (行73-81)
MvcResult result = mockMvc.perform(
        put("/api/users/profile")
                .header("Authorization", "Bearer " + validToken)
                .header("Origin", "http://localhost:3000")
                .header("X-CSRF-TOKEN", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andReturn();
```

##### 多阶段验证
```java
// 响应内容验证 (行84-90)
String responseContent = result.getResponse().getContentAsString();

// 基本 XSS 防御确认（不包含原始脚本标签）
assertFalse(responseContent.contains("<script>"), "不包含原始脚本标签");
assertFalse(responseContent.contains("alert('XSS Attack!');"), "不包含原始 JavaScript 代码");

// 确认请求已正常处理
assertTrue(responseContent.contains("success") || responseContent.contains("data") 
          || result.getResponse().getStatus() == 200,
          "请求已正常处理");

// 记录改进建议
if (!responseContent.contains("&lt;script&gt;") && !responseContent.contains("\\u003cscript\\u003e")) {
    System.out.println("改进建议: 考虑加强 HTML 转义处理");
}

// 确认安全头设置
assertTrue(testUtils.hasSecurityHeaders(result), "安全头已设置");
```

#### 测试用例2: 事件处理程序注入防御测试
**方法**: [testEventHandlerInjectionPrevention](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/xss/XssProtectionTest.java#L117-L175)
**行**: 112-170
**需求对应**: 需求2.2

##### 多攻击模式测试
```java
// Given (行124-137)
String validToken = createValidJwtToken(testNormalUser);
String csrfToken = extractCsrfToken(mockMvc.perform(get("/api/csrf/token")
                .header("Authorization", "Bearer " + validToken))
                .andReturn());

String[] maliciousEventHandlers = {
    "<img src='x' onerror='alert(\"XSS\")'>",
    "<div onload='alert(\"XSS\")'>Content</div>",
    "<input onclick='alert(\"XSS\")' value='Click me'>",
    "<body onmouseover='alert(\"XSS\")'>",
    "<svg onload='alert(\"XSS\")'></svg>"
};

for (String maliciousHandler : maliciousEventHandlers) {
    String requestBody = String.format("""
                    {
                        "fullName": "testuser",
                        "email": "test@example.com",
                        "phone": "%s"
                    }
                    """, maliciousHandler.replace("\"", "\\\""));
```

##### 事件处理程序失效化验证
```java
// When & Then (行139-159)
MvcResult result = mockMvc.perform(
        put("/api/users/profile")
                .header("Authorization", "Bearer " + validToken)
                .header("Origin", "http://localhost:3000")
                .header("X-CSRF-TOKEN", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andReturn();

String responseContent = result.getResponse().getContentAsString();

// 确认事件处理程序不以原始形式包含
assertFalse(responseContent.contains("onerror="), "不包含原始 onerror 事件处理程序");
assertFalse(responseContent.contains("onload="), "不包含原始 onload 事件处理程序");
assertFalse(responseContent.contains("onclick="), "不包含原始 onclick 事件处理程序");
assertFalse(responseContent.contains("onmouseover="), "不包含原始 onmouseover 事件处理程序");

// 确认不包含 JavaScript 代码
assertFalse(responseContent.contains("alert("), "不包含 JavaScript alert 函数");
```

#### 测试用例3: JavaScript URL 注入防御测试
**方法**: [testJavaScriptUrlInjectionPrevention](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/xss/XssProtectionTest.java#L206-L265)
**行**: 200-265
**需求对应**: 需求2.3

##### 危险 URL 模式测试
```java
// Given (行193-207)
String validToken = createValidJwtToken(testNormalUser);
String csrfToken = extractCsrfToken(mockMvc.perform(get("/api/csrf/token")
                .header("Authorization", "Bearer " + validToken))
                .andReturn());
String[] maliciousUrls = {
        "javascript:alert('XSS')",
        "javascript:void(0);alert('XSS')",
        "data:text/html,<script>alert('XSS')</script>",
        "vbscript:alert('XSS')",
        "javascript:eval('alert(\"XSS\")')",
        "data:text/html;base64,PHNjcmlwdD5hbGVydCgnWFNTJyk8L3NjcmlwdD4="
};

for (String maliciousUrl : maliciousUrls) {
    String requestBody = String.format("""
                    {
                        "username": "testuser",
                        "email": "test@example.com",
                        "website": "%s"
                    }
                    """, maliciousUrl.replace("\"", "\\\""));
```

##### URL 模式防御验证
```java
// When & Then (行209-228)
MvcResult result = mockMvc.perform(
        put("/api/users/profile")
                .header("Authorization", "Bearer " + validToken)
                .header("Origin", "http://localhost:3000")
                .header("X-CSRF-TOKEN", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andReturn();

String responseContent = result.getResponse().getContentAsString();

// 确认危险 URL 模式不以原始形式包含
assertFalse(responseContent.contains("javascript:"), "不包含原始 javascript: 模式");
assertFalse(responseContent.contains("vbscript:"), "不包含原始 vbscript: 模式");
assertFalse(responseContent.contains("data:text/html"), "不包含危险的 data: 模式");

// 确认不包含 JavaScript 代码
assertFalse(responseContent.contains("alert("), "不包含 JavaScript alert 函数");
assertFalse(responseContent.contains("eval("), "不包含 JavaScript eval 函数");
```

### 2.2 安全头测试群

#### 测试用例4: Content Security Policy 头测试
**方法**: [testContentSecurityPolicyHeader](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/xss/XssProtectionTest.java#L285-L324)
**行**: 280-324
**需求对应**: 需求2.4

##### CSP 头验证
```java
// When & Then (行290-294)
MvcResult result = mockMvc.perform(
        get("/api/users/profile")
                .header("Authorization", "Bearer " + validToken))
        .andExpect(status().isOk())
        .andReturn();

// 确认 CSP 头存在
String cspHeader = result.getResponse().getHeader("Content-Security-Policy");
assertNotNull(cspHeader, "Content-Security-Policy 头已设置");
```

##### CSP 指令验证
```java
// CSP 指令确认
assertTrue(cspHeader.contains("default-src"), "已设置 default-src 指令");
assertTrue(cspHeader.contains("script-src"), "已设置 script-src 指令");
assertTrue(cspHeader.contains("style-src"), "已设置 style-src 指令");
assertTrue(cspHeader.contains("img-src"), "已设置 img-src 指令");

// 基本 CSP 设置确认
assertTrue(cspHeader.contains("'self'"), "已设置 'self'");
```

#### 测试用例5: X-Frame-Options 头测试
**方法**: [testXFrameOptionsHeader](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/xss/XssProtectionTest.java#L340-L372)
**行**: 334-372
**需求对应**: 需求2.5

##### 点击劫持防护验证
```java
// When & Then (行345-349)
MvcResult result = mockMvc.perform(
        get("/api/users/profile")
                .header("Authorization", "Bearer " + validToken))
        .andExpect(status().isOk())
        .andReturn();

// 确认 X-Frame-Options 头存在
String xFrameOptionsHeader = result.getResponse().getHeader("X-Frame-Options");
assertNotNull(xFrameOptionsHeader, "X-Frame-Options 头已设置");

// 确认设置了适当的值
assertTrue(xFrameOptionsHeader.equals("DENY") ||
        xFrameOptionsHeader.equals("SAMEORIGIN"),
        "X-Frame-Options 设置为 DENY 或 SAMEORIGIN");
```

#### 测试用例6: X-Content-Type-Options 头测试
**方法**: [testXContentTypeOptionsHeader](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/xss/XssProtectionTest.java#L378-L410)
**行**: 372-410
**需求对应**: 需求2.6

##### MIME 类型嗅探防护验证
```java
// When & Then (行383-387)
MvcResult result = mockMvc.perform(
        get("/api/users/profile")
                .header("Authorization", "Bearer " + validToken))
        .andExpect(status().isOk())
        .andReturn();

// 确认 X-Content-Type-Options 头存在
String xContentTypeOptionsHeader = result.getResponse().getHeader("X-Content-Type-Options");
assertNotNull(xContentTypeOptionsHeader, "X-Content-Type-Options 头已设置");

// 确认设置了 nosniff
assertEquals("nosniff", xContentTypeOptionsHeader, "X-Content-Type-Options 设置为 nosniff");
```

### 2.3 响应内容安全性测试群

#### 测试用例7: HTML 响应转义测试
**方法**: [testHtmlResponseEscaping](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/xss/XssProtectionTest.java#L430-L479)
**行**: 424-479
**需求对应**: 需求2.7

##### HTML 特殊字符转义验证
```java
// Given (行417-426)
String validToken = createValidJwtToken(testNormalUser);
// 为避免 JSON 解析错误，适当转义特殊字符
String dangerousChars = "<>&'"; // 移除双引号，使用单引号
String requestBody = String.format("""
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "bio": "Test content with dangerous chars: %s"
                }
                """, dangerousChars);

// When & Then (行429-435)
MvcResult result = mockMvc.perform(
        put("/api/users/profile")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(csrf()))
        .andExpect(status().isOk()) // 确认请求正常处理，HTML字符被转义
        .andReturn();
```

##### 响应类型特定验证
```java
String responseContent = result.getResponse().getContentAsString();
String contentType = result.getResponse().getContentType();

if (contentType != null && contentType.contains("text/html")) {
    // HTML 响应的情况
    assertFalse(responseContent.contains("<") &&
            !responseContent.contains("&lt;"),
            "< 字符已正确转义");
    assertFalse(responseContent.contains(">") &&
            !responseContent.contains("&gt;"),
            "> 字符已正确转义");
    assertFalse(responseContent.contains("&") &&
            !responseContent.contains("&amp;"),
            "& 字符已正确转义");
} else {
    // JSON 响应的情况 - 基本安全性检查
    assertTrue(responseContent.contains("success") || responseContent.contains("data") ||
            responseContent.contains("error"),
            "响应内容包含预期结果");
}
```

#### 测试用例8: JSON 响应安全性测试
**方法**: [testJsonResponseSafety](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/xss/XssProtectionTest.java#L495-L544)
**行**: 489-544
**需求对应**: 需求2.8

##### JSON 结构保护验证
```java
// Given (行497-504)
String maliciousJson = "\\\"}; alert('XSS'); var dummy={\\\"key\\\":\\\"";
String requestBody = String.format("""
        {
            "username": "testuser",
            "email": "test@example.com",
            "bio": "%s"
        }
        """, maliciousJson);

// When & Then (行506-512)
MvcResult result = mockMvc.perform(
        put("/api/users/profile")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();
```

##### JSON 注入防护验证
```java
String responseContent = result.getResponse().getContentAsString();

// 确认 JSON 结构未被破坏
assertTrue(responseContent.startsWith("{") && responseContent.endsWith("}"),
        "JSON 响应结构保持完整");

// 确认不包含危险的 JavaScript 代码
assertFalse(responseContent.contains("alert("), "不包含 JavaScript alert 函数");
assertFalse(responseContent.contains("}; "), "不包含破坏 JSON 结构的字符串");
```

### 2.4 高级 XSS 防护测试群

#### 测试用例9: 复合 XSS 攻击防御测试
**方法**: [testCombinedXssAttackPrevention](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/xss/XssProtectionTest.java#L553-L599)
**行**: 547-599
**需求对应**: 需求2.9

##### 多重攻击模式组合测试
```java
// Given (行555-558)
String combinedAttack = "<script>alert('XSS')</script>" +
        "<img src='x' onerror='alert(\"XSS\")'>" +
        "javascript:alert('XSS')";

String escapedAttack = combinedAttack
        .replace("\\", "\\\\") // 首先转义反斜杠
        .replace("\"", "\\\"") // 转义双引号
        .replace("\n", "\\n") // 转义换行符
        .replace("\r", "\\r"); // 转义回车符

String requestBody = String.format("""
        {
            "username": "testuser",
            "email": "test@example.com",
            "bio": "%s",
            "website": "javascript:alert('XSS')"
        }
        """, escapedAttack);
```

##### 综合防御能力验证
```java
// When & Then (行560-571)
MvcResult result = mockMvc.perform(
        put("/api/users/profile")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andReturn();

String responseContent = result.getResponse().getContentAsString();

// 确认不包含任何原始攻击模式
assertFalse(responseContent.contains("<script>"), "不包含原始脚本标签");
assertFalse(responseContent.contains("onerror="), "不包含原始事件处理程序");
assertFalse(responseContent.contains("javascript:"), "不包含原始 JavaScript URL");
```

#### 测试用例10: XSS 防护性能测试
**方法**: [testXssProtectionPerformance](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/xss/XssProtectionTest.java#L613-L664)
**行**: 607-664
**需求对应**: 需求2.10

##### 性能影响测量
```java
// Given (行615-619)
String largeXssPayload = "<script>alert('XSS')</script>".repeat(100);
String requestBody = String.format("""
        {
            "username": "testuser",
            "email": "test@example.com",
            "bio": "%s"
        }
        """, largeXssPayload.replace("\"", "\\\""));

// 测量 XSS 防护处理时间
long xssProtectionTime = testUtils.measureResponseTime(() -> {
    try {
        mockMvc.perform(
                put("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
});
```

##### 性能基准验证
```java
// 确认 XSS 防护处理在 200ms 内完成
assertTrue(xssProtectionTime < 200, 
        "XSS 防护处理应在 200ms 内完成: " + xssProtectionTime + "ms");

// 确认系统资源使用适当
assertTrue(xssProtectionTime > 0, "XSS 防护处理时间应为正值");

// 记录性能测试结果
System.out.println("XSS 防护性能测试结果: " + xssProtectionTime + "ms");
```

## 3. 测试制作技巧和注意事项

### 3.1 安全测试共通注意事项

#### CSRF 令牌处理
在执行修改操作的测试中，需要正确处理 CSRF 令牌：
```java
MvcResult result = mockMvc.perform(
        put("/api/users/profile")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(csrf()))  // 添加 CSRF 令牌
        .andExpect(status().isOk())
        .andReturn();
```

#### JWT 令牌生成
使用继承自 [SecurityTestBase](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L42-L352) 的 [createValidJwtToken](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L181-L189) 方法生成有效的 JWT 令牌：
```java
String validToken = createValidJwtToken(testNormalUser);
```

### 3.2 测试数据管理

#### 测试结果记录
使用 [testDataManager](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L50-L50) 记录测试结果：
```java
testDataManager.recordTestResult(
        getClass().getSimpleName(),
        "testScriptTagInjectionPrevention",
        "XSS_SCRIPT_TAG_PREVENTION",
        "PASSED",
        getTestExecutionTime(),
        "Script tag injection successfully prevented");
```

#### 改进建议输出
在测试中发现潜在问题时，输出改进建议：
```java
if (!responseContent.contains("&lt;script&gt;") && !responseContent.contains("\\u003cscript\\u003e")) {
    System.out.println("改进建议: 考虑加强 HTML 转义处理");
}
```

### 3.3 多种响应类型处理

#### HTML 响应处理
针对 HTML 响应，验证 HTML 特殊字符的转义：
```java
if (contentType != null && contentType.contains("text/html")) {
    assertFalse(responseContent.contains("<") &&
            !responseContent.contains("&lt;"),
            "< 字符已正确转义");
}
```

#### JSON 响应处理
针对 JSON 响应，验证结构完整性和安全性：
```java
assertTrue(responseContent.startsWith("{") && responseContent.endsWith("}"),
        "JSON 响应结构保持完整");
```

## 4. 常见问题及解决方案

### 4.1 CSRF 令牌相关问题

#### 问题现象
测试中出现 403 Forbidden 错误，提示 CSRF 令牌验证失败。

#### 解决方案
在测试请求中添加 CSRF 令牌：
```java
.with(csrf())
```

### 4.2 JWT 令牌相关问题

#### 问题现象
测试中出现 401 Unauthorized 错误，提示 JWT 令牌无效。

#### 解决方案
使用 [createValidJwtToken](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L181-L189) 方法生成有效的 JWT 令牌：
```java
String validToken = createValidJwtToken(testNormalUser);
```

### 4.3 响应内容验证问题

#### 问题现象
响应内容验证失败，无法确认 XSS 防护效果。

#### 解决方案
根据响应类型（HTML 或 JSON）进行不同的验证：
```java
String contentType = result.getResponse().getContentType();
if (contentType != null && contentType.contains("text/html")) {
    // HTML 响应验证
} else {
    // JSON 响应验证
}
```