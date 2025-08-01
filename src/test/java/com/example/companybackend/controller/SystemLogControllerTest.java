package com.example.companybackend.controller;

import com.example.companybackend.entity.SystemLog;
import com.example.companybackend.repository.SystemLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 系统日志控制器测试类
 * 
 * 测试目标：
 * - 测试文件：SystemLogController.java
 * - 测试类：com.example.companybackend.controller.SystemLogController
 * - 模拟依赖：SystemLogRepository（系统日志仓库类）
 * 
 * 测试规范和技巧：
 * 1. 使用@WebMvcTest注解仅加载Web层相关组件，提高测试效率
 * 2. 使用@MockBean模拟服务层依赖，隔离测试
 * 3. 使用@WithMockUser模拟用户认证信息
 * 4. 遵循Given-When-Then测试模式
 * 5. 对每个API端点编写成功和失败场景的测试用例
 * 6. 验证HTTP状态码、响应结构和关键数据
 */
@WebMvcTest(SystemLogController.class)
@ContextConfiguration(classes = {SystemLogController.class, SystemLogControllerTest.TestSecurityConfig.class})
public class SystemLogControllerTest {

    /**
     * MockMvc是Spring MVC测试用的客户端
     * HTTP请求的模拟和响应验证都通过它来完成
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * SystemLogRepository的模拟对象
     * 用于模拟数据访问层，隔离测试控制器逻辑
     */
    @MockBean
    private SystemLogRepository systemLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private SystemLog testSystemLog;

    /**
     * 测试用的安全配置类
     * 禁用CSRF保护，允许所有请求通过
     */
    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/system-logs/**").hasRole("ADMIN")
                    .anyRequest().permitAll()
                );
            return http.build();
        }
    }

    /**
     * 测试前准备：创建示例系统日志对象
     */
    @BeforeEach
    void setUp() {
        testSystemLog = new SystemLog();
        testSystemLog.setId(1L);
        testSystemLog.setUserId(1);
        testSystemLog.setAction("LOGIN");
        testSystemLog.setStatus("success");
        testSystemLog.setIpAddress("192.168.1.1");
        testSystemLog.setUserAgent("Mozilla/5.0");
        testSystemLog.setDetails("Login successful");
        testSystemLog.setCreatedAt(OffsetDateTime.now());
    }

    /**
     * 测试用例：成功获取系统日志列表
     * 
     * 测试目标方法：
     * - SystemLogController.getSystemLogs()
     * 
     * 测试场景：
     * - 管理员用户请求获取系统日志列表
     * - 仓库层成功返回日志列表
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应包含成功消息"システムログ一覧の取得が完了しました"
     * - 响应数据包含日志列表
     * - 日志列表中包含示例日志的ID和动作
     * 
     * 模拟的依赖方法：
     * - SystemLogRepository.findAll()
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetSystemLogs_Success() throws Exception {
        // Given
        List<SystemLog> logs = Arrays.asList(testSystemLog);
        Page<SystemLog> logsPage = new PageImpl<>(logs);
        when(systemLogRepository.findAll(any(Pageable.class))).thenReturn(logsPage);

        // When & Then
        mockMvc.perform(get("/api/system-logs")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("システムログ一覧の取得が完了しました"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].action").value("LOGIN"))
                .andExpect(jsonPath("$.data[0].status").value("success"));

        // 验证相关服务方法被正确调用
        verify(systemLogRepository, times(1)).findAll(any(Pageable.class));
    }

    /**
     * 测试用例：成功获取系统日志详情
     * 
     * 测试目标方法：
     * - SystemLogController.getSystemLogById()
     * 
     * 测试场景：
     * - 管理员用户请求获取指定ID的系统日志详情
     * - 仓库层成功返回该日志对象
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应包含成功消息"システムログ詳細の取得が完了しました"
     * - 响应数据包含日志ID和动作
     * 
     * 模拟的依赖方法：
     * - SystemLogRepository.findById()
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetSystemLogById_Success() throws Exception {
        // Given
        when(systemLogRepository.findById(1)).thenReturn(Optional.of(testSystemLog));

        // When & Then
        mockMvc.perform(get("/api/system-logs/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("システムログ詳細の取得が完了しました"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.action").value("LOGIN"))
                .andExpect(jsonPath("$.data.status").value("success"));

        // 验证相关服务方法被正确调用
        verify(systemLogRepository, times(1)).findById(1);
    }

    /**
     * 测试用例：获取不存在的系统日志详情
     * 
     * 测试目标方法：
     * - SystemLogController.getSystemLogById()
     * 
     * 测试场景：
     * - 管理员用户请求获取不存在的系统日志详情
     * - 仓库层返回空Optional对象
     * 
     * 预期结果：
     * - HTTP状态码：404 NOT FOUND
     * - 响应包含success=false
     * - 响应包含错误消息"指定されたシステムログが見つかりません"
     * 
     * 模拟的依赖方法：
     * - SystemLogRepository.findById()
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetSystemLogById_NotFound() throws Exception {
        // Given
        when(systemLogRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/system-logs/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("指定されたシステムログが見つかりません"));

        // 验证相关服务方法被正确调用
        verify(systemLogRepository, times(1)).findById(999);
    }

    /**
     * 测试用例：成功删除系统日志
     * 
     * 测试目标方法：
     * - SystemLogController.deleteSystemLog()
     * 
     * 测试场景：
     * - 管理员用户请求删除指定ID的系统日志
     * - 仓库层确认该日志存在并成功删除
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应包含成功消息"システムログを削除しました"
     * 
     * 模拟的依赖方法：
     * - SystemLogRepository.findById()
     * - SystemLogRepository.deleteById()
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteSystemLog_Success() throws Exception {
        // Given
        when(systemLogRepository.findById(1)).thenReturn(Optional.of(testSystemLog));

        // When & Then
        mockMvc.perform(delete("/api/system-logs/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("システムログを削除しました"));

        // 验证相关服务方法被正确调用
        verify(systemLogRepository, times(1)).findById(1);
        verify(systemLogRepository, times(1)).deleteById(1);
    }

    /**
     * 测试用例：删除不存在的系统日志
     * 
     * 测试目标方法：
     * - SystemLogController.deleteSystemLog()
     * 
     * 测试场景：
     * - 管理员用户请求删除不存在的系统日志
     * - 仓库层返回空Optional对象
     * 
     * 预期结果：
     * - HTTP状态码：404 NOT FOUND
     * - 响应包含success=false
     * - 响应包含错误消息"指定されたシステムログが見つかりません"
     * - deleteById方法未被调用
     * 
     * 模拟的依赖方法：
     * - SystemLogRepository.findById()
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteSystemLog_NotFound() throws Exception {
        // Given
        when(systemLogRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/system-logs/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("指定されたシステムログが見つかりません"));

        // 验证相关服务方法被正确调用
        verify(systemLogRepository, times(1)).findById(999);
        // 验证deleteById方法未被调用（因为日志不存在，应该提前返回错误）
        verify(systemLogRepository, times(0)).deleteById(anyInt());
    }

    /**
     * 测试用例：成功导出系统日志（CSV格式）
     * 
     * 测试目标方法：
     * - SystemLogController.exportSystemLogsCsv()
     * 
     * 测试场景：
     * - 管理员用户请求以CSV格式导出系统日志
     * - 仓库层成功返回日志列表
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应Content-Type为text/csv;charset=UTF-8
     * - 响应体包含CSV格式的日志数据
     * 
     * 模拟的依赖方法：
     * - SystemLogRepository.findForExport()
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testExportSystemLogsCsv_Success() throws Exception {
        // Given
        List<SystemLog> logs = Arrays.asList(testSystemLog);
        when(systemLogRepository.findForExport(any(), any(), any(), any())).thenReturn(logs);

        // When & Then
        mockMvc.perform(get("/api/system-logs/export/csv")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ID,ユーザーID,アクション,ステータス")));

        // 验证相关服务方法被正确调用
        verify(systemLogRepository, times(1)).findForExport(any(), any(), any(), any());
    }

    /**
     * 测试用例：成功导出系统日志（JSON格式）
     * 
     * 测试目标方法：
     * - SystemLogController.exportSystemLogsJson()
     * 
     * 测试场景：
     * - 管理员用户请求以JSON格式导出系统日志
     * - 仓库层成功返回日志列表
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应包含成功消息"システムログのエクスポートが完了しました"
     * - 响应数据包含日志列表
     * 
     * 模拟的依赖方法：
     * - SystemLogRepository.findForExport()
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testExportSystemLogsJson_Success() throws Exception {
        // Given
        List<SystemLog> logs = Arrays.asList(testSystemLog);
        when(systemLogRepository.findForExport(any(), any(), any(), any())).thenReturn(logs);

        // When & Then
        mockMvc.perform(get("/api/system-logs/export/json")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("システムログのエクスポートが完了しました"))
                .andExpect(jsonPath("$.data").isArray());

        // 验证相关服务方法被正确调用
        verify(systemLogRepository, times(1)).findForExport(any(), any(), any(), any());
    }

    /**
     * 测试用例：成功获取系统日志统计信息
     * 
     * 测试目标方法：
     * - SystemLogController.getSystemLogStatistics()
     * 
     * 测试场景：
     * - 管理员用户请求获取系统日志统计信息
     * - 仓库层成功返回各类统计数据
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应包含成功消息"システムログ統計情報の取得が完了しました"
     * - 响应数据包含各类统计信息
     * 
     * 模拟的依赖方法：
     * - SystemLogRepository.countByActionGrouped()
     * - SystemLogRepository.countByStatusGrouped()
     * - SystemLogRepository.countByUserGrouped()
     * - SystemLogRepository.countByDateGrouped()
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetSystemLogStatistics_Success() throws Exception {
        // Given
        List<Map<String, Object>> stats = new ArrayList<>();
        Map<String, Object> stat = new HashMap<>();
        stat.put("action", "LOGIN");
        stat.put("count", 5);
        stats.add(stat);
        
        when(systemLogRepository.countByActionGrouped()).thenReturn(stats);
        when(systemLogRepository.countByStatusGrouped()).thenReturn(stats);
        when(systemLogRepository.countByUserGrouped()).thenReturn(stats);
        when(systemLogRepository.countByDateGrouped()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/system-logs/statistics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("システムログ統計情報の取得が完了しました"))
                .andExpect(jsonPath("$.data.actionStats").isArray())
                .andExpect(jsonPath("$.data.statusStats").isArray());

        // 验证相关服务方法被正确调用
        verify(systemLogRepository, times(1)).countByActionGrouped();
        verify(systemLogRepository, times(1)).countByStatusGrouped();
        verify(systemLogRepository, times(1)).countByUserGrouped();
        verify(systemLogRepository, times(1)).countByDateGrouped();
    }

    /**
     * 测试用例：成功搜索系统日志
     * 
     * 测试目标方法：
     * - SystemLogController.searchSystemLogs()
     * 
     * 测试场景：
     * - 管理员用户使用关键词搜索系统日志
     * - 仓库层成功返回匹配的日志列表
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应包含成功消息"システムログの検索が完了しました"
     * - 响应数据包含日志列表
     * - 响应包含搜索关键词
     * 
     * 模拟的依赖方法：
     * - SystemLogRepository.searchByKeyword()
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testSearchSystemLogs_Success() throws Exception {
        // Given
        List<SystemLog> logs = Arrays.asList(testSystemLog);
        Page<SystemLog> logsPage = new PageImpl<>(logs);
        when(systemLogRepository.searchByKeyword(eq("LOGIN"), any(Pageable.class))).thenReturn(logsPage);

        // When & Then
        mockMvc.perform(get("/api/system-logs/search")
                .param("keyword", "LOGIN")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("システムログの検索が完了しました"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));

        // 验证相关服务方法被正确调用
        verify(systemLogRepository, times(1)).searchByKeyword(eq("LOGIN"), any(Pageable.class));
    }

    /**
     * 测试用例：分页参数边界值测试
     * 
     * 测试目标方法：
     * - SystemLogController.getSystemLogs()
     * 
     * 测试场景：
     * - 管理员用户使用边界值分页参数请求系统日志列表
     * - 控制器应正确处理边界值并返回日志列表
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * 
     * 模拟的依赖方法：
     * - SystemLogRepository.findAll()
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetSystemLogs_WithBoundaryPageParameters() throws Exception {
        // Given
        List<SystemLog> logs = Arrays.asList(testSystemLog);
        Page<SystemLog> logsPage = new PageImpl<>(logs);
        when(systemLogRepository.findAll(any(Pageable.class))).thenReturn(logsPage);

        // When & Then
        mockMvc.perform(get("/api/system-logs")
                .param("page", "-1")  // 边界值测试: 负的页码
                .param("size", "0")   // 边界值测试: 大小为0
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证相关服务方法被正确调用
        verify(systemLogRepository, times(1)).findAll(any(Pageable.class));
    }

    /**
     * 测试用例：非管理员用户访问拒绝测试
     * 
     * 测试目标方法：
     * - SystemLogController.getSystemLogs()
     * 
     * 测试场景：
     * - 普通用户尝试访问需要管理员权限的系统日志接口
     * 
     * 预期结果：
     * - HTTP状态码：403 FORBIDDEN
     */
    @Test
    @WithMockUser(roles = "USER")  // 非管理员角色
    void testAccessDenied_ForNonAdminUser() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system-logs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}