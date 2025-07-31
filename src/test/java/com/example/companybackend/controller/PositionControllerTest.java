package com.example.companybackend.controller;

import com.example.companybackend.entity.Position;
import com.example.companybackend.service.PositionService;
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
 * 职位控制器测试类
 * 
 * 该测试类使用@WebMvcTest注解，专门用于测试Spring MVC控制器层
 * 它启动一个轻量级的Spring上下文，只包含Web层相关的组件
 */
@WebMvcTest(PositionController.class)
@ContextConfiguration(classes = {PositionController.class, PositionControllerTest.TestSecurityConfig.class})
public class PositionControllerTest {

    // 注入MockMvc对象，用于模拟HTTP请求
    @Autowired
    private MockMvc mockMvc;

    // 使用@MockBean创建PositionService的模拟对象
    // 这样可以在不依赖真实服务的情况下测试控制器逻辑
    @MockBean
    private PositionService positionService;

    // 测试数据：单个职位对象
    private Position testPosition;
    // 测试数据：职位列表
    private List<Position> testPositions;

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
                    // 配置职位相关API需要ADMIN角色才能访问
                    .requestMatchers("/api/positions/**").hasRole("ADMIN")
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
        // 创建测试用的职位对象1
        testPosition = new Position();
        testPosition.setId(1L);
        testPosition.setName("Manager");
        testPosition.setLevel(5);
        testPosition.setCreatedAt(OffsetDateTime.now());
        testPosition.setUpdatedAt(OffsetDateTime.now());

        // 创建测试用的职位对象2
        Position position2 = new Position();
        position2.setId(2L);
        position2.setName("Developer");
        position2.setLevel(3);
        position2.setCreatedAt(OffsetDateTime.now());
        position2.setUpdatedAt(OffsetDateTime.now());

        // 将两个职位对象放入列表中
        testPositions = Arrays.asList(testPosition, position2);
    }

    /**
     * 职位列表获取测试
     * 验证管理员用户可以成功获取职位列表
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟positionService.getAllPositions()方法返回测试数据
     * 3. 发送GET请求到/api/positions端点
     * 4. 验证响应状态码为200 OK
     * 5. 验证响应数据结构正确
     * 6. 验证服务方法被正确调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllPositions_Success() throws Exception {
        // 设置模拟行为：当调用positionService.getAllPositions()时返回测试数据
        when(positionService.getAllPositions()).thenReturn(testPositions);

        // 执行GET请求并验证响应
        mockMvc.perform(get("/api/positions")
                        .contentType(MediaType.APPLICATION_JSON))
                // 验证HTTP状态码为200 OK
                .andExpect(status().isOk())
                // 验证响应JSON中success字段为true
                .andExpect(jsonPath("$.success").value(true))
                // 验证响应JSON中data数组长度为2
                .andExpect(jsonPath("$.data.length()").value(2))
                // 验证响应JSON中第一个职位名称为"Manager"
                .andExpect(jsonPath("$.data[0].name").value("Manager"));

        // 验证positionService.getAllPositions()方法被调用了一次
        verify(positionService, times(1)).getAllPositions();
    }

    /**
     * 根据ID获取职位信息测试
     * 验证管理员用户可以获取特定职位信息
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟positionService.getPositionById()方法返回指定ID的职位
     * 3. 发送GET请求到/api/positions/{id}端点
     * 4. 验证响应状态码为200 OK
     * 5. 验证响应数据结构正确
     * 6. 验证服务方法被正确调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetPositionById_Success() throws Exception {
        // 设置模拟行为：当调用positionService.getPositionById(1)时返回测试职位对象
        when(positionService.getPositionById(1)).thenReturn(Optional.of(testPosition));

        // 执行GET请求并验证响应
        mockMvc.perform(get("/api/positions/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                // 验证HTTP状态码为200 OK
                .andExpect(status().isOk())
                // 验证响应JSON中success字段为true
                .andExpect(jsonPath("$.success").value(true))
                // 验证响应JSON中职位ID为1
                .andExpect(jsonPath("$.data.id").value(1))
                // 验证响应JSON中职位名称为"Manager"
                .andExpect(jsonPath("$.data.name").value("Manager"));

        // 验证positionService.getPositionById(1)方法被调用了一次
        verify(positionService, times(1)).getPositionById(1);
    }

    /**
     * 根据ID获取职位信息（职位不存在）测试
     * 验证当请求不存在的职位ID时返回404错误
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟positionService.getPositionById()方法返回空Optional（表示职位不存在）
     * 3. 发送GET请求到/api/positions/{id}端点（使用不存在的ID）
     * 4. 验证响应状态码为404 Not Found
     * 5. 验证错误信息正确
     * 6. 验证服务方法被正确调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetPositionById_NotFound() throws Exception {
        // 设置模拟行为：当调用positionService.getPositionById(999)时返回空Optional
        when(positionService.getPositionById(999)).thenReturn(Optional.empty());

        // 执行GET请求并验证响应
        mockMvc.perform(get("/api/positions/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                // 验证HTTP状态码为404 Not Found
                .andExpect(status().isNotFound())
                // 验证响应JSON中success字段为false
                .andExpect(jsonPath("$.success").value(false))
                // 验证响应JSON中错误信息为"职位不存在"
                .andExpect(jsonPath("$.message").value("职位不存在"));

        // 验证positionService.getPositionById(999)方法被调用了一次
        verify(positionService, times(1)).getPositionById(999);
    }

    /**
     * 创建新职位测试
     * 验证管理员用户可以成功创建新职位
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟positionService.existsByName()方法返回false（表示名称未被使用）
     * 3. 模拟positionService.createPosition()方法返回创建的职位对象
     * 4. 发送POST请求到/api/positions端点，包含职位信息
     * 5. 验证响应状态码为201 Created
     * 6. 验证响应数据结构正确
     * 7. 验证服务方法被正确调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreatePosition_Success() throws Exception {
        // 设置模拟行为：
        // 1. 当调用positionService.createPosition()时返回测试职位对象
        when(positionService.createPosition(any(Position.class))).thenReturn(testPosition);
        // 2. 当调用positionService.existsByName("Manager")时返回false（名称未被使用）
        when(positionService.existsByName("Manager")).thenReturn(false);

        // 构造请求JSON数据
        String positionJson = "{"
                + "\"name\":\"Manager\","
                + "\"level\":5"
                + "}";

        // 执行POST请求并验证响应
        mockMvc.perform(post("/api/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(positionJson))
                // 验证HTTP状态码为201 Created
                .andExpect(status().isCreated())
                // 验证响应JSON中success字段为true
                .andExpect(jsonPath("$.success").value(true))
                // 验证响应JSON中成功消息为"职位创建成功"
                .andExpect(jsonPath("$.message").value("职位创建成功"))
                // 验证响应JSON中职位名称为"Manager"
                .andExpect(jsonPath("$.data.name").value("Manager"));

        // 验证相关服务方法被正确调用
        verify(positionService, times(1)).existsByName("Manager");
        verify(positionService, times(1)).createPosition(any(Position.class));
    }

    /**
     * 创建新职位（名称重复）测试
     * 验证当尝试创建名称已存在的职位时返回错误
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟positionService.existsByName()方法返回true（表示名称已被使用）
     * 3. 发送POST请求到/api/positions端点，包含重复名称的职位信息
     * 4. 验证响应状态码为400 Bad Request
     * 5. 验证错误信息正确
     * 6. 验证createPosition方法未被调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreatePosition_NameExists() throws Exception {
        // 设置模拟行为：当调用positionService.existsByName("Manager")时返回true（名称已被使用）
        when(positionService.existsByName("Manager")).thenReturn(true);

        // 构造请求JSON数据（使用已存在的名称）
        String positionJson = "{"
                + "\"name\":\"Manager\","
                + "\"level\":5"
                + "}";

        // 执行POST请求并验证响应
        mockMvc.perform(post("/api/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(positionJson))
                // 验证HTTP状态码为400 Bad Request
                .andExpect(status().isBadRequest())
                // 验证响应JSON中success字段为false
                .andExpect(jsonPath("$.success").value(false))
                // 验证响应JSON中错误信息为"职位名称已存在"
                .andExpect(jsonPath("$.message").value("职位名称已存在"));

        // 验证相关服务方法被正确调用
        verify(positionService, times(1)).existsByName("Manager");
        // 验证createPosition方法未被调用（因为名称重复，应该提前返回错误）
        verify(positionService, times(0)).createPosition(any(Position.class));
    }

    /**
     * 更新职位信息测试
     * 验证管理员用户可以更新职位信息
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟positionService.getPositionById()方法返回要更新的职位
     * 3. 模拟positionService.existsByName()方法返回false（新名称未被使用）
     * 4. 模拟positionService.updatePosition()方法返回更新后的职位对象
     * 5. 发送PUT请求到/api/positions/{id}端点，包含更新信息
     * 6. 验证响应状态码为200 OK
     * 7. 验证响应数据结构正确
     * 8. 验证服务方法被正确调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdatePosition_Success() throws Exception {
        // 创建更新后的职位对象
        Position updatedPosition = new Position();
        updatedPosition.setId(1L);
        updatedPosition.setName("Senior Manager");
        updatedPosition.setLevel(6);
        updatedPosition.setCreatedAt(testPosition.getCreatedAt());
        updatedPosition.setUpdatedAt(OffsetDateTime.now());

        // 设置模拟行为：
        // 1. 当调用positionService.getPositionById(1)时返回原始职位对象
        when(positionService.getPositionById(1)).thenReturn(Optional.of(testPosition));
        // 2. 当调用positionService.updatePosition()时返回更新后的职位对象
        when(positionService.updatePosition(eq(1), any(Position.class))).thenReturn(updatedPosition);
        // 3. 当调用positionService.existsByName("Senior Manager")时返回false（新名称未被使用）
        when(positionService.existsByName("Senior Manager")).thenReturn(false);

        // 构造更新请求JSON数据
        String updateJson = "{"
                + "\"name\":\"Senior Manager\","
                + "\"level\":6"
                + "}";

        // 执行PUT请求并验证响应
        mockMvc.perform(put("/api/positions/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                // 验证HTTP状态码为200 OK
                .andExpect(status().isOk())
                // 验证响应JSON中success字段为true
                .andExpect(jsonPath("$.success").value(true))
                // 验证响应JSON中成功消息为"职位信息更新成功"
                .andExpect(jsonPath("$.message").value("职位信息更新成功"))
                // 验证响应JSON中更新后的职位名称正确
                .andExpect(jsonPath("$.data.name").value("Senior Manager"));

        // 验证相关服务方法被正确调用
        verify(positionService, times(1)).getPositionById(1);
        verify(positionService, times(1)).existsByName("Senior Manager");
        verify(positionService, times(1)).updatePosition(eq(1), any(Position.class));
    }

    /**
     * 更新职位信息（职位不存在）测试
     * 验证当尝试更新不存在的职位时返回404错误
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟positionService.getPositionById()方法返回空Optional（表示职位不存在）
     * 3. 发送PUT请求到/api/positions/{id}端点（使用不存在的ID）
     * 4. 验证响应状态码为404 Not Found
     * 5. 验证错误信息正确
     * 6. 验证updatePosition方法未被调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdatePosition_NotFound() throws Exception {
        // 设置模拟行为：当调用positionService.getPositionById(999)时返回空Optional
        when(positionService.getPositionById(999)).thenReturn(Optional.empty());

        // 构造更新请求JSON数据
        String updateJson = "{"
                + "\"name\":\"Senior Manager\","
                + "\"level\":6"
                + "}";

        // 执行PUT请求并验证响应
        mockMvc.perform(put("/api/positions/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                // 验证HTTP状态码为404 Not Found
                .andExpect(status().isNotFound())
                // 验证响应JSON中success字段为false
                .andExpect(jsonPath("$.success").value(false))
                // 验证响应JSON中错误信息为"职位不存在"
                .andExpect(jsonPath("$.message").value("职位不存在"));

        // 验证相关服务方法被正确调用
        verify(positionService, times(1)).getPositionById(999);
        // 验证updatePosition方法未被调用（因为职位不存在，应该提前返回错误）
        verify(positionService, times(0)).updatePosition(anyInt(), any(Position.class));
    }

    /**
     * 删除职位测试
     * 验证管理员用户可以删除职位
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟positionService.getPositionById()方法返回要删除的职位
     * 3. 发送DELETE请求到/api/positions/{id}端点
     * 4. 验证响应状态码为200 OK
     * 5. 验证成功消息正确
     * 6. 验证服务方法被正确调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeletePosition_Success() throws Exception {
        // 设置模拟行为：当调用positionService.getPositionById(1)时返回测试职位对象
        when(positionService.getPositionById(1)).thenReturn(Optional.of(testPosition));

        // 执行DELETE请求并验证响应
        mockMvc.perform(delete("/api/positions/{id}", 1L))
                // 验证HTTP状态码为200 OK
                .andExpect(status().isOk())
                // 验证响应JSON中success字段为true
                .andExpect(jsonPath("$.success").value(true))
                // 验证响应JSON中成功消息为"职位删除成功"
                .andExpect(jsonPath("$.message").value("职位删除成功"));

        // 验证相关服务方法被正确调用
        verify(positionService, times(1)).getPositionById(1);
        verify(positionService, times(1)).deletePosition(1);
    }

    /**
     * 删除职位（职位不存在）测试
     * 验证当尝试删除不存在的职位时返回404错误
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "ADMIN")模拟管理员用户
     * 2. 模拟positionService.getPositionById()方法返回空Optional（表示职位不存在）
     * 3. 发送DELETE请求到/api/positions/{id}端点（使用不存在的ID）
     * 4. 验证响应状态码为404 Not Found
     * 5. 验证错误信息正确
     * 6. 验证deletePosition方法未被调用
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeletePosition_NotFound() throws Exception {
        // 设置模拟行为：当调用positionService.getPositionById(999)时返回空Optional
        when(positionService.getPositionById(999)).thenReturn(Optional.empty());

        // 执行DELETE请求并验证响应
        mockMvc.perform(delete("/api/positions/{id}", 999L))
                // 验证HTTP状态码为404 Not Found
                .andExpect(status().isNotFound())
                // 验证响应JSON中success字段为false
                .andExpect(jsonPath("$.success").value(false))
                // 验证响应JSON中错误信息为"职位不存在"
                .andExpect(jsonPath("$.message").value("职位不存在"));

        // 验证相关服务方法被正确调用
        verify(positionService, times(1)).getPositionById(999);
        // 验证deletePosition方法未被调用（因为职位不存在，应该提前返回错误）
        verify(positionService, times(0)).deletePosition(anyInt());
    }

    /**
     * 无权限用户访问测试
     * 验证普通用户无法访问职位管理API
     * 
     * 测试要点：
     * 1. 使用@WithMockUser(roles = "USER")模拟普通用户（非管理员）
     * 2. 发送GET请求到/api/positions端点
     * 3. 验证响应状态码为403 Forbidden（权限不足）
     * 4. 验证安全配置正确生效
     */
    @Test
    @WithMockUser(roles = "USER")
    void testAccessDenied_ForNonAdminUser() throws Exception {
        // 执行GET请求并验证响应
        mockMvc.perform(get("/api/positions")
                        .contentType(MediaType.APPLICATION_JSON))
                // 验证HTTP状态码为403 Forbidden
                .andExpect(status().isForbidden());
    }
}