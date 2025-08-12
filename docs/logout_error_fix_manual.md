# 登出错误修复说明书

## 问题描述
前台登出时出现401错误，后端日志显示匿名认证问题和CSRF令牌验证失败。

## 错误分析
1. 前端登出请求缺少CSRF令牌或令牌验证失败
2. 后端安全配置要求CSRF保护，但登出接口未正确处理CSRF令牌
3. 后端登出接口虽然声明需要认证，但在处理时SecurityContext被设置为匿名用户

## 修复文件及修改顺序

### 1. 后端安全配置文件
**文件名**: `src/main/java/com/example/companybackend/config/SecurityConfig.java`
**方法名**: `filterChain`
**修改内容**:
```java
.csrf(csrf -> csrf
        .csrfTokenRepository(createCookieCsrfTokenRepository())
        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
        .ignoringRequestMatchers("/api/csrf/**", "/error")
        .requireCsrfProtectionMatcher(request -> 
            !request.getMethod().equals("GET") && 
            !request.getRequestURI().equals("/api/auth/logout")))
```
**修改说明**: 排除登出接口的CSRF保护要求

### 2. 后端登出接口控制器
**文件名**: `src/main/java/com/example/companybackend/controller/AuthController.java`
**方法名**: `logout`
**修改内容**:
```java
@PostMapping("/logout")
public ResponseEntity<LogoutResponse> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    // ... 方法体保持不变
}
```
**修改说明**: 将Authorization头设置为可选参数

### 3. 前端登出服务
**文件名**: `src/app/services/authService.ts`
**方法名**: `logoutUser`
**修改内容**:
```typescript
export const logoutUser = async (): Promise<boolean> => {
  try {
    const response = await api.post("/auth/logout", {}, {
      withCredentials: true
    });
    return response.data.success;
  } catch (error: any) {
    console.error("登出错误:", error);
    return false;
  }
};
```
**修改说明**: 在登出请求中明确设置withCredentials选项

## 验证步骤
1. 重新编译并启动后端服务
2. 刷新前端页面确保获取新的CSRF令牌
3. 执行登录操作
4. 执行登出操作验证是否成功

## 注意事项
1. 修改后需要重启后端服务才能生效
2. 前端可能需要清除浏览器缓存和Cookie以确保获取最新的CSRF令牌
3. 如果问题仍然存在，请检查网络请求中的CSRF令牌是否正确发送