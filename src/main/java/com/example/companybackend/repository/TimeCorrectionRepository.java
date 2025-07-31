package com.example.companybackend.repository;

import com.example.companybackend.entity.TimeCorrection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 打刻修正申請リポジトリ
 * time_corrections テーブルに対応
 * comsys_dump.sql準拠
 */
@Repository
public interface TimeCorrectionRepository extends JpaRepository<TimeCorrection, Long> {

    /**
     * ユーザーIDで打刻修正申請を検索
     */
    List<TimeCorrection> findByUserId(Integer userId);

    /**
     * 勤怠記録IDで打刻修正申請を検索
     */
    List<TimeCorrection> findByAttendanceId(Long attendanceId);

    /**
     * ステータスで打刻修正申請を検索
     */
    List<TimeCorrection> findByStatus(String status);

    /**
     * 申請種別で打刻修正申請を検索
     */
    List<TimeCorrection> findByRequestType(String requestType);

    /**
     * ユーザーIDとステータスで打刻修正申請を検索
     */
    List<TimeCorrection> findByUserIdAndStatus(Integer userId, String status);

    /**
     * ユーザーID、勤怠記録ID、ステータスで打刻修正申請を検索
     */
    List<TimeCorrection> findByUserIdAndAttendanceIdAndStatus(Integer userId, Long attendanceId, String status);

    /**
     * 承認者IDで打刻修正申請を検索
     */
    List<TimeCorrection> findByApproverId(Integer approverId);

    /**
     * 作成日時範囲で打刻修正申請を検索
     */
    List<TimeCorrection> findByCreatedAtBetween(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * ユーザーIDと作成日時範囲で打刻修正申請を検索
     */
    List<TimeCorrection> findByUserIdAndCreatedAtBetween(
            Integer userId, 
            OffsetDateTime startDate, 
            OffsetDateTime endDate
    );

    /**
     * 承認日時範囲で打刻修正申請を検索
     */
    List<TimeCorrection> findByApprovedAtBetween(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * ユーザーIDで最新の打刻修正申請を取得
     */
    @Query(nativeQuery = true, value = "SELECT tc.* FROM time_corrections tc WHERE tc.user_id = :userId ORDER BY tc.created_at DESC")
    List<TimeCorrection> findByUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);

    /**
     * 承認待ちの打刻修正申請を作成日時順で取得
     */
    @Query(nativeQuery = true, value = "SELECT tc.* FROM time_corrections tc WHERE tc.status = 'pending' ORDER BY tc.created_at ASC")
    List<TimeCorrection> findPendingRequestsOrderByCreatedAt();

    /**
     * 特定期間の承認済み打刻修正申請を取得
     */
    @Query(nativeQuery = true, value = "SELECT tc.* FROM time_corrections tc WHERE tc.status = 'APPROVED' " +
           "AND tc.approved_at BETWEEN :startDate AND :endDate " +
           "ORDER BY tc.approved_at DESC")
    List<TimeCorrection> findApprovedRequestsBetween(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * ユーザーの承認待ち申請数を取得
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(tc) FROM time_corrections tc WHERE tc.user_id = :userId AND tc.status = 'pending'")
    long countPendingRequestsByUserId(@Param("userId") Integer userId);

    /**
     * 承認者の処理待ち申請数を取得
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(tc) FROM time_corrections tc WHERE tc.status = 'pending'")
    long countAllPendingRequests();

    /**
     * 特定期間のユーザー別申請統計を取得
     */
    @Query(nativeQuery = true, value = "SELECT tc.user_id, tc.status, COUNT(tc) FROM time_corrections tc " +
           "WHERE tc.created_at BETWEEN :startDate AND :endDate " +
           "GROUP BY tc.user_id, tc.status " +
           "ORDER BY tc.user_id")
    List<Object[]> getRequestStatisticsByPeriod(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * 申請種別別の統計を取得
     */
    @Query(nativeQuery = true, value = "SELECT tc.request_type, tc.status, COUNT(tc) FROM time_corrections tc " +
           "WHERE tc.created_at BETWEEN :startDate AND :endDate " +
           "GROUP BY tc.request_type, tc.status " +
           "ORDER BY tc.request_type")
    List<Object[]> getRequestTypeStatisticsByPeriod(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * 承認者別の処理統計を取得
     */
    @Query(nativeQuery = true, value = "SELECT tc.approver_id, tc.status, COUNT(tc) FROM time_corrections tc " +
           "WHERE tc.approved_at BETWEEN :startDate AND :endDate " +
           "AND tc.approver_id IS NOT NULL " +
           "GROUP BY tc.approver_id, tc.status " +
           "ORDER BY tc.approver_id")
    List<Object[]> getApproverStatisticsByPeriod(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * 特定の勤怠記録に対する修正申請の存在確認
     */
    boolean existsByAttendanceId(Long attendanceId);

    /**
     * ユーザーの特定期間内の申請回数を取得
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(tc) FROM time_corrections tc " +
           "WHERE tc.user_id = :userId " +
           "AND tc.created_at BETWEEN :startDate AND :endDate")
    long countRequestsByUserIdAndPeriod(
            @Param("userId") Integer userId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * 古い承認済み・却下済み申請を削除用に取得
     */
    @Query(nativeQuery = true, value = "SELECT tc.* FROM time_corrections tc " +
           "WHERE tc.status IN ('APPROVED', 'REJECTED') " +
           "AND tc.approved_at < :cutoffDate " +
           "ORDER BY tc.approved_at ASC")
    List<TimeCorrection> findOldProcessedRequests(@Param("cutoffDate") OffsetDateTime cutoffDate);
    
    /**
     * ユーザーIDとステータスでカウント
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(tc) FROM time_corrections tc WHERE tc.user_id = :userId AND tc.status = :status")
    long countByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") String status);
    
    /**
     * ステータスでカウント
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(tc) FROM time_corrections tc WHERE tc.status = :status")
    long countByStatus(@Param("status") String status);
}