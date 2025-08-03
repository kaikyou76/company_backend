package com.example.companybackend.service;

import com.example.companybackend.entity.LeaveRequest;
import com.example.companybackend.entity.User;
import com.example.companybackend.repository.LeaveRequestRepository;
import com.example.companybackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 休暇申請サービステストクラス
 * 
 * テスト対象：
 * - テストファイル：LeaveService.java
 * - テストクラス：com.example.companybackend.service.LeaveService
 * - モック対象：
 *   - LeaveRequestRepository（休暇申請リポジトリ）
 *   - UserRepository（ユーザーリポジトリ）
 *   - NotificationService（通知サービス）
 * 
 * テスト規約とテクニック：
 * 1. @ExtendWith(MockitoExtension.class)を使用してモック機能を有効化
 * 2. @InjectMocksでテスト対象のサービスを注入
 * 3. @Mockで依存サービスをモック化し、テストを独立させる
 * 4. Given-When-Thenテストパターンに従う
 * 5. 各メソッドに対して成功および例外シナリオのテストケースを作成
 * 6. 重要な戻り値と例外を検証
 */
@ExtendWith(MockitoExtension.class)
public class LeaveServiceTest {

    /**
     * LeaveServiceのテスト対象オブジェクト
     * 依存するサービスはモック化され、このサービスのみをテスト
     */
    @InjectMocks
    private LeaveService leaveService;

    /**
     * LeaveRequestRepositoryのモックオブジェクト
     * 休暇申請のデータベースアクセスをモック化
     */
    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    /**
     * UserRepositoryのモックオブジェクト
     * ユーザー情報のデータベースアクセスをモック化
     */
    @Mock
    private UserRepository userRepository;

    /**
     * NotificationServiceのモックオブジェクト
     * 通知処理をモック化
     */
    @Mock
    private NotificationService notificationService;

    /**
     * テストデータ
     */
    private User testUser;
    private LeaveRequest testLeaveRequest;

    @BeforeEach
    void setUp() {
        // テストユーザーの作成
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setCreatedAt(OffsetDateTime.now());
        testUser.setUpdatedAt(OffsetDateTime.now());

        // テスト休暇申請の作成
        testLeaveRequest = new LeaveRequest();
        testLeaveRequest.setId(1L);
        testLeaveRequest.setUserId(1);
        testLeaveRequest.setType("paid");
        testLeaveRequest.setStatus("pending");
        testLeaveRequest.setStartDate(LocalDate.now().plusDays(1));
        testLeaveRequest.setEndDate(LocalDate.now().plusDays(3));
        testLeaveRequest.setReason("旅行のため");
        testLeaveRequest.setCreatedAt(OffsetDateTime.now());
        testLeaveRequest.setUpdatedAt(OffsetDateTime.now());
    }

    /**
     * テストケース：休暇申請作成の成功
     * 
     * テスト対象メソッド：
     * - LeaveService.createLeaveRequest()
     * 
     * テストシナリオ：
     * - 有効なユーザーIDと休暇申請情報で申請を作成
     * - ユーザーが存在する
     * - 重複する申請がない
     * - リポジトリが申請を保存する
     * 
     * 期待結果：
     * - 休暇申請が正常に作成される
     * - 通知が送信される
     */
    @Test
    void testCreateLeaveRequest_Success() {
        // Given
        Long userId = 1L;
        String type = "paid";
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);
        String reason = "旅行のため";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(leaveRequestRepository.findOverlappingRequests(anyInt(), any(), any(), isNull()))
                .thenReturn(Collections.emptyList());
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest request = invocation.getArgument(0);
            request.setId(1L);
            return request;
        });

        // When
        LeaveRequest result = leaveService.createLeaveRequest(userId, type, startDate, endDate, reason);

        // Then
        assertNotNull(result);
        assertEquals(userId.intValue(), result.getUserId());
        assertEquals(type, result.getType());
        assertEquals("pending", result.getStatus());
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(reason, result.getReason());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        verify(userRepository, times(1)).findById(userId);
        verify(leaveRequestRepository, times(1))
                .findOverlappingRequests(userId.intValue(), startDate, endDate, null);
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
        verify(notificationService, times(1)).sendLeaveRequestNotification(any(LeaveRequest.class), any(User.class));
    }

    /**
     * テストケース：休暇申請作成時のユーザー不存在
     * 
     * テスト対象メソッド：
     * - LeaveService.createLeaveRequest()
     * 
     * テストシナリオ：
     * - 存在しないユーザーIDで休暇申請を作成
     * - ユーザーリポジトリが空のOptionalを返す
     * 
     * 期待結果：
     * - IllegalArgumentExceptionがスローされる
     */
    @Test
    void testCreateLeaveRequest_UserNotFound() {
        // Given
        Long userId = 999L;
        String type = "paid";
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);
        String reason = "旅行のため";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            leaveService.createLeaveRequest(userId, type, startDate, endDate, reason);
        });

        assertEquals("ユーザーが見つかりません: " + userId, exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(leaveRequestRepository, never()).findOverlappingRequests(anyInt(), any(), any(), any());
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
        verify(notificationService, never()).sendLeaveRequestNotification(any(LeaveRequest.class), any(User.class));
    }

    /**
     * テストケース：休暇申請作成時の重複期間エラー
     * 
     * テスト対象メソッド：
     * - LeaveService.createLeaveRequest()
     * 
     * テストシナリオ：
     * - 既存の申請と重複する期間で休暇申請を作成
     * - 重複チェックで既存の申請が見つかる
     * 
     * 期待結果：
     * - IllegalStateExceptionがスローされる
     */
    @Test
    void testCreateLeaveRequest_OverlappingPeriod() {
        // Given
        Long userId = 1L;
        String type = "paid";
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);
        String reason = "旅行のため";

        List<LeaveRequest> overlappingRequests = new ArrayList<>();
        overlappingRequests.add(new LeaveRequest());

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(leaveRequestRepository.findOverlappingRequests(anyInt(), any(), any(), isNull()))
                .thenReturn(overlappingRequests);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            leaveService.createLeaveRequest(userId, type, startDate, endDate, reason);
        });

        assertEquals("指定期間に既存の申請があります", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(leaveRequestRepository, times(1))
                .findOverlappingRequests(userId.intValue(), startDate, endDate, null);
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
        verify(notificationService, never()).sendLeaveRequestNotification(any(LeaveRequest.class), any(User.class));
    }

    /**
     * テストケース：休暇申請作成時の無効な休暇タイプ
     * 
     * テスト対象メソッド：
     * - LeaveService.createLeaveRequest()
     * 
     * テストシナリオ：
     * - 無効な休暇タイプで休暇申請を作成
     * 
     * 期待結果：
     * - IllegalArgumentExceptionがスローされる
     */
    @Test
    void testCreateLeaveRequest_InvalidLeaveType() {
        // Given
        Long userId = 1L;
        String type = "invalid";
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);
        String reason = "旅行のため";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            leaveService.createLeaveRequest(userId, type, startDate, endDate, reason);
        });

        assertTrue(exception.getMessage().contains("無効な休暇タイプです"));
        verify(userRepository, times(1)).findById(userId);
        verify(leaveRequestRepository, never()).findOverlappingRequests(anyInt(), any(), any(), any());
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
        verify(notificationService, never()).sendLeaveRequestNotification(any(LeaveRequest.class), any(User.class));
    }

    /**
     * テストケース：休暇申請更新の成功
     * 
     * テスト対象メソッド：
     * - LeaveService.updateLeaveRequest()
     * 
     * テストシナリオ：
     * - 既存の承認待ち申請を更新
     * - 重複する他の申請がない
     * - リポジトリが申請を保存する
     * 
     * 期待結果：
     * - 休暇申請が正常に更新される
     */
    @Test
    void testUpdateLeaveRequest_Success() {
        // Given
        Long requestId = 1L;
        String type = "sick";
        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(4);
        String reason = "病気のため";

        LeaveRequest existingRequest = new LeaveRequest();
        existingRequest.setId(requestId);
        existingRequest.setUserId(1);
        existingRequest.setType("paid");
        existingRequest.setStatus("pending");
        existingRequest.setStartDate(LocalDate.now().plusDays(1));
        existingRequest.setEndDate(LocalDate.now().plusDays(3));
        existingRequest.setReason("旅行のため");

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
        when(leaveRequestRepository.findOverlappingRequests(anyInt(), any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LeaveRequest result = leaveService.updateLeaveRequest(requestId, type, startDate, endDate, reason);

        // Then
        assertNotNull(result);
        assertEquals(type, result.getType());
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(reason, result.getReason());

        verify(leaveRequestRepository, times(1)).findById(requestId);
        verify(leaveRequestRepository, times(1))
                .findOverlappingRequests(existingRequest.getUserId(), startDate, endDate, requestId.intValue());
        verify(leaveRequestRepository, times(1)).save(existingRequest);
    }

    /**
     * テストケース：休暇申請更新時の申請不存在
     * 
     * テスト対象メソッド：
     * - LeaveService.updateLeaveRequest()
     * 
     * テストシナリオ：
     * - 存在しない申請IDで休暇申請を更新
     * - 申請リポジトリが空のOptionalを返す
     * 
     * 期待結果：
     * - IllegalArgumentExceptionがスローされる
     */
    @Test
    void testUpdateLeaveRequest_RequestNotFound() {
        // Given
        Long requestId = 999L;
        String type = "sick";
        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(4);
        String reason = "病気のため";

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            leaveService.updateLeaveRequest(requestId, type, startDate, endDate, reason);
        });

        assertEquals("休暇申請が見つかりません: " + requestId, exception.getMessage());
        verify(leaveRequestRepository, times(1)).findById(requestId);
        verify(leaveRequestRepository, never()).findOverlappingRequests(anyInt(), any(), any(), anyInt());
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    /**
     * テストケース：休暇申請更新時の承認済み申請
     * 
     * テスト対象メソッド：
     * - LeaveService.updateLeaveRequest()
     * 
     * テストシナリオ：
     * - 承認済みの申請を更新しようとする
     * 
     * 期待結果：
     * - IllegalStateExceptionがスローされる
     */
    @Test
    void testUpdateLeaveRequest_AlreadyApproved() {
        // Given
        Long requestId = 1L;
        String type = "sick";
        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(4);
        String reason = "病気のため";

        LeaveRequest existingRequest = new LeaveRequest();
        existingRequest.setId(requestId);
        existingRequest.setStatus("approved");

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            leaveService.updateLeaveRequest(requestId, type, startDate, endDate, reason);
        });

        assertEquals("承認済みの申請は更新できません", exception.getMessage());
        verify(leaveRequestRepository, times(1)).findById(requestId);
        verify(leaveRequestRepository, never()).findOverlappingRequests(anyInt(), any(), any(), anyInt());
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    /**
     * テストケース：休暇申請削除の成功
     * 
     * テスト対象メソッド：
     * - LeaveService.deleteLeaveRequest()
     * 
     * テストシナリオ：
     * - 承認待ちの申請を削除
     * - 申請が存在する
     * - リポジトリが申請を削除する
     * 
     * 期待結果：
     * - 休暇申請が正常に削除される
     */
    @Test
    void testDeleteLeaveRequest_Success() {
        // Given
        Long requestId = 1L;

        LeaveRequest existingRequest = new LeaveRequest();
        existingRequest.setId(requestId);
        existingRequest.setStatus("pending");

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));

        // When
        leaveService.deleteLeaveRequest(requestId);

        // Then
        verify(leaveRequestRepository, times(1)).findById(requestId);
        verify(leaveRequestRepository, times(1)).delete(existingRequest);
    }

    /**
     * テストケース：休暇申請削除時の申請不存在
     * 
     * テスト対象メソッド：
     * - LeaveService.deleteLeaveRequest()
     * 
     * テストシナリオ：
     * - 存在しない申請IDで休暇申請を削除
     * - 申請リポジトリが空のOptionalを返す
     * 
     * 期待結果：
     * - IllegalArgumentExceptionがスローされる
     */
    @Test
    void testDeleteLeaveRequest_RequestNotFound() {
        // Given
        Long requestId = 999L;

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            leaveService.deleteLeaveRequest(requestId);
        });

        assertEquals("休暇申請が見つかりません: " + requestId, exception.getMessage());
        verify(leaveRequestRepository, times(1)).findById(requestId);
        verify(leaveRequestRepository, never()).delete(any(LeaveRequest.class));
    }

    /**
     * テストケース：休暇申請削除時の承認済み申請
     * 
     * テスト対象メソッド：
     * - LeaveService.deleteLeaveRequest()
     * 
     * テストシナリオ：
     * - 承認済みの申請を削除しようとする
     * 
     * 期待結果：
     * - IllegalStateExceptionがスローされる
     */
    @Test
    void testDeleteLeaveRequest_AlreadyApproved() {
        // Given
        Long requestId = 1L;

        LeaveRequest existingRequest = new LeaveRequest();
        existingRequest.setId(requestId);
        existingRequest.setStatus("approved");

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            leaveService.deleteLeaveRequest(requestId);
        });

        assertEquals("承認済みの申請は削除できません", exception.getMessage());
        verify(leaveRequestRepository, times(1)).findById(requestId);
        verify(leaveRequestRepository, never()).delete(any(LeaveRequest.class));
    }

    /**
     * テストケース：休暇申請承認の成功
     * 
     * テスト対象メソッド：
     * - LeaveService.approveLeaveRequest()
     * 
     * テストシナリオ：
     * - 承認待ちの申請を承認
     * - 承認者ユーザーが存在する
     * - リポジトリが申請を保存する
     * 
     * 期待結果：
     * - 休暇申請が正常に承認される
     */
    @Test
    void testApproveLeaveRequest_Success() {
        // Given
        Long requestId = 1L;
        Integer approverId = 2;

        User approverUser = new User();
        approverUser.setId(2L);

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(requestId);
        leaveRequest.setStatus("pending");

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveRequest));
        when(userRepository.findById(approverId.longValue())).thenReturn(Optional.of(approverUser));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LeaveRequest result = leaveService.approveLeaveRequest(requestId, approverId);

        // Then
        assertNotNull(result);
        assertEquals("approved", result.getStatus());
        assertEquals(approverId, result.getApproverId());
        assertNotNull(result.getApprovedAt());
        assertNotNull(result.getUpdatedAt());

        verify(leaveRequestRepository, times(1)).findById(requestId);
        verify(userRepository, times(1)).findById(approverId.longValue());
        verify(leaveRequestRepository, times(1)).save(leaveRequest);
    }

    /**
     * テストケース：休暇申請承認時の申請不存在
     * 
     * テスト対象メソッド：
     * - LeaveService.approveLeaveRequest()
     * 
     * テストシナリオ：
     * - 存在しない申請IDで休暇申請を承認
     * - 申請リポジトリが空のOptionalを返す
     * 
     * 期待結果：
     * - IllegalArgumentExceptionがスローされる
     */
    @Test
    void testApproveLeaveRequest_RequestNotFound() {
        // Given
        Long requestId = 999L;
        Integer approverId = 2;

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            leaveService.approveLeaveRequest(requestId, approverId);
        });

        assertEquals("休暇申請が見つかりません: " + requestId, exception.getMessage());
        verify(leaveRequestRepository, times(1)).findById(requestId);
        verify(userRepository, never()).findById(anyLong());
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    /**
     * テストケース：休暇申請承認時の承認者不存在
     * 
     * テスト対象メソッド：
     * - LeaveService.approveLeaveRequest()
     * 
     * テストシナリオ：
     * - 存在しない承認者IDで休暇申請を承認
     * - 承認者ユーザーリポジトリが空のOptionalを返す
     * 
     * 期待結果：
     * - IllegalArgumentExceptionがスローされる
     */
    @Test
    void testApproveLeaveRequest_ApproverNotFound() {
        // Given
        Long requestId = 1L;
        Long approverId = 999L;

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(requestId);
        leaveRequest.setStatus("pending");

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveRequest));
        when(userRepository.findById(approverId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            leaveService.approveLeaveRequest(requestId, approverId.intValue());
        });

        assertEquals("承認者が見つかりません: " + approverId, exception.getMessage());
        verify(leaveRequestRepository, times(1)).findById(requestId);
        verify(userRepository, times(1)).findById(approverId);
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    /**
     * テストケース：休暇申請承認時の非承認待ち状態
     * 
     * テスト対象メソッド：
     * - LeaveService.approveLeaveRequest()
     * 
     * テストシナリオ：
     * - 承認待ちではない申請を承認
     * 
     * 期待結果：
     * - IllegalStateExceptionがスローされる
     */
    @Test
    void testApproveLeaveRequest_NotPending() {
        // Given
        Long requestId = 1L;
        Integer approverId = 2;

        User approverUser = new User();
        approverUser.setId(2L);

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(requestId);
        leaveRequest.setStatus("approved");

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveRequest));
        when(userRepository.findById(approverId.longValue())).thenReturn(Optional.of(approverUser));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            leaveService.approveLeaveRequest(requestId, approverId);
        });

        assertTrue(exception.getMessage().contains("承認待ち状態ではありません"));
        verify(leaveRequestRepository, times(1)).findById(requestId);
        verify(userRepository, times(1)).findById(approverId.longValue());
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    /**
     * テストケース：休暇申請却下の成功
     * 
     * テスト対象メソッド：
     * - LeaveService.rejectLeaveRequest()
     * 
     * テストシナリオ：
     * - 承認待ちの申請を却下
     * - 承認者ユーザーが存在する
     * - リポジトリが申請を保存する
     * 
     * 期待結果：
     * - 休暇申請が正常に却下される
     */
    @Test
    void testRejectLeaveRequest_Success() {
        // Given
        Long requestId = 1L;
        Long approverId = 2L;

        User approverUser = new User();
        approverUser.setId(approverId);

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(requestId);
        leaveRequest.setStatus("pending");

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveRequest));
        when(userRepository.findById(approverId)).thenReturn(Optional.of(approverUser));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LeaveRequest result = leaveService.rejectLeaveRequest(requestId, approverId);

        // Then
        assertNotNull(result);
        assertEquals("rejected", result.getStatus());
        assertEquals(approverId.intValue(), result.getApproverId());
        assertNotNull(result.getApprovedAt());
        assertNotNull(result.getUpdatedAt());

        verify(leaveRequestRepository, times(1)).findById(requestId);
        verify(userRepository, times(1)).findById(approverId);
        verify(leaveRequestRepository, times(1)).save(leaveRequest);
    }

    /**
     * テストケース：ユーザーの休暇申請一覧取得
     * 
     * テスト対象メソッド：
     * - LeaveService.getUserLeaveRequests()
     * 
     * テストシナリオ：
     * - ユーザーIDで休暇申請一覧を取得
     * - リポジトリが申請リストを返す
     * 
     * 期待結果：
     * - ユーザーの休暇申請リストが返される
     */
    @Test
    void testGetUserLeaveRequests() {
        // Given
        Long userId = 1L;
        List<LeaveRequest> expectedRequests = Arrays.asList(testLeaveRequest);

        when(leaveRequestRepository.findByUserId(userId)).thenReturn(expectedRequests);

        // When
        List<LeaveRequest> result = leaveService.getUserLeaveRequests(userId);

        // Then
        assertNotNull(result);
        assertEquals(expectedRequests.size(), result.size());
        assertEquals(expectedRequests, result);
        verify(leaveRequestRepository, times(1)).findByUserId(userId);
    }

    /**
     * テストケース：ステータス別休暇申請取得
     * 
     * テスト対象メソッド：
     * - LeaveService.getLeaveRequestsByStatus()
     * 
     * テストシナリオ：
     * - ステータスで休暇申請一覧を取得
     * - リポジトリが申請リストを返す
     * 
     * 期待結果：
     * - 指定ステータスの休暇申請リストが返される
     */
    @Test
    void testGetLeaveRequestsByStatus() {
        // Given
        String status = "pending";
        List<LeaveRequest> expectedRequests = Arrays.asList(testLeaveRequest);

        when(leaveRequestRepository.findByStatus(status)).thenReturn(expectedRequests);

        // When
        List<LeaveRequest> result = leaveService.getLeaveRequestsByStatus(status);

        // Then
        assertNotNull(result);
        assertEquals(expectedRequests.size(), result.size());
        assertEquals(expectedRequests, result);
        verify(leaveRequestRepository, times(1)).findByStatus(status);
    }

    /**
     * テストケース：承認待ち申請取得
     * 
     * テスト対象メソッド：
     * - LeaveService.getPendingRequests()
     * 
     * テストシナリオ：
     * - 承認待ちの休暇申請一覧を取得
     * - リポジトリが申請リストを返す
     * 
     * 期待結果：
     * - 承認待ちの休暇申請リストが返される
     */
    @Test
    void testGetPendingRequests() {
        // Given
        List<LeaveRequest> expectedRequests = Arrays.asList(testLeaveRequest);

        when(leaveRequestRepository.findPendingRequests()).thenReturn(expectedRequests);

        // When
        List<LeaveRequest> result = leaveService.getPendingRequests();

        // Then
        assertNotNull(result);
        assertEquals(expectedRequests.size(), result.size());
        assertEquals(expectedRequests, result);
        verify(leaveRequestRepository, times(1)).findPendingRequests();
    }

    /**
     * テストケース：申請詳細取得（存在する場合）
     * 
     * テスト対象メソッド：
     * - LeaveService.getLeaveRequestById()
     * 
     * テストシナリオ：
     * - 存在する申請IDで申請詳細を取得
     * - リポジトリが申請を返す
     * 
     * 期待結果：
     * - 指定した申請が返される
     */
    @Test
    void testGetLeaveRequestById_Exists() {
        // Given
        Long requestId = 1L;
        Optional<LeaveRequest> expectedRequest = Optional.of(testLeaveRequest);

        when(leaveRequestRepository.findById(requestId)).thenReturn(expectedRequest);

        // When
        Optional<LeaveRequest> result = leaveService.getLeaveRequestById(requestId);

        // Then
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(expectedRequest.get(), result.get());
        verify(leaveRequestRepository, times(1)).findById(requestId);
    }

    /**
     * テストケース：申請詳細取得（存在しない場合）
     * 
     * テスト対象メソッド：
     * - LeaveService.getLeaveRequestById()
     * 
     * テストシナリオ：
     * - 存在しない申請IDで申請詳細を取得
     * - リポジトリが空のOptionalを返す
     * 
     * 期待結果：
     * - 空のOptionalが返される
     */
    @Test
    void testGetLeaveRequestById_NotExists() {
        // Given
        Long requestId = 999L;
        Optional<LeaveRequest> expectedRequest = Optional.empty();

        when(leaveRequestRepository.findById(requestId)).thenReturn(expectedRequest);

        // When
        Optional<LeaveRequest> result = leaveService.getLeaveRequestById(requestId);

        // Then
        assertNotNull(result);
        assertFalse(result.isPresent());
        verify(leaveRequestRepository, times(1)).findById(requestId);
    }
}