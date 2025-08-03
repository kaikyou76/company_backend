# HolidayRepository 单元测试说明书

## 1. 测试概述

### 1.1 测试目标
本测试说明书旨在详细说明对 [HolidayRepository.java](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java) 的单元测试，确保其所有方法都能按预期工作，并且能正确处理各种边界条件和异常情况。

### 1.2 测试范围
- 测试文件：[HolidayRepository.java](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java)
- 测试类：[com.example.companybackend.repository.HolidayRepositoryTest](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\repository\HolidayRepositoryTest.java)
- 测试环境：
  - 使用 H2 内存数据库进行测试
  - 使用 [@DataJpaTest](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\repository\HolidayRepositoryTest.java#L32-L35) 注解进行数据访问层测试

### 1.3 被测试功能
- 祝日的基本查询操作（按日期、名称）
- 祝日の存在確認
- 年別、月別、期間別祝日取得
- 祝日数のカウント
- 統計情報の取得
- 祝日名部分一致検索

## 2. 测试方法详解

### 2.1 测试框架和工具
- 使用 Spring Boot Test 进行集成测试
- 使用 H2 内存数据库作为测试数据库
- 使用 TestEntityManager 管理测试数据
- 使用 AssertJ 进行断言验证

### 2.2 测试配置
- 使用 [@DataJpaTest](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\repository\HolidayRepositoryTest.java#L32-L35) 注解专注于 JPA 层测试
- 使用 [@TestPropertySource](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\repository\HolidayRepositoryTest.java#L36-L39) 配置测试用的内存数据库

## 3. 各测试方法说明

### 3.1 testFindByDate_Exists 测试
测试通过存在的日期查找祝日的情况。

**测试目标方法**：
- [HolidayRepository.findByDate()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L33-L35)

**测试数据**：
- 创建一个测试祝日（2025年1月1日，元日）
- 使用 TestEntityManager 保存到内存数据库

**验证内容**：
- 返回的 Optional 包含祝日对象
- 祝日的日期和名称正确

### 3.2 testFindByDate_NotExists 测试
测试通过不存在的日期查找祝日的情况。

**测试目标方法**：
- [HolidayRepository.findByDate()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L33-L35)

**测试数据**：
- 不创建任何测试数据

**验证内容**：
- 返回的 Optional 为空

### 3.3 testFindByName_Exists 测试
测试通过存在的名称查找祝日的情况。

**测试目标方法**：
- [HolidayRepository.findByName()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L41-L43)

**测试数据**：
- 创建一个测试祝日（2025年2月11日，建国記念の日）
- 使用 TestEntityManager 保存到内存数据库

**验证内容**：
- 返回的 Optional 包含祝日对象
- 祝日的名称和日期正确

### 3.4 testFindByName_NotExists 测试
测试通过不存在的名称查找祝日的情况。

**测试目标方法**：
- [HolidayRepository.findByName()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L41-L43)

**测试数据**：
- 不创建任何测试数据

**验证内容**：
- 返回的 Optional 为空

### 3.5 testExistsByDate_Exists 测试
测试存在的日期祝日存在性检查。

**测试目标方法**：
- [HolidayRepository.existsByDate()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L49-L51)

**测试数据**：
- 创建一个测试祝日（2025年1月1日，元日）
- 使用 TestEntityManager 保存到内存数据库

**验证内容**：
- 返回 true

### 3.6 testExistsByDate_NotExists 测试
测试不存在的日期祝日存在性检查。

**测试目标方法**：
- [HolidayRepository.existsByDate()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L49-L51)

**测试数据**：
- 不创建任何测试数据

**验证内容**：
- 返回 false

### 3.7 testFindByYear 测试
测试获取指定年份的所有祝日。

**测试目标方法**：
- [HolidayRepository.findByYear()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L58-L59)

**测试数据**：
- 创建多个年份的祝日（2025年3个，2024年1个）
- 使用 TestEntityManager 保存到内存数据库

**验证内容**：
- 返回指定年份（2025年）的3个祝日
- 祝日按日期升序排列

### 3.8 testFindByYearAndMonth 测试
测试获取指定年月的所有祝日。

**测试目标方法**：
- [HolidayRepository.findByYearAndMonth()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L67-L69)

**测试数据**：
- 创建同一月份的2个祝日和其他月份的1个祝日
- 使用 TestEntityManager 保存到内存数据库

**验证内容**：
- 返回指定年月（2025年1月）的2个祝日
- 祝日按日期升序排列

### 3.9 testFindByDateRange 测试
测试获取指定日期范围内的所有祝日。

**测试目标方法**：
- [HolidayRepository.findByDateRange()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L76-L78)

**测试数据**：
- 创建范围内2个祝日和范围外1个祝日
- 使用 TestEntityManager 保存到内存数据库

**验证内容**：
- 返回指定日期范围内的2个祝日
- 祝日按日期升序排列

### 3.10 testFindByNameContaining 测试
测试通过名称部分匹配查找祝日。

**测试目标方法**：
- [HolidayRepository.findByNameContaining()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L128-L130)

**测试数据**：
- 创建包含指定模式的3个祝日和不包含的1个祝日
- 使用 TestEntityManager 保存到内存数据库

**验证内容**：
- 返回名称包含指定模式的3个祝日
- 祝日按日期升序排列

### 3.11 testCountByYear 测试
测试统计指定年份的祝日数量。

**测试目标方法**：
- [HolidayRepository.countByYear()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L137-L139)

**测试数据**：
- 创建指定年份（2025年）的3个祝日和其他年份的1个祝日
- 使用 TestEntityManager 保存到内存数据库

**验证内容**：
- 返回指定年份的祝日数量（3）

### 3.12 testCountByYearAndMonth 测试
测试统计指定年月的祝日数量。

**测试目标方法**：
- [HolidayRepository.countByYearAndMonth()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L147-L150)

**测试数据**：
- 创建指定年月（2025年1月）的2个祝日和其他月份的1个祝日
- 使用 TestEntityManager 保存到内存数据库

**验证内容**：
- 返回指定年月的祝日数量（2）

### 3.13 testCountByDateRange 测试
测试统计指定日期范围内的祝日数量。

**测试目标方法**：
- [HolidayRepository.countByDateRange()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L157-L160)

**测试数据**：
- 创建范围内2个祝日和范围外1个祝日
- 使用 TestEntityManager 保存到内存数据库

**验证内容**：
- 返回指定日期范围内的祝日数量（2）

### 3.14 testGetYearlyStatistics 测试
测试获取年别祝日统计信息。

**测试目标方法**：
- [HolidayRepository.getYearlyStatistics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L167-L175)

**测试数据**：
- 创建多个年份的祝日（2025年3个，2024年2个）
- 使用 TestEntityManager 保存到内存数据库

**验证内容**：
- 返回年别统计信息列表
- 结果按年份降序排列
- 各年的祝日数统计正确

### 3.15 testGetMonthlyStatistics 测试
测试获取指定年份的月别祝日统计信息。

**测试目标方法**：
- [HolidayRepository.getMonthlyStatistics()](file://f:\Company_system_project\company_backend\src\main\java\com\example\companybackend\repository\HolidayRepository.java#L184-L193)

**测试数据**：
- 创建指定年份（2025年）多个月份的祝日（1月2个，2月1个，12月1个）
- 使用 TestEntityManager 保存到内存数据库

**验证内容**：
- 返回指定年份的月别统计信息列表
- 结果按月份升序排列
- 各月的祝日数统计正确

## 4. 测试执行方法

```bash
# 执行所有测试
./mvnw test -Dtest=HolidayRepositoryTest

# 执行特定测试方法
./mvnw test -Dtest=HolidayRepositoryTest#testFindByDate_Exists
```

## 5. 测试结果

所有测试均已通过，无失败或错误。

## 6. 常见问题及解决方案

### 6.1 测试环境问题
如果测试中出现数据库连接问题，请确认以下配置：
- [@TestPropertySource](file://f:\Company_system_project\company_backend\src\test\java\com\example\companybackend\repository\HolidayRepositoryTest.java#L36-L39) 是否正确配置了内存数据库
- H2 数据库依赖是否在 pom.xml 中正确声明

### 6.2 测试数据管理问题
如果测试中出现数据相关问题，请检查：
- 是否使用 TestEntityManager 正确持久化测试数据
- 测试数据是否在每个测试方法中独立创建
- 测试完成后数据是否被正确清理

### 6.3 查询结果验证问题
如果测试中出现结果验证问题，请检查：
- 断言条件是否正确
- 返回值类型是否匹配（如 Double 类型的数值比较）
- 结果排序是否符合预期