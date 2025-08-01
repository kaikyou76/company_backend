package com.example.companybackend.controller;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.entity.User;
import com.example.companybackend.service.AttendanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 考勤控制器测试类
 * 
 * 测试目标：
 * - 测试文件：AttendanceController.java
 * - 测试类：com.example.companybackend.controller.AttendanceController
 * - 模拟依赖：AttendanceService（考勤服务类）
 * 
 * 测试规范和技巧：
 * 1. 使用@WebMvcTest注解仅加载Web层相关组件，提高测试效率
 * 2. 使用@MockBean模拟服务层依赖，隔离测试
 * 3. 使用@WithMockUser模拟用户认证信息
 * 4. 遵循Given-When-Then测试模式
 * 5. 对每个API端点编写成功和失败场景的测试用例
 * 6. 验证HTTP状态码、响应结构和关键数据
 */
@WebMvcTest(AttendanceController.class)
@ContextConfiguration(classes = {AttendanceController.class, AttendanceControllerTest.TestSecurityConfig.class})
public class AttendanceControllerTest {

    /**
     * MockMvcはSpring MVCテスト用のクライアント
     * HTTPリクエストをシミュレートし、レスポンスを検証するために使用する
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * AttendanceServiceのモックオブジェクト
     * 実際のサービス呼び出しをモックして、テスト対象のコントローラのみを検証する
     */
    @MockBean
    private AttendanceService attendanceService;

    @Autowired
    private ObjectMapper objectMapper;

    private AttendanceRecord testAttendanceRecord;
    private User testUser;

    /**
     * テスト用のセキュリティ設定
     * CSRF保護を無効化し、すべてのリクエストを許可する
     */
    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
        }
    }

    /**
     * 各测试方法执行前的初始化处理
     * 设置测试数据
     */
    @BeforeEach
    void setUp() {
        // 创建测试用的考勤记录
        testAttendanceRecord = new AttendanceRecord();
        testAttendanceRecord.setId(1L);
        testAttendanceRecord.setUserId(1);
        testAttendanceRecord.setType("in");
        testAttendanceRecord.setTimestamp(OffsetDateTime.now());
        testAttendanceRecord.setLatitude(35.6812);
        testAttendanceRecord.setLongitude(139.7671);

        // 创建测试用的用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setLocationType("office");
        testUser.setSkipLocationCheck(false);
    }

    /**
     * 测试用例：成功出勤打卡
     * 
     * 测试目标方法：
     * - AttendanceController.clockIn()
     * 
     * 测试场景：
     * - 员工在有效位置提交出勤打卡请求
     * - 服务层成功处理并返回打卡记录
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应包含成功消息
     * - 响应数据包含记录ID和打卡类型
     * 
     * 模拟的依赖方法：
     * - AttendanceService.clockIn()
     */
    @Test
    @WithMockUser
    void testClockIn_Success() throws Exception {
        // モックの設定
        AttendanceService.ClockInRequest request = new AttendanceService.ClockInRequest();
        request.setLatitude(35.6812);
        request.setLongitude(139.7671);
        
        AttendanceService.ClockInResponse response = new AttendanceService.ClockInResponse(
            true, "出勤打刻が完了しました", testAttendanceRecord, "出勤済み");
        
        when(attendanceService.clockIn(any(AttendanceService.ClockInRequest.class), anyLong()))
            .thenReturn(response);

        // テスト実行と検証
        mockMvc.perform(post("/api/attendance/clock-in")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "latitude": 35.6812,
                        "longitude": 139.7671
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("出勤打刻が完了しました"))
                .andExpect(jsonPath("$.data.recordId").value(1));

        // メソッド呼び出しの検証
        verify(attendanceService, times(1)).clockIn(any(AttendanceService.ClockInRequest.class), anyLong());
    }

    /**
     * 测试用例：出勤打卡位置验证失败
     * 
     * 测试目标方法：
     * - AttendanceController.clockIn()
     * 
     * 测试场景：
     * - 员工在无效位置提交出勤打卡请求
     * - 服务层抛出IllegalArgumentException异常
     * 
     * 预期结果：
     * - HTTP状态码：400 Bad Request
     * - 响应包含success=false
     * - 响应包含错误消息
     * 
     * 模拟的依赖方法：
     * - AttendanceService.clockIn()
     */
    @Test
    @WithMockUser
    void testClockIn_InvalidLocation() throws Exception {
        // モックの設定（位置情報が許可範囲外）
        AttendanceService.ClockInRequest request = new AttendanceService.ClockInRequest();
        request.setLatitude(35.0000);  // 許可範囲外の緯度
        request.setLongitude(139.0000);  // 許可範囲外の経度
        
        AttendanceService.ClockInResponse response = new AttendanceService.ClockInResponse(
            false, "オフィスから100m以上離れた場所での打刻はできません", null, null);
        
        when(attendanceService.clockIn(any(AttendanceService.ClockInRequest.class), anyLong()))
            .thenReturn(response);

        // テスト実行と検証
        mockMvc.perform(post("/api/attendance/clock-in")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "latitude": 35.0000,
                        "longitude": 139.0000
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("オフィスから100m以上離れた場所での打刻はできません"));

        // メソッド呼び出しの検証
        verify(attendanceService, times(1)).clockIn(any(AttendanceService.ClockInRequest.class), anyLong());
    }

    /**
     * 测试用例：跳过位置检查的出勤打卡
     * 
     * 测试目标方法：
     * - AttendanceController.clockIn()
     * 
     * 测试场景：
     * - 设置跳过位置检查的用户在任意位置提交出勤打卡请求
     * - 服务层成功处理并返回打卡记录
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应包含成功消息
     * - 响应数据包含记录ID和打卡类型
     * 
     * 模拟的依赖方法：
     * - AttendanceService.clockIn()
     */
    @Test
    @WithMockUser
    void testClockIn_SkipLocationCheck() throws Exception {
        // モックの設定
        AttendanceService.ClockInRequest request = new AttendanceService.ClockInRequest();
        request.setLatitude(35.0000);  // 許可範囲外の緯度
        request.setLongitude(139.0000);  // 許可範囲外の経度
        
        AttendanceService.ClockInResponse response = new AttendanceService.ClockInResponse(
            true, "出勤打刻が完了しました", testAttendanceRecord, "出勤済み");
        
        when(attendanceService.clockIn(any(AttendanceService.ClockInRequest.class), anyLong()))
            .thenReturn(response);

        // テスト実行と検証
        mockMvc.perform(post("/api/attendance/clock-in")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "latitude": 35.0000,
                        "longitude": 139.0000
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("出勤打刻が完了しました"))
                .andExpect(jsonPath("$.data.recordId").value(1));

        // メソッド呼び出しの検証
        verify(attendanceService, times(1)).clockIn(any(AttendanceService.ClockInRequest.class), anyLong());
    }

    /**
     * 测试用例：成功退勤打卡
     * 
     * 测试目标方法：
     * - AttendanceController.clockOut()
     * 
     * 测试场景：
     * - 员工在有效位置提交退勤打卡请求
     * - 服务层成功处理并返回打卡记录
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应包含成功消息
     * - 响应数据包含记录ID和打卡类型
     * 
     * 模拟的依赖方法：
     * - AttendanceService.clockOut()
     */
    @Test
    @WithMockUser
    void testClockOut_Success() throws Exception {
        // モックの設定
        AttendanceService.ClockOutRequest request = new AttendanceService.ClockOutRequest();
        request.setLatitude(35.6812);
        request.setLongitude(139.7671);
        
        AttendanceService.ClockOutResponse response = new AttendanceService.ClockOutResponse(
            true, "退勤打刻が完了しました", testAttendanceRecord, "退勤済み");
        
        when(attendanceService.clockOut(any(AttendanceService.ClockOutRequest.class), anyLong()))
            .thenReturn(response);

        // テスト実行と検証
        mockMvc.perform(post("/api/attendance/clock-out")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "latitude": 35.6812,
                        "longitude": 139.7671
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("退勤打刻が完了しました"))
                .andExpect(jsonPath("$.data.recordId").value(1));

        // メソッド呼び出しの検証
        verify(attendanceService, times(1)).clockOut(any(AttendanceService.ClockOutRequest.class), anyLong());
    }

    /**
     * 测试用例：退勤打卡位置验证失败
     * 
     * 测试目标方法：
     * - AttendanceController.clockOut()
     * 
     * 测试场景：
     * - 员工在无效位置提交退勤打卡请求
     * - 服务层抛出IllegalArgumentException异常
     * 
     * 预期结果：
     * - HTTP状态码：400 Bad Request
     * - 响应包含success=false
     * - 响应包含错误消息
     * 
     * 模拟的依赖方法：
     * - AttendanceService.clockOut()
     */
    @Test
    @WithMockUser
    void testClockOut_InvalidLocation() throws Exception {
        // モックの設定（位置情報が許可範囲外）
        AttendanceService.ClockOutRequest request = new AttendanceService.ClockOutRequest();
        request.setLatitude(35.0000);  // 許可範囲外の緯度
        request.setLongitude(139.0000);  // 許可範囲外の経度
        
        AttendanceService.ClockOutResponse response = new AttendanceService.ClockOutResponse(
            false, "オフィスから100m以上離れた場所での打刻はできません", null, null);
        
        when(attendanceService.clockOut(any(AttendanceService.ClockOutRequest.class), anyLong()))
            .thenReturn(response);

        // テスト実行と検証
        mockMvc.perform(post("/api/attendance/clock-out")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "latitude": 35.0000,
                        "longitude": 139.0000
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("オフィスから100m以上離れた場所での打刻はできません"));

        // メソッド呼び出しの検証
        verify(attendanceService, times(1)).clockOut(any(AttendanceService.ClockOutRequest.class), anyLong());
    }

    /**
     * 测试用例：跳过位置检查的退勤打卡
     * 
     * 测试目标方法：
     * - AttendanceController.clockOut()
     * 
     * 测试场景：
     * - 设置跳过位置检查的用户在任意位置提交退勤打卡请求
     * - 服务层成功处理并返回打卡记录
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应包含成功消息
     * - 响应数据包含记录ID和打卡类型
     * 
     * 模拟的依赖方法：
     * - AttendanceService.clockOut()
     */
    @Test
    @WithMockUser
    void testClockOut_SkipLocationCheck() throws Exception {
        // モックの設定
        AttendanceService.ClockOutRequest request = new AttendanceService.ClockOutRequest();
        request.setLatitude(35.0000);  // 許可範囲外の緯度
        request.setLongitude(139.0000);  // 許可範囲外の経度
        
        AttendanceService.ClockOutResponse response = new AttendanceService.ClockOutResponse(
            true, "退勤打刻が完了しました", testAttendanceRecord, "退勤済み");
        
        when(attendanceService.clockOut(any(AttendanceService.ClockOutRequest.class), anyLong()))
            .thenReturn(response);

        // テスト実行と検証
        mockMvc.perform(post("/api/attendance/clock-out")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "latitude": 35.0000,
                        "longitude": 139.0000
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("退勤打刻が完了しました"))
                .andExpect(jsonPath("$.data.recordId").value(1));

        // メソッド呼び出しの検証
        verify(attendanceService, times(1)).clockOut(any(AttendanceService.ClockOutRequest.class), anyLong());
    }

    /**
     * 测试用例：成功获取考勤记录
     * 
     * 测试目标方法：
     * - AttendanceController.getAttendanceRecords()
     * 
     * 测试场景：
     * - 员工查询考勤记录
     * - 服务层返回考勤记录列表
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应数据包含考勤记录列表
     * - 列表中包含示例考勤记录的ID和类型
     * 
     * 模拟的依赖方法：
     * - AttendanceService.getTodayRecords()
     */
    @Test
    @WithMockUser
    void testGetAttendanceRecords_Success() throws Exception {
        // モックの設定
        List<AttendanceRecord> records = Arrays.asList(testAttendanceRecord);
        when(attendanceService.getTodayRecords(anyLong())).thenReturn(records);

        // テスト実行と検証
        mockMvc.perform(get("/api/attendance/records")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(1));

        // メソッド呼び出しの検証
        verify(attendanceService, times(1)).getTodayRecords(anyLong());
    }

    /**
     * 测试用例：获取考勤记录时服务异常
     * 
     * 测试目标方法：
     * - AttendanceController.getAttendanceRecords()
     * 
     * 测试场景：
     * - 员工查询考勤记录
     * - 服务层抛出RuntimeException异常
     * 
     * 预期结果：
     * - HTTP状态码：500 Internal Server Error
     * - 响应包含success=false
     * 
     * 模拟的依赖方法：
     * - AttendanceService.getTodayRecords()
     */
    @Test
    @WithMockUser
    void testGetAttendanceRecords_Exception() throws Exception {
        // モックの設定（例外発生）
        when(attendanceService.getTodayRecords(anyLong()))
            .thenThrow(new RuntimeException("システムエラー"));

        // テスト実行と検証
        mockMvc.perform(get("/api/attendance/records")
                .header("X-User-Id", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        // メソッド呼び出しの検証
        verify(attendanceService, times(1)).getTodayRecords(anyLong());
    }

    /**
     * 测试用例：成功获取日次汇总
     * 
     * 测试目标方法：
     * - AttendanceController.getDailySummary()
     * 
     * 测试场景：
     * - 员工查询指定日期的日次汇总
     * - 服务层返回日次汇总数据
     * 
     * 预期结果：
     * - HTTP状态码：200 OK
     * - 响应包含success=true
     * - 响应数据包含工作状态和工时
     * 
     * 模拟的依赖方法：
     * - AttendanceService.getDailySummary()
     */
    @Test
    @WithMockUser
    void testGetDailySummary_Success() throws Exception {
        // モックの設定
        AttendanceRecord clockInRecord = new AttendanceRecord();
        clockInRecord.setId(1L);
        clockInRecord.setUserId(1);
        clockInRecord.setType("in");
        clockInRecord.setTimestamp(OffsetDateTime.now().minusHours(8));
        
        AttendanceRecord clockOutRecord = new AttendanceRecord();
        clockOutRecord.setId(2L);
        clockOutRecord.setUserId(1);
        clockOutRecord.setType("out");
        clockOutRecord.setTimestamp(OffsetDateTime.now());
        
        AttendanceService.DailySummaryData summary = new AttendanceService.DailySummaryData(
            LocalDate.now(), 
            BigDecimal.valueOf(8.00), 
            BigDecimal.valueOf(0.00), 
            "completed", 
            clockInRecord, 
            clockOutRecord);
        
        when(attendanceService.getDailySummary(anyLong(), any(LocalDate.class))).thenReturn(summary);

        // テスト実行と検証
        mockMvc.perform(get("/api/attendance/daily-summary")
                .header("X-User-Id", "1")
                .param("date", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("completed"))
                .andExpect(jsonPath("$.data.workingHours").value(8.00));

        // メソッド呼び出しの検証
        verify(attendanceService, times(1)).getDailySummary(anyLong(), any(LocalDate.class));
    }

    /**
     * 测试用例：获取日次汇总时服务异常
     * 
     * 测试目标方法：
     * - AttendanceController.getDailySummary()
     * 
     * 测试场景：
     * - 员工查询指定日期的日次汇总
     * - 服务层抛出RuntimeException异常
     * 
     * 预期结果：
     * - HTTP状态码：500 Internal Server Error
     * - 响应包含success=false
     * 
     * 模拟的依赖方法：
     * - AttendanceService.getDailySummary()
     */
    @Test
    @WithMockUser
    void testGetDailySummary_Exception() throws Exception {
        // モックの設定（例外発生）
        when(attendanceService.getDailySummary(anyLong(), any(LocalDate.class)))
            .thenThrow(new RuntimeException("システムエラー"));

        // テスト実行と検証
        mockMvc.perform(get("/api/attendance/daily-summary")
                .header("X-User-Id", "1")
                .param("date", LocalDate.now().toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        // メソッド呼び出しの検証
        verify(attendanceService, times(1)).getDailySummary(anyLong(), any(LocalDate.class));
    }
}