package com.example.companybackend.controller;

import com.example.companybackend.batch.service.BatchJobService;
import com.example.companybackend.service.BatchStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BatchControllerのユニットテストクラス
 * 
 * このテストクラスは、BatchControllerの各エンドポイントの動作を検証します。
 * Spring Securityの@PreAuthorizeアノテーションによる認可制御は、
 * standaloneSetupによるテスト環境では適用されないため、
 * 認可チェックは行わず、各エンドポイントの基本的な動作のみをテストします。
 */
@ExtendWith(MockitoExtension.class)
public class BatchControllerTest {

    @InjectMocks
    private BatchController batchController;

    @Mock
    private BatchJobService batchJobService;

    @Mock
    private BatchStatusService batchStatusService;
    
    @Mock
    private JobLauncher jobLauncher;
    
    @Mock
    @Qualifier("dailyAttendanceSummaryJob")
    private Job dailyAttendanceSummaryJob;
    
    @Mock
    @Qualifier("monthlyAttendanceSummaryJob")
    private Job monthlyAttendanceSummaryJob;
    
    @Mock
    @Qualifier("paidLeaveUpdateJob")
    private Job paidLeaveUpdateJob;
    
    @Mock
    @Qualifier("dataCleanupJob")
    private Job dataCleanupJob;
    
    @Mock
    @Qualifier("dataRepairJob")
    private Job dataRepairJob;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Manually inject mocked Job beans using reflection
        ReflectionTestUtils.setField(batchController, "dailyAttendanceSummaryJob", dailyAttendanceSummaryJob);
        ReflectionTestUtils.setField(batchController, "monthlyAttendanceSummaryJob", monthlyAttendanceSummaryJob);
        ReflectionTestUtils.setField(batchController, "paidLeaveUpdateJob", paidLeaveUpdateJob);
        ReflectionTestUtils.setField(batchController, "dataCleanupJob", dataCleanupJob);
        ReflectionTestUtils.setField(batchController, "dataRepairJob", dataRepairJob);
        ReflectionTestUtils.setField(batchController, "jobLauncher", jobLauncher);
        
        mockMvc = MockMvcBuilders.standaloneSetup(batchController).build();
        objectMapper = new ObjectMapper();
    }

    /**
     * 月次勤怠集計バッチの正常実行テスト
     */
    @Test
    void testExecuteMonthlySummaryBatch_Success() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("yearMonth", "2025-01");

        // When & Then
        mockMvc.perform(post("/api/batch/monthly-summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(parameters)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("月次勤怠集計バッチを実行しました"));
    }

    /**
     * 月次勤怠集計バッチの例外発生時のテスト
     */
    @Test
    void testExecuteMonthlySummaryBatch_Exception() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("yearMonth", "2025-01");
        
        doThrow(new RuntimeException("Batch execution failed"))
                .when(batchJobService).runJob(any(), any());

        // When & Then
        mockMvc.perform(post("/api/batch/monthly-summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(parameters)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("月次勤怠集計バッチの実行に失敗しました: Batch execution failed"));
    }

    /**
     * 有給日数更新バッチの正常実行テスト
     */
    @Test
    void testExecutePaidLeaveUpdateBatch_Success() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("fiscalYear", 2025);

        // When & Then
        mockMvc.perform(post("/api/batch/update-paid-leave")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(parameters)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("有給日数更新バッチを実行しました"));
    }

    /**
     * データクリーンアップバッチの正常実行テスト
     */
    @Test
    void testExecuteDataCleanupBatch_Success() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retentionMonths", 12);

        // When & Then
        mockMvc.perform(post("/api/batch/cleanup-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(parameters)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("データクリーンアップバッチを実行しました"));
    }

    /**
     * データ修復バッチの正常実行テスト
     */
    @Test
    void testExecuteDataRepairBatch_Success() throws Exception {
        // Given & When & Then
        mockMvc.perform(post("/api/batch/repair-data")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("データ修復バッチを実行しました"));
    }

    /**
     * バッチステータス取得の正常実行テスト
     */
    @Test
    void testGetBatchStatus_Success() throws Exception {
        // Given - mocked service returns normal response

        // When & Then
        mockMvc.perform(get("/api/batch/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /**
     * 月次勤怠集計バッチの認可なし実行テスト（standaloneSetup環境では認可チェックされない）
     */
    @Test
    void testExecuteMonthlySummaryBatch_NoAuth() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("yearMonth", "2025-01");

        // When & Then  
        mockMvc.perform(post("/api/batch/monthly-summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(parameters)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * バッチステータス取得の認可なし実行テスト（standaloneSetup環境では認可チェックされない）
     */
    @Test 
    void testGetBatchStatus_NoAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/batch/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}