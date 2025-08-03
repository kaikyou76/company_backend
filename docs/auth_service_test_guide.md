# AuthService 单元测试说明书

## 1. 测试概述

### 1.1 测试目标
本测试说明书旨在详细说明对 [AuthService.java](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java) 的单元测试，确保其所有方法都能按预期工作，并且能正确处理各种边界条件和异常情况。

### 1.2 测试范围
- 测试文件：[AuthService.java](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java)
- 测试类：[com.example.companybackend.service.AuthServiceTest](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java)
- 模拟依赖：
  - [AuthenticationManager](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\config\SecurityConfig.java#L56-L57)（认证管理器）
  - [UserRepository](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\UserRepository.java)（用户仓库）
  - [PositionRepository](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\PositionRepository.java)（职位仓库）
  - [DepartmentRepository](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\DepartmentRepository.java)（部门仓库）
  - [PasswordEncoder](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\config\SecurityConfig.java#L64-L64)（密码编码器）
  - [JwtTokenProviderService](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\JwtTokenProviderService.java)（JWT令牌提供服务）

### 1.3 被测试功能
- 用户认证和令牌生成
- 用户注册（普通）
- 管理员用户注册
- 通过ID获取用户
- 通过用户名获取用户
- 检查用户名是否存在
- 通过部门ID获取部门名
- 通过职位ID获取职位名
- 获取管理员职位列表
- 通过CSV批量注册用户

## 2. 测试方法详解

### 2.1 测试框架和工具
- 使用 [Mockito](file://f:\Company_system_project\company_backend\pom.xml) 进行依赖模拟
- 使用 JUnit 5 进行测试编写和执行

### 2.2 测试配置
- 使用 [@ExtendWith(MockitoExtension.class)](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L46-L46) 注解启用 Mockito 功能
- 使用 [@InjectMocks](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L54-L55) 注解注入被测试的服务
- 使用 [@Mock](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L61-L62) 注解创建模拟依赖对象

## 3. 各测试方法说明

### 3.1 testAuthenticateUser_Success 测试
测试用户认证和令牌生成成功的情况。

**测试目标方法**：
- [AuthService.authenticateUser()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L46-L57)

**模拟对象**：
- [AuthenticationManager.authenticate()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L138-L141) 返回认证对象
- [JwtTokenProviderService.generateToken()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L142-L142) 返回生成的令牌

**验证内容**：
- 返回的令牌与期望值一致
- 认证管理器被调用一次
- 令牌提供服务被调用一次

### 3.2 testAuthenticateUser_Failure 测试
测试用户认证失败的情况。

**测试目标方法**：
- [AuthService.authenticateUser()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L46-L57)

**模拟对象**：
- [AuthenticationManager.authenticate()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L182-L184) 抛出异常

**验证内容**：
- 抛出 RuntimeException 异常
- 异常消息包含"認証に失敗しました"
- 认证管理器被调用一次
- 令牌提供服务从未被调用

### 3.3 testRegisterUser_Success 测试
测试普通用户注册成功的情况。

**测试目标方法**：
- [AuthService.registerUser()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L64-L91)

**模拟对象**：
- [UserRepository.existsByUsername()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L213-L213) 返回 false
- [PasswordEncoder.encode()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L214-L214) 返回编码后的密码
- [UserRepository.save()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L215-L220) 保存用户并返回

**验证内容**：
- 注册的用户对象不为空
- 用户名正确设置
- 密码已编码
- 创建时间和更新时间已设置
- 管理员ID、部门ID、职位ID为null
- 各依赖方法调用次数正确

### 3.4 testRegisterUser_DuplicateUsername 测试
测试用户名重复导致的普通用户注册失败情况。

**测试目标方法**：
- [AuthService.registerUser()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L64-L91)

**模拟对象**：
- [UserRepository.existsByUsername()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L255-L255) 返回 true

**验证内容**：
- 抛出 RuntimeException 异常
- 异常消息为"ユーザー名は既に使用されています"
- 各依赖方法调用次数正确

### 3.5 testRegisterUserByAdmin_Success 测试
测试管理员成功注册用户的情况。

**测试目标方法**：
- [AuthService.registerUserByAdmin()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L99-L157)

**模拟对象**：
- [UserRepository.findByUsername()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L288-L288) 返回管理员用户
- [PositionRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L289-L289) 返回管理员职位
- [UserRepository.existsByUsername()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L290-L290) 返回 false
- [PositionRepository.existsById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L291-L291) 返回 true
- [DepartmentRepository.existsById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L292-L292) 返回 true
- [PasswordEncoder.encode()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L293-L293) 返回编码后的密码
- [UserRepository.save()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L294-L299) 保存用户并返回

**验证内容**：
- 注册的用户对象不为空
- 用户名正确设置
- 密码已编码
- 创建时间和更新时间已设置
- 各依赖方法调用次数正确

### 3.6 testRegisterUserByAdmin_AdminNotFound 测试
测试管理员用户不存在导致注册失败的情况。

**测试目标方法**：
- [AuthService.registerUserByAdmin()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L99-L157)

**模拟对象**：
- [UserRepository.findByUsername()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L330-L330) 返回空Optional

**验证内容**：
- 抛出 RuntimeException 异常
- 异常消息为"管理者ユーザーが見つかりません"
- 各依赖方法调用次数正确

### 3.7 testRegisterUserByAdmin_InsufficientAuthority 测试
测试权限不足导致管理员注册用户失败的情况。

**测试目标方法**：
- [AuthService.registerUserByAdmin()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L99-L157)

**模拟对象**：
- [UserRepository.findByUsername()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L372-L372) 返回低权限用户
- [PositionRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L373-L373) 返回低权限职位

**验证内容**：
- 抛出 RuntimeException 异常
- 异常消息为"権限が不足しています。管理者ユーザーのみ登録可能です。"
- 各依赖方法调用次数正确

### 3.8 testGetUserById_Success 测试
测试通过ID成功获取用户的情况。

**测试目标方法**：
- [AuthService.getUserById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L164-L166)

**模拟对象**：
- [UserRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L410-L410) 返回用户Optional

**验证内容**：
- 返回的Optional包含用户
- 用户与期望值一致
- 用户仓库的findById方法被调用一次

### 3.9 testGetUserById_NotFound 测试
测试通过ID获取用户但用户不存在的情况。

**测试目标方法**：
- [AuthService.getUserById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L164-L166)

**模拟对象**：
- [UserRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L432-L432) 返回空Optional

**验证内容**：
- 返回的Optional为空
- 用户仓库的findById方法被调用一次

### 3.10 testGetUserByUsername_Success 测试
测试通过用户名成功获取用户的情况。

**测试目标方法**：
- [AuthService.getUserByUsername()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L172-L175)

**模拟对象**：
- [UserRepository.findByUsername()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L454-L454) 返回用户Optional

**验证内容**：
- 返回的用户与期望值一致
- 用户仓库的findByUsername方法被调用一次

### 3.11 testGetUserByUsername_NotFound 测试
测试通过用户名获取用户但用户不存在的情况。

**测试目标方法**：
- [AuthService.getUserByUsername()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L172-L175)

**模拟对象**：
- [UserRepository.findByUsername()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L478-L478) 返回空Optional

**验证内容**：
- 抛出 RuntimeException 异常
- 异常消息为"ユーザーが見つかりません"
- 用户仓库的findByUsername方法被调用一次

### 3.12 testCheckUsernameExists_Exists 测试
测试检查存在的用户名的情况。

**测试目标方法**：
- [AuthService.checkUsernameExists()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L181-L183)

**模拟对象**：
- [UserRepository.existsByUsername()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L503-L503) 返回 true

**验证内容**：
- 返回 true
- 用户仓库的existsByUsername方法被调用一次

### 3.13 testCheckUsernameExists_NotExists 测试
测试检查不存在的用户名的情况。

**测试目标方法**：
- [AuthService.checkUsernameExists()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L181-L183)

**模拟对象**：
- [UserRepository.existsByUsername()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L525-L525) 返回 false

**验证内容**：
- 返回 false
- 用户仓库的existsByUsername方法被调用一次

### 3.14 testGetDepartmentNameById_Success 测试
测试通过部门ID成功获取部门名称的情况。

**测试目标方法**：
- [AuthService.getDepartmentNameById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L190-L200)

**模拟对象**：
- [DepartmentRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L552-L552) 返回部门Optional

**验证内容**：
- 返回的部门名称与期望值一致
- 部门仓库的findById方法被调用一次

### 3.15 testGetDepartmentNameById_NotFound 测试
测试通过部门ID获取部门名称但部门不存在的情况。

**测试目标方法**：
- [AuthService.getDepartmentNameById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L190-L200)

**模拟对象**：
- [DepartmentRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L577-L577) 返回空Optional

**验证内容**：
- 返回 null
- 部门仓库的findById方法被调用一次

### 3.16 testGetDepartmentNameById_NullId 测试
测试部门ID为null时获取部门名称的情况。

**测试目标方法**：
- [AuthService.getDepartmentNameById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L190-L200)

**验证内容**：
- 返回 null
- 部门仓库的findById方法从未被调用

### 3.17 testGetPositionNameById_Success 测试
测试通过职位ID成功获取职位名称的情况。

**测试目标方法**：
- [AuthService.getPositionNameById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L206-L216)

**模拟对象**：
- [PositionRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L606-L606) 返回职位Optional

**验证内容**：
- 返回的职位名称与期望值一致
- 职位仓库的findById方法被调用一次

### 3.18 testGetPositionNameById_NotFound 测试
测试通过职位ID获取职位名称但职位不存在的情况。

**测试目标方法**：
- [AuthService.getPositionNameById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L206-L216)

**模拟对象**：
- [PositionRepository.findById()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L631-L631) 返回空Optional

**验证内容**：
- 返回 null
- 职位仓库的findById方法被调用一次

### 3.19 testGetPositionNameById_NullId 测试
测试职位ID为null时获取职位名称的情况。

**测试目标方法**：
- [AuthService.getPositionNameById()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L206-L216)

**验证内容**：
- 返回 null
- 职位仓库的findById方法从未被调用

### 3.20 testGetAdminPositions_Success 测试
测试成功获取管理员职位列表的情况。

**测试目标方法**：
- [AuthService.getAdminPositions()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L222-L233)

**模拟对象**：
- [PositionRepository.findByLevelGreaterThanEqualOrderByLevelDesc()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L663-L663) 返回职位列表

**验证内容**：
- 返回的职位数据列表不为空
- 列表大小与期望值一致
- 职位按等级降序排列（"管理者" 在前，"スーパーバイザー" 在后）
- 职位仓库的方法被调用一次

### 3.21 testRegisterUsersFromCsvList_Success 测试
测试通过CSV用户数据列表成功批量注册用户的情况。

**测试目标方法**：
- [AuthService.registerUsersFromCsv()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L240-L272)（List版本）

**模拟对象**：
- [PasswordEncoder.encode()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L688-L690) 返回编码后的密码
- [UserRepository.existsByUsername()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L691-L691) 返回 false
- [UserRepository.save()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L692-L697) 保存用户并返回

**验证内容**：
- 返回的成功件数为2
- 返回的失败件数为0
- 各依赖方法调用次数正确

### 3.22 testRegisterUsersFromCsvList_WithDuplicates 测试
测试通过CSV用户数据列表批量注册用户但包含重复用户名的情况。

**测试目标方法**：
- [AuthService.registerUsersFromCsv()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L240-L272)（List版本）

**模拟对象**：
- [PasswordEncoder.encode()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L727-L731) 返回编码后的密码
- [UserRepository.existsByUsername()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L732-L733) 部分返回 true
- [UserRepository.save()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L734-L739) 保存用户并返回

**验证内容**：
- 返回的成功件数为1
- 返回的失败件数为1
- 各依赖方法调用次数正确

### 3.23 testRegisterUsersFromCsvFile_Success 测试
测试通过CSV文件成功批量注册用户的情况。

**测试目标方法**：
- [AuthService.registerUsersFromCsv()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L279-L308)（MultipartFile版本）

**模拟对象**：
- [MultipartFile.getInputStream()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L759-L759) 返回CSV内容输入流
- [PasswordEncoder.encode()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L762-L764) 返回编码后的密码
- [UserRepository.existsByUsername()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L765-L765) 返回 false
- [UserRepository.save()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L766-L771) 保存用户并返回

**验证内容**：
- 返回的结果消息包含"登録成功: 2件"和"登録失敗: 0件"
- 各依赖方法调用次数正确

### 3.24 testRegisterUsersFromCsvFile_IOException 测试
测试通过CSV文件批量注册用户时发生IO异常的情况。

**测试目标方法**：
- [AuthService.registerUsersFromCsv()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\service\AuthService.java#L279-L308)（MultipartFile版本）

**模拟对象**：
- [MultipartFile.getInputStream()](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L793-L793) 抛出 IOException

**验证内容**：
- 抛出 RuntimeException 异常
- 异常消息包含"CSVファイルの処理中にエラーが発生しました"
- 各依赖方法调用次数正确

## 4. 测试执行方法

```bash
# 执行所有测试
./mvnw test -Dtest=AuthServiceTest

# 执行特定测试方法
./mvnw test -Dtest=AuthServiceTest#testAuthenticateUser_Success
```

## 5. 测试结果

所有24个测试均已通过，无失败或错误。

## 6. 常见问题及解决方案

### 6.1 依赖注入问题
如果测试中出现依赖注入问题，请确认以下注解是否正确使用：
- [@InjectMocks](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L54-L55) 用于注入被测试的服务
- [@Mock](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\service\AuthServiceTest.java#L61-L62) 用于创建模拟对象

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