package com.example.companybackend.controller;

import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.entity.User;
import com.example.companybackend.service.AttendanceSummaryService;
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AttendanceSummaryControllerのユニットテストクラス
 * 
 * テスト対象: {@link AttendanceSummaryController}
 * 
 * テスト範囲:
 * - 日別勤務時間サマリー取得
 * - 月別勤務時間サマリー取得
 * - 残業時間統計取得
 * - 勤務統計レポート取得
 * - 勤務時間データエクスポート
 * - 日別サマリー計算
 * - 月別サマリー計算
 * - 無効日付範囲テスト
 * - データなし時のレスポンステスト
 */
@WebMvcTest(AttendanceSummaryController.class)
@ContextConfiguration(classes = {AttendanceSummaryController.class, AttendanceSummaryControllerTest.TestSecurityConfig.class})
public class AttendanceSummaryControllerTest {

    /**
     * MockMvcはSpring MVCテスト用のクライアント
     * HTTPリクエストをシミュレートし、レスポンスを検証するために使用する
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * AttendanceSummaryServiceのモックオブジェクト
     * 実際のサービス呼び出しをモックして、テスト対象のコントローラのみを検証する
     */
    @MockBean
    private AttendanceSummaryService attendanceSummaryService;

    @Autowired
    private ObjectMapper objectMapper;

    private AttendanceSummary testAttendanceSummary;
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
     * 各テストメソッド実行前の初期化処理
     * テストデータのセットアップを行う
     */
    @BeforeEach
    void setUp() {
        // テスト用の勤怠サマリーを作成
        testAttendanceSummary = new AttendanceSummary();
        testAttendanceSummary.setId(1L);
        testAttendanceSummary.setUserId(1);
        testAttendanceSummary.setTargetDate(LocalDate.now());
        testAttendanceSummary.setTotalHours(BigDecimal.valueOf(8.00));
        testAttendanceSummary.setOvertimeHours(BigDecimal.valueOf(1.00));
        testAttendanceSummary.setLateNightHours(BigDecimal.valueOf(0.00));
        testAttendanceSummary.setHolidayHours(BigDecimal.valueOf(0.00));
        testAttendanceSummary.setSummaryType("daily");
        testAttendanceSummary.setCreatedAt(OffsetDateTime.now());

        // テスト用のユーザーを作成
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }
    
    /**
     * 勤務統計レポート取得のテスト
     * 正常系：勤務統計レポートが正しく取得できることを検証する
     * 
     * テスト対象メソッド：{@link AttendanceSummaryController#getSummaryStatistics}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスにsuccess=trueが含まれること
     * 3. 統計情報が"data"オブジェクト内に含まれること
     * 4. AttendanceSummaryService.getSummaryStatisticsが1回呼び出されていること
     */
    @Test
    @WithMockUser
    void testGetSummaryStatistics_Success() throws Exception {
        // モックの設定
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalRecords", 10);
        statistics.put("totalHours", 80.0);
        statistics.put("overtimeHours", 5.0);
        
        when(attendanceSummaryService.getSummaryStatistics(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(statistics);

        // テスト実行と検証
        mockMvc.perform(get("/api/reports/attendance/statistics")
                .param("startDate", LocalDate.now().toString())
                .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statistics.totalRecords").value(10));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getSummaryStatistics(any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * 勤務統計レポート取得のテスト
     * 異常系：サービスで例外が発生した場合に適切に処理されることを検証する
     * 
     * テスト対象メソッド：{@link AttendanceSummaryController#getSummaryStatistics}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが500(Internal Server Error)であること
     * 2. レスポンスにsuccess=falseが含まれること
     * 3. 適切なエラーメッセージが返されること
     */
    @Test
    @WithMockUser
    void testGetSummaryStatistics_Exception() throws Exception {
        // モックの設定（例外発生）
        when(attendanceSummaryService.getSummaryStatistics(any(LocalDate.class), any(LocalDate.class)))
            .thenThrow(new RuntimeException("システムエラー"));

        // テスト実行と検証
        mockMvc.perform(get("/api/reports/attendance/statistics")
                .param("startDate", LocalDate.now().toString())
                .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("勤務統計レポートの取得に失敗しました"));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getSummaryStatistics(any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * 日別勤務時間サマリー取得のテスト
     * 正常系：日別勤務時間サマリーが正しく取得できることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceSummaryController#getDailySummaries}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスにsuccess=trueが含まれること
     * 3. サマリーリストが"data"オブジェクト内に含まれること
     * 4. AttendanceSummaryService.getDailySummariesが1回呼び出されていること
     */
    @Test
    @WithMockUser
    void testGetDailySummaries_Success() throws Exception {
        // モックの設定
        List<AttendanceSummary> summaries = Arrays.asList(testAttendanceSummary);
        org.springframework.data.domain.Page<AttendanceSummary> summaryPage = 
            new org.springframework.data.domain.PageImpl<>(summaries);
        
        when(attendanceSummaryService.getDailySummaries(any(LocalDate.class), any(LocalDate.class), any()))
            .thenReturn(summaryPage);

        // テスト実行と検証
        mockMvc.perform(get("/api/reports/attendance/daily")
                .param("startDate", LocalDate.now().toString())
                .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summaries.length()").value(1))
                .andExpect(jsonPath("$.data.summaries[0].id").value(1));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getDailySummaries(any(LocalDate.class), any(LocalDate.class), any());
    }

    /**
     * 日別勤務時間サマリー取得のテスト
     * 異常系：サービスで例外が発生した場合に適切に処理されることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceSummaryController#getDailySummaries}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが500(Internal Server Error)であること
     * 2. レスポンスにsuccess=falseが含まれること
     * 3. 適切なエラーメッセージが返されること
     */
    @Test
    @WithMockUser
    void testGetDailySummaries_Exception() throws Exception {
        // モックの設定（例外発生）
        when(attendanceSummaryService.getDailySummaries(any(LocalDate.class), any(LocalDate.class), any()))
            .thenThrow(new RuntimeException("システムエラー"));

        // テスト実行と検証
        mockMvc.perform(get("/api/reports/attendance/daily")
                .param("startDate", LocalDate.now().toString())
                .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("日別勤務時間サマリーの取得に失敗しました"));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getDailySummaries(any(LocalDate.class), any(LocalDate.class), any());
    }

    /**
     * 月別勤務時間サマリー取得のテスト
     * 正常系：月別勤務時間サマリーが正しく取得できることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceSummaryController#getMonthlySummaries}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスにsuccess=trueが含まれること
     * 3. サマリーリストが"data"オブジェクト内に含まれること
     * 4. AttendanceSummaryService.getMonthlySummariesが1回呼び出されていること
     */
    @Test
    @WithMockUser
    void testGetMonthlySummaries_Success() throws Exception {
        // モックの設定
        List<AttendanceSummary> summaries = Arrays.asList(testAttendanceSummary);
        org.springframework.data.domain.Page<AttendanceSummary> summaryPage = 
            new org.springframework.data.domain.PageImpl<>(summaries);
        
        when(attendanceSummaryService.getMonthlySummaries(any(YearMonth.class), any()))
            .thenReturn(summaryPage);

        // テスト実行と検証
        mockMvc.perform(get("/api/reports/attendance/monthly")
                .param("yearMonth", YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summaries.length()").value(1))
                .andExpect(jsonPath("$.data.summaries[0].id").value(1));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getMonthlySummaries(any(YearMonth.class), any());
    }

    /**
     * 月別勤務時間サマリー取得のテスト
     * 異常系：サービスで例外が発生した場合に適切に処理されることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceSummaryController#getMonthlySummaries}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが500(Internal Server Error)であること
     * 2. レスポンスにsuccess=falseが含まれること
     * 3. 適切なエラーメッセージが返されること
     */
    @Test
    @WithMockUser
    void testGetMonthlySummaries_Exception() throws Exception {
        // モックの設定（例外発生）
        when(attendanceSummaryService.getMonthlySummaries(any(YearMonth.class), any()))
            .thenThrow(new RuntimeException("システムエラー"));

        // テスト実行と検証
        mockMvc.perform(get("/api/reports/attendance/monthly")
                .param("yearMonth", YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("月別勤務時間サマリーの取得に失敗しました"));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getMonthlySummaries(any(YearMonth.class), any());
    }

    /**
     * 残業時間統計取得のテスト
     * 正常系：残業時間統計が正しく取得できることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceSummaryController#getOvertimeStatistics}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスにsuccess=trueが含まれること
     * 3. 統計情報が"data"オブジェクト内に含まれること
     * 4. AttendanceSummaryService.getSummaryStatisticsが1回呼び出されていること
     */
    @Test
    @WithMockUser
    void testGetOvertimeStatistics_Success() throws Exception {
        // モックの設定
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalRecords", 10);
        statistics.put("totalHours", 80.0);
        statistics.put("overtimeHours", 5.0);
        
        when(attendanceSummaryService.getSummaryStatistics(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(statistics);

        // テスト実行と検証
        mockMvc.perform(get("/api/reports/attendance/overtime")
                .param("startDate", LocalDate.now().toString())
                .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statistics.totalRecords").value(10));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getSummaryStatistics(any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * 残業時間統計取得のテスト
     * 異常系：サービスで例外が発生した場合に適切に処理されることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceSummaryController#getOvertimeStatistics}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが500(Internal Server Error)であること
     * 2. レスポンスにsuccess=falseが含まれること
     * 3. 適切なエラーメッセージが返されること
     */
    @Test
    @WithMockUser
    void testGetOvertimeStatistics_Exception() throws Exception {
        // モックの設定（例外発生）
        when(attendanceSummaryService.getSummaryStatistics(any(LocalDate.class), any(LocalDate.class)))
            .thenThrow(new RuntimeException("システムエラー"));

        // テスト実行と検証
        mockMvc.perform(get("/api/reports/attendance/overtime")
                .param("startDate", LocalDate.now().toString())
                .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("残業時間統計の取得に失敗しました"));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getSummaryStatistics(any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * 個人別勤怠統計取得のテスト
     * 正常系：個人別勤怠統計が正しく取得できることを検証する
     * 
     * テスト対象メソッド：{@link AttendanceSummaryController#getPersonalAttendanceStatistics}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスにsuccess=trueが含まれること
     * 3. 統計情報が"data"オブジェクト内に含まれること
     * 4. AttendanceSummaryService.getPersonalAttendanceStatisticsが1回呼び出されていること
     */
    @Test
    @WithMockUser
    void testGetPersonalAttendanceStatistics_Success() throws Exception {
        // モックの設定
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("userId", 1L);
        statistics.put("totalRecords", 20);
        statistics.put("totalHours", 160.0);
        statistics.put("overtimeHours", 10.0);
        
        when(attendanceSummaryService.getPersonalAttendanceStatistics(anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(statistics);

        // テスト実行と検証
        mockMvc.perform(get("/api/reports/attendance/personal")
                .param("userId", "1")
                .param("startDate", LocalDate.now().toString())
                .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statistics.userId").value(1))
                .andExpect(jsonPath("$.data.statistics.totalRecords").value(20));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getPersonalAttendanceStatistics(anyLong(), any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * 個人別勤怠統計取得のテスト
     * 異常系：サービスで例外が発生した場合に適切に処理されることを検証する
     * 
     * テスト対象メソッド：{@link AttendanceSummaryController#getPersonalAttendanceStatistics}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが500(Internal Server Error)であること
     * 2. レスポンスにsuccess=falseが含まれること
     * 3. 適切なエラーメッセージが返されること
     */
    @Test
    @WithMockUser
    void testGetPersonalAttendanceStatistics_Exception() throws Exception {
        // モックの設定（例外発生）
        when(attendanceSummaryService.getPersonalAttendanceStatistics(anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenThrow(new RuntimeException("システムエラー"));

        // テスト実行と検証
        mockMvc.perform(get("/api/reports/attendance/personal")
                .param("userId", "1")
                .param("startDate", LocalDate.now().toString())
                .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("個人別勤怠統計の取得に失敗しました"));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getPersonalAttendanceStatistics(anyLong(), any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * 部門別勤怠統計取得のテスト
     * 正常系：部門別勤怠統計が正しく取得できることを検証する
     * 
     * テスト対象メソッド：{@link AttendanceSummaryController#getDepartmentAttendanceStatistics}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスにsuccess=trueが含まれること
     * 3. 統計情報が"data"オブジェクト内に含まれること
     * 4. AttendanceSummaryService.getDepartmentAttendanceStatisticsが1回呼び出されていること
     */
    @Test
    @WithMockUser
    void testGetDepartmentAttendanceStatistics_Success() throws Exception {
        // モックの設定
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("departmentId", 1);
        statistics.put("userCount", 10);
        statistics.put("totalRecords", 200);
        statistics.put("totalHours", 1600.0);
        statistics.put("averageHoursPerUser", 160.0);
        
        when(attendanceSummaryService.getDepartmentAttendanceStatistics(anyInt(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(statistics);

        // テスト実行と検証
        mockMvc.perform(get("/api/reports/attendance/department")
                .param("departmentId", "1")
                .param("startDate", LocalDate.now().toString())
                .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statistics.departmentId").value(1))
                .andExpect(jsonPath("$.data.statistics.userCount").value(10));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getDepartmentAttendanceStatistics(anyInt(), any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * 部門別勤怠統計取得のテスト
     * 異常系：サービスで例外が発生した場合に適切に処理されることを検証する
     * 
     * テスト対象メソッド：{@link AttendanceSummaryController#getDepartmentAttendanceStatistics}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが500(Internal Server Error)であること
     * 2. レスポンスにsuccess=falseが含まれること
     * 3. 適切なエラーメッセージが返されること
     */
    @Test
    @WithMockUser
    void testGetDepartmentAttendanceStatistics_Exception() throws Exception {
        // モックの設定（例外発生）
        when(attendanceSummaryService.getDepartmentAttendanceStatistics(anyInt(), any(LocalDate.class), any(LocalDate.class)))
            .thenThrow(new RuntimeException("システムエラー"));

        // テスト実行と検証
        mockMvc.perform(get("/api/reports/attendance/department")
                .param("departmentId", "1")
                .param("startDate", LocalDate.now().toString())
                .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("部門別勤怠統計の取得に失敗しました"));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getDepartmentAttendanceStatistics(anyInt(), any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * 日別サマリー計算のテスト
     * 正常系：日別サマリーが正しく計算できることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceSummaryController#calculateDailySummary}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスにsuccess=trueが含まれること
     * 3. サマリー情報が"data"オブジェクト内に含まれること
     * 4. AttendanceSummaryService.generateDailySummaryが1回呼び出されていること
     */
    @Test
    @WithMockUser
    void testCalculateDailySummary_Success() throws Exception {
        // モックの設定
        when(attendanceSummaryService.generateDailySummary(any(LocalDate.class)))
            .thenReturn(testAttendanceSummary);

        // テスト実行と検証
        mockMvc.perform(post("/api/reports/attendance/daily/calculate")
                .param("targetDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("日別サマリーの計算が完了しました"))
                .andExpect(jsonPath("$.data.summary.id").value(1));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .generateDailySummary(any(LocalDate.class));
    }

    /**
     * 日別サマリー計算のテスト
     * 異常系：サービスで例外が発生した場合に適切に処理されることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceSummaryController#calculateDailySummary}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが500(Internal Server Error)であること
     * 2. レスポンスにsuccess=falseが含まれること
     * 3. 適切なエラーメッセージが返されること
     */
    @Test
    @WithMockUser
    void testCalculateDailySummary_Exception() throws Exception {
        // モックの設定（例外発生）
        when(attendanceSummaryService.generateDailySummary(any(LocalDate.class)))
            .thenThrow(new RuntimeException("システムエラー"));

        // テスト実行と検証
        mockMvc.perform(post("/api/reports/attendance/daily/calculate")
                .param("targetDate", LocalDate.now().toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("日別サマリーの計算に失敗しました"));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .generateDailySummary(any(LocalDate.class));
    }

    /**
     * 月別サマリー計算のテスト
     * 正常系：月別サマリーが正しく計算できることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceSummaryController#calculateMonthlySummary}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスにsuccess=trueが含まれること
     * 3. 統計情報が"data"オブジェクト内に含まれること
     * 4. AttendanceSummaryService.getMonthlyStatisticsが1回呼び出されていること
     */
    @Test
    @WithMockUser
    void testCalculateMonthlySummary_Success() throws Exception {
        // モックの設定
        Map<String, Object> monthlyStats = new HashMap<>();
        monthlyStats.put("monthlyHours", Map.of("2025-01-01", 160.0));
        monthlyStats.put("averageDailyHours", 8.0);
        
        when(attendanceSummaryService.getMonthlyStatistics(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(monthlyStats);

        // テスト実行と検証
        mockMvc.perform(post("/api/reports/attendance/monthly/calculate")
                .param("yearMonth", YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("月別サマリーの計算が完了しました"))
                .andExpect(jsonPath("$.data.statistics.monthlyHours").exists());

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getMonthlyStatistics(any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * 月別サマリー計算のテスト
     * 異常系：サービスで例外が発生した場合に適切に処理されることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceSummaryController#calculateMonthlySummary}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが500(Internal Server Error)であること
     * 2. レスポンスにsuccess=falseが含まれること
     * 3. 適切なエラーメッセージが返されること
     */
    @Test
    @WithMockUser
    void testCalculateMonthlySummary_Exception() throws Exception {
        // モックの設定（例外発生）
        when(attendanceSummaryService.getMonthlyStatistics(any(LocalDate.class), any(LocalDate.class)))
            .thenThrow(new RuntimeException("システムエラー"));

        // テスト実行と検証
        mockMvc.perform(post("/api/reports/attendance/monthly/calculate")
                .param("yearMonth", YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("月別サマリーの計算に失敗しました"));

        // メソッド呼び出しの検証
        verify(attendanceSummaryService, times(1))
            .getMonthlyStatistics(any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * 無効日付範囲テスト
     * 境界値：開始日が終了日より後である場合の処理を検証する
     * 
     * テスト対象メソッド: {@link AttendanceSummaryController#getDailySummaries}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが500(Internal Server Error)であること
     */
    @Test
    @WithMockUser
    void testInvalidDateRange() throws Exception {
        // テスト実行と検証
        mockMvc.perform(get("/api/reports/attendance/daily")
                .param("startDate", LocalDate.now().plusDays(1).toString()) // 終了日より後の日付
                .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // /**
    //  * データなし時のレスポンステスト
    //  * 境界値：データが存在しない場合のレスポンスを検証する
    //  * 
    //  * テスト対象メソッド: {@link AttendanceSummaryController#getDailySummaries}
    //  * 
    //  * 検証内容:
    //  * 1. HTTPステータスコードが200(OK)であること
    //  * 2. レスポンスにsuccess=trueが含まれること
    //  * 3. 空のリストが返されること
    //  */
    // @Test
    // @WithMockUser
    // void testNoDataResponse() throws Exception {
    //     // モックの設定（データなし）
    //     Pageable pageable = PageRequest.of(0, 20);
    //     Page<AttendanceSummary> summaryPage = new PageImpl<>(Arrays.asList(), pageable, 0);
        
    //     when(attendanceSummaryService.getDailySummaries(any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
    //         .thenReturn(summaryPage);

    //     // テスト実行と検証
    //     mockMvc.perform(get("/api/reports/attendance/daily")
    //             .param("startDate", LocalDate.now().toString())
    //             .param("endDate", LocalDate.now().toString()))
    //             .andExpect(status().isOk())
    //             .andExpect(jsonPath("$.success").value(true))
    //             .andExpect(jsonPath("$.data.summaries.length()").value(0))
    //             .andExpect(jsonPath("$.data.totalCount").value(0));

    //     // メソッド呼び出しの検証
    //     verify(attendanceSummaryService, times(1))
    //         .getDailySummaries(any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
    // }
}