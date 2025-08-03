package com.example.companybackend.controller;

import com.example.companybackend.dto.request.CreateTimeCorrectionRequest;
import com.example.companybackend.dto.response.CreateTimeCorrectionResponse;
import com.example.companybackend.dto.response.ApproveTimeCorrectionResponse;
import com.example.companybackend.dto.response.RejectTimeCorrectionResponse;
import com.example.companybackend.entity.TimeCorrection;
import com.example.companybackend.service.TimeCorrectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 打刻修正コントローラーテストクラス
 * 
 * テスト対象：
 * - テストファイル：TimeCorrectionController.java
 * - テストクラス：com.example.companybackend.controller.TimeCorrectionController
 * - モック対象：TimeCorrectionService（打刻修正サービスクラス）
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
public class TimeCorrectionControllerTest {

    /**
     * MockMvcはSpring MVCテスト用のクライアント
     * HTTPリクエストのシミュレーションとレスポンスの検証に使用
     */
    @InjectMocks
    private TimeCorrectionController timeCorrectionController;

    /**
     * TimeCorrectionServiceのモックオブジェクト
     * 打刻修正サービスをモック化し、コントローラーロジックのみをテスト
     */
    @Mock
    private TimeCorrectionService timeCorrectionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(timeCorrectionController).build();
    }

    /**
     * テストケース：打刻修正申請作成の成功
     * 
     * テスト対象メソッド：
     * - TimeCorrectionController.createTimeCorrection()
     * 
     * テストシナリオ：
     * - ユーザーが打刻修正申請を作成
     * - サービスが正常に申請を作成
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスに作成された申請データが含まれる
     * 
     * モック対象メソッド：
     * - TimeCorrectionService.createTimeCorrection()
     */
    @Test
    void testCreateTimeCorrection_Success() throws Exception {
        // Given
        TimeCorrection timeCorrection = new TimeCorrection();
        timeCorrection.setId(1L);
        timeCorrection.setUserId(1);
        timeCorrection.setAttendanceId(1L);
        timeCorrection.setRequestType("time");
        timeCorrection.setCurrentType("in");
        timeCorrection.setReason("Forgot to clock out");
        timeCorrection.setStatus("pending");
        timeCorrection.setCreatedAt(OffsetDateTime.now());
        
        CreateTimeCorrectionResponse serviceResponse = CreateTimeCorrectionResponse.success(timeCorrection);
        when(timeCorrectionService.createTimeCorrection(any(CreateTimeCorrectionRequest.class), anyLong()))
            .thenReturn(serviceResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/time-corrections")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"attendanceId\":1,\"requestType\":\"time\",\"currentType\":\"in\",\"reason\":\"Forgot to clock out\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("打刻修正申請が正常に作成されました"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.attendanceId").value(1));

        // Verify that the service method was called
        verify(timeCorrectionService, times(1)).createTimeCorrection(any(CreateTimeCorrectionRequest.class), anyLong());
    }

    /**
     * テストケース：打刻修正申請作成の失敗
     * 
     * テスト対象メソッド：
     * - TimeCorrectionController.createTimeCorrection()
     * 
     * テストシナリオ：
     * - ユーザーが打刻修正申請を作成
     * - サービスがエラーを返す
     * 
     * 期待結果：
     * - HTTPステータスコード：400 Bad Request
     * - レスポンスにsuccess=falseが含まれる
     * - レスポンスにエラーメッセージが含まれる
     * 
     * モック対象メソッド：
     * - TimeCorrectionService.createTimeCorrection()
     */
    @Test
    void testCreateTimeCorrection_Failure() throws Exception {
        // Given
        CreateTimeCorrectionResponse serviceResponse = CreateTimeCorrectionResponse.error("打刻記録が見つかりません");
        when(timeCorrectionService.createTimeCorrection(any(CreateTimeCorrectionRequest.class), anyLong()))
            .thenReturn(serviceResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/time-corrections")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"attendanceId\":1,\"requestType\":\"time\",\"currentType\":\"in\",\"reason\":\"Forgot to clock out\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("打刻記録が見つかりません"));

        // Verify that the service method was called
        verify(timeCorrectionService, times(1)).createTimeCorrection(any(CreateTimeCorrectionRequest.class), anyLong());
    }

    /**
     * テストケース：打刻修正申請承認の成功
     * 
     * テスト対象メソッド：
     * - TimeCorrectionController.approveTimeCorrection()
     * 
     * テストシナリオ：
     * - 管理者が打刻修正申請を承認
     * - サービスが正常に申請を承認
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスに承認された申請データが含まれる
     * 
     * モック対象メソッド：
     * - TimeCorrectionService.approveTimeCorrection()
     */
    @Test
    void testApproveTimeCorrection_Success() throws Exception {
        // Given
        Long correctionId = 1L;
        Long approverId = 2L;
        
        TimeCorrection timeCorrection = new TimeCorrection();
        timeCorrection.setId(correctionId);
        timeCorrection.setStatus("approved");
        timeCorrection.setApproverId(2);
        timeCorrection.setApprovedAt(OffsetDateTime.now());
        
        ApproveTimeCorrectionResponse serviceResponse = new ApproveTimeCorrectionResponse(true, "申請を承認しました", timeCorrection);
        when(timeCorrectionService.approveTimeCorrection(eq(correctionId), eq(approverId)))
            .thenReturn(serviceResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/time-corrections/{id}/approve", correctionId)
                .header("X-User-Id", approverId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("打刻修正申請が承認されました"))
                .andExpect(jsonPath("$.data.id").value(correctionId))
                .andExpect(jsonPath("$.data.status").value("approved"));

        // Verify that the service method was called
        verify(timeCorrectionService, times(1)).approveTimeCorrection(eq(correctionId), eq(approverId));
    }

    /**
     * テストケース：打刻修正申請承認の失敗
     * 
     * テスト対象メソッド：
     * - TimeCorrectionController.approveTimeCorrection()
     * 
     * テストシナリオ：
     * - 管理者が打刻修正申請を承認
     * - サービスがエラーを返す
     * 
     * 期待結果：
     * - HTTPステータスコード：400 Bad Request
     * - レスポンスにsuccess=falseが含まれる
     * - レスポンスにエラーメッセージが含まれる
     * 
     * モック対象メソッド：
     * - TimeCorrectionService.approveTimeCorrection()
     */
    @Test
    void testApproveTimeCorrection_Failure() throws Exception {
        // Given
        Long correctionId = 1L;
        Long approverId = 2L;
        
        ApproveTimeCorrectionResponse serviceResponse = new ApproveTimeCorrectionResponse(false, "申請が見つかりません", null);
        when(timeCorrectionService.approveTimeCorrection(eq(correctionId), eq(approverId)))
            .thenReturn(serviceResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/time-corrections/{id}/approve", correctionId)
                .header("X-User-Id", approverId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("申請が見つかりません"));

        // Verify that the service method was called
        verify(timeCorrectionService, times(1)).approveTimeCorrection(eq(correctionId), eq(approverId));
    }

    /**
     * テストケース：打刻修正申請拒否の成功
     * 
     * テスト対象メソッド：
     * - TimeCorrectionController.rejectTimeCorrection()
     * 
     * テストシナリオ：
     * - 管理者が打刻修正申請を拒否
     * - サービスが正常に申請を拒否
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスに拒否された申請データが含まれる
     * 
     * モック対象メソッド：
     * - TimeCorrectionService.rejectTimeCorrection()
     */
    @Test
    void testRejectTimeCorrection_Success() throws Exception {
        // Given
        Long correctionId = 1L;
        Long approverId = 2L;
        
        TimeCorrection timeCorrection = new TimeCorrection();
        timeCorrection.setId(correctionId);
        timeCorrection.setStatus("rejected");
        timeCorrection.setApproverId(2);
        timeCorrection.setApprovedAt(OffsetDateTime.now());
        
        RejectTimeCorrectionResponse serviceResponse = new RejectTimeCorrectionResponse(true, "申請を拒否しました", timeCorrection);
        when(timeCorrectionService.rejectTimeCorrection(eq(correctionId), eq(approverId)))
            .thenReturn(serviceResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/time-corrections/{id}/reject", correctionId)
                .header("X-User-Id", approverId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("打刻修正申請が拒否されました"))
                .andExpect(jsonPath("$.data.id").value(correctionId))
                .andExpect(jsonPath("$.data.status").value("rejected"));

        // Verify that the service method was called
        verify(timeCorrectionService, times(1)).rejectTimeCorrection(eq(correctionId), eq(approverId));
    }

    /**
     * テストケース：ユーザーの打刻修正申請一覧取得の成功
     * 
     * テスト対象メソッド：
     * - TimeCorrectionController.getUserTimeCorrections()
     * 
     * テストシナリオ：
     * - ユーザーが自分の打刻修正申請一覧を取得
     * - サービスが正常に申請一覧を返す
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスに申請一覧データが含まれる
     * 
     * モック対象メソッド：
     * - TimeCorrectionService.getUserTimeCorrections()
     */
    @Test
    void testGetUserTimeCorrections_Success() throws Exception {
        // Given
        Long userId = 1L;
        
        TimeCorrection correction1 = new TimeCorrection();
        correction1.setId(1L);
        correction1.setUserId(1);
        correction1.setStatus("pending");
        
        TimeCorrection correction2 = new TimeCorrection();
        correction2.setId(2L);
        correction2.setUserId(1);
        correction2.setStatus("approved");
        
        when(timeCorrectionService.getUserTimeCorrections(eq(userId)))
            .thenReturn(Arrays.asList(correction1, correction2));

        // When & Then
        mockMvc.perform(get("/api/v1/time-corrections/user")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ユーザーの打刻修正申請一覧を取得しました"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.corrections[0].id").value(1))
                .andExpect(jsonPath("$.data.corrections[1].id").value(2));

        // Verify that the service method was called
        verify(timeCorrectionService, times(1)).getUserTimeCorrections(eq(userId));
    }

    /**
     * テストケース：承認待ち申請一覧取得の成功
     * 
     * テスト対象メソッド：
     * - TimeCorrectionController.getPendingTimeCorrections()
     * 
     * テストシナリオ：
     * - 管理者が承認待ち申請一覧を取得
     * - サービスが正常に申請一覧を返す
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスに申請一覧データが含まれる
     * 
     * モック対象メソッド：
     * - TimeCorrectionService.getPendingTimeCorrections()
     */
    @Test
    void testGetPendingTimeCorrections_Success() throws Exception {
        // Given
        TimeCorrection correction1 = new TimeCorrection();
        correction1.setId(1L);
        correction1.setUserId(1);
        correction1.setStatus("pending");
        
        TimeCorrection correction2 = new TimeCorrection();
        correction2.setId(2L);
        correction2.setUserId(2);
        correction2.setStatus("pending");
        
        when(timeCorrectionService.getPendingTimeCorrections())
            .thenReturn(Arrays.asList(correction1, correction2));

        // When & Then
        mockMvc.perform(get("/api/v1/time-corrections/pending")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("承認待ち申請一覧を取得しました"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.corrections[0].id").value(1))
                .andExpect(jsonPath("$.data.corrections[1].id").value(2));

        // Verify that the service method was called
        verify(timeCorrectionService, times(1)).getPendingTimeCorrections();
    }

    /**
     * テストケース：申請詳細取得の成功
     * 
     * テスト対象メソッド：
     * - TimeCorrectionController.getTimeCorrectionById()
     * 
     * テストシナリオ：
     * - ユーザーが特定の申請詳細を取得
     * - サービスが正常に申請を返す
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスに申請データが含まれる
     * 
     * モック対象メソッド：
     * - TimeCorrectionService.getTimeCorrectionById()
     */
    @Test
    void testGetTimeCorrectionById_Success() throws Exception {
        // Given
        Long correctionId = 1L;
        
        TimeCorrection correction = new TimeCorrection();
        correction.setId(correctionId);
        correction.setUserId(1);
        correction.setStatus("pending");
        
        when(timeCorrectionService.getTimeCorrectionById(eq(correctionId)))
            .thenReturn(Optional.of(correction));

        // When & Then
        mockMvc.perform(get("/api/v1/time-corrections/{id}", correctionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("申請詳細を取得しました"))
                .andExpect(jsonPath("$.data.id").value(correctionId))
                .andExpect(jsonPath("$.data.userId").value(1));

        // Verify that the service method was called
        verify(timeCorrectionService, times(1)).getTimeCorrectionById(eq(correctionId));
    }

    /**
     * テストケース：申請詳細取得で申請が見つからない場合
     * 
     * テスト対象メソッド：
     * - TimeCorrectionController.getTimeCorrectionById()
     * 
     * テストシナリオ：
     * - ユーザーが存在しない申請の詳細を取得
     * - サービスが空のOptionalを返す
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=falseが含まれる
     * - レスポンスにエラーメッセージが含まれる
     * 
     * モック対象メソッド：
     * - TimeCorrectionService.getTimeCorrectionById()
     */
    @Test
    void testGetTimeCorrectionById_NotFound() throws Exception {
        // Given
        Long correctionId = 999L;
        
        when(timeCorrectionService.getTimeCorrectionById(eq(correctionId)))
            .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/time-corrections/{id}", correctionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("申請が見つかりません"));

        // Verify that the service method was called
        verify(timeCorrectionService, times(1)).getTimeCorrectionById(eq(correctionId));
    }

    /**
     * テストケース：ユーザーの承認待ち申請数取得の成功
     * 
     * テスト対象メソッド：
     * - TimeCorrectionController.getUserPendingCount()
     * 
     * テストシナリオ：
     * - ユーザーが自分の承認待ち申請数を取得
     * - サービスが正常に申請数を返す
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスに申請数データが含まれる
     * 
     * モック対象メソッド：
     * - TimeCorrectionService.getUserPendingCount()
     */
    @Test
    void testGetUserPendingCount_Success() throws Exception {
        // Given
        Long userId = 1L;
        long pendingCount = 3L;
        
        when(timeCorrectionService.getUserPendingCount(eq(userId)))
            .thenReturn(pendingCount);

        // When & Then
        mockMvc.perform(get("/api/v1/time-corrections/user/pending-count")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ユーザーの承認待ち申請数を取得しました"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.pendingCount").value(pendingCount));

        // Verify that the service method was called
        verify(timeCorrectionService, times(1)).getUserPendingCount(eq(userId));
    }

    /**
     * テストケース：全体の承認待ち申請数取得の成功
     * 
     * テスト対象メソッド：
     * - TimeCorrectionController.getAllPendingCount()
     * 
     * テストシナリオ：
     * - 管理者が全体の承認待ち申請数を取得
     * - サービスが正常に申請数を返す
     * 
     * 期待結果：
     * - HTTPステータスコード：200 OK
     * - レスポンスにsuccess=trueが含まれる
     * - レスポンスに申請数データが含まれる
     * 
     * モック対象メソッド：
     * - TimeCorrectionService.getAllPendingCount()
     */
    @Test
    void testGetAllPendingCount_Success() throws Exception {
        // Given
        long pendingCount = 5L;
        
        when(timeCorrectionService.getAllPendingCount())
            .thenReturn(pendingCount);

        // When & Then
        mockMvc.perform(get("/api/v1/time-corrections/pending-count")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("全体の承認待ち申請数を取得しました"))
                .andExpect(jsonPath("$.data.pendingCount").value(pendingCount));

        // Verify that the service method was called
        verify(timeCorrectionService, times(1)).getAllPendingCount();
    }
}