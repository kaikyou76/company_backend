package com.example.companybackend.controller;

import com.example.companybackend.entity.Department;
import com.example.companybackend.service.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 部门控制器测试类
 * 
 * 该测试类使用@WebMvcTest注解，专门用于测试Spring MVC控制器层
 * 它启动一个轻量级的Spring上下文，只包含Web层相关的组件
 */
@WebMvcTest(DepartmentController.class)
@ContextConfiguration(classes = {DepartmentController.class, DepartmentControllerTest.TestSecurityConfig.class})
public class DepartmentControllerTest {

    // 注入MockMvc对象，用于模拟HTTP请求
    @Autowired
    private MockMvc mockMvc;

    // 使用@MockBean创建DepartmentService的模拟对象
    // 这样可以在不依赖真实服务的情况下测试控制器逻辑
    @MockBean
    private DepartmentService departmentService;

    // 测试数据：单个部门对象
    private Department testDepartment;
    // 测试数据：部门列表
    private List<Department> testDepartments;

    /**
     * 测试安全配置类
     * 用于在测试环境中配置Spring Security规则
     */
    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {
        /**
         * 配置安全过滤链
         * @param http HttpSecurity对象
         * @return SecurityFilterChain 安全过滤链
         * @throws Exception 配置异常
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                    // 配置部门相关API需要ADMIN角色才能访问
                    .requestMatchers("/api/departments/**").hasRole("ADMIN")
                    .anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS));
            return http.build();
        }
    }

    /**
     * 测试前的准备工作
     * 在每个测试方法执行前都会运行此方法，用于初始化测试数据
     */
    @BeforeEach
    void setUp() {
        // 创建测试用的部门对象1
        testDepartment = new Department();
        testDepartment.setId(1L);
        testDepartment.setName("开发部");
        testDepartment.setCode("DEV");
        testDepartment.setManagerId(1);
        testDepartment.setCreatedAt(OffsetDateTime.now());
        testDepartment.setUpdatedAt(OffsetDateTime.now());

        // 创建测试用的部门对象2
        Department department2 = new Department();
        department2.setId(2L);
        department2.setName("人事部");
        department2.setCode("HR");
        department2.setManagerId(2);
        department2.setCreatedAt(OffsetDateTime.now());
        department2.setUpdatedAt(OffsetDateTime.now());

        // 将两个部门对象放入列表中
        testDepartments = Arrays.asList(testDepartment, department2);
    }

    /**
     * 部门列表获取测试
     * 验证管理员用户可以成功获取部门列表
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟departmentService.getAllDepartments()方法返回测试数据
     * 3. 发送GET请求到/api/departments端点
     * 4. 验证响应状态码为200 OK
     * 5. 验证响应数据结构正确
     * 6. 验证服务方法被正确调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllDepartments_Success() throws Exception {
        // 设置模拟行为：当调用departmentService.getAllDepartments()时返回测试数据
        when(departmentService.getAllDepartments()).thenReturn(testDepartments);

        // 执行GET请求并验证响应
        mockMvc.perform(get("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON))
                // 验证HTTP状态码为200 OK
                .andExpect(status().isOk())
                // 验证响应JSON中success字段为true
                .andExpect(jsonPath("$.success").value(true))
                // 验证响应JSON中data数组长度为2
                .andExpect(jsonPath("$.data.length()").value(2))
                // 验证响应JSON中第一个部门名称为"开发部"
                .andExpect(jsonPath("$.data[0].name").value("开发部"));

        // 验证departmentService.getAllDepartments()方法被调用了一次
        verify(departmentService, times(1)).getAllDepartments();
    }

    /**
     * 根据ID获取部门信息测试
     * 验证管理员用户可以获取特定部门信息
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟departmentService.getDepartmentById()方法返回指定ID的部门
     * 3. 发送GET请求到/api/departments/{id}端点
     * 4. 验证响应状态码为200 OK
     * 5. 验证响应数据结构正确
     * 6. 验证服务方法被正确调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetDepartmentById_Success() throws Exception {
        // 设置模拟行为：当调用departmentService.getDepartmentById(1)时返回测试部门对象
        when(departmentService.getDepartmentById(1)).thenReturn(Optional.of(testDepartment));

        // 执行GET请求并验证响应
        mockMvc.perform(get("/api/departments/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                // 验证HTTP状态码为200 OK
                .andExpect(status().isOk())
                // 验证响应JSON中success字段为true
                .andExpect(jsonPath("$.success").value(true))
                // 验证响应JSON中部门ID为1
                .andExpect(jsonPath("$.data.id").value(1))
                // 验证响应JSON中部门名称为"开发部"
                .andExpect(jsonPath("$.data.name").value("开发部"));

        // 验证departmentService.getDepartmentById(1)方法被调用了一次
        verify(departmentService, times(1)).getDepartmentById(1);
    }

    /**
     * 根据ID获取部门信息（部门不存在）测试
     * 验证当请求不存在的部门ID时返回404错误
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟departmentService.getDepartmentById()方法返回空Optional（表示部门不存在）
     * 3. 发送GET请求到/api/departments/{id}端点（使用不存在的ID）
     * 4. 验证响应状态码为404 Not Found
     * 5. 验证错误信息正确
     * 6. 验证服务方法被正确调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetDepartmentById_NotFound() throws Exception {
        // 设置模拟行为：当调用departmentService.getDepartmentById(999)时返回空Optional
        when(departmentService.getDepartmentById(999)).thenReturn(Optional.empty());

        // 执行GET请求并验证响应
        mockMvc.perform(get("/api/departments/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                // 验证HTTP状态码为404 Not Found
                .andExpect(status().isNotFound())
                // 验证响应JSON中success字段为false
                .andExpect(jsonPath("$.success").value(false))
                // 验证响应JSON中错误信息为"部门不存在"
                .andExpect(jsonPath("$.message").value("部门不存在"));

        // 验证departmentService.getDepartmentById(999)方法被调用了一次
        verify(departmentService, times(1)).getDepartmentById(999);
    }

    /**
     * 创建新部门测试
     * 验证管理员用户可以成功创建新部门
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟departmentService.existsByName()和existsByCode()方法返回false（表示名称和代码都未被使用）
     * 3. 模拟departmentService.createDepartment()方法返回创建的部门对象
     * 4. 发送POST请求到/api/departments端点，包含部门信息
     * 5. 验证响应状态码为201 Created
     * 6. 验证响应数据结构正确
     * 7. 验证服务方法被正确调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateDepartment_Success() throws Exception {
        // 设置模拟行为：
        // 1. 当调用departmentService.createDepartment()时返回测试部门对象
        when(departmentService.createDepartment(any(Department.class))).thenReturn(testDepartment);
        // 2. 当调用departmentService.existsByName("开发部")时返回false（名称未被使用）
        when(departmentService.existsByName("开发部")).thenReturn(false);
        // 3. 当调用departmentService.existsByCode("DEV")时返回false（代码未被使用）
        when(departmentService.existsByCode("DEV")).thenReturn(false);

        // 构造请求JSON数据
        String departmentJson = "{"
                + "\"name\":\"开发部\","
                + "\"code\":\"DEV\","
                + "\"managerId\":1"
                + "}";

        // 执行POST请求并验证响应
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentJson))
                // 验证HTTP状态码为201 Created
                .andExpect(status().isCreated())
                // 验证响应JSON中success字段为true
                .andExpect(jsonPath("$.success").value(true))
                // 验证响应JSON中成功消息为"部门创建成功"
                .andExpect(jsonPath("$.message").value("部门创建成功"))
                // 验证响应JSON中部门名称为"开发部"
                .andExpect(jsonPath("$.data.name").value("开发部"));

        // 验证相关服务方法被正确调用
        verify(departmentService, times(1)).existsByName("开发部");
        verify(departmentService, times(1)).existsByCode("DEV");
        verify(departmentService, times(1)).createDepartment(any(Department.class));
    }

    /**
     * 创建新部门（名称重复）测试
     * 验证当尝试创建名称已存在的部门时返回错误
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟departmentService.existsByName()方法返回true（表示名称已被使用）
     * 3. 发送POST请求到/api/departments端点，包含重复名称的部门信息
     * 4. 验证响应状态码为400 Bad Request
     * 5. 验证错误信息正确
     * 6. 验证createDepartment方法未被调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateDepartment_NameExists() throws Exception {
        // 设置模拟行为：当调用departmentService.existsByName("开发部")时返回true（名称已被使用）
        when(departmentService.existsByName("开发部")).thenReturn(true);

        // 构造请求JSON数据（使用已存在的名称）
        String departmentJson = "{"
                + "\"name\":\"开发部\","
                + "\"code\":\"DEV2\","
                + "\"managerId\":1"
                + "}";

        // 执行POST请求并验证响应
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentJson))
                // 验证HTTP状态码为400 Bad Request
                .andExpect(status().isBadRequest())
                // 验证响应JSON中success字段为false
                .andExpect(jsonPath("$.success").value(false))
                // 验证响应JSON中错误信息为"部门名称已存在"
                .andExpect(jsonPath("$.message").value("部门名称已存在"));

        // 验证相关服务方法被正确调用
        verify(departmentService, times(1)).existsByName("开发部");
        // 验证createDepartment方法未被调用（因为名称重复，应该提前返回错误）
        verify(departmentService, times(0)).createDepartment(any(Department.class));
    }

    /**
     * 创建新部门（代码重复）测试
     * 验证当尝试创建代码已存在的部门时返回错误
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟departmentService.existsByName()方法返回false（名称未被使用）
     * 3. 模拟departmentService.existsByCode()方法返回true（表示代码已被使用）
     * 4. 发送POST请求到/api/departments端点，包含重复代码的部门信息
     * 5. 验证响应状态码为400 Bad Request
     * 6. 验证错误信息正确
     * 7. 验证createDepartment方法未被调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateDepartment_CodeExists() throws Exception {
        // 设置模拟行为：
        // 1. 当调用departmentService.existsByName("开发部2")时返回false（新名称未被使用）
        when(departmentService.existsByName("开发部2")).thenReturn(false);
        // 2. 当调用departmentService.existsByCode("DEV")时返回true（代码已被使用）
        when(departmentService.existsByCode("DEV")).thenReturn(true);

        // 构造请求JSON数据（使用已存在的代码）
        String departmentJson = "{"
                + "\"name\":\"开发部2\","
                + "\"code\":\"DEV\","
                + "\"managerId\":1"
                + "}";

        // 执行POST请求并验证响应
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentJson))
                // 验证HTTP状态码为400 Bad Request
                .andExpect(status().isBadRequest())
                // 验证响应JSON中success字段为false
                .andExpect(jsonPath("$.success").value(false))
                // 验证响应JSON中错误信息为"部门代码已存在"
                .andExpect(jsonPath("$.message").value("部门代码已存在"));

        // 验证相关服务方法被正确调用
        verify(departmentService, times(1)).existsByName("开发部2");
        verify(departmentService, times(1)).existsByCode("DEV");
        // 验证createDepartment方法未被调用（因为代码重复，应该提前返回错误）
        verify(departmentService, times(0)).createDepartment(any(Department.class));
    }

    /**
     * 更新部门信息测试
     * 验证管理员用户可以更新部门信息
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟departmentService.getDepartmentById()方法返回要更新的部门
     * 3. 模拟departmentService.existsByName()方法返回false（新名称未被使用）
     * 4. 模拟departmentService.updateDepartment()方法返回更新后的部门对象
     * 5. 发送PUT请求到/api/departments/{id}端点，包含更新信息
     * 6. 验证响应状态码为200 OK
     * 7. 验证响应数据结构正确
     * 8. 验证服务方法被正确调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateDepartment_Success() throws Exception {
        // 创建更新后的部门对象
        Department updatedDepartment = new Department();
        updatedDepartment.setId(1L);
        updatedDepartment.setName("开发部（更新）");
        updatedDepartment.setCode("DEV");
        updatedDepartment.setManagerId(2);
        updatedDepartment.setCreatedAt(testDepartment.getCreatedAt());
        updatedDepartment.setUpdatedAt(OffsetDateTime.now());

        // 设置模拟行为：
        // 1. 当调用departmentService.getDepartmentById(1)时返回原始部门对象
        when(departmentService.getDepartmentById(1)).thenReturn(Optional.of(testDepartment));
        // 2. 当调用departmentService.updateDepartment()时返回更新后的部门对象
        when(departmentService.updateDepartment(eq(1), any(Department.class))).thenReturn(updatedDepartment);
        // 3. 当调用departmentService.existsByName("开发部（更新）")时返回false（新名称未被使用）
        when(departmentService.existsByName("开发部（更新）")).thenReturn(false);
        // 4. 当调用departmentService.existsByCode("DEV")时返回false（代码未被使用）
        when(departmentService.existsByCode("DEV")).thenReturn(false);

        // 构造更新请求JSON数据
        String updateJson = "{"
                + "\"name\":\"开发部（更新）\","
                + "\"code\":\"DEV\","
                + "\"managerId\":2"
                + "}";

        // 执行PUT请求并验证响应
        mockMvc.perform(put("/api/departments/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                // 验证HTTP状态码为200 OK
                .andExpect(status().isOk())
                // 验证响应JSON中success字段为true
                .andExpect(jsonPath("$.success").value(true))
                // 验证响应JSON中成功消息为"部门信息更新成功"
                .andExpect(jsonPath("$.message").value("部门信息更新成功"))
                // 验证响应JSON中更新后的部门名称正确
                .andExpect(jsonPath("$.data.name").value("开发部（更新）"));

        // 验证相关服务方法被正确调用
        verify(departmentService, times(1)).getDepartmentById(1);
        verify(departmentService, times(1)).existsByName("开发部（更新）");
        verify(departmentService, times(1)).updateDepartment(eq(1), any(Department.class));
    }

    /**
     * 更新部门信息（部门不存在）测试
     * 验证当尝试更新不存在的部门时返回404错误
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟departmentService.getDepartmentById()方法返回空Optional（表示部门不存在）
     * 3. 发送PUT请求到/api/departments/{id}端点（使用不存在的ID）
     * 4. 验证响应状态码为404 Not Found
     * 5. 验证错误信息正确
     * 6. 验证updateDepartment方法未被调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateDepartment_NotFound() throws Exception {
        // 设置模拟行为：当调用departmentService.getDepartmentById(999)时返回空Optional
        when(departmentService.getDepartmentById(999)).thenReturn(Optional.empty());

        // 构造更新请求JSON数据
        String updateJson = "{"
                + "\"name\":\"开发部（更新）\","
                + "\"code\":\"DEV\","
                + "\"managerId\":2"
                + "}";

        // 执行PUT请求并验证响应
        mockMvc.perform(put("/api/departments/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                // 验证HTTP状态码为404 Not Found
                .andExpect(status().isNotFound())
                // 验证响应JSON中success字段为false
                .andExpect(jsonPath("$.success").value(false))
                // 验证响应JSON中错误信息为"部门不存在"
                .andExpect(jsonPath("$.message").value("部门不存在"));

        // 验证相关服务方法被正确调用
        verify(departmentService, times(1)).getDepartmentById(999);
        // 验证updateDepartment方法未被调用（因为部门不存在，应该提前返回错误）
        verify(departmentService, times(0)).updateDepartment(anyInt(), any(Department.class));
    }

    /**
     * 删除部门测试
     * 验证管理员用户可以删除部门
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟departmentService.getDepartmentById()方法返回要删除的部门
     * 3. 发送DELETE请求到/api/departments/{id}端点
     * 4. 验证响应状态码为200 OK
     * 5. 验证成功消息正确
     * 6. 验证服务方法被正确调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteDepartment_Success() throws Exception {
        // 设置模拟行为：当调用departmentService.getDepartmentById(1)时返回测试部门对象
        when(departmentService.getDepartmentById(1)).thenReturn(Optional.of(testDepartment));

        // 执行DELETE请求并验证响应
        mockMvc.perform(delete("/api/departments/{id}", 1L))
                // 验证HTTP状态码为200 OK
                .andExpect(status().isOk())
                // 验证响应JSON中success字段为true
                .andExpect(jsonPath("$.success").value(true))
                // 验证响应JSON中成功消息为"部门删除成功"
                .andExpect(jsonPath("$.message").value("部门删除成功"));

        // 验证相关服务方法被正确调用
        verify(departmentService, times(1)).getDepartmentById(1);
        verify(departmentService, times(1)).deleteDepartment(1);
    }

    /**
     * 删除部门（部门不存在）测试
     * 验证当尝试删除不存在的部门时返回404错误
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟departmentService.getDepartmentById()方法返回空Optional（表示部门不存在）
     * 3. 发送DELETE请求到/api/departments/{id}端点（使用不存在的ID）
     * 4. 验证响应状态码为404 Not Found
     * 5. 验证错误信息正确
     * 6. 验证deleteDepartment方法未被调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteDepartment_NotFound() throws Exception {
        // 设置模拟行为：当调用departmentService.getDepartmentById(999)时返回空Optional
        when(departmentService.getDepartmentById(999)).thenReturn(Optional.empty());

        // 执行DELETE请求并验证响应
        mockMvc.perform(delete("/api/departments/{id}", 999L))
                // 验证HTTP状态码为404 Not Found
                .andExpect(status().isNotFound())
                // 验证响应JSON中success字段为false
                .andExpect(jsonPath("$.success").value(false))
                // 验证响应JSON中错误信息为"部门不存在"
                .andExpect(jsonPath("$.message").value("部门不存在"));

        // 验证相关服务方法被正确调用
        verify(departmentService, times(1)).getDepartmentById(999);
        // 验证deleteDepartment方法未被调用（因为部门不存在，应该提前返回错误）
        verify(departmentService, times(0)).deleteDepartment(anyInt());
    }

    /**
     * 无权限用户访问测试
     * 验证普通用户无法访问部门管理API
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "USER")模拟普通用户（非管理员）
     * 2. 发送GET请求到/api/departments端点
     * 3. 验证响应状态码为403 Forbidden（权限不足）
     * 4. 验证安全配置正确生效
     */
    @Test
    @WithMockUser(roles = "USER")
    void testAccessDenied_ForNonAdminUser() throws Exception {
        // 执行GET请求并验证响应
        mockMvc.perform(get("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON))
                // 验证HTTP状态码为403 Forbidden
                .andExpect(status().isForbidden());
    }
}