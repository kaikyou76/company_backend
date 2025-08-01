package com.example.companybackend.controller;

import com.example.companybackend.service.BatchMonitoringService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * バッチ管理コントローラーテストクラス
 * 
 * テスト対象：
 * - テストファイル：BatchManagementController.java
 * - テストクラス：com.example.companybackend.controller.BatchManagementController
 * - モック対象：BatchMonitoringService（バッチ監視サービスクラス）
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
public class BatchManagementControllerTest {

    /**
     * MockMvcはSpring MVCテスト用のクライアント
     * HTTPリクエストのシミュレーションとレスポンスの検証に使用
     */
    @InjectMocks
    private BatchManagementController batchManagementController;

    /**
     * BatchMonitoringServiceのモックオブジェクト
     * データアクセス層をモック化し、コントローラーロジックのみをテスト
     */
    @Mock
    private BatchMonitoringService batchMonitoringService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(batchManagementController).build();
        objectMapper = new ObjectMapper();
    }

    /**
     * テストケース：全ジョブインスタンス取得の成功
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getJobInstances()
     * 
     * テストシナリオ：
     * - ユーザーが全ジョブインスタンスをリクエスト
     * - サービス層が正常にジョブインスタンスリストを返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスにtotalCountが含まれる
     * - レスポンスにinstancesリストが含まれ、データが正しいこと
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getAllJobInstances()
     */
    @Test
    void testGetJobInstances_Success() throws Exception {
        // Given
        List<Map<String, Object>> instances = new ArrayList<>();
        Map<String, Object> instance = new HashMap<>();
        instance.put("jobInstanceId", 1L);
        instance.put("jobName", "monthlyAttendanceSummaryJob");
        instances.add(instance);
        
        when(batchMonitoringService.getAllJobInstances()).thenReturn(instances);

        // When & Then
        mockMvc.perform(get("/api/v1/batch/instances")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.instances[0].jobInstanceId").value(1))
                .andExpect(jsonPath("$.instances[0].jobName").value("monthlyAttendanceSummaryJob"));
    }

    /**
     * テストケース：全ジョブインスタンス取得時の例外発生
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getJobInstances()
     * 
     * テストシナリオ：
     * - ユーザーが全ジョブインスタンスをリクエスト
     * - サービス層で例外が発生
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにsuccess=falseが含まれる
     * - レスポンスに適切なエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getAllJobInstances()
     */
    @Test
    void testGetJobInstances_Exception() throws Exception {
        // Given
        when(batchMonitoringService.getAllJobInstances()).thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        mockMvc.perform(get("/api/v1/batch/instances")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("ジョブインスタンス取得に失敗しました: Database connection error"));
    }

    /**
     * テストケース：ジョブ実行履歴取得の成功
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getJobExecutionHistory()
     * 
     * テストシナリオ：
     * - ユーザーが指定されたジョブ名の実行履歴をリクエスト
     * - サービス層が正常にジョブ実行履歴リストを返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスにjobName、page、size、totalCountが含まれる
     * - レスポンスにexecutionsリストが含まれ、データが正しいこと
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getJobExecutionHistory()
     */
    @Test
    void testGetJobExecutionHistory_Success() throws Exception {
        // Given
        String jobName = "monthlyAttendanceSummaryJob";
        List<Map<String, Object>> executions = new ArrayList<>();
        Map<String, Object> execution = new HashMap<>();
        execution.put("jobExecutionId", 1L);
        execution.put("jobInstanceId", 1L);
        execution.put("startTime", DATE_TIME_FORMATTER.format(LocalDateTime.now().atZone(ZoneId.systemDefault())));
        execution.put("endTime", DATE_TIME_FORMATTER.format(LocalDateTime.now().plusSeconds(45).atZone(ZoneId.systemDefault())));
        execution.put("status", "COMPLETED");
        execution.put("exitCode", "COMPLETED");
        executions.add(execution);
        
        when(batchMonitoringService.getJobExecutionHistory(eq(jobName), anyInt(), anyInt())).thenReturn(executions);

        // When & Then
        mockMvc.perform(get("/api/v1/batch/executions/{jobName}", jobName)
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.jobName").value(jobName))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.executions[0].jobExecutionId").value(1))
                .andExpect(jsonPath("$.executions[0].jobInstanceId").value(1))
                .andExpect(jsonPath("$.executions[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.executions[0].exitCode").value("COMPLETED"));
    }

    /**
     * テストケース：ジョブ実行履歴取得時の例外発生
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getJobExecutionHistory()
     * 
     * テストシナリオ：
     * - ユーザーが指定されたジョブ名の実行履歴をリクエスト
     * - サービス層で例外が発生
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにsuccess=falseが含まれる
     * - レスポンスに適切なエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getJobExecutionHistory()
     */
    @Test
    void testGetJobExecutionHistory_Exception() throws Exception {
        // Given
        String jobName = "monthlyAttendanceSummaryJob";
        when(batchMonitoringService.getJobExecutionHistory(eq(jobName), anyInt(), anyInt()))
            .thenThrow(new RuntimeException("Job not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/batch/executions/{jobName}", jobName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("ジョブ実行履歴取得に失敗しました: Job not found"));
    }

    /**
     * テストケース：ステップ実行履歴取得の成功
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getStepExecutionHistory()
     * 
     * テストシナリオ：
     * - ユーザーが指定されたジョブ実行IDのステップ実行履歴をリクエスト
     * - サービス層が正常にステップ実行履歴リストを返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスにjobExecutionId、totalCountが含まれる
     * - レスポンスにstepsリストが含まれ、データが正しいこと
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getStepExecutionHistory()
     */
    @Test
    void testGetStepExecutionHistory_Success() throws Exception {
        // Given
        Long jobExecutionId = 1L;
        List<Map<String, Object>> steps = new ArrayList<>();
        Map<String, Object> step = new HashMap<>();
        step.put("stepExecutionId", 1L);
        step.put("stepName", "processUsersStep");
        step.put("startTime", DATE_TIME_FORMATTER.format(LocalDateTime.now().atZone(ZoneId.systemDefault())));
        step.put("endTime", DATE_TIME_FORMATTER.format(LocalDateTime.now().plusSeconds(30).atZone(ZoneId.systemDefault())));
        step.put("status", "COMPLETED");
        step.put("commitCount", 5);
        step.put("readCount", 50);
        step.put("writeCount", 50);
        step.put("exitCode", "COMPLETED");
        steps.add(step);
        
        when(batchMonitoringService.getStepExecutionHistory(jobExecutionId)).thenReturn(steps);

        // When & Then
        mockMvc.perform(get("/api/v1/batch/steps/{jobExecutionId}", jobExecutionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.jobExecutionId").value(jobExecutionId))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.steps[0].stepExecutionId").value(1))
                .andExpect(jsonPath("$.steps[0].stepName").value("processUsersStep"))
                .andExpect(jsonPath("$.steps[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.steps[0].commitCount").value(5))
                .andExpect(jsonPath("$.steps[0].readCount").value(50))
                .andExpect(jsonPath("$.steps[0].writeCount").value(50))
                .andExpect(jsonPath("$.steps[0].exitCode").value("COMPLETED"));
    }

    /**
     * テストケース：ステップ実行履歴取得時の例外発生
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getStepExecutionHistory()
     * 
     * テストシナリオ：
     * - ユーザーが指定されたジョブ実行IDのステップ実行履歴をリクエスト
     * - サービス層で例外が発生
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにsuccess=falseが含まれる
     * - レスポンスに適切なエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getStepExecutionHistory()
     */
    @Test
    void testGetStepExecutionHistory_Exception() throws Exception {
        // Given
        Long jobExecutionId = 1L;
        when(batchMonitoringService.getStepExecutionHistory(jobExecutionId))
            .thenThrow(new RuntimeException("Execution not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/batch/steps/{jobExecutionId}", jobExecutionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("ステップ実行履歴取得に失敗しました: Execution not found"));
    }

    /**
     * テストケース：バッチ実行統計取得の成功
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getBatchStatistics()
     * 
     * テストシナリオ：
     * - ユーザーがバッチ実行統計をリクエスト
     * - サービス層が正常に統計情報を返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスにstatisticsオブジェクトが含まれ、データが正しいこと
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getBatchExecutionStatistics()
     */
    @Test
    void testGetBatchStatistics_Success() throws Exception {
        // Given
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalJobs", 5);
        statistics.put("successRate", 100.0);
        statistics.put("errorRate", 0.0);
        
        when(batchMonitoringService.getBatchExecutionStatistics()).thenReturn(statistics);

        // When & Then
        mockMvc.perform(get("/api/v1/batch/statistics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statistics.totalJobs").value(5))
                .andExpect(jsonPath("$.statistics.successRate").value(100.0))
                .andExpect(jsonPath("$.statistics.errorRate").value(0.0));
    }

    /**
     * テストケース：バッチ実行統計取得時の例外発生
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getBatchStatistics()
     * 
     * テストシナリオ：
     * - ユーザーがバッチ実行統計をリクエスト
     * - サービス層で例外が発生
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにsuccess=falseが含まれる
     * - レスポンスに適切なエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getBatchExecutionStatistics()
     */
    @Test
    void testGetBatchStatistics_Exception() throws Exception {
        // Given
        when(batchMonitoringService.getBatchExecutionStatistics())
            .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        mockMvc.perform(get("/api/v1/batch/statistics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("バッチ統計取得に失敗しました: Service unavailable"));
    }

    /**
     * テストケース：実行中ジョブ取得の成功
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getRunningJobs()
     * 
     * テストシナリオ：
     * - ユーザーが実行中のジョブをリクエスト
     * - サービス層が正常に実行中ジョブリストを返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスにtotalCountが含まれる
     * - レスポンスにrunningJobsリストが含まれ、データが正しいこと
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getRunningJobs()
     */
    @Test
    void testGetRunningJobs_Success() throws Exception {
        // Given
        List<Map<String, Object>> runningJobs = new ArrayList<>();
        Map<String, Object> job = new HashMap<>();
        job.put("jobExecutionId", 3L);
        job.put("jobName", "dataCleanupJob");
        job.put("status", "STARTED");
        runningJobs.add(job);
        
        when(batchMonitoringService.getRunningJobs()).thenReturn(runningJobs);

        // When & Then
        mockMvc.perform(get("/api/v1/batch/running")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.runningJobs[0].jobExecutionId").value(3))
                .andExpect(jsonPath("$.runningJobs[0].jobName").value("dataCleanupJob"))
                .andExpect(jsonPath("$.runningJobs[0].status").value("STARTED"));
    }

    /**
     * テストケース：実行中ジョブ取得時の例外発生
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getRunningJobs()
     * 
     * テストシナリオ：
     * - ユーザーが実行中のジョブをリクエスト
     * - サービス層で例外が発生
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにsuccess=falseが含まれる
     * - レスポンスに適切なエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getRunningJobs()
     */
    @Test
    void testGetRunningJobs_Exception() throws Exception {
        // Given
        when(batchMonitoringService.getRunningJobs())
            .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(get("/api/v1/batch/running")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("実行中ジョブ取得に失敗しました: Service error"));
    }

    /**
     * テストケース：ジョブ名一覧取得の成功
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getJobNames()
     * 
     * テストシナリオ：
     * - ユーザーがジョブ名一覧をリクエスト
     * - サービス層が正常にジョブ名リストを返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスにtotalCountが含まれる
     * - レスポンスにjobNamesリストが含まれ、データが正しいこと
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getJobNames()
     */
    @Test
    void testGetJobNames_Success() throws Exception {
        // Given
        List<String> jobNames = Arrays.asList(
            "dailyAttendanceSummaryJob",
            "monthlyAttendanceSummaryJob", 
            "paidLeaveUpdateJob", 
            "dataCleanupJob", 
            "dataRepairJob"
        );
        when(batchMonitoringService.getJobNames()).thenReturn(jobNames);

        // When & Then
        mockMvc.perform(get("/api/v1/batch/job-names")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalCount").value(5))
                .andExpect(jsonPath("$.jobNames[0]").value("dailyAttendanceSummaryJob"))
                .andExpect(jsonPath("$.jobNames[1]").value("monthlyAttendanceSummaryJob"))
                .andExpect(jsonPath("$.jobNames[2]").value("paidLeaveUpdateJob"))
                .andExpect(jsonPath("$.jobNames[3]").value("dataCleanupJob"))
                .andExpect(jsonPath("$.jobNames[4]").value("dataRepairJob"));
    }

    /**
     * テストケース：ジョブ名一覧取得時の例外発生
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getJobNames()
     * 
     * テストシナリオ：
     * - ユーザーがジョブ名一覧をリクエスト
     * - サービス層で例外が発生
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにsuccess=falseが含まれる
     * - レスポンスに適切なエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getJobNames()
     */
    @Test
    void testGetJobNames_Exception() throws Exception {
        // Given
        when(batchMonitoringService.getJobNames())
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/api/v1/batch/job-names")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("ジョブ名一覧取得に失敗しました: Database error"));
    }

    /**
     * テストケース：特定ジョブの最新実行情報取得の成功
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getLatestJobExecution()
     * 
     * テストシナリオ：
     * - ユーザーが指定されたジョブ名の最新実行情報をリクエスト
     * - サービス層が正常に最新実行情報を返却
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスにjobNameが含まれる
     * - レスポンスにlatestExecutionオブジェクトが含まれ、データが正しいこと
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getLatestJobExecution()
     */
    @Test
    void testGetLatestJobExecution_Success() throws Exception {
        // Given
        String jobName = "monthlyAttendanceSummaryJob";
        Map<String, Object> latestExecution = new HashMap<>();
        latestExecution.put("jobExecutionId", 1L);
        latestExecution.put("jobName", jobName);
        latestExecution.put("status", "COMPLETED");
        
        when(batchMonitoringService.getLatestJobExecution(jobName)).thenReturn(latestExecution);

        // When & Then
        mockMvc.perform(get("/api/v1/batch/latest/{jobName}", jobName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.jobName").value(jobName))
                .andExpect(jsonPath("$.latestExecution.jobExecutionId").value(1))
                .andExpect(jsonPath("$.latestExecution.jobName").value(jobName))
                .andExpect(jsonPath("$.latestExecution.status").value("COMPLETED"));
    }

    /**
     * テストケース：特定ジョブの最新実行情報取得時の例外発生
     * 
     * テスト対象メソッド：
     * - BatchManagementController.getLatestJobExecution()
     * 
     * テストシナリオ：
     * - ユーザーが指定されたジョブ名の最新実行情報をリクエスト
     * - サービス層で例外が発生
     * 
     * 期待結果：
     * - HTTPステータスコード：500 INTERNAL SERVER ERROR
     * - レスポンスにsuccess=falseが含まれる
     * - レスポンスに適切なエラーメッセージが含まれること
     * 
     * モック対象メソッド：
     * - BatchMonitoringService.getLatestJobExecution()
     */
    @Test
    void testGetLatestJobExecution_Exception() throws Exception {
        // Given
        String jobName = "monthlyAttendanceSummaryJob";
        when(batchMonitoringService.getLatestJobExecution(jobName))
            .thenThrow(new RuntimeException("Job not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/batch/latest/{jobName}", jobName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("最新ジョブ実行情報取得に失敗しました: Job not found"));
    }
}