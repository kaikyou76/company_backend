package com.example.companybackend.repository;

import com.example.companybackend.entity.AttendanceSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 勤怠サマリーリポジトリ
 * comsys_dump.sql attendance_summariesテーブル完全対応
 * 
 * テーブル構造:
 * - id (BIGINT PRIMARY KEY) 
 * - user_id (INTEGER NOT NULL)
 * - target_date (DATE NOT NULL)
 * - total_hours (NUMERIC(5,2) DEFAULT 0.00 NOT NULL)
 * - overtime_hours (NUMERIC(5,2) DEFAULT 0.00 NOT NULL)
 * - late_night_hours (NUMERIC(5,2) DEFAULT 0.00 NOT NULL)
 * - holiday_hours (NUMERIC(5,2) DEFAULT 0.00 NOT NULL)
 * - summary_type (VARCHAR(20) DEFAULT 'daily' CHECK ('daily', 'monthly'))
 * - created_at (TIMESTAMP WITH TIME ZONE DEFAULT NOW())
 */
@Repository
public interface AttendanceSummaryRepository extends JpaRepository<AttendanceSummary, Long> {

    /**
     * ユーザーID別サマリー取得
     * @param userId ユーザーID
     * @return 該当ユーザーのサマリーリスト
     */
    List<AttendanceSummary> findByUserId(Integer userId);

    /**
     * サマリータイプ別取得
     * @param summaryType サマリータイプ
     * @return 該当タイプのサマリーリスト
     */
    List<AttendanceSummary> findBySummaryType(String summaryType);

    /**
     * 対象日別サマリー取得
     * @param targetDate 対象日
     * @return 該当日のサマリーリスト
     */
    List<AttendanceSummary> findByTargetDate(LocalDate targetDate);

    /**
     * ユーザー・対象日別サマリー取得
     * @param userId ユーザーID
     * @param targetDate 対象日
     * @return 該当サマリー
     */
    Optional<AttendanceSummary> findByUserIdAndTargetDate(Integer userId, LocalDate targetDate);

    /**
     * ユーザー・サマリータイプ別取得
     * @param userId ユーザーID
     * @param summaryType サマリータイプ
     * @return 該当サマリーリスト
     */
    List<AttendanceSummary> findByUserIdAndSummaryType(Integer userId, String summaryType);

    /**
     * 期間別サマリー取得
     * @param userId ユーザーID
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 指定期間内のサマリーリスト
     */
    @Query(nativeQuery = true, value = "SELECT attsum.* FROM attendance_summaries attsum WHERE attsum.user_id = :userId AND attsum.target_date BETWEEN :startDate AND :endDate ORDER BY attsum.target_date ASC")
    List<AttendanceSummary> findByUserIdAndTargetDateBetween(@Param("userId") Integer userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 日次サマリー取得
     * @return 日次サマリーリスト
     */
    @Query(nativeQuery = true, value = "SELECT attsum.* FROM attendance_summaries attsum WHERE attsum.summary_type = 'daily'")
    List<AttendanceSummary> findDailySummaries();

    /**
     * 月次サマリー取得
     * @return 月次サマリーリスト
     */
    @Query(nativeQuery = true, value = "SELECT attsum.* FROM attendance_summaries attsum WHERE attsum.summary_type = 'monthly'")
    List<AttendanceSummary> findMonthlySummaries();

    /**
     * ユーザーの月間サマリー取得
     * @param userId ユーザーID
     * @param year 年
     * @param month 月
     * @return 月間サマリー
     */
    @Query(nativeQuery = true, value = "SELECT attsum.* FROM attendance_summaries attsum WHERE attsum.user_id = :userId AND attsum.summary_type = 'monthly' AND EXTRACT(YEAR FROM attsum.target_date) = :year AND EXTRACT(MONTH FROM attsum.target_date) = :month")
    Optional<AttendanceSummary> findMonthlyByUserIdAndYearAndMonth(@Param("userId") Integer userId, @Param("year") int year, @Param("month") int month);

    /**
     * ユーザーの年間サマリー合計取得
     * @param userId ユーザーID
     * @param year 年
     * @return 年間合計サマリー
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            SUM(attsum.total_hours) as totalHours,
            SUM(attsum.overtime_hours) as totalOvertimeHours,
            SUM(attsum.late_night_hours) as totalLateNightHours,
            SUM(attsum.holiday_hours) as totalHolidayHours,
            COUNT(attsum) as recordCount
        FROM attendance_summaries attsum 
        WHERE attsum.user_id = :userId 
        AND EXTRACT(YEAR FROM attsum.target_date) = :year
        """)
    java.util.Map<String, Object> getYearlySummaryByUserId(@Param("userId") Integer userId, @Param("year") int year);

    /**
     * 残業時間閾値以上のサマリー取得
     * @param minOvertimeHours 最小残業時間
     * @return 指定時間以上の残業サマリーリスト
     */
    @Query(nativeQuery = true, value = "SELECT attsum.* FROM attendance_summaries attsum WHERE attsum.overtime_hours >= :minOvertimeHours ORDER BY attsum.overtime_hours DESC, attsum.target_date DESC")
    List<AttendanceSummary> findByOvertimeHoursGreaterThanEqual(@Param("minOvertimeHours") BigDecimal minOvertimeHours);

    /**
     * 深夜勤務のあるサマリー取得
     * @return 深夜勤務サマリーリスト
     */
    @Query(nativeQuery = true, value = "SELECT attsum.* FROM attendance_summaries attsum WHERE attsum.late_night_hours > 0 ORDER BY attsum.late_night_hours DESC")
    List<AttendanceSummary> findWithLateNightWork();

    /**
     * 休日勤務のあるサマリー取得
     * @return 休日勤務サマリーリスト
     */
    @Query(nativeQuery = true, value = "SELECT attsum.* FROM attendance_summaries attsum WHERE attsum.holiday_hours > 0 ORDER BY attsum.holiday_hours DESC")
    List<AttendanceSummary> findWithHolidayWork();

    /**
     * 部署別月間統計取得
     * @param year 年
     * @param month 月
     * @return 部署別統計情報
     */
    @Query(nativeQuery = true, value = "SELECT " + 
        "u.department_id as departmentId, " +
        "COUNT(attsum) as summaryCount, " +
        "SUM(attsum.total_hours) as totalHours, " +
        "SUM(attsum.overtime_hours) as totalOvertimeHours, " +
        "AVG(attsum.total_hours) as avgTotalHours " +
        "FROM attendance_summaries attsum " +
        "JOIN users u ON u.id = attsum.user_id " +
        "WHERE attsum.summary_type = 'monthly' " +
        "AND EXTRACT(YEAR FROM attsum.target_date) = :year " + 
        "AND EXTRACT(MONTH FROM attsum.target_date) = :month " +
        "AND u.department_id IS NOT NULL " +
        "GROUP BY u.department_id " +
        "ORDER BY SUM(attsum.total_hours) DESC")
    List<java.util.Map<String, Object>> getDepartmentMonthlySummary(@Param("year") int year, @Param("month") int month);

    /**
     * サマリー統計情報取得
     * @return サマリー統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            COUNT(attsum) as totalSummaries,
            COUNT(CASE WHEN attsum.summary_type = 'daily' THEN 1 END) as dailyCount,
            COUNT(CASE WHEN attsum.summary_type = 'monthly' THEN 1 END) as monthlyCount,
            SUM(attsum.total_hours) as grandTotalHours,
            SUM(attsum.overtime_hours) as grandTotalOvertimeHours,
            AVG(attsum.total_hours) as avgTotalHours
        FROM attendance_summaries attsum
        """)
    java.util.Map<String, Object> getSummaryStatistics();

    /**
     * 最新サマリー取得
     * @param userId ユーザーID
     * @param limit 取得件数
     * @return 最新サマリーリスト
     */
    @Query(nativeQuery = true, value = "SELECT attsum.* FROM attendance_summaries attsum WHERE attsum.user_id = :userId ORDER BY attsum.target_date DESC LIMIT :limit")
    List<AttendanceSummary> findLatestByUserId(@Param("userId") Integer userId, @Param("limit") int limit);

    /**
     * 作成日時範囲によるサマリー検索
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 該当期間に作成されたサマリーリスト
     */
    @Query(nativeQuery = true, value = "SELECT attsum.* FROM attendance_summaries attsum WHERE attsum.created_at BETWEEN :startDate AND :endDate ORDER BY attsum.created_at DESC")
    List<AttendanceSummary> findByCreatedAtBetween(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    /**
     * バッチ処理用：大量データサマリー取得
     * @param offset オフセット
     * @param limit 取得件数
     * @return サマリーリスト
     */
    @Query(nativeQuery = true, value = "SELECT attsum.* FROM attendance_summaries attsum ORDER BY attsum.id ASC LIMIT :limit OFFSET :offset")
    List<AttendanceSummary> findForBatchProcessing(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 重複サマリーチェック
     * @param userId ユーザーID
     * @param targetDate 対象日
     * @param summaryType サマリータイプ
     * @param excludeId 除外するID（更新時）
     * @return 重複するサマリーがあるかどうか
     */
    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(attsum) > 0 THEN true ELSE false END FROM attendance_summaries attsum WHERE attsum.user_id = :userId AND attsum.target_date = :targetDate AND attsum.summary_type = :summaryType AND (:excludeId IS NULL OR attsum.id != :excludeId)")
    boolean existsByUserIdAndTargetDateAndSummaryTypeExcludingId(@Param("userId") Integer userId, @Param("targetDate") LocalDate targetDate, @Param("summaryType") String summaryType, @Param("excludeId") Long excludeId);

    /**
     * データ整合性チェック：孤立ユーザー参照検索
     * @return 存在しないユーザーを参照しているサマリーリスト
     */
    @Query(nativeQuery = true, value = """
        SELECT attsum.* FROM attendance_summaries attsum 
        WHERE NOT EXISTS (
            SELECT 1 FROM users u WHERE u.id = attsum.user_id
        )
        """)
    List<AttendanceSummary> findSummariesWithOrphanedUserId();

    /**
     * 異常値検出：極端に長い勤務時間
     * @param threshold 異常値閾値（時間）
     * @return 異常値の可能性があるサマリーリスト
     */
    @Query(nativeQuery = true, value = "SELECT attsum.* FROM attendance_summaries attsum WHERE attsum.total_hours > :threshold ORDER BY attsum.total_hours DESC")
    List<AttendanceSummary> findAnomalousSummaries(@Param("threshold") BigDecimal threshold);
}