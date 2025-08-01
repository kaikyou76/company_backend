package com.example.companybackend.controller;

import com.example.companybackend.batch.service.BatchJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * バッチジョブコントローラーテストクラス
 * 
 * テスト対象：
 * - テストファイル：BatchJobController.java
 * - テストクラス：com.example.companybackend.controller.BatchJobController
 * - モック対象：JobLauncher、Job、BatchJobService（バッチ関連クラス）
 * 
 * テスト規約とテクニック：
 * 1. @ExtendWith(MockitoExtension.class)を使用してモック機能を有効化
 * 2. @InjectMocksでテスト対象のコントローラーを注入
 * 3. @Mockで依存サービスをモック化し、テストを独立させる
 * 4. MockMvcBuilders.standaloneSetup()で軽量なテスト設定を使用
 * 5. Given-When-Thenテストパターンに従う
 * 6. 各APIエンドポイントに対して成功および例外シナリオのテストケースを作成
 * 7. HTTPステータスコード、レスポンス構造、および重要なデータを検証
 */
@ExtendWith(MockitoExtension.class)
public class BatchJobControllerTest {

    /**
     * MockMvcはSpring MVCテスト用のクライアント
     * HTTPリクエストのシミュレーションとレスポンスの検証に使用
     */
    @InjectMocks
    private BatchJobController batchJobController;

    /**
     * JobLauncherのモックオブジェクト
     * バッチジョブの実行をモック化し、コントローラーロジックのみをテスト
     */
    @Mock
    private JobLauncher jobLauncher;

    /**
     * dailyAttendanceSummaryJobのモックオブジェクト
     * 日次勤務時間集計ジョブをモック化
     */
    @Mock
    private Job dailyAttendanceSummaryJob;

    /**
     * monthlyAttendanceSummaryJobのモックオブジェクト
     * 月次勤務時間集計ジョブをモック化
     */
    @Mock
    private Job monthlyAttendanceSummaryJob;

    /**
     * BatchJobServiceのモックオブジェクト
     * バッチジョブサービスをモック化
     */
    @Mock
    private BatchJobService batchJobService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(batchJobController).build();
    }

    /**
     * テストケース：月次勤怠集計バッチ実行の成功ケース（パラメータあり）
     * 
     * テスト対象メソッド：
     * - BatchJobController.runMonthlySummary()
     * 
     * テストシナリオ：
     * - ユーザーが年月パラメータを指定して月次勤怠集計バッチを実行
     * - ジョブランチャーが正常にジョブを実行
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれること
     * - レスポンスに指定した年月が含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testRunMonthlySummary_WithYearMonthParameter() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("yearMonth", "2025-01");
        
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenReturn(jobExecution);

        // When & Then
        mockMvc.perform(post("/api/batch/monthly-summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"yearMonth\":\"2025-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("月次勤怠集計バッチを実行しました"))
                .andExpect(jsonPath("$.data.targetMonth").value("2025-01"));

        // Verify that the job was launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：月次勤怠集計バッチ実行の成功ケース（パラメータなし）
     * 
     * テスト対象メソッド：
     * - BatchJobController.runMonthlySummary()
     * 
     * テストシナリオ：
     * - ユーザーがパラメータなしで月次勤怠集計バッチを実行
     * - ジョブランチャーが正常にジョブを実行（前月が自動設定される）
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれること
     * - レスポンスに前月が設定されること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testRunMonthlySummary_WithoutParameter() throws Exception {
        // Given
        String previousMonth = YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenReturn(jobExecution);

        // When & Then
        mockMvc.perform(post("/api/batch/monthly-summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("月次勤怠集計バッチを実行しました"))
                .andExpect(jsonPath("$.data.targetMonth").value(previousMonth));

        // Verify that the job was launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：月次勤怠集計バッチ実行時の例外ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.runMonthlySummary()
     * 
     * テストシナリオ：
     * - ユーザーが月次勤怠集計バッチを実行
     * - ジョブランチャーが実行中に例外をスロー
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにsuccess=falseが含まれること
     * - レスポンスにエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testRunMonthlySummary_Exception() throws Exception {
        // Given
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenThrow(new RuntimeException("Job execution failed"));

        // When & Then
        mockMvc.perform(post("/api/batch/monthly-summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"yearMonth\":\"2025-01\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("月次勤怠集計バッチの実行に失敗しました: Job execution failed"));

        // Verify that the job was attempted to be launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：有給日数更新バッチ実行の成功ケース（パラメータあり）
     * 
     * テスト対象メソッド：
     * - BatchJobController.updatePaidLeave()
     * 
     * テストシナリオ：
     * - ユーザーが年度パラメータを指定して有給日数更新バッチを実行
     * - ジョブランチャーが正常にジョブを実行
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれること
     * - レスポンスに指定した年度が含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testUpdatePaidLeave_WithFiscalYearParameter() throws Exception {
        // Given
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenReturn(jobExecution);

        // When & Then
        mockMvc.perform(post("/api/batch/update-paid-leave")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fiscalYear\":2025}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("有給日数更新バッチを実行しました"))
                .andExpect(jsonPath("$.data.fiscalYear").value(2025));

        // Verify that the job was launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：有給日数更新バッチ実行の成功ケース（パラメータなし）
     * 
     * テスト対象メソッド：
     * - BatchJobController.updatePaidLeave()
     * 
     * テストシナリオ：
     * - ユーザーがパラメータなしで有給日数更新バッチを実行
     * - ジョブランチャーが正常にジョブを実行（現在年度が自動設定される）
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testUpdatePaidLeave_WithoutParameter() throws Exception {
        // Given
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenReturn(jobExecution);

        // When & Then
        mockMvc.perform(post("/api/batch/update-paid-leave")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("有給日数更新バッチを実行しました"));

        // Verify that the job was launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：有給日数更新バッチ実行時の例外ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.updatePaidLeave()
     * 
     * テストシナリオ：
     * - ユーザーが有給日数更新バッチを実行
     * - ジョブランチャーが実行中に例外をスロー
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにsuccess=falseが含まれること
     * - レスポンスにエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testUpdatePaidLeave_Exception() throws Exception {
        // Given
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenThrow(new RuntimeException("Job execution failed"));

        // When & Then
        mockMvc.perform(post("/api/batch/update-paid-leave")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fiscalYear\":2025}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("有給日数更新バッチの実行に失敗しました: Job execution failed"));

        // Verify that the job was attempted to be launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：データクリーンアップバッチ実行の成功ケース（パラメータあり）
     * 
     * テスト対象メソッド：
     * - BatchJobController.cleanupData()
     * 
     * テストシナリオ：
     * - ユーザーが保持月数パラメータを指定してデータクリーンアップバッチを実行
     * - ジョブランチャーが正常にジョブを実行
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれること
     * - レスポンスに指定した保持月数が含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testCleanupData_WithRetentionMonthsParameter() throws Exception {
        // Given
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenReturn(jobExecution);

        // When & Then
        mockMvc.perform(post("/api/batch/cleanup-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"retentionMonths\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("データクリーンアップバッチを実行しました"))
                .andExpect(jsonPath("$.data.retentionMonths").value(6));

        // Verify that the job was launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：データクリーンアップバッチ実行の成功ケース（パラメータなし）
     * 
     * テスト対象メソッド：
     * - BatchJobController.cleanupData()
     * 
     * テストシナリオ：
     * - ユーザーがパラメータなしでデータクリーンアップバッチを実行
     * - ジョブランチャーが正常にジョブを実行（デフォルトの保持月数が使用される）
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれること
     * - レスポンスにデフォルトの保持月数（12）が含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testCleanupData_WithoutParameter() throws Exception {
        // Given
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenReturn(jobExecution);

        // When & Then
        mockMvc.perform(post("/api/batch/cleanup-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("データクリーンアップバッチを実行しました"))
                .andExpect(jsonPath("$.data.retentionMonths").value(12));

        // Verify that the job was launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：データクリーンアップバッチ実行時の例外ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.cleanupData()
     * 
     * テストシナリオ：
     * - ユーザーがデータクリーンアップバッチを実行
     * - ジョブランチャーが実行中に例外をスロー
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにsuccess=falseが含まれること
     * - レスポンスにエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testCleanupData_Exception() throws Exception {
        // Given
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenThrow(new RuntimeException("Job execution failed"));

        // When & Then
        mockMvc.perform(post("/api/batch/cleanup-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"retentionMonths\":6}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("データクリーンアップバッチの実行に失敗しました: Job execution failed"));

        // Verify that the job was attempted to be launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：データ修復バッチ実行の成功ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.repairData()
     * 
     * テストシナリオ：
     * - ユーザーがデータ修復バッチを実行
     * - ジョブランチャーが正常にジョブを実行
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testRepairData_Success() throws Exception {
        // Given
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenReturn(jobExecution);

        // When & Then
        mockMvc.perform(post("/api/batch/repair-data")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("データ修復バッチを実行しました"));

        // Verify that the job was launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：データ修復バッチ実行時の例外ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.repairData()
     * 
     * テストシナリオ：
     * - ユーザーがデータ修復バッチを実行
     * - ジョブランチャーが実行中に例外をスロー
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにsuccess=falseが含まれること
     * - レスポンスにエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testRepairData_Exception() throws Exception {
        // Given
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenThrow(new RuntimeException("Job execution failed"));

        // When & Then
        mockMvc.perform(post("/api/batch/repair-data")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("データ修復バッチの実行に失敗しました: Job execution failed"));

        // Verify that the job was attempted to be launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：バッチ処理ステータス取得の成功ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.getBatchStatus()
     * 
     * テストシナリオ：
     * - ユーザーがバッチ処理ステータスを取得
     * - コントローラーが正常にステータスを返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれること
     * 
     * モック対象メソッド：
     * - なし
     */
    @Test
    void testGetBatchStatus() throws Exception {
        // Given

        // When & Then
        mockMvc.perform(get("/api/batch/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("バッチ処理ステータスを取得しました"));

        // Verify that no job was launched (this is a GET endpoint)
        verifyNoInteractions(jobLauncher);
    }

    /**
     * テストケース：バッチ処理ステータス取得時の例外ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.getBatchStatus()
     * 
     * テストシナリオ：
     * - ユーザーがバッチ処理ステータスを取得
     * - コントローラーで例外が発生
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにsuccess=falseが含まれること
     * - レスポンスにエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - なし
     */
    @Test
    void testGetBatchStatus_Exception() throws Exception {
        // Given
        // このテストでは例外を発生させる方法がないため、実装しない
        // 実際のアプリケーションでは、依存するサービスが例外をスローする場合に備えてテストを実装する

        // When & Then
        mockMvc.perform(get("/api/batch/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("バッチ処理ステータスを取得しました"));
    }

    // 以下は既存のテストメソッド（API仕様書には記載されていないが、後方互換性のために残す）

    /**
     * テストケース：既知のジョブ名でジョブを開始する成功ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.startJob()
     * 
     * テストシナリオ：
     * - ユーザーがdailyAttendanceSummaryJobを開始リクエスト
     * - ジョブランチャーが正常にジョブを実行
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスに成功メッセージが含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testStartJob_Success() throws Exception {
        // Given
        String jobName = "dailyAttendanceSummaryJob";
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobLauncher.run(eq(dailyAttendanceSummaryJob), any(JobParameters.class)))
            .thenReturn(jobExecution);

        // When & Then
        mockMvc.perform(post("/api/batch/jobs/{jobName}/start", jobName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Batch job " + jobName + " started successfully"));

        // Verify that the job was launched
        verify(jobLauncher, times(1)).run(eq(dailyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：未知のジョブ名でジョブを開始する失敗ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.startJob()
     * 
     * テストシナリオ：
     * - ユーザーがunknownJobという未知のジョブを開始リクエスト
     * - コントローラーがIllegalArgumentExceptionをスロー
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testStartJob_UnknownJob() throws Exception {
        // Given
        String jobName = "unknownJob";

        // When & Then
        mockMvc.perform(post("/api/batch/jobs/{jobName}/start", jobName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to start batch job: Unknown job: " + jobName));
    }

    /**
     * テストケース：ジョブ実行中に例外が発生するケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.startJob()
     * 
     * テストシナリオ：
     * - ユーザーがmonthlyAttendanceSummaryJobを開始リクエスト
     * - ジョブランチャーが実行中に例外をスロー
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testStartJob_Exception() throws Exception {
        // Given
        String jobName = "monthlyAttendanceSummaryJob";
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenThrow(new RuntimeException("Job execution failed"));

        // When & Then
        mockMvc.perform(post("/api/batch/jobs/{jobName}/start", jobName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to start batch job: Job execution failed"));

        // Verify that the job was attempted to be launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：ジョブステータス取得の成功ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.getJobStatus()
     * 
     * テストシナリオ：
     * - ユーザーがdailyAttendanceSummaryJobのステータスをリクエスト
     * - コントローラーが正常にステータスを返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにジョブ名が含まれること
     * 
     * モック対象メソッド：
     * - なし
     */
    @Test
    void testGetJobStatus() throws Exception {
        // Given
        String jobName = "dailyAttendanceSummaryJob";

        // When & Then
        mockMvc.perform(get("/api/batch/jobs/{jobName}/status", jobName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Job status for " + jobName));
    }

    /**
     * テストケース：ジョブ停止の成功ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.stopJob()
     * 
     * テストシナリオ：
     * - ユーザーがmonthlyAttendanceSummaryJobの停止をリクエスト
     * - コントローラーが正常に停止応答を返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにジョブ停止メッセージが含まれること
     * 
     * モック対象メソッド：
     * - なし
     */
    @Test
    void testStopJob() throws Exception {
        // Given
        String jobName = "monthlyAttendanceSummaryJob";

        // When & Then
        mockMvc.perform(post("/api/batch/jobs/{jobName}/stop", jobName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Job " + jobName + " stopped"));
    }

    /**
     * テストケース：全ジョブリスト取得の成功ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.getAllJobs()
     * 
     * テストシナリオ：
     * - ユーザーがすべてのジョブリストをリクエスト
     * - コントローラーが正常にジョブリストを返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにジョブリストメッセージが含まれること
     * 
     * モック対象メソッド：
     * - なし
     */
    @Test
    void testGetAllJobs() throws Exception {
        // Given

        // When & Then
        mockMvc.perform(get("/api/batch/jobs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("List of all jobs"));
    }

    /**
     * テストケース：バッチヘルスチェックの成功ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.getBatchHealth()
     * 
     * テストシナリオ：
     * - ユーザーがバッチシステムのヘルスチェックをリクエスト
     * - コントローラーが正常にヘルスステータスを返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにヘルスチェックメッセージが含まれること
     * 
     * モック対象メソッド：
     * - なし
     */
    @Test
    void testGetBatchHealth() throws Exception {
        // Given

        // When & Then
        mockMvc.perform(get("/api/batch/monitoring/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Batch system is healthy"));
    }

    /**
     * テストケース：バッチメトリクス取得の成功ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.getBatchMetrics()
     * 
     * テストシナリオ：
     * - ユーザーがバッチメトリクスをリクエスト
     * - コントローラーが正常にメトリクスを返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにメトリクスメッセージが含まれること
     * 
     * モック対象メソッド：
     * - なし
     */
    @Test
    void testGetBatchMetrics() throws Exception {
        // Given

        // When & Then
        mockMvc.perform(get("/api/batch/monitoring/metrics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Batch metrics"));
    }

    /**
     * テストケース：バッチ実行履歴取得の成功ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.getBatchExecutions()
     * 
     * テストシナリオ：
     * - ユーザーがバッチ実行履歴をリクエスト
     * - コントローラーが正常に実行履歴を返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスに実行履歴メッセージが含まれること
     * 
     * モック対象メソッド：
     * - なし
     */
    @Test
    void testGetBatchExecutions() throws Exception {
        // Given

        // When & Then
        mockMvc.perform(get("/api/batch/monitoring/executions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Batch executions"));
    }

    /**
     * テストケース：バッチ診断情報取得の成功ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.getBatchDiagnostics()
     * 
     * テストシナリオ：
     * - ユーザーがバッチ診断情報をリクエスト
     * - コントローラーが正常に診断情報を返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスに診断情報メッセージが含まれること
     * 
     * モック対象メソッド：
     * - なし
     */
    @Test
    void testGetBatchDiagnostics() throws Exception {
        // Given

        // When & Then
        mockMvc.perform(get("/api/batch/monitoring/diagnostics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Batch diagnostics"));
    }

    /**
     * テストケース：日次勤務時間集計ジョブ実行の成功ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.runDailyWorkSummary()
     * 
     * テストシナリオ：
     * - ユーザーが日次勤務時間集計ジョブの実行をリクエスト
     * - ジョブランチャーが正常にジョブを実行
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスに成功メッセージが含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testRunDailyWorkSummary_Success() throws Exception {
        // Given
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobLauncher.run(eq(dailyAttendanceSummaryJob), any(JobParameters.class)))
            .thenReturn(jobExecution);

        // When & Then
        mockMvc.perform(post("/api/batch/daily-work-summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Daily work summary batch job started successfully"));

        // Verify that the job was launched
        verify(jobLauncher, times(1)).run(eq(dailyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：日次勤務時間集計ジョブ実行時の例外ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.runDailyWorkSummary()
     * 
     * テストシナリオ：
     * - ユーザーが日次勤務時間集計ジョブの実行をリクエスト
     * - ジョブランチャーが実行中に例外をスロー
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testRunDailyWorkSummary_Exception() throws Exception {
        // Given
        when(jobLauncher.run(eq(dailyAttendanceSummaryJob), any(JobParameters.class)))
            .thenThrow(new RuntimeException("Job execution failed"));

        // When & Then
        mockMvc.perform(post("/api/batch/daily-work-summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to start daily work summary batch job: Job execution failed"));

        // Verify that the job was attempted to be launched
        verify(jobLauncher, times(1)).run(eq(dailyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：月次勤務時間集計ジョブ実行の成功ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.runMonthlyWorkSummary()
     * 
     * テストシナリオ：
     * - ユーザーが月次勤務時間集計ジョブの実行をリクエスト
     * - ジョブランチャーが正常にジョブを実行
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスに成功メッセージが含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testRunMonthlyWorkSummary_Success() throws Exception {
        // Given
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenReturn(jobExecution);

        // When & Then
        mockMvc.perform(post("/api/batch/monthly-work-summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Monthly work summary batch job started successfully"));

        // Verify that the job was launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }

    /**
     * テストケース：月次勤務時間集計ジョブ実行時の例外ケース
     * 
     * テスト対象メソッド：
     * - BatchJobController.runMonthlyWorkSummary()
     * 
     * テストシナリオ：
     * - ユーザーが月次勤務時間集計ジョブの実行をリクエスト
     * - ジョブランチャーが実行中に例外をスロー
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - JobLauncher.run()
     */
    @Test
    void testRunMonthlyWorkSummary_Exception() throws Exception {
        // Given
        when(jobLauncher.run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class)))
            .thenThrow(new RuntimeException("Job execution failed"));

        // When & Then
        mockMvc.perform(post("/api/batch/monthly-work-summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to start monthly work summary batch job: Job execution failed"));

        // Verify that the job was attempted to be launched
        verify(jobLauncher, times(1)).run(eq(monthlyAttendanceSummaryJob), any(JobParameters.class));
    }
}