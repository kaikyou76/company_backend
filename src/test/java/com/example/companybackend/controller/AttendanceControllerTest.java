package com.example.companybackend.controller;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.service.AttendanceService;
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
 * AttendanceControllerのユニットテストクラス
 * 
 * テスト対象: {@link AttendanceController}
 * 
 * テスト範囲:
 * - 出勤打刻 (clock-in)
 * - 退勤打刻 (clock-out)
 * - 勤怠記録取得 (records)
 * - 日次サマリー取得 (daily-summary)
 * 
 * テスト用例制作规范和技巧:
 * 1. 各テストメソッドは1つの機能を検証する
 * 2. 正常系（成功ケース）と異常系（失敗ケース）の両方をテストする
 * 3. モックを使用して外部依存を排除し、テスト対象のコードのみを検証する
 * 4. JSONパスを使用してレスポンスの特定フィールドを検証する
 * 5. メソッド呼び出し回数や引数の検証を行う
 * 6. セキュリティが必要なエンドポイントは@WithMockUserを使用してテストする
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

    /**
     * テストで使用する勤怠記録のモックデータ
     */
    private AttendanceRecord testAttendanceRecord;

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
        testAttendanceRecord = new AttendanceRecord();
        testAttendanceRecord.setId(1L);
        testAttendanceRecord.setUserId(1);
        testAttendanceRecord.setType("in");
        testAttendanceRecord.setTimestamp(OffsetDateTime.now());
        testAttendanceRecord.setLatitude(35.6895);
        testAttendanceRecord.setLongitude(139.6917);
    }

    /**
     * 出勤打刻のテスト
     * 正常系：出勤打刻が正しく処理できることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceController#clockIn}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスのsuccessフィールドがtrueであること
     * 3. 打刻記録が正しく返されること
     * 4. AttendanceService.clockInが1回呼び出されていること
     */
    @Test
    @WithMockUser
    void testClockIn_Success() throws Exception {
        // モックの設定
        AttendanceService.ClockInRequest request = new AttendanceService.ClockInRequest();
        request.setLatitude(35.6895);
        request.setLongitude(139.6917);
        
        AttendanceService.ClockInResponse response = new AttendanceService.ClockInResponse(
            true, "出勤打刻が完了しました", testAttendanceRecord, "勤務中");
        
        when(attendanceService.clockIn(any(AttendanceService.ClockInRequest.class), anyLong()))
            .thenReturn(response);

        // テスト実行と検証
        mockMvc.perform(post("/api/attendance/clock-in")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "latitude": 35.6895,
                        "longitude": 139.6917
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
     * 出勤打刻のテスト
     * 異常系：サービスで例外が発生した場合に適切に処理されることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceController#clockIn}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが500(Internal Server Error)であること
     * 2. レスポンスのsuccessフィールドがfalseであること
     * 3. 適切なエラーメッセージが返されること
     */
    @Test
    @WithMockUser
    void testClockIn_Exception() throws Exception {
        // モックの設定（例外発生）
        when(attendanceService.clockIn(any(AttendanceService.ClockInRequest.class), anyLong()))
            .thenThrow(new RuntimeException("システムエラー"));

        // テスト実行と検証
        mockMvc.perform(post("/api/attendance/clock-in")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "latitude": 35.6895,
                        "longitude": 139.6917
                    }
                    """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("システムエラーが発生しました"));

        // メソッド呼び出しの検証
        verify(attendanceService, times(1)).clockIn(any(AttendanceService.ClockInRequest.class), anyLong());
    }

    /**
     * 退勤打刻のテスト
     * 正常系：退勤打刻が正しく処理できることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceController#clockOut}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスのsuccessフィールドがtrueであること
     * 3. 打刻記録が正しく返されること
     * 4. AttendanceService.clockOutが1回呼び出されていること
     */
    @Test
    @WithMockUser
    void testClockOut_Success() throws Exception {
        // モックの設定
        AttendanceService.ClockOutRequest request = new AttendanceService.ClockOutRequest();
        request.setLatitude(35.6895);
        request.setLongitude(139.6917);
        
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
                        "latitude": 35.6895,
                        "longitude": 139.6917
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
     * 退勤打刻のテスト
     * 異常系：サービスで例外が発生した場合に適切に処理されることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceController#clockOut}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが500(Internal Server Error)であること
     * 2. レスポンスのsuccessフィールドがfalseであること
     * 3. 適切なエラーメッセージが返されること
     */
    @Test
    @WithMockUser
    void testClockOut_Exception() throws Exception {
        // モックの設定（例外発生）
        when(attendanceService.clockOut(any(AttendanceService.ClockOutRequest.class), anyLong()))
            .thenThrow(new RuntimeException("システムエラー"));

        // テスト実行と検証
        mockMvc.perform(post("/api/attendance/clock-out")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "latitude": 35.6895,
                        "longitude": 139.6917
                    }
                    """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("システムエラーが発生しました"));

        // メソッド呼び出しの検証
        verify(attendanceService, times(1)).clockOut(any(AttendanceService.ClockOutRequest.class), anyLong());
    }

    /**
     * 勤怠記録取得のテスト
     * 正常系：勤怠記録が正しく取得できることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceController#getAttendanceRecords}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスにsuccess=trueが含まれること
     * 3. レスポンスに記録リストが含まれること
     * 4. AttendanceService.getTodayRecordsが1回呼び出されていること
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
     * 勤怠記録取得のテスト
     * 異常系：サービスで例外が発生した場合に適切に処理されることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceController#getAttendanceRecords}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが500(Internal Server Error)であること
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
     * 日次サマリー取得のテスト
     * 正常系：日次サマリーが正しく取得できることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceController#getDailySummary}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. AttendanceService.getDailySummaryが1回呼び出されていること
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
            new BigDecimal("8.00"), 
            new BigDecimal("0.00"), 
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
     * 日次サマリー取得のテスト
     * 異常系：サービスで例外が発生した場合に適切に処理されることを検証する
     * 
     * テスト対象メソッド: {@link AttendanceController#getDailySummary}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが500(Internal Server Error)であること
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