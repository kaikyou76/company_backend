# FileUploadSecurityTest 测试用例制作流程说明书

## 概要
本文档详细说明了 [FileUploadSecurityTest](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/FileUploadSecurityTest.java#L14-L58) 测试用例制作中的注解、模拟对象、测试制作流程和技巧。提供了针对文件上传安全功能的专用测试策略。

## 1. 测试类结构分析

### 1.1 文件位置
**位置**: [src/test/java/com/example/companybackend/security/test/sql/FileUploadSecurityTest.java](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/FileUploadSecurityTest.java)

### 1.2 基本注解

#### @SpringBootTest
**行**: 12
```java
@SpringBootTest
public class FileUploadSecurityTest extends SecurityTestBase {
```

**目的**:
- 启动完整的 Spring Boot 应用程序上下文
- 集成所有安全配置和过滤器
- 提供真实的 Web 环境进行安全测试

**文件上传安全测试特点**:
- 继承 [SecurityTestBase](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L42-L357) 基类以获得统一的安全测试基础设施
- 使用 MockMvc 模拟 HTTP 请求
- 集成 JWT 认证和 CSRF 保护等安全机制
- 验证系统对恶意文件名的检测和阻止能力

### 1.3 核心组件

#### MockMvc 对象
**来源**: [SecurityTestBase](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L42-L357)
```java
@Autowired
protected MockMvc mockMvc;
```

**作用**:
- 模拟 HTTP 请求，无需启动实际服务器
- 发送包含恶意文件名的 multipart 请求
- 验证系统响应是否正确阻止攻击

**测试方法**:
```java
// 主要测试方法
mockMvc.perform(multipart("/api/test/upload")
        .file(file))
        .andExpect(status().is4xxClientError());
```

#### MockMultipartFile 对象
**行**: 26, 42
```java
MockMultipartFile file = new MockMultipartFile(
        "file",
        maliciousFileName,
        MediaType.TEXT_PLAIN_VALUE,
        "This is test content".getBytes()
);
```

**作用**:
- 模拟上传的文件
- 可以设置文件名、内容类型和文件内容
- 用于测试恶意文件名防护机制

#### MaliciousFileNameFactory
**行**: 16, 31, 46
```java
String maliciousFileName = MaliciousFileNameFactory.getRandomMaliciousFileName();
```

**作用**:
- 提供常见的恶意文件名模式
- 包含路径遍历、操作系统特定文件等危险文件名
- 支持随机模式和全部模式测试

**恶意文件名示例**:
```java
// 常见的恶意文件名
"../../../../etc/passwd",
"test.jsp",
"test.php",
"test.asp",
"test.aspx"
```

### 1.4 测试常量

#### 安全测试类型
**行**: 16
```java
@Override
protected String getSecurityTestType() {
    return "FILE_UPLOAD_SECURITY";
}
```

**作用**:
- 标识测试类型为文件上传安全
- 用于日志记录和测试报告分类
- 与安全测试基础设施集成

## 2. 测试用例详解

### 2.1 文件上传中的恶意文件名测试
**方法**: [testMaliciousFileNameInFileUpload()](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/FileUploadSecurityTest.java#L20-L35)

**目的**:
- 验证系统能否检测并阻止包含恶意文件名的文件上传
- 测试 multipart/form-data 格式数据的防护能力

**测试步骤**:
1. 从 [MaliciousFileNameFactory](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/MaliciousFileNameFactory.java#L8-L50) 获取随机恶意文件名
2. 构造包含恶意文件名的 MockMultipartFile 对象
3. 使用 MockMvc 发送 multipart 请求
4. 验证系统返回 4xx 客户端错误而非 200 成功响应

**代码示例**:
```java
@Test
public void testMaliciousFileNameInFileUpload() throws Exception {
    String maliciousFileName = MaliciousFileNameFactory.getRandomMaliciousFileName();
    
    MockMultipartFile file = new MockMultipartFile(
            "file",
            maliciousFileName,
            MediaType.TEXT_PLAIN_VALUE,
            "This is test content".getBytes()
    );

    ResultActions result = mockMvc.perform(multipart("/api/test/upload")
            .file(file));

    // 预期应该返回400或403错误，而不是200
    result.andExpect(status().is4xxClientError());
}
```

### 2.2 所有恶意文件名测试
**方法**: [testAllMaliciousFileNames()](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/FileUploadSecurityTest.java#L37-L52)

**目的**:
- 验证系统能否检测并阻止所有已知的恶意文件名
- 确保防护机制覆盖所有常见恶意文件名

**测试步骤**:
1. 遍历 [MaliciousFileNameFactory](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/MaliciousFileNameFactory.java#L8-L50) 中的所有恶意文件名
2. 对每个文件名构造包含恶意文件名的 MockMultipartFile 对象
3. 使用 MockMvc 发送 multipart 请求
4. 验证系统对每个文件名都返回 4xx 客户端错误

**代码示例**:
```java
@Test
public void testAllMaliciousFileNames() throws Exception {
    for (String fileName : MaliciousFileNameFactory.getAllMaliciousFileNames()) {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                "This is test content".getBytes()
        );

        ResultActions result = mockMvc.perform(multipart("/api/test/upload")
                .file(file));

        // 预期应该返回400或403错误，而不是200
        result.andExpect(status().is4xxClientError());
    }
}
```

## 3. 测试技巧与注意事项

### 3.1 测试环境配置
- 确保继承 [SecurityTestBase](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/SecurityTestBase.java#L42-L357) 以获得统一的安全测试基础设施
- 使用 [@SpringBootTest](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/FileUploadSecurityTest.java#L12-L13) 注解启动完整的应用程序上下文
- 配置测试专用的 Spring Profile（如 security-test）

### 3.2 恶意文件名选择
- 使用 [MaliciousFileNameFactory](file:///f:/Company_system_project/company_backend/src/test/java/com/example/companybackend/security/test/sql/MaliciousFileNameFactory.java#L8-L50) 提供的标准化恶意文件名
- 定期更新恶意文件名以覆盖新发现的攻击向量
- 结合随机模式测试和全模式测试确保覆盖率

### 3.3 预期结果验证
- 验证系统返回 4xx 客户端错误而非 200 成功响应
- 检查错误日志以确认攻击被正确检测和记录
- 验证系统文件未被恶意文件覆盖或创建

### 3.4 测试扩展建议
- 增加对不同文件类型（MIME 类型）的测试
- 添加对大文件上传的测试
- 测试编码后的恶意文件名（如 URL 编码、Unicode 编码等）
- 集成实际业务 API 端点进行端到端测试，特别是文件上传功能
- 测试多个文件同时上传的情况