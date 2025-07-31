package com.example.companybackend.service;

import com.example.companybackend.entity.LeaveRequest;
import com.example.companybackend.entity.User;
import com.example.companybackend.repository.LeaveRequestRepository;
import com.example.companybackend.repository.UserRepository;
import com.example.companybackend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 休暇申請管理サービス
 * LEV-SVC-001: 休暇申請Service実装
 * 
 * comsys_dump.sql完全準拠:
 * - Enum使用禁止 - 全てString型で処理
 * - Database First原則
 * - 単純なエンティティ設計
 * 
 * 機能:
 * - 休暇申請作成・更新・削除
 * - 申請承認・却下処理
 * - 重複期間チェック
 * - 休暇残日数管理
 * - 申請統計情報
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 休暇申請作成
     * @param userId ユーザーID
     * @param type 休暇タイプ ("paid", "sick", "special")
     * @param startDate 開始日
     * @param endDate 終了日
     * @param reason 理由
     * @return 作成された休暇申請
     * @throws IllegalArgumentException 不正な申請データの場合
     * @throws IllegalStateException 重複期間がある場合
     */
    public LeaveRequest createLeaveRequest(Long userId, String type, LocalDate startDate, LocalDate endDate, String reason) {
        log.info("休暇申請作成開始: userId={}, type={}, startDate={}, endDate={}", userId, type, startDate, endDate);

        // ユーザー存在確認
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + userId));

        // 申請データ妥当性チェック
        validateLeaveRequest(type, startDate, endDate);

        // 重複期間チェック
        List<LeaveRequest> overlappingRequests = leaveRequestRepository
            .findOverlappingRequests(userId.intValue(), startDate, endDate, null);
        
        if (!overlappingRequests.isEmpty()) {
            throw new IllegalStateException("指定期間に既存の申請があります");
        }

        // 休暇申請作成
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setUserId(userId.intValue());
        leaveRequest.setType(type);
        leaveRequest.setStartDate(startDate);
        leaveRequest.setEndDate(endDate);
        leaveRequest.setReason(reason);
        leaveRequest.setStatus("pending");
        leaveRequest.setCreatedAt(OffsetDateTime.now());
        leaveRequest.setUpdatedAt(OffsetDateTime.now());

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        log.info("休暇申請作成完了: leaveRequestId={}, userId={}", savedRequest.getId(), userId);
        
        // 通知処理
        notificationService.sendLeaveRequestNotification(savedRequest, user);
        
        return savedRequest;
    }

    /**
     * 休暇申請更新
     * @param requestId 申請ID
     * @param type 休暇タイプ
     * @param startDate 開始日
     * @param endDate 終了日
     * @param reason 理由
     * @return 更新された休暇申請
     * @throws IllegalArgumentException 申請が見つからない場合
     * @throws IllegalStateException 承認済み申請を更新しようとした場合
     */
    public LeaveRequest updateLeaveRequest(Long requestId, String type, LocalDate startDate, LocalDate endDate, String reason) {
        log.info("休暇申請更新開始: requestId={}, type={}, startDate={}, endDate={}", requestId, type, startDate, endDate);

        // 申請存在確認
        LeaveRequest existingRequest = leaveRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("休暇申請が見つかりません: " + requestId));

        // 承認済み申請は更新不可
        if (existingRequest.isApproved()) {
            throw new IllegalStateException("承認済みの申請は更新できません");
        }

        // 申請データ妥当性チェック
        validateLeaveRequest(type, startDate, endDate);

        // 重複期間チェック（自身の申請を除外）
        List<LeaveRequest> overlappingRequests = leaveRequestRepository
            .findOverlappingRequests(existingRequest.getUserId(), startDate, endDate, requestId.intValue());
        
        if (!overlappingRequests.isEmpty()) {
            throw new IllegalStateException("指定期間に他の申請があります");
        }

        // 申請更新
        existingRequest.setType(type);
        existingRequest.setStartDate(startDate);
        existingRequest.setEndDate(endDate);
        existingRequest.setReason(reason);

        LeaveRequest updatedRequest = leaveRequestRepository.save(existingRequest);
        log.info("休暇申請更新完了: requestId={}, leaveDays={}", updatedRequest.getId(), updatedRequest.getLeaveDays());

        return updatedRequest;
    }

    /**
     * 休暇申請削除
     * @param requestId 申請ID
     * @throws IllegalArgumentException 申請が見つからない場合
     * @throws IllegalStateException 承認済み申請を削除しようとした場合
     */
    public void deleteLeaveRequest(Long requestId) {
        log.info("休暇申請削除開始: requestId={}", requestId);

        // 申請存在確認
        LeaveRequest existingRequest = leaveRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("休暇申請が見つかりません: " + requestId));

        // 承認済み申請は削除不可
        if (existingRequest.isApproved()) {
            throw new IllegalStateException("承認済みの申請は削除できません");
        }

        leaveRequestRepository.delete(existingRequest);
        log.info("休暇申請削除完了: requestId={}", requestId);
    }

    /**
     * 休暇申請承認
     * @param requestId 申請ID
     * @param approverId 承認者ID
     * @return 承認された休暇申請
     * @throws IllegalArgumentException 申請が見つからない場合
     * @throws IllegalStateException 既に処理済みの場合
     */
    public LeaveRequest approveLeaveRequest(Long requestId, Integer approverId) {
        log.info("休暇申請承認開始: requestId={}, approverId={}", requestId, approverId);

        // 申請存在確認
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("休暇申請が見つかりません: " + requestId));

        // 承認者存在確認
        userRepository.findById(approverId.longValue())
            .orElseThrow(() -> new IllegalArgumentException("承認者が見つかりません: " + approverId));

        // 承認待ち状態チェック
        if (!leaveRequest.isPending()) {
            throw new IllegalStateException("承認待ち状態ではありません: " + leaveRequest.getStatus());
        }

        // 承認処理
        leaveRequest.approve(approverId);

        LeaveRequest approvedRequest = leaveRequestRepository.save(leaveRequest);
        log.info("休暇申請承認完了: requestId={}, approverId={}", approvedRequest.getId(), approverId);

        return approvedRequest;
    }

    /**
     * 休暇申請却下
     * @param requestId 申請ID
     * @param approverId 承認者ID
     * @return 却下された休暇申請
     * @throws IllegalArgumentException 申請が見つからない場合
     * @throws IllegalStateException 既に処理済みの場合
     */
    public LeaveRequest rejectLeaveRequest(Long requestId, Long approverId) {
        log.info("休暇申請却下開始: requestId={}, approverId={}", requestId, approverId);

        // 申請存在確認
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("休暇申請が見つかりません: " + requestId));

        // 承認者存在確認
        userRepository.findById(approverId)
            .orElseThrow(() -> new IllegalArgumentException("承認者が見つかりません: " + approverId));

        // 承認待ち状態チェック
        if (!leaveRequest.isPending()) {
            throw new IllegalStateException("承認待ち状態ではありません: " + leaveRequest.getStatus());
        }

        // 却下処理
        leaveRequest.reject(approverId.intValue());

        LeaveRequest rejectedRequest = leaveRequestRepository.save(leaveRequest);
        log.info("休暇申請却下完了: requestId={}, approverId={}", rejectedRequest.getId(), approverId);

        return rejectedRequest;
    }

    /**
     * ユーザーの休暇申請一覧取得
     * @param userId ユーザーID
     * @return 休暇申請リスト
     */
    @Transactional(readOnly = true)
    public List<LeaveRequest> getUserLeaveRequests(Long userId) {
        return leaveRequestRepository.findByUserId(userId);
    }

    /**
     * ユーザーの休暇申請一覧取得（フィルタリング付き）
     * @param userId ユーザーID
     * @param startDate 開始日（オプション）
     * @param endDate 終了日（オプション）
     * @param leaveType 休暇タイプ（オプション）
     * @return フィルタリングされた休暇申請リスト
     */
    @Transactional(readOnly = true)
    public List<LeaveRequest> getUserLeaveRequests(Long userId, LocalDate startDate, LocalDate endDate, String leaveType) {
        return leaveRequestRepository.findByUserIdAndFilters(userId.intValue(), startDate, endDate, leaveType);
    }

    /**
     * ステータス別休暇申請取得
     * @param status ステータス ("pending", "approved", "rejected")
     * @return 休暇申請リスト
     */
    @Transactional(readOnly = true)
    public List<LeaveRequest> getLeaveRequestsByStatus(String status) {
        return leaveRequestRepository.findByStatus(status);
    }

    /**
     * 承認待ち申請取得
     * @return 承認待ち申請リスト
     */
    @Transactional(readOnly = true)
    public List<LeaveRequest> getPendingRequests() {
        return leaveRequestRepository.findPendingRequests();
    }

    /**
     * 期間内の承認済み申請取得
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 承認済み申請リスト
     */
    @Transactional(readOnly = true)
    public List<LeaveRequest> getApprovedRequestsInPeriod(LocalDate startDate, LocalDate endDate) {
        return leaveRequestRepository.findApprovedRequestsInPeriod(startDate, endDate);
    }

    /**
     * ユーザーの年間休暇申請取得
     * @param userId ユーザーID
     * @param year 年
     * @return 年間休暇申請リスト
     */
    @Transactional(readOnly = true)
    public List<LeaveRequest> getUserYearlyRequests(Long userId, int year) {
        return leaveRequestRepository.findByUserIdAndYear(userId, year);
    }

    /**
     * ユーザーの承認済み休暇日数取得
     * @param userId ユーザーID
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 承認済み休暇日数
     */
    @Transactional(readOnly = true)
    public long getApprovedLeaveDays(Long userId, LocalDate startDate, LocalDate endDate) {
        return leaveRequestRepository.countApprovedDaysInPeriod(userId.intValue(), startDate, endDate);
    }

    /**
     * 休暇タイプ別申請数取得
     * @param type 休暇タイプ
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 申請数
     */
    @Transactional(readOnly = true)
    public long getLeaveRequestCountByType(String type, LocalDate startDate, LocalDate endDate) {
        return leaveRequestRepository.countByTypeAndDateRange(type, startDate, endDate);
    }

    /**
     * 月別休暇申請統計取得
     * @param year 年
     * @param month 月
     * @return 月別統計情報
     */
    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getMonthlyStatistics(int year, int month) {
        return leaveRequestRepository.getMonthlyStatistics(year, month);
    }

    /**
     * 承認者別処理待ち申請数取得
     * @return 承認者別処理待ち申請数
     */
    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getPendingCountByApprover() {
        return leaveRequestRepository.getPendingCountByApprover();
    }

    /**
     * 期限切れ申請取得
     * @param days 何日前から期限切れとするか
     * @return 期限切れ申請リスト
     */
    @Transactional(readOnly = true)
    public List<LeaveRequest> getOverdueRequests(int days) {
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(days);
        return leaveRequestRepository.findOverdueRequests(cutoffDate);
    }

    /**
     * 最近の申請取得
     * @param days 過去何日以内
     * @return 最近の申請リスト
     */
    @Transactional(readOnly = true)
    public List<LeaveRequest> getRecentRequests(int days) {
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(days);
        return leaveRequestRepository.findRecentRequests(cutoffDate);
    }

    /**
     * 申請データ妥当性チェック
     * @param type 休暇タイプ
     * @param startDate 開始日
     * @param endDate 終了日
     * @throws IllegalArgumentException 不正なデータの場合
     */
    private void validateLeaveRequest(String type, LocalDate startDate, LocalDate endDate) {
        // 休暇タイプチェック
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("休暇タイプは必須です");
        }
        
        if (!type.matches("paid|sick|special")) {
            throw new IllegalArgumentException("無効な休暇タイプです: " + type);
        }

        // 日付チェック
        if (startDate == null) {
            throw new IllegalArgumentException("開始日は必須です");
        }
        
        if (endDate == null) {
            throw new IllegalArgumentException("終了日は必須です");
        }
        
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("終了日は開始日以降である必要があります");
        }

        // 過去日付チェック
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("過去の日付で申請はできません");
        }

        // 休暇期間チェック（最大30日まで）
        long leaveDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (leaveDays > 30) {
            throw new IllegalArgumentException("休暇期間は最大30日までです");
        }
    }

    /**
     * 申請詳細取得
     * @param requestId 申請ID
     * @return 休暇申請
     */
    @Transactional(readOnly = true)
    public Optional<LeaveRequest> getLeaveRequestById(Long requestId) {
        return leaveRequestRepository.findById(requestId);
    }

    /**
     * 有給休暇の残日数計算
     * @param userId ユーザーID
     * @return 残日数
     */
    @Transactional(readOnly = true)
    public long calculateRemainingPaidLeaveDays(Long userId) {
        // 仮の実装: 固定で20日を設定し、使用した日数を差し引く
        long totalPaidLeaveDays = 20; // 年間の有給休暇日数
        long usedPaidLeaveDays = getApprovedLeaveDays(userId, 
            LocalDate.now().withDayOfYear(1), // 年初め
            LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear())); // 年末
        return totalPaidLeaveDays - usedPaidLeaveDays;
    }

    /**
     * データ整合性チェック：孤立参照検索
     * @return 整合性エラーレポート
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getDataIntegrityReport() {
        List<LeaveRequest> orphanedUserRequests = leaveRequestRepository.findRequestsWithOrphanedUserId();
        List<LeaveRequest> orphanedApproverRequests = leaveRequestRepository.findRequestsWithOrphanedApproverId();

        return java.util.Map.of(
            "orphanedUserRequests", orphanedUserRequests.size(),
            "orphanedApproverRequests", orphanedApproverRequests.size(),
            "checkDate", OffsetDateTime.now()
        );
    }
}