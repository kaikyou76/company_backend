package com.example.companybackend.controller;

import com.example.companybackend.entity.LeaveRequest;
import com.example.companybackend.service.LeaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 休假请求控制器测试类
 * 
 * 测试目标：
 * - 测试文件：LeaveRequestController.java
 * - 测试类：com.example.companybackend.controller.LeaveRequestController
 * - 模拟依赖：LeaveService（休假服务类）
 * 
 * 测试规范和技巧：
 * 1. 使用@WebMvcTest注解仅加载Web层相关组件，提高测试效率
 * 2. 使用@MockBean模拟服务层依赖，隔离测试
 * 3. 使用@WithMockUser模拟用户认证信息
 * 4. 遵循Given-When-Then测试模式
 * 5. 对每个API端点编写成功和失败场景的测试用例
 * 6. 验证HTTP状态码、响应结构和关键数据
 */
@WebMvcTest(LeaveRequestController.class)
@ContextConfiguration(classes = {LeaveRequestController.class, LeaveRequestControllerTest.TestSecurityConfig.class})
public class LeaveRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaveService leaveService;

    @Autowired
    private ObjectMapper objectMapper;

    private LeaveRequest sampleLeaveRequest;

    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .build();
        }
    }

    /**
     * 测试前准备：创建示例休假请求对象
     */
    @BeforeEach
    void setUp() {
        sampleLeaveRequest = new LeaveRequest();
        sampleLeaveRequest.setId(1L);
        sampleLeaveRequest.setUserId(15); // 普通员工
        sampleLeaveRequest.setType("paid");
        sampleLeaveRequest.setStatus("pending");
        sampleLeaveRequest.setStartDate(LocalDate.of(2023, 6, 1));
        sampleLeaveRequest.setEndDate(LocalDate.of(2023, 6, 3));
        sampleLeaveRequest.setReason("旅行のため");
        sampleLeaveRequest.setCreatedAt(OffsetDateTime.now());
        sampleLeaveRequest.setUpdatedAt(OffsetDateTime.now());
    }

    /**
     * 测试用例：成功申请休假
     * 
     * 测试目标方法：
     * - LeaveRequestController.requestLeave()
     * 
     * 测试场景：
     * - 员工提交有效的休假申请
     * - 服务层成功处理并返回休假请求对象
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应包含成功消息
     * - 响应数据包含休假请求ID和类型
     * 
     * 模拟的依赖方法：
     * - LeaveService.createLeaveRequest()
     */
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testRequestLeave_Success() throws Exception {
        // Given
        LeaveRequestController.LeaveRequestRequest request = new LeaveRequestController.LeaveRequestRequest();
        request.setLeaveType("paid");
        request.setStartDate(LocalDate.of(2023, 6, 1));
        request.setEndDate(LocalDate.of(2023, 6, 3));
        request.setReason("旅行のため");

        when(leaveService.createLeaveRequest(anyLong(), anyString(), any(), any(), anyString()))
                .thenReturn(sampleLeaveRequest);

        // When & Then
        mockMvc.perform(post("/api/leave/request")
                .header("X-User-Id", 15L) // 普通员工ID
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("休暇申請を作成しました"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.leaveType").value("paid"));
    }

    /**
     * 测试用例：休假申请验证错误
     * 
     * 测试目标方法：
     * - LeaveRequestController.requestLeave()
     * 
     * 测试场景：
     * - 员工提交无效的休假类型
     * - 服务层抛出IllegalStateException异常
     * 
     * 预期结果：
     * - HTTP状态码：400 Bad Request
     * - 响应包含success=false
     * - 响应包含错误消息
     * 
     * 模拟的依赖方法：
     * - LeaveService.createLeaveRequest()
     */
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testRequestLeave_ValidationError() throws Exception {
        // Given
        LeaveRequestController.LeaveRequestRequest request = new LeaveRequestController.LeaveRequestRequest();
        request.setLeaveType("invalid_type");
        request.setStartDate(LocalDate.of(2023, 6, 1));
        request.setEndDate(LocalDate.of(2023, 6, 3));
        request.setReason("旅行のため");

        when(leaveService.createLeaveRequest(anyLong(), anyString(), any(), any(), anyString()))
                .thenThrow(new IllegalStateException("無効な休暇タイプです: invalid_type"));

        // When & Then
        mockMvc.perform(post("/api/leave/request")
                .header("X-User-Id", 15L) // 普通员工ID
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("無効な休暇タイプです: invalid_type"));
    }

    /**
     * 测试用例：成功获取休假请求列表
     * 
     * 测试目标方法：
     * - LeaveRequestController.getLeaveRequests()
     * 
     * 测试场景：
     * - 员工查询指定时间段内的休假请求
     * - 服务层返回休假请求列表
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应数据包含休假请求列表
     * - 列表中包含示例休假请求的ID和类型
     * 
     * 模拟的依赖方法：
     * - LeaveService.getUserLeaveRequests()
     */
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetLeaveRequests_Success() throws Exception {
        // Given
        List<LeaveRequest> leaveRequests = Arrays.asList(sampleLeaveRequest);
        when(leaveService.getUserLeaveRequests(anyLong(), any(), any(), any())).thenReturn(leaveRequests);

        // When & Then
        mockMvc.perform(get("/api/leave/my-requests")
                .header("X-User-Id", 15L) // 普通员工ID
                .param("startDate", "2023-06-01")
                .param("endDate", "2023-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requests").isArray())
                .andExpect(jsonPath("$.data.requests[0].id").value(1))
                .andExpect(jsonPath("$.data.requests[0].leaveType").value("paid"));
    }

    /**
     * 测试用例：成功批准休假请求
     * 
     * 测试目标方法：
     * - LeaveRequestController.approveLeaveRequest()
     * 
     * 测试场景：
     * - 管理员批准休假请求
     * - 服务层处理并返回已批准的休假请求
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应包含成功消息
     * - 响应数据包含休假请求ID和批准状态
     * 
     * 模拟的依赖方法：
     * - LeaveService.approveLeaveRequest()
     */
    @Test
    @WithMockUser(roles = "MANAGER")
    void testApproveLeaveRequest_Success() throws Exception {
        // Given
        sampleLeaveRequest.setStatus("approved");
        sampleLeaveRequest.setApproverId(5); // 部长级别的用户ID
        sampleLeaveRequest.setApprovedAt(OffsetDateTime.now());

        LeaveRequestController.LeaveApprovalRequest request = new LeaveRequestController.LeaveApprovalRequest();
        request.setAction("approve");
        request.setComment("承認します");

        when(leaveService.approveLeaveRequest(anyLong(), anyInt())).thenReturn(sampleLeaveRequest);

        // When & Then
        mockMvc.perform(post("/api/leave/1/approve")
                .header("X-User-Id", 5L) // 部长级别的用户ID
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("休暇申請を承認しました"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.approvalStatus").value("APPROVED"));
    }

    /**
     * 测试用例：成功拒绝休假请求
     * 
     * 测试目标方法：
     * - LeaveRequestController.approveLeaveRequest()
     * 
     * 测试场景：
     * - 管理员拒绝休假请求
     * - 服务层处理并返回已拒绝的休假请求
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应包含成功消息
     * - 响应数据包含休假请求ID和拒绝状态
     * 
     * 模拟的依赖方法：
     * - LeaveService.rejectLeaveRequest()
     */
    @Test
    @WithMockUser(roles = "MANAGER")
    void testRejectLeaveRequest_Success() throws Exception {
        // Given
        sampleLeaveRequest.setStatus("rejected");
        sampleLeaveRequest.setApproverId(5); // 部长级别的用户ID
        sampleLeaveRequest.setApprovedAt(OffsetDateTime.now());

        LeaveRequestController.LeaveApprovalRequest request = new LeaveRequestController.LeaveApprovalRequest();
        request.setAction("reject");
        request.setComment("却下します");

        when(leaveService.rejectLeaveRequest(anyLong(), anyLong())).thenReturn(sampleLeaveRequest);

        // When & Then
        mockMvc.perform(post("/api/leave/1/approve")
                .header("X-User-Id", 5L) // 部长级别的用户ID
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("休暇申請を却下しました"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.approvalStatus").value("REJECTED"));
    }

    /**
     * 测试用例：成功获取休假余额
     * 
     * 测试目标方法：
     * - LeaveRequestController.getLeaveBalance()
     * 
     * 测试场景：
     * - 员工查询剩余带薪休假天数
     * - 服务层返回剩余天数
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应数据包含剩余带薪休假天数
     * 
     * 模拟的依赖方法：
     * - LeaveService.calculateRemainingPaidLeaveDays()
     */
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetLeaveBalance_Success() throws Exception {
        // Given
        when(leaveService.calculateRemainingPaidLeaveDays(anyLong())).thenReturn(10L);

        // When & Then
        mockMvc.perform(get("/api/leave/balance")
                .header("X-User-Id", 15L)) // 普通员工ID
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.remainingPaidLeaveDays").value(10));
    }

    /**
     * 测试用例：成功获取休假日历
     * 
     * 测试目标方法：
     * - LeaveRequestController.getLeaveCalendar()
     * 
     * 测试场景：
     * - 员工查询指定时间段内的休假日历事件
     * - 服务层返回休假请求列表并转换为日历事件
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应数据包含日历事件列表
     * - 列表中包含示例事件的ID和类型
     * 
     * 模拟的依赖方法：
     * - LeaveService.getUserLeaveRequests()
     */
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetLeaveCalendar_Success() throws Exception {
        // Given
        List<LeaveRequest> leaveRequests = Arrays.asList(sampleLeaveRequest);
        when(leaveService.getUserLeaveRequests(anyLong(), any(), any(), any())).thenReturn(leaveRequests);

        // When & Then
        mockMvc.perform(get("/api/leave/calendar")
                .header("X-User-Id", 15L) // 普通员工ID
                .param("startDate", "2023-06-01")
                .param("endDate", "2023-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.events").isArray())
                .andExpect(jsonPath("$.data.events[0].id").value(1))
                .andExpect(jsonPath("$.data.events[0].type").value("paid"));
    }
}