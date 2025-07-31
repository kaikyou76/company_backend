package com.example.companybackend.repository;

import com.example.companybackend.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 休暇申請リポジトリ
 * comsys_dump.sql leave_requestsテーブル完全対応
 * 
 * テーブル構造:
 * - id (SERIAL PRIMARY KEY)
 * - user_id (INTEGER NOT NULL REFERENCES users(id))
 * - type (leave_type_enum NOT NULL)
 * - start_date (DATE NOT NULL)
 * - end_date (DATE NOT NULL)
 * - reason (TEXT)
 * - status (leave_status_enum DEFAULT 'pending')
 * - approver_id (INTEGER REFERENCES users(id))
 * - approved_at (TIMESTAMP WITH TIME ZONE)
 * - created_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 * - updated_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 */
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    /**
     * ユーザーID別休暇申請取得
     * @param userId ユーザーID
     * @return 該当ユーザーの休暇申請リスト
     */
    List<LeaveRequest> findByUserId(Long userId);

    /**
     * ステータス別休暇申請取得
     * @param status 申請ステータス
     * @return 該当ステータスの休暇申請リスト
     */
    List<LeaveRequest> findByStatus(String status);

    /**
     * 休暇タイプ別申請取得
     * @param type 休暇タイプ
     * @return 該当タイプの休暇申請リスト
     */
    List<LeaveRequest> findByType(String type);

    /**
     * 承認者ID別申請取得
     * @param approverId 承認者ID
     * @return 該当承認者の申請リスト
     */
    List<LeaveRequest> findByApproverId(Long approverId);

    /**
     * ユーザー・ステータス別申請取得
     * @param userId ユーザーID
     * @param status ステータス
     * @return 該当ユーザーの指定ステータス申請リスト
     */
    List<LeaveRequest> findByUserIdAndStatus(Long userId, String status);

    /**
     * 期間別休暇申請取得
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 指定期間内の休暇申請リスト
     */
    @Query(nativeQuery = true, value = "SELECT lr.* FROM leave_requests lr WHERE lr.start_date <= :endDate AND lr.end_date >= :startDate")
    List<LeaveRequest> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 承認待ち申請取得
     * @return 承認待ちの申請リスト
     */
    @Query(nativeQuery = true, value = "SELECT lr.* FROM leave_requests lr WHERE lr.status = 'pending' ORDER BY lr.created_at ASC")
    List<LeaveRequest> findPendingRequests();

    /**
     * 承認済み申請取得（期間指定）
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 承認済みの申請リスト
     */
    @Query(nativeQuery = true, value = "SELECT lr.* FROM leave_requests lr WHERE lr.status = 'approved' AND lr.start_date <= :endDate AND lr.end_date >= :startDate")
    List<LeaveRequest> findApprovedRequestsInPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * ユーザーの年間休暇申請取得
     * @param userId ユーザーID
     * @param year 年
     * @return 指定年のユーザー休暇申請リスト
     */
    @Query(nativeQuery = true, value = "SELECT lr.* FROM leave_requests lr WHERE lr.user_id = :userId AND EXTRACT(YEAR FROM lr.start_date) = :year")
    List<LeaveRequest> findByUserIdAndYear(@Param("userId") Long userId, @Param("year") int year);

    /**
     * 休暇タイプ・期間別申請数取得
     * @param type 休暇タイプ
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 該当期間の指定タイプ申請数
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(lr) FROM leave_requests lr WHERE lr.type = :type AND lr.start_date <= :endDate AND lr.end_date >= :startDate")
    long countByTypeAndDateRange(@Param("type") String type, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * ユーザーの承認済み休暇日数取得
     * @param userId ユーザーID
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 承認済み休暇日数
     */
    @Query(nativeQuery = true, value = """
        SELECT COALESCE(SUM(
            CASE 
                WHEN lr.end_date < :startDate OR lr.start_date > :endDate THEN 0
                ELSE (LEAST(lr.end_date, :endDate) - GREATEST(lr.start_date, :startDate) + 1)
            END
        ), 0)
        FROM leave_requests lr 
        WHERE lr.user_id = :userId 
        AND lr.status = 'approved'
        AND lr.start_date <= :endDate 
        AND lr.end_date >= :startDate
        """)
    long countApprovedDaysInPeriod(@Param("userId") Integer userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 重複期間の申請チェック
     * @param userId ユーザーID
     * @param startDate 開始日
     * @param endDate 終了日
     * @param excludeId 除外する申請ID（更新時）
     * @return 重複する申請リスト
     */
    @Query(nativeQuery = true, value = """
        SELECT lr.* FROM leave_requests lr 
        WHERE lr.user_id = :userId 
        AND lr.start_date <= :endDate 
        AND lr.end_date >= :startDate
        AND lr.status IN ('pending', 'approved')
        AND (:excludeId IS NULL OR lr.id != :excludeId)
        """)
    List<LeaveRequest> findOverlappingRequests(
        @Param("userId") Integer userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludeId") Integer excludeId
    );

    /**
     * 月別休暇申請統計
     * @param year 年
     * @param month 月
     * @return 月別統計情報
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            COUNT(*) as totalRequests,
            SUM(CASE WHEN lr.status = 'pending' THEN 1 ELSE 0 END) as pendingCount,
            SUM(CASE WHEN lr.status = 'approved' THEN 1 ELSE 0 END) as approvedCount,
            SUM(CASE WHEN lr.status = 'rejected' THEN 1 ELSE 0 END) as rejectedCount,
            lr.type as leaveType,
            COUNT(*) as typeCount
        FROM leave_requests lr 
        WHERE EXTRACT(YEAR FROM lr.start_date) = :year 
        AND EXTRACT(MONTH FROM lr.start_date) = :month
        GROUP BY lr.type
        """)
    List<java.util.Map<String, Object>> getMonthlyStatistics(@Param("year") int year, @Param("month") int month);

    /**
     * 承認者別処理待ち申請数
     * @return 承認者別処理待ち申請数
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            lr.approver_id as approverId,
            COUNT(lr) as pendingCount
        FROM leave_requests lr 
        WHERE lr.status = 'pending'
        GROUP BY lr.approver_id
        """)
    List<java.util.Map<String, Object>> getPendingCountByApprover();

    /**
     * 期限切れ申請取得
     * @param days 何日前から期限切れとするか
     * @return 期限切れ申請リスト
     */
    @Query(nativeQuery = true, value = "SELECT lr.* FROM leave_requests lr WHERE lr.status = 'pending' AND lr.created_at < :cutoffDate")
    List<LeaveRequest> findOverdueRequests(@Param("cutoffDate") OffsetDateTime cutoffDate);

    /**
     * 最近の申請取得
     * @param days 過去何日以内
     * @return 最近の申請リスト
     */
    @Query(nativeQuery = true, value = "SELECT lr.* FROM leave_requests lr WHERE lr.created_at >= :cutoffDate ORDER BY lr.created_at DESC")
    List<LeaveRequest> findRecentRequests(@Param("cutoffDate") OffsetDateTime cutoffDate);

    /**
     * バッチ処理用：大量データ申請取得
     * @param offset オフセット
     * @param limit 取得件数
     * @return 申請リスト
     */
    @Query(nativeQuery = true, value = "SELECT lr.* FROM leave_requests lr ORDER BY lr.id ASC LIMIT :limit OFFSET :offset")
    List<LeaveRequest> findForBatchProcessing(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * データ整合性チェック：孤立ユーザー参照検索
     * @return 存在しないユーザーを参照している申請リスト
     */
    @Query(nativeQuery = true, value = """
        SELECT lr.* FROM leave_requests lr 
        WHERE NOT EXISTS (
            SELECT 1 FROM users u WHERE u.id = lr.user_id
        )
        """)
    List<LeaveRequest> findRequestsWithOrphanedUserId();

    /**
     * データ整合性チェック：孤立承認者参照検索
     * @return 存在しない承認者を参照している申請リスト
     */
    @Query(nativeQuery = true, value = """
        SELECT lr.* FROM leave_requests lr 
        WHERE lr.approver_id IS NOT NULL 
        AND NOT EXISTS (
            SELECT 1 FROM users u WHERE u.id = lr.approver_id
        )
        """)
    List<LeaveRequest> findRequestsWithOrphanedApproverId();
}