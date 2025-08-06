# 邮件通知功能重启指南

本文档详细说明了如何在适当时候重新启用系统的邮件通知功能。

## 当前状态

[text](src/test/java/com/example/companybackend/service/AttendanceServiceLocationTest.java.tmp.5416.1754047223796)邮件通知功能已暂时禁用，相关配置如下：

1. 在生产环境配置文件 `src/main/resources/application-prod.properties` 中：

   ```properties
   # 邮件通知的启用标志设置为 false
   app.notification.email.enabled=false
   ```

2. 邮件服务器配置已预置但未激活：
   ```properties
   # 邮件服务器配置（预置但未激活）
   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.username=your-email@gmail.com
   spring.mail.password=your-password
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true
   ```

## 相关代码文件

1. 邮件通知服务类：

   - 文件路径：`src/main/java/com/example/companybackend/service/EmailNotificationService.java`
   - 功能：负责发送邮件通知和保存通知记录
   - 关键特性：检查 `app.notification.email.enabled` 配置决定是否发送邮件

2. 通知服务类：
   - 文件路径：`src/main/java/com/example/companybackend/service/NotificationService.java`
   - 功能：提供统一的通知发送接口，支持多种通知类型（应用内、邮件等）

## 重启步骤

### 1. 配置邮件服务器参数

编辑 `src/main/resources/application-prod.properties` 文件：

```properties
# 修改邮件服务器配置为实际参数
spring.mail.host=your-smtp-server.com
spring.mail.port=587
spring.mail.username=your-actual-email@company.com
spring.mail.password=your-actual-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# 启用邮件通知功能
app.notification.email.enabled=true
```

### 2. （可选）测试邮件功能

在配置完成后，可以编写简单的测试来验证邮件功能是否正常工作。

### 3. 部署更新

将更新后的配置文件部署到生产环境并重启应用服务。

## 故障排除

1. 如果邮件发送失败，请检查：

   - 邮件服务器地址和端口是否正确
   - 用户名和密码是否正确
   - 防火墙是否允许出站邮件连接
   - 邮件服务器是否需要特殊的安全设置（如 OAuth2）

2. 如果应用启动失败，请检查：
   - 配置文件格式是否正确
   - 所有必需的配置项是否都已设置

## 代码逻辑说明

[EmailNotificationService](file:///f:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/EmailNotificationService.java#L15-L82) 类实现了邮件发送功能，其核心逻辑如下：

1. 检查 `app.notification.email.enabled` 配置项，如果为 `false` 则跳过邮件发送
2. 如果启用，则尝试发送邮件
3. 无论邮件发送成功与否，都会保存通知记录到数据库

这种设计确保了即使邮件服务不可用，也不会影响主业务流程的执行。

## 安全注意事项

1. 不要在配置文件中明文存储邮件账户密码，建议使用环境变量或加密配置
2. 确保配置文件的访问权限受到限制
3. 定期更换邮件账户密码并更新配置
