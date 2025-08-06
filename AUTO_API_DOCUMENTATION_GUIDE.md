# API 自动文档生成和访问指南

## 概述

本项目已集成 SpringDoc OpenAPI，可自动生成 API 文档。该功能提供交互式界面，便于开发者测试和理解 API 接口。

## 访问自动生成的 API 文档

### Swagger UI 界面

启动应用程序后，可通过以下 URL 访问 Swagger UI 界面：

```
http://localhost:8080/swagger-ui.html
```

该界面提供：
- 所有 API 端点的交互式文档
- 在线测试 API 功能
- 请求参数和响应格式的详细说明
- 认证机制集成

### OpenAPI JSON 规范

如果需要获取原始的 OpenAPI JSON 规范文件，可访问：

```
http://localhost:8080/v3/api-docs
```

该 JSON 文件可用于：
- 生成客户端 SDK
- 导入到其他 API 工具（如 Postman）
- 进行 API 版本管理

## 配置说明

### 依赖项

项目已添加以下依赖项以支持自动生成文档：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

### 配置类

OpenAPI 配置位于：
```
src/main/java/com/example/companybackend/config/OpenApiConfig.java
```

配置内容包括：
- API 基本信息（标题、版本、描述等）
- 联系人信息
- 许可证信息
- 安全方案（JWT Bearer Token）

## 增强文档内容

为了更好地利用自动生成文档功能，可以在控制器中使用以下注解：

### @Operation 注解

用于描述 API 方法：

```java
@Operation(
    summary = "获取用户信息", 
    description = "获取当前登录用户的基本信息"
)
@GetMapping("/profile")
public ResponseEntity<?> getCurrentUser() {
    // 实现代码
}
```

### @Parameter 注解

用于描述请求参数：

```java
@GetMapping("/list")
public ResponseEntity<?> getUsers(
    @Parameter(description = "页码，从0开始") 
    @RequestParam(defaultValue = "0") int page,
    
    @Parameter(description = "每页大小，默认为10") 
    @RequestParam(defaultValue = "10") int size
) {
    // 实现代码
}
```

### @ApiResponse 注解

用于描述响应信息：

```java
@ApiResponse(
    responseCode = "200", 
    description = "成功获取用户列表",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = UserListResponse.class)
    )
)
@GetMapping("/list")
public ResponseEntity<?> getUsers(...) {
    // 实现代码
}
```

## 使用指南

### 1. 启动应用程序

确保应用程序已在本地运行：
```bash
mvn spring-boot:run
```

或者使用 IDE 运行主类。

### 2. 访问 Swagger UI

在浏览器中打开：
```
http://localhost:8080/swagger-ui.html
```

### 3. 浏览 API 文档

Swagger UI 界面按控制器分组显示所有 API 端点：
- 展开每个端点查看详细信息
- 点击 "Try it out" 按钮在线测试 API
- 输入必要参数并执行请求
- 查看实时响应结果

### 4. 认证使用

对于需要认证的 API：
1. 首先调用登录接口获取 JWT Token
2. 点击界面右上角的 "Authorize" 按钮
3. 输入 `Bearer <your_token>` 格式的认证信息
4. 现在可以测试需要认证的 API 端点

## 注意事项

1. **与手动文档的关系**：自动生成的文档是手动维护文档的补充，不完全替代手动文档。

2. **中文支持**：虽然可以显示中文，但建议在注解中使用日文以保持项目语言一致性。

3. **实时性**：自动生成文档始终与代码保持同步，反映最新的 API 实现。

4. **权限显示**：自动生成文档不直接显示权限信息，需要参考手动维护的 API 权限矩阵。

## 故障排除

### 无法访问 Swagger UI

1. 确认应用程序已正常启动
2. 检查控制台是否有错误信息
3. 确认端口是否为 8080

### 文档内容不完整

1. 检查控制器方法是否正确使用了 Spring Web 注解
2. 确认方法是否为 public
3. 检查是否有条件注解导致方法在当前环境下不可用

### 认证问题

1. 确认 JWT Token 格式正确
2. 检查 Token 是否已过期
3. 确认 Token 具有相应 API 的访问权限